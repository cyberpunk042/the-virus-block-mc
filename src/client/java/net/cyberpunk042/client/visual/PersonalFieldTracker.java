package net.cyberpunk042.client.visual;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.field.instance.FollowConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Tracks and smooths the position of a personal field attached to a player.
 * 
 * <p>Features:
 * <ul>
 *   <li>Movement follow (trailing/leading based on FollowConfig)</li>
 *   <li>Smooth interpolation (prevents jitter)</li>
 *   <li>Look-ahead (field leads look direction)</li>
 * </ul>
 */
public final class PersonalFieldTracker {
    
    // Configuration
    private Identifier definitionId;
    private float responsiveness = 0.5f;  // From FollowConfig
    private float scale = 1.0f;
    private boolean enabled = false;
    private boolean visible = true;
    
    // State - position tracking for render-frame interpolation
    private Vec3d previousPosition = Vec3d.ZERO;  // Position from last tick (for interpolation)
    private Vec3d currentPosition = Vec3d.ZERO;   // Position updated this tick
    private Vec3d targetPosition = Vec3d.ZERO;    // Target we're moving toward
    private float phase = 0;
    private int age = 0;
    
    public PersonalFieldTracker() {}
    
    // =========================================================================
    // Configuration
    // =========================================================================
    
    public void setDefinition(Identifier id) {
        this.definitionId = id;
    }
    
    public void setResponsiveness(float resp) {
        this.responsiveness = Math.max(0.1f, Math.min(1.0f, resp));
    }
    
    public void setScale(float scale) {
        this.scale = Math.max(0.1f, scale);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            currentPosition = Vec3d.ZERO;
        }
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isEnabled() { return enabled; }
    public boolean isVisible() { return visible; }
    public float getScale() { return scale; }
    public Identifier definitionId() { return definitionId; }
    
    public FieldDefinition definition() {
        return definitionId != null ? FieldRegistry.get(definitionId) : null;
    }
    
    // =========================================================================
    // Position Access
    // =========================================================================
    
    /**
     * Gets the current tick-aligned position.
     */
    public Vec3d getPosition() {
        return currentPosition;
    }
    
    /**
     * Gets the smoothly interpolated render position.
     * This should be called during rendering with the partial tick delta.
     * 
     * @param tickDelta partial tick (0-1)
     * @return interpolated position between previousPosition and currentPosition
     */
    public Vec3d getRenderPosition(float tickDelta) {
        return new Vec3d(
            MathHelper.lerp(tickDelta, previousPosition.x, currentPosition.x),
            MathHelper.lerp(tickDelta, previousPosition.y, currentPosition.y),
            MathHelper.lerp(tickDelta, previousPosition.z, currentPosition.z)
        );
    }
    
    public float getPhase() {
        return phase;
    }
    
    public int getAge() {
        return age;
    }
    
    // =========================================================================
    // Tick Update
    // =========================================================================
    
    /**
     * Updates the tracker for a new tick.
     * Called once per client tick (not per frame).
     */
    public void tick(PlayerEntity player) {
        if (!enabled || player == null) return;
        
        age++;
        phase += 0.05f;
        
        // Save previous position for render-frame interpolation
        previousPosition = currentPosition;
        
        // Calculate target based on player and follow config
        targetPosition = calculateTargetPosition(player);
        
        // Get responsiveness from follow config if available
        FieldDefinition def = definition();
        float resp = responsiveness;
        if (def != null && def.follow() != null && def.follow().enabled()) {
            resp = def.follow().responsiveness();
        }
        
        // Interpolate toward target with responsiveness
        currentPosition = currentPosition.lerp(targetPosition, resp);
    }
    
    /**
     * Calculates the target position based on player and follow config.
     */
    private Vec3d calculateTargetPosition(PlayerEntity player) {
        // Base: center of player bounding box, slightly down
        Vec3d base = player.getBoundingBox().getCenter().add(0, -0.1, 0);
        
        // Get follow config from definition
        FieldDefinition def = definition();
        if (def == null || def.follow() == null || !def.follow().enabled()) {
            return base;
        }
        
        FollowConfig follow = def.follow();
        Vec3d result = base;
        
        // Apply look-ahead (offset toward look direction)
        float lookAhead = follow.lookAhead();
        if (Math.abs(lookAhead) > 0.001f) {
            Vec3d look = player.getRotationVec(1.0f).normalize();
            result = result.add(look.multiply(lookAhead));
        }
        
        // Apply lead/trail offset (in velocity direction)
        float leadOffset = follow.leadOffset();
        if (Math.abs(leadOffset) > 0.01f) {
            Vec3d velocity = player.getVelocity();
            
            // Ignore Y velocity when on ground (gravity noise)
            if (player.isOnGround()) {
                velocity = new Vec3d(velocity.x, 0.0, velocity.z);
            }
            
            double speed = velocity.length();
            if (speed > 0.01) {
                // Scale by velocity magnitude for smooth feel
                result = result.add(velocity.normalize().multiply(leadOffset * speed * 5.0));
            }
        }
        
        return result;
    }
    
    /**
     * Resets the tracker state.
     */
    public void reset() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Vec3d startPos = client.player.getBoundingBox().getCenter();
            currentPosition = startPos;
            previousPosition = startPos;  // Initialize both to prevent interpolation glitch
            targetPosition = startPos;
        }
        phase = (float) (Math.random() * Math.PI * 2);
        age = 0;
    }
}
