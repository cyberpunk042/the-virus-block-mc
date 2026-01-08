package net.cyberpunk042.client.visual.transform;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.transform.Facing;
import org.joml.Quaternionf;

/**
 * Resolves {@link Facing} modes to rotation quaternions.
 * 
 * <p>Facing modes determine the static directional orientation of a primitive:</p>
 * <ul>
 *   <li>FIXED - No rotation (uses transform rotation)</li>
 *   <li>TOP - Shape normal points up (+Y), default for most shapes</li>
 *   <li>BOTTOM - Shape normal points down (-Y)</li>
 *   <li>FRONT - Shape normal points forward (+Z)</li>
 *   <li>BACK - Shape normal points backward (-Z)</li>
 *   <li>LEFT - Shape normal points left (-X)</li>
 *   <li>RIGHT - Shape normal points right (+X)</li>
 * </ul>
 * 
 * <p>Note: For dynamic camera-facing behavior, use {@link Billboard} instead.</p>
 * 
 * @see Facing
 * @see Billboard
 * @see TransformApplier
 */
public final class FacingResolver {
    
    private FacingResolver() {}
    
    // =========================================================================
    // Resolution
    // =========================================================================
    
    /**
     * Resolves a facing mode to a rotation quaternion.
     * 
     * <p>Uses the pitch/yaw/roll values from the Facing enum to create
     * a static directional rotation.</p>
     * 
     * @param facing The facing mode
     * @return Rotation quaternion
     */
    public static Quaternionf resolve(Facing facing) {
        if (facing == null || facing == Facing.FIXED || facing == Facing.TOP) {
            return new Quaternionf();  // Identity - no rotation needed
        }
        
        // Create rotation from Facing enum's pitch/yaw/roll values
        Quaternionf rotation = new Quaternionf();
        
        // Apply rotations in order: pitch (X), yaw (Y), roll (Z)
        if (facing.pitch() != 0) {
            rotation.rotateX((float) Math.toRadians(facing.pitch()));
        }
        if (facing.yaw() != 0) {
            rotation.rotateY((float) Math.toRadians(facing.yaw()));
        }
        if (facing.roll() != 0) {
            rotation.rotateZ((float) Math.toRadians(facing.roll()));
        }
        
        Logging.FIELD.topic("facing").trace("Resolved facing {} -> pitch={}, yaw={}, roll={}",
            facing, facing.pitch(), facing.yaw(), facing.roll());
        
        return rotation;
    }
    
    /**
     * Checks if the facing mode requires any rotation.
     * 
     * @param facing The facing mode
     * @return true if rotation is needed
     */
    public static boolean needsRotation(Facing facing) {
        return facing != null && facing != Facing.FIXED && facing != Facing.TOP;
    }
}
