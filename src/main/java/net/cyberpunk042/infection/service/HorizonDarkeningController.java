package net.cyberpunk042.infection.service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.cyberpunk042.config.ColorConfig;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.network.HorizonTintPayload;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

final class HorizonDarkeningController {
	private static final float EPSILON = 0.003F;
	private final Map<RegistryKey<World>, WorldState> worlds = new HashMap<>();

	void update(ServerWorld world, VirusWorldState state, SingularityHudService hud) {
		if (world == null || state == null || hud == null) {
			return;
		}
		HorizonSettings settings = HorizonSettings.snapshot(state);
		if (!settings.enabled()) {
			clearWorld(world, hud);
			return;
		}
		WorldState tracker = worlds.computeIfAbsent(world.getRegistryKey(), key -> new WorldState());
		boolean triggered = isTriggered(state, settings.triggers(), settings.includeDissipation());
		float target = triggered ? settings.maxIntensity() : 0.0F;
		long worldTime = world.getTime();
		tracker.recordWorldTime(worldTime);
		tracker.updateTriggerState(triggered, worldTime);
		boolean allowChange = !triggered || tracker.isDelaySatisfied(worldTime, settings.startDelayTicks());
		float intensity = tracker.advanceToward(target,
				triggered ? settings.fadeInTicks() : settings.fadeOutTicks(),
				allowChange);
		int color = settings.color();

		boolean broadcastNeeded = tracker.shouldBroadcast(triggered, intensity, color);
		boolean syncNeeded = tracker.shouldSync(world, triggered, intensity, color);

		if (broadcastNeeded) {
			dispatch(world, hud, triggered, intensity, color, tracker, true);
		} else if (syncNeeded) {
			dispatch(world, hud, triggered, intensity, color, tracker, false);
		} else {
			tracker.trim(world);
		}

		if (!triggered && tracker.isIdle()) {
			worlds.remove(world.getRegistryKey());
		}
	}

	private static boolean isTriggered(VirusWorldState state,
			Set<SingularityState> triggers,
			boolean includeDissipation) {
		if (!state.infectionState().infected()) {
			return false;
		}
		SingularityState singularity = state.singularityState().singularityState;
		if (singularity == null) {
			return false;
		}
		if (includeDissipation && singularity == SingularityState.DISSIPATION) {
			return true;
		}
		return triggers.contains(singularity);
	}

	private void dispatch(ServerWorld world,
			SingularityHudService hud,
			boolean enabled,
			float intensity,
			int color,
			WorldState tracker,
			boolean force) {
		if (world.getPlayers().isEmpty()) {
			tracker.markBroadcast(enabled, intensity, color);
			return;
		}
		HorizonTintPayload payload = new HorizonTintPayload(enabled, intensity, color);
		long encoded = encode(enabled, intensity, color);
		for (ServerPlayerEntity player : world.getPlayers()) {
			UUID uuid = player.getUuid();
			long cached = tracker.cache().getLong(uuid);
			if (!force && cached == encoded) {
				continue;
			}
			tracker.cache().put(uuid, encoded);
			hud.sendPayload(player, payload);
		}
		tracker.trim(world);
		tracker.markBroadcast(enabled, intensity, color);
	}

	private void clearWorld(ServerWorld world, SingularityHudService hud) {
		WorldState state = worlds.remove(world.getRegistryKey());
		if (state == null || state.cache().isEmpty()) {
			return;
		}
		HorizonTintPayload payload = new HorizonTintPayload(false, 0.0F, state.lastBroadcastColor());
		for (UUID uuid : state.cache().keySet()) {
			PlayerEntity player = world.getPlayerByUuid(uuid);
			if (player instanceof ServerPlayerEntity serverPlayer) {
				hud.sendPayload(serverPlayer, payload);
			}
		}
		state.cache().clear();
		state.resetBroadcast();
	}

	private static long encode(boolean enabled, float intensity, int color) {
		long bits = Float.floatToIntBits(intensity) & 0xFFFFFFFFL;
		long encoded = (bits << 32) | (color & 0xFFFFFFFFL);
		return enabled ? encoded | (1L << 63) : encoded & ~(1L << 63);
	}

	private static final class WorldState {
		private final Object2LongOpenHashMap<UUID> cache = new Object2LongOpenHashMap<>();
		private float currentIntensity;
		private float lastBroadcastIntensity;
		private boolean lastBroadcastEnabled;
		private int lastBroadcastColor = 0xFF000000;
		private boolean broadcastInitialized;
		private long enableTick = -1L;
		private long lastWorldTime;

		private WorldState() {
			cache.defaultReturnValue(Long.MIN_VALUE);
		}

		Object2LongOpenHashMap<UUID> cache() {
			return cache;
		}

