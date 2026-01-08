package net.cyberpunk042.visual.fill;

import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Base interface for cage rendering options.
 * 
 * <p>Cage mode renders a structured grid rather than all tessellation edges.
 * Each shape type has its own implementation with shape-specific options.</p>
 * 
 * <h2>Curved Surface Shapes (Distinct Cage)</h2>
 * <ul>
 *   <li>{@link SphereCageOptions} - Latitude/longitude grid</li>
 *   <li>{@link CylinderCageOptions} - Vertical lines and horizontal rings</li>
 *   <li>{@link PrismCageOptions} - Vertical edges and horizontal rings</li>
 *   <li>{@link ConeCageOptions} - Radial lines to apex and base ring</li>
 *   <li>{@link RingCageOptions} - Radial lines with inner/outer rings</li>
 *   <li>{@link TorusCageOptions} - Major and minor rings</li>
 * </ul>
 * 
 * <h2>Polyhedral Shapes (Cage = Wireframe)</h2>
 * <ul>
 *   <li>{@link PolyhedronCageOptions} - Natural edges (same as wireframe)</li>
 * </ul>
 * 
 * @see FillConfig
 * @see FillMode#CAGE
 */
public sealed interface CageOptions 
    permits SphereCageOptions, PrismCageOptions, CylinderCageOptions, PolyhedronCageOptions,
            RingCageOptions, ConeCageOptions, TorusCageOptions {
    
    /** Line width for cage rendering. */
    @Range(ValueRange.POSITIVE_NONZERO) 
    float lineWidth();
    
    /** Whether to show all structural edges. */
    boolean showEdges();
    
    /** Default line width. */
    float DEFAULT_LINE_WIDTH = 1.0f;
}

