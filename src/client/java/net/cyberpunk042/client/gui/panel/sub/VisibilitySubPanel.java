package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.visibility.MaskType;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Visibility mask and pattern controls.
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for clean,
 * bidirectional state synchronization.</p>
 * 
 * <p>Controls for visibility mask:</p>
 * <ul>
 *   <li>Variant - preset selection (fragments)</li>
 *   <li>Mask Type - FULL, BANDS, STRIPES, CHECKER, RADIAL, GRADIENT</li>
 *   <li>Count, Thickness - pattern parameters</li>
 *   <li>Offset, Feather - fine-tuning</li>
 *   <li>Invert, Animate - toggles</li>
 *   <li>Speed - animation speed</li>
 * </ul>
 */
public class VisibilitySubPanel extends BoundPanel {
    
    private int startY;
    private CyclingButtonWidget<String> variantDropdown;
    private String currentVariant = "Custom";
    private boolean applyingPreset = false;
    
    public VisibilitySubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("VisibilitySubPanel created");
    }
    
    @Override
    protected void buildContent() {
        ContentBuilder content = content(startY);
        
        // Get current mask type
        MaskType currentMask = state.mask() != null ? state.mask().mask() : MaskType.FULL;
        
        // ═══════════════════════════════════════════════════════════════════
        // VARIANT DROPDOWN (fragment presets)
        // ═══════════════════════════════════════════════════════════════════
        
        List<String> presets = FragmentRegistry.listVisibilityFragments();
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int y = content.getCurrentY();
        
        variantDropdown = CyclingButtonWidget.<String>builder(v -> Text.literal(v))
            .values(presets)
            .initially(currentVariant)
            .build(x, y, w, GuiConstants.WIDGET_HEIGHT, Text.literal("Variant"),
                (btn, val) -> applyPreset(val));
        widgets.add(variantDropdown);
        content.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // ═══════════════════════════════════════════════════════════════════
        // MASK TYPE DROPDOWN  
        // ═══════════════════════════════════════════════════════════════════
        
        content.dropdown("Mask Type", "mask.mask", MaskType.class);
        
        // ═══════════════════════════════════════════════════════════════════
        // CONDITIONAL CONTROLS (hidden when mask = FULL)
        // ═══════════════════════════════════════════════════════════════════
        
        boolean showPatternControls = currentMask != MaskType.FULL;
        boolean showBandControls = currentMask == MaskType.BANDS || 
                                    currentMask == MaskType.STRIPES || 
                                    currentMask == MaskType.CHECKER;
        
        if (showBandControls) {
            content.sliderPair(
                "Count", "mask.count", 1f, 128f,
                "Thick", "mask.thickness", 0.01f, 1f
            );
        }
        
        if (showPatternControls) {
            content.sliderPair(
                "Offset", "mask.offset", 0f, 1f,
                "Feather", "mask.feather", 0f, 1f
            );
            
            content.togglePair(
                "Invert", "mask.invert",
                "Animate", "mask.animate"
            );
            
            content.slider("Anim Speed", "mask.animSpeed")
                .range(0.1f, 10f)
                .format("%.1f")
                .add();
        }
        
        contentHeight = content.getContentHeight();
        
        Logging.GUI.topic("panel").debug("VisibilitySubPanel built with {} widgets (mask={})", 
            widgets.size(), currentMask);
    }
    
    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentVariant = name;
            return;
        }
        
        applyingPreset = true;
        currentVariant = name;
        
        if ("Default".equalsIgnoreCase(name)) {
            state.set("mask", net.cyberpunk042.field.loader.DefaultsProvider.getDefaultVisibility());
        } else {
            FragmentRegistry.applyVisibilityFragment(state, name);
        }
        
        // Rebuild to update conditional visibility based on new mask type
        rebuildContent();
        
        // Sync bindings to show new values
        syncAllFromState();
        
        ToastNotification.info("Visibility: " + name);
        applyingPreset = false;
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
