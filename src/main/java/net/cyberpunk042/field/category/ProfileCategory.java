package net.cyberpunk042.field.category;

/**
 * Categories for organizing profiles in the GUI.
 * Used for filtering in Profiles tab.
 */
public enum ProfileCategory {
    COMBAT("combat", "Combat", "Battle-focused configurations"),
    UTILITY("utility", "Utility", "Functional and practical"),
    DECORATIVE("decorative", "Decorative", "Pure visual aesthetics"),
    EXPERIMENTAL("experimental", "Experimental", "Testing and WIP");

    private final String id;
    private final String displayName;
    private final String description;

    ProfileCategory(String id, String displayName, String description) {
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
     * Get category from JSON value.
     */
    public static ProfileCategory fromId(String id) {
        if (id == null) return UTILITY; // Default
        for (ProfileCategory cat : values()) {
            if (cat.id.equalsIgnoreCase(id)) {
                return cat;
            }
        }
        return UTILITY; // Default fallback
    }
}
