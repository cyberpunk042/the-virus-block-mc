package net.cyberpunk042.command.field;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.cyberpunk042.network.gui.GuiOpenS2CPayload;
import net.cyberpunk042.network.gui.FieldEditUpdateS2CPayload;

/**
 * Main field command registration.
 * 
 * <h2>Command Tree</h2>
 * <pre>
 * /field
 * ├── list              - List all field definitions
 * ├── types             - List field types
 * ├── reload            - Reload definitions
 * ├── customize         - Open Field Customizer GUI
 * ├── customize <name>  - Open GUI with profile
 * ├── test spawn|despawn|toggle - Test field control
 * ├── edit/...          - Edit field state (syncs with GUI)
 * │   ├── shape <type>  - Set shape
 * │   ├── radius <v>    - Set radius
 * │   ├── color <hex>   - Set color
 * │   ├── alpha <v>     - Set alpha
 * │   ├── fill <mode>   - Set fill mode
 * │   ├── spin <speed>  - Set spin
 * │   ├── transform/... - Transform controls
 * │   ├── visibility/...- Visibility controls
 * │   ├── appearance/...- Appearance controls
 * │   ├── follow <mode> - Follow mode
 * │   ├── predict on|off- Prediction toggle
 * │   └── reset         - Reset to defaults
 * ├── theme/...         - Theme subcommands
 * ├── shield/...        - Shield subcommands
 * ├── personal/...      - Personal field subcommands
 * └── debug/...         - Debug toggles
 * </pre>
 */
public final class FieldCommand {
    
    // Debug state - accessible for rendering
    private static boolean debugBounds = false;
    private static boolean debugWireframe = false;
    private static boolean debugNormals = false;
    
    private FieldCommand() {}
    
    // Debug state accessors
    public static boolean isDebugBounds() { return debugBounds; }
    public static boolean isDebugWireframe() { return debugWireframe; }
    public static boolean isDebugNormals() { return debugNormals; }
    
    /**
     * Registers the /field command tree.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> cmd = CommandManager.literal("field")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("list")
                .executes(ctx -> handleList(ctx.getSource())))
            .then(CommandManager.literal("types")
                .executes(ctx -> handleTypes(ctx.getSource())))
            .then(CommandManager.literal("customize")
                .executes(ctx -> openCustomizer(ctx.getSource(), null))
                .then(CommandManager.argument("profile", StringArgumentType.word())
                    .executes(ctx -> openCustomizer(ctx.getSource(),
                        StringArgumentType.getString(ctx, "profile")))))
            .then(ThemeSubcommand.build())
            .then(PersonalSubcommand.build())
            .then(buildDebug())
            .then(buildTest())
            .then(FieldEditSubcommand.build());
        
        // Reload action
        CommandKnob.action("field.reload", "Reload field definitions")
            .successMessage("Field definitions reloaded.")
            .handler(FieldCommand::handleReload)
            .attach(cmd);
        
        dispatcher.register(cmd);
        Logging.COMMANDS.info("Registered /field command tree");
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildDebug() {
        var cmd = CommandManager.literal("debug");
        
        CommandKnob.toggle("field.debug.bounds", "Debug bounds rendering")
            .defaultValue(false)
            .handler((src, v) -> {
                debugBounds = v;
                Logging.RENDER.info("Debug bounds: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.toggle("field.debug.wireframe", "Debug wireframe mode")
            .defaultValue(false)
            .handler((src, v) -> {
                debugWireframe = v;
                Logging.RENDER.info("Debug wireframe: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.toggle("field.debug.normals", "Debug normal visualization")
            .defaultValue(false)
            .handler((src, v) -> {
                debugNormals = v;
                Logging.RENDER.info("Debug normals: {}", v);
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static int handleList(ServerCommandSource source) {
        Logging.COMMANDS.topic("field").debug(
            "Player {} executed /field list", source.getName());
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
    
    private static int handleTypes(ServerCommandSource source) {
        Logging.COMMANDS.topic("field").debug(
            "Player {} executed /field types", source.getName());
        CommandFeedback.info(source, "Field types:");
        for (FieldType type : FieldType.values()) {
            source.sendFeedback(() -> Text.literal("  " + type.id() + " - " + type.name()), false);
        }
        return FieldType.values().length;
    }
    
    private static boolean handleReload(ServerCommandSource source) {
        try {
            FieldRegistry.clear();
            FieldRegistry.registerDefaults();
            Logging.COMMANDS.info("Field definitions reloaded");
            return true;
        } catch (Exception e) {
            Logging.COMMANDS.error("Reload failed", e);
            CommandFeedback.error(source, "Reload failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Builds /field test subcommands for test field control.
     * 
     * <pre>
     * /field test spawn   - Spawn test field following player
     * /field test despawn - Remove test field
     * /field test toggle  - Toggle test field on/off
     * </pre>
     */
    private static LiteralArgumentBuilder<ServerCommandSource> buildTest() {
        return CommandManager.literal("test")
            .then(CommandManager.literal("spawn")
                .executes(ctx -> handleTestField(ctx.getSource(), "spawn")))
            .then(CommandManager.literal("despawn")
                .executes(ctx -> handleTestField(ctx.getSource(), "despawn")))
            .then(CommandManager.literal("toggle")
                .executes(ctx -> handleTestField(ctx.getSource(), "toggle")));
    }
    
    /**
     * Handles test field spawn/despawn/toggle commands.
     */
    private static int handleTestField(ServerCommandSource source, String action) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.testField(action));
        
        String message = switch (action) {
            case "spawn" -> "Test field spawned";
            case "despawn" -> "Test field despawned";
            case "toggle" -> "Test field toggled";
            default -> "Test field action: " + action;
        };
        
        CommandFeedback.success(source, message);
        Logging.GUI.topic("command").info("Player {} executed /field test {}", 
            player.getName().getString(), action);
        
        return 1;
    }
    
    /**
     * Opens the Field Customizer GUI for a player.
     * G09: /field customize - Opens GUI with current profile
     * G10: /field customize <profile> - Opens GUI with specified profile
     * 
     * @param source Command source (must be a player)
     * @param profile Profile name to load, or null for current
     * @return 1 on success, 0 on failure
     */
    private static int openCustomizer(ServerCommandSource source, String profile) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        String profileName = profile != null ? profile : "";
        boolean debugUnlocked = source.hasPermissionLevel(3); // OP level 3+ = debug access
        
        Logging.GUI.topic("command").info(
            "Player {} opening Field Customizer (profile: {}, debug: {})", 
            player.getName().getString(), 
            profileName.isEmpty() ? "current" : profileName,
            debugUnlocked);
        
        // Send packet to open GUI on client
        ServerPlayNetworking.send(player, new GuiOpenS2CPayload(profileName, debugUnlocked));
        
        return 1;
    }
}
