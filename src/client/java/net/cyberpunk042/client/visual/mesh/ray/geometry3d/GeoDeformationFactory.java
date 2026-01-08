package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

import net.cyberpunk042.visual.shape.FieldDeformationMode;

/**
 * Factory for creating GeoDeformationStrategy instances.
 * 
 * <p>Based on FieldDeformationMode enum values.</p>
 * 
 * @see net.cyberpunk042.visual.shape.FieldDeformationMode
 */
public final class GeoDeformationFactory {
    
    private GeoDeformationFactory() {}
    
    /**
     * Get the appropriate strategy for a deformation mode.
     */
    public static GeoDeformationStrategy get(FieldDeformationMode mode) {
        if (mode == null || mode == FieldDeformationMode.NONE) {
            return GeoNoDeformation.INSTANCE;
        }
        
        // All active deformation modes use GeoSpaghettification with the mode
        // to determine stretch direction (GRAVITATIONAL, REPULSION, TIDAL)
        return new GeoSpaghettification(mode);
    }
}
