package net.cyberpunk042.client.command.util;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.validation.Range;
import net.cyberpunk042.visual.validation.ValueRange;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Locale;
import net.cyberpunk042.log.LogScope;
import net.cyberpunk042.log.LogLevel;

/**
 * Scans record classes for @Range annotations and auto-generates FieldEditKnob commands.
 * 
 * <p>Uses reflection to extract metadata from record components:</p>
 * <ul>
 *   <li>Field name → command name and state path</li>
 *   <li>@Range annotation → min/max/unit from ValueRange</li>
 *   <li>DEFAULT constant → default value</li>
 *   <li>Field type → knob type (float, int, boolean, enum)</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * var ring = ClientCommandManager.literal("ring");
 * CommandScanner.scanRecord(RingShape.class, "ring", ring);
 * // Auto-generates: innerRadius, outerRadius, segments, y, height, twist
 * </pre>
 * 
 * @see FieldEditKnob
 * @see Range
 */
public final class CommandScanner {
    
    private CommandScanner() {}
    
    /**
     * Scans a record class and generates FieldEditKnob commands for all
     * @Range-annotated components.
     * 
     * @param recordClass The record class to scan (e.g., RingShape.class)
     * @param pathPrefix  The state path prefix (e.g., "ring")
     * @param parent      The command node to attach generated commands to
     * @return The number of commands generated
     */
    public static int scanRecord(
            Class<? extends Record> recordClass,
            String pathPrefix,
            LiteralArgumentBuilder<FabricClientCommandSource> parent) {
        
        int count = 0;
        Object defaultInstance = getDefaultInstance(recordClass);
        
        try (LogScope scope = Logging.GUI.topic("scanner").scope("process-getRecordComponents", LogLevel.DEBUG)) {
            for (RecordComponent component : recordClass.getRecordComponents()) {
                Range range = component.getAnnotation(Range.class);
                if (range == null) {
                    // Also check for @Range on the accessor method
                    try {
                        Method accessor = recordClass.getMethod(component.getName());
                        range = accessor.getAnnotation(Range.class);
                    } catch (NoSuchMethodException ignored) {}
                }

                if (range == null) {
                    scope.branch("component").kv("name", component.getName()).kv("_s", "skip");
                    continue;
                }

                String fieldName = component.getName();
                String path = pathPrefix + "." + fieldName;
                ValueRange vr = range.value();
                Class<?> type = component.getType();

                // Get default value
                Object defaultValue = getComponentValue(recordClass, defaultInstance, fieldName);

                // Generate display name: "innerRadius" → "Inner radius"
                String displayName = humanize(fieldName);

                try {
                    if (type == float.class || type == Float.class) {
                        float defVal = defaultValue != null ? ((Number) defaultValue).floatValue() : 0f;
                        FieldEditKnob.floatValue(path, displayName)
                            .range(vr.min(), vr.max())
                            .unit(vr.unit())
                            .defaultValue(defVal)
                            .attach(parent);
                        count++;

                    } else if (type == int.class || type == Integer.class) {
                        int defVal = defaultValue != null ? ((Number) defaultValue).intValue() : 0;
                        FieldEditKnob.intValue(path, displayName)
                            .range((int) vr.min(), (int) vr.max())
                            .defaultValue(defVal)
                            .attach(parent);
                        count++;

                    } else if (type == boolean.class || type == Boolean.class) {
                        boolean defVal = defaultValue != null && (Boolean) defaultValue;
                        FieldEditKnob.toggle(path, displayName)
                            .defaultValue(defVal)
                            .attach(parent);
                        count++;

                    } else if (type.isEnum()) {
                        @SuppressWarnings("unchecked")
                        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) type;
                        scanEnum(path, displayName, enumClass, defaultValue, parent);
                        count++;

                    } else {
                        scope.branch("component").kv("fieldName", fieldName).kv("type", type.getSimpleName()).kv("_s", "skip");
                    }
                } catch (Exception e) {
                    Logging.GUI.topic("scanner").error("Failed to generate command for {}: {}", 
                        path, e.getMessage());
                }
            }
        }
        
