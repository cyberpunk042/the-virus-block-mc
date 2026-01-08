package net.cyberpunk042.growth;

import net.cyberpunk042.growth.profile.*;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.cyberpunk042.infection.service.ConfigService;
import net.minecraft.util.Identifier;

/**
 * Loads and provides access to all progressive growth profiles from {@code config/the-virus-block}.
 * <p>
 * Delegates to:
 * <ul>
 *   <li>{@link GrowthProfileParser} - JSON parsing</li>
 *   <li>{@link GrowthRegistryDefaults} - Default file creation</li>
 * </ul>
 */
public final class GrowthRegistry {

    private final Map<Identifier, GrowthGlowProfile> glowProfiles;
    private final Map<Identifier, GrowthParticleProfile> particleProfiles;
    private final Map<Identifier, GrowthForceProfile> forceProfiles;
    private final Map<Identifier, GrowthFieldProfile> fieldProfiles;
    private final Map<Identifier, GrowthFuseProfile> fuseProfiles;
    private final Map<Identifier, GrowthExplosionProfile> explosionProfiles;
    private final Map<Identifier, GrowthOpacityProfile> opacityProfiles;
    private final Map<Identifier, GrowthSpinProfile> spinProfiles;
    private final Map<Identifier, GrowthWobbleProfile> wobbleProfiles;
    private final Map<Identifier, GrowthProfile> growthProfiles;
    private final Map<Identifier, GrowthBlockDefinition> definitions;
    private final GrowthBlockDefinition defaultDefinition;

    private GrowthRegistry(Map<Identifier, GrowthGlowProfile> glowProfiles,
            Map<Identifier, GrowthParticleProfile> particleProfiles,
            Map<Identifier, GrowthForceProfile> forceProfiles,
            Map<Identifier, GrowthFieldProfile> fieldProfiles,
            Map<Identifier, GrowthFuseProfile> fuseProfiles,
            Map<Identifier, GrowthExplosionProfile> explosionProfiles,
            Map<Identifier, GrowthOpacityProfile> opacityProfiles,
            Map<Identifier, GrowthSpinProfile> spinProfiles,
            Map<Identifier, GrowthWobbleProfile> wobbleProfiles,
            Map<Identifier, GrowthProfile> growthProfiles,
            Map<Identifier, GrowthBlockDefinition> definitions,
            GrowthBlockDefinition defaultDefinition) {
        this.glowProfiles = glowProfiles;
        this.particleProfiles = particleProfiles;
        this.forceProfiles = forceProfiles;
        this.fieldProfiles = fieldProfiles;
        this.fuseProfiles = fuseProfiles;
        this.explosionProfiles = explosionProfiles;
        this.opacityProfiles = opacityProfiles;
        this.spinProfiles = spinProfiles;
        this.wobbleProfiles = wobbleProfiles;
        this.growthProfiles = growthProfiles;
        this.definitions = definitions;
        this.defaultDefinition = defaultDefinition;
    }

