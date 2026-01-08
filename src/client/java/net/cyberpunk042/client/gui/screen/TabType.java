package net.cyberpunk042.client.gui.screen;

/**
 * Tab types for the Field Customizer GUI.
 */
public enum TabType {
    /** Level 1: Basic controls for all players */
    QUICK("Quick", "Q", "Basic field customization"),
    
    /** Level 2: Advanced controls for detailed editing */
    ADVANCED("Advanced", "A", "Detailed parameters"),
    
    /** Level 3: Debug controls (operator only) */
    DEBUG("Debug", "D", "Internal field controls"),
    
    /** Visual Effects controls (operator only) */
    FX("FX", "F", "Visual effects & shaders"),
    
    /** Profile management */
    PROFILES("Profiles", "P", "Save and load profiles");
    
    private final String label;
    private final String shortLabel;
    private final String tooltip;
    
    TabType(String label, String shortLabel, String tooltip) {
        this.label = label;
        this.shortLabel = shortLabel;
        this.tooltip = tooltip;
    }
    
    public String label() { return label; }
    public String shortLabel() { return shortLabel; }
    public String tooltip() { return tooltip; }
}
