package net.cyberpunk042.client.init.nodes;

import net.cyberpunk042.client.field.JoinWarmupManager;
import net.cyberpunk042.client.field.WarmupOverlay;
import net.cyberpunk042.client.gui.render.TestFieldRenderer;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.profile.ProfileManager;
import net.cyberpunk042.client.visual.ClientFieldManager;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.SphereTessellator;
import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.*;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.cyberpunk042.visual.shape.SphereShape;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Client field system initialization nodes.
 * 
 * <p>These nodes initialize the client-side field system in proper order.
 */
public final class ClientFieldNodes {
    
    private ClientFieldNodes() {}
    
    /**
     * Field registry defaults for client rendering.
     */
    public static final InitNode FIELD_REGISTRY = InitNode.simple(
        "client_field_registry", "Field Registry (Client)",
        () -> {
            FieldRegistry.registerDefaults();
            return FieldRegistry.count();
        }
    );
    
    /**
     * Field network payload receivers.
     */
    public static final InitNode FIELD_NETWORK_RECEIVERS = InitNode.simple(
        "field_network_receivers", "Field Network Receivers",
        () -> {
            // Field spawn
            ClientPlayNetworking.registerGlobalReceiver(FieldSpawnPayload.ID, (payload, context) -> {
                context.client().execute(() -> handleSpawn(payload));
            });
            
            // Field remove
            ClientPlayNetworking.registerGlobalReceiver(FieldRemovePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientFieldManager.get().remove(payload.id())));
            
            // Field update
            ClientPlayNetworking.registerGlobalReceiver(FieldUpdatePayload.ID, (payload, context) ->
                context.client().execute(() -> handleUpdate(payload)));
            
            // Definition sync
            ClientPlayNetworking.registerGlobalReceiver(FieldDefinitionSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> handleDefinitionSync(payload)));
            
