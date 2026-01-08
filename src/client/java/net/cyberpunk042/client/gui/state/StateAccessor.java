package net.cyberpunk042.client.gui.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Reflection-based state accessor for FieldEditState.
 * Eliminates repetitive getter/setter boilerplate.
 * 
 * <p>Usage:</p>
 * <pre>
 * // Get by type (when only one field of that type exists)
 * Transform t = StateAccessor.get(state, Transform.class);
 * 
 * // Get by name
 * SphereShape s = StateAccessor.get(state, "sphere", SphereShape.class);
 * 
 * // Set (automatically finds field by type or value's class)
 * StateAccessor.set(state, newTransform);
 * 
 * // Set by name
 * StateAccessor.set(state, "sphere", newSphere);
 * </pre>
 */
public final class StateAccessor {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Cache field metadata per class
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new HashMap<>();
    
    private StateAccessor() {}
    
    /**
     * Get a @StateField by its type.
     * Only works if there's exactly one field of that type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object state, Class<T> type) {
        for (Field field : getStateFields(state.getClass()).values()) {
            if (type.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return (T) field.get(state);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot access field: " + field.getName(), e);
                }
            }
        }
        throw new IllegalArgumentException("No @StateField of type: " + type.getSimpleName());
    }
    
    /**
     * Get a @StateField by name.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Object state, String name, Class<T> type) {
        Field field = getStateFields(state.getClass()).get(name);
        if (field == null) {
            throw new IllegalArgumentException("No @StateField named: " + name);
        }
        try {
            field.setAccessible(true);
            return (T) field.get(state);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + name, e);
        }
    }
    
    /**
     * Set a @StateField by matching the value's type.
     * Returns true if field was found and set.
     */
    public static boolean set(Object state, Object value) {
        if (value == null) return false;
        
        Class<?> valueType = value.getClass();
        for (Field field : getStateFields(state.getClass()).values()) {
            if (field.getType().isAssignableFrom(valueType)) {
                try {
                    field.setAccessible(true);
                    field.set(state, value);
                    return true;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot set field: " + field.getName(), e);
                }
            }
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED GETTERS (consistent with setters)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get a value by path using dot notation.
     * 
     * <p>Examples:</p>
     * <pre>
     * get(state, "radius");              // Direct field
     * get(state, "sphere.latSteps");     // Record property
     * get(state, "beam.pulse.speed");    // Nested record property
     * </pre>
     */
    public static Object get(Object state, String path) {
        if (path.contains(".")) {
            return getNestedProperty(state, path);
        } else {
            return getDirectField(state, path);
        }
    }
    
    /**
     * Get an int value by path.
     */
    public static int getInt(Object state, String path) {
        Object value = get(state, path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException("Field '" + path + "' is not a number: " + value);
    }
    
    /**
     * Get a float value by path.
     */
    public static float getFloat(Object state, String path) {
        Object value = get(state, path);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        throw new IllegalArgumentException("Field '" + path + "' is not a number: " + value);
    }
    
    /**
     * Get a boolean value by path.
     */
    public static boolean getBool(Object state, String path) {
        Object value = get(state, path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new IllegalArgumentException("Field '" + path + "' is not a boolean: " + value);
    }
    
    /**
     * Get a String value by path.
     */
    public static String getString(Object state, String path) {
        Object value = get(state, path);
        if (value == null) return null;
        if (value instanceof String) {
            return (String) value;
        }
        // Handle enums - return name
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        return value.toString();
    }
    
    /**
     * Get a typed value by path.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getTyped(Object state, String path, Class<T> type) {
        Object value = get(state, path);
        if (value == null) return null;
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new IllegalArgumentException("Field '" + path + "' is not of type " + type.getSimpleName() + ": " + value);
    }
    
    private static Object getDirectField(Object state, String name) {
        Field field = getStateFields(state.getClass()).get(name);
        if (field == null) {
            throw new IllegalArgumentException("No @StateField named: " + name);
        }
        try {
            field.setAccessible(true);
            return field.get(state);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + name, e);
        }
    }
    
    private static Object getNestedProperty(Object state, String path) {
        String[] parts = path.split("\\.", 2);
        String fieldName = parts[0];
        String remaining = parts[1];
        
        // Get the record value
        Field field = getStateFields(state.getClass()).get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("No @StateField named: " + fieldName);
        }
        
        try {
            field.setAccessible(true);
            Object record = field.get(state);
            if (record == null) {
                return null;
            }
            
            // Navigate the remaining path
            return getRecordProperty(record, remaining);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + fieldName, e);
        }
    }
    
    private static Object getRecordProperty(Object record, String path) {
        try {
            if (path.contains(".")) {
                // Nested: "pulse.speed" -> get pulse, then get speed
                String[] parts = path.split("\\.", 2);
                String propertyName = parts[0];
                String remaining = parts[1];
                
                var getter = record.getClass().getMethod(propertyName);
                Object nestedRecord = getter.invoke(record);
                if (nestedRecord == null) {
                    return null;
                }
                return getRecordProperty(nestedRecord, remaining);
            } else {
                // Direct property
                var getter = record.getClass().getMethod(path);
                return getter.invoke(record);
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No property '" + path + "' on " + record.getClass().getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException("Cannot get property: " + path, e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED SETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Set a @StateField by name, or a nested property using dot notation.
     * 
     * <p>Examples:</p>
     * <pre>
     * set(state, "radius", 5.0f);           // Direct field
     * set(state, "spin.speed", 0.5f);       // Record property (uses toBuilder)
     * set(state, "beam.pulse.speed", 1.0f); // Nested record property
     * </pre>
     */
    public static void set(Object state, String path, Object value) {
        if (path.contains(".")) {
            setNestedProperty(state, path, value);
        } else {
            setDirectField(state, path, value);
        }
    }
    
    private static void setDirectField(Object state, String name, Object value) {
        Field field = getStateFields(state.getClass()).get(name);
        if (field == null) {
            throw new IllegalArgumentException("No @StateField named: " + name);
        }
        try {
            field.setAccessible(true);
            field.set(state, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot set field: " + name, e);
        }
    }
    
    /**
     * Set a nested property using dot notation.
     * Handles toBuilder()/build() chain automatically via reflection.
     */
    private static void setNestedProperty(Object state, String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String fieldName = parts[0];
        String remaining = parts[1];
        
        // Get the current record value
        Field field = getStateFields(state.getClass()).get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("No @StateField named: " + fieldName);
        }
        
        try {
            field.setAccessible(true);
            Object record = field.get(state);
            
            // Update the record (handles nested paths recursively)
            Object updated = updateRecordProperty(record, remaining, value);
            
            // DEBUG: Log the update for fill-related changes
            if (fieldName.equals("fill")) {
                Logging.GUI.topic("accessor").debug("[FILL-DEBUG] StateAccessor updating '{}' from {} to {}", 
                    path, record, updated);
            }
            
            // Set the updated record back
            field.set(state, updated);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set nested property: " + path, e);
        }
    }
    
    /**
     * Update a property on a record using toBuilder pattern.
     * Supports nested paths like "pulse.speed" for records containing other records.
     */
    private static Object updateRecordProperty(Object record, String path, Object value) {
        try {
            // Special case: KamehamehaShape.combinedProgress uses withCombinedProgress() factory
            if (record instanceof net.cyberpunk042.visual.shape.KamehamehaShape kamehameha
                    && path.equals("combinedProgress")) {
                float combined = (value instanceof Number num) ? num.floatValue() : 0f;
                return kamehameha.withCombinedProgress(combined);
            }
            
            // Get the builder
            var toBuilderMethod = record.getClass().getMethod("toBuilder");
            Object builder = toBuilderMethod.invoke(record);
            
            if (path.contains(".")) {
                // Nested property: "pulse.speed" -> update pulse record, then set on builder
                String[] parts = path.split("\\.", 2);
                String propertyName = parts[0];
                String remaining = parts[1];
                
                // Get current nested record from the outer record
                var getter = record.getClass().getMethod(propertyName);
                Object nestedRecord = getter.invoke(record);
                
                // If nested record is null, try to create a default instance
                if (nestedRecord == null) {
                    Class<?> nestedType = getter.getReturnType();
                    nestedRecord = createDefaultRecord(nestedType);
                    if (nestedRecord == null) {
                        throw new RuntimeException("Cannot create default for null nested record: " + propertyName);
                    }
                }
                
                // Recursively update the nested record
                Object updatedNested = updateRecordProperty(nestedRecord, remaining, value);
                
                // Set updated nested record on builder
                var builderSetter = findBuilderMethod(builder.getClass(), propertyName);
                builderSetter.invoke(builder, updatedNested);
            } else {
                // Direct property: find and call builder method
                var builderSetter = findBuilderMethod(builder.getClass(), path);
                Object convertedValue = convertToParameterType(value, builderSetter.getParameterTypes()[0]);
                builderSetter.invoke(builder, convertedValue);
            }
            
            // Build and return
            var buildMethod = builder.getClass().getMethod("build");
            return buildMethod.invoke(builder);
        } catch (Exception e) {
            throw new RuntimeException("Cannot update record property: " + path + " on " + record.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Creates a default instance of a record type if it has a NONE/DEFAULT/IDENTITY constant.
     */
    private static Object createDefaultRecord(Class<?> recordType) {
        // Try common default constant names
        String[] defaultNames = {"NONE", "DEFAULT", "IDENTITY", "EMPTY"};
        for (String name : defaultNames) {
            try {
                var field = recordType.getField(name);
                return field.get(null);
            } catch (Exception ignored) {}
        }
        // Try builder pattern with build()
        try {
            var builderMethod = recordType.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            var buildMethod = builder.getClass().getMethod("build");
            return buildMethod.invoke(builder);
        } catch (Exception ignored) {}
        return null;
    }
    
    /**
     * Find a builder method by name (handles primitive type matching).
     */
    private static java.lang.reflect.Method findBuilderMethod(Class<?> builderClass, String name) {
        for (var method : builderClass.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new IllegalArgumentException("No builder method: " + name + " on " + builderClass.getSimpleName());
    }
    
    /**
     * Convert a value to the expected parameter type (handles String-to-Enum, primitive boxing, etc.).
     */
    @SuppressWarnings("unchecked")
    private static Object convertToParameterType(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        // Already correct type
        if (targetType.isInstance(value)) return value;
        
        // String to Enum conversion
        if (targetType.isEnum() && value instanceof String strValue) {
            // First try: use fromId() method if available (many enums have this for flexible parsing)
            try {
                var fromIdMethod = targetType.getMethod("fromId", String.class);
                return fromIdMethod.invoke(null, strValue);
            } catch (NoSuchMethodException ignored) {
                // No fromId method, fall through to standard conversion
            } catch (Exception e) {
                Logging.GUI.topic("state").warn("fromId() failed for {}: {}", targetType.getSimpleName(), e.getMessage());
            }
            
            // Second try: exact match
            try {
                return Enum.valueOf((Class<Enum>) targetType, strValue);
            } catch (IllegalArgumentException e) {
                // Third try: case-insensitive match
                for (Object constant : targetType.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(strValue)) {
                        return constant;
                    }
                }
                // Fourth try: normalized match (remove underscores, spaces, dashes)
                String normalized = strValue.toLowerCase().replaceAll("[_\\-\\s]", "");
                for (Object constant : targetType.getEnumConstants()) {
                    String enumNormalized = ((Enum<?>) constant).name().toLowerCase().replaceAll("[_\\-\\s]", "");
                    if (enumNormalized.equals(normalized)) {
                        return constant;
                    }
                }
                throw e;
            }
        }
        
        // Enum to String conversion (less common)
        if (targetType == String.class && value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        
        // Number conversions
        if (value instanceof Number numValue) {
            if (targetType == int.class || targetType == Integer.class) return numValue.intValue();
            if (targetType == float.class || targetType == Float.class) return numValue.floatValue();
            if (targetType == double.class || targetType == Double.class) return numValue.doubleValue();
            if (targetType == long.class || targetType == Long.class) return numValue.longValue();
        }
        
        // Boolean from String
        if ((targetType == boolean.class || targetType == Boolean.class) && value instanceof String strValue) {
            return Boolean.parseBoolean(strValue);
        }
        
        return value;
    }
    
    /**
     * Serialize all @StateField fields to JSON.
     */
    public static JsonObject toJson(Object state) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Field> entry : getStateFields(state.getClass()).entrySet()) {
            try {
                entry.getValue().setAccessible(true);
                Object value = entry.getValue().get(state);
                if (value != null) {
                    // Use Gson to convert the value to JsonElement
                    json.add(entry.getKey(), GSON.toJsonTree(value));
                }
            } catch (IllegalAccessException e) {
                Logging.GUI.topic("state").warn("Cannot serialize field: {}", entry.getKey());
            }
        }
        return json;
    }
    
    /**
     * Deserialize JSON into @StateField fields.
     */
    public static void fromJson(Object state, JsonObject json) {
        for (Map.Entry<String, Field> entry : getStateFields(state.getClass()).entrySet()) {
            String name = entry.getKey();
            Field field = entry.getValue();
            
            if (json.has(name)) {
                try {
                    field.setAccessible(true);
                    Object value = GSON.fromJson(json.get(name), field.getType());
                    field.set(state, value);
                } catch (Exception e) {
                    Logging.GUI.topic("state").warn("Cannot deserialize field {}: {}", name, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Apply a modifier function to a field and set the result.
     * Useful for immutable records with toBuilder().
     */
    @SuppressWarnings("unchecked")
    public static <T> void update(Object state, String name, java.util.function.Function<T, T> modifier) {
        T current = get(state, name, (Class<T>) Object.class);
        T updated = modifier.apply(current);
        set(state, name, updated);
    }
    
    /**
     * Get all @StateField fields for a class (cached).
     */
    private static Map<String, Field> getStateFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Map<String, Field> fields = new HashMap<>();
            for (Field field : c.getDeclaredFields()) {
                StateField annotation = field.getAnnotation(StateField.class);
                if (annotation != null) {
                    String name = annotation.name().isEmpty() ? field.getName() : annotation.name();
                    fields.put(name, field);
                }
            }
            return fields;
        });
    }
    
    /**
     * Get all field names.
     */
    public static Iterable<String> getFieldNames(Object state) {
        return getStateFields(state.getClass()).keySet();
    }
}

