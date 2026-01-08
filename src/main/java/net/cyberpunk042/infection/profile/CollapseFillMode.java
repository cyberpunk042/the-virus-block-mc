package net.cyberpunk042.infection.profile;

import org.jetbrains.annotations.Nullable;

/**
 * Replacement for the legacy {@code SingularityConfig.CollapseFillMode}. Defines
 * how erosion should remove blocks when falling back to manual fills.
 */
public enum CollapseFillMode {
	AIR("air"),
	DESTROY("destroy");

	private final String id;

	CollapseFillMode(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static CollapseFillMode defaultMode() {
		return AIR;
	}

	@Nullable
	public static CollapseFillMode fromId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		for (CollapseFillMode mode : values()) {
			if (mode.id.equalsIgnoreCase(id)) {
				return mode;
			}
		}
		return null;
	}
}

