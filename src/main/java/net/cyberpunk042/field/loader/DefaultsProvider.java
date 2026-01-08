package net.cyberpunk042.field.loader;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.animation.SpinConfig;
import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.appearance.AlphaRange;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.shape.*;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.VisibilityMask;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides smart defaults for field components.
 * 
 * <p>Per CLASS_DIAGRAM §13, §15:
 * <ul>
 *   <li>Shape defaults vary by type</li>
 *   <li>Other defaults are consistent across primitives</li>
 *   <li>Supports JsonObject-based defaults for JSON parsing</li>
 * </ul>
 * 
 * @see FieldLoader
 * @see ReferenceResolver
 */
public class DefaultsProvider {
    
    // =========================================================================
    // Shape Defaults (per CLASS_DIAGRAM §13)
    // =========================================================================
    
    private static final Map<String, JsonObject> SHAPE_DEFAULTS_JSON = new HashMap<>();
    private static final JsonObject TRANSFORM_DEFAULTS_JSON = new JsonObject();
    private static final JsonObject FILL_DEFAULTS_JSON = new JsonObject();
    private static final JsonObject APPEARANCE_DEFAULTS_JSON = new JsonObject();
    private static final JsonObject ANIMATION_DEFAULTS_JSON = new JsonObject();
    
    static {
        // Shape defaults per CLASS_DIAGRAM §13
        JsonObject sphere = new JsonObject();
        sphere.addProperty("radius", 1.0f);
        sphere.addProperty("latSteps", 32);
        sphere.addProperty("lonSteps", 64);
        sphere.addProperty("algorithm", "LAT_LON");
        SHAPE_DEFAULTS_JSON.put("sphere", sphere);
        
        JsonObject ring = new JsonObject();
        ring.addProperty("innerRadius", 0.8f);
        ring.addProperty("outerRadius", 1.0f);
        ring.addProperty("segments", 64);
        ring.addProperty("y", 0f);
        SHAPE_DEFAULTS_JSON.put("ring", ring);
        
        JsonObject prism = new JsonObject();
        prism.addProperty("sides", 6);
        prism.addProperty("radius", 1.0f);
        prism.addProperty("height", 1.0f);
        SHAPE_DEFAULTS_JSON.put("prism", prism);
        
        JsonObject polyhedron = new JsonObject();
        polyhedron.addProperty("polyType", "CUBE");
        polyhedron.addProperty("radius", 1.0f);
        SHAPE_DEFAULTS_JSON.put("polyhedron", polyhedron);
        
        JsonObject cylinder = new JsonObject();
        cylinder.addProperty("radius", 0.5f);
        cylinder.addProperty("height", 10.0f);
        cylinder.addProperty("segments", 16);
        SHAPE_DEFAULTS_JSON.put("cylinder", cylinder);
        
        JsonObject jet = new JsonObject();
        jet.addProperty("length", 2.0f);
        jet.addProperty("baseRadius", 0.05f);
        jet.addProperty("topTipRadius", 0.15f);
        jet.addProperty("bottomTipRadius", 0.15f);
        jet.addProperty("segments", 16);
        jet.addProperty("lengthSegments", 4);
        jet.addProperty("dualJets", true);
        jet.addProperty("gap", 0.1f);
        jet.addProperty("capTip", true);
        SHAPE_DEFAULTS_JSON.put("jet", jet);
        
        JsonObject molecule = new JsonObject();
        molecule.addProperty("atomCount", 4);
        molecule.addProperty("atomRadius", 0.3f);
        molecule.addProperty("atomDistance", 0.8f);
        molecule.addProperty("neckRadius", 0.12f);
        molecule.addProperty("neckPinch", 0.5f);
        molecule.addProperty("connectionDistance", 1.2f);
        molecule.addProperty("seed", 42);
        molecule.addProperty("distribution", "FIBONACCI");
        SHAPE_DEFAULTS_JSON.put("molecule", molecule);
        
        // Transform defaults
        TRANSFORM_DEFAULTS_JSON.addProperty("anchor", "CENTER");
        TRANSFORM_DEFAULTS_JSON.addProperty("scale", 1.0f);
        TRANSFORM_DEFAULTS_JSON.addProperty("facing", "FIXED");
        TRANSFORM_DEFAULTS_JSON.addProperty("up", "WORLD_UP");
        TRANSFORM_DEFAULTS_JSON.addProperty("billboard", "NONE");
        
        // Fill defaults
        FILL_DEFAULTS_JSON.addProperty("mode", "SOLID");
        FILL_DEFAULTS_JSON.addProperty("wireThickness", 1.0f);
        FILL_DEFAULTS_JSON.addProperty("doubleSided", false);
        
        // Appearance defaults
        APPEARANCE_DEFAULTS_JSON.addProperty("color", "@primary");
        APPEARANCE_DEFAULTS_JSON.addProperty("alpha", 1.0f);
        APPEARANCE_DEFAULTS_JSON.addProperty("glow", 0.0f);
        
        // Animation defaults (none)
        ANIMATION_DEFAULTS_JSON.addProperty("phase", 0.0f);
    }
    
