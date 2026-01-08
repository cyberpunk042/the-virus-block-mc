package net.cyberpunk042.infection.service;

import java.util.List;
import java.util.Objects;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

/**
 * Handles teleport/containment logic for virus sources plus guardian spawns.
 */
public final class SourceControlService {

	private final VirusWorldState host;
	private final ShellRebuildService shellService;
	private final ShellRebuildService.State shellState;
	private final VirusSourceService sourceService;
	private final VirusSourceService.State sourceState;

	public SourceControlService(VirusWorldState host,
			ShellRebuildService shellService,
			ShellRebuildService.State shellState,
			VirusSourceService sourceService,
			VirusSourceService.State sourceState) {
		this.host = Objects.requireNonNull(host, "host");
		this.shellService = Objects.requireNonNull(shellService, "shellService");
		this.shellState = Objects.requireNonNull(shellState, "shellState");
		this.sourceService = Objects.requireNonNull(sourceService, "sourceService");
		this.sourceState = Objects.requireNonNull(sourceState, "sourceState");
	}

	public boolean maybeTeleportSources() {
		ServerWorld world = host.world();
		GameRules rules = world.getGameRules();
		if (!rules.getBoolean(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED)) {
			return false;
		}
		int chunkRadius = rules.getInt(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS);
		if (chunkRadius <= 0) {
			return false;
		}
		boolean moved = sourceService.teleportSources(sourceState, chunkRadius, host.tiers().difficulty().getTeleportChance());
		if (moved) {
			shellService.clearCooldowns(shellState);
			world.getPlayers(ServerPlayerEntity::isAlive)
					.forEach(player -> player.sendMessage(
							Text.translatable("message.the-virus-block.teleport").formatted(Formatting.DARK_PURPLE),
							false));
			host.markDirty();
		}
		return moved;
	}

	public void spawnCoreGuardians() {
		ServerWorld world = host.world();
		if (!host.hasVirusSources()) {
			return;
		}
		host.combat().guardianSpawnService().spawnCoreGuardians(host.world(), List.copyOf(host.getVirusSources()));
	}

	public void forceContainmentReset() {
		ServerWorld world = host.world();
		sourceService.forceContainmentReset(sourceState);
		// End the infection when health is depleted (triggers cleanse)
		host.infectionLifecycle().endInfection();
	}

	public void endInfection() {
		ServerWorld world = host.world();
		host.infectionLifecycle().endInfection();
	}
}

