package net.cyberpunk042.client.visual.effect;

import net.minecraft.util.math.Vec3d;
import net.cyberpunk042.log.Logging;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for all active field visual instances.
 * 
 * <p>Manages the lifecycle of field visuals:
 * <ul>
 *   <li>Registration when fields are created</li>
 *   <li>Updates when fields move or change</li>
 *   <li>Unregistration when fields are destroyed</li>
 *   <li>Provides sorted lists for rendering</li>
 * </ul>
 * 
 * <p>Thread-safe for access from render thread and game thread.</p>
 */
public final class FieldVisualRegistry {
    
    private static final String LOG_TOPIC = "field_visual";
    
    /**
     * Maximum number of fields that can be rendered per frame.
     * Limited by UBO size in shader.
     */
    public static final int MAX_RENDERED_FIELDS = 8;
    
    /**
     * Base render distance for small effects.
     * Larger effects get extended range based on their radius.
     */
    public static final double BASE_RENDER_DISTANCE = 800.0;
    
    /**
     * Maximum render distance for the largest effects (e.g. suns).
     * Fields with large radius can render up to this distance.
     */
    public static final double MAX_RENDER_DISTANCE = 10000.0;
    
    /**
     * Multiplier for radius-based distance extension.
     */
    public static final double RADIUS_DISTANCE_MULTIPLIER = 250.0;
    
    // Active fields indexed by ID
    private static final Map<UUID, FieldVisualInstance> activeFields = new ConcurrentHashMap<>();
    
    // Cached sorted list for rendering (rebuilt when dirty)
    private static volatile List<FieldVisualInstance> renderList = Collections.emptyList();
    private static volatile boolean renderListDirty = true;
    private static volatile Vec3d lastCameraPos = Vec3d.ZERO;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Registers a new field visual instance.
     * 
     * @param instance The instance to register
     */
    public static void register(FieldVisualInstance instance) {
        if (instance == null) return;
        
        activeFields.put(instance.getId(), instance);
        renderListDirty = true;
        
        Logging.RENDER.topic(LOG_TOPIC)
            .kv("id", instance.getId().toString().substring(0, 8))
            .kv("type", instance.getEffectType().getId())
            .debug("Registered field visual");
    }
    
    /**
     * Unregisters a field visual instance.
     * 
     * @param id The ID of the instance to remove
     */
    public static void unregister(UUID id) {
        if (id == null) return;
        
        FieldVisualInstance removed = activeFields.remove(id);
        if (removed != null) {
            renderListDirty = true;
            
            // Clean up the processor for this field
            FieldVisualPostEffect.removeProcessor(id);
            
            Logging.RENDER.topic(LOG_TOPIC)
                .kv("id", id.toString().substring(0, 8))
                .debug("Unregistered field visual");
        }
    }
    
    /**
     * Unregisters all fields owned by a specific entity.
     * Useful when a player disconnects.
     * 
     * @param ownerId The owner's UUID
     */
    public static void unregisterByOwner(UUID ownerId) {
        if (ownerId == null) return;
        
        List<UUID> toRemove = activeFields.values().stream()
            .filter(f -> ownerId.equals(f.getOwnerId()))
            .map(FieldVisualInstance::getId)
            .toList();
        
        toRemove.forEach(activeFields::remove);
        
        if (!toRemove.isEmpty()) {
            renderListDirty = true;
            Logging.RENDER.topic(LOG_TOPIC)
                .kv("owner", ownerId.toString().substring(0, 8))
                .kv("count", toRemove.size())
                .debug("Unregistered fields by owner");
        }
    }
    
