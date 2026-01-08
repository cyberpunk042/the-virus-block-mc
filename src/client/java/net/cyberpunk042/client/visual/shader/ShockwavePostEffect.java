package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.client.visual.shader.shockwave.*;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.hit.HitResult;
import com.mojang.blaze3d.buffers.Std140Builder;
import org.joml.Matrix4f;

/**
 * Manages the GPU shockwave post-effect.
 * Facade for ShockwaveController (logic) and ShockwaveRenderer (rendering).
 * 
 * <p>Refactored Jan 2026 to separate concerns.</p>
 */
public class ShockwavePostEffect {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION & LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        ShockwaveController.getInstance().init();
    }
    
    public static PostEffectProcessor loadProcessor() {
        return net.cyberpunk042.client.visual.shader.shockwave.ShockwaveRenderer.loadProcessor();
    }
    
    public static void writeUniformBuffer(Std140Builder builder, float aspectRatio, float fovRadians,
                                          float isFlying, Matrix4f invViewProj) {
        net.cyberpunk042.client.visual.shader.shockwave.ShockwaveRenderer.writeUniformBuffer(builder, aspectRatio, fovRadians, isFlying, invViewProj);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE & ANIMATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() { return ShockwaveController.getInstance().isEnabled(); }
    public static void setEnabled(boolean state) { ShockwaveController.getInstance().setEnabled(state); }
    
    public static void toggle() {
        boolean next = !isEnabled();
        setEnabled(next);
        net.cyberpunk042.log.Logging.RENDER.topic("shockwave_gpu").info("Toggled " + next);
    }
    
    public static void trigger() { ShockwaveController.getInstance().trigger(); }
    public static void triggerAtCursor(HitResult hit) { ShockwaveController.getInstance().triggerAtCursor(hit); }
    public static void triggerContract() { ShockwaveController.getInstance().triggerContract(); }
    
    public static boolean isAnimating() { return ShockwaveController.getInstance().isAnimating(); }
    public static void stopAnimation() { ShockwaveController.getInstance().stopAnimation(); }
    
    public static void tickOrbitalPhase() { ShockwaveController.getInstance().tickOrbitalPhase(); }
    public static void startOrbitalSpawn() { ShockwaveController.getInstance().startOrbitalSpawn(); }
    public static void startOrbitalRetract() { ShockwaveController.getInstance().startOrbitalRetract(); }
    
    public static String getStatusString() { return ShockwaveController.getInstance().getStatusString(); }

    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER ACCESSORS (Delegates)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static float getCurrentRadius() { return ShockwaveController.getInstance().getCurrentRadius(); }
    public static void setRadius(float r) { ShockwaveController.getInstance().setRadius(r); }
    
    public static float getThickness() { return ShockwaveController.getInstance().getRingParams().thickness(); }
    public static void setThickness(float t) {
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withThickness(t));
    }
    
    public static float getIntensity() { return ShockwaveController.getInstance().getRingParams().intensity(); }
    public static void setIntensity(float i) {
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withIntensity(i));
    }
    
    public static float getSpeed() { return ShockwaveController.getInstance().getRingParams().animationSpeed(); }
    public static void setSpeed(float s) {
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withAnimationSpeed(s));
    }
    
    public static float getMaxRadius() { return ShockwaveController.getInstance().getRingParams().maxRadius(); }
    public static void setMaxRadius(float r) {
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withMaxRadius(r));
    }
    
    // Advanced Params
    public static int getRingCount() { return ShockwaveController.getInstance().getRingParams().count(); }
    public static void setRingCount(int n) { 
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withCount(n));
    }
    
    public static float getRingSpacing() { return ShockwaveController.getInstance().getRingParams().spacing(); }
    public static void setRingSpacing(float s) {
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withSpacing(s));
    }
    
    public static float getGlowWidth() { return ShockwaveController.getInstance().getRingParams().glowWidth(); }
    public static void setGlowWidth(float w) {
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withGlowWidth(w));
    }
    
    public static boolean isContractMode() { return ShockwaveController.getInstance().getRingParams().contractMode(); }
    public static void setContractMode(boolean m) {
        var c = ShockwaveController.getInstance();
        c.setRingParams(c.getRingParams().withContractMode(m));
    }

    // Colors
    public static float getRingR() { return ShockwaveController.getInstance().getRingColor().r(); }
    public static float getRingG() { return ShockwaveController.getInstance().getRingColor().g(); }
    public static float getRingB() { return ShockwaveController.getInstance().getRingColor().b(); }
    public static float getRingOpacity() { return ShockwaveController.getInstance().getRingColor().opacity(); }
    
    public static void setRingColor(float r, float g, float b, float a) {
        ShockwaveController.getInstance().setRingColor(new RingColor(r, g, b, a));
    }
    
    // Screen Effects
    public static void setBlackout(float v) {
        var c = ShockwaveController.getInstance();
        var s = c.getScreenEffects();
        c.setScreenEffects(new ScreenEffects(v, s.vignetteAmount(), s.vignetteRadius(), s.tintR(), s.tintG(), s.tintB(), s.tintAmount()));
    }
    
    public static void setVignette(float amount, float radius) {
        var c = ShockwaveController.getInstance();
        var s = c.getScreenEffects();
        c.setScreenEffects(new ScreenEffects(s.blackout(), amount, radius, s.tintR(), s.tintG(), s.tintB(), s.tintAmount()));
    }
    
    public static void setTint(float r, float g, float b, float amount) {
        var c = ShockwaveController.getInstance();
        var s = c.getScreenEffects();
        c.setScreenEffects(new ScreenEffects(s.blackout(), s.vignetteAmount(), s.vignetteRadius(), r, g, b, amount));
    }
    
    public static void clearScreenEffects() { ShockwaveController.getInstance().clearScreenEffects(); }
    public static float getBlackoutAmount() { return ShockwaveController.getInstance().getScreenEffects().blackout(); }
    public static float getVignetteAmount() { return ShockwaveController.getInstance().getScreenEffects().vignetteAmount(); }
    public static float getVignetteRadius() { return ShockwaveController.getInstance().getScreenEffects().vignetteRadius(); }
    public static float getTintR() { return ShockwaveController.getInstance().getScreenEffects().tintR(); }
    public static float getTintG() { return ShockwaveController.getInstance().getScreenEffects().tintG(); }
    public static float getTintB() { return ShockwaveController.getInstance().getScreenEffects().tintB(); }
    public static float getTintAmount() { return ShockwaveController.getInstance().getScreenEffects().tintAmount(); }

    // Shape Config
    public static ShapeConfig getShapeConfig() { return ShockwaveController.getInstance().getShapeConfig(); }
    public static void setShape(ShapeConfig c) { ShockwaveController.getInstance().setShapeConfig(c); }
    public static void setShapePoint() { setShape(ShapeConfig.POINT); }
    public static void setShapeSphere(float r) { setShape(ShapeConfig.sphere(r)); }
    public static void setShapeTorus(float maj, float min) { setShape(ShapeConfig.torus(maj, min)); }
    public static void setShapePolygon(int s, float r) { setShape(ShapeConfig.polygon(s, r)); }
    public static void setShapeOrbital(float main, float orb, float dist, int c) { setShape(ShapeConfig.orbital(main, orb, dist, c)); }
    
    // Origin / Camera
    public static OriginMode getOriginMode() { return ShockwaveController.getInstance().getOriginMode(); }
    public static void setOriginMode(OriginMode m) { ShockwaveController.getInstance().setOriginMode(m); }
    public static void setTargetPosition(float x, float y, float z) { ShockwaveController.getInstance().setTargetPosition(x, y, z); }
    
    public static void setFollowCamera(boolean f) { ShockwaveController.getInstance().setFollowCamera(f); }
    public static boolean isFollowCamera() { return ShockwaveController.getInstance().isFollowCamera(); }
    public static void tickFollowPosition(float x, float y, float z) { ShockwaveController.getInstance().tickFollowPosition(x, y, z); }
    
    public static void updateCameraPosition(float x, float y, float z) { ShockwaveController.getInstance().updateCameraPosition(x, y, z); }
    public static void updateCameraForward(float x, float y, float z) { ShockwaveController.getInstance().updateCameraForward(x, y, z); }
    public static void updateInvViewProj(Matrix4f m) { ShockwaveController.getInstance().updateInvViewProj(m); }
    public static Matrix4f getInvViewProj() { return ShockwaveController.getInstance().getInvViewProj(); }
    
    public static float getForwardX() { return ShockwaveController.getInstance().getForwardX(); }
    public static float getForwardY() { return ShockwaveController.getInstance().getForwardY(); }
    public static float getForwardZ() { return ShockwaveController.getInstance().getForwardZ(); }
    
    public static float getTargetX() { return ShockwaveController.getInstance().getTargetX(); }
    public static float getTargetY() { return ShockwaveController.getInstance().getTargetY(); }
    public static float getTargetZ() { return ShockwaveController.getInstance().getTargetZ(); }
    public static float getFrozenCamX() { return ShockwaveController.getInstance().getCameraState().frozenX(); }
    public static float getFrozenCamY() { return ShockwaveController.getInstance().getCameraState().frozenY(); }
    public static float getFrozenCamZ() { return ShockwaveController.getInstance().getCameraState().frozenZ(); }
    public static boolean isTargetMode() { return ShockwaveController.getInstance().isTargetMode(); }
    
    // Orbital Effects (Config delegation)
    public static OrbitalEffectConfig getOrbitalEffectConfig() { return ShockwaveController.getInstance().getOrbitalEffectConfig(); }
    public static void setOrbitalEffectConfig(OrbitalEffectConfig c) { ShockwaveController.getInstance().setOrbitalEffectConfig(c); }
    
    public static OrbitalVisualConfig getOrbitalVisual() { return getOrbitalEffectConfig().orbital(); }
    public static void setOrbitalVisual(OrbitalVisualConfig v) { 
        var c = ShockwaveController.getInstance();
        var cur = c.getOrbitalEffectConfig();
        c.setOrbitalEffectConfig(new OrbitalEffectConfig(v, cur.beam(), cur.timing(), cur.blendRadius(), cur.combinedMode()));
    }
    
    public static BeamVisualConfig getBeamVisual() { return getOrbitalEffectConfig().beam(); }
    public static void setBeamVisual(BeamVisualConfig v) {
        var c = ShockwaveController.getInstance();
        var cur = c.getOrbitalEffectConfig();
        c.setOrbitalEffectConfig(new OrbitalEffectConfig(cur.orbital(), v, cur.timing(), cur.blendRadius(), cur.combinedMode()));
    }
    
    public static AnimationTimingConfig getAnimationTiming() { return getOrbitalEffectConfig().timing(); }
    public static void setAnimationTiming(AnimationTimingConfig v) {
        var c = ShockwaveController.getInstance();
        var cur = c.getOrbitalEffectConfig();
        c.setOrbitalEffectConfig(new OrbitalEffectConfig(cur.orbital(), cur.beam(), v, cur.blendRadius(), cur.combinedMode()));
    }
    
    public static float getBlendRadius() { return getOrbitalEffectConfig().blendRadius(); }
    public static void setBlendRadius(float v) {
        var c = ShockwaveController.getInstance();
        var cur = c.getOrbitalEffectConfig();
        c.setOrbitalEffectConfig(new OrbitalEffectConfig(cur.orbital(), cur.beam(), cur.timing(), v, cur.combinedMode()));
    }
    
    public static boolean getCombinedMode() { return getOrbitalEffectConfig().combinedMode(); }
    public static void setCombinedMode(boolean v) {
        var c = ShockwaveController.getInstance();
        var cur = c.getOrbitalEffectConfig();
        c.setOrbitalEffectConfig(new OrbitalEffectConfig(cur.orbital(), cur.beam(), cur.timing(), cur.blendRadius(), v));
    }
    
    // Convenience setters
    public static void setOrbitalSpeed(float v) {
        var c = ShockwaveController.getInstance();
        var cur = c.getOrbitalEffectConfig();
        c.setOrbitalEffectConfig(new OrbitalEffectConfig(cur.orbital(), cur.beam(), cur.timing().withOrbitalSpeed(v), cur.blendRadius(), cur.combinedMode()));
    }
    
    public static void setBeamHeight(float v) {
        var c = ShockwaveController.getInstance();
        var cur = c.getOrbitalEffectConfig();
        c.setOrbitalEffectConfig(new OrbitalEffectConfig(cur.orbital(), cur.beam(), cur.timing().withBeamHeight(v), cur.blendRadius(), cur.combinedMode()));
    }
    
    public static float getOrbitalPhase() { return ShockwaveController.getInstance().getOrbitalPhase(); }
    public static float getOrbitalSpawnProgress() { return ShockwaveController.getInstance().getOrbitalSpawnProgress(); }
    public static float getBeamProgress() { return ShockwaveController.getInstance().getBeamProgress(); }
    
    public static final int BUFFER_SIZE = ShockwaveUBO.BUFFER_SIZE;
    public static final int VEC4_COUNT = BUFFER_SIZE / 16;
    
    // TEST PRESETS
    public static String cycleTest() {
        var config = ShockwaveTestPresets.cycleNext();
        ShockwaveController c = ShockwaveController.getInstance();
        c.setRingParams(config.ringParams());
        c.setRingColor(config.ringColor());
        c.setScreenEffects(config.screenEffects());
        c.setShapeConfig(config.shapeConfig());
        if (config.shouldDisable()) c.setEnabled(false);
        if (config.shouldTrigger()) c.trigger();
        return config.name();
    }
    
    public static String getCurrentTestName() {
        return ShockwaveTestPresets.getCurrentName();
    }
}
