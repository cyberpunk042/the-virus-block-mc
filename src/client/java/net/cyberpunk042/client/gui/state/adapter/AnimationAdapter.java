package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.*;
import org.joml.Vector3f;

import java.util.Set;

/**
 * Adapter for animation-related state (spin, pulse, wave, wobble, colorCycle).
 * 
 * <p>Handles paths like "spin.speed", "pulse.scale", "alphaPulse.min".</p>
 */
@StateCategory("animation")
public class AnimationAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private SpinConfig spin = SpinConfig.NONE;
    @StateField private PulseConfig pulse = PulseConfig.NONE;
    @StateField private AlphaPulseConfig alphaPulse = AlphaPulseConfig.NONE;
    @StateField private WobbleConfig wobble = WobbleConfig.NONE;
    @StateField private WaveConfig wave = WaveConfig.NONE;
    @StateField private ColorCycleConfig colorCycle = ColorCycleConfig.NONE;
    @StateField private PrecessionConfig precession = null;
    @StateField private TravelEffectConfig travelEffect = null;
    @StateField private RayFlowConfig rayFlow = null;
    @StateField private RayMotionConfig rayMotion = null;
    @StateField private RayWiggleConfig rayWiggle = null;
    @StateField private RayTwistConfig rayTwist = null;
    
    @Override
    public String category() { return "animation"; }
    
    @Override
    public void loadFrom(Primitive source) {
        Animation anim = source.animation();
        if (anim != null) {
            this.spin = orDefault(anim.spin(), SpinConfig.NONE);
            this.pulse = orDefault(anim.pulse(), PulseConfig.NONE);
            this.alphaPulse = orDefault(anim.alphaPulse(), AlphaPulseConfig.NONE);
            this.wobble = orDefault(anim.wobble(), WobbleConfig.NONE);
            this.wave = orDefault(anim.wave(), WaveConfig.NONE);
            this.colorCycle = orDefault(anim.colorCycle(), ColorCycleConfig.NONE);
            this.precession = anim.precession();
            this.travelEffect = anim.travelEffect();
            this.rayFlow = anim.rayFlow();
            this.rayMotion = anim.rayMotion();
            this.rayWiggle = anim.rayWiggle();
            this.rayTwist = anim.rayTwist();
        } else {
            reset();
        }
        Logging.GUI.topic("adapter").trace("AnimationAdapter loaded");
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.animation(Animation.builder()
            .spin(spin)
            .pulse(pulse)
            .alphaPulse(alphaPulse)
            .wobble(wobble)
            .wave(wave)
            .colorCycle(colorCycle)
            .precession(precession)
            .travelEffect(travelEffect)
            .rayFlow(rayFlow)
            .rayMotion(rayMotion)
            .rayWiggle(rayWiggle)
            .rayTwist(rayTwist)
            .build());
    }
    
    // Typed accessors
    public SpinConfig spin() { return spin; }
    public void setSpin(SpinConfig spin) { this.spin = spin; }
    
    public PulseConfig pulse() { return pulse; }
    public void setPulse(PulseConfig pulse) { this.pulse = pulse; }
    
    public AlphaPulseConfig alphaPulse() { return alphaPulse; }
    public void setAlphaPulse(AlphaPulseConfig alphaPulse) { this.alphaPulse = alphaPulse; }
    
    public WobbleConfig wobble() { return wobble; }
    public void setWobble(WobbleConfig wobble) { this.wobble = wobble; }
    
    public WaveConfig wave() { return wave; }
    public void setWave(WaveConfig wave) { this.wave = wave; }
    
    public ColorCycleConfig colorCycle() { return colorCycle; }
    public void setColorCycle(ColorCycleConfig colorCycle) { this.colorCycle = colorCycle; }
    
    public PrecessionConfig precession() { return precession; }
    public void setPrecession(PrecessionConfig precession) { this.precession = precession; }
    
    public TravelEffectConfig travelEffect() { return travelEffect; }
    public void setTravelEffect(TravelEffectConfig travelEffect) { this.travelEffect = travelEffect; }
    
    public RayFlowConfig rayFlow() { return rayFlow; }
    public void setRayFlow(RayFlowConfig rayFlow) { this.rayFlow = rayFlow; }
    
    public RayMotionConfig rayMotion() { return rayMotion; }
    public void setRayMotion(RayMotionConfig rayMotion) { this.rayMotion = rayMotion; }
    
    public RayWiggleConfig rayWiggle() { return rayWiggle; }
    public void setRayWiggle(RayWiggleConfig rayWiggle) { this.rayWiggle = rayWiggle; }
    
    public RayTwistConfig rayTwist() { return rayTwist; }
    public void setRayTwist(RayTwistConfig rayTwist) { this.rayTwist = rayTwist; }
    
    /**
     * Override get to handle paths like "spin.speed", "pulse.scale", etc.
     */
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String field = parts[0];
        String prop = parts.length > 1 ? parts[1] : null;
        
        return switch (field) {
            case "spin" -> prop != null ? getSpinProperty(prop) : spin;
            case "pulse" -> prop != null ? getPulseProperty(prop) : pulse;
            case "alphaPulse" -> prop != null ? getAlphaPulseProperty(prop) : alphaPulse;
            case "wobble" -> prop != null ? getWobbleProperty(prop) : wobble;
            case "wave" -> prop != null ? getWaveProperty(prop) : wave;
            case "colorCycle" -> prop != null ? getColorCycleProperty(prop) : colorCycle;
            case "precession" -> prop != null ? getPrecessionProperty(prop) : precession;
            case "travelEffect" -> prop != null ? getTravelEffectProperty(prop) : travelEffect;
            case "rayFlow" -> prop != null ? getRayFlowProperty(prop) : rayFlow;
            case "rayMotion" -> prop != null ? getRayMotionProperty(prop) : rayMotion;
            case "rayWiggle" -> prop != null ? getRayWiggleProperty(prop) : rayWiggle;
            case "rayTwist" -> prop != null ? getRayTwistProperty(prop) : rayTwist;
            default -> super.get(path);
        };
    }
    
    private Object getSpinProperty(String prop) {
        return switch (prop) {
            case "speedX" -> spin.speedX();
            case "speedY" -> spin.speedY();
            case "speedZ" -> spin.speedZ();
            case "oscillateX" -> spin.oscillateX();
            case "oscillateY" -> spin.oscillateY();
            case "oscillateZ" -> spin.oscillateZ();
            case "rangeX" -> spin.rangeX();
            case "rangeY" -> spin.rangeY();
            case "rangeZ" -> spin.rangeZ();
            default -> null;
        };
    }
    
    private Object getPulseProperty(String prop) {
        return switch (prop) {
            case "speed" -> pulse.speed();
            case "scale" -> pulse.scale();
            default -> null;
        };
    }
    
    private Object getAlphaPulseProperty(String prop) {
        return switch (prop) {
            case "speed" -> alphaPulse.speed();
            case "min" -> alphaPulse.min();
            case "max" -> alphaPulse.max();
            default -> null;
        };
    }
    
    private Object getWobbleProperty(String prop) {
        return switch (prop) {
            case "speed" -> wobble.speed();
            case "randomize" -> wobble.randomize();
            // amplitude is a Vector3f - return x for scalar access
            case "amplitude" -> wobble.amplitude() != null ? wobble.amplitude().x : 0f;
            default -> null;
        };
    }
    
    private Object getWaveProperty(String prop) {
        return switch (prop) {
            case "speed" -> wave.speed();
            case "amplitude" -> wave.amplitude();
            case "frequency" -> wave.frequency();
            case "direction" -> wave.direction() != null ? wave.direction().name() : "OUTWARD";
            default -> null;
        };
    }
    
    private Object getColorCycleProperty(String prop) {
        return switch (prop) {
            case "speed" -> colorCycle.speed();
            case "blend" -> colorCycle.blend();
            case "colors" -> colorCycle.colors();
            default -> null;
        };
    }
    
    private Object getPrecessionProperty(String prop) {
        if (precession == null) return null;
        return switch (prop) {
            case "enabled" -> precession.enabled();
            case "tiltAngle" -> precession.tiltAngle();
            case "speed" -> precession.speed();
            case "phase" -> precession.phase();
            default -> null;
        };
    }
    
    /**
     * Override set to handle paths like "spin.speed", "pulse.scale", etc.
     */
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String field = parts[0];
        String prop = parts.length > 1 ? parts[1] : null;
        
        if (prop == null) {
            // Direct assignment of entire config object
            if (field.equals("precession")) {
                if (value == null) {
                    this.precession = null;
                } else if (value instanceof PrecessionConfig pc) {
                    this.precession = pc;
                }
                return;
            }
            super.set(path, value);
            return;
        }
        
        switch (field) {
            case "spin" -> setSpinProperty(prop, value);
            case "pulse" -> setPulseProperty(prop, value);
            case "alphaPulse" -> setAlphaPulseProperty(prop, value);
            case "wobble" -> setWobbleProperty(prop, value);
            case "wave" -> setWaveProperty(prop, value);
            case "colorCycle" -> setColorCycleProperty(prop, value);
            case "precession" -> setPrecessionProperty(prop, value);
            case "travelEffect" -> setTravelEffectProperty(prop, value);
            case "rayFlow" -> setRayFlowProperty(prop, value);
            case "rayMotion" -> setRayMotionProperty(prop, value);
            case "rayWiggle" -> setRayWiggleProperty(prop, value);
            case "rayTwist" -> setRayTwistProperty(prop, value);
            default -> super.set(path, value);
        }
    }
    
    private void setSpinProperty(String prop, Object value) {
        SpinConfig.Builder b = spin.toBuilder();
        switch (prop) {
            case "speedX" -> b.speedX(toFloat(value));
            case "speedY" -> b.speedY(toFloat(value));
            case "speedZ" -> b.speedZ(toFloat(value));
            case "oscillateX" -> b.oscillateX(toBool(value));
            case "oscillateY" -> b.oscillateY(toBool(value));
            case "oscillateZ" -> b.oscillateZ(toBool(value));
            case "rangeX" -> b.rangeX(toFloat(value));
            case "rangeY" -> b.rangeY(toFloat(value));
            case "rangeZ" -> b.rangeZ(toFloat(value));
        }
        this.spin = b.build();
    }
    
    private void setPulseProperty(String prop, Object value) {
        PulseConfig.Builder b = pulse.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "scale" -> b.scale(toFloat(value));
        }
        this.pulse = b.build();
    }
    
    private void setAlphaPulseProperty(String prop, Object value) {
        AlphaPulseConfig.Builder b = alphaPulse.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "min" -> b.min(toFloat(value));
            case "max" -> b.max(toFloat(value));
        }
        this.alphaPulse = b.build();
    }
    
    private void setWobbleProperty(String prop, Object value) {
        WobbleConfig.Builder b = wobble.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "randomize" -> b.randomize(toBool(value));
            case "amplitude" -> {
                // Handle both Vector3f and scalar float values
                if (value instanceof Vector3f vec) {
                    b.amplitude(vec);
                } else {
                    // For scalar amplitude, set uniform across all axes
                    float v = toFloat(value);
                    b.amplitude(new Vector3f(v, v * 0.5f, v));
                }
            }
        }
        this.wobble = b.build();
    }
    
    private void setWaveProperty(String prop, Object value) {
        WaveConfig.Builder b = wave.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "amplitude" -> b.amplitude(toFloat(value));
            case "frequency" -> b.frequency(toFloat(value));
            case "direction" -> b.direction(Axis.fromId(value.toString()));
        }
        this.wave = b.build();
    }
    
    private void setColorCycleProperty(String prop, Object value) {
        ColorCycleConfig.Builder b = colorCycle.toBuilder();
        switch (prop) {
            case "speed" -> b.speed(toFloat(value));
            case "blend" -> b.blend(toBool(value));
            // colors would need special handling for list types
        }
        this.colorCycle = b.build();
    }
    
    private void setPrecessionProperty(String prop, Object value) {
        // Handle null (disable precession)
        if (value == null && prop.isEmpty()) {
            this.precession = null;
            return;
        }
        // Handle setting entire PrecessionConfig
        if (value instanceof PrecessionConfig pc) {
            this.precession = pc;
            return;
        }
        // Build from current or create new
        PrecessionConfig.Builder b = precession != null ? precession.toBuilder() : PrecessionConfig.builder();
        switch (prop) {
            case "enabled" -> b.enabled(toBool(value));
            case "tiltAngle" -> b.tiltAngle(toFloat(value));
            case "speed" -> b.speed(toFloat(value));
            case "phase" -> b.phase(toFloat(value));
        }
        this.precession = b.build();
    }
    
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 0f; }
    private int toInt(Object v) { return v instanceof Number n ? n.intValue() : 0; }
    private boolean toBool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); }
    
    @Override
    public void reset() {
        this.spin = SpinConfig.NONE;
        this.pulse = PulseConfig.NONE;
        this.alphaPulse = AlphaPulseConfig.NONE;
        this.wobble = WobbleConfig.NONE;
        this.wave = WaveConfig.NONE;
        this.colorCycle = ColorCycleConfig.NONE;
        this.precession = null;
        this.travelEffect = null;
        this.rayFlow = null;
        this.rayMotion = null;
        this.rayWiggle = null;
        this.rayTwist = null;
    }
    
    // =========================================================================
    // Travel Effect Property Accessors (General - any shape)
    // =========================================================================
    
    private Object getTravelEffectProperty(String prop) {
        if (travelEffect == null) return null;
        return switch (prop) {
            case "enabled" -> travelEffect.enabled();
            case "mode" -> travelEffect.mode();
            case "speed" -> travelEffect.speed();
            case "blendMode" -> travelEffect.blendMode();
            case "minAlpha" -> travelEffect.minAlpha();
            case "intensity" -> travelEffect.intensity();
            case "direction" -> travelEffect.direction();
            case "travelDirection" -> travelEffect.travelDirection();
            case "dirX" -> travelEffect.dirX();
            case "dirY" -> travelEffect.dirY();
            case "dirZ" -> travelEffect.dirZ();
            case "count" -> travelEffect.count();
            case "width" -> travelEffect.width();
            default -> null;
        };
    }
    
    private void setTravelEffectProperty(String prop, Object value) {
        // If travelEffect is null, create new builder with sensible defaults
        // (enabled + CHASE mode) so the effect actually works
        TravelEffectConfig.Builder b;
        if (travelEffect != null) {
            b = travelEffect.toBuilder();
        } else {
            // Create with defaults that will actually be active
            b = TravelEffectConfig.builder()
                .enabled(true)
                .mode(net.cyberpunk042.visual.energy.EnergyTravel.CHASE);
        }
        
        switch (prop) {
            case "enabled" -> b.enabled(toBool(value));
            case "mode" -> {
                var mode = value instanceof net.cyberpunk042.visual.energy.EnergyTravel et
                    ? et : net.cyberpunk042.visual.energy.EnergyTravel.fromString(value.toString());
                b.mode(mode);
                // Auto-enable when mode is set to something other than NONE
                b.enabled(mode != null && mode != net.cyberpunk042.visual.energy.EnergyTravel.NONE);
            }
            case "speed" -> b.speed(toFloat(value));
            case "blendMode" -> {
                var mode = value instanceof net.cyberpunk042.visual.energy.TravelBlendMode tbm
                    ? tbm : net.cyberpunk042.visual.energy.TravelBlendMode.fromString(value.toString());
                b.blendMode(mode);
            }
            case "minAlpha" -> b.minAlpha(toFloat(value));
            case "intensity" -> b.intensity(toFloat(value));
            case "direction" -> {
                var dir = value instanceof Axis a ? a : Axis.fromId(value.toString());
                b.direction(dir);
            }
            case "travelDirection" -> {
                var td = value instanceof TravelDirection t ? t : TravelDirection.fromString(value.toString());
                b.travelDirection(td);
            }
            case "dirX" -> b.dirX(toFloat(value));
            case "dirY" -> b.dirY(toFloat(value));
            case "dirZ" -> b.dirZ(toFloat(value));
            case "count" -> b.count(toInt(value));
            case "width" -> b.width(toFloat(value));
        }
        this.travelEffect = b.build();
    }
    
    // =========================================================================
    // Ray Flow Property Accessors
    // =========================================================================
    
    private Object getRayFlowProperty(String prop) {
        if (rayFlow == null) return null;
        return switch (prop) {
            case "radiativeEnabled" -> rayFlow.radiativeEnabled();
            case "radiativeSpeed" -> rayFlow.radiativeSpeed();
            case "travel" -> rayFlow.travel();
            case "travelEnabled" -> rayFlow.travelEnabled();
            case "travelSpeed" -> rayFlow.travelSpeed();
            case "chaseCount" -> rayFlow.chaseCount();
            case "chaseWidth" -> rayFlow.chaseWidth();
            case "travelBlendMode" -> rayFlow.travelBlendMode();
            case "travelMinAlpha" -> rayFlow.travelMinAlpha();
            case "travelIntensity" -> rayFlow.travelIntensity();
            case "flicker" -> rayFlow.flicker();
            case "flickerEnabled" -> rayFlow.flickerEnabled();
            case "flickerIntensity" -> rayFlow.flickerIntensity();
            case "flickerFrequency" -> rayFlow.flickerFrequency();
            case "skipSpawnTransition" -> rayFlow.skipSpawnTransition();
            case "pathFollowing" -> rayFlow.pathFollowing();
            default -> null;
        };
    }
    
    private void setRayFlowProperty(String prop, Object value) {
        RayFlowConfig.Builder b = rayFlow != null ? rayFlow.toBuilder() : RayFlowConfig.builder();
        switch (prop) {
            case "radiativeEnabled" -> b.radiativeEnabled(toBool(value));
            case "radiativeSpeed" -> b.radiativeSpeed(toFloat(value));
            case "travel" -> {
                var mode = value instanceof net.cyberpunk042.visual.energy.EnergyTravel et 
                    ? et : net.cyberpunk042.visual.energy.EnergyTravel.fromString(value.toString());
                b.travel(mode);
                // Auto-enable when non-NONE, auto-disable when NONE
                b.travelEnabled(mode != null && mode != net.cyberpunk042.visual.energy.EnergyTravel.NONE);
            }
            case "travelEnabled" -> b.travelEnabled(toBool(value));
            case "travelSpeed" -> b.travelSpeed(toFloat(value));
            case "chaseCount" -> b.chaseCount(toInt(value));
            case "chaseWidth" -> b.chaseWidth(toFloat(value));
            case "flicker" -> {
                var mode = value instanceof net.cyberpunk042.visual.energy.EnergyFlicker ef 
                    ? ef : net.cyberpunk042.visual.energy.EnergyFlicker.fromString(value.toString());
                b.flicker(mode);
                // Auto-enable when non-NONE, auto-disable when NONE
                b.flickerEnabled(mode != null && mode != net.cyberpunk042.visual.energy.EnergyFlicker.NONE);
            }
            case "flickerEnabled" -> b.flickerEnabled(toBool(value));
            case "flickerIntensity" -> b.flickerIntensity(toFloat(value));
            case "flickerFrequency" -> b.flickerFrequency(toFloat(value));
            case "skipSpawnTransition" -> b.skipSpawnTransition(toBool(value));
            case "pathFollowing" -> b.pathFollowing(toBool(value));
            case "travelBlendMode" -> {
                var mode = value instanceof net.cyberpunk042.visual.energy.TravelBlendMode tbm
                    ? tbm : net.cyberpunk042.visual.energy.TravelBlendMode.fromString(value.toString());
                b.travelBlendMode(mode);
            }
            case "travelMinAlpha" -> b.travelMinAlpha(toFloat(value));
            case "travelIntensity" -> b.travelIntensity(toFloat(value));
        }
        this.rayFlow = b.build();
    }
    
    // =========================================================================
    // Ray Motion Property Accessors
    // =========================================================================
    
    private Object getRayMotionProperty(String prop) {
        if (rayMotion == null) return null;
        return switch (prop) {
            case "mode" -> rayMotion.mode();
            case "speed" -> rayMotion.speed();
            case "directionX" -> rayMotion.directionX();
            case "directionY" -> rayMotion.directionY();
            case "directionZ" -> rayMotion.directionZ();
            case "amplitude" -> rayMotion.amplitude();
            case "frequency" -> rayMotion.frequency();
            default -> null;
        };
    }
    
    private void setRayMotionProperty(String prop, Object value) {
        RayMotionConfig.Builder b = rayMotion != null ? rayMotion.toBuilder() : RayMotionConfig.builder();
        switch (prop) {
            case "mode" -> b.mode(value instanceof MotionMode mm ? mm : MotionMode.fromString(value.toString()));
            case "speed" -> b.speed(toFloat(value));
            case "directionX" -> {
                float x = toFloat(value);
                float y = rayMotion != null ? rayMotion.directionY() : 1f;
                float z = rayMotion != null ? rayMotion.directionZ() : 0f;
                b.direction(x, y, z);
            }
            case "directionY" -> {
                float x = rayMotion != null ? rayMotion.directionX() : 0f;
                float y = toFloat(value);
                float z = rayMotion != null ? rayMotion.directionZ() : 0f;
                b.direction(x, y, z);
            }
            case "directionZ" -> {
                float x = rayMotion != null ? rayMotion.directionX() : 0f;
                float y = rayMotion != null ? rayMotion.directionY() : 1f;
                float z = toFloat(value);
                b.direction(x, y, z);
            }
            case "amplitude" -> b.amplitude(toFloat(value));
            case "frequency" -> b.frequency(toFloat(value));
        }
        this.rayMotion = b.build();
    }
    
    // =========================================================================
    // Ray Wiggle Property Accessors
    // =========================================================================
    
    private Object getRayWiggleProperty(String prop) {
        if (rayWiggle == null) return null;
        return switch (prop) {
            case "mode" -> rayWiggle.mode();
            case "speed" -> rayWiggle.speed();
            case "amplitude" -> rayWiggle.amplitude();
            case "frequency" -> rayWiggle.frequency();
            case "phaseOffset" -> rayWiggle.phaseOffset();
            default -> null;
        };
    }
    
    private void setRayWiggleProperty(String prop, Object value) {
        RayWiggleConfig.Builder b = rayWiggle != null ? rayWiggle.toBuilder() : RayWiggleConfig.builder();
        switch (prop) {
            case "mode" -> b.mode(value instanceof WiggleMode wm ? wm : WiggleMode.fromString(value.toString()));
            case "speed" -> b.speed(toFloat(value));
            case "amplitude" -> b.amplitude(toFloat(value));
            case "frequency" -> b.frequency(toFloat(value));
            case "phaseOffset" -> b.phaseOffset(toFloat(value));
        }
        this.rayWiggle = b.build();
    }
    
    // =========================================================================
    // Ray Twist Property Accessors
    // =========================================================================
    
    private Object getRayTwistProperty(String prop) {
        if (rayTwist == null) return null;
        return switch (prop) {
            case "mode" -> rayTwist.mode();
            case "speed" -> rayTwist.speed();
            case "amount" -> rayTwist.amount();
            case "phaseOffset" -> rayTwist.phaseOffset();
            default -> null;
        };
    }
    
    private void setRayTwistProperty(String prop, Object value) {
        RayTwistConfig.Builder b = rayTwist != null ? rayTwist.toBuilder() : RayTwistConfig.builder();
        switch (prop) {
            case "mode" -> b.mode(value instanceof TwistMode tm ? tm : TwistMode.fromString(value.toString()));
            case "speed" -> b.speed(toFloat(value));
            case "amount" -> b.amount(toFloat(value));
            case "phaseOffset" -> b.phaseOffset(toFloat(value));
        }
        this.rayTwist = b.build();
    }
}
