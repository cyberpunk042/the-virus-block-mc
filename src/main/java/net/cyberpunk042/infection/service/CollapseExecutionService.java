package net.cyberpunk042.infection.service;


import net.cyberpunk042.log.Logging;
import java.util.Objects;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.events.CollapseChunkVeilEvent;
import net.cyberpunk042.infection.profile.CollapseFillMode;
import net.cyberpunk042.infection.singularity.CollapseErosionSettings;
import net.cyberpunk042.infection.singularity.SingularityRingSlices;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.border.WorldBorder;

import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.profile.CollapseFillShape;
import net.cyberpunk042.infection.profile.CollapseSyncProfile;
import net.cyberpunk042.infection.profile.DimensionProfile.Collapse.PreCollapseWaterDrainage.PreDrainMode;
import net.cyberpunk042.infection.singularity.BulkFillHelper;
import net.cyberpunk042.infection.singularity.SingularityChunkContext;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;

/**
 * Encapsulates erosion/collapse helpers that were previously embedded in {@link VirusWorldState}.
 */
public final class CollapseExecutionService {

	private final VirusWorldState host;
	private int operationsThisTick = 0;

	public CollapseExecutionService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public int getOperationsThisTick() {
		return operationsThisTick;
	}

	public void resetOperationsThisTick() {
		operationsThisTick = 0;
	}

	public void spawnChunkVeil(ChunkPos chunk) {
		ServerWorld world = host.world();
		BlockPos center = host.singularityState().center;
		if (center == null) {
			return;
		}
		EffectBus bus = host.orchestrator().services().effectBus();
		if (bus != null) {
			bus.post(new CollapseChunkVeilEvent(world, chunk, center.toImmutable()));
		}
	}

	public double chunkDistanceFromCenter(long chunkLong) {
		BlockPos center = host.singularityState().center;
		if (center == null) {
			return 0.0D;
		}
		ChunkPos chunk = new ChunkPos(chunkLong);
		ChunkPos centerPos = new ChunkPos(center);
		double dx = (chunk.x - centerPos.x) * 16.0D;
		double dz = (chunk.z - centerPos.z) * 16.0D;
		return Math.sqrt(dx * dx + dz * dz);
	}

	public double getCurrentCollapseRadius() {
		ServerWorld world = host.world();
		WorldBorder border = world.getWorldBorder();
		return Math.max(0.0D, border.getSize() * 0.5D);
	}

	public void spawnCollapseParticles(BlockPos pos,
			boolean particlesEnabled,
			int particleDensity) {
		ServerWorld world = host.world();
		if (!particlesEnabled) {
			return;
		}
		world.spawnParticles(
				ParticleTypes.ASH,
				pos.getX() + 0.5D,
				pos.getY() + 0.5D,
				pos.getZ() + 0.5D,
				particleDensity,
				0.25D,
				0.25D,
				0.25D,
				0.01D);
		if (world.random.nextInt(20) == 0) {
			world.playSound(null,
					pos,
					SoundEvents.BLOCK_SCULK_SPREAD,
					SoundCategory.HOSTILE,
					0.8F,
					0.4F + world.random.nextFloat() * 0.2F);
		}
	}

	public void carveColumn(BlockPos target,
			int startY,
			int endY,
			int step,
			boolean particlesEnabled,
			int particleDensity) {
		ServerWorld world = host.world();
		long chunkLong = ChunkPos.toLong(target);
		if (!world.isChunkLoaded(chunkLong)) {
			return;
		}
		BlockPos.Mutable pos = new BlockPos.Mutable(target.getX(), startY, target.getZ());
		for (int y = startY; step > 0 ? y <= endY : y >= endY; y += step) {
			pos.setY(y);
			BlockState state = world.getBlockState(pos);
			if (state.isAir()
					|| state.isOf(ModBlocks.VIRUS_BLOCK)
					|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
					|| state.getHardness(world, pos) < 0.0F) {
				continue;
			}
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			spawnCollapseParticles(pos, particlesEnabled, particleDensity);
		}
	}

