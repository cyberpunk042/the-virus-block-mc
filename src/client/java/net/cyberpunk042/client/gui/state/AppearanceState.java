package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.visual.appearance.ColorDistribution;
import net.cyberpunk042.visual.appearance.ColorMode;
import net.cyberpunk042.visual.appearance.ColorSet;
import net.cyberpunk042.visual.appearance.GradientDirection;
import net.cyberpunk042.visual.layer.BlendMode;

/**
 * GUI editing state for appearance properties.
 * 
 * <p>Uses raw int colors (for color pickers) rather than String references
 * like the serialization {@link net.cyberpunk042.visual.appearance.Appearance} record.</p>
 */
public record AppearanceState(
    int color,
    float alphaMin,
    float alphaMax,
    float glow,
    float emissive,
    float saturation,
    float brightness,
    float hueShift,
    int primaryColor,
    int secondaryColor,
    float colorBlend,
    // Color mode system
    ColorMode colorMode,
    ColorDistribution colorDistribution,
    ColorSet colorSet,
    GradientDirection gradientDirection,
    float timePhase,
    // Blend mode for layer compositing
    BlendMode blendMode
) {
    public static final AppearanceState DEFAULT = new AppearanceState(
        0xFF00FFFF,  // color (cyan)
        1.0f,        // alphaMin
        1.0f,        // alphaMax
        0.5f,        // glow
        0f,          // emissive
        1f,          // saturation (1.0 = no change)
        1f,          // brightness (1.0 = no change)
        0f,          // hueShift (0 = no shift)
        0xFF00FFFF,  // primaryColor
        0xFFFF00FF,  // secondaryColor (magenta)
        0f,          // colorBlend (0 = only primary, 1 = only secondary)
        ColorMode.GRADIENT,                // colorMode
        ColorDistribution.UNIFORM,      // colorDistribution
        ColorSet.RAINBOW,               // colorSet
        GradientDirection.Y_AXIS,       // gradientDirection
        0f,                             // timePhase
        BlendMode.NORMAL                // blendMode
    );

    // Legacy compatibility: single alpha accessor
    public float alpha() { return alphaMin; }

    public static Builder builder() { return new Builder(); }
    
    public Builder toBuilder() {
        return new Builder()
            .color(color)
            .alphaMin(alphaMin)
            .alphaMax(alphaMax)
            .glow(glow)
            .emissive(emissive)
            .saturation(saturation)
            .brightness(brightness)
            .hueShift(hueShift)
            .primaryColor(primaryColor)
            .secondaryColor(secondaryColor)
            .colorBlend(colorBlend)
            .colorMode(colorMode)
            .colorDistribution(colorDistribution)
            .colorSet(colorSet)
            .gradientDirection(gradientDirection)
            .timePhase(timePhase)
            .blendMode(blendMode);
    }

    public static class Builder {
        private int color = 0xFF00FFFF;
        private float alphaMin = 1.0f;
        private float alphaMax = 1.0f;
        private float glow = 0.5f;
        private float emissive = 0f;
        private float saturation = 1f;
        private float brightness = 1f;
        private float hueShift = 0f;
        private int primaryColor = 0xFF00FFFF;
        private int secondaryColor = 0xFFFF00FF;
        private float colorBlend = 0f;
        private ColorMode colorMode = ColorMode.GRADIENT;
        private ColorDistribution colorDistribution = ColorDistribution.UNIFORM;
        private ColorSet colorSet = ColorSet.RAINBOW;
        private GradientDirection gradientDirection = GradientDirection.Y_AXIS;
        private float timePhase = 0f;
        private BlendMode blendMode = BlendMode.NORMAL;

        public Builder color(int c) { this.color = c; return this; }
        public Builder alphaMin(float a) { this.alphaMin = a; return this; }
        public Builder alphaMax(float a) { this.alphaMax = a; return this; }
        public Builder alpha(float a) { this.alphaMin = a; this.alphaMax = a; return this; }
        public Builder glow(float g) { this.glow = g; return this; }
        public Builder emissive(float e) { this.emissive = e; return this; }
        public Builder saturation(float s) { this.saturation = s; return this; }
        public Builder brightness(float b) { this.brightness = b; return this; }
        public Builder hueShift(float h) { this.hueShift = h; return this; }
        public Builder primaryColor(int c) { this.primaryColor = c; return this; }
        public Builder secondaryColor(int c) { this.secondaryColor = c; return this; }
        public Builder colorBlend(float b) { this.colorBlend = b; return this; }
        public Builder colorMode(ColorMode m) { this.colorMode = m; return this; }
        public Builder colorDistribution(ColorDistribution d) { this.colorDistribution = d; return this; }
        public Builder colorSet(ColorSet s) { this.colorSet = s; return this; }
        public Builder gradientDirection(GradientDirection d) { this.gradientDirection = d; return this; }
        public Builder timePhase(float p) { this.timePhase = p; return this; }
        public Builder blendMode(BlendMode b) { this.blendMode = b != null ? b : BlendMode.NORMAL; return this; }
        /** @deprecated Use timePhase instead */
        @Deprecated public Builder rainbowSpeed(float s) { this.timePhase = s; return this; }

        public AppearanceState build() {
            return new AppearanceState(color, alphaMin, alphaMax, glow, emissive, 
                saturation, brightness, hueShift, primaryColor, secondaryColor, colorBlend,
                colorMode, colorDistribution, colorSet, gradientDirection, timePhase, blendMode);
        }
    }
}
