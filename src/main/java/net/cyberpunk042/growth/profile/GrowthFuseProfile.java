package net.cyberpunk042.growth.profile;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Visual/timing preset for fuse phases triggered by progressive growth blocks.
 */
public record GrowthFuseProfile(
		Identifier id,
		Trigger trigger,
		double autoProgress,
		boolean requiresItem,
		boolean consumeItem,
		List<Identifier> allowedItems,
		int explosionDelayTicks,
		int shellCollapseTicks,
		int pulseIntervalTicks,
		double collapseTargetScale,
		String primaryColorHex,
		String secondaryColorHex,
		Identifier particleId,
		Identifier soundId) {

	public GrowthFuseProfile {
		allowedItems = List.copyOf(allowedItems);
	}

	public static GrowthFuseProfile defaults() {
		return new GrowthFuseProfile(
				Identifier.of("the-virus-block", "default_fuse"),
				Trigger.AUTO,
				1.0D,
				false,
				false,
				List.of(),
				400,
				20,
				8,
				-1.0D,  // Sentinel: use definition's minScale
				"#FF6A00FF",
				"#FF101010",
				Identifier.of("minecraft", "small_flame"),
				Identifier.of("minecraft", "block.respawn_anchor.charge"));
	}

	public int sanitizedExplosionDelay() {
		return Math.max(20, explosionDelayTicks);
	}

	public int sanitizedShellCollapse() {
		return Math.max(1, shellCollapseTicks);
	}

	public int sanitizedPulseInterval() {
		return Math.max(1, pulseIntervalTicks);
	}

	public double clampedAutoProgress() {
		return MathHelper.clamp(autoProgress, 0.0D, 1.0D);
	}

	public double collapseTargetScaleOrDefault(GrowthBlockDefinition definition) {
		// Negative or NaN means "use definition's minScale"
		if (collapseTargetScale <= 0.0D || Double.isNaN(collapseTargetScale)) {
			return definition.minScale();
		}
		double min = definition.minScale();
		double max = Math.max(min + 1.0E-4D, definition.maxScale());
		return MathHelper.clamp(collapseTargetScale, min, max);
	}

	public List<Identifier> allowedItems() {
		return Collections.unmodifiableList(allowedItems);
	}

	public enum Trigger {
		AUTO,
		ITEM_USE,
		RIGHT_CLICK,
		ATTACK;

		public static Trigger fromString(String raw) {
			if (raw == null) {
				return AUTO;
			}
			return switch (raw.toLowerCase(Locale.ROOT)) {
				case "item_use", "flint_and_steel", "use_item" -> ITEM_USE;
				case "attack", "hit" -> ATTACK;
				case "right_click", "interact" -> RIGHT_CLICK;
				default -> AUTO;
			};
		}
	}
}