	public boolean trySetAir(BlockPos pos, boolean respectImmunity) {
		ServerWorld world = host.world();
		BlockState state;
		try {
			state = world.getBlockState(pos);
		} catch (IllegalStateException ex) {
			return false;
		}
		if (state.isAir()) {
			return false;
		}
		if (respectImmunity) {
			if (state.isOf(ModBlocks.VIRUS_BLOCK)
					|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
					|| state.getHardness(world, pos) < 0.0F) {
				return false;
			}
		}
		try {
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			return true;
		} catch (IllegalStateException ex) {
			return false;
		}
	}

	public boolean tryClearCollapseBlock(BlockPos pos, boolean respectImmunity) {
		ServerWorld world = host.world();
		if (host.collapseConfig().configuredFillMode() == CollapseFillMode.DESTROY) {
			BlockState state;
			try {
				state = world.getBlockState(pos);
			} catch (IllegalStateException ex) {
				return false;
			}
			if (state.isAir()) {
				return false;
			}
			if (respectImmunity) {
				if (state.isOf(ModBlocks.VIRUS_BLOCK)
						|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
						|| state.getHardness(world, pos) < 0.0F) {
					return false;
				}
			}
			try {
				world.breakBlock(pos, false);
				return true;
			} catch (IllegalStateException ex) {
				return false;
			}
		}
		return trySetAir(pos, respectImmunity);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Fluid Draining (migrated from SingularityLifecycleService)
	// ─────────────────────────────────────────────────────────────────────────────

	public void drainFluidsInRange(ChunkPos chunk,
			int minY,
			int maxY,
			CollapseErosionSettings erosion) {
		ServerWorld world = host.world();
		drainFluidsInRange(
				chunk,
				minY,
				maxY,
				erosion.fillShape(),
				erosion.outlineThickness(),
				erosion.useNativeFill());
	}

	public void drainFluidsInRange(ChunkPos chunk,
			int minY,
			int maxY,
			CollapseFillShape shape,
			int outlineThickness,
			boolean useNativeFill) {
		ServerWorld world = host.world();
		drainFluidsInRange(chunk, minY, maxY, shape, outlineThickness, useNativeFill, Integer.MAX_VALUE);
	}

	public void drainFluidsInRange(ChunkPos chunk,
			int minY,
			int maxY,
			CollapseFillShape shape,
			int outlineThickness,
			boolean useNativeFill,
			int maxOps) {
		ServerWorld world = host.world();
		if (minY > maxY) {
			return;
		}
		int worldMinY = world.getBottomY();
		int worldMaxY = worldMinY + world.getDimension().height() - 1;
		int clampedMinY = Math.max(worldMinY, minY);
		int clampedMaxY = Math.min(worldMaxY, maxY);
		if (clampedMinY > clampedMaxY) {
			return;
		}
		BlockBox slice = new BlockBox(chunk.getStartX(),
				clampedMinY,
				chunk.getStartZ(),
				chunk.getEndX(),
				clampedMaxY,
				chunk.getEndZ());
		boolean canUseNativeFill = useNativeFill && shape == CollapseFillShape.MATRIX;
		if (canUseNativeFill && BulkFillHelper.clearFluidRange(world, slice, "minecraft:water", true)) {
			return;
		}
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int y = clampedMinY; y <= clampedMaxY && operationsThisTick < maxOps; y++) {
			for (int x = chunk.getStartX(); x <= chunk.getEndX() && operationsThisTick < maxOps; x++) {
				for (int z = chunk.getStartZ(); z <= chunk.getEndZ() && operationsThisTick < maxOps; z++) {
					if (!BulkFillHelper.shouldFillShape(x,
							y,
							z,
							slice.getMinX(),
							slice.getMinY(),
							slice.getMinZ(),
							slice.getMaxX(),
							slice.getMaxY(),
							slice.getMaxZ(),
							shape,
							outlineThickness)) {
						continue;
					}
					mutable.set(x, y, z);
					BlockState state;
					try {
						state = world.getBlockState(mutable);
					} catch (IllegalStateException ex) {
						SingularityChunkContext.recordSkippedMissing();
						return;
					}
					if (state.isAir()
							|| state.isOf(ModBlocks.VIRUS_BLOCK)
							|| state.isOf(ModBlocks.SINGULARITY_BLOCK)
							|| state.getFluidState().isEmpty()) {
						continue;
					}
					try {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
						SingularityChunkContext.recordWaterCleared();
						operationsThisTick++;
					} catch (IllegalStateException ex) {
						SingularityChunkContext.recordSkippedMissing();
						return;
					}
				}
			}
		}
	}

