package net.cyberpunk042.client.render;

import net.minecraft.client.MinecraftClient;

public final class VirusSkyClientState {
	private static volatile boolean skyCorrupted;
	private static volatile boolean fluidsCorrupted;

	private VirusSkyClientState() {
	}

	public static void setState(boolean sky, boolean fluids) {
		boolean skyChanged = skyCorrupted != sky;
		boolean fluidsChanged = fluidsCorrupted != fluids;
		if (!skyChanged && !fluidsChanged) {
			return;
		}
		skyCorrupted = sky;
		fluidsCorrupted = fluids;
		if (fluidsChanged) {
			CorruptedFireTextures.setCorrupted(fluids);
		} else {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null && client.worldRenderer != null) {
				client.execute(client.worldRenderer::reload);
			}
		}
	}

	public static void reset() {
		setState(false, false);
	}

	public static boolean isSkyCorrupted() {
		return skyCorrupted;
	}

	public static boolean areFluidsCorrupted() {
		return fluidsCorrupted;
	}
}


