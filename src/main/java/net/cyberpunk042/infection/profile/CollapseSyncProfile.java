package net.cyberpunk042.infection.profile;

import org.jetbrains.annotations.Nullable;

/**
 * Sync profiles describe how aggressively a client should receive collapse
 * updates (bossbars, guardian beams, HUD data). These were formerly
 * hard-coded inside {@code SingularityConfig}.
 */
public enum CollapseSyncProfile {
	FULL("full"),
	CINEMATIC("cinematic"),
	MINIMAL("minimal");

	private final String id;

	CollapseSyncProfile(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static CollapseSyncProfile defaultProfile() {
		return FULL;
	}

	@Nullable
	public static CollapseSyncProfile fromId(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		for (CollapseSyncProfile profile : values()) {
			if (profile.id.equalsIgnoreCase(id)) {
				return profile;
			}
		}
		return null;
	}
}

