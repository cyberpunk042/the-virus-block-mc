package net.cyberpunk042.field.force;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.util.json.JsonSerializer;

/**
 * Force field configuration with void tear physics support.
 * 
 * <h2>Attractor Modes</h2>
 * <ul>
 *   <li><b>CENTER</b>: Point attractor - entities slingshot through center (coreRadius > 0)</li>
 *   <li><b>SURFACE</b>: Shell attractor - entities orbit at orbitRadius (coreRadius = 0)</li>
 * </ul>
 * 
 * <h2>Distance-Based Zones</h2>
 * <ul>
 *   <li><b>FAR</b>: Beyond orbitRadius → Strong pull toward target</li>
 *   <li><b>ORBIT</b>: Between core and orbit → Tangential force for orbit</li>
 *   <li><b>CORE</b>: Inside coreRadius → Slingshot deflection</li>
 * </ul>
 */
public record ForceFieldConfig(
    // Pull strength
    @JsonField(skipIfDefault = true, defaultValue = "0.2") float strength,
    
    // Zone radii (as fractions of total radius)
    @JsonField(skipIfDefault = true, defaultValue = "15.0") float radius,
    @JsonField(skipIfDefault = true, defaultValue = "0.4") float orbitRadius,  // 40% of radius
    @JsonField(skipIfDefault = true, defaultValue = "0.15") float coreRadius,  // 15% = CENTER mode, 0 = SURFACE mode
    
    // Tangential/orbital force
    @JsonField(skipIfDefault = true, defaultValue = "0.1") float tangentialStrength,
    @JsonField(skipIfDefault = true, defaultValue = "1.5") float slingshotFactor,
    
    // Constraints
    @JsonField(skipIfDefault = true, defaultValue = "1.5") float maxVelocity,
    @JsonField(skipIfDefault = true, defaultValue = "0.02") float damping,
    @JsonField(skipIfDefault = true, defaultValue = "0.05") float groundLift,
    
    // Falloff
    @JsonField(skipIfDefault = true, defaultValue = "linear") String falloff,
    
    // Orbit Plane Rotation - makes the orbit tilt/spin in 3D
    // Modes: "fixed" (no rotation), "wobble" (oscillates back and forth), "fullcycle" (continuous rotation)
    @JsonField(skipIfDefault = true, defaultValue = "fixed") String tiltMode,
    @JsonField(skipIfDefault = true, defaultValue = "0.0") float tiltRate,
    @JsonField(skipIfDefault = true, defaultValue = "fixed") String orientationMode,
    @JsonField(skipIfDefault = true, defaultValue = "0.0") float orientationRate,
    
    // Motion Law System toggle
    @JsonField(skipIfDefault = true, defaultValue = "false") boolean useMotionLaw,
    @JsonField(skipIfDefault = true, defaultValue = "") String motionLawPreset
) {
    
    /** Default configuration - CENTER mode with slingshot. */
    public static final ForceFieldConfig DEFAULT = new ForceFieldConfig(
        0.2f,    // strength
        15f,     // radius
        0.4f,    // orbitRadius (40%)
        0.15f,   // coreRadius (15%) - CENTER mode
        0.1f,    // tangentialStrength
        1.5f,    // slingshotFactor
        1.5f,    // maxVelocity
        0.02f,   // damping
        0.05f,   // groundLift
        "linear", // falloff
        "fixed", // tiltMode
        0.0f,    // tiltRate
        "fixed", // orientationMode
        0.0f,    // orientationRate
        false,   // useMotionLaw
        ""       // motionLawPreset
    );
    
    /**
     * Compact constructor with validation.
     */
    public ForceFieldConfig {
        strength = Math.max(0.01f, strength);
        radius = Math.max(1f, radius);
        orbitRadius = Math.max(0.1f, Math.min(0.8f, orbitRadius));
        coreRadius = Math.max(0f, Math.min(0.5f, coreRadius));
        tangentialStrength = Math.max(0f, tangentialStrength);
        slingshotFactor = Math.max(0f, slingshotFactor);
        maxVelocity = Math.max(0.1f, maxVelocity);
        damping = Math.max(0f, Math.min(1f, damping));
        groundLift = Math.max(0f, groundLift);
        if (falloff == null || falloff.isBlank()) falloff = "linear";
        if (tiltMode == null || tiltMode.isBlank()) tiltMode = "fixed";
        tiltRate = Math.max(0f, Math.min(0.2f, tiltRate));
        if (orientationMode == null || orientationMode.isBlank()) orientationMode = "fixed";
        orientationRate = Math.max(0f, Math.min(0.2f, orientationRate));
        if (motionLawPreset == null) motionLawPreset = "";
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Computed Properties
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Returns the maximum effect radius. */
    public float maxRadius() {
        return radius;
    }
    
    /** Returns absolute orbit radius in world units. */
    public float absoluteOrbitRadius() {
        return radius * orbitRadius;
    }
    
    /** Returns absolute core radius in world units. */
    public float absoluteCoreRadius() {
        return radius * coreRadius;
    }
    
    /** Returns true if this is SURFACE mode (no core slingshot). */
    public boolean isSurfaceMode() {
        return coreRadius < 0.01f;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static ForceFieldConfig fromJson(JsonObject json) {
        if (json == null) return DEFAULT;
        
        return new ForceFieldConfig(
            json.has("strength") ? json.get("strength").getAsFloat() : 0.2f,
            json.has("radius") ? json.get("radius").getAsFloat() : 15f,
            json.has("orbitRadius") ? json.get("orbitRadius").getAsFloat() : 0.4f,
            json.has("coreRadius") ? json.get("coreRadius").getAsFloat() : 0.15f,
            json.has("tangentialStrength") ? json.get("tangentialStrength").getAsFloat() : 0.1f,
            json.has("slingshotFactor") ? json.get("slingshotFactor").getAsFloat() : 1.5f,
            json.has("maxVelocity") ? json.get("maxVelocity").getAsFloat() : 1.5f,
            json.has("damping") ? json.get("damping").getAsFloat() : 0.02f,
            json.has("groundLift") ? json.get("groundLift").getAsFloat() : 0.05f,
            json.has("falloff") ? json.get("falloff").getAsString() : "linear",
            json.has("tiltMode") ? json.get("tiltMode").getAsString() : "fixed",
            json.has("tiltRate") ? json.get("tiltRate").getAsFloat() : 0.0f,
            json.has("orientationMode") ? json.get("orientationMode").getAsString() : "fixed",
            json.has("orientationRate") ? json.get("orientationRate").getAsFloat() : 0.0f,
            json.has("useMotionLaw") ? json.get("useMotionLaw").getAsBoolean() : false,
            json.has("motionLawPreset") ? json.get("motionLawPreset").getAsString() : ""
        );
    }
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private float strength = 0.2f;
        private float radius = 15f;
        private float orbitRadius = 0.4f;
        private float coreRadius = 0.15f;
        private float tangentialStrength = 0.1f;
        private float slingshotFactor = 1.5f;
        private float maxVelocity = 1.5f;
        private float damping = 0.02f;
        private float groundLift = 0.05f;
        private String falloff = "linear";
        private String tiltMode = "fixed";
        private float tiltRate = 0.0f;
        private String orientationMode = "fixed";
        private float orientationRate = 0.0f;
        private boolean useMotionLaw = false;
        private String motionLawPreset = "";
        
        public Builder strength(float v) { this.strength = v; return this; }
        public Builder radius(float v) { this.radius = v; return this; }
        public Builder orbitRadius(float v) { this.orbitRadius = v; return this; }
        public Builder coreRadius(float v) { this.coreRadius = v; return this; }
        public Builder tangentialStrength(float v) { this.tangentialStrength = v; return this; }
        public Builder slingshotFactor(float v) { this.slingshotFactor = v; return this; }
        public Builder maxVelocity(float v) { this.maxVelocity = v; return this; }
        public Builder damping(float v) { this.damping = v; return this; }
        public Builder groundLift(float v) { this.groundLift = v; return this; }
        public Builder falloff(String v) { this.falloff = v; return this; }
        public Builder tiltMode(String v) { this.tiltMode = v; return this; }
        public Builder tiltRate(float v) { this.tiltRate = v; return this; }
        public Builder orientationMode(String v) { this.orientationMode = v; return this; }
        public Builder orientationRate(float v) { this.orientationRate = v; return this; }
        public Builder useMotionLaw(boolean v) { this.useMotionLaw = v; return this; }
        public Builder motionLawPreset(String v) { this.motionLawPreset = v; return this; }
        
        /** Preset: CENTER mode for void tear slingshot effect. */
        public Builder centerMode() {
            this.coreRadius = 0.15f;
            return this;
        }
        
        /** Preset: SURFACE mode for shell orbit effect. */
        public Builder surfaceMode() {
            this.coreRadius = 0f;
            return this;
        }
        
        /** Preset: Motion law mode with specified preset. */
        public Builder motionLaw(String preset) {
            this.useMotionLaw = true;
            this.motionLawPreset = preset;
            return this;
        }
        
        /** Preset: 3D orbit rotation effect. */
        public Builder rotating3D(float tiltRate, float orientationRate) {
            this.tiltMode = "fullcycle";
            this.tiltRate = tiltRate;
            this.orientationMode = "fullcycle";
            this.orientationRate = orientationRate;
            return this;
        }
        
        public ForceFieldConfig build() {
            return new ForceFieldConfig(strength, radius, orbitRadius, coreRadius,
                tangentialStrength, slingshotFactor, maxVelocity, damping, groundLift, falloff,
                tiltMode, tiltRate, orientationMode, orientationRate,
                useMotionLaw, motionLawPreset);
        }
    }
}
