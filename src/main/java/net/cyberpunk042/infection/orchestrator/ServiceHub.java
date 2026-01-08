package net.cyberpunk042.infection.orchestrator;


import net.cyberpunk042.log.Logging;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.api.VirusScheduler;
import net.cyberpunk042.infection.collapse.CollapseBroadcastManager;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.VirusSchedulerService;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

/**
 * Centralized access to infection services: scheduler, effect bus, and broadcast manager.
 * Extracted from {@link DefaultWorldOrchestrator} to follow Single Responsibility Principle.
 */
public final class ServiceHub {

	private static final org.slf4j.Logger LOGGER = TheVirusBlock.LOGGER;

	private final VirusSchedulerService schedulerService = new VirusSchedulerService();
	private @Nullable EffectBus effectBus;
	private @Nullable CollapseBroadcastManager collapseBroadcastManager;

	// ========== Scheduler ==========

	public VirusSchedulerService schedulerService() {
		return this.schedulerService;
	}

	/** Returns scheduler diagnostics for monitoring. */
	public VirusSchedulerService.SchedulerDiagnostics diagnostics() {
		return this.schedulerService.diagnostics();
	}

	public @Nullable VirusScheduler scheduler() {
		return this.schedulerService.scheduler();
	}

	public VirusScheduler schedulerOrNoop() {
		VirusScheduler s = scheduler();
		return s != null ? s : VirusScheduler.immediate();
	}

	public void installScheduler(@Nullable VirusScheduler scheduler) {
		this.schedulerService.install(scheduler);
	}

	// ========== Effect Bus ==========

	public @Nullable EffectBus effectBus() {
		return this.effectBus;
	}

	public EffectBus effectBusOrNoop() {
		return this.effectBus != null ? this.effectBus : EffectBus.noop();
	}

	public void installEffectBus(@Nullable EffectBus bus) {
		this.effectBus = bus;
	}

	// ========== Collapse Broadcast ==========

	public @Nullable CollapseBroadcastManager collapseBroadcastManager() {
		return this.collapseBroadcastManager;
	}

	public CollapseBroadcastManager collapseBroadcastManagerOrNoop() {
		return this.collapseBroadcastManager != null
			? this.collapseBroadcastManager
			: CollapseBroadcastManager.noop();
	}

	public void installCollapseBroadcastManager(@Nullable CollapseBroadcastManager manager) {
		this.collapseBroadcastManager = manager;
	}

	// ========== Tick ==========

	/**
	 * Ticks scheduler and collapse broadcasts.
	 */
	public void tick(ServerWorld world) {
		long start = System.nanoTime();
		
		try {
			this.schedulerService.tick();
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[ServiceHub] Scheduler tick failed", e);
		}

		try {
			collapseBroadcastManagerOrNoop().tick(world);
		} catch (Exception e) {
			Logging.ORCHESTRATOR.error("[ServiceHub] Collapse broadcast tick failed", e);
		}
		
		long elapsed = (System.nanoTime() - start) / 1_000_000;
		if (elapsed > 10) {
			Logging.ORCHESTRATOR.debug("[ServiceHub] Tick took {}ms", elapsed);
		}
	}

	// ========== Install from Container ==========

	/**
	 * Installs default services from a container.
	 */
	public void installFromContainer(VirusWorldState host, @Nullable InfectionServiceContainer container) {
		if (container == null) {
			Logging.ORCHESTRATOR.debug("[ServiceHub] No container provided - using defaults");
			return;
		}
		EffectBus bus = container.createEffectBus(host);
		if (bus != null) {
			this.effectBus = bus;
		}
		CollapseBroadcastManager broadcast = container.createBroadcastManager(host);
		if (broadcast != null) {
			this.collapseBroadcastManager = broadcast;
		}
	}

	// ========== Shutdown ==========

	/**
	 * Shuts down all services, clearing references.
	 */
	public void shutdown() {
		VirusScheduler scheduler = scheduler();
		if (scheduler != null) {
			scheduler.clear();
		}
		this.effectBus = null;
		this.collapseBroadcastManager = null;
	}
}

