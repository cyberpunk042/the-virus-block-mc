package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.RendererCapabilities.Feature;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.minecraft.client.gui.screen.Screen;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiLayout;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.field.influence.BindingSources;
import net.cyberpunk042.field.influence.InterpolationCurve;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug sub-panel for property bindings.
 * Allows binding field properties to dynamic sources (player stats, etc).
 * 
 * <p>Uses the actual {@link BindingSources} registry for available sources.</p>
 * 
 * @see RequiresFeature
 * 
 * <p>WARNING: This is a Level 3 Debug feature.
 * Requires: debugMenuEnabled=true AND operator permission >= 2</p>
 * 
 * <p><b>Requires Accurate renderer mode.</b></p>
 * 
 * <h2>Binding Structure</h2>
 * <ul>
 *   <li>property: The field property to bind (e.g., "alpha", "glow", "scale")</li>
 *   <li>source: From BindingSources (e.g., "player.health_percent", "player.speed")</li>
 *   <li>inputRange: The source value range to map from</li>
 *   <li>outputRange: The property value range to map to</li>
 *   <li>curve: Interpolation curve from InterpolationCurve</li>
 * </ul>
 * 
 * @see BindingSources
 * @see net.cyberpunk042.field.influence.BindingConfig
 */
@RequiresFeature(Feature.BINDINGS)
public class BindingsSubPanel extends AbstractPanel {
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUMS - Property targets (what to bind TO)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Bindable field properties. */
    public enum BindableProperty {
        ALPHA("alpha", "Alpha", 0f, 1f),
        GLOW("glow", "Glow", 0f, 1f),
        SCALE("scale", "Scale", 0.1f, 10f),
        SPIN_SPEED("spin.speed", "Spin Speed", -0.5f, 0.5f),
        RADIUS("radius", "Radius", 0.1f, 20f),
        HUE_SHIFT("hueShift", "Hue Shift", 0f, 360f),
        SATURATION("saturation", "Saturation", 0f, 1f);
        
        private final String path;
        private final String displayName;
        public final float minValue;
        public final float maxValue;
        
        BindableProperty(String path, String displayName, float min, float max) {
            this.path = path;
            this.displayName = displayName;
            this.minValue = min;
            this.maxValue = max;
        }
        
        public String getPath() { return path; }
        public String getDisplayName() { return displayName; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private GuiLayout layout;
    
    // Available sources from BindingSources registry
    private List<String> availableSources;
    
    // Current binding being edited
    private BindableProperty selectedProperty = BindableProperty.ALPHA;
    private String selectedSource;
    private InterpolationCurve selectedCurve = InterpolationCurve.LINEAR;
    private float inputMin = 0f;
    private float inputMax = 1f;
    private float outputMin = 0f;
    private float outputMax = 1f;
    
    // Widgets
    private CyclingButtonWidget<BindableProperty> propertyButton;
    private CyclingButtonWidget<String> sourceButton;
    private CyclingButtonWidget<InterpolationCurve> curveButton;
    private LabeledSlider inputMinSlider;
    private LabeledSlider inputMaxSlider;
    private LabeledSlider outputMinSlider;
    private LabeledSlider outputMaxSlider;
    private ButtonWidget addBindingButton;
    private ButtonWidget removeBindingButton;

    // Binding list state
    private int selectedBindingIndex = -1;
    private static final int BINDING_ITEM_HEIGHT = 16;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new BindingsSubPanel.
     * 
     * @param state The GUI state to bind to
     * @param x Starting X position
     * @param y Starting Y position
     * @param width Panel width
     */
    public BindingsSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
    }
    
    private int startY;
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        int y = startY;
        this.layout = new GuiLayout(x, y, net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + net.cyberpunk042.client.gui.util.GuiConstants.PADDING, width - net.cyberpunk042.client.gui.util.GuiConstants.PADDING * 2);
        
