package net.cyberpunk042.field.instance;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Unified follow configuration controlling how a field follows its owner.
 * 
 * <p>Replaces the old FollowMode enum and FollowModeConfig with a more
 * flexible system based on two primary parameters:</p>
 * 
 * <ul>
 *   <li><b>leadOffset</b>: Position offset in movement direction
 *       <ul>
 *         <li>-1.0 = trailing behind (floaty ghost)</li>
 *         <li>0.0 = locked to player (default)</li>
 *         <li>+1.0 = leading ahead (anticipating)</li>
 *       </ul>
 *   </li>
 *   <li><b>responsiveness</b>: How quickly field catches up
 *       <ul>
 *         <li>0.2 = very floaty/slow</li>
 *         <li>0.6 = smooth/organic</li>
 *         <li>1.0 = instant/snappy</li>
 *       </ul>
 *   </li>
 *   <li><b>lookAhead</b>: Offset toward look direction (optional enhancement)</li>
 * </ul>
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "follow": {
 *   "enabled": true,
 *   "leadOffset": 0.0,
 *   "responsiveness": 1.0,
 *   "lookAhead": 0.0
 * }
 * </pre>
 * 
 * <h2>Presets</h2>
 * <ul>
 *   <li>LOCKED: leadOffset=0, responsiveness=1.0 (perfect follow)</li>
 *   <li>SMOOTH: leadOffset=-0.1, responsiveness=0.6 (slight organic trail)</li>
 *   <li>GLIDE: leadOffset=-0.2, responsiveness=0.3 (floaty/ethereal)</li>
 *   <li>LEAD: leadOffset=+0.2, responsiveness=0.8 (anticipating)</li>
 * </ul>
 */
