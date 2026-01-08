package net.cyberpunk042.infection.state;

import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.TierCookbook;
import net.cyberpunk042.infection.TierFeature;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.service.InfectionTierService;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

/**
 * Facade around {@link InfectionTierService} that centralizes access to tier/difficulty
 * bookkeeping so callers do not need to juggle the raw service + state pair.
 */
public final class TierModule {

	// Constants for tier five barrier and final vulnerability effects
	public static final int TIER_FIVE_BARRIER_RADIUS = 6;
	public static final int TIER_FIVE_BARRIER_INTERVAL = 15;
	public static final int FINAL_VULNERABILITY_BLAST_RADIUS = 3;
	public static final double FINAL_VULNERABILITY_BLAST_SCALE = 0.6D;

	@Nullable
	private VirusWorldState host;
	private final InfectionTierService service = new InfectionTierService();
	private final InfectionTierService.State state = new InfectionTierService.State();

	public void setHost(VirusWorldState host) {
		this.host = host;
	}

	public InfectionTier currentTier() {
		return service.getCurrentTier(state);
	}

	public InfectionTierService.State snapshotState() {
		return state;
	}

	public int index() {
		return service.getTierIndex(state);
	}

	public void setIndex(int index) {
		service.setTierIndex(state, index);
	}

	public long ticksInTier() {
		return service.getTicksInTier(state);
	}

	public void setTicksInTier(long ticks) {
		service.setTicksInTier(state, ticks);
	}

	public void incrementTierTick() {
		service.incrementTierTick(state);
	}

	public int containmentLevel() {
		return service.getContainmentLevel(state);
	}

	public void setContainmentLevel(int level) {
		service.setContainmentLevel(state, level);
	}

	public boolean degradeContainment() {
		return service.degradeContainment(state);
	}

	public InfectionTierService.TierAdvanceResult advanceTier() {
		return service.advanceTier(state);
	}

	public InfectionTierService.HealthChange applyHealthDamage(double amount) {
		return service.applyHealthDamage(state, currentTier(), amount);
	}

	public InfectionTierService.HealthChange applyHealthDamage(InfectionTier tier, double amount) {
		return service.applyHealthDamage(state, tier, amount);
	}

	public boolean reduceMaxHealth(InfectionTier tier, double factor) {
		return service.reduceMaxHealth(state, tier, factor);
	}

	public boolean applyContainmentCharge(int amount) {
		return service.applyContainmentCharge(state, amount);
	}

	public boolean applyDisturbance(InfectionTier tier, long bonus) {
		return service.applyDisturbance(state, tier, bonus);
	}

	public int claimSurfaceMutations(ServerWorld world, InfectionTier tier, int requested, long totalTicks) {
		return service.claimSurfaceMutations(state, world, tier, requested, totalTicks);
	}

	public long ticksUntilFinalWave() {
		return service.getTicksUntilFinalWave(state);
	}

	public boolean ensureHealthInitialized(InfectionTier tier) {
		return service.ensureHealthInitialized(state, tier);
	}

	public void resetForInfectionStart() {
		service.resetForInfectionStart(state);
	}

	public void resetHealthForTier(InfectionTier tier) {
		service.resetHealthForTier(state, tier);
	}

	public void clearForEnd() {
		service.clearForEnd(state);
	}

	public void setApocalypseMode(boolean apocalypse) {
		service.setApocalypseMode(state, apocalypse);
	}

	public boolean isApocalypseMode() {
		return service.isApocalypseMode(state);
	}

	public InfectionTierService.State rawState() {
		return state;
	}

	public VirusDifficulty difficulty() {
		return service.getDifficulty(state);
	}

	public boolean setDifficulty(VirusDifficulty difficulty) {
		return service.setDifficulty(state, difficulty);
	}

	public void overwriteDifficulty(VirusDifficulty difficulty) {
		service.overwriteDifficulty(state, difficulty);
	}

	public boolean hasShownDifficultyPrompt() {
		return service.hasShownDifficultyPrompt(state);
	}

	public boolean markDifficultyPromptShown() {
		return service.markDifficultyPromptShown(state);
	}

	public void setDifficultyPromptShown(boolean shown) {
		service.setDifficultyPromptShown(state, shown);
	}

	public double maxHealth(InfectionTier tier) {
		return service.getMaxHealth(state, tier);
	}

	public double currentHealth() {
		return service.getCurrentHealth(state);
	}

	public void setCurrentHealth(double health) {
		service.setCurrentHealth(state, health);
	}

	public double healthScale() {
		return service.getHealthScale(state);
	}

	public void setHealthScale(double scale) {
		service.setHealthScale(state, scale);
	}

	/**
	 * Increases the health scale by 1.0 (for each additional virus block).
	 * This effectively doubles, triples, etc. the max health.
	 * Current health is scaled proportionally to maintain the same health percentage.
	 */
	public void increaseHealthScaleForAdditionalSource() {
		double currentScale = healthScale();
		double newScale = currentScale + 1.0D;
		InfectionTier tier = currentTier();
		double previousMax = maxHealth(tier);
		double healthPercent = previousMax > 0.0D ? currentHealth() / previousMax : 1.0D;
		
		setHealthScale(newScale);
		
		// Scale current health to maintain the same percentage
		double newMax = maxHealth(tier);
		setCurrentHealth(healthPercent * newMax);
	}

	public int duration(InfectionTier tier) {
		return service.getTierDuration(state, tier);
	}

	public double getHealthPercent() {
		double max = maxHealth(currentTier());
		if (max <= 0.0D) {
			return 0.0D;
		}
		return net.minecraft.util.math.MathHelper.clamp(currentHealth() / max, 0.0D, 1.0D);
	}

	public float getBoobytrapIntensity() {
		if (isApocalypseMode()) {
			return 1.6F;
		}
		return currentTier().getBoobytrapMultiplier();
	}

	public boolean areLiquidsCorrupted(ServerWorld world) {
		if (host == null) {
			return false;
		}
		return host.infectionState().infected() && TierCookbook.isEnabled(world, currentTier(), isApocalypseMode(), TierFeature.LIQUID_CORRUPTION);
	}
}

