package net.cyberpunk042.client.render;

import net.minecraft.util.math.MathHelper;

public final class VirusHorizonClientState {
	private static volatile boolean enabled;
	private static volatile float intensity;
	private static volatile float red;
	private static volatile float green;
	private static volatile float blue;

	private VirusHorizonClientState() {
	}

	public static void apply(boolean active, float value, int argb) {
		enabled = active;
		intensity = MathHelper.clamp(value, 0.0F, 1.0F);
		red = ((argb >> 16) & 255) / 255.0F;
		green = ((argb >> 8) & 255) / 255.0F;
		blue = (argb & 255) / 255.0F;
	}

	public static boolean isActive() {
		return enabled && intensity > 0.001F;
	}

	public static float intensity() {
		return intensity;
	}

	public static float red() {
		return red;
	}

	public static float green() {
		return green;
	}

	public static float blue() {
		return blue;
	}

	public static void reset() {
		apply(false, 0.0F, 0xFF000000);
	}
}

