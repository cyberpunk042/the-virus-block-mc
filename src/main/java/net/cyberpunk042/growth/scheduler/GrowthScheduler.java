package net.cyberpunk042.growth.scheduler;

import java.util.Objects;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.VirusScheduler;
import net.cyberpunk042.infection.api.VirusSchedulerTaskRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Entry point for scheduling growth block mutations from scenarios or other
 * systems. Wraps {@link VirusScheduler} access so callers only need a world,
 * target position, and mutation payload.
 */
public final class GrowthScheduler {
	public static final Identifier TASK_ID = Identifier.of("the-virus-block", "growth_mutation");

	private GrowthScheduler() {
	}

	public static void registerSchedulerTasks() {
		VirusSchedulerTaskRegistry.register(TASK_ID, GrowthMutationTask::fromNbt);
	}

	public static void schedule(ServerWorld world, BlockPos pos, GrowthMutation mutation, int delayTicks) {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(pos, "pos");
		Objects.requireNonNull(mutation, "mutation");
		if (mutation.isEmpty()) {
			return;
		}
		VirusWorldState state = VirusWorldState.get(world);
		VirusScheduler scheduler = state.orchestrator().services().scheduler();
		scheduler.schedule(Math.max(0, delayTicks), new GrowthMutationTask(world, pos, mutation));
	}

	public static boolean applyNow(ServerWorld world, BlockPos pos, GrowthMutation mutation) {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(pos, "pos");
		Objects.requireNonNull(mutation, "mutation");
		if (mutation.isEmpty()) {
			return false;
		}
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (!(blockEntity instanceof ProgressiveGrowthBlockEntity growth)) {
			return false;
		}
		return growth.applyMutation(mutation);
	}
}

