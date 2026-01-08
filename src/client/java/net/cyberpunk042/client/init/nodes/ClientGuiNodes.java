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
}
