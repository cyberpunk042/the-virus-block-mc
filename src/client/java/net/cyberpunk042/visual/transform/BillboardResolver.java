package net.cyberpunk042.client.visual.transform;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.render.Camera;
import net.cyberpunk042.visual.transform.Billboard;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

/**
 * Applies {@link Billboard} modes to make primitives face the camera.
 * 
 * <p>Billboard modes:</p>
 * <ul>
 *   <li>NONE - No billboarding</li>
 *   <li>FULL - Full 3D billboarding (faces camera completely)</li>
 *   <li>Y_AXIS - Only rotates around Y axis (upright billboarding)</li>
 * </ul>
 * 
 * @see Billboard
 * @see Transform
 * @see TransformApplier
 */
public final class BillboardResolver {
    
    private BillboardResolver() {}
    
    // =========================================================================
    // Application
    // =========================================================================
    
    /**
     * Applies billboard rotation to the matrix stack.
     * 
     * @param billboard The billboard mode
     * @param matrices The matrix stack to modify
     * @param camera The camera to face
     */
    public static void apply(Billboard billboard, MatrixStack matrices, Camera camera) {
        if (billboard == null || billboard == Billboard.NONE || camera == null) {
            return;
        }
        
        Quaternionf rotation = switch (billboard) {
            case NONE -> null;
            
            case FULL -> {
                // Full billboard: face camera completely
                Quaternionf cameraRot = camera.getRotation();
                // Invert camera rotation so primitive faces camera
                yield new Quaternionf(cameraRot).conjugate();
            }
            
            case Y_AXIS -> {
                // Y-axis billboard: only rotate around Y
                float yaw = camera.getYaw();
                yield new Quaternionf().rotateY((float) Math.toRadians(-yaw + 180));
            }
        };
        
        if (rotation != null) {
            matrices.multiply(rotation);
            Logging.FIELD.topic("billboard").trace("Applied billboard {}", billboard);
        }
    }
    
    /**
     * Gets the billboard rotation without applying it.
     * 
     * @param billboard The billboard mode
     * @param camera The camera
     * @return Rotation quaternion, or null if no rotation needed
     */
    public static Quaternionf getRotation(Billboard billboard, Camera camera) {
        if (billboard == null || billboard == Billboard.NONE || camera == null) {
            return null;
        }
        
        return switch (billboard) {
            case NONE -> null;
            case FULL -> new Quaternionf(camera.getRotation()).conjugate();
            case Y_AXIS -> new Quaternionf().rotateY((float) Math.toRadians(-camera.getYaw() + 180));
        };
    }
}
