package net.cyberpunk042.client.visual.mesh.ray.flow;

/**
 * Interface for flow animation pipeline stages.
 * 
 * Each stage processes the animation state and returns a new state.
 * Stages are executed in order by FlowPipeline.
 */
public interface FlowStage {
    
    /**
     * Process the animation state.
     * 
     * @param state Current animation state
     * @param ctx Context with config and ray info
     * @return New animation state (may be same if no changes)
     */
    AnimationState process(AnimationState state, FlowContext ctx);
    
    /**
     * Whether this stage should run for the given context.
     * Override to skip stages based on config.
     */
    default boolean shouldRun(FlowContext ctx) {
        return true;
    }
    
    /**
     * Name for debugging/logging.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
