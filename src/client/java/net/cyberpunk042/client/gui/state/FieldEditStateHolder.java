package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.log.Logging;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import net.cyberpunk042.client.network.GuiPacketSender;

/**
 * Client-side singleton holder for FieldEditState.
 * 
 * <p>This allows:</p>
 * <ul>
 *   <li>Commands to update state even when GUI is closed</li>
 *   <li>Test field renderer to read state</li>
 *   <li>GUI to access/create state</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class FieldEditStateHolder {
    
    private static FieldEditState current = null;
    private static boolean testFieldActive = false;
    
    private FieldEditStateHolder() {}
    
    /**
     * Gets the current editing state, creating if needed.
     * Loads the last used profile from preferences if creating a new state.
     */
    public static FieldEditState getOrCreate() {
        if (current == null) {
            current = new FieldEditState();
            Logging.GUI.topic("state").debug("Created new FieldEditState");
            
            // Try to load the last used profile
            tryLoadSavedProfile();
        }
        return current;
    }
    
    /**
     * Attempts to load the saved profile from preferences.
     * This populates the FieldEditState with the user's last used profile.
     */
    private static void tryLoadSavedProfile() {
        if (current == null) return;
        
        try {
            String profileName = net.cyberpunk042.client.gui.util.GuiConfigPersistence.loadSavedProfile();
            if (profileName == null || profileName.isEmpty()) {
                profileName = "default";
            }
            
            // Try to find and load the profile (including "default" if it's been saved locally)
            var profileManager = net.cyberpunk042.client.profile.ProfileManager.getInstance();
            var profileOpt = profileManager.getProfile(profileName);
            
            if (profileOpt.isPresent()) {
                var profile = profileOpt.get();
                if (profile.definition() != null) {
                    // Load the profile's definition into the current state
                    current.loadFromDefinition(profile.definition());
                    current.setCurrentProfile(profileName, false);
                    current.clearDirty();
                    Logging.GUI.topic("state").info("Loaded saved profile: {}", profileName);
                    return;
                } else {
                    Logging.GUI.topic("state").warn("Profile '{}' has no definition, using defaults", profileName);
                }
            } else {
                Logging.GUI.topic("state").debug("Saved profile '{}' not found, using defaults", profileName);
            }
            
            // If we get here, use programmatic defaults
            Logging.GUI.topic("state").debug("Using programmatic default profile");
            
        } catch (Exception e) {
            Logging.GUI.topic("state").error("Failed to load saved profile", e);
        }
    }
    
    /**
     * Gets the current editing state, or null if none exists.
     */
    @Nullable
    public static FieldEditState get() {
        return current;
    }
    
    /**
     * Sets the current editing state.
     */
    public static void set(FieldEditState state) {
        current = state;
        Logging.GUI.topic("state").debug("FieldEditState set");
    }
    
    /**
     * Clears the current editing state.
     * Called when GUI is closed and no test field is active.
     */
    public static void clear() {
        if (!testFieldActive) {
            current = null;
            Logging.GUI.topic("state").debug("FieldEditState cleared");
        }
    }
    
    /**
     * Resets state to defaults.
     */
    public static void reset() {
        current = new FieldEditState();
        Logging.GUI.topic("state").debug("FieldEditState reset to defaults");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEST FIELD CONTROL
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if test field is currently active.
     */
    public static boolean isTestFieldActive() {
        return testFieldActive;
    }
    
    /**
     * Sets test field active state.
     */
    public static void setTestFieldActive(boolean active) {
        testFieldActive = active;
        Logging.GUI.topic("state").debug("Test field active: {}", active);
    }
    
    /**
     * Spawns the test field (client-side preview).
     */
    public static void spawnTestField() {
        testFieldActive = true;
        // Ensure state exists
        getOrCreate();
        Logging.GUI.topic("state").info("Test field spawned");
        GuiPacketSender.spawnDebugField(getOrCreate().toStateJson());
    }
    
    /**
     * Despawns the test field.
     */
    public static void despawnTestField() {
        testFieldActive = false;
        Logging.GUI.topic("state").info("Test field despawned");
        GuiPacketSender.despawnDebugField();
    }
    
    /**
     * Toggles test field visibility.
     */
    public static void toggleTestField() {
        if (testFieldActive) {
            despawnTestField();
        } else {
            spawnTestField();
        }
    }
}

