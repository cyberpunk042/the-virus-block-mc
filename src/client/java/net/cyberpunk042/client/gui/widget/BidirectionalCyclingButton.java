package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Cycling button that supports right-click to cycle backward.
 * Left-click cycles forward, right-click cycles backward.
 * 
 * <p>This is a custom implementation that provides more control than
 * Minecraft's CyclingButtonWidget, specifically for bidirectional cycling.</p>
 * 
 * @param <T> The type of value being cycled
 */
public class BidirectionalCyclingButton<T> extends ButtonWidget {
    
    private final List<T> values;
    private int currentIndex;
    private final Consumer<T> onChange;
    private final Function<T, String> formatter;
    private final String label;
    
    private BidirectionalCyclingButton(
            int x, int y, int width, int height,
            String label,
            List<T> values, int initialIndex,
            Function<T, String> formatter,
            Consumer<T> onChange) {
        super(x, y, width, height, 
              Text.literal(formatMessage(label, values.get(initialIndex), formatter)),
              btn -> ((BidirectionalCyclingButton<?>) btn).cycleForward(),
              DEFAULT_NARRATION_SUPPLIER);
        
        this.values = values;
        this.currentIndex = initialIndex;
        this.onChange = onChange;
        this.formatter = formatter;
        this.label = label;
    }
    
    private static <T> String formatMessage(String label, T value, Function<T, String> formatter) {
        if (label == null || label.isEmpty()) {
            return formatter.apply(value);
        }
        return label + ": " + formatter.apply(value);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            if (button == 1) { // Right-click: cycle backward
                this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                cycleBackward();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void cycleForward() {
        if (values.isEmpty()) return;
        
        currentIndex = (currentIndex + 1) % values.size();
        updateValue();
        
        Logging.GUI.topic("widget").trace("BidirectionalCyclingButton cycled forward to index {}: {}", 
            currentIndex, getValue());
    }
    
    private void cycleBackward() {
        if (values.isEmpty()) return;
        
        currentIndex = currentIndex <= 0 ? values.size() - 1 : currentIndex - 1;
        updateValue();
        
        Logging.GUI.topic("widget").trace("BidirectionalCyclingButton cycled backward to index {}: {}", 
            currentIndex, getValue());
    }
    
    private void updateValue() {
        T newValue = values.get(currentIndex);
        setMessage(Text.literal(formatMessage(label, newValue, formatter)));
        
        if (onChange != null) {
            onChange.accept(newValue);
        }
    }
    
    /**
     * Gets the current value.
     */
    public T getValue() {
        return values.get(currentIndex);
    }
    
    /**
     * Sets the current value.
     */
    public void setValue(T value) {
        int idx = values.indexOf(value);
        if (idx >= 0) {
            currentIndex = idx;
            setMessage(Text.literal(formatMessage(label, value, formatter)));
        }
    }
    
    /**
     * Builder for creating bidirectional cycling buttons.
     */
    public static class Builder<T> {
        private int x, y, width = GuiConstants.BUTTON_WIDTH, height = GuiConstants.WIDGET_HEIGHT;
        private String label = "";
        private List<T> values;
        private T initialValue;
        private Function<T, String> formatter = Object::toString;
        private Consumer<T> onChange = v -> {};
        
        public Builder<T> position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }
        
        public Builder<T> size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Builder<T> label(String label) {
            this.label = label;
            return this;
        }
        
        public Builder<T> values(List<T> values) {
            this.values = values;
            return this;
        }
        
        @SafeVarargs
        public final Builder<T> values(T... values) {
            this.values = List.of(values);
            return this;
        }
        
        public Builder<T> initial(T value) {
            this.initialValue = value;
            return this;
        }
        
        public Builder<T> formatter(Function<T, String> formatter) {
            this.formatter = formatter;
            return this;
        }
        
        public Builder<T> onChange(Consumer<T> onChange) {
            this.onChange = onChange;
            return this;
        }
        
        public BidirectionalCyclingButton<T> build() {
            if (values == null || values.isEmpty()) {
                throw new IllegalStateException("Values must be provided");
            }
            
            int initialIndex = initialValue != null ? values.indexOf(initialValue) : 0;
            if (initialIndex < 0) initialIndex = 0;
            
            return new BidirectionalCyclingButton<>(
                x, y, width, height,
                label, values, initialIndex,
                formatter, onChange
            );
        }
    }
    
    /**
     * Creates a new builder for a bidirectional cycling button.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    /**
     * Creates a bidirectional enum cycling button.
     */
    public static <E extends Enum<E>> BidirectionalCyclingButton<E> forEnum(
            int x, int y, int width, int height,
            String label, Class<E> enumClass, E initial,
            Consumer<E> onChange) {
        
        return BidirectionalCyclingButton.<E>builder()
            .position(x, y)
            .size(width, height)
            .label(label)
            .values(List.of(enumClass.getEnumConstants()))
            .initial(initial)
            .formatter(e -> e.name().toLowerCase().replace("_", " "))
            .onChange(onChange)
            .build();
    }
    
    /**
     * Creates a bidirectional boolean cycling button (ON/OFF).
     */
    public static BidirectionalCyclingButton<Boolean> forBoolean(
            int x, int y, int width, int height,
            String label, boolean initial,
            Consumer<Boolean> onChange) {
        
        return BidirectionalCyclingButton.<Boolean>builder()
            .position(x, y)
            .size(width, height)
            .label(label)
            .values(Boolean.TRUE, Boolean.FALSE)
            .initial(initial)
            .formatter(val -> val ? "ON" : "OFF")
            .onChange(onChange)
            .build();
    }
}
