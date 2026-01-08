package net.cyberpunk042.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.command.util.EnumSuggester;
import net.cyberpunk042.command.util.ReportBuilder;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.command.CommandFacade;
import net.cyberpunk042.infection.profile.CollapseBroadcastMode;
import net.cyberpunk042.infection.profile.CollapseFillMode;
import net.cyberpunk042.infection.profile.CollapseFillShape;
import net.cyberpunk042.infection.profile.CollapseSyncProfile;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.infection.profile.WaterDrainMode;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

/**
 * Subcommands for /virusblock singularity - handles collapse, diagnostics, erosion, and profiles.
 * Uses the unified CommandKnob system.
 */
public final class SingularitySubcommands {
    
    private SingularitySubcommands() {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Command Tree Building
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Builds the /virusblock singularity command tree.
     */
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        var cmd = CommandManager.literal("singularity")
            .then(buildStart())
            .then(CommandManager.literal("abort").executes(ctx -> abortSingularity(ctx.getSource())))
            .then(CommandManager.literal("status").executes(ctx -> reportSingularity(ctx.getSource())))
            .then(buildBroadcast())
            .then(buildDiagnostics())
            .then(buildErosion())
            .then(buildProfile());
        
        // Attach top-level knobs
        CommandKnob.toggle("singularity.collapse", "Singularity collapse")
            .defaultValue(true)
            .handler((src, v) -> getFacade(src).setCollapseEnabled(v))
            .attach(cmd);
        
        CommandKnob.toggle("singularity.chunk_generation", "Singularity chunk generation")
            .defaultValue(true)
            .handler((src, v) -> getFacade(src).setChunkGenerationAllowed(v))
            .attach(cmd);
        
        CommandKnob.toggle("singularity.outside_border", "Outside-border loading")
            .defaultValue(false)
            .handler((src, v) -> getFacade(src).setOutsideBorderLoadAllowed(v))
            .attach(cmd);
        
        CommandKnob.value("singularity.view_distance", "Collapse view-distance override")
            .range(0, 32)
            .unit("chunks")
            .defaultValue(0)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setCollapseViewDistance(null, v))
            .attach(cmd);
        
