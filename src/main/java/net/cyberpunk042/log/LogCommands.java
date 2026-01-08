package net.cyberpunk042.log;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Commands for managing the logging system at runtime.
 */
public class LogCommands {
    
    private static final SuggestionProvider<ServerCommandSource> CHANNEL_SUGGESTIONS = (ctx, builder) -> {
        Logging.channels().stream().map(Channel::id).forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    private static final SuggestionProvider<ServerCommandSource> LEVEL_SUGGESTIONS = (ctx, builder) -> {
        Stream.of(LogLevel.values()).map(Enum::name).forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("virus").then(literal("logs")
                .executes(LogCommands::dashboard)
                .then(literal("list").executes(LogCommands::list))
                .then(literal("set")
                    .then(argument("channel", StringArgumentType.word())
                        .suggests(CHANNEL_SUGGESTIONS)
                        .then(argument("level", StringArgumentType.word())
                            .suggests(LEVEL_SUGGESTIONS)
                            .executes(LogCommands::setLevel))))
                .then(literal("chat")
                    .then(literal("list").executes(LogCommands::chatList))
                    .then(argument("channel", StringArgumentType.word())
                        .suggests(CHANNEL_SUGGESTIONS)
                        .then(literal("on").executes(ctx -> setChatForward(ctx, true)))
                        .then(literal("off").executes(ctx -> setChatForward(ctx, false)))))
                .then(literal("watchdog")
                    .then(literal("on").executes(ctx -> setWatchdog(ctx, true)))
                    .then(literal("off").executes(ctx -> setWatchdog(ctx, false)))
                    .then(argument("perSec", IntegerArgumentType.integer(1))
                        .then(argument("perMin", IntegerArgumentType.integer(1))
                            .executes(LogCommands::setWatchdogThresholds))))
                // Override commands
                .then(literal("override")
                    .executes(LogCommands::overrideStatus)
                    .then(literal("mute").executes(LogCommands::overrideMute))
                    .then(literal("unmute").executes(LogCommands::overrideUnmute))
                    .then(literal("min")
                        .then(argument("level", StringArgumentType.word())
                            .suggests(LEVEL_SUGGESTIONS)
                            .executes(LogCommands::overrideMinLevel)))
                    .then(literal("clearmin").executes(LogCommands::overrideClearMin))
                    .then(literal("pass")
                        .then(argument("channel", StringArgumentType.word())
                            .suggests(CHANNEL_SUGGESTIONS)
                            .executes(LogCommands::overridePassthrough)))
                    .then(literal("unpass")
                        .then(argument("channel", StringArgumentType.word())
                            .suggests(CHANNEL_SUGGESTIONS)
                            .executes(LogCommands::overrideUnpassthrough)))
                    .then(literal("force")
                        .then(argument("channel", StringArgumentType.word())
                            .suggests(CHANNEL_SUGGESTIONS)
                            .executes(LogCommands::overrideForce)))
                    .then(literal("unforce")
                        .then(argument("channel", StringArgumentType.word())
                            .suggests(CHANNEL_SUGGESTIONS)
                            .executes(LogCommands::overrideUnforce)))
                    .then(literal("redirect")
                        .then(argument("from", StringArgumentType.word())
                            .suggests(LEVEL_SUGGESTIONS)
                            .then(argument("to", StringArgumentType.word())
                                .suggests(LEVEL_SUGGESTIONS)
                                .executes(LogCommands::overrideRedirect))))
                    .then(literal("clear").executes(LogCommands::overrideClear)))
                .then(literal("reload").executes(LogCommands::reload))
                .then(literal("reset").executes(LogCommands::reset))
                .then(literal("save").executes(LogCommands::save))
            )
        );
    }
    
    private static int dashboard(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6=== Logging Dashboard ==="), false);
        src.sendFeedback(() -> Text.literal("§7Channels: §f" + Logging.channels().size()), false);
        src.sendFeedback(() -> Text.literal("§7Watchdog: §f" + (LogWatchdog.isEnabled() ? "ON" : "OFF")), false);
        src.sendFeedback(() -> Text.literal("§7Chat: §f" + (LogConfig.chatEnabled() ? "ON" : "OFF")), false);
        src.sendFeedback(() -> Text.literal("§7Use §e/virus logs list§7 to see all channels"), false);
        return 1;
    }
    
    private static int list(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6=== Log Channels ==="), false);
        for (Channel ch : Logging.channels()) {
            String level = ch.level().name();
            String chat = ch.chatForward() ? " §a[chat]" : "";
            src.sendFeedback(() -> Text.literal("§7" + ch.id() + ": §f" + level + chat), false);
        }
        return 1;
    }
    
    private static int setLevel(CommandContext<ServerCommandSource> ctx) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        String levelStr = StringArgumentType.getString(ctx, "level");
        
        Channel ch = Logging.channel(channelId);
        if (ch == null) {
            ctx.getSource().sendError(Text.literal("Unknown channel: " + channelId));
            return 0;
        }
        
