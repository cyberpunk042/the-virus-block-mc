package net.cyberpunk042.client.gui.state.manager;

import net.cyberpunk042.client.gui.state.FieldEditState;

/**
 * Base class for state managers with common functionality.
 */
public abstract class AbstractManager implements StateManager {
    
    protected final FieldEditState state;
    
    protected AbstractManager(FieldEditState state) {
        this.state = state;
    }
    
    @Override
    public FieldEditState state() {
        return state;
    }
    
    /**
     * Marks the parent state as dirty (modified).
     */
    protected void markDirty() {
        state.markDirty();
    }
}
