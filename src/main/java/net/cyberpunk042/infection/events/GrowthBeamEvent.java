package net.cyberpunk042.infection.events;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record GrowthBeamEvent(
		ServerWorld world,
		BlockPos origin,
		int targetEntityId,
		Vec3d targetPos,
		boolean pulling,
		float[] color,
		int durationTicks) implements InfectionEvent {
}

