#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

// Custom UBO for Fresnel parameters (bound by CustomUniformBinder via Mixin)
// Includes Horizon (rim), Corona (outer glow), AND Animation parameters
layout(std140) uniform FresnelParams {
    vec4 RimColorAndPower;       // xyz = RimColor, w = RimPower
    vec4 RimIntensityAndPad;     // x = RimIntensity, yzw = padding
    // Corona parameters (extended glow layer)
    vec4 CoronaColorAndPower;    // xyz = CoronaColor, w = CoronaPower
    vec4 CoronaIntensityFalloff; // x = CoronaIntensity, y = CoronaFalloff, zw = padding
    vec4 CoronaOffsetWidthPad;   // x = Offset (bias), y = Width, zw = padding
    // Animation parameters (time-based effects)
    // x = time (seconds), y = mode (bitmask as float), z = speed, w = strength
    vec4 AnimationParams;
};

// ═══════════════════════════════════════════════════════════════════════════
// ANIMATION MODE FLAGS (extracted from AnimationParams.y)
// ═══════════════════════════════════════════════════════════════════════════
// Bit 0 (1):  BREATHE_CORONA   - Corona intensity breathing
// Bit 1 (2):  BREATHE_RIM      - Rim intensity breathing
// Bit 2 (4):  PULSE_ALPHA      - Overall alpha pulsing
// Bit 3 (8):  COLOR_CYCLE      - Hue rotation over time
// Bit 4 (16): FLICKER          - Random-ish energy flicker
// Bit 5 (32): WAVE_INTENSITY   - Wave pattern on intensity
// Access via: u_time = AnimationParams.x, u_animMode = int(AnimationParams.y), etc.


in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec3 vNormal;
in vec3 vViewDir;

out vec4 fragColor;

// ═══════════════════════════════════════════════════════════════════════════
// ANIMATION HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Animation mode bit flags
const int ANIM_BREATHE_CORONA = 1;   // Bit 0
const int ANIM_BREATHE_RIM    = 2;   // Bit 1
const int ANIM_PULSE_ALPHA    = 4;   // Bit 2
const int ANIM_COLOR_CYCLE    = 8;   // Bit 3
const int ANIM_FLICKER        = 16;  // Bit 4
const int ANIM_WAVE_INTENSITY = 32;  // Bit 5

// Extract animation parameters from UBO
#define u_time        AnimationParams.x
#define u_animMode    int(AnimationParams.y)
#define u_animSpeed   AnimationParams.z
#define u_animStrength AnimationParams.w

// Check if animation mode is active
bool hasMode(int mode) {
    return (u_animMode & mode) != 0;
}

// Smooth breathing: slow sinusoidal oscillation (good for corona/rim)
// Returns value in range [1 - strength, 1 + strength]
float breathe(float baseSpeed) {
    float t = u_time * u_animSpeed * baseSpeed;
    return 1.0 + sin(t) * u_animStrength;
}

// Sharper pulse: faster with eased peaks
float pulse(float baseSpeed) {
    float t = u_time * u_animSpeed * baseSpeed;
    float s = sin(t);
    return 1.0 + (s * s * sign(s)) * u_animStrength;  // Squared for sharper peaks
}

// Pseudo-random flicker using time
float flicker() {
    float t = u_time * u_animSpeed * 3.0;  // Base frequency (was 15, too fast)
    // Combine multiple frequencies for organic feel
    float f = sin(t * 1.0) * 0.5 + sin(t * 2.3) * 0.3 + sin(t * 5.7) * 0.2;
    return 1.0 + f * u_animStrength;
}

// Wave pattern based on edge factor
float waveIntensity(float edgeFactor) {
    float t = u_time * u_animSpeed * 3.0;
    float wave = sin(edgeFactor * 6.28318 * 3.0 + t);
    return 1.0 + wave * u_animStrength * 0.5;
}

