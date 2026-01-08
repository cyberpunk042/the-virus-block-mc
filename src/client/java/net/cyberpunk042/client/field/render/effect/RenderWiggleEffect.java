package net.cyberpunk042.client.field.render.effect;

import net.cyberpunk042.visual.animation.RayWiggleConfig;
import net.cyberpunk042.visual.animation.WiggleMode;

/**
 * Wiggle effect - deforms ray shape with snake-like motion.
 * 
 * <p>Based on RaysRenderer.applyWiggle - uses proper perpendicular frame calculation
 * to displace vertices perpendicular to the ray direction.</p>
 * 
 * @see net.cyberpunk042.visual.animation.RayWiggleConfig
 */
public final class RenderWiggleEffect implements RenderVertexEffect {
    
    private static final float TWO_PI = (float) (Math.PI * 2);
    
    private final RayWiggleConfig config;
    private final float time;
    
    public RenderWiggleEffect(RayWiggleConfig config, float time) {
        this.config = config;
        this.time = time;
    }
    
    @Override
    public void apply(float[] position, RenderEffectContext ctx) {
        if (config == null || !config.isActive()) {
            return;
        }
        
        WiggleMode mode = config.mode();
        float speed = Math.max(0.1f, config.speed());
        float amp = Math.max(0.02f, config.amplitude());
        float freq = Math.max(0.5f, config.frequency());
        float phase = config.phaseOffset();
        
        float t = ctx.t();
        int rayIndex = ctx.rayIndex();
        
        // Get direction from context
        float dx = ctx.dx();
        float dy = ctx.dy();
        float dz = ctx.dz();
        
        // If no direction, default to Y-up
        if (Math.abs(dx) < 0.0001f && Math.abs(dy) < 0.0001f && Math.abs(dz) < 0.0001f) {
            dy = 1.0f;
        }
        
        // Compute perpendicular vectors for displacement
        // Based on RaysRenderer.computePerpendicular lines 1497-1525
        float[] perp = computePerpendicular(dx, dy, dz);
        float px = perp[0], py = perp[1], pz = perp[2];
        float ux = perp[3], uy = perp[4], uz = perp[5];
        
        float wave1 = 0, wave2 = 0;
        
        switch (mode) {
            case WIGGLE -> {
                // Snake-like: traveling sine wave in perpendicular direction
                wave1 = amp * (float) Math.sin((t * freq + time * speed + phase) * TWO_PI);
            }
            case WOBBLE -> {
                // Tip wobbles: amplitude increases with t
                wave1 = amp * t * (float) Math.sin((time * speed + phase) * TWO_PI);
            }
            case WRITHE -> {
                // 3D tentacle: combine two perpendicular sine waves
                wave1 = amp * (float) Math.sin((t * freq + time * speed + phase) * TWO_PI);
                wave2 = amp * (float) Math.cos((t * freq * 0.7f + time * speed * 1.3f) * TWO_PI);
            }
            case SHIMMER -> {
                // High-frequency, small amplitude noise
                float hashVal = hash(rayIndex, (int)(t * 100 + time * speed * 50));
                wave1 = amp * 0.3f * (hashVal * 2 - 1);
            }
            case RIPPLE -> {
                // Wave travels from base to tip
                wave1 = amp * (float) Math.sin((time * speed - t * freq + phase) * TWO_PI);
            }
            case WHIP -> {
                // Whip crack: sharp wave that travels and decays
                float progress = (time * speed) % 1.0f;
                float dist = Math.abs(t - progress);
                float envelope = Math.max(0, 1 - dist * 5) * t;
                wave1 = amp * envelope * (float) Math.sin(dist * 10 * TWO_PI);
            }
            case FLUTTER -> {
                // Rapid chaotic motion
                float h1 = hash(rayIndex, (int)(time * speed * 30 + t * 10));
                float h2 = hash(rayIndex + 1000, (int)(time * speed * 30 + t * 10));
                wave1 = amp * 0.5f * (h1 * 2 - 1);
                wave2 = amp * 0.5f * (h2 * 2 - 1);
            }
            case SNAKE -> {
                // Fluid slithering: multi-frequency blend
                wave1 = amp * (float) Math.sin((t * freq + time * speed) * TWO_PI);
                wave1 += amp * 0.5f * (float) Math.sin((t * freq * 2 + time * speed * 0.8f) * TWO_PI);
            }
            case PULSE_WAVE -> {
                // Radial pulsing: expand/contract perpendicular to ray
                wave1 = amp * (float) Math.sin((t * freq - time * speed) * TWO_PI);
                wave2 = wave1; // Apply in both perpendicular directions
            }
            default -> { /* NONE - do nothing */ }
        }
        
        // Apply displacement in perpendicular directions
        position[0] += px * wave1 + ux * wave2;
        position[1] += py * wave1 + uy * wave2;
        position[2] += pz * wave1 + uz * wave2;
    }
    
    /**
     * Computes two perpendicular vectors to a direction.
     * Returns [px, py, pz, ux, uy, uz] - two perpendicular unit vectors.
     * From RaysRenderer.computePerpendicular
     */
    private float[] computePerpendicular(float dx, float dy, float dz) {
        // Choose reference axis (avoid parallel)
        float refX, refY, refZ;
        if (Math.abs(dy) > 0.9f) {
            refX = 1; refY = 0; refZ = 0;
        } else {
            refX = 0; refY = 1; refZ = 0;
        }
        
        // p = normalize(d × ref)
        float px = dy * refZ - dz * refY;
        float py = dz * refX - dx * refZ;
        float pz = dx * refY - dy * refX;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen > 0.0001f) {
            px /= plen; py /= plen; pz /= plen;
        }
        
        // u = normalize(p × d)
        float ux = py * dz - pz * dy;
        float uy = pz * dx - px * dz;
        float uz = px * dy - py * dx;
        float ulen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ulen > 0.0001f) {
            ux /= ulen; uy /= ulen; uz /= ulen;
        }
        
        return new float[]{px, py, pz, ux, uy, uz};
    }
    
    /**
     * Simple hash function for deterministic pseudo-random values.
     * From RaysRenderer.hash
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
