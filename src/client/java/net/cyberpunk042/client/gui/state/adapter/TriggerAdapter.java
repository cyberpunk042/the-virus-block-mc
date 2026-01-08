package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;

/**
 * Adapter for trigger testing UI state.
 * 
 * <p>These are UI-only values for the trigger test panel, not part of primitives.</p>
 */
@StateCategory("trigger")
public class TriggerAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private String triggerType = "PLAYER_DAMAGE";
    @StateField private String triggerEffect = "FLASH";
    @StateField private float triggerIntensity = 1.0f;
    @StateField private int triggerDuration = 20;
    
    @Override
    public String category() { return "trigger"; }
    
    @Override
    public void loadFrom(Primitive source) {
        // Trigger settings are UI-only, not loaded from primitive
        Logging.GUI.topic("adapter").trace("TriggerAdapter ready");
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        // Trigger settings are UI-only, not saved to primitive
    }
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        return switch (prop) {
            case "triggerType" -> triggerType;
            case "triggerEffect" -> triggerEffect;
            case "triggerIntensity" -> triggerIntensity;
            case "triggerDuration" -> triggerDuration;
            default -> super.get(path);
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        switch (prop) {
            case "triggerType" -> this.triggerType = value != null ? value.toString() : "PLAYER_DAMAGE";
            case "triggerEffect" -> this.triggerEffect = value != null ? value.toString() : "FLASH";
            case "triggerIntensity" -> this.triggerIntensity = toFloat(value);
            case "triggerDuration" -> this.triggerDuration = toInt(value);
            default -> super.set(path, value);
        }
    }
    
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 1.0f; }
    private int toInt(Object v) { return v instanceof Number n ? n.intValue() : 20; }
    
    @Override
    public void reset() {
        this.triggerType = "PLAYER_DAMAGE";
        this.triggerEffect = "FLASH";
        this.triggerIntensity = 1.0f;
        this.triggerDuration = 20;
    }
}
