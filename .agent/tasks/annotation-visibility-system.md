# Task: Annotation-Based Widget Visibility System

## Overview
Implement a declarative, annotation-based system for conditionally showing/hiding GUI widgets based on state. This replaces manual `if` statements with `@ShowWhen` annotations.

## Design Decisions
- **OR Logic**: Multiple `@ShowWhen` annotations = widget visible if ANY condition matches
- **Dynamic Positioning**: Only position visible widgets (no gaps)
- **Scope**: Start with FillSubPanel, design for reuse in Visibility/Shape panels

---

## Phase 1: Core Annotations

### File: `src/client/java/net/cyberpunk042/client/gui/annotation/ShowWhen.java`

```java
package net.cyberpunk042.client.gui.annotation;

import java.lang.annotation.*;

/**
 * Declares visibility conditions for a widget field.
 * 
 * <p>Multiple annotations are combined with OR logic - widget is visible
 * if ANY condition matches. Leave condition empty to ignore it.</p>
 * 
 * <h3>Examples</h3>
 * <pre>
 * // Show when fill mode is CAGE or WIREFRAME
 * @ShowWhen(fillMode = "CAGE")
 * @ShowWhen(fillMode = "WIREFRAME")
 * private LabeledSlider wireThicknessSlider;
 * 
 * // Show when mask type is NOT "FULL"
 * @ShowWhen(maskType = "FULL", not = true)
 * private LabeledSlider blendSlider;
 * 
 * // Show only for sphere shape in CAGE mode
 * @ShowWhen(fillMode = "CAGE", shapeType = "sphere")
 * private LabeledSlider latitudeSlider;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ShowWhenConditions.class)
public @interface ShowWhen {
    /** Fill mode condition: SOLID, WIREFRAME, CAGE, POINTS */
    String fillMode() default "";
    
    /** Mask type condition */
    String maskType() default "";
    
    /** Shape type condition: sphere, cylinder, etc. */
    String shapeType() default "";
    
    /** Negate the condition (show when NOT matching) */
    boolean not() default false;
}
```

### File: `src/client/java/net/cyberpunk042/client/gui/annotation/ShowWhenConditions.java`

```java
package net.cyberpunk042.client.gui.annotation;

import java.lang.annotation.*;

/**
 * Container for repeatable @ShowWhen annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ShowWhenConditions {
    ShowWhen[] value();
}
```

---

## Phase 2: Visibility Resolver

### File: `src/client/java/net/cyberpunk042/client/gui/visibility/WidgetVisibilityResolver.java`

```java
package net.cyberpunk042.client.gui.visibility;

import net.cyberpunk042.client.gui.annotation.ShowWhen;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.visual.fill.FillMode;

import java.lang.reflect.Field;

/**
 * Evaluates @ShowWhen annotations to determine widget visibility.
 * 
 * <p>Uses reflection to read annotations and match against current state.
 * Performance note: Called once per widget rebuild, not per frame.</p>
 */
public class WidgetVisibilityResolver {
    
    /**
     * Determines if a field's widget should be visible.
     * 
     * @param field The field with potential @ShowWhen annotations
     * @param state Current edit state
     * @return true if widget should be shown
     */
    public static boolean shouldShow(Field field, FieldEditState state) {
        ShowWhen[] conditions = field.getAnnotationsByType(ShowWhen.class);
        
        // No annotations = always visible
        if (conditions.length == 0) {
            return true;
        }
        
        // OR logic: visible if ANY condition matches
        for (ShowWhen condition : conditions) {
            boolean matches = matchesCondition(condition, state);
            if (condition.not()) {
                matches = !matches;
            }
            if (matches) {
                return true;
            }
        }
        
        return false; // No conditions matched
    }
    
    private static boolean matchesCondition(ShowWhen condition, FieldEditState state) {
        // All non-empty conditions must match (AND within single annotation)
        
        // Check fill mode
        if (!condition.fillMode().isEmpty()) {
            FillMode current = state.fill().mode();
            if (!current.name().equalsIgnoreCase(condition.fillMode())) {
                return false;
            }
        }
        
        // Check mask type
        if (!condition.maskType().isEmpty()) {
            String current = state.visibility().maskType();
            if (!current.equalsIgnoreCase(condition.maskType())) {
                return false;
            }
        }
        
        // Check shape type
        if (!condition.shapeType().isEmpty()) {
            String current = state.getString("shapeType");
            if (!current.equalsIgnoreCase(condition.shapeType())) {
                return false;
            }
        }
        
        return true; // All specified conditions matched
    }
}
```

---

## Phase 3: Widget Spec with Position Tracking

