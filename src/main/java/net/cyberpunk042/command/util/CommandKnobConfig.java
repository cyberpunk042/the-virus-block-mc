package net.cyberpunk042.command.util;


import net.cyberpunk042.log.Logging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global configuration for the CommandKnob system.
 * 
 * <p>Loaded from: {@code config/the-virus-block/command_knob_config.json}
 */
public final class CommandKnobConfig {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("the-virus-block")
        .resolve("command_knob_config.json");
    
    // Singleton
    private static CommandKnobConfig INSTANCE;
    
    public static CommandKnobConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }
    
    public static void reload() {
        INSTANCE = load();
        logStartupStatus();
    }
    
    // Config Fields
    
    /**
     * When true, removes min/max validation on all config knobs.
     * 
     * <p><b>WARNING:</b> This can cause crashes, undefined behavior, and
     * corrupt save files. Only enable if you know what you're doing!
     * 
     * <p>Affected systems:
     * <ul>
     *   <li>Protection anti-virus field configurations</li>
     *   <li>Personal field configurations</li>
     *   <li>Field meshes and styles</li>
     *   <li>Field profiles</li>
     *   <li>Growth block scale limits</li>
     *   <li>All other bounded value knobs</li>
     * </ul>
     */
    public boolean removeConfigLimiter = false;
    
    /**
     * Optional: reason for enabling limiter removal (for audit logs).
     */
    public String limiterRemovedReason = "";
    
    // Convenience Methods
    
    /**
     * Check if config limiters are removed (unsafe mode).
     */
    public static boolean isLimiterRemoved() {
        return get().removeConfigLimiter;
    }
    
    /**
     * Get effective min value for a range.
     * Returns Integer.MIN_VALUE if limiter is removed.
     */
    public static int effectiveMin(int normalMin) {
        return isLimiterRemoved() ? Integer.MIN_VALUE : normalMin;
    }
    
    /**
     * Get effective max value for a range.
     * Returns Integer.MAX_VALUE if limiter is removed.
     */
    public static int effectiveMax(int normalMax) {
        return isLimiterRemoved() ? Integer.MAX_VALUE : normalMax;
    }
    
    /**
     * Check if a value exceeds normal limits.
     */
    public static boolean exceedsNormalLimits(int value, int normalMin, int normalMax) {
        return value < normalMin || value > normalMax;
    }
    
    // Loading & Saving
    
    private static CommandKnobConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                CommandKnobConfig config = GSON.fromJson(json, CommandKnobConfig.class);
                if (config != null) {
                    return config;
                }
            }
        } catch (IOException e) {
            Logging.COMMANDS.error("[CommandKnob] Failed to load config: {}", e.getMessage());
        }
        
        // Create default
        CommandKnobConfig config = new CommandKnobConfig();
        save(config);
        return config;
    }
    
    private static void save(CommandKnobConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            Logging.COMMANDS.error("[CommandKnob] Failed to save config: {}", e.getMessage());
        }
    }
    
    // Startup Logging
    
    /**
     * Call on server/client start to log warnings.
     */
    public static void logStartupStatus() {
        if (!isLimiterRemoved()) {
            return;
        }
        
        String reason = get().limiterRemovedReason;
        String reasonText = reason.isEmpty() ? "(no reason provided)" : reason;
        
        Logging.COMMANDS.warn("================================================================================");
        Logging.COMMANDS.warn("  !!!  CONFIG LIMITER REMOVED - UNSAFE MODE ENABLED  !!!");
        Logging.COMMANDS.warn("================================================================================");
        Logging.COMMANDS.warn("  All config value limits have been DISABLED.");
        Logging.COMMANDS.warn("");
        Logging.COMMANDS.warn("  This can cause:");
        Logging.COMMANDS.warn("    - Server crashes");
        Logging.COMMANDS.warn("    - Client crashes");
        Logging.COMMANDS.warn("    - Corrupted save files");
        Logging.COMMANDS.warn("    - Undefined behavior");
        Logging.COMMANDS.warn("    - Memory exhaustion");
        Logging.COMMANDS.warn("");
        Logging.COMMANDS.warn("  Reason: {}", reasonText);
        Logging.COMMANDS.warn("");
        Logging.COMMANDS.warn("  To disable: set removeConfigLimiter=false in:");
        Logging.COMMANDS.warn("  config/the-virus-block/command_knob_config.json");
        Logging.COMMANDS.warn("================================================================================");
    }
    
    /**
     * Send warning to player when they use an out-of-bounds value.
     */
    public static void warnPlayerUnsafeValue(
            net.minecraft.server.command.ServerCommandSource source,
            String knobName,
            int value,
            int normalMin,
            int normalMax
    ) {
        if (!exceedsNormalLimits(value, normalMin, normalMax)) {
            return;
        }
        
        String range = "[" + normalMin + " - " + normalMax + "]";
        CommandFeedback.warning(source, 
            "UNSAFE VALUE: " + knobName + " = " + value + " is outside normal range " + range);
        CommandFeedback.warning(source,
            "This may cause crashes or undefined behavior!");
    }
}
