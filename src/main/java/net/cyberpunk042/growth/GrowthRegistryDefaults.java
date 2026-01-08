package net.cyberpunk042.growth;

import net.cyberpunk042.growth.profile.*;


import net.cyberpunk042.log.Logging;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.util.Identifier;

/**
 * Handles creation of default profile JSON files.
 */
public final class GrowthRegistryDefaults {

    private static final Gson SNAKE_CASE_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Identifier.class, new IdentifierTypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    // Animation presets
    public static final GrowthGlowProfile.AnimationOverride MAGMA_ANIMATION = new GrowthGlowProfile.AnimationOverride(
            8.0F, Boolean.TRUE, List.of(0, 1, 2), null);

    public static final GrowthGlowProfile.AnimationOverride LAVA_STILL_ANIMATION = new GrowthGlowProfile.AnimationOverride(
            2.0F, null,
            List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
            null);

    public static final GrowthGlowProfile.AnimationOverride LAVA_FLOW_ANIMATION = new GrowthGlowProfile.AnimationOverride(
            3.0F, null, null, null);

    private GrowthRegistryDefaults() {}

    /**
     * Ensures all default profile directories exist and writes default JSON files if missing.
     */
    public static void ensureDefaults(Path glowDir, Path particleDir, Path forceDir, Path fieldDir,
            Path fuseDir, Path explosionDir, Path opacityDir, Path spinDir, Path wobbleDir,
            Path growthDir, Path definitionDir) {
        try {
            Files.createDirectories(glowDir);
            Files.createDirectories(particleDir);
            Files.createDirectories(forceDir);
            Files.createDirectories(fieldDir);
            Files.createDirectories(fuseDir);
            Files.createDirectories(explosionDir);
            Files.createDirectories(opacityDir);
            Files.createDirectories(spinDir);
            Files.createDirectories(wobbleDir);
            Files.createDirectories(growthDir);
            Files.createDirectories(definitionDir);
        } catch (IOException ex) {
            Logging.GROWTH.error("[GrowthRegistry] Failed creating directories", ex);
        }

        writeGlowDefaults(glowDir);
        writeParticleDefaults(particleDir);
        writeForceDefaults(forceDir);
        writeFieldDefaults(fieldDir);
        writeFuseDefaults(fuseDir);
        writeExplosionDefaults(explosionDir);
        writeOpacityDefaults(opacityDir);
        writeSpinDefaults(spinDir);
        writeWobbleDefaults(wobbleDir);
        writeGrowthDefaults(growthDir);
        writeDefinitionDefaults(definitionDir);
    }

    // ========================================================================
    // Glow Profiles
    // ========================================================================