    public static GrowthRegistry load(ConfigService config) {
        Path glowDir = config.resolve("growth_block/glow_profiles");
        Path particleDir = config.resolve("growth_block/particle_profiles");
        Path forceDir = config.resolve("growth_block/force_profiles");
        Path fieldDir = config.resolve("growth_block/field_profiles");
        Path fuseDir = config.resolve("growth_block/fuse_profiles");
        Path explosionDir = config.resolve("growth_block/explosion_profiles");
        Path opacityDir = config.resolve("growth_block/opacity_profiles");
        Path spinDir = config.resolve("growth_block/spin_profiles");
        Path wobbleDir = config.resolve("growth_block/wobble_profiles");
        Path growthDir = config.resolve("growth_block/growth_profiles");
        Path definitionDir = config.resolve("growth_block/definitions");

        GrowthRegistryDefaults.ensureDefaults(glowDir, particleDir, forceDir, fieldDir,
                fuseDir, explosionDir, opacityDir, spinDir, wobbleDir, growthDir, definitionDir);

        Map<Identifier, GrowthGlowProfile> glow = GrowthProfileParser.loadGlowProfiles(glowDir);
        Map<Identifier, GrowthParticleProfile> particles = GrowthProfileParser.loadParticleProfiles(particleDir);
        Map<Identifier, GrowthForceProfile> forces = GrowthProfileParser.loadForceProfiles(forceDir);
        Map<Identifier, GrowthFieldProfile> fields = GrowthProfileParser.loadFieldProfiles(fieldDir);
        Map<Identifier, GrowthFuseProfile> fuse = GrowthProfileParser.loadFuseProfiles(fuseDir);
        Map<Identifier, GrowthExplosionProfile> explosions = GrowthProfileParser.loadExplosionProfiles(explosionDir);
        Map<Identifier, GrowthOpacityProfile> opacities = GrowthProfileParser.loadOpacityProfiles(opacityDir);
        Map<Identifier, GrowthSpinProfile> spins = GrowthProfileParser.loadSpinProfiles(spinDir);
        Map<Identifier, GrowthWobbleProfile> wobbles = GrowthProfileParser.loadWobbleProfiles(wobbleDir);
        Map<Identifier, GrowthProfile> growths = GrowthProfileParser.loadGrowthProfiles(growthDir);
        Map<Identifier, GrowthBlockDefinition> definitions = GrowthProfileParser.loadDefinitions(definitionDir, growths);

        // Ensure fallback profiles exist
        if (glow.isEmpty()) {
            GrowthGlowProfile defaults = GrowthGlowProfile.defaults();
            glow = Map.of(defaults.id(), defaults);
        }
        if (particles.isEmpty()) {
            GrowthParticleProfile defaults = GrowthParticleProfile.defaults();
            particles = Map.of(defaults.id(), defaults);
        }
        if (forces.isEmpty()) {
            GrowthForceProfile defaults = GrowthForceProfile.defaultsPull();
            forces = Map.of(defaults.id(), defaults);
        }
        if (fields.isEmpty()) {
            GrowthFieldProfile defaults = GrowthFieldProfile.defaults();
            fields = Map.of(defaults.id(), defaults);
        }
        if (fuse.isEmpty()) {
            GrowthFuseProfile defaults = GrowthFuseProfile.defaults();
            fuse = Map.of(defaults.id(), defaults);
        }
        if (explosions.isEmpty()) {
            GrowthExplosionProfile defaults = GrowthExplosionProfile.defaults();
            explosions = Map.of(defaults.id(), defaults);
        }
        if (opacities.isEmpty()) {
            GrowthOpacityProfile defaults = GrowthOpacityProfile.defaults();
            opacities = Map.of(defaults.id(), defaults);
        }
        if (spins.isEmpty()) {
            GrowthSpinProfile defaults = GrowthSpinProfile.defaults();
            spins = Map.of(defaults.id(), defaults);
        }
        if (wobbles.isEmpty()) {
            GrowthWobbleProfile defaults = GrowthWobbleProfile.none();
            wobbles = Map.of(defaults.id(), defaults);
        }
        if (growths.isEmpty()) {
            GrowthProfile defaults = GrowthProfile.defaults();
            growths = Map.of(defaults.id(), defaults);
        }
        if (definitions.isEmpty()) {
            GrowthBlockDefinition defaults = GrowthBlockDefinition.defaults();
            definitions = Map.of(defaults.id(), defaults);
        }

        GrowthBlockDefinition defaultDefinition = definitions.values()
                .stream()
                .findFirst()
                .orElse(GrowthBlockDefinition.defaults());
        return new GrowthRegistry(glow, particles, forces, fields, fuse, explosions,
                opacities, spins, wobbles, growths, definitions, defaultDefinition);
    }

    // ========================================================================
    // Profile Accessors
    // ========================================================================

    public GrowthGlowProfile glowProfile(Identifier id) {
        return glowProfiles.getOrDefault(id, GrowthGlowProfile.defaults());
    }

