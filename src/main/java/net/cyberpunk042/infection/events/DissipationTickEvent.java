package net.cyberpunk042.infection.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record DissipationTickEvent(ServerWorld world, BlockPos center, int remainingDelay) implements InfectionEvent {
}

