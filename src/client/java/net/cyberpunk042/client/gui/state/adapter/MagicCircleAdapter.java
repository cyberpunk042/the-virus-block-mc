package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.visual.shader.MagicCirclePostEffect;
import net.cyberpunk042.log.Logging;

/**
 * Adapter for magic circle ground effect configuration.
 * 
 * <p>Magic Circle is a <b>field-level</b> effect like Shockwave that renders
 * animated patterns projected onto terrain.</p>
 * 
 * <p>Handles paths like {@code magicCircle.effectRadius}, {@code magicCircle.intensity},
 * {@code magicCircle.primaryR}, etc.</p>
 * 
 * <p>This adapter stores the configuration for JSON persistence AND syncs changes 
 * to the live MagicCirclePostEffect for immediate visual feedback.</p>
 */
@StateCategory("magicCircle")
public class MagicCircleAdapter extends AbstractAdapter {
    
    private MagicCircleConfig config = MagicCircleConfig.DEFAULT;
    
    public String category() { return "magicCircle"; }
    
    @Override
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        // Handle indexed layer properties: layer1Enable, layer2Intensity, etc.
        if (prop.matches("layer[1-8](Enable|Intensity|Speed)")) {
            int idx = Character.getNumericValue(prop.charAt(5)) - 1;
            String suffix = prop.substring(6);
            return switch (suffix) {
                case "Enable" -> config.getLayerEnable(idx);
                case "Intensity" -> config.getLayerIntensity(idx);
                case "Speed" -> config.getLayerSpeed(idx);
                default -> null;
            };
        }
        
