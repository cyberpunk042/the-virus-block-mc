# GUI Architecture Refactor Plan v2

## Critical Analysis (Senior Review)

### Before We Build Anything - Hard Questions

**Q1: Why do widgets break so easily?**
The root cause isn't "too many lines" - it's **state synchronization**:
- Widget created with `initial(value)` → value from state at creation time
- User changes widget → callback fires → state updates
- **BUT**: If state changes externally (profile load, primitive switch), widgets still show OLD values
- Current fix: `rebuildWidgets()` - expensive, destroys references, re-registers with screen

**Q2: What patterns actually repeat?**
Looking at real code, NOT just sliders:
```java
// Pattern A: Simple slider with direct state path
.onChange(v -> state.set("fill.thickness", v))

// Pattern B: Compound update (3 values as Vector3f)
.onChange(v -> updateOffset(v, null, null))  // calls state.set with merged vector

// Pattern C: Conditional rebuild needed
.onChange(m -> { state.set("fill.mode", m); rebuildWidgets(); notifyWidgetsChanged(); })

// Pattern D: Custom formatting/parsing
.onChange(v -> state.set("link.orbitPhaseOffset", v / 360f))  // degrees → 0-1

// Pattern E: Toggle with icon coloring
Text.literal(b ? "§a↻" : "§7↻")  // green if on, gray if off
```

**A naive BoundWidgetFactory only handles Pattern A** - ~40% of actual widgets.

