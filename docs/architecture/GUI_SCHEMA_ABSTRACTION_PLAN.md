# GUI Schema Abstraction for Effect-Specific Controls

> **Status**: Planning  
> **Created**: 2026-01-04  
> **Integrates With**: BoundPanel, ContentBuilder, FieldEditState

## Problem

Current `FieldVisualSubPanel` has:
- Hard-coded conditionals like `if (v2Version != null && v2Version >= 2)`
- Labels determined at runtime with fallbacks
- New effect types require adding more conditionals
- Duplicate code for similar controls across effect types

## Goal

Create a **schema-driven** abstraction that:
- Defines which controls appear for each effect type + version
- Allows custom labels per effect (e.g., "Glow Lines" vs "Ring Count")
- Works seamlessly with existing `ContentBuilder` fluent API
- Is configured in Java (no external JSON)
- Minimizes changes to existing infrastructure

---

## Architecture

### 1. ParameterSpec (Single Control Definition)

```java
/**
 * Defines a single GUI control with its configuration.
 * Immutable record - create with builder pattern.
 */
public record ParameterSpec(
    String path,           // State path: "fieldVisual.intensity"
    String label,          // Display label: "Brightness"
    ControlType type,      // SLIDER, TOGGLE, COLOR_RGB, DROPDOWN
    String group,          // Group name: "Animation", "Colors"
    float min,             // For sliders: minimum value
    float max,             // For sliders: maximum value
    String format,         // For sliders: "%.2f", "%.0f°"
    String tooltip,        // Optional hover text
    Object defaultValue    // Default value for reset
) {
    public enum ControlType { 
        SLIDER, TOGGLE, COLOR_RGB, COLOR_RGBA, DROPDOWN, BUTTON 
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUILDER FACTORIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public static ParameterSpec slider(String path, String label, String group,
                                       float min, float max, float defaultVal) {
        return new ParameterSpec(path, label, ControlType.SLIDER, group,
                                 min, max, "%.2f", null, defaultVal);
    }
    
    public static ParameterSpec slider(String path, String label, String group,
                                       float min, float max, String format, float defaultVal) {
        return new ParameterSpec(path, label, ControlType.SLIDER, group,
                                 min, max, format, null, defaultVal);
    }
    
    public static ParameterSpec toggle(String path, String label, String group, boolean defaultVal) {
        return new ParameterSpec(path, label, ControlType.TOGGLE, group,
                                 0, 1, null, null, defaultVal);
    }
    
    public static ParameterSpec colorRgb(String pathPrefix, String label, String group) {
        // pathPrefix = "fieldVisual.primary" → creates R, G, B controls
        return new ParameterSpec(pathPrefix, label, ControlType.COLOR_RGB, group,
                                 0, 1, "%.2f", null, null);
    }
}
```

### 2. EffectSchema (Effect Configuration)

```java
/**
 * Defines the complete GUI layout for an effect type + version.
 * Controls appear in order specified.
 */
public record EffectSchema(
    EffectType effectType,
    int version,
    String displayName,            // "Energy Orb (Classic)", "Volumetric Star"
    Map<String, List<ParameterSpec>> groups  // Ordered: "Colors" → [...], "Animation" → [...]
) {
    /**
     * Returns parameters for a specific group.
     */
    public List<ParameterSpec> getGroup(String groupName) {
        return groups.getOrDefault(groupName, List.of());
    }
    
    /**
     * Returns all group names in order.
     */
    public List<String> getGroupNames() {
        return new ArrayList<>(groups.keySet());
    }
    
    /**
     * Check if a parameter exists in this schema.
     */
    public boolean hasParameter(String path) {
        return groups.values().stream()
            .flatMap(List::stream)
            .anyMatch(p -> p.path().equals(path));
    }
}
```

### 3. EffectSchemaRegistry (Central Configuration)

