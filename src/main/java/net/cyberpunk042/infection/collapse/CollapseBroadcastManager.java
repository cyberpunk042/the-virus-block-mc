package net.cyberpunk042.infection.collapse;

import net.minecraft.server.world.ServerWorld;
import net.cyberpunk042.infection.service.SingularityHudService.BorderSyncData;

/**
 * Facade around collapse chunk buffering, border deployment, and per-player
 * streaming decisions. Controllers interact with this interface so the host
 * world no longer micromanages packet scheduling directly.
 */
public interface CollapseBroadcastManager {
	void tick(ServerWorld world);

	void flush(ServerWorld world, boolean force);

	void deployBorder(ServerWorld world);

	void syncBorder(ServerWorld world, BorderSyncData data);

	/** Returns a no-op implementation that does nothing. */
	static CollapseBroadcastManager noop() {
		return NoopCollapseBroadcastManager.INSTANCE;
	}
}

/** No-op implementation for when no broadcast manager is installed. */
enum NoopCollapseBroadcastManager implements CollapseBroadcastManager {
	INSTANCE;

	@Override public void tick(ServerWorld world) {}
	@Override public void flush(ServerWorld world, boolean force) {}
	@Override public void deployBorder(ServerWorld world) {}
	@Override public void syncBorder(ServerWorld world, BorderSyncData data) {}
}

