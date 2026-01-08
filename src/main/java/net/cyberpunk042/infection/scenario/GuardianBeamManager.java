package net.cyberpunk042.infection.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.api.VirusWorldContext;
import net.cyberpunk042.infection.events.GuardianBeamEvent;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.cyberpunk042.mixin.GuardianEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

final class GuardianBeamManager implements AutoCloseable {
	private final Object2LongMap<UUID> activeBeams = new Object2LongOpenHashMap<>();
	private final List<Registration<?>> registrations = new ArrayList<>();
	private EffectBus bus;
	private ServerWorld world;
	private ServiceConfig.Guardian config = new ServiceConfig.Guardian();

	void install(VirusWorldContext context) {
		this.bus = context.effectBus();
		this.world = context.world();
		this.config = resolveConfig(context);
		if (config.beams) {
			register(GuardianBeamEvent.class, this::onGuardianBeam);
		}
	}

	void tick() {
		if (activeBeams.isEmpty() || world == null || !config.beams) {
			return;
		}
		long now = world.getTime();
		ObjectIterator<Object2LongMap.Entry<UUID>> iterator = activeBeams.object2LongEntrySet().iterator();
		while (iterator.hasNext()) {
			Object2LongMap.Entry<UUID> entry = iterator.next();
			if (now >= entry.getLongValue()) {
				Entity entity = world.getEntity(entry.getKey());
				if (entity != null) {
					entity.discard();
				}
				iterator.remove();
			}
		}
	}

	private <T> void register(Class<T> type, Consumer<T> handler) {
		bus.register(type, handler);
		registrations.add(new Registration<>(type, handler));
	}

	@Override
	public void close() {
		for (Registration<?> registration : registrations) {
			unregister(registration);
		}
		registrations.clear();
		if (world != null) {
			for (UUID id : activeBeams.keySet()) {
				Entity entity = world.getEntity(id);
				if (entity != null) {
					entity.discard();
				}
			}
		}
		activeBeams.clear();
	}

	private <T> void unregister(Registration<T> registration) {
		bus.unregister(registration.type(), registration.handler());
	}

	private void onGuardianBeam(GuardianBeamEvent event) {
		if (world == null || event.world() != world || !config.beams) {
			return;
		}
		spawnGuardianBeam(event.origin(), event.target(), event.durationTicks());
	}

	private void spawnGuardianBeam(BlockPos origin, ServerPlayerEntity target, int duration) {
		if (!config.beams) {
			return;
		}
		GuardianEntity guardian = EntityType.GUARDIAN.create(world, SpawnReason.TRIGGERED);
		if (guardian == null) {
			return;
		}
		guardian.refreshPositionAndAngles(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D,
				world.getRandom().nextFloat() * 360.0F,
				0.0F);
		guardian.setAiDisabled(true);
		guardian.setInvisible(true);
		guardian.clearStatusEffects();
		guardian.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration + 20, 0, false, false));
		guardian.setGlowing(false);
		guardian.setCustomNameVisible(false);
		guardian.setSilent(true);
		guardian.setNoGravity(true);
		guardian.setInvulnerable(true);
		guardian.setTarget(target);
		guardian.addCommandTag(TheVirusBlock.VIRUS_DEFENSE_BEAM_TAG);
		if (!world.spawnEntity(guardian)) {
			return;
		}
		((GuardianEntityAccessor) guardian).theVirusBlock$setBeamTarget(target.getId());
		world.sendEntityStatus(guardian, EntityStatuses.PLAY_GUARDIAN_ATTACK_SOUND);
		activeBeams.put(guardian.getUuid(), world.getTime() + duration);
	}

	private static ServiceConfig.Guardian resolveConfig(VirusWorldContext context) {
		InfectionServiceContainer services = context.singularity().services();
		if (services == null) {
			return new ServiceConfig.Guardian();
		}
		ServiceConfig.Guardian source = services.settings().guardian;
		ServiceConfig.Guardian copy = new ServiceConfig.Guardian();
		copy.beams = source.beams;
		copy.pushRadius = source.pushRadius;
		copy.pushStrength = source.pushStrength;
		copy.knockbackEnabled = source.knockbackEnabled;
		copy.beamDurationTicks = source.beamDurationTicks;
		return copy;
	}

	private record Registration<T>(Class<T> type, Consumer<T> handler) {
	}
}

