package net.cyberpunk042.client.init.nodes;

import net.cyberpunk042.client.network.GuiClientHandlers;
import net.cyberpunk042.client.screen.PurificationTotemScreen;
import net.cyberpunk042.client.screen.VirusDifficultyScreen;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.screen.ModScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

/**
 * Client GUI initialization nodes.
 */
public final class ClientGuiNodes {
    
    private ClientGuiNodes() {}
    
    /**
     * Screen registrations.
     */
    public static final InitNode SCREENS = InitNode.simple(
        "screens", "Screens",
        () -> {
            HandledScreens.register(ModScreenHandlers.PURIFICATION_TOTEM, PurificationTotemScreen::new);
            HandledScreens.register(ModScreenHandlers.VIRUS_DIFFICULTY, VirusDifficultyScreen::new);
            return 2;
        }
    );
    
    /**
     * GUI client network handlers.
     */
    public static final InitNode GUI_HANDLERS = InitNode.simple(
        "gui_handlers", "GUI Handlers",
        () -> {
            GuiClientHandlers.register();
            return 1;
        }
    );
    
    /**
     * Keybinding registrations.
     */
    public static final InitNode KEYBINDINGS = InitNode.simple(
        "keybindings", "Keybindings",
        () -> {
            net.cyberpunk042.client.input.OrbChargeHandler.register();
            return 1;
        }
    );
    
    /**
     * Orb spawn animation manager tick registration.
     */
    public static final InitNode ORB_SPAWN_MANAGER = InitNode.simple(
        "orb_spawn_manager", "Orb Spawn Manager",
        () -> {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                net.cyberpunk042.client.input.spawn.OrbSpawnManager::tick
            );
            return 1;
        }
    );
}
