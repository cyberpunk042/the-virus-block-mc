package net.cyberpunk042.field.category;

/**
 * Categories for organizing presets in the GUI.
 * Used for two-tier dropdown: [Category ▼] [Preset ▼]
 */
public enum PresetCategory {
    ADDITIVE("additive", "Additive", "Add elements to your field"),
    STYLE("style", "Style", "Change visual appearance"),
    ANIMATION("animation", "Animation", "Motion and effects"),
    EFFECT("effect", "Effect", "Composite transformations"),
    PERFORMANCE("performance", "Performance", "Adjust detail level");

    private final String id;
    private final String displayName;
    private final String description;

    PresetCategory(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get category from folder name/id.
     */
    public static PresetCategory fromId(String id) {
        for (PresetCategory cat : values()) {
            if (cat.id.equalsIgnoreCase(id)) {
                return cat;
            }
        }
        return STYLE; // Default fallback
    }

    /**
     * Get the folder name for this category.
     */
    public String getFolderName() {
        return id;
    }
}
