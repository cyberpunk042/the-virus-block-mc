package net.cyberpunk042.network.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.log.Logging;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Provides official server profiles that all players can use.
 * 
 * <p>Server profiles are read-only for players. They are loaded from:
 * <ul>
 *   <li>{@code config/the-virus-block/server_profiles/} - Admin-customizable</li>
 *   <li>Built-in resources (future)</li>
 * </ul>
 * 
 * <p>Players can use server profiles but cannot modify them. 
 * To customize, they must save a local copy.
 */
public final class ServerProfileProvider {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Profile name -> JSON content
    private static final Map<String, String> PROFILES = new LinkedHashMap<>();
    
    // Profile name -> metadata for listing
    private static final Map<String, ProfileMeta> METADATA = new LinkedHashMap<>();
    
    private ServerProfileProvider() {}
    
    /**
     * Simple metadata for profile listing.
     */
    public record ProfileMeta(String name, String description, String category) {}
    
    /**
     * Load all server profiles from config folder.
     * Call this on server start.
     */
    public static void load() {
        PROFILES.clear();
        METADATA.clear();
        
        Path configDir = FabricLoader.getInstance().getConfigDir()
            .resolve("the-virus-block")
            .resolve("server_profiles");
        
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
                Logging.GUI.topic("profiles").info("Created server_profiles directory");
                // Create example profile
                createExampleProfile(configDir);
            } catch (IOException e) {
                Logging.GUI.topic("profiles").error("Failed to create server_profiles dir: {}", e.getMessage());
            }
            return;
        }
        
        try (Stream<Path> files = Files.list(configDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                 .forEach(ServerProfileProvider::loadProfile);
        } catch (IOException e) {
            Logging.GUI.topic("profiles").error("Failed to scan server_profiles: {}", e.getMessage());
        }
        
        Logging.GUI.topic("profiles").info("Loaded {} server profiles", PROFILES.size());
    }
    
    private static void loadProfile(Path file) {
        try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            
            String name = obj.has("name") ? obj.get("name").getAsString() 
                : file.getFileName().toString().replace(".json", "");
            String description = obj.has("description") ? obj.get("description").getAsString() : "";
            String category = obj.has("category") ? obj.get("category").getAsString() : "general";
            
            PROFILES.put(name, json);
            METADATA.put(name, new ProfileMeta(name, description, category));
            
            Logging.GUI.topic("profiles").debug("Loaded server profile: {}", name);
            
        } catch (Exception e) {
            Logging.GUI.topic("profiles").error("Failed to load profile {}: {}", 
                file.getFileName(), e.getMessage());
        }
    }
    
    /**
     * Get list of all server profile names.
     */
    public static List<String> listProfiles() {
        return new ArrayList<>(PROFILES.keySet());
    }
    
    /**
     * Get metadata for all profiles.
     */
    public static List<ProfileMeta> listMetadata() {
        return new ArrayList<>(METADATA.values());
    }
    
    /**
     * Get a profile by name.
     * @return JSON string or null if not found
     */
    public static String getProfile(String name) {
        return PROFILES.get(name);
    }
    
    /**
     * Check if a profile exists.
     */
    public static boolean hasProfile(String name) {
        return PROFILES.containsKey(name);
    }
    
    /**
     * Reload profiles from disk.
     */
    public static void reload() {
        load();
    }
    

    /**
     * Save a profile to the server_profiles directory.
     * @param name profile name (used as filename)
     * @param json profile JSON content
     * @return true if saved successfully
     */
    public static boolean saveProfile(String name, String json) {
        Path configDir = FabricLoader.getInstance().getConfigDir()
            .resolve("the-virus-block")
            .resolve("server_profiles");
        
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            // Sanitize filename
            String safeName = name.replaceAll("[^a-zA-Z0-9_-]", "_");
            Path file = configDir.resolve(safeName + ".json");
            
            Files.writeString(file, json);
            
            // Update cache
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String displayName = obj.has("name") ? obj.get("name").getAsString() : name;
            String description = obj.has("description") ? obj.get("description").getAsString() : "";
            String category = obj.has("category") ? obj.get("category").getAsString() : "general";
            
            PROFILES.put(displayName, json);
            METADATA.put(displayName, new ProfileMeta(displayName, description, category));
            
            Logging.GUI.topic("profiles").info("Saved server profile: {}", displayName);
            return true;
            
        } catch (Exception e) {
            Logging.GUI.topic("profiles").error("Failed to save server profile {}: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * Create an example server profile.
     */
    private static void createExampleProfile(Path dir) {
        JsonObject example = new JsonObject();
        example.addProperty("name", "Server Default Shield");
        example.addProperty("description", "Default shield configuration provided by server");
        example.addProperty("category", "shields");
        example.addProperty("version", "1.0");
        
        JsonObject definition = new JsonObject();
        definition.addProperty("id", "server_default");
        definition.addProperty("type", "SHIELD");
        definition.addProperty("baseRadius", 1.5f);
        example.add("definition", definition);
        
        try {
            Path file = dir.resolve("server_default.json");
            Files.writeString(file, GSON.toJson(example));
            Logging.GUI.topic("profiles").info("Created example server profile");
        } catch (IOException e) {
            Logging.GUI.topic("profiles").error("Failed to create example profile: {}", e.getMessage());
        }
    }
}














