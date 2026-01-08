package net.cyberpunk042.client.gui.state.manager;

import net.cyberpunk042.client.gui.state.FieldEditState;

/**
 * Base interface for state managers.
 * 
 * <p>Managers handle specific categories of <b>operations</b> on the state,
 * as opposed to Adapters which handle <b>data</b> categories.</p>
 * 
 * <p><b>Pattern:</b></p>
 * <pre>
 * FieldEditState (coordinator)
 *   ├── Adapters (data)    → shape, animation, fill, transform...
 *   └── Managers (operations) → layers, profiles, bindings, triggers...
 * </pre>
 * 
 * @see net.cyberpunk042.client.gui.state.adapter.PrimitiveAdapter
 */
public interface StateManager {
    
    /**
     * Gets the parent state this manager operates on.
     */
    FieldEditState state();
    
    /**
     * Resets this manager to default state.
     */
    void reset();
}
