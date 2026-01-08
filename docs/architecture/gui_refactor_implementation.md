# GUI Refactor - Implementation Details

## Point 1: Refined `Bound<W>` Class with Transform Support

### Core Insight: We Need 3 Layers of Abstraction

Looking at the codebase, widgets fall into these categories:

| Pattern | Example | Frequency | Complexity |
|---------|---------|-----------|------------|
| **Direct** | `state.set("fill.wireThickness", v)` | ~60% | Low |
| **Transform** | `state.set("link.phaseOffset", v / 360f)` | ~20% | Medium |
| **Compound** | `updateOffset(v, null, null)` for Vector3f | ~15% | High |
| **Trigger Rebuild** | Mode changes → `rebuildWidgets()` | ~5% | Special |

### Design: `Bound<W, S, D>` - Full Type Safety

```java
package net.cyberpunk042.client.gui.builder;

import net.minecraft.client.gui.widget.ClickableWidget;
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
 * Bound.of(slider)
 *     .readFrom(() -> state.getFloat("link.phaseOffset"))
 *     .writeTo(v -> state.set("link.phaseOffset", v))
 *     .transform(
 *         stateVal -> stateVal * 360f,  // state→display: 0.5 → 180°
 *         displayVal -> displayVal / 360f  // display→state: 180° → 0.5
 *     )
 *     .build();
 * </pre>
 */
public final class Bound<W extends ClickableWidget, S, D> {
    
    private final W widget;
    private final Supplier<S> stateGetter;
    private final Consumer<S> stateSetter;
    private final Function<S, D> stateToDisplay;
    private final Function<D, S> displayToState;
    private final Consumer<D> widgetUpdater;  // How to push value into widget
    
    private boolean syncing = false;  // Prevent feedback loops
    
    private Bound(Builder<W, S, D> builder) {
        this.widget = builder.widget;
        this.stateGetter = builder.stateGetter;
        this.stateSetter = builder.stateSetter;
        this.stateToDisplay = builder.stateToDisplay;
        this.displayToState = builder.displayToState;
        this.widgetUpdater = builder.widgetUpdater;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Called when USER changes the widget.
     * Transforms display value → state value, updates state.
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
            D displayValue = stateToDisplay.apply(stateValue);
            widgetUpdater.accept(displayValue);
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
    // BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static <W extends ClickableWidget> Builder<W, Object, Object> of(W widget) {
        return new Builder<>(widget);
    }
    
    public static class Builder<W extends ClickableWidget, S, D> {
        private final W widget;
        private Supplier<S> stateGetter;
        private Consumer<S> stateSetter;
        private Function<S, D> stateToDisplay = s -> (D) s;  // Identity by default
        private Function<D, S> displayToState = d -> (S) d;  // Identity by default
        private Consumer<D> widgetUpdater;
        
        private Builder(W widget) {
            this.widget = widget;
        }
        
        /**
         * Sets the state getter for reading current value.
         */
        @SuppressWarnings("unchecked")
        public <NewS> Builder<W, NewS, D> readFrom(Supplier<NewS> getter) {
            Builder<W, NewS, D> cast = (Builder<W, NewS, D>) this;
            cast.stateGetter = getter;
            return cast;
        }
        
        /**
         * Sets the state setter for writing new values.
         */
        public Builder<W, S, D> writeTo(Consumer<S> setter) {
            this.stateSetter = setter;
            return this;
        }
        
        /**
         * Sets bidirectional transforms between state and display values.
         * 
         * @param toDisplay Converts state value to display value (e.g., 0.5 → 180°)
         * @param toState Converts display value to state value (e.g., 180° → 0.5)
         */
        @SuppressWarnings("unchecked")
        public <NewD> Builder<W, S, NewD> transform(
                Function<S, NewD> toDisplay, 
                Function<NewD, S> toState) {
            Builder<W, S, NewD> cast = (Builder<W, S, NewD>) this;
            cast.stateToDisplay = toDisplay;
            cast.displayToState = toState;
            return cast;
        }
        
        /**
         * Sets how to update the widget with a new display value.
         * 
         * <p>For LabeledSlider: {@code slider::setValue}
         * <p>For CyclingButtonWidget: {@code btn::setValue}
         */
        public Builder<W, S, D> applyWith(Consumer<D> updater) {
            this.widgetUpdater = updater;
            return this;
        }
        
        public Bound<W, S, D> build() {
            if (stateGetter == null) throw new IllegalStateException("readFrom() required");
            if (stateSetter == null) throw new IllegalStateException("writeTo() required");
            if (widgetUpdater == null) throw new IllegalStateException("applyWith() required");
            return new Bound<>(this);
        }
    }
}
```

