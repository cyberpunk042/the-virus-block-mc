package net.cyberpunk042.client.field.render.effect;

import net.cyberpunk042.visual.animation.RayMotionConfig;
import net.cyberpunk042.visual.animation.MotionMode;

/**
 * Motion effect - applies vertex position transformation based on RayMotionConfig.
 * 
 * <p>Based on RaysRenderer.applyMotion - transforms ray geometry in world space.</p>
 * 
 * @see net.cyberpunk042.visual.animation.RayMotionConfig
 */
public final class RenderMotionEffect implements RenderVertexEffect {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    private final RayMotionConfig config;
    private final float time;
    
    public RenderMotionEffect(RayMotionConfig config, float time) {
        this.config = config;
        this.time = time;
    }
    
    @Override
    public void apply(float[] position, RenderEffectContext ctx) {
        if (config == null || !config.isActive()) {
            return;
        }
        
        MotionMode mode = config.mode();
        float speed = Math.max(0.1f, config.speed());
        float amp = Math.max(0.1f, config.amplitude());
        float freq = Math.max(0.5f, config.frequency());
        int rayIndex = ctx.rayIndex();
        
        float x = position[0];
        float y = position[1];
        float z = position[2];
        
        // Based on RaysRenderer.applyMotion lines 1131-1269
        float[] result = switch (mode) {
            case RADIAL_DRIFT -> {
                // Ray slides outward along radial direction
                float dist = (float) Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float nx = x / dist;
                float nz = z / dist;
                float offset = (time * speed * 0.5f) % 1.0f;
                float displacement = offset * amp * 2.0f;
                yield new float[]{x + nx * displacement, y, z + nz * displacement};
            }
            case RADIAL_OSCILLATE -> {
                // Ray oscillates in/out radially (sine wave)
                float dist = (float) Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float nx = x / dist;
                float nz = z / dist;
                float wave = (float) Math.sin(time * speed * Math.PI) * amp;
                yield new float[]{x + nx * wave, y, z + nz * wave};
            }
            case ANGULAR_OSCILLATE -> {
                // Ray sways side-to-side angularly around center
                float dist = (float) Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float angle = (float) Math.sin(time * speed * Math.PI) * amp * 0.5f;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case ANGULAR_DRIFT -> {
                // Ray slowly rotates around center
                float angle = time * speed * 0.5f;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case ORBIT -> {
                // Ray revolves around center
                float dist = (float) Math.sqrt(x * x + z * z);
                float spiralAngle = dist * freq * 2.0f + time * speed * 0.5f;
                float cos = (float) Math.cos(spiralAngle);
                float sin = (float) Math.sin(spiralAngle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case FLOAT -> {
                // Ray bobs up and down on Y axis
                float wave = (float) Math.sin(time * speed * Math.PI + rayIndex * 0.5f) * amp;
                yield new float[]{x, y + wave, z};
            }
            case SWAY -> {
                // Ray tip waves while base stays fixed
                float dist = (float) Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float swayAmount = dist * 0.3f;
                float angle = (float) Math.sin(time * speed * Math.PI) * amp * swayAmount;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                yield new float[]{x * cos - z * sin, y, x * sin + z * cos};
            }
            case JITTER -> {
                // Random position noise
                float hx = hash(rayIndex, (int)(time * speed * 10)) * 2 - 1;
                float hy = hash(rayIndex + 1000, (int)(time * speed * 10)) * 2 - 1;
                float hz = hash(rayIndex + 2000, (int)(time * speed * 10)) * 2 - 1;
                yield new float[]{
                    x + hx * amp * 0.3f,
                    y + hy * amp * 0.3f,
                    z + hz * amp * 0.3f
                };
            }
            case PRECESS -> {
                // Ray axis traces a cone pattern
                float dist = (float) Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float rayAngle = (float) Math.atan2(z, x);
                float precAngle = time * speed;
                // Normalize angle difference to [-PI, PI] using modulo (constant time, no loops!)
                float angleDiff = rayAngle - precAngle;
                angleDiff = (float) ((angleDiff + Math.PI) % TWO_PI);
                if (angleDiff < 0) angleDiff += TWO_PI;
                angleDiff -= (float) Math.PI;
                
                float tiltFactor = (float) Math.cos(angleDiff);
                float displacement = tiltFactor * amp * 1.0f;
                float nx = x / dist;
                float nz = z / dist;
                yield new float[]{x + nx * displacement, y, z + nz * displacement};
            }
            case RIPPLE -> {
                // Radial wave pulses outward from center
                float dist = (float) Math.sqrt(x * x + z * z);
                if (dist < 0.001f) {
                    yield new float[]{x, y, z};
                }
                float phase = dist * freq - time * speed * 2.0f;
                float wave = (float) Math.sin(phase * Math.PI * 2.0f) * amp * 0.8f;
                float nx = x / dist;
                float nz = z / dist;
                yield new float[]{x + nx * wave, y, z + nz * wave};
            }
            default -> new float[]{x, y, z};
        };
        
        position[0] = result[0];
        position[1] = result[1];
        position[2] = result[2];
    }
    
    /**
     * Simple hash function for deterministic pseudo-random values.
     */
    private float hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h & 0x7FFFFFFF) / (float)0x7FFFFFFF;
    }
    
    @Override
    public boolean isActive() {
        return config != null && config.isActive();
    }
}
