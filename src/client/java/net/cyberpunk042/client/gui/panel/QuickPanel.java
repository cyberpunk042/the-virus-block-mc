package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ColorButton;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.visual.shape.ShapeRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * G41-G46: Quick customization panel (Level 1).
 * Contains only the most basic controls for quick adjustments.
 * Advanced features (spin, prediction, follow) are in the Advanced panel.
 */
public class QuickPanel extends AbstractPanel {
    
    private LayerPanel layerPanel;
    private PrimitivePanel primitivePanel;
    private CyclingButtonWidget<String> shapeDropdown;
    private LabeledSlider radiusSlider;
    private ColorButton colorButton;
    private LabeledSlider alphaSlider;
    private CyclingButtonWidget<FillMode> fillDropdown;
    
    private GuiLayout layout;
    
    public QuickPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        Logging.GUI.topic("panel").debug("QuickPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int startY = GuiConstants.TAB_HEIGHT + GuiConstants.PADDING;
        
        // Layer management row
        layerPanel = new LayerPanel(parent, state, startY);
        layerPanel.init(width, height);
        startY += layerPanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        // Primitive management row
        primitivePanel = new PrimitivePanel(parent, state, startY);
        primitivePanel.init(width, height);
        layerPanel.setOnLayerChanged(() -> primitivePanel.refreshForLayerChange());
        startY += primitivePanel.getHeight() + GuiConstants.SECTION_SPACING;
        
        this.layout = new GuiLayout(GuiConstants.PADDING, startY);
        
        int widgetWidth = Math.min(width - GuiConstants.PADDING * 2, GuiConstants.SLIDER_WIDTH);
        int x = layout.getX();
        
        // G42: Shape type - dynamic from ShapeRegistry
        List<String> shapeNames = new ArrayList<>(ShapeRegistry.names());
        shapeDropdown = CyclingButtonWidget.<String>builder(Text::literal)
            .values(shapeNames)
            .initially(state.getString("shapeType"))
            .tooltip(val -> Tooltip.of(Text.literal("Shape type")))
            .build(x, layout.nextRow(), widgetWidth, GuiConstants.WIDGET_HEIGHT, 
                   Text.literal("Shape"), (btn, val) -> onShapeChanged(val));
        
        // G43: Radius - uses current shape's radius
        radiusSlider = LabeledSlider.builder("Radius")
            .position(x, layout.nextRow())
            .width(widgetWidth)
            .range(0.1f, 20.0f)
            .initial(getShapeRadius(state.getString("shapeType")))
            .format("%.1f")
            .step(0.1f)
            .onChange(this::onRadiusChanged)
            .build();
        
        // G44: Color - uses primaryColor
        colorButton = ColorButton.create(x, layout.nextRow(), "Color", 
            state.getInt("appearance.primaryColor"), this::onColorChanged);
        // Wire up right-click to show color input modal
        if (parent instanceof net.cyberpunk042.client.gui.screen.FieldCustomizerScreen fcs) {
            colorButton.setRightClickHandler(() -> 
                fcs.showColorInputModal(colorButton.getColorString(), colorString -> {
                    colorButton.setColorString(colorString);
                }));
        }
        
        // G45: Alpha
        alphaSlider = LabeledSlider.builder("Alpha")
            .position(x, layout.nextRow())
            .width(widgetWidth)
            .range(0.0f, 1.0f)
            .initial(state.getFloat("appearance.alpha"))
            .format("%.2f")
            .onChange(this::onAlphaChanged)
            .build();
        
        // G46: Fill mode
        fillDropdown = GuiWidgets.enumDropdown(
            x, layout.nextRow(), widgetWidth,
            "Fill", FillMode.class, FillMode.valueOf(state.getString("fill.mode")), "Fill mode",
            this::onFillChanged
        );
        
        Logging.GUI.topic("panel").debug("QuickPanel initialized");
    }
    
    private void onShapeChanged(String type) { 
        state.set("shapeType", type);
        // Update radius slider to show the new shape's radius
        if (radiusSlider != null) {
            radiusSlider.setValue(getShapeRadius(type));
        }
    }
    
    private void onRadiusChanged(float v) { 
        // Set the radius on the current shape
        String shapeType = state.getString("shapeType").toLowerCase();
        switch (shapeType) {
            case "sphere" -> state.set("sphere.radius", v);
            case "disc" -> state.set("disc.radius", v);
            case "cylinder" -> state.set("cylinder.radius", v);
            case "prism" -> state.set("prism.radius", v);
            case "ring" -> state.set("ring.outerRadius", v);
            default -> state.set("sphere.radius", v);
        }
    }
    
    private float getShapeRadius(String shapeType) {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> state.getFloat("sphere.radius");
            case "disc" -> state.getFloat("disc.radius");
            case "cylinder" -> state.getFloat("cylinder.radius");
            case "prism" -> state.getFloat("prism.radius");
            case "ring" -> state.getFloat("ring.outerRadius");
            default -> state.getFloat("sphere.radius");
        };
    }
    
    private void onColorChanged(int c) { state.set("appearance.primaryColor", c); }
    private void onAlphaChanged(float v) { state.set("appearance.alpha", v); }
    private void onFillChanged(FillMode m) { 
        Logging.GUI.topic("panel").debug("[FILL-DEBUG] QuickPanel changing fill.mode to: {}", m.name());
        state.set("fill.mode", m); 
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, GuiConstants.TAB_HEIGHT, panelWidth, layout.getCurrentY() + GuiConstants.PADDING, GuiConstants.BG_PANEL);
        
        if (layerPanel != null) layerPanel.render(context, mouseX, mouseY, delta);
        if (primitivePanel != null) primitivePanel.render(context, mouseX, mouseY, delta);
        if (shapeDropdown != null) shapeDropdown.render(context, mouseX, mouseY, delta);
        if (radiusSlider != null) radiusSlider.render(context, mouseX, mouseY, delta);
        if (colorButton != null) colorButton.render(context, mouseX, mouseY, delta);
        if (alphaSlider != null) alphaSlider.render(context, mouseX, mouseY, delta);
        if (fillDropdown != null) fillDropdown.render(context, mouseX, mouseY, delta);
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (layerPanel != null) list.addAll(layerPanel.getWidgets());
        if (primitivePanel != null) list.addAll(primitivePanel.getWidgets());
        if (shapeDropdown != null) list.add(shapeDropdown);
        if (radiusSlider != null) list.add(radiusSlider);
        if (colorButton != null) list.add(colorButton);
        if (alphaSlider != null) list.add(alphaSlider);
        if (fillDropdown != null) list.add(fillDropdown);
        return list;
    }
}
