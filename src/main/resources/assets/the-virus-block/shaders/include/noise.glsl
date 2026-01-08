// ═══════════════════════════════════════════════════════════════════════════
// NOISE FUNCTIONS - Procedural noise for organic patterns
// Include with: #include <the-virus-block:include/noise.glsl>
// ═══════════════════════════════════════════════════════════════════════════

// Random/Hash Functions
// ═══════════════════════════════════════════════════════════════════════════

// 2D hash -> 2D (returns vec2 in [0,1])
vec2 hash22(vec2 p) {
    mat2 m = mat2(vec2(15.27, 47.63), vec2(99.41, 89.98));
    return fract(sin(p * m) * 46839.32);
}

// 2D hash -> 1D
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// 1D hash -> 1D
float hash11(float p) {
    return fract(sin(p * 127.1) * 43758.5453);
}

// 3D hash -> 1D
float hash31(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

// Voronoi Noise
// ═══════════════════════════════════════════════════════════════════════════

// Basic Voronoi - returns distance to nearest cell center
float voronoi(vec2 uv, float cellDensity, float animOffset) {
    vec2 gridUV = fract(uv * cellDensity);
    vec2 gridID = floor(uv * cellDensity);
    float minDist = 100.0;
    
    for (float y = -1.0; y <= 1.0; y++) {
        for (float x = -1.0; x <= 1.0; x++) {
            vec2 offset = vec2(x, y);
            vec2 n = hash22(gridID + offset);
            // Animate the cell centers
            vec2 p = offset + vec2(
                sin(n.x * 6.28 + animOffset) * 0.5 + 0.5,
                cos(n.y * 6.28 + animOffset) * 0.5 + 0.5
            );
            float d = distance(gridUV, p);
            minDist = min(minDist, d);
        }
    }
    
    return minDist;
}

// Voronoi with both distance and cell ID
vec3 voronoiExtended(vec2 uv, float cellDensity, float animOffset) {
    vec2 gridUV = fract(uv * cellDensity);
    vec2 gridID = floor(uv * cellDensity);
    float minDist = 100.0;
    vec2 nearestCell = vec2(0.0);
    
    for (float y = -1.0; y <= 1.0; y++) {
        for (float x = -1.0; x <= 1.0; x++) {
            vec2 offset = vec2(x, y);
            vec2 cellId = gridID + offset;
            vec2 n = hash22(cellId);
            vec2 p = offset + vec2(
                sin(n.x * 6.28 + animOffset) * 0.5 + 0.5,
                cos(n.y * 6.28 + animOffset) * 0.5 + 0.5
            );
            float d = distance(gridUV, p);
            if (d < minDist) {
                minDist = d;
                nearestCell = cellId;
            }
        }
    }
    
    return vec3(minDist, nearestCell);
}

// Perlin-like Value Noise
// ═══════════════════════════════════════════════════════════════════════════

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    
    // Smooth interpolation
    f = f * f * (3.0 - 2.0 * f);
    
    // 4 corner values
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    
    // Bilinear interpolation
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// Fractal Brownian Motion (layered noise)
float fbm(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    
    for (int i = 0; i < octaves; i++) {
        value += amplitude * valueNoise(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value;
}

// Simplex-like Gradient Noise (approximation)
// ═══════════════════════════════════════════════════════════════════════════

vec2 gradientDir(vec2 p) {
    float n = hash21(p);
    float angle = n * 6.28318;
    return vec2(cos(angle), sin(angle));
}

float gradientNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    
    // Smooth curve
    vec2 u = f * f * (3.0 - 2.0 * f);
    
    // Gradients at corners
    float n00 = dot(gradientDir(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0));
    float n10 = dot(gradientDir(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0));
    float n01 = dot(gradientDir(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0));
    float n11 = dot(gradientDir(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0));
    
    // Interpolate
    return mix(mix(n00, n10, u.x), mix(n01, n11, u.x), u.y) * 0.5 + 0.5;
}
