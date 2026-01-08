package net.cyberpunk042.visual.animation;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;
import net.cyberpunk042.visual.energy.EnergyFlicker;
import net.cyberpunk042.visual.energy.EnergyTravel;
import net.cyberpunk042.visual.energy.TravelBlendMode;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;

/**
 * Configuration for ray FLOW animation timing and controls.
 * 
 * <p>This config controls WHEN and HOW FAST animations run,
 * NOT what they look like. Visual appearance is controlled by
 * {@code RadiativeInteraction} in {@code RaysShape}.</p>
 * 
 * <h2>Architecture (Energy Interaction Model)</h2>
 * <ul>
 *   <li><b>RaysShape.radiativeInteraction</b>: WHAT it looks like at phase X</li>
 *   <li><b>RayFlowConfig</b>: Enable/speed controls for WHEN phase changes</li>
 * </ul>
 * 
 * <h2>Animation Axes</h2>
 * <ul>
 *   <li><b>Radiative</b>: Enable/speed for EMISSION/ABSORPTION animation</li>
 *   <li><b>Travel</b>: Chase/scroll particles along the ray</li>
 *   <li><b>Flicker</b>: Random/rhythmic alpha overlay</li>
 * </ul>
 * 
 * @see net.cyberpunk042.visual.energy.RadiativeInteraction
 * @see net.cyberpunk042.visual.energy.EnergyTravel
 * @see net.cyberpunk042.visual.energy.EnergyFlicker
 * @see Animation
 */
