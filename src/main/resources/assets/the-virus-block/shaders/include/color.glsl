// ═══════════════════════════════════════════════════════════════════════════
// COLOR UTILITIES - Color space conversions and effects
// Include with: #include <the-virus-block:include/color.glsl>
// ═══════════════════════════════════════════════════════════════════════════

// Color Space Conversions
// ═══════════════════════════════════════════════════════════════════════════

// RGB to HSV
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return c.z * mix(vec3(1.0), rgb, c.y);
}

// Hue shift (cycles through rainbow)
vec3 hueShift(vec3 color, float shift) {
    vec3 hsv = rgb2hsv(color);
    hsv.x = fract(hsv.x + shift);
    return hsv2rgb(hsv);
}

// Saturation adjustment
vec3 adjustSaturation(vec3 color, float amount) {
    vec3 hsv = rgb2hsv(color);
    hsv.y = clamp(hsv.y * amount, 0.0, 1.0);
    return hsv2rgb(hsv);
}

// Brightness adjustment
vec3 adjustBrightness(vec3 color, float amount) {
    vec3 hsv = rgb2hsv(color);
    hsv.z = clamp(hsv.z * amount, 0.0, 1.0);
    return hsv2rgb(hsv);
}

// Color Blending
// ═══════════════════════════════════════════════════════════════════════════

// Additive blend (for glow effects)
vec3 additiveBlend(vec3 base, vec3 add, float strength) {
    return base + add * strength;
}

// Screen blend (brightens without clipping)
vec3 screenBlend(vec3 base, vec3 blend) {
    return 1.0 - (1.0 - base) * (1.0 - blend);
}

// Soft light blend
vec3 softLightBlend(vec3 base, vec3 blend) {
    return mix(
        2.0 * base * blend + base * base * (1.0 - 2.0 * blend),
        sqrt(base) * (2.0 * blend - 1.0) + 2.0 * base * (1.0 - blend),
        step(0.5, blend)
    );
}

// Overlay blend (contrast enhancement)
vec3 overlayBlend(vec3 base, vec3 blend) {
    return mix(
        2.0 * base * blend,
        1.0 - 2.0 * (1.0 - base) * (1.0 - blend),
        step(0.5, base)
    );
}

// Color Gradients
// ═══════════════════════════════════════════════════════════════════════════

// Linear gradient between two colors
vec3 linearGradient(vec3 colorA, vec3 colorB, float t) {
    return mix(colorA, colorB, clamp(t, 0.0, 1.0));
}

// Three-color gradient
vec3 threeColorGradient(vec3 colorA, vec3 colorB, vec3 colorC, float t) {
    t = clamp(t, 0.0, 1.0);
    if (t < 0.5) {
        return mix(colorA, colorB, t * 2.0);
    } else {
        return mix(colorB, colorC, (t - 0.5) * 2.0);
    }
}

// Rainbow gradient (hue-based)
vec3 rainbowGradient(float t) {
    return hsv2rgb(vec3(fract(t), 1.0, 1.0));
}

// Heat map gradient (cold to hot)
vec3 heatGradient(float t) {
    t = clamp(t, 0.0, 1.0);
    vec3 cold = vec3(0.0, 0.0, 1.0);  // Blue
    vec3 mid = vec3(0.0, 1.0, 0.0);   // Green  
    vec3 hot = vec3(1.0, 0.0, 0.0);   // Red
    return threeColorGradient(cold, mid, hot, t);
}

// Visual Effects
// ═══════════════════════════════════════════════════════════════════════════

// Vignette effect
float vignette(vec2 uv, float radius, float softness) {
    vec2 centered = uv - 0.5;
    float dist = length(centered) * 2.0;
    return 1.0 - smoothstep(radius - softness, radius + softness, dist);
}

// Fresnel-like rim effect based on distance
float rimEffect(float dist, float radius, float power, float falloff) {
    float t = clamp(dist / radius, 0.0, 1.0);
    float rim = 1.0 - t;
    return pow(rim, power) * exp(-falloff * t);
}

// Bloom/glow intensity (use with additive blend)
vec3 bloom(vec3 color, float threshold, float intensity) {
    float brightness = max(max(color.r, color.g), color.b);
    float contribution = max(0.0, brightness - threshold);
    return color * contribution * intensity;
}

// Emissive glow (self-illumination)
vec3 emissiveGlow(vec3 baseColor, vec3 glowColor, float intensity) {
    return baseColor + glowColor * intensity;
}

// Tone mapping (HDR to LDR)
vec3 toneMapReinhard(vec3 hdr) {
    return hdr / (hdr + vec3(1.0));
}

// ACES filmic tone mapping
vec3 toneMapACES(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}
