// ═══════════════════════════════════════════════════════════════════════════
// EFFECTS: GEODESIC V1 (Animated Geodesic Sphere)
// ═══════════════════════════════════════════════════════════════════════════
// 
// Ported from Shadertoy "Geodesic Tiling" by tdhooper
// https://www.shadertoy.com/view/llVXRd
//
// Creates an animated geodesic sphere with hexagonal cells,
// using icosahedral symmetry (20-fold) and SDF raymarching.
//
// Include: #include "include/effects/geodesic_v1.glsl"
// Prerequisites: core/, camera/, sdf/
//
// ═══════════════════════════════════════════════════════════════════════════

#ifndef EFFECTS_GEODESIC_V1_GLSL
#define EFFECTS_GEODESIC_V1_GLSL

#include "../core/constants.glsl"
#include "../core/math_utils.glsl"

// ═══════════════════════════════════════════════════════════════════════════
// RESULT STRUCTURE
// ═══════════════════════════════════════════════════════════════════════════

struct GeodesicV1Result {
    bool didHit;
    vec3 color;
    float alpha;
    float distance;
    float glow;
};

// ═══════════════════════════════════════════════════════════════════════════
// HG_SDF UTILITIES
// From https://www.shadertoy.com/view/Xs3GRB
// ═══════════════════════════════════════════════════════════════════════════

// 2D rotation
void pR(inout vec2 p, float a) {
    p = cos(a)*p + sin(a)*vec2(p.y, -p.x);
}

// Reflect across a plane
float pReflect(inout vec3 p, vec3 planeNormal, float offset) {
    float t = dot(p, planeNormal) + offset;
    if (t < 0.) {
        p = p - (2.*t)*planeNormal;
    }
    return sign(t);
}

