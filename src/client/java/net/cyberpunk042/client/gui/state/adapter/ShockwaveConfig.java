package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.ShapeType;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.EasingType;

/**
 * Shockwave configuration stored in FieldEditState.
 * 
 * <p>This record holds all shockwave visual effect parameters that are
 * saved per-field in the field definition JSON.</p>
 * 
 * <p>The configuration is organized into logical groups:</p>
 * <ul>
 *   <li><b>Shape</b>: Type and geometry (orbital count, distances)</li>
 *   <li><b>Ring</b>: Expanding ring appearance and animation</li>
 *   <li><b>Orbital</b>: Orbiting sphere body and corona colors</li>
 *   <li><b>Beam</b>: Beam geometry and colors</li>
 *   <li><b>Animation</b>: Timing, easing, delays</li>
 *   <li><b>Screen</b>: Post-processing effects (blackout, vignette, tint)</li>
 * </ul>
 */
public record ShockwaveConfig(
    // ═══════════════════════════════════════════════════════════════════════
    // SHAPE GEOMETRY
    // ═══════════════════════════════════════════════════════════════════════
    ShapeType shapeType,       // POINT, SPHERE, TORUS, POLYGON, ORBITAL
    float mainRadius,          // Central sphere radius (for ORBITAL mode)
    float orbitalRadius,       // Each orbital sphere size
    float orbitDistance,       // Distance from center to orbitals
    int orbitalCount,          // Number of orbital spheres (1-8)
    
    // ═══════════════════════════════════════════════════════════════════════
    // RING GEOMETRY & VISUAL
    // ═══════════════════════════════════════════════════════════════════════
    int ringCount,             // Number of concentric rings
    float ringSpacing,         // Distance between rings
    float ringThickness,       // Ring width (blocks)
    float ringMaxRadius,       // Maximum expansion radius
    float ringSpeed,           // Animation speed (blocks/sec)
    float ringGlowWidth,       // Glow falloff width
    float ringIntensity,       // Brightness
    boolean ringContractMode,  // false=expand, true=contract
    float ringColorR,          // Ring color RGB
    float ringColorG,
    float ringColorB,
    float ringColorOpacity,
    
    // ═══════════════════════════════════════════════════════════════════════
    // ORBITAL BODY COLOR
    // ═══════════════════════════════════════════════════════════════════════
    float orbitalBodyR,        // Sphere body color RGB
    float orbitalBodyG,
    float orbitalBodyB,
    
    // ═══════════════════════════════════════════════════════════════════════
    // ORBITAL CORONA
    // ═══════════════════════════════════════════════════════════════════════
    float orbitalCoronaR,      // Corona RGBA
    float orbitalCoronaG,
    float orbitalCoronaB,
    float orbitalCoronaA,
    float orbitalCoronaWidth,
    float orbitalCoronaIntensity,
    float orbitalRimPower,
    float orbitalRimFalloff,
    
    // ═══════════════════════════════════════════════════════════════════════
    // BEAM GEOMETRY
    // ═══════════════════════════════════════════════════════════════════════
    float beamHeight,          // Max height (0 = infinity)
    float beamWidth,           // Absolute width
    float beamWidthScale,      // Width as ratio of orbital radius
    float beamTaper,           // Taper factor
    
    // ═══════════════════════════════════════════════════════════════════════
    // BEAM BODY COLOR
    // ═══════════════════════════════════════════════════════════════════════
    float beamBodyR,           // Beam body color RGB
    float beamBodyG,
    float beamBodyB,
    
    // ═══════════════════════════════════════════════════════════════════════
    // BEAM CORONA
    // ═══════════════════════════════════════════════════════════════════════
    float beamCoronaR,         // Corona RGBA
    float beamCoronaG,
    float beamCoronaB,
    float beamCoronaA,
    float beamCoronaWidth,
    float beamCoronaIntensity,
    float beamRimPower,
    float beamRimFalloff,
    
    // ═══════════════════════════════════════════════════════════════════════
    // ANIMATION TIMING
    // ═══════════════════════════════════════════════════════════════════════
    float orbitalSpeed,             // Rotation speed (rad/frame)
    float orbitalSpawnDuration,     // ms to spawn
    float orbitalRetractDuration,   // ms to retract
    float beamGrowDuration,         // ms to grow
    float beamShrinkDuration,       // ms to shrink
    float beamHoldDuration,         // ms at full height
    float beamWidthGrowFactor,      // 0=fixed, 1=animated
    float beamLengthGrowFactor,     // 0=instant, 1=gradual
    
    // ═══════════════════════════════════════════════════════════════════════
    // EASING TYPES
    // ═══════════════════════════════════════════════════════════════════════
    EasingType orbitalSpawnEasing,  // Easing for orbital spawn
    EasingType orbitalRetractEasing,// Easing for orbital retract
    EasingType beamGrowEasing,      // Easing for beam grow
    EasingType beamShrinkEasing,    // Easing for beam shrink
    
    // ═══════════════════════════════════════════════════════════════════════
    // DELAYS
    // ═══════════════════════════════════════════════════════════════════════
    float orbitalSpawnDelay,        // ms delay before spawn
    float beamStartDelay,           // ms delay after spawn before beam
    float retractDelay,             // ms delay after beam shrink
    boolean autoRetractOnRingEnd,   // Auto-shrink when rings finish
    
    // ═══════════════════════════════════════════════════════════════════════
    // SCREEN EFFECTS
    // ═══════════════════════════════════════════════════════════════════════
    float blackout,            // 0-1
    float vignetteAmount,      // 0-1
    float vignetteRadius,      // inner radius
    float tintR,               // tint color RGB
    float tintG,
    float tintB,
    float tintAmount,          // 0-1
    
    // ═══════════════════════════════════════════════════════════════════════
    // SHAPE BLENDING
    // ═══════════════════════════════════════════════════════════════════════
    float blendRadius,          // Smooth min blend (0=sharp, 5+=unified)
    boolean combinedMode,       // true = combined shockwave from center, false = individual orbital sources
    
    // ═══════════════════════════════════════════════════════════════════════
    // GLOBAL SCALE & POSITIONING
    // ═══════════════════════════════════════════════════════════════════════
    float globalScale,          // Global size multiplier (0.0 - 2.0, default 1.0)
    boolean followPosition,     // Follow source primitive position each frame
    float cursorYOffset         // Y offset when spawning at cursor (default 2.0)
) {
    /**
     * Default configuration for orbital shockwave.
     */
    public static final ShockwaveConfig DEFAULT = new ShockwaveConfig(
        // Shape
        ShapeType.ORBITAL, 0f, 2f, 10f, 4,
        // Ring geometry & visual
        10, 8f, 1f, 400f, 15f, 8f, 1f, false,
        1f, 1f, 1f, 1f,  // ring color WHITE
        // Orbital body - BLACK
        0f, 0f, 0f,
        // Orbital corona - WHITE
        1f, 1f, 1f, 1f,  // coronaRGBA
        2f, 0.5f, 2f, 1.3f,  // coronaWidth/intensity/rimPower/rimFalloff
        // Beam geometry
        100f, 0f, 0.1f, 1f,  // height/width/widthScale/taper
        // Beam body - BLACK
        0f, 0f, 0f,
        // Beam corona - WHITE
        1f, 1f, 1f, 1f,  // coronaRGBA
        2f, 0.5f, 2f, 1f,  // coronaWidth/intensity/rimPower/rimFalloff
        // Animation timing
        0.008f, 2500f, 1500f, 3000f, 800f, 0f, 0f, 1f,
        // Easing types
        EasingType.EASE_OUT, EasingType.EASE_IN, EasingType.EASE_IN, EasingType.EASE_IN,
        // Delays
        0f, 0f, 0f, true,
        // Screen effects
        0f, 0f, 0.5f, 1f, 1f, 1f, 0f,
        // Blend
        0f, true,  // blendRadius, combinedMode (true = combined shockwave from center)
        // Global scale & positioning
        1f, false, 2f  // globalScale, followPosition, cursorYOffset
    );
    
    /**
     * Creates a builder initialized with this config's values.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }
    
    /**
     * Mutable builder for ShockwaveConfig.
     */
    public static class Builder {
        // All fields from the record
        private ShapeType shapeType;
        private float mainRadius, orbitalRadius, orbitDistance;
        private int orbitalCount;
        private int ringCount;
        private float ringSpacing, ringThickness, ringMaxRadius, ringSpeed;
        private float ringGlowWidth, ringIntensity;
        private boolean ringContractMode;
        private float ringColorR, ringColorG, ringColorB, ringColorOpacity;
        private float orbitalBodyR, orbitalBodyG, orbitalBodyB;
        private float orbitalCoronaR, orbitalCoronaG, orbitalCoronaB, orbitalCoronaA;
        private float orbitalCoronaWidth, orbitalCoronaIntensity, orbitalRimPower, orbitalRimFalloff;
        private float beamHeight, beamWidth, beamWidthScale, beamTaper;
        private float beamBodyR, beamBodyG, beamBodyB;
        private float beamCoronaR, beamCoronaG, beamCoronaB, beamCoronaA;
        private float beamCoronaWidth, beamCoronaIntensity, beamRimPower, beamRimFalloff;
        private float orbitalSpeed, orbitalSpawnDuration, orbitalRetractDuration;
        private float beamGrowDuration, beamShrinkDuration, beamHoldDuration;
        private float beamWidthGrowFactor, beamLengthGrowFactor;
        private EasingType orbitalSpawnEasing, orbitalRetractEasing, beamGrowEasing, beamShrinkEasing;
        private float orbitalSpawnDelay, beamStartDelay, retractDelay;
        private boolean autoRetractOnRingEnd;
        private float blackout, vignetteAmount, vignetteRadius;
        private float tintR, tintG, tintB, tintAmount;
        private float blendRadius;
        private boolean combinedMode;
        private float globalScale;
        private boolean followPosition;
        private float cursorYOffset;
        
        public Builder() {
            this(DEFAULT);
        }
        
        public Builder(ShockwaveConfig src) {
            this.shapeType = src.shapeType;
            this.mainRadius = src.mainRadius;
            this.orbitalRadius = src.orbitalRadius;
            this.orbitDistance = src.orbitDistance;
            this.orbitalCount = src.orbitalCount;
            this.ringCount = src.ringCount;
            this.ringSpacing = src.ringSpacing;
            this.ringThickness = src.ringThickness;
            this.ringMaxRadius = src.ringMaxRadius;
            this.ringSpeed = src.ringSpeed;
            this.ringGlowWidth = src.ringGlowWidth;
            this.ringIntensity = src.ringIntensity;
            this.ringContractMode = src.ringContractMode;
            this.ringColorR = src.ringColorR;
            this.ringColorG = src.ringColorG;
            this.ringColorB = src.ringColorB;
            this.ringColorOpacity = src.ringColorOpacity;
            this.orbitalBodyR = src.orbitalBodyR;
            this.orbitalBodyG = src.orbitalBodyG;
            this.orbitalBodyB = src.orbitalBodyB;
            this.orbitalCoronaR = src.orbitalCoronaR;
            this.orbitalCoronaG = src.orbitalCoronaG;
            this.orbitalCoronaB = src.orbitalCoronaB;
            this.orbitalCoronaA = src.orbitalCoronaA;
            this.orbitalCoronaWidth = src.orbitalCoronaWidth;
            this.orbitalCoronaIntensity = src.orbitalCoronaIntensity;
            this.orbitalRimPower = src.orbitalRimPower;
            this.orbitalRimFalloff = src.orbitalRimFalloff;
            this.beamHeight = src.beamHeight;
            this.beamWidth = src.beamWidth;
            this.beamWidthScale = src.beamWidthScale;
            this.beamTaper = src.beamTaper;
            this.beamBodyR = src.beamBodyR;
            this.beamBodyG = src.beamBodyG;
            this.beamBodyB = src.beamBodyB;
            this.beamCoronaR = src.beamCoronaR;
            this.beamCoronaG = src.beamCoronaG;
            this.beamCoronaB = src.beamCoronaB;
            this.beamCoronaA = src.beamCoronaA;
            this.beamCoronaWidth = src.beamCoronaWidth;
            this.beamCoronaIntensity = src.beamCoronaIntensity;
            this.beamRimPower = src.beamRimPower;
            this.beamRimFalloff = src.beamRimFalloff;
            this.orbitalSpeed = src.orbitalSpeed;
            this.orbitalSpawnDuration = src.orbitalSpawnDuration;
            this.orbitalRetractDuration = src.orbitalRetractDuration;
            this.beamGrowDuration = src.beamGrowDuration;
            this.beamShrinkDuration = src.beamShrinkDuration;
            this.beamHoldDuration = src.beamHoldDuration;
            this.beamWidthGrowFactor = src.beamWidthGrowFactor;
            this.beamLengthGrowFactor = src.beamLengthGrowFactor;
            this.orbitalSpawnEasing = src.orbitalSpawnEasing;
            this.orbitalRetractEasing = src.orbitalRetractEasing;
            this.beamGrowEasing = src.beamGrowEasing;
            this.beamShrinkEasing = src.beamShrinkEasing;
            this.orbitalSpawnDelay = src.orbitalSpawnDelay;
            this.beamStartDelay = src.beamStartDelay;
            this.retractDelay = src.retractDelay;
            this.autoRetractOnRingEnd = src.autoRetractOnRingEnd;
            this.blackout = src.blackout;
            this.vignetteAmount = src.vignetteAmount;
            this.vignetteRadius = src.vignetteRadius;
            this.tintR = src.tintR;
            this.tintG = src.tintG;
            this.tintB = src.tintB;
            this.tintAmount = src.tintAmount;
            this.blendRadius = src.blendRadius;
            this.combinedMode = src.combinedMode;
            this.globalScale = src.globalScale;
            this.followPosition = src.followPosition;
            this.cursorYOffset = src.cursorYOffset;
        }
        
        // Shape setters
        public Builder shapeType(ShapeType v) { this.shapeType = v; return this; }
        public Builder mainRadius(float v) { this.mainRadius = v; return this; }
        public Builder orbitalRadius(float v) { this.orbitalRadius = v; return this; }
        public Builder orbitDistance(float v) { this.orbitDistance = v; return this; }
        public Builder orbitalCount(int v) { this.orbitalCount = v; return this; }
        
        // Ring setters
        public Builder ringCount(int v) { this.ringCount = v; return this; }
        public Builder ringSpacing(float v) { this.ringSpacing = v; return this; }
        public Builder ringThickness(float v) { this.ringThickness = v; return this; }
        public Builder ringMaxRadius(float v) { this.ringMaxRadius = v; return this; }
        public Builder ringSpeed(float v) { this.ringSpeed = v; return this; }
        public Builder ringGlowWidth(float v) { this.ringGlowWidth = v; return this; }
        public Builder ringIntensity(float v) { this.ringIntensity = v; return this; }
        public Builder ringContractMode(boolean v) { this.ringContractMode = v; return this; }
        public Builder ringColorR(float v) { this.ringColorR = v; return this; }
        public Builder ringColorG(float v) { this.ringColorG = v; return this; }
        public Builder ringColorB(float v) { this.ringColorB = v; return this; }
        public Builder ringColorOpacity(float v) { this.ringColorOpacity = v; return this; }
        
        // Orbital body setters
        public Builder orbitalBodyR(float v) { this.orbitalBodyR = v; return this; }
        public Builder orbitalBodyG(float v) { this.orbitalBodyG = v; return this; }
        public Builder orbitalBodyB(float v) { this.orbitalBodyB = v; return this; }
        
        // Orbital corona setters
        public Builder orbitalCoronaR(float v) { this.orbitalCoronaR = v; return this; }
        public Builder orbitalCoronaG(float v) { this.orbitalCoronaG = v; return this; }
        public Builder orbitalCoronaB(float v) { this.orbitalCoronaB = v; return this; }
        public Builder orbitalCoronaA(float v) { this.orbitalCoronaA = v; return this; }
        public Builder orbitalCoronaWidth(float v) { this.orbitalCoronaWidth = v; return this; }
        public Builder orbitalCoronaIntensity(float v) { this.orbitalCoronaIntensity = v; return this; }
        public Builder orbitalRimPower(float v) { this.orbitalRimPower = v; return this; }
        public Builder orbitalRimFalloff(float v) { this.orbitalRimFalloff = v; return this; }
        
        // Beam geometry setters
        public Builder beamHeight(float v) { this.beamHeight = v; return this; }
        public Builder beamWidth(float v) { this.beamWidth = v; return this; }
        public Builder beamWidthScale(float v) { this.beamWidthScale = v; return this; }
        public Builder beamTaper(float v) { this.beamTaper = v; return this; }
        
        // Beam body setters
        public Builder beamBodyR(float v) { this.beamBodyR = v; return this; }
        public Builder beamBodyG(float v) { this.beamBodyG = v; return this; }
        public Builder beamBodyB(float v) { this.beamBodyB = v; return this; }
        
        // Beam corona setters
        public Builder beamCoronaR(float v) { this.beamCoronaR = v; return this; }
        public Builder beamCoronaG(float v) { this.beamCoronaG = v; return this; }
        public Builder beamCoronaB(float v) { this.beamCoronaB = v; return this; }
        public Builder beamCoronaA(float v) { this.beamCoronaA = v; return this; }
        public Builder beamCoronaWidth(float v) { this.beamCoronaWidth = v; return this; }
        public Builder beamCoronaIntensity(float v) { this.beamCoronaIntensity = v; return this; }
        public Builder beamRimPower(float v) { this.beamRimPower = v; return this; }
        public Builder beamRimFalloff(float v) { this.beamRimFalloff = v; return this; }
        
        // Animation timing setters
        public Builder orbitalSpeed(float v) { this.orbitalSpeed = v; return this; }
        public Builder orbitalSpawnDuration(float v) { this.orbitalSpawnDuration = v; return this; }
        public Builder orbitalRetractDuration(float v) { this.orbitalRetractDuration = v; return this; }
        public Builder beamGrowDuration(float v) { this.beamGrowDuration = v; return this; }
        public Builder beamShrinkDuration(float v) { this.beamShrinkDuration = v; return this; }
        public Builder beamHoldDuration(float v) { this.beamHoldDuration = v; return this; }
        public Builder beamWidthGrowFactor(float v) { this.beamWidthGrowFactor = v; return this; }
        public Builder beamLengthGrowFactor(float v) { this.beamLengthGrowFactor = v; return this; }
        
        // Easing type setters
        public Builder orbitalSpawnEasing(EasingType v) { this.orbitalSpawnEasing = v; return this; }
        public Builder orbitalRetractEasing(EasingType v) { this.orbitalRetractEasing = v; return this; }
        public Builder beamGrowEasing(EasingType v) { this.beamGrowEasing = v; return this; }
        public Builder beamShrinkEasing(EasingType v) { this.beamShrinkEasing = v; return this; }
        
        // Delay setters
        public Builder orbitalSpawnDelay(float v) { this.orbitalSpawnDelay = v; return this; }
        public Builder beamStartDelay(float v) { this.beamStartDelay = v; return this; }
        public Builder retractDelay(float v) { this.retractDelay = v; return this; }
        public Builder autoRetractOnRingEnd(boolean v) { this.autoRetractOnRingEnd = v; return this; }
        
        // Screen effect setters
        public Builder blackout(float v) { this.blackout = v; return this; }
        public Builder vignetteAmount(float v) { this.vignetteAmount = v; return this; }
        public Builder vignetteRadius(float v) { this.vignetteRadius = v; return this; }
        public Builder tintR(float v) { this.tintR = v; return this; }
        public Builder tintG(float v) { this.tintG = v; return this; }
        public Builder tintB(float v) { this.tintB = v; return this; }
        public Builder tintAmount(float v) { this.tintAmount = v; return this; }
        
        // Blend setter
        public Builder blendRadius(float v) { this.blendRadius = v; return this; }
        public Builder combinedMode(boolean v) { this.combinedMode = v; return this; }
        
        // Global scale & positioning setters
        public Builder globalScale(float v) { this.globalScale = v; return this; }
        public Builder followPosition(boolean v) { this.followPosition = v; return this; }
        public Builder cursorYOffset(float v) { this.cursorYOffset = v; return this; }
        
        public ShockwaveConfig build() {
            return new ShockwaveConfig(
                shapeType, mainRadius, orbitalRadius, orbitDistance, orbitalCount,
                ringCount, ringSpacing, ringThickness, ringMaxRadius, ringSpeed,
                ringGlowWidth, ringIntensity, ringContractMode,
                ringColorR, ringColorG, ringColorB, ringColorOpacity,
                orbitalBodyR, orbitalBodyG, orbitalBodyB,
                orbitalCoronaR, orbitalCoronaG, orbitalCoronaB, orbitalCoronaA,
                orbitalCoronaWidth, orbitalCoronaIntensity, orbitalRimPower, orbitalRimFalloff,
                beamHeight, beamWidth, beamWidthScale, beamTaper,
                beamBodyR, beamBodyG, beamBodyB,
                beamCoronaR, beamCoronaG, beamCoronaB, beamCoronaA,
                beamCoronaWidth, beamCoronaIntensity, beamRimPower, beamRimFalloff,
                orbitalSpeed, orbitalSpawnDuration, orbitalRetractDuration,
                beamGrowDuration, beamShrinkDuration, beamHoldDuration,
                beamWidthGrowFactor, beamLengthGrowFactor,
                orbitalSpawnEasing, orbitalRetractEasing, beamGrowEasing, beamShrinkEasing,
                orbitalSpawnDelay, beamStartDelay, retractDelay, autoRetractOnRingEnd,
                blackout, vignetteAmount, vignetteRadius, tintR, tintG, tintB, tintAmount,
                blendRadius, combinedMode,
                globalScale, followPosition, cursorYOffset
            );
        }
    }
}
