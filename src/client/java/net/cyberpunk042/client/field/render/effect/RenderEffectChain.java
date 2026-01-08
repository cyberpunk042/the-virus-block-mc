package net.cyberpunk042.client.field.render.effect;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain of render effects applied in sequence.
 * 
 * <h2>Usage</h2>
 * <pre>
 * RenderEffectChain chain = RenderEffectChain.builder()
 *     .add(new RenderMotionEffect(motionConfig, time))
 *     .add(new RenderWiggleEffect(wiggleConfig, time))
 *     .add(new RenderTwistEffect(twistConfig, time))
 *     .build();
 * 
 * chain.apply(position, ctx);
 * </pre>
 */
public final class RenderEffectChain {
    
    private final List<RenderVertexEffect> effects;
    
    private RenderEffectChain(List<RenderVertexEffect> effects) {
        this.effects = effects;
    }
    
    /**
     * Apply all active effects to the position.
     */
    public void apply(float[] position, RenderEffectContext ctx) {
        for (RenderVertexEffect effect : effects) {
            if (effect.isActive()) {
                effect.apply(position, ctx);
            }
        }
    }
    
    /**
     * Whether any effects are active.
     */
    public boolean hasActiveEffects() {
        for (RenderVertexEffect effect : effects) {
            if (effect.isActive()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Number of effects in the chain.
     */
    public int size() {
        return effects.size();
    }
    
    /**
     * Number of currently active effects.
     */
    public int activeCount() {
        int count = 0;
        for (RenderVertexEffect effect : effects) {
            if (effect.isActive()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Create an empty chain.
     */
    public static RenderEffectChain empty() {
        return new RenderEffectChain(List.of());
    }
    
    /**
     * Create a builder for constructing chains.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for constructing effect chains.
     */
    public static final class Builder {
        private final List<RenderVertexEffect> effects = new ArrayList<>();
        
        /**
         * Add an effect to the chain.
         */
        public Builder add(RenderVertexEffect effect) {
            if (effect != null) {
                effects.add(effect);
            }
            return this;
        }
        
        /**
         * Add an effect only if a condition is true.
         */
        public Builder addIf(boolean condition, RenderVertexEffect effect) {
            if (condition && effect != null) {
                effects.add(effect);
            }
            return this;
        }
        
        /**
         * Build the chain.
         */
        public RenderEffectChain build() {
            return new RenderEffectChain(new ArrayList<>(effects));
        }
    }
}