    // =========================================================================
    // JsonObject API (per CLASS_DIAGRAM §15)
    // =========================================================================
    
    /**
     * Gets defaults as JsonObject for a given type.
     * 
     * @param type Component type ("sphere", "transform", "fill", etc.)
     * @return JsonObject with default values
     */
    public static JsonObject getDefaults(String type) {
        return switch (type.toLowerCase()) {
            case "sphere", "ring", "prism", "polyhedron", "cylinder", "jet", "molecule" 
                -> SHAPE_DEFAULTS_JSON.getOrDefault(type.toLowerCase(), new JsonObject());
            case "transform" -> TRANSFORM_DEFAULTS_JSON.deepCopy();
            case "fill" -> FILL_DEFAULTS_JSON.deepCopy();
            case "appearance" -> APPEARANCE_DEFAULTS_JSON.deepCopy();
            case "animation" -> ANIMATION_DEFAULTS_JSON.deepCopy();
            default -> new JsonObject();
        };
    }
    
    /**
     * Applies defaults to a JsonObject.
     * Only adds default values for missing keys.
     * 
     * @param json Input JsonObject (may be null)
     * @param type Component type
     * @return JsonObject with defaults applied
     */
    public static JsonObject applyDefaults(JsonObject json, String type) {
        JsonObject defaults = getDefaults(type);
        
        if (json == null || json.size() == 0) {
            return defaults.deepCopy();
        }
        
        JsonObject result = defaults.deepCopy();
        for (String key : json.keySet()) {
            result.add(key, json.get(key));
        }
        return result;
    }
    
    // =========================================================================
    // Object API (per CLASS_DIAGRAM §13)
    // =========================================================================
    
    /**
     * Gets default Shape for a given type.
     */
    public static Shape getDefaultShape(String type) {
        return switch (type.toLowerCase()) {
            case "sphere" -> SphereShape.defaults();
            case "ring" -> RingShape.builder()
                .innerRadius(0.8f).outerRadius(1.0f).segments(64).build();
            case "prism" -> PrismShape.builder()
                .sides(6).radius(1.0f).height(1.0f).build();
            case "polyhedron" -> PolyhedronShape.builder()
                .polyType(PolyType.CUBE).radius(1.0f).build();
            case "cylinder" -> CylinderShape.builder()
                .radius(0.5f).height(10.0f).segments(16).build();
            case "jet" -> JetShape.DEFAULT;
            case "molecule" -> MoleculeShape.DEFAULT;
            default -> SphereShape.defaults();
        };
    }
    
    /**
     * Gets default Transform.
     */
    public static Transform getDefaultTransform() {
        return Transform.IDENTITY;
    }
    
    /**
     * Gets default FillConfig.
     */
    public static FillConfig getDefaultFill() {
        return FillConfig.SOLID;
    }
    
    /**
     * Gets default VisibilityMask.
     */
    public static VisibilityMask getDefaultVisibility() {
        return VisibilityMask.FULL;
    }
    
    /**
     * Gets default Appearance.
     */
    public static Appearance getDefaultAppearance() {
        return Appearance.DEFAULT;
    }
    
    /**
     * Gets default Animation.
     */
    public static Animation getDefaultAnimation() {
        return Animation.NONE;
    }
    
    // =========================================================================
    // Shorthand Expansion (per CLASS_DIAGRAM §13)
    // =========================================================================
    
    /**
     * Expands alpha shorthand: 0.5 → AlphaRange(0.5, 0.5)
     */
    public static AlphaRange expandAlpha(float alpha) {
        return AlphaRange.of(alpha);
    }
    
    /**
     * Expands spin shorthand: 0.02 → SpinConfig around Y axis
     */
    public static SpinConfig expandSpin(float speed) {
        return SpinConfig.aroundY(speed);
    }
    
    /**
     * Expands pulse shorthand: 0.5 → PulseConfig with amount
     */
    public static PulseConfig expandPulse(float amount) {
        return PulseConfig.builder().scale(amount).build();
    }
    
    // =========================================================================
    // Fill Variants
    // =========================================================================
    
    public static FillConfig getWireframeFill() {
        return FillConfig.builder().mode(FillMode.WIREFRAME).wireThickness(1.0f).build();
    }
    
    public static FillConfig getCageFill() {
        return FillConfig.builder().mode(FillMode.CAGE).wireThickness(1.5f).build();
    }
    
    public static FillConfig getPointsFill() {
        return FillConfig.builder().mode(FillMode.POINTS).build();
    }
}
