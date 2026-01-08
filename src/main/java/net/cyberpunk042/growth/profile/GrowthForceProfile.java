package net.cyberpunk042.growth.profile;

import java.util.List;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Force-field preset describing pull or push behaviour.
 */
public record GrowthForceProfile(
		Identifier id,
		boolean enabled,
		int intervalTicks,
		double radius,
		double strength,
		double verticalBoost,
		double falloff,
		double startProgress,
		double endProgress,
		boolean guardianBeams,
		double edgeFalloff,
		int beamColor,
		Identifier particleId,
		int particleCount,
		float particleSpeed,
		Identifier soundId,
		double impactDamage,
		int impactCooldownTicks,
		RingBehavior ringBehavior,
		int ringCount,
		double ringSpacing,
		double ringWidth,
		double ringStrength,
		List<Identifier> ringFieldProfileIds) {

	public GrowthForceProfile {
		ringFieldProfileIds = List.copyOf(ringFieldProfileIds);
	}

	public static GrowthForceProfile defaultsPull() {
		return new GrowthForceProfile(
				Identifier.of("the-virus-block", "default_pull"),
				true,
				15,
				10.0D,
				0.7D,
				0.25D,
				0.85D,
				0.0D,
				1.0D,
				false,
				0.25D,
				0xFF6600,
				Identifier.of("minecraft", "enchant"),
				6,
				0.01F,
				Identifier.of("minecraft", "block.respawn_anchor.ambient"),
				0.0D,
				10,
				RingBehavior.NONE,
				0,
				0.0D,
				0.0D,
				1.0D,
				List.of());
	}

	public static GrowthForceProfile defaultsPush() {
		return new GrowthForceProfile(
				Identifier.of("the-virus-block", "default_push"),
				true,
				20,
				12.0D,
				1.0D,
				0.35D,
				0.65D,
				0.0D,
				0.5D,
				true,
				0.2D,
				0xFFCC66,
				Identifier.of("minecraft", "poof"),
				8,
				0.02F,
				Identifier.of("minecraft", "block.respawn_anchor.deplete"),
				0.0D,
				10,
				RingBehavior.NONE,
				0,
				0.0D,
				0.0D,
				1.0D,
				List.of());
	}

	public static GrowthForceProfile vortexPull() {
		return new GrowthForceProfile(
				Identifier.of("the-virus-block", "vortex_pull"),
				true,
				10,
				14.0D,
				0.9D,
				0.4D,
				0.65D,
				0.15D,
				0.95D,
				true,
				0.15D,
				0xFF1E90,
				Identifier.of("minecraft", "portal"),
				10,
				0.04F,
				Identifier.of("minecraft", "entity.guardian.attack"),
				3.0D,
				15,
				RingBehavior.KEEP_ON_RING,
				1,
				0.0D,
				0.8D,
				1.4D,
				List.of(Identifier.of("the-virus-block", "ring_default")));
	}

	public static GrowthForceProfile shockPush() {
		return new GrowthForceProfile(
				Identifier.of("the-virus-block", "shock_push"),
				true,
				12,
				11.0D,
				1.25D,
				0.6D,
				0.7D,
				0.0D,
				0.45D,
				true,
				0.1D,
				0xFFAA00,
				Identifier.of("minecraft", "sonic_boom"),
				14,
				0.05F,
				Identifier.of("minecraft", "entity.guardian.attack"),
				4.0D,
				20,
				RingBehavior.NONE,
				0,
				0.0D,
				0.0D,
				1.0D,
				List.of());
	}

	public int sanitizedInterval() {
		return Math.max(1, intervalTicks);
	}

	public double clampedRadius() {
		return Math.max(0.5D, radius);
	}

	public double clampedStrength() {
		return Math.max(0.0D, strength);
	}

	public double clampedFalloff() {
		return Math.max(0.1D, falloff);
	}

	public double clampedStartProgress() {
		return MathHelper.clamp(startProgress, 0.0D, 1.0D);
	}

	public double clampedEndProgress() {
		return Math.max(clampedStartProgress(), MathHelper.clamp(endProgress, 0.0D, 1.0D));
	}

	public double clampedEdgeFalloff() {
		return Math.max(0.0D, Math.min(0.5D, edgeFalloff));
	}

	public float[] beamColorFloats() {
		float r = ((beamColor >> 16) & 0xFF) / 255.0F;
		float g = ((beamColor >> 8) & 0xFF) / 255.0F;
		float b = (beamColor & 0xFF) / 255.0F;
		return new float[] { r, g, b };
	}

	public double clampedImpactDamage() {
		return Math.max(0.0D, impactDamage);
	}

	public int sanitizedImpactCooldown() {
		return Math.max(1, impactCooldownTicks);
	}

	public boolean hasRingConfig() {
		return ringBehavior != RingBehavior.NONE && sanitizedRingCount() > 0 && sanitizedRingWidth() > 0.0D;
	}

	public RingBehavior ringBehavior() {
		return ringBehavior != null ? ringBehavior : RingBehavior.NONE;
	}

	public int sanitizedRingCount() {
		return Math.max(0, ringCount);
	}

	public double sanitizedRingSpacing() {
		return Math.max(0.0D, ringSpacing);
	}

	public double sanitizedRingWidth() {
		return Math.max(0.0D, ringWidth);
	}

	public double clampedRingStrength() {
		return Math.max(0.0D, ringStrength <= 0.0D ? 1.0D : ringStrength);
	}

	public List<Identifier> ringFieldProfiles() {
		return ringFieldProfileIds;
	}

	public double baseRingRadius() {
		return clampedRadius();
	}

	public enum RingBehavior {
		NONE,
		KEEP_ON_RING,
		KEEP_INSIDE,
		KEEP_OUTSIDE;

		public static RingBehavior fromString(String raw) {
			if (raw == null || raw.isEmpty()) {
				return NONE;
			}
			return switch (raw.toLowerCase()) {
				case "keep_inside", "inside" -> KEEP_INSIDE;
				case "keep_outside", "outside" -> KEEP_OUTSIDE;
				case "keep_on_ring", "ring", "band" -> KEEP_ON_RING;
				default -> NONE;
			};
		}
	}
}

