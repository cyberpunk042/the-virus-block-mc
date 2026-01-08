package net.cyberpunk042.client.visual.shader.shockwave;

/**
 * Type definitions for the Shockwave VFX system.
 * All records and enums used by ShockwavePostEffect.
 */
public final class ShockwaveTypes {
    
    private ShockwaveTypes() {} // Namespace only
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RING CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record RingParams(
        float thickness, float intensity, float animationSpeed, float maxRadius,
        int count, float spacing, float glowWidth, boolean contractMode
    ) {
        public static final RingParams DEFAULT = new RingParams(4.0f, 1.0f, 15.0f, 400.0f, 10, 8.0f, 8.0f, false);
        
        public RingParams withThickness(float v) { return new RingParams(v, intensity, animationSpeed, maxRadius, count, spacing, glowWidth, contractMode); }
        public RingParams withIntensity(float v) { return new RingParams(thickness, v, animationSpeed, maxRadius, count, spacing, glowWidth, contractMode); }
        public RingParams withCount(int v) { return new RingParams(thickness, intensity, animationSpeed, maxRadius, v, spacing, glowWidth, contractMode); }
        public RingParams withSpacing(float v) { return new RingParams(thickness, intensity, animationSpeed, maxRadius, count, v, glowWidth, contractMode); }
        public RingParams withGlowWidth(float v) { return new RingParams(thickness, intensity, animationSpeed, maxRadius, count, spacing, v, contractMode); }
        public RingParams withAnimationSpeed(float v) { return new RingParams(thickness, intensity, v, maxRadius, count, spacing, glowWidth, contractMode); }
        public RingParams withMaxRadius(float v) { return new RingParams(thickness, intensity, animationSpeed, v, count, spacing, glowWidth, contractMode); }
        public RingParams withContractMode(boolean v) { return new RingParams(thickness, intensity, animationSpeed, maxRadius, count, spacing, glowWidth, v); }
    }
    
    public record RingColor(float r, float g, float b, float opacity) {
        public static final RingColor DEFAULT = new RingColor(0f, 1f, 1f, 1f);
    }
    
    public record ScreenEffects(
        float blackout, float vignetteAmount, float vignetteRadius,
        float tintR, float tintG, float tintB, float tintAmount
    ) {
        public static final ScreenEffects NONE = new ScreenEffects(0f, 0f, 0.5f, 1f, 1f, 1f, 0f);
    }
    
