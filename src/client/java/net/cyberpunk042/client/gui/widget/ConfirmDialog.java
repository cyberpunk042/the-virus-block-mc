package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Generic modal confirmation dialog.
 * 
 * <p>Can be used for:</p>
 * <ul>
 *   <li>Preset application confirmation</li>
 *   <li>Unsaved changes warning when switching profiles</li>
 *   <li>Delete confirmation</li>
 *   <li>Any yes/no decision</li>
 * </ul>
 */
public class ConfirmDialog {

    public enum Type {
        INFO(GuiConstants.ACCENT),
        WARNING(GuiConstants.WARNING),
        DANGER(0xFFFF4444);
        
        public final int color;
        Type(int color) { this.color = color; }
    }

    private final String title;
    private final List<String> messageLines;
    private final String confirmText;
    private final String cancelText;
    private final Type type;
    private final Consumer<Boolean> onResult;

    private ButtonWidget cancelBtn;
    private ButtonWidget confirmBtn;

    private int x, y, width, height;
    private boolean visible = false;

    /**
     * Create a confirmation dialog.
     * 
     * @param title Dialog title
     * @param messageLines Lines of message text
     * @param confirmText Text for confirm button (e.g., "Apply", "Discard", "Delete")
     * @param cancelText Text for cancel button (e.g., "Cancel", "Go Back")
     * @param type Dialog type (affects title color)
     * @param onResult Callback with true=confirmed, false=cancelled
     */
    public ConfirmDialog(String title, List<String> messageLines, String confirmText, String cancelText, 
                         Type type, Consumer<Boolean> onResult) {
        this.title = title;
        this.messageLines = messageLines;
        this.confirmText = confirmText;
        this.cancelText = cancelText;
        this.type = type;
        this.onResult = onResult;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Create an "unsaved changes" warning dialog.
     */
    public static ConfirmDialog unsavedChanges(String action, Consumer<Boolean> onResult) {
        return new ConfirmDialog(
            "Unsaved Changes",
            List.of(
                "You have unsaved changes that will be lost.",
                "",
                "Are you sure you want to " + action + "?"
            ),
            "Discard Changes",
            "Go Back",
            Type.WARNING,
            onResult
        );
    }

    /**
     * Create a delete confirmation dialog.
     */
    public static ConfirmDialog delete(String itemName, Consumer<Boolean> onResult) {
        return new ConfirmDialog(
            "Delete " + itemName + "?",
            List.of(
                "This action cannot be undone.",
                "",
                "Are you sure you want to delete \"" + itemName + "\"?"
            ),
            "Delete",
            "Cancel",
            Type.DANGER,
            onResult
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LEGACY STATIC SHOW METHOD (for backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════
    
    // Singleton instance for legacy static show() usage
    private static ConfirmDialog legacyInstance;
    private static net.minecraft.client.gui.screen.Screen legacyParent;
    
    /**
     * Legacy static show method for backward compatibility.
     * Shows a simple confirmation dialog on the parent screen.
     * 
     * @param parent The parent screen
     * @param title Dialog title
     * @param message Dialog message
     * @param onConfirm Callback when confirmed (no-arg Runnable)
     */
    public static void show(net.minecraft.client.gui.screen.Screen parent, String title, String message, Runnable onConfirm) {
        legacyParent = parent;
        legacyInstance = new ConfirmDialog(
            title,
            List.of(message),
            "Confirm",
            "Cancel",
            Type.WARNING,
            confirmed -> {
                if (confirmed) {
                    onConfirm.run();
                }
                legacyInstance = null;
                legacyParent = null;
            }
        );
        
        // Get screen dimensions from parent
        int screenWidth = parent.width;
        int screenHeight = parent.height;
        legacyInstance.show(screenWidth, screenHeight);
    }
    
    /**
     * Get the legacy instance for rendering (called from screens that use legacy API).
     */
    public static ConfirmDialog getLegacyInstance() {
        return legacyInstance;
    }
    
    /**
     * Check if legacy dialog is visible.
     */
    public static boolean isLegacyVisible() {
        return legacyInstance != null && legacyInstance.isVisible();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════

    public void show(int screenWidth, int screenHeight) {
        this.width = 300;
        this.height = 90 + (messageLines.size() * 12);
        this.x = (screenWidth - width) / 2;
        this.y = (screenHeight - height) / 2;

        int btnWidth = 100;
        int btnY = y + height - 30;
        int btnSpacing = 15;
        int totalBtnWidth = btnWidth * 2 + btnSpacing;
        int btnStartX = x + (width - totalBtnWidth) / 2;

        cancelBtn = GuiWidgets.button(btnStartX, btnY, btnWidth, cancelText, "Cancel this action", () -> {
            visible = false;
            onResult.accept(false);
        });

        confirmBtn = GuiWidgets.button(btnStartX + btnWidth + btnSpacing, btnY, btnWidth, confirmText, "Confirm this action", () -> {
            visible = false;
            onResult.accept(true);
        });

        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RENDERING
    // ═══════════════════════════════════════════════════════════════════════

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        // Darken background
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0x88000000);

        // Dialog box
        context.fill(x, y, x + width, y + height, GuiConstants.BG_PRIMARY);
        context.drawBorder(x, y, width, height, GuiConstants.BORDER);

        // Title with type color
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, x + (width - titleWidth) / 2, y + 10, type.color, false);

        // Separator line
        context.drawHorizontalLine(x + 10, x + width - 10, y + 26, GuiConstants.BORDER);

        // Message lines
        int textY = y + 35;
        for (String line : messageLines) {
            if (line.isEmpty()) {
                textY += 6; // Small gap for empty lines
            } else {
                context.drawText(textRenderer, line, x + 15, textY, GuiConstants.TEXT_PRIMARY, false);
                textY += 12;
            }
        }

        // Render buttons
        if (cancelBtn != null) cancelBtn.render(context, mouseX, mouseY, delta);
        if (confirmBtn != null) confirmBtn.render(context, mouseX, mouseY, delta);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INPUT HANDLING
    // ═══════════════════════════════════════════════════════════════════════

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        // Check if click is outside dialog (cancel)
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            visible = false;
            onResult.accept(false);
            return true;
        }

        // Check buttons
        if (cancelBtn != null && cancelBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (confirmBtn != null && confirmBtn.mouseClicked(mouseX, mouseY, button)) return true;

        return true; // Consume click within dialog
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        // ESC to cancel
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            visible = false;
            onResult.accept(false);
            return true;
        }

        // ENTER to confirm
        if (keyCode == 257) { // GLFW_KEY_ENTER
            visible = false;
            onResult.accept(true);
            return true;
        }

        return false;
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        if (!visible) return List.of();
        java.util.ArrayList<net.minecraft.client.gui.widget.ClickableWidget> list = new java.util.ArrayList<>();
        if (cancelBtn != null) list.add(cancelBtn);
        if (confirmBtn != null) list.add(confirmBtn);
        return list;
    }
}
