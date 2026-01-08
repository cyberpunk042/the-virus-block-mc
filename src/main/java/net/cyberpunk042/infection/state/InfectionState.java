package net.cyberpunk042.infection.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.cyberpunk042.infection.VirusEventType;

/**
 * Aggregates fields related to the infection progression so {@link net.cyberpunk042.infection.VirusWorldState}
 * doesn't have to expose raw maps and counters.
 */
public final class InfectionState {

	private boolean infected;
	private boolean dormant;
	private long totalTicks;
	private boolean terrainCorrupted;
	private boolean cleansingActive;
	private boolean boobytrapDefaultsCaptured;
	private boolean defaultBoobytrapsEnabled = true;
	private int defaultWormSpawnChance = -1;
	private int defaultWormTrapSpawnChance = -1;
	private long lastMatrixCubeTick;
	private final Map<VirusEventType, Long> eventHistory = new EnumMap<>(VirusEventType.class);
	private final Object2IntMap<UUID> infectiousInventoryTicks = new Object2IntOpenHashMap<>();
	private final Object2LongMap<UUID> infectiousInventoryWarnCooldowns = new Object2LongOpenHashMap<>();
	private final Object2IntMap<UUID> infectiousContactTicks = new Object2IntOpenHashMap<>();
	private final Object2IntMap<UUID> helmetPingTimers = new Object2IntOpenHashMap<>();
	private final Object2DoubleMap<UUID> heavyPantsVoidWear = new Object2DoubleOpenHashMap<>();
	
	/**
	 * Tracks how many times the virus has been hurt by each damage type.
	 * Keys are damage category strings (e.g., "BED", "TNT", "MELEE:minecraft:diamond_pickaxe").
	 * Values are hit counts (0-4+). Each hit increases resistance by 25%.
	 */
	private final Object2IntMap<String> damageAdaptation = new Object2IntOpenHashMap<>();

	public boolean infected() {
		return infected;
	}

	public void setInfected(boolean infected) {
		this.infected = infected;
	}

	public boolean dormant() {
		return dormant;
	}

	public void setDormant(boolean dormant) {
		this.dormant = dormant;
	}

	public long totalTicks() {
		return totalTicks;
	}

	public void incrementTicks() {
		totalTicks++;
	}

	public void resetTicks() {
		totalTicks = 0L;
	}

	public void setTotalTicks(long totalTicks) {
		this.totalTicks = Math.max(0L, totalTicks);
	}

	public Map<VirusEventType, Long> eventHistory() {
		return eventHistory;
	}

	public Object2IntMap<UUID> infectiousInventoryTicks() {
		return infectiousInventoryTicks;
	}

	public Object2LongMap<UUID> infectiousInventoryWarnCooldowns() {
		return infectiousInventoryWarnCooldowns;
	}

	public Object2IntMap<UUID> infectiousContactTicks() {
		return infectiousContactTicks;
	}

	public Object2IntMap<UUID> helmetPingTimers() {
		return helmetPingTimers;
	}

	public Object2DoubleMap<UUID> heavyPantsVoidWear() {
		return heavyPantsVoidWear;
	}

	public boolean terrainCorrupted() {
		return terrainCorrupted;
	}

	public void setTerrainCorrupted(boolean terrainCorrupted) {
		this.terrainCorrupted = terrainCorrupted;
	}

	public boolean cleansingActive() {
		return cleansingActive;
	}

	public void setCleansingActive(boolean cleansingActive) {
		this.cleansingActive = cleansingActive;
	}

	public boolean boobytrapDefaultsCaptured() {
		return boobytrapDefaultsCaptured;
	}

	public void setBoobytrapDefaultsCaptured(boolean captured) {
		this.boobytrapDefaultsCaptured = captured;
	}

	public boolean defaultBoobytrapsEnabled() {
		return defaultBoobytrapsEnabled;
	}

	public void setDefaultBoobytrapsEnabled(boolean enabled) {
		this.defaultBoobytrapsEnabled = enabled;
	}

	public int defaultWormSpawnChance() {
		return defaultWormSpawnChance;
	}

	public void setDefaultWormSpawnChance(int chance) {
		this.defaultWormSpawnChance = chance;
	}

	public int defaultWormTrapSpawnChance() {
		return defaultWormTrapSpawnChance;
	}

	public void setDefaultWormTrapSpawnChance(int chance) {
		this.defaultWormTrapSpawnChance = chance;
	}

	public long lastMatrixCubeTick() {
		return lastMatrixCubeTick;
	}

	public void setLastMatrixCubeTick(long tick) {
		this.lastMatrixCubeTick = tick;
	}

	// ========== Damage Adaptation ("Viral Evolution") ==========

	/**
	 * Returns the raw adaptation map for persistence.
	 */
	public Object2IntMap<String> damageAdaptation() {
		return damageAdaptation;
	}

	/**
	 * Records a successful damage hit against the virus.
	 * Each hit of the same type increases resistance by 25%.
	 * 
	 * @param damageKey The damage category key (e.g., "BED", "MELEE:minecraft:diamond_sword")
	 * @return The new exposure count (1-4+)
	 */
	public int recordDamageExposure(String damageKey) {
		if (damageKey == null || damageKey.isEmpty()) {
			return 0;
		}
		int current = damageAdaptation.getOrDefault(damageKey, 0);
		int newCount = Math.min(current + 1, 4); // Cap at 4 (100% resistance)
		damageAdaptation.put(damageKey, newCount);
		return newCount;
	}

	/**
	 * Gets the resistance multiplier for a damage type.
	 * 
	 * @param damageKey The damage category key
	 * @return Resistance from 0.0 (no resistance) to 1.0 (immune)
	 */
	public float getDamageResistance(String damageKey) {
		if (damageKey == null || damageKey.isEmpty()) {
			return 0.0F;
		}
		int exposure = damageAdaptation.getOrDefault(damageKey, 0);
		// 0 hits = 0%, 1 hit = 25%, 2 = 50%, 3 = 75%, 4+ = 100%
		return Math.min(1.0F, exposure * 0.25F);
	}

	/**
	 * Gets the current exposure count for a damage type.
	 */
	public int getExposureCount(String damageKey) {
		return damageAdaptation.getOrDefault(damageKey, 0);
	}

	/**
	 * Resets all damage adaptations (e.g., on infection reset).
	 */
	public void resetDamageAdaptation() {
		damageAdaptation.clear();
	}
}

