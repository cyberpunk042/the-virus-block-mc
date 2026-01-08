package net.cyberpunk042.client.gui.shape;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds GUI widgets from {@link ShapeWidgetSpec} specifications.
 * 
 * <p>This is the "interpreter" in the Specification pattern. It takes
 * declarative specs and produces actual Minecraft widgets.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * var builder = new ShapeControlBuilder(state, this::onUserChange);
 * List&lt;Object&gt; specs = ShapeWidgetSpec.forShape("sphere");
 * int nextY = builder.build(specs, x, y, width);
 * widgets.addAll(builder.getWidgets());
 * </pre>
 * 
 * <h3>Benefits</h3>
 * <ul>
 *   <li>Eliminates repetitive widget creation code</li>
 *   <li>Automatic half-width pairing and row management</li>
 *   <li>Centralized styling and layout logic</li>
 *   <li>Widgets stored by key for easy sync</li>
 * </ul>
 */
public class ShapeControlBuilder {
    
    private final FieldEditState state;
    private final Consumer<Runnable> onChangeWrapper;
    
    // Built widgets
    private final List<ClickableWidget> widgets = new ArrayList<>();
    
    // Widgets indexed by state key for sync operations
    private final Map<String, LabeledSlider> sliders = new HashMap<>();
    private final Map<String, CheckboxWidget> checkboxes = new HashMap<>();
    private final Map<String, CyclingButtonWidget<?>> dropdowns = new HashMap<>();
    
    /**
     * Creates a new builder.
     * 
     * @param state The field edit state for reading/writing values
     * @param onChangeWrapper Wrapper for change callbacks (e.g., to mark as "Custom" preset)
     */
    public ShapeControlBuilder(FieldEditState state, Consumer<Runnable> onChangeWrapper) {
        this.state = state;
        this.onChangeWrapper = onChangeWrapper;
    }
    
    /**
     * Clears all built widgets. Call before rebuilding.
     */
    public void clear() {
        widgets.clear();
        sliders.clear();
        checkboxes.clear();
        dropdowns.clear();
    }
    
    /**
     * Builds widgets from a list of specifications.
     * 
     * @param specs List of widget specs (SliderSpec, CheckboxSpec, etc.)
     * @param x Starting X position
     * @param y Starting Y position
     * @param width Available width for widgets
     * @return The Y position after all widgets (for continuing layout)
     */
    public int build(List<Object> specs, int x, int y, int width) {
        int step = GuiConstants.COMPACT_HEIGHT + GuiConstants.COMPACT_GAP;
        int halfW = (width - GuiConstants.COMPACT_GAP) / 2;
        
        boolean pendingHalf = false;  // Is there a half-width widget waiting for a pair?
        int currentX = x;
        int currentY = y;
        
        for (Object spec : specs) {
            boolean isHalfWidth = getHalfWidth(spec);
            
            // Handle row positioning
            if (isHalfWidth) {
                if (pendingHalf) {
                    // Second half of the row - position to right side
                    currentX = x + halfW + GuiConstants.COMPACT_GAP;
                } else {
                    // First half of the row - position to left
                    currentX = x;
                }
            } else {
                // Full width widget
                if (pendingHalf) {
                    // Previous half-width widget was alone, move to next row
                    currentY += step;
                    pendingHalf = false;
                }
                currentX = x;
            }
            
            // Build the widget
            int widgetWidth = isHalfWidth ? halfW : width;
            ClickableWidget widget = buildWidget(spec, currentX, currentY, widgetWidth);
            
            if (widget != null) {
                widgets.add(widget);
            }
            
            // Update row tracking
            if (isHalfWidth) {
                if (pendingHalf) {
                    // Completed a row of two halves
                    currentY += step;
                    pendingHalf = false;
                } else {
                    // Waiting for second half
                    pendingHalf = true;
                }
            } else {
                // Full width always advances row
                currentY += step;
            }
            
            // Handle explicit row breaks
            if (spec instanceof ShapeWidgetSpec.RowBreak) {
                if (pendingHalf) {
                    currentY += step;
                    pendingHalf = false;
                }
            }
        }
        
        // If last widget was half-width without pair, advance row
        if (pendingHalf) {
            currentY += step;
        }
        
        return currentY;
    }
    
    /**
     * Returns all built widgets.
     */
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    /**
     * Returns sliders indexed by state key.
     * Use for syncing slider values from state.
     */
    public Map<String, LabeledSlider> getSliders() {
        return sliders;
    }
    
