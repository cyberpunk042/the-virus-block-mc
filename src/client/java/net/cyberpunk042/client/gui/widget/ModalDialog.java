package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog overlay for confirmations, edits, etc.
 * 
 * <pre>
 * ┌─────────────────────────────────┐
 * │  Title                      [×] │
 * ├─────────────────────────────────┤
 * │                                 │
 * │  (content area)                 │
 * │                                 │
 * ├─────────────────────────────────┤
 * │  [Action1] [Action2] [Cancel]   │
 * └─────────────────────────────────┘
 * </pre>
 */
public class ModalDialog {
    
    private static final int TITLE_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 70;
    private static final int PADDING = 8;
    
    private final String title;
    private final TextRenderer textRenderer;
    private final int screenWidth;
    private final int screenHeight;
    
    private int dialogWidth = 250;
    private int dialogHeight = 150;
    
    private Bounds dialogBounds = Bounds.EMPTY;
    private Bounds contentBounds = Bounds.EMPTY;
    
    private boolean visible = false;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    private final List<ActionButton> actions = new ArrayList<>();
    
    // Content
    private ContentBuilder contentBuilder;
    private Runnable onClose;
    
    // Custom handlers for extended content
    private java.util.function.BiPredicate<Double, Double> clickHandler;
    private ExtraRenderer extraRenderer;
    
