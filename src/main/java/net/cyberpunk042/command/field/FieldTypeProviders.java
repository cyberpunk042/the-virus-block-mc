package net.cyberpunk042.command.field;

import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for field type providers.
 * 
 * <p>Manages the mapping between field types and their command providers.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Register providers during initialization
 * FieldTypeProviders.register(new ShieldTypeProvider());
 * FieldTypeProviders.register(new PersonalTypeProvider());
 * 
 * // Get a provider
 * Optional&lt;FieldTypeProvider&gt; provider = FieldTypeProviders.get(FieldType.SHIELD);
 * </pre>
 * 
 * @see FieldTypeProvider
 * @see FieldCommand
 */
public final class FieldTypeProviders {
    
    private static final Map<FieldType, FieldTypeProvider> PROVIDERS = new EnumMap<>(FieldType.class);
    
    private FieldTypeProviders() {} // Static utility class
    
    /**
     * Registers a provider for its field type.
     * 
     * @param provider the provider to register
     * @throws IllegalStateException if a provider is already registered for the type
     */
    public static void register(FieldTypeProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        
        FieldType type = provider.getType();
        if (PROVIDERS.containsKey(type)) {
            throw new IllegalStateException("Provider already registered for type: " + type);
        }
        
        PROVIDERS.put(type, provider);
        Logging.REGISTRY.topic("field-type").info(
            "Registered provider for field type: {}", type.id());
    }
    
    /**
     * Gets the provider for a field type.
     * 
     * @param type the field type
     * @return the provider, or empty if none registered
     */
    public static Optional<FieldTypeProvider> get(FieldType type) {
        return Optional.ofNullable(PROVIDERS.get(type));
    }
    
    /**
     * Gets the provider for a field type, throwing if not found.
     * 
     * @param type the field type
     * @return the provider
     * @throws IllegalStateException if no provider is registered
     */
    public static FieldTypeProvider getRequired(FieldType type) {
        return get(type).orElseThrow(() -> 
            new IllegalStateException("No provider registered for type: " + type));
    }
    
    /**
     * Checks if a provider is registered for a type.
     */
    public static boolean hasProvider(FieldType type) {
        return PROVIDERS.containsKey(type);
    }
    
    /**
     * Returns all registered providers.
     */
    public static Iterable<FieldTypeProvider> all() {
        return PROVIDERS.values();
    }
    
    /**
     * Clears all providers (for testing).
     */
    public static void clear() {
        PROVIDERS.clear();
    }
    
    /**
     * Notifies all providers of a reload event.
     */
    public static void notifyReload() {
        for (FieldTypeProvider provider : PROVIDERS.values()) {
            try {
                provider.onReload();
            } catch (Exception e) {
                Logging.REGISTRY.topic("field-type").error(
                    "Error during reload for provider {}: {}", 
                    provider.getType().id(), e.getMessage());
            }
        }
    }
}

