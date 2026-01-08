package net.cyberpunk042.growth.profile;

import net.minecraft.util.Identifier;

public record GrowthParticleProfile(
		Identifier id,
		Identifier particleId,
		int count,
		double speed,
		Identifier soundId,
		int soundIntervalTicks,
		int intervalTicks,
		Shape shape,
		double radius,
		double height,
		double offsetX,
		double offsetY,
		double offsetZ,
		double jitterX,
		double jitterY,
		double jitterZ,
		boolean followScale) {

	public static GrowthParticleProfile defaults() {
		return new GrowthParticleProfile(
				Identifier.of("the-virus-block", "default"),
				Identifier.of("minecraft", "small_flame"),
				0,
				0.01D,
				Identifier.of("minecraft", "ambient.cave"),
				60,
				20,
				Shape.SPHERE,
				0.6D,
				1.0D,
				0.0D,
				0.0D,
				0.0D,
				0.0D,
				0.0D,
				0.0D,
				true);
	}

	public int sanitizedCount() {
		return Math.max(0, count);
	}

	public double sanitizedSpeed() {
		return Math.max(0.0D, speed);
	}

	public int sanitizedSoundInterval() {
		return Math.max(1, soundIntervalTicks);
	}

	public int sanitizedInterval() {
		return Math.max(1, intervalTicks);
	}

	public double clampedRadius() {
		return Math.max(0.05D, radius);
	}

	public double clampedHeight() {
		return Math.max(0.05D, height);
	}

	public double clampedJitterX() {
		return Math.max(0.0D, jitterX);
	}

	public double clampedJitterY() {
		return Math.max(0.0D, jitterY);
	}

	public double clampedJitterZ() {
		return Math.max(0.0D, jitterZ);
	}

	public Shape shape() {
		return shape != null ? shape : Shape.SPHERE;
	}

	public enum Shape {
		SPHERE,
		SHELL,
		RING,
		COLUMN
	}
}

