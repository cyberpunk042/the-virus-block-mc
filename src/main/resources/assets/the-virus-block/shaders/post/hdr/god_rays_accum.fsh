// ═══════════════════════════════════════════════════════════════════════════
// GOD RAYS ACCUMULATION PASS - Radial blur toward light source
// ═══════════════════════════════════════════════════════════════════════════
//
// Pass 2 of God Rays pipeline:
// Marches from each pixel toward the light source, accumulating occlusion.
// This creates the characteristic volumetric light shaft effect.
//
// Runs at HALF RESOLUTION for performance.
//
// Inputs:
//   OcclusionSampler - Occlusion mask from god_rays_mask.fsh
//   FieldVisualConfig - For orb position and god ray params
//   CameraDataUBO    - For ViewProj matrix
//
// Outputs:
//   fragColor.rgb - Accumulated god ray intensity (monochrome, HDR-safe)
//
// ═══════════════════════════════════════════════════════════════════════════

#version 150

#define HDR_MODE 1

uniform sampler2D OcclusionSampler;

// Include base which provides UBOs (FieldVisualConfig, CameraDataUBO)
// Also provides: texCoord, fragColor, samplers
#include "../include/core/field_visual_base.glsl"

// Ray-sphere intersection for inward mode masking
#include "../include/sdf/ray_sphere.glsl"

// God rays accumulator
#include "../include/effects/god_rays.glsl"

