package net.cyberpunk042.visual.shape;

import com.google.gson.JsonObject;

import net.cyberpunk042.visual.pattern.CellType;
import org.joml.Vector3f;

import java.util.Map;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Torus (donut) shape - a ring with circular cross-section.
 * 
 * <p>Unlike {@link RingShape} which is a flat band, TorusShape has a
 * circular tube cross-section, creating a true 3D donut shape.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "shape": {
 *   "type": "torus",
 *   "majorRadius": 1.0,    // Distance from center to tube center
 *   "minorRadius": 0.25,   // Radius of the tube itself
 *   "majorSegments": 32,   // Segments around the ring
 *   "minorSegments": 16,   // Segments around the tube cross-section
 *   "arcStart": 0,
 *   "arcEnd": 360
 * }
 * </pre>
 * 
 * <h2>Parts</h2>
 * <ul>
 *   <li><b>surface</b> (QUAD) - Main torus surface</li>
 *   <li><b>innerEdge</b> (EDGE) - Inner ring edge (for partial arcs)</li>
 *   <li><b>outerEdge</b> (EDGE) - Outer ring edge (for partial arcs)</li>
 * </ul>
 * 
 * @see Shape
 * @see RingShape
 * @see CellType
 */
public record TorusShape(
    @Range(ValueRange.RADIUS) float majorRadius,
    @Range(ValueRange.RADIUS) float minorRadius,
    @Range(ValueRange.STEPS) int majorSegments,
    @Range(ValueRange.STEPS) int minorSegments,
    @Range(ValueRange.DEGREES) @JsonField(skipIfDefault = true) float arcStart,
    @Range(ValueRange.DEGREES) @JsonField(skipIfDefault = true, defaultValue = "360") float arcEnd
) implements Shape {
    
    /** Default torus (1.0 major, 0.25 minor). */
    public static final TorusShape DEFAULT = new TorusShape(
        1.0f, 0.25f, 32, 16, 0, 360);
    
    /** Thin torus (1.0 major, 0.1 minor). */
    public static final TorusShape THIN = new TorusShape(
        1.0f, 0.1f, 32, 12, 0, 360);
    
    /** Fat torus (0.75 major, 0.5 minor). */
    public static final TorusShape FAT = new TorusShape(
        0.75f, 0.5f, 32, 24, 0, 360);
    
    /**
     * Creates a torus with specified radii.
     * @param majorRadius Distance from center to tube center
     * @param minorRadius Radius of the tube
     */
    public static TorusShape of(float majorRadius, float minorRadius) {
        return new TorusShape(majorRadius, minorRadius, 32, 16, 0, 360);
    }
    
    @Override
    public String getType() {
        return "torus";
    }
    
    @Override
    public Vector3f getBounds() {
        float d = (majorRadius + minorRadius) * 2;
        float h = minorRadius * 2;
        return new Vector3f(d, h, d);
    }
    
    @Override
    public CellType primaryCellType() {
        return CellType.QUAD;
    }
    
    @Override
    public Map<String, CellType> getParts() {
        return Map.of(
            "surface", CellType.QUAD,
            "innerEdge", CellType.EDGE,
            "outerEdge", CellType.EDGE
        );
    }
    
    @Override
    public float getRadius() {
        return majorRadius + minorRadius;
    }
    
    /** Inner radius (major - minor). */
    public float innerRadius() {
        return majorRadius - minorRadius;
    }
    
    /** Outer radius (major + minor). */
    public float outerRadius() {
        return majorRadius + minorRadius;
    }
    
    /** Whether this is a full torus or partial arc. */
    public boolean isFullTorus() {
        return arcStart == 0 && arcEnd == 360;
    }
    
    @Override
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }

    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    /** Create a builder pre-populated with this shape's values. */
    public Builder toBuilder() {
        return new Builder()
            .majorRadius(majorRadius)
            .minorRadius(minorRadius)
            .majorSegments(majorSegments)
            .minorSegments(minorSegments)
            .arcStart(arcStart)
            .arcEnd(arcEnd);
    }
    
    public static class Builder {
        private float majorRadius = 1.0f;
        private float minorRadius = 0.25f;
        private int majorSegments = 32;
        private int minorSegments = 16;
        private float arcStart = 0;
        private float arcEnd = 360;
        
        public Builder majorRadius(float r) { this.majorRadius = r; return this; }
        public Builder minorRadius(float r) { this.minorRadius = r; return this; }
        public Builder majorSegments(int s) { this.majorSegments = s; return this; }
        public Builder minorSegments(int s) { this.minorSegments = s; return this; }
        public Builder arcStart(float a) { this.arcStart = a; return this; }
        public Builder arcEnd(float a) { this.arcEnd = a; return this; }
        
        public TorusShape build() {
            return new TorusShape(majorRadius, minorRadius, majorSegments, minorSegments, arcStart, arcEnd);
        }
    }
}


