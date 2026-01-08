package net.cyberpunk042.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.client.command.util.CommandScanner;
import net.cyberpunk042.client.command.util.FieldEditKnob;
import net.cyberpunk042.client.command.FieldBindingCommands;
import net.cyberpunk042.client.command.util.FieldPath;
import net.cyberpunk042.client.command.util.FieldPathArgument;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.field.BeamConfig;
import net.cyberpunk042.field.Modifiers;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.shape.*;
import net.cyberpunk042.visual.transform.Anchor;
import net.cyberpunk042.visual.transform.OrbitConfig;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.visual.visibility.MaskType;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-side /field edit commands using FieldEditKnob and CommandScanner.
 * 
 * <p>Commands directly update {@link FieldEditState} without server round-trips.
 * Uses dot notation paths that map to StateAccessor.</p>
 * 
 * <h2>Path-Based Syntax (Recommended)</h2>
 * <pre>
 * /field edit set layer[0].primitive[0].fill.mode solid
 * /field edit set layer[main].primitive[sphere].radius 2.0
 * /field edit set orbit.radius 3.5
 * /field edit get fill.mode
 * </pre>
 * 
 * <h2>Tree-Based Syntax (Shortcuts)</h2>
 * <pre>
 * /field edit layer select 0
 * /field edit primitive add sphere
 * /field edit fill mode solid
 * /field edit orbit radius 3.5
 * /field edit reset
 * </pre>
 * 
 * <p>Path syntax allows explicit layer/primitive targeting with bracket notation.
 * Tree syntax uses the currently selected layer/primitive.</p>
 */
public final class FieldEditCommands {
    
    private FieldEditCommands() {}
    
    /**
     * Registers the client-side /field edit commands.
     */
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var field = ClientCommandManager.literal("field");
        var edit = ClientCommandManager.literal("edit");
        
        // Path-based commands (bracket syntax)
        buildPathCommands(edit);
        
        // Layer/Primitive selection
        buildLayerCommands(edit);
        buildPrimitiveCommands(edit);
        
        // Core params
        buildCoreParams(edit);
        
        // Shape params - auto-generated from @Range annotations
        buildShapeCommands(edit);
        
        // Transform & orbit
        buildOrbitCommands(edit);
        buildTransformCommands(edit);
        
        // Animation - auto-generated
        buildAnimationCommands(edit);
        buildModifiersCommands(edit);
        buildBeamCommands(edit);
        
        // Visual
        buildFillCommands(edit);
        buildMaskCommands(edit);
        buildAppearanceCommands(edit);
        
        // Behavior
        buildFollowCommands(edit);
        buildPredictionCommands(edit);
        
        // Reset command
        edit.then(ClientCommandManager.literal("reset")
            .executes(ctx -> {
                FieldEditState state = FieldEditStateHolder.get();
                if (state != null) {
                    state.reset();
                    ctx.getSource().sendFeedback(Text.literal("Field state reset to defaults")
                        .formatted(Formatting.GREEN));
                }
                return 1;
            }));
        
        field.then(edit);
        
        // Fragment, Preset, Profile commands (delegated to separate files)
        FieldFragmentCommands.register(field);
        FieldProfileCommands.register(field);
        
        dispatcher.register(field);
        
