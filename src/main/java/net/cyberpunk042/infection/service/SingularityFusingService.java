package net.cyberpunk042.infection.service;


import net.cyberpunk042.log.Logging;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.block.entity.SingularityBlockEntity;
import net.cyberpunk042.entity.VirusFuseEntity;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.state.TierModule;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.api.SingularityContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldEvents;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Encapsulates the fusing phase countdown, fuse entity management, and shell-collapse triggers.
 */
public final class SingularityFusingService {

	private static final DustColorTransitionParticleEffect SINGULARITY_FUSE_GLOW =
			new DustColorTransitionParticleEffect(0xFFFFFF, 0xFF3333, 1.1F);

	private final VirusWorldState host;
	private final Map<BlockPos, UUID> activeFuseEntities = new HashMap<>();
	private final Map<BlockPos, BlockState> fuseClearedBlocks = new HashMap<>();

	public SingularityFusingService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void beginFusing(SingularityContext ctx, long fuseDelay) {
		ServerWorld world = ctx.world();
		host.singularity().lifecycle().logSingularityStateChange(SingularityState.FUSING,
				"finalWaveWarning remaining=" + host.tiers().ticksUntilFinalWave() + " fuseDelay=" + fuseDelay);
		host.singularityState().singularityState = SingularityState.FUSING;
		host.singularityState().singularityTicks = fuseDelay;
		host.singularityState().shellCollapsed = false;
		host.singularityState().fusePulseTicker = 0;
		host.singularityState().fuseElapsed = 0L;
		host.singularityState().singularityCollapseTotalChunks = 0;
		host.singularityState().singularityCollapseCompletedChunks = 0;
		host.singularityState().singularityCollapseBarDelay = 0;
		host.singularityState().singularityCollapseCompleteHold = 0;
		BlockPos representativePos = host.infection().representativePos(world, world.getRandom(), host.sourceState());
		host.singularityState().center = representativePos != null ? representativePos.toImmutable() : null;
		host.singularity().phase().applyCollapseDistanceOverrides();
		if (host.collapseConfig().configuredCollapseEnabled(world)) {
			host.singularity().phase().prepareSingularityChunkQueue(ctx);
			// Pre-collapse drainage is scheduled after preload completes (in ChunkPreparationService)
		} else {
			host.collapseModule().queues().chunkQueue().clear();
			host.collapseModule().queues().clearPreCollapseDrainageJob();
		}
		host.infection().broadcast(world, Text.translatable("message.the-virus-block.singularity_warning").formatted(Formatting.LIGHT_PURPLE));
		host.markDirty();
	}

	public void handleSingularityInactive() {
		ServerWorld world = host.world();
		if (host.singularityState().singularityState == SingularityState.DORMANT) {
			return;
		}
		clearFuseEntities();
		revertSingularityBlock(false);
		host.singularity().phase().resetSingularityState();
		host.markDirty();
	}

