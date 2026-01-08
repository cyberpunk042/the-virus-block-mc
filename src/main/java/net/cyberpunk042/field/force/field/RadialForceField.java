package net.cyberpunk042.field.force.field;

import net.cyberpunk042.field.force.ForceFieldConfig;
import net.cyberpunk042.field.force.core.ForceContext;
import net.minecraft.util.math.Vec3d;

/**
 * Pure gravitational force field with orbital dynamics.
 * 
 * <h2>Physics Model</h2>
 * <ul>
 *   <li>Force is ONLY toward center (pure gravity)</li>
 *   <li>Natural elliptical orbits emerge</li>
 *   <li>Center perturbation breaks single-axis oscillation</li>
 * </ul>
 * 
 * <h2>Safety Features</h2>
 * <ul>
 *   <li><b>Gravity floor</b>: Minimum force at edges prevents escape</li>
 *   <li><b>Dynamic softening</b>: Prevents extreme center acceleration</li>
 *   <li><b>Center perturbation</b>: Breaks symmetry for varied orbits</li>
 * </ul>
 */
public class RadialForceField implements ForceField {
    
    private final ForceFieldConfig config;
    
    // Base softening - scales with strength
    private static final double BASE_SOFTENING = 1.0;
    
    // Gravity floor - minimum fraction at edges
    private static final double GRAVITY_FLOOR = 0.12;
    
    // Maximum force multiplier
    private static final double MAX_FORCE_MULTIPLIER = 4.0;
    
    // Center zone where perturbation is applied (fraction of radius)
    private static final double CENTER_ZONE = 0.15;
    
    public RadialForceField(ForceFieldConfig config) {
        this.config = config != null ? config : ForceFieldConfig.DEFAULT;
    }
    