### File: `src/client/java/net/cyberpunk042/client/gui/visibility/WidgetSpec.java`

```java
package net.cyberpunk042.client.gui.visibility;

import net.minecraft.client.gui.widget.ClickableWidget;
import java.util.function.Function;

/**
 * Specification for a widget with its builder and positioning info.
 */
public record WidgetSpec(
    String fieldName,
    Function<WidgetPositioner, ClickableWidget> builder,
    boolean fullWidth // vs half width
) {}

/**
 * Provides current position for widget building.
 */
public record WidgetPositioner(int x, int y, int width, int halfWidth) {
    public int getWidth(boolean full) {
        return full ? width : halfWidth;
    }
}
```

---

## Phase 4: Refactor FillSubPanel

### Changes to FillSubPanel:

1. Add `@ShowWhen` annotations to widget fields
2. Modify `rebuildWidgets()` to use reflection
3. Only position visible widgets

```java
public class FillSubPanel extends AbstractPanel {
    
    // Always shown
    private CyclingButtonWidget<String> fragmentDropdown;
    private CyclingButtonWidget<FillMode> fillModeDropdown;
    
    // Conditionally shown
    @ShowWhen(fillMode = "CAGE")
    @ShowWhen(fillMode = "WIREFRAME")
    private LabeledSlider wireThicknessSlider;
    
    @ShowWhen(fillMode = "SOLID")
    private CyclingButtonWidget<Boolean> depthWriteToggle;
    
    @ShowWhen(fillMode = "CAGE")
    private LabeledSlider primaryCountSlider;
    
    @ShowWhen(fillMode = "CAGE")  
    private LabeledSlider secondaryCountSlider;
    
    @ShowWhen(fillMode = "CAGE")
    private CyclingButtonWidget<Boolean> allEdgesToggle;
    
    @ShowWhen(fillMode = "CAGE")
    private CyclingButtonWidget<Boolean> faceOutlinesToggle;
    
    @ShowWhen(fillMode = "POINTS")
    private LabeledSlider pointSizeSlider;
    
    private void rebuildWidgets() {
        boolean needsOffset = bounds != null && !bounds.isEmpty();
        widgets.clear();
        
        // Build all widgets first
        buildAllWidgets();
        
        // Add only visible ones to widgets list with proper positioning
        int y = startY + GuiConstants.PADDING;
        y = addIfVisible("fragmentDropdown", y, true);
        y = addIfVisible("fillModeDropdown", y, true);
        y = addIfVisible("wireThicknessSlider", y, true);
        y = addIfVisible("depthWriteToggle", y, true);
        y = addIfVisible("primaryCountSlider", y, false); // half width
        y = addIfVisible("secondaryCountSlider", y, false, true); // same row
        // etc.
        
        contentHeight = y - startY;
        if (needsOffset) applyBoundsOffset();
    }
}
```

---

## Phase 5: ShapeSubPanel Compatibility

ShapeSubPanel uses two patterns:
1. **Top-level controls** (shape dropdown, preset) - always visible
2. **ShapeWidgetSpec** - data-driven, already conditional on shape type

The annotation system can complement ShapeWidgetSpec:

```java
// Option A: Annotations for top-level controls
@ShowWhen(shapeType = "cylinder")
@ShowWhen(shapeType = "beam")  
private CyclingButtonWidget<Boolean> cylinderOpenEnded;

// Option B: ShapeWidgetSpec already handles shape-specific controls
// No changes needed - it's already data-driven

// Option C: Hybrid - use annotations for mode-based visibility
@ShowWhen(fillMode = "CAGE", shapeType = "sphere")
private CyclingButtonWidget<QuadPattern> quadPatternDropdown;
```

**Recommendation**: Keep ShapeWidgetSpec for shape-specific params, use annotations for cross-cutting concerns (fill mode, renderer mode).

---

## Implementation Order

1. [ ] Create `ShowWhen` and `ShowWhenConditions` annotations
2. [ ] Create `WidgetVisibilityResolver` 
3. [ ] Add helper method to AbstractPanel: `addIfVisible()`
4. [ ] Refactor FillSubPanel to use annotations
5. [ ] Test mode switching updates visibility correctly
6. [ ] Document pattern for future panels (Visibility, Lifecycle)

---

## Testing Checklist

- [ ] SOLID mode shows: See-Through toggle
- [ ] WIREFRAME mode shows: Wire Thickness slider
- [ ] CAGE mode shows: Wire Thickness, primary/secondary counts, edge toggles
- [ ] POINTS mode shows: Point Size slider
- [ ] Mode change correctly rebuilds and repositions widgets
- [ ] No gaps between visible widgets
- [ ] Widget callbacks still work after rebuild
