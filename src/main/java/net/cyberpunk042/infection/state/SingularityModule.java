package net.cyberpunk042.infection.state;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.service.ChunkPreparationService;
import net.cyberpunk042.infection.service.CollapseProcessor;
import net.cyberpunk042.infection.service.SingularityBarrierService;
import net.cyberpunk042.infection.service.SingularityBorderService;
import net.cyberpunk042.infection.service.SingularityFusingService;
import net.cyberpunk042.infection.service.SingularityLifecycleService;
import net.cyberpunk042.infection.service.SingularityPhaseService;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Facade module consolidating all singularity-related services.
 * Provides access to border, lifecycle, phase, fusing, and collapse processor services.
 */
public final class SingularityModule {

	private final VirusWorldState host;

	// Border management
	private final SingularityBorderService borderService = new SingularityBorderService();
	private final SingularityBorderService.State borderState = new SingularityBorderService.State();

	// Lifecycle and phase management
	private final SingularityLifecycleService lifecycleService;
	private final SingularityLifecycleService.State lifecycleState;
	private final SingularityPhaseService phaseService;

	// Fusing
	private final SingularityFusingService fusingService;

	// Barrier (player push field)
	private final SingularityBarrierService barrierService;
	private final SingularityBarrierService.State barrierState;

	// Chunk preparation
	private final ChunkPreparationService chunkPreparationService;
	private final ChunkPreparationService.State chunkPreparationState;

	// Collapse processor (simple radius-based fill)
	private final CollapseProcessor collapseProcessor;
	private final CollapseProcessor.State collapseProcessorState;

	public SingularityModule(VirusWorldState host) {
		this.host = host;
		this.lifecycleState = new SingularityLifecycleService.State();
		this.lifecycleService = new SingularityLifecycleService(host, lifecycleState);
		this.phaseService = new SingularityPhaseService(host, lifecycleService);
		this.fusingService = new SingularityFusingService(host);
		this.barrierState = new SingularityBarrierService.State();
		this.barrierService = new SingularityBarrierService(host, barrierState);
		this.chunkPreparationState = new ChunkPreparationService.State();
		this.chunkPreparationService = new ChunkPreparationService(host, chunkPreparationState);
		this.collapseProcessorState = new CollapseProcessor.State();
		this.collapseProcessor = new CollapseProcessor(host, collapseProcessorState);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Border
	// ─────────────────────────────────────────────────────────────────────────────

	public SingularityBorderService border() {
		return borderService;
	}

	public SingularityBorderService.State borderState() {
		return borderState;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Lifecycle & State
	// ─────────────────────────────────────────────────────────────────────────────

	public SingularityLifecycleService lifecycle() {
		return lifecycleService;
	}

	public SingularityLifecycleService.State state() {
		return lifecycleState;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Phase
	// ─────────────────────────────────────────────────────────────────────────────

	public SingularityPhaseService phase() {
		return phaseService;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Fusing
	// ─────────────────────────────────────────────────────────────────────────────

	public SingularityFusingService fusing() {
		return fusingService;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Barrier (player push field)
	// ─────────────────────────────────────────────────────────────────────────────

	public SingularityBarrierService barrier() {
		return barrierService;
	}

	public SingularityBarrierService.State barrierState() {
		return barrierState;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Chunk Preparation
	// ─────────────────────────────────────────────────────────────────────────────

	public ChunkPreparationService chunkPreparation() {
		return chunkPreparationService;
	}

	public ChunkPreparationService.State chunkPreparationState() {
		return chunkPreparationState;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Collapse Processor (simple radius-based fill)
	// ─────────────────────────────────────────────────────────────────────────────

	public CollapseProcessor collapseProcessor() {
		return collapseProcessor;
	}

	public CollapseProcessor.State collapseProcessorState() {
		return collapseProcessorState;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Center management
	// ─────────────────────────────────────────────────────────────────────────────

	public void ensureCenter(ServerWorld world) {
		if (lifecycleState.center != null) {
			return;
		}
		BlockPos candidate = host.infection().representativePos(world, world.getRandom(), host.sourceState());
		if (candidate == null && host.hasVirusSources()) {
			candidate = host.getVirusSources().iterator().next();
		}
		if (candidate == null) {
			candidate = world.getSpawnPos();
		}
		if (candidate != null) {
			lifecycleState.center = candidate.toImmutable();
		}
	}
}
