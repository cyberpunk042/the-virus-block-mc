package net.cyberpunk042.growth.scheduler;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.util.ServerRef;
import net.cyberpunk042.infection.api.VirusScheduler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

final class GrowthMutationTask implements VirusScheduler.PersistedTask {
	private final RegistryKey<World> dimension;
	private final BlockPos pos;
	private final GrowthMutation mutation;
	@Nullable
	private final ServerWorld worldRef;

	GrowthMutationTask(ServerWorld world, BlockPos pos, GrowthMutation mutation) {
		this(world.getRegistryKey(), pos.toImmutable(), mutation, world);
	}

	private GrowthMutationTask(RegistryKey<World> dimension, BlockPos pos, GrowthMutation mutation, @Nullable ServerWorld worldRef) {
		this.dimension = dimension;
		this.pos = pos;
		this.mutation = mutation;
		this.worldRef = worldRef;
	}

	@Override
	public void run() {
		ServerWorld world = resolveWorld();
		if (world == null) {
			return;
		}
		BlockEntity entity = world.getBlockEntity(pos);
		if (entity instanceof ProgressiveGrowthBlockEntity growth) {
			growth.applyMutation(mutation);
		}
	}

	@Nullable
	private ServerWorld resolveWorld() {
		if (worldRef != null && !worldRef.isClient()) {
			return worldRef;
		}
		return ServerRef.current()
				.map(server -> server.getWorld(dimension))
				.orElse(null);
	}

	@Override
	public Identifier type() {
		return GrowthScheduler.TASK_ID;
	}

	@Override
	public NbtCompound save() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("dimension", dimension.getValue().toString());
		nbt.putLong("pos", pos.asLong());
		nbt.put("mutation", mutation.toNbt());
		return nbt;
	}

	static VirusScheduler.PersistedTask fromNbt(NbtCompound nbt) {
		String dimensionRaw = nbt.getString("dimension").orElse("");
		Identifier dimensionId = dimensionRaw.isEmpty() ? null : Identifier.tryParse(dimensionRaw);
		RegistryKey<World> key = dimensionId != null ? RegistryKey.of(RegistryKeys.WORLD, dimensionId) : World.OVERWORLD;
		long packedPos = nbt.getLong("pos").orElse(0L);
		BlockPos pos = BlockPos.fromLong(packedPos);
		GrowthMutation mutation = nbt.getCompound("mutation")
				.map(GrowthMutation::fromNbt)
				.orElseGet(GrowthMutation::new);
		return new GrowthMutationTask(key, pos, mutation, null);
	}
}