	public void drainChunkByMode(ChunkPos chunk, PreDrainMode mode) {
		ServerWorld world = host.world();
		drainChunkByMode(chunk, mode, 
			Math.max(1, host.collapseConfig().configuredOutlineThickness()),
			Integer.MAX_VALUE);
	}

	public void drainChunkByMode(ChunkPos chunk, PreDrainMode mode, int thickness, int maxOps) {
		ServerWorld world = host.world();
		if (!world.isChunkLoaded(chunk.toLong())) {
			return;
		}
		int minY = world.getBottomY();
		int maxY = minY + world.getDimension().height() - 1;
		
		// Get direction to center for direction-aware modes
		BlockPos center = host.singularityState().center;
		FaceDirection facingCenter = (center != null) ? getFacingCenter(chunk, center) : null;
		
		switch (mode) {
			case OUTLINE -> {
				// Direction-aware: only drain the face closest to center
				if (facingCenter != null) {
					drainFaceDirectional(world, chunk, minY, maxY, thickness, facingCenter, maxOps);
				} else {
					// No center, fall back to all faces
					drainFluidsInRange(chunk, minY, maxY, CollapseFillShape.OUTLINE, thickness, false, maxOps);
				}
			}
			case ROWS -> {
				// Direction-aware: drain rows on the face closest to center
				if (facingCenter != null) {
					drainRowsDirectional(world, chunk, minY, maxY, thickness, facingCenter, maxOps);
				} else {
					drainFluidsInRange(chunk, minY, maxY, CollapseFillShape.ROW, thickness, false, maxOps);
				}
			}
			case FACING_CENTER -> {
				// Explicit direction-aware face drain
				if (facingCenter != null) {
					drainFaceDirectional(world, chunk, minY, maxY, thickness, facingCenter, maxOps);
				} else {
					drainFluidsInRange(chunk, minY, maxY, CollapseFillShape.MATRIX, 1, true, maxOps);
				}
			}
			default -> {
				// FULL_INSTANT, FULL_PER_CHUNK - drain everything
				drainFluidsInRange(chunk, minY, maxY, CollapseFillShape.MATRIX, 1, true, maxOps);
			}
		}
	}

	private enum FaceDirection { NORTH, SOUTH, EAST, WEST }

