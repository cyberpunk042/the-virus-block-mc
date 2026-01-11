package net.cyberpunk042.client.visual.render.target;

/**
 * OpenGL texture internal formats for framebuffer color attachments.
 * 
 * <p>RGBA16F (half-float) is the sweet spot for HDR post-processing:
 * <ul>
 *   <li>65504 max value (vs 1.0 for RGBA8)</li>
 *   <li>10-bit mantissa precision</li>
 *   <li>2x memory of RGBA8, 0.5x memory of RGBA32F</li>
 * </ul>
 * 
 * <p>Used by {@link HdrTargetFactory} to create framebuffers with custom formats.
 */
public enum TextureFormat {
    
    /** Standard 8-bit per channel (default, LDR) */
    RGBA8(0x8058, "RGBA8"),
    
    /** 16-bit unsigned integer per channel */
    RGBA16(0x805B, "RGBA16"),
    
    /** 16-bit half-float per channel (recommended for HDR) */
    RGBA16F(0x881A, "RGBA16F"),
    
    /** 32-bit float per channel (overkill for most uses) */
    RGBA32F(0x8814, "RGBA32F");
    
    private final int glConstant;
    private final String jsonName;
    
    TextureFormat(int glConstant, String jsonName) {
        this.glConstant = glConstant;
        this.jsonName = jsonName;
    }
    
    /**
     * Get the OpenGL constant for this format.
     * Used in glTexImage2D calls.
     */
    public int glConstant() {
        return glConstant;
    }
    
    /**
     * Get the JSON-friendly name for this format.
     * Used in post_effect JSON target definitions.
     */
    public String jsonName() {
        return jsonName;
    }
    
    /**
     * Parse from JSON string (case-insensitive).
     * 
     * @param name The format name from JSON
     * @return The matching format, or null if not recognized
     */
    public static TextureFormat fromJson(String name) {
        if (name == null) return null;
        for (TextureFormat f : values()) {
            if (f.jsonName.equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }
    
    /**
     * Check if the current GPU supports this format.
     * 
     * <p>RGBA16F has been standard since OpenGL 3.0 / GLES 3.0,
     * which is required for modern Minecraft anyway.
     * 
     * @return true if supported (currently always true for listed formats)
     */
    public boolean isSupported() {
        // For RGBA16F: effectively universal on any machine running modern MC
        // Could add explicit GL capability check if needed:
        // GL.getCapabilities().OpenGL30 or check extension GL_ARB_texture_float
        return true;
    }
    
    /**
     * Get memory multiplier relative to RGBA8.
     * Useful for memory budget calculations.
     */
    public float memoryMultiplier() {
        return switch (this) {
            case RGBA8 -> 1.0f;
            case RGBA16, RGBA16F -> 2.0f;
            case RGBA32F -> 4.0f;
        };
    }
}
