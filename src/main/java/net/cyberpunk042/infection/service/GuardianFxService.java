package net.cyberpunk042.infection.service;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.events.GuardianBeamEvent;
import net.cyberpunk042.util.VirusEquipmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Handles guardian push/beam FX and shield notifications so {@link
 * net.cyberpunk042.infection.VirusWorldState} no longer has to talk directly to
 * players or effect buses for those moments.
 */
public final class GuardianFxService {
	public GuardianFxService() {
	}

	private ServiceConfig.Guardian guardianConfig() {
		InfectionServiceContainer services = InfectionServices.container();
		ServiceConfig settings = services != null ? services.settings() : null;
		if (settings == null || settings.guardian == null) {
			return new ServiceConfig.Guardian();
		}
		return settings.guardian;
	}

	public GuardianResult pushPlayers(ServerWorld world, BlockPos center,
			int radius,
			double strengthScale,
			boolean spawnGuardian,
			double difficultyKnockback,
			Predicate<ServerPlayerEntity> filter,
			@Nullable EffectBus effectBus) {
		ServiceConfig.Guardian guardian = guardianConfig();
		GuardianKnockbackSettings settings = guardian.getKnockbackSettings(radius, strengthScale, spawnGuardian);
		double pushRadius = settings.radius();
		double pushRadiusSq = pushRadius * pushRadius;
		if (difficultyKnockback <= 0.0D) {
			return GuardianResult.none();
		}
		double baseStrength = settings.baseStrength(difficultyKnockback);
		double verticalBoost = settings.verticalBoost(difficultyKnockback);
		Vec3d origin = Vec3d.ofCenter(center);
		boolean applyKnockback = settings.applyKnockback();

		int affected = 0;
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (filter != null && !filter.test(player)) {
				continue;
			}
			double distSq = player.squaredDistanceTo(origin);
			if (distSq > pushRadiusSq) {
				continue;
			}
			Vec3d offset = player.getPos().subtract(origin);
			Vec3d horizontal = new Vec3d(offset.x, 0.0D, offset.z);
			if (horizontal.lengthSquared() < 1.0E-4) {
				double angle = world.getRandom().nextDouble() * Math.PI * 2.0D;
				horizontal = new Vec3d(Math.cos(angle), 0.0D, Math.sin(angle));
			}
			if (applyKnockback) {
				// Heavy Pants reduce push by 80%
				double strengthMultiplier = VirusEquipmentHelper.hasHeavyPants(player) ? 0.2D : 1.0D;
				Vec3d pushVec = horizontal.normalize().multiply(baseStrength * strengthMultiplier);
				player.addVelocity(pushVec.x, verticalBoost * strengthMultiplier, pushVec.z);
				player.velocityModified = true;
			}
			if (spawnGuardian && guardian.beams) {
				EffectBus bus = effectBus;
				if (bus == null) {
					InfectionServiceContainer c = InfectionServices.container();
					bus = c != null ? c.createEffectBus() : null;
				}
				if (bus != null) {
					bus.post(new GuardianBeamEvent(world, center.toImmutable(), player, guardianBeamDuration()));
				}
			}
			affected++;
		}
		if (affected > 0) {
			world.playSound(null,
					center,
					SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),
					SoundCategory.HOSTILE,
					1.25F,
					0.55F);
		}
		return new GuardianResult(affected);
	}

	public void notifyShieldStatus(ServerWorld world, BlockPos pos, boolean active, double radiusSq) {
		Text message = active
				? Text.translatable("message.the-virus-block.shield_field.online").formatted(Formatting.AQUA)
				: Text.translatable("message.the-virus-block.shield_field.offline").formatted(Formatting.GRAY);
		Vec3d center = Vec3d.ofCenter(pos);
		world.getPlayers(player -> player.squaredDistanceTo(center) <= radiusSq)
				.forEach(player -> player.sendMessage(message, true));
	}

	public void notifyShieldFailure(ServerWorld world, BlockPos pos) {
		Text message = Text.translatable("message.the-virus-block.shield_field.rejected").formatted(Formatting.RED);
		Vec3d center = Vec3d.ofCenter(pos);
		world.getPlayers(player -> player.squaredDistanceTo(center) <= 1024.0D)
				.forEach(player -> player.sendMessage(message, true));
	}

	private int guardianBeamDuration() {
		int duration = guardianConfig().beamDurationTicks;
		return duration > 0 ? duration : 60;
	}

	public record GuardianResult(int affectedPlayers) {
		public static GuardianResult none() {
			return new GuardianResult(0);
		}

		public boolean anyAffected() {
			return affectedPlayers > 0;
		}
	}

	public static final class GuardianKnockbackSettings {
		private final double radius;
		private final double baseStrengthFactor;
		private final double verticalFactor;
		private final boolean applyKnockback;

		public GuardianKnockbackSettings(double radius, double baseStrengthFactor, double verticalFactor, boolean applyKnockback) {
			this.radius = radius;
			this.baseStrengthFactor = baseStrengthFactor;
			this.verticalFactor = verticalFactor;
			this.applyKnockback = applyKnockback;
		}

		public double radius() {
			return radius;
		}

		public double baseStrength(double difficultyKnockback) {
			return baseStrengthFactor * difficultyKnockback;
		}

		public double verticalBoost(double difficultyKnockback) {
			return verticalFactor * difficultyKnockback;
		}

		public boolean applyKnockback() {
			return applyKnockback;
		}
	}
}

