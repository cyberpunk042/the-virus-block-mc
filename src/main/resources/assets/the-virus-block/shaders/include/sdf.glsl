// ═══════════════════════════════════════════════════════════════════════════
// SDF PRIMITIVES - Signed Distance Functions for field effect shapes
// Include with: #include <the-virus-block:include/sdf.glsl>
// ═══════════════════════════════════════════════════════════════════════════

// 2D Signed Distance Functions
// ═══════════════════════════════════════════════════════════════════════════

// Circle SDF - returns distance to circle edge (negative inside)
float sdCircle(vec2 p, float r) {
    return length(p) - r;
}

// Line segment SDF - distance to line from a to b with thickness
float sdLine(vec2 p, vec2 a, vec2 b, float thickness) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - thickness;
}

// Box SDF (axis-aligned)
float sdBox(vec2 p, vec2 b) {
    vec2 d = abs(p) - b;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

// Ring SDF (circle outline)
float sdRing(vec2 p, float radius, float thickness) {
    return abs(length(p) - radius) - thickness;
}

// 3D Signed Distance Functions
// ═══════════════════════════════════════════════════════════════════════════

// Sphere SDF
float sdSphere(vec3 p, float r) {
    return length(p) - r;
}

// Torus SDF (centered at origin, in XZ plane)
float sdTorus(vec3 p, vec2 t) {
    // t.x = major radius, t.y = minor radius
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q) - t.y;
}

// Cylinder SDF (infinite along Y axis)
float sdCylinder(vec3 p, float r) {
    return length(p.xz) - r;
}

// Capped cylinder SDF
float sdCappedCylinder(vec3 p, float h, float r) {
    vec2 d = abs(vec2(length(p.xz), p.y)) - vec2(r, h);
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

// Capsule SDF (line with radius)
float sdCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 pa = p - a;
    vec3 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - r;
}

// Operations
// ═══════════════════════════════════════════════════════════════════════════

// Smooth minimum (for blending SDFs)
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// Union (minimum)
float opUnion(float d1, float d2) {
    return min(d1, d2);
}

// Intersection (maximum)
float opIntersection(float d1, float d2) {
    return max(d1, d2);
}

// Subtraction (difference)
float opSubtraction(float d1, float d2) {
    return max(d1, -d2);
}

// Glow/Falloff Functions
// ═══════════════════════════════════════════════════════════════════════════

// Smooth quadratic falloff
float glowFalloff(float dist, float radius) {
    float t = clamp(dist / radius, 0.0, 1.0);
    return 1.0 - t * t;
}

// Linear falloff
float linearFalloff(float dist, float radius) {
    return max(0.0, 1.0 - dist / radius);
}

// Exponential falloff (more concentrated)
float expFalloff(float dist, float radius) {
    return exp(-dist * dist / (radius * radius * 0.5));
}

// Sharp edge with soft falloff
float edgeFalloff(float dist, float thickness, float sharpness) {
    float t = abs(dist) / thickness;
    return pow(max(0.0, 1.0 - t), sharpness);
}
