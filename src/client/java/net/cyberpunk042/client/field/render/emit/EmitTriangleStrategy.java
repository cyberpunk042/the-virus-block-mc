package net.cyberpunk042.client.field.render.emit;

import net.minecraft.client.render.VertexConsumer;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;

import java.util.List;

/**
 * Emit strategy for triangle/solid rendering.
 * 
 * Emits vertices as triangles.
 */
public final class EmitTriangleStrategy implements EmitStrategy {
    
    public static final EmitTriangleStrategy INSTANCE = new EmitTriangleStrategy();
    
    private EmitTriangleStrategy() {}
    
    @Override
    public void emit(VertexConsumer consumer, Mesh mesh, EmitContext ctx) {
        if (mesh == null || !ctx.isVisible()) {
            return;
        }
        
        List<Vertex> vertices = mesh.vertices();
        int[] indices = mesh.indices();
        int baseColor = ctx.color();
        
        if (indices != null && indices.length > 0) {
            // Indexed triangles
            for (int i = 0; i < indices.length - 2; i += 3) {
                emitVertex(consumer, vertices.get(indices[i]), baseColor, ctx);
                emitVertex(consumer, vertices.get(indices[i + 1]), baseColor, ctx);
                emitVertex(consumer, vertices.get(indices[i + 2]), baseColor, ctx);
            }
        } else {
            // Non-indexed triangles
            int vertexCount = vertices.size();
            for (int i = 0; i < vertexCount - 2; i += 3) {
                emitVertex(consumer, vertices.get(i), baseColor, ctx);
                emitVertex(consumer, vertices.get(i + 1), baseColor, ctx);
                emitVertex(consumer, vertices.get(i + 2), baseColor, ctx);
            }
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
