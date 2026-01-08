package net.cyberpunk042.init.nodes;

import net.cyberpunk042.config.InfectionConfigRegistry;
import net.cyberpunk042.config.ModConfigBootstrap;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.log.LogConfig;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.util.ServerRef;

/**
 * Core system initialization nodes.
 * 
 * <p>These run first and have no dependencies on other nodes.
 */
public final class CoreNodes {
    
    private CoreNodes() {}
    
    /**
     * Server reference initialization.
     */
    public static final InitNode SERVER_REF = InitNode.simple(
        "server_ref", "Server Reference",
        () -> {
            ServerRef.init();
            return 1;
        }
    );
    
    /**
     * Core configuration loading.
     */
    public static final InitNode CONFIG = InitNode.simple(
        "config", "Configuration",
        () -> {
            ModConfigBootstrap.prepareCommon();
            InfectionConfigRegistry.loadCommon();
            return 2;
        }
    );
    
    /**
     * Logging system configuration.
     */
    public static final InitNode LOGGING = InitNode.simple(
        "logging", "Logging System",
        () -> {
            LogConfig.load();
            // Use SuperProfiler for comprehensive performance monitoring
            net.cyberpunk042.util.SuperProfiler.init();
            return 1;
        }
    );
    
    /**
     * Command protection system.
     */
    public static final InitNode COMMANDS = InitNode.simple(
        "commands", "Command Protection",
        () -> {
            net.cyberpunk042.command.util.CommandKnobConfig.reload();
            net.cyberpunk042.command.util.CommandProtection.reload();
            net.cyberpunk042.command.util.CommandProtection.auditDeviations();
            return 3;
        }
    );
}
