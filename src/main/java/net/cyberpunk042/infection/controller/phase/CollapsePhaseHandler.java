package net.cyberpunk042.infection.controller.phase;


import net.cyberpunk042.log.Logging;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SingularityContext;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.minecraft.server.world.ServerWorld;

/**
 * Encapsulates the collapse state machine using the simplified CollapseProcessor.
 * Overworld and future scenarios can delegate to this handler until bespoke controllers exist.
 */
public final class CollapsePhaseHandler {
	public void tick(SingularityContext ctx) {
		ServerWorld world = ctx.world();
		VirusWorldState state = ctx.state();
		CollapseBroadcastManager broadcast = ctx.broadcastManager();

		if (!state.collapseConfig().configuredCollapseEnabled(world)) {
			state.singularity().phase().skipSingularityCollapse("disabled mid-collapse", broadcast);
			return;
		}

		state.singularity().lifecycle().tickCollapseBarDelay();
		state.singularity().chunkPreparation().tickChunkPreload();
		broadcast.flush(world, false);

		int barDelay = state.singularityState().singularityCollapseBarDelay;
		boolean pendingDeploy = state.singularity().borderState().pendingDeployment;
		
		// Log border deployment status once when delay expires
		if (barDelay == 0 && pendingDeploy) {
			Logging.PHASE.info("[CollapsePhase] Border deployment triggered (delay expired)");
			broadcast.deployBorder(world);
		} else if (barDelay <= 0 && pendingDeploy) {
			// Already past delay but still pending - deploy now
			broadcast.deployBorder(world);
		}

		state.singularity().ensureCenter(world);
		if (state.singularityState().center == null) {
			return;
		}

		// CollapseProcessor (radius-based fill)
		if (state.singularity().collapseProcessor().isActive()) {
			state.singularity().collapseProcessor().tick();
			state.markDirty();
			
			// Check completion
			if (!state.singularity().collapseProcessor().isActive()) {
				state.collapse().tryCompleteCollapse(world, broadcast);
			}
			return;
		}

		// If processor is not active and no work remains, try to complete
		if (!state.collapse().hasCollapseWorkRemaining()) {
			state.collapse().tryCompleteCollapse(world, broadcast);
		}

		broadcast.flush(world, false);
		state.markDirty();
	}
}