    /**
     * Syncs all slider values from state.
     * Call this after applying a preset or loading data.
     */
    public void syncFromState() {
        for (var entry : sliders.entrySet()) {
            String key = entry.getKey();
            LabeledSlider slider = entry.getValue();
            if (slider != null) {
                float value = state.getFloat(key);
                slider.setValue(value);
            }
        }
        
        // For checkboxes - we'd need to rebuild since CheckboxWidget doesn't have setValue
        // This is a Minecraft limitation, so checkboxes sync happens on rebuild
        
        Logging.GUI.topic("shape").debug("Synced {} sliders from state", sliders.size());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET BUILDERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private ClickableWidget buildWidget(Object spec, int x, int y, int width) {
        if (spec instanceof ShapeWidgetSpec.SliderSpec s) {
            return buildSlider(s, x, y, width);
        } else if (spec instanceof ShapeWidgetSpec.CheckboxSpec c) {
            return buildCheckbox(c, x, y);
        } else if (spec instanceof ShapeWidgetSpec.EnumDropdownSpec<?> e) {
            return buildEnumDropdown(e, x, y, width);
        } else if (spec instanceof ShapeWidgetSpec.SectionHeader h) {
            // Create a styled section header label
            return buildSectionHeader(h, x, y, width);
        } else if (spec instanceof ShapeWidgetSpec.RowBreak) {
            // Row breaks are layout control, not widgets
            return null;
        }
        
        Logging.GUI.topic("shape").warn("Unknown spec type: {}", spec.getClass().getSimpleName());
        return null;
    }
    
    private LabeledSlider buildSlider(ShapeWidgetSpec.SliderSpec spec, int x, int y, int width) {
        float initial = spec.format().contains("d") 
            ? state.getInt(spec.stateKey()) 
            : state.getFloat(spec.stateKey());
        
        var builder = LabeledSlider.builder(spec.label())
            .position(x, y)
            .width(width)
            .height(GuiConstants.COMPACT_HEIGHT)
            .range(spec.min(), spec.max())
            .initial(initial)
            .format(spec.format());
        
        if (spec.step() != null) {
            builder.step(spec.step());
        }
        
        String stateKey = spec.stateKey();
        builder.onChange(v -> onChangeWrapper.accept(() -> {
            if (spec.format().contains("d")) {
                state.set(stateKey, Math.round(v));
            } else {
                state.set(stateKey, v);
            }
        }));
        
        LabeledSlider slider = builder.build();
        sliders.put(stateKey, slider);
        
        return slider;
    }
    
    private CheckboxWidget buildCheckbox(ShapeWidgetSpec.CheckboxSpec spec, int x, int y) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        boolean initial = state.getBool(spec.stateKey());
        
        String stateKey = spec.stateKey();
        CheckboxWidget checkbox = GuiWidgets.checkbox(
            x, y, spec.label(), initial, spec.tooltip(),
            textRenderer, v -> onChangeWrapper.accept(() -> {
                state.set(stateKey, v);
                
                // SPECIAL: When sphere.horizonEnabled OR sphere.coronaEnabled changes, auto-switch quad pattern
                // Shader effects require standard_quad winding, normal rendering uses filled_1
                if ("sphere.horizonEnabled".equals(stateKey) || "sphere.coronaEnabled".equals(stateKey)) {
                    // Check if EITHER effect is now enabled
                    boolean horizonOn = "sphere.horizonEnabled".equals(stateKey) ? v : state.getBool("sphere.horizonEnabled");
                    boolean coronaOn = "sphere.coronaEnabled".equals(stateKey) ? v : state.getBool("sphere.coronaEnabled");
                    
                    if (horizonOn || coronaOn) {
                        // Either shader effect ON → use standard_quad pattern
                        state.set("arrangement.defaultPattern", "standard_quad");
                        Logging.GUI.topic("shape").info("Shader effect enabled: switching to standard_quad pattern");
                    } else {
                        // Both OFF → use filled_1 pattern
                        state.set("arrangement.defaultPattern", "filled_1");
                        Logging.GUI.topic("shape").info("Shader effects disabled: switching to filled_1 pattern");
                    }
                }
            })
        );
        
        checkboxes.put(stateKey, checkbox);
        return checkbox;
    }
    
    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> CyclingButtonWidget<E> buildEnumDropdown(
            ShapeWidgetSpec.EnumDropdownSpec<?> spec, int x, int y, int width) {
        
        // Cast to typed version
        ShapeWidgetSpec.EnumDropdownSpec<E> typed = (ShapeWidgetSpec.EnumDropdownSpec<E>) spec;
        
        E initial;
        try {
            String storedValue = state.getString(spec.stateKey());
            initial = Enum.valueOf(typed.enumClass(), storedValue);
        } catch (IllegalArgumentException | NullPointerException e) {
            initial = typed.defaultValue();
        }
        
        String stateKey = spec.stateKey();
        CyclingButtonWidget<E> dropdown = GuiWidgets.enumDropdown(
            x, y, width, GuiConstants.COMPACT_HEIGHT,
            spec.label(), typed.enumClass(), initial,
            "Select " + spec.label().toLowerCase(),
            v -> onChangeWrapper.accept(() -> state.set(stateKey, v.name()))
        );
        
        dropdowns.put(stateKey, dropdown);
        return dropdown;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean getHalfWidth(Object spec) {
        if (spec instanceof ShapeWidgetSpec.SliderSpec s) {
            return s.halfWidth();
        } else if (spec instanceof ShapeWidgetSpec.CheckboxSpec c) {
            return c.halfWidth();
        } else if (spec instanceof ShapeWidgetSpec.EnumDropdownSpec<?> e) {
            return e.halfWidth();
        }
        return false;  // Section headers, row breaks are not half-width
    }
    
    /**
     * Builds a section header label.
     * Uses an inactive button styled as a header for visual consistency.
     */
    private ClickableWidget buildSectionHeader(ShapeWidgetSpec.SectionHeader spec, int x, int y, int width) {
        // Create a simple button that acts as a label (non-interactive)
        // Using "── Title ──" format for visual clarity
        String title = "── " + spec.text() + " ──";
        
        return ButtonWidget.builder(Text.literal(title), button -> {})
            .dimensions(x, y, width, GuiConstants.COMPACT_HEIGHT)
            .build();
    }
}
