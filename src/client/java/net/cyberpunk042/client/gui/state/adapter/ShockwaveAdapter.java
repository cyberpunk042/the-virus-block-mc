package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.visual.shader.ShockwavePostEffect;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;
import net.cyberpunk042.log.Logging;

/**
 * Adapter for shockwave visual effect configuration.
 * 
 * <p>Shockwave is a <b>field-level</b> effect that sits above the layer/primitive
 * hierarchy. It's not stored per-primitive but alongside the field definition.</p>
 * 
 * <p>Handles paths like {@code shockwave.shapeType}, {@code shockwave.orbitDistance},
 * {@code shockwave.ringSpeed}, etc.</p>
 * 
 * <p>This adapter stores the configuration for JSON persistence AND syncs changes 
 * to the live ShockwavePostEffect for immediate visual feedback.</p>
 * 
 * <p>Future: Can reference a primitive to use its geometry as the shockwave shape source.</p>
 */
@StateCategory("shockwave")
public class ShockwaveAdapter extends AbstractAdapter {
    
    private ShockwaveConfig config = ShockwaveConfig.DEFAULT;
    
    // Optional: reference to a primitive to use as shape source
    // Format: "layerIndex.primitiveIndex" or null
    private String shapeSourceRef = null;
    
    // Polygon side count (for POLYGON shape type, default 6 = hexagon)
    private int polygonSides = 6;
    
    // Currently selected preset name (for UI persistence after rebuild)
    private String currentPresetName = "Default";
    