        LogLevel level = LogLevel.parse(levelStr);
        ch.setLevel(level);
        ctx.getSource().sendFeedback(() -> Text.literal("§aSet §f" + channelId + "§a to §f" + level.name()), false);
        return 1;
    }
    
    private static int chatList(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6=== Chat Forwarding ==="), false);
        for (Channel ch : Logging.channels()) {
            if (ch.chatForward()) {
                src.sendFeedback(() -> Text.literal("§a✓ §f" + ch.id()), false);
            }
        }
        return 1;
    }
    
    private static int setChatForward(CommandContext<ServerCommandSource> ctx, boolean value) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        Channel ch = Logging.channel(channelId);
        if (ch == null) {
            ctx.getSource().sendError(Text.literal("Unknown channel: " + channelId));
            return 0;
        }
        ch.setChatForward(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aChat forwarding for §f" + channelId + "§a: " + (value ? "ON" : "OFF")), false);
        return 1;
    }
    
    private static int setWatchdog(CommandContext<ServerCommandSource> ctx, boolean value) {
        LogWatchdog.setEnabled(value);
        ctx.getSource().sendFeedback(() -> Text.literal("§aWatchdog: " + (value ? "ON" : "OFF")), false);
        return 1;
    }
    
    private static int setWatchdogThresholds(CommandContext<ServerCommandSource> ctx) {
        int perSec = IntegerArgumentType.getInteger(ctx, "perSec");
        int perMin = IntegerArgumentType.getInteger(ctx, "perMin");
        LogWatchdog.setThresholds(perSec, perMin);
        ctx.getSource().sendFeedback(() -> Text.literal("§aWatchdog thresholds: §f" + perSec + "/s, " + perMin + "/m"), false);
        return 1;
    }
    
    private static int reload(CommandContext<ServerCommandSource> ctx) {
        LogConfig.load();
        ctx.getSource().sendFeedback(() -> Text.literal("§aReloaded logging configuration"), false);
        return 1;
    }
    
    private static int reset(CommandContext<ServerCommandSource> ctx) {
        Logging.reset();
        ctx.getSource().sendFeedback(() -> Text.literal("§aReset all channels to defaults"), false);
        return 1;
    }
    
    private static int save(CommandContext<ServerCommandSource> ctx) {
        LogConfig.save();
        ctx.getSource().sendFeedback(() -> Text.literal("§aSaved logging configuration"), false);
        return 1;
    }
    
    // ========== OVERRIDE COMMANDS ==========
    
    private static int overrideStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("§6=== Log Overrides ==="), false);
        
        if (!LogOverride.hasOverrides()) {
            src.sendFeedback(() -> Text.literal("§7No overrides active"), false);
        } else {
            src.sendFeedback(() -> Text.literal("§7" + LogOverride.summary()), false);
        }
        
        src.sendFeedback(() -> Text.literal("§7Commands: mute, unmute, min <level>, clearmin, pass/unpass <ch>, force/unforce <ch>, redirect <from> <to>, clear"), false);
        return 1;
    }
    
    private static int overrideMute(CommandContext<ServerCommandSource> ctx) {
        LogOverride.muteAll();
        ctx.getSource().sendFeedback(() -> Text.literal("§c⊘ All logging MUTED §7(use 'pass <channel>' to allow specific channels)"), false);
        return 1;
    }
    
    private static int overrideUnmute(CommandContext<ServerCommandSource> ctx) {
        LogOverride.unmuteAll();
        ctx.getSource().sendFeedback(() -> Text.literal("§a✓ Logging unmuted"), false);
        return 1;
    }
    
    private static int overrideMinLevel(CommandContext<ServerCommandSource> ctx) {
        String levelStr = StringArgumentType.getString(ctx, "level");
        LogLevel level = LogLevel.parse(levelStr);
        LogOverride.setMinLevel(level);
        ctx.getSource().sendFeedback(() -> Text.literal("§aMinimum level set to §f" + level.name() + " §7(logs below this are hidden)"), false);
        return 1;
    }
    
    private static int overrideClearMin(CommandContext<ServerCommandSource> ctx) {
        LogOverride.clearMinLevel();
        ctx.getSource().sendFeedback(() -> Text.literal("§aMinimum level cleared"), false);
        return 1;
    }
    
    private static int overridePassthrough(CommandContext<ServerCommandSource> ctx) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        LogOverride.addPassthrough(channelId);
        ctx.getSource().sendFeedback(() -> Text.literal("§a+ §f" + channelId + "§a added to passthrough (bypasses mute)"), false);
        return 1;
    }
    
    private static int overrideUnpassthrough(CommandContext<ServerCommandSource> ctx) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        LogOverride.removePassthrough(channelId);
        ctx.getSource().sendFeedback(() -> Text.literal("§c- §f" + channelId + "§c removed from passthrough"), false);
        return 1;
    }
    
    private static int overrideForce(CommandContext<ServerCommandSource> ctx) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        LogOverride.forceOutput(channelId);
        ctx.getSource().sendFeedback(() -> Text.literal("§a! §f" + channelId + "§a forced ON (always outputs)"), false);
        return 1;
    }
    
    private static int overrideUnforce(CommandContext<ServerCommandSource> ctx) {
        String channelId = StringArgumentType.getString(ctx, "channel");
        LogOverride.unforceOutput(channelId);
        ctx.getSource().sendFeedback(() -> Text.literal("§c! §f" + channelId + "§c unforced"), false);
        return 1;
    }
    
    private static int overrideRedirect(CommandContext<ServerCommandSource> ctx) {
        String fromStr = StringArgumentType.getString(ctx, "from");
        String toStr = StringArgumentType.getString(ctx, "to");
        LogLevel from = LogLevel.parse(fromStr);
        LogLevel to = LogLevel.parse(toStr);
        LogOverride.redirect(from, to);
        ctx.getSource().sendFeedback(() -> Text.literal("§a→ §f" + from.name() + "§a redirected to §f" + to.name()), false);
        return 1;
    }
    
    private static int overrideClear(CommandContext<ServerCommandSource> ctx) {
        LogOverride.clearAll();
        ctx.getSource().sendFeedback(() -> Text.literal("§aAll overrides cleared"), false);
        return 1;
    }
}
