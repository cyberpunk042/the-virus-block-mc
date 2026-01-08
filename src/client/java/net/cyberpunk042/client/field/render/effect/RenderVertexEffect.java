package net.cyberpunk042.client.field.render.effect;

/**
 * Interface for vertex modification effects.
 * 
 * Effects like Motion, Wiggle, Twist implement this to modify
 * vertex positions before emission.
 */
public interface RenderVertexEffect {
    
    /**
     * Apply the effect to a vertex position.
     * 
     * @param position Position [x, y, z] - modified in place
     * @param ctx Rendering context
     */
    void apply(float[] position, RenderEffectContext ctx);
    
    /**
     * Whether this effect is currently active.
     */
    boolean isActive();
    
    /**
     * Name for debugging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
