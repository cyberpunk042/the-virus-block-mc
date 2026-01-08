package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Axis;
import net.cyberpunk042.visual.transform.*;
import org.joml.Vector3f;

/**
 * Adapter for transform state (offset, rotation, scale, orbit, orbit3d, etc.).
 * 
 * <p>Handles paths like "transform.offset", "transform.orbit.speed", 
 * "transform.orbit3d.x.amplitude".</p>
 */
@StateCategory("transform")
public class TransformAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private Transform transform = Transform.IDENTITY;
    
    @Override
    public String category() { return "transform"; }
    
    @Override
    public void loadFrom(Primitive source) {
        this.transform = orDefault(source.transform(), Transform.IDENTITY);
        Logging.GUI.topic("adapter").trace("TransformAdapter loaded");
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.transform(transform);
    }
    
    public Transform transform() { return transform; }
    public void setTransform(Transform transform) { this.transform = transform; }
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 3);
        if (parts.length < 2) return super.get(path);
        
        String field = parts[1];  // After "transform."
        String prop = parts.length > 2 ? parts[2] : null;
        
        return switch (field) {
            case "offset" -> transform.offset();
            case "rotation" -> transform.rotation();
            case "scale" -> transform.scale();
            case "scaleXYZ" -> transform.scaleXYZ();
            case "anchor" -> transform.anchor() != null ? transform.anchor().name() : "CENTER";
            case "facing" -> transform.facing() != null ? transform.facing().name() : "FIXED";
            case "billboard" -> transform.billboard() != null ? transform.billboard().name() : "NONE";
            case "scaleWithRadius" -> transform.scaleWithRadius();
            case "inheritRotation" -> transform.inheritRotation();
            case "orbit" -> prop != null ? getOrbitProperty(prop) : transform.orbit();
            case "orbit3d" -> prop != null ? getOrbit3dProperty(prop) : transform.orbit3d();
            default -> super.get(path);
        };
    }
    
    private Object getOrbitProperty(String prop) {
        OrbitConfig orbit = transform.orbit();
        if (orbit == null) return null;
        return switch (prop) {
            case "enabled" -> orbit.isActive();
            case "radius" -> orbit.radius();
            case "speed" -> orbit.speed();
            case "axis" -> orbit.axis() != null ? orbit.axis().name() : "Y";
            case "phase" -> orbit.phase();
            default -> null;
        };
    }
    
    private Object getOrbit3dProperty(String prop) {
        OrbitConfig3D orbit3d = transform.orbit3d();
        if (orbit3d == null) return null;
        
        String[] subParts = prop.split("\\.", 2);
        String axisOrProp = subParts[0];
        String axisProp = subParts.length > 1 ? subParts[1] : null;
        
        return switch (axisOrProp) {
            case "x" -> axisProp != null ? getAxisProperty(orbit3d.x(), axisProp) : orbit3d.x();
            case "y" -> axisProp != null ? getAxisProperty(orbit3d.y(), axisProp) : orbit3d.y();
            case "z" -> axisProp != null ? getAxisProperty(orbit3d.z(), axisProp) : orbit3d.z();
            case "coupling" -> orbit3d.coupling() != null ? orbit3d.coupling().name() : "INDEPENDENT";
            default -> null;
        };
    }
    
    private Object getAxisProperty(AxisMotionConfig config, String prop) {
        if (config == null) return null;
        return switch (prop) {
            case "mode" -> config.mode() != null ? config.mode().name() : "NONE";
            case "amplitude" -> config.amplitude();
            case "frequency" -> config.frequency();
            case "phase" -> config.phase();
            default -> null;
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 3);
        if (parts.length < 2) {
            super.set(path, value);
            return;
        }
        
        String field = parts[1];  // After "transform."
        String prop = parts.length > 2 ? parts[2] : null;
        
        Transform.Builder b = transform.toBuilder();
        
        switch (field) {
            case "offset" -> b.offset((Vector3f) value);
            case "rotation" -> b.rotation((Vector3f) value);
            case "scale" -> b.scale(toFloat(value));
            case "scaleXYZ" -> b.scaleXYZ((Vector3f) value);
            case "anchor" -> b.anchor(Anchor.valueOf(value.toString()));
            case "facing" -> b.facing(Facing.valueOf(value.toString()));
            case "billboard" -> b.billboard(Billboard.valueOf(value.toString()));
            case "scaleWithRadius" -> b.scaleWithRadius(toBool(value));
            case "inheritRotation" -> b.inheritRotation(toBool(value));
            case "orbit" -> setOrbitProperty(b, prop, value);
            case "orbit3d" -> setOrbit3dProperty(b, prop, value);
            default -> { super.set(path, value); return; }
        }
        
        this.transform = b.build();
    }
    
    private void setOrbitProperty(Transform.Builder b, String prop, Object value) {
        OrbitConfig current = transform.orbit() != null ? transform.orbit() : OrbitConfig.NONE;
        OrbitConfig.Builder ob = current.toBuilder();
        
        if (prop == null) {
            if (value instanceof OrbitConfig oc) b.orbit(oc);
            return;
        }
        
        switch (prop) {
            case "enabled" -> ob.enabled(toBool(value));
            case "radius" -> ob.radius(toFloat(value));
            case "speed" -> ob.speed(toFloat(value));
            case "axis" -> ob.axis(Axis.fromId(value.toString()));
            case "phase" -> ob.phase(toFloat(value));
        }
        b.orbit(ob.build());
    }
    
    private void setOrbit3dProperty(Transform.Builder b, String prop, Object value) {
        OrbitConfig3D current = transform.orbit3d() != null ? transform.orbit3d() : OrbitConfig3D.disabled();
        
        if (prop == null) {
            if (value instanceof OrbitConfig3D oc) b.orbit3d(oc);
            return;
        }
        
        String[] subParts = prop.split("\\.", 2);
        String axisOrProp = subParts[0];
        String axisProp = subParts.length > 1 ? subParts[1] : null;
        
        switch (axisOrProp) {
            case "enabled" -> b.orbit3d(current.withEnabled(toBool(value)));
            case "x" -> b.orbit3d(current.withEnabled(true).withX(updateAxisConfig(current.x(), axisProp, value)));
            case "y" -> b.orbit3d(current.withEnabled(true).withY(updateAxisConfig(current.y(), axisProp, value)));
            case "z" -> b.orbit3d(current.withEnabled(true).withZ(updateAxisConfig(current.z(), axisProp, value)));
            case "coupling" -> b.orbit3d(current.withCoupling(
                OrbitConfig3D.CouplingMode.valueOf(value.toString())));
        }
    }
    
    private AxisMotionConfig updateAxisConfig(AxisMotionConfig current, String prop, Object value) {
        if (current == null) {
            current = AxisMotionConfig.none();
        }
        
        if (prop == null) {
            return current;
        }
        
        var builder = current.toBuilder();
        switch (prop) {
            case "mode" -> builder.mode(MotionMode.valueOf(value.toString()));
            case "amplitude" -> builder.amplitude(toFloat(value));
            case "frequency" -> builder.frequency(toFloat(value));
            case "phase" -> builder.phase(toFloat(value));
            case "amplitude2" -> builder.amplitude2(toFloat(value));
            case "frequency2" -> builder.frequency2(toFloat(value));
            case "swingAngle" -> builder.swingAngle(toFloat(value));
            case "phase2" -> builder.phase2(toFloat(value));
            case "orbit2TiltX" -> builder.orbit2TiltX(toFloat(value));
            case "orbit2TiltY" -> builder.orbit2TiltY(toFloat(value));
            case "orbit2TiltZ" -> builder.orbit2TiltZ(toFloat(value));
        }
        return builder.build();
    }
    
    private float toFloat(Object v) { return v instanceof Number n ? n.floatValue() : 0f; }
    private boolean toBool(Object v) { return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); }
    
    @Override
    public void reset() {
        this.transform = Transform.IDENTITY;
    }
}
