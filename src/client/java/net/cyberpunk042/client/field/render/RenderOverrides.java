package net.cyberpunk042.client.field.render;

import net.cyberpunk042.visual.pattern.VertexPattern;
import org.jetbrains.annotations.Nullable;

/**
 * Optional rendering overrides that can be applied at runtime.
 * 
 * <p>Used for dynamic effects like pattern shuffling, color overrides,
 * or scale modifications that need to be applied without modifying
 * the underlying primitive definition.
 * 
 * <h2>Usage</h2>
 * <pre>
 * RenderOverrides overrides = RenderOverrides.builder()
 *     .vertexPattern(DynamicQuadPattern.fromArrangement(shuffledArr))
 *     .alphaMultiplier(0.5f)
 *     .build();
 * 
 * renderer.render(primitive, matrices, consumer, light, time, resolver, overrides);
 * </pre>
 * 
 * @see PrimitiveRenderer
 */
public record RenderOverrides(
    @Nullable VertexPattern vertexPattern,
    @Nullable Integer colorOverride,
    float alphaMultiplier,
    float scaleMultiplier
) {
    
    /**
     * Default overrides (no modifications).
     */
    public static final RenderOverrides NONE = new RenderOverrides(null, null, 1.0f, 1.0f);
    
    /**
     * Creates overrides with just a pattern override.
     */
    public static RenderOverrides withPattern(VertexPattern pattern) {
        return new RenderOverrides(pattern, null, 1.0f, 1.0f);
    }
    
    /**
     * Creates overrides with just an alpha multiplier.
     */
    public static RenderOverrides withAlpha(float alpha) {
        return new RenderOverrides(null, null, alpha, 1.0f);
    }
    
    /**
     * Checks if any overrides are active.
     */
    public boolean hasOverrides() {
        return vertexPattern != null 
            || colorOverride != null 
            || alphaMultiplier != 1.0f 
            || scaleMultiplier != 1.0f;
    }
    
    /**
     * Builder for creating overrides.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private VertexPattern vertexPattern = null;
        private Integer colorOverride = null;
        private float alphaMultiplier = 1.0f;
        private float scaleMultiplier = 1.0f;
        
        public Builder vertexPattern(VertexPattern p) { this.vertexPattern = p; return this; }
        public Builder colorOverride(int c) { this.colorOverride = c; return this; }
        public Builder alphaMultiplier(float a) { this.alphaMultiplier = a; return this; }
        public Builder scaleMultiplier(float s) { this.scaleMultiplier = s; return this; }
        
        public RenderOverrides build() {
            return new RenderOverrides(vertexPattern, colorOverride, alphaMultiplier, scaleMultiplier);
        }
    }
}