        CommandKnob.value("singularity.simulation_distance", "Collapse simulation-distance override")
            .range(0, 32)
            .unit("chunks")
            .defaultValue(0)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setCollapseSimulationDistance(null, v))
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildStart() {
        return CommandManager.literal("start")
            .executes(ctx -> forceSingularity(ctx.getSource(), 60))
            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 120))
                .executes(ctx -> forceSingularity(ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "seconds"))));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildBroadcast() {
        var cmd = CommandManager.literal("broadcast");
        
        CommandKnob.enumValue("singularity.broadcast.mode", "Collapse broadcast mode", CollapseBroadcastMode.class)
            .idMapper(CollapseBroadcastMode::id)
            .parser(CollapseBroadcastMode::fromId)
            .defaultValue(CollapseBroadcastMode.IMMEDIATE)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setCollapseBroadcastMode(null, v))
            .attach(cmd);
        
        CommandKnob.value("singularity.broadcast.radius", "Collapse broadcast radius")
            .range(0, 512)
            .unit("blocks")
            .defaultValue(256)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setCollapseBroadcastRadius(null, v))
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildDiagnostics() {
        var cmd = CommandManager.literal("diagnostics");
        
        CommandKnob.toggle("diagnostics.enable", "Singularity diagnostics")
            .defaultValue(false)
            .handler((src, v) -> getFacade(src).setDiagnosticsEnabled(v))
            .attach(cmd);
        
        CommandKnob.toggle("diagnostics.chunk_samples", "Collapse chunk sample logging")
            .defaultValue(false)
            .handler((src, v) -> getFacade(src).setDiagnosticsChunkSamples(v))
            .attach(cmd);
        
        CommandKnob.toggle("diagnostics.bypasses", "Collapse bypass logging")
            .defaultValue(false)
            .handler((src, v) -> getFacade(src).setDiagnosticsBypasses(v))
            .attach(cmd);
        
        CommandKnob.value("diagnostics.interval", "Diagnostics sample interval")
            .range(1, 6000)
            .unit("ticks")
            .defaultValue(100)
            .handler((src, v) -> getFacade(src).setDiagnosticsSampleInterval(v))
            .attach(cmd);
        
        // Spam subcommand
        var spamCmd = CommandManager.literal("spam");
        
        CommandKnob.toggle("diagnostics.spam.enable", "Singularity log spam watchdog")
            .defaultValue(false)
            .handler((src, v) -> getFacade(src).setDiagnosticsSpamEnabled(v))
            .attach(spamCmd);
        
        CommandKnob.toggle("diagnostics.spam.suppress", "Spam suppression")
            .defaultValue(true)
            .handler((src, v) -> getFacade(src).setDiagnosticsSpamSuppress(v))
            .attach(spamCmd);
        
        CommandKnob.value("diagnostics.spam.per_second", "Per-second spam threshold")
            .range(0, 1000)
            .defaultValue(100)
            .handler((src, v) -> getFacade(src).setDiagnosticsSpamPerSecond(v))
            .attach(spamCmd);
        
        CommandKnob.value("diagnostics.spam.per_minute", "Per-minute spam threshold")
            .range(0, 60000)
            .defaultValue(1000)
            .handler((src, v) -> getFacade(src).setDiagnosticsSpamPerMinute(v))
            .attach(spamCmd);
        
        cmd.then(spamCmd);
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildErosion() {
        var cmd = CommandManager.literal("erosion")
            .then(CommandManager.literal("status").executes(ctx -> reportErosionStatus(ctx.getSource())));
        
        // Toggles
        CommandKnob.toggle("erosion.particles", "Collapse particles")
            .defaultValue(true)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setCollapseParticles(null, v))
            .attach(cmd);
        
        CommandKnob.toggle("erosion.native_fill", "Native fill")
            .defaultValue(false)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setUseNativeFill(null, v))
            .attach(cmd);
        
        CommandKnob.toggle("erosion.protected_blocks", "Protected-block respect")
            .defaultValue(true)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setRespectProtectedBlocks(null, v))
            .attach(cmd);
        
        // Values
        CommandKnob.value("erosion.water_offset", "Water drain offset")
            .range(0, 16)
            .unit("blocks")
            .defaultValue(0)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setWaterDrainOffset(null, v))
            .attach(cmd);
        
        CommandKnob.value("erosion.outline_thickness", "Outline thickness")
            .range(1, 16)
            .unit("blocks")
            .defaultValue(1)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setOutlineThickness(null, v))
            .attach(cmd);
        
        // Enums
        CommandKnob.enumValue("erosion.water_drain_mode", "Water drain mode", WaterDrainMode.class)
            .idMapper(e -> e.name().toLowerCase(Locale.ROOT))
            .parser(SingularitySubcommands::parseWaterDrainMode)
            .defaultValue(WaterDrainMode.OFF)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setWaterDrainMode(null, v))
            .attach(cmd);
        
        CommandKnob.enumValue("erosion.fill_mode", "Collapse fill mode", CollapseFillMode.class)
            .idMapper(CollapseFillMode::id)
            .parser(CollapseFillMode::fromId)
            .defaultValue(CollapseFillMode.AIR)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setFillMode(null, v))
            .attach(cmd);
        
        CommandKnob.enumValue("erosion.fill_shape", "Collapse fill shape", CollapseFillShape.class)
            .idMapper(CollapseFillShape::id)
            .parser(CollapseFillShape::fromId)
            .defaultValue(CollapseFillShape.COLUMN)
            .scenarioRequired(SingularitySubcommands::checkScenario)
            .handler((src, v) -> getFacade(src).setFillShape(null, v))
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildProfile() {
        return CommandManager.literal("profile")
            .then(CommandManager.literal("set")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("profile", StringArgumentType.word())
                        .suggests(EnumSuggester.of(CollapseSyncProfile.class, CollapseSyncProfile::id))
                        .executes(ctx -> setCollapseProfile(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "profile"))))))
            .then(CommandManager.literal("get")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> reportCollapseProfile(ctx.getSource(),
                        EntityArgumentType.getPlayer(ctx, "player")))))
            .then(CommandManager.literal("default")
                .then(CommandManager.argument("profile", StringArgumentType.word())
                    .suggests(EnumSuggester.of(CollapseSyncProfile.class, CollapseSyncProfile::id))
                    .executes(ctx -> setCollapseProfileDefault(ctx.getSource(),
                        StringArgumentType.getString(ctx, "profile")))));
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Special Handlers (non-declarative)
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static int forceSingularity(ServerCommandSource source, int seconds) {
        ServerWorld world = source.getWorld();
        VirusWorldState state = VirusWorldState.get(world);
        BlockPos fallback = BlockPos.ofFloored(source.getPosition());
        if (source.getEntity() != null) {
            fallback = source.getEntity().getBlockPos();
        }
        if (state.collapse().forceStartSingularity(world, seconds, fallback)) {
            CommandFeedback.successBroadcast(source, "Singularity countdown started (" + seconds + "s).");
            return 1;
        }
        CommandFeedback.error(source, "Unable to start singularity (no Virus Block center available).");
        return 0;
    }
    
    private static int abortSingularity(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        VirusWorldState state = VirusWorldState.get(world);
        if (state.singularity().lifecycle().abortSingularity()) {
            CommandFeedback.successBroadcast(source, "Singularity aborted.");
            return 1;
        }
        CommandFeedback.error(source, "Singularity is not currently running.");
        return 0;
    }
    
    static int reportSingularity(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        VirusWorldState state = VirusWorldState.get(world);
        CommandFeedback.info(source, "Singularity state: " + state.singularityState().singularityState
            + " (ticks=" + state.singularityState().singularityTicks + ")");
        return 1;
    }
    
    private static int reportErosionStatus(ServerCommandSource source) {
        CommandFacade facade = getFacade(source);
        if (!checkScenarioTyped(source, facade)) return 0;
        
        boolean reported = facade.describeErosion(source.getWorld(), collapse -> {
            DimensionProfile.Collapse.WaterDrainDeferred deferred = collapse.waterDrainDeferred();
            DimensionProfile.Collapse.PreCollapseWaterDrainage preDrain = collapse.preCollapseWaterDrainage();
            
            ReportBuilder.create("Erosion Settings")
                .kv("water_drain_mode", collapse.waterDrainMode())
                .kv("water_drain_offset", collapse.waterDrainOffset())
                .section("Water Drain Deferred", s -> s
                    .kv("enabled", deferred.enabled())
                    .kv("initial_delay_ticks", deferred.initialDelayTicks())
                    .kv("columns_per_tick", deferred.columnsPerTick()))
                .section("Pre-Collapse Water Drainage", s -> s
                    .kv("enabled", preDrain.enabled())
                    .kv("mode", preDrain.mode())
                    .kv("tick_rate", preDrain.tickRate())
                    .kv("batch_size", preDrain.batchSize())
                    .kv("start_delay_ticks", preDrain.startDelayTicks())
                    .kv("start_from_center", preDrain.startFromCenter()))
                .kv("collapse_particles", collapse.collapseParticles())
                .kv("fill_mode", collapse.fillMode().id())
                .kv("fill_shape", collapse.fillShape().id())
                .kv("outline_thickness", collapse.outlineThickness())
                .kv("use_native_fill", collapse.useNativeFill())
                .kv("respect_protected_blocks", collapse.respectProtectedBlocks())
                .send(source);
        });
        
        if (!reported) {
            CommandFeedback.error(source, "Failed to read erosion settings; check server logs.");
            return 0;
        }
        return 1;
    }
    
    private static int setCollapseProfile(ServerCommandSource source, ServerPlayerEntity target, String profileId) {
        CollapseSyncProfile profile = CollapseSyncProfile.fromId(profileId);
        if (profile == null) {
            CommandFeedback.error(source, "Unknown sync profile: " + profileId);
            return 0;
        }
        ServerWorld world = target.getWorld();
        VirusWorldState state = VirusWorldState.get(world);
        state.presentationCoord().setCollapseProfile(target, profile);
        CommandFeedback.successBroadcast(source, 
            "Singularity sync profile for " + target.getName().getString() + " set to " + profile.id());
        if (source.getPlayer() != target) {
            target.sendMessage(Text.literal("Your singularity sync profile is now " + profile.id()), false);
        }
        return 1;
    }
    
    private static int reportCollapseProfile(ServerCommandSource source, ServerPlayerEntity target) {
        ServerWorld world = target.getWorld();
        VirusWorldState state = VirusWorldState.get(world);
        CollapseSyncProfile profile = state.presentationCoord().getCollapseProfile(target);
        CommandFeedback.info(source, target.getName().getString() + " => " + profile.id());
        return 1;
    }
    
    private static int setCollapseProfileDefault(ServerCommandSource source, String profileId) {
        CollapseSyncProfile profile = CollapseSyncProfile.fromId(profileId);
        if (profile == null) {
            CommandFeedback.error(source, "Unknown sync profile: " + profileId);
            return 0;
        }
        CommandFacade facade = getFacade(source);
        if (!checkScenarioTyped(source, facade)) return 0;
        if (!facade.setCollapseDefaultProfile(source.getWorld(), profile)) {
            CommandFeedback.error(source, "Failed to update collapse profile; check server logs.");
            return 0;
        }
        CommandFeedback.successBroadcast(source, "Collapse default sync profile set to " + profile.id() + ".");
        return 1;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static CommandFacade getFacade(ServerCommandSource source) {
        return new CommandFacade(VirusWorldState.get(source.getWorld()));
    }
    
    private static boolean checkScenario(ServerCommandSource source, Object ignored) {
        CommandFacade facade = getFacade(source);
        if (facade.effectiveScenario(source.getWorld()).isEmpty()) {
            CommandFeedback.error(source, "No infection scenario is active for this dimension.");
            return false;
        }
        return true;
    }
    
    private static boolean checkScenarioTyped(ServerCommandSource source, CommandFacade facade) {
        if (facade.effectiveScenario(source.getWorld()).isEmpty()) {
            CommandFeedback.error(source, "No infection scenario is active for this dimension.");
            return false;
        }
        return true;
    }
    
    private static WaterDrainMode parseWaterDrainMode(String raw) {
        if (raw == null || raw.isBlank()) return WaterDrainMode.OFF;
        try {
            return WaterDrainMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return WaterDrainMode.OFF;
        }
    }
}
