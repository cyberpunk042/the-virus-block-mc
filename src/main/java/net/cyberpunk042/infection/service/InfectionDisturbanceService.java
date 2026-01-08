package net.cyberpunk042.infection.service;

import java.util.Objects;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Handles player disturbances and explosion impacts that adjust tier timers.
 */
public final class InfectionDisturbanceService {

	private final VirusWorldState host;

	public InfectionDisturbanceService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void disturbByPlayer() {
		ServerWorld world = host.world();
		if (!host.infectionState().infected()) {
			return;
		}
		InfectionTier tier = host.tiers().currentTier();
		long bonus = 40L + tier.getIndex() * 30L;
		if (tier.getIndex() >= 3) {
			bonus += 60L;
		}
		host.infectionState().setDormant(false);
		applyDisturbance(world, bonus);
		host.infectionState().setDormant(false);
		BlockPos disturbance = host.infection().representativePos(world, world.getRandom(), host.sourceState());
		if (disturbance == null && host.hasVirusSources()) {
			disturbance = host.getVirusSources().isEmpty() ? null : host.getVirusSources().iterator().next();
		}
		if (disturbance == null) {
			disturbance = BlockPos.ORIGIN;
		}
		world.playSound(null, disturbance, SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.BLOCKS, 1.0F, 0.4F);
		host.markDirty();
	}

	public void handleExplosionImpact(Entity source, Vec3d center, double radius) {
		ServerWorld world = host.world();
		if (!host.infectionState().infected() || radius <= 0.0D || !host.hasVirusSources()) {
			return;
		}
		if (source != null && source.getCommandTags().contains(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG)) {
			return;
		}
		double radiusSq = radius * radius;
		boolean hit = host.getVirusSources().stream().anyMatch(core -> core.toCenterPos().squaredDistanceTo(center) <= radiusSq);
		if (!hit) {
			return;
		}
		long bonus = MathHelper.floor(60L + radius * 6.0D);
		bonus = MathHelper.clamp(bonus, 40L, 400L);
		if (host.tiers().isApocalypseMode()) {
			double damageScale = MathHelper.clamp(radius / 4.0D, 0.5D, 3.5D);
			// Classify the explosion for viral adaptation
			String damageKey = net.cyberpunk042.infection.VirusDamageClassifier.classifyExplosion(source);
			host.infection().applyHealthDamage(world, host.tiers().maxHealth(host.tiers().currentTier()) * (damageScale * 0.02D), damageKey);
			return;
		}
		applyDisturbance(world, bonus);
	}

	private void applyDisturbance(ServerWorld world, long bonus) {
		if (host.tiers().applyDisturbance(host.tiers().currentTier(), bonus)) {
			host.markDirty();
		}
	}
}

