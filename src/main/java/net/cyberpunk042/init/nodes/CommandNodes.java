package net.cyberpunk042.init.nodes;

import net.cyberpunk042.command.GrowthBlockCommands;
import net.cyberpunk042.command.GrowthCollisionCommand;
import net.cyberpunk042.command.VirusDebugCommands;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.log.LogChatBridge;
import net.cyberpunk042.log.LogCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Command registration nodes.
 */
public final class CommandNodes {
    
    private CommandNodes() {}
    
    /**
     * Log commands and chat bridge.
     */
    public static final InitNode LOG_COMMANDS = InitNode.simple(
        "log_commands", "Log Commands",
        () -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> 
                LogCommands.register(dispatcher));
            ServerLifecycleEvents.SERVER_STARTED.register(LogChatBridge::setServer);
            ServerLifecycleEvents.SERVER_STOPPED.register(s -> LogChatBridge.setServer(null));
            return 1;
        }
    );
    
    /**
     * Debug commands.
     */
    public static final InitNode DEBUG_COMMANDS = InitNode.simple(
        "debug_commands", "Debug Commands",
        () -> {
            VirusDebugCommands.register();
            return 1;
        }
    );
    
    /**
     * Growth commands.
     */
    public static final InitNode GROWTH_COMMANDS = InitNode.simple(
        "growth_commands", "Growth Commands",
        () -> {
            GrowthBlockCommands.register();
            GrowthCollisionCommand.register();
            return 2;
        }
    );
}
