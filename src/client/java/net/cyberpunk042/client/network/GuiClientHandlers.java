package net.cyberpunk042.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.client.gui.screen.FieldCustomizerScreen;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.gui.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import org.joml.Vector3f;
/**
 * G131-G134: Client-side packet handlers for GUI.
 */
@Environment(EnvType.CLIENT)
public final class GuiClientHandlers {
    
    private GuiClientHandlers() {}
    
    /**
     * G131: Register all client packet handlers.
     * Call this from client mod initializer.
     */
    public static void register() {
        // G132: Handle GUI open command from server
        ClientPlayNetworking.registerGlobalReceiver(GuiOpenS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").info("Received GUI open: profile={}, debug={}", 
                    payload.profileName(), payload.debugUnlocked());
                
                // Use existing state if available, otherwise create new
                FieldEditState state = FieldEditStateHolder.getOrCreate();
                state.setCurrentProfileName(payload.profileName());
                state.set("debugUnlocked", payload.debugUnlocked());
                
                MinecraftClient.getInstance().setScreen(new FieldCustomizerScreen(state));
            });
        });
        
        // G133: Handle profile sync from server
        ClientPlayNetworking.registerGlobalReceiver(ProfileSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.success()) {
                    Logging.GUI.topic("network").debug("Profile synced: {}", payload.profileName());
                    ToastNotification.success("Profile loaded: " + payload.profileName());
                    
                    // Apply profile JSON to current state
                    if (!payload.profileJson().isEmpty()) {
                        FieldEditState state = FieldEditStateHolder.getOrCreate();
                        state.fromProfileJson(payload.profileJson());
                        state.setCurrentProfile(payload.profileName(), true);  // Server profile
                        state.saveSnapshot();  // For revert functionality
                        Logging.GUI.topic("network").info("Applied profile '{}' to state", payload.profileName());
                    }
                } else {
                    Logging.GUI.topic("network").warn("Profile sync failed: {}", payload.message());
                    ToastNotification.error(payload.message());
                }
            });
        });
        
        // G134: Handle debug field response
        ClientPlayNetworking.registerGlobalReceiver(DebugFieldS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").debug("Debug field update: active={}", payload.active());
                if (!payload.status().isEmpty()) {
                    ToastNotification.info(payload.status());
                }
            });
        });
        
        // Handle server profiles list
        ClientPlayNetworking.registerGlobalReceiver(ServerProfilesS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").info("Received {} server profiles", payload.profileNames().size());
                
                // Update profiles panel with server profiles
                FieldEditState state = FieldEditStateHolder.getOrCreate();
                state.setServerProfiles(payload.profileNames());
            });
        });
        
        // Handle field edit updates from /field commands
        ClientPlayNetworking.registerGlobalReceiver(FieldEditUpdateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").debug("Received edit update: type={}", payload.updateType());
                applyFieldEditUpdate(payload);
            });
        });
        
        // Handle shockwave trigger broadcast - trigger local visual effect
        ClientPlayNetworking.registerGlobalReceiver(ShockwaveTriggerS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                Logging.GUI.topic("network").info("Received shockwave trigger at ({}, {}, {})", 
                    payload.worldX(), payload.worldY(), payload.worldZ());
                
                // Apply shockwave config and trigger effect
                try {
                    // CRITICAL: Disable follow camera - shockwave should stay at world position
                    net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setFollowCamera(false);
                    net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setTargetPosition(
                        payload.worldX(), payload.worldY(), payload.worldZ());
                    net.cyberpunk042.client.visual.shader.ShockwavePostEffect.trigger();
                } catch (Exception e) {
                    Logging.GUI.topic("network").error("Failed to trigger shockwave: {}", e.getMessage());
                }
            });
        });
        
        Logging.GUI.topic("network").info("GUI client handlers registered");
    }
    
    /**
     * Applies a field edit update from server command to client state.
     */
    private static void applyFieldEditUpdate(FieldEditUpdateS2CPayload payload) {
        FieldEditState state = FieldEditStateHolder.getOrCreate();
        String type = payload.updateType();
        
        try {
            JsonObject json = JsonParser.parseString(payload.jsonData()).getAsJsonObject();
            
            switch (type) {
                case "RESET" -> {
                    FieldEditStateHolder.reset();
                    ToastNotification.info("Field state reset");
                }
                case "TEST_FIELD" -> {
                    String action = json.has("action") ? json.get("action").getAsString() : "";
                    switch (action) {
                        case "spawn" -> FieldEditStateHolder.spawnTestField();
                        case "despawn" -> FieldEditStateHolder.despawnTestField();
                        case "toggle" -> FieldEditStateHolder.toggleTestField();
                    }
                }
                case "SHAPE_TYPE" -> {
                    if (json.has("type")) state.set("shapeType", json.get("type").getAsString());
                }
                case "SHAPE" -> applyShapeUpdate(state, json);
                case "TRANSFORM" -> applyTransformUpdate(state, json);
                case "ORBIT" -> applyOrbitUpdate(state, json);
                case "FILL" -> applyFillUpdate(state, json);
                case "VISIBILITY" -> applyVisibilityUpdate(state, json);
                case "APPEARANCE" -> applyAppearanceUpdate(state, json);
                case "ANIMATION" -> applyAnimationUpdate(state, json);
                case "FOLLOW" -> {
                    // New unified follow config
                    if (json.has("enabled")) state.set("follow.enabled", json.get("enabled").getAsBoolean());
                    if (json.has("leadOffset")) state.set("follow.leadOffset", json.get("leadOffset").getAsFloat());
                    if (json.has("responsiveness")) state.set("follow.responsiveness", json.get("responsiveness").getAsFloat());
                    if (json.has("lookAhead")) state.set("follow.lookAhead", json.get("lookAhead").getAsFloat());
                }
                case "PREDICTION" -> applyFollowUpdate(state, json);  // Legacy - redirect to follow
                
                // $ref loading - resolve fragment and apply
                case "SHAPE_REF" -> applyFragmentRef(state, "shape", json);
                case "FILL_REF" -> applyFragmentRef(state, "fill", json);
                case "VISIBILITY_REF" -> applyFragmentRef(state, "visibility", json);
                case "APPEARANCE_REF" -> applyFragmentRef(state, "appearance", json);
                case "ANIMATION_REF" -> applyFragmentRef(state, "animation", json);
                case "TRANSFORM_REF" -> applyFragmentRef(state, "transform", json);
                
                default -> Logging.GUI.topic("network").warn("Unknown edit update type: {}", type);
            }
        } catch (Exception e) {
            Logging.GUI.topic("network").error("Failed to apply edit update: {}", e.getMessage());
        }
    }
    
    private static void applyShapeUpdate(FieldEditState state, JsonObject json) {
        FieldEditState.ProfileSnapshot snapshot = state.getProfileSnapshot();
        
        if (json.has("radius")) {
            float newVal = json.get("radius").getAsFloat();
            state.set("radius", newVal);
            showProfileContext("radius", newVal, snapshot.radius());
        }
        if (json.has("latSteps")) {
            int newVal = json.get("latSteps").getAsInt();
            state.set("sphere.latSteps", newVal);
            showProfileContext("latSteps", newVal, snapshot.latSteps());
        }
        if (json.has("lonSteps")) {
            int newVal = json.get("lonSteps").getAsInt();
            state.set("sphere.lonSteps", newVal);
            showProfileContext("lonSteps", newVal, snapshot.lonSteps());
        }
        if (json.has("algorithm")) state.set("sphere.algorithm", json.get("algorithm").getAsString());
    }
    
    /**
     * Shows profile context if value differs from profile snapshot.
     */
    private static void showProfileContext(String param, Number newVal, Number profileVal) {
        if (!newVal.equals(profileVal)) {
            ToastNotification.info("Profile value: " + profileVal);
        }
    }
    
    private static void applyTransformUpdate(FieldEditState state, JsonObject json) {
        if (json.has("anchor")) state.set("transform.anchor", json.get("anchor").getAsString());
        if (json.has("scale")) state.set("transform.scale", json.get("scale").getAsFloat());
        
        // Handle offset (individual or combined)
        float ox = json.has("offsetX") ? json.get("offsetX").getAsFloat() : state.getFloat("transform.offset.x");
        float oy = json.has("offsetY") ? json.get("offsetY").getAsFloat() : state.getFloat("transform.offset.y");
        float oz = json.has("offsetZ") ? json.get("offsetZ").getAsFloat() : state.getFloat("transform.offset.z");
        if (json.has("offsetX") || json.has("offsetY") || json.has("offsetZ")) {
            state.set("transform.offset", new Vector3f(ox, oy, oz));
        }
        
        // Handle rotation (individual or combined)
        float rx = json.has("rotationX") ? json.get("rotationX").getAsFloat() : state.getFloat("transform.rotation.x");
        float ry = json.has("rotationY") ? json.get("rotationY").getAsFloat() : state.getFloat("transform.rotation.y");
        float rz = json.has("rotationZ") ? json.get("rotationZ").getAsFloat() : state.getFloat("transform.rotation.z");
        if (json.has("rotationX") || json.has("rotationY") || json.has("rotationZ")) {
            state.set("transform.rotation", new Vector3f(rx, ry, rz));
        }
    }
    
    private static void applyOrbitUpdate(FieldEditState state, JsonObject json) {
        if (json.has("enabled")) state.set("transform.orbit.enabled", json.get("enabled").getAsBoolean());
        if (json.has("radius")) state.set("transform.orbit.radius", json.get("radius").getAsFloat());
        if (json.has("speed")) state.set("transform.orbit.speed", json.get("speed").getAsFloat());
        if (json.has("axis")) state.set("transform.orbit.axis", json.get("axis").getAsString());
        if (json.has("phase")) state.set("transform.orbit.phase", json.get("phase").getAsFloat());
    }
    
    private static void applyFillUpdate(FieldEditState state, JsonObject json) {
        if (json.has("mode")) state.set("fill.mode", net.cyberpunk042.visual.fill.FillMode.fromId(json.get("mode").getAsString()));
        if (json.has("wireThickness")) state.set("fill.wireThickness", json.get("wireThickness").getAsFloat());
        if (json.has("doubleSided")) state.set("fill.doubleSided", json.get("doubleSided").getAsBoolean());
    }
    
    private static void applyVisibilityUpdate(FieldEditState state, JsonObject json) {
        FieldEditState.ProfileSnapshot snapshot = state.getProfileSnapshot();
        
        if (json.has("mask")) state.set("mask.type", json.get("mask").getAsString());
        if (json.has("maskType")) state.set("mask.type", json.get("maskType").getAsString());
        if (json.has("count")) {
            int newVal = json.get("count").getAsInt();
            state.set("mask.count", newVal);
            showProfileContext("maskCount", newVal, snapshot.maskCount());
        }
        if (json.has("maskCount")) {
            int newVal = json.get("maskCount").getAsInt();
            state.set("mask.count", newVal);
            showProfileContext("maskCount", newVal, snapshot.maskCount());
        }
        if (json.has("thickness")) state.set("mask.thickness", json.get("thickness").getAsFloat());
    }
    
    private static void applyAppearanceUpdate(FieldEditState state, JsonObject json) {
        if (json.has("color")) state.set("appearance.color", json.get("color").getAsInt());
        if (json.has("alpha")) state.set("appearance.alpha", json.get("alpha").getAsFloat());
        if (json.has("glow")) state.set("appearance.glow", json.get("glow").getAsFloat());
        if (json.has("emissive")) state.set("appearance.emissive", json.get("emissive").getAsFloat());
    }
    
    private static void applyAnimationUpdate(FieldEditState state, JsonObject json) {
        // Spin (per-axis or legacy format)
        if (json.has("spin")) {
            JsonObject spin = json.getAsJsonObject("spin");
            
            // New per-axis format
            if (spin.has("speedX")) state.set("spin.speedX", spin.get("speedX").getAsFloat());
            if (spin.has("speedY")) state.set("spin.speedY", spin.get("speedY").getAsFloat());
            if (spin.has("speedZ")) state.set("spin.speedZ", spin.get("speedZ").getAsFloat());
            if (spin.has("oscillateX")) state.set("spin.oscillateX", spin.get("oscillateX").getAsBoolean());
            if (spin.has("oscillateY")) state.set("spin.oscillateY", spin.get("oscillateY").getAsBoolean());
            if (spin.has("oscillateZ")) state.set("spin.oscillateZ", spin.get("oscillateZ").getAsBoolean());
            if (spin.has("rangeX")) state.set("spin.rangeX", spin.get("rangeX").getAsFloat());
            if (spin.has("rangeY")) state.set("spin.rangeY", spin.get("rangeY").getAsFloat());
            if (spin.has("rangeZ")) state.set("spin.rangeZ", spin.get("rangeZ").getAsFloat());
            
            // Legacy format (axis + speed)
            if (spin.has("axis") && spin.has("speed")) {
                String axis = spin.get("axis").getAsString().toUpperCase();
                float speed = spin.get("speed").getAsFloat();
                switch (axis) {
                    case "X" -> state.set("spin.speedX", speed);
                    case "Y" -> state.set("spin.speedY", speed);
                    case "Z" -> state.set("spin.speedZ", speed);
                }
            }
        }
        // Pulse
        if (json.has("pulse")) {
            JsonObject pulse = json.getAsJsonObject("pulse");
            if (pulse.has("speed")) state.set("pulse.speed", pulse.get("speed").getAsFloat());
            if (pulse.has("amplitude")) state.set("pulse.amplitude", pulse.get("amplitude").getAsFloat());
        }
    }
    
    private static void applyFollowUpdate(FieldEditState state, JsonObject json) {
        // Legacy prediction fields - convert to follow config
        if (json.has("enabled")) state.set("follow.enabled", json.get("enabled").getAsBoolean());
        if (json.has("leadTicks")) {
            // Convert leadTicks to leadOffset (approximate mapping)
            int leadTicks = json.get("leadTicks").getAsInt();
            state.set("follow.leadOffset", leadTicks * 0.1f);
        }
        if (json.has("maxDistance")) {
            // maxDistance is handled differently in new system - ignore for now
        }
        if (json.has("leadOffset")) state.set("follow.leadOffset", json.get("leadOffset").getAsFloat());
        if (json.has("responsiveness")) state.set("follow.responsiveness", json.get("responsiveness").getAsFloat());
        if (json.has("lookAhead")) state.set("follow.lookAhead", json.get("lookAhead").getAsFloat());
    }
    
    /**
     * Loads a fragment from $ref and applies it to state.
     * Uses FragmentRegistry to resolve and apply the reference.
     */
    private static void applyFragmentRef(FieldEditState state, String category, JsonObject json) {
        if (!json.has("$ref")) {
            Logging.GUI.topic("network").warn("Fragment ref missing $ref field");
            return;
        }
        
        String ref = json.get("$ref").getAsString();
        Logging.GUI.topic("network").debug("Loading fragment ref: {} for {}", ref, category);
        
        try {
            // Use FragmentRegistry to apply the fragment
            net.cyberpunk042.client.gui.util.FragmentRegistry.applyFragment(state, category, ref);
            ToastNotification.success("Loaded: " + ref);
        } catch (Exception e) {
            Logging.GUI.topic("network").error("Failed to load fragment {}: {}", ref, e.getMessage());
            ToastNotification.error("Failed to load: " + ref);
        }
    }
}
