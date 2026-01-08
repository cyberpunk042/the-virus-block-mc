package net.cyberpunk042.client.visual.mesh.ray.distribution;

/**
 * Result of distribution computation.
 * 
 * <p>Based on RayPositioner.DistributionResult, contains all offset values
 * needed by arrangement strategies.</p>
 * 
 * @param startOffset Start position offset along the ray
 * @param lengthMod Length multiplier (0.5 = half length, 1.0 = full)
 * @param angleJitter Angular offset in radians
 * @param radiusJitter Radial position jitter multiplier
 * 
 * @see net.cyberpunk042.client.visual.mesh.ray.RayPositioner#computeDistribution
 */
public record DistributionResult(
    float startOffset,
    float lengthMod,
    float angleJitter,
    float radiusJitter
) {
    /**
     * No distribution effect - uniform with no variation.
     */
    public static final DistributionResult NONE = new DistributionResult(0f, 1f, 0f, 0f);
}
