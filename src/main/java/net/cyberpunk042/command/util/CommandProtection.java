package net.cyberpunk042.command.util;


import net.cyberpunk042.log.Logging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Command protection system - controls which commands are blacklisted or untouchable.
 * 
 * <p>Protection levels:
 * <ul>
 *   <li><b>Blacklisted</b>: Command executes but shows a warning with the default value</li>
 *   <li><b>Untouchable</b>: Command is completely blocked, shows error with default value</li>
 * </ul>
 * 
 * <p>Configuration is loaded from: config/the-virus-block/command_protection.json
 */
public final class CommandProtection {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "command_protection.json";
    
    // Runtime state
    private static final Set<String> untouchable = new HashSet<>();
    private static final Map<String, String> blacklisted = new HashMap<>(); // path -> reason
    
    private CommandProtection() {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Loading
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Reload configuration from the default config directory.
     */
    public static void reload() {
        Path configRoot = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir()
            .resolve("the-virus-block");
        load(configRoot);
    }
    
    /**
     * Load protection configuration from the config directory.
     */
    public static void load(Path configRoot) {
        Path configFile = configRoot.resolve(CONFIG_FILE);
        
        if (!Files.exists(configFile)) {
            writeDefaultConfig(configFile);
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            parseConfig(json);
            Logging.COMMANDS.info("[CommandProtection] Loaded {} blacklisted, {} untouchable commands",
                blacklisted.size(), untouchable.size());
        } catch (IOException e) {
            Logging.COMMANDS.error("[CommandProtection] Failed to load config: {}", e.getMessage());
        }
    }
    
    private static void parseConfig(JsonObject json) {
        untouchable.clear();
        blacklisted.clear();
        
        // Parse untouchable array
        if (json.has("untouchable") && json.get("untouchable").isJsonArray()) {
            for (JsonElement elem : json.getAsJsonArray("untouchable")) {
                if (elem.isJsonPrimitive()) {
                    untouchable.add(elem.getAsString());
                }
            }
        }
        
        // Parse blacklisted array (can be string or object with reason)
        if (json.has("blacklisted") && json.get("blacklisted").isJsonArray()) {
            for (JsonElement elem : json.getAsJsonArray("blacklisted")) {
                if (elem.isJsonPrimitive()) {
                    blacklisted.put(elem.getAsString(), null);
                } else if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    String path = obj.has("path") ? obj.get("path").getAsString() : null;
                    String reason = obj.has("reason") ? obj.get("reason").getAsString() : null;
                    if (path != null) {
                        blacklisted.put(path, reason);
                    }
                }
            }
        }
    }
    
    private static void writeDefaultConfig(Path configFile) {
        JsonObject json = new JsonObject();
        
        // Example blacklisted
        JsonArray blacklistedArr = new JsonArray();
        JsonObject example = new JsonObject();
        example.addProperty("path", "erosion.native_fill");
        example.addProperty("reason", "May cause lag on large collapses");
        blacklistedArr.add(example);
        json.add("blacklisted", blacklistedArr);
        
        // Example untouchable
        JsonArray untouchableArr = new JsonArray();
        // Empty by default
        json.add("untouchable", untouchableArr);
        
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(json, writer);
            }
            Logging.COMMANDS.info("[CommandProtection] Created default config at {}", configFile);
        } catch (IOException e) {
            Logging.COMMANDS.error("[CommandProtection] Failed to write default config: {}", e.getMessage());
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Check if a command path is untouchable (completely blocked).
     */
    public static boolean isUntouchable(String path) {
        return path != null && untouchable.contains(path);
    }
    
    /**
     * Check if a command path is blacklisted (warns but executes).
     */
    public static boolean isBlacklisted(String path) {
        return path != null && blacklisted.containsKey(path);
    }
    
    /**
     * Get the blacklist reason for a path, if any.
     */
    public static Optional<String> getBlacklistReason(String path) {
        if (path == null || !blacklisted.containsKey(path)) {
            return Optional.empty();
        }
        String reason = blacklisted.get(path);
        return Optional.ofNullable(reason);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Execution helpers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Check protection and send appropriate feedback.
     * @return true if command should proceed, false if blocked
     */
    public static boolean checkAndWarn(ServerCommandSource source, String path) {
        if (path == null) return true;
        
        // Untouchable = block
        if (isUntouchable(path)) {
            String defaultVal = CommandKnobDefaults.format(path);
            source.sendFeedback(() -> Text.literal("🔒 This setting is locked. Default: " + defaultVal)
                .formatted(Formatting.RED), false);
            return false;
        }
        
        // Blacklisted = warn but allow
        if (isBlacklisted(path)) {
            String defaultVal = CommandKnobDefaults.format(path);
            String reason = getBlacklistReason(path).orElse("This setting is flagged for caution");
            source.sendFeedback(() -> Text.literal("⚠ " + reason + " (default: " + defaultVal + ")")
                .formatted(Formatting.YELLOW), false);
        }
        
        return true;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Server start audit
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Audit registered defaults at startup (no live config comparison).
     * Just logs which paths are protected.
     */
    public static void auditDeviations() {
        if (untouchable.isEmpty() && blacklisted.isEmpty()) {
            Logging.COMMANDS.info("[CommandProtection] No protected commands configured");
            return;
        }
        
        if (!untouchable.isEmpty()) {
            Logging.COMMANDS.info("[CommandProtection] Untouchable commands: {}", untouchable);
        }
        if (!blacklisted.isEmpty()) {
            Logging.COMMANDS.info("[CommandProtection] Blacklisted commands: {}", blacklisted.keySet());
        }
    }
    
    /**
     * Audit and log any deviant configurations on server start.
     * Call this after the world is loaded and configs are applied.
     */
    public static void auditDeviations(java.util.function.Function<String, Object> currentValueProvider) {
        List<String> deviations = new ArrayList<>();
        
        for (String path : CommandKnobDefaults.paths()) {
            Object defaultVal = CommandKnobDefaults.get(path);
            Object currentVal = currentValueProvider.apply(path);
            
            if (currentVal != null && !Objects.equals(defaultVal, currentVal)) {
                String status = isUntouchable(path) ? "UNTOUCHABLE" 
                    : isBlacklisted(path) ? "BLACKLISTED" : "";
                deviations.add(String.format("  %s = %s (default: %s) %s",
                    path, currentVal, defaultVal, status));
            }
        }
        
        if (deviations.isEmpty()) {
            Logging.COMMANDS.info("[CommandProtection] All settings at defaults");
        } else {
            Logging.COMMANDS.warn("[CommandProtection] Configuration deviations:");
            for (String line : deviations) {
                Logging.COMMANDS.warn(line);
            }
        }
    }
}
