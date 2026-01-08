package net.cyberpunk042.field.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract base class for profile registries.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Thread-safe storage</li>
 *   <li>JSON loading/saving</li>
 *   <li>Hot-reload support</li>
 *   <li>Default profiles</li>
 * </ul>
 * 
 * <h2>Implementation</h2>
 * <pre>
 * public class FieldDefinitionRegistry extends ProfileRegistry&lt;FieldDefinition&gt; {
 *     public FieldDefinitionRegistry() {
 *         super("field_definitions");
 *     }
 *     
 *     &#64;Override
 *     protected FieldDefinition parse(JsonObject json, Identifier id) {
 *         return FieldDefinition.fromJson(json, id);
 *     }
 *     
 *     &#64;Override
 *     protected JsonObject serialize(FieldDefinition profile) {
 *         return profile.toJson();
 *     }
 * }
 * </pre>
 * 
 * @param <T> the profile type
 */
public abstract class ProfileRegistry<T> {
    
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    protected final String name;
    protected final Map<Identifier, T> profiles = new ConcurrentHashMap<>();
    protected Path directory;
    
    protected ProfileRegistry(String name) {
        this.name = name;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Abstract methods - Implement in subclasses
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Parses a profile from JSON.
     * @param json the JSON object
     * @param id the profile identifier
     * @return the parsed profile
     */
    protected abstract T parse(JsonObject json, Identifier id);
    
    /**
     * Serializes a profile to JSON.
     * @param profile the profile to serialize
     * @return JSON representation
     */
    protected abstract JsonObject serialize(T profile);
    
    /**
     * Gets the identifier for a profile.
     * @param profile the profile
     * @return the profile's identifier
     */
    protected abstract Identifier getId(T profile);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Registration
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Registers a profile.
     * @param profile the profile to register
     */
    public void register(T profile) {
        if (profile == null) {
            return;
        }
        Identifier id = getId(profile);
        if (id == null) {
            Logging.REGISTRY.topic(name).warn("Attempted to register profile with null ID");
            return;
        }
        profiles.put(id, profile);
        Logging.REGISTRY.topic(name).info("Registered: {}", id);
    }
    
    /**
     * Registers multiple profiles.
     */
    public void registerAll(Collection<T> profileList) {
        for (T profile : profileList) {
            register(profile);
        }
    }
    
    /**
     * Unregisters a profile.
     * @param id the profile ID to remove
     * @return true if removed
     */
    public boolean unregister(Identifier id) {
        if (id == null) {
            return false;
        }
        T removed = profiles.remove(id);
        if (removed != null) {
            Logging.REGISTRY.topic(name).info("Unregistered: {}", id);
            return true;
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Lookup
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Gets a profile by ID.
     * @return profile or null if not found
     */
    public T get(Identifier id) {
        return id != null ? profiles.get(id) : null;
    }
    
    /**
     * Gets a profile by string ID.
     */
    public T get(String id) {
        return id != null ? get(Identifier.tryParse(id)) : null;
    }
    
    /**
     * Gets a profile with fallback.
     */
    public T getOrDefault(Identifier id, T fallback) {
        T profile = get(id);
        return profile != null ? profile : fallback;
    }
    
    /**
     * Checks if a profile exists.
     */
    public boolean exists(Identifier id) {
        return id != null && profiles.containsKey(id);
    }
    
    /**
     * Returns all profiles matching a predicate.
     */
    public List<T> filter(Predicate<T> predicate) {
        return profiles.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Collection access
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns all profile IDs.
     */
    public Set<Identifier> ids() {
        return Collections.unmodifiableSet(profiles.keySet());
    }
    
    /**
     * Returns all profiles.
     */
    public Collection<T> all() {
        return Collections.unmodifiableCollection(profiles.values());
    }
    
    /**
     * Returns profile count.
     */
    public int count() {
        return profiles.size();
    }
    
    /**
     * Checks if empty.
     */
    public boolean isEmpty() {
        return profiles.isEmpty();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Loading
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the directory for file operations.
     */
    public void setDirectory(Path directory) {
        this.directory = directory;
    }
    
    /**
     * Loads all profiles from the directory.
     * @return number of profiles loaded
     */
    public int load() {
        if (directory == null || !Files.isDirectory(directory)) {
            Logging.REGISTRY.topic(name).warn("Cannot load: directory not set or doesn't exist");
            return 0;
        }
        
        int loaded = 0;
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                if (loadFile(file)) {
                    loaded++;
                }
            }
        } catch (IOException e) {
            Logging.REGISTRY.topic(name).error("Failed to list directory", e);
        }
        
        Logging.REGISTRY.topic(name).info("Loaded {} profiles from {}", loaded, directory);
        return loaded;
    }
    
    /**
     * Loads a single profile from a file.
     */
    protected boolean loadFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            // Derive ID from filename
            String filename = file.getFileName().toString();
            String idPath = filename.substring(0, filename.lastIndexOf('.'));
            Identifier id = Identifier.of("the-virus-block", idPath);
            
            T profile = parse(json, id);
            if (profile != null) {
                register(profile);
                return true;
            }
        } catch (Exception e) {
            Logging.REGISTRY.topic(name).error("Failed to load {}: {}", file, e.getMessage());
        }
        return false;
    }
    
    /**
     * Reloads all profiles (clears and re-loads).
     */
    public int reload() {
        clear();
        registerDefaults();
        return load();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Saving
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Saves a profile to file.
     */
    public boolean save(Identifier id) {
        if (directory == null) {
            return false;
        }
        
        T profile = get(id);
        if (profile == null) {
            return false;
        }
        
        Path file = directory.resolve(id.getPath() + ".json");
        try {
            Files.createDirectories(directory);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(serialize(profile), writer);
            }
            Logging.REGISTRY.topic(name).info("Saved: {}", id);
            return true;
        } catch (IOException e) {
            Logging.REGISTRY.topic(name).error("Failed to save {}: {}", id, e.getMessage());
            return false;
        }
    }
    
    /**
     * Saves all profiles.
     */
    public int saveAll() {
        int saved = 0;
        for (Identifier id : ids()) {
            if (save(id)) {
                saved++;
            }
        }
        return saved;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Clears all profiles.
     */
    public void clear() {
        profiles.clear();
        Logging.REGISTRY.topic(name).info("Cleared all profiles");
    }
    
    /**
     * Registers default/built-in profiles.
     * Override in subclasses to provide defaults.
     */
    protected void registerDefaults() {
        // Override in subclasses
    }
    
    /**
     * Logs current registry status.
     */
    public void logStatus() {
        Logging.REGISTRY.topic(name).info("{} registry: {} profiles", name, count());
    }
    
    /**
     * Gets the registry name.
     */
    public String getName() {
        return name;
    }
}
