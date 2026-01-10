package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Factory for creating GUI widgets with consistent styling.
 * 
 * <h2>Widget Categories</h2>
 * <ul>
 *   <li><b>Buttons:</b> button(), toggle(), checkbox()</li>
 *   <li><b>Input:</b> slider(), sliderInt(), editBox()</li>
 *   <li><b>Dropdowns:</b> enumDropdown()</li>
 *   <li><b>Layout:</b> grid(), horizontalLayout(), verticalLayout()</li>
 *   <li><b>Display:</b> icon(), label()</li>
 *   <li><b>Visibility:</b> visibleWhen()</li>
 * </ul>
 * 
 * All widgets use {@link GuiConstants} for consistent theming.
 */
public final class GuiWidgets {
    
    private GuiWidgets() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISIBILITY HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Wraps a widget with a visibility condition.
     * 
     * <p>The widget will be registered with {@link WidgetVisibility} and its
     * visibility will be automatically updated when {@link WidgetVisibility#refreshAll()}
     * is called.</p>
     * 
     * <h3>Usage</h3>
     * <pre>
     * var slider = GuiWidgets.visibleWhen(
     *     GuiWidgets.slider("Wave Amplitude", ...),
     *     () -> RendererCapabilities.isStandardMode()
     * );
     * </pre>
     * 
     * @param <W> Widget type
     * @param widget The widget to wrap
     * @param condition Visibility predicate
     * @return The same widget (for chaining)
     */
    public static <W extends ClickableWidget> W visibleWhen(W widget, BooleanSupplier condition) {
        WidgetVisibility.register(widget, condition);
        return widget;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a styled button.
     */
    public static ButtonWidget button(int x, int y, int width, String label, String tooltip, Runnable onClick) {
        Logging.GUI.topic("widget").trace("Creating button: {}", label);
        
        return ButtonWidget.builder(Text.literal(label), btn -> {
            Logging.GUI.topic("button").trace("Clicked: {}", label);
            onClick.run();
        })
        .dimensions(x, y, width, GuiConstants.widgetHeight())
        .tooltip(Tooltip.of(Text.literal(tooltip)))
        .build();
    }
    
    /**
     * Create a standard width button.
     */
    public static ButtonWidget button(int x, int y, String label, String tooltip, Runnable onClick) {
        return button(x, y, GuiConstants.BUTTON_WIDTH, label, tooltip, onClick);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TOGGLE (Boolean)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a toggle button (ON/OFF).
     */
    public static CyclingButtonWidget<Boolean> toggle(
            int x, int y, int width,
            String label, boolean initial, String tooltip,
            Consumer<Boolean> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating toggle: {} = {}", label, initial);
        
        // Value formatter shows just ON/OFF, label is added by build() call
        return CyclingButtonWidget.<Boolean>builder(val -> 
                Text.literal(val ? "ON" : "OFF"))
            .values(Boolean.TRUE, Boolean.FALSE)
            .initially(initial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .build(x, y, width, GuiConstants.widgetHeight(), Text.literal(label), (btn, val) -> {
                Logging.GUI.topic("toggle").trace("{} → {}", label, val);
                onChange.accept(val);
            });
    }
    
    /**
     * Create a compact toggle button (ON/OFF) with smaller height.
     * Uses omitKeyText() to prevent the "Label: " prefix from being added.
     */
    public static CyclingButtonWidget<Boolean> compactToggle(
            int x, int y, int width,
            String label, boolean initial, String tooltip,
            Consumer<Boolean> onChange) {

        return CyclingButtonWidget.<Boolean>builder(val -> 
                Text.literal(label + ": " + (val ? "ON" : "OFF")))
            .values(Boolean.TRUE, Boolean.FALSE)
            .initially(initial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .omitKeyText()
            .build(x, y, width, GuiConstants.COMPACT_HEIGHT, Text.literal(""), (btn, val) -> {
                onChange.accept(val);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CHECKBOX (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a checkbox with callback.
     */
    public static CheckboxWidget checkbox(
            int x, int y, 
            String label, boolean initial, String tooltip,
            TextRenderer textRenderer,
            Consumer<Boolean> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating checkbox: {} = {}", label, initial);
        
        return CheckboxWidget.builder(Text.literal(label), textRenderer)
            .pos(x, y)
            .checked(initial)
            .callback((cb, isChecked) -> {
                Logging.GUI.topic("checkbox").trace("{} → {}", label, isChecked);
                onChange.accept(isChecked);
            })
            .tooltip(Tooltip.of(Text.literal(tooltip)))
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUM DROPDOWN
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create an enum dropdown selector.
     */
    public static <E extends Enum<E>> CyclingButtonWidget<E> enumDropdown(
            int x, int y, int width,
            String label, Class<E> enumClass, E initial, String tooltip,
            Consumer<E> onChange) {
        return enumDropdown(x, y, width, GuiConstants.widgetHeight(), label, enumClass, initial, tooltip, onChange);
    }
    
    /**
     * Create an enum dropdown selector with custom height.
     */
    public static <E extends Enum<E>> CyclingButtonWidget<E> enumDropdown(
            int x, int y, int width, int height,
            String label, Class<E> enumClass, E initial, String tooltip,
            Consumer<E> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating enum dropdown: {} = {}", label, initial);
        
        // Value formatter shows just the value, label is added by build() call
        return CyclingButtonWidget.<E>builder(val -> 
                Text.literal(formatEnumName(val)))
            .values(enumClass.getEnumConstants())
            .initially(initial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .build(x, y, width, height, Text.literal(label), (btn, val) -> {
                Logging.GUI.topic("enum").trace("{} → {}", label, val);
                onChange.accept(val);
            });
    }
    
    /**
     * Create a compact enum dropdown (no label prefix, smaller height).
     * Uses omitKeyText() to prevent the "Label: " prefix from being added.
     */
    public static <E extends Enum<E>> CyclingButtonWidget<E> compactEnumDropdown(
            int x, int y, int width,
            Class<E> enumClass, E initial, String tooltip,
            Consumer<E> onChange) {

        return CyclingButtonWidget.<E>builder(val ->
                Text.literal(formatEnumName(val)))
            .values(enumClass.getEnumConstants())
            .initially(initial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .omitKeyText()
            .build(x, y, width, GuiConstants.COMPACT_HEIGHT, Text.literal(""), (btn, val) -> {
                onChange.accept(val);
            });
    }
    
    private static <E extends Enum<E>> String formatEnumName(E val) {
        return val.name().toLowerCase().replace("_", " ");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STRING DROPDOWN
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a simple string dropdown selector.
     */
    public static CyclingButtonWidget<String> stringDropdown(
            int x, int y, int width,
            String label, List<String> values, String initial, String tooltip,
            Consumer<String> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating string dropdown: {} = {}", label, initial);
        
        String safeInitial = (initial != null && values.contains(initial) && !values.isEmpty())
                ? initial
                : (values.isEmpty() ? "" : values.get(0));
        
        // Value formatter shows just the value, label is added by build() call
        return CyclingButtonWidget.<String>builder(val ->
                    Text.literal(val))
            .values(values)
            .initially(safeInitial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .build(x, y, width, GuiConstants.widgetHeight(), Text.literal(label), (btn, val) -> {
                Logging.GUI.topic("dropdown").trace("{} → {}", label, val);
                onChange.accept(val);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LABELED CYCLING BUTTON - THE FOOLPROOF HELPER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a cycling button where the label is INSIDE the button text.
     * 
     * <p><b>USE THIS METHOD</b> when you want buttons like "Face: FIXED" or "Orbit: ON"
     * where the label is part of the displayed text. This method ALWAYS includes
     * omitKeyText() to prevent the ": " prefix bug.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * GuiWidgets.labeledCycler(x, y, width, height,
     *     "Face", Facing.values(), currentFacing, f -> f.name(),
     *     val -> state.set("facing", val.name()));
     * </pre>
     * 
     * <p>This will display: "Face: FIXED", "Face: TOP", etc. with NO extra colon prefix.</p>
     * 
     * @param <T> The value type
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param height Button height
     * @param label The label prefix (e.g., "Face", "Orbit", "Axis")
     * @param values Array of possible values
     * @param initial Initial value
     * @param formatter Function to convert value to display string
     * @param onChange Callback when value changes
     * @return The cycling button widget
     */
    public static <T> CyclingButtonWidget<T> labeledCycler(
            int x, int y, int width, int height,
            String label, T[] values, T initial,
            java.util.function.Function<T, String> formatter,
            Consumer<T> onChange) {
        
        return CyclingButtonWidget.<T>builder(val -> Text.literal(label + ": " + formatter.apply(val)))
            .values(values)
            .initially(initial)
            .omitKeyText()  // <-- THIS IS THE KEY! Always included to prevent ": " prefix
            .build(x, y, width, height, Text.literal(""), (btn, val) -> {
                onChange.accept(val);
            });
    }
    
    /**
     * Creates a labeled cycling button for boolean values (ON/OFF style).
     * 
     * <p>Example: "Orbit: ON" or "Orbit: OFF"</p>
     */
    public static CyclingButtonWidget<Boolean> labeledBoolCycler(
            int x, int y, int width, int height,
            String label, boolean initial, String trueText, String falseText,
            Consumer<Boolean> onChange) {
        
        return CyclingButtonWidget.<Boolean>builder(val -> 
                Text.literal(label + ": " + (val ? trueText : falseText)))
            .values(Boolean.TRUE, Boolean.FALSE)
            .initially(initial)
            .omitKeyText()  // <-- ALWAYS prevents ": " prefix
            .build(x, y, width, height, Text.literal(""), (btn, val) -> {
                onChange.accept(val);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VALUE-ONLY CYCLERS - NO LABEL PREFIX
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a cycling button that shows ONLY the value (no label prefix).
     * 
     * <p>Use this when you want buttons like "SOLID" or "WIREFRAME" without
     * any "Mode: " prefix. Always includes omitKeyText().</p>
     * 
     * <p>Example: Shows "SOLID", "WIREFRAME", "CAGE" - not "Mode: SOLID"</p>
     * 
     * @param <T> The value type
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param height Button height
     * @param values List of possible values
     * @param initial Initial value
     * @param formatter Function to convert value to display string
     * @param onChange Callback when value changes
     * @return The cycling button widget
     */
    public static <T> CyclingButtonWidget<T> valueCycler(
            int x, int y, int width, int height,
            List<T> values, T initial,
            java.util.function.Function<T, String> formatter,
            Consumer<T> onChange) {
        
        return CyclingButtonWidget.<T>builder(val -> Text.literal(formatter.apply(val)))
            .values(values)
            .initially(initial)
            .omitKeyText()  // <-- ALWAYS prevents ": " prefix
            .build(x, y, width, height, Text.literal(""), (btn, val) -> {
                onChange.accept(val);
            });
    }
    
    /**
     * Creates a boolean cycling button showing only the value (no label).
     * 
     * <p>Use this for standalone ON/OFF toggles without a label prefix.</p>
     * 
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param height Button height
     * @param initial Initial value
     * @param trueText Text when true (e.g., "ON", "Enabled", "⚙ Standard")
     * @param falseText Text when false (e.g., "OFF", "Disabled", "⚡ Simplified")
     * @param onChange Callback when value changes
     * @return The cycling button widget
     */
    public static CyclingButtonWidget<Boolean> boolCycler(
            int x, int y, int width, int height,
            boolean initial, String trueText, String falseText,
            Consumer<Boolean> onChange) {
        
        return CyclingButtonWidget.<Boolean>builder(val -> 
                Text.literal(val ? trueText : falseText))
            .values(Boolean.TRUE, Boolean.FALSE)
            .initially(initial)
            .omitKeyText()  // <-- ALWAYS prevents ": " prefix
            .build(x, y, width, height, Text.literal(""), (btn, val) -> {
                onChange.accept(val);
            });
    }
    
    /**
     * Creates an enum cycling button showing only the value (no label).
     * 
     * <p>Use this for standalone enum selectors without a label prefix.
     * The enum value name is formatted (lowercase, spaces instead of underscores).</p>
     * 
     * @param <E> The enum type
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param height Button height
     * @param enumClass The enum class
     * @param initial Initial value
     * @param tooltip Tooltip text
     * @param onChange Callback when value changes
     * @return The cycling button widget
     */
    public static <E extends Enum<E>> CyclingButtonWidget<E> enumCycler(
            int x, int y, int width, int height,
            Class<E> enumClass, E initial, String tooltip,
            Consumer<E> onChange) {
        
        return CyclingButtonWidget.<E>builder(val -> Text.literal(formatEnumName(val)))
            .values(enumClass.getEnumConstants())
            .initially(initial)
            .tooltip(val -> Tooltip.of(Text.literal(tooltip)))
            .omitKeyText()  // <-- ALWAYS prevents ": " prefix
            .build(x, y, width, height, Text.literal(""), (btn, val) -> {
                onChange.accept(val);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SLIDERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a float slider with label.
     */
    public static SliderWidget slider(
            int x, int y, int width,
            String label, float min, float max, float initial, String format, String tooltip,
            Consumer<Float> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating slider: {} = {} [{}, {}]", label, initial, min, max);
        
        double normalized = (initial - min) / (max - min);
        
        return new SliderWidget(x, y, width, GuiConstants.widgetHeight(), 
                Text.literal(label + ": " + formatSliderValue(format, initial)), normalized) {
            
            private final float minVal = min;
            private final float maxVal = max;
            private final String fmt = format;
            private final String lbl = label;
            
            @Override
            protected void updateMessage() {
                float val = (float) (minVal + value * (maxVal - minVal));
                setMessage(Text.literal(lbl + ": " + formatSliderValue(fmt, val)));
            }
            
            @Override
            protected void applyValue() {
                float val = (float) (minVal + value * (maxVal - minVal));
                Logging.GUI.topic("slider").trace("{} → {}", lbl, val);
                onChange.accept(val);
            }
        };
    }
    
    /**
     * Create a float slider with step snapping.
     */
    public static SliderWidget sliderStepped(
            int x, int y, int width,
            String label, float min, float max, float step, float initial, String format, String tooltip,
            Consumer<Float> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating stepped slider: {} = {} [{}, {}] step {}", label, initial, min, max, step);
        
        // Snap initial value to step
        float snappedInitial = Math.round(initial / step) * step;
        double normalized = (snappedInitial - min) / (max - min);
        
        return new SliderWidget(x, y, width, GuiConstants.widgetHeight(), 
                Text.literal(label + ": " + formatSliderValue(format, snappedInitial)), normalized) {
            
            private final float minVal = min;
            private final float maxVal = max;
            private final float stepVal = step;
            private final String fmt = format;
            private final String lbl = label;
            
            @Override
            protected void updateMessage() {
                float raw = (float) (minVal + value * (maxVal - minVal));
                float val = Math.round(raw / stepVal) * stepVal;
                setMessage(Text.literal(lbl + ": " + formatSliderValue(fmt, val)));
            }
            
            @Override
            protected void applyValue() {
                float raw = (float) (minVal + value * (maxVal - minVal));
                float val = Math.round(raw / stepVal) * stepVal;
                Logging.GUI.topic("slider").trace("{} → {} (stepped)", lbl, val);
                onChange.accept(val);
            }
        };
    }
    
    /**
     * Formats slider value, handling both %d (int) and %f (float) format strings.
     */
    private static String formatSliderValue(String format, float value) {
        if (format.contains("d")) {
            return String.format(format, Math.round(value));
        }
        return String.format(format, value);
    }
    
    /**
     * Create an integer slider.
     */
    public static SliderWidget sliderInt(
            int x, int y, int width,
            String label, int min, int max, int initial, String tooltip,
            Consumer<Integer> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating int slider: {} = {} [{}, {}]", label, initial, min, max);
        
        double normalized = (double) (initial - min) / (max - min);
        
        return new SliderWidget(x, y, width, GuiConstants.widgetHeight(),
                Text.literal(label + ": " + initial), normalized) {
            
            private final int minVal = min;
            private final int maxVal = max;
            private final String lbl = label;
            
            @Override
            protected void updateMessage() {
                int val = minVal + (int) Math.round(value * (maxVal - minVal));
                setMessage(Text.literal(lbl + ": " + val));
            }
            
            @Override
            protected void applyValue() {
                int val = minVal + (int) Math.round(value * (maxVal - minVal));
                Logging.GUI.topic("slider").trace("{} → {}", lbl, val);
                onChange.accept(val);
            }
        };
    }
    
    /**
     * Create a compact slider with smaller height for dense layouts.
     * Label is displayed inline with value.
     */
    public static SliderWidget sliderCompact(
            int x, int y, int width,
            String label, float initial, float min, float max,
            String tooltip,
            Consumer<Double> onChange) {
        
        double normalized = (initial - min) / (max - min);
        
        return new SliderWidget(x, y, width, GuiConstants.COMPACT_HEIGHT,
                Text.literal(label), normalized) {
            
            private final float minVal = min;
            private final float maxVal = max;
            private final String lbl = label;
            
            @Override
            protected void updateMessage() {
                float val = (float) (minVal + value * (maxVal - minVal));
                setMessage(Text.literal(String.format("%.1f", val)));
            }
            
            @Override
            protected void applyValue() {
                double val = minVal + value * (maxVal - minVal);
                onChange.accept(val);
            }
        };
    }
    
    /**
     * Alias for compactToggle (consistent naming).
     */
    public static CyclingButtonWidget<Boolean> toggleCompact(
            int x, int y, int width,
            String label, boolean initial, String tooltip,
            Consumer<Boolean> onChange) {
        return compactToggle(x, y, width, label, initial, tooltip, onChange);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EDIT BOX (Multiline Text - NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a multiline text edit box.
     */
    public static EditBoxWidget editBox(
            int x, int y, int width, int height,
            String placeholder, TextRenderer textRenderer,
            Consumer<String> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating edit box");
        
        EditBoxWidget box = EditBoxWidget.builder()
            .placeholder(Text.literal(placeholder))
            .textColor(GuiConstants.TEXT_PRIMARY)
            .cursorColor(GuiConstants.ACCENT)
            .textShadow(false)
            .hasBackground(true)
            .build(textRenderer, width, height, Text.literal(""));
        
        box.setChangeListener(text -> {
            Logging.GUI.topic("editbox").trace("Text changed: {} chars", text.length());
            onChange.accept(text);
        });
        
        return box;
    }
    
    /**
     * Create a single-line text field.
     */
    public static TextFieldWidget textField(
            int x, int y, int width,
            String placeholder, TextRenderer textRenderer,
            Consumer<String> onChange) {
        
        Logging.GUI.topic("widget").trace("Creating text field");
        
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, GuiConstants.widgetHeight(), Text.literal(""));
        field.setPlaceholder(Text.literal(placeholder));
        field.setChangedListener(text -> {
            Logging.GUI.topic("textfield").trace("Text: {}", text);
            onChange.accept(text);
        });
        
        return field;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT - GRID (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a grid layout widget.
     * 
     * @param x X position
     * @param y Y position
     * @param columns Number of columns
     * @param spacing Spacing between cells
     * @return GridWidget.Adder for fluent widget adding
     */
    public static GridWidget.Adder grid(int x, int y, int columns, int spacing) {
        Logging.GUI.topic("layout").trace("Creating grid: {}x cols, spacing {}", columns, spacing);
        
        GridWidget grid = new GridWidget(x, y);
        grid.setSpacing(spacing);
        return grid.createAdder(columns);
    }
    
    /**
     * Create a grid with default spacing.
     */
    public static GridWidget.Adder grid(int x, int y, int columns) {
        return grid(x, y, columns, GuiConstants.PADDING);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT - DIRECTIONAL (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a horizontal layout.
     */
    public static DirectionalLayoutWidget horizontalLayout(int spacing) {
        Logging.GUI.topic("layout").trace("Creating horizontal layout, spacing {}", spacing);
        return DirectionalLayoutWidget.horizontal().spacing(spacing);
    }
    
    /**
     * Create a vertical layout.
     */
    public static DirectionalLayoutWidget verticalLayout(int spacing) {
        Logging.GUI.topic("layout").trace("Creating vertical layout, spacing {}", spacing);
        return DirectionalLayoutWidget.vertical().spacing(spacing);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DISPLAY WIDGETS (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a text label.
     */
    public static TextWidget label(int x, int y, int width, String text, TextRenderer textRenderer) {
        Logging.GUI.topic("widget").trace("Creating label: {}", text);
        return new TextWidget(x, y, width, GuiConstants.widgetHeight(), Text.literal(text), textRenderer);
    }
    
    /**
     * Create a text label with color.
     */
    public static TextWidget label(int x, int y, int width, String text, int color, TextRenderer textRenderer) {
        TextWidget widget = label(x, y, width, text, textRenderer);
        widget.setTextColor(color);
        return widget;
    }
    
    /**
     * Create an icon widget from sprite.
     */
    public static IconWidget icon(int x, int y, int size, net.minecraft.util.Identifier sprite) {
        Logging.GUI.topic("widget").trace("Creating icon: {}", sprite);
        return IconWidget.create(size, size, sprite, size, size);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOADING INDICATOR (NEW!)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a loading widget.
     */
    public static LoadingWidget loading(String label, TextRenderer textRenderer) {
        Logging.GUI.topic("widget").trace("Creating loading: {}", label);
        return new LoadingWidget(textRenderer, Text.literal(label));
    }
}
