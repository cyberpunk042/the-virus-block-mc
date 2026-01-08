package net.cyberpunk042.client.gui.component;

import net.cyberpunk042.client.gui.panel.sub.*;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.widget.PanelWrapper;
import net.cyberpunk042.client.gui.widget.SubTabPane;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;

/**
 * Factory for creating sub-tab content providers.
 * 
 * <p>Centralizes the creation of panel wrappers for all sub-tabs,
 * reducing code duplication in FieldCustomizerScreen.</p>
 */
public class ContentProviderFactory {
    
    private final Screen parent;
    private final FieldEditState state;
    private final TextRenderer textRenderer;
    private final Runnable onWidgetsChanged;
    
    /**
     * Creates a content provider factory.
     * 
     * @param parent The parent screen
     * @param state The field edit state
     * @param textRenderer Font renderer
     * @param onWidgetsChanged Called when a panel's widgets change (for re-registration)
     */
    public ContentProviderFactory(
            Screen parent,
            FieldEditState state,
            TextRenderer textRenderer,
            Runnable onWidgetsChanged) {
        this.parent = parent;
        this.state = state;
        this.textRenderer = textRenderer;
        this.onWidgetsChanged = onWidgetsChanged;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // QUICK TAB CONTENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates the Fill sub-tab content.
     */
    public SubTabPane.ContentProvider fill() {
        FillSubPanel panel = new FillSubPanel(parent, state, 0);
        panel.setWidgetChangedCallback(onWidgetsChanged);
        return new PanelWrapper(panel);
    }
    
    /**
     * Creates the Appearance sub-tab content.
     */
    public SubTabPane.ContentProvider appearance() {
        return new PanelWrapper(new AppearanceSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Visibility sub-tab content.
     */
    public SubTabPane.ContentProvider visibility() {
        VisibilitySubPanel panel = new VisibilitySubPanel(parent, state, 0);
        panel.setWidgetChangedCallback(onWidgetsChanged);
        return new PanelWrapper(panel);
    }
    
    /**
     * Creates the Transform sub-tab content (unified with orbit).
     */
    public SubTabPane.ContentProvider transform() {
        TransformSubPanel panel = new TransformSubPanel(parent, state, 0);
        panel.setWidgetChangedCallback(onWidgetsChanged);
        return new PanelWrapper(panel);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ADVANCED TAB CONTENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates the Prediction sub-tab content.
     */
    public SubTabPane.ContentProvider prediction() {
        return new PanelWrapper(new PredictionSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Linking sub-tab content.
     */
    public SubTabPane.ContentProvider linking() {
        return new PanelWrapper(new LinkingSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Modifiers sub-tab content.
     */
    public SubTabPane.ContentProvider modifiers() {
        return new PanelWrapper(new ModifiersSubPanel(parent, state));
    }
    
    
    /**
     * Creates the Arrange sub-tab content.
     */
    public SubTabPane.ContentProvider arrange() {
        ArrangeSubPanel panel = new ArrangeSubPanel(parent, state, textRenderer);
        panel.setWidgetChangedCallback(onWidgetsChanged);
        return new PanelWrapper(panel);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG TAB CONTENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates the Beam sub-tab content.
     */
    public SubTabPane.ContentProvider beam() {
        return new PanelWrapper(new BeamSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Trigger sub-tab content.
     */
    public SubTabPane.ContentProvider trigger() {
        return new PanelWrapper(new TriggerSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Force sub-tab content.
     */
    public SubTabPane.ContentProvider force() {
        return new PanelWrapper(new ForceSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Lifecycle sub-tab content.
     */
    public SubTabPane.ContentProvider lifecycle() {
        return new PanelWrapper(new LifecycleSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Bindings sub-tab content.
     */
    public SubTabPane.ContentProvider bindings() {
        return new PanelWrapper(new BindingsSubPanel(parent, state, 0));
    }
    
    /**
     * Creates the Trace sub-tab content.
     */
    public SubTabPane.ContentProvider trace() {
        return new PanelWrapper(new TraceSubPanel(parent, state));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FX TAB CONTENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates the Shockwave sub-tab content.
     */
    public SubTabPane.ContentProvider shockwave() {
        ShockwaveSubPanel panel = new ShockwaveSubPanel(parent, state, 0);
        panel.setWidgetChangedCallback(onWidgetsChanged);
        return new PanelWrapper(panel);
    }
    
    /**
     * Creates the Field Visual (Energy Orb) sub-tab content.
     */
    public SubTabPane.ContentProvider fieldVisual() {
        FieldVisualSubPanel panel = new FieldVisualSubPanel(parent, state, 0);
        panel.setWidgetChangedCallback(onWidgetsChanged);
        return new PanelWrapper(panel);
    }
    
    /**
     * Creates the Magic Circle sub-tab content.
     */
    public SubTabPane.ContentProvider magicCircle() {
        MagicCircleSubPanel panel = new MagicCircleSubPanel(parent, state, 0);
        panel.setWidgetChangedCallback(onWidgetsChanged);
        return new PanelWrapper(panel);
    }
}
