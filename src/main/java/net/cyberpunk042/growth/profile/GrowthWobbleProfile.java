package net.cyberpunk042.growth.profile;

import net.minecraft.util.Identifier;

/**
 * Controls the wobble (offset oscillation) applied to a growth block’s shape.
 */
public record GrowthWobbleProfile(
		Identifier id,
		boolean enabled,
		float amplitudeX,
		float amplitudeY,
		float amplitudeZ,
		float speedX,
		float speedY,
		float speedZ) {

	public static GrowthWobbleProfile none() {
		return new GrowthWobbleProfile(
				Identifier.of("the-virus-block", "none_wobble"),
				false,
				0.0F,
				0.0F,
				0.0F,
				0.0F,
				0.0F,
				0.0F);
	}

	public static GrowthWobbleProfile standard() {
		return new GrowthWobbleProfile(
				Identifier.of("the-virus-block", "standard_wobble"),
				true,
				0.08F,
				0.04F,
				0.08F,
				0.23F,
				0.17F,
				0.19F);
	}

	public float clampedAmplitudeX() {
		return Math.max(0.0F, amplitudeX);
	}

	public float clampedAmplitudeY() {
		return Math.max(0.0F, amplitudeY);
	}

	public float clampedAmplitudeZ() {
		return Math.max(0.0F, amplitudeZ);
	}

	public float clampedSpeedX() {
		return Math.max(0.0F, speedX);
	}

	public float clampedSpeedY() {
		return Math.max(0.0F, speedY);
	}

	public float clampedSpeedZ() {
		return Math.max(0.0F, speedZ);
	}
}


