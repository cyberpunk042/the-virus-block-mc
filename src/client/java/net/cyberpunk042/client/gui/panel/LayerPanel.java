package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.ConfirmDialog;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.visual.layer.BlendMode;

import java.util.ArrayList;
import java.util.List;

/**
 * G51-G56: Layer management panel for the Quick tab.
 * 
 * <p>Provides controls for managing field layers:</p>
 * <ul>
 *   <li>Layer selection (prev/next navigation)</li>
 *   <li>Add/remove layers</li>
 *   <li>Visibility toggle per layer</li>
 *   <li>Reorder layers (move up/down)</li>
 * </ul>
 * 
 * @see FieldEditState#getSelectedLayerIndex()
 */
public class LayerPanel extends AbstractPanel {
    
    // Navigation
    private ButtonWidget prevLayerBtn;
    private ButtonWidget nextLayerBtn;
    private ButtonWidget layerIndicator; // Shows "Layer 1/3"
    
    // Actions
    private ButtonWidget addLayerBtn;
    private ButtonWidget removeLayerBtn;
    private ButtonWidget visibilityBtn;
    private ButtonWidget moveUpBtn;
    private ButtonWidget moveDownBtn;
    
    // G-LAYER: Layer settings
    private CyclingButtonWidget<BlendMode> blendModeDropdown;
    private LabeledSlider alphaSlider;
    
    // Layout
    private int startY;
    private Runnable onLayerChanged;
    
    public LayerPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("LayerPanel created");
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        int x = GuiConstants.PADDING;
        int y = startY;
        int buttonSize = GuiConstants.WIDGET_HEIGHT;
        int smallBtn = 24;
        
        // G52: Layer navigation row
        // [<] [Layer 1/3] [>]
        prevLayerBtn = GuiWidgets.button(x, y, smallBtn, "<", "Previous layer", this::prevLayer);
        
        int indicatorWidth = 80;
        layerIndicator = ButtonWidget.builder(
            net.minecraft.text.Text.literal(getLayerIndicatorText()),
            btn -> {} // No action, just display
        ).dimensions(x + smallBtn + 2, y, indicatorWidth, buttonSize).build();
        
        nextLayerBtn = GuiWidgets.button(x + smallBtn + indicatorWidth + 4, y, smallBtn, ">", "Next layer", this::nextLayer);
        
        y += buttonSize + GuiConstants.PADDING;
        
        // G53-G54: Add/Remove buttons
        addLayerBtn = GuiWidgets.button(x, y, smallBtn, "+", "Add new layer", this::addLayer);
        removeLayerBtn = GuiWidgets.button(x + smallBtn + 2, y, smallBtn, "-", "Remove layer", this::confirmRemoveLayer);
        
        // G55: Visibility toggle
        visibilityBtn = GuiWidgets.button(x + (smallBtn + 2) * 2, y, smallBtn, getVisibilityIcon(), "Toggle visibility", this::toggleVisibility);
        
        // G56: Reorder buttons
        moveUpBtn = GuiWidgets.button(x + (smallBtn + 2) * 3, y, smallBtn, "▲", "Move layer up", this::moveLayerUp);
        moveDownBtn = GuiWidgets.button(x + (smallBtn + 2) * 4, y, smallBtn, "▼", "Move layer down", this::moveLayerDown);
        
        y += buttonSize + GuiConstants.PADDING;
        
        // G-LAYER: Blend mode and alpha
        int halfW = (panelWidth - 2 * GuiConstants.PADDING - 4) / 2;
        blendModeDropdown = GuiWidgets.enumDropdown(
            x, y, halfW, "Blend", BlendMode.class, BlendMode.NORMAL,
            "Layer blend mode",
            v -> state.setLayerBlendMode(state.getSelectedLayerIndex(), v.name())
        );
        updateBlendModeDropdown();
        
