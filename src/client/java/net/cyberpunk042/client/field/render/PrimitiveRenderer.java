package net.cyberpunk042.client.field.render;

import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.field.primitive.Primitive;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Interface for rendering primitives.
 * 
 * <p>Per CLASS_DIAGRAM §8: Each shape type has its own renderer implementation
 * that knows how to tessellate and emit vertices for that specific shape.
 * 
 * <h2>Rendering Pipeline</h2>
 * <pre>
 * Primitive
 *    ↓
 * PrimitiveRenderer.render()
 *    ↓
 * Tessellator.tessellate(shape) → Mesh
 *    ↓
 * VertexEmitter.emit(mesh) → GPU
 * </pre>
 * 
 * <h2>Implementation Contract</h2>
 * <ul>
 *   <li>Each implementation handles ONE shape type</li>
 *   <li>Implementations are STATELESS (can be singletons)</li>
 *   <li>Transform is already applied to matrices before render()</li>
 *   <li>Use ColorResolver to resolve color references</li>
 * </ul>
 * 
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link SphereRenderer} - spheres, hemispheres</li>
 *   <li>{@link RingRenderer} - rings, tori</li>
 *   <li>{@link DiscRenderer} - discs, fans</li>
 *   <li>{@link PrismRenderer} - prisms, pyramids</li>
 *   <li>{@link CylinderRenderer} - cylinders, cones</li>
 *   <li>{@link PolyhedronRenderer} - platonic solids, geodesic</li>
 * </ul>
 * 
 * @see FieldRenderer
 * @see LayerRenderer
 * @see net.cyberpunk042.client.visual.mesh.Tessellator
 * @see net.cyberpunk042.client.visual.render.VertexEmitter
 */
public interface PrimitiveRenderer {
    
    /**
     * Renders a primitive.
     * 
     * <p>The matrices should already have layer transform applied.
     * This method applies primitive-specific transforms and animation,
     * tessellates the shape, and emits vertices.
     * 
     * @param primitive The primitive to render
     * @param matrices Matrix stack (already positioned at layer origin)
     * @param consumer Vertex consumer for the appropriate render layer
     * @param light Light level (usually fullbright for fields)
     * @param time World time for animations
     * @param resolver Color resolver for theme colors
     * @param overrides Optional render overrides (nullable)
     */
    void render(
        Primitive primitive,
        MatrixStack matrices,
        VertexConsumer consumer,
        int light,
        float time,
        ColorResolver resolver,
        RenderOverrides overrides
    );
    
    /**
     * Convenience method without overrides.
     */
    default void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver resolver) {
        render(primitive, matrices, consumer, light, time, resolver, null);
    }
    
    /**
     * Returns the shape type this renderer handles.
     * Used by {@link PrimitiveRenderers} to dispatch to correct implementation.
     */
    String shapeType();
}

