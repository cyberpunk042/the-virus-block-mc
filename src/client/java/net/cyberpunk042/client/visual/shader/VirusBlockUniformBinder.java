package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cyberpunk042.client.helmet.HelmetHudConfig;
import net.cyberpunk042.client.helmet.HelmetHudState;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;

import java.util.List;

/**
 * Binds VirusBlockParams uniform buffer for the virus block post-effect shader.
 * 
 * <p>Updates block positions, camera data, and effect parameters each frame.
 * Called from CustomUniformBinder when the virus block effect is active.
 * 
 * <h2>UBO Layout (std140)</h2>
 * <pre>
 * VirusBlockParams:
 *   vec4 CameraPosTime;        // xyz = camera, w = time
 *   vec4 EffectFlags;          // x = blockCount, y = espEnabled, z = smokeEnabled, w = screenPoisonEnabled
 *   vec4 InvProjParams;        // x = near, y = far, z = fov, w = aspect
 *   vec4 CameraForward;        // xyz = forward, w = closestBlockDist
 *   vec4 CameraUp;             // xyz = up, w = reserved
 *   vec4 SmokeShape;           // x = height, y = spread, z = density, w = turbulence
 *   vec4 SmokeAnim;            // x = riseSpeed, y = swirl, z = noiseScale, w = fadeHeight
 *   vec4 SmokeColor;           // rgb = color, a = intensity
 *   vec4 ScreenPoisonParams;   // x = triggerDist, y = intensity, z = distortAmount, w = reserved
 *   vec4 ESPClose;             // rgb = color, a = threshold
 *   vec4 ESPMedium;            // rgb = color, a = threshold
 *   vec4 ESPFar;               // rgb = color, a = reserved
 *   vec4 ESPStyle;             // x = pulseSpeed, y = glowPower, z = glowSize, w = edgeSharpness
 *   vec4 BlockPos[32];         // xyz = position, w = intensity
 * </pre>
 */
public final class VirusBlockUniformBinder {
    
    // UBO size: 13 vec4s header + 32 vec4s blocks = 45 vec4s = 720 bytes
    private static final int UBO_SIZE = 45 * 16;  // 720 bytes
    private static final int MAX_BLOCKS = 32;
    
    // Cached GPU buffer
    private static GpuBuffer cachedBuffer = null;
    
    // Effect enable flags
    private static boolean enabled = true;
    private static boolean espEnabled = false;  // Only with helmet
    private static boolean smokeEnabled = true;  // 3D raymarched gas
    private static boolean fumeEnabled = true;   // 2D screen fume distortion
    private static boolean screenPoisonEnabled = true;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // 3D GAS PARAMETERS (configurable) - raymarch gas above blocks
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static float smokeHeight = 5.0f;       // Column height in blocks
    private static float smokeSpread = 0.5f;       // Column half-width (0.5 = 1 block total)
    private static float smokeDensity = 0.5f;      // Density multiplier
    private static float smokeTurbulence = 1.0f;
    private static float smokeRiseSpeed = 1.5f;
    private static float smokeSwirl = 0.3f;
    private static float smokeNoiseScale = 2.0f;
    private static float smokeFadeHeight = 0.5f;
    private static float smokeColorR = 0.1f;
    private static float smokeColorG = 0.6f;
    private static float smokeColorB = 0.15f;
    private static float smokeIntensity = 1.0f;
    private static float gasTimeScale = 1.0f;      // Animation speed (0 = frozen, 0.1 = slow-mo)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCREEN POISON / FUME PARAMETERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static float screenPoisonTriggerDist = 5.0f;
    private static float screenPoisonIntensity = 0.8f;
    private static float fumeNoiseScale = 64.0f;  // Default 64 (lower = smoother flames)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ESP PARAMETERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static float espCloseR = 0.9f, espCloseG = 0.2f, espCloseB = 0.2f;
    private static float espCloseDist = 30.0f;
    private static float espMediumR = 0.9f, espMediumG = 0.6f, espMediumB = 0.2f;
    private static float espMediumDist = 100.0f;
    private static float espFarR = 0.3f, espFarG = 0.8f, espFarB = 0.4f;
    private static float espPulseSpeed = 2.0f;
    private static float espGlowPower = 2.0f;
    private static float espGlowSize = 0.02f;
    private static float espEdgeSharpness = 10.0f;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENABLE/DISABLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setESPEnabled(boolean value) {
        espEnabled = value;
    }
    
    public static void setSmokeEnabled(boolean value) {
        smokeEnabled = value;
    }
    
    public static void setFumeEnabled(boolean value) {
        fumeEnabled = value;
    }
    
    public static boolean isFumeEnabled() {
        return fumeEnabled;
    }
    
