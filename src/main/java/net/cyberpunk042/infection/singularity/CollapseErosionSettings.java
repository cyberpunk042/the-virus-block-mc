package net.cyberpunk042.infection.singularity;

import net.cyberpunk042.infection.profile.CollapseFillMode;
import net.cyberpunk042.infection.profile.CollapseFillShape;
import net.cyberpunk042.infection.profile.WaterDrainMode;

/**
 * Immutable snapshot of erosion/fill behaviour for the active collapse profile.
 */
public record CollapseErosionSettings(
		WaterDrainMode waterDrainMode,
		int waterDrainOffset,
		WaterDrainDeferredSettings waterDrainDeferred,
		boolean collapseParticles,
		CollapseFillMode fillMode,
		CollapseFillShape fillShape,
		int outlineThickness,
		boolean useNativeFill,
		boolean respectProtectedBlocks,
		int maxOperationsPerTick) {
	
	/**
	 * Default max operations per tick if not specified.
	 * Keep low to avoid lag spikes - each op is a block change.
	 */
	public static final int DEFAULT_MAX_OPERATIONS_PER_TICK = 1;

	public record WaterDrainDeferredSettings(boolean enabled, int initialDelayTicks, int columnsPerTick) {
		public static WaterDrainDeferredSettings disabled() {
			return new WaterDrainDeferredSettings(false, 0, 1);
		}
	}
}

