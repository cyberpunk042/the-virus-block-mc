package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.field.BeamConfig;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.field.Modifiers;
import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.influence.TriggerConfig;
import net.cyberpunk042.field.instance.FollowConfig;
import net.cyberpunk042.field.loader.SimplePrimitive;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.AlphaRange;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.shape.PolyhedronShape;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.transform.OrbitConfig;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.cyberpunk042.visual.animation.WobbleConfig;
import net.cyberpunk042.visual.animation.WaveConfig;

import java.lang.reflect.Field;
import java.util.*;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Builds a FieldDefinition from FieldEditState using reflection.
 * 
 * <p>Scans for {@link DefinitionField} and {@link PrimitiveComponent} annotations
 * to automatically construct the nested FieldDefinition structure.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * FieldEditState state = FieldEditStateHolder.get();
 * FieldDefinition def = DefinitionBuilder.fromState(state);
 * FieldRenderer.render(matrices, consumers, def, pos, scale, time, alpha);
 * </pre>
 * 
 * @see DefinitionField
 * @see PrimitiveComponent
 */
public final class DefinitionBuilder {
    
    private DefinitionBuilder() {}
    
    /**
     * Builds a FieldDefinition from the current FieldEditState.
     * 
     * @param state The edit state to convert
     * @return A complete FieldDefinition ready for rendering
     */
    public static FieldDefinition fromState(FieldEditState state) {
        try {
            return buildDefinition(state);
        } catch (Exception e) {
            Logging.GUI.topic("builder").error("Failed to build FieldDefinition from state: {} at {}", 
                e.getMessage(), e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "unknown");
            e.printStackTrace();
            return FieldDefinition.empty("error");
        }
    }
    
    private static FieldDefinition buildDefinition(FieldEditState state) throws Exception {
        // Outer scope: batches all builder logging into single TRACE output
        try (LogScope scope = Logging.GUI.topic("builder").scope("build-definition", LogLevel.TRACE)) {
            // Collect @DefinitionField values
            Map<String, Object> defFields = collectDefinitionFields(state);
            
            // Build layers from state
            List<FieldLayer> layers = buildLayers(state);
            
            // CP2-CP3: Field-level segments (D1-D4)
            // Ensure modifiers is never null - use DEFAULT if not set
            Modifiers modifiers = (Modifiers) defFields.getOrDefault("modifiers", Modifiers.DEFAULT);
            net.cyberpunk042.field.instance.FollowConfig follow = 
                (net.cyberpunk042.field.instance.FollowConfig) defFields.get("follow");
            
            // CP2: State values
            PipelineTracer.trace(PipelineTracer.D1_BOBBING, 2, "state", String.valueOf(modifiers.bobbing()));
            PipelineTracer.trace(PipelineTracer.D2_BREATHING, 2, "state", String.valueOf(modifiers.breathing()));
            if (follow != null && follow.enabled()) {
                PipelineTracer.trace(PipelineTracer.D4_FOLLOW_MODE, 2, "state", 
                    "lead=" + follow.leadOffset() + " resp=" + follow.responsiveness());
            }
            
            // CP2: Beam segments (B1-B7) from state
            BeamConfig beam = (BeamConfig) defFields.get("beam");
            if (beam != null) {
                PipelineTracer.trace(PipelineTracer.B1_BEAM_ENABLED, 2, "state", beam.enabled());
                PipelineTracer.trace(PipelineTracer.B2_BEAM_INNER_RADIUS, 2, "state", beam.innerRadius());
                PipelineTracer.trace(PipelineTracer.B3_BEAM_OUTER_RADIUS, 2, "state", beam.outerRadius());
                PipelineTracer.trace(PipelineTracer.B4_BEAM_COLOR, 2, "state", beam.color());
                PipelineTracer.trace(PipelineTracer.B5_BEAM_HEIGHT, 2, "state", beam.height());
                PipelineTracer.trace(PipelineTracer.B6_BEAM_GLOW, 2, "state", beam.glow());
                PipelineTracer.trace(PipelineTracer.B7_BEAM_PULSE, 2, "state", beam.pulse() != null ? "active" : "null");
                // CP3: Beam built
                PipelineTracer.trace(PipelineTracer.B1_BEAM_ENABLED, 3, "beam", beam.enabled());
                PipelineTracer.trace(PipelineTracer.B2_BEAM_INNER_RADIUS, 3, "beam", beam.innerRadius());
                PipelineTracer.trace(PipelineTracer.B3_BEAM_OUTER_RADIUS, 3, "beam", beam.outerRadius());
                PipelineTracer.trace(PipelineTracer.B4_BEAM_COLOR, 3, "beam", beam.color());
                PipelineTracer.trace(PipelineTracer.B5_BEAM_HEIGHT, 3, "beam", beam.height());
                PipelineTracer.trace(PipelineTracer.B6_BEAM_GLOW, 3, "beam", beam.glow());
                PipelineTracer.trace(PipelineTracer.B7_BEAM_PULSE, 3, "beam", beam.pulse() != null ? "active" : "null");
            }
            
            // Construct the definition
            FieldDefinition def = new FieldDefinition(
                "preview_" + System.currentTimeMillis(),
                FieldType.SHIELD,
                state.getFloat("sphere.radius") > 0 ? state.getFloat("sphere.radius") : 1.0f,
                null, // themeId - could be added later
                layers,
                modifiers,
                follow,
                (BeamConfig) defFields.get("beam"),
                buildBindings(state),
                buildTriggers(state),
                null, // lifecycle - handled separately
                (net.cyberpunk042.field.force.ForceFieldConfig) defFields.get("forceConfig"),
                state.shockwaveAdapter().toJson(),  // Field-level shockwave FX config
                state.fieldVisualAdapter().toJson()  // Field visual (Energy Orb) config
            );
            
            // CP3: Field-level in definition - modifiers should never be null now
            var defMods = def.modifiers();
            PipelineTracer.trace(PipelineTracer.D1_BOBBING, 3, "def", String.valueOf(defMods != null ? defMods.bobbing() : "null"));
            PipelineTracer.trace(PipelineTracer.D2_BREATHING, 3, "def", String.valueOf(defMods != null ? defMods.breathing() : "null"));
            if (def.follow() != null && def.follow().enabled()) {
                PipelineTracer.trace(PipelineTracer.D4_FOLLOW_MODE, 3, "def", 
                    "lead=" + def.follow().leadOffset() + " resp=" + def.follow().responsiveness());
            }
            
            scope.kv("layers", layers.size()).kv("id", def.id());
            
            return def;
        }
    }
    
