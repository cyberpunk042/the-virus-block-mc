package net.cyberpunk042.client.gui.component;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.layout.GuiMode;
// SimplifiedFieldRenderer removed - standard mode always used
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.cyberpunk042.client.gui.widget.DropdownWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Header bar component containing:
 * <ul>
 *   <li>Mode toggle button (fullscreen/windowed)</li>
 *   <li>Field visibility toggle</li>
 *   <li>Reset button</li>
 *   <li>Close button</li>
 * </ul>
 * 
 * <p>Also renders the title "Field Customizer" and dirty indicator.</p>
 */
public class HeaderBar implements ScreenComponent {
    
    private Bounds bounds;
    private final TextRenderer textRenderer;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    
    // Callbacks
    private final Runnable onModeToggle;
    private final Runnable onReset;
    private final Runnable onClose;
    
    // State suppliers
    private final BooleanSupplier isFieldActive;
    private final BooleanSupplier isDirty;
    private final Supplier<GuiMode> currentMode;
    private final Supplier<Bounds> rightPanelBoundsSupplier;
    private final Consumer<String> onPresetSelected;
    
    // Widgets
    private ButtonWidget modeToggleBtn;
    private ButtonWidget fieldToggleBtn;
    private ButtonWidget resetBtn;
    private ButtonWidget closeBtn;
    private DropdownWidget<String> rightPresetDropdown;
    private net.minecraft.client.gui.widget.ClickableWidget rendererModeToggle;
    
    /**
     * Creates a header bar.
     * 
     * @param textRenderer Font renderer for title
     * @param onModeToggle Called when mode toggle clicked
     * @param onReset Called when reset clicked
     * @param onClose Called when close clicked
     * @param isFieldActive Supplies whether test field is active
     * @param isDirty Supplies whether state has unsaved changes
     * @param currentMode Supplies current GUI mode
     */
    public HeaderBar(
            TextRenderer textRenderer,
            Runnable onModeToggle,
            Runnable onReset,
            Runnable onClose,
            BooleanSupplier isFieldActive,
            BooleanSupplier isDirty,
            Supplier<GuiMode> currentMode,
            Supplier<Bounds> rightPanelBoundsSupplier,
            Consumer<String> onPresetSelected) {
        
        this.textRenderer = textRenderer;
        this.onModeToggle = onModeToggle;
        this.onReset = onReset;
        this.onClose = onClose;
        this.isFieldActive = isFieldActive;
        this.isDirty = isDirty;
        this.currentMode = currentMode;
        this.rightPanelBoundsSupplier = rightPanelBoundsSupplier;
        this.onPresetSelected = onPresetSelected;
    }
    
