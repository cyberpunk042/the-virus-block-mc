// ═══════════════════════════════════════════════════════════════════════════
// TRANSFORM FUNCTIONS - UV and coordinate transformations
// Include with: #include <the-virus-block:include/transforms.glsl>
// ═══════════════════════════════════════════════════════════════════════════

// 2D Rotation
// ═══════════════════════════════════════════════════════════════════════════

// Create 2D rotation matrix
mat2 rotate2D(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, s, -s, c);
}

// Rotate UV around a center point
vec2 rotateUV(vec2 uv, vec2 center, float angle) {
    vec2 delta = uv - center;
    delta = rotate2D(angle) * delta;
    return delta + center;
}

// Twirl/Vortex Effect
// ═══════════════════════════════════════════════════════════════════════════

// Twirl UV - rotation increases with distance from center
vec2 twirlUV(vec2 uv, vec2 center, float strength) {
    vec2 delta = uv - center;
    float dist = length(delta);
    float angle = strength * dist;
    delta = rotate2D(angle) * delta;
    return delta + center;
}

// Inverse twirl - rotation decreases with distance
vec2 inverseTwirlUV(vec2 uv, vec2 center, float strength) {
    vec2 delta = uv - center;
    float dist = length(delta);
    float angle = strength / (dist + 0.1);  // +0.1 prevents division by zero
    delta = rotate2D(angle) * delta;
    return delta + center;
}

// Polar Coordinates
// ═══════════════════════════════════════════════════════════════════════════

// Convert cartesian to polar (returns vec2(radius, angle))
vec2 cartToPolar(vec2 p) {
    float r = length(p);
    float theta = atan(p.y, p.x);
    return vec2(r, theta);
}

// Convert polar to cartesian
vec2 polarToCart(vec2 polar) {
    float r = polar.x;
    float theta = polar.y;
    return vec2(r * cos(theta), r * sin(theta));
}

// Polar repeat - tiles pattern around center
vec2 polarRepeat(vec2 uv, float segments) {
    vec2 polar = cartToPolar(uv);
    float segmentAngle = 6.28318 / segments;
    polar.y = mod(polar.y, segmentAngle);
    return polarToCart(polar);
}

// World Position Reconstruction
// ═══════════════════════════════════════════════════════════════════════════

// Reconstruct world position from screen UV and depth
vec3 worldPosFromDepth(vec2 uv, float depth, mat4 invViewProj) {
    // Convert UV to clip space (-1 to 1)
    vec4 clipPos = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    
    // Transform by inverse view-projection
    vec4 worldPos = invViewProj * clipPos;
    
    // Perspective divide
    return worldPos.xyz / worldPos.w;
}

// Screen-Space Effects
// ═══════════════════════════════════════════════════════════════════════════

// Radial distortion (barrel/pincushion)
vec2 radialDistort(vec2 uv, float strength) {
    vec2 centered = uv - 0.5;
    float dist = length(centered);
    float factor = 1.0 + strength * dist * dist;
    return centered * factor + 0.5;
}

// Chromatic aberration direction
vec2 chromaticDir(vec2 uv) {
    return normalize(uv - 0.5);
}

// Wave distortion
vec2 waveDistort(vec2 uv, float amplitude, float frequency, float time) {
    float wave = sin(uv.y * frequency + time) * amplitude;
    return vec2(uv.x + wave, uv.y);
}

// Fisheye effect
vec2 fisheye(vec2 uv, float strength) {
    vec2 centered = uv - 0.5;
    float dist = length(centered);
    float bind = sqrt(dot(centered, centered));
    vec2 result = (centered / bind) * atan(dist * strength) / atan(strength);
    return result + 0.5;
}
