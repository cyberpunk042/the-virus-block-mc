package net.cyberpunk042.client.visual.ubo;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.client.visual.effect.uniform.FieldVisualUniformBinder;
import net.cyberpunk042.client.visual.effect.ShaderTimeSource;
import net.cyberpunk042.client.visual.ubo.base.CameraUBO;
import net.cyberpunk042.client.visual.ubo.base.FrameUBO;
import net.cyberpunk042.client.visual.ubo.base.LightUBO;
import net.cyberpunk042.mixin.client.GameRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.Map;

/**
 * Base UBO Binder - Manages the 5-layer onion base UBOs.
 * 
 * <h2>Purpose</h2>
 * <p>This class is responsible for building and binding the base UBOs
 * (Frame, Camera, Object, Material, Light) that are shared across all effects.</p>
 * 
 * <h2>Usage</h2>
 * <p>Call {@link #updateBaseUBOs(Map)} once per frame before any effect-specific
 * uniform updates. The base UBOs are written to the provided buffer map.</p>
 * 
 * <h2>Update Frequency</h2>
 * <ul>
 *   <li><b>FrameData:</b> Every frame (time, deltaTime)</li>
 *   <li><b>CameraData:</b> Every frame (matrices change)</li>
 *   <li><b>LightData:</b> Only when Tornado/volumetric effects active</li>
 * </ul>
 * 
 * @see FrameUBO
 * @see CameraUBO
 * @see LightUBO
 */
public final class BaseUBOBinder {
    
    private BaseUBOBinder() {} // Utility class
    
    // Track frame to avoid double-updates
    private static long lastFrameUpdate = -1;
    private static int frameCounter = 0;
    
    // Cached buffers (reused across frames)
    private static GpuBuffer frameBuffer = null;
    private static GpuBuffer cameraBuffer = null;
    
    /**
     * Updates all base UBOs for the current frame.
     * 
     * <p>This should be called once per frame, before any effect-specific
     * uniform updates. It writes FrameData and CameraData to the buffer map.</p>
     * 
     * @param uniformBuffers the buffer map from PostEffectPass
     */
    public static void updateBaseUBOs(Map<String, GpuBuffer> uniformBuffers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return;
        }
        
        // Only update once per frame (use our own counter)
        // Note: No public getTickCount() available, so we use world time
        long currentFrame = (long) (client.world.getTime());
        if (currentFrame == lastFrameUpdate) {
            return;
        }
        lastFrameUpdate = currentFrame;
        frameCounter++;
        
