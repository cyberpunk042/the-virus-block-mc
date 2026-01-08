package net.cyberpunk042.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.entity.VirusFuseEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Main /virusblock command - uses CommandKnob consistently for all operations.
 * Note: Log commands are now handled by LogCommands (/virus logs).
 */
public final class VirusCommand {
    
    private VirusCommand() {}
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("virusblock")
            .requires(source -> source.hasPermissionLevel(2))
            .then(buildTeleport())
            .then(SingularitySubcommands.build())
            .then(InfectionSubcommands.build())
            .then(buildFuse()));
        
        // Register sub-command classes
        VirusDifficultyCommand.register(dispatcher);
        VirusStatsCommand.register(dispatcher);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Teleport subcommand - uses CommandKnob
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildTeleport() {
        var cmd = CommandManager.literal("teleport")
            .then(CommandManager.literal("status")
                .executes(ctx -> reportTeleport(ctx.getSource())));
        
        // Teleport toggle
        CommandKnob.toggle("teleport.enabled", "Virus block teleportation")
            .defaultValue(true)
            .handler((src, enabled) -> {
                src.getServer().getGameRules().get(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED)
                    .set(enabled, src.getServer());
                return true;
            })
            .attach(cmd);
        
        // Teleport radius
        CommandKnob.value("teleport.radius", "Virus block teleport radius")
            .range(0, 64)
            .unit("chunks")
            .defaultValue(8)
            .handler((src, chunks) -> {
                src.getServer().getGameRules().get(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS)
                    .set(chunks, src.getServer());
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static int reportTeleport(ServerCommandSource source) {
        boolean enabled = source.getServer().getGameRules().getBoolean(TheVirusBlock.VIRUS_BLOCK_TELEPORT_ENABLED);
        int radius = source.getServer().getGameRules().getInt(TheVirusBlock.VIRUS_BLOCK_TELEPORT_RADIUS);
        CommandFeedback.info(source, "Virus block teleportation is " + (enabled ? "enabled" : "disabled")
            + " (radius " + radius + " chunks)");
        return enabled ? 1 : 0;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Fuse subcommand - uses CommandKnob
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildFuse() {
        var cmd = CommandManager.literal("fuse");
        
        // Fuse test (protected spawn action)
        CommandKnob.action("fuse.test", "Spawn fuse test entities")
            .handler(VirusCommand::spawnFuseTestAction)
            .attach(cmd);
        
        return cmd;
    }
    
    private static boolean spawnFuseTestAction(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        Vec3d pos = source.getPosition();
        Vec3d left = pos.add(-1.5D, 0.0D, 0.0D);
        Vec3d right = pos.add(1.5D, 0.0D, 0.0D);
        
        TntEntity vanilla = new TntEntity(world, left.x, left.y, left.z, null);
        vanilla.setFuse(80);
        world.spawnEntity(vanilla);
        
        BlockPos fusePos = BlockPos.ofFloored(right);
        VirusFuseEntity virusFuse = new VirusFuseEntity(world, fusePos);
        virusFuse.setFuse(200);
        if (!world.spawnEntity(virusFuse)) {
            CommandFeedback.error(source, "Failed to spawn Virus Fuse entity.");
            return false;
        }
        
        source.sendFeedback(() -> Text.literal("Spawned vanilla TNT fuse (left) and Virus fuse (right)."), true);
        return true;
    }
}
