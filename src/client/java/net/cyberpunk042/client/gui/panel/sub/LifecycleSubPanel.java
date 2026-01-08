package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.field.influence.DecayConfig;
import net.cyberpunk042.field.influence.LifecycleConfig;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Lifecycle configuration controls.
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for clean,
 * bidirectional state synchronization.</p>
 * 
 * <p>Controls spawn/despawn animation timing:</p>
 * <ul>
 *   <li><b>Fade In</b> - ticks for alpha fade during SPAWNING</li>
 *   <li><b>Fade Out</b> - ticks for alpha fade during DESPAWNING</li>
 *   <li><b>Scale In</b> - ticks for scale animation during SPAWNING</li>
 *   <li><b>Scale Out</b> - ticks for scale animation during DESPAWNING</li>
 *   <li><b>Decay Rate</b> - per-tick decay multiplier (0.95 = 5% decay/tick)</li>
 *   <li><b>Decay Min</b> - minimum value decay stops at</li>
 * </ul>
 */
public class LifecycleSubPanel extends BoundPanel {
    
    private int startY;
    
    public LifecycleSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("LifecycleSubPanel created");
    }
    
    @Override
    protected void buildContent() {
        ContentBuilder content = content(startY);
        
        // ═══════════════════════════════════════════════════════════════════
        // SPAWN ANIMATIONS
        // ═══════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "Fade In", "lifecycle.fadeIn", 0f, 100f,
            "Scale In", "lifecycle.scaleIn", 0f, 100f
        );
        
        // ═══════════════════════════════════════════════════════════════════
        // DESPAWN ANIMATIONS
        // ═══════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "Fade Out", "lifecycle.fadeOut", 0f, 100f,
            "Scale Out", "lifecycle.scaleOut", 0f, 100f
        );
        
        // ═══════════════════════════════════════════════════════════════════
        // DECAY (active state gradual fade)
        // ═══════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "Decay Rate", "lifecycle.decay.rate", 0.90f, 1.00f,
            "Decay Min", "lifecycle.decay.min", 0.0f, 1.0f
        );
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════
        // PRESETS (custom buttons - not standard bindings)
        // ═══════════════════════════════════════════════════════════════════
        
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int btnW = (w - GuiConstants.PADDING * 2) / 3;
        int y = content.getCurrentY();
        
        ButtonWidget instantBtn = ButtonWidget.builder(Text.literal("Instant"), btn -> {
                applyPreset(LifecycleConfig.INSTANT);
                ToastNotification.info("Lifecycle: Instant");
            })
            .dimensions(x, y, btnW, GuiConstants.WIDGET_HEIGHT)
            .build();
        widgets.add(instantBtn);
        
        ButtonWidget smoothBtn = ButtonWidget.builder(Text.literal("Smooth"), btn -> {
                applyPreset(LifecycleConfig.DEFAULT);
                ToastNotification.info("Lifecycle: Smooth");
            })
            .dimensions(x + btnW + GuiConstants.PADDING, y, btnW, GuiConstants.WIDGET_HEIGHT)
            .build();
        widgets.add(smoothBtn);
        
        ButtonWidget resetBtn = ButtonWidget.builder(Text.literal("Reset"), btn -> {
                applyPreset(LifecycleConfig.DEFAULT);
                ToastNotification.info("Lifecycle reset to defaults");
            })
            .dimensions(x + (btnW + GuiConstants.PADDING) * 2, y, btnW, GuiConstants.WIDGET_HEIGHT)
            .build();
        widgets.add(resetBtn);
        
        contentHeight = y - startY + GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        Logging.GUI.topic("panel").debug("LifecycleSubPanel built with {} widgets", widgets.size());
    }
    
    private void applyPreset(LifecycleConfig preset) {
        state.set("lifecycle", preset);
        // Sync all bindings to reflect new values
        syncAllFromState();
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    public int getHeight() {
        return contentHeight;
    }
}
