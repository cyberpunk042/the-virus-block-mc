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

import java.util.ArrayList;
import java.util.List;

/**
 * PrimitivePanel - manages primitives inside the selected layer.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>Select previous/next primitive</li>
 *   <li>Add / remove primitive</li>
 *   <li>Reorder primitives up/down</li>
 * </ul>
 *
 * This is intentionally simple: it operates on placeholder primitive IDs stored
 * in {@link FieldEditState}. Actual per-primitive parameters remain handled by the
 * sub-panels (Shape, Appearance, etc.) once the primitive is selected.
 */
public class PrimitivePanel extends AbstractPanel {

    // Navigation
    private ButtonWidget prevBtn;
    private ButtonWidget indicatorBtn;
    private ButtonWidget nextBtn;

    // Actions
    private ButtonWidget addBtn;
    private ButtonWidget removeBtn;
    private ButtonWidget moveUpBtn;
    private ButtonWidget moveDownBtn;

    private final int startY;
    
    /** Callback fired when the selected primitive changes (for UI rebuild) */
    private Runnable onPrimitiveChanged;

    public PrimitivePanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("PrimitivePanel created");
    }
    
    /** Sets the callback for when primitive selection changes. */
    public void setOnPrimitiveChanged(Runnable callback) {
        this.onPrimitiveChanged = callback;
    }
    
    /** Notifies that the primitive selection has changed. */
    private void notifyPrimitiveChanged() {
        if (onPrimitiveChanged != null) {
            onPrimitiveChanged.run();
        }
    }

    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;

        int x = GuiConstants.PADDING;
        int y = startY;
        int buttonSize = GuiConstants.WIDGET_HEIGHT;
        int smallBtn = 24;

        // Navigation row: [<] [Primitive 1/3 (name)] [>]
        prevBtn = GuiWidgets.button(x, y, smallBtn, "<", "Previous primitive", this::prevPrimitive);

        indicatorBtn = ButtonWidget.builder(
            net.minecraft.text.Text.literal(getIndicatorText()),
            btn -> {} // display only
        ).dimensions(x + smallBtn + 2, y, 120, buttonSize).build();

        nextBtn = GuiWidgets.button(x + smallBtn + 2 + 120 + 2, y, smallBtn, ">", "Next primitive", this::nextPrimitive);

        y += buttonSize + GuiConstants.PADDING;

        // Actions: [+] [-] [▲] [▼]
        addBtn = GuiWidgets.button(x, y, smallBtn, "+", "Add primitive", this::addPrimitive);
        removeBtn = GuiWidgets.button(x + smallBtn + 2, y, smallBtn, "-", "Remove primitive", this::confirmRemovePrimitive);
        moveUpBtn = GuiWidgets.button(x + (smallBtn + 2) * 2, y, smallBtn, "▲", "Move primitive up", this::movePrimitiveUp);
        moveDownBtn = GuiWidgets.button(x + (smallBtn + 2) * 3, y, smallBtn, "▼", "Move primitive down", this::movePrimitiveDown);

        updateButtonStates();
    }

    private void prevPrimitive() {
        int current = state.getSelectedPrimitiveIndex();
        if (current > 0) {
            state.setSelectedPrimitiveIndex(current - 1);
            updateIndicator();
            updateButtonStates();
            notifyPrimitiveChanged();
            Logging.GUI.topic("primitive").debug("Selected primitive: {}", current - 1);
        }
    }

    private void nextPrimitive() {
        int layer = state.getSelectedLayerIndex();
        int total = state.getPrimitiveCount(layer);
        int current = state.getSelectedPrimitiveIndex();
        if (current < total - 1) {
            state.setSelectedPrimitiveIndex(current + 1);
            updateIndicator();
            updateButtonStates();
            notifyPrimitiveChanged();
            Logging.GUI.topic("primitive").debug("Selected primitive: {}", current + 1);
        }
    }

    private void addPrimitive() {
        int layer = state.getSelectedLayerIndex();
        int newIndex = state.addPrimitive(layer);
        if (newIndex >= 0) {
            state.setSelectedPrimitiveIndex(newIndex);
            updateIndicator();
            updateButtonStates();
            notifyPrimitiveChanged();
            ToastNotification.success("Primitive added");
            Logging.GUI.topic("primitive").info("Added primitive {} to layer {}", newIndex, layer);
        } else {
            ToastNotification.warning("Cannot add primitive");
        }
    }

    private void confirmRemovePrimitive() {
        int layer = state.getSelectedLayerIndex();
        int total = state.getPrimitiveCount(layer);
        if (total <= 1) {
            ToastNotification.warning("Cannot remove last primitive");
            return;
        }

        ConfirmDialog.show(parent,
            "Remove Primitive",
            "Delete primitive " + getIndicatorText() + "?",
            this::removePrimitive);
    }

    private void removePrimitive() {
        int layer = state.getSelectedLayerIndex();
        int index = state.getSelectedPrimitiveIndex();
        if (state.removePrimitive(layer, index)) {
            updateIndicator();
            updateButtonStates();
            notifyPrimitiveChanged();
            ToastNotification.info("Primitive removed");
            Logging.GUI.topic("primitive").info("Removed primitive {} from layer {}", index, layer);
        }
    }

    private void movePrimitiveUp() {
        int layer = state.getSelectedLayerIndex();
        int index = state.getSelectedPrimitiveIndex();
        if (index > 0 && state.swapPrimitives(layer, index, index - 1)) {
            state.setSelectedPrimitiveIndex(index - 1);
            updateIndicator();
            updateButtonStates();
            notifyPrimitiveChanged();
            Logging.GUI.topic("primitive").debug("Moved primitive {} up", index);
        }
    }

    private void movePrimitiveDown() {
        int layer = state.getSelectedLayerIndex();
        int index = state.getSelectedPrimitiveIndex();
        int total = state.getPrimitiveCount(layer);
        if (index < total - 1 && state.swapPrimitives(layer, index, index + 1)) {
            state.setSelectedPrimitiveIndex(index + 1);
            updateIndicator();
            updateButtonStates();
            notifyPrimitiveChanged();
            Logging.GUI.topic("primitive").debug("Moved primitive {} down", index);
        }
    }

    private String getIndicatorText() {
        int layer = state.getSelectedLayerIndex();
        int total = state.getPrimitiveCount(layer);
        int index = state.getSelectedPrimitiveIndex();
        String name = state.getPrimitiveName(layer, index);
        return "Prim " + (index + 1) + "/" + total + (name.isEmpty() ? "" : " • " + name);
    }

    private void updateIndicator() {
        if (indicatorBtn != null) {
            indicatorBtn.setMessage(net.minecraft.text.Text.literal(getIndicatorText()));
        }
    }

    private void updateButtonStates() {
        int layer = state.getSelectedLayerIndex();
        int total = state.getPrimitiveCount(layer);
        int index = state.getSelectedPrimitiveIndex();

        if (prevBtn != null) prevBtn.active = index > 0;
        if (nextBtn != null) nextBtn.active = index < total - 1;
        if (removeBtn != null) removeBtn.active = total > 1;
        if (moveUpBtn != null) moveUpBtn.active = index > 0;
        if (moveDownBtn != null) moveDownBtn.active = index < total - 1;
        updateIndicator();
    }

    /** Refresh UI state when layer selection changes. */
    public void refreshForLayerChange() {
        updateButtonStates();
    }

    @Override
    public void tick() {
        // No-op for now
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Section header
        context.drawText(
            net.minecraft.client.MinecraftClient.getInstance().textRenderer,
            "Primitives",
            GuiConstants.PADDING,
            startY - 12,
            GuiConstants.TEXT_SECONDARY,
            false
        );

        if (prevBtn != null) prevBtn.render(context, mouseX, mouseY, delta);
        if (indicatorBtn != null) indicatorBtn.render(context, mouseX, mouseY, delta);
        if (nextBtn != null) nextBtn.render(context, mouseX, mouseY, delta);
        if (addBtn != null) addBtn.render(context, mouseX, mouseY, delta);
        if (removeBtn != null) removeBtn.render(context, mouseX, mouseY, delta);
        if (moveUpBtn != null) moveUpBtn.render(context, mouseX, mouseY, delta);
        if (moveDownBtn != null) moveDownBtn.render(context, mouseX, mouseY, delta);
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (prevBtn != null) list.add(prevBtn);
        if (indicatorBtn != null) list.add(indicatorBtn);
        if (nextBtn != null) list.add(nextBtn);
        if (addBtn != null) list.add(addBtn);
        if (removeBtn != null) list.add(removeBtn);
        if (moveUpBtn != null) list.add(moveUpBtn);
        if (moveDownBtn != null) list.add(moveDownBtn);
        return list;
    }

    public int getHeight() {
        return (GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING) * 2 + 12; // header
    }
}

