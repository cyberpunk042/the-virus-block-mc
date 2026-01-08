package net.cyberpunk042.client.visual.effect.uniform;

import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Caches camera/view data for field visual effects.
 * 
 * <p>Updated by mixin each frame, used by ReflectiveUBOWriter.</p>
 */
public final class FieldVisualUniformBinder {
    
    
    // Cached camera data (updated by mixin each frame)
    private static Vec3d cameraPosition = Vec3d.ZERO;
    private static Matrix4f invViewProjection = new Matrix4f();
    private static Matrix4f viewProjection = new Matrix4f();
    private static float globalTime = 0f;
    private static float forwardX = 0f;
    private static float forwardY = 0f;
    private static float forwardZ = 1f;  // Default: looking toward +Z
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA/TIME UPDATES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates camera position. Called from mixin each frame.
     */
    public static void updateCameraPosition(Vec3d pos) {
        cameraPosition = pos;
    }
    
    /**
     * Updates camera forward direction. Called from mixin each frame.
     */
    public static void updateCameraForward(float x, float y, float z) {
        forwardX = x;
        forwardY = y;
        forwardZ = z;
    }
    
    /**
     * Updates view-projection matrices. Called from mixin each frame.
     * @param invVP The inverse view-projection matrix
     * @param vp The view-projection matrix
     */
    public static void updateViewProjectionMatrices(Matrix4f invVP, Matrix4f vp) {
        invViewProjection.set(invVP);
        viewProjection.set(vp);
    }
    
    /**
     * Updates inverse view-projection matrix only (legacy support).
     */
    public static void updateInvViewProjection(Matrix4f matrix) {
        invViewProjection.set(matrix);
    }
    
    /**
     * Updates global animation time. Called from tick handler.
     */
    public static void tick(float deltaTime) {
        globalTime += deltaTime;
    }
    
    /**
     * Gets current global time (for animation sync).
     */
    public static float getGlobalTime() {
        return globalTime;
    }
    
    /**
     * Resets time to zero.
     */
    public static void resetTime() {
        globalTime = 0f;
    }
    
    public static float getForwardX() { return forwardX; }
    public static float getForwardY() { return forwardY; }
    public static float getForwardZ() { return forwardZ; }
    

    

    
    // NOTE: bindUniforms() method removed - UBO writing now handled by
    // ReflectiveUBOWriter called directly from PostEffectPassMixin
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS (for debug/status)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Vec3d getCameraPosition() {
        return cameraPosition;
    }
    
    public static Matrix4f getInvViewProjection() {
        return new Matrix4f(invViewProjection);
    }
    
    public static Matrix4f getViewProjection() {
        return new Matrix4f(viewProjection);
    }
    
    public static String getStatusString() {
        return String.format("cam=(%.1f,%.1f,%.1f) t=%.1f",
            cameraPosition.x, cameraPosition.y, cameraPosition.z, globalTime);
    }
    
    private FieldVisualUniformBinder() {}  // Prevent instantiation
}
