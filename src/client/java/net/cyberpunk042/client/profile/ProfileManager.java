package net.cyberpunk042.client.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.field.category.ProfileCategory;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.field.profile.Profile;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages loading, saving, and filtering profiles from multiple sources.
 * 
 * Sources:
 * - SERVER: assets/the-virus-block/field_profiles/*.json (bundled, can be overridden)
 * - BUNDLED: assets/the-virus-block/field_profiles/personal/*.json (personal presets)
 * - LOCAL: config/the-virus-block/field_profiles/local/*.json (user-modified)
 */
public class ProfileManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final List<Profile> allProfiles = new ArrayList<>();
    private final List<Profile> serverProfiles = new ArrayList<>();
    private boolean loaded = false;
    
    // Singleton
    private static ProfileManager instance;
    public static ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }
    
    private ProfileManager() {}
    
    /**
     * Load all profiles from all sources.
     */
    public void loadAll() {
        allProfiles.clear();
        
        loadBundled();
        loadLocal();
        // Server profiles are added via addServerProfiles()
        allProfiles.addAll(serverProfiles);
        
        loaded = true;
        TheVirusBlock.LOGGER.info("Loaded {} total profiles", allProfiles.size());
    }
    
    /**
     * Load profiles from JAR data (data/the-virus-block/field_profiles/).
     * - field_profiles/*.json = SERVER profiles (can be overridden by server)
     * - field_profiles/personal/*.json = BUNDLED personal presets
     */
    private void loadBundled() {
        FabricLoader.getInstance().getModContainer(TheVirusBlock.MOD_ID).ifPresent(container -> {
            container.findPath("data/" + TheVirusBlock.MOD_ID + "/field_profiles")
                .ifPresent(path -> {
                    TheVirusBlock.LOGGER.info("Loading bundled profiles from: {}", path);
                    loadProfilesFromPath(path, ProfileSource.SERVER);
                    
                    // Also load personal presets
                    try {
                        Path personalPath = path.resolve("personal");
                        if (Files.isDirectory(personalPath)) {
                            loadProfilesFromPath(personalPath, ProfileSource.BUNDLED);
                        }
                    } catch (Exception e) {
                        TheVirusBlock.LOGGER.debug("No personal profiles folder");
                    }
                });
        });
    }
    
    /**
     * Load all profiles from a Path (can be in JAR or filesystem).
     */
    private void loadProfilesFromPath(Path dir, ProfileSource source) {
        if (!Files.isDirectory(dir)) {
            TheVirusBlock.LOGGER.debug("Profile directory does not exist: {}", dir);
            return;
        }
        
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(file -> loadProfileFromPath(file, source));
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to scan profiles in: {}", dir, e);
        }
    }
    
    /**
     * Load a single profile from a Path.
     */
    private void loadProfileFromPath(Path file, ProfileSource source) {
        try {
            String content = Files.readString(file);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            
            // Derive id/name from filename
            String filename = file.getFileName().toString();
            String derivedId = filename.replace(".json", "");
            
            // Ensure JSON has id and name
            if (!json.has("id") || "unknown".equals(json.get("id").getAsString())) {
                json.addProperty("id", derivedId);
            }
            if (!json.has("name") || "unknown".equals(json.get("name").getAsString())) {
                String prettyName = derivedId.replace("_", " ").replace("-", " ");
                if (!prettyName.isEmpty()) {
                    prettyName = prettyName.substring(0, 1).toUpperCase() + prettyName.substring(1);
                }
                json.addProperty("name", prettyName);
            }
            
            // Parse the profile
            Profile profile;
            if (!json.has("definition")) {
                // Old format: entire JSON is the definition
                net.cyberpunk042.field.FieldDefinition definition = 
                    net.cyberpunk042.field.FieldDefinition.fromJson(json);
                
                String id = json.has("id") ? json.get("id").getAsString() : derivedId;
                String name = json.has("name") ? json.get("name").getAsString() : derivedId;
                
                profile = Profile.builder()
                    .id(id)
                    .name(name)
                    .source(source)
                    .definition(definition)
                    .build();
            } else {
                // New format: proper Profile structure
                profile = Profile.fromJson(json, source);
            }
            
            allProfiles.add(profile);
            TheVirusBlock.LOGGER.debug("Loaded {} profile: {}", source.name().toLowerCase(), profile.name());
        } catch (Exception e) {
            TheVirusBlock.LOGGER.error("Failed to load profile: {}", file, e);
        }
    }
    
    /**
     * Load a single profile from a file.
     */
    private void loadProfileFromFile(Path file, ProfileSource source) {
        try {
            String content = Files.readString(file);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            
            // Derive id/name from filename if not in JSON
            String filename = file.getFileName().toString();
            String derivedId = filename.replace(".json", "");
            
            // Ensure JSON has id and name
            if (!json.has("id") || "unknown".equals(json.get("id").getAsString())) {
                json.addProperty("id", derivedId);
            }
            if (!json.has("name") || "unknown".equals(json.get("name").getAsString())) {
                // Convert snake_case to Title Case
                String prettyName = derivedId.replace("_", " ").replace("-", " ");
                if (!prettyName.isEmpty()) {
                    prettyName = prettyName.substring(0, 1).toUpperCase() + prettyName.substring(1);
                }
                json.addProperty("name", prettyName);
            }
            
            // Check if this is old format (no 'definition' field - the whole JSON IS the definition)
            // vs new format (has 'definition' field with the actual field data)
            Profile profile;
            if (!json.has("definition")) {
                // Old format: the entire JSON is the field definition
                // Create a Profile wrapper with the JSON as the definition
                net.cyberpunk042.field.FieldDefinition definition = 
                    net.cyberpunk042.field.FieldDefinition.fromJson(json);
                
                String id = json.has("id") ? json.get("id").getAsString() : derivedId;
                String name = json.has("name") ? json.get("name").getAsString() : derivedId;
                
                profile = Profile.builder()
                    .id(id)
                    .name(name)
                    .source(source)
                    .definition(definition)
                    .build();
            } else {
                // New format: proper Profile structure
                profile = Profile.fromJson(json, source);
            }
            
            allProfiles.add(profile);
            TheVirusBlock.LOGGER.debug("Loaded {} profile: {} (def={})", 
                source.name().toLowerCase(), profile.name(), profile.definition() != null ? "yes" : "no");
        } catch (Exception e) {
            TheVirusBlock.LOGGER.error("Failed to load profile: {}", file, e);
        }
    }
    
    /**
     * Load local profiles from config directory.
     * - config/the-virus-block/field_profiles/*.json = Override SERVER profiles (loaded as SERVER source)
     * - config/the-virus-block/field_profiles/local/*.json = User's LOCAL profiles
     */
    private void loadLocal() {
        Path rootDir = getProfilesRootDir();
        
        // Create directory if needed
        if (!Files.isDirectory(rootDir)) {
            try {
                Files.createDirectories(rootDir);
            } catch (IOException e) {
                TheVirusBlock.LOGGER.error("Failed to create profiles directory", e);
            }
            return;
        }
        
        // Load profiles from ROOT (can override bundled SERVER profiles)
        TheVirusBlock.LOGGER.info("Loading config override profiles from: {}", rootDir);
        try (Stream<Path> files = Files.list(rootDir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(file -> {
                    TheVirusBlock.LOGGER.debug("Loading config profile: {}", file);
                    loadProfileFromPath(file, ProfileSource.SERVER); // Use SERVER source to override bundled
                });
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to scan root profiles", e);
        }
        
        // Load profiles from LOCAL subfolder (user's own profiles)
        Path localDir = rootDir.resolve("local");
        if (!Files.isDirectory(localDir)) {
            try {
                Files.createDirectories(localDir);
            } catch (IOException e) {
                TheVirusBlock.LOGGER.debug("Could not create local directory");
            }
            return;
        }
        
        try (Stream<Path> files = Files.list(localDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadLocalProfile);
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to scan local profiles", e);
        }
    }
    
    private void loadLocalProfile(Path file) {
        try {
            String content = Files.readString(file);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            Profile profile = Profile.fromJson(json, ProfileSource.LOCAL);
            allProfiles.add(profile);
            TheVirusBlock.LOGGER.debug("Loaded local profile: {}", profile.name());
        } catch (Exception e) {
            TheVirusBlock.LOGGER.error("Failed to load local profile: {}", file, e);
        }
    }
    
    /**
     * Add server profiles (called when received from network).
     */
    public void addServerProfiles(List<Profile> profiles) {
        serverProfiles.clear();
        serverProfiles.addAll(profiles);
        
        // Refresh all profiles list
        if (loaded) {
            allProfiles.removeIf(p -> p.source() == ProfileSource.SERVER);
            allProfiles.addAll(serverProfiles);
        }
    }
    
    /**
     * Set available server profile names (creates placeholder profiles).
     * Full profile data is fetched when user selects one.
     */
    public void setServerProfileNames(List<String> names) {
        serverProfiles.clear();
        for (String name : names) {
            Profile placeholder = Profile.builder()
                .id("server:" + name)
                .name(name)
                .description("Server profile - select to load")
                .source(ProfileSource.SERVER)
                .build();
            serverProfiles.add(placeholder);
        }
        
        // Refresh all profiles list
        if (loaded) {
            allProfiles.removeIf(p -> p.source() == ProfileSource.SERVER);
            allProfiles.addAll(serverProfiles);
        }
        
        TheVirusBlock.LOGGER.info("Set {} server profile names", names.size());
    }
    
    /**
     * Get all profiles (unfiltered).
     */
    public List<Profile> getAllProfiles() {
        ensureLoaded();
        return Collections.unmodifiableList(allProfiles);
    }
    
    /**
     * Filter profiles by source and category.
     * Pass null for any filter to skip that filter.
     */
    public List<Profile> filterProfiles(ProfileSource source, ProfileCategory category, String searchText) {
        ensureLoaded();
        
        return allProfiles.stream()
            .filter(p -> source == null || p.source() == source)
            .filter(p -> category == null || p.category() == category)
            .filter(p -> searchText == null || searchText.isEmpty() || matchesSearch(p, searchText))
            .sorted(Comparator
                .comparing((Profile p) -> p.source().ordinal())
                .thenComparing(Profile::name, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }
    
    private boolean matchesSearch(Profile profile, String searchText) {
        String lower = searchText.toLowerCase();
        return profile.name().toLowerCase().contains(lower) ||
               profile.description().toLowerCase().contains(lower) ||
               profile.tags().stream().anyMatch(t -> t.toLowerCase().contains(lower));
    }
    
    /**
     * Get profiles grouped by source.
     */
    public Map<ProfileSource, List<Profile>> getProfilesBySource() {
        ensureLoaded();
        return allProfiles.stream()
            .collect(Collectors.groupingBy(Profile::source));
    }
    
    /**
     * Get a profile by ID.
     */
    public Optional<Profile> getProfile(String id) {
        ensureLoaded();
        return allProfiles.stream()
            .filter(p -> p.id().equals(id))
            .findFirst();
    }
    
    /**
     * Get a profile by name (case-insensitive).
     */
    public Optional<Profile> getProfileByName(String name) {
        ensureLoaded();
        return allProfiles.stream()
            .filter(p -> p.name().equalsIgnoreCase(name))
            .findFirst();
    }
    
    /**
     * Save a local profile.
     */
    public void saveProfile(Profile profile) {
        if (profile.source() != ProfileSource.LOCAL) {
            TheVirusBlock.LOGGER.warn("Cannot save non-local profile: {}", profile.name());
            return;
        }
        
        Path file = getLocalProfileDir().resolve(profile.id() + ".json");
        try {
            Files.writeString(file, GSON.toJson(profile.toJson()));
            TheVirusBlock.LOGGER.info("Saved profile: {}", profile.name());
            
            // Update in-memory list
            allProfiles.removeIf(p -> p.id().equals(profile.id()));
            allProfiles.add(profile);
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to save profile: {}", profile.name(), e);
        }
    }
    
    /**
     * Delete a local profile.
     */
    public boolean deleteProfile(String id) {
        Profile profile = getProfile(id).orElse(null);
        if (profile == null || profile.source() != ProfileSource.LOCAL) {
            TheVirusBlock.LOGGER.warn("Cannot delete non-local or missing profile: {}", id);
            return false;
        }
        
        Path file = getLocalProfileDir().resolve(id + ".json");
        try {
            Files.deleteIfExists(file);
            allProfiles.removeIf(p -> p.id().equals(id));
            TheVirusBlock.LOGGER.info("Deleted profile: {}", id);
            return true;
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("Failed to delete profile: {}", id, e);
            return false;
        }
    }
    
    /**
     * Delete a local profile by name.
     */
    public boolean deleteProfileByName(String name) {
        Profile profile = getProfileByName(name).orElse(null);
        if (profile == null) {
            TheVirusBlock.LOGGER.warn("Cannot delete - profile not found: {}", name);
            return false;
        }
        if (profile.source() != ProfileSource.LOCAL) {
            TheVirusBlock.LOGGER.warn("Cannot delete non-local profile: {} (source={})", name, profile.source());
            return false;
        }
        return deleteProfile(profile.id());
    }
    
    /**
     * Rename a local profile.
     */
    public boolean renameProfile(String oldId, String newId, String newName) {
        Profile profile = getProfile(oldId).orElse(null);
        if (profile == null || profile.source() != ProfileSource.LOCAL) {
            return false;
        }
        
        // Delete old file
        deleteProfile(oldId);
        
        // Save with new name
        Profile renamed = profile.withName(newName, newId);
        saveProfile(renamed);
        
        return true;
    }
    
    /**
     * Duplicate a profile (always creates local copy).
     */
    public Profile duplicateProfile(String id, String newName) {
        Profile original = getProfile(id).orElse(null);
        if (original == null) {
            return null;
        }
        
        String newId = newName.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis();
        Profile copy = original.withName(newName, newId);
        saveProfile(copy);
        
        return copy;
    }
    
    /**
     * Restore a bundled profile to its original state.
     * This copies the original bundled version to local storage, overwriting any local changes.
     * 
     * @param bundledId The ID of the bundled profile to restore (e.g., "default")
     * @return true if restoration succeeded
     */
    public boolean restoreBundledProfile(String bundledId) {
        if (bundledId == null || bundledId.isEmpty()) {
            TheVirusBlock.LOGGER.error("Cannot restore profile: empty bundled ID");
            return false;
        }
        
        // Look for the bundled profile in config/field_profiles/personal/
        Path personalDir = getProfilesRootDir().resolve("personal");
        Path profileFile = personalDir.resolve(bundledId + ".json");
        
        if (!Files.exists(profileFile)) {
            // Try with different naming conventions
            try (Stream<Path> files = Files.list(personalDir)) {
                profileFile = files
                    .filter(p -> p.getFileName().toString().replace(".json", "")
                        .equalsIgnoreCase(bundledId.replace("_", "-")))
                    .findFirst()
                    .orElse(null);
            } catch (IOException e) {
                TheVirusBlock.LOGGER.debug("Error scanning personal profiles: {}", e.getMessage());
            }
        }
        
        if (profileFile == null || !Files.exists(profileFile)) {
            TheVirusBlock.LOGGER.warn("Bundled profile '{}' not found in personal directory", bundledId);
            return false;
        }
        
        // Read the bundled profile and use it as the source of truth
        try {
            String content = Files.readString(profileFile);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            
            TheVirusBlock.LOGGER.info("Restored bundled profile '{}' from: {}", bundledId, profileFile);
            return true;
            
        } catch (Exception e) {
            TheVirusBlock.LOGGER.error("Error reading bundled profile: {}", profileFile, e);
            return false;
        }
    }
    
    /**
     * Export a profile to a file.
     */
    public void exportProfile(String id, Path destination) throws IOException {
        Profile profile = getProfile(id).orElseThrow(() -> 
            new IOException("Profile not found: " + id));
        Files.writeString(destination, GSON.toJson(profile.toJson()));
    }
    
    /**
     * Import a profile from a file.
     */
    public Profile importProfile(Path source) throws IOException {
        String content = Files.readString(source);
        JsonObject json = GSON.fromJson(content, JsonObject.class);
        
        // Generate unique ID
        String baseName = source.getFileName().toString().replace(".json", "");
        String id = baseName + "_" + System.currentTimeMillis();
        json.addProperty("id", id);
        
        Profile profile = Profile.fromJson(json, ProfileSource.LOCAL);
        saveProfile(profile);
        
        return profile;
    }
    
    private Path getLocalProfileDir() {
        return getProfilesRootDir().resolve("local");
    }
    
    /**
     * Get the root profiles directory: config/the-virus-block/field_profiles/
     */
    private Path getProfilesRootDir() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve("the-virus-block")
            .resolve("field_profiles");
    }
    
    private void ensureLoaded() {
        if (!loaded) {
            loadAll();
        }
    }
    
    /**
     * Force reload all profiles.
     */
    public void reload() {
        loaded = false;
        loadAll();
    }
}
