package net.cyberpunk042.infection.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public record CollapseChunkVeilEvent(ServerWorld world, ChunkPos chunk, BlockPos singularityCenter) implements InfectionEvent {
}

