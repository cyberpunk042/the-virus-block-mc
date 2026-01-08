package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.render.FieldRenderLayers;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.instance.FieldInstance;
import net.cyberpunk042.field.instance.LifecycleState;
import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.influence.BindingResolver;
import net.cyberpunk042.field.influence.TriggerEffect;
import net.cyberpunk042.field.influence.TriggerProcessor;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.color.ColorTheme;
import net.cyberpunk042.visual.color.ColorThemeRegistry;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import net.cyberpunk042.client.gui.state.PipelineTracer;

import java.util.Map;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Main renderer for field definitions.
 * 
 * <p>Per CLASS_DIAGRAM §8: FieldRenderer is the entry point for rendering
 * a complete field. It resolves the color theme, iterates through layers,
 * and delegates to {@link LayerRenderer} for each.
 * 
 * <h2>Rendering Pipeline</h2>
 * <pre>
 * FieldRenderer.render(definition, ...)
 *    │
 *    ├── Resolve ColorTheme
 *    ├── Create ColorResolver
 *    │
 *    └── for each layer:
 *           └── LayerRenderer.render(layer, ...)
 *                  │
 *                  └── for each primitive:
 *                         └── PrimitiveRenderer.render(primitive, ...)
 * </pre>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In WorldRenderEvents.AFTER_ENTITIES:
 * FieldRenderer.render(
 *     matrices,
 *     vertexConsumers,
 *     definition,
 *     position,
 *     scale,
 *     worldTime,
 *     alpha
 * );
 * </pre>
 * 
 * @see LayerRenderer
 * @see PrimitiveRenderer
 */
public final class FieldRenderer {
    
    private FieldRenderer() {}
    
    // =========================================================================
    // Main Render Methods
    // =========================================================================
    
    /**
     * Renders a field definition at the given position.
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition to render
     * @param position World position (camera-relative)
     * @param scale Overall scale
     * @param time Animation time (world time + tick delta)
     * @param alpha Overall alpha (0-1)
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha) {
        
        render(matrices, consumers, definition, position, scale, time, alpha, null);
    }
    
    /**
     * Renders a field definition with smooth follow applied at render-time.
     * 
     * <p>This method applies follow offset using the player's LERPED (interpolated) position
     * and rotation, eliminating the flickering caused by tick-rate position updates.</p>
     * 
     * <p>The follow system uses:
     * <ul>
     *   <li><b>leadOffset</b>: negative = trailing, positive = leading in velocity direction</li>
     *   <li><b>lookAhead</b>: offset toward player's look direction</li>
     * </ul>
     * </p>
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider  
     * @param definition Field definition to render
     * @param basePosition Base world position (camera-relative, typically player center)
     * @param player Player entity for velocity/rotation (can be null to skip follow)
     * @param tickDelta Partial tick for smooth interpolation (0-1)
     * @param camPos Camera position (for computing render position)
     * @param scale Overall scale
     * @param time Animation time (world time + tick delta)
     * @param alpha Overall alpha (0-1)
     */
    public static void renderWithFollow(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d basePosition,
            PlayerEntity player,
            float tickDelta,
            Vec3d camPos,
            float scale,
            float time,
            float alpha) {
        
        Vec3d renderPos = basePosition;
        
        // Apply follow offset if enabled and player available
        if (player != null && definition.follow() != null && definition.follow().enabled()) {
            renderPos = applyFollowOffset(player, basePosition, definition.follow(), tickDelta);
        }
        
        // Convert to camera-relative
        Vec3d finalPos = renderPos.subtract(camPos);
        
        render(matrices, consumers, definition, finalPos, scale, time, alpha, null);
    }
    
    /**
     * Legacy method name for backward compatibility.
     * @deprecated Use {@link #renderWithFollow} instead.
     */
    @Deprecated
    public static void renderWithPrediction(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d basePosition,
            PlayerEntity player,
            float tickDelta,
            Vec3d camPos,
            float scale,
            float time,
            float alpha) {
        renderWithFollow(matrices, consumers, definition, basePosition, player, tickDelta, camPos, scale, time, alpha);
    }
    
