package net.cyberpunk042.client.field.render.effect;

/**
 * Context for render effects.
 * 
 * <p>Contains all the information a render effect needs to transform a vertex.</p>
 * 
 * @param rayIndex Index of current ray
 * @param t Parameter along ray (0-1)
 * @param direction Ray direction vector [dx, dy, dz] - can be null if not needed
 */
public record RenderEffectContext(
    int rayIndex,
    float t,
    float[] direction
) {
    /**
     * Create a new context with a different t value.
     */
    public RenderEffectContext withT(float newT) {
        return new RenderEffectContext(rayIndex, newT, direction);
    }
    
    /**
     * Get direction X component (0 if direction is null).
     */
    public float dx() {
        return direction != null && direction.length > 0 ? direction[0] : 0;
    }
    
    /**
     * Get direction Y component (0 if direction is null).
     */
    public float dy() {
        return direction != null && direction.length > 1 ? direction[1] : 0;
    }
    
    /**
     * Get direction Z component (0 if direction is null).
     */
    public float dz() {
        return direction != null && direction.length > 2 ? direction[2] : 0;
    }
}
