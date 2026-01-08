package net.cyberpunk042.visual.color;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * A named color theme with role-based colors.
 * 
 * <h2>Color Roles</h2>
 * <ul>
 *   <li><b>primary</b>: Main color for the theme</li>
 *   <li><b>secondary</b>: Complementary color</li>
 *   <li><b>glow</b>: Emissive/glow color</li>
 *   <li><b>beam</b>: Vertical beam color</li>
 *   <li><b>wire</b>: Wireframe color</li>
 *   <li><b>accent</b>: Highlight/accent color</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * ColorTheme theme = ColorTheme.builder("cyber_green")
 *     .primary(0xFF00FF00)
 *     .secondary(0xFF008800)
 *     .glow(0xFF44FF44)
 *     .build();
 * 
 * int color = theme.resolve("primary");
 * </pre>
 * 
 * @see ColorResolver
 */
public final class ColorTheme {
    
    private final Identifier id;
    private final int base;
    private final boolean autoDerive;
    private final Map<String, Integer> roles;
    
    private ColorTheme(Identifier id, int base, boolean autoDerive, Map<String, Integer> roles) {
        this.id = id;
        this.base = base;
        this.autoDerive = autoDerive;
        this.roles = Map.copyOf(roles);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Built-in themes
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final ColorTheme CYBER_GREEN = builder("cyber_green")
        .base(0xFF00FF88)
        .primary(0xFF00FF88)
        .secondary(0xFF008844)
        .glow(0xFF44FFAA)
        .beam(0xFF00FF88)
        .wire(0xFF00CC66)
        .accent(0xFF88FFCC)
        .build();
    
    public static final ColorTheme CYBER_BLUE = builder("cyber_blue")
        .base(0xFF0088FF)
        .primary(0xFF0088FF)
        .secondary(0xFF004488)
        .glow(0xFF44AAFF)
        .beam(0xFF0088FF)
        .wire(0xFF0066CC)
        .accent(0xFF88CCFF)
        .build();
    
    public static final ColorTheme CYBER_RED = builder("cyber_red")
        .base(0xFFFF4444)
        .primary(0xFFFF4444)
        .secondary(0xFF882222)
        .glow(0xFFFF6666)
        .beam(0xFFFF4444)
        .wire(0xFFCC3333)
        .accent(0xFFFF8888)
        .build();
    
    public static final ColorTheme CYBER_PURPLE = builder("cyber_purple")
        .base(0xFFAA44FF)
        .primary(0xFFAA44FF)
        .secondary(0xFF552288)
        .glow(0xFFCC66FF)
        .beam(0xFFAA44FF)
        .wire(0xFF8833CC)
        .accent(0xFFDD88FF)
        .build();
    
    public static final ColorTheme SINGULARITY = builder("singularity")
        .base(0xFF220033)
        .primary(0xFF440066)
        .secondary(0xFF220033)
        .glow(0xFF6600AA)
        .beam(0xFF8800CC)
        .wire(0xFF330055)
        .accent(0xFFAA00FF)
        .build();
    
    public static final ColorTheme WHITE = builder("white")
        .base(0xFFFFFFFF)
        .primary(0xFFFFFFFF)
        .secondary(0xFFCCCCCC)
        .glow(0xFFFFFFFF)
        .beam(0xFFFFFFFF)
        .wire(0xFFDDDDDD)
        .accent(0xFFFFFFFF)
        .build();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Accessors
    // ═══════════════════════════════════════════════════════════════════════════
    
    public Identifier id() {
        return id;
    }
    
    public String name() {
        return id.getPath();
    }
    
    public int getBase() {
        return base;
    }
    
    public boolean isAutoDerive() {
        return autoDerive;
    }
    
    /**
     * Resolves a color role.
     * @param role role name (primary, secondary, glow, etc.)
     * @return the color, or base if role not found
     */
    public int resolve(String role) {
        return roles.getOrDefault(role.toLowerCase(), base);
    }
    
    public int getPrimary() {
        return resolve("primary");
    }
    
    public int getSecondary() {
        return resolve("secondary");
    }
    
    public int getGlow() {
        return resolve("glow");
    }
    
    public int getBeam() {
        return resolve("beam");
    }
    
    public int getWire() {
        return resolve("wire");
    }
    
    public int getAccent() {
        return resolve("accent");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder(String name) {
        return new Builder(Identifier.of("the-virus-block", name));
    }
    
    public static Builder builder(Identifier id) {
        return new Builder(id);
    }
    
    /**
     * Creates a derived theme from a base color.
     * Automatically generates all roles from the base.
     */
    /**
     * Creates a derived theme from a base color.
     * Automatically generates all roles per ARCHITECTURE.md algorithm.
     * 
     * @param name the theme name
     * @param baseColor the base ARGB color to derive from
     * @return derived theme with all standard roles
     */
    public static ColorTheme derive(String name, int baseColor) {
        // Per ARCHITECTURE.md Auto-Derivation Algorithm (lines 437-454)
        return builder(name)
            .base(baseColor)
            .autoDerive(true)
            .primary(baseColor)
            .secondary(ColorMath.darken(baseColor, 0.30f))
            .glow(ColorMath.lighten(baseColor, 0.25f))
            .accent(ColorMath.lighten(ColorMath.saturate(baseColor, 0.10f), 0.40f))
            .beam(ColorMath.lighten(baseColor, 0.35f))
            .wire(ColorMath.darken(baseColor, 0.20f))
            .build();
    }
    
    public static final class Builder {
        private final Identifier id;
        private int base = 0xFFFFFFFF;
        private boolean autoDerive = false;
        private final Map<String, Integer> roles = new HashMap<>();
        
        private Builder(Identifier id) {
            this.id = id;
        }
        
        public Builder base(int color) {
            this.base = color;
            return this;
        }
        
        public Builder autoDerive(boolean auto) {
            this.autoDerive = auto;
            return this;
        }
        
        public Builder role(String name, int color) {
            roles.put(name.toLowerCase(), color);
            return this;
        }
        
        public Builder primary(int color) { return role("primary", color); }
        public Builder secondary(int color) { return role("secondary", color); }
        public Builder glow(int color) { return role("glow", color); }
        public Builder beam(int color) { return role("beam", color); }
        public Builder wire(int color) { return role("wire", color); }
        public Builder accent(int color) { return role("accent", color); }
        
        public ColorTheme build() {
            return new ColorTheme(id, base, autoDerive, roles);
        }
    }
}
