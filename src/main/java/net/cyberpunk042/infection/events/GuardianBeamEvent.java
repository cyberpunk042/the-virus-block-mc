package net.cyberpunk042.infection.events;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public record GuardianBeamEvent(ServerWorld world, BlockPos origin, ServerPlayerEntity target, int durationTicks)
		implements InfectionEvent {
}