    public ModalDialog(String title, TextRenderer textRenderer, int screenWidth, int screenHeight) {
        this.title = title;
        this.textRenderer = textRenderer;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ModalDialog size(int width, int height) {
        this.dialogWidth = width;
        this.dialogHeight = height;
        return this;
    }
    
    public ModalDialog content(ContentBuilder builder) {
        this.contentBuilder = builder;
        return this;
    }
    
    public ModalDialog addAction(String label, Runnable action) {
        actions.add(new ActionButton(label, action, false));
        return this;
    }
    
    public ModalDialog addAction(String label, Runnable action, boolean primary) {
        actions.add(new ActionButton(label, action, primary));
        return this;
    }
    
    public ModalDialog onClose(Runnable callback) {
        this.onClose = callback;
        return this;
    }
    
    /**
     * Sets a custom click handler for extended content (e.g., color palette).
     * Returns true if the click was handled.
     */
    public ModalDialog setClickHandler(java.util.function.BiPredicate<Double, Double> handler) {
        this.clickHandler = handler;
        return this;
    }
    
    /**
     * Sets an extra renderer for custom content that isn't a standard widget.
     */
    public ModalDialog setExtraRenderer(ExtraRenderer renderer) {
        this.extraRenderer = renderer;
        return this;
    }
    
    @FunctionalInterface
    public interface ExtraRenderer {
        void render(DrawContext context, int mouseX, int mouseY, float delta);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISIBILITY
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void show() {
        visible = true;
        rebuild();
    }
    
    public void hide() {
        visible = false;
        widgets.clear();
        if (onClose != null) {
            onClose.run();
        }
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILD
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Rebuilds the dialog content. Call after dynamic changes to content.
     */
    public void rebuild() {
        widgets.clear();
        
        // Center dialog
        dialogBounds = Bounds.centeredIn(screenWidth, screenHeight, dialogWidth, dialogHeight);
        
        // Content area (between title and buttons)
        int contentY = dialogBounds.y() + TITLE_HEIGHT;
        int contentHeight = dialogHeight - TITLE_HEIGHT - BUTTON_HEIGHT - PADDING * 2;
        contentBounds = new Bounds(
            dialogBounds.x() + PADDING,
            contentY,
            dialogWidth - PADDING * 2,
            contentHeight
        );
        
        // Close button
        widgets.add(ButtonWidget.builder(Text.literal("×"), btn -> hide())
            .dimensions(dialogBounds.right() - 18, dialogBounds.y() + 2, 16, 16)
            .build());
        
        // Action buttons (right-aligned)
        int buttonY = dialogBounds.bottom() - BUTTON_HEIGHT - PADDING;
        int totalButtonWidth = actions.size() * BUTTON_WIDTH + (actions.size() - 1) * 4;
        int buttonX = dialogBounds.right() - PADDING - totalButtonWidth;
        
        for (ActionButton action : actions) {
            final Runnable actionRunnable = action.action;
            ButtonWidget btn = ButtonWidget.builder(Text.literal(action.label), b -> {
                actionRunnable.run();
                hide();
            })
                .dimensions(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
            widgets.add(btn);
            buttonX += BUTTON_WIDTH + 4;
        }
        
        // Build custom content
        if (contentBuilder != null) {
            widgets.addAll(contentBuilder.build(contentBounds, textRenderer));
        }

        // Auto-focus the first text field if present
        for (ClickableWidget w : widgets) {
            if (w instanceof TextFieldWidget tf) {
                tf.visible = true;
                tf.active = true;
                tf.setEditable(true);
                tf.setFocused(true);
                // Select all text for easy replacement
                tf.setCursorToStart(false);
                tf.setCursorToEnd(true);
                break;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGETS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        // Darken background
        context.fill(0, 0, screenWidth, screenHeight, 0x88000000);
        
        // Dialog shadow
        context.fill(dialogBounds.x() + 3, dialogBounds.y() + 3,
                     dialogBounds.right() + 3, dialogBounds.bottom() + 3, 0x66000000);
        
        // Dialog border
        context.fill(dialogBounds.x() - 1, dialogBounds.y() - 1,
                     dialogBounds.right() + 1, dialogBounds.bottom() + 1, 0xFF555555);
        
        // Dialog background
        context.fill(dialogBounds.x(), dialogBounds.y(),
                     dialogBounds.right(), dialogBounds.bottom(), 0xFF1a1a1a);
        
        // Title bar
        context.fill(dialogBounds.x(), dialogBounds.y(),
                     dialogBounds.right(), dialogBounds.y() + TITLE_HEIGHT, 0xFF2a2a2a);
        
        // Title text
        context.drawTextWithShadow(textRenderer, title, 
            dialogBounds.x() + PADDING, dialogBounds.y() + 6, 0xFFAAFFAA);
        
        // Separator
        context.fill(dialogBounds.x(), dialogBounds.y() + TITLE_HEIGHT,
                     dialogBounds.right(), dialogBounds.y() + TITLE_HEIGHT + 1, 0xFF333333);
        
        // Button area separator
        int buttonAreaY = dialogBounds.bottom() - BUTTON_HEIGHT - PADDING - 4;
        context.fill(dialogBounds.x() + PADDING, buttonAreaY,
                     dialogBounds.right() - PADDING, buttonAreaY + 1, 0xFF333333);
        
        // Render extra custom content (e.g., color palette grid)
        if (extraRenderer != null) {
            extraRenderer.render(context, mouseX, mouseY, delta);
        }
    }
    
    /**
     * Checks if a click is within the dialog bounds.
     * Used to prevent clicks from passing through to the screen behind.
     */
    public boolean containsClick(double mouseX, double mouseY) {
        if (!visible) return false;
        return dialogBounds.contains(mouseX, mouseY);
    }
    
    /**
     * Handle custom click events (e.g., color palette grid).
     * Call this before processing standard widgets.
     * @return true if the click was handled
     */
    public boolean handleCustomClick(double mouseX, double mouseY) {
        if (!visible) return false;
        if (clickHandler != null && clickHandler.test(mouseX, mouseY)) {
            return true;
        }
        return false;
    }
    
    /**
     * Handles escape key to close.
     */
    public boolean keyPressed(int keyCode) {
        if (visible && keyCode == 256) { // ESCAPE
            hide();
            return true;
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INNER TYPES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private record ActionButton(String label, Runnable action, boolean primary) {}
    
    /**
     * Builder for dialog content.
     */
    @FunctionalInterface
    public interface ContentBuilder {
        List<ClickableWidget> build(Bounds bounds, TextRenderer textRenderer);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRESET DIALOGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a simple confirmation dialog.
     */
    public static ModalDialog confirm(String title, String message, TextRenderer textRenderer,
                                       int screenWidth, int screenHeight,
                                       Runnable onConfirm) {
        return new ModalDialog(title, textRenderer, screenWidth, screenHeight)
            .size(280, 120)
            .content((bounds, tr) -> List.of())  // Message rendered separately
            .addAction("Cancel", () -> {})
            .addAction("Confirm", onConfirm, true);
    }
    
    /**
     * Creates a text input dialog.
     */
    public static ModalDialog textInput(String title, String initialValue, TextRenderer textRenderer,
                                         int screenWidth, int screenHeight,
                                         Consumer<String> onSubmit) {
        final TextFieldWidget[] fieldHolder = new TextFieldWidget[1];
        
        return new ModalDialog(title, textRenderer, screenWidth, screenHeight)
            .size(300, 130)
            .content((bounds, tr) -> {
                TextFieldWidget field = new TextFieldWidget(tr, 
                    bounds.x(), bounds.y() + 10, bounds.width(), 20, Text.literal(""));
                field.setText(initialValue);
                field.setMaxLength(64);
                // Ensure field is interactive from creation
                field.visible = true;
                field.active = true;
                field.setEditable(true);
                field.setFocused(true);
                fieldHolder[0] = field;
                return List.of(field);
            })
            .addAction("Cancel", () -> {})
            .addAction("OK", () -> {
                if (fieldHolder[0] != null) {
                    onSubmit.accept(fieldHolder[0].getText());
                }
            }, true);
    }
}

