package net.cyberpunk042.field.loader;

import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic validation for field JSON.
 * 
 * <p>Per F141: Validates required fields and logs warnings for issues.
 * Does NOT reject fields for extreme values (those just cause lag if used).</p>
 */
public final class ValidationHelper {
    
    private ValidationHelper() {}
    
    /**
     * Validates a field definition JSON.
     * Returns list of warnings (empty if valid).
     */
    public static List<String> validateDefinition(JsonObject json) {
        List<String> warnings = new ArrayList<>();
        
        // Required: id
        if (!json.has("id") || json.get("id").getAsString().isEmpty()) {
            warnings.add("Field definition missing 'id'");
        }
        
        // Required: layers array
        if (!json.has("layers") || !json.get("layers").isJsonArray()) {
            warnings.add("Field definition missing 'layers' array");
        }
        
        return warnings;
    }
    
    /**
     * Validates a layer JSON.
     */
    public static List<String> validateLayer(JsonObject json) {
        List<String> warnings = new ArrayList<>();
        
        // Recommended: id
        if (!json.has("id")) {
            warnings.add("Layer missing 'id' (will use default)");
        }
        
        // Required: primitives array
        if (!json.has("primitives") || !json.get("primitives").isJsonArray()) {
            warnings.add("Layer missing 'primitives' array");
        }
        
        return warnings;
    }
    
    /**
     * Validates a primitive JSON.
     */
    public static List<String> validatePrimitive(JsonObject json, String layerId) {
        List<String> warnings = new ArrayList<>();
        String prefix = layerId != null ? "[" + layerId + "] " : "";
        
        // Required: id (for linking)
        if (!json.has("id")) {
            warnings.add(prefix + "Primitive missing 'id' (required for linking)");
        }
        
        // Required: type
        if (!json.has("type")) {
            warnings.add(prefix + "Primitive missing 'type' (defaulting to sphere)");
        }
        
        return warnings;
    }
    
    /**
     * Logs all warnings.
     */
    public static void logWarnings(List<String> warnings) {
        for (String warning : warnings) {
            Logging.FIELD.topic("validate").warn(warning);
        }
    }
    
    /**
     * Validates and logs. Returns true if no critical errors.
     */
    public static boolean validateAndLog(JsonObject json, String type) {
        List<String> warnings = switch (type) {
            case "definition" -> validateDefinition(json);
            case "layer" -> validateLayer(json);
            case "primitive" -> validatePrimitive(json, null);
            default -> List.of();
        };
        
        logWarnings(warnings);
        return warnings.stream().noneMatch(w -> w.contains("missing"));
    }
}