    private static void writeGlowDefaults(Path dir) {
        writeIfMissing(dir.resolve("prototype.json"), GrowthGlowProfile.defaults());
        writeIfMissing(dir.resolve("none.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "none_glow"),
                Identifier.of("the-virus-block", "textures/block/singularity_block.png"),
                Identifier.of("the-virus-block", "textures/block/singularity_block.png"),
                false, false, "#FFFFFF", "#FFFFFF", null, null, 0));
        writeIfMissing(dir.resolve("magma.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "magma"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_secondary.png"),
                false, false, "#FF6E00", "#FFB347", MAGMA_ANIMATION, MAGMA_ANIMATION, 15));
        writeIfMissing(dir.resolve("magma_primary_mesh.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "magma_primary_mesh"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_secondary.png"),
                true, false, "#FF6E00", "#FFB347", MAGMA_ANIMATION, MAGMA_ANIMATION, 15));
        writeIfMissing(dir.resolve("magma_secondary_mesh.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "magma_secondary_mesh"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_secondary.png"),
                false, true, "#FF6E00", "#FFB347", MAGMA_ANIMATION, MAGMA_ANIMATION, 15));
        writeIfMissing(dir.resolve("magma_dual_mesh.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "magma_dual_mesh"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_magma_secondary.png"),
                true, true, "#FF6E00", "#FFB347", MAGMA_ANIMATION, MAGMA_ANIMATION, 15));
        writeIfMissing(dir.resolve("lava.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "lava"),
                Identifier.of("the-virus-block", "textures/block/glow_lava_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_lava_secondary.png"),
                false, false, "#FF3E00", "#FF995A", LAVA_STILL_ANIMATION, LAVA_FLOW_ANIMATION, 15));
        writeIfMissing(dir.resolve("lava_primary_mesh.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "lava_primary_mesh"),
                Identifier.of("the-virus-block", "textures/block/glow_lava_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_lava_secondary.png"),
                true, false, "#FF3E00", "#FF995A", LAVA_STILL_ANIMATION, LAVA_FLOW_ANIMATION, 15));
        writeIfMissing(dir.resolve("beam.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "beam"),
                Identifier.of("the-virus-block", "textures/block/glow_beam_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_beam_secondary.png"),
                false, false, "#38D3FF", "#A5F1FF", null, null, 12));
        writeIfMissing(dir.resolve("beam_primary_mesh.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "beam_primary_mesh"),
                Identifier.of("the-virus-block", "textures/block/glow_beam_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_beam_secondary.png"),
                true, false, "#38D3FF", "#A5F1FF", null, null, 12));
        writeIfMissing(dir.resolve("glowstone.json"), new GrowthGlowProfile(
                Identifier.of("the-virus-block", "glowstone"),
                Identifier.of("the-virus-block", "textures/block/glow_glowstone_primary.png"),
                Identifier.of("the-virus-block", "textures/block/glow_glowstone_secondary.png"),
                false, false, "#FFEFA1", "#FFD764", null, null, 14));
    }

    // ========================================================================
    // Particle Profiles
    // ========================================================================

    private static void writeParticleDefaults(Path dir) {
        writeIfMissing(dir.resolve("default.json"), GrowthParticleProfile.defaults());
        writeIfMissing(dir.resolve("none.json"), new GrowthParticleProfile(
                Identifier.of("the-virus-block", "none_particle"),
                null, 0, 0.0D, null, 20, 20, GrowthParticleProfile.Shape.SPHERE,
                0.5D, 0.5D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false));
    }

    // ========================================================================
    // Force Profiles
    // ========================================================================

    private static void writeForceDefaults(Path dir) {
        writeIfMissing(dir.resolve("default_pull.json"), GrowthForceProfile.defaultsPull());
        writeIfMissing(dir.resolve("default_push.json"), GrowthForceProfile.defaultsPush());
        writeIfMissing(dir.resolve("vortex_pull.json"), GrowthForceProfile.vortexPull());
        writeIfMissing(dir.resolve("shock_push.json"), GrowthForceProfile.shockPush());
        writeIfMissing(dir.resolve("ring_hold.json"), new GrowthForceProfile(
                Identifier.of("the-virus-block", "ring_hold"),
                true, 12, 9.5D, 0.0D, 0.0D, 1.0D, 0.0D, 1.0D, false, 0.0D, 0xFFFFFFFF,
                Identifier.of("minecraft", "enchant"), 0, 0.0F,
                Identifier.of("minecraft", "entity.enderman.teleport"),
                0.0D, 10, GrowthForceProfile.RingBehavior.KEEP_ON_RING,
                1, 0.0D, 1.1D, 1.2D,
                List.of(Identifier.of("the-virus-block", "ring_default"))));
        writeIfMissing(dir.resolve("none_pull.json"), new GrowthForceProfile(
                Identifier.of("the-virus-block", "none_pull"),
                false, 20, 0.5D, 0.0D, 0.0D, 1.0D, 0.0D, 1.0D, false, 0.0D, 0x000000,
                null, 0, 0.0F, null, 0.0D, 20, GrowthForceProfile.RingBehavior.NONE,
                0, 0.0D, 0.0D, 0.0D, List.of()));
        writeIfMissing(dir.resolve("none_push.json"), new GrowthForceProfile(
                Identifier.of("the-virus-block", "none_push"),
                false, 20, 0.5D, 0.0D, 0.0D, 1.0D, 0.0D, 1.0D, false, 0.0D, 0x000000,
                null, 0, 0.0F, null, 0.0D, 20, GrowthForceProfile.RingBehavior.NONE,
                0, 0.0D, 0.0D, 0.0D, List.of()));
    }

    // ========================================================================
    // Field Profiles
    // ========================================================================

    private static void writeFieldDefaults(Path dir) {
        writeIfMissing(dir.resolve("none.json"), GrowthFieldProfile.none());
        writeIfMissing(dir.resolve("default.json"), GrowthFieldProfile.standardShell());
        writeIfMissing(dir.resolve("ring_default.json"), new GrowthFieldProfile(
                Identifier.of("the-virus-block", "ring_default"),
                "ring",
                Identifier.of("the-virus-block", "textures/misc/singularity_sphere_1.png"),
                0.65F, 1.2F, 1.0F, "#FFFFFFFF"));
    }

    // ========================================================================
    // Fuse Profiles
    // ========================================================================

    private static void writeFuseDefaults(Path dir) {
        writeIfMissing(dir.resolve("default.json"), GrowthFuseProfile.defaults());
        writeIfMissing(dir.resolve("none.json"), new GrowthFuseProfile(
                Identifier.of("the-virus-block", "none_fuse"),
                GrowthFuseProfile.Trigger.ITEM_USE, 1.0D, true, false,
                List.of(Identifier.of("the-virus-block", "none_item")),
                400, 20, 8, -1.0D, "#00000000", "#00000000", null, null));
    }

    // ========================================================================
    // Explosion Profiles
    // ========================================================================

    private static void writeExplosionDefaults(Path dir) {
        writeIfMissing(dir.resolve("default.json"), GrowthExplosionProfile.defaults());
    }

    // ========================================================================
    // Opacity Profiles
    // ========================================================================

    private static void writeOpacityDefaults(Path dir) {
        writeIfMissing(dir.resolve("default.json"), GrowthOpacityProfile.defaults());
        writeIfMissing(dir.resolve("soft_pulse.json"), new GrowthOpacityProfile(
                Identifier.of("the-virus-block", "soft_pulse"),
                new GrowthOpacityProfile.Layer(0.9F, 0.05F, 0.05F),
                new GrowthOpacityProfile.Layer(0.7F, 0.08F, 0.1F)));
        writeIfMissing(dir.resolve("faint.json"), new GrowthOpacityProfile(
                Identifier.of("the-virus-block", "faint"),
                new GrowthOpacityProfile.Layer(0.45F, 0.0F, 0.0F),
                new GrowthOpacityProfile.Layer(0.35F, 0.0F, 0.0F)));
        writeIfMissing(dir.resolve("ghostly.json"), new GrowthOpacityProfile(
                Identifier.of("the-virus-block", "ghostly"),
                new GrowthOpacityProfile.Layer(0.25F, 0.3F, 0.1F),
                new GrowthOpacityProfile.Layer(0.15F, 0.35F, 0.1F)));
    }

    // ========================================================================
    // Spin Profiles
    // ========================================================================

    private static void writeSpinDefaults(Path dir) {
        writeIfMissing(dir.resolve("default.json"), GrowthSpinProfile.defaults());
        writeIfMissing(dir.resolve("half_spin.json"), new GrowthSpinProfile(
                Identifier.of("the-virus-block", "half_spin"),
                new GrowthSpinProfile.Layer(0.5F, 1.0F, 0.0F, 1.0F, 0.0F),
                new GrowthSpinProfile.Layer(0.25F, -1.0F, 0.0F, 1.0F, 0.0F)));
        writeIfMissing(dir.resolve("full_spin.json"), new GrowthSpinProfile(
                Identifier.of("the-virus-block", "full_spin"),
                new GrowthSpinProfile.Layer(1.0F, 1.0F, 0.0F, 1.0F, 0.0F),
                new GrowthSpinProfile.Layer(2.0F, -1.0F, 0.0F, 1.0F, 0.0F)));
    }

    // ========================================================================
    // Wobble Profiles
    // ========================================================================

    private static void writeWobbleDefaults(Path dir) {
        writeIfMissing(dir.resolve("none.json"), GrowthWobbleProfile.none());
        writeIfMissing(dir.resolve("standard.json"), GrowthWobbleProfile.standard());
    }

    // ========================================================================
    // Growth Profiles
    // ========================================================================

    private static void writeGrowthDefaults(Path dir) {
        writeIfMissing(dir.resolve("default.json"), GrowthProfile.defaults());
        writeIfMissing(dir.resolve("slow.json"), new GrowthProfile(
                Identifier.of("the-virus-block", "slow_growth"),
                true, 10, 0.8D, 0.3D, 1.0D, 0.2D, 1.2D));
        writeIfMissing(dir.resolve("fast.json"), new GrowthProfile(
                Identifier.of("the-virus-block", "fast_growth"),
                true, 2, 1.2D, 0.4D, 1.2D, 0.3D, 1.5D));
        writeIfMissing(dir.resolve("small-no-growth.json"), new GrowthProfile(
                Identifier.of("the-virus-block", "small_no_growth"),
                true, 5, 1.0D, 0.1D, 0.1D, 0.05D, 0.2D));
        writeIfMissing(dir.resolve("normail-no-growth-static.json"), new GrowthProfile(
                Identifier.of("the-virus-block", "normal_static_growth"),
                true, 1, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D));
        writeIfMissing(dir.resolve("supersized.json"), new GrowthProfile(
                Identifier.of("the-virus-block", "supersized_growth"),
                true, 2, 1.0D, 0.5D, 4.0D, 0.4D, 5.0D));
    }

    // ========================================================================
    // Block Definitions
    // ========================================================================

    private static void writeDefinitionDefaults(Path dir) {
        GrowthBlockDefinition base = GrowthBlockDefinition.defaults();
        writeIfMissing(dir.resolve("magma.json"), base);
        writeIfMissing(dir.resolve("magma_primary_mesh.json"),
                withGlow(base, Identifier.of("the-virus-block", "magma_primary_mesh"),
                        Identifier.of("the-virus-block", "magma_primary_mesh")));
        writeIfMissing(dir.resolve("magma_secondary_mesh.json"),
                withGlow(base, Identifier.of("the-virus-block", "magma_secondary_mesh"),
                        Identifier.of("the-virus-block", "magma_secondary_mesh")));
        writeIfMissing(dir.resolve("magma_dual_mesh.json"),
                withGlow(base, Identifier.of("the-virus-block", "magma_dual_mesh"),
                        Identifier.of("the-virus-block", "magma_dual_mesh")));
        writeIfMissing(dir.resolve("magma_spin.json"),
                withProfiles(base, Identifier.of("the-virus-block", "magma_spin"),
                        Identifier.of("the-virus-block", "magma_primary_mesh"),
                        Identifier.of("the-virus-block", "full_spin"),
                        base.opacityProfileId(), base.wobbleProfileId()));
        writeIfMissing(dir.resolve("magma_opacity.json"),
                withProfiles(base, Identifier.of("the-virus-block", "magma_opacity"),
                        Identifier.of("the-virus-block", "magma"),
                        base.spinProfileId(),
                        Identifier.of("the-virus-block", "soft_pulse"),
                        base.wobbleProfileId()));
        writeIfMissing(dir.resolve("magma_faint.json"),
                withProfiles(base, Identifier.of("the-virus-block", "magma_faint"),
                        Identifier.of("the-virus-block", "magma"),
                        base.spinProfileId(),
                        Identifier.of("the-virus-block", "faint"),
                        base.wobbleProfileId()));
        writeIfMissing(dir.resolve("magma_ghostly.json"),
                withProfiles(base, Identifier.of("the-virus-block", "magma_ghostly"),
                        Identifier.of("the-virus-block", "magma_secondary_mesh"),
                        base.spinProfileId(),
                        Identifier.of("the-virus-block", "ghostly"),
                        base.wobbleProfileId()));
        writeIfMissing(dir.resolve("magma_wobble.json"),
                withProfiles(base, Identifier.of("the-virus-block", "magma_wobble"),
                        base.glowProfileId(), base.spinProfileId(), base.opacityProfileId(),
                        Identifier.of("the-virus-block", "standard_wobble")));
        writeIfMissing(dir.resolve("magma_small_static.json"),
                withGrowth(base, Identifier.of("the-virus-block", "magma_small_static"),
                        Identifier.of("the-virus-block", "small_no_growth")));
        writeIfMissing(dir.resolve("magma_supersized.json"),
                withGrowth(base, Identifier.of("the-virus-block", "magma_supersized"),
                        Identifier.of("the-virus-block", "supersized_growth")));
        writeIfMissing(dir.resolve("magma_supersized_destruction.json"),
                withVariant(base, Identifier.of("the-virus-block", "magma_supersized_destruction"),
                        Identifier.of("the-virus-block", "supersized_growth"),
                        Identifier.of("the-virus-block", "magma"),
                        Boolean.TRUE, Boolean.TRUE));
        writeIfMissing(dir.resolve("magma_supersized_no_collision.json"),
                withVariant(base, Identifier.of("the-virus-block", "magma_supersized_no_collision"),
                        Identifier.of("the-virus-block", "supersized_growth"),
                        Identifier.of("the-virus-block", "magma"),
                        Boolean.FALSE, Boolean.FALSE));
        writeIfMissing(dir.resolve("lava_block.json"),
                withGlow(base, Identifier.of("the-virus-block", "lava-block"),
                        Identifier.of("the-virus-block", "lava")));
        writeIfMissing(dir.resolve("lava_mesh_block.json"),
                withGlow(base, Identifier.of("the-virus-block", "lava_mesh_block"),
                        Identifier.of("the-virus-block", "lava_primary_mesh")));
        writeIfMissing(dir.resolve("beam_block.json"),
                withGlow(base, Identifier.of("the-virus-block", "beam-block"),
                        Identifier.of("the-virus-block", "beam")));
        writeIfMissing(dir.resolve("beam_mesh_block.json"),
                withGlow(base, Identifier.of("the-virus-block", "beam_mesh_block"),
                        Identifier.of("the-virus-block", "beam_primary_mesh")));
        writeIfMissing(dir.resolve("prototype.json"),
                withGlow(base, Identifier.of("the-virus-block", "prototype"),
                        base.glowProfileId()));
    }

    // ========================================================================
    // Definition Helpers
    // ========================================================================

    public static GrowthBlockDefinition withGlow(GrowthBlockDefinition base, Identifier id, Identifier glowProfileId) {
        return new GrowthBlockDefinition(id,
                base.growthEnabled(), base.rateTicks(), base.rateScale(),
                base.startScale(), base.targetScale(), base.minScale(), base.maxScale(),
                base.hasCollision(), base.doesDestruction(), base.hasFuse(), base.touchDamage(),
                base.growthProfileId(), glowProfileId, base.particleProfileId(), base.fieldProfileId(),
                base.pullProfileId(), base.pushProfileId(), base.fuseProfileId(), base.explosionProfileId(),
                base.opacityProfileId(), base.spinProfileId(), base.wobbleProfileId());
    }

    public static GrowthBlockDefinition withProfiles(GrowthBlockDefinition base, Identifier id,
            Identifier glowProfileId, Identifier spinProfileId, Identifier opacityProfileId, Identifier wobbleProfileId) {
        return new GrowthBlockDefinition(id,
                base.growthEnabled(), base.rateTicks(), base.rateScale(),
                base.startScale(), base.targetScale(), base.minScale(), base.maxScale(),
                base.hasCollision(), base.doesDestruction(), base.hasFuse(), base.touchDamage(),
                base.growthProfileId(), glowProfileId, base.particleProfileId(), base.fieldProfileId(),
                base.pullProfileId(), base.pushProfileId(), base.fuseProfileId(), base.explosionProfileId(),
                opacityProfileId, spinProfileId, wobbleProfileId);
    }

    public static GrowthBlockDefinition withGrowth(GrowthBlockDefinition base, Identifier id, Identifier growthProfileId) {
        return new GrowthBlockDefinition(id,
                base.growthEnabled(), base.rateTicks(), base.rateScale(),
                base.startScale(), base.targetScale(), base.minScale(), base.maxScale(),
                base.hasCollision(), base.doesDestruction(), base.hasFuse(), base.touchDamage(),
                growthProfileId, base.glowProfileId(), base.particleProfileId(), base.fieldProfileId(),
                base.pullProfileId(), base.pushProfileId(), base.fuseProfileId(), base.explosionProfileId(),
                base.opacityProfileId(), base.spinProfileId(), base.wobbleProfileId());
    }

    public static GrowthBlockDefinition withVariant(GrowthBlockDefinition base, Identifier id,
            Identifier growthProfileId, Identifier glowProfileId, Boolean hasCollision, Boolean doesDestruction) {
        return new GrowthBlockDefinition(id,
                base.growthEnabled(), base.rateTicks(), base.rateScale(),
                base.startScale(), base.targetScale(), base.minScale(), base.maxScale(),
                hasCollision != null ? hasCollision : base.hasCollision(),
                doesDestruction != null ? doesDestruction : base.doesDestruction(),
                base.hasFuse(), base.touchDamage(),
                growthProfileId != null ? growthProfileId : base.growthProfileId(),
                glowProfileId != null ? glowProfileId : base.glowProfileId(),
                base.particleProfileId(), base.fieldProfileId(),
                base.pullProfileId(), base.pushProfileId(), base.fuseProfileId(), base.explosionProfileId(),
                base.opacityProfileId(), base.spinProfileId(), base.wobbleProfileId());
    }

    // ========================================================================
    // File Writing
    // ========================================================================

    public static void writeIfMissing(Path path, Object data) {
        if (Files.exists(path)) {
            return;
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            SNAKE_CASE_GSON.toJson(data, writer);
        } catch (IOException ex) {
            Logging.GROWTH.error("[GrowthRegistry] Failed to write {}", path.getFileName(), ex);
        }
    }

    /** Serializes Identifier as a string "namespace:path" instead of an object */
    private static class IdentifierTypeAdapter extends com.google.gson.TypeAdapter<Identifier> {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, Identifier value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public Identifier read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return Identifier.tryParse(in.nextString());
        }
    }
}
