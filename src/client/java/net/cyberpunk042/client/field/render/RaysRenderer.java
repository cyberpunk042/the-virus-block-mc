package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.field.render.effect.RenderEffectChain;
import net.cyberpunk042.client.field.render.effect.RenderEffectContext;
import net.cyberpunk042.client.field.render.effect.RenderMotionEffect;
import net.cyberpunk042.client.field.render.effect.RenderTwistEffect;
import net.cyberpunk042.client.field.render.effect.RenderWiggleEffect;
import net.cyberpunk042.client.field.render.emit.EmitStrategyFactory;
import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.client.visual.animation.WaveDeformer;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.RaysTessellator;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.ColorContext;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.shape.RaysShape;
import net.cyberpunk042.visual.shape.RayType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Renders rays primitives (collections of line segments).
 * 
 * <p>Rays are pure line geometry that support various arrangement patterns:
 * RADIAL, SPHERICAL, PARALLEL, CONVERGING, DIVERGING.</p>
 * 
 * <p>Unlike solid shapes, rays always render as lines regardless of fill mode.
 * SOLID, WIREFRAME, and CAGE all render the same line geometry.</p>
 * 
 * <h3>Supported Features</h3>
 * <ul>
 *   <li>Per-vertex coloring (MESH_GRADIENT, MESH_RAINBOW, RANDOM)</li>
 *   <li>Wave animation</li>
 *   <li>All arrangement modes</li>
 *   <li>Segmented/dashed lines</li>
 *   <li>fadeStart/fadeEnd - per-ray alpha gradient</li>
 *   <li>Ray Flow animation (Length, Travel, Flicker)</li>
 *   <li>Ray Motion animation (Linear, Oscillate, Spiral, Ripple)</li>
 * </ul>
 * 
 * @see RaysShape
 * @see RaysTessellator
 * @see RayFlowConfig
 * @see RayMotionConfig
 */