        alphaSlider = LabeledSlider.builder("Alpha")
            .position(x + halfW + 4, y).width(halfW)
            .range(0f, 1f).initial(state.getLayerAlpha(state.getSelectedLayerIndex())).format("%.2f")
            .onChange(v -> state.setLayerAlpha(state.getSelectedLayerIndex(), v))
            .build();
        
        updateButtonStates();
        
        Logging.GUI.topic("panel").debug("LayerPanel initialized");
    }
    
    private void updateBlendModeDropdown() {
        if (blendModeDropdown != null) {
            try {
                blendModeDropdown.setValue(BlendMode.valueOf(state.getLayerBlendMode(state.getSelectedLayerIndex())));
            } catch (IllegalArgumentException ignored) {
                blendModeDropdown.setValue(BlendMode.NORMAL);
            }
        }
    }
    
    private void updateAlphaSlider() {
        if (alphaSlider != null) {
            alphaSlider.setValue(state.getLayerAlpha(state.getSelectedLayerIndex()));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G52: NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void prevLayer() {
        int current = state.getSelectedLayerIndex();
        if (current > 0) {
            state.setSelectedLayerIndex(current - 1);
            refreshLayerControls();
            updateLayerIndicator();
            updateButtonStates();
            Logging.GUI.topic("layer").debug("Selected layer: {}", current - 1);
            notifyLayerChanged();
        }
    }
    
    private void nextLayer() {
        int current = state.getSelectedLayerIndex();
        int total = state.getLayerCount();
        if (current < total - 1) {
            state.setSelectedLayerIndex(current + 1);
            refreshLayerControls();
            updateLayerIndicator();
            updateButtonStates();
            Logging.GUI.topic("layer").debug("Selected layer: {}", current + 1);
            notifyLayerChanged();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G53: ADD LAYER
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void addLayer() {
        int newIndex = state.addLayer();
        if (newIndex >= 0) {
            state.setSelectedLayerIndex(newIndex);
            refreshLayerControls();
            updateLayerIndicator();
            updateButtonStates();
            ToastNotification.success("Layer added");
            Logging.GUI.topic("layer").info("Added layer at index {}", newIndex);
            notifyLayerChanged();
        } else {
            ToastNotification.warning("Max layers reached");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G54: REMOVE LAYER (with confirmation)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void confirmRemoveLayer() {
        if (state.getLayerCount() <= 1) {
            ToastNotification.warning("Cannot remove last layer");
            return;
        }
        
        ConfirmDialog.show(parent, 
            "Remove Layer", 
            "Delete layer " + (state.getSelectedLayerIndex() + 1) + "?",
            this::removeLayer);
    }
    
    private void removeLayer() {
        int removed = state.getSelectedLayerIndex();
        if (state.removeLayer(removed)) {
            // Adjust selection if needed
            if (state.getSelectedLayerIndex() >= state.getLayerCount()) {
                state.setSelectedLayerIndex(state.getLayerCount() - 1);
            }
            refreshLayerControls();
            updateLayerIndicator();
            updateButtonStates();
            ToastNotification.info("Layer removed");
            Logging.GUI.topic("layer").info("Removed layer {}", removed);
            notifyLayerChanged();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G55: VISIBILITY TOGGLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void toggleVisibility() {
        int index = state.getSelectedLayerIndex();
        boolean visible = state.toggleLayerVisibility(index);
        visibilityBtn.setMessage(net.minecraft.text.Text.literal(getVisibilityIcon()));
        Logging.GUI.topic("layer").debug("Layer {} visibility: {}", index, visible);
    }
    
    private String getVisibilityIcon() {
        return state.isLayerVisible(state.getSelectedLayerIndex()) ? "👁" : "○";
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // G56: REORDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void moveLayerUp() {
        int index = state.getSelectedLayerIndex();
        if (index > 0 && state.swapLayers(index, index - 1)) {
            state.setSelectedLayerIndex(index - 1);
            refreshLayerControls();
            updateLayerIndicator();
            updateButtonStates();
            Logging.GUI.topic("layer").debug("Moved layer {} up", index);
            notifyLayerChanged();
        }
    }
    
    private void moveLayerDown() {
        int index = state.getSelectedLayerIndex();
        if (index < state.getLayerCount() - 1 && state.swapLayers(index, index + 1)) {
            state.setSelectedLayerIndex(index + 1);
            refreshLayerControls();
            updateLayerIndicator();
            updateButtonStates();
            Logging.GUI.topic("layer").debug("Moved layer {} down", index);
            notifyLayerChanged();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String getLayerIndicatorText() {
        return "Layer " + (state.getSelectedLayerIndex() + 1) + "/" + state.getLayerCount();
    }
    
    private void updateLayerIndicator() {
        if (layerIndicator != null) {
            layerIndicator.setMessage(net.minecraft.text.Text.literal(getLayerIndicatorText()));
        }
    }
    
    private void updateButtonStates() {
        int index = state.getSelectedLayerIndex();
        int total = state.getLayerCount();
        
        if (prevLayerBtn != null) prevLayerBtn.active = index > 0;
        if (nextLayerBtn != null) nextLayerBtn.active = index < total - 1;
        if (moveUpBtn != null) moveUpBtn.active = index > 0;
        if (moveDownBtn != null) moveDownBtn.active = index < total - 1;
        if (removeLayerBtn != null) removeLayerBtn.active = total > 1;
        if (visibilityBtn != null) {
            visibilityBtn.setMessage(net.minecraft.text.Text.literal(getVisibilityIcon()));
        }
    }

    public void setOnLayerChanged(Runnable onLayerChanged) {
        this.onLayerChanged = onLayerChanged;
    }

    private void notifyLayerChanged() {
        if (onLayerChanged != null) {
            onLayerChanged.run();
        }
    }
    
    @Override
    public void tick() {
        // Could update layer animations here
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Section header
        context.drawText(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            "Layers",
            GuiConstants.PADDING,
            startY - 12,
            GuiConstants.TEXT_SECONDARY,
            false
        );
        
        // Render buttons
        if (prevLayerBtn != null) prevLayerBtn.render(context, mouseX, mouseY, delta);
        if (layerIndicator != null) layerIndicator.render(context, mouseX, mouseY, delta);
        if (nextLayerBtn != null) nextLayerBtn.render(context, mouseX, mouseY, delta);
        if (addLayerBtn != null) addLayerBtn.render(context, mouseX, mouseY, delta);
        if (removeLayerBtn != null) removeLayerBtn.render(context, mouseX, mouseY, delta);
        if (visibilityBtn != null) visibilityBtn.render(context, mouseX, mouseY, delta);
        if (moveUpBtn != null) moveUpBtn.render(context, mouseX, mouseY, delta);
        if (moveDownBtn != null) moveDownBtn.render(context, mouseX, mouseY, delta);
        if (blendModeDropdown != null) blendModeDropdown.render(context, mouseX, mouseY, delta);
        if (alphaSlider != null) alphaSlider.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * Gets all widgets for parent screen registration.
     */
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (prevLayerBtn != null) list.add(prevLayerBtn);
        if (layerIndicator != null) list.add(layerIndicator);
        if (nextLayerBtn != null) list.add(nextLayerBtn);
        if (addLayerBtn != null) list.add(addLayerBtn);
        if (removeLayerBtn != null) list.add(removeLayerBtn);
        if (visibilityBtn != null) list.add(visibilityBtn);
        if (moveUpBtn != null) list.add(moveUpBtn);
        if (moveDownBtn != null) list.add(moveDownBtn);
        if (blendModeDropdown != null) list.add(blendModeDropdown);
        if (alphaSlider != null) list.add(alphaSlider);
        return list;
    }
    
    /**
     * Called when layer selection changes to update blend mode/alpha controls.
     */
    public void refreshLayerControls() {
        updateBlendModeDropdown();
        updateAlphaSlider();
    }
    
    /**
     * Gets the total height of this panel.
     */
    public int getHeight() {
        return (GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING) * 3 + 12; // header + 3 rows
    }
}
