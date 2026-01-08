package net.cyberpunk042.client.gui.panel.layout;

import net.cyberpunk042.client.gui.layout.Bounds;
import org.jetbrains.annotations.Nullable;

/**
 * Layout contract for ProfilesPanel.
 * 
 * <p>Defines the "infrastructure" - where each UI region renders,
 * decoupling layout decisions from rendering logic.</p>
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │ DUAL MODE                                               │
 * │  ┌──────────────────┐  ┌──────────────────────────────┐ │
 * │  │ filterArea()     │  │ detailsArea()                │ │
 * │  ├──────────────────┤  │  - Config summary            │ │
 * │  │ listArea()       │  │  - Stats                     │ │
 * │  │  - Profile list  │  │                              │ │
 * │  │                  │  ├──────────────────────────────┤ │
 * │  ├──────────────────┤  │ actionsArea()                │ │
 * │  │ nameFieldArea()  │  │  - Load/Save/Delete buttons  │ │
 * │  └──────────────────┘  └──────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────┘
 * 
 * ┌─────────────────────────────────────────────────────────┐
 * │ SINGLE MODE (compact)                                   │
 * │  ┌───────────────────────────────────────────────────┐  │
 * │  │ filterArea()                                      │  │
 * │  ├───────────────────────────────────────────────────┤  │
 * │  │ listArea()                                        │  │
 * │  ├───────────────────────────────────────────────────┤  │
 * │  │ actionsArea()                                     │  │
 * │  ├───────────────────────────────────────────────────┤  │
 * │  │ nameFieldArea()                                   │  │
 * │  └───────────────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 */
public interface ProfilesPanelLayout {
    
    /**
     * Area for filter controls (source, category, search).
     */
    Bounds filterArea();
    
    /**
     * Area for the scrollable profile list.
     */
    Bounds listArea();
    
    /**
     * Area for action buttons (Load, Save, Delete, etc.).
     */
    Bounds actionsArea();
    
    /**
     * Area for the profile name text field.
     */
    Bounds nameFieldArea();
    
    /**
     * Area for configuration summary and stats.
     * Returns null in single-column mode where this is not shown.
     */
    @Nullable
    Bounds detailsArea();
    
    /**
     * @return Width available for each action button.
     */
    int buttonWidth();
    
    /**
     * @return Number of buttons per row in the action area.
     */
    int buttonsPerRow();
    
    /**
     * @return Gap between buttons.
     */
    int buttonGap();
    
    /**
     * @return Whether to show the "Save to Server" button (dual mode only).
     */
    boolean showServerButton();
    
    /**
     * @return true if this is dual-panel mode.
     */
    boolean isDualMode();
    
    /**
     * @return The full panel bounds for hit testing.
     */
    Bounds fullBounds();
}
