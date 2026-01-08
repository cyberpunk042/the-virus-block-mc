package net.cyberpunk042.infection.service;

import java.util.List;
import java.util.Objects;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Encapsulates shell maintenance tasks (cooldowns, collapse flags, and purges).
 */
public final class ShellMaintenanceService {

	private final ShellRebuildService shellService;
	private final ShellRebuildService.State shellState;
	private final ShieldFieldService shieldFieldService;

	public ShellMaintenanceService(VirusWorldState host,
			ShellRebuildService shellService,
			ShellRebuildService.State shellState,
			ShieldFieldService shieldFieldService) {
		this.shellService = Objects.requireNonNull(shellService, "shellService");
		this.shellState = Objects.requireNonNull(shellState, "shellState");
		this.shieldFieldService = Objects.requireNonNull(shieldFieldService, "shieldFieldService");
	}

	public void purgeHostilesAround(ServerWorld world, List<BlockPos> centers, double radius) {
		shieldFieldService.purgeHostilesAround(centers, radius);
	}

	public void clearShellCooldowns() {
		shellService.clearCooldowns(shellState);
	}

	public void setShellsCollapsed(boolean collapsed) {
		shellService.setShellsCollapsed(shellState, collapsed);
	}

	public void setShellRebuildPending(boolean pending) {
		shellService.setShellRebuildPending(shellState, pending);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Snapshot application (called via VirusWorldPersistence)
	// ─────────────────────────────────────────────────────────────────────────────

	public void applyCollapsedFromSnapshot(boolean collapsed) {
		shellService.setShellsCollapsed(shellState, collapsed);
	}

	public void applyRebuildPendingFromSnapshot(boolean pending) {
		shellService.setShellRebuildPending(shellState, pending);
	}

	/**
	 * Creates shell rebuild callbacks that bridge to the virus world state's services.
	 */
	public ShellRebuildService.Callbacks createCallbacks(VirusWorldState host) {
		return new ShellRebuildService.Callbacks() {
			@Override
			public boolean isVirusCoreBlock(BlockPos pos, BlockState state) {
				return host.singularity().fusing().isVirusCoreBlock(pos, state);
			}

			@Override
			public boolean shouldPushDuringShell() {
				InfectionTier tier = host.tiers().currentTier();
				return tier.getIndex() < 3 || host.tiers().ticksInTier() < tier.getDurationTicks() / 2;
			}

			@Override
			public void pushPlayers(ServerWorld world, BlockPos pos, int radius) {
				host.presentationCoord().pushPlayersFromBlock(pos, radius, 1.0D, true);
			}

			@Override
			public void onShellRebuild(ServerWorld world) {
				host.infection().broadcast(world, Text.translatable("message.the-virus-block.shells_reforming").formatted(Formatting.DARK_AQUA));
				host.markDirty();
			}

			@Override
			public void onShellsCollapsed(ServerWorld world) {
				world.getPlayers(PlayerEntity::isAlive).forEach(player ->
						player.sendMessage(Text.translatable("message.the-virus-block.shells_collapsed").formatted(Formatting.RED), false));
				host.markDirty();
			}
		};
	}
}

