package net.cyberpunk042.infection.service;

import java.util.Objects;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.infection.GlobalTerrainCorruption;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.state.InfectionState;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;

/**
 * Handles infection start/end bookkeeping: boobytraps, debug setup, and cleansing state.
 */
public final class InfectionLifecycleService {

	private final VirusWorldState host;

	public InfectionLifecycleService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void captureBoobytrapDefaults() {
		ServerWorld world = host.world();
		InfectionState state = host.infectionState();
		if (state.boobytrapDefaultsCaptured()) {
			return;
		}
		GameRules rules = world.getGameRules();
		state.setDefaultBoobytrapsEnabled(rules.getBoolean(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED));
		state.setDefaultWormSpawnChance(rules.getInt(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE));
		state.setDefaultWormTrapSpawnChance(rules.getInt(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE));
		state.setBoobytrapDefaultsCaptured(true);
		host.markDirty();
	}

	public void disableBoobytraps() {
		ServerWorld world = host.world();
		captureBoobytrapDefaults();
		GameRules rules = world.getGameRules();
		rules.get(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED).set(false, world.getServer());
		int spawnRate = Math.max(1, getDefaultWormSpawnChance());
		int trapRate = Math.max(1, getDefaultWormTrapSpawnChance());
		rules.get(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE).set(spawnRate, world.getServer());
		rules.get(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE).set(trapRate, world.getServer());
		host.infectionState().setDormant(true);
		host.markDirty();
	}

	public void restoreBoobytrapRules() {
		ServerWorld world = host.world();
		captureBoobytrapDefaults();
		GameRules rules = world.getGameRules();
		rules.get(TheVirusBlock.VIRUS_BOOBYTRAPS_ENABLED).set(host.infectionState().defaultBoobytrapsEnabled(), world.getServer());
		rules.get(TheVirusBlock.VIRUS_WORM_SPAWN_CHANCE).set(getDefaultWormSpawnChance(), world.getServer());
		rules.get(TheVirusBlock.VIRUS_WORM_TRAP_SPAWN_CHANCE).set(getDefaultWormTrapSpawnChance(), world.getServer());
		host.infectionState().setDormant(false);
		host.markDirty();
	}

	public void beginCleansing() {
		InfectionState state = host.infectionState();
		if (!state.cleansingActive()) {
			state.setCleansingActive(true);
			state.setTerrainCorrupted(false);
			host.markDirty();
		}
	}

	public void ensureDebugInfection(BlockPos center) {
		ServerWorld world = host.world();
		host.infectionState().setInfected(true);
		host.infectionState().setDormant(false);
		host.tiers().setApocalypseMode(false);
		captureBoobytrapDefaults();
		restoreBoobytrapRules();
		host.sources().clearSources(host.sourceState());
		host.sources().addSource(host.sourceState(), center);
		if (world.isChunkLoaded(ChunkPos.toLong(center)) && world.getBlockState(center).isAir()) {
			world.setBlockState(center, ModBlocks.VIRUS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
		host.singularity().fusing().clearFuseClearedBlocks();
		host.sources().clearSuppressed(host.sourceState());
		host.markDirty();
	}

	public void startInfection(BlockPos pos) {
		ServerWorld world = host.world();
		host.infectionState().setInfected(true);
		captureBoobytrapDefaults();
		restoreBoobytrapRules();
		host.infectionState().setDormant(false);
		host.markDirty();
		host.tiers().resetForInfectionStart();
		host.infectionState().resetTicks();
		host.infectionState().setTerrainCorrupted(false);
		host.shell().setCollapsed(false);
		host.shell().setRebuildPending(host.tiers().currentTier().getIndex() >= 3);
		host.singularity().barrier().setActive(false);
		host.infectionState().setCleansingActive(false);
		host.tiers().resetHealthForTier(host.tiers().currentTier());
		host.pillarChunks().clear();
		host.infectionState().eventHistory().clear();
		host.singularity().phase().resetSingularityState();
		host.singularity().barrier().setNextPushTick(0L);
		host.singularity().barrier().setFinalBlastTriggered(false);
		host.markDirty();
		GlobalTerrainCorruption.trigger(world, pos);
	}

	public void endInfection() {
		ServerWorld world = host.world();
		if (!host.infectionState().infected()) {
			return;
		}
		restoreBoobytrapRules();
		host.infectionState().setInfected(false);
		host.infectionState().setDormant(false);
		host.markDirty();
		host.tiers().clearForEnd();
		host.infectionState().resetTicks();
		host.infectionState().setTerrainCorrupted(false);
		host.shell().setCollapsed(false);
		host.shell().setRebuildPending(false);
		host.singularity().phase().resetSingularityState();
		host.singularity().barrier().deactivate();
		// Trigger actual terrain cleansing (removes virus blocks and corrupted terrain)
		GlobalTerrainCorruption.cleanse(world);
		host.infectionState().eventHistory().clear();
		host.shell().clearCooldowns();
		host.pillarChunks().clear();
		host.singularity().barrier().setNextPushTick(0L);
		host.singularity().barrier().setFinalBlastTriggered(false);
		host.combat().voidTears().burstAndClear();
		host.markDirty();
		MatrixCubeBlockEntity.destroyAll(world);
		Text message = Text.translatable("message.the-virus-block.cleansed").formatted(Formatting.AQUA);
		world.getPlayers(ServerPlayerEntity::isAlive).forEach(player -> player.sendMessage(message, false));
	}

	public void handleContainmentResetCleanup() {
		ServerWorld world = host.world();
		beginCleansing();
		host.presentationCoord().updateBossBars();
		GlobalTerrainCorruption.cleanse(world);
		MatrixCubeBlockEntity.destroyAll(world);
		host.shell().clearCooldowns();
		host.pillarChunks().clear();
		host.singularity().fusing().clearFuseClearedBlocks();
		host.tiers().setApocalypseMode(false);
		host.infectionState().setTerrainCorrupted(false);
		host.shell().setCollapsed(false);
		host.infectionState().setDormant(false);
		host.markDirty();
	}

	private int getDefaultWormSpawnChance() {
		int stored = host.infectionState().defaultWormSpawnChance();
		return stored > 0 ? stored : 6;
	}

	private int getDefaultWormTrapSpawnChance() {
		int stored = host.infectionState().defaultWormTrapSpawnChance();
		return stored > 0 ? stored : 135;
	}

	public boolean enableTerrainCorruption() {
		if (host.infectionState().terrainCorrupted()) {
			return false;
		}
		host.infectionState().setTerrainCorrupted(true);
		host.infectionState().setCleansingActive(false);
		host.markDirty();
		return true;
	}
}