// HSV to RGB conversion for color cycling
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// RGB to HSV conversion
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// Apply color cycling (rainbow effect) to a color
// Blends between original color and hue-shifted version based on strength
vec3 cycleColor(vec3 color) {
    vec3 hsv = rgb2hsv(color);
    float originalHue = hsv.x;
    hsv.x = fract(originalHue + u_time * u_animSpeed * 0.1);  // Slow hue rotation
    vec3 cycled = hsv2rgb(hsv);
    // Blend between original and cycled based on strength
    return mix(color, cycled, u_animStrength);
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN FRAGMENT SHADER
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    // Start with vertex color directly
    vec4 color = vertexColor * ColorModulator;
    
    // Calculate view angle (same for both effects)
    float NdotV = abs(dot(vNormal, vViewDir));
    float edgeFactor = 1.0 - NdotV;  // 1 at edges, 0 at center
    
    // ========================================
    // ANIMATION MODIFIERS
    // ========================================
    float rimAnimMod = 1.0;
    float coronaAnimMod = 1.0;
    float alphaAnimMod = 1.0;
    
    // Apply animation modes if u_animMode > 0
    if (u_animMode > 0) {
        // Rim breathing (slow, gentle)
        if (hasMode(ANIM_BREATHE_RIM)) {
            rimAnimMod *= breathe(0.8);
        }
        
        // Corona breathing (slightly faster)
        if (hasMode(ANIM_BREATHE_CORONA)) {
            coronaAnimMod *= breathe(1.2);
        }
        
        // Pulse (affects rim and corona intensity, not alpha)
        if (hasMode(ANIM_PULSE_ALPHA)) {
            float p = pulse(2.0);
            rimAnimMod *= p;
            coronaAnimMod *= p;
        }
        
        // Flicker (affects both rim and corona)
        if (hasMode(ANIM_FLICKER)) {
            float f = flicker();
            rimAnimMod *= f;
            coronaAnimMod *= f;
        }
        
        // Wave intensity (edge-based pattern, affects both rim and corona)
        if (hasMode(ANIM_WAVE_INTENSITY)) {
            float w = waveIntensity(edgeFactor);
            rimAnimMod *= w;
            coronaAnimMod *= w;
        }
    }
    
    // ========================================
    // HORIZON EFFECT (Rim Lighting)
    // ========================================
    vec3 rimColor = RimColorAndPower.xyz;
    float rimPower = RimColorAndPower.w;
    float rimIntensity = RimIntensityAndPad.x;
    
    // Apply color cycling to rim
    if (hasMode(ANIM_COLOR_CYCLE)) {
        rimColor = cycleColor(rimColor);
    }
    
    // Only apply if intensity > 0
    if (rimIntensity > 0.001) {
        float fresnel = pow(edgeFactor, rimPower);
        vec3 rimContribution = rimColor * fresnel * rimIntensity * rimAnimMod;
        color.rgb += rimContribution;
    }
    
    // ========================================
    // CORONA EFFECT (Outer Glow Layer)
    // ========================================
    vec3 coronaColor = CoronaColorAndPower.xyz;
    float coronaPower = CoronaColorAndPower.w;
    float coronaIntensity = CoronaIntensityFalloff.x;
    float coronaFalloff = CoronaIntensityFalloff.y;
    float coronaOffset = CoronaOffsetWidthPad.x;   // Shifts glow outward (bias)
    float coronaWidth = CoronaOffsetWidthPad.y;    // Controls glow thickness
    
    // Apply color cycling to corona
    if (hasMode(ANIM_COLOR_CYCLE)) {
        coronaColor = cycleColor(coronaColor);
    }
    
    // Only apply if intensity > 0
    if (coronaIntensity > 0.001) {
        // Apply offset (bias): adds to edge factor to extend glow "beyond" edge
        // Positive offset = glow starts earlier (wider coverage)
        float biasedEdge = clamp(edgeFactor + coronaOffset, 0.0, 1.0);
        
        // Apply power for sharpness
        float corona = pow(biasedEdge, coronaPower);
        
        // Apply width modifier
        corona = pow(corona, 1.0 / max(coronaWidth, 0.1));
        
        // Apply intensity with animation modifier
        float glow = corona * coronaIntensity * coronaAnimMod;
        
        // Apply falloff
        glow *= exp(-coronaFalloff * (1.0 - corona));
        
        // Add corona glow to color (additive)
        color.rgb += coronaColor * glow;
    }
    
    // ========================================
    // FINAL OUTPUT
    // ========================================
    // Apply alpha animation
    color.a *= alphaAnimMod;
    
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}

