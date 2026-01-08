package net.cyberpunk042.util;

import java.util.Optional;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Lightweight holder for the active {@link MinecraftServer}. Scheduler tasks
 * and other async helpers can use this to resolve a world outside of the main
 * thread bootstrap sequence.
 */
public final class ServerRef {
	private static volatile MinecraftServer server;

	private ServerRef() {
	}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			if (server == s) {
				server = null;
			}
		});
	}

	public static Optional<MinecraftServer> current() {
		return Optional.ofNullable(server);
	}
}

