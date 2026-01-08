package net.cyberpunk042.field.instance;

import net.cyberpunk042.log.Logging;

import net.cyberpunk042.field.FieldType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * A field instance attached to a player.
 * 
 * <h2>Follow Configuration</h2>
 * <p>Uses {@link FollowConfig} to control how the field follows the player:
 * <ul>
 *   <li>leadOffset: trailing (-) or leading (+) in movement direction</li>
 *   <li>responsiveness: how quickly field catches up (0.1 to 1.0)</li>
 *   <li>lookAhead: offset toward player's look direction</li>
 * </ul>
 */
public class PersonalFieldInstance extends FieldInstance {
    
    private final UUID ownerUuid;
    private FollowConfig followConfig;
    
    // Smoothing state
    private Vec3d targetPosition;
    private Vec3d velocity;
    
    public PersonalFieldInstance(long id, Identifier definitionId, FieldType type,
                                  UUID ownerUuid, Vec3d initialPosition) {
        super(id, definitionId, type, initialPosition);
        this.ownerUuid = ownerUuid;
        this.followConfig = FollowConfig.DEFAULT;
        this.targetPosition = initialPosition;
        this.velocity = Vec3d.ZERO;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Configuration
    // ─────────────────────────────────────────────────────────────────────────────
    
    public FollowConfig getFollowConfig() {
        return followConfig;
    }
    
    public void setFollowConfig(FollowConfig config) {
        if (config != null) {
            Logging.REGISTRY.topic("personal").debug(
                "Follow config updated for player {}: enabled={}, leadOffset={}",
                ownerUuid, config.enabled(), config.leadOffset());
            this.followConfig = config;
        }
    }
    
    public float getResponsiveness() {
        return followConfig.responsiveness();
    }
    
    public void setResponsiveness(float factor) {
        this.followConfig = followConfig.toBuilder()
            .responsiveness(Math.max(0.1f, Math.min(1.0f, factor)))
            .build();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Position Update
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Updates the field position based on player position.
     * Called from server when player moves.
     */
    public void updateFromPlayer(PlayerEntity player) {
        if (player == null || !followConfig.enabled()) {
            return;
        }
        
        Vec3d playerPos = player.getPos().add(0, 1.0, 0); // Center on player
        
        // Apply follow config offsets
        Vec3d targetPos = calculateTargetPosition(player, playerPos);
        
        // If locked (responsiveness >= 1.0 and no lead offset), snap immediately
        if (followConfig.isLocked()) {
            position = targetPos;
            targetPosition = targetPos;
        } else {
            // Set target, interpolation happens in tickInstance
            targetPosition = targetPos;
        }
    }
    
    /**
     * Calculates target position based on follow config.
     */
    private Vec3d calculateTargetPosition(PlayerEntity player, Vec3d playerPos) {
        Vec3d result = playerPos;
        
        // Apply look-ahead (offset toward look direction)
        float lookAhead = followConfig.lookAhead();
        if (Math.abs(lookAhead) > 0.001f) {
            Vec3d look = player.getRotationVector().normalize();
            result = result.add(look.multiply(lookAhead));
        }
        
        // Apply lead/trail offset (in velocity direction)
        float leadOffset = followConfig.leadOffset();
        if (Math.abs(leadOffset) > 0.01f) {
            Vec3d playerVel = player.getVelocity();
            
            // Ignore Y velocity when on ground (gravity noise)
            if (player.isOnGround()) {
                playerVel = new Vec3d(playerVel.x, 0.0, playerVel.z);
            }
            
            double speed = playerVel.length();
            if (speed > 0.01) {
                // Scale by velocity magnitude for smooth feel
                result = result.add(playerVel.normalize().multiply(leadOffset * speed * 5.0));
            }
        }
        
        return result;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    protected void tickInstance() {
        // Interpolate toward target (locked mode is handled in updateFromPlayer)
        if (!followConfig.isLocked() && targetPosition != null) {
            float lerpFactor = followConfig.responsiveness();
            position = position.lerp(targetPosition, lerpFactor);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Overrides
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public UUID ownerUuid() {
        return ownerUuid;
    }
}