    public String category() { return "shockwave"; }
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        return switch (prop) {
            // Shape source reference (for linking to a primitive's geometry)
            case "shapeSourceRef" -> shapeSourceRef;
            case "currentPresetName" -> currentPresetName;
            
            // Shape
            case "shapeType" -> config.shapeType();
            case "mainRadius" -> config.mainRadius();
            case "orbitalRadius" -> config.orbitalRadius();
            case "orbitDistance" -> config.orbitDistance();
            case "orbitalCount" -> config.orbitalCount();
            case "polygonSides" -> polygonSides;
            
            // Ring geometry
            case "ringCount" -> config.ringCount();
            case "ringSpacing" -> config.ringSpacing();
            case "ringThickness" -> config.ringThickness();
            case "ringMaxRadius" -> config.ringMaxRadius();
            case "ringSpeed" -> config.ringSpeed();
            case "ringGlowWidth" -> config.ringGlowWidth();
            case "ringIntensity" -> config.ringIntensity();
            case "ringContractMode" -> config.ringContractMode();
            
            // Ring color
            case "ringColorR" -> config.ringColorR();
            case "ringColorG" -> config.ringColorG();
            case "ringColorB" -> config.ringColorB();
            case "ringColorOpacity" -> config.ringColorOpacity();
            
            // Orbital body
            case "orbitalBodyR" -> config.orbitalBodyR();
            case "orbitalBodyG" -> config.orbitalBodyG();
            case "orbitalBodyB" -> config.orbitalBodyB();
            
            // Orbital corona
            case "orbitalCoronaR" -> config.orbitalCoronaR();
            case "orbitalCoronaG" -> config.orbitalCoronaG();
            case "orbitalCoronaB" -> config.orbitalCoronaB();
            case "orbitalCoronaA" -> config.orbitalCoronaA();
            case "orbitalCoronaWidth" -> config.orbitalCoronaWidth();
            case "orbitalCoronaIntensity" -> config.orbitalCoronaIntensity();
            case "orbitalRimPower" -> config.orbitalRimPower();
            case "orbitalRimFalloff" -> config.orbitalRimFalloff();
            
            // Beam geometry
            case "beamHeight" -> config.beamHeight();
            case "beamWidth" -> config.beamWidth();
            case "beamWidthScale" -> config.beamWidthScale();
            case "beamTaper" -> config.beamTaper();
            
            // Beam body
            case "beamBodyR" -> config.beamBodyR();
            case "beamBodyG" -> config.beamBodyG();
            case "beamBodyB" -> config.beamBodyB();
            
            // Beam corona
            case "beamCoronaR" -> config.beamCoronaR();
            case "beamCoronaG" -> config.beamCoronaG();
            case "beamCoronaB" -> config.beamCoronaB();
            case "beamCoronaA" -> config.beamCoronaA();
            case "beamCoronaWidth" -> config.beamCoronaWidth();
            case "beamCoronaIntensity" -> config.beamCoronaIntensity();
            case "beamRimPower" -> config.beamRimPower();
            case "beamRimFalloff" -> config.beamRimFalloff();
            
            // Animation timing
            case "orbitalSpeed" -> config.orbitalSpeed();
            case "orbitalSpawnDuration" -> config.orbitalSpawnDuration();
            case "orbitalRetractDuration" -> config.orbitalRetractDuration();
            case "beamGrowDuration" -> config.beamGrowDuration();
            case "beamShrinkDuration" -> config.beamShrinkDuration();
            case "beamHoldDuration" -> config.beamHoldDuration();
            case "beamWidthGrowFactor" -> config.beamWidthGrowFactor();
            case "beamLengthGrowFactor" -> config.beamLengthGrowFactor();
            
            // Easing types
            case "orbitalSpawnEasing" -> config.orbitalSpawnEasing();
            case "orbitalRetractEasing" -> config.orbitalRetractEasing();
            case "beamGrowEasing" -> config.beamGrowEasing();
            case "beamShrinkEasing" -> config.beamShrinkEasing();
            
            // Delays
            case "orbitalSpawnDelay" -> config.orbitalSpawnDelay();
            case "beamStartDelay" -> config.beamStartDelay();
            case "retractDelay" -> config.retractDelay();
            case "autoRetractOnRingEnd" -> config.autoRetractOnRingEnd();
            
            // Screen effects
            case "blackout" -> config.blackout();
            case "vignetteAmount" -> config.vignetteAmount();
            case "vignetteRadius" -> config.vignetteRadius();
            case "tintR" -> config.tintR();
            case "tintG" -> config.tintG();
            case "tintB" -> config.tintB();
            case "tintAmount" -> config.tintAmount();
            
            // Blend
            case "blendRadius" -> config.blendRadius();
            case "combinedMode" -> config.combinedMode();
            
            // Global scale & positioning
            case "globalScale" -> config.globalScale();
            case "followPosition" -> config.followPosition();
            case "cursorYOffset" -> config.cursorYOffset();
            
            default -> {
                Logging.GUI.topic("adapter").warn("Unknown shockwave property: {}", prop);
                yield null;
            }
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        ShockwaveConfig.Builder b = config.toBuilder();
        
        switch (prop) {
            // Shape
            // Shape source reference (for linking to a primitive's geometry)
            case "shapeSourceRef" -> shapeSourceRef = value != null ? value.toString() : null;
            case "currentPresetName" -> { currentPresetName = value != null ? value.toString() : "Default"; return; }  // No sync needed for UI-only field
            
            // Shape
            case "shapeType" -> {
                ShapeType type = value instanceof ShapeType st ? st 
                    : ShapeType.valueOf(value.toString());
                b.shapeType(type);
            }
            case "mainRadius" -> b.mainRadius(toFloat(value));
            case "orbitalRadius" -> b.orbitalRadius(toFloat(value));
            case "orbitDistance" -> b.orbitDistance(toFloat(value));
            case "orbitalCount" -> b.orbitalCount(toInt(value));
            case "polygonSides" -> polygonSides = toInt(value);
            
            // Ring geometry
            case "ringCount" -> b.ringCount(toInt(value));
            case "ringSpacing" -> b.ringSpacing(toFloat(value));
            case "ringThickness" -> b.ringThickness(toFloat(value));
            case "ringMaxRadius" -> b.ringMaxRadius(toFloat(value));
            case "ringSpeed" -> b.ringSpeed(toFloat(value));
            case "ringGlowWidth" -> b.ringGlowWidth(toFloat(value));
            case "ringIntensity" -> b.ringIntensity(toFloat(value));
            case "ringContractMode" -> b.ringContractMode(toBool(value));
            
            // Ring color
            case "ringColorR" -> b.ringColorR(toFloat(value));
            case "ringColorG" -> b.ringColorG(toFloat(value));
            case "ringColorB" -> b.ringColorB(toFloat(value));
            case "ringColorOpacity" -> b.ringColorOpacity(toFloat(value));
            
            // Orbital body
            case "orbitalBodyR" -> b.orbitalBodyR(toFloat(value));
            case "orbitalBodyG" -> b.orbitalBodyG(toFloat(value));
            case "orbitalBodyB" -> b.orbitalBodyB(toFloat(value));
            
            // Orbital corona
            case "orbitalCoronaR" -> b.orbitalCoronaR(toFloat(value));
            case "orbitalCoronaG" -> b.orbitalCoronaG(toFloat(value));
            case "orbitalCoronaB" -> b.orbitalCoronaB(toFloat(value));
            case "orbitalCoronaA" -> b.orbitalCoronaA(toFloat(value));
            case "orbitalCoronaWidth" -> b.orbitalCoronaWidth(toFloat(value));
            case "orbitalCoronaIntensity" -> b.orbitalCoronaIntensity(toFloat(value));
            case "orbitalRimPower" -> b.orbitalRimPower(toFloat(value));
            case "orbitalRimFalloff" -> b.orbitalRimFalloff(toFloat(value));
            
            // Beam geometry
            case "beamHeight" -> b.beamHeight(toFloat(value));
            case "beamWidth" -> b.beamWidth(toFloat(value));
            case "beamWidthScale" -> b.beamWidthScale(toFloat(value));
            case "beamTaper" -> b.beamTaper(toFloat(value));
            
            // Beam body
            case "beamBodyR" -> b.beamBodyR(toFloat(value));
            case "beamBodyG" -> b.beamBodyG(toFloat(value));
            case "beamBodyB" -> b.beamBodyB(toFloat(value));
            
            // Beam corona
            case "beamCoronaR" -> b.beamCoronaR(toFloat(value));
            case "beamCoronaG" -> b.beamCoronaG(toFloat(value));
            case "beamCoronaB" -> b.beamCoronaB(toFloat(value));
            case "beamCoronaA" -> b.beamCoronaA(toFloat(value));
            case "beamCoronaWidth" -> b.beamCoronaWidth(toFloat(value));
            case "beamCoronaIntensity" -> b.beamCoronaIntensity(toFloat(value));
            case "beamRimPower" -> b.beamRimPower(toFloat(value));
            case "beamRimFalloff" -> b.beamRimFalloff(toFloat(value));
            
            // Animation timing
            case "orbitalSpeed" -> b.orbitalSpeed(toFloat(value));
            case "orbitalSpawnDuration" -> b.orbitalSpawnDuration(toFloat(value));
            case "orbitalRetractDuration" -> b.orbitalRetractDuration(toFloat(value));
            case "beamGrowDuration" -> b.beamGrowDuration(toFloat(value));
            case "beamShrinkDuration" -> b.beamShrinkDuration(toFloat(value));
            case "beamHoldDuration" -> b.beamHoldDuration(toFloat(value));
            case "beamWidthGrowFactor" -> b.beamWidthGrowFactor(toFloat(value));
            case "beamLengthGrowFactor" -> b.beamLengthGrowFactor(toFloat(value));
            
            // Easing types
            case "orbitalSpawnEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.orbitalSpawnEasing(type);
            }
            case "orbitalRetractEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.orbitalRetractEasing(type);
            }
            case "beamGrowEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.beamGrowEasing(type);
            }
            case "beamShrinkEasing" -> {
                EasingType type = value instanceof EasingType et ? et : EasingType.valueOf(value.toString());
                b.beamShrinkEasing(type);
            }
            
            // Delays
            case "orbitalSpawnDelay" -> b.orbitalSpawnDelay(toFloat(value));
            case "beamStartDelay" -> b.beamStartDelay(toFloat(value));
            case "retractDelay" -> b.retractDelay(toFloat(value));
            case "autoRetractOnRingEnd" -> b.autoRetractOnRingEnd(toBool(value));
            
            // Screen effects
            case "blackout" -> b.blackout(toFloat(value));
            case "vignetteAmount" -> b.vignetteAmount(toFloat(value));
            case "vignetteRadius" -> b.vignetteRadius(toFloat(value));
            case "tintR" -> b.tintR(toFloat(value));
            case "tintG" -> b.tintG(toFloat(value));
            case "tintB" -> b.tintB(toFloat(value));
            case "tintAmount" -> b.tintAmount(toFloat(value));
            
            // Blend
            case "blendRadius" -> b.blendRadius(toFloat(value));
            case "combinedMode" -> b.combinedMode(toBool(value));
            
            // Global scale & positioning
            case "globalScale" -> b.globalScale(toFloat(value));
            case "followPosition" -> b.followPosition(toBool(value));
            case "cursorYOffset" -> b.cursorYOffset(toFloat(value));
            
            default -> Logging.GUI.topic("adapter").warn("Unknown shockwave property: {}", prop);
        }
        
        config = b.build();
        
        // Sync to live effect for immediate visual feedback
        syncToPostEffect();
    }
    
    /**
     * Syncs the current config to ShockwavePostEffect for live preview.
     * Applies globalScale to all size-related parameters.
     */
    public void syncToPostEffect() {
        float scale = config.globalScale();
        
        // Shape (with scale applied to distances/radii)
        // Build ShapeConfig based on actual shapeType from config
        ShapeConfig shapeConfig;
        switch (config.shapeType()) {
            case POINT -> shapeConfig = new ShapeConfig(ShapeType.POINT, 0, 0, 0, 0, 0);
            case SPHERE -> shapeConfig = ShapeConfig.sphere(config.mainRadius() * scale);
            case TORUS -> shapeConfig = ShapeConfig.torus(config.mainRadius() * scale, config.orbitalRadius() * scale);
            case POLYGON -> shapeConfig = ShapeConfig.polygon(polygonSides, config.mainRadius() * scale);
            case ORBITAL -> shapeConfig = ShapeConfig.orbital(
                config.mainRadius() * scale, 
                config.orbitalRadius() * scale, 
                config.orbitDistance() * scale, 
                config.orbitalCount());
            default -> shapeConfig = ShapeConfig.sphere(config.mainRadius() * scale);
        }
        ShockwavePostEffect.setShape(shapeConfig);
        
        // Ring parameters (with scale applied to size parameters)
        ShockwavePostEffect.setRingCount(config.ringCount());
        ShockwavePostEffect.setRingSpacing(config.ringSpacing() * scale);
        ShockwavePostEffect.setThickness(config.ringThickness() * scale);
        ShockwavePostEffect.setMaxRadius(config.ringMaxRadius() * scale);
        ShockwavePostEffect.setSpeed(config.ringSpeed() * scale);
        ShockwavePostEffect.setGlowWidth(config.ringGlowWidth() * scale);
        ShockwavePostEffect.setIntensity(config.ringIntensity());  // Intensity not scaled
        ShockwavePostEffect.setContractMode(config.ringContractMode());
        ShockwavePostEffect.setRingColor(
            config.ringColorR(), config.ringColorG(), 
            config.ringColorB(), config.ringColorOpacity()
        );
        
        // Orbital visual (corona width scaled)
        ShockwavePostEffect.setOrbitalVisual(new OrbitalVisualConfig(
            new Color3f(config.orbitalBodyR(), config.orbitalBodyG(), config.orbitalBodyB()),
            new CoronaConfig(
                new Color4f(config.orbitalCoronaR(), config.orbitalCoronaG(), 
                            config.orbitalCoronaB(), config.orbitalCoronaA()),
                config.orbitalCoronaWidth() * scale, config.orbitalCoronaIntensity(),
                config.orbitalRimPower(), config.orbitalRimFalloff()
            )
        ));
        
        // Beam visual (width and height scaled)
        ShockwavePostEffect.setBeamVisual(new BeamVisualConfig(
            new Color3f(config.beamBodyR(), config.beamBodyG(), config.beamBodyB()),
            new CoronaConfig(
                new Color4f(config.beamCoronaR(), config.beamCoronaG(), 
                            config.beamCoronaB(), config.beamCoronaA()),
                config.beamCoronaWidth() * scale, config.beamCoronaIntensity(),
                config.beamRimPower(), config.beamRimFalloff()
            ),
            config.beamWidth() * scale, config.beamWidthScale(), config.beamTaper()
        ));
        
        // Animation timing (beam height scaled)
        ShockwavePostEffect.setAnimationTiming(new AnimationTimingConfig(
            config.orbitalSpeed(),
            config.orbitalSpawnDuration(),
            config.orbitalRetractDuration(),
            config.orbitalSpawnEasing(),
            config.orbitalRetractEasing(),
            config.beamHeight() * scale,  // Scale beam height
            config.beamGrowDuration(),
            config.beamShrinkDuration(),
            config.beamHoldDuration(),
            config.beamWidthGrowFactor(),
            config.beamLengthGrowFactor(),
            config.beamGrowEasing(),
            config.beamShrinkEasing(),
            config.orbitalSpawnDelay(),
            config.beamStartDelay(),
            config.retractDelay(),
            config.autoRetractOnRingEnd()
        ));
        
        // Screen effects
        ShockwavePostEffect.setBlackout(config.blackout());
        ShockwavePostEffect.setVignette(config.vignetteAmount(), config.vignetteRadius());
        ShockwavePostEffect.setTint(config.tintR(), config.tintG(), config.tintB(), config.tintAmount());
        
        // Blend
        ShockwavePostEffect.setBlendRadius(config.blendRadius());
        ShockwavePostEffect.setCombinedMode(config.combinedMode());
        
        // Follow position
        ShockwavePostEffect.setFollowCamera(config.followPosition());
    }
    
    // Helper conversion methods
    private float toFloat(Object v) { 
        return v instanceof Number n ? n.floatValue() : 0f; 
    }
    
    private int toInt(Object v) { 
        return v instanceof Number n ? n.intValue() : 0; 
    }
    
    private boolean toBool(Object v) { 
        return v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString()); 
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ShockwaveConfig config() { return config; }
    
    public void setConfig(ShockwaveConfig config) { 
        this.config = config;
        syncToPostEffect();
    }
    
    public String shapeSourceRef() { return shapeSourceRef; }
    
    public void setShapeSourceRef(String ref) { this.shapeSourceRef = ref; }
    
    /**
     * Syncs geometry from a source primitive.
     * Called when shapeSourceRef is set and the primitive's geometry should be used.
     * 
     * @param primitive the primitive to use as shape source
     */
    public void syncFromPrimitive(net.cyberpunk042.field.primitive.Primitive primitive) {
        if (primitive == null) return;
        
        var shape = primitive.shape();
        if (shape == null) return;
        
        ShockwaveConfig.Builder b = config.toBuilder();
        
        // Determine shape type and extract radius
        if (shape instanceof net.cyberpunk042.visual.shape.SphereShape s) {
            b.shapeType(ShapeType.ORBITAL);
            b.mainRadius(s.radius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.RingShape r) {
            b.shapeType(ShapeType.TORUS);  // Ring shapes map to TORUS shockwave type
            b.mainRadius(r.outerRadius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.CylinderShape c) {
            b.shapeType(ShapeType.ORBITAL);
            b.mainRadius(c.radius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.PolyhedronShape p) {
            b.shapeType(ShapeType.ORBITAL);
            b.mainRadius(p.radius());
        } else if (shape instanceof net.cyberpunk042.visual.shape.TorusShape t) {
            b.shapeType(ShapeType.TORUS);  // Torus shapes use TORUS shockwave type
            b.mainRadius(t.majorRadius() + t.minorRadius());
        }
        // Add more shape types as needed
        
        config = b.build();
        syncToPostEffect();
        Logging.GUI.topic("adapter").debug("Synced shockwave from primitive: {}", primitive.id());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // POSITION FOLLOWING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns true if shockwave should follow source position each frame.
     */
    public boolean isFollowPositionEnabled() {
        return config.followPosition();
    }
    
    /**
     * Returns the current global scale multiplier.
     */
    public float getGlobalScale() {
        return config.globalScale();
    }
    
    /**
     * Get the current config (for read-only access).
     */
    public ShockwaveConfig getConfig() {
        return config;
    }
    
    @Override
    public void reset() {
        this.config = ShockwaveConfig.DEFAULT;
        this.shapeSourceRef = null;
        syncToPostEffect();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON PERSISTENCE (field-level storage)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Serialize shockwave config to JSON for field definition storage.
     * Uses nested objects for clean, readable structure.
     */
    public com.google.gson.JsonObject toJson() {
        var json = new com.google.gson.JsonObject();
        
        // Shape source reference (optional)
        if (shapeSourceRef != null) {
            json.addProperty("shapeSourceRef", shapeSourceRef);
        }
        
        // ═══ SHAPE ═══
        var shape = new com.google.gson.JsonObject();
        shape.addProperty("type", config.shapeType().name());
        shape.addProperty("mainRadius", config.mainRadius());
        shape.addProperty("orbitalRadius", config.orbitalRadius());
        shape.addProperty("orbitDistance", config.orbitDistance());
        shape.addProperty("orbitalCount", config.orbitalCount());
        json.add("shape", shape);
        
        // ═══ RING ═══
        var ring = new com.google.gson.JsonObject();
        ring.addProperty("count", config.ringCount());
        ring.addProperty("spacing", config.ringSpacing());
        ring.addProperty("thickness", config.ringThickness());
        ring.addProperty("maxRadius", config.ringMaxRadius());
        ring.addProperty("speed", config.ringSpeed());
        ring.addProperty("glowWidth", config.ringGlowWidth());
        ring.addProperty("intensity", config.ringIntensity());
        ring.addProperty("contractMode", config.ringContractMode());
        // Ring color as array
        var ringColor = new com.google.gson.JsonArray();
        ringColor.add(config.ringColorR());
        ringColor.add(config.ringColorG());
        ringColor.add(config.ringColorB());
        ringColor.add(config.ringColorOpacity());
        ring.add("color", ringColor);
        json.add("ring", ring);
        
        // ═══ ORBITAL ═══
        var orbital = new com.google.gson.JsonObject();
        // Body color as array
        var orbBody = new com.google.gson.JsonArray();
        orbBody.add(config.orbitalBodyR());
        orbBody.add(config.orbitalBodyG());
        orbBody.add(config.orbitalBodyB());
        orbital.add("bodyColor", orbBody);
        // Corona
        var orbCorona = new com.google.gson.JsonObject();
        var orbCoronaColor = new com.google.gson.JsonArray();
        orbCoronaColor.add(config.orbitalCoronaR());
        orbCoronaColor.add(config.orbitalCoronaG());
        orbCoronaColor.add(config.orbitalCoronaB());
        orbCoronaColor.add(config.orbitalCoronaA());
        orbCorona.add("color", orbCoronaColor);
        orbCorona.addProperty("width", config.orbitalCoronaWidth());
        orbCorona.addProperty("intensity", config.orbitalCoronaIntensity());
        orbCorona.addProperty("rimPower", config.orbitalRimPower());
        orbCorona.addProperty("rimFalloff", config.orbitalRimFalloff());
        orbital.add("corona", orbCorona);
        json.add("orbital", orbital);
        
        // ═══ BEAM ═══
        var beam = new com.google.gson.JsonObject();
        beam.addProperty("height", config.beamHeight());
        beam.addProperty("width", config.beamWidth());
        beam.addProperty("widthScale", config.beamWidthScale());
        beam.addProperty("taper", config.beamTaper());
        // Body color as array
        var beamBody = new com.google.gson.JsonArray();
        beamBody.add(config.beamBodyR());
        beamBody.add(config.beamBodyG());
        beamBody.add(config.beamBodyB());
        beam.add("bodyColor", beamBody);
        // Corona
        var beamCorona = new com.google.gson.JsonObject();
        var beamCoronaColor = new com.google.gson.JsonArray();
        beamCoronaColor.add(config.beamCoronaR());
        beamCoronaColor.add(config.beamCoronaG());
        beamCoronaColor.add(config.beamCoronaB());
        beamCoronaColor.add(config.beamCoronaA());
        beamCorona.add("color", beamCoronaColor);
        beamCorona.addProperty("width", config.beamCoronaWidth());
        beamCorona.addProperty("intensity", config.beamCoronaIntensity());
        beamCorona.addProperty("rimPower", config.beamRimPower());
        beamCorona.addProperty("rimFalloff", config.beamRimFalloff());
        beam.add("corona", beamCorona);
        json.add("beam", beam);
        
        // ═══ ANIMATION ═══
        var animation = new com.google.gson.JsonObject();
        animation.addProperty("orbitalSpeed", config.orbitalSpeed());
        // Orbital timing
        var orbTiming = new com.google.gson.JsonObject();
        orbTiming.addProperty("spawnDuration", config.orbitalSpawnDuration());
        orbTiming.addProperty("retractDuration", config.orbitalRetractDuration());
        orbTiming.addProperty("spawnEasing", config.orbitalSpawnEasing().name());
        orbTiming.addProperty("retractEasing", config.orbitalRetractEasing().name());
        orbTiming.addProperty("spawnDelay", config.orbitalSpawnDelay());
        animation.add("orbital", orbTiming);
        // Beam timing
        var beamTiming = new com.google.gson.JsonObject();
        beamTiming.addProperty("growDuration", config.beamGrowDuration());
        beamTiming.addProperty("shrinkDuration", config.beamShrinkDuration());
        beamTiming.addProperty("holdDuration", config.beamHoldDuration());
        beamTiming.addProperty("widthGrowFactor", config.beamWidthGrowFactor());
        beamTiming.addProperty("lengthGrowFactor", config.beamLengthGrowFactor());
        beamTiming.addProperty("growEasing", config.beamGrowEasing().name());
        beamTiming.addProperty("shrinkEasing", config.beamShrinkEasing().name());
        beamTiming.addProperty("startDelay", config.beamStartDelay());
        animation.add("beam", beamTiming);
        // Retract
        animation.addProperty("retractDelay", config.retractDelay());
        animation.addProperty("autoRetractOnRingEnd", config.autoRetractOnRingEnd());
        json.add("animation", animation);
        
        // ═══ SCREEN EFFECTS ═══
        var screen = new com.google.gson.JsonObject();
        screen.addProperty("blackout", config.blackout());
        screen.addProperty("vignetteAmount", config.vignetteAmount());
        screen.addProperty("vignetteRadius", config.vignetteRadius());
        // Tint color as array
        var tint = new com.google.gson.JsonArray();
        tint.add(config.tintR());
        tint.add(config.tintG());
        tint.add(config.tintB());
        tint.add(config.tintAmount());
        screen.add("tint", tint);
        screen.addProperty("blendRadius", config.blendRadius());
        json.add("screen", screen);
        
        // ═══ GLOBAL ═══
        json.addProperty("globalScale", config.globalScale());
        json.addProperty("followPosition", config.followPosition());
        json.addProperty("cursorYOffset", config.cursorYOffset());
        
        return json;
    }
    
    /**
     * Load shockwave config from JSON (field definition storage).
     */
    public void loadFromJson(com.google.gson.JsonObject json) {
        if (json == null) {
            reset();
            return;
        }
        
        // Shape source reference
        shapeSourceRef = json.has("shapeSourceRef") ? json.get("shapeSourceRef").getAsString() : null;
        
        ShockwaveConfig.Builder b = ShockwaveConfig.DEFAULT.toBuilder();
        
        // ═══ SHAPE ═══
        if (json.has("shape")) {
            var shape = json.getAsJsonObject("shape");
            if (shape.has("type")) {
                try { b.shapeType(ShapeType.valueOf(shape.get("type").getAsString())); } 
                catch (IllegalArgumentException e) { /* keep default */ }
            }
            if (shape.has("mainRadius")) b.mainRadius(shape.get("mainRadius").getAsFloat());
            if (shape.has("orbitalRadius")) b.orbitalRadius(shape.get("orbitalRadius").getAsFloat());
            if (shape.has("orbitDistance")) b.orbitDistance(shape.get("orbitDistance").getAsFloat());
            if (shape.has("orbitalCount")) b.orbitalCount(shape.get("orbitalCount").getAsInt());
        }
        
        // ═══ RING ═══
        if (json.has("ring")) {
            var ring = json.getAsJsonObject("ring");
            if (ring.has("count")) b.ringCount(ring.get("count").getAsInt());
            if (ring.has("spacing")) b.ringSpacing(ring.get("spacing").getAsFloat());
            if (ring.has("thickness")) b.ringThickness(ring.get("thickness").getAsFloat());
            if (ring.has("maxRadius")) b.ringMaxRadius(ring.get("maxRadius").getAsFloat());
            if (ring.has("speed")) b.ringSpeed(ring.get("speed").getAsFloat());
            if (ring.has("glowWidth")) b.ringGlowWidth(ring.get("glowWidth").getAsFloat());
            if (ring.has("intensity")) b.ringIntensity(ring.get("intensity").getAsFloat());
            if (ring.has("contractMode")) b.ringContractMode(ring.get("contractMode").getAsBoolean());
            if (ring.has("color")) {
                var c = ring.getAsJsonArray("color");
                if (c.size() >= 4) {
                    b.ringColorR(c.get(0).getAsFloat());
                    b.ringColorG(c.get(1).getAsFloat());
                    b.ringColorB(c.get(2).getAsFloat());
                    b.ringColorOpacity(c.get(3).getAsFloat());
                }
            }
        }
        
        // ═══ ORBITAL ═══
        if (json.has("orbital")) {
            var orbital = json.getAsJsonObject("orbital");
            if (orbital.has("bodyColor")) {
                var c = orbital.getAsJsonArray("bodyColor");
                if (c.size() >= 3) {
                    b.orbitalBodyR(c.get(0).getAsFloat());
                    b.orbitalBodyG(c.get(1).getAsFloat());
                    b.orbitalBodyB(c.get(2).getAsFloat());
                }
            }
            if (orbital.has("corona")) {
                var corona = orbital.getAsJsonObject("corona");
                if (corona.has("color")) {
                    var c = corona.getAsJsonArray("color");
                    if (c.size() >= 4) {
                        b.orbitalCoronaR(c.get(0).getAsFloat());
                        b.orbitalCoronaG(c.get(1).getAsFloat());
                        b.orbitalCoronaB(c.get(2).getAsFloat());
                        b.orbitalCoronaA(c.get(3).getAsFloat());
                    }
                }
                if (corona.has("width")) b.orbitalCoronaWidth(corona.get("width").getAsFloat());
                if (corona.has("intensity")) b.orbitalCoronaIntensity(corona.get("intensity").getAsFloat());
                if (corona.has("rimPower")) b.orbitalRimPower(corona.get("rimPower").getAsFloat());
                if (corona.has("rimFalloff")) b.orbitalRimFalloff(corona.get("rimFalloff").getAsFloat());
            }
        }
        
        // ═══ BEAM ═══
        if (json.has("beam")) {
            var beam = json.getAsJsonObject("beam");
            if (beam.has("height")) b.beamHeight(beam.get("height").getAsFloat());
            if (beam.has("width")) b.beamWidth(beam.get("width").getAsFloat());
            if (beam.has("widthScale")) b.beamWidthScale(beam.get("widthScale").getAsFloat());
            if (beam.has("taper")) b.beamTaper(beam.get("taper").getAsFloat());
            if (beam.has("bodyColor")) {
                var c = beam.getAsJsonArray("bodyColor");
                if (c.size() >= 3) {
                    b.beamBodyR(c.get(0).getAsFloat());
                    b.beamBodyG(c.get(1).getAsFloat());
                    b.beamBodyB(c.get(2).getAsFloat());
                }
            }
            if (beam.has("corona")) {
                var corona = beam.getAsJsonObject("corona");
                if (corona.has("color")) {
                    var c = corona.getAsJsonArray("color");
                    if (c.size() >= 4) {
                        b.beamCoronaR(c.get(0).getAsFloat());
                        b.beamCoronaG(c.get(1).getAsFloat());
                        b.beamCoronaB(c.get(2).getAsFloat());
                        b.beamCoronaA(c.get(3).getAsFloat());
                    }
                }
                if (corona.has("width")) b.beamCoronaWidth(corona.get("width").getAsFloat());
                if (corona.has("intensity")) b.beamCoronaIntensity(corona.get("intensity").getAsFloat());
                if (corona.has("rimPower")) b.beamRimPower(corona.get("rimPower").getAsFloat());
                if (corona.has("rimFalloff")) b.beamRimFalloff(corona.get("rimFalloff").getAsFloat());
            }
        }
        
        // ═══ ANIMATION ═══
        if (json.has("animation")) {
            var anim = json.getAsJsonObject("animation");
            if (anim.has("orbitalSpeed")) b.orbitalSpeed(anim.get("orbitalSpeed").getAsFloat());
            if (anim.has("retractDelay")) b.retractDelay(anim.get("retractDelay").getAsFloat());
            if (anim.has("autoRetractOnRingEnd")) b.autoRetractOnRingEnd(anim.get("autoRetractOnRingEnd").getAsBoolean());
            // Orbital timing
            if (anim.has("orbital")) {
                var orb = anim.getAsJsonObject("orbital");
                if (orb.has("spawnDuration")) b.orbitalSpawnDuration(orb.get("spawnDuration").getAsFloat());
                if (orb.has("retractDuration")) b.orbitalRetractDuration(orb.get("retractDuration").getAsFloat());
                if (orb.has("spawnEasing")) {
                    try { b.orbitalSpawnEasing(EasingType.valueOf(orb.get("spawnEasing").getAsString())); }
                    catch (IllegalArgumentException e) { /* keep default */ }
                }
                if (orb.has("retractEasing")) {
                    try { b.orbitalRetractEasing(EasingType.valueOf(orb.get("retractEasing").getAsString())); }
                    catch (IllegalArgumentException e) { /* keep default */ }
                }
                if (orb.has("spawnDelay")) b.orbitalSpawnDelay(orb.get("spawnDelay").getAsFloat());
            }
            // Beam timing
            if (anim.has("beam")) {
                var beamT = anim.getAsJsonObject("beam");
                if (beamT.has("growDuration")) b.beamGrowDuration(beamT.get("growDuration").getAsFloat());
                if (beamT.has("shrinkDuration")) b.beamShrinkDuration(beamT.get("shrinkDuration").getAsFloat());
                if (beamT.has("holdDuration")) b.beamHoldDuration(beamT.get("holdDuration").getAsFloat());
                if (beamT.has("widthGrowFactor")) b.beamWidthGrowFactor(beamT.get("widthGrowFactor").getAsFloat());
                if (beamT.has("lengthGrowFactor")) b.beamLengthGrowFactor(beamT.get("lengthGrowFactor").getAsFloat());
                if (beamT.has("growEasing")) {
                    try { b.beamGrowEasing(EasingType.valueOf(beamT.get("growEasing").getAsString())); }
                    catch (IllegalArgumentException e) { /* keep default */ }
                }
                if (beamT.has("shrinkEasing")) {
                    try { b.beamShrinkEasing(EasingType.valueOf(beamT.get("shrinkEasing").getAsString())); }
                    catch (IllegalArgumentException e) { /* keep default */ }
                }
                if (beamT.has("startDelay")) b.beamStartDelay(beamT.get("startDelay").getAsFloat());
            }
        }
        
        // ═══ SCREEN EFFECTS ═══
        if (json.has("screen")) {
            var screen = json.getAsJsonObject("screen");
            if (screen.has("blackout")) b.blackout(screen.get("blackout").getAsFloat());
            if (screen.has("vignetteAmount")) b.vignetteAmount(screen.get("vignetteAmount").getAsFloat());
            if (screen.has("vignetteRadius")) b.vignetteRadius(screen.get("vignetteRadius").getAsFloat());
            if (screen.has("blendRadius")) b.blendRadius(screen.get("blendRadius").getAsFloat());
            if (screen.has("tint")) {
                var t = screen.getAsJsonArray("tint");
                if (t.size() >= 4) {
                    b.tintR(t.get(0).getAsFloat());
                    b.tintG(t.get(1).getAsFloat());
                    b.tintB(t.get(2).getAsFloat());
                    b.tintAmount(t.get(3).getAsFloat());
                }
            }
        }
        
        // ═══ GLOBAL ═══
        if (json.has("globalScale")) b.globalScale(json.get("globalScale").getAsFloat());
        if (json.has("followPosition")) b.followPosition(json.get("followPosition").getAsBoolean());
        if (json.has("cursorYOffset")) b.cursorYOffset(json.get("cursorYOffset").getAsFloat());
        
        config = b.build();
        syncToPostEffect();
        
        Logging.GUI.topic("adapter").debug("ShockwaveAdapter loaded from JSON");
    }
}
