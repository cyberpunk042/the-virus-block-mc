package net.cyberpunk042.client.render;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.util.math.BlockPos;

/**
 * Manages singularity visual effects.
 * 
 * <p>This class was refactored to use the new field system via
 * {@link SingularityFieldController}. The complex state machine
 * and phased animations are preserved.
 * 
 * <h2>Migration Notes</h2>
 * <ul>
 *   <li>Sphere rendering now uses {@link net.cyberpunk042.client.field.render.FieldRenderer_old}</li>
 *   <li>Each component (primary, core, beam) is a separate ClientFieldState</li>
 *   <li>Legacy config is preserved for backwards compatibility</li>
 * </ul>
 */
public final class SingularityVisualManager {
    
    private SingularityVisualManager() {}

    /**
     * Initializes the singularity visual system.
     */
    public static void init() {
        // Tick handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> 
            SingularityFieldController.tick(client));
        
        // Clear on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(SingularityFieldController::clear));
        
        // Note: Rendering is handled by ClientFieldManager via WorldRenderEvents
        
        Logging.RENDER.topic("singularity").info(
            "Singularity visual manager initialized (using field system)");
    }

    /**
     * Adds a singularity visual at the given position.
     */
    public static void add(BlockPos pos) {
        SingularityFieldController.add(pos);
    }

    /**
     * Removes a singularity visual at the given position.
     */
    public static void remove(BlockPos pos) {
        SingularityFieldController.remove(pos);
    }
}
