package net.cyberpunk042.client.gui.builder;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.state.ChangeType;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.StateChangeListener;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for panels using the bidirectional binding system.
 * 
 * <p>Extends {@link AbstractPanel} with automatic state synchronization:
 * <ul>
 *   <li>Registers as a {@link StateChangeListener} when attached</li>
 *   <li>Automatically syncs all bindings on profile load, primitive switch, etc.</li>
 *   <li>Provides binding registration for {@link Bound} and {@link Vec3Binding}</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>
 * public class MyPanel extends BoundPanel {
 *     &#64;Override
 *     protected void buildContent(ContentBuilder b) {
 *         b.slider("Radius", "sphere.radius", 0.1f, 10f)
 *          .toggle("Spin", "spin.active");
 *     }
 * }
 * </pre>
 * 
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@code init()} - Creates widgets via {@code buildContent()}, registers bindings</li>
 *   <li>{@code onStateChanged()} - Called by {@link FieldEditState} on external changes</li>
 *   <li>{@code dispose()} - Unregisters from state listeners</li>
 * </ol>
 * 
 * @see Bound
 * @see Vec3Binding
 * @see ContentBuilder
 */
public abstract class BoundPanel extends AbstractPanel implements StateChangeListener {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    protected final List<Bound<?, ?, ?>> bindings = new ArrayList<>();
    protected final List<Vec3Binding> vec3Bindings = new ArrayList<>();
    
    // The current ContentBuilder (for rendering labels)
    protected ContentBuilder contentBuilder;
    
    private boolean attached = false;
    
    public BoundPanel(Screen parent, FieldEditState state) {
        super(parent, state);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        // Clear previous state
        bindings.clear();
        vec3Bindings.clear();
        widgets.clear();
        contentBuilder = null;
        scrollOffset = 0;  // Reset scroll when rebuilding
        
        // Build content using fluent API (subclass implements this)
        buildContent();
        
        // Register as listener if not already
        if (!attached) {
            state.addStateListener(this);
            attached = true;
            Logging.GUI.topic("panel").debug("{} attached as state listener", getClass().getSimpleName());
        }
    }
    
    /**
     * Override reflow to use proper initialization with bounds offsetting.
     * Saves and restores scroll position properly.
     */
    @Override
    protected void reflow() {
        if (bounds.isEmpty()) return;
        
        // Save scroll position before rebuilding
        int savedScrollOffset = scrollOffset;
        
        // Reset scroll to 0 for rebuild (widgets are built at scroll=0 positions)
        scrollOffset = 0;
        
        // Use bounds dimensions if panel dimensions not set
        int width = panelWidth > 0 ? panelWidth : bounds.width();
        int height = panelHeight > 0 ? panelHeight : bounds.height();
        
        // Rebuild content (builds widgets at scroll=0 positions)
        init(width, height);
        
        // Apply bounds offset so widgets and labels are positioned correctly
        applyBoundsOffset();
        
        // Restore scroll position using setScrollOffset which moves widgets to visual positions
        int maxScroll = Math.max(0, contentHeight - bounds.height());
        setScrollOffset(Math.min(savedScrollOffset, maxScroll));
    }
    
    /**
     * Build the panel content using bindings.
     * 
     * <p>Subclasses override this to create their UI. Use {@link #content(int)}
     * to create a {@link ContentBuilder} for fluent widget creation:</p>
     * <pre>
     * &#64;Override
     * protected void buildContent() {
     *     ContentBuilder b = content(startY);
     *     b.slider("Radius", "sphere.radius").range(0.1f, 10f).add();
     *     contentHeight = b.getContentHeight();
     * }
     * </pre>
     */
    protected abstract void buildContent();
    
    /**
     * Creates a ContentBuilder for fluent widget creation.
     * Also stores reference for label rendering.
     * 
     * @param startY Starting Y position for content
     * @return A new ContentBuilder
     */
    protected ContentBuilder content(int startY) {
        contentBuilder = new ContentBuilder(state, bindings, widgets, startY, panelWidth);
        return contentBuilder;
    }
    
    /**
     * Returns the content builder for this panel.
     * Used by PanelWrapper to offset labels alongside widgets.
     */
    public ContentBuilder getContentBuilder() {
        return contentBuilder;
    }
    
