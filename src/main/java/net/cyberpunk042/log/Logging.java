package net.cyberpunk042.log;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static entry point for the logging system.
 * 
 * <p>Usage: {@code Logging.GROWTH.info("Message");}</p>
 * <p>With static import: {@code import static net.cyberpunk042.log.Logging.*;}</p>
 */
public final class Logging {
    
    private static final Map<String, Channel> CHANNELS = new LinkedHashMap<>();
    
    // ========== CHANNELS ==========
    
    public static final Channel SINGULARITY   = register(Channel.of("singularity",   "Singularity",   LogLevel.INFO));
    public static final Channel COLLAPSE      = register(Channel.of("collapse",      "Collapse",      LogLevel.INFO));
    public static final Channel FUSE          = register(Channel.of("fuse",          "Fuse",          LogLevel.INFO));
    public static final Channel CHUNKS        = register(Channel.of("chunks",        "Chunks",        LogLevel.INFO));
    public static final Channel GROWTH        = register(Channel.of("growth",        "Growth",        LogLevel.INFO));
    public static final Channel RENDER        = register(Channel.of("render",        "Render",        LogLevel.INFO));
    public static final Channel COLLISION     = register(Channel.of("collision",     "Collision",     LogLevel.INFO));
    public static final Channel PROFILER      = register(Channel.of("profiler",      "Profiler",      LogLevel.INFO));
    public static final Channel ORCHESTRATOR  = register(Channel.of("orchestrator",  "Orchestrator",  LogLevel.INFO));
    public static final Channel SCENARIO      = register(Channel.of("scenario",      "Scenario",      LogLevel.INFO));
    public static final Channel PHASE         = register(Channel.of("phase",         "Phase",         LogLevel.INFO));
    public static final Channel SCHEDULER     = register(Channel.of("scheduler",     "Scheduler",     LogLevel.INFO));
    public static final Channel CONFIG        = register(Channel.of("config",        "Config",        LogLevel.INFO));
    public static final Channel REGISTRY      = register(Channel.of("registry",      "Registry",      LogLevel.INFO));
    public static final Channel COMMANDS      = register(Channel.of("commands",      "Commands",      LogLevel.INFO));
    public static final Channel EFFECTS       = register(Channel.of("effects",       "Effects",       LogLevel.INFO));
    public static final Channel INFECTION     = register(Channel.of("infection",     "Infection",     LogLevel.INFO));
    public static final Channel CALLBACKS     = register(Channel.of("callbacks",     "Callbacks",     LogLevel.INFO));
    public static final Channel FIELD         = register(Channel.of("field",         "Field",         LogLevel.INFO));
    public static final Channel GUI = register(Channel.of("gui", "GUI", LogLevel.INFO));
    
    /** Global state store (State.java). */
    public static final Channel STATE         = register(Channel.of("state",         "State",         LogLevel.INFO));
    
    // ========== FIELD SYSTEM CHANNELS ==========
    
    /** Binding evaluation (player state → field properties). */
    public static final Channel BINDING       = register(Channel.of("binding",       "Binding",       LogLevel.INFO));
    
    /** Animation updates (spin, pulse, wobble). */
    public static final Channel ANIMATION     = register(Channel.of("animation",     "Animation",     LogLevel.INFO));
    
    /** Network sync (field packets, state sync). */
    public static final Channel NETWORK       = register(Channel.of("network",       "Network",       LogLevel.INFO));
    
    // ========== REGISTRY ==========
    
    private static Channel register(Channel channel) {
        CHANNELS.put(channel.id(), channel);
        return channel;
    }
    
    public static Channel channel(String id) {
        return CHANNELS.get(id);
    }
    
    public static Collection<Channel> channels() {
        return Collections.unmodifiableCollection(CHANNELS.values());
    }
    
    // ========== LIFECYCLE ==========
    
    public static void reload() {
        LogConfig.load();
    }
    
    public static void reset() {
        for (Channel ch : CHANNELS.values()) {
            ch.reset();
        }
        LogWatchdog.reset();
    }
    
    private Logging() {}
}
