package net.cyberpunk042.growth;

import net.minecraft.util.Identifier;

/**
 * Immutable configuration describing how a progressive growth block behaves.
 * Instances are loaded from JSON and referenced by runtime block entities.
 */
public record GrowthBlockDefinition(
		Identifier id,
		boolean growthEnabled,
		int rateTicks,
		double rateScale,
		double startScale,
		double targetScale,
		double minScale,
		double maxScale,
		boolean hasCollision,
		boolean doesDestruction,
		boolean hasFuse,
		double touchDamage,
		Identifier growthProfileId,
		Identifier glowProfileId,
		Identifier particleProfileId,
		Identifier fieldProfileId,
		Identifier pullProfileId,
		Identifier pushProfileId,
		Identifier fuseProfileId,
		Identifier explosionProfileId,
		Identifier opacityProfileId,
		Identifier spinProfileId,
		Identifier wobbleProfileId) {

	public static GrowthBlockDefinition defaults() {
		return new GrowthBlockDefinition(
				Identifier.of("the-virus-block", "magma"),
				true,
				5,
				1.0D,
				0.35D,
				1.0D,
				0.25D,
				1.4D,
				true,
				false,
				false,
				4.0D,
				Identifier.of("the-virus-block", "default_growth"),
				Identifier.of("the-virus-block", "magma"),
				Identifier.of("the-virus-block", "none_particle"),
				Identifier.of("the-virus-block", "none_field"),
				Identifier.of("the-virus-block", "none_pull"),
				Identifier.of("the-virus-block", "none_push"),
				Identifier.of("the-virus-block", "none_fuse"),
				Identifier.of("the-virus-block", "default_explosion"),
				Identifier.of("the-virus-block", "default_opacity"),
				Identifier.of("the-virus-block", "default_spin"),
				Identifier.of("the-virus-block", "none_wobble"));
	}

	public int sanitizedRate() {
		return Math.max(1, rateTicks);
	}

	public double clampedRateScale() {
		return Math.max(0.01D, rateScale);
	}

	public double clampedStartScale() {
		return Math.max(minScale, Math.min(maxScale, startScale));
	}

	public double clampedTargetScale() {
		return Math.max(minScale, Math.min(maxScale, targetScale));
	}
}

