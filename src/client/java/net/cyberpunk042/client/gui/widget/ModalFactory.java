package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Factory for creating modal dialogs used in the Field Customizer.
 * Centralizes modal creation logic to reduce FieldCustomizerScreen complexity.
 */
public final class ModalFactory {
    
    private ModalFactory() {} // Static factory only
    
    /**
     * Creates a modal for renaming/deleting a layer.
     */
    public static ModalDialog createLayerModal(
            FieldEditState state,
            String layerName,
            int layerIndex,
            TextRenderer textRenderer,
            int screenWidth, int screenHeight,
            Consumer<String> onRename,
            Runnable onDelete,
            Runnable onClose) {
        
        int count = state.getFieldLayers().size();
        return createRenameModal(
            "Rename Layer", layerName, count > 1,
            textRenderer, screenWidth, screenHeight,
            onRename, onDelete, onClose
        );
    }
    
    /**
     * Creates a modal for renaming/deleting a primitive.
     */
    public static ModalDialog createPrimitiveModal(
            FieldEditState state,
            String primName,
            int layerIndex, int primIndex,
            TextRenderer textRenderer,
            int screenWidth, int screenHeight,
            Consumer<String> onRename,
            Runnable onDelete,
            Runnable onClose) {
        
        int count = state.getPrimitivesForLayer(layerIndex).size();
        return createRenameModal(
            "Rename Primitive", primName, count > 1,
            textRenderer, screenWidth, screenHeight,
            onRename, onDelete, onClose
        );
    }
    
    /**
     * Creates a generic rename/delete modal dialog.
     */
    public static ModalDialog createRenameModal(
            String title,
            String currentName,
            boolean canDelete,
            TextRenderer textRenderer,
            int screenWidth, int screenHeight,
            Consumer<String> onRename,
            Runnable onDelete,
            Runnable onClose) {
        
        final TextFieldWidget[] fieldHolder = new TextFieldWidget[1];
        
        ModalDialog modal = new ModalDialog(title, textRenderer, screenWidth, screenHeight)
            .size(300, canDelete ? 150 : 130)
            .content((bounds, tr) -> {
                List<ClickableWidget> widgets = new ArrayList<>();
                TextFieldWidget field = new TextFieldWidget(tr, 
                    bounds.x(), bounds.y() + 10, bounds.width(), 20, Text.literal(""));
                field.setText(currentName);
                field.setMaxLength(64);
                field.visible = true;
                field.active = true;
                field.setEditable(true);
                fieldHolder[0] = field;
                widgets.add(field);
                return widgets;
            })
            .addAction("Cancel", () -> {});
        
        if (canDelete) {
            modal.addAction("§cDelete", onDelete);
        }
        
        modal.addAction("OK", () -> {
            if (fieldHolder[0] != null) onRename.accept(fieldHolder[0].getText());
        }, true);
        
        modal.onClose(onClose);
        
        return modal;
    }
    
    /**
     * Creates a modal for entering a color value as a string.
     * Supports theme references like @primary, @beam, or hex codes like #FF00FF.
     * 
     * <p>Includes a color palette grid for quick visual selection.</p>
     */
    public static ModalDialog createColorInputModal(
            String currentColorString,
            TextRenderer textRenderer,
            int screenWidth, int screenHeight,
            java.util.function.Consumer<String> onSubmit,
            Runnable onClose) {
        
        final TextFieldWidget[] fieldHolder = new TextFieldWidget[1];
        final ColorPaletteGrid[] paletteHolder = new ColorPaletteGrid[1];
        
        ModalDialog modal = new ModalDialog("Select Color", textRenderer, screenWidth, screenHeight)
            .size(320, 250)  // Taller to accommodate palette
            .content((bounds, tr) -> {
                List<ClickableWidget> widgets = new java.util.ArrayList<>();
                
                // Color palette grid (top portion)
                paletteHolder[0] = new ColorPaletteGrid(
                    bounds.x(), bounds.y() + 5, bounds.width(),
                    // On color selected (hex)
                    color -> {
                        String hex = String.format("#%06X", color & 0xFFFFFF);
                        if (fieldHolder[0] != null) {
                            fieldHolder[0].setText(hex);
                        }
                    },
                    // On theme selected (@name)
                    themeName -> {
                        if (fieldHolder[0] != null) {
                            fieldHolder[0].setText(themeName);
                        }
                    }
                );
                
                // Text input field (below palette)
                int fieldY = bounds.y() + paletteHolder[0].getHeight() + 15;
                TextFieldWidget field = new TextFieldWidget(tr, 
                    bounds.x(), fieldY, bounds.width(), 20, net.minecraft.text.Text.literal(""));
                field.setText(currentColorString);
                field.setMaxLength(64);
                field.visible = true;
                field.active = true;
                field.setEditable(true);
                field.setPlaceholder(net.minecraft.text.Text.literal("#RRGGBB or @theme"));
                fieldHolder[0] = field;
                widgets.add(field);
                
                return widgets;
            })
            .addAction("Cancel", () -> {})
            .addAction("Apply", () -> {
                if (fieldHolder[0] != null) {
                    onSubmit.accept(fieldHolder[0].getText());
                }
            }, true);
        
        // Set custom click handler to handle palette clicks
        modal.setClickHandler((mouseX, mouseY) -> {
            if (paletteHolder[0] != null && paletteHolder[0].handleClick(mouseX, mouseY)) {
                return true;
            }
            return false;
        });
        
        // Set custom render handler to draw the palette
        modal.setExtraRenderer((context, mouseX, mouseY, delta) -> {
            if (paletteHolder[0] != null) {
                paletteHolder[0].render(context, mouseX, mouseY, delta);
            }
        });
        
        modal.onClose(onClose);
        
        return modal;
    }
    
    /**
     * Focuses the text field within a modal and selects all text.
     * Call this after showing the modal and registering widgets.
     * 
     * @param modal The modal dialog
     * @param screen The screen to set focus on
     */
    public static void focusTextField(ModalDialog modal, net.minecraft.client.gui.screen.Screen screen) {
        if (modal == null) return;
        for (ClickableWidget w : modal.getWidgets()) {
            if (w instanceof TextFieldWidget tf) {
                tf.visible = true;
                tf.active = true;
                tf.setEditable(true);
                tf.setFocused(true);
                screen.setFocused(tf);
                tf.setCursorToStart(false);
                tf.setCursorToEnd(true);
                break;
            }
        }
    }
}
