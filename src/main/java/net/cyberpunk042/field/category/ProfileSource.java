package net.cyberpunk042.field.category;

/**
 * Source/origin of a profile.
 * Determines editability and storage location.
 */
public enum ProfileSource {
    BUNDLED("Bundled", false),   // Shipped with mod, read-only
    LOCAL("Local", true),        // User-created, editable
    SERVER("Server", false);     // From server, read-only

    private final String displayName;
    private final boolean editable;

    ProfileSource(String displayName, boolean editable) {
        this.displayName = displayName;
        this.editable = editable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEditable() {
        return editable;
    }

    /**
     * Get icon for UI display.
     * ✎ = editable, 🔒 = read-only
     */
    public String getIcon() {
        return editable ? "✎" : "🔒";
    }
}
