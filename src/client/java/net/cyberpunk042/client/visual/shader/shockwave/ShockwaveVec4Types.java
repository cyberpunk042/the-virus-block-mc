package net.cyberpunk042.client.visual.shader.shockwave;

import net.cyberpunk042.client.visual.ubo.Vec4Serializable;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;

/**
 * Vec4-serializable wrapper records for Shockwave UBO slots.
 * 
 * <p>These wrappers extract specific vec4 slices from complex Shockwave records
 * to match the exact UBO layout expected by shockwave.fsh.</p>
 * 
 * <p>Layout: 18 vec4s + 1 mat4 = 22 vec4s = 352 bytes</p>
 */
public final class ShockwaveVec4Types {
    
    private ShockwaveVec4Types() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOT 0: BASIC PARAMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 0: currentRadius, thickness, intensity, time */
    public record BasicParamsVec4(float radius, float thickness, float intensity, float time) 
            implements Vec4Serializable {
        public static BasicParamsVec4 from(float currentRadius, RingParams ring, float time) {
            return new BasicParamsVec4(currentRadius, ring.thickness(), ring.intensity(), time);
        }
        @Override public float slot0() { return radius; }
        @Override public float slot1() { return thickness; }
        @Override public float slot2() { return intensity; }
        @Override public float slot3() { return time; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOT 1: RING COUNT PARAMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 1: count, spacing, contractMode, glowWidth */
    public record RingCountVec4(float count, float spacing, float contractMode, float glowWidth)
            implements Vec4Serializable {
        public static RingCountVec4 from(RingParams ring) {
            return new RingCountVec4((float) ring.count(), ring.spacing(), 
                ring.contractMode() ? 1f : 0f, ring.glowWidth());
        }
        @Override public float slot0() { return count; }
        @Override public float slot1() { return spacing; }
        @Override public float slot2() { return contractMode; }
        @Override public float slot3() { return glowWidth; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOT 2: TARGET POSITION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 2: targetX, targetY, targetZ, useWorldOrigin */
    public record TargetPosVec4(float x, float y, float z, float useWorldOrigin)
            implements Vec4Serializable {
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return useWorldOrigin; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOTS 3-5: CAMERA
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 3: cameraX, cameraY, cameraZ, aspectRatio */
    public record CameraPosVec4(float x, float y, float z, float aspect)
            implements Vec4Serializable {
        public static CameraPosVec4 from(CameraState cam, float aspect) {
            return new CameraPosVec4(cam.x(), cam.y(), cam.z(), aspect);
        }
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return aspect; }
    }
    
    /** Slot 4: forwardX, forwardY, forwardZ, fov */
    public record CameraForwardVec4(float x, float y, float z, float fov)
            implements Vec4Serializable {
        public static CameraForwardVec4 from(CameraState cam, float fov) {
            return new CameraForwardVec4(cam.forwardX(), cam.forwardY(), cam.forwardZ(), fov);
        }
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return fov; }
    }
    
    /** Slot 5: upX, upY, upZ, isFlying (camera up is hardcoded to 0,1,0) */
    public record CameraUpVec4(float x, float y, float z, float isFlying)
            implements Vec4Serializable {
        public static CameraUpVec4 fromFlying(float isFlying) {
            return new CameraUpVec4(0f, 1f, 0f, isFlying);
        }
        @Override public float slot0() { return x; }
        @Override public float slot1() { return y; }
        @Override public float slot2() { return z; }
        @Override public float slot3() { return isFlying; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOTS 6-7: SCREEN EFFECTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 6: blackout, vignetteAmount, vignetteRadius, reserved */
    public record ScreenVignetteVec4(float blackout, float vignetteAmount, float vignetteRadius, float reserved)
            implements Vec4Serializable {
        public static ScreenVignetteVec4 from(ScreenEffects s) {
            return new ScreenVignetteVec4(s.blackout(), s.vignetteAmount(), s.vignetteRadius(), 0f);
        }
        @Override public float slot0() { return blackout; }
        @Override public float slot1() { return vignetteAmount; }
        @Override public float slot2() { return vignetteRadius; }
        @Override public float slot3() { return reserved; }
    }
    
    /** Slot 7: tintR, tintG, tintB, tintAmount */
    public record ScreenTintVec4(float r, float g, float b, float amount)
            implements Vec4Serializable {
        public static ScreenTintVec4 from(ScreenEffects s) {
            return new ScreenTintVec4(s.tintR(), s.tintG(), s.tintB(), s.tintAmount());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return amount; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOT 8: RING COLOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 8: ringR, ringG, ringB, ringOpacity - RingColor already has 4 floats */
    public record RingColorVec4(float r, float g, float b, float opacity)
            implements Vec4Serializable {
        public static RingColorVec4 from(RingColor c) {
            return new RingColorVec4(c.r(), c.g(), c.b(), c.opacity());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return opacity; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOTS 9-10: SHAPE CONFIG
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 9: shapeType, radius, majorRadius, minorRadius */
    public record ShapeConfigVec4(float type, float radius, float majorRadius, float minorRadius)
            implements Vec4Serializable {
        public static ShapeConfigVec4 from(ShapeConfig s) {
            return new ShapeConfigVec4((float) s.type().getShaderCode(), s.radius(), 
                s.majorRadius(), s.minorRadius());
        }
        @Override public float slot0() { return type; }
        @Override public float slot1() { return radius; }
        @Override public float slot2() { return majorRadius; }
        @Override public float slot3() { return minorRadius; }
    }
    
    /** Slot 10: sideCount, animatedOrbitDistance, orbitalPhase, animatedBeamHeight */
    public record ShapeExtrasVec4(float sideCount, float orbitDistance, float orbitalPhase, float beamHeight)
            implements Vec4Serializable {
        @Override public float slot0() { return sideCount; }
        @Override public float slot1() { return orbitDistance; }
        @Override public float slot2() { return orbitalPhase; }
        @Override public float slot3() { return beamHeight; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOTS 11-13: ORBITAL CONFIG
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 11: corona width, intensity, rimPower, blendRadius */
    public record CoronaConfigVec4(float width, float intensity, float rimPower, float blendRadius)
            implements Vec4Serializable {
        public static CoronaConfigVec4 from(CoronaConfig c, float blendRadius) {
            return new CoronaConfigVec4(c.width(), c.intensity(), c.rimPower(), blendRadius);
        }
        @Override public float slot0() { return width; }
        @Override public float slot1() { return intensity; }
        @Override public float slot2() { return rimPower; }
        @Override public float slot3() { return blendRadius; }
    }
    
    /** Slot 12: orbital body RGB + rimFalloff */
    public record OrbitalBodyVec4(float r, float g, float b, float rimFalloff)
            implements Vec4Serializable {
        public static OrbitalBodyVec4 from(OrbitalVisualConfig orb) {
            return new OrbitalBodyVec4(orb.bodyColor().r(), orb.bodyColor().g(), 
                orb.bodyColor().b(), orb.corona().rimFalloff());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return rimFalloff; }
    }
    
    /** Slot 13: orbital corona color RGBA */
    public record OrbitalCoronaColorVec4(float r, float g, float b, float a)
            implements Vec4Serializable {
        public static OrbitalCoronaColorVec4 from(CoronaConfig c) {
            return new OrbitalCoronaColorVec4(c.color().r(), c.color().g(), c.color().b(), c.color().a());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLOTS 14-17: BEAM CONFIG
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Slot 14: beam body RGB + widthScale */
    public record BeamBodyVec4(float r, float g, float b, float widthScale)
            implements Vec4Serializable {
        public static BeamBodyVec4 from(BeamVisualConfig beam) {
            return new BeamBodyVec4(beam.bodyColor().r(), beam.bodyColor().g(), 
                beam.bodyColor().b(), beam.widthScale());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return widthScale; }
    }
    
    /** Slot 15: beam corona color RGBA */
    public record BeamCoronaColorVec4(float r, float g, float b, float a)
            implements Vec4Serializable {
        public static BeamCoronaColorVec4 from(BeamVisualConfig beam) {
            Color4f c = beam.corona().color();
            return new BeamCoronaColorVec4(c.r(), c.g(), c.b(), c.a());
        }
        @Override public float slot0() { return r; }
        @Override public float slot1() { return g; }
        @Override public float slot2() { return b; }
        @Override public float slot3() { return a; }
    }
    
    /** Slot 16: beam width, taper, retractDelay, combinedMode */
    public record BeamGeometryVec4(float width, float taper, float retractDelay, float combinedMode)
            implements Vec4Serializable {
        public static BeamGeometryVec4 from(BeamVisualConfig beam, float retractDelay, boolean combinedMode) {
            return new BeamGeometryVec4(beam.width(), beam.taper(), retractDelay, combinedMode ? 1f : 0f);
        }
        @Override public float slot0() { return width; }
        @Override public float slot1() { return taper; }
        @Override public float slot2() { return retractDelay; }
        @Override public float slot3() { return combinedMode; }
    }
    
    /** Slot 17: beam corona width, intensity, rimPower, rimFalloff */
    public record BeamCoronaVec4(float width, float intensity, float rimPower, float rimFalloff)
            implements Vec4Serializable {
        public static BeamCoronaVec4 from(BeamVisualConfig beam) {
            CoronaConfig c = beam.corona();
            return new BeamCoronaVec4(c.width(), c.intensity(), c.rimPower(), c.rimFalloff());
        }
        @Override public float slot0() { return width; }
        @Override public float slot1() { return intensity; }
        @Override public float slot2() { return rimPower; }
        @Override public float slot3() { return rimFalloff; }
    }
}
