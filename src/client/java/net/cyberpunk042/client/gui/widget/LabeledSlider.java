package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * G21-G24: Slider with label, min/max range mapping, format string, and optional snap.
 */
public class LabeledSlider extends SliderWidget {
    
    private final String label;
    private final float minValue;
    private final float maxValue;
    private final String format;
    private final Float step;
    private final Consumer<Float> onChange;
    
    /**
     * Creates a labeled slider.
     * @param x X position
     * @param y Y position
     * @param width Widget width
     * @param height Widget height
     * @param label Display label
     * @param minValue Minimum value
     * @param maxValue Maximum value
     * @param initialValue Starting value
     * @param format Format string (e.g., "%.2f" or "%d")
     * @param step Optional snap step, null for continuous
     * @param onChange Callback when value changes
     */
    public LabeledSlider(int x, int y, int width, int height, String label, 
                         float minValue, float maxValue, float initialValue,
                         String format, Float step, Consumer<Float> onChange) {
        super(x, y, width, height, 
              Text.literal(label + ": " + formatValue(format, initialValue)),
              normalizeValue(initialValue, minValue, maxValue));
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.format = format;
        this.step = step;
        this.onChange = onChange;
        
        Logging.GUI.topic("widget").trace(
            "LabeledSlider created: {} [{}-{}]", label, minValue, maxValue);
    }
    
    /**
     * Creates a labeled slider with responsive height.
     */
    public LabeledSlider(int x, int y, int width, String label, 
                         float minValue, float maxValue, float initialValue,
                         String format, Float step, Consumer<Float> onChange) {
        this(x, y, width, GuiConstants.widgetHeight(), label, minValue, maxValue, initialValue, format, step, onChange);
    }
    
    private static double normalizeValue(float value, float min, float max) {
        // Handle edge cases
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0;
        }
        if (max <= min) {
            return 0.0;
        }
        // Normalize and clamp to [0, 1]
        double normalized = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, normalized));
    }
    
    /**
     * Formats value handling both %d (int) and %f (float) format strings.
     */
    private static String formatValue(String format, float value) {
        if (format.contains("d")) {
            return String.format(format, Math.round(value));
        }
        return String.format(format, value);
    }
    
    /**
     * Gets the current value in the min-max range.
     */
    public float getValue() {
        float raw = (float) (minValue + value * (maxValue - minValue));
        if (step != null && step > 0) {
            raw = Math.round(raw / step) * step;
        }
        return raw;
    }
    
    /**
     * Sets the value programmatically.
     */
    public void setValue(float newValue) {
        this.value = normalizeValue(newValue, minValue, maxValue);
        updateMessage();
    }
    
    @Override
    protected void updateMessage() {
        float val = getValue();
        setMessage(Text.literal(label + ": " + formatValue(format, val)));
    }
    
    @Override
    protected void applyValue() {
        float val = getValue();
        Logging.GUI.topic("widget").trace("LabeledSlider {} -> {}", label, val);
        if (onChange != null) {
            onChange.accept(val);
        }
    }
    
    // Builder pattern for convenience
    public static Builder builder(String label) {
        return new Builder(label);
    }
    
    public static class Builder {
        private final String label;
        private int x, y, width = GuiConstants.SLIDER_WIDTH;
        private Integer height = null;  // null = use dynamic height
        private float min = 0f, max = 1f, initial = 0.5f;
        private String format = "%.2f";
        private Float step = null;
        private Consumer<Float> onChange = v -> {};
        
        public Builder(String label) { this.label = label; }
        public Builder position(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder width(int w) { this.width = w; return this; }
        public Builder height(int h) { this.height = h; return this; }
        /** Use compact height for dense layouts. */
        public Builder compact() { this.height = GuiConstants.COMPACT_HEIGHT; return this; }
        public Builder range(float min, float max) { this.min = min; this.max = max; return this; }
        public Builder initial(float v) { this.initial = v; return this; }
        public Builder format(String f) { this.format = f; return this; }
        public Builder step(float s) { this.step = s; return this; }
        public Builder onChange(Consumer<Float> c) { this.onChange = c; return this; }
        
        public LabeledSlider build() {
            int h = height != null ? height : GuiConstants.widgetHeight();
            return new LabeledSlider(x, y, width, h, label, min, max, initial, format, step, onChange);
        }
    }
}