    /**
     * Clears all registered fields.
     * Called on world unload or disconnect.
     */
    public static void clear() {
        int count = activeFields.size();
        activeFields.clear();
        renderList = Collections.emptyList();
        renderListDirty = true;
        
        if (count > 0) {
            Logging.RENDER.topic(LOG_TOPIC)
                .kv("count", count)
                .info("Cleared all field visuals");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates a field's position.
     * 
     * @param id Field ID
     * @param position New world position
     */
    public static void updatePosition(UUID id, Vec3d position) {
        FieldVisualInstance instance = activeFields.get(id);
        if (instance != null) {
            instance.updatePosition(position);
            renderListDirty = true;
        }
    }
    
    /**
     * Updates a field's radius.
     * 
     * @param id Field ID
     * @param radius New radius
     */
    public static void updateRadius(UUID id, float radius) {
        FieldVisualInstance instance = activeFields.get(id);
        if (instance != null) {
            instance.updateRadius(radius);
        }
    }
    
    /**
     * Updates a field's visual configuration.
     * 
     * @param id Field ID
     * @param config New configuration
     */
    public static void updateConfig(UUID id, FieldVisualConfig config) {
        FieldVisualInstance instance = activeFields.get(id);
        if (instance != null) {
            instance.updateConfig(config);
        }
    }
    
    /**
     * Gets a specific field by ID.
     * 
     * @param id Field ID
     * @return The instance, or null if not found
     */
    public static FieldVisualInstance get(UUID id) {
        return activeFields.get(id);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns the list of fields to render, sorted by distance from camera.
     * Limited to MAX_RENDERED_FIELDS.
     * 
     * @param cameraPos Current camera position
     * @return Sorted list of instances to render
     */
    public static List<FieldVisualInstance> getFieldsToRender(Vec3d cameraPos) {
        // Check if we need to rebuild the list
        if (renderListDirty || !cameraPos.equals(lastCameraPos)) {
            rebuildRenderList(cameraPos);
        }
        return renderList;
    }
    
    /**
     * Rebuilds the render list sorted by distance.
     */
    private static void rebuildRenderList(Vec3d cameraPos) {
        lastCameraPos = cameraPos;
        
        renderList = activeFields.values().stream()
            // Only include renderable fields
            .filter(FieldVisualInstance::shouldRender)
            // Only include fields within their dynamic render distance
            .filter(f -> isWithinRenderDistance(f, cameraPos))
            // Sort by distance (closest first)
            .sorted(Comparator.comparingDouble(f -> f.distanceSquaredTo(cameraPos)))
            // Limit to max renderable
            .limit(MAX_RENDERED_FIELDS)
            .collect(Collectors.toList());
        
        renderListDirty = false;
    }
    
    /**
     * Checks if a field is within its render distance.
     * 
     * Formula: BASE_RENDER_DISTANCE + max(radius × MULTIPLIER, distortionRadius × 1.5)
     * 
     * The 1.5× multiplier on distortionRadius provides buffer beyond the fade range,
     * ensuring the effect is loaded before the player enters the visual influence zone.
     * 
     * Examples (with BASE=800, MULTIPLIER=250):
     * - Small orb (r=3, distortion=500): 800 + max(750, 750) = 1550 blocks
     * - Large sun (r=50, distortion=1000): 800 + max(12500, 1500) = 13300 blocks
     * - Flash effect (r=5, distortion=5000): 800 + max(1250, 7500) = 8300 blocks
     */
    private static boolean isWithinRenderDistance(FieldVisualInstance field, Vec3d cameraPos) {
        double radius = field.getRadius();
        
        // Get configured visual influence range (Proximity Darken > Range)
        double distortionRadius = 0.0;
        FieldVisualConfig config = field.getConfig();
        if (config != null && config.distortion() != null) {
            distortionRadius = config.distortion().radius();
        }
        
        // Formula: BASE + max(radius-based, distortion-based with 1.5× buffer)
        double radiusComponent = radius * RADIUS_DISTANCE_MULTIPLIER;
        double distortionComponent = distortionRadius * 1.5;
        double dynamicDist = BASE_RENDER_DISTANCE + Math.max(radiusComponent, distortionComponent);
        
        // Cap at maximum
        dynamicDist = Math.min(dynamicDist, MAX_RENDER_DISTANCE);
        
        double distSq = field.distanceSquaredTo(cameraPos);
        return distSq < dynamicDist * dynamicDist;
    }
    
    /**
     * Returns the count of active fields.
     */
    public static int getActiveCount() {
        return activeFields.size();
    }
    
    /**
     * Returns the count of fields in the current render list.
     */
    public static int getRenderCount() {
        return renderList.size();
    }
    
    /**
     * Returns whether any fields require post-process rendering.
     */
    public static boolean hasRenderableFields() {
        return activeFields.values().stream().anyMatch(FieldVisualInstance::shouldRender);
    }
    
    /**
     * Marks the render list as needing rebuild.
     * Called when camera moves significantly or fields change.
     */
    public static void markDirty() {
        renderListDirty = true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns debug information about the registry state.
     */
    public static String getDebugInfo() {
        return String.format("FieldVisualRegistry: %d active, %d renderable",
            activeFields.size(), renderList.size());
    }
    
    private FieldVisualRegistry() {}  // Prevent instantiation
}
