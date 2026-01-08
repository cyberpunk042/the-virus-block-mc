package net.cyberpunk042.field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.log.Logging;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Manages saving and loading custom field profiles to the config directory.
 * 
 * <p>Profiles are stored in: {@code config/the-virus-block/profiles/}
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Save current profile
 * FieldProfileStore.save("my_custom_shield", definition);
 * 
 * // Load saved profile
 * Optional<FieldDefinition> loaded = FieldProfileStore.load("my_custom_shield");
 * 
 * // List saved profiles
 * List<String> names = FieldProfileStore.list();
 * </pre>
 */
public final class FieldProfileStore {
    
    private static final Path PROFILE_DIR = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("the-virus-block")
        .resolve("profiles");
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    private FieldProfileStore() {}
    
    /**
     * Saves a field definition to the profile directory.
     * 
     * @param name Profile name (will be sanitized)
     * @param definition The definition to save
     * @return true if saved successfully
     */
    public static boolean save(String name, FieldDefinition definition) {
        String sanitized = sanitizeName(name);
        if (sanitized.isEmpty()) {
            Logging.REGISTRY.topic("profile").warn("Invalid profile name: {}", name);
            return false;
        }
        
        Path file = PROFILE_DIR.resolve(sanitized + ".json");
        try {
            Files.createDirectories(PROFILE_DIR);
            
            JsonObject json = definition.toJson();
            // Add metadata
            json.addProperty("_savedAs", sanitized);
            json.addProperty("_savedTime", System.currentTimeMillis());
            
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            
            Logging.REGISTRY.topic("profile").info("Saved profile: {} -> {}", sanitized, file);
            return true;
            
        } catch (IOException e) {
            Logging.REGISTRY.topic("profile").error("Failed to save profile {}: {}", sanitized, e.getMessage());
            return false;
        }
    }
    
    /**
     * Loads a field definition from the profile directory.
     * 
     * @param name Profile name
     * @return The loaded definition, or empty if not found
     */
    public static Optional<FieldDefinition> load(String name) {
        String sanitized = sanitizeName(name);
        if (sanitized.isEmpty()) {
            return Optional.empty();
        }
        
        Path file = PROFILE_DIR.resolve(sanitized + ".json");
        if (!Files.exists(file)) {
            Logging.REGISTRY.topic("profile").debug("Profile not found: {}", sanitized);
            return Optional.empty();
        }
        
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Identifier id = Identifier.of("the-virus-block", "custom/" + sanitized);
            // Parse using FieldLoader
            FieldDefinition def = new net.cyberpunk042.field.loader.FieldLoader().parseDefinition(json);
            
            Logging.REGISTRY.topic("profile").info("Loaded profile: {}", sanitized);
            return Optional.of(def);
            
        } catch (IOException | IllegalStateException e) {
            Logging.REGISTRY.topic("profile").error("Failed to load profile {}: {}", sanitized, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Loads and registers a profile in the FieldRegistry.
     * 
     * @param name Profile name
     * @return true if loaded and registered
     */
    public static boolean loadAndRegister(String name) {
        return load(name).map(def -> {
            FieldRegistry.register(def);
            Logging.REGISTRY.topic("profile").info("Registered custom profile: {}", def.id());
            return true;
        }).orElse(false);
    }
    
    /**
     * Lists all saved profile names.
     */
    public static List<String> list() {
        if (!Files.isDirectory(PROFILE_DIR)) {
            return Collections.emptyList();
        }
        
        List<String> names = new ArrayList<>();
        try {
            Files.list(PROFILE_DIR)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    names.add(fileName.substring(0, fileName.length() - 5));
                });
        } catch (IOException e) {
            Logging.REGISTRY.topic("profile").error("Failed to list profiles: {}", e.getMessage());
        }
        
        Collections.sort(names);
        return names;
    }
    
    /**
     * Deletes a saved profile.
     * 
     * @param name Profile name
     * @return true if deleted
     */
    public static boolean delete(String name) {
        String sanitized = sanitizeName(name);
        if (sanitized.isEmpty()) {
            return false;
        }
        
        Path file = PROFILE_DIR.resolve(sanitized + ".json");
        try {
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                Logging.REGISTRY.topic("profile").info("Deleted profile: {}", sanitized);
            }
            return deleted;
        } catch (IOException e) {
            Logging.REGISTRY.topic("profile").error("Failed to delete profile {}: {}", sanitized, e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a profile exists.
     */
    public static boolean exists(String name) {
        String sanitized = sanitizeName(name);
        if (sanitized.isEmpty()) {
            return false;
        }
        return Files.exists(PROFILE_DIR.resolve(sanitized + ".json"));
    }
    
    /**
     * Gets the profile directory path.
     */
    public static Path getProfileDirectory() {
        return PROFILE_DIR;
    }
    
    /**
     * Sanitizes a profile name for use as a filename.
     */
    public static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < trimmed.length() && sb.length() < 64; i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sb.append(c);
            } else if (Character.isWhitespace(c) && sb.length() > 0) {
                sb.append('_');
            }
        }
        
        // Remove trailing underscores
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '_') {
            sb.deleteCharAt(sb.length() - 1);
        }
        
        return sb.toString();
    }
}
