package net.cyberpunk042.infection.scenario;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.InfectionScenario;
import net.cyberpunk042.infection.api.VirusWorldContext;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Shared scenario scaffolding that handles the common attach / detach lifecycle
 * (effect bus wiring, guardian beams, controller install). Concrete scenarios
 * only need to provide their controller and effect set palette.
 * <p>
 * This class now owns the dimension-specific infection tick logic, consolidating:
 * <ul>
 *   <li>Continuous mechanics (void tears, shields, combat exposure)</li>
 *   <li>Singularity event handling (delegated to SingularityController)</li>
 * </ul>
 */
abstract class AbstractDimensionInfectionScenario implements InfectionScenario {
	private ScenarioEffectSet effectSet;
	private GuardianBeamManager guardianBeams;

	protected abstract ScenarioEffectSet createEffectSet(VirusWorldContext context);

	@Override
	public void onAttach(VirusWorldContext context) {
		if (effectSet != null) {
			return; // Already attached
		}
		effectSet = createEffectSet(context);
		if (effectSet != null) {
			context.state().combat().effects().registerSet(effectSet, context.effectBus());
			trace(context, "effect-set install {}", effectSet.getClass().getSimpleName());
		}
		guardianBeams = new GuardianBeamManager();
		guardianBeams.install(context);
		trace(context, "guardian beams installed");
		context.state().applyDimensionProfile(context.singularity().profile());
		trace(context, "dimension profile applied");
	}

	@Override
	public void tick(VirusWorldContext context) {
		// NOTE: tickOrchestrator is called by DefaultWorldOrchestrator.tick()
		// Scenario only handles scenario-specific tasks here
		if (guardianBeams != null) {
			try {
				guardianBeams.tick();
			} catch (Exception e) {
				trace(context, "guardian beams tick failed: {}", e.getMessage());
			}
		}
	}

	/**
	 * Dimension-specific infection tick. Called by orchestrator.
	 * Handles continuous mechanics and singularity event.
	 */
	@Override
	public void tickInfection(VirusWorldContext context, boolean infected) {
		VirusWorldState state = context.state();
		ServerWorld world = context.world();

		// Continuous mechanics (always run regardless of infection state)
		tickContinuousMechanics(world, state);

		// Active infection frame (only when infected)
		// Note: Phase handling is done by PhaseManager, not scenario
		if (infected) {
			state.infection().runActiveFrame(world);
		} else {
			state.singularity().fusing().handleSingularityInactive();
		}
	}

	/**
	 * Continuous mechanics that run every tick regardless of infection state.
	 * Subclasses can override to customize per-dimension behavior.
	 */
	protected void tickContinuousMechanics(ServerWorld world, VirusWorldState state) {
		state.combat().voidTears().tick();
		state.shieldFieldService().tick();
		state.combat().exposure().tickContact();
		state.combat().exposure().tickInventory();
	}

	@Override
	public void onDetach(VirusWorldContext context) {
		if (effectSet != null) {
			trace(context, "effect-set close {}", effectSet.getClass().getSimpleName());
			context.state().combat().effects().unregisterSet(effectSet);
			effectSet = null;
		}
		if (guardianBeams != null) {
			trace(context, "guardian beams removed");
			guardianBeams.close();
			guardianBeams = null;
		}
	}

	protected ServiceConfig.Effects copyEffects(VirusWorldContext context) {
		InfectionServiceContainer services = context.singularity().services();
		ServiceConfig.Effects source = services != null ? services.settings().effects : new ServiceConfig.Effects();
		ServiceConfig.Effects copy = new ServiceConfig.Effects();
		copy.core = source.core;
		copy.ring = source.ring;
		copy.collapseVeil = source.collapseVeil;
		copy.dissipation = source.dissipation;
		return copy;
	}

	protected ServiceConfig.Audio copyAudio(VirusWorldContext context) {
		InfectionServiceContainer services = context.singularity().services();
		ServiceConfig.Audio source = services != null ? services.settings().audio : new ServiceConfig.Audio();
		ServiceConfig.Audio copy = new ServiceConfig.Audio();
		copy.core = source.core;
		copy.ring = source.ring;
		copy.collapseVeil = source.collapseVeil;
		copy.dissipation = source.dissipation;
		return copy;
	}

	private void trace(VirusWorldContext context, String message, Object... args) {
		Identifier scenario = id();
		Identifier worldId = context.world().getRegistryKey().getValue();
		Logging.EFFECTS.info("[scenario:{} world:{}] " + message,
				scenario,
				worldId,
				args);
	}
}

