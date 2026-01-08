package net.cyberpunk042.client.render;

import net.cyberpunk042.client.visual.ClientFieldManager;
import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.VoidTearBurstPayload;
import net.cyberpunk042.network.VoidTearSpawnPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Manages VoidTear visual effects using the new field system.
 * 
 * <p>VoidTears are temporary spherical effects that:
 * <ul>
 *   <li>Spawn at a location with a specific tier (0-5)</li>
 *   <li>Pulse and spin while active</li>
 *   <li>Fade out over their duration</li>
 *   <li>Burst (instantly remove) when triggered</li>
 * </ul>
 * 
 * <p>Each tier has a slightly different color (purple gradient).
 * 
 * <h2>Migration Notes</h2>
 * <p>This class was refactored to use the new field system instead of
 * manual sphere rendering. The sphere mesh is now handled by
 * {@link net.cyberpunk042.client.field.render.FieldRenderer_old}.
 */
public final class VoidTearVisualManager {
    
    private static final float VISUAL_DURATION_MULTIPLIER = 4.0f;
    private static final Identifier VOID_TEAR_DEFINITION_ID = 
        Identifier.of("the-virus-block", "void_tear");
    
    private VoidTearVisualManager() {}

    /**
     * Initializes the VoidTear visual system.
     * Registers network payload handlers.
     */
    public static void init() {
        // Spawn payload → create field state
        ClientPlayNetworking.registerGlobalReceiver(VoidTearSpawnPayload.ID, (payload, context) ->
            context.client().execute(() -> handleSpawn(payload)));
        
        // Burst payload → remove field state
        ClientPlayNetworking.registerGlobalReceiver(VoidTearBurstPayload.ID, (payload, context) ->
            context.client().execute(() -> handleBurst(payload)));
        
        // Clear on disconnect (handled by ClientFieldManager.clear())
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                // ClientFieldManager.clear() handles this, but log for clarity
                Logging.RENDER.topic("voidtear").debug("Disconnect - void tear states cleared");
            }));
        
        Logging.RENDER.topic("voidtear").info("VoidTear visual manager initialized (using field system)");
    }

    /**
     * Handles a VoidTear spawn payload.
     * Creates a ClientFieldState for the appropriate tier.
     */
    private static void handleSpawn(VoidTearSpawnPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        
        // Calculate visual duration (same multiplier as before)
        int visualDuration = Math.max(1, MathHelper.floor(payload.durationTicks() * VISUAL_DURATION_MULTIPLIER));
        
        // Tier is still available from payload for potential future use (logging, effects)
        int tier = MathHelper.clamp(payload.tierIndex(), 0, 5);
        
        // Create position
        Vec3d position = new Vec3d(payload.x(), payload.y(), payload.z());
        
        // Create client field state
        // Use payload.id() as the field ID for tracking
        ClientFieldState state = ClientFieldState.atPosition(
                payload.id(),
                VOID_TEAR_DEFINITION_ID,
                FieldType.FORCE,
                position)
            .withScale(payload.radius())  // Use radius as scale
            .withPhase((float) (client.world.random.nextFloat() * Math.PI * 2))
            .withLifetime(visualDuration);
        
        // Add to client field manager
        ClientFieldManager.get().addOrUpdate(state);
        
        Logging.RENDER.topic("voidtear").debug(
            "Spawned void tear: id={} tier={} pos=({:.1f},{:.1f},{:.1f}) radius={:.1f} duration={}t",
            payload.id(), tier, position.x, position.y, position.z, 
            payload.radius(), visualDuration);
    }

    /**
     * Handles a VoidTear burst payload.
     * Immediately removes the field state.
     */
    private static void handleBurst(VoidTearBurstPayload payload) {
        ClientFieldManager.get().remove(payload.id());
        
        Logging.RENDER.topic("voidtear").debug("Burst void tear: id={}", payload.id());
    }
}
