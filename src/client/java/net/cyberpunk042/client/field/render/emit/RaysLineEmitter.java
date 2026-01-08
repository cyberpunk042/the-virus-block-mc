package net.cyberpunk042.client.field.render.emit;

import net.cyberpunk042.client.field.render.effect.RenderEffectChain;
import net.cyberpunk042.client.field.render.effect.RenderEffectContext;
import net.cyberpunk042.client.field.render.effect.RenderMotionEffect;
import net.cyberpunk042.client.field.render.effect.RenderTwistEffect;
import net.cyberpunk042.client.field.render.effect.RenderWiggleEffect;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.visual.animation.RayFlowConfig;
import net.cyberpunk042.visual.animation.RayMotionConfig;
import net.cyberpunk042.visual.animation.RayTwistConfig;
import net.cyberpunk042.visual.animation.RayWiggleConfig;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.animation.WaveDistribution;
import net.cyberpunk042.visual.appearance.ColorContext;
import net.cyberpunk042.visual.shape.RaysShape;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Emits ray line vertices with fade and animation support.
 * 
 * <p>Extracted from RaysRenderer to focus on line-based ray rendering.
 * Each vertex has a 't' value stored in UV.u (0 at ray start, 1 at ray end).
 * Alpha is modulated by: fade gradient, flow animation (Radiative, Travel, Flicker).</p>
 */
public final class RaysLineEmitter {
    
    private RaysLineEmitter() {} // Static utility class
    
