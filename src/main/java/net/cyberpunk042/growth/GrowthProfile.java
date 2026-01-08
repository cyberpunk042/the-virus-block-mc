package net.cyberpunk042.growth;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Preset describing how a growth block scales over time.
 */
public record GrowthProfile(
		Identifier id,
		boolean growthEnabled,
		int rateTicks,
		double rateScale,
		double startScale,
		double targetScale,
		double minScale,
		double maxScale) {

	public static GrowthProfile defaults() {
		return new GrowthProfile(
				Identifier.of("the-virus-block", "default_growth"),
				true,
				5,
				1.0D,
				0.35D,
				1.0D,
				0.25D,
				1.4D);
	}

	public int sanitizedRate() {
		return Math.max(1, rateTicks);
	}

	public double clampedRateScale() {
		return Math.max(0.01D, rateScale);
	}

	public double clampedStartScale() {
		return MathHelper.clamp(startScale, minScale, maxScale);
	}

	public double clampedTargetScale() {
		return MathHelper.clamp(targetScale, minScale, maxScale);
	}

	public double clampedMinScale() {
		return Math.max(0.01D, Math.min(minScale, maxScale));
	}

	public double clampedMaxScale() {
		return Math.max(clampedMinScale() + 1.0E-4D, maxScale);
	}
}


