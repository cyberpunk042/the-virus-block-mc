package net.cyberpunk042.growth.profile;

import net.minecraft.util.Identifier;

/**
 * Defines how detonation behaves when a growth block resolves destruction.
 */
public record GrowthExplosionProfile(
		Identifier id,
		double radius,
		int charges,
		int amount,
		int amountDelayTicks,
		double maxDamage,
		double damageScaling,
		boolean causesFire,
		boolean breaksBlocks) {

	public static GrowthExplosionProfile defaults() {
		return new GrowthExplosionProfile(
				Identifier.of("the-virus-block", "default_explosion"),
				3.5D,
				1,
				1,
				0,
				20.0D,
				1.0D,
				false,
				true);
	}

	public float sanitizedRadius() {
		return (float) Math.max(0.1D, radius);
	}

	public int sanitizedCharges() {
		return Math.max(1, charges);
	}

	public int sanitizedAmount() {
		return Math.max(1, amount);
	}

	public int sanitizedAmountDelay() {
		return Math.max(0, amountDelayTicks);
	}

	public double sanitizedMaxDamage() {
		if (Double.isNaN(maxDamage) || maxDamage <= 0.0D) {
			return 0.0D;
		}
		return maxDamage;
	}

	public double clampedDamageScaling() {
		return Math.max(0.1D, damageScaling);
	}

	public boolean causesFire() {
		return causesFire;
	}

	public boolean breaksBlocks() {
		return breaksBlocks;
	}
}

