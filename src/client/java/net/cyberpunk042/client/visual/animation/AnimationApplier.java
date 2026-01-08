package net.cyberpunk042.client.visual.animation;

import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.animation.SpinConfig;
import net.cyberpunk042.visual.animation.PulseConfig;
import net.cyberpunk042.visual.animation.PulseMode;
import net.cyberpunk042.visual.animation.WobbleConfig;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.animation.AlphaPulseConfig;
import net.cyberpunk042.visual.animation.ColorCycleConfig;
import net.cyberpunk042.visual.animation.PrecessionConfig;
import net.cyberpunk042.visual.animation.Waveform;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Applies {@link Animation} effects to a {@link MatrixStack}.
 * 
 * <p>Animation effects:</p>
 * <ul>
 *   <li>Spin - Continuous rotation</li>
 *   <li>Pulse - Scale oscillation</li>
 *   <li>Wobble - Random offset (future)</li>
 *   <li>Wave - Sinusoidal deformation (future)</li>
 * </ul>
 * 
 * @see Animation
 * @see SpinConfig
 * @see PulseConfig
 */
public final class AnimationApplier {
    
    private AnimationApplier() {}
    
    // =========================================================================
    // Full Application
    // =========================================================================
    
    /**
     * Applies all animation effects to the matrix stack.
     * 
     * @param matrices The matrix stack to modify
     * @param animation The animation configuration
     * @param time Current time in ticks
     */
    public static void apply(MatrixStack matrices, Animation animation, float time) {
        if (animation == null || !animation.isActive()) {
            return;
        }
        
        // Apply phase offset
        float effectiveTime = time + (animation.phase() * 100);  // phase is 0-1
        
        // 1. Precession (tilt axis wobble) - applied FIRST so spin rotates within tilted frame
        if (animation.hasPrecession()) {
            applyPrecession(matrices, animation.precession(), effectiveTime);
        }
        
        // 2. Spin
        if (animation.hasSpin()) {
            applySpin(matrices, animation.spin(), effectiveTime);
        }
        
        // 3. Pulse (scale)
        if (animation.hasPulse()) {
            applyPulse(matrices, animation.pulse(), effectiveTime);
        }
        
        // 4. Wobble
        if (animation.hasWobble()) {
            applyWobble(matrices, animation.wobble(), effectiveTime);
        }
        
        // 5. Wave - affects vertices, not matrix transform. Applied in tessellator.
        // if (animation.hasWave()) { ... }
    }
    
    // =========================================================================
    // Individual Effects
    // =========================================================================
    
    /**
     * Applies spin rotation on all three axes.
     * Uses YXZ rotation order (common for 3D applications).
     */
    public static void applySpin(MatrixStack matrices, SpinConfig spin, float time) {
        if (spin == null || !spin.isActive()) return;
        
        Quaternionf rotation = new Quaternionf();
        
        // Apply Y rotation first (most common, usually the "look around" axis)
        if (spin.speedY() != 0) {
            float angleY = calculateSpinAngle(spin.speedY(), spin.oscillateY(), spin.rangeY(), time);
            rotation.rotateY(angleY);
        }
        
        // Then X rotation (pitch)
        if (spin.speedX() != 0) {
            float angleX = calculateSpinAngle(spin.speedX(), spin.oscillateX(), spin.rangeX(), time);
            rotation.rotateX(angleX);
        }
        
        // Finally Z rotation (roll)
        if (spin.speedZ() != 0) {
            float angleZ = calculateSpinAngle(spin.speedZ(), spin.oscillateZ(), spin.rangeZ(), time);
            rotation.rotateZ(angleZ);
        }
        
        matrices.multiply(rotation);
        Logging.ANIMATION.topic("spin").trace("Spin: X={}, Y={}, Z={}", spin.speedX(), spin.speedY(), spin.speedZ());
    }
    
    /**
     * Calculates the rotation angle for a single axis.
     * 
     * @param speed Speed in degrees/second
     * @param oscillate Whether to oscillate back and forth
     * @param range Oscillation range in degrees
     * @param time Current time in ticks
     * @return Angle in radians
     */
    private static float calculateSpinAngle(float speed, boolean oscillate, float range, float time) {
        // speed is in degrees per second, time is in ticks (20 ticks/second)
        // angle_rad = (time / 20) * speed_deg * (π/180) = time * speed * (π / 3600)
        float speedRadPerTick = (float) (speed * Math.PI / 3600.0);
        
        if (oscillate) {
            // Oscillate within range
            float rangeRad = (float) Math.toRadians(range);
            float progress = MathHelper.sin(time * speedRadPerTick);
            return progress * rangeRad / 2;
        } else {
            // Continuous rotation
            return time * speedRadPerTick;
        }
    }
    
