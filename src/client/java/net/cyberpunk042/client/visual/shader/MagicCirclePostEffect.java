package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.client.gui.state.adapter.MagicCircleConfig;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.buffers.Std140Builder;
import org.joml.Matrix4f;

/**
 * Magic Circle ground effect post-processor.
 * 
 * <p>Renders animated magic circle patterns projected onto terrain.
 * Simpler than ShockwavePostEffect - no complex animation state machine.</p>
 */
public class MagicCirclePostEffect {
    
    private static final String LOG_TOPIC = "magic_circle";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static MagicCircleConfig config = MagicCircleConfig.DEFAULT;
    private static boolean enabled = false;
    private static long startTime = System.currentTimeMillis();
    
    // Camera state (updated each frame)
    private static float cameraX, cameraY, cameraZ;
    private static float forwardX, forwardY = 0, forwardZ = 1;
    private static float upX, upY = 1, upZ;
    private static Matrix4f invViewProj = new Matrix4f();
    
    // Flying detection
    private static boolean hiddenDueToFlying = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION STATE MACHINE (Phase 4)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Animation states for spawn/despawn transitions */
    public enum AnimationState {
        IDLE,       // Not animating - stage is static
        SPAWNING,   // Stage 0 → 8 (layers appearing)
        DESPAWNING  // Stage 8 → 0 (layers disappearing)
    }
    
