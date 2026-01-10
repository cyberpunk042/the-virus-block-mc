package net.cyberpunk042.client.input.spawn;

import net.minecraft.util.math.Vec3d;

/**
 * Defines the direction from which an orb spawns.
 */
public enum SpawnOriginMode {
    /**
     * Orb descends from directly above (straight down from sky).
     */
    FROM_ABOVE,
    
    /**
     * Orb rises from below (like rising from the ground).
     */
    FROM_BELOW,
    
    /**
     * Orb approaches horizontally from the direction the player is facing.
     */
    FROM_HORIZON,
    
    /**
     * Orb approaches diagonally from the sky horizon (45° angle).
     */
    FROM_SKY_HORIZON;
    
    /**
     * Get the spawn direction vector for this origin mode.
     * 
     * @param playerLookDir The player's normalized look direction (for HORIZON modes)
     * @return Normalized direction vector pointing FROM spawn TO player
     */
    public Vec3d getSpawnDirection(Vec3d playerLookDir) {
        return switch (this) {
            case FROM_ABOVE -> new Vec3d(0, 1, 0);
            case FROM_BELOW -> new Vec3d(0, -1, 0);
            case FROM_HORIZON -> playerLookDir.normalize();
            case FROM_SKY_HORIZON -> {
                // 45° angle: combine up and forward
                Vec3d up = new Vec3d(0, 1, 0);
                Vec3d forward = playerLookDir.withAxis(net.minecraft.util.math.Direction.Axis.Y, 0).normalize();
                yield up.add(forward).normalize();
            }
        };
    }
    
    /**
     * Get display name for GUI.
     */
    public String getDisplayName() {
        return switch (this) {
            case FROM_ABOVE -> "Above";
            case FROM_BELOW -> "Below";
            case FROM_HORIZON -> "Horizon";
            case FROM_SKY_HORIZON -> "Diagonal";
        };
    }
}
