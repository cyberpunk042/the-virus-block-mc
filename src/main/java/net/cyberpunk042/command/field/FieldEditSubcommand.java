package net.cyberpunk042.command.field;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.PerformanceThresholds;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.gui.FieldEditUpdateS2CPayload;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.shape.ShapeRegistry;
import net.cyberpunk042.visual.transform.Anchor;
import net.cyberpunk042.visual.visibility.MaskType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * /field edit subcommands - syncs with GUI FieldEditState.
 * 
 * <h2>Command Tree</h2>
 * <pre>
 * /field edit
 * ├── shape <type>           - Set shape type (sphere, ring, disc, etc.)
 * ├── radius <value>         - Set radius (0.1-50)
 * ├── color <hex>            - Set color (#RRGGBB)
 * ├── alpha <value>          - Set alpha (0-1)
 * ├── fill <mode>            - Set fill mode (solid, wireframe, cage, points)
 * │
 * ├── spin <speed>           - Set spin speed (degrees/sec)
 * ├── spin off               - Disable spin
 * │
 * ├── transform
 * │   ├── anchor <type>      - Set anchor (center, base, top)
 * │   ├── scale <value>      - Set scale (0.1-10)
 * │   ├── offset <x> <y> <z> - Set offset
 * │   └── rotation <x> <y> <z> - Set rotation
 * │
 * ├── visibility
 * │   ├── mask <type>        - Set mask type
 * │   └── count <n>          - Set mask count
 * │
 * ├── appearance
 * │   ├── glow <value>       - Set glow intensity
 * │   └── emissive <value>   - Set emissive intensity
 * │
 * ├── follow <mode>          - Set follow mode (snap, smooth, glide)
 * ├── predict <on|off>       - Toggle prediction
 * │
 * └── reset                  - Reset all to defaults
 * </pre>
 * 
 * <p>All commands send S2C packets to update the client's FieldEditState,
 * which is then rendered by SimplifiedFieldRenderer.</p>
 */
public final class FieldEditSubcommand {
    
    private FieldEditSubcommand() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SUGGESTION PROVIDERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final SuggestionProvider<ServerCommandSource> SHAPE_SUGGESTIONS = 
        (ctx, builder) -> CommandSource.suggestMatching(ShapeRegistry.names(), builder);
    
    private static final SuggestionProvider<ServerCommandSource> FILL_SUGGESTIONS =
        (ctx, builder) -> CommandSource.suggestMatching(
            Arrays.stream(FillMode.values()).map(m -> m.name().toLowerCase()).collect(Collectors.toList()), builder);
    
    private static final SuggestionProvider<ServerCommandSource> ANCHOR_SUGGESTIONS =
        (ctx, builder) -> CommandSource.suggestMatching(
            Arrays.stream(Anchor.values()).map(a -> a.name().toLowerCase()).collect(Collectors.toList()), builder);
    
    private static final SuggestionProvider<ServerCommandSource> MASK_SUGGESTIONS =
        (ctx, builder) -> CommandSource.suggestMatching(
            Arrays.stream(MaskType.values()).map(m -> m.name().toLowerCase()).collect(Collectors.toList()), builder);
    
    private static final SuggestionProvider<ServerCommandSource> FOLLOW_SUGGESTIONS =
        (ctx, builder) -> CommandSource.suggestMatching(
            java.util.List.of("snap", "smooth", "glide"), builder);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // $REF SUGGESTION PROVIDERS (scan config folders for fragments)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static SuggestionProvider<ServerCommandSource> refSuggestions(String folder) {
        return (ctx, builder) -> {
            // Suggest the pattern
            builder.suggest("$" + folder + "/");
            
            // Scan config folder for available fragments
            Path configPath = Path.of("config/the-virus-block/" + folder);
            if (Files.exists(configPath)) {
                try (var stream = Files.list(configPath)) {
                    stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            name = name.substring(0, name.length() - 5); // remove .json
                            builder.suggest("$" + folder + "/" + name);
                        });
                } catch (Exception ignored) {}
            }
            
            // Also check data folder (bundled defaults)
            Path dataPath = Path.of("src/main/resources/data/the-virus-block/" + folder);
            if (Files.exists(dataPath)) {
                try (var stream = Files.list(dataPath)) {
                    stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            name = name.substring(0, name.length() - 5);
                            builder.suggest("$" + folder + "/" + name);
                        });
                } catch (Exception ignored) {}
            }
            
            return builder.buildFuture();
        };
    }
    
    private static final SuggestionProvider<ServerCommandSource> SHAPE_REF = refSuggestions("field_shapes");
    private static final SuggestionProvider<ServerCommandSource> FILL_REF = refSuggestions("field_fills");
    private static final SuggestionProvider<ServerCommandSource> VISIBILITY_REF = refSuggestions("field_masks");
    private static final SuggestionProvider<ServerCommandSource> APPEARANCE_REF = refSuggestions("field_appearances");
    private static final SuggestionProvider<ServerCommandSource> ANIMATION_REF = refSuggestions("field_animations");
    private static final SuggestionProvider<ServerCommandSource> TRANSFORM_REF = refSuggestions("field_transforms");
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILD COMMAND TREE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Builds the /field edit subcommand tree.
     */
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("edit")
            // Shape
            .then(CommandManager.literal("shape")
                .then(CommandManager.argument("type", StringArgumentType.word())
                    .suggests(SHAPE_SUGGESTIONS)
                    .executes(ctx -> setShapeType(ctx.getSource(), 
                        StringArgumentType.getString(ctx, "type"))))
                .then(CommandManager.literal("latSteps")
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(4, 512))
                        .executes(ctx -> setLatStepsWithWarning(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "value")))))
                .then(CommandManager.literal("lonSteps")
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(8, 1024))
                        .executes(ctx -> setLonStepsWithWarning(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "value")))))
                .then(CommandManager.literal("$ref")
                    .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                        .suggests(SHAPE_REF)
                        .executes(ctx -> loadRef(ctx.getSource(), "SHAPE", 
                            StringArgumentType.getString(ctx, "ref"))))))
            
            // Radius (with performance warning)
            .then(CommandManager.literal("radius")
                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 50f))
                    .executes(ctx -> setRadiusWithWarning(ctx.getSource(),
                        FloatArgumentType.getFloat(ctx, "value")))))
            
            // Color
            .then(CommandManager.literal("color")
                .then(CommandManager.argument("hex", StringArgumentType.word())
                    .executes(ctx -> setColor(ctx.getSource(),
                        StringArgumentType.getString(ctx, "hex")))))
            
            // Alpha
            .then(CommandManager.literal("alpha")
                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                    .executes(ctx -> setFloat(ctx.getSource(), "APPEARANCE", "alpha",
                        FloatArgumentType.getFloat(ctx, "value")))))
            
            // Fill mode
            .then(CommandManager.literal("fill")
                .then(CommandManager.argument("mode", StringArgumentType.word())
                    .suggests(FILL_SUGGESTIONS)
                    .executes(ctx -> setString(ctx.getSource(), "FILL", "mode",
                        StringArgumentType.getString(ctx, "mode"))))
                .then(CommandManager.literal("$ref")
                    .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                        .suggests(FILL_REF)
                        .executes(ctx -> loadRef(ctx.getSource(), "FILL",
                            StringArgumentType.getString(ctx, "ref"))))))
            
            // Spin / Animation
            .then(CommandManager.literal("spin")
                .then(CommandManager.literal("off")
                    .executes(ctx -> setBool(ctx.getSource(), "ANIMATION", "spinEnabled", false)))
                .then(CommandManager.argument("speed", FloatArgumentType.floatArg(-360f, 360f))
                    .executes(ctx -> setSpin(ctx.getSource(),
                        FloatArgumentType.getFloat(ctx, "speed")))))
            .then(CommandManager.literal("animation")
                .then(CommandManager.literal("$ref")
                    .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                        .suggests(ANIMATION_REF)
                        .executes(ctx -> loadRef(ctx.getSource(), "ANIMATION",
                            StringArgumentType.getString(ctx, "ref"))))))
            
            // Transform subcommands
            .then(buildTransform())
            
            // Visibility subcommands
            .then(buildVisibility())
            
            // Appearance subcommands
            .then(buildAppearance())
            
            // Follow mode
            .then(CommandManager.literal("follow")
                .then(CommandManager.argument("mode", StringArgumentType.word())
                    .suggests(FOLLOW_SUGGESTIONS)
                    .executes(ctx -> setString(ctx.getSource(), "FOLLOW", "mode",
                        StringArgumentType.getString(ctx, "mode")))))
            
            // Prediction
            .then(CommandManager.literal("predict")
                .then(CommandManager.literal("on")
                    .executes(ctx -> setBool(ctx.getSource(), "PREDICTION", "enabled", true)))
                .then(CommandManager.literal("off")
                    .executes(ctx -> setBool(ctx.getSource(), "PREDICTION", "enabled", false))))
            
            // Reset
            .then(CommandManager.literal("reset")
                .executes(ctx -> reset(ctx.getSource())));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildTransform() {
        return CommandManager.literal("transform")
            .then(CommandManager.literal("anchor")
                .then(CommandManager.argument("type", StringArgumentType.word())
                    .suggests(ANCHOR_SUGGESTIONS)
                    .executes(ctx -> setString(ctx.getSource(), "TRANSFORM", "anchor",
                        StringArgumentType.getString(ctx, "type")))))
            .then(CommandManager.literal("scale")
                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 10f))
                    .executes(ctx -> setFloat(ctx.getSource(), "TRANSFORM", "scale",
                        FloatArgumentType.getFloat(ctx, "value")))))
            .then(CommandManager.literal("offset")
                .then(CommandManager.argument("x", FloatArgumentType.floatArg(-50f, 50f))
                    .then(CommandManager.argument("y", FloatArgumentType.floatArg(-50f, 50f))
                        .then(CommandManager.argument("z", FloatArgumentType.floatArg(-50f, 50f))
                            .executes(ctx -> setOffset(ctx.getSource(),
                                FloatArgumentType.getFloat(ctx, "x"),
                                FloatArgumentType.getFloat(ctx, "y"),
                                FloatArgumentType.getFloat(ctx, "z")))))))
            .then(CommandManager.literal("rotation")
                .then(CommandManager.argument("x", FloatArgumentType.floatArg(-180f, 180f))
                    .then(CommandManager.argument("y", FloatArgumentType.floatArg(-180f, 180f))
                        .then(CommandManager.argument("z", FloatArgumentType.floatArg(-180f, 180f))
                            .executes(ctx -> setRotation(ctx.getSource(),
                                FloatArgumentType.getFloat(ctx, "x"),
                                FloatArgumentType.getFloat(ctx, "y"),
                                FloatArgumentType.getFloat(ctx, "z")))))))
            .then(CommandManager.literal("$ref")
                .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                    .suggests(TRANSFORM_REF)
                    .executes(ctx -> loadRef(ctx.getSource(), "TRANSFORM",
                        StringArgumentType.getString(ctx, "ref")))));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildVisibility() {
        return CommandManager.literal("visibility")
            .then(CommandManager.literal("mask")
                .then(CommandManager.argument("type", StringArgumentType.word())
                    .suggests(MASK_SUGGESTIONS)
                    .executes(ctx -> setString(ctx.getSource(), "VISIBILITY", "maskType",
                        StringArgumentType.getString(ctx, "type")))))
            .then(CommandManager.literal("count")
                .then(CommandManager.argument("n", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> setMaskCountWithWarning(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "n")))))
            .then(CommandManager.literal("$ref")
                .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                    .suggests(VISIBILITY_REF)
                    .executes(ctx -> loadRef(ctx.getSource(), "VISIBILITY",
                        StringArgumentType.getString(ctx, "ref")))));
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildAppearance() {
        return CommandManager.literal("appearance")
            .then(CommandManager.literal("glow")
                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                    .executes(ctx -> setFloat(ctx.getSource(), "APPEARANCE", "glow",
                        FloatArgumentType.getFloat(ctx, "value")))))
            .then(CommandManager.literal("emissive")
                .then(CommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                    .executes(ctx -> setFloat(ctx.getSource(), "APPEARANCE", "emissive",
                        FloatArgumentType.getFloat(ctx, "value")))))
            .then(CommandManager.literal("$ref")
                .then(CommandManager.argument("ref", StringArgumentType.greedyString())
                    .suggests(APPEARANCE_REF)
                    .executes(ctx -> loadRef(ctx.getSource(), "APPEARANCE",
                        StringArgumentType.getString(ctx, "ref")))));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMMAND HANDLERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int setShapeType(ServerCommandSource source, String type) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        // Validate shape type
        if (!ShapeRegistry.names().contains(type.toLowerCase())) {
            CommandFeedback.error(source, "Unknown shape type: " + type);
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.shapeType(type.toLowerCase()));
        CommandFeedback.success(source, "Shape set to: " + type);
        Logging.GUI.topic("command").debug("Player {} set shape to {}", player.getName().getString(), type);
        return 1;
    }
    
    private static int setColor(ServerCommandSource source, String hex) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        // Parse hex color
        String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
        int color;
        try {
            color = Integer.parseInt(cleanHex, 16);
        } catch (NumberFormatException e) {
            CommandFeedback.error(source, "Invalid color format. Use #RRGGBB or RRGGBB");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.intParam("APPEARANCE", "color", color));
        CommandFeedback.success(source, "Color set to: #" + cleanHex);
        return 1;
    }
    
    private static int setSpin(ServerCommandSource source, float speed) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        // Send both enable and speed
        String json = "{\"spinEnabled\":true,\"spinSpeed\":" + speed + "}";
        ServerPlayNetworking.send(player, new FieldEditUpdateS2CPayload("ANIMATION", json));
        CommandFeedback.success(source, "Spin set to: " + speed + "°/sec");
        return 1;
    }
    
    private static int setOffset(ServerCommandSource source, float x, float y, float z) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        String json = "{\"offsetX\":" + x + ",\"offsetY\":" + y + ",\"offsetZ\":" + z + "}";
        ServerPlayNetworking.send(player, new FieldEditUpdateS2CPayload("TRANSFORM", json));
        CommandFeedback.success(source, String.format("Offset set to: (%.1f, %.1f, %.1f)", x, y, z));
        return 1;
    }
    
    private static int setRotation(ServerCommandSource source, float x, float y, float z) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        String json = "{\"rotationX\":" + x + ",\"rotationY\":" + y + ",\"rotationZ\":" + z + "}";
        ServerPlayNetworking.send(player, new FieldEditUpdateS2CPayload("TRANSFORM", json));
        CommandFeedback.success(source, String.format("Rotation set to: (%.1f, %.1f, %.1f)", x, y, z));
        return 1;
    }
    
    private static int setFloat(ServerCommandSource source, String category, String param, float value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.floatParam(category, param, value));
        CommandFeedback.success(source, param + " set to: " + value);
        return 1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERFORMANCE-AWARE HANDLERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int setRadiusWithWarning(ServerCommandSource source, float value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.floatParam("SHAPE", "radius", value));
        CommandFeedback.success(source, "radius set to: " + value);
        
        // Performance warning
        PerformanceThresholds.checkAndWarn(source, "radius", value, 
            PerformanceThresholds.RADIUS_WARN, PerformanceThresholds.RADIUS_CRITICAL);
        
        return 1;
    }
    
    private static int setLatStepsWithWarning(ServerCommandSource source, int value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.intParam("SHAPE", "latSteps", value));
        CommandFeedback.success(source, "latSteps set to: " + value);
        
        // Performance warning
        PerformanceThresholds.checkAndWarn(source, "latSteps", value,
            PerformanceThresholds.LAT_STEPS_WARN, PerformanceThresholds.LAT_STEPS_CRITICAL);
        
        return 1;
    }
    
    private static int setLonStepsWithWarning(ServerCommandSource source, int value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.intParam("SHAPE", "lonSteps", value));
        CommandFeedback.success(source, "lonSteps set to: " + value);
        
        // Performance warning
        PerformanceThresholds.checkAndWarn(source, "lonSteps", value,
            PerformanceThresholds.LON_STEPS_WARN, PerformanceThresholds.LON_STEPS_CRITICAL);
        
        return 1;
    }
    
    private static int setMaskCountWithWarning(ServerCommandSource source, int value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.intParam("VISIBILITY", "maskCount", value));
        CommandFeedback.success(source, "maskCount set to: " + value);
        
        // Performance warning
        PerformanceThresholds.checkAndWarn(source, "maskCount", value,
            PerformanceThresholds.MASK_COUNT_WARN, PerformanceThresholds.MASK_COUNT_CRITICAL);
        
        return 1;
    }
    
    private static int setInt(ServerCommandSource source, String category, String param, int value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.intParam(category, param, value));
        CommandFeedback.success(source, param + " set to: " + value);
        return 1;
    }
    
    private static int setBool(ServerCommandSource source, String category, String param, boolean value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.boolParam(category, param, value));
        CommandFeedback.success(source, param + " set to: " + value);
        return 1;
    }
    
    private static int setString(ServerCommandSource source, String category, String param, String value) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.stringParam(category, param, value));
        CommandFeedback.success(source, param + " set to: " + value);
        return 1;
    }
    
    private static int reset(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        ServerPlayNetworking.send(player, FieldEditUpdateS2CPayload.reset());
        CommandFeedback.success(source, "Field state reset to defaults");
        Logging.GUI.topic("command").info("Player {} reset field state", player.getName().getString());
        return 1;
    }
    
    /**
     * Loads a $ref fragment and sends it to the client.
     * The client will resolve the reference and apply it to FieldEditState.
     * 
     * @param source Command source
     * @param category Category (SHAPE, FILL, VISIBILITY, etc.)
     * @param ref Reference path like "$field_shapes/simple_sphere"
     */
    private static int loadRef(ServerCommandSource source, String category, String ref) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            CommandFeedback.error(source, "This command can only be used by players.");
            return 0;
        }
        
        // Clean up the ref path
        String cleanRef = ref.trim();
        if (!cleanRef.startsWith("$")) {
            cleanRef = "$" + cleanRef;
        }
        
        // Send as a $ref update - client will resolve and apply
        String json = "{\"$ref\":\"" + cleanRef + "\"}";
        ServerPlayNetworking.send(player, new FieldEditUpdateS2CPayload(category + "_REF", json));
        
        CommandFeedback.success(source, "Loading " + category.toLowerCase() + " from: " + cleanRef);
        Logging.GUI.topic("command").debug("Player {} loading ref {} for {}", 
            player.getName().getString(), cleanRef, category);
        return 1;
    }
}