    /**
     * Computes follow offset at render-time using player's lerped position/rotation.
     * 
     * <p>The base position is already smoothly interpolated via getLerpedPos(tickDelta).
     * Follow offset adds position adjustment based on:
     * <ul>
     *   <li>leadOffset: velocity-based offset (negative=trail, positive=lead)</li>
     *   <li>lookAhead: offset toward look direction</li>
     * </ul>
     * </p>
     * 
     * <p>IMPORTANT: Ignores Y velocity when player is on ground to prevent gravity-induced
     * flickering. Minecraft's gravity causes ~-0.0784 Y velocity even when standing still.</p>
     */
    private static Vec3d applyFollowOffset(
            PlayerEntity player, 
            Vec3d base, 
            net.cyberpunk042.field.instance.FollowConfig follow,
            float tickDelta) {
        
        // If locked (no offset), just return base
        if (follow.isLocked()) {
            return base;
        }
        
        Vec3d result = base;
        
        // Apply look-ahead offset (in look direction)
        float lookAhead = follow.lookAhead();
        if (Math.abs(lookAhead) > 0.001f) {
            Vec3d look = player.getRotationVec(tickDelta);
            result = result.add(look.multiply(lookAhead));
        }
        
        // Apply lead/trail offset (in velocity direction)
        float leadOffset = follow.leadOffset();
        if (Math.abs(leadOffset) > 0.01f) {
            Vec3d velocity = player.getVelocity();
            
            // Ignore Y velocity when on ground (gravity noise)
            if (player.isOnGround()) {
                velocity = new Vec3d(velocity.x, 0.0, velocity.z);
            }
            
            // Only apply if moving significantly
            double speed = velocity.length();
            if (speed > 0.01) {
                // Scale the offset by velocity magnitude for smooth feel
                // leadOffset of 1.0 with speed of 0.2 should give ~0.2 offset
                result = result.add(velocity.normalize().multiply(leadOffset * speed * 5.0));
            }
        }
        
        return result;
    }
    
    /**
     * Renders a field instance with lifecycle-aware alpha.
     * 
     * <p>F181: Applies fadeProgress from the instance's lifecycle state:
     * <ul>
     *   <li>SPAWNING: alpha scales from 0 → 1 as fadeProgress increases</li>
     *   <li>ACTIVE: full alpha (fadeProgress = 1.0)</li>
     *   <li>DESPAWNING: alpha scales from 1 → 0 as fadeProgress increases</li>
     *   <li>COMPLETE: alpha = 0 (not rendered)</li>
     * </ul>
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition to render
     * @param instance Field instance with lifecycle state
     * @param position World position (camera-relative)
     * @param time Animation time
     */
    public static void renderInstance(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            FieldInstance instance,
            Vec3d position,
            float time) {
        
        if (instance == null || instance.isComplete()) {
            return;
        }
        
        // F181: Calculate effective alpha from lifecycle state
        float effectiveAlpha = calculateLifecycleAlpha(instance);
        
        if (effectiveAlpha <= 0.01f) {
            return;
        }
        
        render(matrices, consumers, definition, position, 
               instance.scale(), time, effectiveAlpha, null);
        
        Logging.FIELD.topic("render").trace(
            "Rendered instance {} (state={}, fadeProgress={:.2f}, alpha={:.2f})",
            instance.id(), instance.lifecycleState(), 
            instance.fadeProgress(), effectiveAlpha);
    }
    
    /**
     * F181: Calculates effective alpha based on lifecycle state and fadeProgress.
     * 
     * @param instance The field instance
     * @return Effective alpha (0-1)
     */
    private static float calculateLifecycleAlpha(FieldInstance instance) {
        float baseAlpha = instance.alpha();
        float fadeProgress = instance.fadeProgress();
        LifecycleState state = instance.lifecycleState();
        
        return switch (state) {
            case SPAWNING -> baseAlpha * fadeProgress;           // Fade in
            case ACTIVE -> baseAlpha;                            // Full alpha
            case DESPAWNING -> baseAlpha * (1.0f - fadeProgress); // Fade out
            case COMPLETE -> 0.0f;                               // Invisible
        };
    }
    
