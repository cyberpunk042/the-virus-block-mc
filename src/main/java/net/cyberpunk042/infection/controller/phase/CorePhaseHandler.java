package net.cyberpunk042.infection.controller.phase;

import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.events.CoreChargeTickEvent;
import net.cyberpunk042.infection.events.CoreDetonationEvent;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World.ExplosionSourceType;

/**
 * Handles the CORE phase of the singularity lifecycle.
 * During this phase, the singularity center charges and then detonates.
 */
public final class CorePhaseHandler {

	public void tick(SingularityContext ctx) {
		VirusWorldState state = ctx.state();
		ServerWorld world = ctx.world();
		EffectBus bus = ctx.effectBus();
		DimensionProfile profile = ctx.profile();

		BlockPos center = state.singularityState().center;
		if (center == null) {
			return;
		}

		// Charging countdown
		if (state.singularityState().singularityPhaseDelay > 0) {
			state.singularityState().singularityPhaseDelay--;
			bus.post(new CoreChargeTickEvent(world, center, state.singularityState().singularityPhaseDelay));
			state.markDirty();
			return;
		}

		// Detonation
		int pushRadius = profile.physics().pushRadius();
		state.presentationCoord().pushPlayersFromBlock(center, pushRadius, 2.0D, false);
		world.createExplosion(null,
				null,
				null,
				center.getX() + 0.5D,
				center.getY() + 0.5D,
				center.getZ() + 0.5D,
				10.0F,
				false,
				ExplosionSourceType.TNT);
		bus.post(new CoreDetonationEvent(world, center));

		// Transition to RING phase
		state.collapse().transitionSingularityState(SingularityState.RING, "core detonation");
		state.singularityState().singularityPhaseDelay = profile.collapse().tickInterval();
		state.singularityState().singularityRingTicks = profile.collapse().tickInterval() * profile.collapse().columnsPerTick();
		state.singularity().phase().syncSingularityBorder();
		state.markDirty();
	}
}

