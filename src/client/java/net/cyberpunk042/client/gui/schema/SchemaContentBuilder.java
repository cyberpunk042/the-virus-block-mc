package net.cyberpunk042.client.gui.schema;

import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.log.Logging;

import java.util.List;

/**
 * Generates GUI widgets from an EffectSchema.
 * 
 * <p>Bridges the gap between EffectSchema definitions and the existing
 * ContentBuilder system. Creates appropriate widgets (sliders, toggles, etc.)
 * based on ParameterSpec configurations.</p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * ContentBuilder content = content(startY);
 * EffectSchema schema = EffectSchemaRegistry.get(EffectType.ENERGY_ORB, 2);
 * SchemaContentBuilder.buildFromSchema(content, schema, state);
 * }</pre>
 */
public final class SchemaContentBuilder {
    
    private static final String LOG_TOPIC = "schema_builder";
    
    private SchemaContentBuilder() {} // Static utility class
    
    /**
     * Builds all widgets from a schema, organized by groups.
     * 
     * @param content The ContentBuilder to add widgets to
     * @param schema  The effect schema defining parameters
     * @param state   The state for reading initial values
     */
    public static void buildFromSchema(ContentBuilder content, EffectSchema schema, FieldEditState state) {
        if (schema == null) {
            Logging.GUI.topic(LOG_TOPIC).warn("Cannot build from null schema");
            return;
        }
        
        Logging.GUI.topic(LOG_TOPIC).debug("Building UI from schema: {} ({} groups, {} params)",
            schema.displayName(), schema.groupNames().size(), schema.parameterCount());
        
        // Process each group in order
        for (String groupName : schema.groupNames()) {
            List<ParameterSpec> params = schema.getGroup(groupName);
            if (params.isEmpty()) continue;
            
            // Add section header
            content.sectionHeader(groupName);
            
            // Build widgets for each parameter
            for (ParameterSpec param : params) {
                buildWidget(content, param, state);
            }
        }
    }
    
    /**
     * Builds widgets for a single group only.
     * 
     * @param content   The ContentBuilder to add widgets to
     * @param schema    The effect schema
     * @param groupName The specific group to build
     * @param state     The state for reading initial values
     */
    public static void buildGroup(ContentBuilder content, EffectSchema schema, String groupName, FieldEditState state) {
        List<ParameterSpec> params = schema.getGroup(groupName);
        if (params.isEmpty()) {
            Logging.GUI.topic(LOG_TOPIC).debug("No params in group: {}", groupName);
            return;
        }
        
        content.sectionHeader(groupName);
        for (ParameterSpec param : params) {
            buildWidget(content, param, state);
        }
    }
    
    /**
     * Builds a single widget from a ParameterSpec.
     */
    public static void buildWidget(ContentBuilder content, ParameterSpec param, FieldEditState state) {
        switch (param.type()) {
            case SLIDER -> buildSlider(content, param);
            case INT_SLIDER -> buildIntSlider(content, param);
            case TOGGLE -> buildToggle(content, param);
            case COLOR_PICKER -> buildColorPicker(content, param);
            case DROPDOWN -> buildDropdown(content, param);
            case LABEL -> buildLabel(content, param, state);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET BUILDERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildSlider(ContentBuilder content, ParameterSpec param) {
        content.slider(param.label(), param.path())
            .range(param.min(), param.max())
            .add();
    }
    
    private static void buildIntSlider(ContentBuilder content, ParameterSpec param) {
        // Use slider with integer format
        content.slider(param.label(), param.path())
            .range(param.min(), param.max())
            .format("%.0f")
            .add();
    }
    
    private static void buildToggle(ContentBuilder content, ParameterSpec param) {
        content.toggle(param.label(), param.path());
    }
    
    private static void buildColorPicker(ContentBuilder content, ParameterSpec param) {
        // Color pickers are typically RGB sliders grouped together
        // For now, treat as normalized sliders
        content.slider(param.label(), param.path())
            .range(0f, 1f)
            .add();
    }
    
    private static void buildDropdown(ContentBuilder content, ParameterSpec param) {
        // Dropdowns need enum options - for now log a warning
        Logging.GUI.topic(LOG_TOPIC).warn("Dropdown not yet implemented for: {}", param.path());
    }
    
    private static void buildLabel(ContentBuilder content, ParameterSpec param, FieldEditState state) {
        Object value = state.get(param.path());
        String displayValue = value != null ? value.toString() : "—";
        content.infoText(param.label() + ": " + displayValue);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Builds a color group (R, G, B sliders) from a base path.
     * Uses existing vec3Row which expects X/Y/Z labels and a base path.
     * 
     * @param content    The ContentBuilder
     * @param basePath   Base state path (e.g., "fieldVisual.primary")
     * @param min        Min value for all sliders
     * @param max        Max value for all sliders
     */
    public static void buildColorRGB(ContentBuilder content, String basePath, float min, float max) {
        // Use vec3Row with R/G/B labels - but this expects a Vector3f path
        // For individual color component paths, we use individual sliders instead
        content.slider("R", basePath + "R").range(min, max).add();
        content.slider("G", basePath + "G").range(min, max).add();
        content.slider("B", basePath + "B").range(min, max).add();
    }
    
    /**
     * Builds a pair of related sliders on one row.
     */
    public static void buildSliderPair(ContentBuilder content, ParameterSpec left, ParameterSpec right) {
        content.sliderPair(
            left.label(), left.path(), left.min(), left.max(),
            right.label(), right.path(), right.min(), right.max()
        );
    }
    
    /**
     * Builds a triple of related sliders on one row.
     */
    public static void buildSliderTriple(ContentBuilder content, ParameterSpec a, ParameterSpec b, ParameterSpec c) {
        content.sliderTriple(
            a.label(), a.path(), a.min(), a.max(),
            b.label(), b.path(), b.min(), b.max(),
            c.label(), c.path(), c.min(), c.max()
        );
    }
}
