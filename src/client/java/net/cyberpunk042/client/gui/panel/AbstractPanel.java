package net.cyberpunk042.client.gui.panel;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.RendererCapabilities;
import net.cyberpunk042.client.gui.state.RequiresFeature;
import net.cyberpunk042.client.gui.widget.ExpandableSection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * G06: Base class for GUI panels.
 * 
 * <p>All panels render relative to their assigned bounds.
 * This enables the same panel code to work in both windowed and fullscreen modes.</p>
 * 
 * <h3>Template Method Pattern</h3>
 * <pre>
 * panel.setBounds(bounds);  // Set where to render
 * panel.init();             // Creates widgets at correct positions
 * panel.render(...);        // Renders within bounds
 * </pre>
 */
public abstract class AbstractPanel {
    
    protected final Screen parent;
    protected final FieldEditState state;
    
    /**
     * The bounds this panel should render within.
     * All widget positions should be relative to bounds.x, bounds.y
     */
    protected Bounds bounds = Bounds.EMPTY;
    
    /**
     * @deprecated Use bounds.width() instead. Kept for backward compatibility.
     */
    @Deprecated
    protected int panelWidth;
    
    /**
     * @deprecated Use bounds.height() instead. Kept for backward compatibility.
     */
    @Deprecated
    protected int panelHeight;
    
    /**
     * List of widgets owned by this panel.
     */
    protected final List<ClickableWidget> widgets = new ArrayList<>();
    
    /**
     * List of expandable sections owned by this panel.
     * These need to be offset along with widgets.
     */
    protected final List<ExpandableSection> sections = new ArrayList<>();
    
    /**
     * Scroll offset for panels with scrollable content.
     */
    protected int scrollOffset = 0;
    
    /**
     * Total content height (for scroll calculation).
     */
    protected int contentHeight = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FRAGMENT/PRESET HANDLING (shared by Fill, Visibility, Animation, Shape)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Callback when widgets are rebuilt (for screen widget re-registration). */
    protected Runnable widgetChangedCallback;
    
    /** Current fragment/preset name. */
    protected String currentFragment = "Custom";
    
    /** True while applying a fragment to prevent feedback loops. */
    protected boolean applyingFragment = false;
    
    /** Starting Y position for content. */
    protected int startY = 0;
    
