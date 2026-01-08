package net.cyberpunk042.infection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.network.DifficultySyncPayload;
import net.cyberpunk042.network.SkyTintPayload;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class VirusTierBossBar {
	private static final class BossBars {
		ServerBossBar tierBar;
		ServerBossBar singularityBar;
	}

	private static final Map<RegistryKey<World>, BossBars> BARS = new HashMap<>();
	private static final Object2ByteMap<UUID> SKY_TINT = new Object2ByteOpenHashMap<>();
	
	private static volatile boolean initialized = false;

	private VirusTierBossBar() {
	}

	public static void init() {
		if (initialized) return;
		initialized = true;
		
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SKY_TINT.removeByte(handler.player.getUuid()));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SKY_TINT.removeByte(handler.player.getUuid());
			ServerPlayNetworking.send(handler.player, new SkyTintPayload(false, false));
			ServerWorld world = handler.player.getWorld();
			VirusWorldState state = VirusWorldState.get(world);
			ServerPlayNetworking.send(handler.player, new DifficultySyncPayload(state.tiers().difficulty()));
		});

		// Cleanup on world unload to prevent memory leaks
		ServerWorldEvents.UNLOAD.register((server, world) -> {
			BossBars bars = BARS.remove(world.getRegistryKey());
			if (bars != null) {
				if (bars.tierBar != null) {
					clearBar(bars.tierBar);
				}
				if (bars.singularityBar != null) {
					clearBar(bars.singularityBar);
				}
			}
		});

		// Full cleanup on server stop
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BARS.clear();
			SKY_TINT.clear();
		});
	}

	public static void update(ServerWorld world, VirusWorldState state) {
		syncSkyTint(world, state);
		RegistryKey<World> key = world.getRegistryKey();
		BossBars bars = BARS.get(key);
		if (!state.infectionState().infected()) {
			if (bars != null) {
				if (bars.tierBar != null) {
					clearBar(bars.tierBar);
				}
				if (bars.singularityBar != null) {
					clearBar(bars.singularityBar);
				}
				BARS.remove(key);
			}
			return;
		}

		if (bars == null) {
			bars = new BossBars();
			BARS.put(key, bars);
		}

		updateTierBar(world, state, bars);
		updateSingularityBar(world, state, bars);
	}

	private static void updateTierBar(ServerWorld world, VirusWorldState state, BossBars bars) {
		if (state.singularityState().singularityState != SingularityState.DORMANT) {
			if (bars.tierBar != null) {
				clearBar(bars.tierBar);
			}
			return;
		}
		ServerBossBar bar = bars.tierBar;
		if (bar == null) {
			bar = createBar();
			bars.tierBar = bar;
		}
		syncPlayers(world, bar);
		bar.setVisible(true);
		updateBarSegments(bar, state);
	}

	private static void updateSingularityBar(ServerWorld world, VirusWorldState state, BossBars bars) {
		SingularityState singularityState = state.singularityState().singularityState;
		boolean active = singularityState == SingularityState.FUSING || singularityState == SingularityState.COLLAPSE;
		ServerBossBar bar = bars.singularityBar;
		if (!active) {
			if (bar != null) {
				clearBar(bar);
			}
			return;
		}
		if (bar == null) {
			bar = createSingularityBar();
			bars.singularityBar = bar;
		}
		syncPlayers(world, bar);
		bar.setVisible(true);
		if (singularityState == SingularityState.FUSING) {
			updateSingularityFuseBar(bar, state);
		} else {
			updateSingularityCollapseBar(bar, state);
		}
	}

	private static ServerBossBar createBar() {
		ServerBossBar bar = new ServerBossBar(
				Text.literal("Virus Tier"),
				BossBar.Color.PURPLE,
				BossBar.Style.NOTCHED_10);
		bar.setVisible(false);
		return bar;
	}

	private static ServerBossBar createSingularityBar() {
		ServerBossBar bar = new ServerBossBar(
				Text.translatable("bossbar.the-virus-block.singularity.fuse"),
				BossBar.Color.YELLOW,
				BossBar.Style.PROGRESS);
		bar.setVisible(false);
		return bar;
	}

	private static void clearBar(ServerBossBar bar) {
		bar.setVisible(false);
		bar.setPercent(0.0F);
		for (ServerPlayerEntity player : List.copyOf(bar.getPlayers())) {
			bar.removePlayer(player);
		}
	}

	private static void updateBarSegments(ServerBossBar bar, VirusWorldState state) {
		float progressRatio = getProgressRatio(state);
		float healthRatio = (float) state.tiers().getHealthPercent();
		int displayTier = state.tiers().currentTier().getIndex() + 1;
		String key = state.infectionState().dormant() ? "bossbar.the-virus-block.state.calm" : "bossbar.the-virus-block.state.active";

		if (state.tiers().isApocalypseMode()) {
			MutableText vulnerableTitle = Text.translatable("bossbar.the-virus-block.vulnerable")
					.formatted(Formatting.RED)
					.append(Text.literal(String.format(" [%d%%]", Math.round(healthRatio * 100))).formatted(Formatting.DARK_RED));
			bar.setName(vulnerableTitle);
			bar.setPercent(MathHelper.clamp(healthRatio, 0.0F, 1.0F));
			bar.setColor(BossBar.Color.RED);
			return;
		}

		MutableText title = Text.translatable("bossbar.the-virus-block.tier", displayTier, Text.translatable(key))
				.formatted(state.infectionState().dormant() ? Formatting.AQUA : Formatting.DARK_PURPLE)
				.append(Text.literal(String.format("  [P:%d%%] ", Math.round(progressRatio * 100))).formatted(Formatting.AQUA))
				.append(Text.literal(String.format("[H:%d%%]", Math.round(healthRatio * 100))).formatted(Formatting.DARK_RED));
		bar.setName(title);

		bar.setPercent(progressRatio);
		bar.setColor(progressRatio >= 1.0F ? BossBar.Color.RED : BossBar.Color.PURPLE);
	}

	private static void updateSingularityFuseBar(ServerBossBar bar, VirusWorldState state) {
		float progress = MathHelper.clamp(state.singularity().fusing().fuseProgress(), 0.0F, 1.0F);
		int percent = Math.round(progress * 100.0F);
		MutableText title = Text.translatable("bossbar.the-virus-block.singularity.fuse")
				.formatted(Formatting.GOLD)
				.append(Text.literal(String.format(" [%d%%]", percent)).formatted(Formatting.YELLOW));
		bar.setName(title);
		bar.setColor(BossBar.Color.YELLOW);
		bar.setPercent(progress);
	}

	private static void updateSingularityCollapseBar(ServerBossBar bar, VirusWorldState state) {
		float remaining = MathHelper.clamp(state.singularity().fusing().collapseProgress(), 0.0F, 1.0F);
		int percent = Math.round(remaining * 100.0F);
		boolean priming = state.singularityState().singularityCollapseBarDelay > 0;
		String key = priming
				? "bossbar.the-virus-block.singularity.collapse_priming"
				: "bossbar.the-virus-block.singularity.collapse";
		MutableText title = Text.translatable(key)
				.formatted(Formatting.DARK_RED)
				.append(Text.literal(String.format(" [%d%%]", percent)).formatted(Formatting.RED));
		bar.setName(title);
		bar.setColor(BossBar.Color.RED);
		bar.setPercent(remaining);
	}

	private static float getProgressRatio(VirusWorldState state) {
		int duration = state.tiers().duration(state.tiers().currentTier());
		if (duration <= 0) {
			return 1.0F;
		}
		return MathHelper.clamp((float) state.tiers().ticksInTier() / (float) duration, 0.0F, 1.0F);
	}

	private static void syncPlayers(ServerWorld world, ServerBossBar bar) {
		// Use Set for O(1) contains check instead of O(n) List.contains
		java.util.Set<ServerPlayerEntity> currentPlayers = new java.util.HashSet<>(bar.getPlayers());
		java.util.Set<ServerPlayerEntity> worldPlayers = new java.util.HashSet<>(world.getPlayers());
		
		// Remove players not in world
		for (ServerPlayerEntity player : currentPlayers) {
			if (!worldPlayers.contains(player)) {
				bar.removePlayer(player);
			}
		}
		
		// Add players in world not yet in bar
		for (ServerPlayerEntity player : worldPlayers) {
			if (!currentPlayers.contains(player)) {
				bar.addPlayer(player);
			}
		}
	}

	private static void syncSkyTint(ServerWorld world, VirusWorldState state) {
		boolean skyCorrupted = state.infectionState().infected() && (state.tiers().currentTier().getIndex() >= 0 || state.tiers().isApocalypseMode());
		boolean fluidsCorrupted = state.tiers().areLiquidsCorrupted(world);
		byte encoded = encodeSkyState(skyCorrupted, fluidsCorrupted);
		for (ServerPlayerEntity player : world.getPlayers()) {
			byte cached = SKY_TINT.getOrDefault(player.getUuid(), (byte) 0);
			if (cached == encoded) {
				continue;
			}
			SKY_TINT.put(player.getUuid(), encoded);
			ServerPlayNetworking.send(player, new SkyTintPayload(skyCorrupted, fluidsCorrupted));
		}
	}

	private static byte encodeSkyState(boolean sky, boolean fluids) {
		byte value = 0;
		if (sky) {
			value |= 1;
		}
		if (fluids) {
			value |= 2;
		}
		return value;
	}
}

