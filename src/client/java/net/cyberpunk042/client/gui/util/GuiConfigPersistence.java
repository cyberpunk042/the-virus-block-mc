package net.cyberpunk042.client.gui.util;

import net.cyberpunk042.client.gui.screen.TabType;
import net.cyberpunk042.client.gui.layout.GuiMode;
import net.cyberpunk042.log.Logging;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Handles persistence of GUI preferences (mode, tabs, etc.).
 * 
 * <p>Preferences are stored in the Fabric config directory at:
 * {@code config/the-virus-block/gui_preferences.properties}</p>
 * 
 * <h2>Stored Preferences</h2>
 * <ul>
 *   <li>{@code gui.mode} - FULLSCREEN or WINDOWED</li>
 *   <li>{@code gui.tab} - The main tab (QUICK, ADVANCED, DEBUG, PROFILES)</li>
 *   <li>{@code gui.subtab.quick} - Subtab index for QUICK tab</li>
 *   <li>{@code gui.subtab.advanced} - Subtab index for ADVANCED tab</li>
 *   <li>{@code gui.subtab.debug} - Subtab index for DEBUG tab</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Load saved mode/tab
 * GuiMode mode = GuiConfigPersistence.loadSavedMode();
 * TabType tab = GuiConfigPersistence.loadSavedTab();
 * 
 * // Save when changed
 * GuiConfigPersistence.saveMode(GuiMode.WINDOWED);
 * GuiConfigPersistence.saveTab(TabType.ADVANCED);
 * </pre>
 */
public final class GuiConfigPersistence {
    
    private GuiConfigPersistence() {}
    
    /**
     * Path to the GUI preferences file.
     */
    private static final Path GUI_CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("the-virus-block")
        .resolve("gui_preferences.properties");
    
    // =========================================================================
    // Mode Persistence
    // =========================================================================
    
    /**
     * Loads the saved GUI mode from preferences.
     * 
     * @return The saved mode, or {@link GuiMode#FULLSCREEN} if not found or error
     */
    public static GuiMode loadSavedMode() {
        Properties props = loadProperties();
        String modeStr = props.getProperty("gui.mode", "FULLSCREEN");
        try {
            return GuiMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            return GuiMode.FULLSCREEN;
        }
    }
    
    /**
     * Saves the current GUI mode to preferences.
     * 
     * @param mode The mode to save
     */
    public static void saveMode(GuiMode mode) {
        saveProperty("gui.mode", mode.name());
    }
    
    // =========================================================================
    // Tab Persistence
    // =========================================================================
    
    /**
     * Loads the saved main tab from preferences.
     * 
     * @return The saved tab, or {@link TabType#QUICK} if not found or error
     */
    public static TabType loadSavedTab() {
        Properties props = loadProperties();
        String tabStr = props.getProperty("gui.tab", "QUICK");
        try {
            return TabType.valueOf(tabStr);
        } catch (IllegalArgumentException e) {
            return TabType.QUICK;
        }
    }
    
    /**
     * Saves the current main tab to preferences.
     * 
     * @param tab The tab to save
     */
    public static void saveTab(TabType tab) {
        saveProperty("gui.tab", tab.name());
    }
    
    /**
     * Loads the saved subtab index for a given main tab.
     * 
     * @param mainTab The main tab to load subtab for
     * @return The saved subtab index, or 0 if not found
     */
    public static int loadSavedSubtab(TabType mainTab) {
        Properties props = loadProperties();
        String key = "gui.subtab." + mainTab.name().toLowerCase();
        String indexStr = props.getProperty(key, "0");
        try {
            return Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Saves the current subtab index for a given main tab.
     * 
     * @param mainTab The main tab
     * @param subtabIndex The subtab index to save
     */
    public static void saveSubtab(TabType mainTab, int subtabIndex) {
        String key = "gui.subtab." + mainTab.name().toLowerCase();
        saveProperty(key, String.valueOf(subtabIndex));
    }
    
    // =========================================================================
    // Profile Persistence
    // =========================================================================
    
    /**
     * Loads the saved current profile name from preferences.
     * 
     * @return The saved profile name, or "default" if not found
     */
    public static String loadSavedProfile() {
        Properties props = loadProperties();
        return props.getProperty("profile.current", "default");
    }
    
    /**
     * Saves the current profile name to preferences.
     * 
     * @param profileName The profile name to save
     */
    public static void saveCurrentProfile(String profileName) {
        saveProperty("profile.current", profileName != null ? profileName : "default");
    }
    
    // =========================================================================
    // Internal Helpers
    // =========================================================================
    
    /**
     * Loads all properties from the config file.
     * 
     * @return Properties object (may be empty if file doesn't exist)
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        try {
            if (Files.exists(GUI_CONFIG_PATH)) {
                try (var reader = Files.newBufferedReader(GUI_CONFIG_PATH)) {
                    props.load(reader);
                }
            }
        } catch (Exception e) {
            Logging.GUI.topic("config").warn("Failed to load GUI preferences: {}", e.getMessage());
        }
        return props;
    }
    
    /**
     * Saves a single property, preserving existing properties.
     * 
     * @param key Property key
     * @param value Property value
     */
    private static void saveProperty(String key, String value) {
        try {
            Files.createDirectories(GUI_CONFIG_PATH.getParent());
            
            // Load existing properties
            Properties props = loadProperties();
            
            // Update/add the property
            props.setProperty(key, value);
            
            // Save all properties
            try (var writer = Files.newBufferedWriter(GUI_CONFIG_PATH)) {
                props.store(writer, "GUI Preferences");
            }
        } catch (IOException e) {
            Logging.GUI.topic("config").warn("Failed to save GUI preference {}: {}", key, e.getMessage());
        }
    }
    
    /**
     * Returns the path to the GUI config file.
     */
    public static Path getConfigPath() {
        return GUI_CONFIG_PATH;
    }
}
