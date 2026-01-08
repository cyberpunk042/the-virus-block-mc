package net.cyberpunk042.visual.animation;

/**
 * How the travel effect computes 't' position for a vertex.
 * 
 * <p>Different shapes need different projection methods:</p>
 * <ul>
 *   <li><b>LINEAR</b>: Project onto axis (X/Y/Z) - default, works for jets, beams</li>
 *   <li><b>RADIAL</b>: Distance from center (XZ plane) - for rings, discs, tori</li>
 *   <li><b>ANGULAR</b>: Angle around Y axis - for rings, cylinders (travel around)</li>
 *   <li><b>SPHERICAL</b>: Distance from origin - for spheres (radial from center)</li>
 * </ul>
 * 
 * @see TravelEffectConfig
 */
public enum TravelDirection {
    /** 
     * Project onto specified axis (uses Axis.X/Y/Z).
     * Good for: jets, beams, prisms, cylinders (travel along length)
     */
    LINEAR("Linear"),
    
    /**
     * Distance from center in XZ plane, normalized to outer radius.
     * Good for: rings, discs, tori (travel from inner to outer)
     * t = 0 at center, t = 1 at outer edge
     */
    RADIAL("Radial"),
    
    /**
     * Angle around Y axis (0 to 2π mapped to 0-1).
     * Good for: rings, cylinders, tori (travel around circumference)
     * Uses atan2(z, x) for angle
     */
    ANGULAR("Angular"),
    
    /**
     * Distance from origin in 3D, normalized to max radius.
     * Good for: spheres (already the default for spheres)
     * t = 0 at center, t = 1 at surface
     */
    SPHERICAL("Spherical");
    
    private final String displayName;
    
    TravelDirection(String displayName) {
        this.displayName = displayName;
    }
    
    public String displayName() {
        return displayName;
    }
    
    public static TravelDirection fromString(String s) {
        if (s == null || s.isEmpty()) return LINEAR;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LINEAR;
        }
    }
}