        Logging.GUI.topic("command").info("Registered client-side /field edit commands");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED COMMANDS (bracket syntax)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildPathCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        // /field edit set <path> <value>
        edit.then(ClientCommandManager.literal("set")
            .then(ClientCommandManager.argument("path", FieldPathArgument.path())
                .then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        FieldPath path = FieldPathArgument.getPath(ctx, "path");
                        String value = StringArgumentType.getString(ctx, "value");
                        
                        FieldEditState state = FieldEditStateHolder.get();
                        if (state == null) {
                            ctx.getSource().sendError(Text.literal("Field state not available"));
                            return 0;
                        }
                        
                        // Apply layer/primitive context
                        path.applyContext();
                        
                        // Set the value via StateAccessor
                        String statePath = path.toStatePath();
                        boolean success = state.setFromString(statePath, value);
                        
                        if (success) {
                            ctx.getSource().sendFeedback(
                                Text.literal("Set " + path.displayPath() + " = " + value)
                                    .formatted(Formatting.GREEN));
                            return 1;
                        } else {
                            ctx.getSource().sendError(
                                Text.literal("Failed to set " + path.displayPath()));
                            return 0;
                        }
                    }))));
        
        // /field edit get <path>
        edit.then(ClientCommandManager.literal("get")
            .then(ClientCommandManager.argument("path", FieldPathArgument.path())
                .executes(ctx -> {
                    FieldPath path = FieldPathArgument.getPath(ctx, "path");
                    
                    FieldEditState state = FieldEditStateHolder.get();
                    if (state == null) {
                        ctx.getSource().sendError(Text.literal("Field state not available"));
                        return 0;
                    }
                    
                    // Apply layer/primitive context
                    path.applyContext();
                    
                    // Get the value via StateAccessor
                    String statePath = path.toStatePath();
                    Object value = state.get(statePath);
                    
                    ctx.getSource().sendFeedback(
                        Text.literal(path.displayPath() + " = ")
                            .formatted(Formatting.AQUA)
                            .append(Text.literal(String.valueOf(value))
                                .formatted(Formatting.WHITE)));
                    return 1;
                })));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildLayerCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var layer = ClientCommandManager.literal("layer");
        
        // /field edit layer select <index>
        layer.then(ClientCommandManager.literal("select")
            .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(0, 10))
                .executes(ctx -> {
                    int index = IntegerArgumentType.getInteger(ctx, "index");
                    FieldEditState state = FieldEditStateHolder.get();
                    if (state != null) {
                        state.setSelectedLayerIndex(index);
                        ctx.getSource().sendFeedback(Text.literal("Selected layer " + index)
                            .formatted(Formatting.GREEN));
                    }
                    return 1;
                })));
        
        // /field edit layer add
        layer.then(ClientCommandManager.literal("add")
            .executes(ctx -> {
                FieldEditState state = FieldEditStateHolder.get();
                if (state != null) {
                    int newIndex = state.addLayer();
                    ctx.getSource().sendFeedback(Text.literal("Added layer " + newIndex)
                        .formatted(Formatting.GREEN));
                }
                return 1;
            }));
        
        // /field edit layer remove
        layer.then(ClientCommandManager.literal("remove")
            .executes(ctx -> {
                FieldEditState state = FieldEditStateHolder.get();
                if (state != null) {
                    int current = state.getSelectedLayerIndex();
                    if (state.removeLayer(current)) {
                        ctx.getSource().sendFeedback(Text.literal("Removed layer " + current)
                            .formatted(Formatting.YELLOW));
                    } else {
                        ctx.getSource().sendFeedback(Text.literal("Cannot remove last layer")
                            .formatted(Formatting.RED));
                    }
                }
                return 1;
            }));
        
        // /field edit layer (show current)
        layer.executes(ctx -> {
            FieldEditState state = FieldEditStateHolder.get();
            if (state != null) {
                int current = state.getSelectedLayerIndex();
                int total = state.getLayerCount();
                ctx.getSource().sendFeedback(Text.literal("Layer: " + current + "/" + (total - 1))
                    .formatted(Formatting.AQUA));
            }
            return 1;
        });
        
        edit.then(layer);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildPrimitiveCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var primitive = ClientCommandManager.literal("primitive");
        
        // /field edit primitive select <index>
        primitive.then(ClientCommandManager.literal("select")
            .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(0, 20))
                .executes(ctx -> {
                    int index = IntegerArgumentType.getInteger(ctx, "index");
                    FieldEditState state = FieldEditStateHolder.get();
                    if (state != null) {
                        state.setSelectedPrimitiveIndex(index);
                        ctx.getSource().sendFeedback(Text.literal("Selected primitive " + index)
                            .formatted(Formatting.GREEN));
                    }
                    return 1;
                })));
        
        // /field edit primitive add [type]
        primitive.then(ClientCommandManager.literal("add")
            .executes(ctx -> {
                // Add default primitive
                FieldEditState state = FieldEditStateHolder.get();
                if (state != null) {
                    int layerIndex = state.getSelectedLayerIndex();
                    int newIndex = state.addPrimitive(layerIndex);
                    ctx.getSource().sendFeedback(Text.literal("Added primitive " + newIndex + " to layer " + layerIndex)
                        .formatted(Formatting.GREEN));
                }
                return 1;
            })
            .then(ClientCommandManager.argument("type", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (String name : ShapeRegistry.names()) {
                        builder.suggest(name);
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    String type = StringArgumentType.getString(ctx, "type");
                    FieldEditState state = FieldEditStateHolder.get();
                    if (state != null) {
                        int layerIndex = state.getSelectedLayerIndex();
                        int newIndex = state.addPrimitiveWithId(layerIndex, type);
                        ctx.getSource().sendFeedback(Text.literal("Added " + type + " primitive to layer " + layerIndex)
                            .formatted(Formatting.GREEN));
                    }
                    return 1;
                })));
        
        // /field edit primitive remove
        primitive.then(ClientCommandManager.literal("remove")
            .executes(ctx -> {
                FieldEditState state = FieldEditStateHolder.get();
                if (state != null) {
                    int layerIndex = state.getSelectedLayerIndex();
                    int primitiveIndex = state.getSelectedPrimitiveIndex();
                    if (state.removePrimitive(layerIndex, primitiveIndex)) {
                        ctx.getSource().sendFeedback(Text.literal("Removed primitive " + primitiveIndex)
                            .formatted(Formatting.YELLOW));
                    } else {
                        ctx.getSource().sendFeedback(Text.literal("Cannot remove last primitive")
                            .formatted(Formatting.RED));
                    }
                }
                return 1;
            }));
        
        // /field edit primitive (show current)
        primitive.executes(ctx -> {
            FieldEditState state = FieldEditStateHolder.get();
            if (state != null) {
                int layerIndex = state.getSelectedLayerIndex();
                int current = state.getSelectedPrimitiveIndex();
                int total = state.getPrimitiveCount(layerIndex);
                ctx.getSource().sendFeedback(Text.literal("Primitive: " + current + "/" + (total - 1) + " (layer " + layerIndex + ")")
                    .formatted(Formatting.AQUA));
            }
            return 1;
        });
        
        edit.then(primitive);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE PARAMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildCoreParams(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        FieldEditKnob.stringValue("shapeType", "Shape type")
            .attach(edit);
        
        FieldEditKnob.floatValue("radius", "Radius")
            .range(ValueRange.RADIUS)
            .attach(edit);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE COMMANDS - Auto-generated via CommandScanner
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildShapeCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        // Sphere
        var sphere = ClientCommandManager.literal("sphere");
        CommandScanner.scanRecord(SphereShape.class, "sphere", sphere);
        edit.then(sphere);
        
        // Ring
        var ring = ClientCommandManager.literal("ring");
        CommandScanner.scanRecord(RingShape.class, "ring", ring);
        edit.then(ring);
        
        // Prism
        var prism = ClientCommandManager.literal("prism");
        CommandScanner.scanRecord(PrismShape.class, "prism", prism);
        edit.then(prism);
        
        // Cylinder
        var cylinder = ClientCommandManager.literal("cylinder");
        CommandScanner.scanRecord(CylinderShape.class, "cylinder", cylinder);
        edit.then(cylinder);
        
        // Polyhedron
        var polyhedron = ClientCommandManager.literal("polyhedron");
        CommandScanner.scanRecord(PolyhedronShape.class, "polyhedron", polyhedron);
        edit.then(polyhedron);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ORBIT - Auto-generated
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildOrbitCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var orbit = ClientCommandManager.literal("orbit");
        CommandScanner.scanRecord(OrbitConfig.class, "orbit", orbit);
        edit.then(orbit);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRANSFORM
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildTransformCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var transform = ClientCommandManager.literal("transform");
        
        FieldEditKnob.floatValue("transform.scale", "Scale")
            .range(ValueRange.SCALE)
            .attach(transform);
        
        FieldEditKnob.enumValue("transform.anchor", "Anchor", Anchor.class)
            .attach(transform);
        
        edit.then(transform);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION - Auto-generated where possible
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildAnimationCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        // Spin
        var spin = ClientCommandManager.literal("spin");
        CommandScanner.scanRecord(SpinConfig.class, "spin", spin);
        edit.then(spin);
        
        // Pulse
        var pulse = ClientCommandManager.literal("pulse");
        CommandScanner.scanRecord(PulseConfig.class, "pulse", pulse);
        edit.then(pulse);
        
        // Alpha pulse
        var alphaPulse = ClientCommandManager.literal("alphaPulse");
        CommandScanner.scanRecord(AlphaPulseConfig.class, "alphaPulse", alphaPulse);
        edit.then(alphaPulse);
        
        // Wobble
        var wobble = ClientCommandManager.literal("wobble");
        CommandScanner.scanRecord(WobbleConfig.class, "wobble", wobble);
        edit.then(wobble);
        
        // Wave
        var wave = ClientCommandManager.literal("wave");
        CommandScanner.scanRecord(WaveConfig.class, "wave", wave);
        edit.then(wave);
        
        // Color cycle
        var colorCycle = ClientCommandManager.literal("colorCycle");
        CommandScanner.scanRecord(ColorCycleConfig.class, "colorCycle", colorCycle);
        edit.then(colorCycle);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILL
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildFillCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var fill = ClientCommandManager.literal("fill");
        
        FieldEditKnob.enumValue("fill.mode", "Fill mode", FillMode.class)
            .attach(fill);
        
        FieldEditKnob.floatValue("fill.wireThickness", "Wire thickness")
            .range(ValueRange.POSITIVE_NONZERO)
            .attach(fill);
        
        FieldEditKnob.toggle("fill.doubleSided", "Double sided")
            .attach(fill);
        
        edit.then(fill);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISIBILITY MASK
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildMaskCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var mask = ClientCommandManager.literal("mask");
        
        FieldEditKnob.enumValue("mask.type", "Mask type", MaskType.class)
            .attach(mask);
        
        FieldEditKnob.intValue("mask.count", "Mask count")
            .range(ValueRange.SIDES)
            .attach(mask);
        
        FieldEditKnob.floatValue("mask.thickness", "Mask thickness")
            .range(ValueRange.NORMALIZED)
            .attach(mask);
        
        edit.then(mask);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // APPEARANCE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildAppearanceCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var appearance = ClientCommandManager.literal("appearance");
        
        FieldEditKnob.stringValue("appearance.color", "Color")
            .attach(appearance);
        
        FieldEditKnob.floatValue("appearance.alpha", "Alpha")
            .range(ValueRange.ALPHA)
            .attach(appearance);
        
        FieldEditKnob.floatValue("appearance.glow", "Glow")
            .range(ValueRange.NORMALIZED)
            .attach(appearance);
        
        FieldEditKnob.floatValue("appearance.emissive", "Emissive")
            .range(ValueRange.NORMALIZED)
            .attach(appearance);
        
        edit.then(appearance);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildFollowCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var follow = ClientCommandManager.literal("follow");
        
        FieldEditKnob.toggle("followEnabled", "Follow enabled")
            .attach(follow);
        
        FieldEditKnob.stringValue("followMode", "Follow mode")
            .attach(follow);
        
        edit.then(follow);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PREDICTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildPredictionCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var prediction = ClientCommandManager.literal("prediction");
        
        FieldEditKnob.toggle("predictionEnabled", "Prediction enabled")
            .attach(prediction);
        
        FieldEditKnob.intValue("prediction.leadTicks", "Lead ticks")
            .range(ValueRange.TICKS)
            .attach(prediction);
        
        FieldEditKnob.floatValue("prediction.maxDistance", "Max distance")
            .range(ValueRange.RADIUS)
            .attach(prediction);
        
        FieldEditKnob.floatValue("prediction.lookAhead", "Look ahead")
            .range(ValueRange.NORMALIZED)
            .attach(prediction);
        
        edit.then(prediction);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MODIFIERS COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildModifiersCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var modifiers = ClientCommandManager.literal("modifiers");
        
        // Use CommandScanner to auto-generate from @Range annotations
        CommandScanner.scanRecord(Modifiers.class, "modifiers", modifiers);
        
        // Manual commands for booleans (not covered by @Range)
        FieldEditKnob.toggle("modifiers.inverted", "Inverted")
            .attach(modifiers);
        
        FieldEditKnob.toggle("modifiers.pulsing", "Pulsing")
            .attach(modifiers);
        
        edit.then(modifiers);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BEAM COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildBeamCommands(LiteralArgumentBuilder<FabricClientCommandSource> edit) {
        var beam = ClientCommandManager.literal("beam");
        
        // Toggle
        FieldEditKnob.toggle("beam.enabled", "Beam enabled")
            .attach(beam);
        
        // Auto-generate from @Range annotations (innerRadius, outerRadius, height, glow)
        CommandScanner.scanRecord(BeamConfig.class, "beam", beam);
        
        // Color (string)
        FieldEditKnob.stringValue("beam.color", "Beam color")
            .attach(beam);
        
        // Nested pulse config
        var beamPulse = ClientCommandManager.literal("pulse");
        FieldEditKnob.floatValue("beam.pulse.scale", "Pulse scale")
            .range(ValueRange.NORMALIZED)
            .attach(beamPulse);
        FieldEditKnob.floatValue("beam.pulse.speed", "Pulse speed")
            .range(ValueRange.POSITIVE)
            .attach(beamPulse);
        beam.then(beamPulse);
        
        // Quick presets
        beam.then(ClientCommandManager.literal("on")
            .executes(ctx -> {
                FieldEditState state = FieldEditStateHolder.get();
                if (state != null) {
                    state.set("beam.enabled", true);
                    ctx.getSource().sendFeedback(Text.literal("Beam enabled")
                        .formatted(Formatting.GREEN));
                }
                return 1;
            }));
        
        beam.then(ClientCommandManager.literal("off")
            .executes(ctx -> {
                FieldEditState state = FieldEditStateHolder.get();
                if (state != null) {
                    state.set("beam.enabled", false);
                    ctx.getSource().sendFeedback(Text.literal("Beam disabled")
                        .formatted(Formatting.YELLOW));
                }
                return 1;
            }));
        
        edit.then(beam);
    }

}
