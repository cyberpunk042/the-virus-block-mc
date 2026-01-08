package net.cyberpunk042.log;

import net.cyberpunk042.TheVirusBlock;
import org.slf4j.Logger;

/**
 * Central output pipeline. All log messages flow through here.
 * 
 * Pipeline:
 * 1. Override check (global mute, min level, force output)
 * 2. Watchdog check (spam detection)
 * 3. Level redirect (if configured)
 * 4. Output to TheVirusBlock.LOGGER
 * 5. Forward to chat if enabled
 * 
 * @see LogOverride
 */
public final class LogOutput {
    
    private static final Logger LOGGER = TheVirusBlock.LOGGER;
    
    public static void emit(Channel channel, String topic, LogLevel level, 
                           String message, Throwable exception) {
        emit(channel, topic, level, message, exception, false);
    }
    
    public static void emit(Channel channel, String topic, LogLevel level, 
                           String message, Throwable exception, boolean forceChat) {
        
        // 0. Null safety
        if (channel == null || level == null || level == LogLevel.OFF) {
            return;
        }
        
        // 1. Override check (mute, min level, force output)
        if (!LogOverride.shouldEmit(channel, level)) {
            return;
        }
        
        // 2. Watchdog check
        WatchdogDecision decision = LogWatchdog.observe(channel.id(), topic, message);
        if (decision.suppress()) {
            if (decision.summary() != null) {
                // Log suppression summary (bypasses overrides)
                doLog(LogLevel.WARN, "[Watchdog] " + decision.summary(), null);
            }
            return;
        }
        
        // 3. Apply level redirect
        LogLevel outputLevel = LogOverride.effectiveOutputLevel(level);
        
        // 4. Log to console
        doLog(outputLevel, message, exception);
        
        // 5. Forward to chat if enabled OR forced
        if ((channel.chatForward() || forceChat) && LogConfig.chatEnabled()) {
            LogChatBridge.forward(channel, topic, level, message);
        }
    }
    
    private static void doLog(LogLevel level, String message, Throwable exception) {
        switch (level) {
            case ERROR -> {
                if (exception != null) LOGGER.error(message, exception);
                else LOGGER.error(message);
            }
            case WARN -> {
                if (exception != null) LOGGER.warn(message, exception);
                else LOGGER.warn(message);
            }
            case INFO -> LOGGER.info(message);
            case DEBUG -> LOGGER.debug(message);
            case TRACE -> LOGGER.trace(message);
            default -> {}
        }
    }
    
    private LogOutput() {}
}
