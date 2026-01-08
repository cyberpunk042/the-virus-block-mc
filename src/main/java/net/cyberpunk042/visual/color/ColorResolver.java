package net.cyberpunk042.visual.color;

import net.cyberpunk042.log.Logging;

import net.cyberpunk042.config.ColorConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves color references to ARGB values.
 * 
 * <h2>Reference Formats</h2>
 * <ul>
 *   <li><b>@role</b>: Theme role reference (e.g., "@primary", "@glow")</li>
 *   <li><b>$slot</b>: ColorConfig slot reference (e.g., "$primaryColor")</li>
 *   <li><b>#hex</b>: Hex color (e.g., "#FF00AA", "#F0A")</li>
 *   <li><b>name</b>: Basic color name (e.g., "red", "cyan")</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * ColorResolver resolver = new ColorResolver(theme);
 * int color = resolver.resolve("@primary");
 * int hex = resolver.resolve("#FF00AA");
 * int named = resolver.resolve("cyan");
 * </pre>
 */
public final class ColorResolver {
    
    // =========================================================================
    // Static Constants & Factory Methods
    // =========================================================================
    
    /**
     * Default resolver using CYBER_GREEN theme.
     */
    public static final ColorResolver DEFAULT = new ColorResolver();
    
    /**
     * Creates a resolver that uses the given theme.
     * 
     * @param theme the color theme to use
     * @return a new resolver, or DEFAULT if theme is null
     */
    public static ColorResolver fromTheme(ColorTheme theme) {
        if (theme == null) return DEFAULT;
        return new ColorResolver(theme);
    }
    
    // =========================================================================
    // Basic Colors
    // =========================================================================
    
    private static final Map<String, Integer> BASIC_COLORS = new HashMap<>();
    
    static {
        // Basic color names
        BASIC_COLORS.put("white", 0xFFFFFFFF);
        BASIC_COLORS.put("black", 0xFF000000);
        BASIC_COLORS.put("red", 0xFFFF0000);
        BASIC_COLORS.put("green", 0xFF00FF00);
        BASIC_COLORS.put("blue", 0xFF0000FF);
        BASIC_COLORS.put("yellow", 0xFFFFFF00);
        BASIC_COLORS.put("cyan", 0xFF00FFFF);
        BASIC_COLORS.put("magenta", 0xFFFF00FF);
        BASIC_COLORS.put("orange", 0xFFFF8800);
        BASIC_COLORS.put("purple", 0xFF8800FF);
        BASIC_COLORS.put("pink", 0xFFFF88CC);
        BASIC_COLORS.put("gray", 0xFF888888);
        BASIC_COLORS.put("grey", 0xFF888888);
        BASIC_COLORS.put("lime", 0xFF88FF00);
        BASIC_COLORS.put("aqua", 0xFF00FFFF);
        BASIC_COLORS.put("gold", 0xFFFFCC00);
        BASIC_COLORS.put("silver", 0xFFCCCCCC);
    }
    
    // =========================================================================
    // Instance Fields
    // =========================================================================
    
    private final ColorTheme theme;
    private final String colorOverride;
    
    // =========================================================================
    // Constructors
    // =========================================================================
    
    /**
     * Creates a resolver with a theme.
     */
    public ColorResolver(ColorTheme theme) {
        this.theme = theme;
        this.colorOverride = null;
    }
    
    /**
     * Creates a resolver with a theme and a color override.
     * When an override is set, all @primary references use this color instead.
     */
    public ColorResolver(ColorTheme theme, String colorOverride) {
        this.theme = theme;
        this.colorOverride = colorOverride;
    }
    
    /**
     * Creates a resolver with the default green theme.
     */
    public ColorResolver() {
        this(ColorTheme.CYBER_GREEN);
    }
    
    // =========================================================================
    // Resolution
    // =========================================================================
    
    /**
     * Resolves a color reference string to an ARGB value.
     * 
     * @param reference color reference string
     * @return resolved ARGB color, or white (0xFFFFFFFF) if unresolved
     */
    public int resolve(String reference) {
        if (reference == null || reference.isEmpty()) {
            return 0xFFFFFFFF;
        }
        
        reference = reference.trim();
        
        // @role - Theme role reference
        if (reference.startsWith("@")) {
            String role = reference.substring(1).toLowerCase(Locale.ROOT);
            
            // If color override is set and this is @primary, use override
            if (colorOverride != null && "primary".equals(role)) {
                return resolve(colorOverride);
            }
            
            return theme.resolve(role);
        }
        
        // $slot - ColorConfig slot reference
        if (reference.startsWith("$")) {
            String slot = reference.substring(1);
            return resolveConfigSlot(slot);
        }
        
        // #hex - Hex color
        if (reference.startsWith("#")) {
            int parsed = ColorMath.parseHex(reference);
            return parsed != 0 ? parsed : 0xFFFFFFFF;
        }
        
        // Basic color name
        String lower = reference.toLowerCase(Locale.ROOT);
        if (BASIC_COLORS.containsKey(lower)) {
            return BASIC_COLORS.get(lower);
        }
        
        // Try parsing as raw hex without #
        try {
            if (reference.length() == 6 || reference.length() == 8) {
                return ColorMath.parseHex(reference);
            }
        } catch (Exception e) {
            // Fall through
        }
        
        // Default to white
        Logging.RENDER.topic("color").trace(
            "Unresolved color reference '{}', using white", reference);
        return 0xFFFFFFFF;
    }
    
    /**
     * Resolves a color with alpha override.
     */
    public int resolve(String reference, float alpha) {
        int color = resolve(reference);
        return ColorMath.withAlpha(color, alpha);
    }
    
    /**
     * Resolves a ColorConfig slot reference.
     */
    private int resolveConfigSlot(String slot) {
        // Try to match ColorConfig.ColorSlot enum values
        try {
            ColorConfig.ColorSlot colorSlot = ColorConfig.ColorSlot.valueOf(slot.toUpperCase(Locale.ROOT));
            return ColorConfig.argb(colorSlot);
        } catch (IllegalArgumentException e) {
            // Not a valid slot, try named color lookup
            Integer named = ColorConfig.resolveNamedColor(slot);
            return named != null ? named : 0xFFFFFFFF;
        }
    }
    
    // =========================================================================
    // Accessors
    // =========================================================================
    
    /**
     * Gets the current theme.
     */
    public ColorTheme getTheme() {
        return theme;
    }
    
    /**
     * Creates a new resolver with a different theme.
     */
    public ColorResolver withTheme(ColorTheme newTheme) {
        return new ColorResolver(newTheme);
    }
    
    // =========================================================================
    // Static Utilities
    // =========================================================================
    
    /**
     * Checks if a reference is a theme role reference.
     */
    public static boolean isRoleReference(String reference) {
        return reference != null && reference.startsWith("@");
    }
    
    /**
     * Checks if a reference is a config slot reference.
     */
    public static boolean isConfigReference(String reference) {
        return reference != null && reference.startsWith("$");
    }
    
    /**
     * Checks if a reference is a hex color.
     */
    public static boolean isHexColor(String reference) {
        return reference != null && reference.startsWith("#");
    }
}
