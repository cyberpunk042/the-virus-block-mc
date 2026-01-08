package net.cyberpunk042.command.field;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorMath;
import net.cyberpunk042.visual.color.ColorTheme;
import net.cyberpunk042.visual.color.ColorThemeRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * /field theme subcommands
 */
public final class ThemeSubcommand {
    
    private ThemeSubcommand() {}
    
    private static final SuggestionProvider<ServerCommandSource> THEME_SUGGESTIONS =
        (ctx, builder) -> {
            for (Identifier id : ColorThemeRegistry.ids()) {
                builder.suggest(id.getPath());
            }
            return builder.buildFuture();
        };
    
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("theme")
            .then(CommandManager.literal("list")
                .executes(ctx -> handleList(ctx.getSource())))
            .then(CommandManager.literal("show")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .suggests(THEME_SUGGESTIONS)
                    .executes(ctx -> handleShow(ctx.getSource(), 
                        StringArgumentType.getString(ctx, "name")))))
            .then(CommandManager.literal("create")
                .then(CommandManager.argument("name", StringArgumentType.word())
                    .then(CommandManager.argument("baseColor", StringArgumentType.word())
                        .executes(ctx -> handleCreate(ctx.getSource(),
                            StringArgumentType.getString(ctx, "name"),
                            StringArgumentType.getString(ctx, "baseColor"))))));
    }
    
    private static int handleList(ServerCommandSource source) {
        var ids = ColorThemeRegistry.ids();
        if (ids.isEmpty()) {
            CommandFeedback.info(source, "No themes registered.");
            return 0;
        }
        
        CommandFeedback.info(source, "Color themes (" + ids.size() + "):");
        for (Identifier id : ids) {
            ColorTheme theme = ColorThemeRegistry.get(id);
            if (theme != null) {
                String hex = String.format("#%06X", theme.getPrimary() & 0xFFFFFF);
                source.sendFeedback(() -> Text.literal("  " + id.getPath() + " [" + hex + "]"), false);
            }
        }
        return ids.size();
    }
    
    private static int handleShow(ServerCommandSource source, String name) {
        ColorTheme theme = ColorThemeRegistry.get(name);
        if (theme == null) {
            CommandFeedback.error(source, "Unknown theme: " + name);
            return 0;
        }
        
        CommandFeedback.info(source, "Theme: " + name);
        showColor(source, "primary", theme.getPrimary());
        showColor(source, "secondary", theme.getSecondary());
        showColor(source, "glow", theme.getGlow());
        showColor(source, "beam", theme.getBeam());
        showColor(source, "wire", theme.getWire());
        showColor(source, "accent", theme.getAccent());
        return 1;
    }
    
    private static void showColor(ServerCommandSource source, String name, int color) {
        String hex = String.format("#%06X", color & 0xFFFFFF);
        source.sendFeedback(() -> Text.literal("  " + name + ": " + hex), false);
    }
    
    private static int handleCreate(ServerCommandSource source, String name, String baseColor) {
        int color = ColorMath.parseHex(baseColor);
        if (color == 0) {
            Logging.COMMANDS.topic("theme").warn(
                "Player {} attempted to create theme with invalid color: {}", 
                source.getName(), baseColor);
            CommandFeedback.error(source, "Invalid color: " + baseColor);
            return 0;
        }
        
        ColorTheme theme = ColorThemeRegistry.derive(name, color);
        Logging.COMMANDS.topic("theme").info(
            "Player {} created theme '{}' with base color {}", 
            source.getName(), name, baseColor);
        CommandFeedback.success(source, "Created theme: " + name);
        return 1;
    }
}