```java
/**
 * Central registry of all effect schemas.
 * Add new effects here - single source of truth.
 */
public final class EffectSchemaRegistry {
    
    private static final Map<String, EffectSchema> SCHEMAS = new LinkedHashMap<>();
    
    static {
        registerEnergyOrbV1();
        registerEnergyOrbV2();
        registerVolumetricStar();
        registerGeodesic();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ENERGY ORB V1 - Classic controls
    // ═══════════════════════════════════════════════════════════════════════
    
    private static void registerEnergyOrbV1() {
        var groups = new LinkedHashMap<String, List<ParameterSpec>>();
        
        groups.put("Colors", List.of(
            ParameterSpec.colorRgb("fieldVisual.primary", "Core Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.secondary", "Edge Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.tertiary", "Outer Color", "Colors")
        ));
        
        groups.put("Animation", List.of(
            ParameterSpec.slider("fieldVisual.intensity", "Brightness", "Animation", 0.1f, 3f, 1.2f),
            ParameterSpec.slider("fieldVisual.animationSpeed", "Speed", "Animation", 0.1f, 10f, 1f)
        ));
        
        groups.put("Shape", List.of(
            ParameterSpec.slider("fieldVisual.coreSize", "Core Size", "Shape", 0.01f, 3f, 0.15f),
            ParameterSpec.slider("fieldVisual.edgeSharpness", "Edge Sharpness", "Shape", 1f, 10f, 4f)
        ));
        
        groups.put("Effects", List.of(
            ParameterSpec.slider("fieldVisual.spiralDensity", "Spiral Density", "Effects", 1f, 32f, 5f),
            ParameterSpec.slider("fieldVisual.spiralTwist", "Spiral Twist", "Effects", 1f, 20f, 5f),
            ParameterSpec.slider("fieldVisual.glowLineCount", "Glow Lines", "Effects", 4f, 64f, "%.0f", 16f),
            ParameterSpec.slider("fieldVisual.glowLineIntensity", "Line Intensity", "Effects", 0.1f, 5f, 0.8f),
            ParameterSpec.slider("fieldVisual.coronaWidth", "Lens Width", "Effects", 0f, 2f, 0.5f)
        ));
        
        register(new EffectSchema(EffectType.ENERGY_ORB, 1, "Energy Orb (Classic)", groups));
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ENERGY ORB V2 - Enhanced with external effects
    // ═══════════════════════════════════════════════════════════════════════
    
    private static void registerEnergyOrbV2() {
        var groups = new LinkedHashMap<String, List<ParameterSpec>>();
        
        // Same Colors as V1
        groups.put("Colors", List.of(
            ParameterSpec.colorRgb("fieldVisual.primary", "Core Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.secondary", "Edge Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.tertiary", "Glow Tint", "Colors")  // Different label!
        ));
        
        // Same Animation as V1
        groups.put("Animation", List.of(
            ParameterSpec.slider("fieldVisual.intensity", "Intensity", "Animation", 0.1f, 3f, 1.2f),
            ParameterSpec.slider("fieldVisual.animationSpeed", "Speed", "Animation", 0.1f, 10f, 1f)
        ));
        
        // Shape with slightly different labels
        groups.put("Shape", List.of(
            ParameterSpec.slider("fieldVisual.coreSize", "Core Size", "Shape", 0.01f, 3f, 0.15f),
            ParameterSpec.slider("fieldVisual.edgeSharpness", "Edge Falloff", "Shape", 1f, 10f, 4f)
        ));
        
        // Effects with additional params
        groups.put("Effects", List.of(
            ParameterSpec.slider("fieldVisual.spiralDensity", "Pattern Scale", "Effects", 1f, 32f, 5f),
            ParameterSpec.slider("fieldVisual.spiralTwist", "Pattern Twist", "Effects", 1f, 20f, 5f),
            ParameterSpec.slider("fieldVisual.glowLineCount", "Noise Scale", "Effects", 4f, 64f, "%.0f", 16f),
            ParameterSpec.slider("fieldVisual.glowLineIntensity", "Noise Intensity", "Effects", 0.1f, 5f, 0.8f),
            ParameterSpec.slider("fieldVisual.coronaWidth", "Lens Width", "Effects", 0f, 2f, 0.5f)
        ));
        
        // V2 Only: External effects section
        groups.put("External Effects", List.of(
            ParameterSpec.toggle("fieldVisual.showExternalRays", "External Rays", "External Effects", true),
            ParameterSpec.toggle("fieldVisual.showCorona", "Corona Glow", "External Effects", true)
        ));
        
        register(new EffectSchema(EffectType.ENERGY_ORB, 2, "Energy Orb (Shadertoy)", groups));
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VOLUMETRIC STAR - Full control set
    // ═══════════════════════════════════════════════════════════════════════
    
    private static void registerVolumetricStar() {
        var groups = new LinkedHashMap<String, List<ParameterSpec>>();
        
        // 5 colors for this effect type
        groups.put("Colors", List.of(
            ParameterSpec.colorRgb("fieldVisual.primary", "Body Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.secondary", "Base Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.tertiary", "Dark Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.highlight", "Highlight", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.ray", "Ray Color", "Colors")
        ));
        
        // Multi-speed animation controls
        groups.put("Animation", List.of(
            ParameterSpec.slider("fieldVisual.anim.speedHigh", "Detail Speed", "Animation", 0f, 10f, 2f),
            ParameterSpec.slider("fieldVisual.anim.speedLow", "Base Speed", "Animation", 0f, 10f, 2f),
            ParameterSpec.slider("fieldVisual.anim.speedRay", "Ray Speed", "Animation", 0f, 10f, 5f),
            ParameterSpec.slider("fieldVisual.anim.speedRing", "Ring Speed", "Animation", 0f, 10f, 2f)
        ));
        
        // Detail controls
        groups.put("Detail", List.of(
            ParameterSpec.slider("fieldVisual.noiseDetail.octaves", "Detail Level", "Detail", 0f, 5f, "%.0f", 3f),
            ParameterSpec.slider("fieldVisual.noiseConfig.seed", "Seed", "Detail", -10f, 10f, "%.0f", 0f)
        ));
        
        // Ray controls
        groups.put("Rays", List.of(
            ParameterSpec.slider("fieldVisual.glowLine.rayPower", "Ray Power", "Rays", 1f, 10f, 2f),
            ParameterSpec.slider("fieldVisual.glowLine.raySharpness", "Ray Sharpness", "Rays", 0.02f, 10f, 1f),
            ParameterSpec.slider("fieldVisual.corona.ringPower", "Ring Power", "Rays", 1f, 10f, 1f)
        ));
        
        // Glow controls
        groups.put("Glow", List.of(
            ParameterSpec.slider("fieldVisual.coreEdge.coreFalloff", "Glow Falloff", "Glow", 1f, 100f, 4f),
            ParameterSpec.slider("fieldVisual.corona.multiplier", "Glow Intensity", "Glow", 10f, 100f, 50f)
        ));
        
        register(new EffectSchema(EffectType.ENERGY_ORB, 3, "Volumetric Star", groups));
        // Note: Using version=3 to distinguish from V1/V2, or add new EffectType
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // GEODESIC - SDF geometry controls
    // ═══════════════════════════════════════════════════════════════════════
    
    private static void registerGeodesic() {
        var groups = new LinkedHashMap<String, List<ParameterSpec>>();
        
        groups.put("Colors", List.of(
            ParameterSpec.colorRgb("fieldVisual.primary", "Face Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.secondary", "Back Color", "Colors"),
            ParameterSpec.colorRgb("fieldVisual.tertiary", "Edge Glow", "Colors")
        ));
        
        // Geometry controls unique to this effect
        groups.put("Geometry", List.of(
            ParameterSpec.slider("fieldVisual.geometry.subdivisions", "Subdivisions", "Geometry", 1f, 8f, "%.0f", 3f),
            ParameterSpec.slider("fieldVisual.geometry.roundTop", "Edge Rounding", "Geometry", 0f, 0.5f, 0.05f),
            ParameterSpec.slider("fieldVisual.geometry.roundCorner", "Corner Rounding", "Geometry", 0f, 0.5f, 0.1f),
            ParameterSpec.slider("fieldVisual.geometry2.gap", "Tile Gap", "Geometry", 0f, 0.5f, 0.005f),
            ParameterSpec.slider("fieldVisual.geometry2.height", "Tile Height", "Geometry", 0.5f, 5f, 2f)
        ));
        
        // Animation timing
        groups.put("Animation", List.of(
            ParameterSpec.slider("fieldVisual.timing.animFrequency", "Anim Frequency", "Animation", 5f, 50f, 10f),
            ParameterSpec.slider("fieldVisual.timing.sceneDuration", "Cycle Duration", "Animation", 1f, 20f, 6f)
        ));
        
        // Lighting
        groups.put("Lighting", List.of(
            ParameterSpec.slider("fieldVisual.lighting.diffuseStrength", "Diffuse", "Lighting", 0f, 2f, 1.2f),
            ParameterSpec.slider("fieldVisual.lighting.ambientStrength", "Ambient", "Lighting", 0f, 2f, 0.8f),
            ParameterSpec.slider("fieldVisual.lighting.fresnelStrength", "Fresnel", "Lighting", 0f, 1f, 0.2f)
        ));
        
        // Transform
        groups.put("Transform", List.of(
            ParameterSpec.slider("fieldVisual.transform.rotationX", "Rotation X", "Transform", 0f, 6.28f, 0.3f),
            ParameterSpec.slider("fieldVisual.transform.rotationY", "Rotation Y", "Transform", 0f, 6.28f, 0.25f)
        ));
        
        register(new EffectSchema(EffectType.GEODESIC, 1, "Geodesic Sphere", groups));
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // REGISTRY ACCESS
    // ═══════════════════════════════════════════════════════════════════════
    
    private static void register(EffectSchema schema) {
        SCHEMAS.put(key(schema.effectType(), schema.version()), schema);
    }
    
    private static String key(EffectType type, int version) {
        return type.name() + "_V" + version;
    }
    
    /**
     * Gets the schema for an effect type and version.
     * Falls back to version 1 if specific version not found.
     */
    public static EffectSchema getSchema(EffectType type, int version) {
        EffectSchema schema = SCHEMAS.get(key(type, version));
        if (schema == null) {
            schema = SCHEMAS.get(key(type, 1)); // Fallback to V1
        }
        return schema;
    }
    
    /**
     * Gets all registered effect types and versions.
     */
    public static List<EffectSchema> getAllSchemas() {
        return new ArrayList<>(SCHEMAS.values());
    }
}
```

