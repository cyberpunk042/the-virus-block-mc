package net.cyberpunk042.client.gui.config;

import net.cyberpunk042.log.Logging;

/**
 * G39-G40: Client-side GUI configuration.
 */
public final class GuiConfig {
    
    private static GuiConfig instance = new GuiConfig();
    
    // G39: Basic config
    private int maxUndoSteps = 50;
    private boolean showTooltips = true;
    
    // G40: Advanced config  
    private boolean rememberTabState = true;
    private boolean debugMenuEnabled = false;
    
    private GuiConfig() {}
    
    public static GuiConfig get() {
        return instance;
    }
    
    // Getters
    public int getMaxUndoSteps() { return maxUndoSteps; }
    public boolean isShowTooltips() { return showTooltips; }
    public boolean isRememberTabState() { return rememberTabState; }
    public boolean isDebugMenuEnabled() { return debugMenuEnabled; }
    
    // Setters with logging
    public void setMaxUndoSteps(int value) {
        this.maxUndoSteps = Math.max(1, Math.min(value, 200));
        Logging.GUI.topic("config").debug("maxUndoSteps = {}", this.maxUndoSteps);
    }
    
    public void setShowTooltips(boolean value) {
        this.showTooltips = value;
        Logging.GUI.topic("config").debug("showTooltips = {}", value);
    }
    
    public void setRememberTabState(boolean value) {
        this.rememberTabState = value;
        Logging.GUI.topic("config").debug("rememberTabState = {}", value);
    }
    
    public void setDebugMenuEnabled(boolean value) {
        this.debugMenuEnabled = value;
        Logging.GUI.topic("config").debug("debugMenuEnabled = {}", value);
    }
    
    /**
     * Resets all config to defaults.
     */
    public void reset() {
        maxUndoSteps = 50;
        showTooltips = true;
        rememberTabState = true;
        debugMenuEnabled = false;
        Logging.GUI.topic("config").info("Config reset to defaults");
    }
}