    /**
     * Applies precession (axis wobble) animation.
     * 
     * <p>Precession tilts the shape axis and rotates that tilt around the Y axis,
     * creating a lighthouse/gyroscopic wobble effect. Works for any shape.</p>
     * 
     * <p>The transform order is:
     * <ol>
     *   <li>Rotate around Y by precession angle (positions the tilt direction)</li>
     *   <li>Tilt around X by tilt angle (creates the off-axis lean)</li>
     *   <li>Rotate back around Y to maintain local orientation</li>
     * </ol>
     * 
     * @param matrices The matrix stack to modify
     * @param precession The precession configuration
     * @param time Current time in ticks
     */
    public static void applyPrecession(MatrixStack matrices, PrecessionConfig precession, float time) {
        if (precession == null || !precession.isActive()) return;
        
        // Calculate the current angle around Y for the wobble direction
        // speed is in revolutions per second, time is in ticks (20 ticks/second)
        float precessionAngle = precession.getCurrentAngle(time / 20f);
        
        // Get tilt angle in radians
        float tiltAngle = precession.tiltRadians();
        
        // Create the precession transform:
        // 1. Rotate around Y to position the tilt direction
        // 2. Tilt around X
        // 3. The combination creates an axis that traces a cone around Y
        Quaternionf rotation = new Quaternionf()
            .rotateY(precessionAngle)      // Position the tilt direction
            .rotateX(tiltAngle);           // Apply the tilt
        
        matrices.multiply(rotation);
        
        Logging.ANIMATION.topic("precession").trace(
            "Precession: tilt={}°, angle={}rad", precession.tiltAngle(), precessionAngle);
    }
    
    /**
     * Applies pulse based on mode.
     * <ul>
     *   <li>SCALE: applies to MatrixStack</li>
     *   <li>ALPHA/GLOW/COLOR: use {@link #getPulseValue} instead</li>
     * </ul>
     */
    public static void applyPulse(MatrixStack matrices, PulseConfig pulse, float time) {
        if (pulse == null || !pulse.isActive()) return;
        
        // Only apply to matrices if mode is SCALE
        if (pulse.mode() == PulseMode.SCALE) {
            float scale = getPulseValue(pulse, time);
            matrices.scale(scale, scale, scale);
            Logging.ANIMATION.topic("pulse").trace("Pulse SCALE: {}", scale);
        }
    }
    
    /**
     * Gets the pulse value for any mode.
     * @return Value between min and max based on waveform
     */
    public static float getPulseValue(PulseConfig pulse, float time) {
        if (pulse == null || !pulse.isActive()) return 1.0f;
        
        // Use Waveform.evaluate() for clean waveform calculation (returns 0-1)
        float normalizedWave = pulse.waveform().evaluate(time * pulse.speed());
        
        // Map wave (0 to 1) to range (min to max)
        return pulse.min() + normalizedWave * (pulse.max() - pulse.min());
    }
    
    /**
     * Gets pulse alpha multiplier (only if mode is ALPHA).
     */
    public static float getPulseAlpha(PulseConfig pulse, float time) {
        if (pulse == null || !pulse.isActive() || pulse.mode() != PulseMode.ALPHA) {
            return 1.0f;
        }
        return getPulseValue(pulse, time);
    }
    
    /**
     * Gets pulse glow multiplier (only if mode is GLOW).
     */
    public static float getPulseGlow(PulseConfig pulse, float time) {
        if (pulse == null || !pulse.isActive() || pulse.mode() != PulseMode.GLOW) {
            return 1.0f;
        }
        return getPulseValue(pulse, time);
    }
    
    /**
     * Gets pulse color/hue shift (only if mode is COLOR).
     * @return Hue shift value (0-1 mapped from min-max)
     */
    public static float getPulseHueShift(PulseConfig pulse, float time) {
        if (pulse == null || !pulse.isActive() || pulse.mode() != PulseMode.COLOR) {
            return 0.0f;
        }
        // For color mode, return the raw wave value (0-1) for hue cycling
        float normalizedWave = pulse.waveform().evaluate(time * pulse.speed());
        return normalizedWave;
    }
    