    /**
     * Called when disposing the panel. Unregisters state listeners.
     */
    public void dispose() {
        if (attached) {
            state.removeStateListener(this);
            attached = false;
            Logging.GUI.topic("panel").debug("{} detached from state listeners", getClass().getSimpleName());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDING REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Registers a binding and adds its widget to the panel.
     * 
     * @param binding The binding to register
     * @return The binding (for chaining)
     */
    protected <W extends net.minecraft.client.gui.widget.ClickableWidget, S, D> 
            Bound<W, S, D> registerBinding(Bound<W, S, D> binding) {
        bindings.add(binding);
        widgets.add(binding.widget());
        return binding;
    }
    
    /**
     * Registers a Vec3 binding and adds all three sliders to the panel.
     * 
     * @param binding The Vec3 binding to register
     * @return The binding (for chaining)
     */
    protected Vec3Binding registerVec3Binding(Vec3Binding binding) {
        vec3Bindings.add(binding);
        for (var slider : binding.widgets()) {
            if (slider != null) widgets.add(slider);
        }
        return binding;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE CHANGE LISTENER
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void onStateChanged(ChangeType changeType) {
        switch (changeType) {
            case PROFILE_LOADED, PRIMITIVE_SWITCHED, LAYER_SWITCHED, FULL_RESET -> {
                // Major change: sync all bindings
                syncAllFromState();
            }
            case FRAGMENT_APPLIED -> {
                // Might need rebuild if mode changed, otherwise just sync
                if (needsRebuildOnFragmentApply()) {
                    rebuildContent();
                } else {
                    syncAllFromState();
                }
            }
            case PROPERTY_CHANGED -> {
                // Single property: sync all (could be more targeted in future)
                syncAllFromState();
            }
        }
    }
    
    /**
     * Syncs all registered bindings from state.
     * Call this when state changes externally.
     */
    protected void syncAllFromState() {
        for (Bound<?, ?, ?> binding : bindings) {
            binding.syncFromState();
        }
        for (Vec3Binding v3 : vec3Bindings) {
            v3.syncFromState();
        }
        
        Logging.GUI.topic("panel").trace("{} synced {} bindings + {} vec3 bindings", 
            getClass().getSimpleName(), bindings.size(), vec3Bindings.size());
    }
    
    /**
     * Rebuilds the panel content. Call when structural changes require new widgets.
     * Saves and restores scroll position properly.
     */
    protected void rebuildContent() {
        // Save scroll position before rebuilding
        int savedScrollOffset = scrollOffset;
        
        // Reset scroll to 0 for rebuild (widgets are built at scroll=0 positions)
        scrollOffset = 0;
        
        init(panelWidth, panelHeight);
        
        // Apply bounds offset so widgets are positioned correctly relative to panel
        if (bounds != null && !bounds.isEmpty()) {
            applyBoundsOffset();
            
            // Restore scroll position using setScrollOffset which moves widgets to visual positions
            int maxScroll = Math.max(0, contentHeight - bounds.height());
            setScrollOffset(Math.min(savedScrollOffset, maxScroll));
        }
        
        notifyWidgetsChanged();
    }
    
    /**
     * Labels are now TextWidget instances in the widget list, so
     * they get offset automatically by super.applyBoundsOffset().
     */
    @Override
    public void applyBoundsOffset() {
        super.applyBoundsOffset();
        // Labels are now widgets - no separate handling needed
    }
    
    /**
     * Override in panels that need rebuild when a fragment is applied.
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>FillSubPanel: Fill mode change requires different widgets</li>
     *   <li>TransformSubPanel: Motion mode change requires different sliders</li>
     * </ul>
     * 
     * @return true if fragment application should trigger widget rebuild
     */
    protected boolean needsRebuildOnFragmentApply() {
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCROLLING SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Renders widgets with scroll offset applied.
     * Use this in render() for panels with scrollable content.
     * 
     * @param context The draw context
     * @param mouseX Mouse X
     * @param mouseY Mouse Y
     * @param delta Frame delta
     */
    /**
     * Applies scroll offset to widget positions for rendering.
     * Call this BEFORE super.render() and call restoreScrollPositions() AFTER.
     * 
     * <p>This method does NOT render widgets - that's done by Screen.render().
     * It only repositions widgets so they appear scrolled when rendered.</p>
     */
    protected void applyScrollPositions() {
        if (scrollOffset != 0) {
            for (var widget : widgets) {
                widget.setY(widget.getY() - scrollOffset);
            }
        }
    }
    
    /**
     * Restores widget positions after scrolled rendering.
     * Call this AFTER super.render() to undo the scroll offset.
     */
    protected void restoreScrollPositions() {
        if (scrollOffset != 0) {
            for (var widget : widgets) {
                widget.setY(widget.getY() + scrollOffset);
            }
        }
    }
    
    /**
     * Renders widgets with scissor clipping.
     * Widgets are already at visual positions (via setScrollOffset).
     * Labels are now TextWidget instances in the widgets list - no separate rendering needed.
     */
    @Override
    protected void renderWithScroll(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // Calculate effective bounds for rendering
        // When bounds is empty, widgets are at their local coordinates (no offset needed)
        int boundsX = (bounds != null && !bounds.isEmpty()) ? bounds.x() : 0;
        int boundsY = (bounds != null && !bounds.isEmpty()) ? bounds.y() : 0;
        int boundsRight = (bounds != null && !bounds.isEmpty()) ? bounds.right() : panelWidth;
        int boundsBottom = (bounds != null && !bounds.isEmpty()) ? bounds.bottom() : panelHeight;
        
        // Clip to panel bounds (if we have them)
        if (bounds != null && !bounds.isEmpty()) {
            context.enableScissor(boundsX, boundsY, boundsRight, boundsBottom);
        }
        
        // Render all widgets (includes TextWidget labels now)
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
        
        if (bounds != null && !bounds.isEmpty()) {
            context.disableScissor();
        }
        
        // Render scroll indicator if scrollable
        if (isScrollable() && bounds != null && !bounds.isEmpty()) {
            renderScrollIndicator(context);
        }
    }
    
    /**
     * Renders a simple scroll indicator bar.
     */
    protected void renderScrollIndicator(net.minecraft.client.gui.DrawContext context) {
        int barWidth = 3;
        int barX = bounds.right() - barWidth - 1;
        int trackHeight = bounds.height() - 4;
        int trackY = bounds.y() + 2;
        
        // Track
        context.fill(barX, trackY, barX + barWidth, trackY + trackHeight, 0x40FFFFFF);
        
        // Thumb
        int maxScroll = Math.max(1, contentHeight - bounds.height());
        float scrollPercent = (float) scrollOffset / maxScroll;
        int thumbHeight = Math.max(10, trackHeight * bounds.height() / contentHeight);
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * scrollPercent);
        
        context.fill(barX, thumbY, barX + barWidth, thumbY + thumbHeight, 0xFFAAAAAA);
    }
}

