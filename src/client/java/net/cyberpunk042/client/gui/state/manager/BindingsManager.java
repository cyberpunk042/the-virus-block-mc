package net.cyberpunk042.client.gui.state.manager;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.field.influence.BindingConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages dynamic property bindings.
 * 
 * <p>Bindings allow properties to be dynamically computed based on
 * player stats, time, or other factors.</p>
 */
public class BindingsManager extends AbstractManager {
    
    private final List<BindingConfig> bindings = new ArrayList<>();
    
    public BindingsManager(FieldEditState state) {
        super(state);
    }
    
    public List<BindingConfig> getAll() { return bindings; }
    
    public void add(BindingConfig binding) { 
        bindings.add(binding); 
        markDirty(); 
    }
    
    public boolean remove(String property) { 
        boolean removed = bindings.removeIf(b -> b.property().equals(property));
        if (removed) markDirty();
        return removed;
    }
    
    public void clear() { 
        bindings.clear(); 
        markDirty(); 
    }
    
    public int count() { return bindings.size(); }
    
    @Override
    public void reset() {
        bindings.clear();
    }
}
