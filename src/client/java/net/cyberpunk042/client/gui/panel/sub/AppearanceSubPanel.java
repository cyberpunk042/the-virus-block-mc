package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.ColorButton;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * G71-G80: Appearance controls for the Advanced panel.
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for clean,
 * bidirectional state synchronization.</p>
 * 
 * <p>Controls for visual appearance:</p>
 * <ul>
 *   <li>G71: Glow intensity slider (0.0 - 2.0)</li>
 *   <li>G72: Emissive intensity slider (0.0 - 1.0)</li>
 *   <li>G73: Saturation modifier (0.0 to 2.0)</li>
 *   <li>G74: Brightness modifier (0.0 to 2.0)</li>
 *   <li>G75: Hue shift slider (0 - 360 degrees)</li>
 *   <li>G76: Alpha min/max sliders</li>
 *   <li>G77: Primary color button with picker</li>
 *   <li>G78: Secondary color button (gradient end)</li>
 *   <li>G79: Color blend slider (0.0 - 1.0)</li>
 * </ul>
 */
public class AppearanceSubPanel extends BoundPanel {
    
    private int startY;
    
    // Color buttons (special widgets, not standard bindings)
    private ColorButton primaryColorBtn;
    private ColorButton secondaryColorBtn;
    
    public AppearanceSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("AppearanceSubPanel created");
    }
    
    @Override
    protected void buildContent() {
        ContentBuilder content = content(startY);
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 1: Glow + Emissive
        // ═══════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "Glow", "appearance.glow", 0f, 2f,
            "Emissive", "appearance.emissive", 0f, 1f
        );
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 2: Saturation + Brightness
        // ═══════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "Sat", "appearance.saturation", 0f, 2f,
            "Bright", "appearance.brightness", 0f, 2f
        );
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 3: Hue Shift (full width)
        // ═══════════════════════════════════════════════════════════════════
        
        content.slider("Hue Shift", "appearance.hueShift")
            .range(0f, 360f)
            .format("%.0f°")
            .add();
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 4: Alpha Min + Alpha Max
        // ═══════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "α Min", "appearance.alphaMin", 0f, 1f,
            "α Max", "appearance.alphaMax", 0f, 1f
        );
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 5: Color Buttons (special widgets)
        // ═══════════════════════════════════════════════════════════════════
        
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;
        int y = content.getCurrentY();
        
        primaryColorBtn = new ColorButton(x, y, halfW, "Primary", 
            state.getInt("appearance.primaryColor"), 
            color -> state.set("appearance.primaryColor", color));
        // Wire up right-click for color input modal
        if (parent instanceof net.cyberpunk042.client.gui.screen.FieldCustomizerScreen fcs) {
            primaryColorBtn.setRightClickHandler(() -> 
                fcs.showColorInputModal(primaryColorBtn.getColorString(), colorString -> {
                    primaryColorBtn.setColorString(colorString);
                }));
        }
        widgets.add(primaryColorBtn);
        
        secondaryColorBtn = new ColorButton(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, "Secondary", 
            state.getInt("appearance.secondaryColor"), 
            color -> state.set("appearance.secondaryColor", color));
        // Wire up right-click for color input modal
        if (parent instanceof net.cyberpunk042.client.gui.screen.FieldCustomizerScreen fcs) {
            secondaryColorBtn.setRightClickHandler(() -> 
                fcs.showColorInputModal(secondaryColorBtn.getColorString(), colorString -> {
                    secondaryColorBtn.setColorString(colorString);
                }));
        }
        widgets.add(secondaryColorBtn);
        
        // Advance past the color buttons (they use WIDGET_HEIGHT, not COMPACT_HEIGHT)
        content.advanceBy(GuiConstants.WIDGET_HEIGHT + GuiConstants.COMPACT_GAP);
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 6: Color Blend (full width)
        // ═══════════════════════════════════════════════════════════════════
        
        content.slider("Color Blend", "appearance.colorBlend")
            .range(0f, 1f)
            .format("%.2f")
            .add();
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 7: Color Mode dropdown
        // ═══════════════════════════════════════════════════════════════════
        
        content.dropdown("Color Mode", "appearance.colorMode", 
            net.cyberpunk042.visual.appearance.ColorMode.class);
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 8: Color Distribution (UNIFORM / PER_CELL)
        // ═══════════════════════════════════════════════════════════════════
        
        content.dropdown("Distribution", "appearance.colorDistribution", 
            net.cyberpunk042.visual.appearance.ColorDistribution.class);
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 8b: Blend Mode (NORMAL / ADD / MULTIPLY / SCREEN)
        // ═══════════════════════════════════════════════════════════════════
        
        content.dropdown("Blend Mode", "appearance.blendMode", 
            net.cyberpunk042.visual.layer.BlendMode.class);
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 9: Color Set (for CYCLING and RANDOM modes)
        // ═══════════════════════════════════════════════════════════════════
        
        content.dropdown("Color Set", "appearance.colorSet", 
            net.cyberpunk042.visual.appearance.ColorSet.class);
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 10: Gradient Direction (for MESH_* modes)
        // ═══════════════════════════════════════════════════════════════════
        
        content.dropdown("Direction", "appearance.gradientDirection", 
            net.cyberpunk042.visual.appearance.GradientDirection.class);
        
        // ═══════════════════════════════════════════════════════════════════
        // ROW 11: Time Phase (animation offset for CYCLING and MESH_RAINBOW)
        // ═══════════════════════════════════════════════════════════════════
        
        content.slider("Time Phase", "appearance.timePhase")
            .range(0f, 1f)
            .format("%.2f")
            .add();
        
        contentHeight = content.getContentHeight();
        
        Logging.GUI.topic("panel").debug("AppearanceSubPanel built with {} widgets", widgets.size());
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