    /**
     * Applies wobble offset.
     * Uses {@link MathHelper#sin} for fast lookup-table based sine.
     */
    public static void applyWobble(MatrixStack matrices, WobbleConfig wobble, float time) {
        if (wobble == null) {
            Logging.ANIMATION.topic("wobble").debug("[WOBBLE-DEBUG] wobble config is null");
            return;
        }
        if (!wobble.isActive()) {
            Logging.ANIMATION.topic("wobble").debug("[WOBBLE-DEBUG] isActive=false, amp={}, speed={}", 
                wobble.amplitude(), wobble.speed());
            return;
        }
        
        Vector3f amplitude = wobble.amplitude();
        if (amplitude == null) {
            return;
        }
        
        // Use modulo to prevent integer overflow in sin lookup table
        float safeTime = time % 1000f;
        
        // Generate pseudo-random wobble using MathHelper.sin (fast lookup table)
        float wobbleX = MathHelper.sin(safeTime * wobble.speed() * 1.0f) * amplitude.x;
        float wobbleY = MathHelper.sin(safeTime * wobble.speed() * 1.3f) * amplitude.y;
        float wobbleZ = MathHelper.sin(safeTime * wobble.speed() * 0.7f) * amplitude.z;
        
        matrices.translate(wobbleX, wobbleY, wobbleZ);
    }
    
    // =========================================================================
    // Modifiers: Bobbing & Breathing
    // =========================================================================
    
    /**
     * Applies bobbing (vertical oscillation) from Modifiers.
     * 
     * @param matrices MatrixStack to modify
     * @param bobbing Bobbing strength (0-1)
     * @param time Current time in ticks
     */
    public static void applyBobbing(MatrixStack matrices, float bobbing, float time) {
        if (bobbing <= 0) return;
        
        // Use modulo to prevent integer overflow in sin lookup table
        // 1000 gives us ~157 seconds of unique values before repeating
        float safeTime = time % 1000f;
        
        // Visible bob - ~3 second cycle at 20 tps, up to 0.4 blocks movement
        float amplitude = bobbing * 0.4f;  // Max 0.4 blocks at full strength
        float y = MathHelper.sin(safeTime * 0.4f) * amplitude;
        
        matrices.translate(0, y, 0);
    }
    
    /**
     * Applies breathing (scale oscillation) from Modifiers.
     * 
     * @param matrices MatrixStack to modify
     * @param breathing Breathing strength (0-1)
     * @param time Current time in ticks
     */
    public static void applyBreathing(MatrixStack matrices, float breathing, float time) {
        if (breathing <= 0) return;
        
        // Use modulo to prevent integer overflow in sin lookup table
        float safeTime = time % 1000f;
        
        // Visible breathing - ~4 second cycle, up to ±25% scale change
        float normalizedWave = (MathHelper.sin(safeTime * 0.2f) + 1) / 2;  // 0-1
        float scaleVariation = breathing * 0.25f;  // Max ±25% at full strength
        float scale = 1.0f + (normalizedWave - 0.5f) * scaleVariation * 2;
        
        matrices.scale(scale, scale, scale);
    }
    
    /**
     * Applies all modifier animations (bobbing + breathing).
     * 
     * @param matrices MatrixStack to modify
     * @param modifiers The Modifiers config
     * @param time Current time in ticks
     */
    public static void applyModifiers(MatrixStack matrices, net.cyberpunk042.field.Modifiers modifiers, float time) {
        if (modifiers == null) {
            return;
        }
        
        applyBobbing(matrices, modifiers.bobbing(), time);
        applyBreathing(matrices, modifiers.breathing(), time);
    }
    
    // =========================================================================
    // Alpha Pulse
    // =========================================================================
    
    /**
     * Calculates alpha pulse value (doesn't affect matrix).
     * Uses {@link Waveform#evaluate(float)} for waveform calculation.
     * 
     * @param alphaPulse The alpha pulse config
     * @param time Current time
     * @return Alpha multiplier (min to max)
     */
    public static float getAlphaPulse(AlphaPulseConfig alphaPulse, float time) {
        if (alphaPulse == null || !alphaPulse.isActive()) {
            return 1.0f;
        }
        
        // Use Waveform.evaluate() for clean calculation (returns 0-1)
        float normalizedWave = alphaPulse.waveform().evaluate(time * alphaPulse.speed());
        
        // Use MathHelper.lerp for smooth interpolation
        return MathHelper.lerp(normalizedWave, alphaPulse.min(), alphaPulse.max());
    }
    
    // =========================================================================
    // Color Cycle
    // =========================================================================
    
