package net.cyberpunk042.field.instance;

/**
 * Tracks the lifecycle state of a field instance.
 * 
 * <p>Used with fadeProgress (0.0-1.0) to animate transitions.</p>
 * 
 * <h3>State Flow</h3>
 * <pre>
 * spawn() called → SPAWNING (fadeIn/scaleIn)
 *     ↓
 *   ACTIVE (normal operation, bindings/triggers/decay)
 *     ↓
 * despawn() called → DESPAWNING (fadeOut/scaleOut)
 *     ↓
 *   COMPLETE → remove from manager
 * </pre>
 * 
 * @see LifecycleConfig
 * @see FieldInstance
 */
public enum LifecycleState {
    /** Field is spawning (fade in / scale in animation) */
    SPAWNING,
    
    /** Field is fully active (bindings, triggers, decay running) */
    ACTIVE,
    
    /** Field is despawning (fade out / scale out animation) */
    DESPAWNING,
    
    /** Field lifecycle is complete - ready for removal */
    COMPLETE;
    
    /** Returns true if this state allows visual rendering */
    public boolean isVisible() {
        return this == SPAWNING || this == ACTIVE || this == DESPAWNING;
    }
    
    /** Returns true if this state is a transition state */
    public boolean isTransitioning() {
        return this == SPAWNING || this == DESPAWNING;
    }
    
    /** Returns true if bindings/triggers should be evaluated */
    public boolean isProcessing() {
        return this == ACTIVE;
    }
}