// Smooth maximum for rounded edges
float smax(float a, float b, float r) {
    float m = max(a, b);
    if ((-a < r) && (-b < r)) {
        return max(m, -(r - sqrt((r+a)*(r+a) + (r+b)*(r+b))));
    } else {
        return m;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ICOSAHEDRON DOMAIN MIRRORING
// Adapted from knighty https://www.shadertoy.com/view/MsKGzw
// ═══════════════════════════════════════════════════════════════════════════

// Icosahedron folding planes
vec3 geo_nc;      // 3rd folding plane
vec3 geo_pab;     // Vertex planes
vec3 geo_pbc;
vec3 geo_pca;

// Face projection planes
vec3 geo_facePlane;
vec3 geo_uPlane;
vec3 geo_vPlane;

// Inner radius of icosahedron face
const float geo_faceRadius = 0.3819660112501051;

void initIcosahedron() {
    // Type 5 = icosahedron (5-fold symmetry)
    float cospin = cos(PI / 5.0);
    float scospin = sqrt(0.75 - cospin * cospin);
    
    // 3rd folding plane (others are xz and yz)
    geo_nc = vec3(-0.5, -cospin, scospin);
    
    // Vertex planes (normalized for better DE)
    geo_pbc = normalize(vec3(scospin, 0.0, 0.5));
    geo_pca = normalize(vec3(0.0, scospin, cospin));
    geo_pab = vec3(0.0, 0.0, 1.0);
    
    // Face plane for coordinate projection
    geo_facePlane = geo_pca;
    geo_uPlane = cross(vec3(1.0, 0.0, 0.0), geo_facePlane);
    geo_vPlane = vec3(1.0, 0.0, 0.0);
}

// Mirror point through icosahedral symmetry (20-fold → 1 fundamental domain)
void pModIcosahedron(inout vec3 p) {
    p = abs(p);
    pReflect(p, geo_nc, 0.0);
    p.xy = abs(p.xy);
    pReflect(p, geo_nc, 0.0);
    p.xy = abs(p.xy);
    pReflect(p, geo_nc, 0.0);
}

// ═══════════════════════════════════════════════════════════════════════════
// TRIANGLE TILING (2D)
// Adapted from mattz https://www.shadertoy.com/view/4d2GzV
// ═══════════════════════════════════════════════════════════════════════════

const float sqrt3 = 1.7320508075688772;
const float i3 = 0.5773502691896258;

const mat2 cart2hex = mat2(1.0, 0.0, i3, 2.0 * i3);
const mat2 hex2cart = mat2(1.0, 0.0, -0.5, 0.5 * sqrt3);

struct TriPoints {
    vec2 a;
    vec2 b;
    vec2 c;
    vec2 center;
    vec2 ab;
    vec2 bc;
    vec2 ca;
};

TriPoints closestTriPoints(vec2 p) {    
    vec2 pTri = cart2hex * p;
    vec2 pi = floor(pTri);
    vec2 pf = fract(pTri);
    
    float split1 = step(pf.y, pf.x);
    float split2 = step(pf.x, pf.y);
    
    vec2 a = vec2(split1, 1.0);
    vec2 b = vec2(1.0, split2);
    vec2 c = vec2(0.0, 0.0);

    a += pi;
    b += pi;
    c += pi;

    a = hex2cart * a;
    b = hex2cart * b;
    c = hex2cart * c;
    
    vec2 center = (a + b + c) / 3.0;
    
    vec2 ab = (a + b) / 2.0;
    vec2 bc = (b + c) / 2.0;
    vec2 ca = (c + a) / 2.0;

    return TriPoints(a, b, c, center, ab, bc, ca);
}

// ═══════════════════════════════════════════════════════════════════════════
// GEODESIC TILING (3D projection)
// ═══════════════════════════════════════════════════════════════════════════

struct TriPoints3D {
    vec3 a;
    vec3 b;
    vec3 c;
    vec3 center;
    vec3 ab;
    vec3 bc;
    vec3 ca;
};

// Ray-plane intersection
vec3 geo_intersection(vec3 n, vec3 planeNormal, float planeOffset) {
    float denominator = dot(planeNormal, n);
    float t = (dot(vec3(0.0), planeNormal) + planeOffset) / -denominator;
    return n * t;
}

// Get 2D coordinates on icosahedron face
vec2 icosahedronFaceCoordinates(vec3 p) {
    vec3 pn = normalize(p);
    vec3 i = geo_intersection(pn, geo_facePlane, -1.0);
    return vec2(dot(i, geo_uPlane), dot(i, geo_vPlane));
}

// Project 2D face coordinates back to sphere
vec3 faceToSphere(vec2 facePoint) {
    return normalize(geo_facePlane + (geo_uPlane * facePoint.x) + (geo_vPlane * facePoint.y));
}

// Get geodesic triangle points for a 3D position
TriPoints3D geodesicTriPoints(vec3 p, float subdivisions) {
    // Get 2D cartesian coordinates on face
    vec2 uv = icosahedronFaceCoordinates(p);
    
    // Scale by subdivisions
    float uvScale = subdivisions / geo_faceRadius / 2.0;
    TriPoints points = closestTriPoints(uv * uvScale);
    
    // Project 2D triangle coordinates back to sphere
    vec3 a = faceToSphere(points.a / uvScale);
    vec3 b = faceToSphere(points.b / uvScale);
    vec3 c = faceToSphere(points.c / uvScale);
    vec3 center = faceToSphere(points.center / uvScale);
    vec3 ab = faceToSphere(points.ab / uvScale);
    vec3 bc = faceToSphere(points.bc / uvScale);
    vec3 ca = faceToSphere(points.ca / uvScale);
    
    return TriPoints3D(a, b, c, center, ab, bc, ca);
}

// ═══════════════════════════════════════════════════════════════════════════
// SPECTRUM COLOR PALETTE
// From IQ https://www.shadertoy.com/view/ll2GD3
// ═══════════════════════════════════════════════════════════════════════════

vec3 geo_pal(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    return a + b * cos(TAU * (c * t + d));
}

vec3 geo_spectrum(float n) {
    return geo_pal(n, vec3(0.5), vec3(0.5), vec3(1.0), vec3(0.0, 0.33, 0.67));
}

// ═══════════════════════════════════════════════════════════════════════════
// HEX CELL PARAMETERS
// ═══════════════════════════════════════════════════════════════════════════

struct HexSpec {
    float roundTop;
    float roundCorner;
    float height;
    float thickness;
    float gap;
};

HexSpec newHexSpec(float subdivisions) {
    return HexSpec(
        0.05 / subdivisions,   // roundTop
        0.1 / subdivisions,    // roundCorner
        2.0,                   // height
        2.0,                   // thickness
        0.005                  // gap
    );
}

// ═══════════════════════════════════════════════════════════════════════════
// HEX CELL MODEL
// ═══════════════════════════════════════════════════════════════════════════

struct GeoModel {
    float dist;
    vec3 albedo;
    float glow;
};

GeoModel hexModel(
    vec3 p,
    vec3 hexCenter,
    vec3 edgeA,
    vec3 edgeB,
    HexSpec spec,
    vec3 faceColor,
    vec3 backColor,
    float edgeColorOffset
) {
    float d;

    float edgeADist = dot(p, edgeA) + spec.gap;
    float edgeBDist = dot(p, edgeB) - spec.gap;
    float edgeDist = smax(edgeADist, -edgeBDist, spec.roundCorner);

    float outerDist = length(p) - spec.height;
    d = smax(edgeDist, outerDist, spec.roundTop);

    float innerDist = length(p) - spec.height + spec.thickness;
    d = smax(d, -innerDist, spec.roundTop);
    
    vec3 color;

    float faceBlend = (spec.height - length(p)) / spec.thickness;
    faceBlend = clamp(faceBlend, 0.0, 1.0);
    color = mix(faceColor, backColor, step(0.5, faceBlend));
    
    // Spectrum edge coloring with offset parameter
    vec3 edgeColor = geo_spectrum(dot(hexCenter, geo_pca) * edgeColorOffset + length(p) + 0.8);    
    float edgeBlend = smoothstep(-0.04, -0.005, edgeDist);
    color = mix(color, edgeColor, edgeBlend); 

    return GeoModel(d, color, edgeBlend);
}

// Union operation
GeoModel opU(GeoModel m1, GeoModel m2) {
    if (m1.dist < m2.dist) {
        return m1;
    } else {
        return m2;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GEODESIC MODEL (combines all hex cells)
// ═══════════════════════════════════════════════════════════════════════════

GeoModel geodesicModel(
    vec3 p,
    float subdivisions,
    float hexHeight,
    float hexThickness,
    float hexGap,
    float roundTop,
    float roundCorner,
    float time,
    float animMode,
    vec3 faceColor,
    vec3 backColor,
    float edgeColorOffset,
    float waveResolution,      // USER CONTROLLED wave resolution (1-100)
    float waveAmplitude  // USER CONTROLLED wave amplitude (0-1)
) {
    // Fold into icosahedral fundamental domain
    pModIcosahedron(p);
    
    // Get geodesic triangle points
    TriPoints3D points = geodesicTriPoints(p, subdivisions);
    
    // Calculate edge planes
    vec3 edgeAB = normalize(cross(points.center, points.ab));
    vec3 edgeBC = normalize(cross(points.center, points.bc));
    vec3 edgeCA = normalize(cross(points.center, points.ca));
    
    // Base spec values
    float baseRoundTop = roundTop / subdivisions;
    float baseRoundCorner = roundCorner / subdivisions;
    
    GeoModel model, part;
    HexSpec spec;
    vec3 hexCenter;
    float offset, blend, wave, heightVar, gapVar;
    
    // ═══════════════════════════════════════════════════════════════════════
    // HEX CELL B - Calculate spec based on its center
    // ═══════════════════════════════════════════════════════════════════════
    hexCenter = points.b;
    spec.roundTop = baseRoundTop;
    spec.roundCorner = baseRoundCorner;
    spec.height = hexHeight;
    spec.thickness = hexThickness;
    spec.gap = hexGap;
    
    if (animMode > 0.05 && animMode < 0.15) {
        offset = time * 3.0 * PI - subdivisions;
        blend = dot(hexCenter, geo_pca);
        blend = cos(blend * waveResolution + offset) * 0.5 + 0.5;
        heightVar = hexHeight * waveAmplitude;
        spec.height = hexHeight - heightVar + blend * heightVar * 2.0;
        spec.thickness = spec.height;
    } else if (animMode > 0.15 && animMode < 0.25) {
        blend = hexCenter.y;
        wave = sin(blend * waveResolution * 0.5 + time * PI) * 0.5 + 0.5;
        heightVar = hexHeight * waveAmplitude;
        spec.height = hexHeight - heightVar + wave * heightVar * 2.0;
    } else if (animMode > 0.25) {
        blend = acos(dot(hexCenter, geo_pab)) * 10.0;
        blend = cos(blend + time * PI) * 0.5 + 0.5;
        spec.gap = mix(0.01, 0.4 * waveAmplitude, blend) / subdivisions;
        spec.thickness = spec.roundTop * 2.0;
    }
    
    part = hexModel(p, points.b, edgeAB, edgeBC, spec, faceColor, backColor, edgeColorOffset);
    model = part;

    // ═══════════════════════════════════════════════════════════════════════
    // HEX CELL C - Calculate spec based on its center
    // ═══════════════════════════════════════════════════════════════════════
    hexCenter = points.c;
    spec.roundTop = baseRoundTop;
    spec.roundCorner = baseRoundCorner;
    spec.height = hexHeight;
    spec.thickness = hexThickness;
    spec.gap = hexGap;
    
    if (animMode > 0.05 && animMode < 0.15) {
        offset = time * 3.0 * PI - subdivisions;
        blend = dot(hexCenter, geo_pca);
        blend = cos(blend * waveResolution + offset) * 0.5 + 0.5;
        heightVar = hexHeight * waveAmplitude;
        spec.height = hexHeight - heightVar + blend * heightVar * 2.0;
        spec.thickness = spec.height;
    } else if (animMode > 0.15 && animMode < 0.25) {
        blend = hexCenter.y;
        wave = sin(blend * waveResolution * 0.5 + time * PI) * 0.5 + 0.5;
        heightVar = hexHeight * waveAmplitude;
        spec.height = hexHeight - heightVar + wave * heightVar * 2.0;
    } else if (animMode > 0.25) {
        blend = acos(dot(hexCenter, geo_pab)) * 10.0;
        blend = cos(blend + time * PI) * 0.5 + 0.5;
        spec.gap = mix(0.01, 0.4 * waveAmplitude, blend) / subdivisions;
        spec.thickness = spec.roundTop * 2.0;
    }
    
    part = hexModel(p, points.c, edgeBC, edgeCA, spec, faceColor, backColor, edgeColorOffset);
    model = opU(model, part);
    
    // ═══════════════════════════════════════════════════════════════════════
    // HEX CELL A - Calculate spec based on its center
    // ═══════════════════════════════════════════════════════════════════════
    hexCenter = points.a;
    spec.roundTop = baseRoundTop;
    spec.roundCorner = baseRoundCorner;
    spec.height = hexHeight;
    spec.thickness = hexThickness;
    spec.gap = hexGap;
    
    if (animMode > 0.05 && animMode < 0.15) {
        offset = time * 3.0 * PI - subdivisions;
        blend = dot(hexCenter, geo_pca);
        blend = cos(blend * waveResolution + offset) * 0.5 + 0.5;
        heightVar = hexHeight * waveAmplitude;
        spec.height = hexHeight - heightVar + blend * heightVar * 2.0;
        spec.thickness = spec.height;
    } else if (animMode > 0.15 && animMode < 0.25) {
        blend = hexCenter.y;
        wave = sin(blend * waveResolution * 0.5 + time * PI) * 0.5 + 0.5;
        heightVar = hexHeight * waveAmplitude;
        spec.height = hexHeight - heightVar + wave * heightVar * 2.0;
    } else if (animMode > 0.25) {
        blend = acos(dot(hexCenter, geo_pab)) * 10.0;
        blend = cos(blend + time * PI) * 0.5 + 0.5;
        spec.gap = mix(0.01, 0.4 * waveAmplitude, blend) / subdivisions;
        spec.thickness = spec.roundTop * 2.0;
    }
    
    part = hexModel(p, points.a, edgeCA, edgeAB, spec, faceColor, backColor, edgeColorOffset);
    model = opU(model, part);
    
    return model;
}

// ═══════════════════════════════════════════════════════════════════════════
// NORMAL CALCULATION (SDF gradient)
// ═══════════════════════════════════════════════════════════════════════════

vec3 calcGeodesicNormal(
    vec3 pos,
    float subdivisions,
    float hexHeight,
    float hexThickness,
    float hexGap,
    float roundTop,
    float roundCorner,
    float time,
    float animMode,
    vec3 faceColor,
    vec3 backColor,
    float edgeColorOffset,
    float waveResolution,
    float waveAmplitude
) {
    vec3 eps = vec3(0.001, 0.0, 0.0);
    float d = geodesicModel(pos, subdivisions, hexHeight, hexThickness, hexGap, roundTop, roundCorner, time, animMode, faceColor, backColor, edgeColorOffset, waveResolution, waveAmplitude).dist;
    return normalize(vec3(
        geodesicModel(pos + eps.xyy, subdivisions, hexHeight, hexThickness, hexGap, roundTop, roundCorner, time, animMode, faceColor, backColor, edgeColorOffset, waveResolution, waveAmplitude).dist - d,
        geodesicModel(pos + eps.yxy, subdivisions, hexHeight, hexThickness, hexGap, roundTop, roundCorner, time, animMode, faceColor, backColor, edgeColorOffset, waveResolution, waveAmplitude).dist - d,
        geodesicModel(pos + eps.yyx, subdivisions, hexHeight, hexThickness, hexGap, roundTop, roundCorner, time, animMode, faceColor, backColor, edgeColorOffset, waveResolution, waveAmplitude).dist - d
    ));
}

// ═══════════════════════════════════════════════════════════════════════════
// LIGHTING
// Adapted from IQ https://www.shadertoy.com/view/Xds3zN
// ═══════════════════════════════════════════════════════════════════════════

vec3 doGeodesicLighting(GeoModel model, vec3 pos, vec3 nor, vec3 rd, float intensity) {
    vec3 lightPos = normalize(vec3(0.5, 0.5, -1.0));
    vec3 backLightPos = normalize(vec3(-0.5, -0.3, 1.0));
    vec3 ambientPos = vec3(0.0, 1.0, 0.0);
    
    float amb = clamp((dot(nor, ambientPos) + 1.0) / 2.0, 0.0, 1.0);
    float dif = clamp(dot(nor, lightPos), 0.0, 1.0);
    float bac = pow(clamp(dot(nor, backLightPos), 0.0, 1.0), 1.5);
    float fre = pow(clamp(1.0 + dot(nor, rd), 0.0, 1.0), 2.0);
    
    vec3 lin = vec3(0.0);
    lin += 1.20 * dif * vec3(0.9);
    lin += 0.80 * amb * vec3(0.5, 0.7, 0.8);
    lin += 0.30 * bac * vec3(0.25);
    lin += 0.20 * fre * vec3(1.0);
    
    vec3 albedo = model.albedo;
    vec3 col = mix(albedo * lin, albedo, model.glow);    

    return col * intensity;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN RENDER FUNCTION
// ═══════════════════════════════════════════════════════════════════════════

GeodesicV1Result renderGeodesicV1(
    // Ray & Position
    vec3 rayOrigin,
    vec3 rayDir,
    vec3 forward,
    float maxDist,
    vec3 sphereCenter,
    float sphereRadius,
    
    // Time
    float time,
    float rotationSpeed,
    
    // Colors
    vec3 faceColor,
    vec3 backColor,
    vec3 backgroundColor,
    
    // Geodesic parameters
    float subdivisions,
    float hexHeight,
    float hexThickness,
    float hexGap,
    float roundTop,
    float roundCorner,
    float animMode,
    float edgeColorOffset,
    float domeClip,        // 0 = full sphere, >0 = clip bottom (0-1 range, 0.5=hemisphere)
    float waveResolution,        // Wave resolution (1-100) - USER CONTROL
    float waveAmplitude,   // Wave amplitude (0-1) - USER CONTROL
    
    // Intensity
    float intensity
) {
    GeodesicV1Result result;
    result.didHit = false;
    result.color = vec3(0.0);
    result.alpha = 0.0;
    result.distance = maxDist;
    result.glow = 0.0;
    
    // Initialize icosahedron folding planes
    initIcosahedron();
    
    // Raymarch constants (dynamic based on camera distance)
    float distToCenter = length(rayOrigin - sphereCenter);
    float MAX_TRACE_DISTANCE = (distToCenter + sphereRadius * 3.0) / sphereRadius;
    const float INTERSECTION_PRECISION = 0.001;
    const int NUM_TRACE_STEPS = 100;
    const float FUDGE_FACTOR = 0.9;
    
    // Scale ray to sphere space
    vec3 ro = (rayOrigin - sphereCenter) / sphereRadius;
    vec3 rd = rayDir;
    
    float t = 0.0;
    GeoModel model;
    
    for (int i = 0; i < NUM_TRACE_STEPS; i++) {
        if (t > MAX_TRACE_DISTANCE) break;
        
        vec3 p = ro + rd * t;
        
        // Apply model rotation (user-controlled speed)
        pR(p.xz, time * rotationSpeed);
        
        // Dome clipping: skip points below clip plane
        // domeClip of 0.5 = hemisphere (clips at y=0)
        // domeClip of 0 = full sphere (no clipping)
        // domeClip of 1.0 = clips almost everything
        if (domeClip > 0.001) {
            float clipY = (domeClip * 2.0 - 1.0) * hexHeight;  // Map 0-1 to -height to +height
            if (p.y < clipY) {
                // Below clip plane - step forward and continue
                t += 0.1;
                continue;
            }
        }
        
        model = geodesicModel(p, subdivisions, hexHeight, hexThickness, hexGap, roundTop, roundCorner, time, animMode, faceColor, backColor, edgeColorOffset, waveResolution, waveAmplitude);
        
        if (model.dist < INTERSECTION_PRECISION) {
            result.didHit = true;
            result.distance = t * sphereRadius;
            break;
        }
        
        t += model.dist * FUDGE_FACTOR;
    }
    
    if (result.didHit) {
        vec3 pos = ro + rd * t;
        
        // Apply same rotation for normal calculation
        pR(pos.xz, time * rotationSpeed);
        
        vec3 normal = calcGeodesicNormal(pos, subdivisions, hexHeight, hexThickness, hexGap, roundTop, roundCorner, time, animMode, faceColor, backColor, edgeColorOffset, waveResolution, waveAmplitude);
        
        vec3 color = doGeodesicLighting(model, pos, normal, rd, intensity);
        
        // Occlusion check (V1 style)
        float hitZDepth = result.distance * dot(rayDir, forward);
        if (hitZDepth > maxDist) {
            // Behind scene geometry
            float distSceneToGeo = hitZDepth - maxDist;
            distSceneToGeo = max(10.0, distSceneToGeo);
            float bleedRange = 1000.0;
            float occlusionBleed = max(0.0, 1.0 - (distSceneToGeo / bleedRange));
            float effectVisibility = min(occlusionBleed, 0.9);
            color *= effectVisibility;
            result.alpha = effectVisibility;
        } else {
            result.alpha = 1.0;
        }
        
        result.color = color;
        result.glow = model.glow;
    } else {
        // Background
        result.color = backgroundColor;
        result.alpha = 0.0;
    }
    
    return result;
}

#endif // EFFECTS_GEODESIC_V1_GLSL