    /**
     * Scans FieldEditState for @DefinitionField annotations and collects values.
     */
    private static Map<String, Object> collectDefinitionFields(FieldEditState state) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try (LogScope scope = Logging.GUI.topic("builder").scope("process-getClass", LogLevel.TRACE)) {
            for (Field field : state.getClass().getDeclaredFields()) {
                DefinitionField annotation = field.getAnnotation(DefinitionField.class);
                if (annotation != null) {
                    field.setAccessible(true);
                    Object value = field.get(state);
                    if (value != null) {
                        result.put(annotation.value(), value);
                        scope.branch("field").kv("name", annotation.value()).kv("type", value.getClass().getSimpleName());
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Builds FieldLayers from the state's layer list.
     */
    private static List<FieldLayer> buildLayers(FieldEditState state) throws Exception {
        List<FieldLayer> layers = new ArrayList<>();
        
        // Get layers from state
        var stateLayers = state.getFieldLayers();
        if (stateLayers == null || stateLayers.isEmpty()) {
            // Create a default layer with current primitive
            layers.add(buildDefaultLayer(state));
        } else {
            // Convert each layer
            for (int i = 0; i < stateLayers.size(); i++) {
                FieldLayer stateLayer = stateLayers.get(i);
                // For now, use the layer as-is since it's already a FieldLayer
                // In the future, we might need to rebuild primitives with current @PrimitiveComponent values
                if (i == state.getSelectedLayerIndex()) {
                    // Rebuild selected layer with current editor values
                    layers.add(rebuildLayerWithCurrentState(stateLayer, state));
                } else {
                    layers.add(stateLayer);
                }
            }
        }
        
        return layers;
    }
    
    /**
     * Builds a default layer when no layers exist.
     */
    private static FieldLayer buildDefaultLayer(FieldEditState state) throws Exception {
        SimplePrimitive primitive = buildCurrentPrimitive(state);
        return FieldLayer.of("default_layer", List.of(primitive));
    }
    
    /**
     * Rebuilds a layer, replacing the selected primitive with current editor values.
     */
    private static FieldLayer rebuildLayerWithCurrentState(FieldLayer original, FieldEditState state) throws Exception {
        List<Primitive> newPrimitives = new ArrayList<>();
        var origPrimitives = original.primitives();
        int selectedPrimIdx = state.getSelectedPrimitiveIndex();
        
        for (int i = 0; i < origPrimitives.size(); i++) {
            if (i == selectedPrimIdx) {
                // Replace with current editor state
                newPrimitives.add(buildCurrentPrimitive(state));
            } else {
                newPrimitives.add(origPrimitives.get(i));
            }
        }
        
        // CP2-CP3: Layer-level segments
        PipelineTracer.trace(PipelineTracer.L1_LAYER_ALPHA, 2, "state", String.valueOf(original.alpha()));
        PipelineTracer.trace(PipelineTracer.L2_LAYER_VISIBLE, 2, "state", String.valueOf(original.visible()));
        PipelineTracer.trace(PipelineTracer.L3_BLEND_MODE, 2, "state", original.blendMode() != null ? original.blendMode().name() : "null");
        
        // Rebuild layer with new primitives
        FieldLayer rebuilt = new FieldLayer(
            original.id(),
            newPrimitives,
            original.transform(),
            original.animation(),
            original.alpha(),
            original.visible(),
            original.blendMode()
        );
        
        // CP3: Layer built
        PipelineTracer.trace(PipelineTracer.L1_LAYER_ALPHA, 3, "layer", String.valueOf(rebuilt.alpha()));
        PipelineTracer.trace(PipelineTracer.L2_LAYER_VISIBLE, 3, "layer", String.valueOf(rebuilt.visible()));
        PipelineTracer.trace(PipelineTracer.L3_BLEND_MODE, 3, "layer", rebuilt.blendMode() != null ? rebuilt.blendMode().name() : "null");
        
        return rebuilt;
    }
    
    /**
     * Builds a Primitive from current @PrimitiveComponent annotated fields.
     */
    private static SimplePrimitive buildCurrentPrimitive(FieldEditState state) throws Exception {
        // Get current shape based on shapeType
        String shapeType = state.getString("shapeType");
        Shape shape = getCurrentShape(state, shapeType);
        
        // CP2-CP3: Shape segments
        PipelineTracer.trace(PipelineTracer.S1_SHAPE_TYPE, 2, "state", shapeType);
        PipelineTracer.trace(PipelineTracer.S1_SHAPE_TYPE, 3, "shape", shape.getClass().getSimpleName());
        traceShapeDetails(shapeType, shape);
        
        // Get components directly from adapter accessor methods
        Transform transform = state.transform();
        FillConfig fill = state.fill();
        VisibilityMask mask = state.mask();
        ArrangementConfig arrangement = state.arrangement();
        
        // CP2-CP3: Fill segments (ALL)
        traceFillDetails(fill);
        
        // CP2-CP3: Transform segments (ALL)
        traceTransformDetails(transform);
        
        // CP2-CP3: Visibility/Mask segments (ALL)
        traceVisibilityDetails(mask);
        
        // Build appearance from adapter (convert AppearanceState to Appearance)
        Appearance appearance = buildAppearance(state);
        
        // Build animation from adapter
        Animation animation = buildAnimation(state);
        traceAnimationDetails(state, animation);
        
        // Get primitiveId from link adapter  
        String primitiveId = state.getString("primitiveId");
        if (primitiveId == null || primitiveId.isEmpty()) {
            primitiveId = "primitive_" + System.currentTimeMillis();
        }
        
        // Add primitive info to current scope (auto-nests into parent build-definition scope)
        LogScope.currentOrNoop().branch("primitive")
            .kv("type", shapeType)
            .kv("shapeClass", shape != null ? shape.getClass().getSimpleName() : "null");
        
        // Get link from adapter
        PrimitiveLink link = state.link();
        
        return new SimplePrimitive(
            primitiveId,
            shapeType,
            shape,
            transform != null ? transform : Transform.IDENTITY,
            fill != null ? fill : FillConfig.SOLID,
            mask != null ? mask : VisibilityMask.FULL,
            arrangement != null ? arrangement : ArrangementConfig.DEFAULT,
            appearance != null ? appearance : Appearance.DEFAULT,
            animation != null ? animation : Animation.NONE,
            link
        );
    }
    
    /**
     * Traces shape-specific details based on shape type.
     */
    private static void traceShapeDetails(String shapeType, Shape shape) {
        switch (shapeType.toLowerCase()) {
            case "sphere" -> {
                if (shape instanceof net.cyberpunk042.visual.shape.SphereShape s) {
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 2, "state", s.radius());
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 3, "shape", s.radius());
                    PipelineTracer.trace(PipelineTracer.S3_LAT_STEPS, 2, "state", s.latSteps());
                    PipelineTracer.trace(PipelineTracer.S3_LAT_STEPS, 3, "shape", s.latSteps());
                    PipelineTracer.trace(PipelineTracer.S4_LON_STEPS, 2, "state", s.lonSteps());
                    PipelineTracer.trace(PipelineTracer.S4_LON_STEPS, 3, "shape", s.lonSteps());
                    PipelineTracer.trace(PipelineTracer.S5_ALGORITHM, 2, "state", s.algorithm().name());
                    PipelineTracer.trace(PipelineTracer.S5_ALGORITHM, 3, "shape", s.algorithm().name());
                }
            }
            case "ring" -> {
                if (shape instanceof net.cyberpunk042.visual.shape.RingShape r) {
                    PipelineTracer.trace(PipelineTracer.S6_INNER_RADIUS, 2, "state", r.innerRadius());
                    PipelineTracer.trace(PipelineTracer.S6_INNER_RADIUS, 3, "shape", r.innerRadius());
                    PipelineTracer.trace(PipelineTracer.S7_OUTER_RADIUS, 2, "state", r.outerRadius());
                    PipelineTracer.trace(PipelineTracer.S7_OUTER_RADIUS, 3, "shape", r.outerRadius());
                    PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 2, "state", r.segments());
                    PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 3, "shape", r.segments());
                    PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 2, "state", r.height());
                    PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 3, "shape", r.height());
                }
            }
            case "cylinder" -> {
                if (shape instanceof net.cyberpunk042.visual.shape.CylinderShape c) {
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 2, "state", c.radius());
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 3, "shape", c.radius());
                    PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 2, "state", c.height());
                    PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 3, "shape", c.height());
                    PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 2, "state", c.segments());
                    PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 3, "shape", c.segments());
                }
            }
            case "prism" -> {
                if (shape instanceof net.cyberpunk042.visual.shape.PrismShape p) {
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 2, "state", p.radius());
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 3, "shape", p.radius());
                    PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 2, "state", p.height());
                    PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 3, "shape", p.height());
                    PipelineTracer.trace(PipelineTracer.S10_SIDES, 2, "state", p.sides());
                    PipelineTracer.trace(PipelineTracer.S10_SIDES, 3, "shape", p.sides());
                }
            }
            case "polyhedron", "cube", "octahedron", "icosahedron", "tetrahedron", "dodecahedron" -> {
                if (shape instanceof PolyhedronShape p) {
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 2, "state", p.radius());
                    PipelineTracer.trace(PipelineTracer.S2_RADIUS, 3, "shape", p.radius());
                    PipelineTracer.trace(PipelineTracer.S11_POLY_TYPE, 2, "state", p.polyType().name());
                    PipelineTracer.trace(PipelineTracer.S11_POLY_TYPE, 3, "shape", p.polyType().name());
                }
            }
        }
    }
    
    /**
     * Traces ALL animation details.
     */
    private static void traceAnimationDetails(FieldEditState state, Animation animation) {
        // Spin animation (now per-axis)
        if (state.spin() != null) {
            PipelineTracer.trace(PipelineTracer.N1_SPIN_SPEED, 2, "state.Y", state.spin().speedY());
            if (animation.spin() != null) {
                PipelineTracer.trace(PipelineTracer.N1_SPIN_SPEED, 3, "anim.Y", animation.spin().speedY());
            }
        }
        
        // Pulse animation
        if (state.pulse() != null) {
            PipelineTracer.trace(PipelineTracer.N3_PULSE_SPEED, 2, "state", state.pulse().speed());
            PipelineTracer.trace(PipelineTracer.N4_PULSE_SCALE, 2, "state", state.pulse().scale());
            PipelineTracer.trace(PipelineTracer.N5_PULSE_MODE, 2, "state", state.pulse().mode().name());
            if (animation.pulse() != null) {
                PipelineTracer.trace(PipelineTracer.N3_PULSE_SPEED, 3, "anim", animation.pulse().speed());
                PipelineTracer.trace(PipelineTracer.N4_PULSE_SCALE, 3, "anim", animation.pulse().scale());
                PipelineTracer.trace(PipelineTracer.N5_PULSE_MODE, 3, "anim", animation.pulse().mode().name());
            }
        }
        
        // Alpha pulse animation
        if (state.alphaPulse() != null) {
            PipelineTracer.trace(PipelineTracer.N6_ALPHA_PULSE_SPEED, 2, "state", state.alphaPulse().speed());
            PipelineTracer.trace(PipelineTracer.N7_ALPHA_PULSE_MIN, 2, "state", state.alphaPulse().min());
            PipelineTracer.trace(PipelineTracer.N8_ALPHA_PULSE_MAX, 2, "state", state.alphaPulse().max());
            if (animation.alphaPulse() != null) {
                PipelineTracer.trace(PipelineTracer.N6_ALPHA_PULSE_SPEED, 3, "anim", animation.alphaPulse().speed());
                PipelineTracer.trace(PipelineTracer.N7_ALPHA_PULSE_MIN, 3, "anim", animation.alphaPulse().min());
                PipelineTracer.trace(PipelineTracer.N8_ALPHA_PULSE_MAX, 3, "anim", animation.alphaPulse().max());
            }
        }
        
        // Wave animation
        if (state.wave() != null) {
            PipelineTracer.trace(PipelineTracer.N9_WAVE_SPEED, 2, "state", state.wave().frequency());
            PipelineTracer.trace(PipelineTracer.N10_WAVE_AMPLITUDE, 2, "state", state.wave().amplitude());
            if (animation.wave() != null) {
                PipelineTracer.trace(PipelineTracer.N9_WAVE_SPEED, 3, "anim", animation.wave().frequency());
                PipelineTracer.trace(PipelineTracer.N10_WAVE_AMPLITUDE, 3, "anim", animation.wave().amplitude());
            }
        }
        
        // Wobble animation
        if (state.wobble() != null) {
            PipelineTracer.trace(PipelineTracer.N11_WOBBLE_SPEED, 2, "state", state.wobble().speed());
            if (animation.wobble() != null) {
                PipelineTracer.trace(PipelineTracer.N11_WOBBLE_SPEED, 3, "anim", animation.wobble().speed());
            }
        }
        
        // Color cycle animation
        if (state.colorCycle() != null) {
            PipelineTracer.trace(PipelineTracer.N12_COLOR_CYCLE, 2, "state", state.colorCycle().isActive() ? "active" : "inactive");
            PipelineTracer.trace(PipelineTracer.N16_COLOR_CYCLE_SPEED, 2, "state", state.colorCycle().speed());
            if (animation.colorCycle() != null) {
                PipelineTracer.trace(PipelineTracer.N12_COLOR_CYCLE, 3, "anim", animation.colorCycle().isActive() ? "active" : "inactive");
                PipelineTracer.trace(PipelineTracer.N16_COLOR_CYCLE_SPEED, 3, "anim", animation.colorCycle().speed());
            }
        }
    }
    
    /**
     * Traces ALL transform details.
     */
    private static void traceTransformDetails(Transform transform) {
        if (transform == null || transform == Transform.IDENTITY) {
            return;
        }
        // CP2: State values
        PipelineTracer.trace(PipelineTracer.T1_OFFSET, 2, "state", transform.offset() != null ? transform.offset().toString() : "null");
        PipelineTracer.trace(PipelineTracer.T2_ROTATION, 2, "state", transform.rotation() != null ? transform.rotation().toString() : "null");
        PipelineTracer.trace(PipelineTracer.T3_SCALE, 2, "state", transform.scale());
        PipelineTracer.trace(PipelineTracer.T4_SCALE_XYZ, 2, "state", transform.scaleXYZ() != null ? transform.scaleXYZ().toString() : "null");
        PipelineTracer.trace(PipelineTracer.T5_ANCHOR, 2, "state", transform.anchor() != null ? transform.anchor().name() : "null");
        PipelineTracer.trace(PipelineTracer.T6_BILLBOARD, 2, "state", transform.billboard() != null ? transform.billboard().name() : "null");
        PipelineTracer.trace(PipelineTracer.T7_ORBIT, 2, "state", transform.orbit() != null ? "active" : "null");
        PipelineTracer.trace(PipelineTracer.T8_INHERIT_ROTATION, 2, "state", transform.inheritRotation());
        PipelineTracer.trace(PipelineTracer.T9_SCALE_WITH_RADIUS, 2, "state", transform.scaleWithRadius());
        PipelineTracer.trace(PipelineTracer.T10_FACING, 2, "state", transform.facing() != null ? transform.facing().name() : "null");
        PipelineTracer.trace(PipelineTracer.T11_UP_VECTOR, 2, "state", transform.up() != null ? transform.up().name() : "null");
        if (transform.orbit() != null) {
            PipelineTracer.trace(PipelineTracer.T12_ORBIT_RADIUS, 2, "state", transform.orbit().radius());
            PipelineTracer.trace(PipelineTracer.T13_ORBIT_SPEED, 2, "state", transform.orbit().speed());
        }
        // CP3: Built values
        PipelineTracer.trace(PipelineTracer.T1_OFFSET, 3, "transform", transform.offset() != null ? transform.offset().toString() : "null");
        PipelineTracer.trace(PipelineTracer.T2_ROTATION, 3, "transform", transform.rotation() != null ? transform.rotation().toString() : "null");
        PipelineTracer.trace(PipelineTracer.T3_SCALE, 3, "transform", transform.scale());
        PipelineTracer.trace(PipelineTracer.T4_SCALE_XYZ, 3, "transform", transform.scaleXYZ() != null ? transform.scaleXYZ().toString() : "null");
        PipelineTracer.trace(PipelineTracer.T5_ANCHOR, 3, "transform", transform.anchor() != null ? transform.anchor().name() : "null");
        PipelineTracer.trace(PipelineTracer.T6_BILLBOARD, 3, "transform", transform.billboard() != null ? transform.billboard().name() : "null");
        PipelineTracer.trace(PipelineTracer.T7_ORBIT, 3, "transform", transform.orbit() != null ? "active" : "null");
        PipelineTracer.trace(PipelineTracer.T8_INHERIT_ROTATION, 3, "transform", transform.inheritRotation());
        PipelineTracer.trace(PipelineTracer.T9_SCALE_WITH_RADIUS, 3, "transform", transform.scaleWithRadius());
        PipelineTracer.trace(PipelineTracer.T10_FACING, 3, "transform", transform.facing() != null ? transform.facing().name() : "null");
        PipelineTracer.trace(PipelineTracer.T11_UP_VECTOR, 3, "transform", transform.up() != null ? transform.up().name() : "null");
        if (transform.orbit() != null) {
            PipelineTracer.trace(PipelineTracer.T12_ORBIT_RADIUS, 3, "transform", transform.orbit().radius());
            PipelineTracer.trace(PipelineTracer.T13_ORBIT_SPEED, 3, "transform", transform.orbit().speed());
        }
    }
    
    /**
     * Traces ALL visibility/mask details.
     */
    private static void traceVisibilityDetails(VisibilityMask mask) {
        if (mask == null) {
            return;
        }
        // CP2: State values
        PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 2, "state", mask.mask().name());
        PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 2, "state", mask.count());
        PipelineTracer.trace(PipelineTracer.V3_MASK_THICKNESS, 2, "state", mask.thickness());
        PipelineTracer.trace(PipelineTracer.V4_MASK_OFFSET, 2, "state", mask.offset());
        PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 2, "state", mask.animate());
        PipelineTracer.trace(PipelineTracer.V6_MASK_ANIM_SPEED, 2, "state", mask.animSpeed());
        PipelineTracer.trace(PipelineTracer.V7_MASK_INVERT, 2, "state", mask.invert());
        PipelineTracer.trace(PipelineTracer.V8_MASK_FEATHER, 2, "state", mask.feather());
        PipelineTracer.trace(PipelineTracer.V9_MASK_DIRECTION, 2, "state", mask.direction());
        PipelineTracer.trace(PipelineTracer.V10_MASK_FALLOFF, 2, "state", mask.falloff());
        PipelineTracer.trace(PipelineTracer.V11_GRADIENT_START, 2, "state", mask.gradientStart());
        PipelineTracer.trace(PipelineTracer.V12_GRADIENT_END, 2, "state", mask.gradientEnd());
        PipelineTracer.trace(PipelineTracer.V13_CENTER_X, 2, "state", mask.centerX());
        PipelineTracer.trace(PipelineTracer.V14_CENTER_Y, 2, "state", mask.centerY());
        // CP3: Built values
        PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 3, "mask", mask.mask().name());
        PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 3, "mask", mask.count());
        PipelineTracer.trace(PipelineTracer.V3_MASK_THICKNESS, 3, "mask", mask.thickness());
        PipelineTracer.trace(PipelineTracer.V4_MASK_OFFSET, 3, "mask", mask.offset());
        PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 3, "mask", mask.animate());
        PipelineTracer.trace(PipelineTracer.V6_MASK_ANIM_SPEED, 3, "mask", mask.animSpeed());
        PipelineTracer.trace(PipelineTracer.V7_MASK_INVERT, 3, "mask", mask.invert());
        PipelineTracer.trace(PipelineTracer.V8_MASK_FEATHER, 3, "mask", mask.feather());
        PipelineTracer.trace(PipelineTracer.V9_MASK_DIRECTION, 3, "mask", mask.direction());
        PipelineTracer.trace(PipelineTracer.V10_MASK_FALLOFF, 3, "mask", mask.falloff());
        PipelineTracer.trace(PipelineTracer.V11_GRADIENT_START, 3, "mask", mask.gradientStart());
        PipelineTracer.trace(PipelineTracer.V12_GRADIENT_END, 3, "mask", mask.gradientEnd());
        PipelineTracer.trace(PipelineTracer.V13_CENTER_X, 3, "mask", mask.centerX());
        PipelineTracer.trace(PipelineTracer.V14_CENTER_Y, 3, "mask", mask.centerY());
    }
    
    /**
     * Traces ALL fill details.
     */
    private static void traceFillDetails(FillConfig fill) {
        if (fill == null) {
            return;
        }
        // CP2: State values
        PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 2, "state", fill.mode().name());
        PipelineTracer.trace(PipelineTracer.F2_WIRE_THICKNESS, 2, "state", fill.wireThickness());
        PipelineTracer.trace(PipelineTracer.F3_DOUBLE_SIDED, 2, "state", fill.doubleSided());
        PipelineTracer.trace(PipelineTracer.F4_DEPTH_TEST, 2, "state", fill.depthTest());
        PipelineTracer.trace(PipelineTracer.F5_DEPTH_WRITE, 2, "state", fill.depthWrite());
        PipelineTracer.trace(PipelineTracer.F6_CAGE_OPTIONS, 2, "state", fill.cage() != null ? "present" : "null");
        // CP3: Built values
        PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 3, "fill", fill.mode().name());
        PipelineTracer.trace(PipelineTracer.F2_WIRE_THICKNESS, 3, "fill", fill.wireThickness());
        PipelineTracer.trace(PipelineTracer.F3_DOUBLE_SIDED, 3, "fill", fill.doubleSided());
        PipelineTracer.trace(PipelineTracer.F4_DEPTH_TEST, 3, "fill", fill.depthTest());
        PipelineTracer.trace(PipelineTracer.F5_DEPTH_WRITE, 3, "fill", fill.depthWrite());
        PipelineTracer.trace(PipelineTracer.F6_CAGE_OPTIONS, 3, "fill", fill.cage() != null ? "present" : "null");
    }
    
    /**
     * Scans for @PrimitiveComponent annotations and collects values.
     */
    private static Map<String, Object> collectPrimitiveComponents(FieldEditState state) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String shapeType = state.getString("shapeType");
        
        try (LogScope scope = Logging.GUI.topic("builder").scope("process-getClass", LogLevel.TRACE)) {
            for (Field field : state.getClass().getDeclaredFields()) {
                PrimitiveComponent annotation = field.getAnnotation(PrimitiveComponent.class);
                if (annotation != null) {
                    // Check if shape-specific
                    if (annotation.shapeSpecific()) {
                        // Only include if field name starts with current shapeType
                        if (!field.getName().toLowerCase().startsWith(shapeType.toLowerCase())) {
                            continue;
                        }
                    }

                    field.setAccessible(true);
                    Object value = field.get(state);
                    if (value != null) {
                        result.put(annotation.value(), value);
                        scope.branch("field").kv("annotation", annotation.value()).kv("simpleName", value.getClass().getSimpleName());
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets the current shape object based on shapeType.
     */
    private static Shape getCurrentShape(FieldEditState state, String shapeType) {
        // Use the adapter's currentShape() which correctly handles shape type switching
        Shape shape = state.currentShape();
        if (shape != null) {
            return shape;
        }
        // Fallback - shouldn't happen if adapter is working
        Logging.GUI.topic("builder").warn("[SHAPE] currentShape() returned null, falling back to sphere");
        return state.sphere();
    }
    
    /**
     * Builds Appearance from AppearanceState.
     * Converts int colors to String color references.
     */
    private static Appearance buildAppearance(FieldEditState state) {
        var appState = state.appearance();
        if (appState == null) {
            Logging.GUI.topic("builder").warn("[APPEARANCE] AppearanceState is null, using default");
            return Appearance.DEFAULT;
        }
        
        // CP2: State values for ALL appearance segments
        PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 2, "state", "0x" + Integer.toHexString(appState.primaryColor()));
        PipelineTracer.trace(PipelineTracer.A2_ALPHA, 2, "state", appState.alpha());
        PipelineTracer.trace(PipelineTracer.A3_GLOW, 2, "state", appState.glow());
        PipelineTracer.trace(PipelineTracer.A4_EMISSIVE, 2, "state", appState.emissive());
        PipelineTracer.trace(PipelineTracer.A5_SATURATION, 2, "state", appState.saturation());
        PipelineTracer.trace(PipelineTracer.A6_SECONDARY_COLOR, 2, "state", "0x" + Integer.toHexString(appState.secondaryColor()));
        
        // Convert int color to hex string format
        String colorHex = String.format("#%06X", appState.primaryColor() & 0xFFFFFF);
        String secondaryHex = String.format("#%06X", appState.secondaryColor() & 0xFFFFFF);
        
        // CP3: Builder conversion
        PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 3, "hex", colorHex);
        PipelineTracer.trace(PipelineTracer.A2_ALPHA, 3, "range", appState.alpha());
        PipelineTracer.trace(PipelineTracer.A3_GLOW, 3, "val", appState.glow());
        PipelineTracer.trace(PipelineTracer.A4_EMISSIVE, 3, "val", appState.emissive());
        PipelineTracer.trace(PipelineTracer.A5_SATURATION, 3, "val", appState.saturation());
        PipelineTracer.trace(PipelineTracer.A6_SECONDARY_COLOR, 3, "hex", secondaryHex);
        
        return Appearance.builder()
            .color(colorHex)
            .alpha(AlphaRange.of(appState.alpha()))
            .glow(appState.glow())
            .emissive(appState.emissive())
            .saturation(appState.saturation())
            .brightness(appState.brightness())
            .hueShift(appState.hueShift())
            .secondaryColor(secondaryHex)
            .colorBlend(appState.colorBlend())
            .colorMode(appState.colorMode())
            .colorDistribution(appState.colorDistribution())
            .colorSet(appState.colorSet())
            .gradientDirection(appState.gradientDirection())
            .timePhase(appState.timePhase())
            .build();
    }
    
    /**
     * Builds Animation from spin, pulse, wave, wobble configs.
     */
    private static Animation buildAnimation(FieldEditState state) {
        return Animation.builder()
            .spin(state.spin())
            .pulse(state.pulse())
            .alphaPulse(state.alphaPulse())
            .wobble(state.wobble())
            .wave(state.wave())
            .colorCycle(state.colorCycle())
            .precession(state.precession())
            .travelEffect(state.travelEffect())
            .rayFlow(state.rayFlow())
            .rayMotion(state.rayMotion())
            .rayWiggle(state.rayWiggle())
            .rayTwist(state.rayTwist())
            .build();
    }
    
    /**
     * Builds bindings map from state.
     */
    private static Map<String, BindingConfig> buildBindings(FieldEditState state) {
        // TODO: Implement when bindings are editable in GUI
        return Map.of();
    }
    
    /**
     * Builds triggers list from state.
     */
    private static List<TriggerConfig> buildTriggers(FieldEditState state) {
        // TODO: Implement when triggers are editable in GUI
        return List.of();
    }
}
