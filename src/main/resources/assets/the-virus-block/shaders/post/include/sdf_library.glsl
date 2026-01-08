// ═══════════════════════════════════════════════════════════════════════════
// SDF LIBRARY - Signed Distance Functions for all shape types
// ═══════════════════════════════════════════════════════════════════════════
// Include: #include "include/sdf_library.glsl"

// Shape type constants
#define SHAPE_POINT    0
#define SHAPE_SPHERE   1
#define SHAPE_TORUS    2
#define SHAPE_POLYGON  3
#define SHAPE_ORBITAL  4

// ═══════════════════════════════════════════════════════════════════════════
// SDF COMBINATION OPERATIONS (from Ronja's tutorial / hg_sdf library)
// These properly handle inside/outside/boundary for correct isodistance contours
// ═══════════════════════════════════════════════════════════════════════════

// Basic merge (union) - hard minimum
float sdf_merge(float shape1, float shape2) {
    return min(shape1, shape2);
}

// Basic intersect - hard maximum
float sdf_intersect(float shape1, float shape2) {
    return max(shape1, shape2);
}

// Smooth merge (round union) - creates smooth blend at intersection
// This is the PROPER way to combine SDFs for flower patterns!
// radius = blend zone size (0 = sharp corners, larger = more rounded)
float round_merge(float shape1, float shape2, float radius) {
    if (radius < 0.001) return sdf_merge(shape1, shape2);
    
    // Grow shapes by radius, then compute smooth inside distance
    vec2 intersectionSpace = vec2(shape1 - radius, shape2 - radius);
    intersectionSpace = min(intersectionSpace, 0.0);
    float insideDistance = -length(intersectionSpace);
    
    // Compute outside distance with proper handling
    float simpleUnion = sdf_merge(shape1, shape2);
    float outsideDistance = max(simpleUnion, radius);
    
    // Combine for complete SDF
    return insideDistance + outsideDistance;
}

// Smooth intersect - rounded intersection
float round_intersect(float shape1, float shape2, float radius) {
    if (radius < 0.001) return sdf_intersect(shape1, shape2);
    
    vec2 intersectionSpace = vec2(shape1 + radius, shape2 + radius);
    intersectionSpace = max(intersectionSpace, 0.0);
    float outsideDistance = length(intersectionSpace);
    
    float simpleIntersection = sdf_intersect(shape1, shape2);
    float insideDistance = min(simpleIntersection, -radius);
    
    return outsideDistance + insideDistance;
}

// Champfer merge - creates beveled/chamfered corners instead of rounded
float champfer_merge(float shape1, float shape2, float champferSize) {
    const float SQRT_05 = 0.70710678118;
    float simpleMerge = sdf_merge(shape1, shape2);
    float champfer = (shape1 + shape2) * SQRT_05;
    champfer = champfer - champferSize;
    return sdf_merge(simpleMerge, champfer);
}

// Legacy polynomial smooth min (kept for reference/comparison)
float smin(float a, float b, float k) {
    if (k < 0.001) return min(a, b);
    float h = clamp(0.5 + 0.5*(b-a)/k, 0.0, 1.0);
    return mix(b, a, h) - k*h*(1.0-h);
}

// ═══════════════════════════════════════════════════════════════════════════
// PRIMITIVE SDF FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Point/Sphere: distance from center (POINT has radius=0, SPHERE has radius>0)
float sdfSphere(vec3 p, vec3 center, float radius) {
    return length(p - center) - radius;
}

// Torus: donut shape in XZ plane
float sdfTorus(vec3 p, vec3 center, float majorR, float minorR) {
    vec3 q = p - center;
    vec2 t = vec2(length(q.xz) - majorR, q.y);
    return length(t) - minorR;
}

// Polygon: n-sided shape in XZ plane (distance to edges)
float sdfPolygon(vec2 p, int sides, float radius) {
    float angle = atan(p.y, p.x);
    float segmentAngle = 6.28318 / float(sides);
    float d = cos(floor(0.5 + angle / segmentAngle) * segmentAngle - angle) * length(p);
    return d - radius;
}

// Capsule SDF (line segment with radius) - for beams
float sdfCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 ab = b - a;
    vec3 ap = p - a;
    float t = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    vec3 closest = a + t * ab;
    return length(p - closest) - r;
}

// Tapered capsule (cone-like with caps)
float sdfTaperedCapsule(vec3 p, vec3 a, vec3 b, float rBottom, float rTop) {
    vec3 ab = b - a;
    vec3 ap = p - a;
    float t = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    float radius = mix(rBottom, rTop, t);  // Interpolate radius along beam
    vec3 closest = a + t * ab;
    return length(p - closest) - radius;
}