    public GrowthProfile growthProfile(Identifier id) {
        if (id == null) {
            return GrowthProfile.defaults();
        }
        return growthProfiles.getOrDefault(id, GrowthProfile.defaults());
    }

    public GrowthParticleProfile particleProfile(Identifier id) {
        if (id == null) {
            return GrowthParticleProfile.defaults();
        }
        return particleProfiles.getOrDefault(id, GrowthParticleProfile.defaults());
    }

    public GrowthForceProfile forceProfile(Identifier id) {
        return forceProfile(id, GrowthForceProfile.defaultsPull());
    }

    public GrowthForceProfile forceProfile(Identifier id, GrowthForceProfile fallback) {
        GrowthForceProfile resolvedFallback = fallback != null ? fallback : GrowthForceProfile.defaultsPull();
        if (id == null) {
            return resolvedFallback;
        }
        return forceProfiles.getOrDefault(id, resolvedFallback);
    }

    public GrowthFieldProfile fieldProfile(Identifier id) {
        if (id == null) {
            return GrowthFieldProfile.defaults();
        }
        return fieldProfiles.getOrDefault(id, GrowthFieldProfile.defaults());
    }

    public GrowthFuseProfile fuseProfile(Identifier id) {
        if (id == null) {
            return GrowthFuseProfile.defaults();
        }
        return fuseProfiles.getOrDefault(id, GrowthFuseProfile.defaults());
    }

    public GrowthExplosionProfile explosionProfile(Identifier id) {
        if (id == null) {
            return GrowthExplosionProfile.defaults();
        }
        return explosionProfiles.getOrDefault(id, GrowthExplosionProfile.defaults());
    }

    public GrowthOpacityProfile opacityProfile(Identifier id) {
        if (id == null) {
            return GrowthOpacityProfile.defaults();
        }
        return opacityProfiles.getOrDefault(id, GrowthOpacityProfile.defaults());
    }

    public GrowthSpinProfile spinProfile(Identifier id) {
        if (id == null) {
            return GrowthSpinProfile.defaults();
        }
        return spinProfiles.getOrDefault(id, GrowthSpinProfile.defaults());
    }

    public GrowthWobbleProfile wobbleProfile(Identifier id) {
        if (id == null) {
            return GrowthWobbleProfile.none();
        }
        return wobbleProfiles.getOrDefault(id, GrowthWobbleProfile.none());
    }

    public GrowthBlockDefinition definition(Identifier id) {
        if (id == null) {
            return defaultDefinition;
        }
        return definitions.getOrDefault(id, defaultDefinition);
    }

    public GrowthBlockDefinition defaultDefinition() {
        return defaultDefinition;
    }

    // ========================================================================
    // ID Listings
    // ========================================================================

    public List<Identifier> definitionIds() {
        return definitions.keySet().stream().sorted().toList();
    }

    public boolean hasDefinition(Identifier id) {
        return id != null && definitions.containsKey(id);
    }

    public Collection<Identifier> glowProfileIds() {
        return glowProfiles.keySet();
    }

    public Collection<Identifier> particleProfileIds() {
        return particleProfiles.keySet();
    }

    public Collection<Identifier> fieldProfileIds() {
        return fieldProfiles.keySet();
    }

    public Collection<Identifier> forceProfileIds() {
        return forceProfiles.keySet();
    }

    public Collection<Identifier> fuseProfileIds() {
        return fuseProfiles.keySet();
    }

    public Collection<Identifier> explosionProfileIds() {
        return explosionProfiles.keySet();
    }

    public Collection<Identifier> opacityProfileIds() {
        return opacityProfiles.keySet();
    }

    public Collection<Identifier> spinProfileIds() {
        return spinProfiles.keySet();
    }

    public Collection<Identifier> wobbleProfileIds() {
        return wobbleProfiles.keySet();
    }

    public Collection<Identifier> growthProfileIds() {
        return growthProfiles.keySet();
    }
}
