package net.cyberpunk042.collision;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Central toggle + tuning knobs for growth-collision debugging.
 * Enabled by starting the JVM with -Dthevirusblock.growthCollisionDebug=true.
 */
public final class GrowthCollisionDebug {
	public static final String SYSTEM_PROPERTY = "thevirusblock.growthCollisionDebug";
	private static final String DISABLE_ANTI_CHEAT_PROPERTY = "thevirusblock.disableGrowthAntiCheatCollisions";
	private static final String SHAPE_MODE_PROPERTY = "thevirusblock.growthShapeMode";

	private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY, "false"));
	private static final boolean DISABLE_ANTI_CHEAT = Boolean.parseBoolean(
			System.getProperty(DISABLE_ANTI_CHEAT_PROPERTY, "false"));
	private static volatile ShapeMode shapeMode = parseShapeMode(
			System.getProperty(SHAPE_MODE_PROPERTY, ShapeMode.SOLID.name()));

	private GrowthCollisionDebug() {
	}

	public static boolean isEnabled() {
		return ENABLED;
	}

	public static boolean shouldLog(@Nullable Entity entity) {
		return ENABLED && entity instanceof PlayerEntity;
	}

	public static boolean disableAntiCheatCollisions() {
		return DISABLE_ANTI_CHEAT;
	}

	public static ShapeMode getShapeMode() {
		return shapeMode;
	}

	/**
	 * @return true if the mode changed.
	 */
	public static boolean setShapeMode(ShapeMode newMode) {
		if (newMode == null || newMode == shapeMode) {
			return false;
		}
		shapeMode = newMode;
		return true;
	}

	private static ShapeMode parseShapeMode(String raw) {
		if (raw == null) {
			return ShapeMode.SOLID;
		}
		try {
			return ShapeMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return ShapeMode.SOLID;
		}
	}

	public enum ShapeMode {
		SOLID,
		SHELL
	}
}

