package net.cyberpunk042.client.field.render.effect;

import net.cyberpunk042.visual.animation.RayTwistConfig;
import net.cyberpunk042.visual.animation.TwistMode;

/**
 * Twist effect - rotates ray around its own lengthwise axis.
 * 
 * <p>Based on RaysRenderer.applyTwist - uses Rodrigues' rotation formula
 * to rotate vertices around the ray axis.</p>
 * 
 * @see net.cyberpunk042.visual.animation.RayTwistConfig
 */
public final class RenderTwistEffect implements RenderVertexEffect {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    private final RayTwistConfig config;
    private final float time;
    
    public RenderTwistEffect(RayTwistConfig config, float time) {
        this.config = config;
        this.time = time;
    }
    
    @Override
    public void apply(float[] position, RenderEffectContext ctx) {
        if (config == null || !config.isActive()) {
            return;
        }
        
        float speed = Math.max(0.1f, config.speed());
        float amountRad = config.amountRadians();
        float phaseRad = config.phaseOffsetRadians();
        
        float t = ctx.t();
        
        // Get direction from context (axis to rotate around)
        float dx = ctx.dx();
        float dy = ctx.dy();
        float dz = ctx.dz();
        
        // If no direction, default to Y-up
        if (Math.abs(dx) < 0.0001f && Math.abs(dy) < 0.0001f && Math.abs(dz) < 0.0001f) {
            dy = 1.0f;
        }
        
        // Calculate the twist angle based on mode
        // From RaysRenderer.applyTwist lines 1433-1450
        float angle = switch (config.mode()) {
            case TWIST -> 
                // Continuous rotation
                (time * speed + phaseRad) * amountRad / TWO_PI;
            case OSCILLATE_TWIST -> 
                // Back and forth oscillation
                (float) Math.sin(time * speed * TWO_PI + phaseRad) * amountRad;
            case WIND_UP -> 
                // Progressive increase
                Math.min(time * speed * amountRad, amountRad * 10);
            case UNWIND -> 
                // Progressive decrease from wound state
                Math.max(amountRad * 10 - time * speed * amountRad, 0);
            case SPIRAL_TWIST -> 
                // Twist varies along length: more twist at tip
                t * (time * speed + phaseRad) * amountRad / TWO_PI;
            default -> 0f;
        };
        
        if (Math.abs(angle) < 0.0001f) {
            return;
        }
        
        // Rodrigues' rotation formula: v_rot = v*cos(θ) + (k×v)*sin(θ) + k*(k·v)*(1-cos(θ))
        // where k is the axis (dx, dy, dz), v is the position
        // From RaysRenderer.applyTwist lines 1470-1490
        
        float x = position[0];
        float y = position[1];
        float z = position[2];
        
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float oneMinusCos = 1 - cos;
        
        // k × v (cross product)
        float kCrossX = dy * z - dz * y;
        float kCrossY = dz * x - dx * z;
        float kCrossZ = dx * y - dy * x;
        
        // k · v (dot product)
        float kDotV = dx * x + dy * y + dz * z;
        
        // Rodrigues formula
        position[0] = x * cos + kCrossX * sin + dx * kDotV * oneMinusCos;
        position[1] = y * cos + kCrossY * sin + dy * kDotV * oneMinusCos;
        position[2] = z * cos + kCrossZ * sin + dz * kDotV * oneMinusCos;
    }
    
    @Override
    public boolean isActive() {
        return config != null && config.isActive();
    }
}
