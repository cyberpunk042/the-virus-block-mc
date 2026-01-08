package net.cyberpunk042.infection.service;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusDifficulty;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

/**
 * Holds tier progression, containment, health, and difficulty state.
 * Encapsulating this logic keeps {@link net.cyberpunk042.infection.VirusWorldState} leaner.
 */
public final class InfectionTierService {

	public static final class State {
		int tierIndex;
		long ticksInTier;
		int containmentLevel;
		boolean apocalypseMode;
		double healthScale = 1.0D;
		double currentHealth;
		int surfaceMutationBudget;
		long surfaceMutationBudgetTick = -1L;
		VirusDifficulty difficulty = VirusDifficulty.HARD;
		boolean difficultyPromptShown;
	}

	public InfectionTier getCurrentTier(State state) {
		return InfectionTier.byIndex(MathHelper.clamp(state.tierIndex, 0, InfectionTier.maxIndex()));
	}

	public int getTierIndex(State state) {
		return MathHelper.clamp(state.tierIndex, 0, InfectionTier.maxIndex());
	}

	public void setTierIndex(State state, int tierIndex) {
		state.tierIndex = MathHelper.clamp(tierIndex, 0, InfectionTier.maxIndex());
	}

	public long getTicksInTier(State state) {
		return Math.max(0L, state.ticksInTier);
	}

	public void setTicksInTier(State state, long ticks) {
		state.ticksInTier = Math.max(0L, ticks);
	}

	public void incrementTierTick(State state) {
		state.ticksInTier = Math.max(0L, state.ticksInTier + 1L);
	}

	public boolean applyContainmentCharge(State state, int amount) {
		int previous = state.containmentLevel;
		state.containmentLevel = MathHelper.clamp(previous + amount, 0, 10);
		return previous != state.containmentLevel;
	}

	public boolean degradeContainment(State state) {
		if (state.containmentLevel <= 0) {
			return false;
		}
		state.containmentLevel--;
		return true;
	}

	public int getContainmentLevel(State state) {
		return MathHelper.clamp(state.containmentLevel, 0, 10);
	}

	public void setContainmentLevel(State state, int level) {
		state.containmentLevel = MathHelper.clamp(level, 0, 10);
	}

	public boolean isApocalypseMode(State state) {
		return state.apocalypseMode;
	}

	public void setApocalypseMode(State state, boolean value) {
		state.apocalypseMode = value;
	}

	public void resetForInfectionStart(State state) {
		state.tierIndex = 0;
		state.ticksInTier = 0L;
		state.containmentLevel = 0;
		state.apocalypseMode = false;
		state.surfaceMutationBudget = 0;
		state.surfaceMutationBudgetTick = -1L;
		state.healthScale = 1.0D;
		state.currentHealth = 0.0D;
	}

	public void clearForEnd(State state) {
		resetForInfectionStart(state);
		state.currentHealth = 0.0D;
	}

	public TierAdvanceResult advanceTier(State state) {
		if (state.tierIndex >= InfectionTier.maxIndex()) {
			state.apocalypseMode = true;
			return TierAdvanceResult.apocalypse();
		}
		state.tierIndex = Math.min(InfectionTier.maxIndex(), state.tierIndex + 1);
		state.ticksInTier = 0L;
		return TierAdvanceResult.tierAdvanced(InfectionTier.byIndex(state.tierIndex));
	}

	public boolean ensureHealthInitialized(State state, InfectionTier tier) {
		boolean mutated = false;
		if (state.healthScale <= 0.0D) {
			state.healthScale = 1.0D;
			mutated = true;
		}
		double max = getMaxHealth(state, tier);
		if (state.currentHealth <= 0.0D || state.currentHealth > max) {
			state.currentHealth = max;
			mutated = true;
		}
		return mutated;
	}

	public void resetHealthForTier(State state, InfectionTier tier) {
		state.healthScale = 1.0D;
		state.currentHealth = getMaxHealth(state, tier);
	}

	public double getMaxHealth(State state, InfectionTier tier) {
		double base = Math.max(1.0D, tier.getBaseHealth());
		// Scale capped at 10.0 to support up to 10 virus blocks (1.0 per block)
		double scale = MathHelper.clamp(state.healthScale <= 0.0D ? 1.0D : state.healthScale, 0.1D, 10.0D);
		return Math.max(1.0D, base * scale);
	}

	public double getCurrentHealth(State state) {
		return Math.max(0.0D, state.currentHealth);
	}

	public void setCurrentHealth(State state, double value) {
		state.currentHealth = Math.max(0.0D, value);
	}

	public double getHealthScale(State state) {
		return state.healthScale <= 0.0D ? 1.0D : state.healthScale;
	}

	public void setHealthScale(State state, double scale) {
		state.healthScale = Math.max(0.1D, scale);
	}

