package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.primitive.PrimitiveLink;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Axis;

/**
 * Adapter for primitive linking state.
 * 
 * <p>Manages a single link for the current primitive. The link has:
 * <ul>
 *   <li>ONE target primitive</li>
 *   <li>Boolean flags for link types (radiusMatch, follow, etc.)</li>
 *   <li>Offset values</li>
 * </ul>
 * </p>
 */
@StateCategory("link")
public class LinkAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    // Current primitive ID (read-only from primitive)
    private String primitiveId = "";
    
    // The single link
    private PrimitiveLink link = PrimitiveLink.NONE;
    
    @Override
    public String category() { return "link"; }
    
    @Override
    public void loadFrom(Primitive source) {
        if (source == null) {
            reset();
            return;
        }
        
        this.primitiveId = source.id() != null ? source.id() : "";
        this.link = source.link() != null ? source.link() : PrimitiveLink.NONE;
        
        Logging.GUI.topic("adapter").trace("LinkAdapter loaded: target={}", link.target());
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        // Only save if link is valid (has target and at least one link type)
        if (link.isValid() && link.hasAnyLinkType()) {
            builder.link(link);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GET/SET
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        return switch (prop) {
            case "primitiveId" -> primitiveId;
            case "target" -> link.target();
            case "radiusMatch" -> link.radiusMatch();
            case "radiusOffset" -> link.radiusOffset();
            case "follow" -> link.follow();
            case "followDynamic" -> link.followDynamic();
            case "mirror" -> link.mirror() != null ? link.mirror().name() : "NONE";
            case "phaseOffset" -> link.phaseOffset();
            case "scaleWith" -> link.scaleWith();
            case "orbitSync" -> link.orbitSync();
            case "orbitPhaseOffset" -> link.orbitPhaseOffset();
            case "orbitRadiusOffset" -> link.orbitRadiusOffset();
            case "orbitSpeedMult" -> link.orbitSpeedMult();
            case "orbitInclinationOffset" -> link.orbitInclinationOffset();
            case "orbitPrecessionOffset" -> link.orbitPrecessionOffset();
            case "colorMatch" -> link.colorMatch();
            case "alphaMatch" -> link.alphaMatch();
            default -> super.get(path);
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        PrimitiveLink.Builder b = link.toBuilder();
        
        switch (prop) {
            case "target" -> b.target(toStringOrNull(value));
            case "radiusMatch" -> b.radiusMatch(toBool(value));
            case "radiusOffset" -> b.radiusOffset(toFloat(value));
            case "follow" -> b.follow(toBool(value));
            case "followDynamic" -> b.followDynamic(toBool(value));
            case "mirror" -> b.mirror(parseMirror(value));
            case "phaseOffset" -> b.phaseOffset(toFloat(value));
            case "scaleWith" -> b.scaleWith(toBool(value));
            case "orbitSync" -> b.orbitSync(toBool(value));
            case "orbitPhaseOffset" -> b.orbitPhaseOffset(toFloat(value));
            case "orbitRadiusOffset" -> b.orbitRadiusOffset(toFloat(value));
            case "orbitSpeedMult" -> b.orbitSpeedMult(toFloat(value));
            case "orbitInclinationOffset" -> b.orbitInclinationOffset(toFloat(value));
            case "orbitPrecessionOffset" -> b.orbitPrecessionOffset(toFloat(value));
            case "colorMatch" -> b.colorMatch(toBool(value));
            case "alphaMatch" -> b.alphaMatch(toBool(value));
            default -> { super.set(path, value); return; }
        }
        
        this.link = b.build();
    }
    
    private Axis parseMirror(Object value) {
        if (value == null) return null;
        String s = value.toString();
        if ("NONE".equals(s) || s.isEmpty()) return null;
        try {
            return Axis.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
    
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 0f; }
    private boolean toBool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); }
    private String toStringOrNull(Object v) { 
        if (v == null) return null;
        String s = v.toString();
        return s.isEmpty() ? null : s;
    }
    
    // Typed accessors
    public String primitiveId() { return primitiveId; }
    public PrimitiveLink currentLink() { return link; }
    
    @Override
    public void reset() {
        this.primitiveId = "";
        this.link = PrimitiveLink.NONE;
    }
}
