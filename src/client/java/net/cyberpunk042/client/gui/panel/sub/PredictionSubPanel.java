package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.field.instance.FollowConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Sub-panel for follow/movement settings.
 * Uses BoundPanel for bidirectional state binding.
 */
@RequiresFeature(Feature.PREDICTION)
public class PredictionSubPanel extends BoundPanel {
    
    private int startY;
    private CyclingButtonWidget<FollowConfig.Preset> presetButton;
    private CyclingButtonWidget<Boolean> enabledButton;
    private FollowConfig.Preset currentPreset = FollowConfig.Preset.LOCKED;
    private boolean applyingPreset = false;
    
    public PredictionSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    @Override
    protected void buildContent() {
        ContentBuilder content = content(startY);
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        // Detect preset from current values
        detectCurrentPreset();
        
        // Preset button
        presetButton = CyclingButtonWidget.<FollowConfig.Preset>builder(
                p -> Text.literal(p.label() + " - " + p.description()))
            .values(FollowConfig.Preset.values())
            .initially(currentPreset)
            .build(x, content.getCurrentY(), w, GuiConstants.WIDGET_HEIGHT, 
                Text.literal("reset"), (btn, val) -> applyPreset(val));
        widgets.add(presetButton);
        content.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // Follow toggle
        enabledButton = CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Following" : "Static"))
            .values(List.of(false, true))
            .initially(state.getBool("follow.enabled"))
            .build(x, content.getCurrentY(), w, GuiConstants.WIDGET_HEIGHT,
                Text.literal("Follow"), (btn, val) -> {
                    state.set("follow.enabled", val);
                    markAsCustom();
                    updateSliderStates();
                });
        widgets.add(enabledButton);
        content.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // Sliders - binding handles state sync automatically
        content.slider("Lead/Trail", "follow.leadOffset").range(-1f, 1f).format("%.2f").add();
        content.slider("Responsiveness", "follow.responsiveness").range(0.1f, 1f).format("%.2f").add();
        content.slider("Look Ahead", "follow.lookAhead").range(0f, 0.5f).format("%.2f").add();
        
        contentHeight = content.getContentHeight();
        updateSliderStates();
    }
    
    private void detectCurrentPreset() {
        FollowConfig current = state.follow();
        if (current == null) { currentPreset = FollowConfig.Preset.LOCKED; return; }
        for (FollowConfig.Preset p : FollowConfig.Preset.values()) {
            if (p == FollowConfig.Preset.CUSTOM) continue;
            FollowConfig c = p.config();
            if (c != null && c.enabled() == current.enabled() &&
                Math.abs(c.leadOffset() - current.leadOffset()) < 0.01f &&
                Math.abs(c.responsiveness() - current.responsiveness()) < 0.01f &&
                Math.abs(c.lookAhead() - current.lookAhead()) < 0.01f) {
                currentPreset = p; return;
            }
        }
        currentPreset = FollowConfig.Preset.CUSTOM;
    }
    
    private void applyPreset(FollowConfig.Preset preset) {
        currentPreset = preset;
        if (preset == FollowConfig.Preset.CUSTOM) return;
        
        applyingPreset = true;
        FollowConfig cfg = preset.config();
        if (cfg != null) {
            state.set("follow.enabled", cfg.enabled());
            state.set("follow.leadOffset", cfg.leadOffset());
            state.set("follow.responsiveness", cfg.responsiveness());
            state.set("follow.lookAhead", cfg.lookAhead());
            syncAllFromState();
            if (enabledButton != null) enabledButton.setValue(cfg.enabled());
            updateSliderStates();
        }
        applyingPreset = false;
    }
    
    private void markAsCustom() {
        if (!applyingPreset && currentPreset != FollowConfig.Preset.CUSTOM) {
            currentPreset = FollowConfig.Preset.CUSTOM;
            if (presetButton != null) presetButton.setValue(FollowConfig.Preset.CUSTOM);
        }
    }
    
    private void updateSliderStates() {
        boolean enabled = state.getBool("follow.enabled");
        for (var b : bindings) {
            if (b.widget() instanceof net.cyberpunk042.client.gui.widget.LabeledSlider s) {
                s.active = enabled;
            }
        }
    }
    
    @Override
    protected void syncAllFromState() {
        super.syncAllFromState();
        if (enabledButton != null) enabledButton.setValue(state.getBool("follow.enabled"));
        detectCurrentPreset();
        if (presetButton != null) presetButton.setValue(currentPreset);
        updateSliderStates();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    @Override public void tick() {}
    public int getHeight() { return contentHeight; }
}
