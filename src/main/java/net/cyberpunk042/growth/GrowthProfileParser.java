package net.cyberpunk042.growth;

import net.cyberpunk042.growth.profile.*;


import net.cyberpunk042.log.Logging;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Handles parsing of all growth profile JSON files.
 */
public final class GrowthProfileParser {

    private GrowthProfileParser() {}

    // ========================================================================
    // Load Methods
    // ========================================================================

    public static Map<Identifier, GrowthGlowProfile> loadGlowProfiles(Path dir) {
        Map<Identifier, GrowthGlowProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthGlowProfile profile = parseGlowProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping glow profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthParticleProfile> loadParticleProfiles(Path dir) {
        Map<Identifier, GrowthParticleProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthParticleProfile profile = parseParticleProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping particle profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthForceProfile> loadForceProfiles(Path dir) {
        Map<Identifier, GrowthForceProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthForceProfile profile = parseForceProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping force profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthFieldProfile> loadFieldProfiles(Path dir) {
        Map<Identifier, GrowthFieldProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthFieldProfile profile = parseFieldProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping field profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthFuseProfile> loadFuseProfiles(Path dir) {
        Map<Identifier, GrowthFuseProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthFuseProfile profile = parseFuseProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping fuse profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthExplosionProfile> loadExplosionProfiles(Path dir) {
        Map<Identifier, GrowthExplosionProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthExplosionProfile profile = parseExplosionProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping explosion profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthOpacityProfile> loadOpacityProfiles(Path dir) {
        Map<Identifier, GrowthOpacityProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthOpacityProfile profile = parseOpacityProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping opacity profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthSpinProfile> loadSpinProfiles(Path dir) {
        Map<Identifier, GrowthSpinProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthSpinProfile profile = parseSpinProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping spin profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthWobbleProfile> loadWobbleProfiles(Path dir) {
        Map<Identifier, GrowthWobbleProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthWobbleProfile profile = parseWobbleProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping wobble profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthProfile> loadGrowthProfiles(Path dir) {
        Map<Identifier, GrowthProfile> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthProfile profile = parseGrowthProfile(json);
            if (profile != null) {
                profiles.put(profile.id(), profile);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping growth profile {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    public static Map<Identifier, GrowthBlockDefinition> loadDefinitions(Path dir, Map<Identifier, GrowthProfile> growthProfiles) {
        Map<Identifier, GrowthBlockDefinition> profiles = new HashMap<>();
        loadObjects(dir, (path, json) -> {
            GrowthBlockDefinition definition = parseDefinition(json, growthProfiles);
            if (definition != null) {
                profiles.put(definition.id(), definition);
            } else {
                Logging.GROWTH.warn("[GrowthRegistry] Skipping growth block {}", path.getFileName());
            }
        });
        return Collections.unmodifiableMap(profiles);
    }

    // ========================================================================
    // Parse Methods
    // ========================================================================

    public static GrowthGlowProfile parseGlowProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthGlowProfile defaults = GrowthGlowProfile.defaults();
        Identifier primary = readOptionalTexture(json, "primary_texture", defaults.primaryTexture());
        Identifier secondary = readOptionalTexture(json, "secondary_texture", defaults.secondaryTexture());
        boolean primaryUseMesh = json.has("primary_use_mesh") ? json.get("primary_use_mesh").getAsBoolean() : defaults.primaryUseMesh();
        boolean secondaryUseMesh = json.has("secondary_use_mesh") ? json.get("secondary_use_mesh").getAsBoolean() : defaults.secondaryUseMesh();
        String primaryColor = readOptionalColor(json, "primary_color", defaults.primaryColorHex());
        String secondaryColor = readOptionalColor(json, "secondary_color", defaults.secondaryColorHex());
        GrowthGlowProfile.AnimationOverride primaryAnimation = readAnimationOverride(json, "primary_mesh_animation");
        GrowthGlowProfile.AnimationOverride secondaryAnimation = readAnimationOverride(json, "secondary_mesh_animation");
        int lightLevel = json.has("light_level") ? MathHelper.clamp(json.get("light_level").getAsInt(), 0, 15) : defaults.lightLevel();
        return new GrowthGlowProfile(id, primary, secondary, primaryUseMesh, secondaryUseMesh, primaryColor, secondaryColor, primaryAnimation, secondaryAnimation, lightLevel);
    }

    public static GrowthParticleProfile parseParticleProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthParticleProfile defaults = GrowthParticleProfile.defaults();
        Identifier particle = parseId(json, "particle", defaults.particleId());
        int count = json.has("count") ? json.get("count").getAsInt() : defaults.count();
        double speed = json.has("speed") ? json.get("speed").getAsDouble() : defaults.speed();
        Identifier sound = parseId(json, "sound", defaults.soundId());
        int soundInterval = json.has("sound_interval") ? json.get("sound_interval").getAsInt() : defaults.soundIntervalTicks();
        int intervalTicks = json.has("interval_ticks") ? json.get("interval_ticks").getAsInt() : defaults.intervalTicks();
        GrowthParticleProfile.Shape shape = defaults.shape();
        if (json.has("shape")) {
            String shapeRaw = json.get("shape").getAsString();
            try {
                shape = GrowthParticleProfile.Shape.valueOf(shapeRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                shape = defaults.shape();
            }
        }
        double radius = json.has("radius") ? json.get("radius").getAsDouble() : defaults.radius();
        double height = json.has("height") ? json.get("height").getAsDouble() : defaults.height();
        double offsetX = json.has("offset_x") ? json.get("offset_x").getAsDouble() : defaults.offsetX();
        double offsetY = json.has("offset_y") ? json.get("offset_y").getAsDouble() : defaults.offsetY();
        double offsetZ = json.has("offset_z") ? json.get("offset_z").getAsDouble() : defaults.offsetZ();
        double jitterX = json.has("jitter_x") ? json.get("jitter_x").getAsDouble() : defaults.jitterX();
        double jitterY = json.has("jitter_y") ? json.get("jitter_y").getAsDouble() : defaults.jitterY();
        double jitterZ = json.has("jitter_z") ? json.get("jitter_z").getAsDouble() : defaults.jitterZ();
        boolean followScale = json.has("follow_scale") ? json.get("follow_scale").getAsBoolean() : defaults.followScale();
        return new GrowthParticleProfile(id, particle, count, speed, sound, soundInterval, intervalTicks, shape,
                radius, height, offsetX, offsetY, offsetZ, jitterX, jitterY, jitterZ, followScale);
    }

    public static GrowthForceProfile parseForceProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        boolean enabled = getBoolean(json, true, "enabled");
        int interval = json.has("interval_ticks") ? json.get("interval_ticks").getAsInt() : 20;
        double radius = json.has("radius") ? json.get("radius").getAsDouble() : 10.0D;
        double strength = json.has("strength") ? json.get("strength").getAsDouble() : 0.8D;
        double verticalBoost = json.has("vertical_boost") ? json.get("vertical_boost").getAsDouble() : 0.25D;
        double falloff = json.has("falloff") ? json.get("falloff").getAsDouble() : 0.85D;
        double startProgress = json.has("start_progress") ? json.get("start_progress").getAsDouble() : 0.0D;
        double endProgress = json.has("end_progress") ? json.get("end_progress").getAsDouble() : 1.0D;
        boolean guardianBeams = json.has("guardian_beams") && json.get("guardian_beams").getAsBoolean();
        double edgeFalloff = json.has("edge_falloff") ? json.get("edge_falloff").getAsDouble() : 0.0D;
        int beamColor = json.has("beam_color") ? Integer.decode(json.get("beam_color").getAsString()) : 0xFFFFFF;
        Identifier particle = parseId(json, "particle", Identifier.of("minecraft", "poof"));
        int particleCount = json.has("particle_count") ? json.get("particle_count").getAsInt() : 6;
        float particleSpeed = json.has("particle_speed") ? json.get("particle_speed").getAsFloat() : 0.02F;
        Identifier sound = parseId(json, "sound", Identifier.of("minecraft", "block.beacon.activate"));
        double impactDamage = json.has("impact_damage") ? json.get("impact_damage").getAsDouble() : 0.0D;
        int impactCooldown = json.has("impact_cooldown_ticks") ? json.get("impact_cooldown_ticks").getAsInt() : 10;
        GrowthForceProfile.RingBehavior ringBehavior = json.has("ring_behavior")
                ? GrowthForceProfile.RingBehavior.fromString(json.get("ring_behavior").getAsString())
                : GrowthForceProfile.RingBehavior.NONE;
        int ringCount = json.has("ring_count") ? json.get("ring_count").getAsInt() : 0;
        double ringSpacing = json.has("ring_spacing") ? json.get("ring_spacing").getAsDouble() : 0.0D;
        double ringWidth = json.has("ring_width") ? json.get("ring_width").getAsDouble() : 0.0D;
        double ringStrength = json.has("ring_strength") ? json.get("ring_strength").getAsDouble() : 1.0D;
        List<Identifier> ringFields = json.has("ring_field_profiles")
                ? parseIdArray(json, "ring_field_profiles")
                : List.of();
        return new GrowthForceProfile(id, enabled, interval, radius, strength, verticalBoost, falloff,
                startProgress, endProgress, guardianBeams, edgeFalloff, beamColor, particle,
                particleCount, particleSpeed, sound, impactDamage, impactCooldown, ringBehavior,
                ringCount, ringSpacing, ringWidth, ringStrength, ringFields);
    }

    public static GrowthFieldProfile parseFieldProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthFieldProfile defaults = GrowthFieldProfile.defaults();
        String mesh = json.has("mesh") ? json.get("mesh").getAsString() : defaults.meshType();
        Identifier texture = parseId(json, "texture", defaults.texture());
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : defaults.alpha();
        float spinSpeed = json.has("spin_speed") ? json.get("spin_speed").getAsFloat() : defaults.spinSpeed();
        float scaleMultiplier = json.has("scale_multiplier") ? json.get("scale_multiplier").getAsFloat() : defaults.scaleMultiplier();
        String color = json.has("color") ? json.get("color").getAsString() : defaults.colorHex();
        return new GrowthFieldProfile(id, mesh, texture, alpha, spinSpeed, scaleMultiplier, color);
    }

    public static GrowthFuseProfile parseFuseProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthFuseProfile defaults = GrowthFuseProfile.defaults();
        String triggerRaw = json.has("trigger") ? json.get("trigger").getAsString() : defaults.trigger().name();
        GrowthFuseProfile.Trigger trigger = GrowthFuseProfile.Trigger.fromString(triggerRaw);
        double autoProgress = json.has("auto_progress") ? json.get("auto_progress").getAsDouble() : defaults.autoProgress();
        boolean requiresItem = json.has("requires_item") ? json.get("requires_item").getAsBoolean() : defaults.requiresItem();
        boolean consumeItem = json.has("consume_item") ? json.get("consume_item").getAsBoolean() : defaults.consumeItem();
        List<Identifier> allowedItems = json.has("allowed_items") ? parseIdArray(json, "allowed_items") : defaults.allowedItems();
        int explosionDelay = json.has("explosion_delay_ticks") ? json.get("explosion_delay_ticks").getAsInt() : defaults.explosionDelayTicks();
        int shellCollapse = json.has("shell_collapse_ticks") ? json.get("shell_collapse_ticks").getAsInt() : defaults.shellCollapseTicks();
        int pulseInterval = json.has("pulse_interval_ticks") ? json.get("pulse_interval_ticks").getAsInt() : defaults.pulseIntervalTicks();
        double collapseTarget = json.has("collapse_target_scale") ? json.get("collapse_target_scale").getAsDouble() : defaults.collapseTargetScale();
        String primaryColor = json.has("primary_color") ? json.get("primary_color").getAsString() : defaults.primaryColorHex();
        String secondaryColor = json.has("secondary_color") ? json.get("secondary_color").getAsString() : defaults.secondaryColorHex();
        Identifier particle = parseId(json, "particle", defaults.particleId());
        Identifier sound = parseId(json, "sound", defaults.soundId());
        return new GrowthFuseProfile(id, trigger, autoProgress, requiresItem, consumeItem, allowedItems,
                explosionDelay, shellCollapse, pulseInterval, collapseTarget, primaryColor, secondaryColor, particle, sound);
    }

    public static GrowthExplosionProfile parseExplosionProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthExplosionProfile defaults = GrowthExplosionProfile.defaults();
        double radius = json.has("radius") ? json.get("radius").getAsDouble() : defaults.radius();
        int charges = json.has("charges") ? json.get("charges").getAsInt() : defaults.charges();
        int amount = json.has("amount") ? json.get("amount").getAsInt() : defaults.amount();
        int amountDelay = json.has("amount_delay_ticks") ? json.get("amount_delay_ticks").getAsInt() : defaults.amountDelayTicks();
        double maxDamage = json.has("max_damage") ? json.get("max_damage").getAsDouble() : defaults.maxDamage();
        double damageScaling = json.has("damage_scaling") ? json.get("damage_scaling").getAsDouble() : defaults.damageScaling();
        boolean causesFire = json.has("causes_fire") ? json.get("causes_fire").getAsBoolean() : defaults.causesFire();
        boolean breaksBlocks = json.has("breaks_blocks") ? json.get("breaks_blocks").getAsBoolean() : defaults.breaksBlocks();
        return new GrowthExplosionProfile(id, radius, charges, amount, amountDelay, maxDamage, damageScaling, causesFire, breaksBlocks);
    }

    public static GrowthSpinProfile parseSpinProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthSpinProfile defaults = GrowthSpinProfile.defaults();
        GrowthSpinProfile.Layer primary = parseSpinLayer(json, "primary", defaults.primaryLayer());
        GrowthSpinProfile.Layer secondary = parseSpinLayer(json, "secondary", defaults.secondaryLayer());
        return new GrowthSpinProfile(id, primary, secondary);
    }

    public static GrowthOpacityProfile parseOpacityProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthOpacityProfile defaults = GrowthOpacityProfile.defaults();
        GrowthOpacityProfile.Layer primary = parseOpacityLayer(json, "primary", defaults.primaryLayer());
        GrowthOpacityProfile.Layer secondary = parseOpacityLayer(json, "secondary", defaults.secondaryLayer());
        return new GrowthOpacityProfile(id, primary, secondary);
    }

    public static GrowthWobbleProfile parseWobbleProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        boolean enabled = json.has("enabled") && json.get("enabled").getAsBoolean();
        float amplitudeX = getFloat(json, 0.0F, "amplitude_x", "amplitudeX");
        float amplitudeY = getFloat(json, 0.0F, "amplitude_y", "amplitudeY");
        float amplitudeZ = getFloat(json, 0.0F, "amplitude_z", "amplitudeZ");
        float speedX = getFloat(json, 0.0F, "speed_x", "speedX");
        float speedY = getFloat(json, 0.0F, "speed_y", "speedY");
        float speedZ = getFloat(json, 0.0F, "speed_z", "speedZ");
        return new GrowthWobbleProfile(id, enabled, amplitudeX, amplitudeY, amplitudeZ, speedX, speedY, speedZ);
    }

    public static GrowthProfile parseGrowthProfile(JsonObject json) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthProfile defaults = GrowthProfile.defaults();
        boolean enabled = getBoolean(json, defaults.growthEnabled(), "growth_enabled", "enabled");
        int rateTicks = getInt(json, defaults.rateTicks(), "rate", "rate_ticks");
        double rateScale = getDouble(json, defaults.rateScale(), "rate_scale", "scale_by_rate");
        double start = getDouble(json, defaults.startScale(), "start", "start_scale");
        double target = getDouble(json, defaults.targetScale(), "target", "target_scale");
        double min = getDouble(json, defaults.minScale(), "min", "min_scale");
        double max = getDouble(json, defaults.maxScale(), "max", "max_scale");
        return new GrowthProfile(id, enabled, rateTicks, rateScale, start, target, min, max);
    }

    public static GrowthBlockDefinition parseDefinition(JsonObject json, Map<Identifier, GrowthProfile> growthProfiles) {
        Identifier id = parseId(json, "id");
        if (id == null) {
            return null;
        }
        GrowthBlockDefinition defaults = GrowthBlockDefinition.defaults();
        boolean growthEnabled = getBoolean(json, defaults.growthEnabled(), "growth_enabled", "growthEnabled");
        int rate = getInt(json, defaults.rateTicks(), "rate", "rate_ticks", "rateTicks");
        double rateScale = getDouble(json, defaults.rateScale(), "scale_by_rate", "rateScale");
        double start = getDouble(json, defaults.startScale(), "start", "start_scale", "startScale");
        double target = getDouble(json, defaults.targetScale(), "target", "target_scale", "targetScale");
        double min = getDouble(json, defaults.minScale(), "min", "min_scale", "minScale");
        double max = getDouble(json, defaults.maxScale(), "max", "max_scale", "maxScale");
        boolean hasCollision = getBoolean(json, defaults.hasCollision(), "has_collision", "hasCollision");
        boolean doesDestruction = getBoolean(json, defaults.doesDestruction(), "does_destruction", "doesDestruction");
        boolean hasFuse = getBoolean(json, defaults.hasFuse(), "has_fuse", "hasFuse");
        double touchDamage = getDouble(json, defaults.touchDamage(), "touch_damage", "touchDamage");
        Identifier explicitGrowthProfile = parseId(json, null, "growth_profile", "growth_profile_id", "growthProfileId");
        Identifier growthProfile = explicitGrowthProfile != null ? explicitGrowthProfile : defaults.growthProfileId();
        Identifier glowProfile = parseId(json, defaults.glowProfileId(), "glow_profile", "glow_profile_id", "glowProfileId");
        Identifier particleProfile = parseId(json, defaults.particleProfileId(), "particle_profile", "particle_profile_id", "particleProfileId");
        Identifier fieldProfile = parseId(json, defaults.fieldProfileId(), "field_profile", "field_profile_id", "fieldProfileId");
        Identifier pullProfile = parseId(json, defaults.pullProfileId(), "pull_profile", "pull_profile_id", "pullProfileId");
        Identifier pushProfile = parseId(json, defaults.pushProfileId(), "push_profile", "push_profile_id", "pushProfileId");
        Identifier fuseProfile = parseId(json, defaults.fuseProfileId(), "fuse_profile", "fuse_profile_id", "fuseProfileId");
        Identifier explosionProfile = parseId(json, defaults.explosionProfileId(), "explosion_profile", "explosion_profile_id", "explosionProfileId");
        Identifier opacityProfile = parseId(json, defaults.opacityProfileId(), "opacity_profile", "opacity_profile_id", "opacityProfileId");
        Identifier spinProfile = parseId(json, defaults.spinProfileId(), "spin_profile", "spin_profile_id", "spinProfileId");
        Identifier wobbleProfile = parseId(json, defaults.wobbleProfileId(), "wobble_profile", "wobble_profile_id", "wobbleProfileId");

        if (explicitGrowthProfile != null) {
            GrowthProfile preset = growthProfiles.getOrDefault(growthProfile, GrowthProfile.defaults());
            growthEnabled = preset.growthEnabled();
            rate = preset.rateTicks();
            rateScale = preset.rateScale();
            start = preset.startScale();
            target = preset.targetScale();
            min = preset.minScale();
            max = preset.maxScale();
        }
        return new GrowthBlockDefinition(id, growthEnabled, rate, rateScale, start, target, min, max,
                hasCollision, doesDestruction, hasFuse, touchDamage, growthProfile, glowProfile,
                particleProfile, fieldProfile, pullProfile, pushProfile, fuseProfile, explosionProfile,
                opacityProfile, spinProfile, wobbleProfile);
    }

    // ========================================================================
    // Layer Parsing Helpers
    // ========================================================================

    private static GrowthSpinProfile.Layer parseSpinLayer(JsonObject parent, String prefix, GrowthSpinProfile.Layer fallback) {
        if (parent == null) {
            return fallback;
        }
        JsonObject explicit = parent.has(prefix) && parent.get(prefix).isJsonObject() ? parent.getAsJsonObject(prefix) : null;
        if (explicit != null) {
            float speed = getFloat(explicit, fallback.speed(), "speed");
            float direction = getFloat(explicit, fallback.directionMultiplier(), "direction", "direction_multiplier");
            float axisX = getFloat(explicit, fallback.axisX(), "axis_x", "axisX");
            float axisY = getFloat(explicit, fallback.axisY(), "axis_y", "axisY");
            float axisZ = getFloat(explicit, fallback.axisZ(), "axis_z", "axisZ");
            return new GrowthSpinProfile.Layer(speed, direction, axisX, axisY, axisZ);
        }
        float speed = getFloat(parent, fallback.speed(), prefix + "_speed", prefix + "Speed");
        float direction = getFloat(parent, fallback.directionMultiplier(), prefix + "_direction", prefix + "Direction", prefix + "_direction_multiplier", prefix + "DirectionMultiplier");
        float axisX = getFloat(parent, fallback.axisX(), prefix + "_axis_x", prefix + "AxisX");
        float axisY = getFloat(parent, fallback.axisY(), prefix + "_axis_y", prefix + "AxisY");
        float axisZ = getFloat(parent, fallback.axisZ(), prefix + "_axis_z", prefix + "AxisZ");
        return new GrowthSpinProfile.Layer(speed, direction, axisX, axisY, axisZ);
    }

    private static GrowthOpacityProfile.Layer parseOpacityLayer(JsonObject parent, String prefix, GrowthOpacityProfile.Layer fallback) {
        if (parent == null) {
            return fallback;
        }
        JsonObject explicit = parent.has(prefix) && parent.get(prefix).isJsonObject() ? parent.getAsJsonObject(prefix) : null;
        if (explicit != null) {
            float base = getFloat(explicit, fallback.baseAlpha(), "base_alpha", "alpha");
            float pulseSpeed = getFloat(explicit, fallback.pulseSpeed(), "pulse_speed");
            float pulseAmp = getFloat(explicit, fallback.pulseAmplitude(), "pulse_amplitude");
            return new GrowthOpacityProfile.Layer(base, pulseSpeed, pulseAmp);
        }
        float base = getFloat(parent, fallback.baseAlpha(), prefix + "_alpha", prefix + "Alpha", prefix + "_base_alpha");
        float pulseSpeed = getFloat(parent, fallback.pulseSpeed(), prefix + "_pulse_speed", prefix + "PulseSpeed");
        float pulseAmp = getFloat(parent, fallback.pulseAmplitude(), prefix + "_pulse_amplitude", prefix + "PulseAmplitude");
        return new GrowthOpacityProfile.Layer(base, pulseSpeed, pulseAmp);
    }

    // ========================================================================
    // Animation Override Parsing
    // ========================================================================

    private static Identifier readOptionalTexture(JsonObject json, String key, Identifier fallback) {
        if (json == null || key == null || !json.has(key)) {
            return fallback;
        }
        if (json.get(key).isJsonNull()) {
            return null;
        }
        return parseId(json, key, fallback);
    }

    private static String readOptionalColor(JsonObject json, String key, String fallback) {
        if (json == null || key == null || !json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        return json.get(key).getAsString();
    }

    private static GrowthGlowProfile.AnimationOverride readAnimationOverride(JsonObject json, String key) {
        if (json == null || key == null || !json.has(key) || !json.get(key).isJsonObject()) {
            return null;
        }
        JsonObject obj = json.getAsJsonObject(key);
        Float frameTime = obj.has("frame_time") ? obj.get("frame_time").getAsFloat() : null;
        Boolean interpolate = obj.has("interpolate") ? obj.get("interpolate").getAsBoolean() : null;
        List<Integer> frames = null;
        if (obj.has("frames") && obj.get("frames").isJsonArray()) {
            frames = new ArrayList<>();
            for (JsonElement element : obj.getAsJsonArray("frames")) {
                frames.add(element.getAsInt());
            }
        }
        if (frameTime == null && interpolate == null && (frames == null || frames.isEmpty())) {
            return null;
        }
        Float scrollSpeed = obj.has("scroll_speed") ? obj.get("scroll_speed").getAsFloat() : null;
        return new GrowthGlowProfile.AnimationOverride(frameTime, interpolate, frames, scrollSpeed);
    }

    // ========================================================================
    // File Loading
    // ========================================================================

    public static void loadObjects(Path dir, JsonFileConsumer consumer) {
        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (var reader = Files.newBufferedReader(path)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (!parsed.isJsonObject()) {
                        Logging.GROWTH.warn("[GrowthRegistry] Skipping {} (not an object)", path.getFileName());
                        return;
                    }
                    consumer.accept(path, parsed.getAsJsonObject());
                } catch (Exception ex) {
                    Logging.GROWTH.error("[GrowthRegistry] Failed parsing {}", path.getFileName(), ex);
                }
            });
        } catch (IOException ex) {
            Logging.GROWTH.error("[GrowthRegistry] Failed listing {}", dir, ex);
        }
    }

    // ========================================================================
    // Utility Parsers
    // ========================================================================

    public static Identifier parseId(JsonObject json, String key) {
        if (json == null || key == null || !json.has(key)) {
            return null;
        }
        return parseId(json, key, null);
    }

    public static Identifier parseId(JsonObject json, String key, Identifier fallback) {
        if (json == null || key == null || !json.has(key)) {
            return fallback;
        }
        JsonElement element = json.get(key);
        if (element.isJsonPrimitive()) {
            String raw = element.getAsString();
            Identifier parsed = Identifier.tryParse(raw);
            return parsed != null ? parsed : fallback;
        } else if (element.isJsonObject()) {
            JsonObject idObj = element.getAsJsonObject();
            String namespace = idObj.has("namespace") ? idObj.get("namespace").getAsString() : "minecraft";
            String path = idObj.has("path") ? idObj.get("path").getAsString() : null;
            if (path != null) {
                return Identifier.of(namespace, path);
            }
        }
        return fallback;
    }

    public static Identifier parseId(JsonObject json, Identifier fallback, String... keys) {
        for (String key : keys) {
            Identifier parsed = parseId(json, key);
            if (parsed != null) {
                return parsed;
            }
        }
        return fallback;
    }

    public static List<Identifier> parseIdArray(JsonObject json, String key) {
        if (json == null || key == null || !json.has(key) || !json.get(key).isJsonArray()) {
            return List.of();
        }
        List<Identifier> ids = new ArrayList<>();
        json.getAsJsonArray(key).forEach(element -> {
            if (element.isJsonPrimitive()) {
                Identifier parsed = Identifier.tryParse(element.getAsString());
                if (parsed != null) {
                    ids.add(parsed);
                }
            }
        });
        return List.copyOf(ids);
    }

    public static boolean getBoolean(JsonObject json, boolean fallback, String... keys) {
        for (String key : keys) {
            if (json != null && key != null && json.has(key)) {
                try {
                    return json.get(key).getAsBoolean();
                } catch (Exception ignored) {}
            }
        }
        return fallback;
    }

    public static int getInt(JsonObject json, int fallback, String... keys) {
        for (String key : keys) {
            if (json != null && key != null && json.has(key)) {
                try {
                    return json.get(key).getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return fallback;
    }

    public static double getDouble(JsonObject json, double fallback, String... keys) {
        for (String key : keys) {
            if (json != null && key != null && json.has(key)) {
                try {
                    return json.get(key).getAsDouble();
                } catch (Exception ignored) {}
            }
        }
        return fallback;
    }

    public static float getFloat(JsonObject json, float fallback, String... keys) {
        for (String key : keys) {
            if (json != null && key != null && json.has(key)) {
                try {
                    return json.get(key).getAsFloat();
                } catch (Exception ignored) {}
            }
        }
        return fallback;
    }

    @FunctionalInterface
    public interface JsonFileConsumer {
        void accept(Path path, JsonObject json) throws Exception;
    }
}
