package net.cyberpunk042.log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.cyberpunk042.TheVirusBlock;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Configuration management for the logging system.
 * Loads/saves logs.json from the mod config directory.
 * 
 * <p>Config is regenerated when version changes to ensure all channels are present.
 */
public final class LogConfig {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "logs.json";
    
    // Increment this whenever channels are added/removed to force config regeneration
    private static final int CONFIG_VERSION = 2;
    
    private static volatile boolean chatEnabled = true;
    private static volatile ChatRecipients chatRecipients = ChatRecipients.OPS;
    private static volatile int chatRateLimit = 10;
    
    public static boolean chatEnabled() { return chatEnabled; }
    public static ChatRecipients chatRecipients() { return chatRecipients; }
    public static int chatRateLimit() { return chatRateLimit; }
    
    public static void setChatEnabled(boolean value) { chatEnabled = value; }
    public static void setChatRecipients(ChatRecipients value) { chatRecipients = value; }
    public static void setChatRateLimit(int value) { chatRateLimit = value; }
    
    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(TheVirusBlock.MOD_ID);
        Path file = configDir.resolve(FILE_NAME);
        
        if (!Files.exists(file)) {
            save(); // Create default
            return;
        }
        
        try {
            String content = Files.readString(file);
            JsonObject root = GSON.fromJson(content, JsonObject.class);
            
            // Check version - regenerate if outdated
            int fileVersion = root.has("_version") ? root.get("_version").getAsInt() : 0;
            if (fileVersion < CONFIG_VERSION) {
                TheVirusBlock.LOGGER.info("[LogConfig] Config version {} is outdated (current: {}), regenerating with all channels",
                    fileVersion, CONFIG_VERSION);
                save();
                return;
            }
            
            // Load channels
            if (root.has("channels")) {
                JsonObject channels = root.getAsJsonObject("channels");
                for (Map.Entry<String, JsonElement> entry : channels.entrySet()) {
                    Channel ch = Logging.channel(entry.getKey());
                    if (ch == null) continue;
                    
                    JsonObject chConfig = entry.getValue().getAsJsonObject();
                    if (chConfig.has("level")) {
                        ch.setLevel(LogLevel.parse(chConfig.get("level").getAsString()));
                    }
                    if (chConfig.has("chat")) {
                        ch.setChatForward(chConfig.get("chat").getAsBoolean());
                    }
                    if (chConfig.has("topics")) {
                        JsonObject topics = chConfig.getAsJsonObject("topics");
                        for (Map.Entry<String, JsonElement> topicEntry : topics.entrySet()) {
                            ch.setTopicLevel(topicEntry.getKey(), 
                                LogLevel.parse(topicEntry.getValue().getAsString()));
                        }
                    }
                }
            }
            
            // Load chat settings
            if (root.has("chat")) {
                JsonObject chat = root.getAsJsonObject("chat");
                if (chat.has("enabled")) chatEnabled = chat.get("enabled").getAsBoolean();
                if (chat.has("recipients")) chatRecipients = ChatRecipients.parse(chat.get("recipients").getAsString());
                if (chat.has("rateLimit")) chatRateLimit = chat.get("rateLimit").getAsInt();
            }
            
            // Load watchdog settings
            if (root.has("watchdog")) {
                JsonObject wd = root.getAsJsonObject("watchdog");
                if (wd.has("enabled")) LogWatchdog.setEnabled(wd.get("enabled").getAsBoolean());
                if (wd.has("perSecond") && wd.has("perMinute")) {
                    LogWatchdog.setThresholds(wd.get("perSecond").getAsInt(), wd.get("perMinute").getAsInt());
                }
            }
            
            TheVirusBlock.LOGGER.info("[LogConfig] Loaded logging configuration");
            
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("[LogConfig] Failed to load logs.json", e);
        }
    }
    
    public static void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(TheVirusBlock.MOD_ID);
        Path file = configDir.resolve(FILE_NAME);
        
        try {
            Files.createDirectories(configDir);
            
            JsonObject root = new JsonObject();
            
            // Save version for upgrade detection
            root.addProperty("_version", CONFIG_VERSION);
            
            // Save channels
            JsonObject channels = new JsonObject();
            for (Channel ch : Logging.channels()) {
                JsonObject chConfig = new JsonObject();
                chConfig.addProperty("level", ch.level().name());
                chConfig.addProperty("chat", ch.chatForward());
                channels.add(ch.id(), chConfig);
            }
            root.add("channels", channels);
            
            // Save chat settings
            JsonObject chat = new JsonObject();
            chat.addProperty("enabled", chatEnabled);
            chat.addProperty("recipients", chatRecipients.name().toLowerCase());
            chat.addProperty("rateLimit", chatRateLimit);
            root.add("chat", chat);
            
            // Save watchdog settings
            JsonObject wd = new JsonObject();
            wd.addProperty("enabled", LogWatchdog.isEnabled());
            wd.addProperty("perSecond", LogWatchdog.perSecond());
            wd.addProperty("perMinute", LogWatchdog.perMinute());
            root.add("watchdog", wd);
            
            Files.writeString(file, GSON.toJson(root));
            TheVirusBlock.LOGGER.info("[LogConfig] Saved logging configuration");
            
        } catch (IOException e) {
            TheVirusBlock.LOGGER.error("[LogConfig] Failed to save logs.json", e);
        }
    }
    
    private LogConfig() {}
}
