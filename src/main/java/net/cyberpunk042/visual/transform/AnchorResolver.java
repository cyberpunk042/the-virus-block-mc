package net.cyberpunk042.visual.transform;

import net.cyberpunk042.log.Logging;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Vector3f;

/**
 * Resolves {@link Anchor} enum values to actual world-space offsets.
 * 
 * <p>Anchor positions are relative to the player or field origin:</p>
 * <ul>
 *   <li>CENTER - Player center (eye height / 2)</li>
 *   <li>FEET - Player feet (y=0 relative)</li>
 *   <li>HEAD - Player head (eye height)</li>
 *   <li>ABOVE - Above player head</li>
 *   <li>BELOW - Below player feet</li>
 *   <li>FRONT/BACK/LEFT/RIGHT - Directional offsets</li>
 * </ul>
 * 
 * @see Anchor
 * @see Transform
 * @see TransformApplier
 */
public final class AnchorResolver {
    
    private AnchorResolver() {}
    
    /** Default offset distance for directional anchors. */
    public static final float DIRECTIONAL_OFFSET = 1.0f;
    
    /** Offset above head. */
    public static final float ABOVE_OFFSET = 0.5f;
    
    /** Offset below feet. */
    public static final float BELOW_OFFSET = -0.5f;
    
    // =========================================================================
    // Resolution
    // =========================================================================
    
    /**
     * Resolves an anchor to a position offset.
     * 
     * @param anchor The anchor type
     * @param player The player (for eye height, facing direction)
     * @return Offset vector from player position
     */
    public static Vector3f resolve(Anchor anchor, PlayerEntity player) {
        if (anchor == null) anchor = Anchor.CENTER;
        
        float eyeHeight = player != null ? player.getStandingEyeHeight() : 1.62f;
        float yaw = player != null ? (float) Math.toRadians(player.getYaw()) : 0;
        
        Vector3f offset = switch (anchor) {
            case CENTER -> new Vector3f(0, eyeHeight / 2, 0);
            case FEET -> new Vector3f(0, 0, 0);
            case HEAD -> new Vector3f(0, eyeHeight, 0);
            case ABOVE -> new Vector3f(0, eyeHeight + ABOVE_OFFSET, 0);
            case BELOW -> new Vector3f(0, BELOW_OFFSET, 0);
            case FRONT -> {
                float x = (float) -Math.sin(yaw) * DIRECTIONAL_OFFSET;
                float z = (float) Math.cos(yaw) * DIRECTIONAL_OFFSET;
                yield new Vector3f(x, eyeHeight / 2, z);
            }
            case BACK -> {
                float x = (float) Math.sin(yaw) * DIRECTIONAL_OFFSET;
                float z = (float) -Math.cos(yaw) * DIRECTIONAL_OFFSET;
                yield new Vector3f(x, eyeHeight / 2, z);
            }
            case LEFT -> {
                float x = (float) Math.cos(yaw) * DIRECTIONAL_OFFSET;
                float z = (float) Math.sin(yaw) * DIRECTIONAL_OFFSET;
                yield new Vector3f(x, eyeHeight / 2, z);
            }
            case RIGHT -> {
                float x = (float) -Math.cos(yaw) * DIRECTIONAL_OFFSET;
                float z = (float) -Math.sin(yaw) * DIRECTIONAL_OFFSET;
                yield new Vector3f(x, eyeHeight / 2, z);
            }
        };
        
        Logging.FIELD.topic("anchor").trace("Resolved anchor {}: offset=({}, {}, {})", 
            anchor, offset.x, offset.y, offset.z);
        
        return offset;
    }
    
    /**
     * Resolves an anchor without player context (uses defaults).
     * 
     * @param anchor The anchor type
     * @return Offset vector
     */
    public static Vector3f resolve(Anchor anchor) {
        return resolve(anchor, null);
    }
}
