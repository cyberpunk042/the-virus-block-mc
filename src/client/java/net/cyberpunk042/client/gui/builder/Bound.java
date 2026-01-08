package net.cyberpunk042.client.gui.builder;

import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Bidirectional binding wrapper for widgets.
 * 
 * <p>Handles the critical state↔widget synchronization problem:</p>
 * <ul>
 *   <li><b>Widget → State</b>: User changes widget, value transforms and updates state</li>
 *   <li><b>State → Widget</b>: External change (profile load, primitive switch), widget refreshes</li>
 * </ul>
 * 
 * <h3>Type Parameters</h3>
 * <ul>
 *   <li><b>W</b>: Widget type (e.g., LabeledSlider, CyclingButtonWidget)</li>
 *   <li><b>S</b>: State value type (what's stored, e.g., Float for normalized 0-1)</li>
 *   <li><b>D</b>: Display value type (what widget shows, e.g., Float for degrees 0-360)</li>
 * </ul>
 * 
 * <h3>Example - Phase Degrees</h3>
 * <pre>
 * // State stores 0.0-1.0, widget shows 0-360°
 * Bound.sliderDegrees(slider, 
 *     () -&gt; state.getFloat("link.phaseOffset"),
 *     v -&gt; state.set("link.phaseOffset", v));
 * </pre>
 * 
 * @param <W> Widget type
 * @param <S> State value type
 * @param <D> Display value type
 */
public final class Bound<W extends ClickableWidget, S, D> {
    
    private final W widget;
    private final Supplier<S> stateGetter;
    private final Consumer<S> stateSetter;
    private final Function<S, D> stateToDisplay;
    private final Function<D, S> displayToState;
    private final Consumer<D> widgetUpdater;
    
    private boolean syncing = false;  // Prevent feedback loops
    
    /**
     * Direct constructor for typed bindings.
     */
    public Bound(W widget,
                 Supplier<S> stateGetter,
                 Consumer<S> stateSetter,
                 Function<S, D> stateToDisplay,
                 Function<D, S> displayToState,
                 Consumer<D> widgetUpdater) {
        this.widget = widget;
        this.stateGetter = stateGetter;
        this.stateSetter = stateSetter;
        this.stateToDisplay = stateToDisplay;
        this.displayToState = displayToState;
        this.widgetUpdater = widgetUpdater;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Called when USER changes the widget.
     * Transforms display value → state value, updates state.
     * 
     * @param displayValue The value from the widget
     */
    public void onWidgetChanged(D displayValue) {
        if (syncing) return;
        syncing = true;
        try {
            S stateValue = displayToState.apply(displayValue);
            stateSetter.accept(stateValue);
        } finally {
            syncing = false;
        }
    }
    
    /**
     * Called when STATE changes externally (profile load, primitive switch).
     * Reads state value, transforms to display value, updates widget.
     */
    public void syncFromState() {
        if (syncing) return;
        syncing = true;
        try {
            S stateValue = stateGetter.get();
            if (stateValue != null) {
                D displayValue = stateToDisplay.apply(stateValue);
                widgetUpdater.accept(displayValue);
            }
        } finally {
            syncing = false;
        }
    }
    
    /**
     * @return The wrapped widget for adding to panel.
     */
    public W widget() {
        return widget;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONVENIENCE FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a direct binding (no transform) for a float slider.
     */
    public static Bound<LabeledSlider, Float, Float> slider(
            LabeledSlider slider,
            Supplier<Float> getter,
            Consumer<Float> setter) {
        return new Bound<>(
            slider,
            getter,
            setter,
            v -> v,           // identity: state→display
            v -> v,           // identity: display→state
            slider::setValue  // updater
        );
    }
    
    /**
     * Creates a degree↔normalized binding for a float slider.
     * State stores 0.0-1.0, widget shows 0-360°.
     */
    public static Bound<LabeledSlider, Float, Float> sliderDegrees(
            LabeledSlider slider,
            Supplier<Float> getter,
            Consumer<Float> setter) {
        return new Bound<>(
            slider,
            getter,
            setter,
            v -> v * 360f,    // state→display: 0.5 → 180°
            v -> v / 360f,    // display→state: 180° → 0.5
            slider::setValue
        );
    }
    
    /**
     * Creates a direct binding for a boolean toggle.
     */
    public static Bound<CyclingButtonWidget<Boolean>, Boolean, Boolean> toggle(
            CyclingButtonWidget<Boolean> toggle,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter) {
        return new Bound<>(
            toggle,
            getter,
            setter,
            v -> v,
            v -> v,
            toggle::setValue
        );
    }
    
    /**
     * Creates a direct binding for an enum dropdown.
     */
    public static <E extends Enum<E>> Bound<CyclingButtonWidget<E>, E, E> dropdown(
            CyclingButtonWidget<E> dropdown,
            Supplier<E> getter,
            Consumer<E> setter) {
        return new Bound<>(
            dropdown,
            getter,
            setter,
            v -> v,
            v -> v,
            dropdown::setValue
        );
    }
    
    /**
     * Creates an inverted binding for a boolean toggle.
     * Display shows opposite of state (e.g., "See Through" for !depthWrite).
     */
    public static Bound<CyclingButtonWidget<Boolean>, Boolean, Boolean> toggleInverted(
            CyclingButtonWidget<Boolean> toggle,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter) {
        return new Bound<>(
            toggle,
            getter,
            setter,
            v -> !v,  // state→display: depthWrite=true → seeThrough=false
            v -> !v,  // display→state: seeThrough=false → depthWrite=true
            toggle::setValue
        );
    }
    
    /**
     * Creates a slider binding with custom transform.
     * 
     * @param toDisplay Converts state value to display value
     * @param toState Converts display value to state value
     */
    public static Bound<LabeledSlider, Float, Float> sliderTransform(
            LabeledSlider slider,
            Supplier<Float> getter,
            Consumer<Float> setter,
            Function<Float, Float> toDisplay,
            Function<Float, Float> toState) {
        return new Bound<>(
            slider,
            getter,
            setter,
            toDisplay,
            toState,
            slider::setValue
        );
    }
    
    /**
     * Creates an integer slider binding.
     */
    public static Bound<LabeledSlider, Integer, Float> sliderInt(
            LabeledSlider slider,
            Supplier<Integer> getter,
            Consumer<Integer> setter) {
        return new Bound<>(
            slider,
            getter,
            setter,
            Integer::floatValue,  // int → float for display
            Float::intValue,      // float → int for state
            slider::setValue
        );
    }
}