        Logging.GUI.topic("scanner").debug("Generated {} commands for {}", 
            count, recordClass.getSimpleName());
        return count;
    }
    
    /**
     * Scans a record class for boolean fields and generates toggle commands.
     * Useful for config records without @Range annotations.
     */
    public static int scanBooleans(
            Class<? extends Record> recordClass,
            String pathPrefix,
            LiteralArgumentBuilder<FabricClientCommandSource> parent) {
        
        int count = 0;
        Object defaultInstance = getDefaultInstance(recordClass);
        
        for (RecordComponent component : recordClass.getRecordComponents()) {
            Class<?> type = component.getType();
            if (type != boolean.class && type != Boolean.class) continue;
            
            String fieldName = component.getName();
            String path = pathPrefix + "." + fieldName;
            String displayName = humanize(fieldName);
            
            Object defaultValue = getComponentValue(recordClass, defaultInstance, fieldName);
            boolean defVal = defaultValue != null && (Boolean) defaultValue;
            
            FieldEditKnob.toggle(path, displayName)
                .defaultValue(defVal)
                .attach(parent);
            count++;
        }
        
        return count;
    }
    
    /**
     * Generates an enum selection command.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void scanEnum(
            String path,
            String displayName,
            Class<? extends Enum<?>> enumClass,
            Object defaultValue,
            LiteralArgumentBuilder<FabricClientCommandSource> parent) {
        
        // Check if enum has id() method (common pattern)
        boolean hasIdMethod = false;
        try {
            enumClass.getMethod("id");
            hasIdMethod = true;
        } catch (NoSuchMethodException ignored) {}
        
        // Check if enum has fromId() method
        boolean hasFromIdMethod = false;
        try {
            enumClass.getMethod("fromId", String.class);
            hasFromIdMethod = true;
        } catch (NoSuchMethodException ignored) {}
        
        var builder = FieldEditKnob.enumValue(path, displayName, (Class) enumClass);
        
        if (hasIdMethod && hasFromIdMethod) {
            builder.idMapper(e -> {
                try {
                    return (String) e.getClass().getMethod("id").invoke(e);
                } catch (Exception ex) {
                    return ((Enum<?>) e).name().toLowerCase();
                }
            });
            builder.parser(id -> {
                try {
                    return (Enum) enumClass.getMethod("fromId", String.class).invoke(null, id);
                } catch (Exception ex) {
                    return null;
                }
            });
        }
        
        if (defaultValue != null) {
            builder.defaultValue((Enum) defaultValue);
        }
        
        builder.attach(parent);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets the DEFAULT static field from a record class.
     */
    private static Object getDefaultInstance(Class<?> recordClass) {
        try {
            Field defaultField = recordClass.getField("DEFAULT");
            return defaultField.get(null);
        } catch (Exception e) {
            Logging.GUI.topic("scanner").trace("No DEFAULT field in {}", recordClass.getSimpleName());
            return null;
        }
    }
    
    /**
     * Gets a component value from a record instance using the accessor method.
     */
    private static Object getComponentValue(Class<?> recordClass, Object instance, String componentName) {
        if (instance == null) return null;
        try {
            Method accessor = recordClass.getMethod(componentName);
            return accessor.invoke(instance);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Converts camelCase to "Human readable" format.
     * Examples:
     *   "innerRadius" → "Inner radius"
     *   "latSteps" → "Lat steps"
     *   "capTop" → "Cap top"
     */
    public static String humanize(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return camelCase;
        
        // Insert space before uppercase letters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append(' ');
            }
            sb.append(i == 0 ? Character.toUpperCase(c) : Character.toLowerCase(c));
        }
        return sb.toString();
    }
    
    /**
     * Converts a class name to a command-friendly lowercase name.
     * Examples:
     *   "RingShape" → "ring"
     *   "SphereShape" → "sphere"
     *   "FillConfig" → "fill"
     */
    public static String toCommandName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        // Remove common suffixes
        for (String suffix : new String[]{"Shape", "Config", "State"}) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        return name.toLowerCase(Locale.ROOT);
    }
}

