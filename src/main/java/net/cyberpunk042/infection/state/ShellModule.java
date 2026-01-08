package net.cyberpunk042.infection.state;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.service.ShellMaintenanceService;
import net.cyberpunk042.infection.service.ShellRebuildService;
import net.cyberpunk042.infection.service.ShieldFieldService;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import java.util.List;

/**
 * Facade module consolidating all shell-related services.
 * Manages shell rebuilding, collapsing, and maintenance.
 */
public final class ShellModule {

	private final ShellRebuildService rebuildService = new ShellRebuildService();
	private final ShellRebuildService.State rebuildState = new ShellRebuildService.State();
	private final ShellMaintenanceService maintenanceService;
	private final ShellRebuildService.Callbacks callbacks;

	public ShellModule(VirusWorldState host, ShieldFieldService shieldFieldService) {
		this.maintenanceService = new ShellMaintenanceService(host, rebuildService, rebuildState, shieldFieldService);
		this.callbacks = maintenanceService.createCallbacks(host);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Core service access (for other services that need direct references)
	// ─────────────────────────────────────────────────────────────────────────────

	public ShellRebuildService rebuildService() {
		return rebuildService;
	}

	public ShellRebuildService.State rebuildState() {
		return rebuildState;
	}

	public ShellMaintenanceService maintenance() {
		return maintenanceService;
	}

	public ShellRebuildService.Callbacks callbacks() {
		return callbacks;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Convenience methods
	// ─────────────────────────────────────────────────────────────────────────────

	public boolean isCollapsed() {
		return rebuildService.shellsCollapsed(rebuildState);
	}

	public void setCollapsed(boolean collapsed) {
		maintenanceService.setShellsCollapsed(collapsed);
	}

	public void setRebuildPending(boolean pending) {
		maintenanceService.setShellRebuildPending(pending);
	}

	public void clearCooldowns() {
		maintenanceService.clearShellCooldowns();
	}

	public void collapse(ServerWorld world, Iterable<BlockPos> virusSources) {
		rebuildService.collapseShells(world, rebuildState, virusSources, callbacks);
	}

	public void purgeHostilesAround(ServerWorld world, List<BlockPos> centers, double radius) {
		maintenanceService.purgeHostilesAround(world, centers, radius);
	}
}

