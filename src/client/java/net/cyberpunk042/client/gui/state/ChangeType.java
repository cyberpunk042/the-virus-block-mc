package net.cyberpunk042.client.gui.state;

/**
 * Types of state changes that can trigger widget synchronization.
 * 
 * <p>Used by {@link StateChangeListener} to determine the appropriate response:
 * <ul>
 *   <li>{@link #PROFILE_LOADED} - Major change, all widgets need sync</li>
 *   <li>{@link #LAYER_SWITCHED} - Selection changed, reload primitive data</li>
 *   <li>{@link #PRIMITIVE_SWITCHED} - Selection changed, reload primitive data</li>
 *   <li>{@link #FRAGMENT_APPLIED} - Preset applied, may need rebuild if mode changed</li>
 *   <li>{@link #PROPERTY_CHANGED} - Single property, lightweight sync</li>
 *   <li>{@link #FULL_RESET} - State reset, full reinitialize</li>
 * </ul>
 * 
 * @see StateChangeListener
 * @see FieldEditState#notifyStateChanged(ChangeType)
 */
public enum ChangeType {
    
    /**
     * Entire profile was replaced (loaded from file or server).
     * All widgets should sync from state.
     */
    PROFILE_LOADED,
    
    /**
     * User selected a different primitive within the layer.
     * Primitive-scoped adapters have new data.
     */
    PRIMITIVE_SWITCHED,
    
    /**
     * User selected a different layer.
     * Both layer and primitive adapters have new data.
     */
    LAYER_SWITCHED,
    
    /**
     * A preset/fragment was applied (animation, fill, etc.).
     * May require widget rebuild if mode changed, otherwise just sync.
     */
    FRAGMENT_APPLIED,
    
    /**
     * Single property changed (e.g., from /field command).
     * Lightweight sync sufficient.
     */
    PROPERTY_CHANGED,
    
    /**
     * State was completely reset to defaults.
     * Full reinitialization needed.
     */
    FULL_RESET
}