---

## Convenience Factory Methods

For common patterns, add static factories:

```java
public final class Bound<W, S, D> {
    
    // ... existing code ...
    
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
        return Bound.of(slider)
            .readFrom(getter)
            .writeTo(setter)
            .applyWith(slider::setValue)
            .build();
    }
    
    /**
     * Creates a degree↔normalized binding for a float slider.
     * State stores 0.0-1.0, widget shows 0-360°.
     */
    public static Bound<LabeledSlider, Float, Float> sliderDegrees(
            LabeledSlider slider,
            Supplier<Float> getter,
            Consumer<Float> setter) {
        return Bound.of(slider)
            .readFrom(getter)
            .writeTo(setter)
            .transform(
                v -> v * 360f,  // state→display
                v -> v / 360f   // display→state
            )
            .applyWith(slider::setValue)
            .build();
    }
    
    /**
     * Creates a direct binding for a boolean toggle.
     */
    public static Bound<CyclingButtonWidget<Boolean>, Boolean, Boolean> toggle(
            CyclingButtonWidget<Boolean> toggle,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter) {
        return Bound.of(toggle)
            .readFrom(getter)
            .writeTo(setter)
            .applyWith(toggle::setValue)
            .build();
    }
    
    /**
     * Creates a direct binding for an enum dropdown.
     */
    public static <E extends Enum<E>> Bound<CyclingButtonWidget<E>, E, E> dropdown(
            CyclingButtonWidget<E> dropdown,
            Supplier<E> getter,
            Consumer<E> setter) {
        return Bound.of(dropdown)
            .readFrom(getter)
            .writeTo(setter)
            .applyWith(dropdown::setValue)
            .build();
    }
}
```

---

## Compound Binding: `Vec3Binding`

For Vector3f patterns (offset, rotation, scale), create a specialized binding:

```java
package net.cyberpunk042.client.gui.builder;

import net.cyberpunk042.client.gui.widget.LabeledSlider;
import org.joml.Vector3f;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Three-slider binding for Vector3f state values.
 * 
 * <p>Each slider modifies one component while preserving others.</p>
 * 
 * <h3>Example - Transform Offset</h3>
 * <pre>
 * Vec3Binding.of(sliderX, sliderY, sliderZ)
 *     .readFrom(() -> state.transform().offset())
 *     .writeTo(v -> state.set("transform.offset", v))
 *     .build();
 * </pre>
 */
public final class Vec3Binding {
    
    private final LabeledSlider sliderX, sliderY, sliderZ;
    private final Supplier<Vector3f> getter;
    private final Consumer<Vector3f> setter;
    
    private boolean syncing = false;
    
    private Vec3Binding(LabeledSlider x, LabeledSlider y, LabeledSlider z,
            Supplier<Vector3f> getter, Consumer<Vector3f> setter) {
        this.sliderX = x;
        this.sliderY = y;
        this.sliderZ = z;
        this.getter = getter;
        this.setter = setter;
    }
    
    /**
     * Called when X slider changes.
     */
    public void onXChanged(float value) {
        if (syncing) return;
        Vector3f current = new Vector3f(getter.get());
        current.x = value;
        setter.accept(current);
    }
    
    /**
     * Called when Y slider changes.
     */
    public void onYChanged(float value) {
        if (syncing) return;
        Vector3f current = new Vector3f(getter.get());
        current.y = value;
        setter.accept(current);
    }
    
    /**
     * Called when Z slider changes.
     */
    public void onZChanged(float value) {
        if (syncing) return;
        Vector3f current = new Vector3f(getter.get());
        current.z = value;
        setter.accept(current);
    }
    
    /**
     * Syncs all three sliders from state.
     */
    public void syncFromState() {
        if (syncing) return;
        syncing = true;
        try {
            Vector3f v = getter.get();
            if (v == null) v = new Vector3f();
            sliderX.setValue(v.x);
            sliderY.setValue(v.y);
            sliderZ.setValue(v.z);
        } finally {
            syncing = false;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder of(LabeledSlider x, LabeledSlider y, LabeledSlider z) {
        return new Builder(x, y, z);
    }
    
    public static class Builder {
        private final LabeledSlider x, y, z;
        private Supplier<Vector3f> getter;
        private Consumer<Vector3f> setter;
        
        private Builder(LabeledSlider x, LabeledSlider y, LabeledSlider z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public Builder readFrom(Supplier<Vector3f> getter) {
            this.getter = getter;
            return this;
        }
        
        public Builder writeTo(Consumer<Vector3f> setter) {
            this.setter = setter;
            return this;
        }
        
        public Vec3Binding build() {
            return new Vec3Binding(x, y, z, getter, setter);
        }
    }
}
```

