package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.panel.layout.DualColumnLayout;
import net.cyberpunk042.client.gui.panel.layout.ProfilesPanelLayout;
import net.cyberpunk042.client.gui.panel.layout.SingleColumnLayout;
import net.cyberpunk042.client.gui.panel.service.ProfileActionService;
import net.cyberpunk042.client.gui.panel.service.ProfileActionService.Result;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditState.ProfileEntry;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.widget.ModalDialog;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * G91-G100: Profiles management panel.
 * 
 * <p>Uses {@link ProfilesPanelLayout} for positioning and
 * {@link ProfileActionService} for business logic.</p>
 */
public class ProfilesPanel extends AbstractPanel {
    
    private static final int ITEM_HEIGHT = 22;
    private static final int LIST_HEADER_HEIGHT = 20;
    private static final int BTN_HEIGHT = 20;
    
    // Infrastructure
    private ProfilesPanelLayout layout;
    private final ProfileActionService actionService;
    
    // Data
    private final List<ProfileEntry> allProfiles;
    private List<ProfileEntry> filteredProfiles;
    private int selectedProfileIndex = 0;
    private int listScrollOffset = 0;
    
    // Filters
    private String selectedSource = null;
    private String selectedCategory = null;
    
    // Widgets
    private CyclingButtonWidget<String> sourceFilter;
    private CyclingButtonWidget<String> categoryFilter;
    private TextFieldWidget searchField;
    private TextFieldWidget nameField;
    private ButtonWidget loadBtn, saveBtn, saveAsBtn, deleteBtn;
    private ButtonWidget duplicateBtn, renameBtn, factoryResetBtn;
    private ButtonWidget saveToServerBtn;
    
    // Modal
    private ModalDialog activeDialog;
    
