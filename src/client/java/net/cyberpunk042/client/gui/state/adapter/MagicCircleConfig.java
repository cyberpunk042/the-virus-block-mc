package net.cyberpunk042.client.gui.state.adapter;

/**
 * Magic Circle ground effect configuration.
 * 
 * <p>Phase 2: Per-layer enable, intensity, and rotation speed controls.</p>
 * 
 * <p>The configuration is organized into logical groups:</p>
 * <ul>
 *   <li><b>Global</b>: Position, radius, intensity</li>
 *   <li><b>Colors</b>: Primary glow color</li>
 *   <li><b>Animation</b>: Rotation speed, breathing</li>
 *   <li><b>Layer Controls</b>: Per-layer enable/intensity/speed</li>
 * </ul>
 * 
 * <p><b>Layer Index Mapping:</b></p>
 * <ul>
 *   <li>0 = Outer Ring + Radiation</li>
 *   <li>1 = Hexagram (6 rectangles)</li>
 *   <li>2 = Outer Dot Ring</li>
 *   <li>3 = Middle Ring</li>
 *   <li>4 = Inner Triangle</li>
 *   <li>5 = Inner Dot Ring</li>
 *   <li>6 = Inner Radiation</li>
 *   <li>7 = Spinning Core</li>
 * </ul>
 */