		float advanceToward(float target, int durationTicks, boolean allowChange) {
			float clampedTarget = MathHelper.clamp(target, 0.0F, 1.0F);
			if (Math.abs(currentIntensity - clampedTarget) <= EPSILON || durationTicks <= 0) {
				currentIntensity = clampedTarget;
				return currentIntensity;
			}
			if (!allowChange && clampedTarget > currentIntensity) {
				return currentIntensity;
			}
			float step = Math.max(0.001F, 1.0F / Math.max(1, durationTicks));
			if (currentIntensity < clampedTarget) {
				currentIntensity = Math.min(clampedTarget, currentIntensity + step);
			} else {
				currentIntensity = Math.max(clampedTarget, currentIntensity - step);
			}
			if (Math.abs(currentIntensity - clampedTarget) <= EPSILON) {
				currentIntensity = clampedTarget;
			}
			return currentIntensity;
		}

		boolean shouldBroadcast(boolean enabled, float intensity, int color) {
			if (!broadcastInitialized) {
				return intensity > EPSILON;
			}
			if (lastBroadcastEnabled != enabled) {
				return true;
			}
			if (lastBroadcastColor != color) {
				return true;
			}
			return Math.abs(lastBroadcastIntensity - intensity) > EPSILON;
		}

		boolean shouldSync(ServerWorld world, boolean enabled, float intensity, int color) {
			if (world.getPlayers().isEmpty()) {
				return false;
			}
			long encoded = encode(enabled, intensity, color);
			for (ServerPlayerEntity player : world.getPlayers()) {
				if (cache.getLong(player.getUuid()) != encoded) {
					return true;
				}
			}
			return false;
		}

		void markBroadcast(boolean enabled, float intensity, int color) {
			this.lastBroadcastEnabled = enabled;
			this.lastBroadcastIntensity = intensity;
			this.lastBroadcastColor = color;
			this.broadcastInitialized = true;
		}

		void trim(ServerWorld world) {
			if (cache.isEmpty()) {
				return;
			}
			cache.keySet().removeIf(uuid -> world.getPlayerByUuid(uuid) == null);
		}

		boolean isIdle() {
			return cache.isEmpty()
					&& !lastBroadcastEnabled
					&& Math.abs(currentIntensity) <= EPSILON
					&& Math.abs(lastBroadcastIntensity) <= EPSILON;
		}

		int lastBroadcastColor() {
			return lastBroadcastColor;
		}

		void resetBroadcast() {
			broadcastInitialized = false;
			lastBroadcastEnabled = false;
			lastBroadcastIntensity = 0.0F;
		}

		void recordWorldTime(long time) {
			this.lastWorldTime = time;
		}

		void updateTriggerState(boolean triggered, long worldTime) {
			if (triggered) {
				if (enableTick < 0) {
					enableTick = worldTime;
				}
			} else {
				enableTick = -1;
			}
		}

		boolean isDelaySatisfied(long worldTime, int delayTicks) {
			if (delayTicks <= 0) {
				return true;
			}
			if (enableTick < 0) {
				return false;
			}
			return worldTime - enableTick >= delayTicks;
		}
	}

	private record HorizonSettings(boolean enabled,
			int color,
			float maxIntensity,
			int fadeInTicks,
			int fadeOutTicks,
			int startDelayTicks,
			boolean includeDissipation,
			Set<SingularityState> triggers) {
		private static final HorizonSettings DISABLED = new HorizonSettings(false,
				0xFF000000,
				0.0F,
				1,
				1,
				0,
				false,
				EnumSet.noneOf(SingularityState.class));

		static HorizonSettings snapshot(VirusWorldState state) {
			InfectionServiceContainer services = InfectionServices.container();
			if (services == null) {
				return DISABLED;
			}
			ServiceConfig settings = services.settings();
			if (settings == null || settings.singularity == null || settings.singularity.visuals == null) {
				return DISABLED;
			}
			ServiceConfig.HorizonDarkening config = settings.singularity.visuals.horizonDarkening;
			if (config == null || !config.enabled) {
				return DISABLED;
			}
			int color = sanitizeColor(config.color, 0xFF100107);
			float intensity = MathHelper.clamp(config.maxIntensity, 0.0F, 1.0F);
			int fadeIn = Math.max(1, config.fadeInTicks);
			int fadeOut = Math.max(1, config.fadeOutTicks);
			Set<SingularityState> triggers = parsePhases(config.triggerPhases);
			int delay = Math.max(0, config.startDelayTicks);
			return new HorizonSettings(true, color, intensity, fadeIn, fadeOut, delay, config.includeDissipation, triggers);
		}

		private static int sanitizeColor(@Nullable String value, int fallback) {
			Integer parsed = ColorConfig.parseUserColor(value);
			return parsed != null ? parsed : fallback;
		}

		private static Set<SingularityState> parsePhases(@Nullable Iterable<String> entries) {
			EnumSet<SingularityState> set = EnumSet.noneOf(SingularityState.class);
			if (entries != null) {
				for (String entry : entries) {
					if (entry == null || entry.isBlank()) {
						continue;
					}
					try {
						set.add(SingularityState.valueOf(entry.trim().toUpperCase(Locale.ROOT)));
					} catch (IllegalArgumentException ignored) {
					}
				}
			}
			if (set.isEmpty()) {
				set.add(SingularityState.FUSING);
				set.add(SingularityState.COLLAPSE);
				set.add(SingularityState.CORE);
				set.add(SingularityState.RING);
			}
			return set;
		}
	}
}

