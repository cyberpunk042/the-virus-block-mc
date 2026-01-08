package net.cyberpunk042.client.visual.mesh.ray.distribution;

import net.cyberpunk042.visual.shape.RaysShape;
import java.util.Random;

/**
 * Strategy for computing distribution offsets for ray positioning.
 * 
 * <p>Distribution strategies add variation to ray positions based on
 * different algorithms (UNIFORM, RANDOM, STOCHASTIC).</p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayPositioner#computeDistribution
 */
public interface DistributionStrategy {
    
    /**
     * Compute distribution offsets for a ray.
     * 
     * @param shape The rays shape configuration
     * @param index Ray index (0 to count-1)
     * @param count Total number of rays
     * @param rng Random number generator
     * @return Distribution result with offset values
     */
    DistributionResult compute(RaysShape shape, int index, int count, Random rng);
}
