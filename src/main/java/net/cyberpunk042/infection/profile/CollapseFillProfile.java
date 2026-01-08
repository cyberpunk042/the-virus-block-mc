package net.cyberpunk042.infection.profile;

/**
 * Predefined fill profiles that define batching/chunking strategies for collapse processing.
 * Each profile bundles shape, thickness, and throughput settings.
 * 
 * - thickness: how wide each batch/slice is
 * - columnsPerTick: radius increments per tick  
 * - maxOperationsPerTick: block change budget per tick
 */
public enum CollapseFillProfile {
	// ─────────────────────────────────────────────────────────────────────────
	// Thin variants (thickness=1)
	// Params: shape, thickness, columnsPerTick, maxOperationsPerTick
	// ─────────────────────────────────────────────────────────────────────────
	
	/**
	 * Default outline - baseline profile.
	 * Shell processing, moderate block count.
	 */
	DEFAULT(CollapseFillShape.OUTLINE, 1, 1, 1),

	/**
	 * Column-by-column - lightest, vertical line.
	 */
	COLUMN_BY_COLUMN(CollapseFillShape.COLUMN, 1, 1, 4),

	/**
	 * Row-by-row - light, horizontal line.
	 */
	ROW_BY_ROW(CollapseFillShape.ROW, 1, 1, 4),

	/**
	 * Vector - light, along dominant axis.
	 */
	VECTOR(CollapseFillShape.VECTOR, 1, 1, 4),

	/**
	 * Full matrix - heaviest, all blocks.
	 */
	FULL_MATRIX(CollapseFillShape.MATRIX, 1, 1, 1),

	// ─────────────────────────────────────────────────────────────────────────
	// Thick variants (thickness=2)
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Thick outline - 2-block shell.
	 */
	OUTLINE_THICK(CollapseFillShape.OUTLINE, 2, 1, 2),

	/**
	 * Thick column - 2x2 column.
	 */
	COLUMN_THICK(CollapseFillShape.COLUMN, 2, 1, 8),

	/**
	 * Thick row - 2x2 row.
	 */
	ROW_THICK(CollapseFillShape.ROW, 2, 1, 8);

	private final CollapseFillShape shape;
	private final int thickness;
	private final int columnsPerTick;
	private final int maxOperationsPerTick;

	CollapseFillProfile(CollapseFillShape shape, int thickness, int columnsPerTick, int maxOperationsPerTick) {
		this.shape = shape;
		this.thickness = thickness;
		this.columnsPerTick = columnsPerTick;
		this.maxOperationsPerTick = maxOperationsPerTick;
	}

	public CollapseFillShape shape() {
		return shape;
	}

	public int thickness() {
		return thickness;
	}

	public int columnsPerTick() {
		return columnsPerTick;
	}

	public int maxOperationsPerTick() {
		return maxOperationsPerTick;
	}

	/**
	 * Find profile by name (case-insensitive).
	 */
	public static CollapseFillProfile fromName(String name) {
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

