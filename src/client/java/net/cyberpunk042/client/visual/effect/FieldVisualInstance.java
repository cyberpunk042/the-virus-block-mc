package net.cyberpunk042.client.visual.effect;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Represents a single field instance for visual effect rendering.
 * 
 * <p>Each instance tracks:
 * <ul>
 *   <li>Identity (unique ID, owner for network sync)</li>
 *   <li>Spatial data (world position, radius)</li>
 *   <li>Shape type (for SDF calculations)</li>
 *   <li>Visual configuration (effect type, colors, animation)</li>
 *   <li>Animation state (current phase/time offset)</li>
 * </ul>
 * 
 * <p>Updated each frame from the Field system to track movement.</p>
 */
public class FieldVisualInstance {
    
    // Shape type constants (matching field system)
    public static final String SHAPE_SPHERE = "sphere";
    public static final String SHAPE_TORUS = "torus";
    public static final String SHAPE_CYLINDER = "cylinder";
    public static final String SHAPE_PRISM = "prism";
    
    // Identity
    private final UUID id;
    private final UUID ownerId;  // Player/entity owner (for network sync)
    
    // Spatial data (updated each frame)
    private volatile Vec3d worldCenter;
    private volatile Vec3d previousWorldCenter;  // For tick interpolation (smooth movement)
    private volatile float radius;
    private volatile String shapeType;  // "sphere", "torus", etc.
    
    // Anchor offset (offset from player/entity anchor point)
    private volatile Vec3d anchorOffset = Vec3d.ZERO;
    
    // Visual configuration
    private volatile FieldVisualConfig config;
    
    // Animation state
    private float animationPhase;  // Current time offset for unique animation
    private long creationTime;     // For animation phase calculation
    
    // Warmup state (progressive loading for GPU efficiency)
    private static final long WARMUP_DURATION_MS = 2000;  // 2 seconds to reach full effect
    
    // Dirty tracking (for optimization)
    private volatile boolean dirty = true;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a new field visual instance.
     * 
     * @param id Unique identifier for this instance
     * @param ownerId Owner's UUID (player or entity)
     * @param worldCenter Initial world position
     * @param radius Field radius in blocks
     * @param shapeType Shape type string ("sphere", "torus", etc.)
     * @param config Visual effect configuration
     */
    public FieldVisualInstance(
            UUID id,
            UUID ownerId,
            Vec3d worldCenter,
            float radius,
            String shapeType,
            FieldVisualConfig config) {
        this.id = id;
        this.ownerId = ownerId;
        this.worldCenter = worldCenter;
        this.previousWorldCenter = worldCenter;  // Start with same position
        this.radius = radius;
        this.shapeType = shapeType != null ? shapeType.toLowerCase() : SHAPE_SPHERE;
        this.config = config;
        this.creationTime = System.currentTimeMillis();
        this.animationPhase = (float)(Math.random() * Math.PI * 2);  // Random start phase
    }
    
