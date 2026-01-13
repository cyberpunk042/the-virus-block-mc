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
    // VECTOR-BASIS PROJECTION (Section 5.5 - Production Standard)
    // Projects orb position using camera basis vectors.
    // This is immune to UBO slot displacement and coordinate origin conflicts.
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
    
    // Default to center if orb is behind camera
    vec2 lightUV = vec2(0.5, 0.5);
    float lightVisible = 0.0;
    
    if (zDist > 0.0) {
        // Convert to NDC using FOV (Perspective Divide)
        float tanHalfFov = tan(fov * 0.5);
        float ndcX = (xProj / zDist) / (tanHalfFov * aspect);
        float ndcY = (yProj / zDist) / tanHalfFov;
        
        // NDC to UV
        lightUV = vec2(ndcX * 0.5 + 0.5, ndcY * 0.5 + 0.5);
        
        // Visible if within extended screen bounds (allow off-screen margin)
        lightVisible = (abs(ndcX) < 1.5 && abs(ndcY) < 1.5) ? 1.0 : 0.0;
    }
    
    // Early out if light is behind camera or way off-screen
    if (lightVisible < 0.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    
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
    
    // For inward-marching modes (ABSORPTION=2, OSCILLATION=6), check if pixel is inside orb
    // using proper ray-sphere intersection (same math as field_visual)
    bool isInwardMode = (energyMode > 1.5 && energyMode < 2.5) || (energyMode > 5.5 && energyMode < 6.5);
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
    
    // Accumulate god rays with full style support
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
        time
    );
    
    // Output monochrome illumination (HDR-safe, may exceed 1.0)
    // Color tinting happens at composite stage
    fragColor = vec4(illumination, illumination, illumination, 1.0);
}