	/**
	 * Determine which face of the chunk faces toward the singularity center.
	 */
	private FaceDirection getFacingCenter(ChunkPos chunk, BlockPos center) {
		int chunkCenterX = (chunk.getStartX() + chunk.getEndX()) / 2;
		int chunkCenterZ = (chunk.getStartZ() + chunk.getEndZ()) / 2;
		
		int dx = center.getX() - chunkCenterX;
		int dz = center.getZ() - chunkCenterZ;
		
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? FaceDirection.EAST : FaceDirection.WEST;
		} else {
			return dz > 0 ? FaceDirection.SOUTH : FaceDirection.NORTH;
		}
	}

	/**
	 * Drain only the chunk face that faces toward the singularity center.
	 */
	private void drainFaceDirectional(ServerWorld world, ChunkPos chunk, int minY, int maxY, int thickness, FaceDirection face, int maxOps) {
		int chunkMinX = chunk.getStartX();
		int chunkMaxX = chunk.getEndX();
		int chunkMinZ = chunk.getStartZ();
		int chunkMaxZ = chunk.getEndZ();
		
		int drainMinX, drainMaxX, drainMinZ, drainMaxZ;
		
		switch (face) {
			case EAST -> {
				drainMinX = chunkMaxX - thickness + 1;
				drainMaxX = chunkMaxX;
				drainMinZ = chunkMinZ;
				drainMaxZ = chunkMaxZ;
			}
			case WEST -> {
				drainMinX = chunkMinX;
				drainMaxX = chunkMinX + thickness - 1;
				drainMinZ = chunkMinZ;
				drainMaxZ = chunkMaxZ;
			}
			case SOUTH -> {
				drainMinX = chunkMinX;
				drainMaxX = chunkMaxX;
				drainMinZ = chunkMaxZ - thickness + 1;
				drainMaxZ = chunkMaxZ;
			}
			case NORTH -> {
				drainMinX = chunkMinX;
				drainMaxX = chunkMaxX;
				drainMinZ = chunkMinZ;
				drainMaxZ = chunkMinZ + thickness - 1;
			}
			default -> {
				return;
			}
		}
		
		drainFluidsInBox(world, drainMinX, drainMaxX, drainMinZ, drainMaxZ, minY, maxY, maxOps);
	}

	/**
	 * Drain rows on the face closest to center.
	 */
	private void drainRowsDirectional(ServerWorld world, ChunkPos chunk, int minY, int maxY, int thickness, FaceDirection face, int maxOps) {
		int chunkMinX = chunk.getStartX();
		int chunkMaxX = chunk.getEndX();
		int chunkMinZ = chunk.getStartZ();
		int chunkMaxZ = chunk.getEndZ();
		
		int drainMinX, drainMaxX, drainMinZ, drainMaxZ;
		
		// For rows, we drain a horizontal strip on the facing side
		int centerY = (minY + maxY) / 2;
		int rowMinY = centerY - thickness / 2;
		int rowMaxY = centerY + thickness / 2;
		
		switch (face) {
			case EAST -> {
				drainMinX = chunkMaxX - thickness + 1;
				drainMaxX = chunkMaxX;
				drainMinZ = chunkMinZ;
				drainMaxZ = chunkMaxZ;
			}
			case WEST -> {
				drainMinX = chunkMinX;
				drainMaxX = chunkMinX + thickness - 1;
				drainMinZ = chunkMinZ;
				drainMaxZ = chunkMaxZ;
			}
			case SOUTH -> {
				drainMinX = chunkMinX;
				drainMaxX = chunkMaxX;
				drainMinZ = chunkMaxZ - thickness + 1;
				drainMaxZ = chunkMaxZ;
			}
			case NORTH -> {
				drainMinX = chunkMinX;
				drainMaxX = chunkMaxX;
				drainMinZ = chunkMinZ;
				drainMaxZ = chunkMinZ + thickness - 1;
			}
			default -> {
				return;
			}
		}
		
		// Drain only the center rows on that face
		drainFluidsInBox(world, drainMinX, drainMaxX, drainMinZ, drainMaxZ, rowMinY, rowMaxY, maxOps);
	}

	/**
	 * Drain fluids in a specific box region with operations budget.
	 */
	private void drainFluidsInBox(ServerWorld world, int minX, int maxX, int minZ, int maxZ, int minY, int maxY, int maxOps) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockState air = Blocks.AIR.getDefaultState();
		
		for (int x = minX; x <= maxX && operationsThisTick < maxOps; x++) {
			for (int z = minZ; z <= maxZ && operationsThisTick < maxOps; z++) {
				for (int y = minY; y <= maxY && operationsThisTick < maxOps; y++) {
					mutable.set(x, y, z);
					try {
						BlockState state = world.getBlockState(mutable);
						if (!state.getFluidState().isEmpty()
								&& !state.isOf(ModBlocks.VIRUS_BLOCK)
								&& !state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
							world.setBlockState(mutable, air, Block.NOTIFY_LISTENERS);
							SingularityChunkContext.recordWaterCleared();
							operationsThisTick++;
						}
					} catch (IllegalStateException ex) {
						// Chunk not loaded
					}
				}
			}
		}
	}

}