---

## Summary: Binding Types

| Type | Use Case | State Type | Display Type |
|------|----------|------------|--------------|
| `Bound<W,S,S>` | Direct binding (no transform) | Float/Boolean/Enum | Same |
| `Bound<W,Float,Float>` | Degree transform | 0.0-1.0 | 0-360° |
| `Bound<W,Boolean,Boolean>` | Inverted display | depthWrite | !seeThrough |
| `Vec3Binding` | Compound Vector3f | Vector3f | 3× Float |

---

## Point 2: Panel Complexity Analysis

Now analyzing which panels have the most complex patterns...

---

## Point 2: Panel Complexity Analysis

Analyzing all 16 SubPanels to rank by complexity and identify patterns:

### Panel Complexity Matrix

| Panel | Lines | Widgets | Patterns Used | Complexity | Migration Priority |
|-------|-------|---------|---------------|------------|-------------------|
| **LinkingSubPanel** | 293 | ~15 | Direct, Toggle rows | ⭐⭐ Low | 1️⃣ First |
| **AppearanceSubPanel** | 225 | ~10 | Direct, Color buttons | ⭐⭐ Low | 2️⃣ Early |
| **VisibilitySubPanel** | ~340 | ~8 | Direct, Conditional | ⭐⭐ Low | 3️⃣ Early |
| **PredictionSubPanel** | ~285 | ~5 | Direct, Enable/Disable | ⭐⭐ Low | 4️⃣ Early |
| **BeamSubPanel** | ~300 | ~10 | Direct, Nested config | ⭐⭐⭐ Medium | 5️⃣ Mid |
| **LifecycleSubPanel** | ~200 | ~6 | Direct | ⭐⭐ Low | 6️⃣ Early |
| **BindingsSubPanel** | ~300 | ~8 | Dynamic lists | ⭐⭐⭐ Medium | 7️⃣ Mid |
| **ArrangeSubPanel** | ~250 | ~6 | Enums, Preview | ⭐⭐⭐ Medium | 8️⃣ Mid |
| **AnimationSubPanel** | 333 | ~12 | Direct, Enable groups, Fragment | ⭐⭐⭐ Medium | 9️⃣ Mid |
| **FillSubPanel** | 316 | ~10 | Conditional rebuild, Fragment | ⭐⭐⭐⭐ High | 🔟 Later |
| **ModifiersSubPanel** | 275 | ~12 | Nested objects, Multiple groups | ⭐⭐⭐ Medium | 1️⃣1️⃣ Mid |
| **TriggerSubPanel** | ~350 | ~10 | Dynamic lists, Complex | ⭐⭐⭐⭐ High | 1️⃣2️⃣ Later |
| **ForceSubPanel** | 268 | ~8 | Modal integration, Spawn action | ⭐⭐⭐⭐ High | 1️⃣3️⃣ Later |
| **TraceSubPanel** | ~150 | ~4 | Debug-only | ⭐ Minimal | 1️⃣4️⃣ Last |
| **TransformSubPanel** | 470 | ~25 | Vec3, Degrees, Conditional, Icon | ⭐⭐⭐⭐⭐ Complex | 1️⃣5️⃣ Last |
| **ShapeSubPanel** | 412 | ~20 | Dynamic per-shape, Pattern | ⭐⭐⭐⭐⭐ Complex | 1️⃣6️⃣ Last |

### Patterns Discovered

#### Pattern 1: **Enable/Disable Groups** (AnimationSubPanel)
```java
// Toggle enables/disables related widgets
spinToggle = createToggle("Spin", enabled -> {
    spinAxis.active = enabled;
    spinSpeed.active = enabled;
});
```
**ContentBuilder support needed:**
```java
b.group("Spin")
     .enabledBy("spin.active")
     .slider("Speed", "spin.speed", -360, 360)
     .dropdown("Axis", "spin.axis", SpinAxis.class)
 .endGroup();
```

#### Pattern 2: **Fragment/Preset System** (AnimationSubPanel, FillSubPanel)
```java
fragmentDropdown = createDropdown(presets, name -> {
    applyingFragment = true;
    FragmentRegistry.apply(state, name);
    syncFromState();
    applyingFragment = false;
});
```
**This is orthogonal to binding** - presets are UI-only state. Keep as-is.