        // Original build logic...
        // Get sources from the REAL registry
        this.availableSources = new ArrayList<>(BindingSources.getAvailableIds());
        this.availableSources.sort(String::compareTo);
        this.selectedSource = availableSources.isEmpty() ? "" : availableSources.get(0);
        
        initWidgets();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initWidgets() {
        int controlWidth = layout.getPanelWidth() - GuiConstants.PADDING * 2;
        int halfWidth = (controlWidth - GuiConstants.ELEMENT_SPACING) / 2;
        
        // ─────────────────────────────────────────────────────────────────────────
        // Property Selector - which field property to bind
        // ─────────────────────────────────────────────────────────────────────────
        propertyButton = CyclingButtonWidget.<BindableProperty>builder(p -> Text.literal(p.getDisplayName()))
            .values(BindableProperty.values())
            .initially(selectedProperty)
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                layout.getY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Property"),
                (button, value) -> {
                    selectedProperty = value;
                    updateOutputSliderRanges();
                }
            );
        widgets.add(propertyButton);
        
        // ─────────────────────────────────────────────────────────────────────────
        // Source Selector - from BindingSources registry
        // ─────────────────────────────────────────────────────────────────────────
        sourceButton = CyclingButtonWidget.<String>builder(s -> Text.literal(formatSourceName(s)))
            .values(availableSources)
            .initially(selectedSource)
            .build(
                layout.getStartX() + GuiConstants.PADDING + halfWidth + GuiConstants.ELEMENT_SPACING,
                layout.getY(),
                halfWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Source"),
                (button, value) -> {
                    selectedSource = value;
                    updateInputSliderRanges();
                }
            );
        widgets.add(sourceButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Curve Selector - using real InterpolationCurve enum
        // ─────────────────────────────────────────────────────────────────────────
        curveButton = CyclingButtonWidget.<InterpolationCurve>builder(c -> Text.literal(c.name()))
            .values(InterpolationCurve.values())
            .initially(selectedCurve)
            .build(
                layout.getStartX() + GuiConstants.PADDING,
                layout.getY(),
                controlWidth,
                GuiConstants.ELEMENT_HEIGHT,
                Text.literal("Curve"),
                (button, value) -> selectedCurve = value
            );
        widgets.add(curveButton);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Input Range - source value range
        // ─────────────────────────────────────────────────────────────────────────
        inputMinSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            layout.getY(),
            halfWidth,
            "In Min",
            0f, 100f, // Will be updated based on source
            inputMin,
            "%.2f", null,
            v -> inputMin = v
        );
        widgets.add(inputMinSlider);
        
        inputMaxSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING + halfWidth + GuiConstants.ELEMENT_SPACING,
            layout.getY(),
            halfWidth,
            "In Max",
            0f, 100f,
            inputMax,
            "%.2f", null,
            v -> inputMax = v
        );
        widgets.add(inputMaxSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Output Range - property value range
        // ─────────────────────────────────────────────────────────────────────────
        outputMinSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING,
            layout.getY(),
            halfWidth,
            "Out Min",
            selectedProperty.minValue, selectedProperty.maxValue,
            outputMin,
            "%.2f", null,
            v -> outputMin = v
        );
        widgets.add(outputMinSlider);
        
        outputMaxSlider = new LabeledSlider(
            layout.getStartX() + GuiConstants.PADDING + halfWidth + GuiConstants.ELEMENT_SPACING,
            layout.getY(),
            halfWidth,
            "Out Max",
            selectedProperty.minValue, selectedProperty.maxValue,
            outputMax,
            "%.2f", null,
            v -> outputMax = v
        );
        widgets.add(outputMaxSlider);
        layout.nextRow();
        
