package net.cyberpunk042.client.input.spawn;

/**
 * Defines how the orb targets its destination.
 */
public enum TargetMode {
    /**
     * RELATIVE: Orb stops at a sweetspot distance from the reference point.
     * Example: spawn 1000 blocks above, target 500 blocks above → orb stops halfway.
     */
    RELATIVE,
    
    /**
     * TRUE_TARGET: Orb travels to exact world coordinates.
     * The orb goes all the way to the specified X, Y, Z position.
     */
    TRUE_TARGET;
    
    /**
     * Get display name for GUI.
     */
    public String getDisplayName() {
        return switch (this) {
            case RELATIVE -> "Relative";
            case TRUE_TARGET -> "True Target";
        };
    }
}