    protected AbstractPanel(Screen parent, FieldEditState state) {
        this.parent = parent;
        this.state = state;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERER CAPABILITY CHECKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Checks if this panel's required features are supported by the current renderer.
     * 
     * <p>Uses the {@link RequiresFeature} annotation on the class to determine
     * which features are needed.</p>
     * 
     * @return true if all required features are supported
     */
    public boolean isFeatureSupported() {
        RequiresFeature annotation = getClass().getAnnotation(RequiresFeature.class);
        if (annotation == null) {
            return true; // No requirements = always supported
        }
        
        RendererCapabilities.Feature[] required = annotation.value();
        if (required.length == 0) {
            return true;
        }
        
        if (annotation.requireAll()) {
            return RendererCapabilities.areAllSupported(required);
        } else {
            return RendererCapabilities.isAnySupported(required);
        }
    }
    
    /**
     * Gets the tooltip to show when this panel is disabled due to renderer mode.
     * 
     * @return Tooltip text, or null if panel is enabled
     */
    public String getDisabledTooltip() {
        if (isFeatureSupported()) {
            return null;
        }
        
        RequiresFeature annotation = getClass().getAnnotation(RequiresFeature.class);
        if (annotation == null || annotation.value().length == 0) {
            return null;
        }
        
        // Return tooltip for first unsupported feature
        for (RendererCapabilities.Feature f : annotation.value()) {
            String tooltip = RendererCapabilities.getDisabledTooltip(f);
            if (tooltip != null) {
                return tooltip;
            }
        }
        return "§cRequires Accurate mode";
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BOUNDS MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the bounds for this panel and triggers reflow.
     */
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        reflow();
    }
    
    /**
     * Sets bounds WITHOUT triggering reflow.
     * Use this when widgets are already initialized and positioned.
     */
    public void setBoundsQuiet(Bounds bounds) {
        this.bounds = bounds;
    }
    
    /**
     * @return Current bounds of this panel.
     */
    public Bounds getBounds() {
        return bounds;
    }
    
    /**
     * @return Available width for content (bounds width).
     */
    protected int getWidth() {
        return bounds.width();
    }
    
    /**
     * @return Available height for content (bounds height).
     */
    protected int getHeight() {
        return bounds.height();
    }
    
    /**
     * Converts a local X coordinate to screen coordinate.
     */
    protected int toScreenX(int localX) {
        return bounds.x() + localX;
    }
    
    /**
     * Converts a local Y coordinate to screen coordinate.
     */
    protected int toScreenY(int localY) {
        return bounds.y() + localY - scrollOffset;
    }
    
    /**
     * Converts screen coordinates to local coordinates.
     */
    protected int toLocalX(int screenX) {
        return screenX - bounds.x();
    }
    
    /**
     * Converts screen coordinates to local coordinates.
     */
    protected int toLocalY(int screenY) {
        return screenY - bounds.y() + scrollOffset;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * @return Unmodifiable list of widgets owned by this panel.
     */
    public List<ClickableWidget> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }
    
    /**
     * Offsets all widgets and sections by the given delta.
     * This is used to move widgets created at (0,0) to their actual bounds position.
     * Call this AFTER init() if widgets were created at local coordinates.
     */
    public void offsetWidgets(int dx, int dy) {
        for (ClickableWidget widget : widgets) {
            widget.setX(widget.getX() + dx);
            widget.setY(widget.getY() + dy);
        }
        for (ExpandableSection section : sections) {
            section.offsetInternalPosition(dx, dy);
        }
    }
    
    /**
     * Moves all widgets and sections to be relative to the current bounds.
     * Assumes widgets were created starting at (0,0).
     */
    public void applyBoundsOffset() {
        offsetWidgets(bounds.x(), bounds.y());
    }
    
    /**
     * Registers an ExpandableSection to be offset along with widgets.
     */
    protected void registerSection(ExpandableSection section) {
        if (section != null && !sections.contains(section)) {
            sections.add(section);
        }
    }
    
    /**
     * Adds a widget to this panel.
     */
    protected <T extends ClickableWidget> T addWidget(T widget) {
        widgets.add(widget);
        return widget;
    }
    
    /**
     * Clears all widgets from this panel.
     */
    protected void clearWidgets() {
        widgets.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FRAGMENT/PRESET METHODS (shared by Fill, Visibility, Animation, Shape)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Sets callback for when widgets are rebuilt (for screen widget re-registration). */
    public void setWidgetChangedCallback(Runnable callback) {
        this.widgetChangedCallback = callback;
    }
    
    /** Notifies that widgets have changed, triggering re-registration if callback is set. */
    protected void notifyWidgetsChanged() {
        if (widgetChangedCallback != null) {
            widgetChangedCallback.run();
        }
    }
    
    /**
     * Wraps a user action, marking fragment as "Custom" if not applying a preset.
     * Subclasses should call this in widget callbacks to track custom changes.
     * 
     * @param action The action to run
     * @param fragmentDropdown The fragment dropdown to update (may be null)
     */
    protected void onUserChange(Runnable action, net.minecraft.client.gui.widget.CyclingButtonWidget<String> fragmentDropdown) {
        action.run();
        if (!applyingFragment) {
            currentFragment = "Custom";
            if (fragmentDropdown != null) {
                fragmentDropdown.setValue("Custom");
            }
        }
    }
    
    /** @return Current content height for this panel (may be larger than visible height). */
    public int getContentHeight() {
        return contentHeight;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET POSITIONING HELPERS (for annotation-based visibility)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Positions and adds a widget, returning the next Y position.
     * 
     * @param widget The widget to add (may be null)
     * @param x X position
     * @param y Y position
     * @param w Width
     * @return Next Y position (after widget height + padding)
     */
    protected int positionAndAddWidget(ClickableWidget widget, int x, int y, int w) {
        if (widget == null) return y;
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(w);
        widgets.add(widget);
        return y + net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT 
             + net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
    }
    
    /**
     * Positions and adds two widgets side-by-side, returning the next Y position.
     * 
     * @param left Left widget (may be null)
     * @param right Right widget (may be null)
     * @param x Starting X position
     * @param y Y position
     * @param halfW Half width (each widget gets this width)
     * @return Next Y position (after widget height + padding)
     */
    protected int positionAndAddWidgetPair(ClickableWidget left, ClickableWidget right, 
                                            int x, int y, int halfW) {
        int padding = net.cyberpunk042.client.gui.util.GuiConstants.PADDING;
        if (left != null) {
            left.setX(x);
            left.setY(y);
            left.setWidth(halfW);
            widgets.add(left);
        }
        if (right != null) {
            right.setX(x + halfW + padding);
            right.setY(y);
            right.setWidth(halfW);
            widgets.add(right);
        }
        return y + net.cyberpunk042.client.gui.util.GuiConstants.WIDGET_HEIGHT + padding;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCROLL MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the scroll offset for this panel.
     * 
     * <p>When scroll offset changes, all widget Y positions are permanently updated
     * to match their visual positions. This ensures Screen's input handling works
     * correctly - widgets are always at their rendered positions, so isMouseOver(),
     * mouseClicked(), and mouseDragged() all work without coordinate adjustments.</p>
     */
    public void setScrollOffset(int offset) {
        if (bounds == null || bounds.isEmpty()) return;
        
        int maxScroll = Math.max(0, contentHeight - bounds.height());
        int newOffset = Math.max(0, Math.min(offset, maxScroll));
        
        int delta = newOffset - this.scrollOffset;
        if (delta == 0) return;
        
        this.scrollOffset = newOffset;
        
        // Permanently move widgets to visual positions
        for (var widget : widgets) {
            widget.setY(widget.getY() - delta);
        }
    }
    
    /**
     * @return Current scroll offset.
     */
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * @return Maximum scroll offset.
     */
    public int getMaxScroll() {
        return Math.max(0, contentHeight - bounds.height());
    }
    
    /**
     * @return Whether this panel has scrollable content.
     */
    public boolean isScrollable() {
        return contentHeight > bounds.height();
    }
    
    /**
     * @return The parent screen.
     */
    public Screen getParent() {
        return parent;
    }
    
    /**
     * Handles mouse scroll input.
     * @return true if scroll was consumed.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        if (!isScrollable()) return false;
        
        int scrollDelta = (int)(verticalAmount * 10);
        setScrollOffset(scrollOffset - scrollDelta);
        return true;
    }
    
    /**
     * Mouse clicks are handled by Screen since widgets are at visual positions.
     * @return false - let Screen handle input
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
    
    /**
     * Renders widgets with scissor clipping.
     * Widgets are already at visual positions (via setScrollOffset).
     */
    protected void renderWithScroll(DrawContext context, int mouseX, int mouseY, float delta) {
        if (bounds == null || bounds.isEmpty()) return;
        
        // Clip to panel bounds
        context.enableScissor(bounds.x(), bounds.y(), bounds.right(), bounds.bottom());
        
        // Render all widgets (already at visual positions)
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
        
        context.disableScissor();
        
        // Render scroll indicator if scrollable
        if (isScrollable()) {
            renderScrollIndicator(context);
        }
    }
    
    /**
     * @deprecated Use renderWithScroll() instead.
     */
    @Deprecated
    protected void applyScrollPositions() {
        // No-op: kept for backward compatibility
    }
    
    /**
     * @deprecated Use renderWithScroll() instead.
     */
    @Deprecated
    protected void restoreScrollPositions() {
        // No-op: kept for backward compatibility
    }
    
    /**
     * Renders a scroll indicator bar on the right edge.
     */
    protected void renderScrollIndicator(DrawContext context) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;
        
        // Scroll track
        int trackX = bounds.right() - 4;
        int trackY = bounds.y() + 2;
        int trackH = bounds.height() - 4;
        
        // Scroll thumb
        float scrollRatio = (float) scrollOffset / maxScroll;
        int thumbH = Math.max(10, trackH * bounds.height() / contentHeight);
        int thumbY = trackY + (int)((trackH - thumbH) * scrollRatio);
        
        context.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x40FFFFFF);
        context.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xA0FFFFFF);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE (Template Method Pattern)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Initializes the panel with given dimensions.
     * @deprecated Use {@link #setBounds(Bounds)} and {@link #init()} instead.
     */
    @Deprecated
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        setBounds(new Bounds(0, 0, width, height));
    }
    
    /**
     * Initializes the panel within current bounds.
     * Override this to create widgets.
     */
    protected void init() {
        // Default: do nothing, subclasses override
    }
    
    /**
     * Called when bounds change to reposition widgets.
     * Default implementation clears and re-inits.
     */
    protected void reflow() {
        clearWidgets();
        scrollOffset = 0;
        if (!bounds.isEmpty()) {
            init();
        }
    }
    
    /**
     * Called each tick for updates.
     */
    public abstract void tick();
    
    /**
     * Renders the panel within its bounds.
     * 
     * @param context Draw context
     * @param mouseX Mouse X in screen coordinates
     * @param mouseY Mouse Y in screen coordinates
     * @param delta Frame delta
     */
    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Checks if a point (in screen coordinates) is within this panel's bounds.
     */
    public boolean containsPoint(double screenX, double screenY) {
        return bounds.contains(screenX, screenY);
    }
    
    /**
     * Draws a debug outline around this panel's bounds.
     */
    protected void drawDebugBounds(DrawContext context, int color) {
        context.drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), color);
    }
}
