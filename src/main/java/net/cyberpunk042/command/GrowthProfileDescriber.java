package net.cyberpunk042.command;

import java.util.Locale;
import java.util.stream.Collectors;

import net.cyberpunk042.command.util.CommandFormatters;
import net.cyberpunk042.command.util.ProfileDescriber;
import net.cyberpunk042.growth.profile.GrowthFieldProfile;
import net.cyberpunk042.growth.profile.GrowthForceProfile;
import net.cyberpunk042.growth.profile.GrowthGlowProfile;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.GrowthProfile;
import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.growth.profile.GrowthOpacityProfile;
import net.cyberpunk042.growth.profile.GrowthSpinProfile;
import net.cyberpunk042.growth.profile.GrowthWobbleProfile;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.infection.service.InfectionServices;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Growth-specific profile description utilities.
 */
public final class GrowthProfileDescriber {

    private GrowthProfileDescriber() {}

    public static void reportDefinition(
            ServerCommandSource source,
            String label,
            Identifier definitionId,
            GrowthBlockDefinition definition,
            GrowthOverrides overrides
    ) {
        source.sendFeedback(() -> Text.literal(label + " definition: " + definitionId), false);
        source.sendFeedback(() -> Text.literal("Growth: enabled=" + definition.growthEnabled()
                + " rateTicks=" + definition.rateTicks()
                + " rateScale=" + CommandFormatters.formatDouble(definition.rateScale())), false);
        source.sendFeedback(() -> Text.literal("Scale: start=" + CommandFormatters.formatDouble(definition.startScale())
                + " target=" + CommandFormatters.formatDouble(definition.targetScale())
                + " min=" + CommandFormatters.formatDouble(definition.minScale())
                + " max=" + CommandFormatters.formatDouble(definition.maxScale())), false);
        source.sendFeedback(() -> Text.literal("Flags: collision=" + definition.hasCollision()
                + " destruction=" + definition.doesDestruction()
                + " fuse=" + definition.hasFuse()), false);
        source.sendFeedback(() -> Text.literal("Touch damage=" + CommandFormatters.formatDouble(definition.touchDamage())), false);
        source.sendFeedback(() -> Text.literal("Profiles: growth=" + definition.growthProfileId()
                + " glow=" + definition.glowProfileId()
                + " particle=" + definition.particleProfileId()
                + " field=" + definition.fieldProfileId()
                + " opacity=" + definition.opacityProfileId()
                + " spin=" + definition.spinProfileId()
                + " wobble=" + definition.wobbleProfileId()), false);
        source.sendFeedback(() -> Text.literal("Force profiles: pull=" + definition.pullProfileId()
                + " push=" + definition.pushProfileId()), false);
        source.sendFeedback(() -> Text.literal("Fuse & explosion: fuse=" + definition.fuseProfileId()
                + " explosion=" + definition.explosionProfileId()), false);

        GrowthRegistry registry = InfectionServices.get().growth();
        if (registry != null) {
            GrowthProfile growthProfile = registry.growthProfile(definition.growthProfileId());
            GrowthGlowProfile glow = registry.glowProfile(definition.glowProfileId());
            GrowthOpacityProfile opacity = registry.opacityProfile(definition.opacityProfileId());
            GrowthSpinProfile spin = registry.spinProfile(definition.spinProfileId());
            GrowthFieldProfile field = registry.fieldProfile(definition.fieldProfileId());
            GrowthForceProfile pull = registry.forceProfile(definition.pullProfileId(), GrowthForceProfile.defaultsPull());
            GrowthForceProfile push = registry.forceProfile(definition.pushProfileId(), GrowthForceProfile.defaultsPush());
            GrowthWobbleProfile wobble = registry.wobbleProfile(definition.wobbleProfileId());
            source.sendFeedback(() -> Text.literal("Growth detail: " + describeGrowth(growthProfile)), false);
            source.sendFeedback(() -> Text.literal("Glow detail: " + describeGlow(glow)), false);
            source.sendFeedback(() -> Text.literal("Opacity detail: " + describeOpacity(opacity)), false);
            source.sendFeedback(() -> Text.literal("Spin detail: " + describeSpin(spin)), false);
            source.sendFeedback(() -> Text.literal("Field detail: " + describeField(field)), false);
            source.sendFeedback(() -> Text.literal("Force detail: pull=" + describeForce(pull) + " | push=" + describeForce(push)), false);
            source.sendFeedback(() -> Text.literal("Wobble detail: " + describeWobble(wobble)), false);
        } else {
            source.sendFeedback(() -> Text.literal("Profile details unavailable (registry not loaded)."), false);
        }

        source.sendFeedback(() -> Text.literal("Overrides: " + formatOverrides(overrides)), false);
    }

    public static String describeGlow(GrowthGlowProfile profile) {
        if (profile == null) return "unavailable";
        return ProfileDescriber.builder(profile, profile.id())
                .add("primaryTex", GrowthGlowProfile::primaryTexture)
                .add("secondaryTex", GrowthGlowProfile::secondaryTexture)
                .add("primaryUseMesh", GrowthGlowProfile::primaryUseMesh)
                .add("secondaryUseMesh", GrowthGlowProfile::secondaryUseMesh)
                .add("primaryColor", GrowthGlowProfile::primaryColorHex)
                .add("secondaryColor", GrowthGlowProfile::secondaryColorHex)
                .add("lightLevel", GrowthGlowProfile::lightLevel)
                .addRaw("primaryAnim", describeAnimation(profile.primaryMeshAnimation()))
                .addRaw("secondaryAnim", describeAnimation(profile.secondaryMeshAnimation()))
                .build();
    }