public record MagicCircleConfig(
    // ═══════════════════════════════════════════════════════════════════════
    // GLOBAL SETTINGS
    // ═══════════════════════════════════════════════════════════════════════
    float effectRadius,        // World-space radius in blocks
    float heightTolerance,     // Y-axis tolerance for ground detection
    float intensity,           // Master brightness (0-5)
    float glowExponent,        // Power curve for final glow (1-5)
    boolean enabled,           // Master enable
    
    // ═══════════════════════════════════════════════════════════════════════
    // COLORS
    // ═══════════════════════════════════════════════════════════════════════
    float primaryR,            // Primary color RGB (0-1)
    float primaryG,
    float primaryB,
    
    // ═══════════════════════════════════════════════════════════════════════
    // ANIMATION
    // ═══════════════════════════════════════════════════════════════════════
    float rotationSpeed,       // Global rotation speed multiplier
    float breathingSpeed,      // Scale pulsing speed
    float breathingAmount,     // Scale pulsing amplitude
    
    // ═══════════════════════════════════════════════════════════════════════
    // POSITIONING (Runtime, not saved)
    // ═══════════════════════════════════════════════════════════════════════
    float centerX,             // World X position
    float centerY,             // World Y position (ground level)
    float centerZ,             // World Z position
    boolean followPlayer,      // Follow player position
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER CONTROLS (Phase 2)
    // ═══════════════════════════════════════════════════════════════════════
    // Layer enables (8 layers)
    boolean layer1Enable,      // Outer Ring + Radiation
    boolean layer2Enable,      // Hexagram
    boolean layer3Enable,      // Outer Dot Ring
    boolean layer4Enable,      // Middle Ring
    boolean layer5Enable,      // Inner Triangle
    boolean layer6Enable,      // Inner Dot Ring
    boolean layer7Enable,      // Inner Radiation
    boolean layer8Enable,      // Spinning Core
    
    // Layer intensities (0-2 multiplier)
    float layer1Intensity,
    float layer2Intensity,
    float layer3Intensity,
    float layer4Intensity,
    float layer5Intensity,
    float layer6Intensity,
    float layer7Intensity,
    float layer8Intensity,
    
    // Layer rotation speeds (-2 to 2 multiplier, negative = reverse)
    float layer1Speed,
    float layer2Speed,
    float layer3Speed,
    float layer4Speed,
    float layer5Speed,
    float layer6Speed,
    float layer7Speed,
    float layer8Speed,
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 4 GEOMETRY - Middle Ring (Phase 3A)
    // ═══════════════════════════════════════════════════════════════════════
    float layer4InnerRadius,   // Inner edge of ring (0.0-1.0) default: 0.5
    float layer4OuterRadius,   // Outer edge of ring (0.0-1.0) default: 0.55
    float layer4Thickness,     // Line power (0.0005-0.02) default: 0.002
    float layer4RotOffset,     // Initial angle offset (0-2π) default: 0.0
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 7 GEOMETRY - Inner Radiation (Phase 3A)
    // ═══════════════════════════════════════════════════════════════════════
    float layer7InnerRadius,   // Spoke start radius (0.0-1.0) default: 0.25
    float layer7OuterRadius,   // Spoke end radius (0.0-1.0) default: 0.3
    int layer7SpokeCount,      // Number of spokes (3-72) default: 12
    float layer7Thickness,     // Line power (0.0005-0.02) default: 0.005
    float layer7RotOffset,     // Initial angle offset (0-2π) default: 0.0
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 2 GEOMETRY - Hexagram (Phase 3B)
    // ═══════════════════════════════════════════════════════════════════════
    int layer2RectCount,       // Number of rectangles (3-12) default: 6
    float layer2RectSize,      // Rectangle half-extent (0.1-1.0) default: 0.601
    float layer2Thickness,     // Line power (0.0005-0.01) default: 0.0015
    float layer2RotOffset,     // Initial angle offset (0-2π) default: 0.0
    boolean layer2SnapRotation, // Use angular snapping default: true
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 5 GEOMETRY - Inner Triangle (Phase 3B)
    // ═══════════════════════════════════════════════════════════════════════
    int layer5RectCount,       // Number of rectangles (3-12) default: 3
    float layer5RectSize,      // Rectangle half-extent (0.1-1.0) default: 0.36
    float layer5Thickness,     // Line power (0.0005-0.01) default: 0.0015
    float layer5RotOffset,     // Initial angle offset (0-2π) default: 0.0
    boolean layer5SnapRotation, // Use angular snapping default: true
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 3 GEOMETRY - Outer Dot Ring (Phase 3C)
    // ═══════════════════════════════════════════════════════════════════════
    int layer3DotCount,        // Number of dots (4-36) default: 12
    float layer3OrbitRadius,   // Distance from center (0.1-1.0) default: 0.875
    float layer3RingInner,     // Ring inner edge (0.0-0.1) default: 0.001
    float layer3RingOuter,     // Ring outer edge (0.01-0.2) default: 0.05
    float layer3RingThickness, // Ring power (0.001-0.02) default: 0.004
    float layer3DotRadius,     // Center dot size (0.0-0.05) default: 0.001
    float layer3DotThickness,  // Dot brightness (0.001-0.03) default: 0.008
    float layer3RotOffset,     // Initial angle (0-2π) default: 0.262
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 6 GEOMETRY - Inner Dot Ring (Phase 3C)
    // ═══════════════════════════════════════════════════════════════════════
    int layer6DotCount,        // Number of dots (4-36) default: 12
    float layer6OrbitRadius,   // Distance from center (0.1-1.0) default: 0.53
    float layer6RingInner,     // Ring inner edge (0.0-0.1) default: 0.001
    float layer6RingOuter,     // Ring outer edge (0.01-0.2) default: 0.035
    float layer6RingThickness, // Ring power (0.001-0.02) default: 0.004
    float layer6DotRadius,     // Center dot size (0.0-0.05) default: 0.001
    float layer6DotThickness,  // Dot brightness (0.0005-0.03) default: 0.001
    float layer6RotOffset,     // Initial angle (0-2π) default: 0.262
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 1 GEOMETRY - Outer Ring + Radiation (Phase 3D)
    // ═══════════════════════════════════════════════════════════════════════
    float layer1RingInner,     // Ring inner edge (0.0-1.0) default: 0.85
    float layer1RingOuter,     // Ring outer edge (0.0-1.0) default: 0.9
    float layer1RingThickness, // Ring power (0.001-0.05) default: 0.006
    float layer1RadInner,      // Spoke start (0.0-1.0) default: 0.87
    float layer1RadOuter,      // Spoke end (0.0-1.0) default: 0.88
    int layer1RadCount,        // Number of spokes (3-72) default: 36
    float layer1RadThickness,  // Spoke power (0.0001-0.01) default: 0.0008
    float layer1RotOffset,     // Initial angle (0-2π) default: 0.0
    
    // ═══════════════════════════════════════════════════════════════════════
    // LAYER 8 GEOMETRY - Spinning Core (Phase 3D)
    // ═══════════════════════════════════════════════════════════════════════
    float layer8BreathAmp,     // Breathing amplitude (0.0-0.2) default: 0.04
    float layer8BreathCenter,  // Base scale (0.8-1.5) default: 1.1
    int layer8OrbitalCount,    // Number of orbitals (1-12) default: 6
    float layer8OrbitalStart,  // First circle radius (0.05-0.3) default: 0.13
    float layer8OrbitalStep,   // Radius decrement (0.0-0.05) default: 0.01
    float layer8OrbitalDist,   // Translation offset (0.0-0.3) default: 0.1
    float layer8OrbitalThickness, // Circle power (0.0005-0.01) default: 0.002
    float layer8CenterRadius,  // Center dot size (0.01-0.1) default: 0.04
    float layer8CenterThickness, // Center power (0.001-0.02) default: 0.004
    float layer8RotOffset,     // Initial angle (0-2π) default: 0.0
    
    // ═══════════════════════════════════════════════════════════════════════
    // STAGE ANIMATION (Phase 4)
    // ═══════════════════════════════════════════════════════════════════════
    float animationStage,      // Current stage (0-8), fractional = phase transition
    float stageSpeed,          // Seconds per stage transition (0.1-2.0) default: 0.5
    int transitionMode,        // 0=INSTANT, 1=FADE, 2=SCALE, 3=FADE_SCALE
    boolean animateOnSpawn,    // Whether spawn buttons trigger animation
    boolean animationFromCenter  // true = layers appear center→out (default), false = out→center
) {
    /** Number of layers */
    public static final int LAYER_COUNT = 8;
    
    /** Layer names for UI */
    public static final String[] LAYER_NAMES = {
        "Outer Ring", "Hexagram", "Outer Dots", "Middle Ring",
        "Inner Tri", "Inner Dots", "Inner Rad", "Core"
    };
    
    /**
     * Default configuration - golden/warm magic circle with all layers enabled.
     */
    public static final MagicCircleConfig DEFAULT = new MagicCircleConfig(
        // Global
        10.0f,      // effectRadius
        2.0f,       // heightTolerance
        1.0f,       // intensity
        2.5f,       // glowExponent
        false,      // enabled (off by default)
        // Colors - warm gold
        1.0f, 0.95f, 0.8f,
        // Animation
        1.0f,       // rotationSpeed
        1.0f,       // breathingSpeed
        0.02f,      // breathingAmount
        // Position
        0.0f, 64.0f, 0.0f,  // center (default at sea level)
        true,       // followPlayer
        // Layer enables - all on by default
        true, true, true, true, true, true, true, true,
        // Layer intensities - all at 1.0
        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
        // Layer speeds - default rotation directions from original
        1.0f,   // Layer 1: +time
        1.0f,   // Layer 2: +time
        1.0f,   // Layer 3: +time
        0.0f,   // Layer 4: static
        -1.0f,  // Layer 5: -time
        -1.0f,  // Layer 6: -time
        1.0f,   // Layer 7: +time
        -1.0f,  // Layer 8: -time
        // Layer 4 Geometry (Middle Ring) - from layer4_params.md
        0.5f,       // layer4InnerRadius
        0.55f,      // layer4OuterRadius
        0.002f,     // layer4Thickness
        0.0f,       // layer4RotOffset
        // Layer 7 Geometry (Inner Radiation) - from layer7_params.md
        0.25f,      // layer7InnerRadius
        0.3f,       // layer7OuterRadius
        12,         // layer7SpokeCount
        0.005f,     // layer7Thickness
        0.0f,       // layer7RotOffset
        // Layer 2 Geometry (Hexagram) - from layer2_params.md
        6,          // layer2RectCount
        0.601f,     // layer2RectSize (0.85/√2)
        0.0015f,    // layer2Thickness
        0.0f,       // layer2RotOffset
        true,       // layer2SnapRotation
        // Layer 5 Geometry (Inner Triangle) - from layer5_params.md
        3,          // layer5RectCount
        0.36f,      // layer5RectSize
        0.0015f,    // layer5Thickness
        0.0f,       // layer5RotOffset
        true,       // layer5SnapRotation
        // Layer 3 Geometry (Outer Dot Ring) - from layer3_params.md
        12,         // layer3DotCount
        0.875f,     // layer3OrbitRadius
        0.001f,     // layer3RingInner
        0.05f,      // layer3RingOuter
        0.004f,     // layer3RingThickness
        0.001f,     // layer3DotRadius
        0.008f,     // layer3DotThickness
        0.262f,     // layer3RotOffset (π/12)
        // Layer 6 Geometry (Inner Dot Ring) - from layer6_params.md
        12,         // layer6DotCount
        0.53f,      // layer6OrbitRadius
        0.001f,     // layer6RingInner
        0.035f,     // layer6RingOuter (smaller than L3)
        0.004f,     // layer6RingThickness
        0.001f,     // layer6DotRadius
        0.001f,     // layer6DotThickness (dimmer than L3)
        0.262f,     // layer6RotOffset (π/12)
        // Layer 1 Geometry (Outer Ring + Radiation) - from layer1_params.md
        0.85f,      // layer1RingInner
        0.9f,       // layer1RingOuter
        0.006f,     // layer1RingThickness
        0.87f,      // layer1RadInner
        0.88f,      // layer1RadOuter
        36,         // layer1RadCount
        0.0008f,    // layer1RadThickness
        0.0f,       // layer1RotOffset
        // Layer 8 Geometry (Spinning Core) - from layer8_params.md
        0.04f,      // layer8BreathAmp
        1.1f,       // layer8BreathCenter
        6,          // layer8OrbitalCount
        0.13f,      // layer8OrbitalStart
        0.01f,      // layer8OrbitalStep
        0.1f,       // layer8OrbitalDist
        0.002f,     // layer8OrbitalThickness
        0.04f,      // layer8CenterRadius
        0.004f,     // layer8CenterThickness
        0.0f,       // layer8RotOffset
        // Stage Animation (Phase 4)
        8.0f,       // animationStage (fully visible by default)
        0.5f,       // stageSpeed (seconds per stage)
        1,          // transitionMode (FADE)
        false,      // animateOnSpawn
        true        // animationFromCenter (center outward is default)
    );
    
    // ═══════════════════════════════════════════════════════════════════════
    // ACCESSOR HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Get layer enable by index (0-7) */
    public boolean getLayerEnable(int index) {
        return switch (index) {
            case 0 -> layer1Enable;
            case 1 -> layer2Enable;
            case 2 -> layer3Enable;
            case 3 -> layer4Enable;
            case 4 -> layer5Enable;
            case 5 -> layer6Enable;
            case 6 -> layer7Enable;
            case 7 -> layer8Enable;
            default -> false;
        };
    }
    
    /** Get layer intensity by index (0-7) */
    public float getLayerIntensity(int index) {
        return switch (index) {
            case 0 -> layer1Intensity;
            case 1 -> layer2Intensity;
            case 2 -> layer3Intensity;
            case 3 -> layer4Intensity;
            case 4 -> layer5Intensity;
            case 5 -> layer6Intensity;
            case 6 -> layer7Intensity;
            case 7 -> layer8Intensity;
            default -> 1.0f;
        };
    }
    
    /** Get layer speed by index (0-7) */
    public float getLayerSpeed(int index) {
        return switch (index) {
            case 0 -> layer1Speed;
            case 1 -> layer2Speed;
            case 2 -> layer3Speed;
            case 3 -> layer4Speed;
            case 4 -> layer5Speed;
            case 5 -> layer6Speed;
            case 6 -> layer7Speed;
            case 7 -> layer8Speed;
            default -> 1.0f;
        };
    }
    
    /**
     * Creates a builder initialized with this config's values.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }
    
    /**
     * Mutable builder for MagicCircleConfig.
     */
    public static class Builder {
        private float effectRadius;
        private float heightTolerance;
        private float intensity;
        private float glowExponent;
        private boolean enabled;
        private float primaryR, primaryG, primaryB;
        private float rotationSpeed;
        private float breathingSpeed;
        private float breathingAmount;
        private float centerX, centerY, centerZ;
        private boolean followPlayer;
        
        // Layer enables
        private boolean layer1Enable, layer2Enable, layer3Enable, layer4Enable;
        private boolean layer5Enable, layer6Enable, layer7Enable, layer8Enable;
        // Layer intensities
        private float layer1Intensity, layer2Intensity, layer3Intensity, layer4Intensity;
        private float layer5Intensity, layer6Intensity, layer7Intensity, layer8Intensity;
        // Layer speeds
        private float layer1Speed, layer2Speed, layer3Speed, layer4Speed;
        private float layer5Speed, layer6Speed, layer7Speed, layer8Speed;
        
        // Layer 4 Geometry (Phase 3A)
        private float layer4InnerRadius, layer4OuterRadius, layer4Thickness, layer4RotOffset;
        // Layer 7 Geometry (Phase 3A)
        private float layer7InnerRadius, layer7OuterRadius, layer7Thickness, layer7RotOffset;
        private int layer7SpokeCount;
        // Layer 2 Geometry (Phase 3B)
        private int layer2RectCount;
        private float layer2RectSize, layer2Thickness, layer2RotOffset;
        private boolean layer2SnapRotation;
        // Layer 5 Geometry (Phase 3B)
        private int layer5RectCount;
        private float layer5RectSize, layer5Thickness, layer5RotOffset;
        private boolean layer5SnapRotation;
        // Layer 3 Geometry (Phase 3C)
        private int layer3DotCount;
        private float layer3OrbitRadius, layer3RingInner, layer3RingOuter, layer3RingThickness;
        private float layer3DotRadius, layer3DotThickness, layer3RotOffset;
        // Layer 6 Geometry (Phase 3C)
        private int layer6DotCount;
        private float layer6OrbitRadius, layer6RingInner, layer6RingOuter, layer6RingThickness;
        private float layer6DotRadius, layer6DotThickness, layer6RotOffset;
        // Layer 1 Geometry (Phase 3D)
        private float layer1RingInner, layer1RingOuter, layer1RingThickness;
        private float layer1RadInner, layer1RadOuter, layer1RadThickness, layer1RotOffset;
        private int layer1RadCount;
        // Layer 8 Geometry (Phase 3D)
        private float layer8BreathAmp, layer8BreathCenter;
        private int layer8OrbitalCount;
        private float layer8OrbitalStart, layer8OrbitalStep, layer8OrbitalDist, layer8OrbitalThickness;
        private float layer8CenterRadius, layer8CenterThickness, layer8RotOffset;
        // Stage Animation (Phase 4)
        private float animationStage, stageSpeed;
        private int transitionMode;
        private boolean animateOnSpawn;
        private boolean animationFromCenter;
        
        public Builder() {
            this(DEFAULT);
        }
        
        public Builder(MagicCircleConfig src) {
            this.effectRadius = src.effectRadius;
            this.heightTolerance = src.heightTolerance;
            this.intensity = src.intensity;
            this.glowExponent = src.glowExponent;
            this.enabled = src.enabled;
            this.primaryR = src.primaryR;
            this.primaryG = src.primaryG;
            this.primaryB = src.primaryB;
            this.rotationSpeed = src.rotationSpeed;
            this.breathingSpeed = src.breathingSpeed;
            this.breathingAmount = src.breathingAmount;
            this.centerX = src.centerX;
            this.centerY = src.centerY;
            this.centerZ = src.centerZ;
            this.followPlayer = src.followPlayer;
            // Layer enables
            this.layer1Enable = src.layer1Enable;
            this.layer2Enable = src.layer2Enable;
            this.layer3Enable = src.layer3Enable;
            this.layer4Enable = src.layer4Enable;
            this.layer5Enable = src.layer5Enable;
            this.layer6Enable = src.layer6Enable;
            this.layer7Enable = src.layer7Enable;
            this.layer8Enable = src.layer8Enable;
            // Layer intensities
            this.layer1Intensity = src.layer1Intensity;
            this.layer2Intensity = src.layer2Intensity;
            this.layer3Intensity = src.layer3Intensity;
            this.layer4Intensity = src.layer4Intensity;
            this.layer5Intensity = src.layer5Intensity;
            this.layer6Intensity = src.layer6Intensity;
            this.layer7Intensity = src.layer7Intensity;
            this.layer8Intensity = src.layer8Intensity;
            // Layer speeds
            this.layer1Speed = src.layer1Speed;
            this.layer2Speed = src.layer2Speed;
            this.layer3Speed = src.layer3Speed;
            this.layer4Speed = src.layer4Speed;
            this.layer5Speed = src.layer5Speed;
            this.layer6Speed = src.layer6Speed;
            this.layer7Speed = src.layer7Speed;
            this.layer8Speed = src.layer8Speed;
            // Layer 4 Geometry
            this.layer4InnerRadius = src.layer4InnerRadius;
            this.layer4OuterRadius = src.layer4OuterRadius;
            this.layer4Thickness = src.layer4Thickness;
            this.layer4RotOffset = src.layer4RotOffset;
            // Layer 7 Geometry
            this.layer7InnerRadius = src.layer7InnerRadius;
            this.layer7OuterRadius = src.layer7OuterRadius;
            this.layer7SpokeCount = src.layer7SpokeCount;
            this.layer7Thickness = src.layer7Thickness;
            this.layer7RotOffset = src.layer7RotOffset;
            // Layer 2 Geometry
            this.layer2RectCount = src.layer2RectCount;
            this.layer2RectSize = src.layer2RectSize;
            this.layer2Thickness = src.layer2Thickness;
            this.layer2RotOffset = src.layer2RotOffset;
            this.layer2SnapRotation = src.layer2SnapRotation;
            // Layer 5 Geometry
            this.layer5RectCount = src.layer5RectCount;
            this.layer5RectSize = src.layer5RectSize;
            this.layer5Thickness = src.layer5Thickness;
            this.layer5RotOffset = src.layer5RotOffset;
            this.layer5SnapRotation = src.layer5SnapRotation;
            // Layer 3 Geometry
            this.layer3DotCount = src.layer3DotCount;
            this.layer3OrbitRadius = src.layer3OrbitRadius;
            this.layer3RingInner = src.layer3RingInner;
            this.layer3RingOuter = src.layer3RingOuter;
            this.layer3RingThickness = src.layer3RingThickness;
            this.layer3DotRadius = src.layer3DotRadius;
            this.layer3DotThickness = src.layer3DotThickness;
            this.layer3RotOffset = src.layer3RotOffset;
            // Layer 6 Geometry
            this.layer6DotCount = src.layer6DotCount;
            this.layer6OrbitRadius = src.layer6OrbitRadius;
            this.layer6RingInner = src.layer6RingInner;
            this.layer6RingOuter = src.layer6RingOuter;
            this.layer6RingThickness = src.layer6RingThickness;
            this.layer6DotRadius = src.layer6DotRadius;
            this.layer6DotThickness = src.layer6DotThickness;
            this.layer6RotOffset = src.layer6RotOffset;
            // Layer 1 Geometry
            this.layer1RingInner = src.layer1RingInner;
            this.layer1RingOuter = src.layer1RingOuter;
            this.layer1RingThickness = src.layer1RingThickness;
            this.layer1RadInner = src.layer1RadInner;
            this.layer1RadOuter = src.layer1RadOuter;
            this.layer1RadCount = src.layer1RadCount;
            this.layer1RadThickness = src.layer1RadThickness;
            this.layer1RotOffset = src.layer1RotOffset;
            // Layer 8 Geometry
            this.layer8BreathAmp = src.layer8BreathAmp;
            this.layer8BreathCenter = src.layer8BreathCenter;
            this.layer8OrbitalCount = src.layer8OrbitalCount;
            this.layer8OrbitalStart = src.layer8OrbitalStart;
            this.layer8OrbitalStep = src.layer8OrbitalStep;
            this.layer8OrbitalDist = src.layer8OrbitalDist;
            this.layer8OrbitalThickness = src.layer8OrbitalThickness;
            this.layer8CenterRadius = src.layer8CenterRadius;
            this.layer8CenterThickness = src.layer8CenterThickness;
            this.layer8RotOffset = src.layer8RotOffset;
            // Stage Animation (Phase 4)
            this.animationStage = src.animationStage;
            this.stageSpeed = src.stageSpeed;
            this.transitionMode = src.transitionMode;
            this.animateOnSpawn = src.animateOnSpawn;
            this.animationFromCenter = src.animationFromCenter;
        }
        
        // Global setters
        public Builder effectRadius(float v) { this.effectRadius = v; return this; }
        public Builder heightTolerance(float v) { this.heightTolerance = v; return this; }
        public Builder intensity(float v) { this.intensity = v; return this; }
        public Builder glowExponent(float v) { this.glowExponent = v; return this; }
        public Builder enabled(boolean v) { this.enabled = v; return this; }
        
        // Color setters
        public Builder primaryR(float v) { this.primaryR = v; return this; }
        public Builder primaryG(float v) { this.primaryG = v; return this; }
        public Builder primaryB(float v) { this.primaryB = v; return this; }
        
        // Animation setters
        public Builder rotationSpeed(float v) { this.rotationSpeed = v; return this; }
        public Builder breathingSpeed(float v) { this.breathingSpeed = v; return this; }
        public Builder breathingAmount(float v) { this.breathingAmount = v; return this; }
        
        // Position setters
        public Builder centerX(float v) { this.centerX = v; return this; }
        public Builder centerY(float v) { this.centerY = v; return this; }
        public Builder centerZ(float v) { this.centerZ = v; return this; }
        public Builder followPlayer(boolean v) { this.followPlayer = v; return this; }
        
        // Layer enable setters
        public Builder layer1Enable(boolean v) { this.layer1Enable = v; return this; }
        public Builder layer2Enable(boolean v) { this.layer2Enable = v; return this; }
        public Builder layer3Enable(boolean v) { this.layer3Enable = v; return this; }
        public Builder layer4Enable(boolean v) { this.layer4Enable = v; return this; }
        public Builder layer5Enable(boolean v) { this.layer5Enable = v; return this; }
        public Builder layer6Enable(boolean v) { this.layer6Enable = v; return this; }
        public Builder layer7Enable(boolean v) { this.layer7Enable = v; return this; }
        public Builder layer8Enable(boolean v) { this.layer8Enable = v; return this; }
        
        // Layer intensity setters
        public Builder layer1Intensity(float v) { this.layer1Intensity = v; return this; }
        public Builder layer2Intensity(float v) { this.layer2Intensity = v; return this; }
        public Builder layer3Intensity(float v) { this.layer3Intensity = v; return this; }
        public Builder layer4Intensity(float v) { this.layer4Intensity = v; return this; }
        public Builder layer5Intensity(float v) { this.layer5Intensity = v; return this; }
        public Builder layer6Intensity(float v) { this.layer6Intensity = v; return this; }
        public Builder layer7Intensity(float v) { this.layer7Intensity = v; return this; }
        public Builder layer8Intensity(float v) { this.layer8Intensity = v; return this; }
        
        // Layer speed setters
        public Builder layer1Speed(float v) { this.layer1Speed = v; return this; }
        public Builder layer2Speed(float v) { this.layer2Speed = v; return this; }
        public Builder layer3Speed(float v) { this.layer3Speed = v; return this; }
        public Builder layer4Speed(float v) { this.layer4Speed = v; return this; }
        public Builder layer5Speed(float v) { this.layer5Speed = v; return this; }
        public Builder layer6Speed(float v) { this.layer6Speed = v; return this; }
        public Builder layer7Speed(float v) { this.layer7Speed = v; return this; }
        public Builder layer8Speed(float v) { this.layer8Speed = v; return this; }
        
        // Layer 4 Geometry setters (Phase 3A)
        public Builder layer4InnerRadius(float v) { this.layer4InnerRadius = v; return this; }
        public Builder layer4OuterRadius(float v) { this.layer4OuterRadius = v; return this; }
        public Builder layer4Thickness(float v) { this.layer4Thickness = v; return this; }
        public Builder layer4RotOffset(float v) { this.layer4RotOffset = v; return this; }
        
        // Layer 7 Geometry setters (Phase 3A)
        public Builder layer7InnerRadius(float v) { this.layer7InnerRadius = v; return this; }
        public Builder layer7OuterRadius(float v) { this.layer7OuterRadius = v; return this; }
        public Builder layer7SpokeCount(int v) { this.layer7SpokeCount = v; return this; }
        public Builder layer7Thickness(float v) { this.layer7Thickness = v; return this; }
        public Builder layer7RotOffset(float v) { this.layer7RotOffset = v; return this; }
        
        // Layer 2 Geometry setters (Phase 3B)
        public Builder layer2RectCount(int v) { this.layer2RectCount = v; return this; }
        public Builder layer2RectSize(float v) { this.layer2RectSize = v; return this; }
        public Builder layer2Thickness(float v) { this.layer2Thickness = v; return this; }
        public Builder layer2RotOffset(float v) { this.layer2RotOffset = v; return this; }
        public Builder layer2SnapRotation(boolean v) { this.layer2SnapRotation = v; return this; }
        
        // Layer 5 Geometry setters (Phase 3B)
        public Builder layer5RectCount(int v) { this.layer5RectCount = v; return this; }
        public Builder layer5RectSize(float v) { this.layer5RectSize = v; return this; }
        public Builder layer5Thickness(float v) { this.layer5Thickness = v; return this; }
        public Builder layer5RotOffset(float v) { this.layer5RotOffset = v; return this; }
        public Builder layer5SnapRotation(boolean v) { this.layer5SnapRotation = v; return this; }
        
        // Layer 3 Geometry setters (Phase 3C)
        public Builder layer3DotCount(int v) { this.layer3DotCount = v; return this; }
        public Builder layer3OrbitRadius(float v) { this.layer3OrbitRadius = v; return this; }
        public Builder layer3RingInner(float v) { this.layer3RingInner = v; return this; }
        public Builder layer3RingOuter(float v) { this.layer3RingOuter = v; return this; }
        public Builder layer3RingThickness(float v) { this.layer3RingThickness = v; return this; }
        public Builder layer3DotRadius(float v) { this.layer3DotRadius = v; return this; }
        public Builder layer3DotThickness(float v) { this.layer3DotThickness = v; return this; }
        public Builder layer3RotOffset(float v) { this.layer3RotOffset = v; return this; }
        
        // Layer 6 Geometry setters (Phase 3C)
        public Builder layer6DotCount(int v) { this.layer6DotCount = v; return this; }
        public Builder layer6OrbitRadius(float v) { this.layer6OrbitRadius = v; return this; }
        public Builder layer6RingInner(float v) { this.layer6RingInner = v; return this; }
        public Builder layer6RingOuter(float v) { this.layer6RingOuter = v; return this; }
        public Builder layer6RingThickness(float v) { this.layer6RingThickness = v; return this; }
        public Builder layer6DotRadius(float v) { this.layer6DotRadius = v; return this; }
        public Builder layer6DotThickness(float v) { this.layer6DotThickness = v; return this; }
        public Builder layer6RotOffset(float v) { this.layer6RotOffset = v; return this; }
        
        // Layer 1 Geometry setters (Phase 3D)
        public Builder layer1RingInner(float v) { this.layer1RingInner = v; return this; }
        public Builder layer1RingOuter(float v) { this.layer1RingOuter = v; return this; }
        public Builder layer1RingThickness(float v) { this.layer1RingThickness = v; return this; }
        public Builder layer1RadInner(float v) { this.layer1RadInner = v; return this; }
        public Builder layer1RadOuter(float v) { this.layer1RadOuter = v; return this; }
        public Builder layer1RadCount(int v) { this.layer1RadCount = v; return this; }
        public Builder layer1RadThickness(float v) { this.layer1RadThickness = v; return this; }
        public Builder layer1RotOffset(float v) { this.layer1RotOffset = v; return this; }
        
        // Layer 8 Geometry setters (Phase 3D)
        public Builder layer8BreathAmp(float v) { this.layer8BreathAmp = v; return this; }
        public Builder layer8BreathCenter(float v) { this.layer8BreathCenter = v; return this; }
        public Builder layer8OrbitalCount(int v) { this.layer8OrbitalCount = v; return this; }
        public Builder layer8OrbitalStart(float v) { this.layer8OrbitalStart = v; return this; }
        public Builder layer8OrbitalStep(float v) { this.layer8OrbitalStep = v; return this; }
        public Builder layer8OrbitalDist(float v) { this.layer8OrbitalDist = v; return this; }
        public Builder layer8OrbitalThickness(float v) { this.layer8OrbitalThickness = v; return this; }
        public Builder layer8CenterRadius(float v) { this.layer8CenterRadius = v; return this; }
        public Builder layer8CenterThickness(float v) { this.layer8CenterThickness = v; return this; }
        public Builder layer8RotOffset(float v) { this.layer8RotOffset = v; return this; }
        
        // Stage Animation setters (Phase 4)
        public Builder animationStage(float v) { this.animationStage = v; return this; }
        public Builder stageSpeed(float v) { this.stageSpeed = v; return this; }
        public Builder transitionMode(int v) { this.transitionMode = v; return this; }
        public Builder animateOnSpawn(boolean v) { this.animateOnSpawn = v; return this; }
        public Builder animationFromCenter(boolean v) { this.animationFromCenter = v; return this; }
        
        // Indexed setters for UI convenience
        public Builder setLayerEnable(int index, boolean v) {
            switch (index) {
                case 0 -> layer1Enable = v; case 1 -> layer2Enable = v;
                case 2 -> layer3Enable = v; case 3 -> layer4Enable = v;
                case 4 -> layer5Enable = v; case 5 -> layer6Enable = v;
                case 6 -> layer7Enable = v; case 7 -> layer8Enable = v;
            }
            return this;
        }
        
        public Builder setLayerIntensity(int index, float v) {
            switch (index) {
                case 0 -> layer1Intensity = v; case 1 -> layer2Intensity = v;
                case 2 -> layer3Intensity = v; case 3 -> layer4Intensity = v;
                case 4 -> layer5Intensity = v; case 5 -> layer6Intensity = v;
                case 6 -> layer7Intensity = v; case 7 -> layer8Intensity = v;
            }
            return this;
        }
        
        public Builder setLayerSpeed(int index, float v) {
            switch (index) {
                case 0 -> layer1Speed = v; case 1 -> layer2Speed = v;
                case 2 -> layer3Speed = v; case 3 -> layer4Speed = v;
                case 4 -> layer5Speed = v; case 5 -> layer6Speed = v;
                case 6 -> layer7Speed = v; case 7 -> layer8Speed = v;
            }
            return this;
        }
        
        public MagicCircleConfig build() {
            return new MagicCircleConfig(
                effectRadius, heightTolerance, intensity, glowExponent, enabled,
                primaryR, primaryG, primaryB,
                rotationSpeed, breathingSpeed, breathingAmount,
                centerX, centerY, centerZ, followPlayer,
                layer1Enable, layer2Enable, layer3Enable, layer4Enable,
                layer5Enable, layer6Enable, layer7Enable, layer8Enable,
                layer1Intensity, layer2Intensity, layer3Intensity, layer4Intensity,
                layer5Intensity, layer6Intensity, layer7Intensity, layer8Intensity,
                layer1Speed, layer2Speed, layer3Speed, layer4Speed,
                layer5Speed, layer6Speed, layer7Speed, layer8Speed,
                // Layer 4 Geometry
                layer4InnerRadius, layer4OuterRadius, layer4Thickness, layer4RotOffset,
                // Layer 7 Geometry
                layer7InnerRadius, layer7OuterRadius, layer7SpokeCount, layer7Thickness, layer7RotOffset,
                // Layer 2 Geometry
                layer2RectCount, layer2RectSize, layer2Thickness, layer2RotOffset, layer2SnapRotation,
                // Layer 5 Geometry
                layer5RectCount, layer5RectSize, layer5Thickness, layer5RotOffset, layer5SnapRotation,
                // Layer 3 Geometry
                layer3DotCount, layer3OrbitRadius, layer3RingInner, layer3RingOuter, layer3RingThickness,
                layer3DotRadius, layer3DotThickness, layer3RotOffset,
                // Layer 6 Geometry
                layer6DotCount, layer6OrbitRadius, layer6RingInner, layer6RingOuter, layer6RingThickness,
                layer6DotRadius, layer6DotThickness, layer6RotOffset,
                // Layer 1 Geometry
                layer1RingInner, layer1RingOuter, layer1RingThickness,
                layer1RadInner, layer1RadOuter, layer1RadCount, layer1RadThickness, layer1RotOffset,
                // Layer 8 Geometry
                layer8BreathAmp, layer8BreathCenter, layer8OrbitalCount,
                layer8OrbitalStart, layer8OrbitalStep, layer8OrbitalDist, layer8OrbitalThickness,
                layer8CenterRadius, layer8CenterThickness, layer8RotOffset,
                // Stage Animation (Phase 4)
                animationStage, stageSpeed, transitionMode, animateOnSpawn, animationFromCenter
            );
        }
    }
}

