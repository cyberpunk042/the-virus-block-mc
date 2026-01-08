package net.cyberpunk042.client.render.field;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import net.cyberpunk042.client.visual.ClientFieldManager;
import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.GrowthRingFieldPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Handles growth ring field effects using the new field system.
 * 
 * <p>Growth rings are temporary ring-shaped effects that:
 * <ul>
 *   <li>Spawn at a position with a given radius and width</li>
 *   <li>Fade out over their duration</li>
 *   <li>Use the ring primitive for rendering</li>
 * </ul>
 * 
 * <h2>Migration Notes</h2>
 * <p>This class was refactored to use the new field system instead of
 * manual ring rendering. The ring mesh is now handled by
 * {@link net.cyberpunk042.client.visual.render.RingRenderer_old}.
 */
public final class GrowthRingFieldRenderer {
    
    private static final Identifier DEF_GROWTH_RING = Identifier.of("the-virus-block", "growth_ring");
    
    // Track active rings for lifetime management
    private static final ConcurrentLinkedDeque<RingTracker> ACTIVE = new ConcurrentLinkedDeque<>();
    
    // ID generation
    private static long nextId = Long.MIN_VALUE / 3;

    private GrowthRingFieldRenderer() {}

    /**
     * Initializes the growth ring field system.
     */
    public static void init() {
        // Register payload handler
        ClientPlayNetworking.registerGlobalReceiver(GrowthRingFieldPayload.ID, (payload, context) ->
            context.client().execute(() -> handlePayload(payload)));

        // Tick handler for lifetime management
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.isPaused()) return;
            
            Iterator<RingTracker> it = ACTIVE.iterator();
            while (it.hasNext()) {
                RingTracker tracker = it.next();
                if (tracker.tick()) {
                    // Ring expired - remove field
                    ClientFieldManager.get().remove(tracker.fieldId);
                    it.remove();
                }
            }
        });

        // Clear on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                for (RingTracker tracker : ACTIVE) {
                    ClientFieldManager.get().remove(tracker.fieldId);
                }
                ACTIVE.clear();
            }));

        // Note: Rendering is handled by ClientFieldManager via WorldRenderEvents
        
        Logging.RENDER.topic("growth").info("Growth ring renderer initialized (using field system)");
    }

    /**
     * Handles a growth ring field payload.
     */
    private static void handlePayload(GrowthRingFieldPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || !world.getRegistryKey().equals(payload.worldKey())) {
            return;
        }
        
        Vec3d origin = Vec3d.ofCenter(payload.origin());
        
        for (GrowthRingFieldPayload.RingEntry entry : payload.rings()) {
            createRingField(origin, entry);
        }
    }

    /**
     * Creates a ring field from a payload entry.
     */
    private static void createRingField(Vec3d origin, GrowthRingFieldPayload.RingEntry entry) {
        long fieldId = nextId++;
        
        // Calculate inner/outer radius from center radius and width
        float radius = entry.radius();
        float width = entry.width();
        float innerRadius = Math.max(0.05f, radius - (width * 0.5f));
        float outerRadius = Math.max(innerRadius + 0.05f, radius + (width * 0.5f));
        
        // Create field state
        // Note: The growth_ring definition uses @primary color, which will be
        // resolved from the field profile's color config
        ClientFieldState field = ClientFieldState.atPosition(
                fieldId, DEF_GROWTH_RING, FieldType.SHIELD, origin)
            .withScale(radius)  // Use radius as scale
            .withLifetime(entry.durationTicks());
        
        ClientFieldManager.get().addOrUpdate(field);
        
        // Track for lifetime management
        ACTIVE.add(new RingTracker(fieldId, entry.durationTicks()));
        
        Logging.RENDER.topic("growth").trace(
            "Created ring field: id={} radius={:.1f} duration={}t",
            fieldId, radius, entry.durationTicks());
    }

    /**
     * Tracks a ring field's lifetime.
     */
    private static final class RingTracker {
        final long fieldId;
        final int maxTicks;
        int ticksRemaining;

        RingTracker(long fieldId, int durationTicks) {
            this.fieldId = fieldId;
            this.maxTicks = Math.max(1, durationTicks);
            this.ticksRemaining = this.maxTicks;
        }

        /**
         * @return true if expired
         */
        boolean tick() {
            ticksRemaining--;
            
            // Update alpha based on remaining time
            float alphaFactor = MathHelper.clamp(ticksRemaining / (float) maxTicks, 0, 1);
            ClientFieldState field = ClientFieldManager.get().get(fieldId);
            if (field != null) {
                field.withAlpha(alphaFactor);
            }
            
            return ticksRemaining <= 0;
        }
    }
}
