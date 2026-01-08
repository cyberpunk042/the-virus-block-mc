package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.gui.state.PipelineTracer;
import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.LinkResolver;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.ScopeNode;
import net.cyberpunk042.log.LogLevel;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.transform.Anchor;
import net.cyberpunk042.visual.transform.Billboard;
import net.cyberpunk042.visual.transform.Facing;
import net.cyberpunk042.visual.transform.Transform;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import net.cyberpunk042.client.visual.render.FieldRenderLayers;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Renders a single layer of a field definition.
 * 
 * <p>Per CLASS_DIAGRAM §8: LayerRenderer applies layer-level transforms
 * and animations, then delegates to appropriate PrimitiveRenderer for
 * each primitive in the layer.
 * 
 * <h2>Rendering Flow</h2>
 * <pre>
 * LayerRenderer.render(layer, ...)
 *    │
 *    ├── Apply layer transform (position, rotation, scale)
 *    ├── Apply layer animation (AnimationApplier)
 *    │
 *    └── for each primitive:
 *           └── PrimitiveRenderers.get(type).render(primitive, ...)
 * </pre>
 * 
 * <h2>Layer Visibility</h2>
 * <p>Layers have a {@code visible} flag. If false, the layer is skipped entirely.</p>
 * 
 * @see FieldRenderer
 * @see PrimitiveRenderer
 * @see AnimationApplier
 */
public final class LayerRenderer {
    
    private LayerRenderer() {}
    
    /**
     * Cache for primitive positions during rendering.
     * Maps primitive ID -> computed world offset (including orbit displacement).
     * Used for followDynamic linking where a primitive follows another's animated position.
     * 
     * <p>Thread-local to avoid concurrency issues with multiple render threads.</p>
     */
    private static final ThreadLocal<java.util.Map<String, Vector3f>> positionCache = 
        ThreadLocal.withInitial(java.util.HashMap::new);
    
    /**
     * Clears the position cache. Should be called at the start of each layer render.
     */
    private static void clearPositionCache() {
        positionCache.get().clear();
    }
    
    /**
     * Stores a primitive's computed position in the cache.
     */
    private static void cachePosition(String primitiveId, Vector3f position) {
        positionCache.get().put(primitiveId, new Vector3f(position));
    }
    
    /**
     * Gets a cached primitive position.
     * @return cached position or null if not found
     */
    private static Vector3f getCachedPosition(String primitiveId) {
        return positionCache.get().get(primitiveId);
    }
    