        // ─────────────────────────────────────────────────────────────────────────
        // Add/Remove Binding Buttons
        // ─────────────────────────────────────────────────────────────────────────
        addBindingButton = ButtonWidget.builder(Text.literal("+ Add"), button -> {
            addCurrentBinding();
            selectedBindingIndex = state.getBindings().size() - 1;
        }).dimensions(
            layout.getStartX() + GuiConstants.PADDING,
            layout.getY(),
            halfWidth,
            GuiConstants.ELEMENT_HEIGHT
        ).build();
        widgets.add(addBindingButton);

        removeBindingButton = ButtonWidget.builder(Text.literal("- Remove"), button -> {
            removeSelectedBinding();
        }).dimensions(
            layout.getStartX() + GuiConstants.PADDING + halfWidth + GuiConstants.ELEMENT_SPACING,
            layout.getY(),
            halfWidth,
            GuiConstants.ELEMENT_HEIGHT
        ).build();
        widgets.add(removeBindingButton);
        layout.nextRow();

        // Initialize ranges
        updateInputSliderRanges();
        updateOutputSliderRanges();
        updateRemoveButtonState();
    }

    /**
     * Removes the currently selected binding.
     */
    private void removeSelectedBinding() {
        var bindings = state.getBindings();
        if (selectedBindingIndex >= 0 && selectedBindingIndex < bindings.size()) {
            String property = bindings.get(selectedBindingIndex).property();
            state.removeBinding(property);
            selectedBindingIndex = Math.min(selectedBindingIndex, state.getBindings().size() - 1);
            updateRemoveButtonState();
        }
    }

    /**
     * Updates remove button state based on selection.
     */
    private void updateRemoveButtonState() {
        if (removeBindingButton != null) {
            removeBindingButton.active = selectedBindingIndex >= 0 && selectedBindingIndex < state.getBindings().size();
        }
    }

    /**
     * Loads the selected binding into the form for editing.
     */
    private void loadBindingToForm(net.cyberpunk042.field.influence.BindingConfig binding) {
        // Find matching property enum
        for (BindableProperty prop : BindableProperty.values()) {
            if (prop.getPath().equals(binding.property())) {
                selectedProperty = prop;
                if (propertyButton != null) propertyButton.setValue(prop);
                break;
            }
        }

        // Set source
        selectedSource = binding.source();
        if (sourceButton != null && availableSources.contains(selectedSource)) {
            sourceButton.setValue(selectedSource);
        }

        // Set curve
        selectedCurve = binding.curve();
        if (curveButton != null) curveButton.setValue(selectedCurve);

        // Set ranges
        inputMin = binding.inputMin();
        inputMax = binding.inputMax();
        outputMin = binding.outputMin();
        outputMax = binding.outputMax();
    }
    
    /**
     * Formats source ID for display (e.g., "player.health_percent" -> "Health %").
     */
    private String formatSourceName(String sourceId) {
        if (sourceId == null || sourceId.isEmpty()) return "None";
        
        // Remove prefix and format
        String name = sourceId;
        if (name.startsWith("player.")) name = name.substring(7);
        if (name.startsWith("field.")) name = name.substring(6);
        
        // Convert snake_case to Title Case
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
    
    /**
     * Updates input slider ranges based on the selected source.
     */
    private void updateInputSliderRanges() {
        // Get source metadata if available
        var source = BindingSources.get(selectedSource);
        if (source.isPresent()) {
            boolean isBoolean = source.get().isBoolean();
            if (isBoolean) {
                inputMin = 0f;
                inputMax = 1f;
            }
            // For non-boolean sources, keep user's range or use sensible defaults
        }
    }
    
    /**
     * Updates the output slider ranges based on the selected property.
     */
    private void updateOutputSliderRanges() {
        outputMin = Math.max(selectedProperty.minValue, Math.min(selectedProperty.maxValue, outputMin));
        outputMax = Math.max(selectedProperty.minValue, Math.min(selectedProperty.maxValue, outputMax));
    }
    
    /**
     * Adds the currently configured binding to the state.
     */
    private void addCurrentBinding() {
        // Store binding in FieldEditState using BindingConfig
        state.addBinding(net.cyberpunk042.field.influence.BindingConfig.builder()
            .property(selectedProperty.getPath())
            .source(selectedSource)
            .inputRange(inputMin, inputMax)
            .outputRange(outputMin, outputMax)
            .curve(selectedCurve)
            .build()
        );
        
        net.cyberpunk042.log.Logging.GUI.topic("bindings").info(
            "Added binding: {} <- {} [in: {}-{} out: {}-{}] curve={}",
            selectedProperty.getPath(),
            selectedSource,
            inputMin, inputMax,
            outputMin, outputMax,
            selectedCurve.name()
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Returns all widgets for registration with the parent screen. */
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    /** Renders the sub-panel. */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // Section header
        context.drawText(textRenderer, "§e§lProperty Bindings",
            layout.getStartX() + GuiConstants.PADDING,
            layout.getY() - 14,
            GuiConstants.TEXT_PRIMARY, false);

        // Render existing bindings list
        int listY = layout.getCurrentY() + 4;
        int listX = layout.getStartX() + GuiConstants.PADDING;
        int listWidth = layout.getPanelWidth() - GuiConstants.PADDING * 2;

        var bindings = state.getBindings();
        if (bindings.isEmpty()) {
            context.drawText(textRenderer, "§7No bindings configured",
                listX, listY, GuiConstants.TEXT_SECONDARY, false);
        } else {
            context.drawText(textRenderer, "§7Active Bindings (" + bindings.size() + "):",
                listX, listY, GuiConstants.TEXT_SECONDARY, false);
            listY += 12;

            for (int i = 0; i < bindings.size(); i++) {
                var binding = bindings.get(i);
                boolean selected = i == selectedBindingIndex;
                boolean hovered = mouseX >= listX && mouseX < listX + listWidth &&
                                  mouseY >= listY && mouseY < listY + BINDING_ITEM_HEIGHT;

                // Background for selected/hovered
                if (selected) {
                    context.fill(listX - 2, listY - 1, listX + listWidth, listY + BINDING_ITEM_HEIGHT - 1, 0x40FFFFFF);
                } else if (hovered) {
                    context.fill(listX - 2, listY - 1, listX + listWidth, listY + BINDING_ITEM_HEIGHT - 1, 0x20FFFFFF);
                }

                // Binding text: "property ← source (curve)"
                String bindingText = String.format("%s ← %s (%s)",
                    binding.property(),
                    formatSourceName(binding.source()),
                    binding.curve().name().toLowerCase()
                );
                int color = selected ? 0xFFFFFF : (hovered ? 0xCCCCCC : 0xAAAAAA);
                context.drawText(textRenderer, bindingText, listX, listY + 2, color, false);

                listY += BINDING_ITEM_HEIGHT;
            }
        }
    }

    /**
     * Handle mouse clicks on binding list.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int listY = layout.getCurrentY() + 4 + 12; // After header
        int listX = layout.getStartX() + GuiConstants.PADDING;
        int listWidth = layout.getPanelWidth() - GuiConstants.PADDING * 2;

        var bindings = state.getBindings();
        for (int i = 0; i < bindings.size(); i++) {
            int itemY = listY + i * BINDING_ITEM_HEIGHT;
            if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= itemY && mouseY < itemY + BINDING_ITEM_HEIGHT) {

                selectedBindingIndex = i;
                loadBindingToForm(bindings.get(i));
                updateRemoveButtonState();
                return true;
            }
        }
        return false;
    }
    
    /** Returns the total height of this sub-panel. */
    public int getHeight() {
        int baseHeight = layout.getCurrentY() - layout.getY();
        // Add space for binding list
        int bindingListHeight = 16 + state.getBindings().size() * BINDING_ITEM_HEIGHT;
        return baseHeight + bindingListHeight;
    }

    @Override
    public void tick() {
        // No per-tick updates needed
    }

}