    /**
     * Calculates color cycle value (doesn't affect matrix).
     * Uses {@link ColorHelper#lerp} for smooth color blending.
     * 
     * <p>This implements smooth cycling through multiple colors:
     * <ul>
     *   <li>With blend=true: smoothly interpolates between adjacent colors</li>
     *   <li>With blend=false: hard cuts between colors</li>
     * </ul>
     * 
     * @param colorCycle The color cycle config
     * @param time Current time in ticks
     * @return ARGB color value
     */
    public static int getColorCycle(ColorCycleConfig colorCycle, float time) {
        if (colorCycle == null || !colorCycle.isActive()) {
            return 0xFFFFFFFF; // Default white
        }
        
        java.util.List<String> colors = colorCycle.colors();
        if (colors == null || colors.isEmpty()) {
            return 0xFFFFFFFF;
        }
        
        int colorCount = colors.size();
        
        // Calculate position in cycle (0 to colorCount)
        float cyclePos = (time * colorCycle.speed() * 0.05f) % colorCount;
        if (cyclePos < 0) cyclePos += colorCount;
        
        int index1 = (int) cyclePos;
        int index2 = (index1 + 1) % colorCount;
        
        int color1 = parseHexColor(colors.get(index1));
        int color2 = parseHexColor(colors.get(index2));
        
        if (colorCycle.blend()) {
            // Smooth blend using ColorHelper.lerp (Minecraft native)
            float blendFactor = cyclePos - index1;
            return ColorHelper.lerp(blendFactor, color1, color2);
        } else {
            // Hard cut
            return color1;
        }
    }
    
    /**
     * Parses a hex color string (#RRGGBB or #AARRGGBB).
     */
    private static int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0xFFFFFFFF;
        
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (cleaned.length() == 6) {
                // RGB only, add full alpha
                return 0xFF000000 | Integer.parseUnsignedInt(cleaned, 16);
            } else if (cleaned.length() == 8) {
                // ARGB
                return Integer.parseUnsignedInt(cleaned, 16);
            }
        } catch (NumberFormatException e) {
            Logging.ANIMATION.topic("color").warn("Invalid hex color: {}", hex);
        }
        return 0xFFFFFFFF;
    }
    
    // =========================================================================
    // Wave Displacement
    // =========================================================================
    
    /**
     * Calculates wave displacement for a vertex position.
     * 
     * <p>Wave animation creates a sinusoidal surface deformation based on
     * vertex position and time. Uses {@link MathHelper#sin} for fast calculation.</p>
     * 
     * <p>The wave travels across the surface perpendicular to the direction axis:
     * <ul>
     *   <li>Direction Y: wave travels along XZ plane, displaces in Y</li>
     *   <li>Direction X: wave travels along YZ plane, displaces in X</li>
     *   <li>Direction Z: wave travels along XY plane, displaces in Z</li>
     * </ul>
     * 
     * @param wave The wave configuration
     * @param x Vertex X position
     * @param y Vertex Y position
     * @param z Vertex Z position
     * @param time Current time in ticks
     * @return Displacement vector [dx, dy, dz]
     */
    public static float[] getWaveDisplacement(WaveConfig wave, float x, float y, float z, float time) {
        if (wave == null || !wave.isActive()) {
            return new float[] {0, 0, 0};
        }
        
        float amplitude = wave.amplitude();
        float frequency = wave.frequency();
        
        // Calculate wave phase based on position perpendicular to direction
        float phase;
        switch (wave.direction()) {
            case X -> phase = (y + z) * frequency + time * 0.1f;
            case Y -> phase = (x + z) * frequency + time * 0.1f;
            case Z -> phase = (x + y) * frequency + time * 0.1f;
            default -> phase = (x + z) * frequency + time * 0.1f;
        }
        
        // Calculate displacement using MathHelper.sin (fast lookup table)
        float displacement = MathHelper.sin(phase) * amplitude;
        
        // Apply displacement along the direction axis
        return switch (wave.direction()) {
            case X -> new float[] {displacement, 0, 0};
            case Y -> new float[] {0, displacement, 0};
            case Z -> new float[] {0, 0, displacement};
            default -> new float[] {0, displacement, 0};
        };
    }
    
    /**
     * Applies wave displacement to a vertex position, returning new coordinates.
     * 
     * @param wave The wave configuration
     * @param x Original X
     * @param y Original Y
     * @param z Original Z
     * @param time Current time
     * @return New position [x, y, z] with wave applied
     */
    public static float[] applyWaveToVertex(WaveConfig wave, float x, float y, float z, float time) {
        float[] displacement = getWaveDisplacement(wave, x, y, z, time);
        return new float[] {
            x + displacement[0],
            y + displacement[1],
            z + displacement[2]
        };
    }
}
