package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.client.visual.ubo.Vec4Serializable;
import net.cyberpunk042.client.visual.effect.FieldVisualTypes.*;

/**
 * Vec4-serializable wrapper records for complex FieldVisual types.
 * 
 * <p>These wrappers extract specific vec4 slices from records that contain
 * more than 4 floats (like ColorParams → 5 colors, CameraParams → 12 floats).</p>
 * 
 * <p>Simple 4-float records in FieldVisualTypes already implement Vec4Serializable
 * directly. Only complex records need these wrappers.</p>
 */
public final class FieldVisualVec4Types {
    
    private FieldVisualVec4Types() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESERVED/DEPRECATED SLOTS
    // Used for maintaining UBO layout when slots are deprecated
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Reserved slot - writes zeros. Used for deprecated UBO slots. */
    public record ReservedVec4(float x, float y, float z, float w) implements Vec4Serializable {
        public static final ReservedVec4 ZERO = new ReservedVec4(0f, 0f, 0f, 0f);
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return w; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR EXTRACTORS (Slots 1-5)
    // ColorParams has 5 colors, each needs its own vec4 wrapper
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Extracts primary color (RGBA) from ColorParams as vec4. */
    public record PrimaryColorVec4(float r, float g, float b, float a) implements Vec4Serializable {
        public static PrimaryColorVec4 from(ColorParams c) {
            return new PrimaryColorVec4(c.primaryR(), c.primaryG(), c.primaryB(), c.primaryA());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    /** Extracts secondary color (RGBA) from ColorParams as vec4. */
    public record SecondaryColorVec4(float r, float g, float b, float a) implements Vec4Serializable {
        public static SecondaryColorVec4 from(ColorParams c) {
            return new SecondaryColorVec4(c.secondaryR(), c.secondaryG(), c.secondaryB(), c.secondaryA());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    /** Extracts tertiary color (RGBA) from ColorParams as vec4. */
    public record TertiaryColorVec4(float r, float g, float b, float a) implements Vec4Serializable {
        public static TertiaryColorVec4 from(ColorParams c) {
            return new TertiaryColorVec4(c.tertiaryR(), c.tertiaryG(), c.tertiaryB(), c.tertiaryA());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    /** Extracts highlight color (RGBA) from ColorParams as vec4. */
    public record HighlightColorVec4(float r, float g, float b, float a) implements Vec4Serializable {
        public static HighlightColorVec4 from(ColorParams c) {
            return new HighlightColorVec4(c.highlightR(), c.highlightG(), c.highlightB(), c.highlightA());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    /** Extracts ray color (RGBA) from ColorParams as vec4. */
    public record RayColorVec4(float r, float g, float b, float a) implements Vec4Serializable {
        public static RayColorVec4 from(ColorParams c) {
            return new RayColorVec4(c.rayR(), c.rayG(), c.rayB(), c.rayA());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION EXTRACTORS (Slots 6-7)
    // AnimParams has 8 fields → 2 vec4s
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Extracts base animation params (phase, speed, intensity, effectType) from AnimParams. */
    public record AnimBaseVec4(float phase, float speed, float intensity, float effectType) implements Vec4Serializable {
        public static AnimBaseVec4 from(AnimParams a) {
            return new AnimBaseVec4(a.phase(), a.speed(), a.intensity(), (float) a.effectType().ordinal());
        }
        @Override public float slot0() { return phase; }
        @Override public float slot1() { return speed; }
        @Override public float slot2() { return intensity; }
        @Override public float slot3() { return effectType; }
    }
    
    /** Extracts multi-speed channels from AnimParams. */
    public record AnimMultiSpeedVec4(float speedHigh, float speedLow, float speedRay, float speedRing) implements Vec4Serializable {
        public static AnimMultiSpeedVec4 from(AnimParams a) {
            return new AnimMultiSpeedVec4(a.speedHigh(), a.speedLow(), a.speedRay(), a.speedRing());
        }
        @Override public float slot0() { return speedHigh; }
        @Override public float slot1() { return speedLow; }
        @Override public float slot2() { return speedRay; }
        @Override public float slot3() { return speedRing; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA EXTRACTORS (Slots 29-31)
    // CameraParams has 12 floats → 3 vec4s
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Extracts camera position + time from CameraParams. */
    public record CameraPosTimeVec4(float x, float y, float z, float time) implements Vec4Serializable {
        public static CameraPosTimeVec4 from(CameraParams c) {
            return new CameraPosTimeVec4(c.posX(), c.posY(), c.posZ(), c.time());
        }
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return time; }
    }
    
    /** Extracts camera forward direction + aspect from CameraParams. */
    public record CameraForwardVec4(float x, float y, float z, float aspect) implements Vec4Serializable {
        public static CameraForwardVec4 from(CameraParams c) {
            return new CameraForwardVec4(c.forwardX(), c.forwardY(), c.forwardZ(), c.aspect());
        }
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return aspect; }
    }
    
    /** Extracts camera up direction + fov from CameraParams. */
    public record CameraUpVec4(float x, float y, float z, float fov) implements Vec4Serializable {
        public static CameraUpVec4 from(CameraParams c) {
            return new CameraUpVec4(c.upX(), c.upY(), c.upZ(), c.fov());
        }
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return fov; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDER PARAMS (Slot 32)
    // RenderParams has 3 floats → needs padding to vec4
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Wraps RenderParams (3 floats) as vec4 with padding. */
    public record RenderParamsVec4(float nearPlane, float farPlane, float reserved, float isFlying) implements Vec4Serializable {
        public static RenderParamsVec4 from(RenderParams r) {
            return new RenderParamsVec4(r.nearPlane(), r.farPlane(), 0f, r.isFlying());
        }
        @Override public float slot0() { return nearPlane; }
        @Override public float slot1() { return farPlane; }
        @Override public float slot2() { return reserved; }
        @Override public float slot3() { return isFlying; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG PARAMS (Slot 36)
    // DebugParams has 2 floats → needs padding to vec4
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Wraps DebugParams (2 floats) as vec4 with padding. */
    public record DebugParamsVec4(float camMode, float debugMode, float reserved1, float reserved2) implements Vec4Serializable {
        public static DebugParamsVec4 from(DebugParams d) {
            return new DebugParamsVec4(d.camMode(), d.debugMode(), 0f, 0f);
        }
        @Override public float slot0() { return camMode; }
        @Override public float slot1() { return debugMode; }
        @Override public float slot2() { return reserved1; }
        @Override public float slot3() { return reserved2; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FLAMES PARAMS (Slots 42-43) - Pulsar V5
    // FlamesParams has 6 floats → 2 vec4s
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** First vec4 of FlamesParams: edge, power, multiplier, timeScale. */
    public record FlamesParams1Vec4(float edge, float power, float multiplier, float timeScale) implements Vec4Serializable {
        public static FlamesParams1Vec4 from(FlamesParams f) {
            return new FlamesParams1Vec4(f.edge(), f.power(), f.multiplier(), f.timeScale());
        }
        public static final FlamesParams1Vec4 DEFAULT = new FlamesParams1Vec4(1.1f, 2.0f, 50.0f, 1.2f);
        @Override public float slot0() { return edge; }
        @Override public float slot1() { return power; }
        @Override public float slot2() { return multiplier; }
        @Override public float slot3() { return timeScale; }
    }
    
    /** Second vec4 of FlamesParams: insideFalloff, surfaceNoiseScale, reserved, reserved. */
    public record FlamesParams2Vec4(float insideFalloff, float surfaceNoiseScale, float reserved1, float reserved2) implements Vec4Serializable {
        public static FlamesParams2Vec4 from(FlamesParams f) {
            return new FlamesParams2Vec4(f.insideFalloff(), f.surfaceNoiseScale(), 0f, 0f);
        }
        public static final FlamesParams2Vec4 DEFAULT = new FlamesParams2Vec4(24.0f, 5.0f, 0f, 0f);
        @Override public float slot0() { return insideFalloff; }
        @Override public float slot1() { return surfaceNoiseScale; }
        @Override public float slot2() { return reserved1; }
        @Override public float slot3() { return reserved2; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // V8 ELECTRIC AURA (Slots 45-48)
    // V8PlasmaParams = 1 vec4, V8RingParams = 2 vec4, V8CoronaParams = 1 vec4
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** V8 Plasma params: scale, speed, turbulence, intensity. (Slot 45) */
    public record V8PlasmaVec4(float scale, float speed, float turbulence, float intensity) implements Vec4Serializable {
        public static V8PlasmaVec4 from(V8PlasmaParams p) {
            return new V8PlasmaVec4(p.scale(), p.speed(), p.turbulence(), p.intensity());
        }
        public static final V8PlasmaVec4 DEFAULT = new V8PlasmaVec4(10f, 1f, 1f, 1f);
        @Override public float slot0() { return scale; }
        @Override public float slot1() { return speed; }
        @Override public float slot2() { return turbulence; }
        @Override public float slot3() { return intensity; }
    }
    
    /** First vec4 of V8RingParams: frequency, speed, sharpness, centerValue. (Slot 46) */
    public record V8Ring1Vec4(float frequency, float speed, float sharpness, float centerValue) implements Vec4Serializable {
        public static V8Ring1Vec4 from(V8RingParams r) {
            return new V8Ring1Vec4(r.frequency(), r.speed(), r.sharpness(), r.centerValue());
        }
        public static final V8Ring1Vec4 DEFAULT = new V8Ring1Vec4(4f, 10f, 3f, 0.1f);
        @Override public float slot0() { return frequency; }
        @Override public float slot1() { return speed; }
        @Override public float slot2() { return sharpness; }
        @Override public float slot3() { return centerValue; }
    }
    
    /** Second vec4 of V8RingParams: modPower, intensity, coreType, reserved. (Slot 47) */
    public record V8Ring2Vec4(float modPower, float intensity, float coreType, float reserved) implements Vec4Serializable {
        public static V8Ring2Vec4 from(V8RingParams r) {
            return new V8Ring2Vec4(r.modPower(), r.intensity(), r.coreType(), 0f);
        }
        public static final V8Ring2Vec4 DEFAULT = new V8Ring2Vec4(0.9f, 1f, 0f, 0f);
        @Override public float slot0() { return modPower; }
        @Override public float slot1() { return intensity; }
        @Override public float slot2() { return coreType; }
        @Override public float slot3() { return reserved; }
    }
    
    /** V8 Corona envelope: extent, fadeStart, fadePower, intensity. (Slot 48) */
    public record V8CoronaVec4(float extent, float fadeStart, float fadePower, float intensity) implements Vec4Serializable {
        public static V8CoronaVec4 from(V8CoronaParams c) {
            return new V8CoronaVec4(c.extent(), c.fadeStart(), c.fadePower(), c.intensity());
        }
        public static final V8CoronaVec4 DEFAULT = new V8CoronaVec4(2f, 0.5f, 1f, 1f);
        @Override public float slot0() { return extent; }
        @Override public float slot1() { return fadeStart; }
        @Override public float slot2() { return fadePower; }
        @Override public float slot3() { return intensity; }
    }
    
    /** V8 Electric Core: flash, fillIntensity, fillDarken, lineWidth. (Slot 49) */
    public record V8ElectricVec4(float flash, float fillIntensity, float fillDarken, float lineWidth) implements Vec4Serializable {
        public static V8ElectricVec4 from(V8ElectricParams e) {
            return new V8ElectricVec4(e.flash(), e.fillIntensity(), e.fillDarken(), e.lineWidth());
        }
        public static final V8ElectricVec4 DEFAULT = new V8ElectricVec4(0f, 0.5f, 0.60f, 0.5f);
        @Override public float slot0() { return flash; }
        @Override public float slot1() { return fillIntensity; }
        @Override public float slot2() { return fillDarken; }
        @Override public float slot3() { return lineWidth; }
    }
}