    /**
     * Renders all primitives in a layer.
     * 
     * @param matrices Matrix stack (positioned at field origin)
     * @param consumer Vertex consumer
     * @param layer The layer to render
     * @param resolver Color resolver for theme
     * @param fieldScale Overall field scale
     * @param time Animation time
     * @param alpha Overall alpha
     * @param overrides Optional render overrides
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldLayer layer,
            ColorResolver resolver,
            float fieldScale,
            float time,
            float alpha,
            RenderOverrides overrides) {
        
        // Use LogScope for batched output instead of per-iteration spam
        try (LogScope scope = Logging.FIELD.topic("render").scope("layer:" + layer.id(), LogLevel.DEBUG)) {
            scope.kv("visible", layer.visible())
                 .kv("primitives", layer.primitives() != null ? layer.primitives().size() : 0);
            
            // Check visibility
            if (!layer.visible()) {
                scope.leaf("SKIPPED: not visible");
                return;
            }
            
            // Skip empty layers
            if (layer.primitives() == null || layer.primitives().isEmpty()) {
                scope.leaf("SKIPPED: no primitives");
                return;
            }
            
            
            matrices.push();
            try {
            
            // === PHASE 1: Apply Layer Transform ===
            applyLayerTransform(matrices, layer, fieldScale);
            
            // === PHASE 2: Apply Layer Animation ===
            if (layer.animation() != null && layer.animation().isActive()) {
                AnimationApplier.apply(matrices, layer.animation(), time);
                scope.leaf("animation: applied");
            }
            
            // === PHASE 3: Render Each Primitive ===
            int light = 0xF000F0; // Full bright for fields
            float effectiveAlpha = alpha * layer.alpha();
            
            // Build primitive index for link resolution
            Map<String, Primitive> primitiveIndex = LinkResolver.buildIndex(layer.primitives());
            
            // Clear position cache for dynamic follow
            clearPositionCache();
            
            // Check if ANY primitive in this layer needs see-through mode
            boolean anySeeThroughPrimitive = false;
            for (Primitive p : layer.primitives()) {
                FillConfig fill = p.fill();
                if (fill != null && !fill.depthWrite()) {
                    anySeeThroughPrimitive = true;
                    break;
                }
            }
            
            // If any primitive wants see-through, enable it for the whole layer
            if (anySeeThroughPrimitive) {
                // Flush any pending geometry first
                if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                    immediate.draw();
                }
                FieldRenderLayers.disableDepthWrite();
            }
            
            try {
                int primCount = 0;
                for (Primitive primitive : layer.primitives()) {
                    ScopeNode primNode = scope.branch("prim:" + primCount);
                    primNode.kv("type", primitive.type());
                    
                    if (primitive.shape() != null) {
                        primNode.kv("shape", primitive.shape().getClass().getSimpleName());
                    }
                    if (primitive.appearance() != null) {
                        primNode.kv("color", primitive.appearance().color());
                        if (primitive.appearance().alpha() != null) {
                            primNode.kv("alpha", primitive.appearance().alpha().max());
                        }
                    }
                    
                    // Flush buffer before POINTS mode to prevent orphaned vertices
                    FillConfig fill = primitive.fill();
                    FillMode mode = fill != null ? fill.mode() : FillMode.SOLID;
                    if (mode == FillMode.POINTS && consumers instanceof VertexConsumerProvider.Immediate immediate) {
                        immediate.draw();
                    }
                    
                    VertexConsumer consumer = getConsumerForPrimitive(consumers, primitive);
                    renderPrimitive(matrices, consumer, primitive, resolver, light, time, effectiveAlpha, overrides, primitiveIndex);
                    
                    
                    // NOTE: Corona effect is now handled in the combined Fresnel shader
                    // (see getConsumerForPrimitive where both Horizon and Corona params are set)
                    
                    primCount++;
                }
                
                // Flush the layer's geometry
                if (anySeeThroughPrimitive && consumers instanceof VertexConsumerProvider.Immediate immediate) {
                    immediate.draw();
                }
                
                scope.count("rendered", primCount);
            } finally {
                // Restore depth write if we changed it
                if (anySeeThroughPrimitive) {
                    FieldRenderLayers.enableDepthWrite();
                }
            }
            } finally {
                matrices.pop();
            }
        } // Auto-emits single tree output here
    }
    
    /**
     * Convenience method without overrides.
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            FieldLayer layer,
            ColorResolver resolver,
            float fieldScale,
            float time,
            float alpha) {
        render(matrices, consumers, layer, resolver, fieldScale, time, alpha, null);
    }
    
    /**
     * Gets the appropriate VertexConsumer for a primitive based on its fill mode.
     * 
     * - SOLID → solidTranslucent, solidTranslucentNoDepth (if depthWrite=false), or NoCull (if doubleSided)
     * - WIREFRAME, CAGE → lines with custom thickness via RenderPhase.LineWidth
     * - POINTS → solidTranslucent (uses tiny quads)
     * - RaysShape → Always lines, uses rayWidth to select thin/thick (rayWidth < 1.0 = thin, >= 1.0 = thick)
     */
    private static VertexConsumer getConsumerForPrimitive(VertexConsumerProvider consumers, Primitive primitive) {
        FillConfig fill = primitive.fill();
        FillMode mode = fill != null ? fill.mode() : FillMode.SOLID;
        boolean doubleSided = fill != null && fill.doubleSided();
        boolean depthWrite = fill == null || fill.depthWrite();
        float wireThickness = fill != null ? fill.wireThickness() : 1.0f;
        
        // Special case: RaysShape - use LINES for 2D rays, respect fill mode for 3D rays
        if (primitive.shape() instanceof net.cyberpunk042.visual.shape.RaysShape rays) {
            // Check if this is a 3D ray type with proper tessellator
            net.cyberpunk042.visual.shape.RayType rayType = rays.effectiveRayType();
            boolean is3DRay = rayType.is3D() && 
                net.cyberpunk042.client.visual.mesh.ray.RayTypeTessellatorRegistry.isImplemented(rayType);
            
            if (is3DRay) {
                // 3D ray types respect fill mode like other shapes
                return switch (mode) {
                    case WIREFRAME, CAGE -> consumers.getBuffer(FieldRenderLayers.lines(wireThickness));
                    case SOLID, POINTS -> {
                        if (doubleSided) {
                            yield consumers.getBuffer(FieldRenderLayers.solidTranslucentNoCull());
                        } else {
                            yield consumers.getBuffer(FieldRenderLayers.solidTranslucent());
                        }
                    }
                };
            } else {
                // 2D ray types use line rendering
                float thickness = rays.rayWidth();
                return consumers.getBuffer(FieldRenderLayers.lines(thickness));
            }
        }
        
        return switch (mode) {
            case WIREFRAME, CAGE -> consumers.getBuffer(FieldRenderLayers.lines(wireThickness));
            case SOLID, POINTS -> {
                // Check if this is a sphere with ANY shader effect (Horizon OR Corona)
                if (primitive.shape() instanceof net.cyberpunk042.visual.shape.SphereShape sphere 
                        && (sphere.hasHorizonEffect() || sphere.hasCoronaEffect())) {
                    // Use Fresnel shader for rim lighting and/or corona glow
                    // Set Horizon params (enabled or disabled - shader handles both)
                    net.cyberpunk042.visual.effect.HorizonEffect horizonEffect = sphere.toHorizonEffect();
                    net.cyberpunk042.client.visual.shader.CustomUniformBinder.setHorizonParams(horizonEffect);
                    
                    // Set Corona params (enabled or disabled - shader handles both)
                    net.cyberpunk042.visual.effect.CoronaEffect coronaEffect = sphere.toCoronaEffect();
                    net.cyberpunk042.client.visual.shader.CustomUniformBinder.setCoronaParams(coronaEffect);
                    
                    RenderLayer fresnelLayer = doubleSided 
                        ? net.cyberpunk042.client.visual.shader.FresnelRenderLayers.fresnelTranslucentNoCull()
                        : net.cyberpunk042.client.visual.shader.FresnelRenderLayers.fresnelTranslucent();
                    yield consumers.getBuffer(fresnelLayer);
                }
                
                // Standard layer selection
                RenderLayer layer;
                if (!depthWrite) {
                    // No depth write = true transparency (see-through)
                    layer = FieldRenderLayers.solidTranslucentNoDepth();
                } else if (doubleSided) {
                    layer = FieldRenderLayers.solidTranslucentNoCull();
                } else {
                    layer = FieldRenderLayers.solidTranslucent();
                }
                yield consumers.getBuffer(layer);
            }
        };
    }
    