        return switch (prop) {
            // Global settings
            case "effectRadius" -> config.effectRadius();
            case "heightTolerance" -> config.heightTolerance();
            case "intensity" -> config.intensity();
            case "glowExponent" -> config.glowExponent();
            case "enabled" -> config.enabled();
            
            // Colors
            case "primaryR" -> config.primaryR();
            case "primaryG" -> config.primaryG();
            case "primaryB" -> config.primaryB();
            
            // Animation
            case "rotationSpeed" -> config.rotationSpeed();
            case "breathingSpeed" -> config.breathingSpeed();
            case "breathingAmount" -> config.breathingAmount();
            
            // Position
            case "centerX" -> config.centerX();
            case "centerY" -> config.centerY();
            case "centerZ" -> config.centerZ();
            case "followPlayer" -> config.followPlayer();
            
            // Layer 4 Geometry (Phase 3A)
            case "layer4InnerRadius" -> config.layer4InnerRadius();
            case "layer4OuterRadius" -> config.layer4OuterRadius();
            case "layer4Thickness" -> config.layer4Thickness();
            case "layer4RotOffset" -> config.layer4RotOffset();
            
            // Layer 7 Geometry (Phase 3A)
            case "layer7InnerRadius" -> config.layer7InnerRadius();
            case "layer7OuterRadius" -> config.layer7OuterRadius();
            case "layer7SpokeCount" -> config.layer7SpokeCount();
            case "layer7Thickness" -> config.layer7Thickness();
            case "layer7RotOffset" -> config.layer7RotOffset();
            
            // Layer 2 Geometry (Phase 3B)
            case "layer2RectCount" -> config.layer2RectCount();
            case "layer2RectSize" -> config.layer2RectSize();
            case "layer2Thickness" -> config.layer2Thickness();
            case "layer2RotOffset" -> config.layer2RotOffset();
            case "layer2SnapRotation" -> config.layer2SnapRotation();
            
            // Layer 5 Geometry (Phase 3B)
            case "layer5RectCount" -> config.layer5RectCount();
            case "layer5RectSize" -> config.layer5RectSize();
            case "layer5Thickness" -> config.layer5Thickness();
            case "layer5RotOffset" -> config.layer5RotOffset();
            case "layer5SnapRotation" -> config.layer5SnapRotation();
            
            // Layer 3 Geometry (Phase 3C)
            case "layer3DotCount" -> config.layer3DotCount();
            case "layer3OrbitRadius" -> config.layer3OrbitRadius();
            case "layer3RingInner" -> config.layer3RingInner();
            case "layer3RingOuter" -> config.layer3RingOuter();
            case "layer3RingThickness" -> config.layer3RingThickness();
            case "layer3DotRadius" -> config.layer3DotRadius();
            case "layer3DotThickness" -> config.layer3DotThickness();
            case "layer3RotOffset" -> config.layer3RotOffset();
            
            // Layer 6 Geometry (Phase 3C)
            case "layer6DotCount" -> config.layer6DotCount();
            case "layer6OrbitRadius" -> config.layer6OrbitRadius();
            case "layer6RingInner" -> config.layer6RingInner();
            case "layer6RingOuter" -> config.layer6RingOuter();
            case "layer6RingThickness" -> config.layer6RingThickness();
            case "layer6DotRadius" -> config.layer6DotRadius();
            case "layer6DotThickness" -> config.layer6DotThickness();
            case "layer6RotOffset" -> config.layer6RotOffset();
            
            // Layer 1 Geometry (Phase 3D)
            case "layer1RingInner" -> config.layer1RingInner();
            case "layer1RingOuter" -> config.layer1RingOuter();
            case "layer1RingThickness" -> config.layer1RingThickness();
            case "layer1RadInner" -> config.layer1RadInner();
            case "layer1RadOuter" -> config.layer1RadOuter();
            case "layer1RadCount" -> config.layer1RadCount();
            case "layer1RadThickness" -> config.layer1RadThickness();
            case "layer1RotOffset" -> config.layer1RotOffset();
            
            // Layer 8 Geometry (Phase 3D)
            case "layer8BreathAmp" -> config.layer8BreathAmp();
            case "layer8BreathCenter" -> config.layer8BreathCenter();
            case "layer8OrbitalCount" -> config.layer8OrbitalCount();
            case "layer8OrbitalStart" -> config.layer8OrbitalStart();
            case "layer8OrbitalStep" -> config.layer8OrbitalStep();
            case "layer8OrbitalDist" -> config.layer8OrbitalDist();
            case "layer8OrbitalThickness" -> config.layer8OrbitalThickness();
            case "layer8CenterRadius" -> config.layer8CenterRadius();
            case "layer8CenterThickness" -> config.layer8CenterThickness();
            case "layer8RotOffset" -> config.layer8RotOffset();
            
            // Stage Animation (Phase 4)
            case "animationStage" -> config.animationStage();
            case "stageSpeed" -> config.stageSpeed();
            case "transitionMode" -> config.transitionMode();
            case "animateOnSpawn" -> config.animateOnSpawn();
            case "animationFromCenter" -> config.animationFromCenter();
            
            default -> {
                Logging.GUI.topic("adapter").warn("Unknown magicCircle property: {}", prop);
                yield null;
            }
        };
    }
    
    @Override
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String prop = parts.length > 1 ? parts[1] : parts[0];
        
        MagicCircleConfig.Builder b = config.toBuilder();
        
        // Handle indexed layer properties: layer1Enable, layer2Intensity, etc.
        if (prop.matches("layer[1-8](Enable|Intensity|Speed)")) {
            int idx = Character.getNumericValue(prop.charAt(5)) - 1;
            String suffix = prop.substring(6);
            switch (suffix) {
                case "Enable" -> b.setLayerEnable(idx, toBool(value));
                case "Intensity" -> b.setLayerIntensity(idx, toFloat(value));
                case "Speed" -> b.setLayerSpeed(idx, toFloat(value));
            }
        } else {
            switch (prop) {
                // Global settings
                case "effectRadius" -> b.effectRadius(toFloat(value));
                case "heightTolerance" -> b.heightTolerance(toFloat(value));
                case "intensity" -> b.intensity(toFloat(value));
                case "glowExponent" -> b.glowExponent(toFloat(value));
                case "enabled" -> b.enabled(toBool(value));
                
                // Colors
                case "primaryR" -> b.primaryR(toFloat(value));
                case "primaryG" -> b.primaryG(toFloat(value));
                case "primaryB" -> b.primaryB(toFloat(value));
                
                // Animation
                case "rotationSpeed" -> b.rotationSpeed(toFloat(value));
                case "breathingSpeed" -> b.breathingSpeed(toFloat(value));
                case "breathingAmount" -> b.breathingAmount(toFloat(value));
                
                // Position
                case "centerX" -> b.centerX(toFloat(value));
                case "centerY" -> b.centerY(toFloat(value));
                case "centerZ" -> b.centerZ(toFloat(value));
                case "followPlayer" -> b.followPlayer(toBool(value));
                
                // Layer 4 Geometry (Phase 3A)
                case "layer4InnerRadius" -> b.layer4InnerRadius(toFloat(value));
                case "layer4OuterRadius" -> b.layer4OuterRadius(toFloat(value));
                case "layer4Thickness" -> b.layer4Thickness(toFloat(value));
                case "layer4RotOffset" -> b.layer4RotOffset(toFloat(value));
                
                // Layer 7 Geometry (Phase 3A)
                case "layer7InnerRadius" -> b.layer7InnerRadius(toFloat(value));
                case "layer7OuterRadius" -> b.layer7OuterRadius(toFloat(value));
                case "layer7SpokeCount" -> b.layer7SpokeCount(toInt(value));
                case "layer7Thickness" -> b.layer7Thickness(toFloat(value));
                case "layer7RotOffset" -> b.layer7RotOffset(toFloat(value));
                
                // Layer 2 Geometry (Phase 3B)
                case "layer2RectCount" -> b.layer2RectCount(toInt(value));
                case "layer2RectSize" -> b.layer2RectSize(toFloat(value));
                case "layer2Thickness" -> b.layer2Thickness(toFloat(value));
                case "layer2RotOffset" -> b.layer2RotOffset(toFloat(value));
                case "layer2SnapRotation" -> b.layer2SnapRotation(toBool(value));
                
                // Layer 5 Geometry (Phase 3B)
                case "layer5RectCount" -> b.layer5RectCount(toInt(value));
                case "layer5RectSize" -> b.layer5RectSize(toFloat(value));
                case "layer5Thickness" -> b.layer5Thickness(toFloat(value));
                case "layer5RotOffset" -> b.layer5RotOffset(toFloat(value));
                case "layer5SnapRotation" -> b.layer5SnapRotation(toBool(value));
                
                // Layer 3 Geometry (Phase 3C)
                case "layer3DotCount" -> b.layer3DotCount(toInt(value));
                case "layer3OrbitRadius" -> b.layer3OrbitRadius(toFloat(value));
                case "layer3RingInner" -> b.layer3RingInner(toFloat(value));
                case "layer3RingOuter" -> b.layer3RingOuter(toFloat(value));
                case "layer3RingThickness" -> b.layer3RingThickness(toFloat(value));
                case "layer3DotRadius" -> b.layer3DotRadius(toFloat(value));
                case "layer3DotThickness" -> b.layer3DotThickness(toFloat(value));
                case "layer3RotOffset" -> b.layer3RotOffset(toFloat(value));
                
                // Layer 6 Geometry (Phase 3C)
                case "layer6DotCount" -> b.layer6DotCount(toInt(value));
                case "layer6OrbitRadius" -> b.layer6OrbitRadius(toFloat(value));
                case "layer6RingInner" -> b.layer6RingInner(toFloat(value));
                case "layer6RingOuter" -> b.layer6RingOuter(toFloat(value));
                case "layer6RingThickness" -> b.layer6RingThickness(toFloat(value));
                case "layer6DotRadius" -> b.layer6DotRadius(toFloat(value));
                case "layer6DotThickness" -> b.layer6DotThickness(toFloat(value));
                case "layer6RotOffset" -> b.layer6RotOffset(toFloat(value));
                
                // Layer 1 Geometry (Phase 3D)
                case "layer1RingInner" -> b.layer1RingInner(toFloat(value));
                case "layer1RingOuter" -> b.layer1RingOuter(toFloat(value));
                case "layer1RingThickness" -> b.layer1RingThickness(toFloat(value));
                case "layer1RadInner" -> b.layer1RadInner(toFloat(value));
                case "layer1RadOuter" -> b.layer1RadOuter(toFloat(value));
                case "layer1RadCount" -> b.layer1RadCount(toInt(value));
                case "layer1RadThickness" -> b.layer1RadThickness(toFloat(value));
                case "layer1RotOffset" -> b.layer1RotOffset(toFloat(value));
                
                // Layer 8 Geometry (Phase 3D)
                case "layer8BreathAmp" -> b.layer8BreathAmp(toFloat(value));
                case "layer8BreathCenter" -> b.layer8BreathCenter(toFloat(value));
                case "layer8OrbitalCount" -> b.layer8OrbitalCount(toInt(value));
                case "layer8OrbitalStart" -> b.layer8OrbitalStart(toFloat(value));
                case "layer8OrbitalStep" -> b.layer8OrbitalStep(toFloat(value));
                case "layer8OrbitalDist" -> b.layer8OrbitalDist(toFloat(value));
                case "layer8OrbitalThickness" -> b.layer8OrbitalThickness(toFloat(value));
                case "layer8CenterRadius" -> b.layer8CenterRadius(toFloat(value));
                case "layer8CenterThickness" -> b.layer8CenterThickness(toFloat(value));
                case "layer8RotOffset" -> b.layer8RotOffset(toFloat(value));
                
                // Stage Animation (Phase 4)
                case "animationStage" -> b.animationStage(toFloat(value));
                case "stageSpeed" -> b.stageSpeed(toFloat(value));
                case "transitionMode" -> b.transitionMode(toInt(value));
                case "animateOnSpawn" -> b.animateOnSpawn(toBool(value));
                case "animationFromCenter" -> b.animationFromCenter(toBool(value));
                
                default -> Logging.GUI.topic("adapter").warn("Unknown magicCircle property: {}", prop);
            }
        }
        
        config = b.build();
        
        // Sync to live effect for immediate visual feedback
        syncToPostEffect();
    }
    
    /**
     * Syncs the current config to MagicCirclePostEffect for live preview.
     */
    public void syncToPostEffect() {
        MagicCirclePostEffect.updateFromConfig(config);
        MagicCirclePostEffect.setEnabled(config.enabled());
    }
    
    // Helper conversion methods
    private float toFloat(Object v) { 
        return v instanceof Number n ? n.floatValue() : 0f; 
    }
    
    private boolean toBool(Object v) { 
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.floatValue() > 0.5f;
        return Boolean.parseBoolean(v.toString()); 
    }
    
    private int toInt(Object v) { 
        return v instanceof Number n ? n.intValue() : 0; 
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public MagicCircleConfig config() { return config; }
    
    public void setConfig(MagicCircleConfig config) { 
        this.config = config;
        syncToPostEffect();
    }
    
    public MagicCircleConfig getConfig() {
        return config;
    }
    
    @Override
    public void reset() {
        this.config = MagicCircleConfig.DEFAULT;
        syncToPostEffect();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JSON PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Serialize magic circle config to JSON for field definition storage.
     */
    public com.google.gson.JsonObject toJson() {
        var json = new com.google.gson.JsonObject();
        
        // Global
        json.addProperty("effectRadius", config.effectRadius());
        json.addProperty("heightTolerance", config.heightTolerance());
        json.addProperty("intensity", config.intensity());
        json.addProperty("glowExponent", config.glowExponent());
        json.addProperty("enabled", config.enabled());
        
        // Color as array
        var color = new com.google.gson.JsonArray();
        color.add(config.primaryR());
        color.add(config.primaryG());
        color.add(config.primaryB());
        json.add("primaryColor", color);
        
        // Animation
        var animation = new com.google.gson.JsonObject();
        animation.addProperty("rotationSpeed", config.rotationSpeed());
        animation.addProperty("breathingSpeed", config.breathingSpeed());
        animation.addProperty("breathingAmount", config.breathingAmount());
        json.add("animation", animation);
        
        // Position
        var position = new com.google.gson.JsonObject();
        position.addProperty("centerX", config.centerX());
        position.addProperty("centerY", config.centerY());
        position.addProperty("centerZ", config.centerZ());
        position.addProperty("followPlayer", config.followPlayer());
        json.add("position", position);
        
        // Layer controls (Phase 2)
        var layers = new com.google.gson.JsonObject();
        var enables = new com.google.gson.JsonArray();
        var intensities = new com.google.gson.JsonArray();
        var speeds = new com.google.gson.JsonArray();
        for (int i = 0; i < MagicCircleConfig.LAYER_COUNT; i++) {
            enables.add(config.getLayerEnable(i));
            intensities.add(config.getLayerIntensity(i));
            speeds.add(config.getLayerSpeed(i));
        }
        layers.add("enables", enables);
        layers.add("intensities", intensities);
        layers.add("speeds", speeds);
        json.add("layers", layers);
        
        // Layer 4 Geometry (Phase 3A)
        var layer4Geom = new com.google.gson.JsonObject();
        layer4Geom.addProperty("innerRadius", config.layer4InnerRadius());
        layer4Geom.addProperty("outerRadius", config.layer4OuterRadius());
        layer4Geom.addProperty("thickness", config.layer4Thickness());
        layer4Geom.addProperty("rotOffset", config.layer4RotOffset());
        json.add("layer4Geometry", layer4Geom);
        
        // Layer 7 Geometry (Phase 3A)
        var layer7Geom = new com.google.gson.JsonObject();
        layer7Geom.addProperty("innerRadius", config.layer7InnerRadius());
        layer7Geom.addProperty("outerRadius", config.layer7OuterRadius());
        layer7Geom.addProperty("spokeCount", config.layer7SpokeCount());
        layer7Geom.addProperty("thickness", config.layer7Thickness());
        layer7Geom.addProperty("rotOffset", config.layer7RotOffset());
        json.add("layer7Geometry", layer7Geom);
        
        // Layer 2 Geometry (Phase 3B)
        var layer2Geom = new com.google.gson.JsonObject();
        layer2Geom.addProperty("rectCount", config.layer2RectCount());
        layer2Geom.addProperty("rectSize", config.layer2RectSize());
        layer2Geom.addProperty("thickness", config.layer2Thickness());
        layer2Geom.addProperty("rotOffset", config.layer2RotOffset());
        layer2Geom.addProperty("snapRotation", config.layer2SnapRotation());
        json.add("layer2Geometry", layer2Geom);
        
        // Layer 5 Geometry (Phase 3B)
        var layer5Geom = new com.google.gson.JsonObject();
        layer5Geom.addProperty("rectCount", config.layer5RectCount());
        layer5Geom.addProperty("rectSize", config.layer5RectSize());
        layer5Geom.addProperty("thickness", config.layer5Thickness());
        layer5Geom.addProperty("rotOffset", config.layer5RotOffset());
        layer5Geom.addProperty("snapRotation", config.layer5SnapRotation());
        json.add("layer5Geometry", layer5Geom);
        
        // Layer 3 Geometry (Phase 3C)
        var layer3Geom = new com.google.gson.JsonObject();
        layer3Geom.addProperty("dotCount", config.layer3DotCount());
        layer3Geom.addProperty("orbitRadius", config.layer3OrbitRadius());
        layer3Geom.addProperty("ringInner", config.layer3RingInner());
        layer3Geom.addProperty("ringOuter", config.layer3RingOuter());
        layer3Geom.addProperty("ringThickness", config.layer3RingThickness());
        layer3Geom.addProperty("dotRadius", config.layer3DotRadius());
        layer3Geom.addProperty("dotThickness", config.layer3DotThickness());
        layer3Geom.addProperty("rotOffset", config.layer3RotOffset());
        json.add("layer3Geometry", layer3Geom);
        
        // Layer 6 Geometry (Phase 3C)
        var layer6Geom = new com.google.gson.JsonObject();
        layer6Geom.addProperty("dotCount", config.layer6DotCount());
        layer6Geom.addProperty("orbitRadius", config.layer6OrbitRadius());
        layer6Geom.addProperty("ringInner", config.layer6RingInner());
        layer6Geom.addProperty("ringOuter", config.layer6RingOuter());
        layer6Geom.addProperty("ringThickness", config.layer6RingThickness());
        layer6Geom.addProperty("dotRadius", config.layer6DotRadius());
        layer6Geom.addProperty("dotThickness", config.layer6DotThickness());
        layer6Geom.addProperty("rotOffset", config.layer6RotOffset());
        json.add("layer6Geometry", layer6Geom);
        
        // Layer 1 Geometry (Phase 3D)
        var layer1Geom = new com.google.gson.JsonObject();
        layer1Geom.addProperty("ringInner", config.layer1RingInner());
        layer1Geom.addProperty("ringOuter", config.layer1RingOuter());
        layer1Geom.addProperty("ringThickness", config.layer1RingThickness());
        layer1Geom.addProperty("radInner", config.layer1RadInner());
        layer1Geom.addProperty("radOuter", config.layer1RadOuter());
        layer1Geom.addProperty("radCount", config.layer1RadCount());
        layer1Geom.addProperty("radThickness", config.layer1RadThickness());
        layer1Geom.addProperty("rotOffset", config.layer1RotOffset());
        json.add("layer1Geometry", layer1Geom);
        
        // Layer 8 Geometry (Phase 3D)
        var layer8Geom = new com.google.gson.JsonObject();
        layer8Geom.addProperty("breathAmp", config.layer8BreathAmp());
        layer8Geom.addProperty("breathCenter", config.layer8BreathCenter());
        layer8Geom.addProperty("orbitalCount", config.layer8OrbitalCount());
        layer8Geom.addProperty("orbitalStart", config.layer8OrbitalStart());
        layer8Geom.addProperty("orbitalStep", config.layer8OrbitalStep());
        layer8Geom.addProperty("orbitalDist", config.layer8OrbitalDist());
        layer8Geom.addProperty("orbitalThickness", config.layer8OrbitalThickness());
        layer8Geom.addProperty("centerRadius", config.layer8CenterRadius());
        layer8Geom.addProperty("centerThickness", config.layer8CenterThickness());
        layer8Geom.addProperty("rotOffset", config.layer8RotOffset());
        json.add("layer8Geometry", layer8Geom);
        
        return json;
    }
    
    /**
     * Load magic circle config from JSON (field definition storage).
     */
    public void loadFromJson(com.google.gson.JsonObject json) {
        if (json == null) {
            reset();
            return;
        }
        
        MagicCircleConfig.Builder b = MagicCircleConfig.DEFAULT.toBuilder();
        
        // Global
        if (json.has("effectRadius")) b.effectRadius(json.get("effectRadius").getAsFloat());
        if (json.has("heightTolerance")) b.heightTolerance(json.get("heightTolerance").getAsFloat());
        if (json.has("intensity")) b.intensity(json.get("intensity").getAsFloat());
        if (json.has("glowExponent")) b.glowExponent(json.get("glowExponent").getAsFloat());
        if (json.has("enabled")) b.enabled(json.get("enabled").getAsBoolean());
        
        // Color
        if (json.has("primaryColor")) {
            var c = json.getAsJsonArray("primaryColor");
            if (c.size() >= 3) {
                b.primaryR(c.get(0).getAsFloat());
                b.primaryG(c.get(1).getAsFloat());
                b.primaryB(c.get(2).getAsFloat());
            }
        }
        
        // Animation
        if (json.has("animation")) {
            var anim = json.getAsJsonObject("animation");
            if (anim.has("rotationSpeed")) b.rotationSpeed(anim.get("rotationSpeed").getAsFloat());
            if (anim.has("breathingSpeed")) b.breathingSpeed(anim.get("breathingSpeed").getAsFloat());
            if (anim.has("breathingAmount")) b.breathingAmount(anim.get("breathingAmount").getAsFloat());
        }
        
        // Position
        if (json.has("position")) {
            var pos = json.getAsJsonObject("position");
            if (pos.has("centerX")) b.centerX(pos.get("centerX").getAsFloat());
            if (pos.has("centerY")) b.centerY(pos.get("centerY").getAsFloat());
            if (pos.has("centerZ")) b.centerZ(pos.get("centerZ").getAsFloat());
            if (pos.has("followPlayer")) b.followPlayer(pos.get("followPlayer").getAsBoolean());
        }
        
        // Layer controls (Phase 2)
        if (json.has("layers")) {
            var layers = json.getAsJsonObject("layers");
            if (layers.has("enables")) {
                var arr = layers.getAsJsonArray("enables");
                for (int i = 0; i < Math.min(arr.size(), MagicCircleConfig.LAYER_COUNT); i++) {
                    b.setLayerEnable(i, arr.get(i).getAsBoolean());
                }
            }
            if (layers.has("intensities")) {
                var arr = layers.getAsJsonArray("intensities");
                for (int i = 0; i < Math.min(arr.size(), MagicCircleConfig.LAYER_COUNT); i++) {
                    b.setLayerIntensity(i, arr.get(i).getAsFloat());
                }
            }
            if (layers.has("speeds")) {
                var arr = layers.getAsJsonArray("speeds");
                for (int i = 0; i < Math.min(arr.size(), MagicCircleConfig.LAYER_COUNT); i++) {
                    b.setLayerSpeed(i, arr.get(i).getAsFloat());
                }
            }
        }
        
        // Layer 4 Geometry (Phase 3A)
        if (json.has("layer4Geometry")) {
            var geom = json.getAsJsonObject("layer4Geometry");
            if (geom.has("innerRadius")) b.layer4InnerRadius(geom.get("innerRadius").getAsFloat());
            if (geom.has("outerRadius")) b.layer4OuterRadius(geom.get("outerRadius").getAsFloat());
            if (geom.has("thickness")) b.layer4Thickness(geom.get("thickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer4RotOffset(geom.get("rotOffset").getAsFloat());
        }
        
        // Layer 7 Geometry (Phase 3A)
        if (json.has("layer7Geometry")) {
            var geom = json.getAsJsonObject("layer7Geometry");
            if (geom.has("innerRadius")) b.layer7InnerRadius(geom.get("innerRadius").getAsFloat());
            if (geom.has("outerRadius")) b.layer7OuterRadius(geom.get("outerRadius").getAsFloat());
            if (geom.has("spokeCount")) b.layer7SpokeCount(geom.get("spokeCount").getAsInt());
            if (geom.has("thickness")) b.layer7Thickness(geom.get("thickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer7RotOffset(geom.get("rotOffset").getAsFloat());
        }
        
        // Layer 2 Geometry (Phase 3B)
        if (json.has("layer2Geometry")) {
            var geom = json.getAsJsonObject("layer2Geometry");
            if (geom.has("rectCount")) b.layer2RectCount(geom.get("rectCount").getAsInt());
            if (geom.has("rectSize")) b.layer2RectSize(geom.get("rectSize").getAsFloat());
            if (geom.has("thickness")) b.layer2Thickness(geom.get("thickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer2RotOffset(geom.get("rotOffset").getAsFloat());
            if (geom.has("snapRotation")) b.layer2SnapRotation(geom.get("snapRotation").getAsBoolean());
        }
        
        // Layer 5 Geometry (Phase 3B)
        if (json.has("layer5Geometry")) {
            var geom = json.getAsJsonObject("layer5Geometry");
            if (geom.has("rectCount")) b.layer5RectCount(geom.get("rectCount").getAsInt());
            if (geom.has("rectSize")) b.layer5RectSize(geom.get("rectSize").getAsFloat());
            if (geom.has("thickness")) b.layer5Thickness(geom.get("thickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer5RotOffset(geom.get("rotOffset").getAsFloat());
            if (geom.has("snapRotation")) b.layer5SnapRotation(geom.get("snapRotation").getAsBoolean());
        }
        
        // Layer 3 Geometry (Phase 3C)
        if (json.has("layer3Geometry")) {
            var geom = json.getAsJsonObject("layer3Geometry");
            if (geom.has("dotCount")) b.layer3DotCount(geom.get("dotCount").getAsInt());
            if (geom.has("orbitRadius")) b.layer3OrbitRadius(geom.get("orbitRadius").getAsFloat());
            if (geom.has("ringInner")) b.layer3RingInner(geom.get("ringInner").getAsFloat());
            if (geom.has("ringOuter")) b.layer3RingOuter(geom.get("ringOuter").getAsFloat());
            if (geom.has("ringThickness")) b.layer3RingThickness(geom.get("ringThickness").getAsFloat());
            if (geom.has("dotRadius")) b.layer3DotRadius(geom.get("dotRadius").getAsFloat());
            if (geom.has("dotThickness")) b.layer3DotThickness(geom.get("dotThickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer3RotOffset(geom.get("rotOffset").getAsFloat());
        }
        
        // Layer 6 Geometry (Phase 3C)
        if (json.has("layer6Geometry")) {
            var geom = json.getAsJsonObject("layer6Geometry");
            if (geom.has("dotCount")) b.layer6DotCount(geom.get("dotCount").getAsInt());
            if (geom.has("orbitRadius")) b.layer6OrbitRadius(geom.get("orbitRadius").getAsFloat());
            if (geom.has("ringInner")) b.layer6RingInner(geom.get("ringInner").getAsFloat());
            if (geom.has("ringOuter")) b.layer6RingOuter(geom.get("ringOuter").getAsFloat());
            if (geom.has("ringThickness")) b.layer6RingThickness(geom.get("ringThickness").getAsFloat());
            if (geom.has("dotRadius")) b.layer6DotRadius(geom.get("dotRadius").getAsFloat());
            if (geom.has("dotThickness")) b.layer6DotThickness(geom.get("dotThickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer6RotOffset(geom.get("rotOffset").getAsFloat());
        }
        
        // Layer 1 Geometry (Phase 3D)
        if (json.has("layer1Geometry")) {
            var geom = json.getAsJsonObject("layer1Geometry");
            if (geom.has("ringInner")) b.layer1RingInner(geom.get("ringInner").getAsFloat());
            if (geom.has("ringOuter")) b.layer1RingOuter(geom.get("ringOuter").getAsFloat());
            if (geom.has("ringThickness")) b.layer1RingThickness(geom.get("ringThickness").getAsFloat());
            if (geom.has("radInner")) b.layer1RadInner(geom.get("radInner").getAsFloat());
            if (geom.has("radOuter")) b.layer1RadOuter(geom.get("radOuter").getAsFloat());
            if (geom.has("radCount")) b.layer1RadCount(geom.get("radCount").getAsInt());
            if (geom.has("radThickness")) b.layer1RadThickness(geom.get("radThickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer1RotOffset(geom.get("rotOffset").getAsFloat());
        }
        
        // Layer 8 Geometry (Phase 3D)
        if (json.has("layer8Geometry")) {
            var geom = json.getAsJsonObject("layer8Geometry");
            if (geom.has("breathAmp")) b.layer8BreathAmp(geom.get("breathAmp").getAsFloat());
            if (geom.has("breathCenter")) b.layer8BreathCenter(geom.get("breathCenter").getAsFloat());
            if (geom.has("orbitalCount")) b.layer8OrbitalCount(geom.get("orbitalCount").getAsInt());
            if (geom.has("orbitalStart")) b.layer8OrbitalStart(geom.get("orbitalStart").getAsFloat());
            if (geom.has("orbitalStep")) b.layer8OrbitalStep(geom.get("orbitalStep").getAsFloat());
            if (geom.has("orbitalDist")) b.layer8OrbitalDist(geom.get("orbitalDist").getAsFloat());
            if (geom.has("orbitalThickness")) b.layer8OrbitalThickness(geom.get("orbitalThickness").getAsFloat());
            if (geom.has("centerRadius")) b.layer8CenterRadius(geom.get("centerRadius").getAsFloat());
            if (geom.has("centerThickness")) b.layer8CenterThickness(geom.get("centerThickness").getAsFloat());
            if (geom.has("rotOffset")) b.layer8RotOffset(geom.get("rotOffset").getAsFloat());
        }
        
        config = b.build();
        syncToPostEffect();
    }
}
