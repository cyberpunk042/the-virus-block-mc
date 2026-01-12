package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.schema.EffectSchema;
import net.cyberpunk042.client.gui.schema.EffectSchemaRegistry;
import net.cyberpunk042.client.gui.schema.ParameterSpec;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.visual.effect.ColorBlendMode;
import net.cyberpunk042.client.visual.effect.EffectType;
import net.cyberpunk042.client.visual.effect.FieldVisualPostEffect;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Field Visual Effect controls panel.
 * 
 * <p>Uses the schema-based GUI system to dynamically generate controls
 * based on the selected effect type and version.</p>
 */
public class FieldVisualSubPanel extends BoundPanel {
    
    private final int startY;
    
    public FieldVisualSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("FieldVisualSubPanel created (schema-driven)");
    }
    
    @Override
    protected void buildContent() {
        syncToEffect();
        
        ContentBuilder content = content(startY);
        
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;
        int thirdW = (w - GuiConstants.COMPACT_GAP * 2) / 3;
        int x2 = x + thirdW + GuiConstants.COMPACT_GAP;
        int x3 = x + (thirdW + GuiConstants.COMPACT_GAP) * 2;
        int y;
        
        // ═══════════════════════════════════════════════════════════════════════════
        // HEADER - Preset, Enable, Source, Version
        // ═══════════════════════════════════════════════════════════════════════════
        
        // Get current effect type and version for schema lookup
        EffectType presetFilterType = (EffectType) state.get("fieldVisual.effectType");
        if (presetFilterType == null) presetFilterType = EffectType.ENERGY_ORB;
        Integer schemaVersion = (Integer) state.get("fieldVisual.version");
        if (schemaVersion == null) schemaVersion = 1;
        
        // Get schema early so header controls can use schema values (enables General group override)
        EffectSchema headerSchema = EffectSchemaRegistry.get(presetFilterType, schemaVersion);
        if (headerSchema == null) {
            List<Integer> available = EffectSchemaRegistry.versionsFor(presetFilterType);
            if (!available.isEmpty()) {
                schemaVersion = available.get(0);
                headerSchema = EffectSchemaRegistry.get(presetFilterType, schemaVersion);
            }
        }
        
        // List only presets matching the current effect type
        List<String> fragments = net.cyberpunk042.client.gui.util.FragmentRegistry.listFieldVisualFragments(presetFilterType);
        
        // Section header matches effect type
        String headerLabel = presetFilterType == EffectType.GEODESIC ? "Geodesic Sphere Effect" : "Energy Orb Effect";
        content.sectionHeader(headerLabel);
        
        // Build primitive options list
        List<String> primitiveOptions = new java.util.ArrayList<>();
        primitiveOptions.add("None");
        var fieldLayers = state.getFieldLayers();
        if (fieldLayers != null) {
            for (int li = 0; li < fieldLayers.size(); li++) {
                var layer = fieldLayers.get(li);
                var prims = layer.primitives();
                for (int pi = 0; pi < prims.size(); pi++) {
                    var prim = prims.get(pi);
                    String label = String.format("L%d.P%d:%s", li, pi, 
                        prim.type().toString().substring(0, Math.min(4, prim.type().toString().length())));
                    primitiveOptions.add(label);
                }
            }
        }
        
        String currentRef = state.fieldVisualAdapter().sourceRef();
        int initialIndex = 0;
        if (currentRef != null) {
            for (int i = 1; i < primitiveOptions.size(); i++) {
                if (primitiveOptions.get(i).startsWith("L" + currentRef.replace(".", ".P"))) {
                    initialIndex = i;
                    break;
                }
            }
        }
        
        final int fInitialIndex = initialIndex;
        final List<String> fOptions = primitiveOptions;
        final List<net.cyberpunk042.field.FieldLayer> fLayers = fieldLayers;
        boolean hasSourcePrimitive = currentRef != null;
        
        y = content.getCurrentY();
        
        // ─── Row 0: Preset selector ───
        if (fragments.size() > 2) {
            String currentPreset = (String) state.get("fieldVisual.currentPresetName");
            if (currentPreset == null || !fragments.contains(currentPreset)) currentPreset = "Default";
            final String fCurrentPreset = currentPreset;
            
            widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                name -> Text.literal(name))
                .values(fragments)
                .initially(fCurrentPreset)
                .build(x, y, w, 20, Text.literal("Preset"), (btn, value) -> {
                    state.set("fieldVisual.currentPresetName", value);
                    if ("Default".equals(value)) {
                        // Reset to schema defaults for current version
                        state.fieldVisualAdapter().reset();
                        state.applyFieldVisualSchemaDefaults();
                        state.markDirty();
                    } else if (!"Custom".equals(value)) {
                        net.cyberpunk042.client.gui.util.FragmentRegistry.applyFieldVisualFragment(state, value);
                        state.markDirty();
                    }
                    rebuildContent();
                    syncToEffect();
                }));
            content.advanceBy(22);
            y = content.getCurrentY();
        }
        
        // ─── Row 1: Enable + Source + Effect Type ───
        Boolean enabled = (Boolean) state.get("fieldVisual.enabled");
        if (enabled == null) enabled = false;
        final boolean fEnabled = enabled;
        widgets.add(GuiWidgets.toggle(x, y, thirdW,
            fEnabled ? "§aEnabled" : "Disabled", fEnabled, "Toggle effect",
            v -> { 
                state.set("fieldVisual.enabled", v); 
                // Sync to effect system to register/unregister preview orb
                var client = net.minecraft.client.MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    state.fieldVisualAdapter().syncToEffect(client.player.getBoundingBox().getCenter());
                }
                rebuildContent(); 
            }));
        
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(v -> Text.literal(v))
            .values(fOptions).initially(fOptions.get(fInitialIndex))
            .build(x2, y, thirdW, 20, Text.literal("Src"), (btn, value) -> {
                if (value.equals("None")) {
                    state.set("fieldVisual.sourceRef", null);
                } else {
                    String prefix = value.split(":")[0].trim();
                    int li = Integer.parseInt(prefix.substring(1, prefix.indexOf('.')));
                    int pi = Integer.parseInt(prefix.substring(prefix.indexOf('P') + 1));
                    state.set("fieldVisual.sourceRef", li + "." + pi);
                    if (fLayers != null && li < fLayers.size() && pi < fLayers.get(li).primitives().size()) {
                        state.fieldVisualAdapter().syncFromPrimitive(fLayers.get(li).primitives().get(pi));
                    }
                }
                rebuildContent();
            }));
        
        // Effect type selector
        EffectType currentType = (EffectType) state.get("fieldVisual.effectType");
        if (currentType == null) currentType = EffectType.ENERGY_ORB;
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<EffectType>builder(
            t -> Text.literal(t.getDisplayName()))
            .values(EffectType.values()).initially(currentType)
            .build(x3, y, thirdW, 20, Text.literal("Type"), (btn, v) -> {
                state.set("fieldVisual.effectType", v);
                // Apply schema defaults for the new effect type
                state.applyFieldVisualSchemaDefaults();
                rebuildContent();  // Rebuild to refresh preset list for new type
                syncToEffect();
            }));
        content.advanceBy(22);
        
        // ─── Row 2: Intensity + Speed + Version ───
        // Use schema values if available (enables General group override)
        y = content.getCurrentY();
        var intensitySpec = headerSchema != null ? headerSchema.getParameter("fieldVisual.intensity") : null;
        var speedSpec = headerSchema != null ? headerSchema.getParameter("fieldVisual.animationSpeed") : null;
        float intMin = intensitySpec != null ? intensitySpec.min() : 0f;
        float intMax = intensitySpec != null ? intensitySpec.max() : 5f;
        float intDef = intensitySpec != null ? intensitySpec.defaultValue() : 1.0f;
        float spdMin = speedSpec != null ? speedSpec.min() : 0.01f;
        float spdMax = speedSpec != null ? speedSpec.max() : 10f;
        float spdDef = speedSpec != null ? speedSpec.defaultValue() : 1.0f;
        Float intensity = getFloat("fieldVisual.intensity", intDef);
        Float speed = getFloat("fieldVisual.animationSpeed", spdDef);
        widgets.add(GuiWidgets.slider(x, y, thirdW, "Intensity", intMin, intMax, intensity, "%.2f", null,
            v -> { state.set("fieldVisual.intensity", v); syncToEffect(); }));
        widgets.add(GuiWidgets.slider(x2, y, thirdW, "Speed", spdMin, spdMax, speed, "%.2f", null,
            v -> { state.set("fieldVisual.animationSpeed", v); syncToEffect(); }));
        
        // Version button (V1/V2/V3/V4)
        Integer ver = (Integer) state.get("fieldVisual.version");
        if (ver == null) ver = 1;
        final int fVer = ver;
        List<Integer> versions = EffectSchemaRegistry.versionsFor(currentType);
        if (versions.isEmpty()) versions = List.of(1, 2);
        final List<Integer> fVersions = versions;
        final EffectType fCurrentType = currentType;
        
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<Integer>builder(
            v -> {
                EffectSchema s = EffectSchemaRegistry.get(fCurrentType, v);
                String lbl = s != null ? s.displayName() : "V" + v;
                return Text.literal(lbl.replace("Energy Orb ", "").replace("Volumetric ", "Vol "));
            })
            .values(fVersions).initially(fVer)
            .build(x3, y, thirdW, 20, Text.literal("Ver"), (btn, next) -> {
                state.set("fieldVisual.version", next);
                
                // Check if a real preset is selected (not Default/Custom)
                String currentPreset = (String) state.get("fieldVisual.currentPresetName");
                if (currentPreset != null && !"Default".equals(currentPreset) && !"Custom".equals(currentPreset)) {
                    // Reload the preset but skip its version field - keep the user's selected version
                    net.cyberpunk042.client.gui.util.FragmentRegistry.applyFieldVisualFragment(state, currentPreset, true);
                    Logging.GUI.topic("panel").info("Version changed to V{}, reloaded preset '{}' (version preserved)", next, currentPreset);
                } else {
                    // No preset selected, apply schema defaults for the new version
                    state.applyFieldVisualSchemaDefaults();
                }
                
                rebuildContent();
                syncToEffect();
            }));
        content.advanceBy(22);
        
        // ─── Row 3: Follow + Position + Radius ───
        y = content.getCurrentY();
        Boolean follow = (Boolean) state.get("fieldVisual.followMode");
        if (follow == null) follow = true;
        final boolean isFollow = follow;
        widgets.add(ButtonWidget.builder(Text.literal(isFollow ? "§aFollow" : "§7Fixed"), btn -> {
            state.set("fieldVisual.followMode", !isFollow);
            FieldVisualPostEffect.setFollowMode(!isFollow);
            rebuildContent();
        }).dimensions(x, y, thirdW, 20).build());
        
        String pos = (String) state.get("fieldVisual.orbStartPosition");
        if (pos == null) pos = "center";
        List<String> positions = List.of("center", "front", "left", "right", "behind", "above", 
                                         "left-hand", "right-hand", "left-front", "right-front");
        final String fPos = pos;
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
            p -> Text.literal("Pos: " + p.substring(0,1).toUpperCase() + p.substring(1)))
            .values(positions).initially(fPos)
            .build(x2, y, thirdW, 20, Text.literal("Pos"), (btn, newPos) -> {
                state.set("fieldVisual.orbStartPosition", newPos);
                rebuildContent();
            }));
        
        if (!hasSourcePrimitive) {
            // Use schema values if available (enables General group override)
            var radiusSpec = headerSchema != null ? headerSchema.getParameter("fieldVisual.previewRadius") : null;
            float radMin = radiusSpec != null ? radiusSpec.min() : 0.01f;
            float radMax = radiusSpec != null ? radiusSpec.max() : 50f;
            float radDef = radiusSpec != null ? radiusSpec.defaultValue() : 3f;
            Float radius = getFloat("fieldVisual.previewRadius", radDef);
            widgets.add(GuiWidgets.slider(x3, y, thirdW, "Radius", radMin, radMax, radius, "%.2f", null,
                v -> { state.set("fieldVisual.previewRadius", v); syncToEffect(); }));
        }
        content.advanceBy(22);
        
        // ─── Row 4: Color Blend Mode (includes SUBTRACT for darkening) ───
        y = content.getCurrentY();
        ColorBlendMode currentMode = (ColorBlendMode) state.get("fieldVisual.colorBlendMode");
        if (currentMode == null) currentMode = ColorBlendMode.MULTIPLY;
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<ColorBlendMode>builder(
            mode -> Text.literal(mode.getDisplayName()))
            .values(ColorBlendMode.values()).initially(currentMode)
            .tooltip(mode -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(mode.getTooltip())))
            .build(x, y, w, 20, Text.literal("Color Mode"), (btn, mode) -> {
                state.set("fieldVisual.colorBlendMode", mode);
                syncToEffect();
            }));
        // NOTE: Hardware blend equations don't work for post-processing.
        // All blending is done in the shader via ColorBlendMode (including SUBTRACT for darkening).
        content.advanceBy(22);
        
        // ─── Row 5: HDR Targets Toggle ───
        y = content.getCurrentY();
        net.cyberpunk042.client.gui.config.RenderConfig renderConfig = net.cyberpunk042.client.gui.config.RenderConfig.get();
        boolean hdrEnabled = renderConfig.isHdrEnabled();
        widgets.add(GuiWidgets.toggle(x, y, halfW,
            hdrEnabled ? "§aHDR Targets" : "§7HDR Targets", hdrEnabled, 
            "Enable RGBA16F render targets for smoother glow (eliminates banding)",
            v -> { 
                Logging.GUI.topic("HDR_TOGGLE")
                    .kv("newValue", v)
                    .info("══════════════════════════════════════════════════════════════");
                Logging.GUI.topic("HDR_TOGGLE")
                    .kv("newValue", v)
                    .info("[HDR_TOGGLE_START] User toggled HDR to: {}", v);
                
                renderConfig.setHdrEnabled(v);
                net.cyberpunk042.client.visual.render.PostFxPipeline.getInstance().invalidateTargets();
                
                // Clear BOTH caches to force recreation with new format
                net.cyberpunk042.client.visual.effect.FieldVisualPostEffect.clearProcessorCache();
                net.cyberpunk042.client.visual.shader.util.ShaderLoaderCacheHelper.clearOurProcessors();
                
                Logging.GUI.topic("HDR_TOGGLE")
                    .info("[HDR_TOGGLE_DONE] Caches cleared, next frame should reload processors");
                Logging.GUI.topic("HDR_TOGGLE")
                    .info("══════════════════════════════════════════════════════════════");
                
                rebuildContent(); 
            }));
        
        // Blur quality slider (only shown when HDR is enabled)
        if (hdrEnabled) {
            float blurQuality = renderConfig.getBlurQuality();
            // Display as 25-100%, apply as 0.25-1.0
            widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 
                "Blur", 25f, 100f, blurQuality * 100f, "%.0f%%", 
                "Lower = faster, Higher = smoother",
                v -> {
                    renderConfig.setBlurQuality(v / 100f);
                    net.cyberpunk042.client.visual.render.PostFxPipeline.getInstance().invalidateTargets();
                }));
        }
        content.advanceBy(22);
        
        // ─── Row 6: God Rays Toggle (only when HDR enabled) ───
        if (hdrEnabled) {
            y = content.getCurrentY();
            boolean godRaysEnabled = renderConfig.isGodRaysEnabled();
            widgets.add(GuiWidgets.toggle(x, y, halfW,
                godRaysEnabled ? "§aGod Rays" : "§7God Rays", godRaysEnabled, 
                "Volumetric light shafts emanating from orb",
                v -> { 
                    renderConfig.setGodRaysEnabled(v);
                    // Clear caches to reload shaders with god ray passes
                    net.cyberpunk042.client.visual.effect.FieldVisualPostEffect.clearProcessorCache();
                    rebuildContent(); 
                }));
            
            // God Rays parameters (only when enabled)
            if (godRaysEnabled) {
                // Decay slider (controls RANGE)
                float decay = renderConfig.getGodRaysDecay();
                widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 
                    "Reach", 0.94f, 0.99f, decay, "%.3f", 
                    "Higher = longer rays (range control)",
                    v -> renderConfig.setGodRaysDecay(v)));
                content.advanceBy(22);
                
                // Exposure slider (controls STRENGTH)
                y = content.getCurrentY();
                float exposure = renderConfig.getGodRaysExposure();
                widgets.add(GuiWidgets.slider(x, y, halfW, 
                    "Strength", 0.005f, 0.1f, exposure, "%.3f", 
                    "Higher = brighter rays (strength control)",
                    v -> renderConfig.setGodRaysExposure(v)));
                
                // Threshold slider (controls what creates rays)
                float threshold = renderConfig.getGodRaysThreshold();
                widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 
                    "Threshold", 0f, 1f, threshold, "%.2f", 
                    "Lower = more rays from dimmer areas",
                    v -> renderConfig.setGodRaysThreshold(v)));
                content.advanceBy(22);
                
                // Sky rays toggle
                y = content.getCurrentY();
                boolean skyEnabled = renderConfig.isGodRaysSkyEnabled();
                widgets.add(GuiWidgets.toggle(x, y, halfW, 
                    "Sky Rays", skyEnabled, 
                    "Enable atmospheric rays from sky",
                    v -> renderConfig.setGodRaysSkyEnabled(v)));
                
                // Energy mode: Radiation/Absorption/Pulse
                int energyMode = renderConfig.getGodRaysEnergyMode();
                List<String> energyModes = List.of("Radiation", "Absorption", "Pulse");
                widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                    m -> Text.literal(m))
                    .values(energyModes).initially(energyModes.get(energyMode))
                    .tooltip(m -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                        m.equals("Radiation") ? "Rays flow outward from orb" :
                        m.equals("Absorption") ? "Rays flow inward toward orb" :
                        "Rays alternate direction")))
                    .build(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 20, Text.literal("Energy"), (btn, mode) -> {
                        renderConfig.setGodRaysEnergyMode(energyModes.indexOf(mode));
                    }));
                content.advanceBy(22);
                
                // Color mode and Distribution mode
                y = content.getCurrentY();
                int colorMode = renderConfig.getGodRaysColorMode();
                List<String> colorModes = List.of("Solid", "Gradient", "Temperature");
                widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                    m -> Text.literal(m))
                    .values(colorModes).initially(colorModes.get(colorMode))
                    .tooltip(m -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                        m.equals("Solid") ? "Single color tint" :
                        m.equals("Gradient") ? "Color shifts center to edge" :
                        "Warm core, cool edges")))
                    .build(x, y, halfW, 20, Text.literal("Color"), (btn, mode) -> {
                        renderConfig.setGodRaysColorMode(colorModes.indexOf(mode));
                        rebuildContent();  // Show/hide gradient power slider
                    }));
                
                int distMode = renderConfig.getGodRaysDistributionMode();
                List<String> distModes = List.of("Uniform", "Weighted", "Noise");
                widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                    m -> Text.literal(m))
                    .values(distModes).initially(distModes.get(distMode))
                    .tooltip(m -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                        m.equals("Uniform") ? "Equal in all directions" :
                        m.equals("Weighted") ? "Bias toward vertical/horizontal" :
                        "Organic noise variation")))
                    .build(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 20, Text.literal("Dist"), (btn, mode) -> {
                        renderConfig.setGodRaysDistributionMode(distModes.indexOf(mode));
                        rebuildContent();  // Show/hide noise sliders
                    }));
                content.advanceBy(22);
                
                // Noise controls (only when distribution mode is Noise)
                if (distMode == 2) {
                    y = content.getCurrentY();
                    float noiseScale = renderConfig.getGodRaysNoiseScale();
                    widgets.add(GuiWidgets.slider(x, y, halfW, 
                        "Noise Scale", 1f, 20f, noiseScale, "%.1f", 
                        "Angular noise frequency",
                        v -> renderConfig.setGodRaysNoiseScale(v)));
                    
                    float noiseIntensity = renderConfig.getGodRaysNoiseIntensity();
                    widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 
                        "Intensity", 0f, 1f, noiseIntensity, "%.2f", 
                        "Noise modulation strength",
                        v -> renderConfig.setGodRaysNoiseIntensity(v)));
                    content.advanceBy(22);
                }
                
                // Gradient power (only when color mode is Gradient)
                if (colorMode == 1) {
                    y = content.getCurrentY();
                    float gradPower = renderConfig.getGodRaysGradientPower();
                    widgets.add(GuiWidgets.slider(x, y, halfW, 
                        "Grad Power", 0.1f, 5f, gradPower, "%.2f", 
                        "Gradient blend curve (1=linear)",
                        v -> renderConfig.setGodRaysGradientPower(v)));
                    content.advanceBy(22);
                }
                
                // Curvature mode
                y = content.getCurrentY();
                float curvatureMode = renderConfig.getGodRaysCurvatureMode();
                List<String> curvModes = List.of("Radial", "Vortex", "Spiral", "Tangential", "Pinwheel");
                int curvIdx = Math.max(0, Math.min((int)curvatureMode, curvModes.size() - 1));
                widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                    m -> Text.literal(m))
                    .values(curvModes).initially(curvModes.get(curvIdx))
                    .tooltip(m -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                        m.equals("Radial") ? "Straight rays from center" :
                        m.equals("Vortex") ? "Spinning whirlpool pattern" :
                        m.equals("Spiral") ? "Galaxy spiral arm pattern" :
                        m.equals("Tangential") ? "Perpendicular to radial" :
                        "Windmill blade pattern")))
                    .build(x, y, halfW, 20, Text.literal("Curvature"), (btn, mode) -> {
                        renderConfig.setGodRaysCurvatureMode(curvModes.indexOf(mode));
                    }));
                
                // Curvature strength slider
                float curvStrength = renderConfig.getGodRaysCurvatureStrength();
                widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 
                    "Strength", 0f, 2f, curvStrength, "%.2f", 
                    "How much to curve the rays",
                    v -> renderConfig.setGodRaysCurvatureStrength(v)));
                content.advanceBy(22);
                
                // Flicker mode
                y = content.getCurrentY();
                float flickerMode = renderConfig.getGodRaysFlickerMode();
                List<String> flickModes = List.of("None", "Scintillation", "Strobe", "Fade Pulse", "Heartbeat", "Lightning");
                int flickIdx = Math.max(0, Math.min((int)flickerMode, flickModes.size() - 1));
                widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                    m -> Text.literal(m))
                    .values(flickModes).initially(flickModes.get(flickIdx))
                    .tooltip(m -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                        m.equals("None") ? "No flicker" :
                        m.equals("Scintillation") ? "Star twinkling" :
                        m.equals("Strobe") ? "Rhythmic on/off" :
                        m.equals("Fade Pulse") ? "Smooth breathing" :
                        m.equals("Heartbeat") ? "Double-pulse rhythm" :
                        "Flash then fade")))
                    .build(x, y, halfW, 20, Text.literal("Flicker"), (btn, mode) -> {
                        renderConfig.setGodRaysFlickerMode(flickModes.indexOf(mode));
                    }));
                
                // Flicker intensity slider
                float flickIntensity = renderConfig.getGodRaysFlickerIntensity();
                widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 
                    "Intensity", 0f, 1f, flickIntensity, "%.2f", 
                    "How strong the flicker effect is",
                    v -> renderConfig.setGodRaysFlickerIntensity(v)));
                content.advanceBy(22);
                
                // Travel mode
                y = content.getCurrentY();
                float travelMode = renderConfig.getGodRaysTravelMode();
                List<String> travModes = List.of("None", "Scroll", "Chase", "Pulse Wave", "Comet");
                int travIdx = Math.max(0, Math.min((int)travelMode, travModes.size() - 1));
                widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                    m -> Text.literal(m))
                    .values(travModes).initially(travModes.get(travIdx))
                    .tooltip(m -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(
                        m.equals("None") ? "No travel animation" :
                        m.equals("Scroll") ? "Gradient scrolls outward" :
                        m.equals("Chase") ? "Dots travel along rays" :
                        m.equals("Pulse Wave") ? "Brightness pulses travel" :
                        "Bright head, fading tail")))
                    .build(x, y, halfW, 20, Text.literal("Travel"), (btn, mode) -> {
                        renderConfig.setGodRaysTravelMode(travModes.indexOf(mode));
                    }));
                
                // Travel speed slider
                float travSpeed = renderConfig.getGodRaysTravelSpeed();
                widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 
                    "Speed", 0f, 5f, travSpeed, "%.1f", 
                    "How fast energy travels along rays",
                    v -> renderConfig.setGodRaysTravelSpeed(v)));
                content.advanceBy(22);
            }
            content.advanceBy(22);
        }
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════════
        // SCHEMA-DRIVEN PARAMETERS (using GuiWidgets for callbacks)
        // ═══════════════════════════════════════════════════════════════════════════
        
        EffectType effectType = (EffectType) state.get("fieldVisual.effectType");
        if (effectType == null) effectType = EffectType.ENERGY_ORB;
        Integer version = (Integer) state.get("fieldVisual.version");
        if (version == null) version = 1;
        
        EffectSchema effectSchema = EffectSchemaRegistry.get(effectType, version);
        
        // Fallback: if schema is null, try version 1 for this effect type
        if (effectSchema == null) {
            List<Integer> available = EffectSchemaRegistry.versionsFor(effectType);
            if (!available.isEmpty()) {
                version = available.get(0);
                state.set("fieldVisual.version", version);
                effectSchema = EffectSchemaRegistry.get(effectType, version);
            }
        }
        
        // Schema always exists for V1-V4, but guard just in case
        if (effectSchema == null) {
            Logging.GUI.topic("panel").warn("No schema found for {} V{}", effectType, version);
            return;
        }
        
        for (String groupName : effectSchema.groupNames()) {
            // Skip General - handled in header section using getParameter() for override support
            if ("General".equals(groupName)) continue;
            
            List<ParameterSpec> params = effectSchema.getGroup(groupName);
            if (params.isEmpty()) continue;
            
            content.sectionHeader(groupName);
            
            // Colors group = 3 columns, everything else = 2 columns
            boolean useThreeColumns = "Colors".equals(groupName);
            buildParamsMultiColumn(content, params, x, w, halfW, thirdW, useThreeColumns);
        }
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // SPAWN ANIMATION
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Spawn Animation");
        y = content.getCurrentY();
        
        // Origin and Target Mode dropdowns
        var adapter = state.fieldVisualAdapter();
        var originModes = List.of(
            net.cyberpunk042.client.input.spawn.SpawnOriginMode.FROM_ABOVE,
            net.cyberpunk042.client.input.spawn.SpawnOriginMode.FROM_BELOW,
            net.cyberpunk042.client.input.spawn.SpawnOriginMode.FROM_HORIZON,
            net.cyberpunk042.client.input.spawn.SpawnOriginMode.FROM_SKY_HORIZON
        );
        var currentOrigin = adapter.buildSpawnConfig().originMode();
        System.out.println("[GUI] Origin button initial value: " + currentOrigin.name());
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<net.cyberpunk042.client.input.spawn.SpawnOriginMode>builder(
            m -> Text.literal(m.getDisplayName()))
            .values(originModes).initially(currentOrigin)
            .build(x, y, halfW, 20, Text.literal("Origin"), (btn, mode) -> {
                System.out.println("[GUI] Origin button callback: mode=" + mode.name());
                state.set("fieldVisual.spawnOriginMode", mode);
            }));
            
        var targetModes = List.of(
            net.cyberpunk042.client.input.spawn.TargetMode.RELATIVE,
            net.cyberpunk042.client.input.spawn.TargetMode.TRUE_TARGET
        );
        var currentTarget = adapter.buildSpawnConfig().targetMode();
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<net.cyberpunk042.client.input.spawn.TargetMode>builder(
            m -> Text.literal(m.getDisplayName()))
            .values(targetModes).initially(currentTarget)
            .build(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 20, Text.literal("Target"), (btn, mode) -> {
                state.set("fieldVisual.spawnTargetMode", mode);
                // When switching to TRUE_TARGET, initialize coords to player position
                if (mode == net.cyberpunk042.client.input.spawn.TargetMode.TRUE_TARGET) {
                    var c = MinecraftClient.getInstance();
                    if (c.player != null) {
                        state.set("fieldVisual.trueTargetX", c.player.getX());
                        state.set("fieldVisual.trueTargetY", c.player.getY());
                        state.set("fieldVisual.trueTargetZ", c.player.getZ());
                    }
                }
                rebuildContent();  // Refresh to show/hide true target coords
            }));
        content.advanceBy(22);
        
        // Target distance display (computed) + percent slider
        y = content.getCurrentY();
        // Get the already-computed target distance from buildSpawnConfig()
        float targetDist = adapter.buildSpawnConfig().targetDistance();
        // Get current percent for slider
        Object tdpVal = state.get("fieldVisual.targetDistancePercent");
        float targetPct = tdpVal instanceof Number n ? n.floatValue() : 50f;
        widgets.add(ButtonWidget.builder(Text.literal(String.format("Target: %.0f", targetDist)), btn -> {})
            .dimensions(x, y, halfW, 20).build());
        widgets.add(GuiWidgets.slider(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, "Reach", 0f, 100f, targetPct, "%.0f%%", null,
            v -> {
                state.set("fieldVisual.targetDistancePercent", v);
                rebuildContent();  // Update display
            }));
        content.advanceBy(22);
        
        // If TRUE_TARGET mode, show coordinate text inputs (default to player position)
        if (currentTarget == net.cyberpunk042.client.input.spawn.TargetMode.TRUE_TARGET) {
            y = content.getCurrentY();
            var client = MinecraftClient.getInstance();
            
            // Get current values or default to player position
            double px = client.player != null ? client.player.getX() : 0;
            double py = client.player != null ? client.player.getY() : 64;
            double pz = client.player != null ? client.player.getZ() : 0;
            
            net.minecraft.util.math.Vec3d trueTarget = adapter.buildSpawnConfig().trueTargetCoords();
            double tx = trueTarget != null ? trueTarget.x : px;
            double ty = trueTarget != null ? trueTarget.y : py;
            double tz = trueTarget != null ? trueTarget.z : pz;
            
            // X field
            var xField = new TextFieldWidget(client.textRenderer, x, y, thirdW, 18, Text.literal("X"));
            xField.setPlaceholder(Text.literal("X"));
            xField.setText(String.format("%.1f", tx));
            xField.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Target X coordinate (East/West)")));
            xField.setChangedListener(s -> {
                try { state.set("fieldVisual.trueTargetX", Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
            });
            widgets.add(xField);
            
            // Y field
            var yField = new TextFieldWidget(client.textRenderer, x + thirdW + 2, y, thirdW, 18, Text.literal("Y"));
            yField.setPlaceholder(Text.literal("Y"));
            yField.setText(String.format("%.1f", ty));
            yField.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Target Y coordinate (Height)")));
            yField.setChangedListener(s -> {
                try { state.set("fieldVisual.trueTargetY", Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
            });
            widgets.add(yField);
            
            // Z field
            var zField = new TextFieldWidget(client.textRenderer, x + thirdW * 2 + 4, y, thirdW, 18, Text.literal("Z"));
            zField.setPlaceholder(Text.literal("Z"));
            zField.setText(String.format("%.1f", tz));
            zField.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Target Z coordinate (North/South)")));
            zField.setChangedListener(s -> {
                try { state.set("fieldVisual.trueTargetZ", Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
            });
            widgets.add(zField);
            
            content.advanceBy(22);
        }
        
        // Travel time and easing
        y = content.getCurrentY();
        long travelMs = adapter.buildSpawnConfig().interpolationDurationMs();
        widgets.add(GuiWidgets.slider(x, y, halfW, "Travel Time", 1000f, 60000f, (float)travelMs, "%.0fms", null,
            v -> state.set("fieldVisual.spawnInterpolationDurationMs", v.longValue())));
        var easingCurves = List.of(
            net.cyberpunk042.client.input.spawn.EasingCurve.LINEAR,
            net.cyberpunk042.client.input.spawn.EasingCurve.EASE_IN,
            net.cyberpunk042.client.input.spawn.EasingCurve.EASE_OUT,
            net.cyberpunk042.client.input.spawn.EasingCurve.EASE_IN_OUT
        );
        var currentEasing = adapter.buildSpawnConfig().easingCurve();
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<net.cyberpunk042.client.input.spawn.EasingCurve>builder(
            e -> Text.literal(e.getDisplayName()))
            .values(easingCurves).initially(currentEasing)
            .build(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 20, Text.literal("Ease"), (btn, curve) -> {
                state.set("fieldVisual.spawnEasingCurve", curve);
            }));
        content.advanceBy(22);
        
        // Fade in/out and lifetime
        y = content.getCurrentY();
        long fadeIn = adapter.buildSpawnConfig().fadeInDurationMs();
        widgets.add(GuiWidgets.slider(x, y, thirdW, "Fade In", 0f, 5000f, (float)fadeIn, "%.0fms", null,
            v -> state.set("fieldVisual.spawnFadeInMs", v.longValue())));
        long fadeOut = adapter.buildSpawnConfig().fadeOutDurationMs();
        widgets.add(GuiWidgets.slider(x + thirdW + 2, y, thirdW, "Fade Out", 0f, 5000f, (float)fadeOut, "%.0fms", null,
            v -> state.set("fieldVisual.spawnFadeOutMs", v.longValue())));
        long lifetime = adapter.buildSpawnConfig().lifetimeMs();
        // Lifetime in seconds for readability (0 = infinite)
        widgets.add(GuiWidgets.slider(x + thirdW * 2 + 4, y, thirdW, "Life", 0f, 3600f, (float)(lifetime/1000), "%.0fs", null,
            v -> state.set("fieldVisual.spawnLifetimeMs", (long)(v * 1000f))));
        content.advanceBy(22);
        
        // Summon button
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("§b⚡ SUMMON ORB"), btn -> {
            System.out.println("[SPAWN] SUMMON button clicked!");
            
            // Multi-field rendering is now supported - each field gets its own shader
            // Preview orb can coexist with spawn orbs
            
            var config = state.fieldVisualAdapter().buildSpawnConfig();
            var visualConfig = state.fieldVisualAdapter().buildConfig();
            System.out.println("[SPAWN] SpawnConfig: origin=" + config.originMode() + ", spawnDist=" + config.spawnDistance() + ", targetDist=" + config.targetDistance());
            System.out.println("[SPAWN] VisualConfig: effectType=" + visualConfig.effectType() + ", version=" + visualConfig.reserved().version());
            
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.player != null) {
                net.minecraft.util.math.Vec3d referencePos;
                if (config.targetMode() == net.cyberpunk042.client.input.spawn.TargetMode.TRUE_TARGET && config.trueTargetCoords() != null) {
                    referencePos = config.trueTargetCoords();
                } else {
                    referencePos = client.player.getBoundingBox().getCenter();
                }
                System.out.println("[SPAWN] Calling OrbSpawnManager.spawnOrb with ref=" + referencePos);
                var orbId = net.cyberpunk042.client.input.spawn.OrbSpawnManager.spawnOrb(config, referencePos);
                System.out.println("[SPAWN] Spawned orb: " + orbId);
            } else {
                System.out.println("[SPAWN] client.player is null!");
            }
        }).dimensions(x, y, w, 20).build());
        content.advanceBy(22);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // ACTIONS
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Actions");
        y = content.getCurrentY();
        Float range = getFloat("fieldVisual.throwRange", 30f);
        widgets.add(GuiWidgets.slider(x, y, w, "Throw Range", 5f, 80f, range, "%.0f", null,
            v -> state.set("fieldVisual.throwRange", v)));
        content.advanceBy(24);
        
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("§6⚡ Throw Orb"), btn -> triggerThrowAnimation())
            .dimensions(x, y, halfW, 20).build());
        widgets.add(ButtonWidget.builder(Text.literal("Reset"), btn -> {
            state.fieldVisualAdapter().reset();  // reset() now applies schema defaults automatically
            rebuildContent();
            setScrollOffset(0);
        }).dimensions(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 20).build());
        content.advanceBy(22);
        
        contentHeight = content.getContentHeight();
    }
    
    /**
     * Builds parameters in multi-column layout.
     * @param useThreeColumns true for 3-column (colors), false for 2-column
     */
    private void buildParamsMultiColumn(ContentBuilder content, List<ParameterSpec> params, 
                                         int x, int w, int halfW, int thirdW, boolean useThreeColumns) {
        int colW = useThreeColumns ? thirdW : halfW;
        int cols = useThreeColumns ? 3 : 2;
        int gap = GuiConstants.COMPACT_GAP;
        int idx = 0;
        
        while (idx < params.size()) {
            int y = content.getCurrentY();
            int remaining = params.size() - idx;
            int rowCols = Math.min(cols, remaining);
            
            for (int c = 0; c < rowCols; c++) {
                int cx = x + c * (colW + gap);
                addParamWidget(params.get(idx++), cx, y, colW);
            }
            content.advanceBy(22);
        }
    }
    
    /**
     * Adds a single parameter widget using GuiWidgets (with syncToEffect callback).
     */
    private void addParamWidget(ParameterSpec param, int x, int y, int width) {
        String tooltip = param.tooltip() != null ? param.tooltip() : "";
        
        switch (param.type()) {
            case SLIDER, INT_SLIDER -> {
                Float val = getFloat(param.path(), param.defaultValue());
                String fmt = param.type() == ParameterSpec.ControlType.INT_SLIDER ? "%.0f" : "%.2f";
                final boolean isInt = param.type() == ParameterSpec.ControlType.INT_SLIDER;
                widgets.add(GuiWidgets.slider(x, y, width, param.label(), 
                    param.min(), param.max(), val, fmt, tooltip.isEmpty() ? null : tooltip,
                    v -> { 
                        float storeVal = isInt ? Math.round(v) : v;
                        state.set(param.path(), storeVal); 
                        syncToEffect(); 
                    }));
            }
            case STEPPED_SLIDER -> {
                Float val = getFloat(param.path(), param.defaultValue());
                widgets.add(GuiWidgets.sliderStepped(x, y, width, param.label(), 
                    param.min(), param.max(), param.step(), val, "%.2f", tooltip.isEmpty() ? null : tooltip,
                    v -> { 
                        state.set(param.path(), v); 
                        syncToEffect(); 
                    }));
            }
            case TOGGLE -> {
                // Handle both Boolean and Float storage (adapter uses Float)
                Object raw = state.get(param.path());
                boolean val;
                if (raw instanceof Boolean b) {
                    val = b;
                } else if (raw instanceof Number n) {
                    val = n.floatValue() > 0.5f;
                } else {
                    val = param.defaultValue() > 0.5f;
                }
                final boolean fVal = val;
                String label = fVal ? "§a" + param.label() : "§7" + param.label();
                widgets.add(GuiWidgets.toggle(x, y, width, label, fVal, tooltip,
                    v -> { 
                        // Store as Float for adapter compatibility (1.0 = on, 0.0 = off)
                        state.set(param.path(), v ? 1.0f : 0.0f); 
                        syncToEffect(); 
                        rebuildContent(); 
                    }));
            }
            default -> {
                // Unsupported type
            }
        }
    }
    
    private Float getFloat(String path, float def) {
        Object v = state.get(path);
        return v instanceof Number n ? n.floatValue() : def;
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    private void syncToEffect() {
        var client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            state.fieldVisualAdapter().syncToEffect(client.player.getBoundingBox().getCenter());
        }
    }
    
    private void triggerThrowAnimation() {
        var client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        var player = client.player;
        var playerPos = player.getBoundingBox().getCenter();
        var lookDir = player.getRotationVec(1.0f);
        var worldUp = new net.minecraft.util.math.Vec3d(0, 1, 0);
        var rightDir = lookDir.crossProduct(worldUp).normalize();
        if (rightDir.lengthSquared() < 0.01) rightDir = new net.minecraft.util.math.Vec3d(1, 0, 0);
        FieldVisualPostEffect.setThrowAdapter(state.fieldVisualAdapter());
        state.fieldVisualAdapter().triggerThrow(playerPos, lookDir, rightDir);
    }
    
    @Override
    public void dispose() {
        state.fieldVisualAdapter().clearPreview();
        super.dispose();
    }
}