    public static String describeAnimation(GrowthGlowProfile.AnimationOverride anim) {
        if (anim == null) return "default";
        StringBuilder builder = new StringBuilder("{");
        if (anim.frameTimeTicks() != null) {
            builder.append("frameTime=").append(anim.frameTimeTicks());
        }
        if (anim.interpolate() != null) {
            if (builder.length() > 1) builder.append(' ');
            builder.append("interpolate=").append(anim.interpolate());
        }
        if (anim.hasFrames()) {
            if (builder.length() > 1) builder.append(' ');
            builder.append("frames=").append(anim.frames());
        }
        if (anim.scrollSpeed() != null) {
            if (builder.length() > 1) builder.append(' ');
            builder.append("scrollSpeed=").append(anim.scrollSpeed());
        }
        builder.append('}');
        return builder.toString();
    }

    public static String describeOpacity(GrowthOpacityProfile profile) {
        if (profile == null) return "unavailable";
        return ProfileDescriber.builder(profile, profile.id())
                .addRaw("primary", describeOpacityLayer(profile.primaryLayer()))
                .addRaw("secondary", describeOpacityLayer(profile.secondaryLayer()))
                .build();
    }

    private static String describeOpacityLayer(GrowthOpacityProfile.Layer layer) {
        return "(alpha=" + CommandFormatters.formatFloat(layer.baseAlpha())
                + " pulseSpeed=" + CommandFormatters.formatFloat(layer.pulseSpeed())
                + " pulseAmp=" + CommandFormatters.formatFloat(layer.pulseAmplitude()) + ")";
    }

    public static String describeSpin(GrowthSpinProfile profile) {
        if (profile == null) return "unavailable";
        return ProfileDescriber.builder(profile, profile.id())
                .addRaw("primary", describeSpinLayer(profile.primaryLayer()))
                .addRaw("secondary", describeSpinLayer(profile.secondaryLayer()))
                .build();
    }

    private static String describeSpinLayer(GrowthSpinProfile.Layer layer) {
        return "(speed=" + CommandFormatters.formatFloat(layer.speed())
                + " dir=" + CommandFormatters.formatFloat(layer.directionMultiplier())
                + " axis=" + axisString(layer) + ")";
    }

    private static String axisString(GrowthSpinProfile.Layer layer) {
        return "(" + CommandFormatters.formatFloat(layer.axisX())
                + "," + CommandFormatters.formatFloat(layer.axisY())
                + "," + CommandFormatters.formatFloat(layer.axisZ()) + ")";
    }

    public static String describeField(GrowthFieldProfile profile) {
        if (profile == null) return "unavailable";
        return ProfileDescriber.builder(profile, profile.id())
                .add("mesh", GrowthFieldProfile::meshType)
                .add("alpha", GrowthFieldProfile::alpha)
                .add("spin", GrowthFieldProfile::spinSpeed)
                .add("scale", GrowthFieldProfile::scaleMultiplier)
                .add("texture", GrowthFieldProfile::texture)
                .build();
    }

    public static String describeForce(GrowthForceProfile profile) {
        if (profile == null) return "unavailable";
        return ProfileDescriber.builder(profile, profile.id())
                .add("enabled", GrowthForceProfile::enabled)
                .add("interval", GrowthForceProfile::intervalTicks)
                .add("radius", GrowthForceProfile::radius)
                .add("strength", GrowthForceProfile::strength)
                .build();
    }

    public static String describeGrowth(GrowthProfile profile) {
        if (profile == null) return "unavailable";
        return ProfileDescriber.builder(profile, profile.id())
                .add("enabled", GrowthProfile::growthEnabled)
                .add("rate", GrowthProfile::rateTicks)
                .add("rateScale", GrowthProfile::rateScale)
                .add("start", GrowthProfile::startScale)
                .add("target", GrowthProfile::targetScale)
                .add("min", GrowthProfile::minScale)
                .add("max", GrowthProfile::maxScale)
                .build();
    }

    public static String describeWobble(GrowthWobbleProfile profile) {
        if (profile == null) return "unavailable";
        return ProfileDescriber.builder(profile, profile.id())
                .add("enabled", GrowthWobbleProfile::enabled)
                .addRaw("amplitude", "(" + CommandFormatters.formatDouble(profile.amplitudeX())
                        + "," + CommandFormatters.formatDouble(profile.amplitudeY())
                        + "," + CommandFormatters.formatDouble(profile.amplitudeZ()) + ")")
                .addRaw("speed", "(" + CommandFormatters.formatDouble(profile.speedX())
                        + "," + CommandFormatters.formatDouble(profile.speedY())
                        + "," + CommandFormatters.formatDouble(profile.speedZ()) + ")")
                .build();
    }

    public static String formatOverrides(GrowthOverrides overrides) {
        if (overrides == null || overrides.isEmpty()) return "none";
        return overrides.snapshot().entrySet().stream()
                .map(entry -> entry.getKey().name().toLowerCase(Locale.ROOT) + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