    private static AnimationState animationState = AnimationState.IDLE;
    private static long animationStartTimeMs = 0;
    private static float animationStartStage = 0f;  // Stage value when animation started
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        Logging.RENDER.topic(LOG_TOPIC).info("MagicCirclePostEffect initialized");
    }
    
    public static PostEffectProcessor loadProcessor() {
        var client = MinecraftClient.getInstance();
        if (client == null) return null;
        
        var shaderLoader = client.getShaderLoader();
        if (shaderLoader == null) return null;
        
        try {
            var id = Identifier.of("the-virus-block", "magic_circle");
            return shaderLoader.loadPostEffect(id, net.minecraft.client.render.DefaultFramebufferSet.STAGES);
        } catch (Exception e) {
            Logging.RENDER.topic(LOG_TOPIC).error("Failed to load magic circle shader: {}", e.getMessage());
            return null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENABLE/DISABLE WITH ANIMATION SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() { return enabled; }
    
    /**
     * Set enabled state. If animateOnSpawn is true, triggers spawn/despawn animation.
     */
    public static void setEnabled(boolean state) {
        if (enabled == state) return;
        
        if (state) {
            // Enabling - start spawn animation if configured
            enabled = true;
            startTime = System.currentTimeMillis();
            
            if (config.animateOnSpawn()) {
                startSpawnAnimation();
            } else {
                // No animation - immediately set to full stage
                config = config.toBuilder().animationStage(8.0f).build();
                animationState = AnimationState.IDLE;
            }
            Logging.RENDER.topic(LOG_TOPIC).info("Magic Circle SPAWNING (animate={})", config.animateOnSpawn());
        } else {
            // Disabling - start despawn animation if configured
            if (config.animateOnSpawn() && config.animationStage() > 0.01f) {
                startDespawnAnimation();
                Logging.RENDER.topic(LOG_TOPIC).info("Magic Circle DESPAWNING");
                // Don't disable yet - wait for animation to complete
            } else {
                // No animation or already at stage 0 - immediately disable
                enabled = false;
                config = config.toBuilder().animationStage(0.0f).build();
                animationState = AnimationState.IDLE;
                Logging.RENDER.topic(LOG_TOPIC).info("Magic Circle disabled");
            }
        }
    }
    
    public static void toggle() {
        setEnabled(!enabled);
    }
    
    /**
     * Start spawn animation (stage 0 → 8).
     */
    public static void startSpawnAnimation() {
        animationState = AnimationState.SPAWNING;
        animationStartTimeMs = System.currentTimeMillis();
        animationStartStage = 0.0f;
        config = config.toBuilder().animationStage(0.0f).build();
    }
    
    /**
     * Start despawn animation (stage → 0).
     */
    public static void startDespawnAnimation() {
        animationState = AnimationState.DESPAWNING;
        animationStartTimeMs = System.currentTimeMillis();
        animationStartStage = config.animationStage();
    }
    
    /**
     * Tick the animation state machine. Call this each frame.
     * Advances animationStage based on elapsed time and stageSpeed.
     */
    public static void tickAnimation() {
        if (animationState == AnimationState.IDLE) {
            return;  // Nothing to animate
        }
        
        // Calculate elapsed time in seconds
        float elapsedSec = (System.currentTimeMillis() - animationStartTimeMs) / 1000.0f;
        
        // Calculate stages per second (inverse of stageSpeed which is seconds per stage)
        float stagesPerSecond = 1.0f / Math.max(0.01f, config.stageSpeed());
        float stagesElapsed = elapsedSec * stagesPerSecond;
        
        float newStage;
        boolean animationComplete = false;
        
        if (animationState == AnimationState.SPAWNING) {
            // Stage increases from 0 toward 8
            newStage = animationStartStage + stagesElapsed;
            if (newStage >= 8.0f) {
                newStage = 8.0f;
                animationComplete = true;
            }
        } else {
            // DESPAWNING: Stage decreases toward 0
            newStage = animationStartStage - stagesElapsed;
            if (newStage <= 0.0f) {
                newStage = 0.0f;
                animationComplete = true;
            }
        }
        
        // Update config with new stage
        config = config.toBuilder().animationStage(newStage).build();
        
        if (animationComplete) {
            if (animationState == AnimationState.DESPAWNING) {
                // Despawn complete - now actually disable
                enabled = false;
                Logging.RENDER.topic(LOG_TOPIC).info("Magic Circle despawn complete, disabled");
            } else {
                Logging.RENDER.topic(LOG_TOPIC).info("Magic Circle spawn complete");
            }
            animationState = AnimationState.IDLE;
        }
    }
    
    public static AnimationState getAnimationState() { return animationState; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIG
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static MagicCircleConfig getConfig() { return config; }
    
    public static void setConfig(MagicCircleConfig c) {
        config = c;
        enabled = c.enabled();
    }
    
    public static void updateFromConfig(MagicCircleConfig c) {
        config = c;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // POSITION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void setCenter(float x, float y, float z) {
        config = config.toBuilder()
            .centerX(x)
            .centerY(y)
            .centerZ(z)
            .build();
    }
    
    /**
     * Update camera position and handle follow mode with proper interpolation.
     * 
     * @param x Camera X
     * @param y Camera Y  
     * @param z Camera Z
     * @param tickDelta Partial tick for smooth interpolation (0.0-1.0)
     */
    public static void updateCameraPosition(float x, float y, float z, float tickDelta) {
        cameraX = x;
        cameraY = y;
        cameraZ = z;
        
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        var player = client.player;
        
        // Follow player if enabled - use interpolated PLAYER position for smooth movement
        // This ensures correct behavior in third-person view where camera != player
        if (config.followPlayer()) {
            // Use Minecraft's built-in interpolation API
            Vec3d lerpedPos = player.getLerpedPos(tickDelta);
            
            config = config.toBuilder()
                .centerX((float) lerpedPos.x)
                .centerY((float) lerpedPos.y)
                .centerZ((float) lerpedPos.z)
                .build();
        }
        
        // Flying detection: hide circle when player is too high above the circle's Y
        // Uses the current circle Y as the "ground" reference
        float playerY = (float) player.getY();
        float heightAboveCircle = playerY - config.centerY();
        hiddenDueToFlying = heightAboveCircle > config.heightTolerance();
    }
    
    /**
     * Returns true if the effect should be rendered.
     * False when disabled or when player is flying too high above circle.
     */
    public static boolean shouldRender() {
        return enabled && !hiddenDueToFlying;
    }
    
    public static void updateCameraForward(float x, float y, float z) {
        forwardX = x;
        forwardY = y;
        forwardZ = z;
    }
    
    public static void updateCameraUp(float x, float y, float z) {
        upX = x;
        upY = y;
        upZ = z;
    }
    
    public static void updateInvViewProj(Matrix4f m) {
        invViewProj.set(m);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UNIFORM BUFFER
    // ═══════════════════════════════════════════════════════════════════════════
    
    // UBO layout (Phase 3D - FINAL):
    // vec4 0-3: Global params (center, camera)
    // vec4 4-5: Camera/visual
    // mat4 (vec4 6-9): InvViewProj
    // vec4 10-11: Layer enables (8 floats)
    // vec4 12-13: Layer intensities (8 floats)
    // vec4 14-15: Layer speeds (8 floats)
    // vec4 16: Layer 4 Geometry
    // vec4 17-18: Layer 7 Geometry
    // vec4 19-20: Layer 2 Geometry
    // vec4 21-22: Layer 5 Geometry
    // vec4 23-24: Layer 3 Geometry
    // vec4 25-26: Layer 6 Geometry
    // vec4 27-28: Layer 1 Geometry (ring + radiation)
    // vec4 29-31: Layer 8 Geometry (breathing, orbital, center)
    // vec4 32: Animation direction + reserved
    public static final int VEC4_COUNT = 33;
    public static final int BUFFER_SIZE = VEC4_COUNT * 16;
    
    /**
     * Write uniforms to the UBO builder.
     */
    public static void writeUniformBuffer(Std140Builder builder, float aspectRatio, float fovRadians) {
        float time = (System.currentTimeMillis() - startTime) / 1000.0f;
        
        // vec4 0: Center position + radius
        builder.putFloat(config.centerX());
        builder.putFloat(config.centerY());
        builder.putFloat(config.centerZ());
        builder.putFloat(config.effectRadius());
        
        // vec4 1: Camera position + aspect
        builder.putFloat(cameraX);
        builder.putFloat(cameraY);
        builder.putFloat(cameraZ);
        builder.putFloat(aspectRatio);
        
        // vec4 2: Forward direction + FOV
        builder.putFloat(forwardX);
        builder.putFloat(forwardY);
        builder.putFloat(forwardZ);
        builder.putFloat(fovRadians);
        
        // vec4 3: Up direction xy + BreathTime + RotationTime
        // (upZ is rarely used significantly in ground projection, pack breathing there)
        builder.putFloat(upX);
        builder.putFloat(upY);
        builder.putFloat(time * config.breathingSpeed());  // BreathTime (was upZ)
        builder.putFloat(time * config.rotationSpeed());   // RotationTime
        
        // vec4 4: Visual settings
        builder.putFloat(config.intensity());
        builder.putFloat(config.glowExponent());
        builder.putFloat(config.heightTolerance());
        builder.putFloat(enabled ? 1.0f : 0.0f);
        
        // vec4 5: Primary color + breathing
        builder.putFloat(config.primaryR());
        builder.putFloat(config.primaryG());
        builder.putFloat(config.primaryB());
        builder.putFloat(config.breathingAmount());  // Use alpha slot for breathingAmount
        
        // mat4 (vec4 6-9): Inverse view-projection
        for (int col = 0; col < 4; col++) {
            builder.putFloat(invViewProj.get(0, col));
            builder.putFloat(invViewProj.get(1, col));
            builder.putFloat(invViewProj.get(2, col));
            builder.putFloat(invViewProj.get(3, col));
        }
        
        // vec4 10-11: Layer enables (1.0 = enabled, 0.0 = disabled)
        builder.putFloat(config.layer1Enable() ? 1f : 0f);
        builder.putFloat(config.layer2Enable() ? 1f : 0f);
        builder.putFloat(config.layer3Enable() ? 1f : 0f);
        builder.putFloat(config.layer4Enable() ? 1f : 0f);
        builder.putFloat(config.layer5Enable() ? 1f : 0f);
        builder.putFloat(config.layer6Enable() ? 1f : 0f);
        builder.putFloat(config.layer7Enable() ? 1f : 0f);
        builder.putFloat(config.layer8Enable() ? 1f : 0f);
        
        // vec4 12-13: Layer intensities
        builder.putFloat(config.layer1Intensity());
        builder.putFloat(config.layer2Intensity());
        builder.putFloat(config.layer3Intensity());
        builder.putFloat(config.layer4Intensity());
        builder.putFloat(config.layer5Intensity());
        builder.putFloat(config.layer6Intensity());
        builder.putFloat(config.layer7Intensity());
        builder.putFloat(config.layer8Intensity());
        
        // vec4 14-15: Layer speeds
        builder.putFloat(config.layer1Speed());
        builder.putFloat(config.layer2Speed());
        builder.putFloat(config.layer3Speed());
        builder.putFloat(config.layer4Speed());
        builder.putFloat(config.layer5Speed());
        builder.putFloat(config.layer6Speed());
        builder.putFloat(config.layer7Speed());
        builder.putFloat(config.layer8Speed());
        
        // ═══════════════════════════════════════════════════════════════════════
        // Phase 3A: Layer 4/7 Geometry
        // ═══════════════════════════════════════════════════════════════════════
        
        // vec4 16: Layer 4 Geometry (Middle Ring)
        builder.putFloat(config.layer4InnerRadius());
        builder.putFloat(config.layer4OuterRadius());
        builder.putFloat(config.layer4Thickness());
        builder.putFloat(config.layer4RotOffset());
        
        // vec4 17: Layer 7 Geometry (Inner Radiation) - part 1
        builder.putFloat(config.layer7InnerRadius());
        builder.putFloat(config.layer7OuterRadius());
        builder.putFloat((float) config.layer7SpokeCount());
        builder.putFloat(config.layer7Thickness());
        
        // vec4 18: Layer 7 Geometry - part 2
        builder.putFloat(config.layer7RotOffset());
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
        
        // ═══════════════════════════════════════════════════════════════════════
        // Phase 3B: Layer 2/5 Geometry
        // ═══════════════════════════════════════════════════════════════════════
        
        // vec4 19: Layer 2 Geometry (Hexagram) - part 1
        builder.putFloat((float) config.layer2RectCount());
        builder.putFloat(config.layer2RectSize());
        builder.putFloat(config.layer2Thickness());
        builder.putFloat(config.layer2RotOffset());
        
        // vec4 20: Layer 2 Geometry - part 2
        builder.putFloat(config.layer2SnapRotation() ? 1f : 0f);
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
        
        // vec4 21: Layer 5 Geometry (Inner Triangle) - part 1
        builder.putFloat((float) config.layer5RectCount());
        builder.putFloat(config.layer5RectSize());
        builder.putFloat(config.layer5Thickness());
        builder.putFloat(config.layer5RotOffset());
        
        // vec4 22: Layer 5 Geometry - part 2
        builder.putFloat(config.layer5SnapRotation() ? 1f : 0f);
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
        
        // ═══════════════════════════════════════════════════════════════════════
        // Phase 3C: Layer 3/6 Dot Ring Geometry
        // ═══════════════════════════════════════════════════════════════════════
        
        // vec4 23: Layer 3 Geometry (Outer Dot Ring) - part 1
        builder.putFloat((float) config.layer3DotCount());
        builder.putFloat(config.layer3OrbitRadius());
        builder.putFloat(config.layer3RingInner());
        builder.putFloat(config.layer3RingOuter());
        
        // vec4 24: Layer 3 Geometry - part 2
        builder.putFloat(config.layer3RingThickness());
        builder.putFloat(config.layer3DotRadius());
        builder.putFloat(config.layer3DotThickness());
        builder.putFloat(config.layer3RotOffset());
        
        // vec4 25: Layer 6 Geometry (Inner Dot Ring) - part 1
        builder.putFloat((float) config.layer6DotCount());
        builder.putFloat(config.layer6OrbitRadius());
        builder.putFloat(config.layer6RingInner());
        builder.putFloat(config.layer6RingOuter());
        
        // vec4 26: Layer 6 Geometry - part 2
        builder.putFloat(config.layer6RingThickness());
        builder.putFloat(config.layer6DotRadius());
        builder.putFloat(config.layer6DotThickness());
        builder.putFloat(config.layer6RotOffset());
        
        // ═══════════════════════════════════════════════════════════════════════
        // Phase 3D: Layer 1/8 Complex Geometry
        // ═══════════════════════════════════════════════════════════════════════
        
        // vec4 27: Layer 1 Geometry (Outer Ring) - part 1
        builder.putFloat(config.layer1RingInner());
        builder.putFloat(config.layer1RingOuter());
        builder.putFloat(config.layer1RingThickness());
        builder.putFloat(config.layer1RadInner());
        
        // vec4 28: Layer 1 Geometry - part 2
        builder.putFloat(config.layer1RadOuter());
        builder.putFloat((float) config.layer1RadCount());
        builder.putFloat(config.layer1RadThickness());
        builder.putFloat(config.layer1RotOffset());
        
        // vec4 29: Layer 8 Geometry (Spinning Core) - breathing
        builder.putFloat(config.layer8BreathAmp());
        builder.putFloat(config.layer8BreathCenter());
        builder.putFloat((float) config.layer8OrbitalCount());
        builder.putFloat(config.layer8OrbitalStart());
        
        // vec4 30: Layer 8 Geometry - orbital
        builder.putFloat(config.layer8OrbitalStep());
        builder.putFloat(config.layer8OrbitalDist());
        builder.putFloat(config.layer8OrbitalThickness());
        builder.putFloat(config.layer8CenterRadius());
        
        // vec4 31: Layer 8 Geometry - center + animation
        builder.putFloat(config.layer8CenterThickness());
        builder.putFloat(config.layer8RotOffset());
        builder.putFloat(config.animationStage());      // Phase 4: animation stage (0-8)
        builder.putFloat((float) config.transitionMode());  // Phase 4: 0=INSTANT, 1=FADE, 2=SCALE, 3=BOTH
        
        // vec4 32: Animation direction + reserved
        builder.putFloat(config.animationFromCenter() ? 1.0f : 0.0f);  // 1.0 = center outward, 0.0 = outside inward
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
        builder.putFloat(0f);  // reserved
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void setRadius(float r) {
        config = config.toBuilder().effectRadius(r).build();
    }
    
    public static void setIntensity(float i) {
        config = config.toBuilder().intensity(i).build();
    }
    
    public static void setColor(float r, float g, float b) {
        config = config.toBuilder()
            .primaryR(r)
            .primaryG(g)
            .primaryB(b)
            .build();
    }
    
    public static void setRotationSpeed(float s) {
        config = config.toBuilder().rotationSpeed(s).build();
    }
    
    public static void setFollowPlayer(boolean f) {
        config = config.toBuilder().followPlayer(f).build();
    }
    
    public static String getStatusString() {
        if (!enabled) return "Disabled";
        float time = (System.currentTimeMillis() - startTime) / 1000.0f;
        return String.format("Active | R=%.1f | I=%.2f | T=%.1fs", 
            config.effectRadius(), config.intensity(), time);
    }
}
