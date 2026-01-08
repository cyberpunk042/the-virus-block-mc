package net.cyberpunk042.client.field.render.emit;

import net.minecraft.client.render.VertexConsumer;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;

import java.util.List;

/**
 * Emit strategy for line rendering.
 * 
 * Emits vertices as line segments.
 */
public final class EmitLineStrategy implements EmitStrategy {
    
    public static final EmitLineStrategy INSTANCE = new EmitLineStrategy();
    
    private EmitLineStrategy() {}
    
    @Override
    public void emit(VertexConsumer consumer, Mesh mesh, EmitContext ctx) {
        if (mesh == null || !ctx.isVisible()) {
            return;
        }
        
        List<Vertex> vertices = mesh.vertices();
        int vertexCount = vertices.size();
        int baseColor = ctx.color();
        
        // Emit as line pairs
        for (int i = 0; i < vertexCount - 1; i += 2) {
            // First vertex
            emitVertex(consumer, vertices.get(i), baseColor, ctx);
            // Second vertex
            emitVertex(consumer, vertices.get(i + 1), baseColor, ctx);
        }
    }
    
    private void emitVertex(VertexConsumer consumer, Vertex vertex, int baseColor, EmitContext ctx) {
        // Apply vertex alpha and flicker alpha to base color
        float vertexAlpha = vertex.alpha();
        float alpha = ((baseColor >> 24) & 0xFF) / 255f * vertexAlpha * ctx.flickerAlpha();
        int color = (baseColor & 0x00FFFFFF) | ((int)(alpha * 255) << 24);
        
        consumer.vertex(vertex.x(), vertex.y(), vertex.z())
            .color(color)
            .normal(vertex.nx(), vertex.ny(), vertex.nz());
    }
}
