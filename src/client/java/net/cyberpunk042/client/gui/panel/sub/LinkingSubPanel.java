package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.minecraft.client.gui.screen.Screen;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * LinkingSubPanel - Manages primitive-to-primitive linking.
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for clean,
 * bidirectional state synchronization. When switching primitives or
 * loading profiles, all widgets automatically update to reflect the new state.</p>
 * 
 * <p><b>Link Options:</b></p>
 * <ul>
 *   <li>Target: Which primitive to link to</li>
 *   <li>Follow / RadiusMatch / ScaleWith: Position inheritance</li>
 *   <li>OrbitSync / Color / Alpha: Behavior inheritance</li>
 *   <li>Phase offsets: Timing relationships</li>
 * </ul>
 */
@RequiresFeature(Feature.LINKING)
public class LinkingSubPanel extends BoundPanel {

    private static final int COMPACT_HEIGHT = 16;
    private static final int GAP = 2;
    
    private final int startY;
    private List<String> availableTargetIds = new ArrayList<>();
    private CyclingButtonWidget<String> targetDropdown;
    
    public LinkingSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    @Override
    protected void buildContent() {
        buildAvailableTargets();
        
        // ═══════════════════════════════════════════════════════════════════════
        // TARGET SELECTION (special widget, not using ContentBuilder)
        // ═══════════════════════════════════════════════════════════════════════
        
        buildTargetDropdown();
        
        // ContentBuilder starts AFTER the target dropdown
        int contentStartY = startY + COMPACT_HEIGHT + GAP + GAP;
        ContentBuilder content = content(contentStartY);
        
        // ═══════════════════════════════════════════════════════════════════════
        // POSITION TOGGLES
        // Row: Follow | FollowDynamic | RadiusMatch | ScaleWith
        // ═══════════════════════════════════════════════════════════════════════
        
        content.gap();
        content.row()
            .toggle("Follow", "link.follow")
            .toggle("FollwDyn", "link.followDynamic")
            .toggle("RadMtch", "link.radiusMatch")
            .toggle("ScaleW", "link.scaleWith")
            .end();
        
        // ═══════════════════════════════════════════════════════════════════════
        // BEHAVIOR TOGGLES  
        // Row: OrbitSync | ColorMatch | AlphaMatch
        // ═══════════════════════════════════════════════════════════════════════
        
        content.row()
            .toggle("OrbitSync", "link.orbitSync")
            .toggle("Color", "link.colorMatch")
            .toggle("Alpha", "link.alphaMatch")
            .end();
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // PHASE OFFSETS (degrees display, 0-1 state storage)
        // Row: PhaseOffset | OrbitPhaseOffset
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sliderPairDegrees(
            "Phase°", "link.phaseOffset",
            "OrbPh°", "link.orbitPhaseOffset"
        );
        
        // ═══════════════════════════════════════════════════════════════════════
        // ORBIT PARAMETERS
        // Row: OrbitRadiusOffset | SpeedMultiplier
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "OrbRad", "link.orbitRadiusOffset", -10f, 10f,
            "SpdMul", "link.orbitSpeedMult", 0.1f, 10f
        );
        
        // ═══════════════════════════════════════════════════════════════════════
        // INCLINATION & PRECESSION (degrees)
        // Row: InclinationOffset | PrecessionOffset
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sliderPairDegrees(
            "Inc°", "link.orbitInclinationOffset",
            "Prec°", "link.orbitPrecessionOffset"
        );
        
        // ═══════════════════════════════════════════════════════════════════════
        // SHAPE RADIUS OFFSET (full width)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.slider("ShapeRadOff", "link.radiusOffset")
            .range(-10f, 10f)
            .format("%.1f")
            .add();
        
        contentHeight = content.getContentHeight();
    }
    
    /**
     * Builds the target dropdown widget (special case - not a standard binding).
     */
    private void buildTargetDropdown() {
        List<String> targetOptions = new ArrayList<>();
        targetOptions.add("(none)");
        targetOptions.addAll(availableTargetIds);
        
        String currentTarget = state.getString("link.target");
        String initialSelection = currentTarget != null ? currentTarget : "(none)";
        
        int xPosition = GuiConstants.PADDING;
        int widgetWidth = panelWidth - GuiConstants.PADDING * 2;
        int yPosition = startY + GAP;
        
        targetDropdown = CyclingButtonWidget.<String>builder(
                value -> Text.literal("Target: " + value))
            .values(targetOptions.toArray(new String[0]))
            .initially(initialSelection)
            .omitKeyText()
            .build(xPosition, yPosition, widgetWidth, COMPACT_HEIGHT, Text.literal(""),
                (button, selectedValue) -> {
                    String newTarget = "(none)".equals(selectedValue) ? null : selectedValue;
                    state.set("link.target", newTarget);
                    Logging.GUI.topic("link").debug("Target set to: {}", newTarget);
                });
        
        widgets.add(targetDropdown);
    }
    
    /**
     * Builds the list of available link targets (primitives before current one).
     */
    private void buildAvailableTargets() {
        availableTargetIds.clear();
        
        FieldLayer currentLayer = state.getSelectedLayer();
        if (currentLayer == null || currentLayer.primitives() == null) {
            return;
        }
        
        int currentPrimitiveIndex = state.getSelectedPrimitiveIndex();
        
        // Only primitives BEFORE the current one can be linked to
        for (int i = 0; i < currentPrimitiveIndex && i < currentLayer.primitives().size(); i++) {
            Primitive primitive = currentLayer.primitives().get(i);
            String primitiveId = primitive.id() != null ? primitive.id() : "prim_" + i;
            availableTargetIds.add(primitiveId);
        }
        
        Logging.GUI.topic("link").debug(
            "Available link targets: {} (current primitive index={})", 
            availableTargetIds.size(), 
            currentPrimitiveIndex
        );
    }
    
    @Override
    public void tick() {
        // No tick behavior needed
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int backgroundY = startY + scrollOffset;
        context.fill(
            bounds.x(), 
            backgroundY, 
            bounds.x() + panelWidth, 
            backgroundY + contentHeight, 
            GuiConstants.BG_PANEL
        );
        
        for (ClickableWidget widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
}
