package net.cyberpunk042.infection.orchestrator;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Bridge that exposes controlled mutations and queries to orchestration code.
 */
public interface IWorldCallbacks {

	/* -------- State queries -------- */

	<T> T query(Function<VirusWorldState, T> extractor);

	boolean mutate(Consumer<VirusWorldState> mutation);

	/* -------- Broadcast / messaging -------- */

	void broadcast(ServerWorld world, Text message);

	/* -------- Scheduling -------- */

	UUID schedule(Runnable task, int delayTicks);

	void cancel(UUID taskId);

	/* -------- Config helpers -------- */

	ServiceConfig.PostReset postResetConfig();
}
