package net.cyberpunk042.client.visual.shader.shockwave;

import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import org.joml.Matrix4f;

/**
 * Controller for the Shockwave VFX system.
 * Handles state management, animation logic, and configuration.
 */
public class ShockwaveController {
    
    private static final ShockwaveController INSTANCE = new ShockwaveController();
    
    public static ShockwaveController getInstance() {
        return INSTANCE;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean enabled = false;
    private boolean animating = false;
    private long animationStartTime = 0;
    private float currentRadius = 20.0f;
    
    private RingParams ringParams = RingParams.DEFAULT;
    private RingColor ringColor = RingColor.DEFAULT;
    private ScreenEffects screenEffects = ScreenEffects.NONE;
    private CameraState cameraState = CameraState.ORIGIN;
    private ShapeConfig shapeConfig = ShapeConfig.POINT;
    private OrbitalEffectConfig orbitalEffectConfig = OrbitalEffectConfig.DEFAULT;
    
    // Orbital animation state
    private float orbitalPhase = 0f;
    private float orbitalSpawnProgress = 0f;
    private long orbitalSpawnStartTime = 0;
    private boolean orbitalRetracting = false;
    private float beamProgress = 0f;
    private long beamStartTime = 0;
    private boolean beamShrinking = false;
    private long retractDelayStartTime = 0;
    private boolean waitingForRetractDelay = false;
    
    // Origin mode and target
    private OriginMode originMode = OriginMode.CAMERA;
    private float targetX = 0, targetY = 0, targetZ = 0;
    private boolean followCamera = false;
    
    private final Matrix4f cachedInvViewProj = new Matrix4f();

    private ShockwaveController() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOGIC & ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void init() {
        Logging.RENDER.topic("shockwave_gpu")
            .info("ShockwaveController initialized");
    }

    public void trigger() {
        this.enabled = true;
        this.animating = true;
        this.currentRadius = 0.0f;
        this.animationStartTime = System.currentTimeMillis();
        
        if (shapeConfig.type() == ShapeType.ORBITAL) {
            startOrbitalSpawn();
        }
        
        Logging.RENDER.topic("shockwave_gpu")
            .kv("speed", ringParams.animationSpeed())
            .kv("maxRadius", ringParams.maxRadius())
            .info("Shockwave triggered");
    }

    public void tickOrbitalPhase() {
        if (shapeConfig.type() == ShapeType.ORBITAL) {
            AnimationTimingConfig timing = orbitalEffectConfig.timing();
            orbitalPhase += timing.orbitalSpeed();
            if (orbitalPhase > 6.28318f) orbitalPhase -= 6.28318f;
            if (orbitalPhase < 0f) orbitalPhase += 6.28318f;
        }
        
        if (!enabled) return;
        
        if (shapeConfig.type() == ShapeType.ORBITAL) {
            updateOrbitalAnimation();
        }
    }
    
    private void updateOrbitalAnimation() {
            AnimationTimingConfig timing = orbitalEffectConfig.timing();
            long now = System.currentTimeMillis();
            
            // Spawn/retract animation
            if (orbitalRetracting) {
                float elapsed = now - orbitalSpawnStartTime;
                float linear = Math.min(1f, elapsed / timing.orbitalRetractDuration());
                orbitalSpawnProgress = applyEasing(1f - linear, timing.orbitalRetractEasing());
                
                if (orbitalSpawnProgress <= 0.01f) {
                    orbitalSpawnProgress = 0f;
                    orbitalRetracting = false;
                    enabled = false;
                }
            } else if (orbitalSpawnProgress < 1f) {
                float elapsed = now - orbitalSpawnStartTime;
                float linear = Math.min(1f, elapsed / timing.orbitalSpawnDuration());
                orbitalSpawnProgress = applyEasing(linear, timing.orbitalSpawnEasing());
                
                if (orbitalSpawnProgress >= 0.99f && beamProgress == 0f && !beamShrinking) {
                    beamStartTime = now + (long) timing.beamStartDelay();
                }
            }
            
            // Beam and Retract Logic
            if (beamShrinking) {
                float elapsed = now - beamStartTime;
                float linear = Math.min(1f, elapsed / timing.beamShrinkDuration());
                beamProgress = 1f - applyEasing(linear, timing.beamShrinkEasing());
                
                if (beamProgress <= 0.01f) {
                    beamProgress = 0f;
                    beamShrinking = false;
                    
                    if (timing.retractDelay() > 0) {
                        waitingForRetractDelay = true;
                        retractDelayStartTime = now;
                    } else {
                        startOrbitalRetract();
                    }
                }
            } else if (waitingForRetractDelay) {
                float elapsed = now - retractDelayStartTime;
                if (elapsed >= timing.retractDelay()) {
                    waitingForRetractDelay = false;
                    startOrbitalRetract();
                }
            } else if (orbitalSpawnProgress >= 0.99f && beamProgress < 1f && !orbitalRetracting && now >= beamStartTime) {
                float elapsed = now - beamStartTime;
                float linear = Math.min(1f, elapsed / timing.beamGrowDuration());
                beamProgress = applyEasing(linear, timing.beamGrowEasing());
            }
    }
    
    private float applyEasing(float t, EasingType easing) {
        return net.cyberpunk042.util.math.EasingFunctions.apply(t, easing.toFunctionType());
    }
    
    public void startOrbitalSpawn() {
        orbitalSpawnProgress = 0f;
        orbitalRetracting = false;
        orbitalSpawnStartTime = System.currentTimeMillis();
    }
    
    public void startOrbitalRetract() {
        orbitalRetracting = true;
        orbitalSpawnStartTime = System.currentTimeMillis();
    }
    
    public void triggerContract() {
        enabled = true;
        animating = true;
        ringParams = ringParams.withContractMode(true);
        currentRadius = ringParams.maxRadius();
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("shockwave_gpu")
            .kv("from", ringParams.maxRadius())
            .info("Contract animation triggered");
    }
    
    public float getCurrentRadius() {
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            float animRadius;
            float maxR = ringParams.maxRadius();
            float speed = ringParams.animationSpeed();
            
            if (ringParams.contractMode()) {
                animRadius = maxR - (elapsed * speed);
                if (animRadius <= 0.0f) {
                    animating = false;
                    enabled = false;
                    return 0.0f;
                }
            } else {
                animRadius = elapsed * speed;
                if (animRadius >= maxR) {
                    animating = false;
                    if (shapeConfig.type() == ShapeType.ORBITAL && !orbitalRetracting && !beamShrinking) {
                        beamShrinking = true;
                        beamStartTime = System.currentTimeMillis();
                    } else if (shapeConfig.type() != ShapeType.ORBITAL) {
                        enabled = false;
                    }
                    return maxR;
                }
            }
            return animRadius;
        }
        return currentRadius;
    }
    
