package net.cyberpunk042.infection.profile;

import net.cyberpunk042.infection.profile.DimensionProfile.Collapse.PreCollapseWaterDrainage.PreDrainMode;

/**
 * Predefined profiles for pre-collapse water drainage.
 * Fluids are much lighter to remove than blocks, so throughput is ~32x higher.
 * 
 * - mode: drainage pattern (direction-aware for outline/rows)
 * - thickness: face depth for outline/rows modes
 * - maxOperationsPerTick: fluid removal budget per tick
 */
public enum PreDrainProfile {
	// ─────────────────────────────────────────────────────────────────────────
	// Thin variants (thickness=1)
	// Params: mode, thickness, maxOperationsPerTick
	// ─────────────────────────────────────────────────────────────────────────
	
	/**
	 * Default - direction-aware outline drainage.
	 * Drains face closest to singularity.
	 */
	DEFAULT(PreDrainMode.OUTLINE, 1, 32),

	/**
	 * Row-by-row - horizontal rows on facing side.
	 */
	ROW_BY_ROW(PreDrainMode.ROWS, 1, 32),

	/**
	 * Full chunk - drains everything in chunk.
	 * Heaviest but thorough.
	 */
	FULL(PreDrainMode.FULL_PER_CHUNK, 1, 16),

	/**
	 * Facing center - explicit direction-aware face.
	 */
	FACING(PreDrainMode.FACING_CENTER, 1, 32),

	// ─────────────────────────────────────────────────────────────────────────
	// Thick variants (thickness=2)
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Thick outline - 2-block deep face drainage.
	 */
	OUTLINE_THICK(PreDrainMode.OUTLINE, 2, 64),

	/**
	 * Thick rows - 2-block deep row drainage.
	 */
	ROW_THICK(PreDrainMode.ROWS, 2, 64);

	private final PreDrainMode mode;
	private final int thickness;
	private final int maxOperationsPerTick;

	PreDrainProfile(PreDrainMode mode, int thickness, int maxOperationsPerTick) {
		this.mode = mode;
		this.thickness = thickness;
		this.maxOperationsPerTick = maxOperationsPerTick;
	}

	public PreDrainMode mode() {
		return mode;
	}

	public int thickness() {
		return thickness;
	}

	public int maxOperationsPerTick() {
		return maxOperationsPerTick;
	}

	/**
	 * Find profile by name (case-insensitive).
	 */
	public static PreDrainProfile fromName(String name) {
		if (name == null || name.isBlank()) {
			return DEFAULT;
		}
		try {
			return valueOf(name.toUpperCase().replace("-", "_"));
		} catch (IllegalArgumentException e) {
			return DEFAULT;
		}
	}
}