    /**
     * Creates an instance with default Energy Orb configuration.
     */
    public static FieldVisualInstance createEnergyOrb(
            UUID id,
            UUID ownerId,
            Vec3d position,
            float radius) {
        return new FieldVisualInstance(
            id, ownerId, position, radius,
            SHAPE_SPHERE,
            FieldVisualConfig.defaultEnergyOrb()
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE METHODS (called each frame from Field system)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Updates the world position.
     * Called when the field moves (e.g., player movement).
     * Saves previous position for smooth interpolation, unless position jumped significantly.
     */
    public void updatePosition(Vec3d pos) {
        if (!this.worldCenter.equals(pos)) {
            // Check if this is a teleport (large position jump) vs normal movement
            double distSq = this.worldCenter.squaredDistanceTo(pos);
            if (distSq > 1.0) {  // More than 1 block = teleport, don't interpolate
                this.previousWorldCenter = pos;  // No lerp for teleports
            } else {
                this.previousWorldCenter = this.worldCenter;  // Normal smooth movement
            }
            this.worldCenter = pos;
            this.dirty = true;
        }
    }
    
    /**
     * Updates the field radius.
     */
    public void updateRadius(float newRadius) {
        if (this.radius != newRadius) {
            this.radius = newRadius;
            this.dirty = true;
        }
    }
    
    /**
     * Updates the shape type.
     */
    public void updateShapeType(String type) {
        String normalized = type != null ? type.toLowerCase() : SHAPE_SPHERE;
        if (!this.shapeType.equals(normalized)) {
            this.shapeType = normalized;
            this.dirty = true;
        }
    }
    
    /**
     * Updates the visual configuration.
     */
    public void updateConfig(FieldVisualConfig newConfig) {
        this.config = newConfig;
        this.dirty = true;
    }
    
    /**
     * Bulk update for efficiency.
     */
    public void update(Vec3d position, float radius, String shapeType) {
        this.worldCenter = position;
        this.radius = radius;
        this.shapeType = shapeType != null ? shapeType.toLowerCase() : SHAPE_SPHERE;
        this.dirty = true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public Vec3d getWorldCenter() { return worldCenter; }
    
    /**
     * Returns the interpolated render position for smooth movement.
     * Uses lerp between previous and current position based on tickDelta.
     * 
     * @param tickDelta Partial tick progress (0.0 to 1.0)
     * @return Interpolated position for rendering
     */
    public Vec3d getRenderPosition(float tickDelta) {
        if (previousWorldCenter == null || previousWorldCenter.equals(worldCenter)) {
            return worldCenter;
        }
        return new Vec3d(
            net.minecraft.util.math.MathHelper.lerp(tickDelta, previousWorldCenter.x, worldCenter.x),
            net.minecraft.util.math.MathHelper.lerp(tickDelta, previousWorldCenter.y, worldCenter.y),
            net.minecraft.util.math.MathHelper.lerp(tickDelta, previousWorldCenter.z, worldCenter.z)
        );
    }
    
    /**
     * Resets interpolation state to prevent flickering after teleportation.
     * Sets previousWorldCenter to current worldCenter.
     */
    public void resetInterpolation() {
        this.previousWorldCenter = this.worldCenter;
    }
    
    public float getRadius() { return radius; }
    public String getShapeType() { return shapeType; }
    public FieldVisualConfig getConfig() { return config; }
    public EffectType getEffectType() { return config.effectType(); }
    public Vec3d getAnchorOffset() { return anchorOffset; }
    
    public void setAnchorOffset(Vec3d offset) {
        this.anchorOffset = offset != null ? offset : Vec3d.ZERO;
    }
    
    /**
     * Returns the current animation phase, including time offset.
     * This ensures each instance has a unique animation state.
     */
    public float getAnimationPhase() {
        float elapsed = (System.currentTimeMillis() - creationTime) / 1000f;
        return animationPhase + elapsed * config.animationSpeed();
    }
    
    /**
     * Returns warmup progress for progressive effect loading.
     * 
     * <p>Returns 0.0 at creation, 1.0 when fully warmed up.
     * Used to gradually scale radius and detail for GPU efficiency.</p>
     * 
     * @return Progress from 0.0 to 1.0
     */
    public float getWarmupProgress() {
        long elapsed = System.currentTimeMillis() - creationTime;
        if (elapsed >= WARMUP_DURATION_MS) return 1.0f;
        return (float) elapsed / WARMUP_DURATION_MS;
    }
    
    /**
     * Returns the effective radius scaled by warmup progress.
     * Starts at 10% and progresses to full radius.
     */
    public float getEffectiveRadius() {
        float progress = getWarmupProgress();
        // Ease-out curve: starts fast, slows down at end
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        // Scale from 10% to 100%
        return radius * (0.1f + 0.9f * eased);
    }
    
    /**
     * Returns effective detail/octaves scaled by warmup progress.
     * Starts at 20% (minimum 1) and progresses to full detail.
     * 
     * @param fullDetail The target detail level
     * @return Scaled detail level (minimum 1)
     */
    public int getEffectiveDetail(int fullDetail) {
        float progress = getWarmupProgress();
        // Linear ramp for detail
        int minDetail = Math.max(1, (int)(fullDetail * 0.2f));
        return minDetail + (int)((fullDetail - minDetail) * progress);
    }
    
    /**
     * Returns whether this instance needs to be re-packed to the UBO.
     */
    public boolean isDirty() { return dirty; }
    
    /**
     * Clears the dirty flag after packing to UBO.
     */
    public void clearDirty() { dirty = false; }
    
    /**
     * Returns whether this instance should be rendered.
     */
    public boolean shouldRender() {
        return config.effectType().requiresPostProcess() && config.intensity() > 0.01f;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Calculates squared distance from camera for sorting.
     */
    public double distanceSquaredTo(Vec3d cameraPos) {
        return worldCenter.squaredDistanceTo(cameraPos);
    }
    
    @Override
    public String toString() {
        return String.format("FieldVisualInstance[id=%s, type=%s, pos=%s, r=%.1f]",
            id.toString().substring(0, 8),
            config.effectType().getId(),
            worldCenter,
            radius);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldVisualInstance other)) return false;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
