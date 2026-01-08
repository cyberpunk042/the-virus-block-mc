package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.Bound;
import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.CageOptionsAdapter;
import net.cyberpunk042.visual.fill.FillMode;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * FillSubPanel - Detailed fill mode options using ContentBuilder.
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for bidirectional
 * state synchronization. Widgets automatically update on profile load,
 * primitive switch, etc.</p>
 * 
 * <p>Widget visibility rules (implemented via ContentBuilder.when()):</p>
 * <ul>
 *   <li>Wire Thickness: CAGE, WIREFRAME modes</li>
 *   <li>See-Through: SOLID mode only</li>
 *   <li>Cage sliders: CAGE mode only</li>
 *   <li>Point Size: POINTS mode only</li>
 * </ul>
 */
public class FillSubPanel extends BoundPanel {
    
    private static final int COMPACT_H = 16;
    
    private final int startY;
    
    // Shape-aware cage options adapter
    private CageOptionsAdapter cageAdapter;
    
    // Fragment dropdown - manual widget (preset system)
    private CyclingButtonWidget<String> fragmentDropdown;
    private String currentFragment = "Custom";
    private boolean applyingFragment = false;
    
    public FillSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("FillSubPanel created (BoundPanel version)");
    }
    
    @Override
    protected void buildContent() {
        // Initialize adapter based on current shape
        cageAdapter = CageOptionsAdapter.forShape(state.getString("shapeType"), state.fill().cage());
        
        FillMode currentMode = state.fill().mode();
        ContentBuilder content = content(startY);
        
        // ═══════════════════════════════════════════════════════════════════════
        // FRAGMENT DROPDOWN (preset system - manual widget)
        // ═══════════════════════════════════════════════════════════════════════
        
        buildFragmentDropdown(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // FILL MODE DROPDOWN
        // ═══════════════════════════════════════════════════════════════════════
        
        buildModeDropdown(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // WIRE THICKNESS (CAGE, WIREFRAME)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(currentMode == FillMode.CAGE || currentMode == FillMode.WIREFRAME, c -> {
            c.slider("Wire", "fill.wireThickness")
                .range(0.1f, 2f)
                .format("%.1f")
                .add();
        });
        
        // ═══════════════════════════════════════════════════════════════════════
        // SEE-THROUGH (SOLID) - Note: state stores depthWrite, display is inverted
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(currentMode == FillMode.SOLID, c -> {
            // "See-Through" = !depthWrite - handled via manual toggle with inversion
            buildSeeThruToggle(c);
            // Double-sided rendering toggle
            buildDoubleSidedToggle(c);
        });
        
        // ═══════════════════════════════════════════════════════════════════════
        // CAGE OPTIONS (CAGE mode with count support)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(currentMode == FillMode.CAGE && cageAdapter.supportsCountOptions(), c -> {
            buildCageCountSliders(c);
        });
        
        content.when(currentMode == FillMode.CAGE && cageAdapter.hasPolyhedronExtras(), c -> {
            buildPolyhedronExtras(c);
        });
        
        // ═══════════════════════════════════════════════════════════════════════
        // POINT SIZE (POINTS)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(currentMode == FillMode.POINTS, c -> {
            c.slider("Point Size", "fill.pointSize")
                .range(1f, 10f)
                .format("%.1f")
                .add();
        });
        
        contentHeight = content.getContentHeight();
        Logging.GUI.topic("panel").debug("FillSubPanel built: {} widgets, mode={}", widgets.size(), currentMode);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MANUAL WIDGET BUILDERS (widgets that need special handling)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void buildFragmentDropdown(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        List<String> fillPresets = FragmentRegistry.listFillFragments();
        if (!applyingFragment) {
            currentFragment = "Custom";
        }
        
        fragmentDropdown = CyclingButtonWidget.<String>builder(v -> Text.literal(v))
            .values(fillPresets)
            .initially(currentFragment)
            .build(x, y, w, COMPACT_H, Text.literal("Variant"),
                (btn, val) -> {
                    Logging.GUI.topic("panel").info("Fragment: {}", val);
                    applyPreset(val);
                });
        widgets.add(fragmentDropdown);
        content.advanceRow();
    }
    
    private void buildModeDropdown(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        FillMode currentMode = state.fill().mode();
        
        CyclingButtonWidget<FillMode> modeDropdown = CyclingButtonWidget.<FillMode>builder(
                mode -> Text.literal("Mode: " + mode.name()))
            .values(FillMode.values())
            .initially(currentMode)
            .omitKeyText()
            .build(x, y, w, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    state.set("fill.mode", val);
                    Logging.GUI.topic("fill").info("Fill mode: {}", val.name());
                    rebuildContent();
                    notifyWidgetsChanged();
                });
        widgets.add(modeDropdown);
        
        // Register binding for sync
        bindings.add(new Bound<>(
            modeDropdown,
            () -> state.fill().mode(),
            v -> state.set("fill.mode", v),
            v -> v,
            v -> v,
            modeDropdown::setValue
        ));
        
        content.advanceRow();
    }
    
    private void buildSeeThruToggle(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        // Use GuiWidgets.toggle which works correctly
        CyclingButtonWidget<Boolean> toggle = net.cyberpunk042.client.gui.util.GuiWidgets.toggle(
            x, y, w, "See-Through",
            !state.fill().depthWrite(), "Enable see-through translucency",
            v -> state.set("fill.depthWrite", !v)
        );
        widgets.add(toggle);
        
        // Binding for sync from state
        bindings.add(new Bound<>(
            toggle,
            () -> !state.fill().depthWrite(),
            v -> state.set("fill.depthWrite", !v),
            v -> v,
            v -> v,
            toggle::setValue
        ));
        
        content.advanceRow();
    }
    
    private void buildDoubleSidedToggle(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        CyclingButtonWidget<Boolean> toggle = net.cyberpunk042.client.gui.util.GuiWidgets.toggle(
            x, y, w, "Double-Sided",
            state.fill().doubleSided(), "Render both front and back faces",
            v -> state.set("fill.doubleSided", v)
        );
        widgets.add(toggle);
        
        // Binding for sync from state
        bindings.add(new Bound<>(
            toggle,
            () -> state.fill().doubleSided(),
            v -> state.set("fill.doubleSided", v),
            v -> v,
            v -> v,
            toggle::setValue
        ));
        
        content.advanceRow();
    }
    
    private void buildCageCountSliders(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Primary count slider
        LabeledSlider primarySlider = LabeledSlider.builder(cageAdapter.primaryLabel())
            .position(x, y).width(halfW)
            .range(1, 128).initial(cageAdapter.primaryCount()).format("%d").step(1)
            .onChange(v -> {
                cageAdapter = cageAdapter.withPrimaryCount(v.intValue());
                updateCageInState();
            }).build();
        widgets.add(primarySlider);
        
        // Secondary count slider
        LabeledSlider secondarySlider = LabeledSlider.builder(cageAdapter.secondaryLabel())
            .position(x + halfW + GuiConstants.PADDING, y).width(halfW)
            .range(1, 256).initial(cageAdapter.secondaryCount()).format("%d").step(1)
            .onChange(v -> {
                cageAdapter = cageAdapter.withSecondaryCount(v.intValue());
                updateCageInState();
            }).build();
        widgets.add(secondarySlider);
        
        content.advanceRow();
    }
    
    private void buildPolyhedronExtras(ContentBuilder content) {
        // Use togglePair from ContentBuilder once cage paths are normalized
        // For now, manual due to CageOptionsAdapter pattern
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        CyclingButtonWidget<Boolean> allEdgesToggle = CyclingButtonWidget.<Boolean>builder(
                v -> Text.literal(v ? "§a✓ All Edges" : "§7 All Edges"))
            .values(true, false)
            .initially(cageAdapter.allEdges())
            .omitKeyText()
            .build(x, y, halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    cageAdapter = cageAdapter.withAllEdges(val);
                    updateCageInState();
                });
        widgets.add(allEdgesToggle);
        
        CyclingButtonWidget<Boolean> faceOutlinesToggle = CyclingButtonWidget.<Boolean>builder(
                v -> Text.literal(v ? "§a✓ Face Lines" : "§7 Face Lines"))
            .values(true, false)
            .initially(cageAdapter.faceOutlines())
            .omitKeyText()
            .build(x + halfW + GuiConstants.PADDING, y, halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    cageAdapter = cageAdapter.withFaceOutlines(val);
                    updateCageInState();
                });
        widgets.add(faceOutlinesToggle);
        
        content.advanceRow();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void updateCageInState() {
        var newFill = state.fill().toBuilder().cage(cageAdapter.build()).build();
        state.set("fill", newFill);
    }
    
    private void applyPreset(String name) {
        if ("Custom".equalsIgnoreCase(name)) {
            currentFragment = name;
            return;
        }
        applyingFragment = true;
        currentFragment = name;
        
        if ("Default".equalsIgnoreCase(name)) {
            state.set("fill", net.cyberpunk042.field.loader.DefaultsProvider.getDefaultFill());
        } else {
            FragmentRegistry.applyFillFragment(state, name);
        }
        
        rebuildContent();
        notifyWidgetsChanged();
        applyingFragment = false;
    }
    
    @Override
    protected boolean needsRebuildOnFragmentApply() {
        // Fill mode change requires new widgets
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use parent's scroll-aware rendering
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    /** Called when shape type changes - rebuilds widgets with new labels. */
    public void onShapeChanged() {
        rebuildContent();
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<ClickableWidget> getWidgets() {
        return new ArrayList<>(widgets);
    }
}
