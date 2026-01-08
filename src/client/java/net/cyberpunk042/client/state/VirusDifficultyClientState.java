package net.cyberpunk042.client.state;

import net.cyberpunk042.infection.VirusDifficulty;

public final class VirusDifficultyClientState {
	private static volatile VirusDifficulty difficulty = VirusDifficulty.HARD;

	private VirusDifficultyClientState() {
	}

	public static void set(VirusDifficulty newDifficulty) {
		difficulty = newDifficulty;
	}

	public static VirusDifficulty get() {
		return difficulty;
	}
}

