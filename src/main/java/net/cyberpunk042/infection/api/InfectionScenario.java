package net.cyberpunk042.infection.api;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Strategy interface for per-dimension infection behavior. Only the method
 * surface is defined for now; implementations will arrive once the Overworld
 * logic is extracted out of {@code VirusWorldState}.
 */
public interface InfectionScenario {
	Identifier id();

	default void onAttach(VirusWorldContext context) {
	}

	void tick(VirusWorldContext context);

	/**
	 * Called by orchestrator to run dimension-specific infection mechanics.
	 * This includes continuous mechanics (void tears, shields, combat) and
	 * the active infection frame. Scenarios can override for dimension-specific behavior.
	 * <p>
	 * The default implementation provides standard infection behavior:
	 * <ul>
	 *   <li>Void tear ticking</li>
	 *   <li>Shield field ticking</li>
	 *   <li>Contact and inventory exposure</li>
	 *   <li>Active infection frame (when infected) or singularity inactive handling</li>
	 * </ul>
	 *
	 * @param context the world context
	 * @param infected whether the world is currently infected
	 */
	default void tickInfection(VirusWorldContext context, boolean infected) {
		VirusWorldState state = context.state();
		ServerWorld world = context.world();
		
		// Continuous mechanics (always run regardless of infection state)
		state.combat().voidTears().tick();
		state.shieldFieldService().tick();
		state.combat().exposure().tickContact();
		state.combat().exposure().tickInventory();
		
		// Active infection frame or inactive handling
		if (infected) {
			state.infection().runActiveFrame(world);
		} else {
			state.singularity().fusing().handleSingularityInactive();
		}
	}

	default void onDetach(VirusWorldContext context) {
	}
}