    // =========================================================================
    // Transform Application
    // =========================================================================
    
    /**
     * Applies layer transform to the matrix stack.
     * 
     * <p>Transform order: translate → rotate → scale
     */
    private static void applyLayerTransform(MatrixStack matrices, FieldLayer layer, float fieldScale) {
        Transform transform = layer.transform();
        if (transform == null || transform == Transform.IDENTITY) {
            return;
        }
        
        // 1. Translation (offset)
        if (transform.offset() != null) {
            matrices.translate(
                transform.offset().x * fieldScale,
                transform.offset().y * fieldScale,
                transform.offset().z * fieldScale
            );
        }
        
        // 2. Rotation
        if (transform.rotation() != null) {
            Quaternionf rotation = new Quaternionf()
                .rotateX((float) Math.toRadians(transform.rotation().x))
                .rotateY((float) Math.toRadians(transform.rotation().y))
                .rotateZ((float) Math.toRadians(transform.rotation().z));
            matrices.multiply(rotation);
        }
        
        // 3. Scale
        float scaleX = (transform.scaleXYZ() != null ? transform.scaleXYZ().x : transform.scale()) * fieldScale;
        float scaleY = (transform.scaleXYZ() != null ? transform.scaleXYZ().y : transform.scale()) * fieldScale;
        float scaleZ = (transform.scaleXYZ() != null ? transform.scaleXYZ().z : transform.scale()) * fieldScale;
        matrices.scale(scaleX, scaleY, scaleZ);
        
        Logging.FIELD.topic("render").trace(
            "Applied transform: offset={}, rotation={}, scale=({}, {}, {})",
            transform.offset(), transform.rotation(), scaleX, scaleY, scaleZ);
    }
    
    // =========================================================================
    // Primitive Rendering
    // =========================================================================
    
