package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.ArrangementConfig;

import java.util.Set;

/**
 * Adapter for arrangement/pattern state.
 */
@StateCategory("arrangement")
public class ArrangementAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private ArrangementConfig arrangement = ArrangementConfig.DEFAULT;
    
    @Override
    public String category() { return "arrangement"; }
    
    @Override
    public void loadFrom(Primitive source) {
        this.arrangement = orDefault(source.arrangement(), ArrangementConfig.DEFAULT);
        Logging.GUI.topic("adapter").trace("ArrangementAdapter loaded");
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.arrangement(arrangement);
    }
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        return switch (prop) {
            case "arrangement" -> arrangement;
            case "defaultPattern" -> arrangement.defaultPattern();
            case "main" -> arrangement.main();
            case "poles" -> arrangement.poles();
            case "equator" -> arrangement.equator();
            case "hemisphereTop" -> arrangement.hemisphereTop();
            case "hemisphereBottom" -> arrangement.hemisphereBottom();
            case "surface" -> arrangement.surface();
            case "innerEdge" -> arrangement.innerEdge();
            case "outerEdge" -> arrangement.outerEdge();
            case "sides" -> arrangement.sides();
            case "capTop" -> arrangement.capTop();
            case "capBottom" -> arrangement.capBottom();
            default -> super.get(path);
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        switch (prop) {
            case "arrangement" -> {
                if (value instanceof ArrangementConfig ac) this.arrangement = ac;
            }
            case "defaultPattern" -> this.arrangement = arrangement.toBuilder().defaultPattern(toString(value)).build();
            case "main" -> this.arrangement = arrangement.toBuilder().main(toString(value)).build();
            case "poles" -> this.arrangement = arrangement.toBuilder().poles(toString(value)).build();
            case "equator" -> this.arrangement = arrangement.toBuilder().equator(toString(value)).build();
            case "hemisphereTop" -> this.arrangement = arrangement.toBuilder().hemisphereTop(toString(value)).build();
            case "hemisphereBottom" -> this.arrangement = arrangement.toBuilder().hemisphereBottom(toString(value)).build();
            case "surface" -> this.arrangement = arrangement.toBuilder().surface(toString(value)).build();
            case "innerEdge" -> this.arrangement = arrangement.toBuilder().innerEdge(toString(value)).build();
            case "outerEdge" -> this.arrangement = arrangement.toBuilder().outerEdge(toString(value)).build();
            case "sides" -> this.arrangement = arrangement.toBuilder().sides(toString(value)).build();
            case "capTop" -> this.arrangement = arrangement.toBuilder().capTop(toString(value)).build();
            case "capBottom" -> this.arrangement = arrangement.toBuilder().capBottom(toString(value)).build();
            default -> super.set(path, value);
        }
    }
    
    private String toString(Object v) { return v != null ? v.toString() : null; }
    
    public ArrangementConfig arrangement() { return arrangement; }
    public void setArrangement(ArrangementConfig arrangement) { this.arrangement = arrangement; }
    
    @Override
    public void reset() {
        this.arrangement = ArrangementConfig.DEFAULT;
    }
}

