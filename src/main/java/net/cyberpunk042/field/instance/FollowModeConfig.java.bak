package net.cyberpunk042.field.instance;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for how a personal field follows its owner.
 * 
 * <h2>JSON Format</h2>
 * <pre>
 * "followMode": {
 *   "enabled": true,
 *   "mode": "SMOOTH",
 *   "playerOverride": true
 * }
 * </pre>
 * 
 * <p>When enabled=false, the field is static at spawn position.</p>
 * 
 * @see FollowMode
 * @see PersonalFieldInstance
 */
public record FollowModeConfig(
    boolean enabled,
    FollowMode mode,
    boolean playerOverride
) {
    /** Static field (doesn't follow player). */
    public static final FollowModeConfig STATIC = new FollowModeConfig(false, FollowMode.SNAP, false);
    
    /** Default following (smooth, player can change). */
    public static final FollowModeConfig DEFAULT = new FollowModeConfig(true, FollowMode.SMOOTH, true);
    
    /** Snap following (instant, no lag). */
    public static final FollowModeConfig SNAP = new FollowModeConfig(true, FollowMode.SNAP, true);
    
    /** Glide following (slow, floaty). */
    public static final FollowModeConfig GLIDE = new FollowModeConfig(true, FollowMode.GLIDE, true);
    
    /**
     * Creates a follow config with default player override.
     * @param mode Follow mode
     */
    public static FollowModeConfig of(FollowMode mode) {
        return new FollowModeConfig(true, mode, true);
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a FollowModeConfig from JSON.
     * @param json The JSON object
     * @return Parsed config or null if json is null
     */
    @Nullable
    public static FollowModeConfig fromJson(JsonObject json) {
        if (json == null) return null;
        
        Logging.FIELD.topic("parse").trace("Parsing FollowModeConfig...");
        
        boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;
        FollowMode mode = FollowMode.SMOOTH;
        if (json.has("mode")) {
            String modeStr = json.get("mode").getAsString();
            mode = FollowMode.fromId(modeStr);
            if (mode == null) {
                Logging.FIELD.topic("parse").warn("Unknown followMode '{}', using SMOOTH", modeStr);
                mode = FollowMode.SMOOTH;
            }
        }
        boolean playerOverride = json.has("playerOverride") ? json.get("playerOverride").getAsBoolean() : true;
        
        FollowModeConfig result = new FollowModeConfig(enabled, mode, playerOverride);
        Logging.FIELD.topic("parse").trace("Parsed FollowModeConfig: enabled={}, mode={}, playerOverride={}", 
            enabled, mode, playerOverride);
        return result;
    }
    
    // =========================================================================
    // Builder
    // =========================================================================
    
    public static Builder builder() { return new Builder(); }
    /**
     * Serializes this follow mode config to JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("mode", mode.id());
        json.addProperty("playerOverride", playerOverride);
        return json;
    }


    
    public static class Builder {
        private boolean enabled = true;
        private FollowMode mode = FollowMode.SMOOTH;
        private boolean playerOverride = true;
        
        public Builder enabled(boolean e) { this.enabled = e; return this; }
        public Builder mode(FollowMode m) { this.mode = m; return this; }
        public Builder playerOverride(boolean p) { this.playerOverride = p; return this; }
        
        public FollowModeConfig build() {
            return new FollowModeConfig(enabled, mode, playerOverride);
        }
    }
}
