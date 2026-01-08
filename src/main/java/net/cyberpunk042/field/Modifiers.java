package net.cyberpunk042.field;

import net.cyberpunk042.log.Logging;

import com.google.gson.JsonObject;
import net.cyberpunk042.util.json.JsonField;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.cyberpunk042.util.json.JsonSerializer;


/**
 * Modifiers that affect field behavior and appearance.
 * 
 * <h2>Multipliers</h2>
 * <ul>
 *   <li><b>radiusMultiplier</b>: Scales the base radius</li>
 *   <li><b>strengthMultiplier</b>: Scales effect strength</li>
 *   <li><b>alphaMultiplier</b>: Scales visual opacity</li>
 *   <li><b>spinMultiplier</b>: Scales rotation speed</li>
 * </ul>
 * 
 * <h2>Visual Modifiers</h2>
 * <ul>
 *   <li><b>visualScale</b>: Global render scale (different from radius)</li>
 *   <li><b>tiltMultiplier</b>: Movement-based tilt intensity</li>
 *   <li><b>swirlStrength</b>: Surface distortion/swirl effect</li>
 * </ul>
 * 
 * <h2>Animation Modifiers</h2>
 * <ul>
 *   <li><b>bobbing</b>: Vertical oscillation strength (0-1)</li>
 *   <li><b>breathing</b>: Scale breathing strength (0-1)</li>
 * </ul>
 * 
 * <h2>Flags</h2>
 * <ul>
 *   <li><b>inverted</b>: Inverts push/pull effects</li>
 *   <li><b>pulsing</b>: Enables pulse animation</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * Modifiers mods = Modifiers.builder()
 *     .radiusMultiplier(1.5f)
 *     .visualScale(1.2f)
 *     .swirlStrength(0.5f)
 *     .pulsing(true)
 *     .build();
 * </pre>
 */