void main() {
    // Early out if god rays disabled
    if (GodRayEnabled < 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DISTANCE-BASED PARALLELISM PROJECTION (360° God Rays)
    // Projects orb position with distance-aware ray parallelism.
    // 
    // Key insight: Light rays become more parallel as distance increases.
    // Close source = diverging rays, Far source = parallel rays (like sunlight).
    // This naturally handles behind-camera because parallel rays have no
    // convergence point on screen.
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Get orb position (camera-relative offset from FieldVisualConfig)
    vec3 toOrb = vec3(CenterX, CenterY, CenterZ);
    
    // Get camera basis from CameraDataUBO
    vec3 forward = normalize(CameraForwardUBO.xyz);
    float fov = CameraUpUBO.w;        // FOV in radians
    float aspect = CameraForwardUBO.w; // Width / Height
    
    // Build orthonormal basis: Right and Up from Forward
    vec3 right = normalize(cross(forward, vec3(0.0, 1.0, 0.0)));
    vec3 up = normalize(cross(right, forward));
    
    // Project orb position onto camera axes
    float zDist = dot(toOrb, forward);
    float xProj = dot(toOrb, right);
    float yProj = dot(toOrb, up);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HEMISPHERE-BASED 360° MODEL
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // FRONT HEMISPHERE (0° - 90°): Pure radial rays, NO parallelism
    //   - Rays converge toward the light's screen position
    //   - This is the original god ray behavior, preserved exactly
    //
    // BACK HEMISPHERE (90° - 180°): Parallel rays from edge
    //   - When light is behind camera, rays can't converge to a visible point
    //   - Instead, rays come from the screen edge in the light's direction
    //
    // TRANSITION: Smooth crossfade at the 90° boundary
    // ═══════════════════════════════════════════════════════════════════════════
    
    // World-space distance to orb (always positive)
    float orbDistance = length(toOrb);
    
    // Angle factor: +1 = directly in front, 0 = 90° to side, -1 = directly behind
    vec3 orbDirWorld = (orbDistance > 0.001) ? normalize(toOrb) : forward;
    float angleFactor = dot(orbDirWorld, forward);
    
    // 2D direction toward orb (for back hemisphere parallel rays)
    vec2 orbDir2D = vec2(xProj, yProj);
    float orbDir2DLen = length(orbDir2D);
    vec2 parallelDir = (orbDir2DLen > 0.001) ? normalize(orbDir2D) : vec2(0.0, 1.0);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARALLELISM FACTOR: Based purely on hemisphere, NOT distance
    // ═══════════════════════════════════════════════════════════════════════════
    //
    // angleFactor > 0 (front hemisphere):  parallelFactor = 0 (pure radial)
    // angleFactor < 0 (back hemisphere):   parallelFactor = 1 (parallel)
    // Smooth transition around 90° (angleFactor near 0)
    //
    // smoothstep creates the "curved" transition you asked for
    // ═══════════════════════════════════════════════════════════════════════════
    
    float parallelFactor = smoothstep(0.2, -0.2, angleFactor);
    // At angleFactor = 0.2 (~78°): parallelFactor = 0 (still radial)
    // At angleFactor = 0.0 (90°):  parallelFactor = 0.5 (halfway)
    // At angleFactor = -0.2 (~101°): parallelFactor = 1.0 (full parallel)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANGLE-BASED INTENSITY (for behind-camera fade)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Global intensity based on angle:
    // - Front (angleFactor = 1.0): full intensity
    // - Side (angleFactor = 0.0): reduced but still visible
    // - Behind (angleFactor = -1.0): minimal/faded
    float angleIntensity = angleFactor * 0.5 + 0.5;  // Remap -1..1 to 0..1
    angleIntensity = max(0.1, angleIntensity);        // Minimum 10% when behind
    angleIntensity = pow(angleIntensity, 0.7);        // Smooth the curve
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIGHT UV PROJECTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    vec2 lightUV = vec2(0.5, 0.5);
    float tanHalfFov = tan(fov * 0.5);
    
    if (zDist > 0.001) {
        // FRONT HEMISPHERE: Standard perspective projection
        // Light has a valid screen position, rays converge toward it
        float ndcX = (xProj / zDist) / (tanHalfFov * aspect);
        float ndcY = (yProj / zDist) / tanHalfFov;
        lightUV = vec2(ndcX * 0.5 + 0.5, ndcY * 0.5 + 0.5);
    } else {
        // BACK HEMISPHERE: Push light far off-screen
        // This creates parallel rays from the edge in the light's direction
        // The 5.0 multiplier ensures rays are nearly parallel (convergence point is far away)
        lightUV = vec2(0.5, 0.5) + parallelDir * 5.0;
    }
    
    // Clamp lightUV to reasonable bounds to avoid numerical issues
    lightUV = clamp(lightUV, vec2(-3.0), vec2(4.0));
    
    // Get god ray config from UBO
    int samples = int(GodRaySamples);
    float decay = GodRayDecay;
    float exposure = GodRayExposure;
    
    // Get style parameters from UBO (Slots 52-54)
    float energyMode = GodRayEnergyMode;
    float distributionMode = GodRayDistributionMode;
    float arrangementMode = GodRayArrangementMode;
    float noiseScale = GodRayNoiseScale;
    float noiseSpeed = GodRayNoiseSpeed;
    float noiseIntensity = GodRayNoiseIntensity;
    float angularBias = GodRayAngularBias;
    
    // Calculate orb radius in UV space (ellipse due to aspect ratio)
    // Must account for effect type scaling and CoreSize
    float orbRadiusX = 0.0;
    float orbRadiusY = 0.0;
    if (zDist > 0.0) {
        // Get visual radius: base Radius * effect scaling * CoreSize
        float visualRadius = Radius;
        
        // V6 uses 10x scale (Version = 6.0), V7 uses 1x (Version = 7.0)
        if (Version > 5.5 && Version < 6.5) {
            visualRadius *= 10.0;
        }
        
        // Apply CoreSize multiplier
        visualRadius *= CoreSize;
        
        float tanHalfFov = tan(fov * 0.5);
        // Y radius: project to NDC then to UV (0-1 range)
        orbRadiusY = (visualRadius / zDist) / tanHalfFov * 0.5;
        // X radius: same but divide by aspect ratio
        orbRadiusX = orbRadiusY / aspect;
    }
    
    // Get curvature params from UBO (Slot 55)
    float curvatureMode = GodRayCurvatureMode;
    float curvatureStrength = GodRayCurvatureStrength;
    
    // Get flicker params from UBO (Slot 56)
    float flickerMode = GodRayFlickerMode;
    float flickerIntensity = GodRayFlickerIntensity;
    float flickerFrequency = GodRayFlickerFrequency;
    
    // Get travel params from UBO (Slot 57)
    float travelMode = GodRayTravelMode;
    float travelSpeed = GodRayTravelSpeed;
    float travelCount = GodRayTravelCount;
    float travelWidth = GodRayTravelWidth;
    
    // Get animated time from FrameDataUBO (accumulated seconds since game start)
    float time = FrameTimeUBO.x * AnimSpeed;
    
    // For inward-marching modes (ABSORPTION=2), check if pixel is inside orb
    // using proper ray-sphere intersection (same math as field_visual)
    // Note: OSCILLATION (6) now uses outward direction, so not included here
    bool isInwardMode = (energyMode > 1.5 && energyMode < 2.5);
    if (isInwardMode) {
        // Build camera data for ray generation (use same method as field_visual)
        vec3 camPos = CameraPositionUBO.xyz;
        float near = CameraClipUBO.x;
        float far = CameraClipUBO.y;
        CameraData cam = buildCameraData(camPos, forward, fov, aspect, near, far);
        
        // Generate ray for this pixel
        Ray pixelRay = getRayFromBasis(texCoord, cam);
        
        // Calculate world-space sphere center and radius
        vec3 sphereCenter = camPos + toOrb;
        float visualRadius = Radius;
        if (Version > 5.5 && Version < 6.5) {
            visualRadius *= 10.0;  // V6 uses 10x scale
        }
        visualRadius *= CoreSize;
        
        // Check if ray hits sphere
        float hitDist = raySphereIntersectSimple(pixelRay.origin, pixelRay.direction, sphereCenter, visualRadius);
        if (hitDist >= 0.0) {
            // Pixel's ray hits the sphere - no god rays for inward modes
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
            return;
        }
    }
    
    // Accumulate god rays with full style support (including 360° parallelism)
    float illumination = accumulateGodRaysStyled(
        texCoord,
        lightUV,
        OcclusionSampler,
        samples,
        GOD_RAYS_DEFAULT_MAX_LENGTH,
        decay,
        exposure,
        orbRadiusX,
        orbRadiusY,
        energyMode,
        distributionMode,
        arrangementMode,
        noiseScale,
        noiseSpeed,
        noiseIntensity,
        angularBias,
        curvatureMode,
        curvatureStrength,
        flickerMode,
        flickerIntensity,
        flickerFrequency,
        travelMode,
        travelSpeed,
        travelCount,
        travelWidth,
        time,
        // New 360° parallelism parameters
        parallelFactor,
        parallelDir
    );
    
    // Apply angle-based intensity for 360° coverage fade
    illumination *= angleIntensity;
    
    // Output monochrome illumination (HDR-safe, may exceed 1.0)
    // Color tinting happens at composite stage
    fragColor = vec4(illumination, illumination, illumination, 1.0);
}

