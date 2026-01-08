package net.cyberpunk042.client.visual.camera;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Unified camera utilities for extracting camera data from Minecraft.
 * 
 * <p>This is the SINGLE SOURCE OF TRUTH for camera information.
 * All shaders (shockwave, field_visual, etc.) should use this to ensure
 * consistent camera handling across all effects.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // In your render mixin:
 * CameraSnapshot snapshot = CameraUtils.capture(camera, projectionMatrix, fov, aspectRatio);
 * 
 * // Then pass to your UBO:
 * builder.putVec4(snapshot.posX(), snapshot.posY(), snapshot.posZ(), aspectRatio);
 * builder.putVec4(snapshot.forwardX(), snapshot.forwardY(), snapshot.forwardZ(), fov);
 * </pre>
 */
public final class CameraUtils {
    
    private CameraUtils() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA SNAPSHOT - Immutable capture of camera state at a point in time
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Immutable snapshot of camera state.
     * Create via {@link CameraUtils#capture}.
     */
    public record CameraSnapshot(
        // Position
        float posX, float posY, float posZ,
        
        // Orientation (normalized)
        float forwardX, float forwardY, float forwardZ,
        float rightX, float rightY, float rightZ,
        float upX, float upY, float upZ,
        
        // Projection parameters
        float fov,          // Vertical FOV in radians
        float aspectRatio,  // width / height
        float nearPlane,
        float farPlane
    ) {
        /**
         * Position as Vec3d.
         */
        public Vec3d positionVec() {
            return new Vec3d(posX, posY, posZ);
        }
        
        /**
         * Forward direction as Vector3f.
         */
        public Vector3f forwardVec() {
            return new Vector3f(forwardX, forwardY, forwardZ);
        }
        
        /**
         * Right direction as Vector3f.
         */
        public Vector3f rightVec() {
            return new Vector3f(rightX, rightY, rightZ);
        }
        
        /**
         * Up direction as Vector3f.
         */
        public Vector3f upVec() {
            return new Vector3f(upX, upY, upZ);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAPTURE METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Captures camera state from Minecraft's Camera object.
     * 
     * <p>This extracts the forward vector directly from the camera's quaternion
     * rotation, ensuring it matches exactly what Minecraft uses for rendering.</p>
     * 
     * @param camera       Minecraft's Camera object
     * @param fovRadians   Field of view in radians
     * @param aspectRatio  Screen width / height
     * @return             Immutable camera snapshot
     */
    public static CameraSnapshot capture(Camera camera, float fovRadians, float aspectRatio) {
        // Position
        Vec3d pos = camera.getPos();
        float px = (float) pos.x;
        float py = (float) pos.y;
        float pz = (float) pos.z;
        
        // Get rotation quaternion from camera
        Quaternionf rotation = camera.getRotation();
        
        // Extract forward vector by transforming -Z (OpenGL convention)
        // In OpenGL, forward is -Z in view space
        Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f);
        rotation.transform(forward);
        
        // Extract right vector by transforming +X
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
        rotation.transform(right);
        
        // Extract up vector by transforming +Y
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        rotation.transform(up);
        
        return new CameraSnapshot(
            px, py, pz,
            forward.x, forward.y, forward.z,
            right.x, right.y, right.z,
            up.x, up.y, up.z,
            fovRadians, aspectRatio,
            0.05f, 1000.0f  // Standard Minecraft near/far
        );
    }
    
    /**
     * Captures camera state with custom near/far planes.
     */
    public static CameraSnapshot capture(Camera camera, float fovRadians, float aspectRatio,
                                         float nearPlane, float farPlane) {
        CameraSnapshot base = capture(camera, fovRadians, aspectRatio);
        return new CameraSnapshot(
            base.posX, base.posY, base.posZ,
            base.forwardX, base.forwardY, base.forwardZ,
            base.rightX, base.rightY, base.rightZ,
            base.upX, base.upY, base.upZ,
            fovRadians, aspectRatio,
            nearPlane, farPlane
        );
    }
    
    /**
     * Captures camera state using explicit position and angles.
     * Use when you need to override camera position or orientation.
     * 
     * @param posX         Camera X position
     * @param posY         Camera Y position
     * @param posZ         Camera Z position
     * @param yawDegrees   Yaw rotation in degrees
     * @param pitchDegrees Pitch rotation in degrees
     * @param fovRadians   Field of view in radians
     * @param aspectRatio  Screen width / height
     * @return             Camera snapshot
     */
    public static CameraSnapshot captureExplicit(
            float posX, float posY, float posZ,
            float yawDegrees, float pitchDegrees,
            float fovRadians, float aspectRatio
    ) {
        // Convert to radians
        float yaw = (float) Math.toRadians(yawDegrees);
        float pitch = (float) Math.toRadians(pitchDegrees);
        
        // Clamp pitch to prevent gimbal lock
        float maxPitch = (float) Math.toRadians(89.0);
        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));
        
        // Calculate forward vector
        // In Minecraft: yaw 0 = south (+Z), yaw 90 = west (-X)
        float cosPitch = (float) Math.cos(pitch);
        float forwardX = (float) (-Math.sin(yaw) * cosPitch);
        float forwardY = (float) (-Math.sin(pitch));
        float forwardZ = (float) (Math.cos(yaw) * cosPitch);
        
        // Calculate right vector (cross product of forward and world up)
        // Using the same formula as the shader
        float rightX = forwardZ;  // Simplified from cross(forward, worldUp)
        float rightY = 0.0f;
        float rightZ = -forwardX;
        float rightLen = (float) Math.sqrt(rightX * rightX + rightZ * rightZ);
        
        if (rightLen < 0.001f) {
            // Looking straight up or down - use world X as right
            rightX = 1.0f;
            rightY = 0.0f;
            rightZ = 0.0f;
        } else {
            rightX /= rightLen;
            rightZ /= rightLen;
        }
        
        // Calculate up vector (cross product of right and forward)
        float upX = rightY * forwardZ - rightZ * forwardY;
        float upY = rightZ * forwardX - rightX * forwardZ;
        float upZ = rightX * forwardY - rightY * forwardX;
        
        return new CameraSnapshot(
            posX, posY, posZ,
            forwardX, forwardY, forwardZ,
            rightX, rightY, rightZ,
            upX, upY, upZ,
            fovRadians, aspectRatio,
            0.05f, 1000.0f
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Calculates the world position offset from camera based on a camera-relative offset.
     * 
     * @param snapshot     Camera state
     * @param rightOffset  Offset in camera-right direction
     * @param upOffset     Offset in camera-up direction
     * @param forwardOffset Offset in camera-forward direction
     * @return             World position
     */
    public static Vec3d cameraRelativePosition(CameraSnapshot snapshot, 
                                                float rightOffset, float upOffset, float forwardOffset) {
        return new Vec3d(
            snapshot.posX + snapshot.rightX * rightOffset + snapshot.upX * upOffset + snapshot.forwardX * forwardOffset,
            snapshot.posY + snapshot.rightY * rightOffset + snapshot.upY * upOffset + snapshot.forwardY * forwardOffset,
            snapshot.posZ + snapshot.rightZ * rightOffset + snapshot.upZ * upOffset + snapshot.forwardZ * forwardOffset
        );
    }
    
    /**
     * Calculates the world position in front of the camera at a given distance.
     */
    public static Vec3d positionInFront(CameraSnapshot snapshot, float distance) {
        return cameraRelativePosition(snapshot, 0, 0, distance);
    }
    
    /**
     * Gets the current FOV from Minecraft client options.
     */
    public static float getCurrentFov() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null) {
            return (float) Math.toRadians(client.options.getFov().getValue());
        }
        return (float) Math.toRadians(70.0);  // Default
    }
    
    /**
     * Gets the current aspect ratio from Minecraft window.
     */
    public static float getCurrentAspectRatio() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            return (float) client.getWindow().getFramebufferWidth() / 
                   (float) client.getWindow().getFramebufferHeight();
        }
        return 16.0f / 9.0f;  // Default
    }
}
