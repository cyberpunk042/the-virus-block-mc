package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditState.ProfileEntry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.cyberpunk042.field.category.PresetCategory;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Global bottom action bar with two-tier preset selection.
 * Layout: [Category▼][Preset▼] ... [Profile▼][SAVE][REVERT]
 */
public class BottomActionBar {

    private final FieldEditState state;
    private final Consumer<String> onSave;
    private final Consumer<String> onRevert;
    private final Runnable onProfileChanged;

    // Two-tier preset selection
    private CyclingButtonWidget<PresetCategory> presetCategoryDropdown;
    private CyclingButtonWidget<String> presetDropdown;
    private PresetCategory selectedCategory = PresetCategory.STYLE;
    
    // Profile selection
    private CyclingButtonWidget<String> profileDropdown;
    private ButtonWidget saveBtn;
    private ButtonWidget revertBtn;
    
    private String currentPreset = "None";
    private int x, y, width;
    private int screenWidth, screenHeight;
    private boolean visible = true;

    public BottomActionBar(FieldEditState state, Consumer<String> onSave, Consumer<String> onRevert, Runnable onProfileChanged) {
        this.state = state;
        this.onSave = onSave;
        this.onRevert = onRevert;
        this.onProfileChanged = onProfileChanged;
    }

    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.width = screenWidth;
        this.x = 0;
        this.y = screenHeight - GuiConstants.BOTTOM_BAR_HEIGHT;

        int padding = GuiConstants.PADDING;
        int categoryWidth = 100;
        int presetWidth = 130;
        int profileWidth = 150;
        int btnWidth = 70;

        // ═══════════════════════════════════════════════════════════════
        // LEFT: Two-tier preset selection [Category▼][Preset▼]
        // ═══════════════════════════════════════════════════════════════
        
        // Category dropdown
        List<PresetCategory> categories = List.of(PresetCategory.values());
        presetCategoryDropdown = CyclingButtonWidget.<PresetCategory>builder(
                cat -> Text.literal(cat.getDisplayName()))
            .values(categories)
            .initially(selectedCategory)
            .build(padding, y + 4, categoryWidth, 20, Text.literal("Category"), 
                (btn, value) -> {
                    selectedCategory = value;
                    refreshPresetDropdown();
                });

        // Preset dropdown (filtered by category)
        createPresetDropdown(padding + categoryWidth + 4, y + 4, presetWidth);

        // ═══════════════════════════════════════════════════════════════
        // RIGHT: Profile + Save + Revert
        // ═══════════════════════════════════════════════════════════════
        
        int revertX = width - padding - btnWidth;
        int saveX = revertX - padding - btnWidth;
        int profileX = saveX - padding - profileWidth;

        List<String> profileNames = state.getProfiles().stream().map(ProfileEntry::name).toList();
        if (profileNames.isEmpty()) {
            profileNames = List.of("Default");
        }
        
        profileDropdown = GuiWidgets.stringDropdown(
            profileX, y + 4, profileWidth,
            "Profile", profileNames, state.getCurrentProfileName(),
            "Select profile to load",
            this::onProfileSelect
        );

        saveBtn = GuiWidgets.button(saveX, y + 4, btnWidth, "Save", "Save current profile", this::doSave);
        revertBtn = GuiWidgets.button(revertX, y + 4, btnWidth, "Revert", "Revert to last saved", this::doRevert);

