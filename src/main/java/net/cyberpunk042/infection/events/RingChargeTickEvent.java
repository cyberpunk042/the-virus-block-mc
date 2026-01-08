package net.cyberpunk042.infection.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record RingChargeTickEvent(ServerWorld world, BlockPos center, int remainingTicks) implements InfectionEvent {
}