    public static void setScreenPoisonEnabled(boolean value) {
        screenPoisonEnabled = value;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER SETTERS (for tuning panel or commands)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void setSmokeHeight(float v) { smokeHeight = Math.max(0.5f, Math.min(20f, v)); }
    public static void setSmokeSpread(float v) { smokeSpread = Math.max(0.5f, Math.min(10f, v)); }
    public static void setSmokeDensity(float v) { smokeDensity = Math.max(0f, Math.min(3f, v)); }
    public static void setSmokeTurbulence(float v) { smokeTurbulence = Math.max(0f, Math.min(3f, v)); }
    public static void setSmokeRiseSpeed(float v) { smokeRiseSpeed = Math.max(0.1f, Math.min(5f, v)); }
    public static void setSmokeSwirl(float v) { smokeSwirl = Math.max(0f, Math.min(2f, v)); }
    public static void setSmokeNoiseScale(float v) { smokeNoiseScale = Math.max(0.5f, Math.min(10f, v)); }
    public static void setSmokeFadeHeight(float v) { smokeFadeHeight = Math.max(0.1f, Math.min(2f, v)); }
    public static void setSmokeColor(float r, float g, float b) {
        smokeColorR = r; smokeColorG = g; smokeColorB = b;
    }
    public static void setSmokeIntensity(float v) { smokeIntensity = Math.max(0f, Math.min(3f, v)); }
    
    // Easy controls for 3D gas
    public static void setGasRadius(float v) { smokeSpread = Math.max(0.1f, Math.min(5f, v)); }
    public static float getGasRadius() { return smokeSpread; }
    public static void setGasIntensity(float v) { smokeDensity = Math.max(0.01f, Math.min(2f, v)); }
    public static float getGasIntensity() { return smokeDensity; }
    public static void setGasHeight(float v) { smokeHeight = Math.max(1f, Math.min(20f, v)); }
    public static float getGasHeight() { return smokeHeight; }
    
    public static void setScreenPoisonTriggerDist(float v) { screenPoisonTriggerDist = Math.max(1f, Math.min(20f, v)); }
    public static void setScreenPoisonIntensity(float v) { screenPoisonIntensity = Math.max(0f, Math.min(2f, v)); }
    public static void setFumeNoiseScale(float v) { fumeNoiseScale = Math.max(1f, Math.min(512f, v)); }
    public static float getFumeNoiseScale() { return fumeNoiseScale; }
    
    public static void setESPCloseDist(float v) { espCloseDist = Math.max(5f, Math.min(100f, v)); }
    public static void setESPMediumDist(float v) { espMediumDist = Math.max(20f, Math.min(300f, v)); }
    public static void setESPPulseSpeed(float v) { espPulseSpeed = Math.max(0.5f, Math.min(10f, v)); }
    public static void setESPGlowPower(float v) { espGlowPower = Math.max(0.5f, Math.min(5f, v)); }
    public static void setESPGlowSize(float v) { espGlowSize = Math.max(0.005f, Math.min(0.1f, v)); }
    public static void setESPEdgeSharpness(float v) { espEdgeSharpness = Math.max(1f, Math.min(30f, v)); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDING - Called from CustomUniformBinder
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Binds VirusBlockParams UBO to the render pass.
     * 
     * @param renderPass The active RenderPass
     * @return true if bound successfully, false if no blocks or disabled
     */
    public static boolean bindUniforms(RenderPass renderPass) {
        if (!enabled) return false;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;
        
        // Get block positions from telemetry (server-provided, unlimited range)
        var telemetryState = VirusBlockTelemetryState.get();
        List<BlockPos> blocks = telemetryState.getNearbyPositions();
        
        // If no blocks, skip
        if (blocks.isEmpty()) return false;
        
        // Check if ESP should be enabled (player has helmet)
        HelmetHudConfig config = HelmetHudConfig.get();
        boolean hasHelmet = hasAugmentedHelmet(client);
        boolean currentEspEnabled = espEnabled && hasHelmet && config.isMarkersEnabled();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, UBO_SIZE);
            
            Camera camera = client.gameRenderer.getCamera();
            Vec3d camPos = camera.getPos();
            float time = (System.currentTimeMillis() % 60000) / 1000.0f;  // 60 second loop
            
            // Find closest block distance (from telemetry)
            float closestDist = telemetryState.getClosestDistance();
            
            // === vec4 0: CameraPosTime ===
            builder.putVec4(
                (float) camPos.x,
                (float) camPos.y,
                (float) camPos.z,
                time
            );
            
            // === vec4 1: EffectFlags ===
            // x = blockCount, y = espEnabled, z = smokeEnabled (3D gas), w = fumeEnabled (2D screen)
            builder.putVec4(
                (float) Math.min(blocks.size(), MAX_BLOCKS),  // blockCount
                currentEspEnabled ? 1.0f : 0.0f,              // espEnabled
                smokeEnabled ? 1.0f : 0.0f,                   // smokeEnabled (3D gas)
                fumeEnabled ? 1.0f : 0.0f                     // fumeEnabled (2D screen fume)
            );
            
            // === vec4 2: InvProjParams ===
            float fov = (float) Math.toRadians(client.options.getFov().getValue());
            float aspect = (float) client.getWindow().getFramebufferWidth() / 
                          (float) client.getWindow().getFramebufferHeight();
            builder.putVec4(
                0.05f,      // near
                1000.0f,    // far
                fov,
                aspect
            );
            
            // === vec4 3: CameraForward ===
            Vec3d forward = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
            builder.putVec4(
                (float) forward.x,
                (float) forward.y,
                (float) forward.z,
                closestDist  // w = closest block distance
            );
            
            // === vec4 4: CameraUp ===
            // Approximate up vector (assumes no roll)
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            Vec3d up = Vec3d.fromPolar(pitch - 90, yaw);
            builder.putVec4(
                (float) up.x,
                (float) up.y,
                (float) up.z,
                0.0f
            );
            
            // === vec4 5: SmokeShape ===
            builder.putVec4(smokeHeight, smokeSpread, smokeDensity, smokeTurbulence);
            
            // === vec4 6: SmokeAnim ===
            builder.putVec4(smokeRiseSpeed, smokeSwirl, smokeNoiseScale, smokeFadeHeight);
            
            // === vec4 7: SmokeColor ===
            builder.putVec4(smokeColorR, smokeColorG, smokeColorB, smokeIntensity);
            
            // === vec4 8: ScreenPoisonParams ===
            // x = triggerDist, y = intensity, z = fumeNoiseScale, w = reserved
            builder.putVec4(screenPoisonTriggerDist, screenPoisonIntensity, fumeNoiseScale, 0.0f);
            
            // === vec4 9: ESPClose ===
            builder.putVec4(espCloseR, espCloseG, espCloseB, espCloseDist);
            
            // === vec4 10: ESPMedium ===
            builder.putVec4(espMediumR, espMediumG, espMediumB, espMediumDist);
            
            // === vec4 11: ESPFar ===
            builder.putVec4(espFarR, espFarG, espFarB, 0.0f);
            
            // === vec4 12: ESPStyle ===
            builder.putVec4(espPulseSpeed, espGlowPower, espGlowSize, espEdgeSharpness);
            
            // === vec4 13-44: BlockPos[32] ===
            int blockCount = Math.min(blocks.size(), MAX_BLOCKS);
            for (int i = 0; i < MAX_BLOCKS; i++) {
                if (i < blockCount) {
                    BlockPos pos = blocks.get(i);
                    float intensity = 1.0f;
                    builder.putVec4(pos.getX(), pos.getY(), pos.getZ(), intensity);
                } else {
                    builder.putVec4(0.0f, 0.0f, 0.0f, 0.0f);
                }
            }
            
            // Close old buffer
            if (cachedBuffer != null && !cachedBuffer.isClosed()) {
                cachedBuffer.close();
            }
            
            // Upload and bind
            cachedBuffer = RenderSystem.getDevice().createBuffer(
                () -> "VirusBlockParams UBO",
                16,  // usage: uniform buffer
                builder.get()
            );
            renderPass.setUniform("VirusBlockParams", cachedBuffer);
            
            return true;
            
        } catch (Exception e) {
            Logging.RENDER.topic("virus_block_bind")
                .kv("error", e.getMessage())
                .warn("Failed to bind VirusBlockParams");
            return false;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static float findClosestBlockDistance(List<BlockPos> blocks, Vec3d camPos) {
        float closest = 999.0f;
        for (BlockPos pos : blocks) {
            Vec3d blockCenter = Vec3d.ofCenter(pos);
            float dist = (float) camPos.distanceTo(blockCenter);
            if (dist < closest) {
                closest = dist;
            }
        }
        return closest;
    }
    
    private static boolean hasAugmentedHelmet(MinecraftClient client) {
        if (client.player == null) return false;
        var headSlot = client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        return !headSlot.isEmpty() && headSlot.isOf(net.cyberpunk042.registry.ModItems.AUGMENTED_HELMET);
    }
    
    /**
     * Returns status string for debugging.
     */
    public static String getStatusString() {
        int telemetryBlocks = VirusBlockTelemetryState.get().getNearbyPositions().size();
        return String.format("telemetry=%d esp=%s smoke=%s", 
            telemetryBlocks, espEnabled, smokeEnabled);
    }
    
    private VirusBlockUniformBinder() {}
}
