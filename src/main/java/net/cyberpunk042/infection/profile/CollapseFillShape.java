package net.cyberpunk042.infection.profile;

import org.jetbrains.annotations.Nullable;

/**
 * Replacement for the legacy {@code SingularityConfig.FillShape}`.
 */
public enum CollapseFillShape {
	COLUMN("column"),
	ROW("row"),
	VECTOR("vector"),
	MATRIX("matrix"),
	OUTLINE("outline"),
	WALLS("walls");  // 4 separate wall commands - faster than outline

	private final String id;

	CollapseFillShape(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static CollapseFillShape defaultShape() {
		return WALLS;  // 4 wall commands - much faster than OUTLINE
	}

	@Nullable
	public static CollapseFillShape fromId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		for (CollapseFillShape shape : values()) {
			if (shape.id.equalsIgnoreCase(id)) {
				return shape;
			}
		}
		return null;
	}
}

