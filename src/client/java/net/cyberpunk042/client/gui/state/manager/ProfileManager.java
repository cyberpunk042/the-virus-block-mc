package net.cyberpunk042.client.gui.state.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.client.gui.state.ChangeType;
import net.cyberpunk042.client.gui.state.FieldEditState;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages profile operations: save, load, snapshots, fragments.
 * 
 * <p>Extracted from FieldEditState to follow Single Responsibility Principle.</p>
 */
public class ProfileManager extends AbstractManager {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE ENTRY
    // ═══════════════════════════════════════════════════════════════════════════
    
    public record ProfileEntry(
        String name, 
        boolean isServer, 
        String description,
        net.cyberpunk042.field.category.ProfileSource source
    ) {
        public ProfileEntry(String name, boolean isServer) {
            this(name, isServer, "", isServer 
                ? net.cyberpunk042.field.category.ProfileSource.SERVER 
                : net.cyberpunk042.field.category.ProfileSource.LOCAL);
        }
        
        public ProfileEntry(String name, boolean isServer, String description) {
            this(name, isServer, description, isServer 
                ? net.cyberpunk042.field.category.ProfileSource.SERVER 
                : net.cyberpunk042.field.category.ProfileSource.LOCAL);
        }
        
        public boolean isBundled() {
            return source == net.cyberpunk042.field.category.ProfileSource.BUNDLED;
        }
        
        public boolean isLocal() {
            return source == net.cyberpunk042.field.category.ProfileSource.LOCAL;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final List<ProfileEntry> profiles = new ArrayList<>();
    private String currentProfileName = "default";
    private boolean currentProfileServer = false;
    private String snapshotJson = null;
    
    public ProfileManager(FieldEditState state) {
        super(state);
        // Sync with file-based ProfileManager to load profile list from disk
        syncFromFileProfileManager();
    }
    
    /**
     * Sync the profile list from the file-based ProfileManager.
     * This loads all profiles from disk (bundled + local + server).
     */
    public void syncFromFileProfileManager() {
        profiles.clear();
        
        var fileProfileManager = net.cyberpunk042.client.profile.ProfileManager.getInstance();
        var allFromFile = fileProfileManager.getAllProfiles();
        
        for (net.cyberpunk042.field.profile.Profile p : allFromFile) {
            boolean isServer = p.source() == net.cyberpunk042.field.category.ProfileSource.SERVER;
            String desc = p.description() != null ? p.description() : "";
            profiles.add(new ProfileEntry(p.name(), isServer, desc, p.source()));
        }
        
        // Ensure default profile exists
        if (profiles.stream().noneMatch(p -> p.name().equalsIgnoreCase("default"))) {
            profiles.add(0, new ProfileEntry("default", false));
        }
        
        net.cyberpunk042.log.Logging.GUI.topic("state").debug("Synced {} profiles from file manager", profiles.size());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE LIST
    // ═══════════════════════════════════════════════════════════════════════════
    
    public List<ProfileEntry> getProfiles() { return profiles; }
    public String getCurrentName() { return currentProfileName; }
    public void setCurrentName(String name) { this.currentProfileName = name; }
    public boolean isCurrentServerSourced() { return currentProfileServer; }
    
    public void setCurrent(String name, boolean isServer) {
        this.currentProfileName = name;
        this.currentProfileServer = isServer;
    }
    
    public void updateServerProfiles(List<String> names) {
        profiles.removeIf(ProfileEntry::isServer);
        for (String name : names) {
            profiles.add(new ProfileEntry(name, true));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SNAPSHOTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void saveSnapshot() {
        this.snapshotJson = state.toStateJson();
    }
    
    public void restoreFromSnapshot() {
        if (snapshotJson != null) {
            state.fromStateJson(snapshotJson);
        }
    }
    
    public ProfileSnapshot getSnapshot() {
        return new ProfileSnapshot(snapshotJson);
    }
    
    public String getSnapshotJson() {
        return snapshotJson;
    }
    
    public void setSnapshot(String json) {
        this.snapshotJson = json;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE JSON
    // ═══════════════════════════════════════════════════════════════════════════
    
    public String toJson(String profileName) {
        JsonObject json = new JsonObject();
        json.addProperty("name", profileName);
        json.addProperty("version", "1.0");
        json.add("state", state.toJson());
        return json.toString();
    }
    
    public void fromJson(String jsonStr) {
        JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
        if (json.has("state")) {
            state.fromJson(json.getAsJsonObject("state"));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FRAGMENT NAMES (for UI display)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public String getCurrentShapeFragmentName() { return state.getShapeType(); }
    public String getCurrentFillFragmentName() { return state.fill().mode().name(); }
    public String getCurrentAnimationFragmentName() { return state.spin() != null && state.spin().isActive() ? "spin" : "none"; }
    
    /**
     * Factory reset the current profile to its original state.
     * 
     * <p>For the "default" profile, this resets to programmatic defaults
     * (SphereShape.DEFAULT, FillConfig.SOLID, etc.).</p>
     * 
     * <p>For other bundled profiles, this restores from the bundled asset file.</p>
     * 
     * @return true if factory reset succeeded
     */
    public boolean factoryReset() {
        // For "default" profile, reset to programmatic defaults
        if ("default".equals(currentProfileName)) {
            state.reset();  // Resets all adapters to their default values
            snapshotJson = null;
            return true;
        }
        
        // For other profiles, try to restore from bundled assets
        boolean restored = net.cyberpunk042.client.profile.ProfileManager.getInstance()
            .restoreBundledProfile(currentProfileName);
        
        if (restored) {
            // TODO: Load the restored profile into state
            // For now, just reset to defaults
            state.reset();
            snapshotJson = null;
            return true;
        }
        
        return false;
    }
    
    @Override
    public void reset() {
        currentProfileName = "default";
        currentProfileServer = false;
        snapshotJson = null;
    }
    
    /**
     * Loads state from a FieldDefinition (saved profile).
     * Replaces current layers and settings with those from the definition.
     */
    public void loadFromDefinition(net.cyberpunk042.field.FieldDefinition definition) {
        if (definition == null) return;
        
        net.cyberpunk042.log.Logging.GUI.topic("state").debug("Loading definition: {}", definition.id());
        
        // Delegate the actual loading to LayerManager (it has access to field layers)
        state.layers().loadFromDefinition(definition);
        
        // Load definition-level fields
        if (definition.modifiers() != null) {
            state.set("modifiers", definition.modifiers());
        }
        if (definition.follow() != null) {
            state.set("follow", definition.follow());
        }
        if (definition.beam() != null) {
            state.set("beam", definition.beam());
        }
        if (definition.lifecycle() != null) {
            state.set("lifecycle", definition.lifecycle());
        }
        
        net.cyberpunk042.log.Logging.GUI.topic("state").info("Loaded definition: {}", definition.id());
        
        // Notify listeners that profile was loaded (widgets should sync)
        state.notifyStateChanged(ChangeType.PROFILE_LOADED);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE SNAPSHOT (nested class for network sync)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static class ProfileSnapshot {
        private final JsonObject json;
        
        public ProfileSnapshot(String jsonStr) {
            this.json = (jsonStr != null && !jsonStr.isEmpty()) 
                ? JsonParser.parseString(jsonStr).getAsJsonObject() 
                : new JsonObject();
        }
        
        public float radius() { return json.has("radius") ? json.get("radius").getAsFloat() : 3.0f; }
        public int latSteps() { return json.has("sphere") && json.getAsJsonObject("sphere").has("latSteps") 
            ? json.getAsJsonObject("sphere").get("latSteps").getAsInt() : 16; }
        public int lonSteps() { return json.has("sphere") && json.getAsJsonObject("sphere").has("lonSteps") 
            ? json.getAsJsonObject("sphere").get("lonSteps").getAsInt() : 32; }
        public int maskCount() { return json.has("mask") && json.getAsJsonObject("mask").has("count") 
            ? json.getAsJsonObject("mask").get("count").getAsInt() : 1; }
    }
}