public record FollowConfig(
    boolean enabled,
    float leadOffset,      // -1.0 to +1.0
    float responsiveness,  // 0.1 to 1.0
    float lookAhead        // 0.0 to 1.0
) {
    
    // =========================================================================
    // Presets
    // =========================================================================
    
    /** Disabled - field is static at spawn position. */
    public static final FollowConfig DISABLED = new FollowConfig(false, 0f, 1f, 0f);
    
    /** Locked exactly to player position (default). */
    public static final FollowConfig LOCKED = new FollowConfig(true, 0f, 1f, 0f);
    
    /** Smooth organic follow with slight trail. */
    public static final FollowConfig SMOOTH = new FollowConfig(true, -0.1f, 0.6f, 0f);
    
    /** Floaty/ethereal follow with significant trail. */
    public static final FollowConfig GLIDE = new FollowConfig(true, -0.2f, 0.3f, 0f);
    
    /** Anticipating follow - slightly ahead of movement. */
    public static final FollowConfig LEAD = new FollowConfig(true, 0.15f, 0.8f, 0.05f);
    
    /** Default configuration. */
    public static final FollowConfig DEFAULT = LOCKED;
    
    // =========================================================================
    // Factory Methods
    // =========================================================================
    
    /**
     * Creates a simple follow config with just lead offset.
     */
    public static FollowConfig withLead(float leadOffset) {
        return new FollowConfig(true, leadOffset, 1.0f, 0f);
    }
    
    /**
     * Creates a trailing config (negative lead).
     */
    public static FollowConfig trailing(float amount, float responsiveness) {
        return new FollowConfig(true, -Math.abs(amount), responsiveness, 0f);
    }
    
    /**
     * Creates a leading config (positive lead).
     */
    public static FollowConfig leading(float amount, float responsiveness) {
        return new FollowConfig(true, Math.abs(amount), responsiveness, 0f);
    }
    
    // =========================================================================
    // Utility Methods
    // =========================================================================
    
    /** Whether this config is effectively active (enabled with non-zero effects). */
    public boolean isActive() {
        return enabled;
    }
    
    /** Whether field trails behind player. */
    public boolean isTrailing() {
        return leadOffset < -0.01f;
    }
    
    /** Whether field leads ahead of player. */
    public boolean isLeading() {
        return leadOffset > 0.01f;
    }
    
    /** Whether field is locked to player. */
    public boolean isLocked() {
        return Math.abs(leadOffset) <= 0.01f && responsiveness >= 0.99f;
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a FollowConfig from JSON.
     * Also supports legacy format with "mode" field.
     */
    public static FollowConfig fromJson(JsonObject json) {
        if (json == null) return DISABLED;
        
        Logging.FIELD.topic("parse").trace("Parsing FollowConfig...");
        
        boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;
        
        // New format
        if (json.has("leadOffset") || json.has("responsiveness")) {
            float leadOffset = json.has("leadOffset") ? json.get("leadOffset").getAsFloat() : 0f;
            float responsiveness = json.has("responsiveness") ? json.get("responsiveness").getAsFloat() : 1f;
            float lookAhead = json.has("lookAhead") ? json.get("lookAhead").getAsFloat() : 0f;
            
            return new FollowConfig(enabled, leadOffset, responsiveness, lookAhead);
        }
        
        // Legacy format - convert mode to new values
        if (json.has("mode")) {
            String modeStr = json.get("mode").getAsString().toUpperCase();
            return switch (modeStr) {
                case "SNAP" -> enabled ? LOCKED : DISABLED;
                case "SMOOTH" -> enabled ? SMOOTH : DISABLED;
                case "GLIDE" -> enabled ? GLIDE : DISABLED;
                default -> enabled ? LOCKED : DISABLED;
            };
        }
        
        return enabled ? LOCKED : DISABLED;
    }
    
    /**
     * Serializes this config to JSON.
     */
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    
    public Builder toBuilder() {
        return new Builder()
            .enabled(enabled)
            .leadOffset(leadOffset)
            .responsiveness(responsiveness)
            .lookAhead(lookAhead);
    }
    
    public static class Builder {
        private boolean enabled = true;
        private float leadOffset = 0f;
        private float responsiveness = 1f;
        private float lookAhead = 0f;
        
        public Builder enabled(boolean e) { this.enabled = e; return this; }
        public Builder leadOffset(float l) { this.leadOffset = Math.max(-1f, Math.min(1f, l)); return this; }
        public Builder responsiveness(float r) { this.responsiveness = Math.max(0.1f, Math.min(1f, r)); return this; }
        public Builder lookAhead(float l) { this.lookAhead = Math.max(0f, Math.min(1f, l)); return this; }
        
        public FollowConfig build() {
            return new FollowConfig(enabled, leadOffset, responsiveness, lookAhead);
        }
    }
    
    // =========================================================================
    // Preset Enum (for GUI dropdowns)
    // =========================================================================
    
    /**
     * Named presets for quick selection in GUI.
     */
    public enum Preset {
        LOCKED("Locked", "Perfect follow", FollowConfig.LOCKED),
        SMOOTH("Smooth", "Slight trail", FollowConfig.SMOOTH),
        GLIDE("Glide", "Floaty/ethereal", FollowConfig.GLIDE),
        LEAD("Lead", "Anticipating", FollowConfig.LEAD),
        CUSTOM("Custom", "Manual values", null);
        
        private final String label;
        private final String description;
        private final FollowConfig config;
        
        Preset(String label, String description, FollowConfig config) {
            this.label = label;
            this.description = description;
            this.config = config;
        }
        
        public String label() { return label; }
        public String description() { return description; }
        public FollowConfig config() { return config; }
        
        /** Get preset that matches a config, or CUSTOM if no match. */
        public static Preset fromConfig(FollowConfig cfg) {
            if (cfg == null || !cfg.enabled) return LOCKED;
            for (Preset p : values()) {
                if (p.config != null && 
                    Math.abs(p.config.leadOffset - cfg.leadOffset) < 0.01f &&
                    Math.abs(p.config.responsiveness - cfg.responsiveness) < 0.01f) {
                    return p;
                }
            }
            return CUSTOM;
        }
    }
}
