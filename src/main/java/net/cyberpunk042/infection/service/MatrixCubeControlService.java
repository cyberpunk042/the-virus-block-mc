package net.cyberpunk042.infection.service;

import java.util.Objects;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.server.world.ServerWorld;

/**
 * Centralizes matrix cube spawn logic and cleanup hooks.
 */
public final class MatrixCubeControlService {

	private final VirusWorldState host;
	private final MatrixCubeSpawnService spawnService;

	public MatrixCubeControlService(VirusWorldState host, MatrixCubeSpawnService spawnService) {
		this.host = Objects.requireNonNull(host, "host");
		this.spawnService = Objects.requireNonNull(spawnService, "spawnService");
	}

	public boolean maybeSpawnMatrixCube() {
		ServerWorld world = host.world();
		if (!host.infectionState().infected()) {
			MatrixCubeBlockEntity.trimActive(world, 0);
			return false;
		}
		return spawnService.maybeSpawnMatrixCube(world, host.singularity().fusing().matrixCubeSingularityFactor(), host.singularity().fusing().singularityActivityMultiplier());
	}

	public void destroyAll() {
		ServerWorld world = host.world();
		MatrixCubeBlockEntity.destroyAll(world);
	}
}