### 4. SchemaContentBuilder (ContentBuilder Extension)

```java
/**
 * Extends ContentBuilder with schema-aware widget creation.
 * Automatically creates appropriate widgets based on ParameterSpec.
 */
public class SchemaContentBuilder {
    
    private final ContentBuilder content;
    private final EffectSchema schema;
    private final int panelWidth;
    
    public SchemaContentBuilder(ContentBuilder content, EffectSchema schema, int panelWidth) {
        this.content = content;
        this.schema = schema;
        this.panelWidth = panelWidth;
    }
    
    /**
     * Builds all controls for the current schema.
     * Creates sections for each group automatically.
     */
    public void buildAll() {
        for (String groupName : schema.getGroupNames()) {
            buildGroup(groupName);
        }
    }
    
    /**
     * Builds controls for a specific group.
     */
    public void buildGroup(String groupName) {
        List<ParameterSpec> params = schema.getGroup(groupName);
        if (params.isEmpty()) return;
        
        content.sectionHeader(groupName);
        
        for (ParameterSpec param : params) {
            buildControl(param);
        }
        
        content.gap();
    }
    
    /**
     * Builds a single control from its specification.
     */
    private void buildControl(ParameterSpec param) {
        switch (param.type()) {
            case SLIDER -> content.slider(param.label(), param.path())
                .range(param.min(), param.max())
                .format(param.format())
                .add();
                
            case TOGGLE -> content.toggle(param.label(), param.path());
            
            case COLOR_RGB -> buildColorRgbRow(param);
            
            case DROPDOWN -> {
                // Would need enum class info - handle separately
            }
        }
    }
    
    /**
     * Builds a row of 3 RGB sliders.
     */
    private void buildColorRgbRow(ParameterSpec param) {
        String pathPrefix = param.path(); // e.g., "fieldVisual.primary"
        content.sliderTriple(
            "R", pathPrefix + "R", 0f, 1f,
            "G", pathPrefix + "G", 0f, 1f,
            "B", pathPrefix + "B", 0f, 1f
        );
    }
}
```

