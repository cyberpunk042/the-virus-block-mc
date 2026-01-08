package net.cyberpunk042.infection.profile;

import org.jetbrains.annotations.Nullable;

/**
 * Broadcast behaviour for collapse notifications. Mirrors the legacy
 * {@code SingularityConfig} enum but lives alongside the new profile data so
 * per-dimension JSON knobs can reference it directly.
 */
public enum CollapseBroadcastMode {
	IMMEDIATE("immediate"),
	DELAYED("delayed"),
	SUMMARY("summary");

	private final String id;

	CollapseBroadcastMode(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static CollapseBroadcastMode defaultMode() {
		return IMMEDIATE;
	}

	@Nullable
	public static CollapseBroadcastMode fromId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		for (CollapseBroadcastMode mode : values()) {
			if (mode.id.equalsIgnoreCase(id)) {
				return mode;
			}
		}
		return null;
	}
}