    public ProfilesPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        this.allProfiles = state.getProfiles();
        this.filteredProfiles = new ArrayList<>(allProfiles);
        this.actionService = new ProfileActionService(state);
    }
    
    /**
     * Sets dual-panel bounds. Call before init() for dual-column mode.
     */
    public void setDualBounds(Bounds left, Bounds right) {
        this.layout = new DualColumnLayout(left, right);
        this.bounds = left;
    }
    
    public boolean isDualMode() {
        return layout != null && layout.isDualMode();
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        // Default to single layout if not set
        if (layout == null) {
            layout = new SingleColumnLayout(new Bounds(0, 0, width, height));
        }
        
        initFilters();
        initButtons();
        applyFilters();
        updateButtonStates();
    }
    
    private void initFilters() {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Bounds f = layout.filterArea();
        int halfW = (f.width() - GuiConstants.PADDING) / 2;
        
        sourceFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(List.of("All", "Local", "Server", "Bundled"))
            .initially("All")
            .build(f.x(), f.y(), halfW, 20, Text.literal("Source"),
                (btn, val) -> { selectedSource = "All".equals(val) ? null : val; applyFilters(); });
        
        List<String> cats = layout.isDualMode() 
            ? List.of("All", "Combat", "Utility", "Decorative", "Experimental")
            : List.of("All", "Combat", "Utility", "Deco", "Exp");
        categoryFilter = CyclingButtonWidget.<String>builder(Text::literal)
            .values(cats)
            .initially("All")
            .build(f.x() + halfW + GuiConstants.PADDING, f.y(), halfW, 20, Text.literal("Category"),
                (btn, val) -> { selectedCategory = "All".equals(val) ? null : val; applyFilters(); });
        
        int searchY = layout.isDualMode() ? f.y() + 24 : f.y();
        int searchW = layout.isDualMode() ? f.width() : Math.max(40, f.width() - halfW * 2 - 4);
        int searchX = layout.isDualMode() ? f.x() : f.x() + halfW * 2 + 4;
        
        if (layout.isDualMode()) {
            searchField = new TextFieldWidget(tr, f.x(), f.y() + 24, f.width(), 20, Text.literal("Search"));
            searchField.setPlaceholder(Text.literal("🔍 Search profiles..."));
        } else {
            searchField = new TextFieldWidget(tr, searchX, searchY, searchW, 20, Text.literal("Search"));
            searchField.setPlaceholder(Text.literal("🔍"));
        }
        searchField.setChangedListener(text -> applyFilters());
        
        Bounds n = layout.nameFieldArea();
        nameField = new TextFieldWidget(tr, n.x(), n.y(), n.width(), n.height(), Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setText(getSelectedProfileName());
    }
    
    private void initButtons() {
        Bounds a = layout.actionsArea();
        int bw = layout.buttonWidth();
        int gap = layout.buttonGap();
        int x = a.x();
        int y = a.y();
        
        // Row 1
        loadBtn = button(x, y, bw, "Load", "Load selected profile", this::onLoad);
        saveBtn = button(x + bw + gap, y, bw, "Save", "Save current changes", this::onSave);
        deleteBtn = button(x + (bw + gap) * 2, y, bw, layout.isDualMode() ? "Delete" : "Del", "Delete selected profile", this::onDelete);
        
        y += BTN_HEIGHT + gap;
        
        // Row 2
        saveAsBtn = button(x, y, bw, layout.isDualMode() ? "Save As" : "As", "Save as new profile", this::onSaveAs);
        duplicateBtn = button(x + bw + gap, y, bw, layout.isDualMode() ? "Copy" : "Dup", "Duplicate profile", this::onDuplicate);
        renameBtn = button(x + (bw + gap) * 2, y, bw, layout.isDualMode() ? "Rename" : "Ren", "Rename profile", this::onRename);
        factoryResetBtn = button(x + (bw + gap) * 2, y, bw, "⟲ Reset", "Reset to factory defaults", this::onFactoryReset);
        
        // Row 3 (dual mode only)
        if (layout.showServerButton()) {
            y += BTN_HEIGHT + gap;
            saveToServerBtn = button(x, y, a.width(), "⚡ Save to Server", "Save as server profile (OP only)", this::onSaveToServer);
        }
    }
    
    private ButtonWidget button(int x, int y, int w, String label, String tooltip, Runnable action) {
        return ButtonWidget.builder(Text.literal(label), btn -> action.run())
            .dimensions(x, y, w, BTN_HEIGHT)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(tooltip)))
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILTERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void applyFilters() {
        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        
        filteredProfiles = allProfiles.stream()
            .filter(p -> matchesSource(p) && matchesCategory(p) && matchesSearch(p, search))
            .sorted(Comparator.comparingInt(this::sourcePriority).thenComparing(ProfileEntry::name, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
        
        selectedProfileIndex = Math.min(selectedProfileIndex, Math.max(0, filteredProfiles.size() - 1));
        listScrollOffset = 0;
        if (nameField != null) nameField.setText(getSelectedProfileName());
        updateButtonStates();
    }
    
    private boolean matchesSource(ProfileEntry p) {
        if (selectedSource == null) return true;
        return switch (selectedSource) {
            case "Local" -> p.isLocal();
            case "Server" -> p.isServer();
            case "Bundled" -> p.isBundled();
            default -> true;
        };
    }
    
    private boolean matchesCategory(ProfileEntry p) {
        if (selectedCategory == null) return true;
        return p.description() != null && p.description().toLowerCase().contains(selectedCategory.toLowerCase());
    }
    
    private boolean matchesSearch(ProfileEntry p, String search) {
        return search.isEmpty() || p.name().toLowerCase().contains(search);
    }
    
    private int sourcePriority(ProfileEntry p) {
        return p.isLocal() ? 0 : (p.isBundled() ? 1 : 2);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS (thin wrappers around service)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void onLoad() { 
        actionService.load(getSelectedProfile()).handle(ToastNotification::success, ToastNotification::error);
    }
    
    private void onSave() { 
        actionService.save(getSelectedProfile()).handle(ToastNotification::success, ToastNotification::error);
        updateButtonStates();
    }
    
    private void onSaveAs() {
        activeDialog = ModalDialog.textInput("Save Profile As", "new_profile",
            MinecraftClient.getInstance().textRenderer, parent.width, parent.height, name -> {
                Result<String> valid = actionService.validateNewName(name, allProfiles);
                if (!valid.isSuccess()) { ToastNotification.warning(valid.error()); return; }
                
                actionService.saveAs(valid.value()).handle(msg -> {
                    syncProfiles();
                    selectProfileByName(valid.value());
                    ToastNotification.success(msg);
                }, ToastNotification::error);
            });
        activeDialog.show();
    }
    
    private void onDelete() {
        ProfileEntry entry = getSelectedProfile();
        Result<Void> can = actionService.canDelete(entry);
        if (!can.isSuccess()) { ToastNotification.warning(can.error()); return; }
        
        actionService.delete(entry).handle(msg -> {
            syncProfiles();
            selectedProfileIndex = -1;
            ToastNotification.success(msg);
        }, ToastNotification::error);
    }
    
    private void onDuplicate() {
        ProfileEntry entry = getSelectedProfile();
        if (entry == null) return;
        String copyName = actionService.generateCopyName(entry.name(), allProfiles);
        allProfiles.add(new ProfileEntry(copyName, false, entry.description()));
        applyFilters();
        ToastNotification.success("Duplicated: " + copyName);
    }
    
    private void onRename() {
        ProfileEntry entry = getSelectedProfile();
        String newName = nameField != null ? nameField.getText().trim() : "";
        Result<Void> can = actionService.canRename(entry, newName, allProfiles);
        if (!can.isSuccess()) { ToastNotification.warning(can.error()); return; }
        
        int idx = allProfiles.indexOf(entry);
        if (idx >= 0) {
            allProfiles.set(idx, new ProfileEntry(newName, entry.isServer(), entry.description()));
            applyFilters();
            ToastNotification.success("Renamed to: " + newName);
        }
    }
    
    private void onFactoryReset() {
        // Reset reloads the CURRENTLY LOADED profile, not the selected list item
        String currentName = state.getCurrentProfileName();
        if (currentName == null || currentName.isEmpty()) {
            currentName = "default";
        }
        // Find or create the profile entry for the current profile
        ProfileEntry currentEntry = null;
        for (ProfileEntry p : allProfiles) {
            if (p.name().equalsIgnoreCase(currentName)) {
                currentEntry = p;
                break;
            }
        }
        if (currentEntry == null) {
            // Fallback: create a default entry
            currentEntry = new ProfileEntry(currentName, false, "");
        }
        actionService.factoryReset(currentEntry).handle(ToastNotification::success, ToastNotification::error);
    }
    
    private void onSaveToServer() {
        actionService.saveToServer(getSelectedProfile()).handle(ToastNotification::info, ToastNotification::error);
    }
    
    private void syncProfiles() {
        state.profiles().syncFromFileProfileManager();
        allProfiles.clear();
        allProfiles.addAll(state.getProfiles());
        applyFilters();
    }
    
    private void selectProfileByName(String name) {
        for (int i = 0; i < filteredProfiles.size(); i++) {
            if (filteredProfiles.get(i).name().equals(name)) {
                selectedProfileIndex = i;
                break;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String getSelectedProfileName() {
        ProfileEntry e = getSelectedProfile();
        return e != null ? e.name() : "";
    }
    
    private ProfileEntry getSelectedProfile() {
        return (selectedProfileIndex >= 0 && selectedProfileIndex < filteredProfiles.size())
            ? filteredProfiles.get(selectedProfileIndex) : null;
    }
    
    private void updateButtonStates() {
        ProfileEntry sel = getSelectedProfile();
        boolean has = sel != null;
        boolean canMod = has && !sel.isServer() && !sel.isBundled();
        boolean canDel = canMod && !actionService.isDefaultProfile(sel);
        boolean showReset = has && (actionService.isDefaultProfile(sel) || sel.isBundled());
        
        if (loadBtn != null) loadBtn.active = has;
        if (saveBtn != null) saveBtn.active = canMod;
        if (deleteBtn != null) deleteBtn.active = canDel;
        if (saveAsBtn != null) saveAsBtn.active = true;
        if (duplicateBtn != null) duplicateBtn.active = has;
        if (renameBtn != null) { renameBtn.visible = !showReset; renameBtn.active = canMod; }
        if (factoryResetBtn != null) { factoryResetBtn.visible = showReset; factoryResetBtn.active = showReset; }
        if (saveToServerBtn != null) { 
            boolean op = actionService.isPlayerOp();
            saveToServerBtn.visible = op; 
            saveToServerBtn.active = op && has; 
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override public void tick() {}
    
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        
        // Panel backgrounds
        if (layout instanceof DualColumnLayout dual) {
            renderPanelBg(ctx, dual.leftPanel(), "Profiles");
            renderPanelBg(ctx, dual.rightPanel(), "Details");
            renderDetails(ctx, tr, layout.detailsArea());
        } else {
            renderPanelBg(ctx, layout.fullBounds(), null);
        }
        
        // Profile list
        renderList(ctx, tr, mx, my);
        
        // Widgets
        renderWidget(ctx, mx, my, delta, sourceFilter);
        renderWidget(ctx, mx, my, delta, categoryFilter);
        renderWidget(ctx, mx, my, delta, searchField);
        renderWidget(ctx, mx, my, delta, nameField);
        renderWidget(ctx, mx, my, delta, loadBtn);
        renderWidget(ctx, mx, my, delta, saveBtn);
        renderWidget(ctx, mx, my, delta, deleteBtn);
        renderWidget(ctx, mx, my, delta, saveAsBtn);
        renderWidget(ctx, mx, my, delta, duplicateBtn);
        if (actionService.isDefaultProfile(getSelectedProfile()) || (getSelectedProfile() != null && getSelectedProfile().isBundled())) {
            renderWidget(ctx, mx, my, delta, factoryResetBtn);
        } else {
            renderWidget(ctx, mx, my, delta, renameBtn);
        }
        renderWidget(ctx, mx, my, delta, saveToServerBtn);
        
        // Dirty indicator
        if (state.isDirty() && layout.detailsArea() != null) {
            Bounds d = layout.detailsArea();
            ctx.drawText(tr, "● Unsaved changes", d.x(), d.bottom() - 16, GuiConstants.WARNING, false);
        }
        
        // Modal overlay
        if (activeDialog != null && activeDialog.isVisible()) {
            activeDialog.render(ctx, mx, my, delta);
            for (var w : activeDialog.getWidgets()) w.render(ctx, mx, my, delta);
        }
    }
    
    private void renderPanelBg(DrawContext ctx, Bounds b, String title) {
        ctx.fill(b.x(), b.y(), b.right(), b.bottom(), GuiConstants.BG_PANEL);
        ctx.drawBorder(b.x(), b.y(), b.width(), b.height(), GuiConstants.BORDER);
        if (title != null) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, title, b.x() + GuiConstants.PADDING, b.y() + 4, GuiConstants.TEXT_PRIMARY, false);
        }
    }
    
    private void renderList(DrawContext ctx, TextRenderer tr, int mx, int my) {
        Bounds l = layout.listArea();
        ctx.fill(l.x(), l.y(), l.right(), l.bottom(), 0xFF1A1A1A);
        ctx.drawBorder(l.x(), l.y(), l.width(), l.height(), GuiConstants.BORDER);
        ctx.drawText(tr, "Profiles (" + filteredProfiles.size() + ")", l.x() + 4, l.y() + 4, GuiConstants.TEXT_SECONDARY, false);
        
        ctx.enableScissor(l.x(), l.y() + LIST_HEADER_HEIGHT, l.right(), l.bottom());
        int y = l.y() + LIST_HEADER_HEIGHT - listScrollOffset;
        for (int i = 0; i < filteredProfiles.size(); i++) {
            if (y + ITEM_HEIGHT > l.y() + LIST_HEADER_HEIGHT && y < l.bottom()) {
                boolean sel = i == selectedProfileIndex;
                boolean hov = mx >= l.x() && mx < l.right() && my >= y && my < y + ITEM_HEIGHT;
                if (sel) ctx.fill(l.x() + 2, y, l.right() - 2, y + ITEM_HEIGHT - 2, GuiConstants.ACCENT);
                else if (hov) ctx.fill(l.x() + 2, y, l.right() - 2, y + ITEM_HEIGHT - 2, 0x40FFFFFF);
                
                ProfileEntry e = filteredProfiles.get(i);
                String icon = e.isServer() ? "🔒" : (e.isBundled() ? "📦" : "✎");
                ctx.drawText(tr, icon, l.x() + 6, y + 5, GuiConstants.TEXT_SECONDARY, false);
                ctx.drawText(tr, e.name(), l.x() + 22, y + 5, sel ? 0xFFFFFFFF : GuiConstants.TEXT_PRIMARY, false);
                String src = e.isServer() ? "server" : (e.isBundled() ? "bundled" : "local");
                ctx.drawText(tr, src, l.right() - tr.getWidth(src) - 8, y + 5, GuiConstants.TEXT_SECONDARY, false);
            }
            y += ITEM_HEIGHT;
        }
        ctx.disableScissor();
    }
    
    private void renderDetails(DrawContext ctx, TextRenderer tr, Bounds d) {
        if (d == null) return;
        int y = d.y() + 16;
        
        // Summary box
        ctx.fill(d.x(), y, d.right(), y + 70, GuiConstants.BG_SECONDARY);
        ctx.drawBorder(d.x(), y, d.width(), 70, GuiConstants.BORDER);
        ctx.drawText(tr, "Current Configuration", d.x() + 4, y + 4, GuiConstants.TEXT_PRIMARY, false);
        int ly = y + 18;
        ly = line(ctx, tr, d.x() + 6, ly, "Shape", state.getCurrentShapeFragmentName());
        ly = line(ctx, tr, d.x() + 6, ly, "Fill", state.getCurrentFillFragmentName());
        line(ctx, tr, d.x() + 6, ly, "Animation", state.getCurrentAnimationFragmentName());
        
        y += 78;
        
        // Stats box
        ctx.fill(d.x(), y, d.right(), y + 50, GuiConstants.BG_SECONDARY);
        ctx.drawBorder(d.x(), y, d.width(), 50, GuiConstants.BORDER);
        ctx.drawText(tr, "Stats", d.x() + 4, y + 4, GuiConstants.TEXT_PRIMARY, false);
        ctx.drawText(tr, "latSteps: " + state.getInt("sphere.latSteps"), d.x() + 6, y + 18, GuiConstants.TEXT_SECONDARY, false);
        ctx.drawText(tr, "lonSteps: " + state.getInt("sphere.lonSteps"), d.x() + 90, y + 18, GuiConstants.TEXT_SECONDARY, false);
        ctx.drawText(tr, "radius: " + String.format("%.1f", state.getFloat("sphere.radius")), d.x() + 6, y + 30, GuiConstants.TEXT_SECONDARY, false);
        ctx.drawText(tr, "alpha: " + String.format("%.2f", state.getFloat("appearance.alpha")), d.x() + 90, y + 30, GuiConstants.TEXT_SECONDARY, false);
    }
    
    private int line(DrawContext ctx, TextRenderer tr, int x, int y, String label, String val) {
        ctx.drawText(tr, label + ":", x, y, GuiConstants.TEXT_SECONDARY, false);
        ctx.drawText(tr, val != null ? val : "—", x + 70, y, GuiConstants.TEXT_PRIMARY, false);
        return y + 14;
    }
    
    private void renderWidget(DrawContext ctx, int mx, int my, float d, ClickableWidget w) {
        if (w != null && w.visible) w.render(ctx, mx, my, d);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INPUT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean mouseClicked(double mx, double my, int btn) {
        if (activeDialog != null && activeDialog.isVisible()) {
            for (var w : activeDialog.getWidgets()) if (w.mouseClicked(mx, my, btn)) return true;
            if (!activeDialog.containsClick(mx, my)) { activeDialog.hide(); activeDialog = null; }
            return true;
        }
        if (btn != 0) return false;
        
        Bounds l = layout.listArea();
        if (mx >= l.x() && mx < l.right() && my >= l.y() + LIST_HEADER_HEIGHT && my < l.bottom()) {
            int idx = ((int) my - l.y() - LIST_HEADER_HEIGHT + listScrollOffset) / ITEM_HEIGHT;
            if (idx >= 0 && idx < filteredProfiles.size()) {
                selectedProfileIndex = idx;
                if (nameField != null) nameField.setText(getSelectedProfileName());
                updateButtonStates();
                return true;
            }
        }
        return false;
    }
    
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        Bounds l = layout.listArea();
        int contentH = filteredProfiles.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, contentH - (l.height() - LIST_HEADER_HEIGHT));
        listScrollOffset = Math.max(0, Math.min(maxScroll, listScrollOffset - (int)(v * ITEM_HEIGHT)));
        return true;
    }
    
    public boolean keyPressed(int key, int scan, int mods) {
        if (activeDialog != null && activeDialog.isVisible()) {
            if (activeDialog.keyPressed(key)) { activeDialog = null; return true; }
            for (var w : activeDialog.getWidgets()) {
                if (w instanceof TextFieldWidget tf && tf.keyPressed(key, scan, mods)) return true;
            }
            return true;
        }
        return false;
    }
    
    public boolean charTyped(char c, int mods) {
        if (activeDialog != null && activeDialog.isVisible()) {
            for (var w : activeDialog.getWidgets()) {
                if (w instanceof TextFieldWidget tf && tf.charTyped(c, mods)) return true;
            }
            return true;
        }
        return false;
    }
    
    public boolean isDialogVisible() { return activeDialog != null && activeDialog.isVisible(); }
    
    public List<ClickableWidget> getWidgets() {
        List<ClickableWidget> list = new ArrayList<>();
        if (sourceFilter != null) list.add(sourceFilter);
        if (categoryFilter != null) list.add(categoryFilter);
        if (searchField != null) list.add(searchField);
        if (nameField != null) list.add(nameField);
        if (loadBtn != null) list.add(loadBtn);
        if (saveBtn != null) list.add(saveBtn);
        if (saveAsBtn != null) list.add(saveAsBtn);
        if (deleteBtn != null) list.add(deleteBtn);
        if (duplicateBtn != null) list.add(duplicateBtn);
        if (actionService.isDefaultProfile(getSelectedProfile()) || (getSelectedProfile() != null && getSelectedProfile().isBundled())) {
            if (factoryResetBtn != null) list.add(factoryResetBtn);
        } else {
            if (renameBtn != null) list.add(renameBtn);
        }
        if (saveToServerBtn != null) list.add(saveToServerBtn);
        return list;
    }
    
    public List<String> getProfileNames() {
        return allProfiles.stream().map(ProfileEntry::name).collect(Collectors.toList());
    }
    
    @Override
    public void applyBoundsOffset() {
        // Widgets already at absolute positions via layout
    }
}
