package net.cyberpunk042.infection;

import java.util.Optional;

import net.cyberpunk042.infection.service.AmbientPressureService;
import net.cyberpunk042.infection.service.CollapseSnapshotService;
import net.cyberpunk042.infection.service.CollapseSnapshotService.SingularitySnapshot;
import net.cyberpunk042.infection.service.InfectionLifecycleService;
import net.cyberpunk042.infection.service.InfectionTierService;
import net.cyberpunk042.infection.service.VoidTearService;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.infection.service.VirusSourceService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class InfectionOperations {
	private final VirusWorldState state;
	private final InfectionLifecycleService infectionLifecycleService;
	private final AmbientPressureService ambientPressureService;
	private final CollapseSnapshotService snapshotService;
	private final VoidTearService voidTearService;

	InfectionOperations(VirusWorldState state,
			InfectionLifecycleService infectionLifecycleService,
			AmbientPressureService ambientPressureService,
			CollapseSnapshotService snapshotService,
			VoidTearService voidTearService) {
		this.state = state;
		this.infectionLifecycleService = infectionLifecycleService;
		this.ambientPressureService = ambientPressureService;
		this.snapshotService = snapshotService;
		this.voidTearService = voidTearService;
	}

	public void ensureDebugInfection(ServerWorld world, BlockPos center) {
		infectionLifecycleService.ensureDebugInfection(center);
	}

	public void applyDifficultyRules(ServerWorld world, InfectionTier tier) {
		ambientPressureService.applyDifficultyRules(tier);
	}

	public void broadcast(ServerWorld world, Text text) {
		world.getPlayers(ServerPlayerEntity::isAlive).forEach(player -> player.sendMessage(text, false));
	}

	public Optional<SingularitySnapshot> getSingularitySnapshot() {
		return snapshotService.buildSingularitySnapshot();
	}

	public void applySingularitySnapshot(SingularitySnapshot snapshot) {
		snapshotService.applySingularitySnapshot(snapshot);
	}

	public boolean spawnVoidTearForCommand(ServerWorld world, BlockPos center) {
		return voidTearService.spawnViaCommand(center);
	}

	public BlockPos representativePos(ServerWorld world, Random random, VirusSourceService.State sourceState) {
		return state.sources().representativePos(random, sourceState);
	}

	public boolean applyHealthDamage(ServerWorld world, double amount) {
		return applyHealthDamage(world, amount, null);
	}

	/**
	 * Applies damage to the infection health pool, with viral adaptation.
	 * 
	 * <p>The virus develops resistance to damage types it has experienced:
	 * <ul>
	 *   <li>1st hit: 25% resistance</li>
	 *   <li>2nd hit: 50% resistance</li>
	 *   <li>3rd hit: 75% resistance</li>
	 *   <li>4th+ hit: 100% resistance (immune)</li>
	 * </ul>
	 * 
	 * @param world The server world
	 * @param amount The base damage amount
	 * @param damageKey The damage category key from {@link VirusDamageClassifier}, or null for no adaptation
	 * @return true if damage was applied
	 */
	public boolean applyHealthDamage(ServerWorld world, double amount, String damageKey) {
		if (!state.infectionState().infected() || amount <= 0.0D) {
			return false;
		}

		// Apply viral adaptation resistance
		double finalAmount = amount;
		float resistance = 0.0F;
		if (damageKey != null && !damageKey.isEmpty()) {
			resistance = state.infectionState().getDamageResistance(damageKey);
			finalAmount = amount * (1.0 - resistance);
			System.out.println("[Adaptation DEBUG] damageKey=" + damageKey + " resistance=" + resistance + " amount=" + amount + " finalAmount=" + finalAmount);
		} else {
			System.out.println("[Adaptation DEBUG] No damageKey provided, applying full damage: " + amount);
		}

		// Check if immune
		if (finalAmount <= 0.0D) {
			// Virus is immune - notify nearby players
			notifyImmunity(world, damageKey);
			return false;
		}

		InfectionTierService.HealthChange change = state.tiers().applyHealthDamage(state.tiers().currentTier(), finalAmount);
		if (!change.changed()) {
			return false;
		}

		// Record this damage exposure (develops resistance for next time)
		int newExposure = 0;
		if (damageKey != null && !damageKey.isEmpty()) {
			newExposure = state.infectionState().recordDamageExposure(damageKey);
			System.out.println("[Adaptation DEBUG] Recorded exposure for " + damageKey + " -> newExposure=" + newExposure);
			notifyAdaptation(world, damageKey, newExposure, resistance);
		}

		if (change.crossedHalfThreshold() && !state.shell().isCollapsed()) {
			state.shell().collapse(world, state.getVirusSources());
		}

		state.markDirty();
		state.presentationCoord().updateBossBars();
		if (change.depleted()) {
			state.sourceControl().forceContainmentReset();
		}
		return true;
	}

	/**
	 * Notifies nearby players that the virus has adapted to a damage type.
	 */
	private void notifyAdaptation(ServerWorld world, String damageKey, int exposure, float previousResistance) {
		if (exposure <= 0 || exposure > 4) {
			return; // No notification for first hit or after max
		}
		
		String displayName = VirusDamageClassifier.getDisplayName(damageKey);
		if (displayName == null) {
			displayName = "Unknown";
		}
		int resistPercent = exposure * 25;
		
		// Use literal text to avoid any serialization issues
		Text message = Text.literal("The virus adapts! " + displayName + " now deals " + resistPercent + "% less damage.")
				.formatted(net.minecraft.util.Formatting.DARK_PURPLE);
		
		// Broadcast to all players in this world
		for (ServerPlayerEntity player : world.getPlayers()) {
			player.sendMessage(message, true); // Action bar message
		}
	}

	/**
	 * Notifies nearby players that the virus is immune to a damage type.
	 */
	private void notifyImmunity(ServerWorld world, String damageKey) {
		String displayName = VirusDamageClassifier.getDisplayName(damageKey);
		if (displayName == null) {
			displayName = "Unknown";
		}
		// Use literal text to avoid any serialization issues
		Text message = Text.literal("The virus is immune to " + displayName + "!")
				.formatted(net.minecraft.util.Formatting.DARK_RED);
		
		for (ServerPlayerEntity player : world.getPlayers()) {
			player.sendMessage(message, true);
		}
	}

	public int claimSurfaceMutations(ServerWorld world, InfectionTier tier, int requested) {
		if (!state.infectionState().infected() || requested <= 0) {
			return 0;
		}
		return state.tiers().claimSurfaceMutations(world, tier, requested, state.infectionState().totalTicks());
	}

	/**
	 * Runs a single frame of active infection processing.
	 * Called each tick when infection is active.
	 * <p>
	 * NOTE: Consider adding try-catch around subsections for defensive error isolation.
	 * Currently, a failure in any step will abort the entire frame.
	 */
	public void runActiveFrame(ServerWorld world) {
		if (!state.infectionState().infected()) {
			return;
		}
		state.infectionState().incrementTicks();
		state.tiers().incrementTierTick();

		state.sources().removeMissingSources(state.sourceState());

		InfectionTier tier = state.tiers().currentTier();
		if (state.tiers().ensureHealthInitialized(tier)) {
			state.markDirty();
		}
		int tierDuration = state.tiers().duration(tier);
		boolean apocalypse = state.tiers().isApocalypseMode();

		if (!apocalypse && tierDuration > 0 && state.tiers().ticksInTier() >= tierDuration 
				&& state.singularityState().singularityState == SingularityState.DORMANT) {
			state.tierProgression().advanceTier();
		}

		if (state.infectionState().totalTicks() % 200 == 0 && state.tiers().degradeContainment()) {
			state.markDirty();
		}

		if (!state.singularity().fusing().isSingularityActive() 
				&& !state.singularity().fusing().shouldSkipSpread()) {
			BlockMutationHelper.mutateAroundSources(world, state.getVirusSources(), tier, apocalypse);
		}
		state.tierProgression().reinforceCores(tier);
		if (state.trySpawnMatrixCube()) {
			state.markDirty();
		}

		applyDifficultyRules(world, tier);
		state.singularity().barrier().tick(tier, tierDuration);
		state.combat().tick(world, tier);
	}
}