        try {
            updateFrameUBO(uniformBuffers, client);
            updateCameraUBO(uniformBuffers, client);
            // LightUBO only updated when needed (Tornado effect)
        } catch (Exception e) {
            // Log but don't crash - base UBOs failing shouldn't break effects
            if (frameCounter % 60 == 1) {
                System.err.println("[BaseUBOBinder] Failed to update base UBOs: " + e.getMessage());
            }
        }
    }
    
    /**
     * Updates FrameData UBO (binding 0).
     */
    private static void updateFrameUBO(Map<String, GpuBuffer> uniformBuffers, MinecraftClient client) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = 16;  // 1 vec4
            Std140Builder builder = Std140Builder.onStack(stack, size + 16);
            
            // Get time data
            float time = ShaderTimeSource.getTime();
            float deltaTime = client.getRenderTickCounter().getTickProgress(false) / 20f;
            
            // Build and write FrameUBO
            FrameUBO frameUbo = FrameUBO.from(time, deltaTime, frameCounter);
            ReflectiveUBOWriter.write(builder, frameUbo);
            
            // Close old buffer
            GpuBuffer old = frameBuffer;
            if (old != null) {
                old.close();
            }
            
            // Create new buffer
            frameBuffer = RenderSystem.getDevice().createBuffer(
                () -> "FrameData Base",
                16,
                builder.get()
            );
            
            uniformBuffers.put("FrameDataUBO", frameBuffer);
        }
    }
    
    /**
     * Updates CameraData UBO (binding 1).
     * 
     * CRITICAL: Uses the SAME data sources as PostEffectPassMixin to ensure consistency:
     * - Forward vectors from FieldVisualUniformBinder (not calculated independently)
     * - Camera position: 0,0,0 (camera-relative coordinates, camera at origin)
     * - Matrices from FieldVisualUniformBinder
     * - FOV from GameRendererAccessor.invokeGetFov()
     * - Aspect from window dimensions
     * - IsFlying from getEffectiveIsFlying() pattern
     */
    private static void updateCameraUBO(Map<String, GpuBuffer> uniformBuffers, MinecraftClient client) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = 224;  // 4 vec4 + 2 mat4 + 2 reserved vec4
            Std140Builder builder = Std140Builder.onStack(stack, size + 16);
            
            Camera camera = client.gameRenderer.getCamera();
            float tickDelta = client.getRenderTickCounter().getTickProgress(false);
            
            // Aspect ratio - SAME as PostEffectPassMixin
            float aspect = (float) client.getWindow().getFramebufferWidth() / 
                          (float) client.getWindow().getFramebufferHeight();
            
            // FOV - SAME as PostEffectPassMixin
            float dynamicFov = ((GameRendererAccessor) client.gameRenderer).invokeGetFov(camera, tickDelta, true);
            float fov = (float) Math.toRadians(dynamicFov);
            
            // Forward vector - FROM FieldVisualUniformBinder (SAME as PostEffectPassMixin)
            float forwardX = FieldVisualUniformBinder.getForwardX();
            float forwardY = FieldVisualUniformBinder.getForwardY();
            float forwardZ = FieldVisualUniformBinder.getForwardZ();
            
            // Up vector (world up, same as PostEffectPassMixin)
            float upX = 0f;
            float upY = 1f;
            float upZ = 0f;
            
            // Matrices - FROM FieldVisualUniformBinder (SAME as PostEffectPassMixin)
            Matrix4f viewProj = FieldVisualUniformBinder.getViewProjection();
            Matrix4f invViewProj = FieldVisualUniformBinder.getInvViewProjection();
            
            if (viewProj == null) {
                viewProj = new Matrix4f().identity();
            }
            if (invViewProj == null) {
                invViewProj = new Matrix4f().identity();
            }
            
            // IsFlying - SAME logic as PostEffectPassMixin.getEffectiveIsFlying()
            float isFlying = 0f;
            if (client.player != null) {
                if (client.player.getAbilities().flying) {
                    isFlying = 1f;
                } else if (camera.isThirdPerson()) {
                    isFlying = 1f;
                }
            }
            
            // Near/far planes (standard Minecraft values, same as PostEffectPassMixin)
            float near = 0.05f;
            float far = 1000f;
            
            // Camera position: 0,0,0 in camera-relative coordinates
            // (The shader uses camera-relative coords where camera is at origin)
            float camX = 0f;
            float camY = 0f;
            float camZ = 0f;
            
            // Build CameraUBO with SAME values as PostEffectPassMixin
            CameraUBO cameraUbo = CameraUBO.from(
                camX, camY, camZ,
                forwardX, forwardY, forwardZ,
                upX, upY, upZ,
                fov, aspect,
                near, far,
                isFlying > 0.5f,
                viewProj,
                invViewProj
            );
            ReflectiveUBOWriter.write(builder, cameraUbo);
            
            // Close old buffer
            GpuBuffer old = cameraBuffer;
            if (old != null) {
                old.close();
            }
            
            // Create new buffer
            cameraBuffer = RenderSystem.getDevice().createBuffer(
                () -> "CameraData Base",
                16,
                builder.get()
            );
            
            uniformBuffers.put("CameraDataUBO", cameraBuffer);
        }
    }
    
    /**
     * Updates LightData UBO (binding 4) for Tornado effect.
     * 
     * <p>Only call this when volumetric lighting is needed.</p>
     * 
     * @param uniformBuffers the buffer map
     * @param tornadoCenter center position of tornado effect
     * @param light0Strength strength of warm light
     * @param light1Strength strength of cool light
     */
    public static void updateLightUBO(
            Map<String, GpuBuffer> uniformBuffers,
            float[] tornadoCenter,
            float light0Strength,
            float light1Strength
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = 208;  // 13 vec4
            Std140Builder builder = Std140Builder.onStack(stack, size + 16);
            
            // Build LightUBO for Tornado (two-point volumetric)
            LightUBO lightUbo = LightUBO.forTornado(
                tornadoCenter[0], tornadoCenter[1], tornadoCenter[2],
                -3f, 2f, 0f, light0Strength,   // Light 0 offset + strength
                3f, 2f, 0f, light1Strength,    // Light 1 offset + strength
                0.01f, 0.01f, 0.002f           // Ambient
            );
            ReflectiveUBOWriter.write(builder, lightUbo);
            
            // Create buffer
            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(
                () -> "LightData Base",
                16,
                builder.get()
            );
            
            uniformBuffers.put("LightData", buffer);
        }
    }
    
    /**
     * Gets the current frame counter (for debugging).
     */
    public static int getFrameCounter() {
        return frameCounter;
    }
    
    /**
     * Resets state (for testing or mod reload).
     */
    public static void reset() {
        lastFrameUpdate = -1;
        frameCounter = 0;
        if (frameBuffer != null) {
            frameBuffer.close();
            frameBuffer = null;
        }
        if (cameraBuffer != null) {
            cameraBuffer.close();
            cameraBuffer = null;
        }
    }
}
