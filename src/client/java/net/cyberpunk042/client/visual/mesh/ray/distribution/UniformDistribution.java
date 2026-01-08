package net.cyberpunk042.client.visual.mesh.ray.distribution;

import net.cyberpunk042.visual.shape.RaysShape;
import java.util.Random;

/**
 * Uniform distribution - even spacing with minimal variation.
 * 
 * <p>Based on RayPositioner.computeDistribution UNIFORM case:
 * - lengthMod = 1 - lengthVariation * random
 * - angleJitter = randomness * (random - 0.5) * 2π/count * 0.5
 * </p>
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayPositioner#computeDistribution
 */
public final class UniformDistribution implements DistributionStrategy {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    public static final UniformDistribution INSTANCE = new UniformDistribution();
    
    private UniformDistribution() {}
    
    @Override
    public DistributionResult compute(RaysShape shape, int index, int count, Random rng) {
        // From RayPositioner.computeDistribution lines 1002-1005:
        // case UNIFORM -> {
        //     lengthMod = 1f - lengthVariation * rng.nextFloat();
        //     angleJitter = randomness * (rng.nextFloat() - 0.5f) * TWO_PI / count * 0.5f;
        // }
        
        float lengthVariation = shape.lengthVariation();
        float randomness = shape.randomness();
        
        float lengthMod = 1f - lengthVariation * rng.nextFloat();
        float angleJitter = randomness * (rng.nextFloat() - 0.5f) * TWO_PI / count * 0.5f;
        
        return new DistributionResult(0f, lengthMod, angleJitter, 0f);
    }
}
