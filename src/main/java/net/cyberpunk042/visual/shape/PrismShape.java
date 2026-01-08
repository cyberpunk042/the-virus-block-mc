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
 * N-sided prism shape (extruded polygon).
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "shape": {
 *   "type": "prism",
 *   "sides": 6,
 *   "radius": 1.0,
 *   "height": 2.0,
 *   "topRadius": 1.0,
 *   "twist": 0.0,
 *   "heightSegments": 1,
 *   "capTop": true,
 *   "capBottom": true
 * }
 * </pre>
 * 
 * <h2>Parts</h2>
 * <ul>
 *   <li><b>sides</b> (QUAD) - Side faces</li>
 *   <li><b>capTop</b> (SECTOR) - Top cap</li>
 *   <li><b>capBottom</b> (SECTOR) - Bottom cap</li>
 *   <li><b>edges</b> (EDGE) - Corner edges</li>
 * </ul>
 * 
 * @see Shape
 * @see CellType
 */
public record PrismShape(
    @Range(ValueRange.SIDES) int sides,
    @Range(ValueRange.RADIUS) float radius,
    @Range(ValueRange.POSITIVE_NONZERO) float height,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfEqualsField = "radius") float topRadius,
    @Range(ValueRange.DEGREES_FULL) @JsonField(skipIfDefault = true) float twist,
    @Range(ValueRange.STEPS) @JsonField(skipIfDefault = true, defaultValue = "1") int heightSegments,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean capTop,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean capBottom
)implements Shape {
    
    /** Default hexagonal prism. */
    public static final PrismShape DEFAULT = new PrismShape(
        6, 1.0f, 2.0f, 1.0f, 0, 1, true, true);
    
    /** Cube (4 sides). */
    public static final PrismShape CUBE = new PrismShape(
        4, 1.0f, 2.0f, 1.0f, 0, 1, true, true);
    
    /** Triangular prism. */
    public static final PrismShape TRIANGLE = new PrismShape(
        3, 1.0f, 2.0f, 1.0f, 0, 1, true, true);
    
    /** Octagonal prism. */
    public static final PrismShape OCTAGON = new PrismShape(
        8, 1.0f, 2.0f, 1.0f, 0, 1, true, true);
    
    /**
     * Creates a regular prism.
     * @param sides Number of sides
     * @param radius Radius
     * @param height Height
     */
    public static PrismShape of(@Range(ValueRange.SIDES) int sides, @Range(ValueRange.RADIUS) float radius, @Range(ValueRange.POSITIVE_NONZERO) float height) {
        return new PrismShape(sides, radius, height, radius, 0, 1, true, true);
    }
    
    /**
     * Creates a tapered prism (cone-like).
     * @param sides Number of sides
     * @param bottomRadius Bottom radius
     * @param topRadius Top radius
     * @param height Height
     */
    public static PrismShape tapered(@Range(ValueRange.SIDES) int sides, float bottomRadius, @Range(ValueRange.POSITIVE) float topRadius, @Range(ValueRange.POSITIVE_NONZERO) float height) {
        return new PrismShape(sides, bottomRadius, height, topRadius, 0, 1, true, true);
    }
    
    @Override
    public String getType() {
        return "prism";
    }
    
    @Override
    public Vector3f getBounds() {
        float maxR = Math.max(radius, topRadius);
        return new Vector3f(maxR * 2, height, maxR * 2);
    }
    
    @Override
    public CellType primaryCellType() {
        return CellType.QUAD;
    }
    
    @Override
    public Map<String, CellType> getParts() {
        return Map.of(
            "sides", CellType.QUAD,
            "capTop", CellType.SECTOR,
            "capBottom", CellType.SECTOR,
            "edges", CellType.EDGE
        );
    }
    
    /** Whether this is a regular prism (same top/bottom radius). */
    public boolean isRegular() {
        return radius == topRadius;
    }
    
    /** Whether this has twist (spiral). */
    public boolean hasTwist() {
        return twist != 0;
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
            .sides(sides)
            .radius(radius)
            .height(height)
            .topRadius(topRadius)
            .twist(twist)
            .heightSegments(heightSegments)
            .capTop(capTop)
            .capBottom(capBottom);
    }
    
    public static class Builder {
        private @Range(ValueRange.SIDES) int sides = 6;
        private @Range(ValueRange.RADIUS) float radius = 1.0f;
        private @Range(ValueRange.POSITIVE_NONZERO) float height = 2.0f;
        private @Range(ValueRange.POSITIVE) float topRadius = 1.0f;
        private @Range(ValueRange.DEGREES_FULL) float twist = 0;
        private @Range(ValueRange.STEPS) int heightSegments = 1;
        private boolean capTop = true;
        private boolean capBottom = true;
        
        public Builder sides(int s) { this.sides = s; return this; }
        public Builder radius(float r) { this.radius = r; return this; }
        public Builder height(float h) { this.height = h; return this; }
        public Builder topRadius(float r) { this.topRadius = r; return this; }
        public Builder twist(float t) { this.twist = t; return this; }
        public Builder heightSegments(int s) { this.heightSegments = s; return this; }
        public Builder capTop(boolean c) { this.capTop = c; return this; }
        public Builder capBottom(boolean c) { this.capBottom = c; return this; }
        
        public PrismShape build() {
            return new PrismShape(sides, radius, height, topRadius, twist, heightSegments, capTop, capBottom);
        }
    }
}
