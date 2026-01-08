package net.cyberpunk042.client.field.render.emit;

import net.minecraft.client.render.VertexConsumer;
import net.cyberpunk042.client.visual.mesh.Mesh;

/**
 * Strategy for emitting vertices to the render buffer.
 * 
 * Different strategies for LINE, TRIANGLE, CAGE rendering.
 */
public interface EmitStrategy {
    
    /**
     * Emit vertices for a mesh.
     * 
     * @param consumer Vertex consumer to emit to
     * @param mesh The mesh to emit
     * @param ctx Emission context
     */
    void emit(VertexConsumer consumer, Mesh mesh, EmitContext ctx);
    
    /**
     * Name for debugging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
