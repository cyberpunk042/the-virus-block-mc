package net.cyberpunk042.infection.events;

import net.cyberpunk042.growth.profile.GrowthForceProfile;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public record GrowthForceEvent(ServerWorld world,
		BlockPos origin,
		ForceType type,
		double radius,
		double strength,
		GrowthForceProfile profile) implements InfectionEvent {
	public enum ForceType {
		PULL,
		PUSH
	}
}

