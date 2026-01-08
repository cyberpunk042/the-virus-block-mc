package net.cyberpunk042.util.json;

import com.google.gson.*;
import net.cyberpunk042.TheVirusBlock;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized JSON serialization utility using reflection.
 * 
 * <p>Replaces manual field-by-field JSON mapping with automatic reflection-based
 * serialization. Respects {@link JsonField} annotations for customization.</p>
 * 
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // Serialize any object to JSON
 * JsonObject json = JsonSerializer.toJson(myObject);
 * 
 * // Deserialize JSON to new object
 * MyClass obj = JsonSerializer.fromJson(json, MyClass.class);
 * 
 * // Apply JSON to existing object (for mutable state like FieldEditState)
 * JsonSerializer.apply(json, existingObject);
 * }</pre>
 * 
 * <h3>Prefix Mapping (for FragmentRegistry)</h3>
 * <pre>{@code
 * // JSON has {"latSteps": 32}, but field is "sphereLatSteps"
 * JsonSerializer.applyWithGroup(json, state, "sphere");
 * // Maps "latSteps" → sphereLatSteps using @JsonField(group="sphere", aliases={"latSteps"})
 * }</pre>
 * 
 * @see JsonField
 */
public final class JsonSerializer {
    
    /**
     * TypeAdapter for Vector3f - serializes as [x, y, z] array.
     */
    private static final JsonSerializer.Vector3fAdapter VECTOR3F_ADAPTER = new Vector3fAdapter();
    private static final ShapeTypeAdapter SHAPE_ADAPTER = new ShapeTypeAdapter();
    private static final InstantAdapter INSTANT_ADAPTER = new InstantAdapter();
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapter(Vector3f.class, VECTOR3F_ADAPTER)
            .registerTypeAdapter(net.cyberpunk042.visual.shape.Shape.class, SHAPE_ADAPTER)
            .registerTypeAdapter(Instant.class, INSTANT_ADAPTER)
            .create();
    
    private static final Gson GSON_COMPACT = new GsonBuilder()
            .registerTypeAdapter(Vector3f.class, VECTOR3F_ADAPTER)
            .registerTypeAdapter(net.cyberpunk042.visual.shape.Shape.class, SHAPE_ADAPTER)
            .registerTypeAdapter(Instant.class, INSTANT_ADAPTER)
            .create();
    
    /**
     * Gson TypeAdapter for JOML Vector3f.
     * Serializes as [x, y, z] array, deserializes from same format.
     */
    private static class Vector3fAdapter implements com.google.gson.JsonSerializer<Vector3f>, JsonDeserializer<Vector3f> {
        @Override
        public JsonElement serialize(Vector3f src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            JsonArray arr = new JsonArray();
            arr.add(src.x);
            arr.add(src.y);
            arr.add(src.z);
            return arr;
        }
        