public record RayFlowConfig(
    // === Radiative Animation (enable/speed only - mode is in RaysShape) ===
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean radiativeEnabled,
    @Range(ValueRange.UNBOUNDED) @JsonField(skipIfDefault = true, defaultValue = "1.0") float radiativeSpeed,
    
    // === Travel Animation ===
    @JsonField(skipIfDefault = true) EnergyTravel travel,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean travelEnabled,
    @Range(ValueRange.UNBOUNDED) @JsonField(skipIfDefault = true, defaultValue = "1.0") float travelSpeed,
    @Range(ValueRange.STEPS) @JsonField(skipIfDefault = true, defaultValue = "1") int chaseCount,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "0.3") float chaseWidth,
    @JsonField(skipIfDefault = true) TravelBlendMode travelBlendMode,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "0.0") float travelMinAlpha,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true, defaultValue = "1.0") float travelIntensity,
    
    // === Flicker Animation ===
    @JsonField(skipIfDefault = true) EnergyFlicker flicker,
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean flickerEnabled,
    @Range(ValueRange.NORMALIZED) @JsonField(skipIfDefault = true) float flickerIntensity,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "5.0") float flickerFrequency,
    
    // === Spawn Transition ===
    @JsonField(skipIfDefault = true, defaultValue = "true") boolean skipSpawnTransition,
    
    // === Path Following ===
    @JsonField(skipIfDefault = true, defaultValue = "false") boolean pathFollowing
) {
    // =========================================================================
    // Static Constants
    // =========================================================================
    
    /** No flow animation. */
    public static final RayFlowConfig NONE = new RayFlowConfig(
        false, 0f,                              // radiative disabled
        EnergyTravel.NONE, false, 0f, 1, 0.3f, TravelBlendMode.REPLACE, 0f, 1f,  // travel disabled
        EnergyFlicker.NONE, false, 0f, 5f,      // flicker disabled
        true, false
    );
    
    /** Default with radiative animation enabled. */
    public static final RayFlowConfig DEFAULT = new RayFlowConfig(
        true, 1f,                               // radiative enabled at speed 1
        EnergyTravel.NONE, false, 1f, 1, 0.3f, TravelBlendMode.REPLACE, 0f, 1f,  // travel disabled
        EnergyFlicker.NONE, false, 0f, 5f,      // flicker disabled
        true, false
    );
    
    /** Chase particles flowing along rays. */
    public static final RayFlowConfig CHASE = new RayFlowConfig(
        false, 0f,                              // radiative disabled
        EnergyTravel.CHASE, true, 1.5f, 3, 0.2f, TravelBlendMode.REPLACE, 0f, 1f, // chase enabled
        EnergyFlicker.NONE, false, 0f, 5f,
        true, false
    );
    
    /** Scrolling energy flow. */
    public static final RayFlowConfig SCROLL = new RayFlowConfig(
        false, 0f,
        EnergyTravel.SCROLL, true, 1f, 1, 0.3f, TravelBlendMode.REPLACE, 0f, 1f,
        EnergyFlicker.NONE, false, 0f, 5f,
        true, false
    );
    
    /** Twinkling stars effect. */
    public static final RayFlowConfig SCINTILLATE = new RayFlowConfig(
        false, 0f,
        EnergyTravel.NONE, false, 0f, 1, 0.3f, TravelBlendMode.REPLACE, 0f, 1f,
        EnergyFlicker.SCINTILLATION, true, 0.5f, 8f,
        true, false
    );
    
    // =========================================================================
    // Queries
    // =========================================================================
    
    /** Whether any flow animation is active. */
    public boolean isActive() {
        return (radiativeEnabled) || 
               (travelEnabled && travel != EnergyTravel.NONE) || 
               (flickerEnabled && flicker != EnergyFlicker.NONE);
    }
    
    /** Whether radiative animation is active. */
    public boolean hasRadiative() {
        return radiativeEnabled;
    }
    
    /** Whether travel animation is active. */
    public boolean hasTravel() {
        return travelEnabled && travel != null && travel != EnergyTravel.NONE;
    }
    
    /** Whether flicker animation is active. */
    public boolean hasFlicker() {
        return flickerEnabled && flicker != null && flicker != EnergyFlicker.NONE;
    }
    
    /** Gets effective travel mode, defaulting to NONE if not set. */
    public EnergyTravel effectiveTravel() {
        return travel != null ? travel : EnergyTravel.NONE;
    }
    
    /** Gets effective flicker mode, defaulting to NONE if not set. */
    public EnergyFlicker effectiveFlicker() {
        return flicker != null ? flicker : EnergyFlicker.NONE;
    }
    
    /** Gets effective travel blend mode, defaulting to REPLACE if not set. */
    public TravelBlendMode effectiveTravelBlendMode() {
        return travelBlendMode != null ? travelBlendMode : TravelBlendMode.REPLACE;
    }
    
    // =========================================================================
    // JSON Parsing
    // =========================================================================
    
    /**
     * Parses a RayFlowConfig from JSON.
     */
    public static RayFlowConfig fromJson(JsonObject json) {
        if (json == null) return NONE;
        
        // Radiative animation
        boolean radiativeEnabled = json.has("radiativeEnabled") ? json.get("radiativeEnabled").getAsBoolean() : true;
        float radiativeSpeed = json.has("radiativeSpeed") ? json.get("radiativeSpeed").getAsFloat() : 1f;
        
        // Legacy support: map old "length" field to radiativeEnabled
        if (json.has("length")) {
            String lengthMode = json.get("length").getAsString();
            radiativeEnabled = !"NONE".equalsIgnoreCase(lengthMode);
        }
        if (json.has("lengthSpeed")) {
            radiativeSpeed = json.get("lengthSpeed").getAsFloat();
        }
        
        // Travel animation
        EnergyTravel travel = EnergyTravel.NONE;
        if (json.has("travel")) {
            travel = EnergyTravel.fromString(json.get("travel").getAsString());
        }
        boolean travelEnabled = json.has("travelEnabled") ? json.get("travelEnabled").getAsBoolean() : travel.isActive();
        float travelSpeed = json.has("travelSpeed") ? json.get("travelSpeed").getAsFloat() : 1f;
        int chaseCount = json.has("chaseCount") ? json.get("chaseCount").getAsInt() : 1;
        float chaseWidth = json.has("chaseWidth") ? json.get("chaseWidth").getAsFloat() : 0.3f;
        
        // Travel blend mode
        TravelBlendMode travelBlendMode = TravelBlendMode.REPLACE;
        if (json.has("travelBlendMode")) {
            travelBlendMode = TravelBlendMode.fromString(json.get("travelBlendMode").getAsString());
        }
        float travelMinAlpha = json.has("travelMinAlpha") ? json.get("travelMinAlpha").getAsFloat() : 0f;
        float travelIntensity = json.has("travelIntensity") ? json.get("travelIntensity").getAsFloat() : 1f;
        
        // Flicker animation
        EnergyFlicker flicker = EnergyFlicker.NONE;
        if (json.has("flicker")) {
            flicker = EnergyFlicker.fromString(json.get("flicker").getAsString());
        }
        boolean flickerEnabled = json.has("flickerEnabled") ? json.get("flickerEnabled").getAsBoolean() : flicker.isActive();
        float flickerIntensity = json.has("flickerIntensity") ? json.get("flickerIntensity").getAsFloat() : 0.3f;
        float flickerFrequency = json.has("flickerFrequency") ? json.get("flickerFrequency").getAsFloat() : 5f;
        
        // Spawn/path
        boolean skipSpawnTransition = json.has("skipSpawnTransition") ? json.get("skipSpawnTransition").getAsBoolean() : 
            (json.has("startFullLength") ? json.get("startFullLength").getAsBoolean() : true);
        boolean pathFollowing = json.has("pathFollowing") ? json.get("pathFollowing").getAsBoolean() :
            (json.has("followCurve") ? json.get("followCurve").getAsBoolean() : false);
        
        return new RayFlowConfig(
            radiativeEnabled, radiativeSpeed,
            travel, travelEnabled, travelSpeed, chaseCount, chaseWidth, travelBlendMode, travelMinAlpha, travelIntensity,
            flicker, flickerEnabled, flickerIntensity, flickerFrequency,
            skipSpawnTransition, pathFollowing
        );
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
            .radiativeEnabled(radiativeEnabled).radiativeSpeed(radiativeSpeed)
            .travel(travel).travelEnabled(travelEnabled).travelSpeed(travelSpeed)
            .chaseCount(chaseCount).chaseWidth(chaseWidth).travelBlendMode(travelBlendMode)
            .travelMinAlpha(travelMinAlpha).travelIntensity(travelIntensity)
            .flicker(flicker).flickerEnabled(flickerEnabled)
            .flickerIntensity(flickerIntensity).flickerFrequency(flickerFrequency)
            .skipSpawnTransition(skipSpawnTransition).pathFollowing(pathFollowing);
    }
    
    public static class Builder {
        private boolean radiativeEnabled = false;
        private float radiativeSpeed = 1f;
        private EnergyTravel travel = EnergyTravel.NONE;
        private boolean travelEnabled = false;
        private float travelSpeed = 1f;
        private int chaseCount = 1;
        private float chaseWidth = 0.3f;
        private TravelBlendMode travelBlendMode = TravelBlendMode.REPLACE;
        private float travelMinAlpha = 0f;
        private float travelIntensity = 1f;
        private EnergyFlicker flicker = EnergyFlicker.NONE;
        private boolean flickerEnabled = false;
        private float flickerIntensity = 0.3f;
        private float flickerFrequency = 5f;
        private boolean skipSpawnTransition = true;
        private boolean pathFollowing = false;
        
        public Builder radiativeEnabled(boolean b) { this.radiativeEnabled = b; return this; }
        public Builder radiativeSpeed(float s) { this.radiativeSpeed = s; return this; }
        public Builder travel(EnergyTravel m) { this.travel = m != null ? m : EnergyTravel.NONE; return this; }
        public Builder travelEnabled(boolean b) { this.travelEnabled = b; return this; }
        public Builder travelSpeed(float s) { this.travelSpeed = s; return this; }
        public Builder chaseCount(int c) { this.chaseCount = c; return this; }
        public Builder chaseWidth(float w) { this.chaseWidth = w; return this; }
        public Builder travelBlendMode(TravelBlendMode m) { this.travelBlendMode = m != null ? m : TravelBlendMode.REPLACE; return this; }
        public Builder travelMinAlpha(float a) { this.travelMinAlpha = a; return this; }
        public Builder travelIntensity(float i) { this.travelIntensity = i; return this; }
        public Builder flicker(EnergyFlicker m) { this.flicker = m != null ? m : EnergyFlicker.NONE; return this; }
        public Builder flickerEnabled(boolean b) { this.flickerEnabled = b; return this; }
        public Builder flickerIntensity(float i) { this.flickerIntensity = i; return this; }
        public Builder flickerFrequency(float f) { this.flickerFrequency = f; return this; }
        public Builder skipSpawnTransition(boolean b) { this.skipSpawnTransition = b; return this; }
        public Builder pathFollowing(boolean b) { this.pathFollowing = b; return this; }
        
        public RayFlowConfig build() {
            return new RayFlowConfig(
                radiativeEnabled, radiativeSpeed,
                travel, travelEnabled, travelSpeed, chaseCount, chaseWidth, travelBlendMode, travelMinAlpha, travelIntensity,
                flicker, flickerEnabled, flickerIntensity, flickerFrequency,
                skipSpawnTransition, pathFollowing
            );
        }
    }
}
