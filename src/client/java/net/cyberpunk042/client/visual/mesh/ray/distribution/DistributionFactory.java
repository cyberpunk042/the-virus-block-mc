package net.cyberpunk042.client.visual.mesh.ray.distribution;

import net.cyberpunk042.visual.shape.RayDistribution;

/**
 * Factory for creating DistributionStrategy instances.
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayPositioner#computeDistribution
 */
public final class DistributionFactory {
    
    private DistributionFactory() {}
    
    /**
     * Get the appropriate strategy for a distribution type.
     */
    public static DistributionStrategy get(RayDistribution distribution) {
        if (distribution == null) {
            return UniformDistribution.INSTANCE;
        }
        
        return switch (distribution) {
            case UNIFORM -> UniformDistribution.INSTANCE;
            case RANDOM -> RandomDistribution.INSTANCE;
            case STOCHASTIC -> StochasticDistribution.INSTANCE;
        };
    }
}