#### Pattern 3: **Conditional Rebuild** (FillSubPanel, TransformSubPanel)
```java
// Mode change requires different widgets
modeDropdown.onChange(mode -> {
    rebuildWidgets();  // Destroys and recreates widgets
    notifyWidgetsChanged();
});
```
**ContentBuilder approach:** Use `when()` clauses:
```java
b.dropdown("Mode", "fill.mode", FillMode.class)
 .when(() -> mode() == CAGE, cage ->
     cage.slider("Wire", "fill.cage.thickness", 0, 0.1f)
        .slider("Solid", "fill.cage.solid", 0, 1)
 );
```

#### Pattern 4: **Icon Toggles** (TransformSubPanel)
```java
// Colored icon based on state: §a (green) / §7 (gray)
CyclingButtonWidget.builder(b -> Text.literal(b ? "§a↻" : "§7↻"))
```
**ContentBuilder support:**
```java
b.iconToggle("↻", "transform.inheritRotation")
 .activeColor("§a").inactiveColor("§7");
```

#### Pattern 5: **Modal Integration** (ForceSubPanel)
```java
configureButton.onClick(() -> {
    onConfigureRequest.accept(getSelectedConfig());  // Opens modal
});
```
**Keep as custom widget** - modals are too complex for declarative binding.

---

### Migration Order Recommendation

**Phase 2 Candidates (Proof of Concept):**
1. **LinkingSubPanel** - Cleanest example, 15 widgets, all patterns representable
2. **AppearanceSubPanel** - Simple sliders + color buttons

**Phase 4 Order (Gradual Migration):**
1. VisibilitySubPanel
2. PredictionSubPanel  
3. LifecycleSubPanel
4. BeamSubPanel
5. AnimationSubPanel (introduces enable groups)
6. ArrangeSubPanel
7. BindingsSubPanel
8. ModifiersSubPanel
9. FillSubPanel (introduces conditional rebuild)
10. TriggerSubPanel
11. ForceSubPanel
12. TraceSubPanel
13. TransformSubPanel (most complex)
14. ShapeSubPanel (most dynamic)

---

## Point 3: Phase 0 - Sync Trigger Mechanism

### The Core Question: When is `syncFromState()` called?

Currently, each panel has its own `syncFromState()` but it's called inconsistently:
- `AnimationSubPanel.applyPreset()` → calls `syncFromState()`
- `FillSubPanel.applyPreset()` → calls `rebuildWidgets()` instead
- Profile load → calls `rebuildWidgets()` on the whole screen

**We need a centralized, automatic sync system.**

### Design: Observer Pattern with `FieldEditState`

```java
// ═══════════════════════════════════════════════════════════════════════════
// IN FieldEditState.java - Add these methods
// ═══════════════════════════════════════════════════════════════════════════

public interface StateChangeListener {
    /**
     * Called when state changes externally (profile load, primitive switch, command).
     * 
     * @param changeType Type of change (helps panels decide if they care)
     */
    void onStateChanged(ChangeType changeType);
}

public enum ChangeType {
    PROFILE_LOADED,     // Entire profile replaced
    PRIMITIVE_SWITCHED, // User selected different primitive in layer
    LAYER_SWITCHED,     // User selected different layer
    FRAGMENT_APPLIED,   // Preset applied (animation, fill, etc.)
    PROPERTY_CHANGED,   // Single property changed (e.g., from /field command)
    FULL_RESET          // State completely reset
}

private final List<StateChangeListener> stateListeners = new ArrayList<>();

public void addStateListener(StateChangeListener listener) {
    stateListeners.add(listener);
}

public void removeStateListener(StateChangeListener listener) {
    stateListeners.remove(listener);
}

/**
 * Notifies all listeners of a state change.
 * Call this after bulk updates (profile load, primitive switch).
 */
public void notifyStateChanged(ChangeType type) {
    for (StateChangeListener listener : stateListeners) {
        try {
            listener.onStateChanged(type);
        } catch (Exception e) {
            Logging.GUI.topic("state").warn("Listener error: {}", e.getMessage());
        }
    }
}
```

### BoundPanel Integration

