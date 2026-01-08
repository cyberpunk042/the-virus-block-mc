package net.cyberpunk042.client.visual.mesh.ray.geometry3d;

/**
 * No deformation - identity transform.
 */
public final class GeoNoDeformation implements GeoDeformationStrategy {
    
    public static final GeoNoDeformation INSTANCE = new GeoNoDeformation();
    
    private GeoNoDeformation() {}
    
    @Override
    public void apply(float[] position, float t, float[] fieldCenter, float intensity, float fieldRadius) {
        // No change
    }
    
    @Override
    public boolean isActive() {
        return false;
    }
}