            // Legacy shield payloads
            ClientPlayNetworking.registerGlobalReceiver(ShieldFieldSpawnPayload.ID, (payload, context) ->
                context.client().execute(() -> handleLegacyShieldSpawn(payload)));
            ClientPlayNetworking.registerGlobalReceiver(ShieldFieldRemovePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientFieldManager.get().remove(payload.id())));
            
            Logging.GUI.topic("field").info("Registered field payload receivers");
            return 6;
        }
    );
    
    /**
     * Field render event registration.
     */
    public static final InitNode FIELD_RENDER_EVENT = InitNode.simple(
        "field_render_event", "Field Render Event",
        () -> {
            WorldRenderEvents.AFTER_ENTITIES.register(context -> 
                ClientFieldManager.get().render(context));
            return 1;
        }
    );
    
    /**
     * Field tick event registration.
     */
    public static final InitNode FIELD_TICK_EVENT = InitNode.simple(
        "field_tick_event", "Field Tick Event",
        () -> {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                ClientFieldManager.get().tick();
                // Server-authoritative forces - client prediction disabled
                // ClientForceApplicator.tick();
            });
            return 1;
        }
    );
    
    /**
     * Field disconnect cleanup.
     */
    public static final InitNode FIELD_DISCONNECT_HANDLER = InitNode.simple(
        "field_disconnect_handler", "Field Disconnect Handler",
        () -> {
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> 
                client.execute(() -> ClientFieldManager.get().clear()));
            return 1;
        }
    );
    
    /**
     * Bundled server profiles registration.
     */
    public static final InitNode BUNDLED_PROFILES = InitNode.simple(
        "bundled_profiles", "Bundled Profiles",
        () -> {
            var profileManager = ProfileManager.getInstance();
            profileManager.loadAll();
            
            int registered = 0;
            for (var profile : profileManager.getAllProfiles()) {
                if (profile.source() != ProfileSource.SERVER) {
                    continue;
                }
                FieldDefinition definition = profile.definition();
                if (definition == null) {
                    continue;
                }
                FieldRegistry.register(definition);
                registered++;
            }
            
            Logging.RENDER.topic("field").info("Registered {} bundled SERVER profiles into FieldRegistry", registered);
            return registered;
        }
    ).dependsOn("client_field_registry");
    
    /**
     * Join warmup manager.
     */
    public static final InitNode JOIN_WARMUP = InitNode.simple(
        "join_warmup", "Join Warmup",
        () -> {
            JoinWarmupManager.init();
            ClientTickEvents.END_CLIENT_TICK.register(client -> JoinWarmupManager.tick());
            return 1;
        }
    );
    
    /**
     * Warmup overlay HUD.
     */
    public static final InitNode WARMUP_OVERLAY = InitNode.simple(
        "warmup_overlay", "Warmup Overlay",
        () -> {
            WarmupOverlay.init();
            return 1;
        }
    );
    
    /**
     * Rendering pipeline warmup (JIT compilation).
     */
    public static final InitNode RENDER_WARMUP = InitNode.simple(
        "render_warmup", "Render Warmup",
        () -> {
            try {
                long start = System.currentTimeMillis();
                
                var sphereShape = SphereShape.builder()
                    .radius(12.0f)
                    .latSteps(19)
                    .lonSteps(23)
                    .algorithm(SphereAlgorithm.LAT_LON)
                    .build();
                
                Mesh mesh1 = SphereTessellator.tessellate(sphereShape);
                Mesh mesh2 = SphereTessellator.tessellate(sphereShape, QuadPattern.STRIPE_1, null, null, 0);
                
                long took = System.currentTimeMillis() - start;
                Logging.RENDER.topic("field").info("Rendering pipeline warmup complete: {}ms", took);
                return 2;
            } catch (Exception e) {
                Logging.RENDER.topic("field").warn("Warmup failed (non-critical): {}", e.getMessage());
                return 0;
            }
        }
    );
    
    /**
     * Shader program warmup (GPU compilation).
     * 
     * <p>Pre-compiles field visual and shockwave shaders during world join
     * to eliminate first-enable lag when effects are activated.</p>
     * 
     * <p>Note: Actual shader compilation happens in JoinWarmupManager because
     * ShaderLoader requires a fully initialized client with loaded resources.</p>
     */
    public static final InitNode SHADER_WARMUP = InitNode.simple(
        "shader_warmup", "Shader Warmup",
        () -> {
            // Just register - actual warmup happens in JoinWarmupManager 
            // because ShaderLoader isn't ready during client init
            Logging.RENDER.topic("shader_warmup").info("Shader warmup service registered - will compile on world join");
            return 1;
        }
    );

    
    /**
     * Test field renderer for debug preview.
     */
    public static final InitNode TEST_RENDERER = InitNode.simple(
        "test_renderer", "Test Field Renderer",
        () -> {
            TestFieldRenderer.init();
            return 1;
        }
    );
    
    /**
     * Fragment presets (preload for faster GUI open).
     */
    public static final InitNode FRAGMENT_PRESETS = InitNode.simple(
        "fragment_presets", "Fragment Presets",
        () -> {
            FragmentRegistry.ensureLoaded();
            return 1;
        }
    );
    
    /**
     * Fresnel (Horizon) shader for rim lighting effects.
     */
    public static final InitNode FRESNEL_SHADER = InitNode.simple(
        "fresnel_shader", "Fresnel Shader",
        () -> {
            net.cyberpunk042.client.visual.shader.FresnelPipelines.init();
            
            // Initialize shader animation system (time-based effects)
            net.cyberpunk042.client.visual.shader.ShaderAnimationManager.init();
            
            // Register tick event to update animation time
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                client -> net.cyberpunk042.client.visual.shader.ShaderAnimationManager.tick()
            );
            
            return 1;
        }
    );
    
    /**
     * Depth test shader for post-processing POC (used by Shockwave).
     */
    public static final InitNode DEPTH_TEST_SHADER = InitNode.simple(
        "depth_test_shader", "Depth Test Shader",
        () -> {
            net.cyberpunk042.client.visual.shader.DepthTestShader.init();
            net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.init();
            net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.init();
            net.cyberpunk042.client.visual.shader.ShockwavePostEffect.init();
            net.cyberpunk042.client.visual.shader.MagicCirclePostEffect.init();
            
            // Register HUD overlay for depth visualization and shockwave
            // NOTE: CAPTURE happens in WorldRenderer mixin, only RENDER here
            net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
                (context, tickCounter) -> {
                    var client = net.minecraft.client.MinecraftClient.getInstance();
                    if (client != null && client.getWindow() != null) {
                        int width = client.getWindow().getScaledWidth();
                        int height = client.getWindow().getScaledHeight();
                        
                        // DirectDepthRendererArchive overlay (if enabled)
                        net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.renderOverlay(context, width, height);
                        
                        // ShockwaveGlowRenderer overlay - ONLY RENDER (capture is in WorldRenderer)
                        net.cyberpunk042.client.visual.shader.ShockwaveGlowRenderer.render(context, client, width, height);
                    }
                }
            );
            
            return 1;
        }
    );
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final Identifier ANTI_VIRUS_FIELD_ID = 
        Identifier.of("the-virus-block", "anti_virus");
    
    private static void handleSpawn(FieldSpawnPayload payload) {
        Identifier defId = payload.definitionIdentifier();
        if (defId == null) {
            Logging.GUI.topic("field").warn("Invalid definition ID in spawn payload: {}", payload.definitionId());
            return;
        }
        
        FieldDefinition def = FieldRegistry.get(defId);
        if (def == null) {
            Logging.GUI.topic("field").info("Definition {} not found, skipping spawn", defId);
            return;
        }
        
        Vec3d pos = new Vec3d(payload.x(), payload.y(), payload.z());
        ClientFieldState state = ClientFieldState.atPosition(payload.id(), defId, def.type(), pos)
            .withScale(payload.scale())
            .withPhase(payload.phase())
            .withLifetime(payload.lifetimeTicks());
        
        ClientFieldManager.get().addOrUpdate(state);
        Logging.RENDER.topic("field").info("Spawned field: id={} def={}", payload.id(), defId.getPath());
    }
    
    private static void handleUpdate(FieldUpdatePayload payload) {
        ClientFieldState existing = ClientFieldManager.get().get(payload.id());
        if (existing == null) return;
        
        Vec3d newPos = new Vec3d(payload.x(), payload.y(), payload.z());
        existing.withPosition(newPos).withAlpha(payload.alpha());
        
        String shuffleType = payload.shuffleType();
        int shuffleIndex = payload.shuffleIndex();
        if (shuffleType != null && !shuffleType.isEmpty()) {
            existing.withShuffleOverride(shuffleType, shuffleIndex);
        }
        
        existing.withFollowMode(payload.followMode())
            .withPrediction(payload.predictionEnabled(), payload.predictionLeadTicks(),
                payload.predictionMaxDistance(), payload.predictionLookAhead(),
                payload.predictionVerticalBoost());
    }
    
    private static void handleDefinitionSync(FieldDefinitionSyncPayload payload) {
        Identifier id = payload.definitionIdentifier();
        if (id == null) {
            Logging.GUI.topic("field").warn("Invalid definition ID: {}", payload.definitionId());
            return;
        }
        
        try {
            var json = com.google.gson.JsonParser.parseString(payload.definitionJson()).getAsJsonObject();
            var loader = new net.cyberpunk042.field.loader.FieldLoader();
            var definition = loader.parseDefinition(json);
            FieldRegistry.register(definition);
            Logging.GUI.topic("field").info("Definition REGISTERED: {}", id);
        } catch (Exception e) {
            Logging.GUI.topic("field").error("Failed to parse definition {}: {}", id, e.getMessage());
        }
    }
    
    private static void handleLegacyShieldSpawn(ShieldFieldSpawnPayload payload) {
        FieldDefinition def = FieldRegistry.get(ANTI_VIRUS_FIELD_ID);
        if (def == null) {
            Logging.RENDER.topic("field").warn("Anti-virus field definition not found: {}", ANTI_VIRUS_FIELD_ID);
            return;
        }
        
        Vec3d pos = new Vec3d(payload.x(), payload.y(), payload.z());
        float scale = payload.radius() / 12.0f;
        
        ClientFieldState state = ClientFieldState.atPosition(payload.id(), ANTI_VIRUS_FIELD_ID, FieldType.SHIELD, pos)
            .withScale(scale)
            .withLifetime(-1);
        
        ClientFieldManager.get().addOrUpdate(state);
        Logging.RENDER.topic("field").info("Spawned anti-virus field: id={}", payload.id());
    }
}
