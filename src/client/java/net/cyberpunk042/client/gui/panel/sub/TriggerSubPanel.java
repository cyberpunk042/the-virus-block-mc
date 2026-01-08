package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.field.influence.FieldEvent;
import net.cyberpunk042.field.influence.TriggerConfig;
import net.cyberpunk042.field.influence.TriggerEffect;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * G86-G90: Trigger configuration for the Debug panel.
 * 
 * <ul>
 *   <li>G86: Trigger type dropdown (DAMAGE, HEAL, DEATH, RESPAWN)</li>
 *   <li>G87: Trigger effect dropdown (FLASH, PULSE, SHAKE, GLOW, COLOR_SHIFT)</li>
 *   <li>G88: Effect intensity slider</li>
 *   <li>G89: Effect duration slider</li>
 *   <li>G90: Test trigger button</li>
 * </ul>
 * 
 * <p><b>Requires Accurate renderer mode.</b></p>
 */
@RequiresFeature(Feature.TRIGGERS)
public class TriggerSubPanel extends AbstractPanel {
    
    private int startY;    
    private CyclingButtonWidget<FieldEvent> triggerType;
    private CyclingButtonWidget<TriggerEffect> triggerEffect;
    private LabeledSlider effectIntensity;
    private LabeledSlider effectDuration;
    private net.minecraft.client.gui.widget.ButtonWidget testBtn;
    
    public TriggerSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("TriggerSubPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int w = width - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // G86: Trigger type (using actual FieldEvent enum)
        triggerType = GuiWidgets.enumDropdown(
            x, y, halfW, "Event", FieldEvent.class, FieldEvent.PLAYER_DAMAGE,
            "Trigger event type", t -> {
                state.set("triggerType", t.name());
                Logging.GUI.topic("trigger").debug("Trigger type: {}", t);
            }
        );
        widgets.add(triggerType);
        
        // G87: Trigger effect (using actual TriggerEffect enum)
        triggerEffect = GuiWidgets.enumDropdown(
            x + halfW + GuiConstants.PADDING, y, halfW, "Effect", TriggerEffect.class, TriggerEffect.FLASH,
            "Visual effect", e -> {
                state.set("triggerEffect", e.name());
                Logging.GUI.topic("trigger").debug("Trigger effect: {}", e);
            }
        );
        widgets.add(triggerEffect);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G88: Effect intensity
        effectIntensity = LabeledSlider.builder("Intensity")
            .position(x, y).width(halfW)
            .range(0f, 2f).initial(state.getFloat("triggerIntensity")).format("%.2f")
            .onChange(v -> {
                state.set("triggerIntensity", v);
                Logging.GUI.topic("trigger").trace("Intensity: {}", v);
            })
            .build();
        widgets.add(effectIntensity);
        
        // G89: Effect duration
        effectDuration = LabeledSlider.builder("Duration (ticks)")
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(1, 60).initial(state.getFloat("triggerDuration")).format("%d").step(1)
            .onChange(v -> {
                state.set("triggerDuration", v.intValue());
                Logging.GUI.topic("trigger").trace("Duration: {}", v.intValue());
            })
            .build();
        widgets.add(effectDuration);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // G90: Test button - fires the trigger on the preview field
        testBtn = GuiWidgets.button(x, y, w, "Test Trigger", "Fire test trigger effect", this::fireTestTrigger);
        widgets.add(testBtn);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        
        Logging.GUI.topic("panel").debug("TriggerSubPanel initialized");
    }
    
    /**
     * Fires a test trigger on the preview field.
     */
    private void fireTestTrigger() {
        FieldEvent event = triggerType.getValue();
        TriggerEffect effect = triggerEffect.getValue();
        float intensity = state.getFloat("triggerIntensity");
        int duration = state.getInt("triggerDuration");
        
        // Create trigger config
        TriggerConfig config = new TriggerConfig(
            event, effect, duration,
            null, // color (use default)
            intensity, // scale
            0.1f, // amplitude
            intensity // intensity
        );
        
        // Fire the trigger on the preview field via state
        state.fireTestTrigger(config);
        
        Logging.GUI.topic("trigger").info("Test trigger fired: {} -> {} (intensity={}, duration={})", 
            event, effect, intensity, duration);
        ToastNotification.info("Trigger: " + effect + " (" + duration + " ticks)");
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets directly (no expandable section)
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> all = new ArrayList<>();
        // No header button (direct content)
        all.addAll(widgets);
        return all;
    }
}