	public HealthChange applyHealthDamage(State state, InfectionTier tier, double amount) {
		if (amount <= 0.0D) {
			return HealthChange.NONE;
		}
		double previous = state.currentHealth;
		state.currentHealth = Math.max(0.0D, previous - amount);
		if (Math.abs(previous - state.currentHealth) < 0.0001D) {
			return HealthChange.NONE;
		}
		double max = getMaxHealth(state, tier);
		boolean crossed = previous > max * 0.5D && state.currentHealth <= max * 0.5D;
		boolean depleted = state.currentHealth <= 0.0D;
		return new HealthChange(true, crossed, depleted);
	}

	public boolean reduceMaxHealth(State state, InfectionTier tier, double factor) {
		if (factor <= 0.0D || factor >= 1.0D) {
			return false;
		}
		double previousMax = getMaxHealth(state, tier);
		double newScale = Math.max(0.1D, getHealthScale(state) * factor);
		if (Math.abs(newScale - state.healthScale) < 0.0001D) {
			return false;
		}
		double percent = previousMax <= 0.0D ? 1.0D : MathHelper.clamp(state.currentHealth / previousMax, 0.0D, 1.0D);
		state.healthScale = newScale;
		double actualMax = getMaxHealth(state, tier);
		state.currentHealth = Math.max(1.0D, Math.min(actualMax, percent * actualMax));
		return true;
	}

	public boolean applyDisturbance(State state, InfectionTier tier, long bonus) {
		if (bonus <= 0) {
			return false;
		}
		long duration = getTierDuration(state, tier);
		long previous = state.ticksInTier;
		state.ticksInTier = Math.min(duration, previous + bonus);
		return state.ticksInTier != previous;
	}

	public int getTierDuration(State state, InfectionTier tier) {
		return Math.max(1, MathHelper.ceil(tier.getDurationTicks() * state.difficulty.getDurationMultiplier()));
	}

	public long getTicksUntilFinalWave(State state) {
		if (state.apocalypseMode) {
			return 0L;
		}
		InfectionTier current = getCurrentTier(state);
		long remaining = Math.max(0L, getTierDuration(state, current) - state.ticksInTier);
		for (int tier = current.getIndex() + 1; tier <= InfectionTier.maxIndex(); tier++) {
			remaining += getTierDuration(state, InfectionTier.byIndex(tier));
		}
		return remaining;
	}

	public int claimSurfaceMutations(State state, ServerWorld world, InfectionTier tier, int requested, long totalTicks) {
		if (requested <= 0) {
			return 0;
		}
		if (state.surfaceMutationBudgetTick != totalTicks) {
			int base = MathHelper.clamp(world.getGameRules().getInt(TheVirusBlock.VIRUS_SURFACE_CORRUPT_ATTEMPTS), 0, 4096);
			int scaled = MathHelper.clamp(base * Math.max(1, tier.getLevel()), 0, 4096);
			if (state.apocalypseMode) {
				scaled = Math.min(4096, scaled + base / 2);
			}
			state.surfaceMutationBudget = scaled;
			state.surfaceMutationBudgetTick = totalTicks;
		}
		if (state.surfaceMutationBudget <= 0) {
			return 0;
		}
		int granted = Math.min(requested, state.surfaceMutationBudget);
		state.surfaceMutationBudget -= granted;
		return granted;
	}

	public VirusDifficulty getDifficulty(State state) {
		return state.difficulty;
	}

	public boolean setDifficulty(State state, VirusDifficulty difficulty) {
		if (state.difficulty == difficulty) {
			return false;
		}
		state.difficulty = difficulty;
		return true;
	}

	public void overwriteDifficulty(State state, VirusDifficulty difficulty) {
		state.difficulty = difficulty;
	}

	public boolean hasShownDifficultyPrompt(State state) {
		return state.difficultyPromptShown;
	}

	public boolean markDifficultyPromptShown(State state) {
		if (state.difficultyPromptShown) {
			return false;
		}
		state.difficultyPromptShown = true;
		return true;
	}

	public void setDifficultyPromptShown(State state, boolean shown) {
		state.difficultyPromptShown = shown;
	}

	public record TierAdvanceResult(boolean apocalypseActivated, InfectionTier tier) {
		public static TierAdvanceResult apocalypse() {
			return new TierAdvanceResult(true, InfectionTier.byIndex(InfectionTier.maxIndex()));
		}

		public static TierAdvanceResult tierAdvanced(InfectionTier tier) {
			return new TierAdvanceResult(false, tier);
		}

		public boolean advancedTier() {
			return !apocalypseActivated;
		}
	}

	public record HealthChange(boolean changed, boolean crossedHalfThreshold, boolean depleted) {
		public static final HealthChange NONE = new HealthChange(false, false, false);
	}
}

