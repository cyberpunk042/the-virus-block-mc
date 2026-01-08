package net.cyberpunk042.client.gui.screen;

import net.cyberpunk042.client.gui.component.*;
import net.cyberpunk042.client.gui.layout.*;
import net.cyberpunk042.client.gui.panel.*;
import net.cyberpunk042.client.gui.panel.sub.*;
import net.cyberpunk042.client.gui.preview.FieldPreviewRenderer;
import net.cyberpunk042.client.gui.render.TestFieldRenderer;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.gui.widget.*;
import net.cyberpunk042.client.gui.util.GuiConfigPersistence;
import net.cyberpunk042.client.gui.util.WidgetVisibility;
import net.cyberpunk042.client.network.GuiPacketSender;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * G01: Main Field Customizer GUI screen.
 * 
 * This is a thin orchestrator that delegates to components:
 * - HeaderBar: title + mode/field/reset/close buttons
 * - TabBar: main tabs + preset dropdown + renderer toggle  
 * - SelectorBar: layer + primitive selectors
 * - ContentArea: sub-tab panes
 * - Layout: bounds calculation and background rendering
 */
public class FieldCustomizerScreen extends Screen {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELDS (~25 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final FieldEditState state;
    private GuiMode mode;
    private LayoutManager layout;
    
    // Components
    private HeaderBar headerBar;
    private TabBar tabBar;
    private SelectorBar selectorBar;
    private ContentArea contentArea;
    private StatusBar statusBar;
    private ShapeSubPanel shapePanel;
    private ProfilesPanel profilesPanel;
    
    // Factories and controllers
    private VisibilityController visibilityController;
    private ContentProviderFactory contentFactory;
    
    // State - load saved tab from persistence
    private TabType currentTab = GuiConfigPersistence.loadSavedTab();
    private DropdownWidget<String> presetDropdown;
    private ClickableWidget previewModeCheckbox;  // Kept for potential future use
    // Full 3D preview is now always used (better performance and visual quality)
    private PresetConfirmDialog presetConfirmDialog;
    private ModalDialog activeModal;
    private Bounds shapePanelBounds;
    private Bounds profilesLeftBounds, profilesRightBounds;
    private Runnable stateChangeListener;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS (~15 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public FieldCustomizerScreen() {
        this(FieldEditStateHolder.getOrCreate());
    }
    
    public FieldCustomizerScreen(FieldEditState state) {
        super(Text.literal("Field Customizer"));
        this.state = state;
        this.mode = GuiConfigPersistence.loadSavedMode();
        Logging.GUI.topic("screen").info("FieldCustomizerScreen created, mode={}", mode);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INIT (~40 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    protected void init() {
        super.init();
        
        // Ensure fragment presets are loaded (cached, only loads once)
        net.cyberpunk042.client.gui.util.FragmentRegistry.ensureLoaded();
        
        WidgetVisibility.clearAll();
        
        // State change listener for preview refresh
        stateChangeListener = this::onStateChanged;
        state.addChangeListener(stateChangeListener);
        
        // Selection change listener for content panel rebuild
        state.setSelectionChangeListener(this::onSelectionChanged);
        
        // Factories and controllers
        visibilityController = new VisibilityController(state, mode);
        contentFactory = new ContentProviderFactory(this, state, textRenderer, this::refreshSubTabWidgets);
        
        // Create layout strategy
        layout = (mode == GuiMode.FULLSCREEN) ? new FullscreenLayout() : new WindowedLayout();
        layout.calculate(width, height);
        
        // Create components (they auto-create their widgets)
        headerBar = new HeaderBar(textRenderer, this::toggleMode, this::resetState, this::close,
            FieldEditStateHolder::isTestFieldActive, state::isDirty, () -> mode,
            layout::getRightTitleBarBounds, this::onPresetSelected);
        headerBar.setBounds(layout.getTitleBarBounds());
        
        tabBar = new TabBar(textRenderer, visibilityController, this::switchTab, 
            this::onPresetSelected, this::refreshTabsForRendererMode);
        tabBar.setBounds(layout.getTabBarBounds());
        tabBar.setActiveTab(currentTab);
        presetDropdown = tabBar.getPresetDropdown();
        
        selectorBar = new SelectorBar(textRenderer, this::getLayerNames, this::getPrimitiveNames,
            state::getSelectedLayerIndex, state::getSelectedPrimitiveIndex,
            this::onLayerSelected, this::onPrimitiveSelected, this::showLayerModal, this::showPrimitiveModal,
            this::onLayerAdd, this::onPrimitiveAdd);
        selectorBar.setBounds(layout.getSelectorBounds());
        
        contentArea = new ContentArea(textRenderer, visibilityController, contentFactory, this::refreshSubTabWidgets);
        contentArea.setBounds(layout.getContentBounds());
        contentArea.setActiveMainTab(currentTab);
        
        statusBar = new StatusBar(state, textRenderer);
        statusBar.setBounds(layout.getStatusBarBounds());
        
        initShapePanel();
        initProfilesPanel();
        initPreviewCheckbox();
        
        registerWidgets();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET REGISTRATION (~15 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void registerWidgets() {
        clearChildren();
        
        // Add all component widgets
        addWidgetsFrom(headerBar);
        addWidgetsFrom(tabBar);
        if (currentTab != TabType.PROFILES) {
            addWidgetsFrom(selectorBar);
            // ContentArea: tab buttons are drawable (Screen renders)
            // Content panel widgets are selectable only (panel renders, Screen handles input)
            if (contentArea != null) {
                SubTabPane active = contentArea.getActiveSubTabPane();
                if (active != null) {
                    // Tab buttons - Screen renders these
                    for (var w : active.getWidgets()) addDrawableChild(w);
                    // Content widgets - Panel renders, Screen handles input
                    for (var w : active.getContentWidgets()) addSelectableChild(w);
                }
            }
            // ShapePanel: selectable only (panel renders with scroll)
            addWidgetsAsSelectableOnly(shapePanel);
        } else {
            addWidgetsFrom(profilesPanel);
        }
        if (previewModeCheckbox != null && currentTab != TabType.PROFILES) addDrawableChild(previewModeCheckbox);
        
        // Modal overlay
        if (activeModal != null && activeModal.isVisible()) {
            for (ClickableWidget w : activeModal.getWidgets()) addDrawableChild(w);
        }
    }
    
    private void addWidgetsFrom(Object component) {
        if (component == null) return;
        List<ClickableWidget> widgets = null;
        if (component instanceof ScreenComponent sc) widgets = sc.getWidgets();
        else if (component instanceof AbstractPanel ap) widgets = ap.getWidgets();
        if (widgets != null) for (ClickableWidget w : widgets) addDrawableChild(w);
    }
    
    /**
     * Adds widgets as selectable only (for input handling) but NOT as drawables.
     * Use this for components that handle their own rendering (e.g., with scroll).
     * 
     * <p>This prevents double-rendering: widgets receive input via Screen,
     * but are rendered by their parent panel with proper scroll handling.</p>
     */
    private void addWidgetsAsSelectableOnly(Object component) {
        if (component == null) return;
        List<ClickableWidget> widgets = null;
        if (component instanceof ScreenComponent sc) widgets = sc.getWidgets();
        else if (component instanceof AbstractPanel ap) widgets = ap.getWidgets();
        if (widgets != null) {
            for (ClickableWidget w : widgets) {
                addSelectableChild(w);  // Input only, NOT rendered by super.render()
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE & TAB SWITCHING (~30 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void toggleMode() {
        mode = mode.toggle();
        GuiConfigPersistence.saveMode(mode);
        clearChildren();
        init();
        Logging.GUI.topic("screen").info("Toggled to {} mode", mode);
    }
    
    private void resetState() {
        // Reload the current profile from disk (revert unsaved changes)
        String currentProfileName = state.profiles().getCurrentName();
        
        Logging.GUI.topic("screen").info("Reset requested for profile: '{}'", currentProfileName);
        
        var profileManager = net.cyberpunk042.client.profile.ProfileManager.getInstance();
        var profileOpt = profileManager.getProfile(currentProfileName);
        
        Logging.GUI.topic("screen").info("Profile lookup result: present={}, hasDef={}", 
            profileOpt.isPresent(), 
            profileOpt.isPresent() && profileOpt.get().definition() != null);
        
        if (profileOpt.isPresent() && profileOpt.get().definition() != null) {
            // Reload from saved profile
            state.loadFromDefinition(profileOpt.get().definition());
            state.clearDirty();
            ToastNotification.info("Reverted to saved: " + currentProfileName);
        } else {
            // No saved profile found - reset to programmatic defaults
            state.reset();
            ToastNotification.info("Reset to defaults (no saved profile: " + currentProfileName + ")");
        }
        
        clearChildren();
        init();
    }
    
    private void switchTab(TabType tab) {
        if (tab == currentTab) return;
        currentTab = tab;
        GuiConfigPersistence.saveTab(tab);  // Persist tab selection
        tabBar.setActiveTab(tab);
        contentArea.setActiveMainTab(tab);
        registerWidgets();
    }
    
    private void refreshSubTabWidgets() { registerWidgets(); }
    
    private void refreshTabsForRendererMode() {
        tabBar.refreshVisibility();
        if (contentArea != null) contentArea.refreshForRendererMode();
        registerWidgets();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACKS (~40 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void onStateChanged() {
        TestFieldRenderer.markDirty();
        if (state.getBool("livePreviewEnabled")) {
            GuiPacketSender.updateDebugField(state.toStateJson());
        }
    }
    
    /** Called when the selected layer or primitive changes. Rebuilds UI panels. */
    private void onSelectionChanged() {
        if (contentArea != null) {
            contentArea.rebuild();
            contentArea.setActiveMainTab(currentTab);
        }
        if (selectorBar != null) {
            selectorBar.refresh();
        }
        // IMPORTANT: Rebuild shape panel to show selected primitive's shape values
        initShapePanel();
        registerWidgets();
        TestFieldRenderer.markDirty();
    }
    
    private void onPresetSelected(String presetName) {
        if (presetName.equals("Select Preset…") || presetName.equals("No presets found")) return;
        presetConfirmDialog = new PresetConfirmDialog(presetName, confirmed -> {
            if (confirmed) {
                net.cyberpunk042.client.gui.util.PresetRegistry.applyPreset(state, presetName);
                ToastNotification.success("Applied preset: " + presetName);
            }
            presetDropdown.setSelectedIndex(0);
        });
        presetConfirmDialog.show(width, height);
    }
    
    private void onLayerSelected(String name) {
        int idx = getLayerNames().indexOf(name);
        state.setSelectedLayerIndex(idx);
        selectorBar.refreshPrimitives();
        contentArea.rebuild();  // Rebuild panels with new layer's primitive values
        contentArea.setActiveMainTab(currentTab);
        registerWidgets();
    }
    
    private void onPrimitiveSelected(String name) {
        int idx = getPrimitiveNames().indexOf(name);
        state.setSelectedPrimitiveIndex(idx);
        contentArea.rebuild();  // Rebuild panels with new primitive's values
        contentArea.setActiveMainTab(currentTab);
        registerWidgets();
    }
    
    private void onLayerAdd() {
        int newIdx = state.addLayer();
        state.setSelectedLayerIndex(newIdx);
        selectorBar.refreshLayers();
        selectorBar.selectLayerIndex(newIdx);
        selectorBar.refreshPrimitives();
        contentArea.rebuild();  // Rebuild panels with new layer's primitive values
        contentArea.setActiveMainTab(currentTab);
        registerWidgets();
    }
    
    private void onPrimitiveAdd() {
        int newIdx = state.addPrimitive(state.getSelectedLayerIndex());
        state.setSelectedPrimitiveIndex(newIdx);
        selectorBar.refreshPrimitives();
        selectorBar.selectPrimitiveIndex(newIdx);
        contentArea.rebuild();  // Rebuild panels with new primitive's values
        contentArea.setActiveMainTab(currentTab);
        registerWidgets();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODALS (~20 lines - delegates to ModalFactory)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void showLayerModal(String layerName) {
        int idx = state.getSelectedLayerIndex();
        activeModal = ModalFactory.createLayerModal(state, layerName, idx, textRenderer, width, height,
            newName -> { state.renameLayer(idx, newName); selectorBar.refreshLayers(); },
            () -> { state.removeLayer(idx); selectorBar.refreshLayers(); 
                    selectorBar.selectLayerIndex(Math.max(0, idx - 1)); 
                    ToastNotification.info("Layer deleted"); },
            () -> { activeModal = null; registerWidgets(); });
        activeModal.show();
        registerWidgets();
        ModalFactory.focusTextField(activeModal, this);
    }
    
    private void showPrimitiveModal(String primName) {
        int layerIdx = state.getSelectedLayerIndex();
        int primIdx = state.getSelectedPrimitiveIndex();
        activeModal = ModalFactory.createPrimitiveModal(state, primName, layerIdx, primIdx, textRenderer, width, height,
            newName -> { state.renamePrimitive(layerIdx, primIdx, newName); selectorBar.refreshPrimitives(); },
            () -> { state.removePrimitive(layerIdx, primIdx); selectorBar.refreshPrimitives();
                    selectorBar.selectPrimitiveIndex(Math.max(0, primIdx - 1));
                    ToastNotification.info("Primitive deleted"); },
            () -> { activeModal = null; registerWidgets(); });
        activeModal.show();
        registerWidgets();
        ModalFactory.focusTextField(activeModal, this);
    }
    
    /**
     * Shows a color input modal for entering color values.
     * Called by ColorButton widgets on right-click.
     * 
     * @param currentColorString Current color value (e.g., "@primary" or "#FF00FF")
     * @param onSubmit Callback when user submits a new color value
     */
    public void showColorInputModal(String currentColorString, java.util.function.Consumer<String> onSubmit) {
        activeModal = ModalFactory.createColorInputModal(
            currentColorString, textRenderer, width, height,
            colorString -> {
                onSubmit.accept(colorString);
                ToastNotification.success("Color updated");
            },
            () -> { activeModal = null; registerWidgets(); });
        activeModal.show();
        registerWidgets();
        ModalFactory.focusTextField(activeModal, this);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TICK & RENDER (~60 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void tick() {
        super.tick();
        if (contentArea != null) contentArea.tick();
        if (shapePanel != null) shapePanel.tick();
        if (currentTab == TabType.PROFILES && profilesPanel != null) profilesPanel.tick();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background and frame from layout
        layout.renderBackground(context, width, height);
        layout.renderFrame(context);
        
        // Components
        if (headerBar != null) headerBar.render(context, mouseX, mouseY, delta);
        
        if (currentTab != TabType.PROFILES) {
            if (selectorBar != null) selectorBar.render(context, mouseX, mouseY, delta);
            if (contentArea != null) contentArea.render(context, mouseX, mouseY, delta);
            if (shapePanel != null) renderPanelInBounds(context, shapePanel, shapePanelBounds, mouseX, mouseY, delta);
        } else {
            if (profilesPanel != null) profilesPanel.render(context, mouseX, mouseY, delta);
        }
        
        // 3D Preview (fullscreen only, NOT on Profiles tab which uses full left column)
        if (layout.hasPreviewWidget() && currentTab != TabType.PROFILES) {
            render3DPreview(context, layout.getPreviewBounds(), delta);
        }
        
        // Status bar
        if (statusBar != null) statusBar.render(context, mouseX, mouseY, delta);
        
        // Widgets
        super.render(context, mouseX, mouseY, delta);
        
        // Overlays
        if (presetDropdown != null && presetDropdown.isExpanded()) {
            presetDropdown.renderOverlay(context, mouseX, mouseY);
        }
        if (headerBar != null) {
            headerBar.renderDropdownOverlay(context, mouseX, mouseY);
        }
        if (activeModal != null && activeModal.isVisible()) {
            activeModal.render(context, mouseX, mouseY, delta);
            for (ClickableWidget w : activeModal.getWidgets()) w.render(context, mouseX, mouseY, delta);
        }
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            presetConfirmDialog.render(context, mouseX, mouseY, delta);
        }
        ToastNotification.renderAll(context, textRenderer, width, height);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INPUT HANDLERS (~100 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        if (activeModal != null && activeModal.isVisible()) return true;
        if (presetDropdown != null && presetDropdown.isExpanded() && presetDropdown.handleScroll(mouseX, mouseY, vAmt)) return true;
        if (currentTab != TabType.PROFILES) {
            if (contentArea != null && contentArea.mouseScrolled(mouseX, mouseY, hAmt, vAmt)) return true;
            // Forward to shapePanel for scroll handling
            if (shapePanel != null && shapePanelBounds != null && shapePanelBounds.contains(mouseX, mouseY)) {
                if (shapePanel.mouseScrolled(mouseX, mouseY, hAmt, vAmt)) return true;
            }
        } else {
            if (profilesPanel != null && profilesPanel.mouseScrolled(mouseX, mouseY, hAmt, vAmt)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // ═══════════════════════════════════════════════════════════════════
        // MODAL HANDLING - MUST be first to block ALL background interaction
        // ═══════════════════════════════════════════════════════════════════
        if (activeModal != null && activeModal.isVisible()) {
            // First, check if the modal's custom click handler wants to process this
            // (e.g., color palette grid clicks)
            if (activeModal.handleCustomClick(mouseX, mouseY)) {
                return true;  // Custom handler consumed the click
            }
            
            // Process modal widgets (buttons, text fields, etc.)
            for (ClickableWidget w : activeModal.getWidgets()) {
                if (w.isMouseOver(mouseX, mouseY)) {
                    // Set focus for the widget (important for text fields)
                    setFocused(w);
                    w.setFocused(true);
                    w.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
            // Click was inside modal but not on any widget - block it
            return true;
        }
        
        // Forward to ProfilesPanel dialog first (if visible)
        if (profilesPanel != null && profilesPanel.isDialogVisible()) {
            if (profilesPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        // Forward to ProfilesPanel for list selection when on PROFILES tab
        if (currentTab == TabType.PROFILES && profilesPanel != null) {
            if (profilesPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            presetConfirmDialog.mouseClicked((int)mouseX, (int)mouseY, button);
            return true;
        }
        if (presetDropdown != null && presetDropdown.isExpanded()) {
            if (presetDropdown.handleClick(mouseX, mouseY, button)) return true;
            presetDropdown.close();
        }
        
        // Let super.mouseClicked handle all registered widgets
        // Widgets are at visual positions, so this works correctly
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Forward to ProfilesPanel dialog first (if visible)
        if (profilesPanel != null && profilesPanel.isDialogVisible()) {
            if (profilesPanel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        // Modal handling
        if (activeModal != null && activeModal.isVisible()) {
            if (keyCode == 256) { activeModal.hide(); return true; }
            if (keyCode == 257 || keyCode == 335) { /* enter handled by button */ return super.keyPressed(keyCode, scanCode, modifiers); }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            if (keyCode == 256) { presetConfirmDialog.hide(); return true; }
            if (keyCode == 257 || keyCode == 335) { /* enter handled by button */ }
        }
        // Escape
        if (keyCode == 256) { close(); return true; }
        // Tab key toggles fullscreen/windowed mode (OLD: lines 1466-1470)
        if (keyCode == 258) { toggleMode(); return true; }
        // Tab switching (1-4)
        if (keyCode >= 49 && keyCode <= 52) {
            TabType[] tabs = TabType.values();
            int idx = keyCode - 49;
            if (idx < tabs.length) switchTab(tabs[idx]);
            return true;
        }
        // Undo/Redo
        if ((modifiers & 2) != 0) {
            if (keyCode == 90) { state.undo(); registerWidgets(); return true; }
            if (keyCode == 89) { state.redo(); registerWidgets(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Forward to ProfilesPanel dialog first (if visible)
        if (profilesPanel != null && profilesPanel.isDialogVisible()) {
            if (profilesPanel.charTyped(chr, modifiers)) {
                return true;
            }
        }
        if (activeModal != null && activeModal.isVisible()) return super.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE (~20 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean shouldPause() { return mode == GuiMode.FULLSCREEN; }
    
    @Override
    protected void applyBlur(DrawContext context) {
        // In windowed mode, skip blur so game world is clearly visible
        if (mode == GuiMode.FULLSCREEN) {
            super.applyBlur(context);
        }
        // In windowed mode, do nothing (no blur)
    }
    
    @Override
    public void close() {
        if (presetConfirmDialog != null && presetConfirmDialog.isVisible()) {
            presetConfirmDialog.hide();
            return;
        }
        if (stateChangeListener != null) state.removeChangeListener(stateChangeListener);
        WidgetVisibility.clearAll();
        super.close();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS (~40 lines)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void initShapePanel() {
        shapePanelBounds = layout.getShapePanelBounds();
        shapePanel = new ShapeSubPanel(this, state, 0);
        shapePanel.setWarningCallback((w, c) -> { if (statusBar != null) { if (w != null) statusBar.setWarning(w, c); else statusBar.clearWarning(); }});
        // IMPORTANT: Re-register widgets AND rebuild content panels when shape type changes
        shapePanel.setShapeChangedCallback(() -> {
            // Rebuild content area (this reinitializes FillSubPanel with new shape adapter)
            if (contentArea != null) {
                contentArea.rebuild();
                contentArea.setActiveMainTab(currentTab);
            }
            registerWidgets();
        });
        shapePanel.init(shapePanelBounds.width(), shapePanelBounds.height());
        shapePanel.setBoundsQuiet(shapePanelBounds);
        shapePanel.applyBoundsOffset();
    }
    
    private void initProfilesPanel() {
        profilesLeftBounds = layout.getProfilesLeftBounds();
        profilesRightBounds = layout.getProfilesRightBounds();
        profilesPanel = new ProfilesPanel(this, state);
        profilesPanel.setDualBounds(profilesLeftBounds, profilesRightBounds);
        profilesPanel.init(profilesLeftBounds.width(), profilesLeftBounds.height());
        if (mode == GuiMode.FULLSCREEN) profilesPanel.applyBoundsOffset();
    }
    
    private void initPreviewCheckbox() {
        // Full 3D preview is now permanent - no toggle needed
        previewModeCheckbox = null;
    }
    
    private List<String> getLayerNames() {
        return state.getFieldLayers().stream().map(l -> l.id()).toList();
    }
    
    private List<String> getPrimitiveNames() {
        return state.getPrimitivesForLayer(state.getSelectedLayerIndex())
            .stream().map(p -> p.id()).collect(java.util.stream.Collectors.toList());
    }
    
    private void renderPanelInBounds(DrawContext ctx, AbstractPanel panel, Bounds bounds, int mouseX, int mouseY, float delta) {
        if (panel == null || bounds == null) return;
        ctx.enableScissor(bounds.x(), bounds.y(), bounds.right(), bounds.bottom());
        panel.render(ctx, mouseX, mouseY, delta);
        ctx.disableScissor();
    }
    
    private void render3DPreview(DrawContext context, Bounds bounds, float delta) {
        if (bounds == null || bounds.isEmpty()) return;
        FieldPreviewRenderer.drawField(context, state, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), true);  // Always use full 3D renderer
    }
}
