package net.cyberpunk042.field;

import net.cyberpunk042.field.loader.FieldLoader;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry for field definitions.
 * 
 * <p>Provides a simple static API for accessing field definitions.
 * Internally uses FieldLoader for actual loading/storage.
 * 
 * <p>For client-side: Definitions should be received via network payloads
 * and registered here. For server-side: Definitions are loaded via FieldLoader.
 */
public final class FieldRegistry {
    
    private static final Map<String, FieldDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private static FieldLoader loader;
    
    private FieldRegistry() {}
    
    /**
     * Initializes the registry with a FieldLoader instance.
     */
    public static void initialize(FieldLoader fieldLoader) {
        loader = fieldLoader;
        Logging.FIELD.topic("registry").debug("FieldRegistry initialized");
    }
    
    /**
     * Registers a field definition.
     */
    public static void register(FieldDefinition definition) {
        if (definition == null || definition.id() == null) {
            Logging.FIELD.topic("registry").warn("Attempted to register null definition");
            return;
        }
        DEFINITIONS.put(definition.id(), definition);
        Logging.FIELD.topic("registry").debug("Registered field: {}", definition.id());
    }
    
    /**
     * Gets a field definition by ID.
     * 
     * @param id The definition ID (e.g., "shield_default" or Identifier)
     * @return The definition or null if not found
     */
    @Nullable
    public static FieldDefinition get(String id) {
        FieldDefinition def = DEFINITIONS.get(id);
        if (def != null) {
            return def;
        }
        // Try via loader if available
        if (loader != null) {
            def = loader.getDefinition(id);
            if (def != null) {
                DEFINITIONS.put(id, def); // Cache it
            }
        }
        return def;
    }
    
    /**
     * Gets a field definition by Identifier.
     */
    @Nullable
    public static FieldDefinition get(Identifier id) {
        // Try full identifier string first
        String fullId = id.toString();
        FieldDefinition def = get(fullId);
        if (def != null) {
            return def;
        }
        // Try just the path
        return get(id.getPath());
    }
    
    /**
     * Clears all registered definitions.
     */
    public static void clear() {
        DEFINITIONS.clear();
        Logging.FIELD.topic("registry").debug("FieldRegistry cleared");
    }
    
    /**
     * Reloads definitions (if loader is available).
     */
    public static void reload() {
        if (loader != null) {
            loader.reload();
            DEFINITIONS.clear(); // Clear cache, will reload on next get()
        }
    }
    
    /**
     * Returns all registered field definitions.
     */
    public static java.util.Collection<FieldDefinition> all() {
        return java.util.Collections.unmodifiableCollection(DEFINITIONS.values());
    }
    
    /**
     * Returns count of all registered definitions.
     */
    public static int count() {
        return DEFINITIONS.size();
    }
    
    /**
     * Returns count of definitions matching a specific field type.
     */
    public static int count(FieldType type) {
        if (type == null) return 0;
        return (int) DEFINITIONS.values().stream()
            .filter(def -> def.type() == type)
            .count();
    }
    
    /**
     * Registers default field definitions.
     * Should be called during mod initialization.
     */
    /**
     * Returns definitions matching a specific field type.
     */
    public static java.util.List<FieldDefinition> byType(FieldType type) {
        if (type == null) return java.util.List.of();
        return DEFINITIONS.values().stream()
            .filter(def -> def.type() == type)
            .toList();
    }
    
    /**
     * Returns all registered definition IDs.
     */
    public static java.util.Set<String> ids() {
        return java.util.Collections.unmodifiableSet(DEFINITIONS.keySet());
    }
    
    /**
     * Logs the current registry status.
     */
    public static void logStatus() {
        Logging.FIELD.topic("registry").info(
            "FieldRegistry: {} definitions loaded", DEFINITIONS.size());
    }
    
    public static void registerDefaults() {
        if (loader != null) {
            loader.loadAll();
            Logging.FIELD.topic("registry").info("Loaded {} default field definitions", DEFINITIONS.size());
        } else {
            Logging.FIELD.topic("registry").warn("Cannot load defaults - no FieldLoader initialized");
        }
    }
}