    public void tickFollowPosition(float camX, float camY, float camZ) {
        if (followCamera && originMode == OriginMode.TARGET) {
            targetX = camX;
            targetY = camY;
            targetZ = camZ;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS & GETTERS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isAnimating() { return animating; }
    public void stopAnimation() { this.animating = false; }
    
    public void setRadius(float radius) {
        this.enabled = true;
        this.animating = false;
        this.currentRadius = Math.max(0f, radius);
    }
    
    public RingParams getRingParams() { return ringParams; }
    public void setRingParams(RingParams params) { this.ringParams = params; }
    
    public RingColor getRingColor() { return ringColor; }
    public void setRingColor(RingColor color) { this.ringColor = color; }
    
    public ScreenEffects getScreenEffects() { return screenEffects; }
    public void setScreenEffects(ScreenEffects effects) { this.screenEffects = effects; }
    public void clearScreenEffects() { this.screenEffects = ScreenEffects.NONE; }
    
    public ShapeConfig getShapeConfig() { return shapeConfig; }
    public void setShapeConfig(ShapeConfig config) { this.shapeConfig = config; }
    
    public OrbitalEffectConfig getOrbitalEffectConfig() { return orbitalEffectConfig; }
    public void setOrbitalEffectConfig(OrbitalEffectConfig config) { this.orbitalEffectConfig = config; }
    
    public OriginMode getOriginMode() { return originMode; }
    public void setOriginMode(OriginMode mode) { this.originMode = mode; }
    
    public boolean isFollowCamera() { return followCamera; }
    public void setFollowCamera(boolean follow) {
        this.followCamera = follow;
        if (follow) this.originMode = OriginMode.TARGET;
    }
    
    public void setTargetPosition(float x, float y, float z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.originMode = OriginMode.TARGET;
        
        // Freeze camera
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            var camEntity = client.getCameraEntity();
            if (camEntity == null) camEntity = client.player;
            var camPos = camEntity.getCameraPosVec(1.0f);
            this.cameraState = new CameraState(
                cameraState.x(), cameraState.y(), cameraState.z(),
                cameraState.forwardX(), cameraState.forwardY(), cameraState.forwardZ(),
                (float) camPos.x, (float) camPos.y, (float) camPos.z
            );
        }
    }
    
    public void updateCameraPosition(float x, float y, float z) {
        this.cameraState = new CameraState(
            x, y, z,
            cameraState.forwardX(), cameraState.forwardY(), cameraState.forwardZ(),
            cameraState.frozenX(), cameraState.frozenY(), cameraState.frozenZ()
        );
    }
    
    public void updateCameraForward(float x, float y, float z) {
        this.cameraState = new CameraState(
            cameraState.x(), cameraState.y(), cameraState.z(),
            x, y, z,
            cameraState.frozenX(), cameraState.frozenY(), cameraState.frozenZ()
        );
    }
    
    public void updateInvViewProj(Matrix4f invViewProj) {
        this.cachedInvViewProj.set(invViewProj);
    }
    
    public Matrix4f getInvViewProj() {
        return new Matrix4f(cachedInvViewProj);
    }

    // Accessors for UBO writing
    public CameraState getCameraState() { return cameraState; }
    public float getOrbitalPhase() { return orbitalPhase; }
    public float getOrbitalSpawnProgress() { return orbitalSpawnProgress; }
    public float getBeamProgress() { return beamProgress; }
    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }
    public float getTargetZ() { return targetZ; }
    public float getForwardX() { return cameraState.forwardX(); }
    public float getForwardY() { return cameraState.forwardY(); }
    public float getForwardZ() { return cameraState.forwardZ(); }
    public boolean isTargetMode() { return originMode == OriginMode.TARGET; }

    public String getStatusString() {
        if (!enabled) return "OFF";
        if (animating) {
            return String.format("ANIM r=%.1f spd=%.1f max=%.1f", 
                getCurrentRadius(), ringParams.animationSpeed(), ringParams.maxRadius());
        }
        return String.format("STATIC r=%.1f t=%.1f i=%.1f", 
            currentRadius, ringParams.thickness(), ringParams.intensity());
    }
    
    public void triggerAtCursor(HitResult hit) {
         if (hit != null && hit.getType() != HitResult.Type.MISS) {
            var pos = hit.getPos();
            setTargetPosition((float)pos.x, (float)pos.y, (float)pos.z);
            trigger();
        } else {
            originMode = OriginMode.CAMERA;
            trigger();
        }
    }
}
