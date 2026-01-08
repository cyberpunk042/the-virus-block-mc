package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.client.network.GuiPacketSender;
import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Force Field controls sub-panel.
 * 
 * <p>Uses FragmentRegistry to load presets from field_force/*.json</p>
 * <p>Has Configure button that opens ForceConfigModal</p>
 * <p>Has Spawn button with location selector to actually spawn force fields</p>
 */
public class ForceSubPanel extends AbstractPanel {
    
    private static final String DEFAULT_PRESET = "Default";
    private static final String CUSTOM_PRESET = "Custom";
    
    /**
     * Spawn location options for force field placement relative to player.
     */
    public enum SpawnLocation {
        IN_FRONT("In Front", 3.0f, 0f, 0f),
        FAR_IN_FRONT("Far Front", 8.0f, 0f, 0f),
        BEHIND("Behind", -3.0f, 0f, 0f),
        FAR_BEHIND("Far Behind", -8.0f, 0f, 0f),
        AT_PLAYER("At Player", 0f, 0f, 0f),
        ABOVE("Above", 0f, 5.0f, 0f),
        BELOW("Below", 0f, -3.0f, 0f);
        
        private final String displayName;
        private final float offsetForward;
        private final float offsetVertical;
        private final float offsetSide;
        
        SpawnLocation(String displayName, float forward, float vertical, float side) {
            this.displayName = displayName;
            this.offsetForward = forward;
            this.offsetVertical = vertical;
            this.offsetSide = side;
        }
        
        public String getDisplayName() { return displayName; }
        public float getOffsetForward() { return offsetForward; }
        public float getOffsetVertical() { return offsetVertical; }
        public float getOffsetSide() { return offsetSide; }
    }
    
    private int startY;
    private CyclingButtonWidget<String> presetDropdown;
    private CyclingButtonWidget<SpawnLocation> locationDropdown;
    private LabeledSlider durationSlider;
    private ButtonWidget spawnButton;
    private ButtonWidget configureButton;
    
    private String selectedPreset = DEFAULT_PRESET;
    private ForceFieldConfig customConfig = null;
    private SpawnLocation spawnLocation = SpawnLocation.IN_FRONT;
    private int duration = 120;
    
    // Callback to open the Force config modal (set by screen)
    // Takes the current config as parameter so the modal opens with correct state
    private Consumer<ForceFieldConfig> onConfigureRequest;
    
