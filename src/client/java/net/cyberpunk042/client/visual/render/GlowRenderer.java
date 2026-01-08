package net.cyberpunk042.client.visual.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorMath;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Utility for rendering glow/emissive effects.
 * 
 * <p>Provides additive blending and bloom-like effects for
 * energy fields, beams, and other glowing elements.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Render a glowing mesh
 * GlowRenderer.render(matrices, consumers, mesh, color, intensity, light);
 * 
 * // Render with additive blending
 * GlowRenderer.renderAdditive(matrices, consumers, mesh, color, light);
 * </pre>
 */
public final class GlowRenderer {
    
    /** Default glow intensity (bloom factor) */
    public static final float DEFAULT_INTENSITY = 1.0f;
    
    /** Maximum glow intensity */
    public static final float MAX_INTENSITY = 3.0f;
    
    private GlowRenderer() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Glow Rendering
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Renders a mesh with glow effect.
     * 
     * @param matrices Matrix stack
     * @param consumers Vertex consumer provider
     * @param mesh Mesh to render
     * @param color Base ARGB color
     * @param intensity Glow intensity (1.0 = normal, 2.0 = bright)
     * @param light Light level
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Mesh mesh,
            int color,
            float intensity,
            int light) {
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // Clamp intensity
        intensity = Math.min(intensity, MAX_INTENSITY);
        
        // Brighten color based on intensity
        int glowColor = intensity > 1.0f 
            ? ColorMath.lighten(color, (intensity - 1.0f) * 0.3f)
            : color;
        
        // Use translucent layer for glow
        RenderLayer layer = FieldRenderLayers.glowTranslucent();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        emitGlowMesh(matrices, consumer, mesh, glowColor, light);
        
        Logging.RENDER.topic("glow").trace(
            "Rendered glow mesh: {} verts, intensity={:.2f}", 
            mesh.vertexCount(), intensity);
    }
    
    /**
     * Renders with default intensity.
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Mesh mesh,
            int color,
            int light) {
        render(matrices, consumers, mesh, color, DEFAULT_INTENSITY, light);
    }
    
    /**
     * Renders with additive blending for pure glow.
     */
    public static void renderAdditive(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Mesh mesh,
            int color,
            int light) {
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // Use additive layer
        RenderLayer layer = FieldRenderLayers.glowAdditive();
        VertexConsumer consumer = consumers.getBuffer(layer);
        
        emitGlowMesh(matrices, consumer, mesh, color, light);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Halo Effect
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Renders a multi-layer halo effect.
     * Creates a soft bloom by rendering at increasing scales with decreasing alpha.
     * 
     * @param layers Number of halo layers
     * @param scaleStep Scale increase per layer
     * @param alphaFalloff Alpha multiplier per layer
     */
    public static void renderHalo(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Mesh mesh,
            int color,
            int light,
            int layers,
            float scaleStep,
            float alphaFalloff) {
        
        if (mesh == null || mesh.isEmpty() || layers <= 0) {
            return;
        }
        
        float alpha = ColorMath.alphaF(color);
        
        for (int i = 0; i < layers; i++) {
            float layerScale = 1.0f + (i * scaleStep);
            float layerAlpha = alpha * (float) Math.pow(alphaFalloff, i);
            
            int layerColor = ColorMath.withAlpha(color, layerAlpha);
            
            matrices.push();
            matrices.scale(layerScale, layerScale, layerScale);
            
            render(matrices, consumers, mesh, layerColor, 1.0f, light);
            
            matrices.pop();
        }
        
        Logging.RENDER.topic("glow").trace(
            "Rendered halo: {} layers, scaleStep={:.2f}", layers, scaleStep);
    }
    
    /**
     * Renders a simple 3-layer halo.
     */
    public static void renderSimpleHalo(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Mesh mesh,
            int color,
            int light) {
        renderHalo(matrices, consumers, mesh, color, light, 3, 0.1f, 0.5f);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────
    
    private static void emitGlowMesh(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light) {
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float a = ColorMath.alphaF(color);
        float r = ColorMath.redF(color);
        float g = ColorMath.greenF(color);
        float b = ColorMath.blueF(color);
        
        // Full bright for glow
        int fullBright = 15728880;
        
        mesh.forEachTriangle((v0, v1, v2) -> {
            emitVertex(consumer, matrix, v0.x(), v0.y(), v0.z(), 
                       v0.nx(), v0.ny(), v0.nz(), v0.u(), v0.v(),
                       r, g, b, a, fullBright);
            emitVertex(consumer, matrix, v1.x(), v1.y(), v1.z(),
                       v1.nx(), v1.ny(), v1.nz(), v1.u(), v1.v(),
                       r, g, b, a, fullBright);
            emitVertex(consumer, matrix, v2.x(), v2.y(), v2.z(),
                       v2.nx(), v2.ny(), v2.nz(), v2.u(), v2.v(),
                       r, g, b, a, fullBright);
        });
    }
    
    private static void emitVertex(
            VertexConsumer consumer, Matrix4f matrix,
            float x, float y, float z,
            float nx, float ny, float nz,
            float u, float v,
            float r, float g, float b, float a,
            int light) {
        consumer.vertex(matrix, x, y, z)
            .color(r, g, b, a)
            .texture(u, v)
            .overlay(0)
            .light(light)
            .normal(nx, ny, nz);
    }
}
