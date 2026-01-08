package net.cyberpunk042.field.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility methods for parsing JSON configs.
 * 
 * <p>Reduces repetitive parsing code in FieldLoader.
 */
public final class JsonParseUtils {
    
    private JsonParseUtils() {}
    
    /**
     * Parses an optional object field using a parser function.
     * 
     * @param json The parent JSON object
     * @param key The field key
     * @param parser Function to parse the JsonObject
     * @return Parsed value or null if field doesn't exist or isn't an object
     */
    @Nullable
    public static <T> T parseOptional(JsonObject json, String key, Function<JsonObject, T> parser) {
        if (!json.has(key)) {
            return null;
        }
        JsonElement element = json.get(key);
        if (!element.isJsonObject()) {
            return null;
        }
        return parser.apply(element.getAsJsonObject());
    }
    
    /**
     * Parses an optional object field with a default value.
     */
    public static <T> T parseOptional(JsonObject json, String key, Function<JsonObject, T> parser, T defaultValue) {
        T result = parseOptional(json, key, parser);
        return result != null ? result : defaultValue;
    }
    
    /**
     * Parses an array of objects using a parser function.
     * 
     * @param json The parent JSON object
     * @param key The array field key
     * @param parser Function to parse each JsonObject in the array
     * @return List of parsed values (empty if field doesn't exist or isn't an array)
     */
    public static <T> List<T> parseArray(JsonObject json, String key, Function<JsonObject, T> parser) {
        List<T> results = new ArrayList<>();
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return results;
        }
        
        JsonArray array = json.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                T parsed = parser.apply(element.getAsJsonObject());
                if (parsed != null) {
                    results.add(parsed);
                }
            }
        }
        return results;
    }
    
    /**
     * Parses a map of objects using a parser function.
     * 
     * @param json The parent JSON object
     * @param key The object field key (treated as a map)
     * @param parser Function to parse each JsonObject value
     * @return Map of parsed values (empty if field doesn't exist or isn't an object)
     */
    public static <T> Map<String, T> parseMap(JsonObject json, String key, Function<JsonObject, T> parser) {
        Map<String, T> results = new HashMap<>();
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            return results;
        }
        
        JsonObject mapObj = json.getAsJsonObject(key);
        for (String mapKey : mapObj.keySet()) {
            JsonElement element = mapObj.get(mapKey);
            if (element.isJsonObject()) {
                T parsed = parser.apply(element.getAsJsonObject());
                if (parsed != null) {
                    results.put(mapKey, parsed);
                }
            }
        }
        return results;
    }
    
    /**
     * Gets a float value with a default.
     */
    public static float getFloat(JsonObject json, String key, float defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() 
            ? json.get(key).getAsFloat() 
            : defaultValue;
    }
    
    /**
     * Gets a boolean value with a default.
     */
    public static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() 
            ? json.get(key).getAsBoolean() 
            : defaultValue;
    }
    
    /**
     * Gets a string value with a default.
     */
    @Nullable
    public static String getString(JsonObject json, String key, @Nullable String defaultValue) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            return defaultValue;
        }
        String value = json.get(key).getAsString();
        return value.isEmpty() ? defaultValue : value;
    }
}

