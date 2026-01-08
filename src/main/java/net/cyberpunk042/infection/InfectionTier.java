package net.cyberpunk042.infection;

import java.util.Arrays;

public enum InfectionTier {
	// Per-tier values: index, duration, bursts, auraRadius, health, boobytrap, mobSpawn, corruptionSpread
	ONE(0, 2400, 3, 4.5F, 120.0D, 0.25F, 0.0F, 0.2F),
	TWO(1, 3600, 5, 5.5F, 220.0D, 0.5F, 0.25F, 0.4F),
	THREE(2, 4800, 7, 6.5F, 320.0D, 0.85F, 0.5F, 0.6F),
	FOUR(3, 3600, 9, 8.0F, 420.0D, 1.1F, 0.85F, 0.8F),
	FIVE(4, 4800, 12, 10.0F, 600.0D, 1.4F, 1.2F, 1.0F);

	private final int index;
	private final int durationTicks;
	private final int mutationBursts;
	private final float baseAuraRadius;
	private final double baseHealth;
	private final float boobytrapMultiplier;
	private final float mobSpawnMultiplier;
	private final float corruptionSpreadMultiplier;

	InfectionTier(int index, int durationTicks, int mutationBursts, float baseAuraRadius, double baseHealth, float boobytrapMultiplier, float mobSpawnMultiplier, float corruptionSpreadMultiplier) {
		this.index = index;
		this.durationTicks = durationTicks;
		this.mutationBursts = mutationBursts;
		this.baseAuraRadius = baseAuraRadius;
		this.baseHealth = baseHealth;
		this.boobytrapMultiplier = boobytrapMultiplier;
		this.mobSpawnMultiplier = mobSpawnMultiplier;
		this.corruptionSpreadMultiplier = corruptionSpreadMultiplier;
	}

	public int getIndex() {
		return index;
	}

	public int getLevel() {
		return index + 1;
	}

	public boolean isAtLeast(InfectionTier other) {
		return this.index >= other.index;
	}

	public boolean isBelow(InfectionTier other) {
		return this.index < other.index;
	}

	public int getDurationTicks() {
		return durationTicks;
	}

	public int getMutationBursts() {
		return mutationBursts;
	}

	public float getBaseAuraRadius() {
		return baseAuraRadius;
	}

	public double getBaseHealth() {
		return baseHealth;
	}

	public float getBoobytrapMultiplier() {
		return boobytrapMultiplier;
	}

	public float getMobSpawnMultiplier() {
		return mobSpawnMultiplier;
	}

	public float getCorruptionSpreadMultiplier() {
		return corruptionSpreadMultiplier;
	}

	public InfectionTier next() {
		return values()[Math.min(values().length - 1, index + 1)];
	}

	public static InfectionTier byIndex(int index) {
		return Arrays.stream(values())
				.filter(tier -> tier.index == index)
				.findFirst()
				.orElse(FIVE);
	}

	public static int maxIndex() {
		return values()[values().length - 1].index;
	}
}