    public ForceSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        FragmentRegistry.ensureLoaded();
        Logging.GUI.topic("panel").debug("ForceSubPanel created");
    }
    
    /**
     * Sets the callback for when Configure button is clicked.
     * The screen should set this to open ForceConfigModal with the provided config.
     * 
     * @param callback Receives the current config to edit
     */
    public void setOnConfigureRequest(Consumer<ForceFieldConfig> callback) {
        this.onConfigureRequest = callback;
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int x = GuiConstants.PADDING;
        int y = startY + GuiConstants.PADDING;
        int fullW = width - GuiConstants.PADDING * 2;
        int halfW = (fullW - GuiConstants.PADDING) / 2;
        
        // Build preset names: Default + loaded presets + Custom
        List<String> presetList = new ArrayList<>();
        presetList.add(DEFAULT_PRESET);
        presetList.addAll(FragmentRegistry.listForceFragments());
        presetList.add(CUSTOM_PRESET);
        String[] presetNames = presetList.toArray(new String[0]);
        
        // Preset dropdown (full width) - APPLIES force config to current field
        presetDropdown = CyclingButtonWidget.<String>builder(s -> net.minecraft.text.Text.literal(s))
            .values(presetNames)
            .initially(selectedPreset)
            .build(x, y, fullW, GuiConstants.WIDGET_HEIGHT, 
                net.minecraft.text.Text.literal("Force"), 
                (btn, value) -> {
                    selectedPreset = value;
                    applyPresetToState(value);
                    Logging.GUI.topic("force").debug("Applied force preset: {}", value);
                });
        widgets.add(presetDropdown);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Duration slider (full width)
        durationSlider = LabeledSlider.builder("Duration (ticks)")
            .position(x, y).width(fullW)
            .range(20, 600).initial(duration).step(10).format("%d")
            .onChange(v -> duration = v.intValue())
            .build();
        widgets.add(durationSlider);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // ═══════════════════════════════════════════════════════════════════
        // SPAWN ROW: [Location dropdown] [▶ Spawn button]
        // ═══════════════════════════════════════════════════════════════════
        
        // Location dropdown (left half) - cycles through spawn locations
        locationDropdown = CyclingButtonWidget.<SpawnLocation>builder(
                loc -> net.minecraft.text.Text.literal("@ " + loc.getDisplayName()))
            .values(SpawnLocation.values())
            .initially(spawnLocation)
            .build(x, y, halfW, GuiConstants.WIDGET_HEIGHT, 
                net.minecraft.text.Text.literal("Location"), 
                (btn, value) -> {
                    spawnLocation = value;
                    Logging.GUI.topic("force").debug("Spawn location: {}", value.getDisplayName());
                });
        widgets.add(locationDropdown);
        
        // Spawn button (right half)
        spawnButton = GuiWidgets.button(x + halfW + GuiConstants.PADDING, y, halfW, 
            "▶ Spawn", "Spawn force field at selected location", this::spawnForceField);
        widgets.add(spawnButton);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        // Configure button (full width)
        configureButton = GuiWidgets.button(x, y, fullW, 
            "⚙ Configure...", "Open full force field configuration", this::openConfigure);
        widgets.add(configureButton);
        y += GuiConstants.WIDGET_HEIGHT + GuiConstants.PADDING;
        
        contentHeight = y - startY + GuiConstants.PADDING;
        Logging.GUI.topic("panel").debug("ForceSubPanel initialized with {} presets", presetNames.length);
    }
    
    private void openConfigure() {
        if (onConfigureRequest != null) {
            // Pass the CURRENT selected config to the modal
            ForceFieldConfig config = getSelectedConfig();
            onConfigureRequest.accept(config);
        } else {
            ToastNotification.error("Configure not available");
            Logging.GUI.topic("force").warn("onConfigureRequest not set");
        }
    }
    
    private void spawnForceField() {
        // The state IS the current field being edited - spawn that!
        // No need to load from files - the forceConfig is already part of the state
        
        // Get the current field definition as JSON
        String fieldJson = state.toStateJson();
        
        if (fieldJson == null || fieldJson.isEmpty()) {
            ToastNotification.error("No field to spawn");
            return;
        }
        
        // Send to server to spawn at the selected location
        GuiPacketSender.spawnForceField(fieldJson, duration, 
            spawnLocation.getOffsetForward(),
            spawnLocation.getOffsetVertical(),
            spawnLocation.getOffsetSide());
        
        ToastNotification.success("Force field spawned @ " + spawnLocation.getDisplayName());
        Logging.GUI.topic("force").info("Spawned force field for {} ticks at {}", 
            duration, spawnLocation.getDisplayName());
    }
    
    /**
     * Applies a force preset to the FieldEditState's forceConfig.
     */
    private void applyPresetToState(String presetName) {
        if (DEFAULT_PRESET.equals(presetName)) {
            state.set("forceConfig", null);
            return;
        }
        if (CUSTOM_PRESET.equals(presetName)) {
            return; // Keep existing config
        }
        
        var jsonOpt = FragmentRegistry.getForceJson(presetName);
        if (jsonOpt.isEmpty()) {
            Logging.GUI.topic("force").warn("Force preset not found: {}", presetName);
            return;
        }
        
        ForceFieldConfig config = ForceFieldConfig.fromJson(jsonOpt.get());
        state.set("forceConfig", config);
        customConfig = config; // Also keep local reference
        ToastNotification.info("Applied: " + presetName);
    }
    
    /**
     * Gets the current force configuration from state.
     */
    public ForceFieldConfig getSelectedConfig() {
        ForceFieldConfig cfg = state.forceConfig();
        return cfg != null ? cfg : ForceFieldConfig.DEFAULT;
    }
    
    /**
     * Sets custom config (called after modal save).
     */
    public void setCustomConfig(ForceFieldConfig config) {
        this.customConfig = config;
        this.selectedPreset = CUSTOM_PRESET;
        
        // Apply to state
        state.set("forceConfig", config);
        
        // Update dropdown to show Custom
        if (presetDropdown != null) {
            while (!presetDropdown.getValue().equals(CUSTOM_PRESET)) {
                presetDropdown.onPress();
            }
        }
    }
    
    @Override public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (var widget : widgets) widget.render(context, mouseX, mouseY, delta);
    }
    
    public int getHeight() { return contentHeight; }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        return new ArrayList<>(widgets);
    }
}
