package net.cyberpunk042.client.render;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.config.ColorConfig;
import net.cyberpunk042.config.ColorConfig.ColorSlot;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.network.SingularityBorderPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.border.WorldBorder;

public final class SingularityBorderClientState {
	private static boolean active;
	private static double centerX;
	private static double centerZ;
	private static double initialDiameter;
	private static double currentDiameter;
	private static double targetDiameter;
	private static SingularityState stage = SingularityState.DORMANT;

	private SingularityBorderClientState() {
	}

	public static void init() {
		ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayNetworkHandler handler, MinecraftClient client) ->
				client.execute(SingularityBorderClientState::reset));
	}

	public static void apply(SingularityBorderPayload payload) {
		active = payload.active();
		centerX = payload.centerX();
		centerZ = payload.centerZ();
		initialDiameter = payload.initialDiameter();
		currentDiameter = payload.currentDiameter();
		targetDiameter = payload.targetDiameter();
		stage = parseStage(payload.phase());
	}

	public static void captureBorder(WorldBorder border) {
		if (!active) {
			return;
		}
		currentDiameter = border.getSize();
		centerX = border.getCenterX();
		centerZ = border.getCenterZ();
	}

	public static int resolveForcefieldColor(int vanilla) {
		if (!active) {
			return vanilla;
		}
		float collapseProgress = getCollapseProgress();
		int color;
		if (stage == SingularityState.COLLAPSE) {
			color = lerpColor(collapseColor(), coreColor(), collapseProgress);
		} else if (stage == SingularityState.CORE || stage == SingularityState.RING) {
			color = lerpColor(coreColor(), flareColor(), Math.min(1.0F, collapseProgress + 0.35F));
		} else if (stage == SingularityState.DISSIPATION) {
			color = lerpColor(coreColor(), dissipateColor(), Math.min(1.0F, collapseProgress + 0.5F));
		} else {
			color = vanilla;
		}
		return applyPulse(color, collapseProgress);
	}

	public static void reset() {
		active = false;
		centerX = 0.0D;
		centerZ = 0.0D;
		initialDiameter = 0.0D;
		currentDiameter = 0.0D;
		targetDiameter = 0.0D;
		stage = SingularityState.DORMANT;
	}

	private static float getCollapseProgress() {
		if (!active || initialDiameter <= targetDiameter + 0.001D) {
			return 0.0F;
		}
		double clamped = MathHelper.clamp(currentDiameter, targetDiameter, initialDiameter);
		double span = initialDiameter - targetDiameter;
		double normalized = 1.0D - (clamped - targetDiameter) / span;
		return (float) MathHelper.clamp(normalized, 0.0D, 1.0D);
	}

	private static int applyPulse(int color, float progress) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return color;
		}
		float tickDelta = client.getRenderTickCounter().getTickProgress(false);
		double time = client.world.getTime() + tickDelta;
		float pulse = 0.03F + (float) (Math.sin(time * 0.09F + progress * MathHelper.TAU) * 0.03F);
		int a = (color >>> 24) & 0xFF;
		int r = (color >>> 16) & 0xFF;
		int g = (color >>> 8) & 0xFF;
		int b = color & 0xFF;
		int boosted = (int) MathHelper.clamp(r + pulse * 255.0F, 0.0F, 255.0F);
		int boostedG = (int) MathHelper.clamp(g + pulse * 128.0F, 0.0F, 255.0F);
		return (a << 24) | (boosted << 16) | (boostedG << 8) | b;
	}

	private static int lerpColor(int start, int end, float progress) {
		float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
		int a1 = (start >>> 24) & 0xFF;
		int r1 = (start >>> 16) & 0xFF;
		int g1 = (start >>> 8) & 0xFF;
		int b1 = start & 0xFF;
		int a2 = (end >>> 24) & 0xFF;
		int r2 = (end >>> 16) & 0xFF;
		int g2 = (end >>> 8) & 0xFF;
		int b2 = end & 0xFF;
		int a = MathHelper.floor(MathHelper.lerp(clamped, a1, a2));
		int r = MathHelper.floor(MathHelper.lerp(clamped, r1, r2));
		int g = MathHelper.floor(MathHelper.lerp(clamped, g1, g2));
		int b = MathHelper.floor(MathHelper.lerp(clamped, b1, b2));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static SingularityState parseStage(@Nullable String value) {
		if (value == null) {
			return SingularityState.DORMANT;
		}
		try {
			return SingularityState.valueOf(value);
		} catch (IllegalArgumentException ex) {
			return SingularityState.DORMANT;
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static double getCenterX() {
		return centerX;
	}

	public static double getCenterZ() {
		return centerZ;
	}

	private static int collapseColor() {
		return ColorConfig.argb(ColorSlot.SINGULARITY_BORDER_COLLAPSE);
	}

	private static int coreColor() {
		return ColorConfig.argb(ColorSlot.SINGULARITY_BORDER_CORE);
	}

	private static int dissipateColor() {
		return ColorConfig.argb(ColorSlot.SINGULARITY_BORDER_DISSIPATE);
	}

	private static int flareColor() {
		return ColorConfig.argb(ColorSlot.SINGULARITY_BORDER_FLARE);
	}
}

