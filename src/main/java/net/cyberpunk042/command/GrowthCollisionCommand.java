package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.cyberpunk042.block.entity.GrowthCollisionTracker;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionDebug.ShapeMode;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

/**
 * Lightweight runtime controls for growth-collision debugging.
 * Uses CommandKnob for shape mode configuration.
 */
public final class GrowthCollisionCommand {
    private GrowthCollisionCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> 
            registerInternal(dispatcher));
    }

    private static void registerInternal(CommandDispatcher<ServerCommandSource> dispatcher) {
        var cmd = CommandManager.literal("growthcollision")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("dump")
                .executes(GrowthCollisionCommand::dumpActiveBlocks));
        
        // Shape mode enum knob
        CommandKnob.enumValue("collision.shape_mode", "Collision shape mode", ShapeMode.class)
            .idMapper(mode -> mode.name().toLowerCase())
            .parser(GrowthCollisionCommand::parseShapeMode)
            .defaultValue(ShapeMode.SOLID)
            .handler((src, mode) -> {
                boolean changed = GrowthCollisionDebug.setShapeMode(mode);
                if (changed) {
                    GrowthCollisionTracker.forEachActive(ProgressiveGrowthBlockEntity::forceRebuildCollisionShape);
                }
                return true;
            })
            .attach(cmd);
        
        // Status subcommand
        cmd.then(CommandManager.literal("status")
            .executes(GrowthCollisionCommand::describeShapeMode));
        
        dispatcher.register(cmd);
    }
    
    private static ShapeMode parseShapeMode(String raw) {
        if (raw == null) return ShapeMode.SOLID;
        try {
            return ShapeMode.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ShapeMode.SOLID;
        }
    }

    private static int describeShapeMode(CommandContext<ServerCommandSource> ctx) {
        CommandFeedback.info(ctx.getSource(), "Current growth collision shape mode: "
            + GrowthCollisionDebug.getShapeMode().name().toLowerCase() + ".");
        return 1;
    }

    private static int dumpActiveBlocks(CommandContext<ServerCommandSource> ctx) {
        final int[] count = {0};
        GrowthCollisionTracker.forEachActive(block -> {
            count[0]++;
            ctx.getSource().sendFeedback(
                () -> Text.literal("Growth block at " + block.getPos() + " world=" + block.getWorld().getRegistryKey().getValue()
                    + " collisionMode=" + GrowthCollisionDebug.getShapeMode().name().toLowerCase()),
                false);
            VoxelShape worldShape = block.worldShape(ProgressiveGrowthBlock.ShapeType.COLLISION);
            if (worldShape != null && !worldShape.isEmpty()) {
                int idx = 0;
                for (Box box : worldShape.getBoundingBoxes()) {
                    int boxIndex = idx++;
                    ctx.getSource().sendFeedback(
                        () -> Text.literal("  box#" + boxIndex + " = " + box),
                        false);
                    if (idx >= 6) break;
                }
            }
            block.forceRebuildCollisionShape();
        });
        if (count[0] == 0) {
            CommandFeedback.info(ctx.getSource(), "No active growth collision blocks registered.");
        }
        return count[0];
    }
}
