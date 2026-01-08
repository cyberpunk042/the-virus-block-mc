package net.cyberpunk042.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.cyberpunk042.client.gui.screen.LogViewerScreen;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Client command to open the Log Viewer GUI.
 * 
 * Usage: /logs (or /clog for "client log")
 */
public class LogViewerCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // /logs - opens the log viewer
        dispatcher.register(
            literal("logs")
                .executes(ctx -> {
                    // Schedule GUI opening on main thread
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new LogViewerScreen());
                    });
                    return 1;
                })
        );
        
        // Alias: /clog (client log)
        dispatcher.register(
            literal("clog")
                .executes(ctx -> {
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient.getInstance().setScreen(new LogViewerScreen());
                    });
                    return 1;
                })
        );
    }
}

