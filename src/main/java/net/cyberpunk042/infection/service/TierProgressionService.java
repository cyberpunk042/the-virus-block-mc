package net.cyberpunk042.infection.service;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Objects;
import net.cyberpunk042.block.corrupted.CorruptedStoneBlock;
import net.cyberpunk042.block.corrupted.CorruptionStage;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.state.TierModule;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

/**
 * Handles tier advancement, containment adjustments, and shell/pillar reinforcement.
 */
public final class TierProgressionService {

	private final VirusWorldState host;
	private final TierModule tiers;
	private final ShellRebuildService shellService;
	private final ShellRebuildService.State shellState;
	private final ShellRebuildService.Callbacks shellCallbacks;
	private final SingularityLifecycleService lifecycle;
	private final LongSet pillarChunks;

	public TierProgressionService(VirusWorldState host,
			TierModule tiers,
			ShellRebuildService shellService,
			ShellRebuildService.State shellState,
			ShellRebuildService.Callbacks shellCallbacks,
			SingularityLifecycleService lifecycle,
			LongSet pillarChunks) {
		this.host = Objects.requireNonNull(host, "host");
		this.tiers = Objects.requireNonNull(tiers, "tiers");
		this.shellService = Objects.requireNonNull(shellService, "shellService");
		this.shellState = Objects.requireNonNull(shellState, "shellState");
		this.shellCallbacks = Objects.requireNonNull(shellCallbacks, "shellCallbacks");
		this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
		this.pillarChunks = Objects.requireNonNull(pillarChunks, "pillarChunks");
	}

	public void advanceTier() {
		ServerWorld world = host.world();
		InfectionTierService.TierAdvanceResult result = tiers.advanceTier();
		if (!result.advancedTier()) {
			lifecycle.triggerFinalBarrierBlast();
			host.markDirty();
			return;
		}

		InfectionTier tier = result.tier();
		if (tier.getIndex() >= 3) {
			shellService.setShellRebuildPending(shellState, true);
		}
		shellService.clearCooldowns(shellState);
		host.singularity().barrier().resetTimers();
		tiers.resetHealthForTier(tier);

		BlockPos pos = host.infection().representativePos(host.world(), world.getRandom(), host.sourceState());
		if (pos != null) {
			world.playSound(null, pos, SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.BLOCKS, 2.5F, 1.0F);
		}

		Text message = Text.translatable("message.the-virus-block.tier_up", tier.getIndex() + 1).formatted(Formatting.DARK_PURPLE);
		world.getPlayers(PlayerEntity::isAlive).forEach(player -> player.sendMessage(message, false));
		host.sources().teleportSources();
		if (tier.getIndex() >= InfectionTier.maxIndex()) {
			host.sourceControl().spawnCoreGuardians();
		}
		host.markDirty();
	}

	public boolean forceAdvanceTier() {
		ServerWorld world = host.world();
		if (tiers.index() >= InfectionTier.maxIndex()) {
			tiers.setApocalypseMode(true);
			host.markDirty();
			return false;
		}

		advanceTier();
		return true;
	}

	public void applyContainmentCharge(int amount) {
		if (tiers.applyContainmentCharge(amount)) {
			host.markDirty();
		}
	}

	public boolean reduceMaxHealth(double factor) {
		ServerWorld world = host.world();
		if (!host.infectionState().infected() || factor <= 0.0D || factor >= 1.0D) {
			return false;
		}

		if (!tiers.reduceMaxHealth(host.tiers().currentTier(), factor)) {
			return false;
		}
		host.markDirty();
		host.presentationCoord().updateBossBars();
		return true;
	}

	public boolean bleedHealth(double fraction) {
		return bleedHealth(fraction, null);
	}

	/**
	 * Bleeds health from the infection, with viral adaptation tracking.
	 * 
	 * @param fraction Fraction of max health to damage (0.0 - 1.0)
	 * @param damageKey The damage category key for adaptation, or null for no tracking
	 * @return true if damage was applied
	 */
	public boolean bleedHealth(double fraction, String damageKey) {
		ServerWorld world = host.world();
		if (!host.infectionState().infected() || fraction <= 0.0D) {
			return false;
		}

		double amount = Math.max(1.0D, host.tiers().maxHealth(host.tiers().currentTier()) * MathHelper.clamp(fraction, 0.0D, 1.0D));
		return host.infection().applyHealthDamage(world, amount, damageKey);
	}

	public void reinforceCores(InfectionTier tier) {
		ServerWorld world = host.world();
		if (!host.hasVirusSources()) {
			return;
		}
		if (shellService.shellsCollapsed(shellState)) {
			return;
		}

		int tierIndex = tier.getIndex();
		if (tierIndex >= 2) {
			spawnCorruptedPillars(world, tierIndex);
		}
		if (tierIndex < 3) {
			return;
		}
		shellService.reinforceShells(host.world(), shellState, tier, host.getVirusSources(), shellCallbacks);
	}

	private void spawnCorruptedPillars(ServerWorld world, int tierIndex) {
		Random random = world.getRandom();
		int chunkRadius = MathHelper.clamp(1 + tierIndex, 1, 8);
		for (BlockPos core : host.getVirusSources()) {
			ChunkPos origin = new ChunkPos(core);
			for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
				for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
					ChunkPos chunk = new ChunkPos(origin.x + dx, origin.z + dz);
					long key = chunk.toLong();
					if (pillarChunks.contains(key)) {
						continue;
					}
					if (!world.isChunkLoaded(chunk.x, chunk.z)) {
						continue;
					}
					BlockPos base = findPillarBase(world, chunk, random);
					if (base == null) {
						continue;
					}
					buildPillar(world, base, tierIndex, random);
					pillarChunks.add(key);
					host.markDirty();
				}
			}
		}
	}

	private BlockPos findPillarBase(ServerWorld world, ChunkPos chunk, Random random) {
		for (int attempt = 0; attempt < 8; attempt++) {
			int x = chunk.getStartX() + random.nextInt(14) + 1;
			int z = chunk.getStartZ() + random.nextInt(14) + 1;
			if (!world.isChunkLoaded(chunk.x, chunk.z)) {
				continue;
			}
			int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
			if (topY < world.getBottomY()) {
				continue;
			}
			return new BlockPos(x, topY, z);
		}
		return null;
	}

	private void buildPillar(ServerWorld world, BlockPos base, int tierIndex, Random random) {
		int height = 4 + random.nextInt(4);
		BlockState state = ModBlocks.CORRUPTED_STONE.getDefaultState();
		CorruptionStage stage = tierIndex >= 4 ? CorruptionStage.STAGE_2 : CorruptionStage.STAGE_1;
		if (state.contains(CorruptedStoneBlock.STAGE)) {
			state = state.with(CorruptedStoneBlock.STAGE, stage);
		}
		for (int i = 0; i < height; i++) {
			BlockPos target = base.up(i);
			world.setBlockState(target, state, Block.NOTIFY_LISTENERS);
		}
	}
}

