package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import net.cyberpunk042.infection.GlobalTerrainCorruption;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.SingularityState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.cyberpunk042.command.util.CommandFormatters;
import net.cyberpunk042.command.util.CommandProtection;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class VirusStatsCommand {
	private VirusStatsCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("virusstats")
				.executes(ctx -> report(ctx.getSource())));
	}

	private static int report(ServerCommandSource source) {
		// Stats are info-only but can be protected if desired
		if (!CommandProtection.checkAndWarn(source, "stats.view")) {
			return 0;
		}
		ServerWorld world = source.getWorld();
		VirusWorldState state = VirusWorldState.get(world);

		source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.header").formatted(Formatting.AQUA), false);
		source.sendFeedback(() -> Text.translatable(
				"command.the-virus-block.stats.mod_time",
				Text.literal(CommandFormatters.formatDurationTicks(world.getTime()))
		).formatted(Formatting.GRAY), false);

		if (!state.infectionState().infected()) {
			source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.infection_idle").formatted(Formatting.RED), false);
			return 1;
		}

		source.sendFeedback(() -> Text.translatable(
				"command.the-virus-block.stats.infection_time",
				Text.literal(CommandFormatters.formatDurationTicks(state.infectionState().totalTicks()))
		).formatted(Formatting.GRAY), false);

		long ticksUntilFinalWave = state.tiers().ticksUntilFinalWave();
		if (ticksUntilFinalWave <= 0L) {
			source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.final_wave_now").formatted(Formatting.DARK_RED), false);
		} else {
			source.sendFeedback(() -> Text.translatable(
					"command.the-virus-block.stats.final_wave_eta",
					Text.literal(CommandFormatters.formatDurationTicks(ticksUntilFinalWave))
			).formatted(Formatting.GOLD), false);
		}

		int trackedChunks = GlobalTerrainCorruption.getTrackedChunkCount(world);
		source.sendFeedback(() -> Text.translatable(
				"command.the-virus-block.stats.chunks_alive",
				trackedChunks
		).formatted(Formatting.DARK_GREEN), false);

		SingularityState singularityState = state.singularityState().singularityState;
		switch (singularityState) {
			case DORMANT -> source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.singularity_dormant").formatted(Formatting.DARK_PURPLE), false);
			case FUSING -> source.sendFeedback(() -> Text.translatable(
					"command.the-virus-block.stats.singularity_fusing",
					Text.literal(CommandFormatters.formatDurationTicks(state.singularityState().singularityTicks))
			).formatted(Formatting.DARK_PURPLE), false);
			case COLLAPSE -> source.sendFeedback(() -> Text.translatable("command.the-virus-block.stats.singularity_collapse").formatted(Formatting.DARK_PURPLE), false);
		}
		return 1;
	}

}