public final class RaysRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "rays";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof RaysShape shape)) {
            return null;
        }
        
        Logging.RENDER.topic("tessellate").debug(
            "[RAYS] Tessellating: count={}, arrangement={}, layers={}, wave={}",
            shape.count(), shape.arrangement(), shape.layers(), wave != null && wave.isActive());
        
        // Resolve pattern from arrangement config (same as SphereRenderer)
        net.cyberpunk042.visual.pattern.VertexPattern pattern = null;
        net.cyberpunk042.visual.pattern.ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("main", shape.primaryCellType());
            Logging.RENDER.topic("tessellate").debug(
                "[RAYS] Pattern resolved: {} for CellType={}", 
                pattern != null ? pattern.getClass().getSimpleName() : "null",
                shape.primaryCellType());
        }
        
        // Get visibility mask (same as SphereRenderer)
        net.cyberpunk042.visual.visibility.VisibilityMask visibility = primitive.visibility();
        
        // Get flow config for 3D ray animations
        Animation anim = primitive.animation();
        RayFlowConfig flowConfig = anim != null ? anim.rayFlow() : null;
        
        return RaysTessellator.tessellate(shape, pattern, visibility, wave, time, flowConfig);
    }
    
    /**
     * Override render to handle rays specially with fadeStart/fadeEnd support
     * and ray-specific animations (RayFlowConfig, RayMotionConfig).
     */
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver resolver,
            RenderOverrides overrides) {
        
        if (!(primitive.shape() instanceof RaysShape shape)) {
            return;
        }
        
        Logging.FIELD.topic("render").trace("[RAYS] Rendering: count={}, arrangement={}", 
            shape.count(), shape.arrangement());
        
        // === Tessellate ===
        WaveConfig wave = primitive.animation() != null ? primitive.animation().wave() : null;
        Mesh mesh = tessellate(primitive, wave, time);
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // === Resolve Color ===
        int color = resolveColor(primitive, resolver, overrides, time);
        
        // Apply alpha from appearance
        Appearance app = primitive.appearance();
        float baseAlpha = 1.0f;
        if (app != null && app.alpha() != null) {
            baseAlpha = app.alpha().max();
        }
        
        // === Get Animation Configs ===
        Animation anim = primitive.animation();
        WaveConfig waveConfig = anim != null ? anim.wave() : null;
        RayFlowConfig flowConfig = anim != null ? anim.rayFlow() : null;
        RayMotionConfig motionConfig = anim != null ? anim.rayMotion() : null;
        RayWiggleConfig wiggleConfig = anim != null ? anim.rayWiggle() : null;
        RayTwistConfig twistConfig = anim != null ? anim.rayTwist() : null;
        
        // === Create ColorContext for per-vertex coloring ===
        ColorContext colorCtx = null;
        if (app != null && app.isPerVertex()) {
            int primaryColor = color;
            int secondaryColor = color;
            
            String primaryRef = app.color();
            if (primaryRef != null && resolver != null) {
                primaryColor = resolver.resolve(primaryRef);
            }
            
            String secondaryRef = app.secondaryColor();
            if (secondaryRef != null && resolver != null) {
                secondaryColor = resolver.resolve(secondaryRef);
            } else if (secondaryRef != null && secondaryRef.startsWith("#")) {
                try {
                    secondaryColor = 0xFF000000 | Integer.parseInt(secondaryRef.substring(1), 16);
                } catch (NumberFormatException ignored) {}
            } else {
                secondaryColor = primaryColor;
            }
            
            // For rays, we pass t (0-1) as Y coordinate, so use height=1.0
            colorCtx = ColorContext.from(app, primaryColor, secondaryColor, time, 
                shape.outerRadius(), 1.0f);  // height=1.0 since y=t ranges 0-1
        }
        
        // === Check mesh type and render appropriately ===
        if (mesh.isLines()) {
            // Line-based ray rendering - delegated to RaysLineEmitter
            float fadeStart = shape.fadeStart();
            float fadeEnd = shape.fadeEnd();
            boolean hasFade = fadeStart != 1.0f || fadeEnd != 1.0f;
            
            net.cyberpunk042.client.field.render.emit.RaysLineEmitter.emit(
                matrices, consumer, mesh, shape, color, baseAlpha, light, 
                colorCtx, waveConfig, time, fadeStart, fadeEnd, hasFade,
                flowConfig, motionConfig, wiggleConfig, twistConfig);
            
            Logging.FIELD.topic("render").trace("[RAYS] DONE lines: {} vertices", mesh.vertexCount());
        } else {
            // Triangle-based 3D ray rendering (droplet, egg, sphere ray types)
            // RESPECT FILL MODE like other shapes
            net.cyberpunk042.visual.fill.FillConfig fill = primitive.fill();
            net.cyberpunk042.visual.fill.FillMode mode = fill != null ? fill.mode() : net.cyberpunk042.visual.fill.FillMode.SOLID;
            
            switch (mode) {
                case SOLID -> emitRayTriangles(matrices, consumer, mesh, shape, color, baseAlpha, light, colorCtx, 
                    waveConfig, time, flowConfig, motionConfig, wiggleConfig, twistConfig);
                case WIREFRAME -> emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
                case CAGE -> emitCage(matrices, consumer, mesh, color, light, fill, primitive, waveConfig, time);
                case POINTS -> emitPoints(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            }
            
            Logging.FIELD.topic("render").trace("[RAYS] DONE 3D: {} vertices, mode={}", mesh.vertexCount(), mode);
        }
    }
    
    /**
     * Emits 3D ray triangles (for droplet, egg, sphere ray types).
     * Supports Wave, Motion, Wiggle, Twist, and Flicker effects.
     */
    private void emitRayTriangles(MatrixStack matrices, VertexConsumer consumer,
                                   Mesh mesh, RaysShape shape, int baseColor, float baseAlpha, int light,
                                   ColorContext colorCtx, WaveConfig waveConfig, float time,
                                   RayFlowConfig flowConfig, RayMotionConfig motionConfig,
                                   RayWiggleConfig wiggleConfig, RayTwistConfig twistConfig) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        
        // Calculate ray count and triangles per ray for INDEXED/RANDOM distribution
        int rayCount = shape != null ? Math.max(1, shape.count()) : 1;
        int layerCount = shape != null ? Math.max(1, shape.layers()) : 1;
        int totalRays = rayCount * layerCount;
        int triangleCount = mesh.primitiveCount();
        int trianglesPerRay = totalRays > 0 ? Math.max(1, triangleCount / totalRays) : 1;
        
        // Check if wave deformation should be applied
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        
        // Build effect chain for geometry vertex effects (Motion, Wiggle, Twist)
        final net.cyberpunk042.client.field.render.effect.RenderEffectChain effectChain = 
            net.cyberpunk042.client.field.render.effect.RenderEffectChain.builder()
                .addIf(motionConfig != null && motionConfig.isActive(), 
                       new net.cyberpunk042.client.field.render.effect.RenderMotionEffect(motionConfig, time))
                .addIf(wiggleConfig != null && wiggleConfig.isActive(), 
                       new net.cyberpunk042.client.field.render.effect.RenderWiggleEffect(wiggleConfig, time))
                .addIf(twistConfig != null && twistConfig.isActive(), 
                       new net.cyberpunk042.client.field.render.effect.RenderTwistEffect(twistConfig, time))
                .build();
        final boolean hasEffects = effectChain.hasActiveEffects();
        
        // Calculate flicker alpha multiplier (applies to entire shape)
        final float flickerAlpha = calculateFlickerAlpha(flowConfig, time);
        
        // Triangle index for effects
        final int[] triIndex = {0};
        
        // Triangle rendering with optional effects
        final int trisPerRay = trianglesPerRay;  // Capture for lambda
        mesh.forEachTriangle((v0, v1, v2) -> {
            int idx = triIndex[0]++;
            int rayIndex = idx / trisPerRay;  // Which ray this triangle belongs to
            
            Vertex w0 = v0, w1 = v1, w2 = v2;
            
            // Apply wave deformation first
            if (applyWave) {
                w0 = WaveDeformer.applyToVertex(w0, waveConfig, time);
                w1 = WaveDeformer.applyToVertex(w1, waveConfig, time);
                w2 = WaveDeformer.applyToVertex(w2, waveConfig, time);
            }
            
            // Apply effect chain (Motion, Wiggle, Twist) if active
            if (hasEffects) {
                w0 = applyEffectChain(w0, effectChain, rayIndex);
                w1 = applyEffectChain(w1, effectChain, rayIndex);
                w2 = applyEffectChain(w2, effectChain, rayIndex);
            }
            
            // Emit vertices with flicker alpha and ray index for INDEXED/RANDOM distribution
            float finalAlpha = baseAlpha * flickerAlpha;
            emitVertex(consumer, positionMatrix, normalMatrix, w0, baseColor, finalAlpha, light, colorCtx, rayIndex);
            emitVertex(consumer, positionMatrix, normalMatrix, w1, baseColor, finalAlpha, light, colorCtx, rayIndex);
            emitVertex(consumer, positionMatrix, normalMatrix, w2, baseColor, finalAlpha, light, colorCtx, rayIndex);
        });
    }
    
    /**
     * Apply effect chain to a vertex.
     * 
     * For 3D ray shapes, computes the ray direction from vertex position
     * (radial direction from center) since Wiggle/Twist effects need the 
     * ray axis, not the surface normal.
     * 
     * @param v The vertex to transform
     * @param chain The effect chain to apply
     * @param rayIndex The ray index (not triangle index) - ensures all vertices of the same ray get same offsets
     */
    private Vertex applyEffectChain(Vertex v, 
            net.cyberpunk042.client.field.render.effect.RenderEffectChain chain, int rayIndex) {
        float[] pos = new float[]{v.x(), v.y(), v.z()};
        
        // Compute ray direction from vertex position (radial from center)
        // For 3D rays, the ray points from center outward through this vertex
        float x = v.x(), y = v.y(), z = v.z();
        float dist = (float) Math.sqrt(x * x + z * z);
        float[] dir;
        if (dist > 0.001f) {
            // Radial direction on XZ plane, normalized
            dir = new float[]{x / dist, 0f, z / dist};
        } else {
            // Fallback to surface normal if at center
            dir = new float[]{v.nx(), v.ny(), v.nz()};
        }
        
        // For 3D shapes, the t-value (position along ray axis) is stored in v.v(), not v.u()
        // v.u() is texture U coordinate, v.v() is the actual parametric position (0=base, 1=tip)
        net.cyberpunk042.client.field.render.effect.RenderEffectContext ctx = 
            new net.cyberpunk042.client.field.render.effect.RenderEffectContext(rayIndex, v.v(), dir);
        chain.apply(pos, ctx);
        
        return new Vertex(pos[0], pos[1], pos[2], v.nx(), v.ny(), v.nz(), v.u(), v.v(), v.alpha());
    }
    
    /**
     * Calculate flicker alpha for the entire shape.
     */
    private float calculateFlickerAlpha(RayFlowConfig config, float time) {
        if (config == null || !config.hasFlicker()) {
            return 1f;
        }
        net.cyberpunk042.visual.energy.EnergyFlicker mode = config.effectiveFlicker();
        if (mode == net.cyberpunk042.visual.energy.EnergyFlicker.NONE) {
            return 1f;
        }
        
        float intensity = Math.max(0.1f, config.flickerIntensity());
        float freq = Math.max(0.5f, config.flickerFrequency());
        
        return net.cyberpunk042.client.visual.mesh.ray.flow.FlowFlickerStage.computeFlickerAlpha(
            mode, time, 0, intensity, freq);
    }
    
    private void emitVertex(VertexConsumer consumer, Matrix4f positionMatrix, Matrix3f normalMatrix,
                            Vertex v, int color, float alpha, int light, ColorContext colorCtx, int cellIndex) {
        float x = v.x(), y = v.y(), z = v.z();
        float nx = v.nx(), ny = v.ny(), nz = v.nz();
        
        // Apply color context if available - pass cellIndex for INDEXED/RANDOM distribution
        int finalColor = color;
        if (colorCtx != null) {
            finalColor = colorCtx.calculateColor(x, y, z, cellIndex);
        }
        
        // Apply alpha (base alpha * vertex alpha from tessellation)
        int a = (int) ((finalColor >> 24 & 0xFF) * alpha * v.alpha());
        finalColor = (a << 24) | (finalColor & 0x00FFFFFF);
        
        consumer.vertex(positionMatrix, x, y, z)
            .color(finalColor)
            .texture(v.u(), v.v())
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(nx, ny, nz);
    }
    
    /**
     * Override emitCage to generate lat/lon grid lines for 3D ray shapes.
     * 
     * <p>For 3D rays (droplets, eggs, etc.), cage mode draws latitude rings and
     * longitude meridians around each shape, providing a cleaner grid than
     * full triangle wireframe.</p>
     */
    @Override
    protected void emitCage(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            net.cyberpunk042.visual.fill.FillConfig fill,
            Primitive primitive,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        if (!(primitive.shape() instanceof RaysShape shape)) {
            // Fallback to wireframe if not rays
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Only use custom cage for 3D ray types
        RayType rayType = shape.effectiveRayType();
        if (!rayType.is3D() || !net.cyberpunk042.client.visual.mesh.ray.RayTypeTessellatorRegistry.isImplemented(rayType)) {
            // 2D rays just use wireframe
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage configuration from fill config, with reasonable defaults
        int latCount = 6;  // Default horizontal rings
        int lonCount = 8;  // Default vertical meridians
        
        if (fill != null && fill.cage() instanceof net.cyberpunk042.visual.fill.SphereCageOptions sphereCage) {
            latCount = sphereCage.latitudeCount();
            lonCount = sphereCage.longitudeCount();
        }
        
        // Build cage mesh with lines
        net.cyberpunk042.client.visual.mesh.MeshBuilder builder = net.cyberpunk042.client.visual.mesh.MeshBuilder.lines();
        
        // Generate cage lines for each ray
        int count = shape.count();
        int layers = Math.max(1, shape.layers());
        java.util.Random rng = new java.util.Random(42);
        
        for (int layer = 0; layer < layers; layer++) {
            for (int i = 0; i < count; i++) {
                // Compute context for this ray
                net.cyberpunk042.client.visual.mesh.ray.RayContext context = 
                    net.cyberpunk042.client.visual.mesh.ray.RayPositioner.computeContext(shape, i, layer, rng, waveConfig, time);
                
                // Generate cage for this droplet
                net.cyberpunk042.client.field.render.emit.RayCageGenerator.generateDropletCage(
                    builder, context, latCount, lonCount);
            }
        }
        
        // Emit the cage mesh
        Mesh cageMesh = builder.build();
        net.cyberpunk042.client.visual.render.VertexEmitter emitter = 
            new net.cyberpunk042.client.visual.render.VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
}