**Q3: What are the REAL migration risks?**
- 17 SubPanels, ~200 widgets total
- Each has custom edge cases
- Some panels have inter-widget dependencies (mode changes what's visible)
- Widget references stored as fields (for `syncFromState()`)

---

## Revised Architecture

### Core Insight: **Bidirectional Binding is the Real Problem**

Current flow (unidirectional):
```
State ← Widget (on user change)
State → Widget (only on rebuildWidgets)
```

Needed flow (bidirectional):
```
State ↔ Widget (automatic sync both directions)
```

### Solution: **Bindable Widget Wrapper**

Instead of replacing widgets, **wrap** existing Minecraft widgets:

```java
public class Bound<W extends ClickableWidget> {
    private final W widget;
    private final Supplier<Object> getter;
    private final Consumer<Object> setter;
    
    // Sync widget → state (on user action)
    private void onWidgetChange(Object newValue) {
        setter.accept(newValue);
        state.markDirty();
    }
    
    // Sync state → widget (call when state changes externally)
    public void syncFromState() {
        Object value = getter.get();
        applyToWidget(value);
    }
    
    public W widget() { return widget; }
}
```

### Panel Structure:

```java
public abstract class BoundPanel extends AbstractPanel {
    protected final List<Bound<?>> bindings = new ArrayList<>();
    
    // Subclass registers bindings
    protected abstract void buildContent(ContentBuilder b);
    
    @Override
    public void init(int width, int height) {
        bindings.clear();
        widgets.clear();
        
        ContentBuilder b = new ContentBuilder(this, width);
        buildContent(b);
        
        contentHeight = b.getCurrentY();
    }
    
    // Called when state changes externally (profile load, primitive switch)
    public void syncFromState() {
        for (Bound<?> binding : bindings) {
            binding.syncFromState();
        }
    }
}
```

### ContentBuilder (Fluent API):

```java
public class ContentBuilder {
    private final BoundPanel panel;
    private final FieldEditState state;
    private final int width;
    private int x, y;
    
    // ═══════════════════════════════════════════════════════════════════
    // SIMPLE WIDGETS (direct path binding)
    // ═══════════════════════════════════════════════════════════════════
    
    public ContentBuilder slider(String label, String path, float min, float max) {
        return slider(label, path, min, max, "%.2f", null);
    }
    
    public ContentBuilder slider(String label, String path, float min, float max,
                                  String format, @Nullable Consumer<Float> extra) {
        var slider = LabeledSlider.builder(label)
            .position(x, y).width(width)
            .range(min, max).initial(state.getFloat(path)).format(format)
            .compact()
            .onChange(v -> {
                state.set(path, v);
                state.markDirty();
                if (extra != null) extra.accept(v);
            })
            .build();
        
        var bound = Bound.of(slider,
            () -> state.getFloat(path),
            v -> slider.setValue((Float) v));
        
        panel.bindings.add(bound);
        panel.widgets.add(slider);
        y += GuiConstants.COMPACT_HEIGHT + GAP;
        return this;
    }
    
    public ContentBuilder toggle(String label, String path) {
        // Similar pattern...
    }
    
    public <E extends Enum<E>> ContentBuilder dropdown(String label, String path, E[] values) {
        // Similar pattern...
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // COMPOUND WIDGETS (custom getter/setter)
    // ═══════════════════════════════════════════════════════════════════
    
    public ContentBuilder slider(String label, 
                                  Supplier<Float> getter,
                                  Consumer<Float> setter,
                                  float min, float max, String format) {
        // Uses custom get/set instead of path
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // LAYOUT HELPERS
    // ═══════════════════════════════════════════════════════════════════
    
    public ContentBuilder gap(int pixels) { y += pixels; return this; }
    public ContentBuilder label(String text) { /* draw label, advance y */ return this; }
    public RowBuilder row() { return new RowBuilder(this); }
    public int getCurrentY() { return y; }
}
```

### Row Builder (for side-by-side widgets):

```java
public class RowBuilder {
    private final ContentBuilder parent;
    private final List<WidgetSpec> specs = new ArrayList<>();
    
    public RowBuilder slider(String label, String path, float min, float max) {
        specs.add(new SliderSpec(label, path, min, max));
        return this;
    }
    
    public RowBuilder toggle(String label, String path) {
        specs.add(new ToggleSpec(label, path));
        return this;
    }
    
    public ContentBuilder endRow() {
        // Calculate widths based on spec count
        int each = (parent.width - (specs.size() - 1) * GAP) / specs.size();
        int x = parent.x;
        for (WidgetSpec spec : specs) {
            spec.build(parent, x, each);
            x += each + GAP;
        }
        parent.y += COMPACT_H + GAP;
        return parent;
    }
}
```

---

## Migration Example: LinkingSubPanel

### BEFORE (~290 lines):
```java
@Override
public void init(int width, int height) {
    widgets.clear();
    
    int x = GuiConstants.PADDING;
    int y = startY + GAP;
    int w = width - GuiConstants.PADDING * 2;
    int halfW = (w - GAP) / 2;
    int thirdW = (w - GAP * 2) / 3;
    
    buildAvailableTargets();
    
    Primitive currentPrim = state.getSelectedPrimitive();
    PrimitiveLink link = currentPrim != null ? currentPrim.link() : null;
    String currentTarget = link != null ? link.target() : null;
    
    List<String> targetOptions = new ArrayList<>();
    targetOptions.add("(none)");
    targetOptions.addAll(availableTargets);
    
    targetDropdown = CyclingButtonWidget.<String>builder(v -> Text.literal("Target: " + v))
        .values(targetOptions.toArray(new String[0]))
        .initially(currentTarget != null ? currentTarget : "(none)")
        .omitKeyText()
        .build(x, y, w, COMPACT_H, Text.literal(""),
            (btn, val) -> {
                String newTarget = "(none)".equals(val) ? null : val;
                state.set("link.target", newTarget);
            });
    widgets.add(targetDropdown);
    y += COMPACT_H + GAP;
    
    // ... 200+ more lines of similar boilerplate
}
```

### AFTER (~80 lines):
```java
public class LinkingSubPanel extends BoundPanel {
    
    @Override
    protected void buildContent(ContentBuilder b) {
        b.dropdown("Target", "link.target", getTargetOptions())
         .gap(4)
         
         .label("Position Linking")
         .row()
             .toggle("Follow", "link.follow")
             .toggle("FollowDyn", "link.followDynamic")
             .toggle("RadMatch", "link.radiusMatch")
         .endRow()
         
         .label("Orbit Linking")
         .row()
             .toggle("OrbitSync", "link.orbitSync")
             .toggle("Color", "link.colorMatch")
             .toggle("Alpha", "link.alphaMatch")
         .endRow()
         
         .gap(8)
         .label("Phase Parameters")
         .slider("Phase", "link.phaseOffset", 0f, 1f)
         .slider("OrbPh°", "link.orbitPhaseOffset", 0f, 1f)
         
         .label("Orbit Parameters")
         .slider("OrbRadOff", "link.orbitRadiusOffset", -5f, 5f)
         .slider("SpdMult", "link.orbitSpeedMult", 0.1f, 5f)
         .slider("IncOff", "link.orbitInclinationOffset", -90f, 90f)
         .slider("PrecOff", "link.orbitPrecessionOffset", -180f, 180f)
         .slider("ShapeRadOff", "link.radiusOffset", -5f, 5f);
    }
    
    private List<String> getTargetOptions() {
        List<String> targets = new ArrayList<>();
        targets.add("(none)");
        targets.addAll(findAvailablePrimitives());
        return targets;
    }
}
```

---

## Handling Edge Cases

### Case 1: Conditional Visibility (Fill mode changes what's visible)
```java
b.dropdown("Mode", "fill.mode", FillMode.values())
 .onValueChange(mode -> rebuildWidgets());  // Triggers full rebuild

// Widgets below are only added when condition is true
b.when(() -> state.fill().mode() == FillMode.CAGE, cage -> {
    cage.slider("Wire", "fill.cage.thickness", 0.001f, 0.1f);
    cage.slider("Solid", "fill.cage.solid", 0f, 1f);
});
```

### Case 2: Compound Vector3f
```java
b.vec3("Offset", "transform.offset", -10f, 10f);
// Internally creates 3 sliders, updates state as Vector3f
```

### Case 3: Custom Formatting
```java
b.slider("Phase°", "link.orbitPhaseOffset", 0f, 1f)
 .displayAs(v -> String.format("%.0f°", v * 360))  // Show as degrees
 .parseAs(v -> v / 360f);  // Store as 0-1
```

### Case 4: Icon Toggles
```java
b.iconToggle("↻", "transform.inheritRotation", "§a", "§7")  // green/gray coloring
```

---

## Implementation Phases (Revised)

### Phase 1: Foundation (3-4 hours)
1. Create `Bound<W>` wrapper class
2. Create `BoundPanel` base class with `syncFromState()`
3. Create `ContentBuilder` with basic `slider()`, `toggle()`, `dropdown()`
4. Create `RowBuilder` for side-by-side layouts

### Phase 2: Proof of Concept (2 hours)
1. Migrate **LinkingSubPanel** (simplest, newest)
2. Verify bidirectional binding works
3. Test profile loading → widgets update

### Phase 3: Advanced Features (2-3 hours)
1. Add `when()` for conditional widgets
2. Add `vec3()` for Vector3f
3. Add `iconToggle()` for styled toggles
4. Add scrolling support in BoundPanel

### Phase 4: Gradual Migration (ongoing)
1. Migrate one panel per session
2. Start with simpler panels (Appearance, Lifecycle)
3. Leave complex panels (Transform, Shape) for last

---

## Success Criteria

| Metric | Threshold |
|--------|-----------|
| Lines per simple widget | ≤ 1 |
| Lines for side-by-side row | ≤ 4 |
| syncFromState() required | Yes, automated |
| Scroll support | Built-in |
| Widget references accessible | Yes, via `Bound.widget()` |
| Backward compatible | Old panels still compile |

---

## Files to Create

```
gui/
├── builder/
│   ├── Bound.java                   # Widget binding wrapper
│   ├── ContentBuilder.java          # Fluent widget builder
│   └── RowBuilder.java              # Row layout helper
└── panel/
    └── BoundPanel.java              # Base class with binding support
```

---

## Key Differences from v1

| v1 | v2 |
|----|-----|
| Just returns next Y | Returns `this` for chaining |
| No state→widget sync | `syncFromState()` built-in |
| Only simple paths | Supports custom getters/setters |
| No conditional | `when()` for conditional widgets |
| No row support | `row().endRow()` for side-by-side |
| Replace widgets | Wrap existing widgets |

---

## Senior Review Checklist

- [ ] **Does it solve the actual problem?** Yes - bidirectional binding fixes sync issues
- [ ] **Is it adoptable incrementally?** Yes - one panel at a time, old panels still work
- [ ] **Does it handle edge cases?** Yes - custom getters, conditionals, icon toggles
- [ ] **Is the API intuitive?** Yes - fluent builder reads like a spec
- [ ] **Is performance acceptable?** Yes - no extra overhead vs current approach
- [ ] **Is it testable?** Yes - `syncFromState()` can be unit tested
