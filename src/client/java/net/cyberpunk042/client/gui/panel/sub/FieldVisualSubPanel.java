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
        
        // Get current effect type for filtering presets
        EffectType presetFilterType = (EffectType) state.get("fieldVisual.effectType");
        if (presetFilterType == null) presetFilterType = EffectType.ENERGY_ORB;
        
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
                    if (!"Default".equals(value) && !"Custom".equals(value)) {
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
            v -> { state.set("fieldVisual.enabled", v); rebuildContent(); }));
        
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
        y = content.getCurrentY();
        Float intensity = getFloat("fieldVisual.intensity", 1.2f);
        Float speed = getFloat("fieldVisual.animationSpeed", 1.0f);
        widgets.add(GuiWidgets.slider(x, y, thirdW, "Intensity", 0f, 5f, intensity, "%.2f", null,
            v -> { state.set("fieldVisual.intensity", v); syncToEffect(); }));
        widgets.add(GuiWidgets.slider(x2, y, thirdW, "Speed", 0.01f, 10f, speed, "%.2f", null,
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
                state.applyFieldVisualSchemaDefaults();
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
            Float radius = getFloat("fieldVisual.previewRadius", 3f);
            widgets.add(GuiWidgets.slider(x3, y, thirdW, "Radius", 0.01f, 50f, radius, "%.2f", null,
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
            if ("General".equals(groupName)) continue; // Already in header
            
            List<ParameterSpec> params = effectSchema.getGroup(groupName);
            if (params.isEmpty()) continue;
            
            content.sectionHeader(groupName);
            
            // Colors group = 3 columns, everything else = 2 columns
            boolean useThreeColumns = "Colors".equals(groupName);
            buildParamsMultiColumn(content, params, x, w, halfW, thirdW, useThreeColumns);
        }
        
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