    @Override
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        rebuild();
    }
    
    @Override
    public void rebuild() {
        widgets.clear();
        if (bounds == null) return;
        
        int x = bounds.x();
        int y = bounds.y();
        int rightEdge = bounds.right();
        int btnSize = 14;
        int btnHeight = bounds.height() - 4;
        boolean isWindowed = (currentMode.get() == GuiMode.WINDOWED);
        
        if (isWindowed) {
            // WINDOWED LEFT PANEL HEADER:
            // [Label "⬡ Field"] .... [Renderer Toggle in middle] .... [⬜] [◉] (float right)
            
            // Toggle buttons float RIGHT
            int bx = rightEdge - btnSize - 2;
            fieldToggleBtn = ButtonWidget.builder(
                    Text.literal(isFieldActive.getAsBoolean() ? "◉" : "○"),
                    btn -> {
                        FieldEditStateHolder.toggleTestField();
                        btn.setMessage(Text.literal(isFieldActive.getAsBoolean() ? "◉" : "○"));
                    })
                .dimensions(bx, y + 2, btnSize, btnHeight)
                .tooltip(Tooltip.of(Text.literal("Toggle field visibility")))
                .build();
            widgets.add(fieldToggleBtn);
            bx -= btnSize + 2;
            
            modeToggleBtn = ButtonWidget.builder(
                    Text.literal("⬜"),
                    btn -> { onModeToggle.run(); })
                .dimensions(bx, y + 2, btnSize, btnHeight)
                .tooltip(Tooltip.of(Text.literal("Go to Fullscreen")))
                .build();
            widgets.add(modeToggleBtn);
            
            // Renderer mode toggle in the MIDDLE
            String title = "⬡ Field";
            int titleWidth = textRenderer.getWidth(title);
            int titleEndX = x + 4 + titleWidth + 8;
            int togglesStartX = bx - 4;  // Before the mode toggle
            int middleSpace = togglesStartX - titleEndX;
            
            // Renderer mode toggle - DEPRECATED (SimplifiedFieldRenderer removed, standard mode always used)
            // Space reserved for future features if needed
            
            // WINDOWED RIGHT PANEL HEADER:
            // [Label "Context"] [Preset dropdown] .... [R] [×] (float right)
            Bounds rightBounds = rightPanelBoundsSupplier.get();
            if (rightBounds != null && !rightBounds.equals(Bounds.EMPTY)) {
                int ry = rightBounds.y() + 2;
                int rRightEdge = rightBounds.right();
                int rBtnHeight = rightBounds.height() - 4;
                
                // Close button at far right of right panel
                closeBtn = ButtonWidget.builder(Text.literal("×"), btn -> onClose.run())
                    .dimensions(rRightEdge - btnSize - 2, ry, btnSize, rBtnHeight)
                    .tooltip(Tooltip.of(Text.literal("Close")))
                    .build();
                widgets.add(closeBtn);
                
                // Reset button left of close
                resetBtn = ButtonWidget.builder(Text.literal("R"), btn -> onReset.run())
                    .dimensions(rRightEdge - btnSize * 2 - 4, ry, btnSize, rBtnHeight)
                    .tooltip(Tooltip.of(Text.literal("Reset to defaults")))
                    .build();
                widgets.add(resetBtn);
                
                // Preset dropdown after "Context" label
                int contextLabelWidth = textRenderer.getWidth("Context");
                int dropdownX = rightBounds.x() + 4 + contextLabelWidth + 8;
                int dropdownWidth = Math.min(80, rRightEdge - btnSize * 2 - 12 - dropdownX);
                
                if (dropdownWidth > 40) {
                    PresetRegistry.loadAll();
                    List<String> presetNames = new ArrayList<>();
                    presetNames.add("Preset");
                    presetNames.addAll(PresetRegistry.listPresets());
                    if (presetNames.size() == 1) presetNames.add("None");
                    
                    rightPresetDropdown = new DropdownWidget<>(
                        textRenderer, dropdownX, ry, dropdownWidth, rBtnHeight,
                        presetNames, s -> Text.literal(s),
                        name -> {
                            if (!name.equals("Preset") && !name.equals("None")) {
                                onPresetSelected.accept(name);
                            }
                        }
                    );
                    widgets.add(rightPresetDropdown);
                }
            }
        } else {
            // FULLSCREEN HEADER:
            // [Label "⬡ Field Customizer"] [⬛] [◉] .... [RST] [×]
            
            String title = "⬡ Field Customizer";
            int labelWidth = textRenderer.getWidth(title);
            int toggleStartX = x + 4 + labelWidth + 8;
            
            modeToggleBtn = ButtonWidget.builder(
                    Text.literal("⬛"),
                    btn -> { onModeToggle.run(); })
                .dimensions(toggleStartX, y + 2, btnSize, btnHeight)
                .tooltip(Tooltip.of(Text.literal("Go to Windowed")))
                .build();
            widgets.add(modeToggleBtn);
            
            fieldToggleBtn = ButtonWidget.builder(
                    Text.literal(isFieldActive.getAsBoolean() ? "◉" : "○"),
                    btn -> {
                        FieldEditStateHolder.toggleTestField();
                        btn.setMessage(Text.literal(isFieldActive.getAsBoolean() ? "◉" : "○"));
                    })
                .dimensions(toggleStartX + btnSize + 2, y + 2, btnSize, btnHeight)
                .tooltip(Tooltip.of(Text.literal("Toggle field visibility")))
                .build();
            widgets.add(fieldToggleBtn);
            
            // RST and × at far right
            btnSize = 18;
            closeBtn = ButtonWidget.builder(Text.literal("×"), btn -> onClose.run())
                .dimensions(rightEdge - btnSize, y, btnSize, btnSize)
                .tooltip(Tooltip.of(Text.literal("Close")))
                .build();
            widgets.add(closeBtn);
            
            resetBtn = ButtonWidget.builder(Text.literal("RST"), btn -> onReset.run())
                .dimensions(rightEdge - btnSize - 2 - 40, y, 40, btnSize)
                .tooltip(Tooltip.of(Text.literal("Reset to defaults")))
                .build();
            widgets.add(resetBtn);
        }
    }
    
    @Override
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (bounds == null) return;
        
        boolean isWindowed = (currentMode.get() == GuiMode.WINDOWED);
        
        // Title - shorter in windowed mode
        String title = isWindowed ? "⬡ Field" : "⬡ Field Customizer";
        int titleX = bounds.x() + 4;
        int titleY = bounds.y() + (bounds.height() - textRenderer.fontHeight) / 2;
        
        context.drawTextWithShadow(textRenderer, title, titleX, titleY, 0xFFAAFFAA);  // Green
        
        // Dirty indicator removed - it's shown in status bar
        
        // WINDOWED: Right panel "Context" label
        if (isWindowed) {
            Bounds rightBounds = rightPanelBoundsSupplier.get();
            if (rightBounds != null && !rightBounds.equals(Bounds.EMPTY)) {
                int rTitleY = rightBounds.y() + (rightBounds.height() - textRenderer.fontHeight) / 2;
                context.drawTextWithShadow(textRenderer, "Context", rightBounds.x() + 4, rTitleY, 0xFFAAFFAA);
            }
        }
    }
    
    /**
     * Updates the mode toggle button text.
     */
    public void refreshModeButton() {
        if (modeToggleBtn != null) {
            modeToggleBtn.setMessage(Text.literal(currentMode.get() == GuiMode.FULLSCREEN ? "⬛" : "⬜"));
        }
    }
    
    /**
     * Updates the field toggle button text.
     */
    public void refreshFieldButton() {
        if (fieldToggleBtn != null) {
            fieldToggleBtn.setMessage(Text.literal(isFieldActive.getAsBoolean() ? "◉" : "○"));
        }
    }
    
    /**
     * Renders the dropdown overlay (must be called after all other rendering).
     */
    public void renderDropdownOverlay(DrawContext context, int mouseX, int mouseY) {
        if (rightPresetDropdown != null && rightPresetDropdown.isExpanded()) {
            rightPresetDropdown.renderOverlay(context, mouseX, mouseY);
        }
    }
    
    /**
     * Returns the right panel preset dropdown (for windowed mode).
     */
    public DropdownWidget<String> getRightPresetDropdown() {
        return rightPresetDropdown;
    }
}
