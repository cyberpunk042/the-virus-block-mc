package net.cyberpunk042.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public final class DelayedServerTasks {
	private static final Map<MinecraftServer, Long2ObjectMap<List<Runnable>>> TASKS = new WeakHashMap<>();
	
	private static volatile boolean initialized = false;

	private DelayedServerTasks() {
	}

	public static void init() {
		if (initialized) return;
		initialized = true;
		ServerTickEvents.END_SERVER_TICK.register(DelayedServerTasks::run);
	}

	public static void schedule(MinecraftServer server, int delayTicks, Runnable task) {
		if (server == null || task == null) {
			return;
		}
		int clampedDelay = Math.max(1, delayTicks);
		long targetTick = server.getOverworld().getTime() + clampedDelay;
		Long2ObjectMap<List<Runnable>> serverTasks = TASKS.computeIfAbsent(server, s -> new Long2ObjectOpenHashMap<>());
		serverTasks.computeIfAbsent(targetTick, t -> new ArrayList<>()).add(task);
	}

	private static void run(MinecraftServer server) {
		Long2ObjectMap<List<Runnable>> serverTasks = TASKS.get(server);
		if (serverTasks == null || serverTasks.isEmpty()) {
			return;
		}
		long currentTick = server.getOverworld().getTime();
		LongArrayList dueKeys = new LongArrayList();
		for (Long2ObjectMap.Entry<List<Runnable>> entry : serverTasks.long2ObjectEntrySet()) {
			if (entry.getLongKey() <= currentTick) {
				for (Runnable task : entry.getValue()) {
					task.run();
				}
				dueKeys.add(entry.getLongKey());
			}
		}
		for (long key : dueKeys) {
			serverTasks.remove(key);
		}
	}
}

