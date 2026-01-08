package net.cyberpunk042.infection.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record RingPulseEvent(ServerWorld world, BlockPos center, double radius, int remainingTicks) implements InfectionEvent {
}

