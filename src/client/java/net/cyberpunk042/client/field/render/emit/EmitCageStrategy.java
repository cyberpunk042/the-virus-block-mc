package net.cyberpunk042.client.field.render.emit;

import net.minecraft.client.render.VertexConsumer;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;

import java.util.List;

/**
 * Emit strategy for cage/wireframe rendering.
 * 
 * Emits triangle edges as lines for wireframe effect.
 */
public final class EmitCageStrategy implements EmitStrategy {
    
    public static final EmitCageStrategy INSTANCE = new EmitCageStrategy();
    
    private EmitCageStrategy() {}
    
    @Override
    public void emit(VertexConsumer consumer, Mesh mesh, EmitContext ctx) {
        if (mesh == null || !ctx.isVisible()) {
            return;
        }
        
        List<Vertex> vertices = mesh.vertices();
        int[] indices = mesh.indices();
        int baseColor = ctx.color();
        
        if (indices != null && indices.length > 0) {
            // Indexed triangles - emit edges
            for (int i = 0; i < indices.length - 2; i += 3) {
                int i0 = indices[i];
                int i1 = indices[i + 1];
                int i2 = indices[i + 2];
                
                // Edge 0-1
                emitEdge(consumer, vertices.get(i0), vertices.get(i1), baseColor, ctx);
                // Edge 1-2
                emitEdge(consumer, vertices.get(i1), vertices.get(i2), baseColor, ctx);
                // Edge 2-0
                emitEdge(consumer, vertices.get(i2), vertices.get(i0), baseColor, ctx);
            }
        } else {
            // Non-indexed triangles - emit edges
            int vertexCount = vertices.size();
            for (int i = 0; i < vertexCount - 2; i += 3) {
                // Edge i to i+1
                emitEdge(consumer, vertices.get(i), vertices.get(i + 1), baseColor, ctx);
                // Edge i+1 to i+2
                emitEdge(consumer, vertices.get(i + 1), vertices.get(i + 2), baseColor, ctx);
                // Edge i+2 to i
                emitEdge(consumer, vertices.get(i + 2), vertices.get(i), baseColor, ctx);
            }
        }
    }
    
    private void emitEdge(VertexConsumer consumer, Vertex v0, Vertex v1, int baseColor, EmitContext ctx) {
        emitVertex(consumer, v0, baseColor, ctx);
        emitVertex(consumer, v1, baseColor, ctx);
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
