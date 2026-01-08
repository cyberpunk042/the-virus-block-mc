package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.animation.Waveform;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * BeamSubPanel - Central beam configuration.
 * Uses BoundPanel for clean bidirectional state binding.
 * 
 * Controls: enabled, radii, height, glow, color, pulse settings.
 */
@RequiresFeature(Feature.BEAM)
public class BeamSubPanel extends BoundPanel {
    
    private int startY;
    private CyclingButtonWidget<String> variantDropdown;
    private CyclingButtonWidget<Boolean> enabledToggle;
    private CyclingButtonWidget<Boolean> pulseToggle;
    private CyclingButtonWidget<Waveform> waveformDropdown;
    private TextFieldWidget colorField;
    
    private String currentVariant = "Custom";
    private boolean applyingPreset = false;
    
    public BeamSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    @Override
    protected void buildContent() {
        ContentBuilder c = content(startY);
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // ═══════════════════════════════════════════════════════════════════
        // VARIANT PRESET
        // ═══════════════════════════════════════════════════════════════════
        List<String> presets = FragmentRegistry.listBeamFragments();
        variantDropdown = CyclingButtonWidget.<String>builder(v -> Text.literal(v))
            .values(presets)
            .initially(currentVariant)
            .build(x, c.getCurrentY(), w, GuiConstants.WIDGET_HEIGHT,
                Text.literal("Variant"), (btn, val) -> applyPreset(val));
        widgets.add(variantDropdown);
        c.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // ═══════════════════════════════════════════════════════════════════
        // BEAM ENABLED
        // ═══════════════════════════════════════════════════════════════════
        enabledToggle = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Beam: ON" : "Beam: OFF"))
            .values(List.of(false, true))
            .initially(state.getBool("beam.enabled"))
            .build(x, c.getCurrentY(), w, GuiConstants.WIDGET_HEIGHT,
                Text.literal("Beam"), (btn, val) -> {
                    state.set("beam.enabled", val);
                    markAsCustom();
                    updateWidgetStates();
                });
        widgets.add(enabledToggle);
        c.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // ═══════════════════════════════════════════════════════════════════
        // RADII (paired)
        // ═══════════════════════════════════════════════════════════════════
        c.sliderPair("Inner", "beam.innerRadius", 0.01f, 1f,
                     "Outer", "beam.outerRadius", 0.02f, 2f);
        
        // ═══════════════════════════════════════════════════════════════════
        // HEIGHT (full width)
        // ═══════════════════════════════════════════════════════════════════
        c.slider("Height", "beam.height").range(0.5f, 64f).format("%.1f").add();
        
        // ═══════════════════════════════════════════════════════════════════
        // GLOW + COLOR (side by side)
        // ═══════════════════════════════════════════════════════════════════
        // Glow slider (half width - manually positioned)
        c.slider("Glow", "beam.glow").range(0f, 2f).format("%.2f").add();
        
        // Color text field
        int colorY = c.getCurrentY();
        colorField = new TextFieldWidget(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            x, colorY, w, GuiConstants.WIDGET_HEIGHT,
            Text.literal("Color"));
        String beamColor = state.getString("beam.color");
        colorField.setText(beamColor != null ? beamColor : "@beam");
        colorField.setChangedListener(v -> { state.set("beam.color", v); markAsCustom(); });
        widgets.add(colorField);
        c.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING);
        
        // ═══════════════════════════════════════════════════════════════════
        // PULSE SECTION
        // ═══════════════════════════════════════════════════════════════════
        PulseConfig pulse = state.beam() != null ? state.beam().pulse() : null;
        boolean pulseActive = pulse != null && pulse.isActive();
        
        pulseToggle = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Pulse: ON" : "Pulse: OFF"))
            .values(List.of(false, true))
            .initially(pulseActive)
            .build(x, c.getCurrentY(), w, GuiConstants.WIDGET_HEIGHT,
                Text.literal("Pulse"), (btn, val) -> {
                    if (!val) state.set("beam.pulse", PulseConfig.NONE);
                    else if (state.beam() == null || state.beam().pulse() == null) 
                        state.set("beam.pulse", PulseConfig.DEFAULT);
                    markAsCustom();
                    updateWidgetStates();
                });
        widgets.add(pulseToggle);
        c.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // Pulse scale + speed
        c.sliderPair("Scale", "beam.pulse.scale", 0f, 1f,
                     "Speed", "beam.pulse.speed", 0f, 5f);
        
        // Waveform dropdown
        Waveform currentWave = pulse != null ? pulse.waveform() : Waveform.SINE;
        waveformDropdown = CyclingButtonWidget.<Waveform>builder(wf -> Text.literal(wf.name()))
            .values(Waveform.values())
            .initially(currentWave)
            .build(x, c.getCurrentY(), w, GuiConstants.WIDGET_HEIGHT,
                Text.literal("Waveform"), (btn, val) -> {
                    state.set("beam.pulse.waveform", val.name());
                    markAsCustom();
                });
        widgets.add(waveformDropdown);
        c.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // Pulse min + max
        c.sliderPair("Min", "beam.pulse.min", 0f, 1f,
                     "Max", "beam.pulse.max", 0f, 2f);
        
        contentHeight = c.getContentHeight();
        updateWidgetStates();
    }
    
    private void updateWidgetStates() {
        boolean beamEnabled = state.getBool("beam.enabled");
        PulseConfig pulse = state.beam() != null ? state.beam().pulse() : null;
        boolean pulseEnabled = pulse != null && pulse.isActive();
        
        // Disable all beam controls when beam is off
        for (var b : bindings) {
            String path = ""; // Would need to expose path from Bound for precise control
            b.widget().active = beamEnabled;
        }
        
        // Pulse controls need both beam AND pulse enabled
        if (waveformDropdown != null) waveformDropdown.active = beamEnabled && pulseEnabled;
    }
    
    private void markAsCustom() {
        if (!applyingPreset && !"Custom".equals(currentVariant)) {
            currentVariant = "Custom";
            if (variantDropdown != null) variantDropdown.setValue("Custom");
        }
    }
    
    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) { currentVariant = name; return; }
        
        applyingPreset = true;
        currentVariant = name;
        FragmentRegistry.applyBeamFragment(state, name);
        syncAllFromState();
        applyingPreset = false;
    }
    
    @Override
    protected void syncAllFromState() {
        super.syncAllFromState();
        
        if (enabledToggle != null) enabledToggle.setValue(state.getBool("beam.enabled"));
        
        PulseConfig pulse = state.beam() != null ? state.beam().pulse() : null;
        if (pulseToggle != null) pulseToggle.setValue(pulse != null && pulse.isActive());
        if (waveformDropdown != null) waveformDropdown.setValue(pulse != null ? pulse.waveform() : Waveform.SINE);
        
        if (colorField != null) {
            String c = state.getString("beam.color");
            colorField.setText(c != null ? c : "@beam");
        }
        
        updateWidgetStates();
    }
    
    @Override
    public void onStateChanged(net.cyberpunk042.client.gui.state.ChangeType changeType) {
        switch (changeType) {
            case PROFILE_LOADED, FULL_RESET -> rebuildContent();
            default -> super.onStateChanged(changeType);
        }
    }
    
    @Override
    protected boolean needsRebuildOnFragmentApply() {
        return true; // Beam fragments may change pulse structure
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    @Override public void tick() {}
    public int getHeight() { return contentHeight; }
}
