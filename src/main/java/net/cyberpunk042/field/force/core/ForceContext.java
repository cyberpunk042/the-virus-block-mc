package net.cyberpunk042.field.force.core;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Context for force calculations.
 * Contains entity, field center, timing info.
 */
public record ForceContext(
    @Nullable Entity entity,
    Vec3d fieldCenter,
    float normalizedTime,
    int ticksElapsed,
    int durationTicks
) {
    
    /**
     * Creates context for an entity in a force field.
     */
    public static ForceContext forEntity(Entity entity, Vec3d center, 
            float normalizedTime, int ticksElapsed, int durationTicks) {
        return new ForceContext(entity, center, normalizedTime, ticksElapsed, durationTicks);
    }
    
    /**
     * Returns the entity's current position (center mass).
     */
    public Vec3d entityPosition() {
        if (entity == null) return fieldCenter;
        return entity.getPos().add(0, entity.getHeight() / 2, 0);
    }
    
    /**
     * Returns the distance from entity to field center.
     */
    public double distance() {
        return entityPosition().distanceTo(fieldCenter);
    }
    
    /**
     * Returns normalized direction FROM entity TO center (inward).
     */
    public Vec3d directionToCenter() {
        Vec3d delta = fieldCenter.subtract(entityPosition());
        double len = delta.length();
        return len > 0.001 ? delta.multiply(1.0 / len) : Vec3d.ZERO;
    }
    
    /**
     * Returns normalized direction FROM center TO entity (outward).
     */
    public Vec3d directionFromCenter() {
        return directionToCenter().multiply(-1);
    }
}