```java
public abstract class BoundPanel extends AbstractPanel implements StateChangeListener {
    
    protected final List<Bound<?, ?, ?>> bindings = new ArrayList<>();
    protected final List<Vec3Binding> vec3Bindings = new ArrayList<>();
    
    @Override
    protected void onAttached() {
        state.addStateListener(this);
    }
    
    @Override
    protected void onDetached() {
        state.removeStateListener(this);
    }
    
    @Override
    public void onStateChanged(ChangeType type) {
        switch (type) {
            case PROFILE_LOADED, PRIMITIVE_SWITCHED, LAYER_SWITCHED, FULL_RESET ->
                // Major change: sync all bindings
                syncAllFromState();
            case FRAGMENT_APPLIED ->
                // Might need rebuild if mode changed, otherwise just sync
                if (needsRebuildOnFragmentApply()) {
                    rebuildContent();
                } else {
                    syncAllFromState();
                }
            case PROPERTY_CHANGED ->
                // Single property: could be more targeted, but sync all for now
                syncAllFromState();
        }
    }
    
    protected void syncAllFromState() {
        for (Bound<?, ?, ?> binding : bindings) {
            binding.syncFromState();
        }
        for (Vec3Binding v3 : vec3Bindings) {
            v3.syncFromState();
        }
    }
    
    /**
     * Override in panels that need rebuild on fragment apply (e.g., FillSubPanel).
     */
    protected boolean needsRebuildOnFragmentApply() {
        return false;
    }
}
```

### Where to Call `notifyStateChanged()`

| Location | Change Type | Current Code |
|----------|-------------|--------------|
| `ProfileManager.loadProfile()` | PROFILE_LOADED | After `state.loadFrom(definition)` |
| `LayerManager.selectLayer()` | LAYER_SWITCHED | After updating selectedLayerIndex |
| `LayerManager.selectPrimitive()` | PRIMITIVE_SWITCHED | After updating selectedPrimitiveIndex |
| `FragmentRegistry.apply*()` | FRAGMENT_APPLIED | At end of each apply method |
| `FieldEditCommands` (client commands) | PROPERTY_CHANGED | After `state.set()` |
| `FieldEditState.reset()` | FULL_RESET | At end of reset |

### Implementation Checklist

```java
// 1. ProfileManager.loadProfile() - after loading adapters
public void loadProfile(String name) {
    // ... existing load logic ...
    state.clearDirty();
    state.notifyStateChanged(ChangeType.PROFILE_LOADED);  // ADD THIS
}

// 2. LayerManager.selectLayer()
public void selectLayer(int index) {
    if (index == selectedLayerIndex) return;
    saveCurrentPrimitive();  // Save before switching
    selectedLayerIndex = index;
    selectedPrimitiveIndex = 0;
    loadPrimitiveToAdapters();
    state.notifyStateChanged(ChangeType.LAYER_SWITCHED);  // ADD THIS
}

// 3. LayerManager.selectPrimitive()
public void selectPrimitive(int index) {
    if (index == selectedPrimitiveIndex) return;
    saveCurrentPrimitive();  // Save before switching
    selectedPrimitiveIndex = index;
    loadPrimitiveToAdapters();
    state.notifyStateChanged(ChangeType.PRIMITIVE_SWITCHED);  // ADD THIS
}

// 4. FragmentRegistry - at end of each apply method
public static void applyFillFragment(FieldEditState state, String name) {
    // ... existing apply logic ...
    state.notifyStateChanged(ChangeType.FRAGMENT_APPLIED);  // ADD THIS
}
```

---

## Summary: Implementation Phases

### Phase 0: Foundation Infrastructure (2-3 hours)
- [ ] Add `StateChangeListener` interface to `FieldEditState`
- [ ] Add `ChangeType` enum
- [ ] Add listener registration methods
- [ ] Add `notifyStateChanged()` calls to 6 locations
- [ ] Test that listeners fire correctly

### Phase 1: Binding Classes (3-4 hours)
- [ ] Create `Bound<W,S,D>` class
- [ ] Create `Vec3Binding` class
- [ ] Add convenience factory methods
- [ ] Unit test sync behavior

### Phase 2: BoundPanel Base Class (2 hours)
- [ ] Create `BoundPanel` extending `AbstractPanel`
- [ ] Implement `StateChangeListener`
- [ ] Add auto-registration/deregistration
- [ ] Add `syncAllFromState()` method

### Phase 3: Proof of Concept (2 hours)
- [ ] Migrate `LinkingSubPanel` to `BoundPanel`
- [ ] Verify bidirectional sync works
- [ ] Test profile load → widgets update
- [ ] Test primitive switch → widgets update

### Phase 4: ContentBuilder API (3-4 hours)
- [ ] Create `ContentBuilder` class
- [ ] Create `RowBuilder` for side-by-side widgets
- [ ] Add `slider()`, `toggle()`, `dropdown()` methods
- [ ] Add `vec3()` for Vector3f
- [ ] Add `when()` for conditional widgets

### Phase 5+: Gradual Migration (ongoing)
- [ ] Migrate panels in order of complexity
- [ ] Leave ShapeSubPanel and TransformSubPanel for last
