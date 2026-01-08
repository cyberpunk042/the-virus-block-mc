package net.cyberpunk042.field.force.field;

import net.cyberpunk042.field.force.core.ForceContext;
import net.minecraft.util.math.Vec3d;

/**
 * Interface for force field implementations.
 */
public interface ForceField {
    
    /**
     * Calculates the force to apply at the given context.
     */
    Vec3d calculateForce(ForceContext context);
    
    /**
     * Returns whether this field affects entities at the given distance.
     */
    boolean affectsDistance(double distance);
    
    /**
     * Returns the maximum effect radius.
     */
    float maxRadius();
}
