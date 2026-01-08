package net.cyberpunk042.visual.shape;

/**
 * Controls how rays are distributed along the radial dimension.
 * 
 * <h3>Modes</h3>
 * <ul>
 *   <li><b>UNIFORM</b> - All rays have the same length and start position.
 *       Rays span from innerRadius to innerRadius + rayLength.</li>
 *   <li><b>RANDOM</b> - Each ray has randomized start position and length.
 *       Starts anywhere in [innerRadius, outerRadius - minLength], with random length.</li>
 *   <li><b>STOCHASTIC</b> - Heavily randomized with variable density.
 *       Some rays may be very short, some very long, scattered throughout the range.</li>
 * </ul>
 * 
 * @see RaysShape
 */
public enum RayDistribution {
    /**
     * Uniform distribution - all rays identical length and position.
     * Classic radial burst pattern.
     */
    UNIFORM,
    
    /**
     * Random distribution - rays have random start and length.
     * Creates a scattered, organic look while staying within radius bounds.
     */
    RANDOM,
    
    /**
     * Stochastic distribution - heavily randomized with variable density.
     * Most chaotic, particle-field-like appearance.
     */
    STOCHASTIC
}