public record Modifiers(
    // Multipliers
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "1.0") float radiusMultiplier,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "1.0") float strengthMultiplier,
    @Range(ValueRange.ALPHA) @JsonField(skipIfDefault = true, defaultValue = "1.0") float alphaMultiplier,
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "1.0") float spinMultiplier,
    // Visual modifiers
    @Range(ValueRange.POSITIVE) @JsonField(skipIfDefault = true, defaultValue = "1.0") float visualScale,
    @Range(ValueRange.ALPHA) @JsonField(skipIfDefault = true) float tiltMultiplier,
    @Range(ValueRange.ALPHA) @JsonField(skipIfDefault = true) float swirlStrength,
    // Animation modifiers
    @Range(ValueRange.ALPHA) @JsonField(skipIfDefault = true) float bobbing,
    // Vertical oscillation strength (0-1)
    @Range(ValueRange.ALPHA) @JsonField(skipIfDefault = true) float breathing,
    // Scale breathing strength (0-1)
    
    // Flags
    @JsonField(skipIfDefault = true) boolean inverted,
    @JsonField(skipIfDefault = true) boolean pulsing
){
    
    /**
     * Default modifiers (no changes).
     */
    public static final Modifiers DEFAULT = new Modifiers(
        1.0f, 1.0f, 1.0f, 1.0f,  // multipliers
        1.0f, 0.0f, 0.0f,         // visual
        0.0f, 0.0f,               // animation (bobbing, breathing)
        false, false              // flags
    );
    
    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods
    // ─────────────────────────────────────────────────────────────────────────
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a builder pre-populated with this record's values.
     */
    public Builder toBuilder() {
        return new Builder()
            .radiusMultiplier(radiusMultiplier)
            .strengthMultiplier(strengthMultiplier)
            .alphaMultiplier(alphaMultiplier)
            .spinMultiplier(spinMultiplier)
            .visualScale(visualScale)
            .tiltMultiplier(tiltMultiplier)
            .swirlStrength(swirlStrength)
            .bobbing(bobbing)
            .breathing(breathing)
            .inverted(inverted)
            .pulsing(pulsing);
    }
    
    /**
     * Creates modifiers with just radius scaling.
     */
    public static Modifiers withRadius(float multiplier) {
        return builder().radiusMultiplier(multiplier).build();
    }
    
    /**
     * Creates modifiers with just strength scaling.
     */
    public static Modifiers withStrength(float multiplier) {
        return builder().strengthMultiplier(multiplier).build();
    }
    
    /**
     * Creates modifiers with visual effects.
     */
    public static Modifiers visual(float scale, float swirl) {
        return builder().visualScale(scale).swirlStrength(swirl).build();
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Computed Values
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Applies radius modifier to a base value.
     */
    public float applyRadius(float base) {
        return base * radiusMultiplier;
    }
    
    /**
     * Applies strength modifier to a base value.
     */
    public float applyStrength(float base) {
        float result = base * strengthMultiplier;
        return inverted ? -result : result;
    }
    
    /**
     * Applies alpha modifier to a base value.
     */
    public float applyAlpha(float base) {
        return Math.min(1.0f, base * alphaMultiplier);
    }
    
    /**
     * Applies spin modifier to a base value.
     */
    public float applySpin(float base) {
        return base * spinMultiplier;
    }
    
    /**
     * Applies visual scale to a render size.
     */
    public float applyVisualScale(float base) {
        return base * visualScale;
    }
    
    /**
     * Checks if any visual modifiers are active.
     */
    public boolean hasVisualModifiers() {
        return visualScale != 1.0f || tiltMultiplier != 0.0f || swirlStrength != 0.0f;
    }
    
    /**
     * Checks if any animation modifiers are active.
     */
    public boolean hasAnimationModifiers() {
        return bobbing != 0.0f || breathing != 0.0f;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Combining
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Combines this with another modifiers (multiplicative for multipliers, additive for visual).
     */
    public Modifiers combine(Modifiers other) {
        return new Modifiers(
            radiusMultiplier * other.radiusMultiplier,
            strengthMultiplier * other.strengthMultiplier,
            alphaMultiplier * other.alphaMultiplier,
            spinMultiplier * other.spinMultiplier,
            visualScale * other.visualScale,
            tiltMultiplier + other.tiltMultiplier,
            swirlStrength + other.swirlStrength,
            Math.max(bobbing, other.bobbing),      // Use stronger animation
            Math.max(breathing, other.breathing),  // Use stronger animation
            inverted ^ other.inverted,  // XOR for inversion
            pulsing || other.pulsing
        );
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Serialization
    // ─────────────────────────────────────────────────────────────────────────
    
    public JsonObject toJson() {
        return JsonSerializer.toJson(this);
    }
    
    public static Modifiers fromJson(JsonObject json) {
        Modifiers mods = builder()
            .radiusMultiplier(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
            .strengthMultiplier(json.has("strength") ? json.get("strength").getAsFloat() : 1.0f)
            .alphaMultiplier(json.has("alpha") ? json.get("alpha").getAsFloat() : 1.0f)
            .spinMultiplier(json.has("spin") ? json.get("spin").getAsFloat() : 1.0f)
            .visualScale(json.has("visualScale") ? json.get("visualScale").getAsFloat() : 1.0f)
            .tiltMultiplier(json.has("tilt") ? json.get("tilt").getAsFloat() : 0.0f)
            .swirlStrength(json.has("swirl") ? json.get("swirl").getAsFloat() : 0.0f)
            .bobbing(json.has("bobbing") ? json.get("bobbing").getAsFloat() : 0.0f)
            .breathing(json.has("breathing") ? json.get("breathing").getAsFloat() : 0.0f)
            .inverted(json.has("inverted") && json.get("inverted").getAsBoolean())
            .pulsing(json.has("pulsing") && json.get("pulsing").getAsBoolean())
            .build();
        
        if (mods.hasVisualModifiers() || mods.hasAnimationModifiers()) {
            Logging.REGISTRY.topic("modifiers").debug(
                "Parsed modifiers: scale={:.2f}, tilt={:.2f}, swirl={:.2f}, bobbing={:.2f}, breathing={:.2f}",
                mods.visualScale(), mods.tiltMultiplier(), mods.swirlStrength(), 
                mods.bobbing(), mods.breathing());
        }
        
        return mods;
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Builder
    // ─────────────────────────────────────────────────────────────────────────
    
    public static final class Builder {
        private float radiusMultiplier = 1.0f;
        private float strengthMultiplier = 1.0f;
        private float alphaMultiplier = 1.0f;
        private float spinMultiplier = 1.0f;
        private float visualScale = 1.0f;
        private float tiltMultiplier = 0.0f;
        private float swirlStrength = 0.0f;
        private float bobbing = 0.0f;
        private float breathing = 0.0f;
        private boolean inverted = false;
        private boolean pulsing = false;
        
        public Builder radiusMultiplier(float v) { this.radiusMultiplier = v; return this; }
        public Builder strengthMultiplier(float v) { this.strengthMultiplier = v; return this; }
        public Builder alphaMultiplier(float v) { this.alphaMultiplier = v; return this; }
        public Builder spinMultiplier(float v) { this.spinMultiplier = v; return this; }
        public Builder visualScale(float v) { this.visualScale = v; return this; }
        public Builder tiltMultiplier(float v) { this.tiltMultiplier = v; return this; }
        public Builder swirlStrength(float v) { this.swirlStrength = v; return this; }
        public Builder bobbing(float v) { this.bobbing = v; return this; }
        public Builder breathing(float v) { this.breathing = v; return this; }
        public Builder inverted(boolean v) { this.inverted = v; return this; }
        public Builder pulsing(boolean v) { this.pulsing = v; return this; }
        
        public Modifiers build() {
            return new Modifiers(
                radiusMultiplier, strengthMultiplier, alphaMultiplier, spinMultiplier,
                visualScale, tiltMultiplier, swirlStrength,
                bobbing, breathing,
                inverted, pulsing
            );
        }
    }
}