    /**
     * Emits ray lines from a mesh with full animation support.
     */
    public static void emit(MatrixStack matrices, VertexConsumer consumer,
                           Mesh mesh, RaysShape raysShape, int baseColor, float baseAlpha, int light,
                           ColorContext colorCtx, WaveConfig waveConfig, float time,
                           float fadeStart, float fadeEnd, boolean hasFade,
                           RayFlowConfig flowConfig, RayMotionConfig motionConfig,
                           RayWiggleConfig wiggleConfig, RayTwistConfig twistConfig) {
        
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        
        // Phase model: Phase-based segment positioning is now done in the tessellator
        // (RayLineTessellator). The tessellator already positions segments correctly
        // based on each copy's phase. The emitter just needs to emit the geometry.
        
        final float travelOffset = calculateTravelOffset(flowConfig, time);
        
        // Get ray count for per-ray effects (color, flicker, travel)
        final int rayCount = raysShape != null ? Math.max(1, raysShape.count()) : 1;
        
        // Build effect chain for all vertex effects
        final RenderEffectChain effectChain = RenderEffectChain.builder()
            .addIf(motionConfig != null && motionConfig.isActive(), 
                   new RenderMotionEffect(motionConfig, time))
            .addIf(wiggleConfig != null && wiggleConfig.isActive(), 
                   new RenderWiggleEffect(wiggleConfig, time))
            .addIf(twistConfig != null && twistConfig.isActive(), 
                   new RenderTwistEffect(twistConfig, time))
            .build();
        final boolean hasEffects = effectChain.hasActiveEffects();
        
        // For each line segment
        // Segments are ordered: ray0-seg0, ray0-seg1, ..., ray1-seg0, ray1-seg1, ...
        final int lineResolution = raysShape != null ? Math.max(1, raysShape.lineResolution()) : 1;
        final int[] segmentIndex = {0};
        mesh.forEachLine((v0, v1) -> {
            int segIdx = segmentIndex[0]++;
            int actualRayIndex = segIdx / lineResolution;
            
            // Get base positions
            float x0 = v0.x(), y0 = v0.y(), z0 = v0.z();
            float x1 = v1.x(), y1 = v1.y(), z1 = v1.z();
            
            // Get t values for each vertex
            float t0 = v0.u();
            float t1 = v1.u();
            
            // NOTE: Radiative clipping is NO LONGER done here!
            // The tessellator (RayLineTessellator) now handles phase-based segment positioning.
            // Each copy of a ray has its segment positioned at a different phase offset,
            // so the mesh already contains correctly positioned geometry.
            
            // Apply All Vertex Effects via RenderEffectChain
            if (hasEffects) {
                float dx = x1 - x0;
                float dy = y1 - y0;
                float dz = z1 - z0;
                float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 0.0001f) {
                    dx /= len; dy /= len; dz /= len;
                }
                
                float[] dir = new float[]{dx, dy, dz};
                float[] pos0 = new float[]{x0, y0, z0};
                float[] pos1 = new float[]{x1, y1, z1};
                
                RenderEffectContext ctx0 = new RenderEffectContext(segIdx, t0, dir);
                RenderEffectContext ctx1 = new RenderEffectContext(segIdx, t1, dir);
                
                effectChain.apply(pos0, ctx0);
                effectChain.apply(pos1, ctx1);
                
                x0 = pos0[0]; y0 = pos0[1]; z0 = pos0[2];
                x1 = pos1[0]; y1 = pos1[1]; z1 = pos1[2];
            }
            
            // Emit both vertices of the line
            emitLineVertex(consumer, x0, y0, z0, x1, y1, z1, positionMatrix, normalMatrix,
                          baseColor, baseAlpha, light, colorCtx, v0, flowConfig, time, actualRayIndex, rayCount,
                          travelOffset, fadeStart, fadeEnd, hasFade);
            emitLineVertex(consumer, x1, y1, z1, x0, y0, z0, positionMatrix, normalMatrix,
                          baseColor, baseAlpha, light, colorCtx, v1, flowConfig, time, actualRayIndex, rayCount,
                          travelOffset, fadeStart, fadeEnd, hasFade);
        });
    }
    
    /**
     * Emits a single line vertex with color, fade, and all flow animations.
     */
    private static void emitLineVertex(VertexConsumer consumer, 
                                 float x, float y, float z,
                                 float otherX, float otherY, float otherZ,
                                 Matrix4f positionMatrix, Matrix3f normalMatrix,
                                 int baseColor, float baseAlpha, int light,
                                 ColorContext colorCtx, Vertex originalVertex,
                                 RayFlowConfig flowConfig, float time, int rayIndex, int rayCount,
                                 float travelOffset,
                                 float fadeStart, float fadeEnd, boolean hasFade) {
        
        // Transform position
        Vector4f pos = new Vector4f(x, y, z, 1.0f);
        pos.mul(positionMatrix);
        
        // Normal is direction to other vertex
        Vector3f dir = new Vector3f(otherX - x, otherY - y, otherZ - z);
        if (dir.lengthSquared() > 0.0001f) {
            dir.normalize();
        } else {
            dir.set(0, 1, 0);
        }
        dir.mul(normalMatrix);
        
        // Calculate color
        int vertexColor;
        if (colorCtx != null && colorCtx.isPerVertex()) {
            float rayT = originalVertex.u();
            
            // For GRADIENT distribution, use normalized ray position across the whole mesh
            // This makes the gradient span all rays, not repeat per-ray
            // For INDEXED/RANDOM, cellIndex (rayIndex) handles it
            float normalizedRayPos = rayCount > 1 ? (float) rayIndex / (rayCount - 1) : 0.5f;
            
            // Combine ray position with vertex t-position for 2D gradient
            // Y axis: normalized ray position (0=first ray, 1=last ray)
            // Vertex t: position along the ray (0=start, 1=end)
            float gradientY = normalizedRayPos - 0.5f;  // Center around 0 for height normalization
            
            vertexColor = colorCtx.calculateColor(
                originalVertex.x(), 
                gradientY,
                originalVertex.z(), 
                rayIndex
            );
        } else {
            vertexColor = baseColor;
        }
        
        // Calculate alpha with all modulations
        float t = originalVertex.u();
        float alpha = baseAlpha;
        
        // 1. Apply base fade gradient
        if (hasFade) {
            float fadeFactor = fadeStart + (fadeEnd - fadeStart) * t;
            alpha *= fadeFactor;
        }
        
        // 2. Apply Travel animation
        alpha *= calculateTravelAlpha(flowConfig, t, travelOffset, rayIndex);
        
        // 3. Apply Flicker animation
        alpha *= calculateFlickerAlpha(flowConfig, time, rayIndex);
        
        // Apply alpha to color
        int a = (int)(alpha * 255) & 0xFF;
        int r = (vertexColor >> 16) & 0xFF;
        int g = (vertexColor >> 8) & 0xFF;
        int b = vertexColor & 0xFF;
        
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .normal(dir.x(), dir.y(), dir.z());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Flow Animation Helpers
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Compute base radiative phase for animation.
     */
    public static float calculateLengthPhase(RayFlowConfig config, float time) {
        if (config == null || !config.hasRadiative()) {
            return 0f;
        }
        float phase = (time * config.radiativeSpeed()) % 1.0f;
        if (phase < 0) phase += 1.0f;
        return phase;
    }
    
    /**
     * Compute travel offset for travel animation.
     */
    private static float calculateTravelOffset(RayFlowConfig config, float time) {
        if (config == null || !config.hasTravel()) {
            return 0f;
        }
        float travelSpeed = Math.max(0.1f, config.travelSpeed());
        float travelPhase = (time * travelSpeed * 0.3f) % 1.0f;
        if (travelPhase < 0) travelPhase += 1.0f;
        return travelPhase;
    }
    
    /**
     * Compute travel alpha for a vertex.
     */
    private static float calculateTravelAlpha(RayFlowConfig config, float t, float travelOffset, int rayIndex) {
        if (config == null || !config.hasTravel()) {
            return 1f;
        }
        net.cyberpunk042.visual.energy.EnergyTravel mode = config.effectiveTravel();
        if (mode == net.cyberpunk042.visual.energy.EnergyTravel.NONE) {
            return 1f;
        }
        return net.cyberpunk042.client.visual.mesh.ray.flow.FlowTravelStage.computeTravelAlpha(
            t, mode, travelOffset, config.chaseCount(), config.chaseWidth());
    }
    
    /**
     * Compute flicker alpha for a ray.
     */
    private static float calculateFlickerAlpha(RayFlowConfig config, float time, int rayIndex) {
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
            mode, time, rayIndex, intensity, freq);
    }
}