	public void emitFuseEffects() {
		ServerWorld world = host.world();
		Set<BlockPos> sources = host.getVirusSources();
		if (sources.isEmpty()) {
			return;
		}
		float intensity = 1.0F - (float) host.singularityState().singularityTicks / Math.max(1.0F, (float) host.collapseConfig().configuredFuseExplosionDelayTicks());
		int particleCount = MathHelper.clamp(2 + (int) (intensity * 6), 2, 10);
		int pulseInterval = Math.max(4, host.collapseConfig().configuredFusePulseInterval() - MathHelper.floor(intensity * 4));
		host.singularityState().fusePulseTicker = host.singularityState().fusePulseTicker + 1;
		boolean pulse = host.singularityState().fusePulseTicker >= pulseInterval;
		if (pulse) {
			host.singularityState().fusePulseTicker = 0;
		}
		for (BlockPos source : sources) {
			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
				continue;
			}
			Vec3d fusePos = resolveFusePosition(world, source);
			double baseX = fusePos.x;
			double baseY = fusePos.y;
			double baseZ = fusePos.z;
			for (int i = 0; i < particleCount; i++) {
				double x = baseX + world.random.nextGaussian() * 0.3D;
				double y = baseY + world.random.nextGaussian() * 0.3D;
				double z = baseZ + world.random.nextGaussian() * 0.3D;
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.0D, 0.02D, 0.0D, 0.01D);
			}
			if (pulse) {
				world.spawnParticles(ParticleTypes.FLASH, baseX, baseY, baseZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
				world.spawnParticles(SINGULARITY_FUSE_GLOW, baseX, baseY + 0.1D, baseZ, 6, 0.25D, 0.2D, 0.25D, 0.0D);
				world.spawnParticles(ParticleTypes.GLOW, baseX, baseY + 0.2D, baseZ, 4, 0.2D, 0.2D, 0.2D, 0.0D);
				world.playSound(null,
						baseX,
						baseY,
						baseZ,
						SoundEvents.ENTITY_TNT_PRIMED,
						SoundCategory.BLOCKS,
						0.8F + intensity * 0.4F,
						0.8F + world.random.nextFloat() * 0.2F);
			}
		}
	}

	public void maintainFuseEntities() {
		ServerWorld world = host.world();
		activeFuseEntities.entrySet().removeIf(entry -> {
			Entity entity = world.getEntity(entry.getValue());
			if (entity instanceof VirusFuseEntity fuse && fuse.isAlive()) {
				return false;
			}
			Logging.FUSE.info("Removing dead fuse entity at {}", entry.getKey());
			return true;
		});
		try (LogScope scope = Logging.FUSE.scope("process-getVirusSources", LogLevel.INFO)) {
    		for (BlockPos source : host.getVirusSources()) {
    			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
    				continue;
    			}
    			clearBlockForFuse(world, source);
    			if (activeFuseEntities.containsKey(source)) {
    				continue;
    			}
    			VirusFuseEntity fuse = new VirusFuseEntity(world, source);
    			if (world.spawnEntity(fuse)) {
    				activeFuseEntities.put(source.toImmutable(), fuse.getUuid());
    				scope.branch("entry").kv("source", source);
    			} else {
    				Logging.SINGULARITY.topic("fusing").warn("[Fuse] Failed to spawn fuse entity at {}", source);
    			}
    		}
		}
	}

	public void clearFuseEntities() {
		ServerWorld world = host.world();
		clearFuseEntities(true);
	}

	public void clearFuseEntities(boolean restoreBlocks) {
		ServerWorld world = host.world();
		if (activeFuseEntities.isEmpty()) {
			if (restoreBlocks) {
				restoreClearedFuseBlocks(world);
			}
			return;
		}
		for (UUID uuid : activeFuseEntities.values()) {
			Entity entity = world.getEntity(uuid);
			if (entity != null) {
				entity.discard();
			}
		}
		activeFuseEntities.clear();
		if (restoreBlocks) {
			restoreClearedFuseBlocks(world);
		}
	}

	public boolean isFuseClearedBlock(BlockPos pos) {
		return fuseClearedBlocks.containsKey(pos);
	}

	public boolean isVirusCoreBlock(BlockPos pos, BlockState state) {
		return state.isOf(ModBlocks.VIRUS_BLOCK) || fuseClearedBlocks.containsKey(pos);
	}

	public void activateSingularityBlock() {
		ServerWorld world = host.world();
		BlockPos center = host.singularityState().center;
		if (center == null || !world.isChunkLoaded(ChunkPos.toLong(center))) {
			return;
		}
		BlockState state = world.getBlockState(center);
		if (!state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
			world.setBlockState(center, ModBlocks.SINGULARITY_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
		}
		fuseClearedBlocks.remove(center);
		BlockEntity blockEntity = world.getBlockEntity(center);
		if (blockEntity instanceof SingularityBlockEntity singularityBlock) {
			singularityBlock.startSequence(world);
		}
	}

	public void detonateFuseCore() {
		ServerWorld world = host.world();
		BlockPos center = host.singularityState().center;
		if (center == null) {
			return;
		}
		Vec3d fusePos = resolveFusePosition(world, center);
		BlockPos elevatedCenter = BlockPos.ofFloored(fusePos);
		if (!elevatedCenter.equals(center)) {
			host.singularityState().center = elevatedCenter != null ? elevatedCenter.toImmutable() : null;
			center = elevatedCenter;
		}
		world.playSound(null,
				fusePos.x,
				fusePos.y,
				fusePos.z,
				SoundEvents.ENTITY_GENERIC_EXPLODE,
				SoundCategory.BLOCKS,
				4.0F,
				0.6F);
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
				fusePos.x,
				fusePos.y,
				fusePos.z,
				2,
				0.2D,
				0.2D,
				0.2D,
				0.01D);
		host.presentationCoord().pushPlayersFromBlock(
				center,
				TierModule.FINAL_VULNERABILITY_BLAST_RADIUS,
				TierModule.FINAL_VULNERABILITY_BLAST_SCALE,
				false);
		world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, center, Block.getRawIdFromState(ModBlocks.VIRUS_BLOCK.getDefaultState()));
		clearFuseEntities(false);
	}

	public void revertSingularityBlock(boolean remove) {
		ServerWorld world = host.world();
		BlockPos center = host.singularityState().center;
		if (center == null || !world.isChunkLoaded(ChunkPos.toLong(center))) {
			return;
		}
		BlockState state = world.getBlockState(center);
		if (!state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
			return;
		}
		BlockState replacement = remove ? Blocks.AIR.getDefaultState() : ModBlocks.VIRUS_BLOCK.getDefaultState();
		world.setBlockState(center, replacement, Block.NOTIFY_LISTENERS);
		SingularityBlockEntity.notifyStop(world, center);
		world.getChunkManager().markForUpdate(center);
		fuseClearedBlocks.remove(center);
	}

	public void clearFuseClearedBlocks() {
		fuseClearedBlocks.clear();
	}

	public boolean isSingularitySuppressionActive() {
		return host.singularityState().singularityState == SingularityState.FUSING;
	}

	public boolean isSingularityActive() {
		return host.singularityState().singularityState != SingularityState.DORMANT;
	}

	public float getSingularitySuppressionProgress() {
		if (!isSingularitySuppressionActive()) {
			return 0.0F;
		}
		return MathHelper.clamp(
				1.0F - (float) host.singularityState().singularityTicks / (float) host.collapseConfig().configuredFuseExplosionDelayTicks(),
				0.0F,
				1.0F);
	}

	public double singularityActivityMultiplier() {
		if (!isSingularitySuppressionActive()) {
			return 1.0D;
		}
		return MathHelper.clamp(1.0D - getSingularitySuppressionProgress(), 0.0D, 1.0D);
	}

	public float matrixCubeSingularityFactor() {
		return switch (host.singularityState().singularityState) {
			case DORMANT -> 1.0F;
			case FUSING -> 0.35F;
			default -> 0.0F;
		};
	}

	public boolean shouldSkipSpread() {
		ServerWorld world = host.world();
		float progress = getSingularitySuppressionProgress();
		if (progress <= 0.0F) {
			return false;
		}
		if (progress >= 0.999F) {
			return true;
		}
		return world.random.nextFloat() < progress;
	}

	private void clearBlockForFuse(ServerWorld world, BlockPos pos) {
		if (fuseClearedBlocks.containsKey(pos)) {
			return;
		}
		BlockState state = world.getBlockState(pos);
		if (!state.isOf(ModBlocks.VIRUS_BLOCK)) {
			return;
		}
		host.sources().suppressUnregister(host.sourceState(), pos);
		fuseClearedBlocks.put(pos.toImmutable(), state);
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.SKIP_DROPS);
	}

	private void restoreClearedFuseBlocks(ServerWorld world) {
		if (fuseClearedBlocks.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<BlockPos, BlockState>> iterator = fuseClearedBlocks.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<BlockPos, BlockState> entry = iterator.next();
			BlockPos pos = entry.getKey();
			if (!world.isChunkLoaded(ChunkPos.toLong(pos))) {
				continue;
			}
			BlockState current = world.getBlockState(pos);
			if (current.isAir()) {
				world.setBlockState(pos, entry.getValue(), Block.NOTIFY_LISTENERS);
			}
			iterator.remove();
		}
	}

	private Vec3d resolveFusePosition(ServerWorld world, BlockPos source) {
		UUID fuseId = activeFuseEntities.get(source);
		if (fuseId != null) {
			Entity entity = world.getEntity(fuseId);
			if (entity instanceof VirusFuseEntity fuse) {
				return fuse.getPos();
			}
		}
		return Vec3d.ofCenter(source);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Progress accessors (for boss bar display)
	// ─────────────────────────────────────────────────────────────────────────────

	public float fuseProgress() {
		return getSingularitySuppressionProgress();
	}

	public float collapseProgress() {
		if (host.singularityState().singularityState != SingularityState.COLLAPSE) {
			return 0.0F;
		}
		if (host.singularityState().singularityCollapseBarDelay > 0) {
			return 1.0F;
		}
		// Use CollapseProcessor progress (radius-based)
		if (host.singularity().collapseProcessor().isActive()) {
			double processorProgress = host.singularity().collapseProcessor().progress();
			// Progress goes 0→1, we want to show 100%→0% (remaining)
			return MathHelper.clamp(1.0F - (float) processorProgress, 0.0F, 1.0F);
		}
		// Legacy: border-based progress
		if (host.singularity().borderState().duration > 0L) {
			float elapsed = MathHelper.clamp((float) host.singularity().borderState().elapsed / (float) host.singularity().borderState().duration, 0.0F, 1.0F);
			return MathHelper.clamp(1.0F - elapsed, 0.0F, 1.0F);
		}
		// Legacy: chunk-based progress
		if (host.singularityState().singularityCollapseTotalChunks <= 0) {
			return 0.0F;
		}
		float completed = MathHelper.clamp((float) host.singularityState().singularityCollapseCompletedChunks / (float) host.singularityState().singularityCollapseTotalChunks, 0.0F, 1.0F);
		return MathHelper.clamp(1.0F - completed, 0.0F, 1.0F);
	}
}

