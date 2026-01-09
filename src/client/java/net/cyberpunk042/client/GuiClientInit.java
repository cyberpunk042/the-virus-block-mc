package net.cyberpunk042.client;


import net.cyberpunk042.client.command.FieldEditCommands;
import net.cyberpunk042.client.command.LogViewerCommand;
import net.cyberpunk042.client.gui.render.TestFieldRenderer;
import net.cyberpunk042.client.network.GuiClientHandlers;
import net.cyberpunk042.log.Logging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * G141-G145: Client-side GUI initialization.
 * 
 * <p>Wires together all GUI components:</p>
 * <ul>
 *   <li>G141: Client packet handlers</li>
 *   <li>G142: Keybind registration (future)</li>
 *   <li>G143: Config loading</li>
 *   <li>G144: Profile storage init</li>
 *   <li>G145: Debug field renderer (future)</li>
 *   <li>G146: Client-side /field edit commands</li>
 * </ul>
 * 
 * <p>Register this in fabric.mod.json under "client" entrypoints.</p>
 */
@Environment(EnvType.CLIENT)
public class GuiClientInit implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        Logging.GUI.topic("init").info("Initializing GUI client...");
        
        // G141: Register packet handlers
        GuiClientHandlers.register();
        
        // G142: Keybinds
        net.cyberpunk042.client.input.OrbChargeHandler.register();
        
        // G143: Load client config
        // GuiConfig.load();
        
        // G144: Initialize profile storage
        // ProfileStorage.init();
        
        // G145: Register test field renderer (client-side preview)
        TestFieldRenderer.init();
        
        // Preload fragment presets so they're ready when GUI opens
        net.cyberpunk042.client.gui.util.FragmentRegistry.reload();
        
        // Clear test field on disconnect to prevent double-rendering on rejoin
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
            (handler, client) -> {
                client.execute(() -> {
                    net.cyberpunk042.client.gui.state.FieldEditStateHolder.despawnTestField();
                    Logging.GUI.topic("init").debug("Cleared test field on disconnect");
                });
            });
        
        // G146: Register client-side commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            FieldEditCommands.register(dispatcher);
            LogViewerCommand.register(dispatcher);

        });
        
        Logging.GUI.topic("init").info("GUI client initialized");
    }
}