        @Override
        public Vector3f deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            if (json == null || json.isJsonNull()) return null;
            if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                if (arr.size() >= 3) {
                    return new Vector3f(
                        arr.get(0).getAsFloat(),
                        arr.get(1).getAsFloat(),
                        arr.get(2).getAsFloat()
                    );
                }
            }
            throw new JsonParseException("Expected [x, y, z] array for Vector3f");
        }
    }
    
    /**
     * Gson TypeAdapter for java.time.Instant.
     * Serializes as ISO-8601 string, deserializes from same format.
     * This avoids the Java module system reflection issue.
     */
    private static class InstantAdapter implements com.google.gson.JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            return new JsonPrimitive(src.toString()); // ISO-8601 format
        }
        
        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            if (json == null || json.isJsonNull()) return null;
            if (json.isJsonPrimitive()) {
                try {
                    return Instant.parse(json.getAsString());
                } catch (Exception e) {
                    throw new JsonParseException("Invalid Instant format: " + json.getAsString(), e);
                }
            }
            throw new JsonParseException("Expected string for Instant");
        }
    }
    
    // Cache field metadata per class for performance
    private static final Map<Class<?>, FieldMeta[]> FIELD_CACHE = new ConcurrentHashMap<>();
    
    private JsonSerializer() {} // Utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SERIALIZATION (Object → JSON)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Serialize an object to JsonObject using reflection.
     * Respects @JsonField annotations for naming, exclusion, and skip conditions.
     */
    public static JsonObject toJson(Object obj) {
        if (obj == null) return new JsonObject();
        
        JsonObject json = new JsonObject();
        
        for (FieldMeta meta : getFieldMeta(obj.getClass())) {
            if (meta.exclude) continue;
            
            try {
                Object value = meta.field.get(obj);
                
                // Skip null values (default behavior)
                if (value == null) continue;
                
                // Skip if matches default, null condition, or same as another field
                if (shouldSkip(meta, value, obj)) continue;
                
                String key = meta.jsonName;
                JsonElement element = serializeValue(value);
                json.add(key, element);
                
            } catch (IllegalAccessException e) {
                TheVirusBlock.LOGGER.warn("Failed to serialize field: {}", meta.field.getName(), e);
            }
        }
        
        return json;
    }
    
    /**
     * Serialize a value, recursively handling nested objects with @JsonField.
     */
    private static JsonElement serializeValue(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        
        Class<?> type = value.getClass();
        
        // Primitives and simple types - use Gson directly
        if (type.isPrimitive() || value instanceof Number || value instanceof Boolean || 
            value instanceof String || value instanceof Enum) {
            return GSON_COMPACT.toJsonTree(value);
        }
        
        // Collections - recursively serialize elements
        if (value instanceof java.util.Collection<?> collection) {
            JsonArray array = new JsonArray();
            for (Object element : collection) {
                array.add(serializeValue(element));
            }
            return array;
        }
        
        // Maps - recursively serialize values
        if (value instanceof java.util.Map<?, ?> map) {
            JsonObject obj = new JsonObject();
            for (var entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                obj.add(key, serializeValue(entry.getValue()));
            }
            return obj;
        }
        
        // Arrays - recursively serialize elements
        if (type.isArray()) {
            JsonArray array = new JsonArray();
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                array.add(serializeValue(java.lang.reflect.Array.get(value, i)));
            }
            return array;
        }
        
        // Check if the type has @JsonField annotations - if so, use our serializer
        if (hasJsonFieldAnnotations(type)) {
            return toJson(value);
        }
        
        // Fallback to Gson for other types (e.g., Vector3f with TypeAdapter)
        return GSON_COMPACT.toJsonTree(value);
    }
    
    /**
     * Check if a class has any fields with @JsonField annotations.
     */
    private static boolean hasJsonFieldAnnotations(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(JsonField.class)) {
                return true;
            }
        }
        // Check record components too
        if (clazz.isRecord()) {
            for (var component : clazz.getRecordComponents()) {
                if (component.isAnnotationPresent(JsonField.class)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Serialize to JSON string (pretty printed).
     */
    public static String toJsonString(Object obj) {
        return GSON.toJson(toJson(obj));
    }
    
    /**
     * Serialize to JSON string (compact, no whitespace).
     */
    public static String toJsonStringCompact(Object obj) {
        return GSON_COMPACT.toJson(toJson(obj));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DESERIALIZATION (JSON → Object)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Deserialize JSON to a new object instance.
     * Uses Gson for actual instantiation, then applies aliases.
     */
    public static <T> T fromJson(JsonObject json, Class<T> clazz) {
        // First, let Gson create the object with standard deserialization
        T obj = GSON.fromJson(json, clazz);
        
        // Then apply any aliased fields that Gson wouldn't have caught
        applyAliases(json, obj);
        
        return obj;
    }
    
    /**
     * Deserialize from JSON string.
     */
    public static <T> T fromJsonString(String jsonStr, Class<T> clazz) {
        JsonObject json = GSON.fromJson(jsonStr, JsonObject.class);
        return fromJson(json, clazz);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // APPLICATION (JSON → Existing Object)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Apply JSON values to an existing object.
     * Only sets fields that exist in the JSON.
     * Supports @JsonField aliases.
     */
    public static void apply(JsonObject json, Object target) {
        if (json == null || target == null) return;
        
        for (FieldMeta meta : getFieldMeta(target.getClass())) {
            if (meta.exclude) continue;
            
            // Try primary name first, then aliases
            String matchedKey = findMatchingKey(json, meta);
            if (matchedKey == null) continue;
            
            try {
                JsonElement element = json.get(matchedKey);
                Object value = GSON_COMPACT.fromJson(element, meta.field.getType());
                meta.field.set(target, value);
                
            } catch (Exception e) {
                TheVirusBlock.LOGGER.warn("Failed to apply field {} from key {}: {}", 
                        meta.field.getName(), matchedKey, e.getMessage());
            }
        }
    }
    
    /**
     * Apply JSON values, but only to fields in a specific group.
     * 
     * <p>Used by FragmentRegistry to apply shape-specific presets:</p>
     * <pre>{@code
     * // Only apply to fields marked @JsonField(group = "sphere")
     * JsonSerializer.applyGroup(json, state, "sphere");
     * }</pre>
     */
    public static void applyGroup(JsonObject json, Object target, String group) {
        if (json == null || target == null || group == null) return;
        
        for (FieldMeta meta : getFieldMeta(target.getClass())) {
            if (meta.exclude) continue;
            if (!group.equals(meta.group)) continue;
            
            String matchedKey = findMatchingKey(json, meta);
            if (matchedKey == null) continue;
            
            try {
                JsonElement element = json.get(matchedKey);
                Object value = GSON_COMPACT.fromJson(element, meta.field.getType());
                meta.field.set(target, value);
                
            } catch (Exception e) {
                TheVirusBlock.LOGGER.warn("Failed to apply grouped field {} from key {}: {}", 
                        meta.field.getName(), matchedKey, e.getMessage());
            }
        }
    }
    
    /**
     * Apply JSON using prefix-based mapping.
     * 
     * <p>Maps JSON key "latSteps" → field "sphereLatSteps" when prefix is "sphere".</p>
     * 
     * <p>This handles the FragmentRegistry pattern where JSON files use short names
     * but FieldEditState uses prefixed names.</p>
     * 
     * @param json The JSON object with short keys
     * @param target The target object (e.g., FieldEditState)
     * @param prefix The prefix to prepend (e.g., "sphere" → "sphereLatSteps")
     */
    public static void applyWithPrefix(JsonObject json, Object target, String prefix) {
        if (json == null || target == null) return;
        
        String lowerPrefix = prefix.toLowerCase();
        Map<String, FieldMeta> fieldsByShortName = new HashMap<>();
        
        // Build map of short names → fields
        for (FieldMeta meta : getFieldMeta(target.getClass())) {
            if (meta.exclude) continue;
            
            String fieldName = meta.field.getName();
            
            // If field starts with prefix (e.g., "sphereLatSteps" starts with "sphere")
            if (fieldName.toLowerCase().startsWith(lowerPrefix)) {
                // Extract short name: "sphereLatSteps" → "latSteps"
                String shortName = fieldName.substring(prefix.length());
                if (!shortName.isEmpty()) {
                    // Lowercase first char: "LatSteps" → "latSteps"
                    shortName = Character.toLowerCase(shortName.charAt(0)) + shortName.substring(1);
                    fieldsByShortName.put(shortName.toLowerCase(), meta);
                }
            }
            
            // Also check aliases
            for (String alias : meta.aliases) {
                fieldsByShortName.put(alias.toLowerCase(), meta);
            }
        }
        
        // Apply JSON values using short name mapping
        for (String jsonKey : json.keySet()) {
            FieldMeta meta = fieldsByShortName.get(jsonKey.toLowerCase());
            if (meta == null) continue;
            
            try {
                JsonElement element = json.get(jsonKey);
                Object value = GSON_COMPACT.fromJson(element, meta.field.getType());
                meta.field.set(target, value);
                
            } catch (Exception e) {
                TheVirusBlock.LOGGER.warn("Failed to apply prefixed field {} from key {}: {}", 
                        meta.field.getName(), jsonKey, e.getMessage());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COPYING (Object → Object)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Copy all fields from source to target.
     * Used for restoring state from snapshot.
     */
    public static void copyFields(Object source, Object target) {
        if (source == null || target == null) return;
        if (source.getClass() != target.getClass()) {
            throw new IllegalArgumentException("Source and target must be same class");
        }
        
        for (FieldMeta meta : getFieldMeta(source.getClass())) {
            try {
                Object value = meta.field.get(source);
                meta.field.set(target, value);
            } catch (IllegalAccessException e) {
                TheVirusBlock.LOGGER.warn("Failed to copy field: {}", meta.field.getName(), e);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void applyAliases(JsonObject json, Object target) {
        for (FieldMeta meta : getFieldMeta(target.getClass())) {
            if (meta.aliases.length == 0) continue;
            
            // Skip if primary name already matched
            if (json.has(meta.jsonName)) continue;
            
            // Try aliases
            for (String alias : meta.aliases) {
                if (json.has(alias)) {
                    try {
                        JsonElement element = json.get(alias);
                        Object value = GSON_COMPACT.fromJson(element, meta.field.getType());
                        meta.field.set(target, value);
                        break;
                    } catch (Exception e) {
                        TheVirusBlock.LOGGER.warn("Failed to apply alias {} for field {}", 
                                alias, meta.field.getName());
                    }
                }
            }
        }
    }
    
    private static String findMatchingKey(JsonObject json, FieldMeta meta) {
        // Try primary name
        if (json.has(meta.jsonName)) {
            return meta.jsonName;
        }
        
        // Try field name if different from jsonName
        if (!meta.jsonName.equals(meta.field.getName()) && json.has(meta.field.getName())) {
            return meta.field.getName();
        }
        
        // Try aliases
        for (String alias : meta.aliases) {
            if (json.has(alias)) {
                return alias;
            }
        }
        
        return null;
    }
    
    private static FieldMeta[] getFieldMeta(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, JsonSerializer::buildFieldMeta);
    }
    
    private static FieldMeta[] buildFieldMeta(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        FieldMeta[] metas = new FieldMeta[fields.length];
        int count = 0;
        
        // For records, build a map of component name -> annotation
        Map<String, JsonField> recordComponentAnnotations = new HashMap<>();
        if (clazz.isRecord()) {
            for (var component : clazz.getRecordComponents()) {
                JsonField annotation = component.getAnnotation(JsonField.class);
                if (annotation != null) {
                    recordComponentAnnotations.put(component.getName(), annotation);
                }
            }
        }
        
        for (Field field : fields) {
            // Skip static fields
            if (Modifier.isStatic(field.getModifiers())) continue;
            
            // Skip synthetic fields (compiler-generated)
            if (field.isSynthetic()) continue;
            
            field.setAccessible(true);
            
            FieldMeta meta = new FieldMeta();
            meta.field = field;
            
            // Try to get annotation from field first, then from record component
            JsonField annotation = field.getAnnotation(JsonField.class);
            if (annotation == null && clazz.isRecord()) {
                annotation = recordComponentAnnotations.get(field.getName());
            }
            
            if (annotation != null) {
                meta.jsonName = annotation.name().isEmpty() ? field.getName() : annotation.name();
                meta.aliases = annotation.aliases();
                meta.exclude = annotation.exclude();
                meta.group = annotation.group();
                meta.skipIfDefault = annotation.skipIfDefault();
                meta.defaultValue = annotation.defaultValue();
                meta.skipIfNull = annotation.skipIfNull();
                meta.skipIfEmpty = annotation.skipIfEmpty();
                meta.skipIfEqualsField = annotation.skipIfEqualsField();
                meta.skipUnless = annotation.skipUnless();
                meta.skipIfEqualsConstant = annotation.skipIfEqualsConstant();
            } else {
                meta.jsonName = field.getName();
                meta.aliases = new String[0];
                meta.exclude = Modifier.isTransient(field.getModifiers());
                meta.group = "";
                meta.skipIfDefault = false;
                meta.defaultValue = "";
                meta.skipIfNull = false;
                meta.skipIfEmpty = false;
                meta.skipIfEqualsField = "";
                meta.skipUnless = "";
                meta.skipIfEqualsConstant = "";
            }
            
            metas[count++] = meta;
        }
        
        // Trim array
        FieldMeta[] result = new FieldMeta[count];
        System.arraycopy(metas, 0, result, 0, count);
        return result;
    }
    
    /**
     * Cached metadata for a field.
     */
    private static class FieldMeta {
        Field field;
        String jsonName;
        String[] aliases;
        boolean exclude;
        String group;
        boolean skipIfDefault;
        String defaultValue;
        boolean skipIfNull;
        boolean skipIfEmpty;          // Skip if null or empty (for collections/strings)
        String skipIfEqualsField;     // Name of another field to compare against
        String skipUnless;            // Method name to call on field value
        String skipIfEqualsConstant;  // Static constant name to compare against
    }
    
    /**
     * Check if a value should be skipped during serialization.
     */
    private static boolean shouldSkip(FieldMeta meta, Object value, Object obj) {
        // Skip null if requested
        if (meta.skipIfNull && value == null) {
            return true;
        }
        
        // Skip if null or empty (for collections, strings, arrays)
        if (meta.skipIfEmpty) {
            if (value == null) {
                return true;
            }
            if (value instanceof java.util.Collection<?> c && c.isEmpty()) {
                return true;
            }
            if (value instanceof java.util.Map<?, ?> m && m.isEmpty()) {
                return true;
            }
            if (value instanceof String s && s.isEmpty()) {
                return true;
            }
            if (value.getClass().isArray() && java.lang.reflect.Array.getLength(value) == 0) {
                return true;
            }
        }
        
        // Skip unless method returns true (calls method on the field value)
        if (!meta.skipUnless.isEmpty()) {
            if (value == null) {
                return true;  // Null values are skipped when skipUnless is set
            }
            try {
                java.lang.reflect.Method method = value.getClass().getMethod(meta.skipUnless);
                Object result = method.invoke(value);
                if (result instanceof Boolean && !((Boolean) result)) {
                    return true;  // Skip if method returns false
                }
            } catch (Exception e) {
                TheVirusBlock.LOGGER.warn("skipUnless: method '{}' failed on {}: {}", 
                        meta.skipUnless, value.getClass().getSimpleName(), e.getMessage());
            }
        }
        
        // Skip if equals another field's value
        if (!meta.skipIfEqualsField.isEmpty()) {
            try {
                Field otherField = obj.getClass().getDeclaredField(meta.skipIfEqualsField);
                otherField.setAccessible(true);
                Object otherValue = otherField.get(obj);
                if (value.equals(otherValue)) {
                    return true;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                TheVirusBlock.LOGGER.warn("skipIfEqualsField: field '{}' not found in {}", 
                        meta.skipIfEqualsField, obj.getClass().getSimpleName());
            }
        }
        
        // Skip if equals a static constant (e.g., NONE, IDENTITY)
        if (!meta.skipIfEqualsConstant.isEmpty()) {
            if (value == null) {
                return true;  // Null is always skipped when skipIfEqualsConstant is set
            }
            
            String constantSpec = meta.skipIfEqualsConstant;
            
            // Handle primitive literals like "0.0f", "1.0f"
            if (constantSpec.matches("-?\\d+(\\.\\d+)?[fFdD]?")) {
                try {
                    if (value instanceof Float) {
                        float literal = Float.parseFloat(constantSpec.replaceAll("[fFdD]$", ""));
                        if (Math.abs((Float) value - literal) < 0.0001f) {
                            return true;
                        }
                    } else if (value instanceof Double) {
                        double literal = Double.parseDouble(constantSpec.replaceAll("[fFdD]$", ""));
                        if (Math.abs((Double) value - literal) < 0.0001) {
                            return true;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Fall through to constant lookup
                }
                return false;  // Non-matching primitive
            }
            
            // Extract constant name from potential class-prefixed format (e.g., "Transform.IDENTITY" -> "IDENTITY")
            String constantName = constantSpec;
            if (constantSpec.contains(".")) {
                constantName = constantSpec.substring(constantSpec.lastIndexOf('.') + 1);
            }
            
            try {
                // Look up the static constant on the field's type
                Class<?> fieldType = meta.field.getType();
                Field constantField = fieldType.getDeclaredField(constantName);
                constantField.setAccessible(true);
                Object constantValue = constantField.get(null);  // null for static field
                if (value.equals(constantValue)) {
                    return true;
                }
            } catch (NoSuchFieldException e) {
                // Only warn if it's not a known-good constant format
                TheVirusBlock.LOGGER.trace("skipIfEqualsConstant: constant '{}' not found in {}", 
                        constantName, meta.field.getType().getSimpleName());
            } catch (IllegalAccessException e) {
                TheVirusBlock.LOGGER.warn("skipIfEqualsConstant: cannot access constant '{}' in {}", 
                        constantName, meta.field.getType().getSimpleName());
            }
        }
        
        // Skip if matches default
        if (meta.skipIfDefault) {
            if (value == null) return true;
            
            Class<?> type = meta.field.getType();
            
            // Check custom default value
            if (!meta.defaultValue.isEmpty()) {
                Object defaultVal = parseDefaultValue(meta.defaultValue, type);
                if (defaultVal != null && defaultVal.equals(value)) {
                    return true;
                }
                // For primitives, also check via string comparison
                if (meta.defaultValue.equals(String.valueOf(value))) {
                    return true;
                }
            } else {
                // Check primitive defaults
                if (type == boolean.class || type == Boolean.class) {
                    return Boolean.FALSE.equals(value);
                }
                if (type == int.class || type == Integer.class) {
                    return Integer.valueOf(0).equals(value);
                }
                if (type == long.class || type == Long.class) {
                    return Long.valueOf(0L).equals(value);
                }
                if (type == float.class || type == Float.class) {
                    return Float.valueOf(0f).equals(value);
                }
                if (type == double.class || type == Double.class) {
                    return Double.valueOf(0d).equals(value);
                }
            }
        }
        
        return false;
    }
    
    /**
     * Parse a default value string to the appropriate type.
     */
    private static Object parseDefaultValue(String value, Class<?> type) {
        try {
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            }
            if (type == float.class || type == Float.class) {
                return Float.parseFloat(value);
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            }
            if (type == String.class) {
                return value;
            }
            if (type.isEnum()) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object enumVal = Enum.valueOf((Class<Enum>) type, value);
                return enumVal;
            }
        } catch (Exception e) {
            // Failed to parse, return null
        }
        return null;
    }
}

