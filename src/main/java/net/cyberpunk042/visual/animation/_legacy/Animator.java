package net.cyberpunk042.visual.animation;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.transform.Transform;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Utility class for applying animations to transforms.
 * 
 * <h2>Usage</h2>
 * <pre>
 * Animation anim = Animation.spinning(0.5f);
 * Transform base = Transform.identity();
 * Transform animated = Animator.apply(base, anim, worldTime);
 * </pre>
 */
public final class Animator {
    
    private Animator() {}
    
    // ─────────────────────────────────────────────────────────────────────────
    // Core Application
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Applies animation to a transform at the given time.
     * 
     * @param base Base transform
     * @param animation Animation to apply
     * @param time World time (ticks + partial)
     * @return New transform with animation applied
     */
    public static Transform apply(Transform base, Animation animation, float time) {
        if (animation == null || animation.equals(Animation.none())) {
            return base;
        }
        
        Logging.RENDER.topic("animator").trace(
            "Applying animation: spin={:.3f} pulse={:.3f} at time={:.1f}",
            animation.spin(), animation.pulse(), time);
        
        // Calculate animated values
        float rotation = animation.getRotation(time);
        float scale = animation.getScale(time);
        
        // Apply to base transform
        Vec3d offset = base.offset();
        Vec3d rot = base.rotation();
        if (rot == null) rot = Vec3d.ZERO;
        
        // Add spin to Y rotation
        Vec3d newRotation = new Vec3d(rot.x, rot.y + Math.toDegrees(rotation), rot.z);
        
        // Multiply scale
        float newScale = base.scale() * scale;
        
        return new Transform(offset, newRotation, newScale);
    }
    
    /**
     * Applies animation with phase offset.
     */
    public static Transform apply(Transform base, Animation animation, float time, float phaseOffset) {
        return apply(base, animation, time + phaseOffset);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Individual Animations
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Calculates spin rotation at given time.
     * 
     * @param spinSpeed Rotation speed (radians per tick)
     * @param time World time
     * @return Rotation angle in radians
     */
    public static float spin(float spinSpeed, float time) {
        return time * spinSpeed;
    }
    
    /**
     * Calculates pulse scale at given time.
     * 
     * @param pulseSpeed Pulse frequency
     * @param pulseAmount Pulse amplitude (0.1 = ±10%)
     * @param time World time
     * @return Scale multiplier
     */
    public static float pulse(float pulseSpeed, float pulseAmount, float time) {
        if (pulseSpeed <= 0 || pulseAmount <= 0) {
            return 1.0f;
        }
        float phase = time * pulseSpeed;
        return 1.0f + pulseAmount * MathHelper.sin(phase);
    }
    
    /**
     * Calculates wobble offset at given time.
     * 
     * @param wobbleSpeed Wobble frequency
     * @param wobbleAmount Wobble amplitude
     * @param time World time
     * @return Offset vector for wobble
     */
    public static Vec3d wobble(float wobbleSpeed, float wobbleAmount, float time) {
        if (wobbleSpeed <= 0 || wobbleAmount <= 0) {
            return Vec3d.ZERO;
        }
        float phase = time * wobbleSpeed;
        float x = MathHelper.sin(phase) * wobbleAmount;
        float z = MathHelper.cos(phase * 1.3f) * wobbleAmount;
        return new Vec3d(x, 0, z);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Interpolation
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Linearly interpolates between two transforms.
     */
    public static Transform lerp(Transform a, Transform b, float t) {
        t = MathHelper.clamp(t, 0, 1);
        
        Vec3d offsetA = a.offset() != null ? a.offset() : Vec3d.ZERO;
        Vec3d offsetB = b.offset() != null ? b.offset() : Vec3d.ZERO;
        Vec3d offset = offsetA.lerp(offsetB, t);
        
        Vec3d rotA = a.rotation() != null ? a.rotation() : Vec3d.ZERO;
        Vec3d rotB = b.rotation() != null ? b.rotation() : Vec3d.ZERO;
        Vec3d rotation = rotA.lerp(rotB, t);
        
        float scale = MathHelper.lerp(t, a.scale(), b.scale());
        
        return new Transform(offset, rotation, scale);
    }
    
    /**
     * Smoothly interpolates between transforms (ease in/out).
     */
    public static Transform smoothLerp(Transform a, Transform b, float t) {
        // Smoothstep: 3t² - 2t³
        t = MathHelper.clamp(t, 0, 1);
        t = t * t * (3 - 2 * t);
        return lerp(a, b, t);
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Easing Functions
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * Ease in (slow start).
     */
    public static float easeIn(float t) {
        return t * t;
    }
    
    /**
     * Ease out (slow end).
     */
    public static float easeOut(float t) {
        return 1 - (1 - t) * (1 - t);
    }
    
    /**
     * Ease in and out.
     */
    public static float easeInOut(float t) {
        return t < 0.5f 
            ? 2 * t * t 
            : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
    }
    
    /**
     * Bounce effect.
     */
    public static float bounce(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        
        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            return n1 * (t -= 1.5f / d1) * t + 0.75f;
        } else if (t < 2.5 / d1) {
            return n1 * (t -= 2.25f / d1) * t + 0.9375f;
        } else {
            return n1 * (t -= 2.625f / d1) * t + 0.984375f;
        }
    }
}