    // =========================================================================
    // F182: Binding Evaluation
    // =========================================================================
    
    /**
     * Renders a field with bindings evaluated against the player.
     * 
     * <p>F182: Bindings dynamically modify render properties:
     * <ul>
     *   <li>"alpha" → alpha multiplier (e.g., fade based on health)</li>
     *   <li>"scale" → scale multiplier (e.g., grow based on damage)</li>
     * </ul>
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition with bindings
     * @param player Player for binding sources
     * @param position World position
     * @param scale Base scale
     * @param time Animation time
     * @param alpha Base alpha
     * @param fieldAge Field age in ticks
     */
    public static void renderWithBindings(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            PlayerEntity player,
            Vec3d position,
            float scale,
            float time,
            float alpha,
            int fieldAge) {
        
        // Evaluate bindings
        RenderOverrides overrides = evaluateBindings(definition, player, fieldAge);
        
        // Apply binding results
        float effectiveScale = scale * overrides.scaleMultiplier();
        float effectiveAlpha = alpha * overrides.alphaMultiplier();
        
        render(matrices, consumers, definition, position, effectiveScale, time, effectiveAlpha, overrides);
        
        if (overrides.hasOverrides()) {
            Logging.FIELD.topic("binding").trace(
                "Applied bindings: alpha×{:.2f}, scale×{:.2f}", 
                overrides.alphaMultiplier(), overrides.scaleMultiplier());
        }
    }
    
    /**
     * Evaluates all bindings in a definition and returns RenderOverrides.
     * 
     * @param definition Field definition with bindings
     * @param player Player for binding sources
     * @param fieldAge Field age in ticks
     * @return RenderOverrides with evaluated binding values
     */
    public static RenderOverrides evaluateBindings(
            FieldDefinition definition, 
            PlayerEntity player, 
            int fieldAge) {
        
        Map<String, BindingConfig> bindings = definition.bindings();
        if (bindings == null || bindings.isEmpty()) {
            return RenderOverrides.NONE;
        }
        
        // Evaluate all bindings
        Map<String, Float> results = BindingResolver.evaluateAll(bindings, player, fieldAge);
        
        // Build overrides from results
        RenderOverrides.Builder builder = RenderOverrides.builder();
        
        // Apply known binding targets
        if (results.containsKey("alpha")) {
            builder.alphaMultiplier(results.get("alpha"));
        }
        if (results.containsKey("scale")) {
            builder.scaleMultiplier(results.get("scale"));
        }
        
        return builder.build();
    }
    
    // =========================================================================
    // F183: Trigger Effect Application
    // =========================================================================
    
    /**
     * Renders a field with active trigger effects applied.
     * 
     * <p>F183: Trigger effects modify the render state:
     * <ul>
     *   <li>PULSE → scale multiplier</li>
     *   <li>SHAKE → position offset</li>
     *   <li>FLASH → color overlay with intensity</li>
     *   <li>GLOW → glow intensity multiplier</li>
     *   <li>COLOR_SHIFT → temporary color override</li>
     * </ul>
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param definition Field definition
     * @param triggerProcessor Active trigger processor
     * @param position World position
     * @param scale Base scale
     * @param time Animation time
     * @param alpha Base alpha
     */
    public static void renderWithTriggers(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            TriggerProcessor triggerProcessor,
            Vec3d position,
            float scale,
            float time,
            float alpha) {
        
        if (triggerProcessor == null || !triggerProcessor.hasActiveTriggers()) {
            render(matrices, consumers, definition, position, scale, time, alpha, null);
            return;
        }
        
        // Apply trigger effects
        RenderOverrides overrides = applyTriggerEffects(triggerProcessor);
        
        // Apply PULSE scale
        float pulseScale = triggerProcessor.getEffectValue(TriggerEffect.PULSE);
        float effectiveScale = scale * (pulseScale > 0 ? pulseScale : 1.0f);
        
        // Apply SHAKE offset
        float[] shakeOffset = triggerProcessor.getShakeOffset();
        Vec3d effectivePosition = position.add(shakeOffset[0], shakeOffset[1], shakeOffset[2]);
        
        render(matrices, consumers, definition, effectivePosition, effectiveScale, time, alpha, overrides);
        
        Logging.FIELD.topic("trigger").trace(
            "Applied triggers: pulse={:.2f}, shake=[{:.2f},{:.2f},{:.2f}]",
            pulseScale, shakeOffset[0], shakeOffset[1], shakeOffset[2]);
    }
    
