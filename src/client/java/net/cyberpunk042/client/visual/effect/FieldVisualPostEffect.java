package net.cyberpunk042.client.visual.effect;

import net.cyberpunk042.client.visual.effect.uniform.FieldVisualUniformBinder;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the Field Visual post-process effect.
 * 
 * <p>This is the main entry point for the field visual system,
 * handling initialization, state management, and processor loading.</p>
 */
public final class FieldVisualPostEffect {
    
    private static final String LOG_TOPIC = "field_visual";
    
    /** Required framebuffer targets */
    private static final Set<Identifier> REQUIRED_TARGETS = DefaultFramebufferSet.STAGES;
    
    // State
    private static boolean enabled = false;
    private static boolean initialized = false;
    
    /** Cached processors per shader key (version + effectType) */
    private static final ConcurrentHashMap<ShaderKey, PostEffectProcessor> PROCESSOR_CACHE = new ConcurrentHashMap<>();
    
    /** Last used shader key (for logging) */
    private static ShaderKey lastShaderKey = null;
    
    /** Current field being rendered (set per-pass for multi-field support) */
    private static FieldVisualInstance currentField = null;
    
    /** Current tick delta for position interpolation */
    private static float currentTickDelta = 0.0f;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Initializes the field visual effect system.
     * Called during mod client initialization.
     */
    public static void init() {
        if (initialized) return;
        
        Logging.RENDER.topic(LOG_TOPIC)
            .info("Initializing Field Visual Post-Effect system");
        
        // Register tick handler for animation time
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
            .END_CLIENT_TICK.register(client -> {
                if (enabled && !client.isPaused()) {
                    // Tick at ~60fps = 0.016s per tick, but MC runs at 20tps = 0.05s
                    FieldVisualUniformBinder.tick(0.05f);
                }
            });
        
        initialized = true;
        Logging.RENDER.topic(LOG_TOPIC)
            .info("Field Visual Post-Effect initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE CONTROL
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean state) {
        enabled = state;
        Logging.RENDER.topic(LOG_TOPIC)
            .kv("enabled", state)
            .info("Field Visual effect toggled");
    }
    
    public static void toggle() {
        setEnabled(!enabled);
    }
    
    /**
     * Enables the effect and ensures at least the local player's field is registered.
     */
    public static void enable() {
        setEnabled(true);
    }
    
    /**
     * Disables the effect.
     */
    public static void disable() {
        setEnabled(false);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FOLLOW MODE (updates field position each frame to follow player)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static boolean followMode = true;
    private static java.util.UUID followFieldId = null;
    
    public static void setFollowMode(boolean follow) {
        followMode = follow;
    }
    
    public static boolean isFollowMode() {
        return followMode;
    }
    
    public static void setFollowFieldId(java.util.UUID id) {
        followFieldId = id;
    }
    
    /**
     * Called each frame from the render mixin to update position if following.
     * Gets player position directly and applies anchor offset + orb position offset.
     */
    public static void tickFollowPosition(float camX, float camY, float camZ,
                                          float fwdX, float fwdY, float fwdZ) {
        if (!enabled || !followMode || followFieldId == null) {
            return;
        }
        
        // Get client player for actual position
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        // Get the field to determine its anchor offset
        FieldVisualInstance field = FieldVisualRegistry.get(followFieldId);
        if (field == null) return;
        
        // Use bounding box center - same as ClientFieldManager for consistency
        Vec3d playerCenter = client.player.getBoundingBox().getCenter();
        
        // Apply anchor offset from the primitive
        Vec3d anchorOffset = field.getAnchorOffset();
        Vec3d finalPos = playerCenter.add(anchorOffset);
        
        // Apply orb position offset if adapter is set and no linked primitive
        if (throwAdapter != null && field.getAnchorOffset().equals(Vec3d.ZERO)) {
            String orbPos = (String) throwAdapter.get("orbStartPosition");
            if (orbPos != null && !orbPos.equals("center")) {
                // Calculate right vector from forward
                Vec3d forward = new Vec3d(fwdX, fwdY, fwdZ).normalize();
                Vec3d worldUp = new Vec3d(0, 1, 0);
                Vec3d right = forward.crossProduct(worldUp).normalize();
                if (right.lengthSquared() < 0.01) {
                    right = new Vec3d(1, 0, 0); // Fallback when looking straight up/down
                }
                
                float dist = 2.0f;
                Vec3d orbOffset = switch (orbPos) {
                    case "front" -> forward.multiply(dist);
                    case "behind" -> forward.multiply(-dist);
                    case "left" -> right.multiply(-dist);
                    case "right" -> right.multiply(dist);
                    case "above" -> new Vec3d(0, dist, 0);
                    default -> Vec3d.ZERO;
                };
                finalPos = finalPos.add(orbOffset);
            }
        }
        
        FieldVisualRegistry.updatePosition(followFieldId, finalPos);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // THROW ANIMATION (hooks into adapter animation)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Reference to the adapter for throw animation ticks */
    private static net.cyberpunk042.client.gui.state.adapter.FieldVisualAdapter throwAdapter = null;
    
    /**
     * Sets the adapter to use for throw animation.
     * Called when a throw is triggered from the GUI.
     */
    public static void setThrowAdapter(net.cyberpunk042.client.gui.state.adapter.FieldVisualAdapter adapter) {
        throwAdapter = adapter;
    }
    
    /**
     * Gets the current adapter (for blend equation, etc.).
     */
    public static net.cyberpunk042.client.gui.state.adapter.FieldVisualAdapter getAdapter() {
        return throwAdapter;
    }
    
    /**
     * Ticks the throw animation if one is active.
     * Called each frame from the world renderer mixin.
     */
    public static void tickThrowAnimation() {
        if (throwAdapter != null && throwAdapter.isThrowActive()) {
            throwAdapter.tickThrow();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROCESSOR LOADING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Loads (or returns cached) the post-effect processor for the given config.
     * 
     * <p>Uses ShaderKey to determine which standalone shader to load:
     * <ul>
     *   <li>V2 → field_visual_v2.fsh</li>
     *   <li>V3 → field_visual_v3.fsh</li>
     *   <li>V6 → field_visual_v6.fsh</li>
     *   <li>V7 → field_visual_v7.fsh</li>
     *   <li>Geodesic → field_visual_geodesic.fsh</li>
     *   <li>Other (V1/V5) → field_visual.fsh (monolith)</li>
     * </ul></p>
     * 
     * @param config The field's visual configuration
     * @return The processor, or null if loading failed or disabled
     */
    public static PostEffectProcessor loadProcessor(FieldVisualConfig config) {
        if (!enabled) return null;
        if (config == null) return null;
        
        ShaderKey key = ShaderKey.fromConfig(config);
        Identifier shaderId = key.toShaderId();
        
        if (shaderId == null) {
            // EffectType.NONE - no shader needed
            return null;
        }
        
        // Check cache first
        PostEffectProcessor cached = PROCESSOR_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        
        // Load new processor
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;
        
        ShaderLoader shaderLoader = client.getShaderLoader();
        if (shaderLoader == null) {
            Logging.RENDER.topic(LOG_TOPIC)
                .warn("ShaderLoader not available");
            return null;
        }
        
        try {
            PostEffectProcessor processor = shaderLoader.loadPostEffect(shaderId, REQUIRED_TARGETS);
            
            // Cache it
            PROCESSOR_CACHE.put(key, processor);
            
            // Log shader selection (only when shader changes)
            if (lastShaderKey == null || !lastShaderKey.equals(key)) {
                Logging.RENDER.topic(LOG_TOPIC)
                    .kv("shader", shaderId.toString())
                    .kv("effect", key.describe())
                    .kv("standalone", key.isStandalone())
                    .info("Loaded shader for effect");
                lastShaderKey = key;
            }
            
            return processor;
            
        } catch (Exception e) {
            Logging.RENDER.topic(LOG_TOPIC)
                .kv("id", shaderId.toString())
                .kv("effect", key.describe())
                .kv("error", e.getMessage())
                .error("Failed to load post-effect processor");
            return null;
        }
    }
    
    /**
     * Clears the processor cache. Call when shaders need to be reloaded.
     */
    public static void clearProcessorCache() {
        PROCESSOR_CACHE.clear();
        lastShaderKey = null;
        Logging.RENDER.topic(LOG_TOPIC)
            .info("Processor cache cleared");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMERA UPDATES (called by mixin)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates camera position for field distance calculations.
     */
    public static void updateCameraPosition(float x, float y, float z) {
        FieldVisualUniformBinder.updateCameraPosition(new Vec3d(x, y, z));
    }
    
    /**
     * Updates camera forward direction (normalized).
     */
    public static void updateCameraForward(float x, float y, float z) {
        FieldVisualUniformBinder.updateCameraForward(x, y, z);
    }
    
    /**
     * Updates view-projection matrices for world reconstruction.
     */
    public static void updateViewProjectionMatrices(Matrix4f invViewProj, Matrix4f viewProj) {
        FieldVisualUniformBinder.updateViewProjectionMatrices(invViewProj, viewProj);
    }
    
    /**
     * Updates inverse view-projection matrix only (legacy support).
     */
    public static void updateInvViewProjection(Matrix4f invViewProj) {
        FieldVisualUniformBinder.updateInvViewProjection(invViewProj);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns a debug status string.
     */
    public static String getStatusString() {
        if (!enabled) return "Field Visual: OFF";
        return String.format("Field Visual: ON, %d fields, %s",
            FieldVisualRegistry.getRenderCount(),
            FieldVisualUniformBinder.getStatusString());
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static float getGlobalTime() {
        return FieldVisualUniformBinder.getGlobalTime();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CURRENT FIELD (for single-field-per-pass rendering)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the current field to render.
     * Called by WorldRendererFieldVisualMixin before each processor.render() call.
     */
    public static void setCurrentField(FieldVisualInstance field) {
        currentField = field;
    }
    
    /**
     * Gets the current field being rendered.
     * Called by PostEffectPassMixin to get field data for UBO.
     */
    public static FieldVisualInstance getCurrentField() {
        return currentField;
    }
    
    /**
     * Clears the current field after rendering.
     */
    public static void clearCurrentField() {
        currentField = null;
    }
    
    /**
     * Sets the current tick delta for position interpolation.
     * Called by WorldRendererFieldVisualMixin at start of render.
     */
    public static void setTickDelta(float delta) {
        currentTickDelta = delta;
    }
    
    /**
     * Gets the current tick delta for position interpolation.
     * Used by PostEffectPassMixin for smooth field positions.
     */
    public static float getTickDelta() {
        return currentTickDelta;
    }
    
    private FieldVisualPostEffect() {}  // Prevent instantiation
}