        updateButtonStates();
    }
    
    private void createPresetDropdown(int xPos, int yPos, int dropdownWidth) {
        // Get presets for selected category
        List<PresetRegistry.PresetEntry> presets = PresetRegistry.getPresets(selectedCategory);
        List<String> presetIds = new ArrayList<>();
        presetIds.add("None");
        presets.forEach(p -> presetIds.add(p.id()));
        
        presetDropdown = CyclingButtonWidget.<String>builder(
                id -> {
                    if ("None".equals(id)) return Text.literal("None");
                    return PresetRegistry.getPreset(id)
                        .map(p -> Text.literal(p.name()))
                        .orElse(Text.literal(id));
                })
            .values(presetIds)
            .initially("None")
            .build(xPos, yPos, dropdownWidth, 20, Text.literal("Preset"),
                (btn, value) -> onPresetSelect(value));
    }
    
    private void refreshPresetDropdown() {
        // Recreate preset dropdown with new category's presets
        if (presetDropdown != null) {
            int xPos = presetDropdown.getX();
            int yPos = presetDropdown.getY();
            int w = presetDropdown.getWidth();
            createPresetDropdown(xPos, yPos, w);
        }
        currentPreset = "None";
    }

    private void onPresetSelect(String presetName) {
        if (!"None".equals(presetName)) {
            PresetRegistry.applyPreset(state, presetName);
            currentPreset = presetName;
            state.markDirty();
            
            // Show what categories were affected
            List<String> affected = PresetRegistry.getAffectedCategories(presetName);
            ToastNotification.success("Applied: " + presetName + " → " + String.join(", ", affected));
        } else {
            currentPreset = "None";
        }
        updateButtonStates();
    }

    private void onProfileSelect(String profileName) {
        if (!profileName.equals(state.getCurrentProfileName())) {
            boolean isServer = state.getProfiles().stream()
                .filter(p -> p.name().equals(profileName))
                .findFirst()
                .map(ProfileEntry::isServer)
                .orElse(false);
            state.setCurrentProfile(profileName, isServer);
            currentPreset = "None";
            if (presetDropdown != null) presetDropdown.setValue("None");
            updateButtonStates();
            onProfileChanged.run();
        }
    }

    private void doSave() {
        String name = state.getCurrentProfileName();
        onSave.accept(name);
        state.clearDirty();
        updateButtonStates();
    }

    private void doRevert() {
        onRevert.accept(state.getCurrentProfileName());
        state.clearDirty();
        currentPreset = "None";
        if (presetDropdown != null) presetDropdown.setValue("None");
        updateButtonStates();
    }

    public void updateButtonStates() {
        boolean dirty = state.isDirty();
        boolean serverProfile = state.isCurrentProfileServerSourced();
        if (saveBtn != null) {
            saveBtn.active = dirty;
            saveBtn.setMessage(Text.literal(serverProfile ? "Save As" : "Save"));
        }
        if (revertBtn != null) {
            revertBtn.active = dirty && !serverProfile;
        }
    }

    public String getCurrentPreset() { return currentPreset; }
    
    public PresetCategory getSelectedCategory() { return selectedCategory; }
    
    public void resetPreset() {
        currentPreset = "None";
        if (presetDropdown != null) presetDropdown.setValue("None");
    }
    
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        // Background
        context.fill(x, y, x + width, y + GuiConstants.BOTTOM_BAR_HEIGHT, GuiConstants.BG_SECONDARY);
        context.drawHorizontalLine(x, x + width - 1, y, GuiConstants.BORDER);
        
        // Render widgets
        if (presetCategoryDropdown != null) presetCategoryDropdown.render(context, mouseX, mouseY, delta);
        if (presetDropdown != null) presetDropdown.render(context, mouseX, mouseY, delta);
        if (profileDropdown != null) profileDropdown.render(context, mouseX, mouseY, delta);
        if (saveBtn != null) saveBtn.render(context, mouseX, mouseY, delta);
        if (revertBtn != null) revertBtn.render(context, mouseX, mouseY, delta);
        
        // Show current preset hint if one is selected
        if (!"None".equals(currentPreset)) {
            var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            List<String> affected = PresetRegistry.getAffectedCategories(currentPreset);
            String hint = "→ " + String.join(", ", affected);
            int hintX = presetDropdown != null ? presetDropdown.getX() + presetDropdown.getWidth() + 8 : 250;
            context.drawText(textRenderer, hint, hintX, y + 10, GuiConstants.TEXT_SECONDARY, false);
        }
        
        // Dirty indicator
        if (state.isDirty()) {
            var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int indicatorX = width / 2;
            context.drawText(textRenderer, "● Unsaved", indicatorX, y + 10, GuiConstants.WARNING, false);
        }
    }

    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        List<net.minecraft.client.gui.widget.ClickableWidget> list = new ArrayList<>();
        if (presetCategoryDropdown != null) list.add(presetCategoryDropdown);
        if (presetDropdown != null) list.add(presetDropdown);
        if (profileDropdown != null) list.add(profileDropdown);
        if (saveBtn != null) list.add(saveBtn);
        if (revertBtn != null) list.add(revertBtn);
        return list;
    }

    public int getHeight() { return GuiConstants.BOTTOM_BAR_HEIGHT; }
}
