package net.cyberpunk042.client.gui.widget;

import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * G25-G26: Composite widget for editing Vec3 values (x, y, z).
 * Three TextFieldWidgets with labels and linked value updates.
 */
public class Vec3Editor {
    
    private static final int FIELD_WIDTH = 50;
    private static final int FIELD_GAP = 4;
    
    private final String label;
    private final TextFieldWidget fieldX;
    private final TextFieldWidget fieldY;
    private final TextFieldWidget fieldZ;
    private final Consumer<Vector3f> onChange;
    
    private int x, y;
    private Vector3f currentValue;
    
    /**
     * Creates a Vec3 editor.
     * @param textRenderer Font renderer
     * @param x X position
     * @param y Y position
     * @param label Label text
     * @param initial Initial value
     * @param onChange Callback when value changes
     */
    public Vec3Editor(TextRenderer textRenderer, int x, int y, 
                      String label, Vector3f initial, Consumer<Vector3f> onChange) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.currentValue = new Vector3f(initial);
        this.onChange = onChange;
        
        int fieldY = y;
        int fx = x + 20; // After "X:" label
        
        this.fieldX = createField(textRenderer, fx, fieldY, initial.x, "X");
        this.fieldY = createField(textRenderer, fx + FIELD_WIDTH + FIELD_GAP + 20, fieldY, initial.y, "Y");
        this.fieldZ = createField(textRenderer, fx + (FIELD_WIDTH + FIELD_GAP + 20) * 2, fieldY, initial.z, "Z");
        
        Logging.GUI.topic("widget").trace("Vec3Editor created: {}", label);
    }
    
    private TextFieldWidget createField(TextRenderer textRenderer, int x, int y, float value, String axis) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, FIELD_WIDTH, GuiConstants.WIDGET_HEIGHT, Text.literal(axis));
        field.setText(String.format("%.2f", value));
        field.setChangedListener(text -> onFieldChanged(axis, text));
        return field;
    }
    
    private void onFieldChanged(String axis, String text) {
        try {
            float val = Float.parseFloat(text);
            switch (axis) {
                case "X" -> currentValue.x = val;
                case "Y" -> currentValue.y = val;
                case "Z" -> currentValue.z = val;
            }
            if (onChange != null) {
                onChange.accept(new Vector3f(currentValue));
            }
            Logging.GUI.topic("widget").trace("Vec3Editor {} axis {} -> {}", label, axis, val);
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }
    
    /**
     * Gets the current Vec3 value.
     */
    public Vector3f getValue() {
        return new Vector3f(currentValue);
    }
    
    /**
     * Sets the value programmatically.
     */
    public void setValue(Vector3f value) {
        this.currentValue.set(value);
        fieldX.setText(String.format("%.2f", value.x));
        fieldY.setText(String.format("%.2f", value.y));
        fieldZ.setText(String.format("%.2f", value.z));
    }
    
    /**
     * Renders the editor.
     */
    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        // Label
        context.drawText(textRenderer, label, x, y + 5, GuiConstants.TEXT_PRIMARY, false);
        
        // Axis labels
        int fx = x + 20;
        context.drawText(textRenderer, "X:", fx - 15, y + 5, GuiConstants.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "Y:", fx + FIELD_WIDTH + FIELD_GAP + 5, y + 5, GuiConstants.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "Z:", fx + (FIELD_WIDTH + FIELD_GAP + 20) * 2 - 15, y + 5, GuiConstants.TEXT_SECONDARY, false);
        
        // Fields
        fieldX.render(context, mouseX, mouseY, delta);
        fieldY.render(context, mouseX, mouseY, delta);
        fieldZ.render(context, mouseX, mouseY, delta);
    }
    
    public TextFieldWidget getFieldX() { return fieldX; }
    public TextFieldWidget getFieldY() { return fieldY; }
    public TextFieldWidget getFieldZ() { return fieldZ; }
    
    public int getHeight() { return GuiConstants.WIDGET_HEIGHT; }
    public int getWidth() { return 20 + (FIELD_WIDTH + FIELD_GAP + 20) * 3; }
}