### 5. Updated FieldVisualSubPanel (Simplified)

```java
public class FieldVisualSubPanel extends BoundPanel {
    
    @Override
    protected void buildContent() {
        syncToEffect();
        ContentBuilder content = content(startY);
        
        // Get current effect type and version from state
        EffectType effectType = (EffectType) state.get("fieldVisual.effectType");
        Integer version = (Integer) state.get("fieldVisual.version");
        if (effectType == null) effectType = EffectType.ENERGY_ORB;
        if (version == null) version = 1;
        
        // Get schema for this effect
        EffectSchema schema = EffectSchemaRegistry.getSchema(effectType, version);
        
        // ═══════════════════════════════════════════════════════════════════════
        // HEADER (always present)
        // ═══════════════════════════════════════════════════════════════════════
        content.sectionHeader(schema.displayName());
        buildConfigControls(content);  // Enable, Source, Version toggle
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // SCHEMA-DRIVEN CONTROLS
        // ═══════════════════════════════════════════════════════════════════════
        SchemaContentBuilder schemaBuilder = new SchemaContentBuilder(content, schema, panelWidth);
        schemaBuilder.buildAll();
        
        // ═══════════════════════════════════════════════════════════════════════
        // ACTIONS (always present)
        // ═══════════════════════════════════════════════════════════════════════
        buildActionControls(content);
        
        contentHeight = content.getContentHeight();
    }
    
    private void buildConfigControls(ContentBuilder content) {
        // Enable/Source/Version row - same as before
        // This is effect-independent infrastructure
    }
    
    private void buildActionControls(ContentBuilder content) {
        // Throw, Reset buttons - same as before
    }
}
```

---

## Benefits

| Before | After |
|--------|-------|
| 500+ lines of manual widget creation | ~200 lines + schema definitions |
| Hard-coded `if (version >= 2)` checks | Schema automatically shows/hides groups |
| Duplicate slider creation code | Single `buildControl()` handles all |
| Labels scattered in buildContent() | Labels centralized in schema |
| Adding new effect = modify buildContent() | Adding new effect = add register call |

---

## Migration Path

1. **Phase 1**: Create `ParameterSpec`, `EffectSchema`, `EffectSchemaRegistry`
2. **Phase 2**: Create `SchemaContentBuilder` 
3. **Phase 3**: Refactor `FieldVisualSubPanel` to use schema
4. **Phase 4**: Verify existing V1/V2 behavior preserved
5. **Phase 5**: Add new effect schemas (Star, Geodesic)

---

## Schema File Location

```
src/client/java/net/cyberpunk042/client/gui/schema/
├── ParameterSpec.java
├── EffectSchema.java
├── EffectSchemaRegistry.java
└── SchemaContentBuilder.java
```
