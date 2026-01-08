package net.cyberpunk042.field.primitive;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Axis;
import org.jetbrains.annotations.Nullable;

/**
 * Links a primitive to another primitive for coordinated behavior.
 * 
 * <p>Per ARCHITECTURE §9: Primitives within a layer can be linked
 * for coordinated behavior like radius matching, position following,
 * mirroring, and animation phase offset.
 * 
 * <h2>Link Types</h2>
 * <table>
 *   <tr><th>Field</th><th>Description</th></tr>
 *   <tr><td>{@code radiusMatch}</td><td>Match another primitive's radius</td></tr>
 *   <tr><td>{@code radiusOffset}</td><td>Offset from matched radius</td></tr>
 *   <tr><td>{@code follow}</td><td>Follow another primitive's position</td></tr>
 *   <tr><td>{@code mirror}</td><td>Mirror on specified axis</td></tr>
 *   <tr><td>{@code phaseOffset}</td><td>Animation phase offset</td></tr>
 *   <tr><td>{@code scaleWith}</td><td>Scale proportionally with another</td></tr>
 * </table>
 * 
 * <h2>Cycle Prevention</h2>
 * <p>Links are resolved in primitive declaration order. A primitive can only
 * link to primitives declared BEFORE it. This prevents circular references.
 * 
 * @see Primitive
 * @see LinkResolver
 */
public record PrimitiveLink(
    @Nullable String radiusMatch,
    float radiusOffset,
    @Nullable String follow,
    @Nullable Axis mirror,
    float phaseOffset,
    @Nullable String scaleWith
) {
    
    /** No linking. */
    public static final PrimitiveLink NONE = new PrimitiveLink(null, 0, null, null, 0, null);
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /** Creates a radius match link. */
    public static PrimitiveLink radiusMatch(String targetId, float offset) {
        return new PrimitiveLink(targetId, offset, null, null, 0, null);
    }
    
    /** Creates a follow link. */
    public static PrimitiveLink follow(String targetId) {
        return new PrimitiveLink(null, 0, targetId, null, 0, null);
    }
    
    /** Creates a mirror link. */
    public static PrimitiveLink mirror(Axis axis) {
        return new PrimitiveLink(null, 0, null, axis, 0, null);
    }
    
    // =========================================================================
    // Query Methods
    // =========================================================================
    
    /** Checks if this link has any active references. */
    public boolean hasLinks() {
        return radiusMatch != null || follow != null || mirror != null 
            || scaleWith != null || phaseOffset != 0;
    }
    
    /** Returns all referenced primitive IDs. */
    public java.util.Set<String> getReferencedIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        if (radiusMatch != null) ids.add(radiusMatch);
        if (follow != null) ids.add(follow);
        if (scaleWith != null) ids.add(scaleWith);
        return ids;
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    public static PrimitiveLink fromJson(JsonObject json) {
        if (json == null || json.size() == 0) return NONE;
        
        String radiusMatch = json.has("radiusMatch") ? json.get("radiusMatch").getAsString() : null;
        float radiusOffset = json.has("radiusOffset") ? json.get("radiusOffset").getAsFloat() : 0;
        String follow = json.has("follow") ? json.get("follow").getAsString() : null;
        String scaleWith = json.has("scaleWith") ? json.get("scaleWith").getAsString() : null;
        float phaseOffset = json.has("phaseOffset") ? json.get("phaseOffset").getAsFloat() : 0;
        
        Axis mirror = null;
        if (json.has("mirror")) {
            try {
                mirror = Axis.valueOf(json.get("mirror").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                Logging.FIELD.topic("link").warn("Unknown mirror axis: {}", json.get("mirror"));
            }
        }
        
        return new PrimitiveLink(radiusMatch, radiusOffset, follow, mirror, phaseOffset, scaleWith);
    }
    
    /**
     * Serializes this link to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        
        if (radiusMatch != null) {
            json.addProperty("radiusMatch", radiusMatch);
        }
        if (radiusOffset != 0) {
            json.addProperty("radiusOffset", radiusOffset);
        }
        if (follow != null) {
            json.addProperty("follow", follow);
        }
        if (mirror != null) {
            json.addProperty("mirror", mirror.name());
        }
        if (phaseOffset != 0) {
            json.addProperty("phaseOffset", phaseOffset);
        }
        if (scaleWith != null) {
            json.addProperty("scaleWith", scaleWith);
        }
        
        return json;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private String radiusMatch = null;
        private float radiusOffset = 0;
        private String follow = null;
        private Axis mirror = null;
        private float phaseOffset = 0;
        private String scaleWith = null;
        
        public Builder radiusMatch(String id) { this.radiusMatch = id; return this; }
        public Builder radiusOffset(float offset) { this.radiusOffset = offset; return this; }
        public Builder follow(String id) { this.follow = id; return this; }
        public Builder mirror(Axis axis) { this.mirror = axis; return this; }
        public Builder phaseOffset(float offset) { this.phaseOffset = offset; return this; }
        public Builder scaleWith(String id) { this.scaleWith = id; return this; }
        
        public PrimitiveLink build() {
            return new PrimitiveLink(radiusMatch, radiusOffset, follow, mirror, phaseOffset, scaleWith);
        }
    }
}
