package net.cyberpunk042.block.entity;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.entity.FallingMatrixCubeEntity;
import net.cyberpunk042.infection.CorruptionProfiler;
import net.cyberpunk042.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Wrapper block entity used solely to convert the placed matrix cube block
 * into its falling entity counterpart. Once the entity is spawned the block
 * entity removes itself and the block is turned into air.
 */
public class MatrixCubeBlockEntity extends BlockEntity {

	private static final ConcurrentHashMap<ServerWorld, Set<UUID>> ACTIVE = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<ServerWorld, ConcurrentHashMap<UUID, BlockPos>> ACTIVE_POSITIONS = new ConcurrentHashMap<>();
	private static final String SPAWNED_KEY = "Spawned";
	private boolean spawned;

	public MatrixCubeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MATRIX_CUBE, pos, state);
	}

	public static void tick(World world, BlockPos pos, BlockState state, MatrixCubeBlockEntity cube) {
		if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (cube.spawned) {
			return;
		}
		cube.spawned = true;

		world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		FallingMatrixCubeEntity entity = new FallingMatrixCubeEntity(serverWorld, pos, state);
		register(serverWorld, entity.getUuid(), pos);
		entity.markRegistered();
		serverWorld.spawnEntity(entity);
		CorruptionProfiler.logMatrixCubeEntity(serverWorld, pos);
		world.removeBlockEntity(pos);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putBoolean(SPAWNED_KEY, this.spawned);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		this.spawned = view.getBoolean(SPAWNED_KEY, false);
	}

	public void markSettled() {
		this.spawned = true;
		this.markDirty();
	}

	public static int getActiveCount(ServerWorld world) {
		return ActiveTracker.count(world);
	}

	public static void destroyAll(ServerWorld world) {
		Set<UUID> ids = ACTIVE.remove(world);
		ACTIVE_POSITIONS.remove(world);
		if (ids == null) {
			return;
		}
		for (UUID id : ids) {
			Entity entity = world.getEntity(id);
			if (entity != null) {
				entity.discard();
			}
		}
	}

	public static void register(ServerWorld world, UUID id) {
		ActiveTracker.register(world, id, null);
	}

	public static void register(ServerWorld world, UUID id, BlockPos pos) {
		ActiveTracker.register(world, id, pos);
	}

	public static void unregister(ServerWorld world, UUID id) {
		ActiveTracker.unregister(world, id);
	}

	public static void trimActive(ServerWorld world, int limit) {
		ActiveTracker.trim(world, limit);
	}

	public static final class ActiveTracker {
		private ActiveTracker() {
		}

		public static void register(ServerWorld world, UUID id, @Nullable BlockPos pos) {
			ACTIVE.computeIfAbsent(world, ignored -> ConcurrentHashMap.newKeySet()).add(id);
			if (pos != null) {
				ACTIVE_POSITIONS.computeIfAbsent(world, ignored -> new ConcurrentHashMap<>()).put(id, pos.toImmutable());
			}
		}

		public static void unregister(ServerWorld world, UUID id) {
			Set<UUID> ids = ACTIVE.get(world);
			if (ids != null) {
				ids.remove(id);
			}
			ConcurrentHashMap<UUID, BlockPos> positions = ACTIVE_POSITIONS.get(world);
			if (positions != null) {
				positions.remove(id);
			}
		}

		public static int count(ServerWorld world) {
			Set<UUID> ids = ACTIVE.get(world);
			if (ids == null || ids.isEmpty()) {
				return 0;
			}
			prune(world, ids);
			return ids.size();
		}

		public static void trim(ServerWorld world, int limit) {
			if (limit < 0) {
				limit = 0;
			}
			Set<UUID> ids = ACTIVE.get(world);
			if (ids == null) {
				return;
			}
			prune(world, ids);
			if (ids.size() <= limit) {
				return;
			}
			Iterator<UUID> iterator = ids.iterator();
			while (ids.size() > limit && iterator.hasNext()) {
				UUID id = iterator.next();
				iterator.remove();
				ConcurrentHashMap<UUID, BlockPos> positions = ACTIVE_POSITIONS.get(world);
				if (positions != null) {
					positions.remove(id);
				}
				Entity entity = world.getEntity(id);
				if (entity != null) {
					entity.discard();
				}
			}
		}

		private static void prune(ServerWorld world, Set<UUID> ids) {
			Iterator<UUID> iterator = ids.iterator();
			while (iterator.hasNext()) {
				UUID id = iterator.next();
				Entity entity = world.getEntity(id);
				if (entity == null || !entity.isAlive()) {
					iterator.remove();
					BlockPos pos = removePosition(world, id);
					CorruptionProfiler.logMatrixCubeCleanup(world, pos, id);
				}
			}
		}

		private static BlockPos removePosition(ServerWorld world, UUID id) {
			Map<UUID, BlockPos> map = ACTIVE_POSITIONS.get(world);
			if (map == null) {
				return null;
			}
			return map.remove(id);
		}
	}
}

