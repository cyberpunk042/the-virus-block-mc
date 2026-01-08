package net.cyberpunk042.infection.profile;

/**
 * Directional water draining strategy used by the singularity erosion phase.
 */
public enum WaterDrainMode {
	OFF,
	AHEAD,
	BEHIND,
	BOTH;

	public boolean drainsAhead() {
		return this == AHEAD || this == BOTH;
	}

	public boolean drainsBehind() {
		return this == BEHIND || this == BOTH;
	}
}