    /**
     * Creates RenderOverrides from active trigger effects.
     */
    private static RenderOverrides applyTriggerEffects(TriggerProcessor processor) {
        RenderOverrides.Builder builder = RenderOverrides.builder();
        
        // FLASH: color overlay with intensity as alpha multiplier
        float flashIntensity = processor.getFlashIntensity();
        String flashColor = processor.getFlashColor();
        if (flashIntensity > 0 && flashColor != null) {
            int color = parseFlashColor(flashColor, flashIntensity);
            builder.colorOverride(color);
        }
        
        // GLOW: Would need glow support in RenderOverrides (future)
        // float glowIntensity = processor.getEffectValue(TriggerEffect.GLOW);
        
        return builder.build();
    }
    
    /**
     * Parses a flash color string and applies intensity to alpha.
     */
    private static int parseFlashColor(String colorStr, float intensity) {
        try {
            String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
            int rgb = Integer.parseUnsignedInt(hex, 16);
            int alpha = (int) (intensity * 255) & 0xFF;
            return (alpha << 24) | (rgb & 0x00FFFFFF);
        } catch (NumberFormatException e) {
            return 0x80FF0000; // Default red with 50% alpha
        }
    }
    
    /**
     * Renders a field definition with optional overrides.
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldDefinition definition,
            Vec3d position,
            float scale,
            float time,
            float alpha,
            RenderOverrides overrides) {
        
        // Outer scope: batches all logging into single TRACE output
        try (LogScope scope = Logging.FIELD.topic("render").scope("field:" + (definition != null ? definition.id() : "null"), LogLevel.TRACE)) {
            scope.kv("alpha", alpha)
                 .kv("scale", scale);
            
            // Early exit checks
            if (definition == null || alpha <= 0.01f) {
                scope.leaf("SKIP: definition=" + (definition == null ? "null" : "ok") + ", alpha=" + alpha);
                return;
            }
            
            if (definition.layers() == null || definition.layers().isEmpty()) {
                scope.leaf("SKIP: no layers");
                return;
            }
            
            scope.kv("layers", definition.layers().size());
        
            // === PHASE 1: Resolve Theme ===
            ColorTheme theme = resolveTheme(definition);
            ColorResolver resolver = theme != null 
                ? ColorResolver.fromTheme(theme) 
                : ColorResolver.DEFAULT;
            
            // === PHASE 2: Render Layer Selection ===
            // Note: Layer selection is now done per-primitive in LayerRenderer based on fill mode
            
            // === PHASE 3: Position ===
            matrices.push();
            try {
            matrices.translate(position.x, position.y, position.z);
        
            // === PHASE 3.5: Apply Field Modifiers (bobbing, breathing) ===
            var mods = definition.modifiers();
            
            if (mods != null) {
                // CP5: Values reaching renderer
                PipelineTracer.trace(PipelineTracer.D1_BOBBING, 5, "render", String.valueOf(mods.bobbing()));
                PipelineTracer.trace(PipelineTracer.D2_BREATHING, 5, "render", String.valueOf(mods.breathing()));
                
                // Apply modifiers if any value is non-zero
                if (mods.bobbing() > 0 || mods.breathing() > 0) {
                    scope.branch("modifiers").kv("bobbing", mods.bobbing()).kv("breathing", mods.breathing());
                    net.cyberpunk042.client.visual.animation.AnimationApplier.applyModifiers(
                        matrices, mods, time);
                    // CP6: Matrix modified
                    PipelineTracer.trace(PipelineTracer.D1_BOBBING, 6, "matrix", "applied");
                    PipelineTracer.trace(PipelineTracer.D2_BREATHING, 6, "matrix", "applied");
                    // CP7: Transformation complete
                    PipelineTracer.trace(PipelineTracer.D1_BOBBING, 7, "complete", "transformed");
                    PipelineTracer.trace(PipelineTracer.D2_BREATHING, 7, "complete", "transformed");
                } else {
                    PipelineTracer.trace(PipelineTracer.D1_BOBBING, 6, "matrix", "skipped-zero");
                    PipelineTracer.trace(PipelineTracer.D2_BREATHING, 6, "matrix", "skipped-zero");
                }
            } else {
                PipelineTracer.trace(PipelineTracer.D1_BOBBING, 5, "render", "null-modifiers");
                PipelineTracer.trace(PipelineTracer.D2_BREATHING, 5, "render", "null-modifiers");
            }
        
            // === PHASE 4: Render Each Layer ===
            // CP4: Layer-level segments (trace for first layer)
            if (!definition.layers().isEmpty()) {
                FieldLayer firstLayer = definition.layers().get(0);
                PipelineTracer.trace(PipelineTracer.L1_LAYER_ALPHA, 4, "layer", String.valueOf(firstLayer.alpha()));
                PipelineTracer.trace(PipelineTracer.L2_LAYER_VISIBLE, 4, "layer", String.valueOf(firstLayer.visible()));
                PipelineTracer.trace(PipelineTracer.L3_BLEND_MODE, 4, "layer", firstLayer.blendMode() != null ? firstLayer.blendMode().name() : "null");
            }
            
            // CP4: Field-level segments (D1-D4) - trace unconditionally
            if (mods != null) {
                PipelineTracer.trace(PipelineTracer.D1_BOBBING, 4, "def", String.valueOf(mods.bobbing()));
                PipelineTracer.trace(PipelineTracer.D2_BREATHING, 4, "def", String.valueOf(mods.breathing()));
            }
            // Note: Follow affects field POSITIONING (before render),
            // not the rendering itself. It's used by the field tracking system to 
            // calculate the `position` parameter passed to this method.
            if (definition.follow() != null && definition.follow().enabled()) {
                var f = definition.follow();
                PipelineTracer.trace(PipelineTracer.D3_PREDICTION, 4, "def", "leadOffset=" + f.leadOffset());
                PipelineTracer.trace(PipelineTracer.D4_FOLLOW_MODE, 4, "def", "responsiveness=" + f.responsiveness());
                // Mark as complete - affects position calculation, not render
                PipelineTracer.trace(PipelineTracer.D3_PREDICTION, 7, "position", "complete");
                PipelineTracer.trace(PipelineTracer.D4_FOLLOW_MODE, 7, "position", "complete");
            }
            
            // CP4-7: Beam segments (rendered separately if enabled)
            if (definition.beam() != null) {
                var b = definition.beam();
                PipelineTracer.trace(PipelineTracer.B1_BEAM_ENABLED, 4, "def", String.valueOf(b.enabled()));
                PipelineTracer.trace(PipelineTracer.B2_BEAM_INNER_RADIUS, 4, "def", String.valueOf(b.innerRadius()));
                PipelineTracer.trace(PipelineTracer.B3_BEAM_OUTER_RADIUS, 4, "def", String.valueOf(b.outerRadius()));
                PipelineTracer.trace(PipelineTracer.B4_BEAM_COLOR, 4, "def", b.color());
                PipelineTracer.trace(PipelineTracer.B5_BEAM_HEIGHT, 4, "def", String.valueOf(b.height()));
                PipelineTracer.trace(PipelineTracer.B6_BEAM_GLOW, 4, "def", String.valueOf(b.glow()));
                PipelineTracer.trace(PipelineTracer.B7_BEAM_PULSE, 4, "def", b.pulse() != null ? "active" : "null");
                
                // Actually render the beam (CP5-7 traces are inside BeamRenderer)
                if (b.enabled()) {
                    scope.leaf("beam: rendering");
                    // Beam needs world time and tick delta for scrolling animation
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    long worldTime = client.world != null ? client.world.getTime() : 0L;
                    float tickDelta = client.getRenderTickCounter().getTickProgress(false);
                    BeamRenderer.render(matrices, consumers, b, resolver, worldTime, tickDelta, alpha);
                }
            }
        
            int layerCount = 0;
            // Inner scope auto-nests into outer scope
            try (LogScope layerScope = Logging.FIELD.topic("render").scope("process-layers", LogLevel.TRACE)) {
                for (FieldLayer layer : definition.layers()) {
                    layerScope.branch("layer").kv("index", layerCount).kv("id", layer.id());

                    LayerRenderer.render(
                        matrices,
                        consumers,
                        layer,
                        resolver,
                        scale,
                        time,
                        alpha,
                        overrides
                    );

                    // CP5-7: Layer & Field-level segments rendered (trace once for first layer)
                    if (layerCount == 0) {
                        PipelineTracer.trace(PipelineTracer.L1_LAYER_ALPHA, 5, "render", "passed");
                        PipelineTracer.trace(PipelineTracer.L2_LAYER_VISIBLE, 5, "render", "passed");
                        PipelineTracer.trace(PipelineTracer.L3_BLEND_MODE, 5, "render", "passed");
                        PipelineTracer.trace(PipelineTracer.L1_LAYER_ALPHA, 6, "vtx", "emitted");
                        PipelineTracer.trace(PipelineTracer.L2_LAYER_VISIBLE, 6, "vtx", "emitted");
                        PipelineTracer.trace(PipelineTracer.L3_BLEND_MODE, 6, "vtx", "emitted");
                        PipelineTracer.trace(PipelineTracer.L1_LAYER_ALPHA, 7, "vtx", "complete");
                        PipelineTracer.trace(PipelineTracer.L2_LAYER_VISIBLE, 7, "vtx", "complete");
                        PipelineTracer.trace(PipelineTracer.L3_BLEND_MODE, 7, "vtx", "complete");
                        // D1-D4 complete when layer renders
                        PipelineTracer.trace(PipelineTracer.D1_BOBBING, 5, "render", "applied");
                        PipelineTracer.trace(PipelineTracer.D2_BREATHING, 5, "render", "applied");
                        PipelineTracer.trace(PipelineTracer.D1_BOBBING, 6, "vtx", "matrix");
                        PipelineTracer.trace(PipelineTracer.D2_BREATHING, 6, "vtx", "matrix");
                        PipelineTracer.trace(PipelineTracer.D1_BOBBING, 7, "vtx", "complete");
                        PipelineTracer.trace(PipelineTracer.D2_BREATHING, 7, "vtx", "complete");
                    }

                    layerCount++;
                }
            }
            
            scope.count("layers-rendered", layerCount);
            } finally {
                matrices.pop();
            }
        } // End outer LogScope - emits single batched log
    }
    
    // =========================================================================
    // Theme Resolution
    // =========================================================================
    
    /**
     * Resolves the color theme for a field definition.
     */
    private static ColorTheme resolveTheme(FieldDefinition definition) {
        // Try definition's explicit theme
        String themeId = definition.themeId();
        if (themeId != null && !themeId.isEmpty()) {
            ColorTheme theme = ColorThemeRegistry.get(themeId);
            if (theme != null) {
                return theme;
            }
            Logging.FIELD.topic("render").trace(
                "Theme '{}' not found, using default", themeId);
        }
        
        // Fall back to default theme
        return ColorThemeRegistry.getOrDefault("cyber_green", ColorTheme.CYBER_GREEN);
    }
    
    // =========================================================================
    // Static Utilities
    // =========================================================================
    
    /**
     * Checks if a definition can be rendered.
     */
    public static boolean canRender(FieldDefinition definition) {
        return definition != null 
            && definition.layers() != null 
            && !definition.layers().isEmpty();
    }
}

