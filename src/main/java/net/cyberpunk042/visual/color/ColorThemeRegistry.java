package net.cyberpunk042.visual.color;

import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Registry for color themes.
 * 
 * <p>Provides access to built-in and custom themes.
 */
public final class ColorThemeRegistry {
    
    private static final Map<Identifier, ColorTheme> THEMES = new LinkedHashMap<>();
    
    static {
        // Register built-in themes
        register(ColorTheme.CYBER_GREEN);
        register(ColorTheme.CYBER_BLUE);
        register(ColorTheme.CYBER_RED);
        register(ColorTheme.CYBER_PURPLE);
        register(ColorTheme.SINGULARITY);
        register(ColorTheme.WHITE);
    }
    
    private ColorThemeRegistry() {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Registers a theme.
     */
    public static void register(ColorTheme theme) {
        if (theme == null || theme.id() == null) {
            return;
        }
        THEMES.put(theme.id(), theme);
        Logging.REGISTRY.topic("color").info("Registered color theme: {}", theme.name());
    }
    
    /**
     * Unregisters a theme.
     */
    public static boolean unregister(Identifier id) {
        boolean removed = THEMES.remove(id) != null;
        if (removed) {
            Logging.REGISTRY.topic("color").info("Unregistered color theme: {}", id);
        }
        return removed;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Lookup
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Gets a theme by ID.
     */
    public static ColorTheme get(Identifier id) {
        return THEMES.get(id);
    }
    
    /**
     * Gets a theme by name (assumes the-virus-block namespace).
     */
    public static ColorTheme get(String name) {
        return get(Identifier.of("the-virus-block", name));
    }
    
    /**
     * Gets a theme or returns a default.
     */
    public static ColorTheme getOrDefault(String name, ColorTheme fallback) {
        ColorTheme theme = get(name);
        return theme != null ? theme : fallback;
    }
    
    /**
     * Gets a theme or returns CYBER_GREEN.
     */
    public static ColorTheme getOrDefault(String name) {
        return getOrDefault(name, ColorTheme.CYBER_GREEN);
    }
    
    /**
     * Checks if a theme exists.
     */
    public static boolean exists(String name) {
        return get(name) != null;
    }
    
    /**
     * Derives and registers a new theme from a base color.
     */
    public static ColorTheme derive(String name, int baseColor) {
        ColorTheme theme = ColorTheme.derive(name, baseColor);
        register(theme);
        Logging.REGISTRY.topic("color").info(
            "Derived theme '{}' from base color #{}", 
            name, Integer.toHexString(baseColor).toUpperCase());
        return theme;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Returns all theme IDs.
     */
    public static Set<Identifier> ids() {
        return Collections.unmodifiableSet(THEMES.keySet());
    }
    
    /**
     * Returns all themes.
     */
    public static Collection<ColorTheme> all() {
        return Collections.unmodifiableCollection(THEMES.values());
    }
    
    /**
     * Returns the count of registered themes.
     */
    public static int count() {
        return THEMES.size();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Clears custom themes (keeps built-ins).
     */
    public static void clearCustom() {
        Set<Identifier> builtins = Set.of(
            ColorTheme.CYBER_GREEN.id(),
            ColorTheme.CYBER_BLUE.id(),
            ColorTheme.CYBER_RED.id(),
            ColorTheme.CYBER_PURPLE.id(),
            ColorTheme.SINGULARITY.id(),
            ColorTheme.WHITE.id()
        );
        int before = THEMES.size();
        THEMES.keySet().removeIf(id -> !builtins.contains(id));
        int removed = before - THEMES.size();
        if (removed > 0) {
            Logging.REGISTRY.topic("color").info("Cleared {} custom themes", removed);
        }
    }
    
    /**
     * Reloads all themes (for hot-reload).
     */
    public static void reload() {
        clearCustom();
        // Re-register built-ins (in case they were modified)
        register(ColorTheme.CYBER_GREEN);
        register(ColorTheme.CYBER_BLUE);
        register(ColorTheme.CYBER_RED);
        register(ColorTheme.CYBER_PURPLE);
        register(ColorTheme.SINGULARITY);
        register(ColorTheme.WHITE);
        Logging.REGISTRY.topic("color").info("Color theme registry reloaded");
    }
}
