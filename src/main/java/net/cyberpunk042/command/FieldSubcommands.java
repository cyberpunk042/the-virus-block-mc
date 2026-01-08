package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldManager;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.field.instance.FieldInstance;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorThemeRegistry;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;

/**
 * Subcommands for field operations.
 */
public final class FieldSubcommands {
    
    private FieldSubcommands() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("field")
            .requires(source -> source.hasPermissionLevel(2));
        
        root.then(CommandManager.literal("list")
            .executes(ctx -> listFields(ctx.getSource())));
        
        root.then(CommandManager.literal("themes")
            .executes(ctx -> listThemes(ctx.getSource())));
        
        root.then(CommandManager.literal("spawn")
            .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                .suggests((ctx, builder) -> {
                    for (String id : FieldRegistry.ids()) {
                        builder.suggest(id.toString());
                    }
                    return builder.buildFuture();
                })
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .executes(ctx -> spawnField(
                        ctx.getSource(),
                        IdentifierArgumentType.getIdentifier(ctx, "id"),
                        BlockPosArgumentType.getBlockPos(ctx, "pos"),
                        1.0f))
                    .then(CommandManager.argument("scale", FloatArgumentType.floatArg(0.1f, 10f))
                        .executes(ctx -> spawnField(
                            ctx.getSource(),
                            IdentifierArgumentType.getIdentifier(ctx, "id"),
                            BlockPosArgumentType.getBlockPos(ctx, "pos"),
                            FloatArgumentType.getFloat(ctx, "scale")))))));
        
        root.then(CommandManager.literal("reload")
            .executes(ctx -> reloadFields(ctx.getSource())));
        
        dispatcher.register(root);
        Logging.COMMANDS.info("Registered /field subcommands");
    }
    
    private static int listFields(ServerCommandSource source) {
        int total = FieldRegistry.count();
        if (total == 0) {
            CommandFeedback.info(source, "No field definitions loaded.");
            return 0;
        }
        
        CommandFeedback.info(source, "Field definitions (" + total + "):");
        for (FieldType type : FieldType.values()) {
            int count = FieldRegistry.count(type);
            if (count > 0) {
                source.sendFeedback(() -> Text.literal("  " + type.id() + ": " + count), false);
            }
        }
        return total;
    }
    
    private static int listThemes(ServerCommandSource source) {
        var ids = ColorThemeRegistry.ids();
        if (ids.isEmpty()) {
            CommandFeedback.info(source, "No themes registered.");
            return 0;
        }
        
        CommandFeedback.info(source, "Color themes (" + ids.size() + "):");
        for (Identifier id : ids) {
            source.sendFeedback(() -> Text.literal("  " + id.getPath()), false);
        }
        return ids.size();
    }
    
    private static int spawnField(ServerCommandSource source, Identifier id, BlockPos pos, float scale) {
        ServerWorld serverWorld = source.getWorld();
        
        FieldDefinition def = FieldRegistry.get(id);
        if (def == null) {
            CommandFeedback.error(source, "Unknown field: " + id);
            return 0;
        }
        
        Vec3d center = Vec3d.ofCenter(pos);
        var instance = FieldManager.get(serverWorld).spawnAt(id, center, scale, -1);
        
        if (instance == null) {
            CommandFeedback.error(source, "Failed to spawn field");
            return 0;
        }
        
        CommandFeedback.success(source, "Spawned " + id.getPath() + " at " + pos.toShortString());
        return 1;
    }
    
    private static int reloadFields(ServerCommandSource source) {
        try {
            FieldRegistry.clear();
            FieldRegistry.registerDefaults();
            ColorThemeRegistry.reload();
            CommandFeedback.success(source, "Reloaded field definitions and themes.");
            return 1;
        } catch (Exception e) {
            CommandFeedback.error(source, "Reload failed: " + e.getMessage());
            return 0;
        }
    }
}
