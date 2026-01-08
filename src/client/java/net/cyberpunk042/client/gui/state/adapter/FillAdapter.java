package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;

import java.util.Set;

/**
 * Adapter for fill configuration (fill mode, wire thickness, depth options, cage).
 */
@StateCategory("fill")
public class FillAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private FillConfig fill = FillConfig.SOLID;
    
    @Override
    public String category() { return "fill"; }
    
    @Override
    public void loadFrom(Primitive source) {
        this.fill = orDefault(source.fill(), FillConfig.SOLID);
        Logging.GUI.topic("adapter").trace("FillAdapter loaded: mode={}", fill.mode());
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.fill(fill);
    }
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        return switch (prop) {
            case "fill" -> fill;
            case "mode" -> fill.mode().name();
            case "wireThickness" -> fill.wireThickness();
            case "pointSize" -> fill.pointSize();
            case "doubleSided" -> fill.doubleSided();
            case "depthTest" -> fill.depthTest();
            case "depthWrite" -> fill.depthWrite();
            case "cage" -> fill.cage();
            default -> super.get(path);
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        switch (prop) {
            case "fill" -> {
                if (value instanceof FillConfig fc) this.fill = fc;
            }
            case "mode" -> {
                FillMode mode = value instanceof FillMode fm ? fm : FillMode.valueOf(value.toString());
                this.fill = fill.toBuilder().mode(mode).build();
            }
            case "wireThickness" -> this.fill = fill.toBuilder().wireThickness(toFloat(value)).build();
            case "pointSize" -> this.fill = fill.toBuilder().pointSize(toFloat(value)).build();
            case "doubleSided" -> this.fill = fill.toBuilder().doubleSided(toBool(value)).build();
            case "depthTest" -> this.fill = fill.toBuilder().depthTest(toBool(value)).build();
            case "depthWrite" -> this.fill = fill.toBuilder().depthWrite(toBool(value)).build();
            case "cage" -> {
                if (value instanceof net.cyberpunk042.visual.fill.CageOptions co) {
                    this.fill = fill.toBuilder().cage(co).build();
                }
            }
            default -> super.set(path, value);
        }
    }
    
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 0f; }
    private boolean toBool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); }
    
    public FillConfig fill() { return fill; }
    public void setFill(FillConfig fill) { this.fill = fill; }
    
    @Override
    public void reset() {
        this.fill = FillConfig.SOLID;
    }
}

