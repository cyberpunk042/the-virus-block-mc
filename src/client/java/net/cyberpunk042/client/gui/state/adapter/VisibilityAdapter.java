package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.visibility.MaskType;
import net.cyberpunk042.visual.visibility.VisibilityMask;

import java.util.Set;

/**
 * Adapter for visibility/mask state.
 * 
 * <p>Handles path-based access like "mask.count", "mask.thickness", etc.
 * Routes to the underlying VisibilityMask record.</p>
 */
@StateCategory("mask")
public class VisibilityAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private VisibilityMask mask = VisibilityMask.FULL;
    
    @Override
    public String category() { return "mask"; }
    
    @Override
    public void loadFrom(Primitive source) {
        this.mask = orDefault(source.visibility(), VisibilityMask.FULL);
        Logging.GUI.topic("adapter").trace("VisibilityAdapter loaded: mask={}", mask.mask());
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.visibility(mask);
    }
    
    public VisibilityMask mask() { return mask; }
    public void setMask(VisibilityMask mask) { this.mask = mask; }
    
    /**
     * Override get to handle paths like "mask.count", "mask.thickness" etc.
     * Path format: "mask.property" where property is a VisibilityMask field.
     */
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String field = parts[0];
        String prop = parts.length > 1 ? parts[1] : null;
        
        if (!"mask".equals(field)) {
            return super.get(path);
        }
        
        if (prop == null) {
            return mask; // Return the whole mask object
        }
        
        // Navigate into the VisibilityMask record
        return switch (prop) {
            case "mask" -> mask.mask(); // The MaskType enum
            case "count" -> mask.count();
            case "thickness" -> mask.thickness();
            case "offset" -> mask.offset();
            case "invert" -> mask.invert();
            case "feather" -> mask.feather();
            case "animate" -> mask.animate();
            case "animSpeed" -> mask.animSpeed();
            case "direction" -> mask.direction();
            case "falloff" -> mask.falloff();
            case "gradientStart" -> mask.gradientStart();
            case "gradientEnd" -> mask.gradientEnd();
            case "centerX" -> mask.centerX();
            case "centerY" -> mask.centerY();
            default -> null;
        };
    }
    
    /**
     * Override set to handle paths like "mask.count", "mask.thickness" etc.
     */
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String field = parts[0];
        String prop = parts.length > 1 ? parts[1] : null;
        
        if (!"mask".equals(field) || prop == null) {
            super.set(path, value);
            return;
        }
        
        VisibilityMask.Builder builder = mask.toBuilder();
        
        switch (prop) {
            case "mask" -> {
                MaskType type = value instanceof MaskType ? (MaskType) value :
                    MaskType.valueOf(value.toString().toUpperCase());
                builder.mask(type);
            }
            case "count" -> builder.count(toInt(value));
            case "thickness" -> builder.thickness(toFloat(value));
            case "offset" -> builder.offset(toFloat(value));
            case "invert" -> builder.invert(toBool(value));
            case "feather" -> builder.feather(toFloat(value));
            case "animate" -> builder.animate(toBool(value));
            case "animSpeed" -> builder.animSpeed(toFloat(value));
            case "direction" -> builder.direction(value != null ? value.toString() : null);
            case "falloff" -> builder.falloff(toFloat(value));
            case "gradientStart" -> builder.gradientStart(toFloat(value));
            case "gradientEnd" -> builder.gradientEnd(toFloat(value));
            case "centerX" -> builder.centerX(toFloat(value));
            case "centerY" -> builder.centerY(toFloat(value));
            default -> {
                super.set(path, value);
                return;
            }
        }
        
        this.mask = builder.build();
    }
    
    private int toInt(Object v) { return v instanceof Number n ? n.intValue() : 0; }
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 0f; }
    private boolean toBool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); }
    
    @Override
    public void reset() {
        this.mask = VisibilityMask.FULL;
    }
}
