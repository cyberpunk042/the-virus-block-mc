package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.screen.handler.VirusDifficultyScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Virus difficulty command - uses CommandKnob for difficulty selection.
 */
public final class VirusDifficultyCommand {

    private VirusDifficultyCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var cmd = CommandManager.literal("virusdifficulty")
            .executes(ctx -> openDifficultyMenu(ctx.getSource()));
        
        // Difficulty enum knob
        CommandKnob.enumValue("difficulty.level", "Virus difficulty", VirusDifficulty.class)
            .idMapper(VirusDifficulty::getId)
            .parser(VirusDifficultyCommand::parseDifficulty)
            .defaultValue(VirusDifficulty.MEDIUM)
            .handler((src, difficulty) -> {
                ServerWorld world = src.getWorld();
                boolean locked = world.getGameRules().getBoolean(TheVirusBlock.VIRUS_DIFFICULTY_LOCKED);
                if (locked && !src.hasPermissionLevel(2)) {
                    CommandFeedback.error(src, "Difficulty is locked by server.");
                    return false;
                }
                VirusWorldState state = VirusWorldState.get(world);
                state.setDifficulty(world, difficulty);
                return true;
            })
            .attach(cmd);
        
        dispatcher.register(cmd);
    }
    
    private static VirusDifficulty parseDifficulty(String value) {
        if (value == null) return VirusDifficulty.MEDIUM;
        for (VirusDifficulty diff : VirusDifficulty.values()) {
            if (diff.getId().equalsIgnoreCase(value)) {
                return diff;
            }
        }
        return VirusDifficulty.MEDIUM;
    }

    private static int openDifficultyMenu(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        return openMenuFor(player) ? 1 : 0;
    }

    public static boolean openMenuFor(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        boolean locked = world.getGameRules().getBoolean(TheVirusBlock.VIRUS_DIFFICULTY_LOCKED);
        if (locked && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.translatable("message.the-virus-block.difficulty.locked"), true);
            return false;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, inventory, playerEntity) -> new VirusDifficultyScreenHandler(syncId, inventory),
            Text.translatable("screen.the-virus-block.difficulty.title")));
        return true;
    }
}
