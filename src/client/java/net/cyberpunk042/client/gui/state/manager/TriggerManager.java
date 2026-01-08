package net.cyberpunk042.client.gui.state.manager;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.field.influence.TriggerConfig;
import net.cyberpunk042.log.Logging;

/**
 * Manages trigger testing and preview.
 * 
 * <p>Handles test trigger firing for GUI preview purposes.</p>
 */
public class TriggerManager extends AbstractManager {
    
    private TriggerConfig activeConfig;
    private long startTime;
    
    public TriggerManager(FieldEditState state) {
        super(state);
    }
    
    /**
     * Fires a test trigger for preview.
     */
    public void fire(TriggerConfig config) {
        this.activeConfig = config;
        this.startTime = System.currentTimeMillis();
        Logging.GUI.topic("trigger").debug("Test trigger fired: {}", config);
    }
    
    /**
     * Gets the active test trigger if still running, null otherwise.
     */
    public TriggerConfig getActive() {
        if (activeConfig == null) return null;
        long elapsed = System.currentTimeMillis() - startTime;
        long durationMs = activeConfig.duration() * 50L; // ticks to ms
        if (elapsed > durationMs) {
            activeConfig = null;
            return null;
        }
        return activeConfig;
    }
    
    /**
     * Gets the progress of the active trigger (0-1), or -1 if none.
     */
    public float getProgress() {
        if (activeConfig == null) return -1f;
        long elapsed = System.currentTimeMillis() - startTime;
        long durationMs = activeConfig.duration() * 50L;
        if (elapsed > durationMs) {
            activeConfig = null;
            return -1f;
        }
        return elapsed / (float) durationMs;
    }
    
    /**
     * Returns true if a test trigger is currently active.
     */
    public boolean isActive() {
        return getActive() != null;
    }
    
    @Override
    public void reset() {
        activeConfig = null;
        startTime = 0;
    }
}
