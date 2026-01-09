package net.cyberpunk042.client.visual.ubo;

import org.joml.Matrix4f;

/**
 * Shared Camera State Manager - The authoritative source for camera data.
 * 
 * <h2>Purpose</h2>
 * <p>This class holds camera state that is updated by WorldRenderer mixins
 * and consumed by BaseUBOBinder. It ensures that camera data is always
 * fresh regardless of which effects are enabled.</p>
 * 
 * <h2>Update Pattern</h2>
 * <p>ALL WorldRenderer*Mixin classes should call updateFromRender() at the
 * start of their injection, BEFORE adding any post-effect passes. This
 * ensures the camera data is current for all subsequent operations.</p>
 * 
 * <h2>Data Sources</h2>
 * <ul>
 *   <li><b>forward:</b> Computed from camera.getYaw()/getPitch()</li>
 *   <li><b>viewProj:</b> projectionMatrix.mul(positionMatrix)</li>
 *   <li><b>invViewProj:</b> viewProj.invert()</li>
 *   <li><b>worldPosition:</b> camX, camY, camZ from render method locals</li>
 * </ul>
 */
public final class CameraStateManager {
    
    private CameraStateManager() {} // Utility class
    
    // Forward vector (computed from yaw/pitch)
    private static float forwardX = 0f;
    private static float forwardY = 0f;
    private static float forwardZ = 1f;
    
    // View and projection matrices
    private static final Matrix4f viewProj = new Matrix4f();
    private static final Matrix4f invViewProj = new Matrix4f();
    
    // World camera position
    private static float worldPosX = 0f;
    private static float worldPosY = 64f;
    private static float worldPosZ = 0f;
    
    // Tick delta - MUST use the same value Minecraft used for this frame
    private static float tickDelta = 0f;
    
    /**
     * Updates camera state from render method.
     * 
     * <p>Called by WorldRenderer*Mixin classes with data captured from
     * the render injection point. Uses the SAME calculations that all
     * working effects have always used.</p>
     * 
     * <p>This is called every render frame by all mixins. The cost is
     * minimal (a few trig operations + matrix multiply), so we always
     * update to ensure smooth camera movement.</p>
     * 
     * @param camX Camera X position (from render method locals)
     * @param camY Camera Y position
     * @param camZ Camera Z position
     * @param cameraYaw Camera yaw in degrees
     * @param cameraPitch Camera pitch in degrees
     * @param positionMatrix Position/view matrix from render
     * @param projectionMatrix Projection matrix from render
     * @param capturedTickDelta The tickDelta captured from render method - MUST match what Minecraft used
     */
    public static void updateFromRender(
            float camX, float camY, float camZ,
            float cameraYaw, float cameraPitch,
            Matrix4f positionMatrix, Matrix4f projectionMatrix,
            float capturedTickDelta
    ) {
        // Always update - cost is minimal and fresh data is critical for smooth rendering
        
        // Store the EXACT tickDelta that Minecraft used for this frame
        tickDelta = capturedTickDelta;
        
        // Store world position
        worldPosX = camX;
        worldPosY = camY;
        worldPosZ = camZ;
        
        // Compute forward vector from yaw/pitch (SAME as all working mixins)
        float yaw = (float) Math.toRadians(cameraYaw);
        float pitch = (float) Math.toRadians(cameraPitch);
        
        // Clamp pitch to ±89° to prevent gimbal lock
        float maxPitch = (float) Math.toRadians(89.0);
        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));
        
        forwardX = (float) (-Math.sin(yaw) * Math.cos(pitch));
        forwardY = (float) (-Math.sin(pitch));
        forwardZ = (float) (Math.cos(yaw) * Math.cos(pitch));
        
        // Compute view-projection and inverse (SAME as all working mixins)
        viewProj.set(projectionMatrix).mul(positionMatrix);
        viewProj.invert(invViewProj);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static float getForwardX() { return forwardX; }
    public static float getForwardY() { return forwardY; }
    public static float getForwardZ() { return forwardZ; }
    
    public static float getWorldPosX() { return worldPosX; }
    public static float getWorldPosY() { return worldPosY; }
    public static float getWorldPosZ() { return worldPosZ; }
    
    public static float getTickDelta() { return tickDelta; }
    
    public static Matrix4f getViewProjection() { return new Matrix4f(viewProj); }
    public static Matrix4f getInvViewProjection() { return new Matrix4f(invViewProj); }
}
