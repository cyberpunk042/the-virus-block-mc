// ═══════════════════════════════════════════════════════════════════════════
// RAY SPHERE - Ray-Sphere Intersection Utilities
// ═══════════════════════════════════════════════════════════════════════════
//
// Purpose:
// ─────────
// Provides standard ray-sphere intersection calculations used by all
// spherical effects (energy orbs, pulsars, geodesics, etc.)
//
// Key Functions:
// ─────────
// raySphereIntersect(...)     - Full intersection with entry/exit points
// raySphereIntersectSimple(...) - Returns just distance (or -1 if miss)
// sphereProjection(...)       - Projection point for corona/glow effects
//
// Terminology:
// ─────────
// rayProj   - Projection of sphere center onto ray (signed distance)
// sqDist    - Squared distance from ray origin to sphere center
// sphere    - Perpendicular distance² from ray to sphere center
// sqRadius  - Squared sphere radius
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef RAY_SPHERE_GLSL
#define RAY_SPHERE_GLSL

// ═══════════════════════════════════════════════════════════════════════════
// INTERSECTION RESULT STRUCT
// ═══════════════════════════════════════════════════════════════════════════

struct RaySphereHit {
    bool hit;          // True if ray intersects sphere
    float tEntry;      // Distance to entry point (front of sphere)
    float tExit;       // Distance to exit point (back of sphere)
    vec3 entryPoint;   // World position of entry point
    vec3 normal;       // Surface normal at entry point
    float rayProj;     // Projection of center onto ray
    float sqDist;      // Squared distance to center
    float perpDistSq;  // Perpendicular distance² (for corona calcs)
};

// ═══════════════════════════════════════════════════════════════════════════
// FULL INTERSECTION
// ═══════════════════════════════════════════════════════════════════════════
// Computes complete intersection data for raymarching and shading.
//
// Parameters:
//   rayOrigin   - Ray start point (camera position)
//   rayDir      - Normalized ray direction
//   sphereCenter - World position of sphere center
//   sphereRadius - Sphere radius
//
// Returns:
//   RaySphereHit with all intersection data

RaySphereHit raySphereIntersect(
    vec3 rayOrigin,
    vec3 rayDir,
    vec3 sphereCenter,
    float sphereRadius
) {
    RaySphereHit result;
    
    // Vector from ray origin to sphere center
    vec3 toCenter = sphereCenter - rayOrigin;
    
    // Project center onto ray direction
    result.rayProj = dot(rayDir, toCenter);
    
    // Squared distance from origin to center
    result.sqDist = dot(toCenter, toCenter);
    
    // Perpendicular distance squared (Pythagorean theorem)
    result.perpDistSq = result.sqDist - result.rayProj * result.rayProj;
    
    float sqRadius = sphereRadius * sphereRadius;
    
    // Check for intersection
    if (result.perpDistSq > sqRadius) {
        // Ray misses sphere
        result.hit = false;
        result.tEntry = -1.0;
        result.tExit = -1.0;
        result.entryPoint = vec3(0.0);
        result.normal = vec3(0.0);
        return result;
    }
    
    // Ray hits sphere - calculate entry/exit distances
    float halfChord = sqrt(sqRadius - result.perpDistSq);
    result.tEntry = result.rayProj - halfChord;
    result.tExit = result.rayProj + halfChord;
    
    // Check if sphere is behind us
    if (result.tExit < 0.0) {
        result.hit = false;
        result.tEntry = -1.0;
        result.tExit = -1.0;
        result.entryPoint = vec3(0.0);
        result.normal = vec3(0.0);
        return result;
    }
    
    // If we're inside the sphere, entry is at origin
    if (result.tEntry < 0.0) {
        result.tEntry = 0.0;
    }
    
    result.hit = true;
    result.entryPoint = rayOrigin + rayDir * result.tEntry;
    result.normal = normalize(result.entryPoint - sphereCenter);
    
    return result;
}

// ═══════════════════════════════════════════════════════════════════════════
// SIMPLE INTERSECTION
// ═══════════════════════════════════════════════════════════════════════════
// Returns just the entry distance, or -1.0 if no intersection.
// Faster than full intersection when you only need distance.

float raySphereIntersectSimple(
    vec3 rayOrigin,
    vec3 rayDir,
    vec3 sphereCenter,
    float sphereRadius
) {
    vec3 toCenter = sphereCenter - rayOrigin;
    float rayProj = dot(rayDir, toCenter);
    float sqDist = dot(toCenter, toCenter);
    float perpDistSq = sqDist - rayProj * rayProj;
    float sqRadius = sphereRadius * sphereRadius;
    
    if (perpDistSq > sqRadius) {
        return -1.0;  // Miss
    }
    
    float halfChord = sqrt(sqRadius - perpDistSq);
    float tEntry = rayProj - halfChord;
    
    if (tEntry < 0.0) {
        tEntry = rayProj + halfChord;  // We're inside, use exit
    }
    
    return tEntry > 0.0 ? tEntry : -1.0;
}

// NOTE: SphereProjection for screen-space effects is in camera/types.glsl
// This file only provides ray-sphere intersection math

// ═══════════════════════════════════════════════════════════════════════════
// INSIDE CHECK
// ═══════════════════════════════════════════════════════════════════════════
// Quick check if point is inside sphere.

bool isInsideSphere(vec3 point, vec3 sphereCenter, float sphereRadius) {
    vec3 diff = point - sphereCenter;
    return dot(diff, diff) < sphereRadius * sphereRadius;
}

// ═══════════════════════════════════════════════════════════════════════════
// DEPTH OCCLUSION CHECK
// ═══════════════════════════════════════════════════════════════════════════
// Determines if sphere is occluded by scene geometry using Z-depth.
//
// Parameters:
//   hitDist   - Distance to sphere surface
//   rayDir    - Ray direction
//   forward   - Camera forward vector
//   maxDist   - Scene depth (linearized)
//
// Returns:
//   True if sphere is completely behind scene geometry

bool isSphereOccluded(
    float hitDist,
    vec3 rayDir,
    vec3 forward,
    float maxDist
) {
    // Project distance along camera forward axis
    float zDepth = hitDist * dot(rayDir, forward);
    return zDepth > maxDist;
}

#endif // RAY_SPHERE_GLSL
