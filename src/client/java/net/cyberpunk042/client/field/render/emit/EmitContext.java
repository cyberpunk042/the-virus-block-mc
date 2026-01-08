package net.cyberpunk042.client.field.render.emit;

import org.joml.Matrix4f;
import net.cyberpunk042.visual.appearance.ColorContext;
import net.cyberpunk042.client.visual.mesh.ray.flow.AnimationState;
import net.cyberpunk042.visual.shape.ShapeState;
import net.cyberpunk042.visual.shape.RayFlowStage;

/**
 * Context for vertex emission.
 * 
 * Contains all information needed to emit vertices for a ray.
 */
public record EmitContext(
    // Transform
    Matrix4f modelMatrix,
    
    // Appearance
    ColorContext colorContext,
    int baseColor,
    
    // Animation state
    ShapeState<RayFlowStage> shapeState,
    AnimationState animState,
    
    // Ray info
    int rayIndex,
    int rayCount,
    float time,
    
    // Geometry bounds
    float innerRadius,
    float outerRadius
) {
    /**
     * Get the base color (convenience method).
     */
    public int color() {
        return baseColor;
    }
    /**
     * Normalized ray index (0-1).
     */
    public float normalizedIndex() {
        return rayCount > 1 ? (float) rayIndex / (rayCount - 1) : 0f;
    }
    
    /**
     * Travel distance for this ray field.
     */
    public float travelDistance() {
        return outerRadius - innerRadius;
    }
    
    /**
     * Current phase from animation state.
     */
    public float phase() {
        return animState != null ? animState.phase() : 0f;
    }
    
    /**
     * Current flicker alpha from animation state.
     */
    public float flickerAlpha() {
        return animState != null ? animState.flickerAlpha() : 1f;
    }
    
    /**
     * Whether shape is visible based on shape state.
     */
    public boolean isVisible() {
        return shapeState != null && shapeState.isVisible();
    }
}
