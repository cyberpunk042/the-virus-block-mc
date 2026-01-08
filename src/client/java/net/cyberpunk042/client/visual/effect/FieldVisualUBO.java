package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.client.visual.ubo.annotation.*;
import net.cyberpunk042.client.visual.effect.FieldVisualTypes.*;
import net.cyberpunk042.client.visual.effect.FieldVisualVec4Types.*;
import org.joml.Matrix4f;

/**
 * Complete UBO structure for Field Visual effect.
 * 
 * <p><b>This record IS THE DOCUMENTATION for the UBO layout.</b>
 * Field order here = slot order in GLSL. Adding/removing/reordering
 * components here automatically changes the UBO structure.</p>
 * 
 * <h3>Layout: 50 vec4 slots = 200 floats = 800 bytes</h3>
 * <ul>
 *   <li>Slots 0-23: Effect Parameters</li>
 *   <li>Slots 24-28: V2 Detail Parameters</li>
 *   <li>Slots 29-32: Camera/Runtime</li>
 *   <li>Slots 33-40: Matrices (2 mat4)</li>
 *   <li>Slot 41: Debug</li>
 *   <li>Slots 42-49: V5/V8 Special Effects</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter
 */
@UBOStruct(name = "FieldVisualConfig", glslPath = "the-virus-block:shaders/post/field_visual.fsh")
public record FieldVisualUBO(
    // ═══════════════════════════════════════════════════════════════════════════
    // EFFECT PARAMETERS (Slots 0-23)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 PositionParams position,              // Slot 0: center + radius
    @Vec4 PrimaryColorVec4 primaryColor,        // Slot 1: primary RGBA
    @Vec4 SecondaryColorVec4 secondaryColor,    // Slot 2: secondary RGBA
    @Vec4 TertiaryColorVec4 tertiaryColor,      // Slot 3: tertiary RGBA
    @Vec4 HighlightColorVec4 highlightColor,    // Slot 4: highlight RGBA
    @Vec4 RayColorVec4 rayColor,                // Slot 5: ray RGBA
    @Vec4 AnimBaseVec4 animBase,                // Slot 6: phase, speed, intensity, effectType
    @Vec4 AnimMultiSpeedVec4 animMultiSpeed,    // Slot 7: speedHigh, speedLow, speedRay, speedRing
    @Vec4 AnimTimingParams animTiming,          // Slot 8: timeScale, radialSpeed1/2, axialSpeed
    @Vec4 CoreEdgeParams coreEdge,              // Slot 9: coreSize, edgeSharpness, shapeType, coreFalloff
    @Vec4 FalloffParams falloff,                // Slot 10: fadePower, fadeScale, insideFalloff, coronaEdge
    @Vec4 NoiseConfigParams noiseConfig,        // Slot 11: resLow, resHigh, amplitude, seed
    @Vec4 NoiseDetailParams noiseDetail,        // Slot 12: baseScale, scaleMultiplier, octaves, baseLevel
    @Vec4 GlowLineParams glowLine,              // Slot 13: count, intensity, rayPower, raySharpness
    @Vec4 CoronaParams corona,                  // Slot 14: width, power, multiplier, ringPower
    @Vec4 GeometryParams geometry,              // Slot 15: subdivisions, roundTop, roundCorner, thickness
    @Vec4 GeometryParams2 geometry2,            // Slot 16: gap, height, smoothRadius, reserved
    @Vec4 TransformParams transform,            // Slot 17: rotationX, rotationY, scale, reserved
    @Vec4 LightingParams lighting,              // Slot 18: diffuse, ambient, backLight, fresnel
    @Vec4 TimingParams timing,                  // Slot 19: sceneDuration, crossfade, loopMode, animFreq
    @Vec4 ScreenEffects screen,                 // Slot 20: blackout, vignetteAmount, vignetteRadius, tintAmount
    @Vec4 DistortionParams distortion,          // Slot 21: strength, radius, frequency, speed
    @Vec4 BlendParams blend,                    // Slot 22: opacity, blendMode, fadeIn, fadeOut
    @Vec4 ReservedParams reserved,              // Slot 23: version, rayCoronaFlags, slot3, slot4
    
    // ═══════════════════════════════════════════════════════════════════════════
    // V2 DETAIL PARAMETERS (Slots 24-28)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 V2CoronaDetail v2Corona,              // Slot 24: coronaStart, brightness, coreRadiusScale, coreMaskRadius
    @Vec4 V2CoreDetail v2Core,                  // Slot 25: coreSpread, coreGlow, coreMaskSoft, edgeRadius
    @Vec4 V2EdgeDetail v2Edge,                  // Slot 26: edgeSpread, edgeGlow, sharpScale, linesUVScale
    @Vec4 V2LinesDetail v2Lines,                // Slot 27: linesDensityMult, linesContrast1/2, linesMaskRadius
    @Vec4 V2AlphaDetail v2Alpha,                // Slot 28: linesMaskSoft, rayRotSpeed, rayStartRadius, alphaScale
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA/RUNTIME (Slots 29-32)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 CameraPosTimeVec4 cameraPosTime,      // Slot 29: posX, posY, posZ, time
    @Vec4 CameraForwardVec4 cameraForward,      // Slot 30: forwardX, forwardY, forwardZ, aspect
    @Vec4 CameraUpVec4 cameraUp,                // Slot 31: upX, upY, upZ, fov
    @Vec4 RenderParamsVec4 renderParams,        // Slot 32: nearPlane, farPlane, reserved, isFlying
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MATRICES (Slots 33-40)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Mat4 Matrix4f invViewProj,                 // Slots 33-36: inverse view-projection
    @Mat4 Matrix4f viewProj,                    // Slots 37-40: view-projection
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG (Slot 41)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 DebugParamsVec4 debug,                // Slot 41: camMode, debugMode, reserved, reserved
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PULSAR V5 FLAMES (Slots 42-43)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 FlamesParams1Vec4 flames1,            // Slot 42: edge, power, multiplier, timeScale
    @Vec4 FlamesParams2Vec4 flames2,            // Slot 43: insideFalloff, surfaceNoiseScale, reserved, reserved
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GEODESIC ANIMATION (Slot 44)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 GeoAnimParams geoAnim,                 // Slot 44: animMode, rotationSpeed, domeClip, reserved
    
    // ═══════════════════════════════════════════════════════════════════════════
    // V8 ELECTRIC AURA (Slots 45-49)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Vec4 V8PlasmaVec4 v8Plasma,                 // Slot 45: scale, speed, turbulence, intensity
    @Vec4 V8Ring1Vec4 v8Ring1,                   // Slot 46: frequency, speed, sharpness, centerValue
    @Vec4 V8Ring2Vec4 v8Ring2,                   // Slot 47: modPower, intensity, coreType, reserved
    @Vec4 V8CoronaVec4 v8Corona,                 // Slot 48: extent, fadeStart, fadePower, intensity
    @Vec4 V8ElectricVec4 v8Electric              // Slot 49: flash, fillIntensity, fillDarken, reserved
) {
    
    /** Buffer size in bytes, calculated from annotations */
    public static final int BUFFER_SIZE = net.cyberpunk042.client.visual.ubo.ReflectiveUBOWriter.calculateBufferSize(FieldVisualUBO.class);
    
    /**
     * Factory method to build from current data structures.
     * 
     * <p>This bridges the old API (separate params) to the new UBO structure.</p>
     * 
     * @param config The field visual configuration
     * @param position Position params (camera-relative)
     * @param camera Camera state for this frame
     * @param render Render params
     * @param invViewProj Inverse view-projection matrix
     * @param viewProj View-projection matrix
     * @param debug Debug params
     * @return A complete UBO ready for serialization
     */
    public static FieldVisualUBO from(
            FieldVisualConfig config,
            PositionParams position,
            CameraParams camera,
            RenderParams render,
            Matrix4f invViewProj,
            Matrix4f viewProj,
            DebugParams debug
    ) {
        ColorParams colors = config.colors();
        AnimParams anim = config.anim();
        
        return new FieldVisualUBO(
            // Effect params (slots 0-23)
            position,
            PrimaryColorVec4.from(colors),
            SecondaryColorVec4.from(colors),
            TertiaryColorVec4.from(colors),
            HighlightColorVec4.from(colors),
            RayColorVec4.from(colors),
            AnimBaseVec4.from(anim),
            AnimMultiSpeedVec4.from(anim),
            config.animTiming(),
            config.coreEdge(),
            config.falloff(),
            config.noiseConfig(),
            config.noiseDetail(),
            config.glowLine(),
            config.corona(),
            config.geometry(),
            config.geometry2(),
            config.transform(),
            config.lighting(),
            config.timing(),
            config.screen(),
            config.distortion(),
            config.blend(),
            config.reserved(),
            
            // V2 detail params (slots 24-28)
            config.v2Corona(),
            config.v2Core(),
            config.v2Edge(),
            config.v2Lines(),
            config.v2Alpha(),
            
            // Camera/runtime (slots 29-32)
            CameraPosTimeVec4.from(camera),
            CameraForwardVec4.from(camera),
            CameraUpVec4.from(camera),
            RenderParamsVec4.from(render),
            
            // Matrices (slots 33-40)
            invViewProj,
            viewProj,
            
            // Debug (slot 41)
            DebugParamsVec4.from(debug),
            
            // Flames (slots 42-43) - get from config if available, else defaults
            config.flames() != null ? FlamesParams1Vec4.from(config.flames()) : FlamesParams1Vec4.DEFAULT,
            config.flames() != null ? FlamesParams2Vec4.from(config.flames()) : FlamesParams2Vec4.DEFAULT,
            
            // Geodesic Animation (slot 44)
            config.geoAnim() != null ? config.geoAnim() : GeoAnimParams.DEFAULT,
            
            // V8 Electric Aura (slots 45-49)
            config.v8Plasma() != null ? V8PlasmaVec4.from(config.v8Plasma()) : V8PlasmaVec4.DEFAULT,
            config.v8Ring() != null ? V8Ring1Vec4.from(config.v8Ring()) : V8Ring1Vec4.DEFAULT,
            config.v8Ring() != null ? V8Ring2Vec4.from(config.v8Ring()) : V8Ring2Vec4.DEFAULT,
            config.v8Corona() != null ? V8CoronaVec4.from(config.v8Corona()) : V8CoronaVec4.DEFAULT,
            config.v8Electric() != null ? V8ElectricVec4.from(config.v8Electric()) : V8ElectricVec4.DEFAULT
        );
    }
    
    /**
     * Factory method with warmup progress for progressive effect loading.
     * 
     * <p>Scales octaves based on warmup progress (0.0 to 1.0) to reduce
     * GPU load when effects first appear.</p>
     * 
     * @param config The field visual configuration
     * @param position Position params (camera-relative)
     * @param camera Camera state for this frame
     * @param render Render params
     * @param invViewProj Inverse view-projection matrix
     * @param viewProj View-projection matrix
     * @param debug Debug params
     * @param warmupProgress Progress from 0.0 (just created) to 1.0 (fully loaded)
     * @return A complete UBO ready for serialization
     */
    public static FieldVisualUBO fromWithWarmup(
            FieldVisualConfig config,
            PositionParams position,
            CameraParams camera,
            RenderParams render,
            Matrix4f invViewProj,
            Matrix4f viewProj,
            DebugParams debug,
            float warmupProgress
    ) {
        ColorParams colors = config.colors();
        AnimParams anim = config.anim();
        
        // Scale octaves based on warmup: start at 20% (min 1), ramp to full
        NoiseDetailParams originalDetail = config.noiseDetail();
        int fullOctaves = (int) originalDetail.octaves();
        int minOctaves = Math.max(1, (int)(fullOctaves * 0.2f));
        int scaledOctaves = minOctaves + (int)((fullOctaves - minOctaves) * warmupProgress);
        NoiseDetailParams scaledDetail = originalDetail.withOctaves(scaledOctaves);
        
        return new FieldVisualUBO(
            // Effect params (slots 0-23)
            position,
            PrimaryColorVec4.from(colors),
            SecondaryColorVec4.from(colors),
            TertiaryColorVec4.from(colors),
            HighlightColorVec4.from(colors),
            RayColorVec4.from(colors),
            AnimBaseVec4.from(anim),
            AnimMultiSpeedVec4.from(anim),
            config.animTiming(),
            config.coreEdge(),
            config.falloff(),
            config.noiseConfig(),
            scaledDetail,  // Warmup-scaled octaves
            config.glowLine(),
            config.corona(),
            config.geometry(),
            config.geometry2(),
            config.transform(),
            config.lighting(),
            config.timing(),
            config.screen(),
            config.distortion(),
            config.blend(),
            config.reserved(),
            
            // V2 detail params (slots 24-28)
            config.v2Corona(),
            config.v2Core(),
            config.v2Edge(),
            config.v2Lines(),
            config.v2Alpha(),
            
            // Camera/runtime (slots 29-32)
            CameraPosTimeVec4.from(camera),
            CameraForwardVec4.from(camera),
            CameraUpVec4.from(camera),
            RenderParamsVec4.from(render),
            
            // Matrices (slots 33-40)
            invViewProj,
            viewProj,
            
            // Debug (slot 41)
            DebugParamsVec4.from(debug),
            
            // Flames (slots 42-43)
            config.flames() != null ? FlamesParams1Vec4.from(config.flames()) : FlamesParams1Vec4.DEFAULT,
            config.flames() != null ? FlamesParams2Vec4.from(config.flames()) : FlamesParams2Vec4.DEFAULT,
            
            // Geodesic Animation (slot 44)
            config.geoAnim() != null ? config.geoAnim() : GeoAnimParams.DEFAULT,
            
            // V8 Electric Aura (slots 45-49)
            config.v8Plasma() != null ? V8PlasmaVec4.from(config.v8Plasma()) : V8PlasmaVec4.DEFAULT,
            config.v8Ring() != null ? V8Ring1Vec4.from(config.v8Ring()) : V8Ring1Vec4.DEFAULT,
            config.v8Ring() != null ? V8Ring2Vec4.from(config.v8Ring()) : V8Ring2Vec4.DEFAULT,
            config.v8Corona() != null ? V8CoronaVec4.from(config.v8Corona()) : V8CoronaVec4.DEFAULT,
            config.v8Electric() != null ? V8ElectricVec4.from(config.v8Electric()) : V8ElectricVec4.DEFAULT
        );
    }
}