    @Override
    public Vec3d calculateForce(ForceContext context) {
        double distance = context.distance();
        double radius = config.radius();
        
        // Outside field radius - no force
        if (distance > radius) {
            return Vec3d.ZERO;
        }
        
        // Direction toward center (standard gravitational direction)
        Vec3d towardCenter = context.directionToCenter();
        double normalizedDist = distance / radius;
        
        // ═══════════════════════════════════════════════════════════════════════
        // PURE GRAVITATIONAL FORCE
        // ═══════════════════════════════════════════════════════════════════════
        double gravityStrength = calculateGravity(distance, normalizedDist);
        Vec3d force = towardCenter.multiply(gravityStrength);
        
        // ═══════════════════════════════════════════════════════════════════════
        // ORBIT PLANE EVOLUTION - adds perpendicular nudges to evolve orbit
        // TILT (X axis): Tips orbit forward/backward - uses cross with X axis
        // ORIENTATION (Z axis): Tips orbit left/right - uses cross with Z axis
        // ═══════════════════════════════════════════════════════════════════════
        if (shouldRotateOrbit()) {
            int ticks = context.ticksElapsed();
            double nudgeStrength = gravityStrength * 0.25;
            
            // TILT: Cross with X axis (1,0,0) for forward/backward tilt
            // Result is perpendicular in YZ plane
            if (!"fixed".equals(config.tiltMode()) && config.tiltRate() > 0) {
                double phase = ticks * config.tiltRate();
                double amplitude = "wobble".equals(config.tiltMode()) ? 0.6 : 1.0;
                double nudge = Math.sin(phase) * nudgeStrength * amplitude;
                
                Vec3d xAxis = new Vec3d(1, 0, 0);
                Vec3d tiltPerp = towardCenter.crossProduct(xAxis);
                if (tiltPerp.lengthSquared() > 0.001) {
                    tiltPerp = tiltPerp.normalize();
                    force = force.add(tiltPerp.multiply(nudge));
                }
            }
            
            // ORIENTATION: Cross with Z axis (0,0,1) for left/right tilt
            // Result is perpendicular in XY plane
            if (!"fixed".equals(config.orientationMode()) && config.orientationRate() > 0) {
                double phase = ticks * config.orientationRate();
                double amplitude = "wobble".equals(config.orientationMode()) ? 0.6 : 1.0;
                double nudge = Math.sin(phase) * nudgeStrength * amplitude;
                
                Vec3d zAxis = new Vec3d(0, 0, 1);
                Vec3d orientPerp = towardCenter.crossProduct(zAxis);
                if (orientPerp.lengthSquared() > 0.001) {
                    orientPerp = orientPerp.normalize();
                    force = force.add(orientPerp.multiply(nudge));
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // CENTER PERTURBATION - breaks single-axis oscillation
        // When entity passes near center, add tiny perpendicular force
        // This causes each pass to exit at slightly different angle
        // ═══════════════════════════════════════════════════════════════════════
        if (normalizedDist < CENTER_ZONE && context.entity() != null) {
            Vec3d velocity = context.entity().getVelocity();
            double speed = velocity.length();
            
            if (speed > 0.1) {
                // Perpendicular to velocity in horizontal plane
                Vec3d perpendicular = velocity.crossProduct(new Vec3d(0, 1, 0));
                if (perpendicular.lengthSquared() > 0.001) {
                    perpendicular = perpendicular.normalize();
                    
                    // Small perturbation - just enough to break symmetry
                    // Scales with how close to center (stronger at exact center)
                    double perturbStrength = config.strength() * 0.08 * (1 - normalizedDist / CENTER_ZONE);
                    force = force.add(perpendicular.multiply(perturbStrength));
                }
            }
        }
        
        // Ground lift
        if (config.groundLift() > 0 && context.entity() != null && context.entity().isOnGround()) {
            force = force.add(0, config.groundLift(), 0);
        }
        
        return force;
    }
    
    /**
     * Checks if orbit rotation is enabled.
     */
    private boolean shouldRotateOrbit() {
        return (!"fixed".equals(config.tiltMode()) && config.tiltRate() > 0) ||
               (!"fixed".equals(config.orientationMode()) && config.orientationRate() > 0);
    }
    
    /**
     * Gets the rotation angle based on mode and rate.
     */
    private double getRotationAngle(String mode, float rate, int ticks) {
        if ("wobble".equals(mode)) {
            // Oscillate: sin wave
            return ticks * rate;  // Sin applied in caller
        } else {
            // fullcycle: continuous rotation
            return ticks * rate;
        }
    }
    
    /**
     * Applies rotation to a vector based on mode and rate.
     * @param vec The vector to rotate
     * @param mode "fixed", "wobble", or "fullcycle"
     * @param rate Radians per tick
     * @param ticks Current tick count
     * @param aroundY true for Y-axis (orientation), false for X-axis (tilt)
     */
    private Vec3d applyRotation(Vec3d vec, String mode, float rate, int ticks, boolean aroundY) {
        if ("fixed".equals(mode) || rate <= 0) {
            return vec;
        }
        
        double angle;
        if ("wobble".equals(mode)) {
            // Oscillate: sin wave from -rate to +rate
            angle = Math.sin(ticks * rate) * rate * 10;  // Multiply for visible effect
        } else {
            // fullcycle: continuous rotation
            angle = ticks * rate;
        }
        
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        if (aroundY) {
            // Rotate around Y axis (horizontal spin)
            double nx = vec.x * cos - vec.z * sin;
            double nz = vec.x * sin + vec.z * cos;
            return new Vec3d(nx, vec.y, nz);
        } else {
            // Rotate around X axis (tilt up/down)
            double ny = vec.y * cos - vec.z * sin;
            double nz = vec.y * sin + vec.z * cos;
            return new Vec3d(vec.x, ny, nz);
        }
    }
    
    /**
     * Calculates gravity strength with floor and softening.
     */
    private double calculateGravity(double distance, double normalizedDist) {
        String falloff = config.falloff();
        double strength = config.strength();
        
        // Dynamic softening
        double softening = BASE_SOFTENING * (1 + strength * 0.5);
        
        double calculatedStrength = switch (falloff) {
            case "quadratic" -> strength * (1 - normalizedDist) * (1 - normalizedDist);
            case "inverse" -> {
                double distSq = distance * distance;
                yield strength / (distSq + softening);
            }
            case "constant" -> strength;
            default -> strength * (1 - normalizedDist);  // linear
        };
        
        // Gravity floor
        double floorStrength = strength * GRAVITY_FLOOR;
        calculatedStrength = Math.max(calculatedStrength, floorStrength);
        
        // Cap maximum
        double maxStrength = strength * MAX_FORCE_MULTIPLIER;
        calculatedStrength = Math.min(calculatedStrength, maxStrength);
        
        return calculatedStrength;
    }
    
    @Override
    public boolean affectsDistance(double distance) {
        return distance <= config.radius();
    }
    
    @Override
    public float maxRadius() {
        return config.radius();
    }
}