    public record CameraState(
        float x, float y, float z,
        float forwardX, float forwardY, float forwardZ,
        float frozenX, float frozenY, float frozenZ
    ) {
        public static final CameraState ORIGIN = new CameraState(0,0,0, 0,0,1, 0,0,0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum ShapeType {
        POINT(0), SPHERE(1), TORUS(2), POLYGON(3), ORBITAL(4);
        private final int shaderCode;
        ShapeType(int code) { this.shaderCode = code; }
        public int getShaderCode() { return shaderCode; }
    }
    
    public record ShapeConfig(
        ShapeType type, float radius, float majorRadius, float minorRadius,
        int sideCount, float orbitDistance
    ) {
        public static final ShapeConfig POINT = new ShapeConfig(ShapeType.POINT, 0f, 0f, 0f, 0, 0f);
        
        public static ShapeConfig sphere(float radius) { return new ShapeConfig(ShapeType.SPHERE, radius, 0, 0, 0, 0); }
        public static ShapeConfig torus(float major, float minor) { return new ShapeConfig(ShapeType.TORUS, 0, major, minor, 0, 0); }
        public static ShapeConfig polygon(int sides, float radius) { return new ShapeConfig(ShapeType.POLYGON, radius, 0, 0, sides, 0); }
        public static ShapeConfig orbital(float mainRadius, float orbitalRadius, float orbitDistance, int count) {
            return new ShapeConfig(ShapeType.ORBITAL, mainRadius, 0, orbitalRadius, Math.max(1, Math.min(32, count)), orbitDistance);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR TYPES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record Color3f(float r, float g, float b) {
        public static final Color3f BLACK = new Color3f(0f, 0f, 0f);
        public static final Color3f WHITE = new Color3f(1f, 1f, 1f);
        public static final Color3f CYAN = new Color3f(0f, 1f, 1f);
    }
    
    public record Color4f(float r, float g, float b, float a) {
        public static final Color4f CYAN_FULL = new Color4f(0f, 1f, 1f, 1f);
        public static final Color4f WHITE_FULL = new Color4f(1f, 1f, 1f, 1f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORONA / ORBITAL CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record CoronaConfig(Color4f color, float width, float intensity, float rimPower, float rimFalloff) {
        public static final CoronaConfig DEFAULT = new CoronaConfig(Color4f.CYAN_FULL, 2.0f, 1.0f, 2.0f, 1.0f);
    }
    
    public record OrbitalVisualConfig(Color3f bodyColor, CoronaConfig corona) {
        public static final OrbitalVisualConfig DEFAULT = new OrbitalVisualConfig(Color3f.BLACK, CoronaConfig.DEFAULT);
    }
    
    public record BeamVisualConfig(Color3f bodyColor, CoronaConfig corona, float width, float widthScale, float taper) {
        public static final BeamVisualConfig DEFAULT = new BeamVisualConfig(Color3f.BLACK, CoronaConfig.DEFAULT, 0f, 0.3f, 1.0f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EASING AND TIMING
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum EasingType { 
        LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT;
        
        public net.cyberpunk042.util.math.EasingFunctions.Type toFunctionType() {
            return switch (this) {
                case LINEAR -> net.cyberpunk042.util.math.EasingFunctions.Type.LINEAR;
                case EASE_IN -> net.cyberpunk042.util.math.EasingFunctions.Type.EASE_IN;
                case EASE_OUT -> net.cyberpunk042.util.math.EasingFunctions.Type.EASE_OUT;
                case EASE_IN_OUT -> net.cyberpunk042.util.math.EasingFunctions.Type.EASE_IN_OUT;
            };
        }
    }
    
    public record AnimationTimingConfig(
        float orbitalSpeed, float orbitalSpawnDuration, float orbitalRetractDuration,
        EasingType orbitalSpawnEasing, EasingType orbitalRetractEasing,
        float beamHeight, float beamGrowDuration, float beamShrinkDuration,
        float beamHoldDuration, float beamWidthGrowFactor, float beamLengthGrowFactor,
        EasingType beamGrowEasing, EasingType beamShrinkEasing,
        float orbitalSpawnDelay, float beamStartDelay, float retractDelay,
        boolean autoRetractOnRingEnd
    ) {
        public static final AnimationTimingConfig DEFAULT = new AnimationTimingConfig(
            0.008f, 2500f, 1500f, EasingType.EASE_OUT, EasingType.EASE_IN,
            100f, 1500f, 800f, 0f, 0f, 1f, EasingType.EASE_OUT, EasingType.EASE_IN,
            0f, 0f, 0f, true
        );
        
        public AnimationTimingConfig withOrbitalSpeed(float v) {
            return new AnimationTimingConfig(v, orbitalSpawnDuration, orbitalRetractDuration, orbitalSpawnEasing, orbitalRetractEasing, beamHeight, beamGrowDuration, beamShrinkDuration, beamHoldDuration, beamWidthGrowFactor, beamLengthGrowFactor, beamGrowEasing, beamShrinkEasing, orbitalSpawnDelay, beamStartDelay, retractDelay, autoRetractOnRingEnd);
        }
        
        public AnimationTimingConfig withBeamHeight(float v) {
            return new AnimationTimingConfig(orbitalSpeed, orbitalSpawnDuration, orbitalRetractDuration, orbitalSpawnEasing, orbitalRetractEasing, v, beamGrowDuration, beamShrinkDuration, beamHoldDuration, beamWidthGrowFactor, beamLengthGrowFactor, beamGrowEasing, beamShrinkEasing, orbitalSpawnDelay, beamStartDelay, retractDelay, autoRetractOnRingEnd);
        }
    }
    
    public record OrbitalEffectConfig(
        OrbitalVisualConfig orbital, BeamVisualConfig beam,
        AnimationTimingConfig timing, float blendRadius, boolean combinedMode
    ) {
        public static final OrbitalEffectConfig DEFAULT = new OrbitalEffectConfig(
            OrbitalVisualConfig.DEFAULT, BeamVisualConfig.DEFAULT, AnimationTimingConfig.DEFAULT, 3.0f, true
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ORIGIN MODE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum OriginMode { CAMERA, TARGET }
}