    /**
     * Renders a single primitive with its own transform and animation.
     * 
     * @param primitiveIndex Index of all primitives in the layer for link resolution
     */
    private static void renderPrimitive(
            MatrixStack matrices,
            VertexConsumer consumer,
            Primitive primitive,
            ColorResolver resolver,
            int light,
            float time,
            float alpha,
            RenderOverrides overrides,
            Map<String, Primitive> primitiveIndex) {
        
        matrices.push();
        try {
        
        // === LINK RESOLUTION ===
        // Resolve links first - may provide orbit config, color, alpha, etc.
        LinkResolver.ResolvedValues resolvedLinks = LinkResolver.resolveLinks(primitive, primitiveIndex);
        
        // Calculate effective time with orbit phase offset from links
        float orbitTime = time;
        if (resolvedLinks.hasOrbitPhaseOffset()) {
            // orbitPhaseOffset is 0-1 (representing 0-360°)
            // At frequency=1, one cycle = 20 ticks
            // So orbitPhaseOffset * 20 gives us the tick offset for freq=1
            // Example: 0.5 (180°) = 10 ticks, 0.983 (354°) = 19.67 ticks
            orbitTime = time + resolvedLinks.orbitPhaseOffset() * 20f;
        }
        
        // Track this primitive's computed position for caching
        Vector3f computedPosition = new Vector3f(0, 0, 0);
        
        // === DYNAMIC FOLLOW (follows target's animated position) ===
        if (resolvedLinks.hasFollowDynamic()) {
            // Get the target primitive's ID from the link
            PrimitiveLink link = primitive.link();
            if (link != null && link.target() != null) {
                Vector3f targetPos = getCachedPosition(link.target());
                if (targetPos != null) {
                    matrices.translate(targetPos.x, targetPos.y, targetPos.z);
                    computedPosition.add(targetPos);
                    Logging.ANIMATION.topic("link").trace(
                        "'{}' dynamic follow '{}' -> ({}, {}, {})", 
                        primitive.id(), link.target(), targetPos.x, targetPos.y, targetPos.z);
                }
            }
        }
        
        // === STATIC FOLLOW (follows target's base offset) ===
        if (resolvedLinks.hasOffset()) {
            Vector3f followOffset = resolvedLinks.offset();
            matrices.translate(followOffset.x, followOffset.y, followOffset.z);
            computedPosition.add(followOffset);
        }
        
        // Apply scale from linked primitive
        if (resolvedLinks.hasScale()) {
            float linkedScale = resolvedLinks.scale();
            matrices.scale(linkedScale, linkedScale, linkedScale);
        }
        
        // Apply color/alpha overrides from links to the renderer overrides
        RenderOverrides effectiveOverrides = overrides;
        if (resolvedLinks.hasColor() || resolvedLinks.hasAlpha()) {
            // Build new overrides, copying existing values if present
            RenderOverrides.Builder ovBuilder = RenderOverrides.builder();
            
            // Copy existing pattern if present
            if (overrides != null && overrides.vertexPattern() != null) {
                ovBuilder.vertexPattern(overrides.vertexPattern());
            }
            
            // Color from link or existing
            if (resolvedLinks.hasColor()) {
                // Parse color string to int (e.g., "#FF0000" or "0xFF0000")
                String colorStr = resolvedLinks.color();
                int colorInt = parseColorString(colorStr);
                ovBuilder.colorOverride(colorInt);
            } else if (overrides != null && overrides.colorOverride() != null) {
                ovBuilder.colorOverride(overrides.colorOverride());
            }
            
            // Alpha from link or existing
            if (resolvedLinks.hasAlpha()) {
                ovBuilder.alphaMultiplier(resolvedLinks.alpha());
            } else if (overrides != null) {
                ovBuilder.alphaMultiplier(overrides.alphaMultiplier());
            }
            
            // Scale from existing
            if (overrides != null) {
                ovBuilder.scaleMultiplier(overrides.scaleMultiplier());
            }
            
            effectiveOverrides = ovBuilder.build();
        }
        
        // Apply primitive transform (with resolved orbit if linked)
        Transform transform = primitive.transform();
        if (transform != null && transform != Transform.IDENTITY) {
            // CP5: ALL transform segments applied
            PipelineTracer.trace(PipelineTracer.T1_OFFSET, 5, "render", transform.offset() != null ? transform.offset().toString() : "null");
            PipelineTracer.trace(PipelineTracer.T2_ROTATION, 5, "render", transform.rotation() != null ? transform.rotation().toString() : "null");
            PipelineTracer.trace(PipelineTracer.T3_SCALE, 5, "render", String.valueOf(transform.scale()));
            PipelineTracer.trace(PipelineTracer.T4_SCALE_XYZ, 5, "render", transform.scaleXYZ() != null ? transform.scaleXYZ().toString() : "null");
            PipelineTracer.trace(PipelineTracer.T5_ANCHOR, 5, "render", transform.anchor() != null ? transform.anchor().name() : "null");
            PipelineTracer.trace(PipelineTracer.T6_BILLBOARD, 5, "render", transform.billboard() != null ? transform.billboard().name() : "null");
            PipelineTracer.trace(PipelineTracer.T7_ORBIT, 5, "render", transform.orbit() != null ? "active" : "null");
            
            // Calculate orbit offset for position caching
            if (resolvedLinks.hasOrbit()) {
                // Linked orbit
                var orbitOffset = resolvedLinks.orbitConfig().getOffset(orbitTime);
                computedPosition.add(orbitOffset);
            } else if (transform.hasOrbit3D()) {
                // Own 3D orbit
                var orbitOffset = transform.orbit3d().getOffset(orbitTime);
                computedPosition.add(orbitOffset);
            } else if (transform.orbit() != null && transform.orbit().isActive()) {
                // Legacy orbit
                var orbit = transform.orbit();
                float angle = orbitTime * orbit.speed();
                float orbitX = 0, orbitY = 0, orbitZ = 0;
                switch (orbit.axis()) {
                    case X -> { orbitY = orbit.radius() * (float) Math.cos(angle + orbit.phase()); orbitZ = orbit.radius() * (float) Math.sin(angle + orbit.phase()); }
                    case Z -> { orbitX = orbit.radius() * (float) Math.cos(angle + orbit.phase()); orbitY = orbit.radius() * (float) Math.sin(angle + orbit.phase()); }
                    default -> { orbitX = orbit.radius() * (float) Math.cos(angle + orbit.phase()); orbitZ = orbit.radius() * (float) Math.sin(angle + orbit.phase()); }
                }
                computedPosition.add(orbitX, orbitY, orbitZ);
            }
            
            // Add static offset
            if (transform.offset() != null) {
                computedPosition.add(transform.offset());
            }
            
            // If we have a resolved orbit from link, apply it with the phase-adjusted time
            if (resolvedLinks.hasOrbit()) {
                // Use linked orbit config instead of primitive's own
                applyPrimitiveTransformWithLinkedOrbit(matrices, transform, resolvedLinks.orbitConfig(), orbitTime);
            } else {
                applyPrimitiveTransform(matrices, transform, orbitTime);
            }
            
            // CP6: ALL transform segments in matrix
            PipelineTracer.trace(PipelineTracer.T1_OFFSET, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.T2_ROTATION, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.T3_SCALE, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.T4_SCALE_XYZ, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.T5_ANCHOR, 6, "matrix", transform.anchor() != Anchor.CENTER ? "applied" : "default");
            PipelineTracer.trace(PipelineTracer.T6_BILLBOARD, 6, "matrix", transform.billboard() != Billboard.NONE ? "applied" : "none");
            PipelineTracer.trace(PipelineTracer.T7_ORBIT, 6, "matrix", transform.orbit() != null && transform.orbit().isActive() ? "applied" : "n/a");
            PipelineTracer.trace(PipelineTracer.T10_FACING, 5, "render", transform.facing() != null ? transform.facing().name() : "null");
            PipelineTracer.trace(PipelineTracer.T10_FACING, 6, "matrix", transform.facing() != Facing.FIXED ? "applied" : "fixed");
        }
        
        // Cache this primitive's computed position for dynamic follow by other primitives
        if (primitive.id() != null) {
            cachePosition(primitive.id(), computedPosition);
        }
        
        // F184: Apply primitive animation with link phase offset
        var anim = primitive.animation();
        
        if (anim != null && anim.isActive()) {
            float effectiveTime = time + getLinkPhaseOffset(primitive);
            
            // CP5: ALL animation segments applied
            if (anim.spin() != null) {
                PipelineTracer.trace(PipelineTracer.N1_SPIN_SPEED, 5, "render.Y", String.valueOf(anim.spin().speedY()));
            }
            if (anim.pulse() != null) {
                PipelineTracer.trace(PipelineTracer.N3_PULSE_SPEED, 5, "render", String.valueOf(anim.pulse().speed()));
                PipelineTracer.trace(PipelineTracer.N4_PULSE_SCALE, 5, "render", String.valueOf(anim.pulse().scale()));
                PipelineTracer.trace(PipelineTracer.N5_PULSE_MODE, 5, "render", anim.pulse().mode().name());
            }
            if (anim.alphaPulse() != null) {
                PipelineTracer.trace(PipelineTracer.N6_ALPHA_PULSE_SPEED, 5, "render", String.valueOf(anim.alphaPulse().speed()));
                PipelineTracer.trace(PipelineTracer.N7_ALPHA_PULSE_MIN, 5, "render", String.valueOf(anim.alphaPulse().min()));
                PipelineTracer.trace(PipelineTracer.N8_ALPHA_PULSE_MAX, 5, "render", String.valueOf(anim.alphaPulse().max()));
            }
            if (anim.wave() != null) {
                PipelineTracer.trace(PipelineTracer.N9_WAVE_SPEED, 5, "render", String.valueOf(anim.wave().frequency()));
                PipelineTracer.trace(PipelineTracer.N10_WAVE_AMPLITUDE, 5, "render", String.valueOf(anim.wave().amplitude()));
            }
            if (anim.wobble() != null) {
                PipelineTracer.trace(PipelineTracer.N11_WOBBLE_SPEED, 5, "render", String.valueOf(anim.wobble().speed()));
            }
            if (anim.colorCycle() != null && anim.colorCycle().isActive()) {
                PipelineTracer.trace(PipelineTracer.N12_COLOR_CYCLE, 5, "render", "active");
            }
            
            AnimationApplier.apply(matrices, anim, effectiveTime);
            
            // CP6: ALL animation segments in matrix
            PipelineTracer.trace(PipelineTracer.N1_SPIN_SPEED, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.N2_SPIN_AXIS, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.N3_PULSE_SPEED, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.N4_PULSE_SCALE, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.N5_PULSE_MODE, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.N6_ALPHA_PULSE_SPEED, 6, "alpha", "applied");
            PipelineTracer.trace(PipelineTracer.N7_ALPHA_PULSE_MIN, 6, "alpha", "applied");
            PipelineTracer.trace(PipelineTracer.N8_ALPHA_PULSE_MAX, 6, "alpha", "applied");
            PipelineTracer.trace(PipelineTracer.N9_WAVE_SPEED, 6, "wave", "passed");
            PipelineTracer.trace(PipelineTracer.N10_WAVE_AMPLITUDE, 6, "wave", "passed");
            PipelineTracer.trace(PipelineTracer.N11_WOBBLE_SPEED, 6, "matrix", "applied");
            PipelineTracer.trace(PipelineTracer.N12_COLOR_CYCLE, 6, "color", "applied");
        }
        
        // CP5: ALL visibility/mask segments
        if (primitive.visibility() != null) {
            var v = primitive.visibility();
            PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 5, "render", v.mask().name());
            PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 5, "render", String.valueOf(v.count()));
            PipelineTracer.trace(PipelineTracer.V3_MASK_THICKNESS, 5, "render", String.valueOf(v.thickness()));
            PipelineTracer.trace(PipelineTracer.V4_MASK_OFFSET, 5, "render", String.valueOf(v.offset()));
            PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 5, "render", String.valueOf(v.animate()));
            PipelineTracer.trace(PipelineTracer.V6_MASK_ANIM_SPEED, 5, "render", String.valueOf(v.animSpeed()));
            // CP6: Visibility passed to primitive renderer
            PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 6, "toRenderer", v.mask().name());
            PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 6, "toRenderer", String.valueOf(v.count()));
            PipelineTracer.trace(PipelineTracer.V3_MASK_THICKNESS, 6, "toRenderer", String.valueOf(v.thickness()));
            PipelineTracer.trace(PipelineTracer.V4_MASK_OFFSET, 6, "toRenderer", String.valueOf(v.offset()));
            PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 6, "toRenderer", String.valueOf(v.animate()));
            PipelineTracer.trace(PipelineTracer.V6_MASK_ANIM_SPEED, 6, "toRenderer", String.valueOf(v.animSpeed()));
        }
        
        // CP5: ALL fill segments
        if (primitive.fill() != null) {
            var f = primitive.fill();
            PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 5, "render", f.mode().name());
            PipelineTracer.trace(PipelineTracer.F2_WIRE_THICKNESS, 5, "render", String.valueOf(f.wireThickness()));
            PipelineTracer.trace(PipelineTracer.F3_DOUBLE_SIDED, 5, "render", String.valueOf(f.doubleSided()));
            PipelineTracer.trace(PipelineTracer.F4_DEPTH_TEST, 5, "render", String.valueOf(f.depthTest()));
            // CP6: Fill passed to primitive renderer
            PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 6, "toRenderer", f.mode().name());
            PipelineTracer.trace(PipelineTracer.F2_WIRE_THICKNESS, 6, "toRenderer", String.valueOf(f.wireThickness()));
            PipelineTracer.trace(PipelineTracer.F3_DOUBLE_SIDED, 6, "toRenderer", String.valueOf(f.doubleSided()));
            PipelineTracer.trace(PipelineTracer.F4_DEPTH_TEST, 6, "toRenderer", String.valueOf(f.depthTest()));
        }
        
        // CP5-CP6: ALL appearance segments before passing to renderer
        if (primitive.appearance() != null) {
            var app = primitive.appearance();
            PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 5, "render", app.color());
            PipelineTracer.trace(PipelineTracer.A2_ALPHA, 5, "render", app.alpha() != null ? String.valueOf(app.alpha().max()) : "1.0");
            PipelineTracer.trace(PipelineTracer.A3_GLOW, 5, "render", String.valueOf(app.glow()));
            PipelineTracer.trace(PipelineTracer.A4_EMISSIVE, 5, "render", String.valueOf(app.emissive()));
            PipelineTracer.trace(PipelineTracer.A5_SATURATION, 5, "render", String.valueOf(app.saturation()));
            PipelineTracer.trace(PipelineTracer.A6_SECONDARY_COLOR, 5, "render", app.secondaryColor());
            // CP6: Appearance passed to primitive renderer
            PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 6, "toRenderer", app.color());
            PipelineTracer.trace(PipelineTracer.A2_ALPHA, 6, "toRenderer", app.alpha() != null ? String.valueOf(app.alpha().max()) : "1.0");
            PipelineTracer.trace(PipelineTracer.A3_GLOW, 6, "toRenderer", String.valueOf(app.glow()));
            PipelineTracer.trace(PipelineTracer.A4_EMISSIVE, 6, "toRenderer", String.valueOf(app.emissive()));
            PipelineTracer.trace(PipelineTracer.A5_SATURATION, 6, "toRenderer", String.valueOf(app.saturation()));
            PipelineTracer.trace(PipelineTracer.A6_SECONDARY_COLOR, 6, "toRenderer", app.secondaryColor());
        }
        
        // CP5-CP6: ALL shape segments before passing to renderer
        if (primitive.shape() != null) {
            var shape = primitive.shape();
            PipelineTracer.trace(PipelineTracer.S1_SHAPE_TYPE, 5, "render", shape.getClass().getSimpleName());
            PipelineTracer.trace(PipelineTracer.S1_SHAPE_TYPE, 6, "toRenderer", shape.getClass().getSimpleName());
            // Shape-specific segments traced in AbstractPrimitiveRenderer.traceShapeAtCP4
            if (shape instanceof net.cyberpunk042.visual.shape.SphereShape s) {
                PipelineTracer.trace(PipelineTracer.S2_RADIUS, 5, "render", String.valueOf(s.radius()));
                PipelineTracer.trace(PipelineTracer.S3_LAT_STEPS, 5, "render", String.valueOf(s.latSteps()));
                PipelineTracer.trace(PipelineTracer.S4_LON_STEPS, 5, "render", String.valueOf(s.lonSteps()));
                PipelineTracer.trace(PipelineTracer.S5_ALGORITHM, 5, "render", s.algorithm().name());
                PipelineTracer.trace(PipelineTracer.S2_RADIUS, 6, "toRenderer", String.valueOf(s.radius()));
                PipelineTracer.trace(PipelineTracer.S3_LAT_STEPS, 6, "toRenderer", String.valueOf(s.latSteps()));
                PipelineTracer.trace(PipelineTracer.S4_LON_STEPS, 6, "toRenderer", String.valueOf(s.lonSteps()));
                PipelineTracer.trace(PipelineTracer.S5_ALGORITHM, 6, "toRenderer", s.algorithm().name());
            } else if (shape instanceof net.cyberpunk042.visual.shape.RingShape r) {
                PipelineTracer.trace(PipelineTracer.S6_INNER_RADIUS, 5, "render", String.valueOf(r.innerRadius()));
                PipelineTracer.trace(PipelineTracer.S7_OUTER_RADIUS, 5, "render", String.valueOf(r.outerRadius()));
                PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 5, "render", String.valueOf(r.height()));
                PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 5, "render", String.valueOf(r.segments()));
                PipelineTracer.trace(PipelineTracer.S6_INNER_RADIUS, 6, "toRenderer", String.valueOf(r.innerRadius()));
                PipelineTracer.trace(PipelineTracer.S7_OUTER_RADIUS, 6, "toRenderer", String.valueOf(r.outerRadius()));
                PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 6, "toRenderer", String.valueOf(r.height()));
                PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 6, "toRenderer", String.valueOf(r.segments()));
            } else if (shape instanceof net.cyberpunk042.visual.shape.CylinderShape c) {
                PipelineTracer.trace(PipelineTracer.S2_RADIUS, 5, "render", String.valueOf(c.radius()));
                PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 5, "render", String.valueOf(c.height()));
                PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 5, "render", String.valueOf(c.segments()));
                PipelineTracer.trace(PipelineTracer.S2_RADIUS, 6, "toRenderer", String.valueOf(c.radius()));
                PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 6, "toRenderer", String.valueOf(c.height()));
                PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 6, "toRenderer", String.valueOf(c.segments()));
            } else if (shape instanceof net.cyberpunk042.visual.shape.PrismShape p) {
                PipelineTracer.trace(PipelineTracer.S2_RADIUS, 5, "render", String.valueOf(p.radius()));
                PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 5, "render", String.valueOf(p.height()));
                PipelineTracer.trace(PipelineTracer.S10_SIDES, 5, "render", String.valueOf(p.sides()));
                PipelineTracer.trace(PipelineTracer.S2_RADIUS, 6, "toRenderer", String.valueOf(p.radius()));
                PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 6, "toRenderer", String.valueOf(p.height()));
                PipelineTracer.trace(PipelineTracer.S10_SIDES, 6, "toRenderer", String.valueOf(p.sides()));
            }
        }
        
        // NOTE: Primitive-level alpha is handled in AbstractPrimitiveRenderer.resolveColor()
        // We only pass field/layer-level alpha through overrides here
        // DO NOT apply primitive.appearance().alpha() here - it would be double-applied!
        
        // Get renderer and render
        PrimitiveRenderer renderer = PrimitiveRenderers.get(primitive);
        if (renderer != null) {
            // Pass through effective overrides (includes link color/alpha)
            renderer.render(primitive, matrices, consumer, light, time, resolver, effectiveOverrides);
        } else {
            Logging.FIELD.topic("render")
                .reason("Missing renderer")
                .warn("No renderer for primitive type: {}", primitive.type());
        }
        } finally {
            matrices.pop();
        }
    }
    
    /**
     * Applies primitive-level transform.
     */
    private static void applyPrimitiveTransform(MatrixStack matrices, Transform transform) {
        applyPrimitiveTransform(matrices, transform, 0f);
    }
    
    /**
     * Applies primitive-level transform with time for orbit animation.
     */
    private static void applyPrimitiveTransform(MatrixStack matrices, Transform transform, float time) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        
        // 1. Anchor offset (applied first - positions relative to player center)
        if (transform.anchor() != null) {
            var anchor = transform.anchor();
            matrices.translate(anchor.getX(), anchor.getY(), anchor.getZ());
        }
        
        // 2. Orbit (apply before offset, so primitive orbits around anchor position)
        // Priority: orbit3d (new) > orbit (legacy)
        if (transform.hasOrbit3D()) {
            // New 3D orbit system
            var orbit3d = transform.orbit3d();
            var offset = orbit3d.getOffset(time);
            matrices.translate(offset.x, offset.y, offset.z);
        } else if (transform.orbit() != null && transform.orbit().isActive()) {
            // Legacy single-axis orbit
            var orbit = transform.orbit();
            float angle = time * orbit.speed();
            float orbitX, orbitY, orbitZ;
            
            switch (orbit.axis()) {
                case X -> {
                    // Orbit in YZ plane around X axis
                    orbitY = orbit.radius() * (float) Math.cos(angle + orbit.phase());
                    orbitZ = orbit.radius() * (float) Math.sin(angle + orbit.phase());
                    orbitX = 0;
                }
                case Z -> {
                    // Orbit in XY plane around Z axis
                    orbitX = orbit.radius() * (float) Math.cos(angle + orbit.phase());
                    orbitY = orbit.radius() * (float) Math.sin(angle + orbit.phase());
                    orbitZ = 0;
                }
                default -> { // Y axis (most common)
                    // Orbit in XZ plane around Y axis
                    orbitX = orbit.radius() * (float) Math.cos(angle + orbit.phase());
                    orbitZ = orbit.radius() * (float) Math.sin(angle + orbit.phase());
                    orbitY = 0;
                }
            }
            matrices.translate(orbitX, orbitY, orbitZ);
        }

        
        // 3. Translation (offset from anchor/orbit position)
        if (transform.offset() != null) {
            matrices.translate(
                transform.offset().x,
                transform.offset().y,
                transform.offset().z
            );
        }
        
        // 4. Facing - rotate based on player direction/camera
        if (transform.facing() != null && transform.facing() != Facing.FIXED) {
            applyFacing(matrices, transform.facing(), client);
        }
        
        // 5. Rotation (explicit rotation, applied after facing)
        if (transform.rotation() != null) {
            Quaternionf rotation = new Quaternionf()
                .rotateX((float) Math.toRadians(transform.rotation().x))
                .rotateY((float) Math.toRadians(transform.rotation().y))
                .rotateZ((float) Math.toRadians(transform.rotation().z));
            matrices.multiply(rotation);
        }
        
        // 6. Scale
        if (transform.scaleXYZ() != null) {
            matrices.scale(transform.scaleXYZ().x, transform.scaleXYZ().y, transform.scaleXYZ().z);
        } else {
            matrices.scale(transform.scale(), transform.scale(), transform.scale());
        }
        
        // 7. Billboard - rotate to face camera (applied last for correct orientation)
        if (transform.billboard() != null && transform.billboard() != Billboard.NONE) {
            applyBillboard(matrices, transform.billboard(), client);
        }
    }
    
    /**
     * Applies primitive-level transform using a LINKED orbit config from another primitive.
     * This is used for orbitSync linking where multiple primitives share the same orbit pattern
     * but with different phase offsets (like electrons around an atom).
     * 
     * @param matrices Matrix stack to apply transforms to
     * @param transform The primitive's own transform (for non-orbit properties)
     * @param linkedOrbit The orbit config from the linked primitive
     * @param time Time value (already adjusted for orbit phase offset)
     */
    private static void applyPrimitiveTransformWithLinkedOrbit(
            MatrixStack matrices, 
            Transform transform, 
            net.cyberpunk042.visual.transform.OrbitConfig3D linkedOrbit, 
            float time) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        
        // 1. Anchor offset (applied first - positions relative to player center)
        if (transform.anchor() != null) {
            var anchor = transform.anchor();
            matrices.translate(anchor.getX(), anchor.getY(), anchor.getZ());
        }
        
        // 2. Apply LINKED orbit (from orbitSync target)
        if (linkedOrbit != null && linkedOrbit.isActive()) {
            var offset = linkedOrbit.getOffset(time);
            matrices.translate(offset.x, offset.y, offset.z);
            Logging.FIELD.topic("link").trace("Applied linked orbit: offset={}", offset);
        }
        
        // 3. Translation (offset from anchor/orbit position)
        if (transform.offset() != null) {
            matrices.translate(
                transform.offset().x,
                transform.offset().y,
                transform.offset().z
            );
        }
        
        // 4. Facing - rotate based on player direction/camera
        if (transform.facing() != null && transform.facing() != Facing.FIXED) {
            applyFacing(matrices, transform.facing(), client);
        }
        
        // 5. Rotation (explicit rotation, applied after facing)
        if (transform.rotation() != null) {
            Quaternionf rotation = new Quaternionf()
                .rotateX((float) Math.toRadians(transform.rotation().x))
                .rotateY((float) Math.toRadians(transform.rotation().y))
                .rotateZ((float) Math.toRadians(transform.rotation().z));
            matrices.multiply(rotation);
        }
        
        // 6. Scale
        if (transform.scaleXYZ() != null) {
            matrices.scale(transform.scaleXYZ().x, transform.scaleXYZ().y, transform.scaleXYZ().z);
        } else {
            matrices.scale(transform.scale(), transform.scale(), transform.scale());
        }
        
        // 7. Billboard - rotate to face camera (applied last for correct orientation)
        if (transform.billboard() != null && transform.billboard() != Billboard.NONE) {
            applyBillboard(matrices, transform.billboard(), client);
        }
    }
    
    /**
     * Applies facing rotation (static directional orientation).
     * Facing makes the primitive orient in a specific direction (TOP, FRONT, BACK, etc.).
     */
    private static void applyFacing(MatrixStack matrices, Facing facing, net.minecraft.client.MinecraftClient client) {
        if (facing == Facing.FIXED || facing == Facing.TOP) {
            // FIXED and TOP are default orientation - no rotation needed
            return;
        }
        
        // Apply rotations from the Facing enum
        if (facing.pitch() != 0) {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(facing.pitch()));
        }
        if (facing.yaw() != 0) {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(facing.yaw()));
        }
        if (facing.roll() != 0) {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(facing.roll()));
        }
    }
    
    /**
     * Applies billboard rotation to face camera.
     * Billboard makes the primitive always face the camera.
     */
    private static void applyBillboard(MatrixStack matrices, Billboard billboard, net.minecraft.client.MinecraftClient client) {
        if (client.gameRenderer == null || client.gameRenderer.getCamera() == null) return;
        
        var camera = client.gameRenderer.getCamera();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        
        switch (billboard) {
            case FULL -> {
                // Full billboard - face camera completely
                // Add 180 to yaw to face TOWARDS camera (not away from)
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));
                // Negate pitch so looking down makes object tilt up to face camera
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-pitch));
            }
            case Y_AXIS -> {
                // Y-axis billboard - only rotate around Y (useful for trees, vertical sprites)
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));
            }
            case NONE -> {} // No billboard
        }
    }
    
    // =========================================================================
    // F184: Link Resolution
    // =========================================================================
    
    /**
     * Gets the phase offset from a primitive's link configuration.
     * 
     * <p>F184: Primitives can specify a phase offset relative to a linked
     * primitive. This offset is added to the animation time to create
     * synchronized or offset animations between linked primitives.
     * 
     * @param primitive The primitive to check
     * @return Phase offset in ticks (0 if no link or no phase offset)
     */
    private static float getLinkPhaseOffset(Primitive primitive) {
        PrimitiveLink link = primitive.link();
        if (link == null) {
            return 0;
        }
        return LinkResolver.resolvePhaseOffset(link);
    }
    
    /**
     * Parses a color string to an integer.
     * Supports formats: "#RRGGBB", "#AARRGGBB", "0xRRGGBB", "0xAARRGGBB", or plain hex.
     */
    private static int parseColorString(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return 0xFFFFFFFF; // Default white
        }
        
        String hex = colorStr.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        
        try {
            // If 6 chars, assume RGB and add FF alpha
            if (hex.length() == 6) {
                return (int) Long.parseLong("FF" + hex, 16);
            }
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }
}

