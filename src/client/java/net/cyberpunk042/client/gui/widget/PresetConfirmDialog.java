package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog shown before applying a multi-scope preset.
 * 
 * <p>Displays the preset name, description, and what categories will be affected,
 * allowing the user to confirm or cancel the operation.</p>
 */
public class PresetConfirmDialog {

    private final String presetName;
    private final String description;
    private final List<String> affectedCategories;
    private final Consumer<Boolean> onResult; // true = apply, false = cancel

    private ButtonWidget cancelBtn;
    private ButtonWidget applyBtn;

    private int x, y, width, height;
    private boolean visible = false;

    public PresetConfirmDialog(String presetName, Consumer<Boolean> onResult) {
        this.presetName = presetName;
        this.description = PresetRegistry.getDescription(presetName);
        this.affectedCategories = PresetRegistry.getAffectedCategories(presetName);
        this.onResult = onResult;
    }

    public void show(int screenWidth, int screenHeight) {
        this.width = 280;
        this.height = 120 + (affectedCategories.size() * 12);
        this.x = (screenWidth - width) / 2;
        this.y = (screenHeight - height) / 2;

        int btnWidth = 80;
        int btnY = y + height - 30;
        int btnSpacing = 10;
        int totalBtnWidth = btnWidth * 2 + btnSpacing;
        int btnStartX = x + (width - totalBtnWidth) / 2;

        cancelBtn = GuiWidgets.button(btnStartX, btnY, btnWidth, "Cancel", "Don't apply preset", () -> {
            visible = false;
            onResult.accept(false);
        });

        applyBtn = GuiWidgets.button(btnStartX + btnWidth + btnSpacing, btnY, btnWidth, "Apply", "Apply this preset", () -> {
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

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        // Darken background
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0x88000000);

        // Dialog box
        context.fill(x, y, x + width, y + height, GuiConstants.BG_PRIMARY);
        context.drawBorder(x, y, width, height, GuiConstants.BORDER);

        // Title
        String title = "Apply Preset: \"" + presetName + "\"";
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, x + (width - titleWidth) / 2, y + 10, GuiConstants.TEXT_PRIMARY, false);

        // Separator line
        context.drawHorizontalLine(x + 10, x + width - 10, y + 26, GuiConstants.BORDER);

        // Description
        int textY = y + 35;
        if (!description.isEmpty()) {
            context.drawText(textRenderer, description, x + 15, textY, GuiConstants.TEXT_SECONDARY, false);
            textY += 16;
        }

        // "This will modify:" header
        context.drawText(textRenderer, "This will modify:", x + 15, textY, GuiConstants.TEXT_PRIMARY, false);
        textY += 14;

        // List affected categories
        for (String category : affectedCategories) {
            String bullet = "• " + formatCategory(category);
            context.drawText(textRenderer, bullet, x + 25, textY, GuiConstants.ACCENT, false);
            textY += 12;
        }

        // Render buttons
        if (cancelBtn != null) cancelBtn.render(context, mouseX, mouseY, delta);
        if (applyBtn != null) applyBtn.render(context, mouseX, mouseY, delta);
    }

    private String formatCategory(String category) {
        // Capitalize and add details based on category
        return switch (category) {
            case "appearance" -> "Appearance (glow, emissive, colors)";
            case "animation" -> "Animation (spin, pulse, alpha)";
            case "fill" -> "Fill (mode, depth, double-sided)";
            case "visibility" -> "Visibility (mask type, feather)";
            case "arrangement" -> "Arrangement (patterns)";
            case "beam" -> "Beam (radius, height, pulse)";
            case "follow" -> "Follow Mode";
            case "prediction" -> "Prediction Settings";
            case "transform" -> "Transform (offset, rotation, scale)";
            case "layers" -> "Layers (structural changes!)";
            default -> category;
        };
    }

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
        if (applyBtn != null && applyBtn.mouseClicked(mouseX, mouseY, button)) return true;

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

        // ENTER to apply
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
        if (applyBtn != null) list.add(applyBtn);
        return list;
    }
}

