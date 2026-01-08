package net.cyberpunk042.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.cyberpunk042.growth.profile.GrowthForceProfile;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.network.GrowthRingFieldPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Handles force field application (push/pull) for growth blocks.
 */
public class GrowthForceHandler {

    public record RingBand(double radius, double inner, double outer, Identifier fieldProfileId) {}
    public record RingForceDecision(double directionSign, double strengthMultiplier) {}

    private GrowthForceHandler() {}

    public static boolean isForceActive(@Nullable GrowthForceProfile profile) {
        return profile != null && profile.enabled() && profile.clampedStrength() > 0.0D;
    }

    public static void applyForce(
            ServerWorld world,
            BlockPos pos,
            GrowthBlockDefinition definition,
            GrowthForceProfile profile,
            boolean pulling,
            double growthProgress,
            Object2LongMap<UUID> forceDamageCooldowns,
            GrowthEventPublisher eventPublisher
    ) {
        if (growthProgress < profile.clampedStartProgress() || growthProgress > profile.clampedEndProgress()) {
            return;
        }

        double baseRadius = Math.max(0.5D, profile.clampedRadius());
        Box box = new Box(pos).expand(baseRadius);
        Vec3d center = Vec3d.ofCenter(pos);
        List<RingBand> ringBands = profile.hasRingConfig() ? buildRingBands(profile) : Collections.emptyList();
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity.isAlive() && !entity.isSpectator());
        if (!targets.isEmpty()) {
            for (LivingEntity living : targets) {
                Vec3d offset = living.getPos().subtract(center);
                double distance = Math.max(0.001D, offset.length());
                if (distance > baseRadius) {
                    continue;
                }
                double normalized = 1.0D - Math.min(1.0D, distance / baseRadius);
                double impulse = computeImpulse(profile, normalized, distance, baseRadius);
                if (impulse <= 0.0D) {
                    continue;
                }
                Vec3d radial = offset.normalize();
                double directionSign = pulling ? -1.0D : 1.0D;
                double impulseScale = 1.0D;
                if (!ringBands.isEmpty()) {
                    RingBand band = findNearestRingBand(ringBands, distance);
                    RingForceDecision decision = band != null ? evaluateRingDecision(profile, band, distance) : null;
                    if (decision == null) {
                        continue;
                    }
                    directionSign = decision.directionSign();
                    impulseScale = decision.strengthMultiplier();
                }
                Vec3d direction = radial.multiply(directionSign);
                double verticalBoost = profile.verticalBoost() * Math.pow(normalized, 0.65D);
                Vec3d push = direction.multiply(impulse * impulseScale);
                living.addVelocity(push.x, verticalBoost, push.z);
                living.velocityModified = true;
                tryApplyForceDamage(world, living, profile, forceDamageCooldowns);
                if (profile.guardianBeams()) {
                    eventPublisher.postBeamEvent(world, pos, living, profile, pulling);
                }
            }
        }
        if (!ringBands.isEmpty()) {
            sendRingFieldVisuals(world, pos, ringBands, profile);
        }
        spawnForceParticles(world, pos, profile);
        playSound(world, pos, profile.soundId(), 0.9F, pulling ? 0.8F : 1.1F);
        eventPublisher.postForceEvent(world, pos, profile, pulling, baseRadius, profile.clampedStrength());
    }

    public static void tryApplyForceDamage(ServerWorld world, LivingEntity living, GrowthForceProfile profile, Object2LongMap<UUID> forceDamageCooldowns) {
        double damage = profile.clampedImpactDamage();
        if (damage <= 0.0D) {
            return;
        }
        int cooldown = profile.sanitizedImpactCooldown();
        if (cooldown <= 0) {
            cooldown = 1;
        }
        UUID id = living.getUuid();
        long now = world.getTime();
        long nextAllowed = forceDamageCooldowns.getLong(id);
        if (nextAllowed > now) {
            return;
        }
        living.damage(world, world.getDamageSources().magic(), (float) damage);
        forceDamageCooldowns.put(id, now + cooldown);
    }

    public static double computeImpulse(GrowthForceProfile profile, double normalized, double distance, double radius) {
        double strength = profile.clampedStrength();
        double falloff = Math.pow(normalized, profile.clampedFalloff());
        double edgeFactor = 1.0D;
        double edgeFalloff = profile.clampedEdgeFalloff();
        if (edgeFalloff > 0.0D) {
            double edgeDistance = Math.max(0.0D, (radius - distance) / (radius * edgeFalloff));
            edgeFactor = Math.min(1.0D, edgeDistance);
        }
        return strength * falloff * edgeFactor;
    }

    public static List<RingBand> buildRingBands(GrowthForceProfile profile) {
        if (!profile.hasRingConfig()) {
            return Collections.emptyList();
        }
        int count = profile.sanitizedRingCount();
        if (count <= 0) {
            return Collections.emptyList();
        }
        double base = Math.max(0.25D, profile.baseRingRadius());
        double spacing = Math.max(0.0D, profile.sanitizedRingSpacing());
        double width = Math.max(0.05D, profile.sanitizedRingWidth());
        List<Identifier> fields = profile.ringFieldProfiles();
        List<RingBand> bands = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double radius = base + (spacing * i);
            double inner = Math.max(0.0D, radius - width * 0.5D);
            double outer = radius + width * 0.5D;
            Identifier field = i < fields.size() ? fields.get(i) : null;
            bands.add(new RingBand(radius, inner, outer, field));
        }
        return bands;
    }

    @Nullable
    public static RingBand findNearestRingBand(List<RingBand> bands, double distance) {
        if (bands.isEmpty()) {
            return null;
        }
        RingBand nearest = null;
        double best = Double.MAX_VALUE;
        for (RingBand band : bands) {
            double diff = Math.abs(distance - band.radius());
            if (diff < best) {
                best = diff;
                nearest = band;
            }
        }
        return nearest;
    }

    @Nullable
    public static RingForceDecision evaluateRingDecision(GrowthForceProfile profile, RingBand band, double distance) {
        if (band == null) {
            return null;
        }
        double inner = band.inner();
        double outer = band.outer();
        double strength = profile.clampedRingStrength();
        return switch (profile.ringBehavior()) {
            case KEEP_ON_RING -> {
                if (distance >= inner && distance <= outer) {
                    yield null;
                }
                double delta = distance - band.radius();
                double width = Math.max(outer - inner, 0.001D);
                double normalized = MathHelper.clamp(Math.abs(delta) / width, 0.25D, 1.0D);
                yield new RingForceDecision(delta < 0.0D ? 1.0D : -1.0D, strength * normalized);
            }
            case KEEP_INSIDE -> {
                if (distance < inner) {
                    yield new RingForceDecision(1.0D, strength);
                }
                if (distance > outer) {
                    yield new RingForceDecision(-1.0D, strength);
                }
                yield null;
            }
            case KEEP_OUTSIDE -> {
                if (distance <= outer) {
                    yield new RingForceDecision(1.0D, strength);
                }
                yield null;
            }
            case NONE -> null;
            default -> null;
        };
    }

    public static void sendRingFieldVisuals(ServerWorld world, BlockPos pos, List<RingBand> bands, GrowthForceProfile profile) {
        if (bands.isEmpty()) {
            return;
        }
        List<GrowthRingFieldPayload.RingEntry> entries = new ArrayList<>();
        int duration = Math.max(10, profile.sanitizedInterval());
        for (RingBand band : bands) {
            if (band.fieldProfileId() == null) {
                continue;
            }
            entries.add(new GrowthRingFieldPayload.RingEntry(
                    band.fieldProfileId(),
                    (float) band.radius(),
                    (float) Math.max(0.05D, band.outer() - band.inner()),
                    duration));
        }
        if (entries.isEmpty()) {
            return;
        }
        GrowthRingFieldPayload payload = new GrowthRingFieldPayload(world.getRegistryKey(), pos.toImmutable(), entries);
        for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void spawnForceParticles(ServerWorld world, BlockPos pos, GrowthForceProfile profile) {
        ParticleEffect effect = resolveParticle(profile.particleId());
        if (effect == null || profile.particleCount() <= 0) {
            return;
        }
        world.spawnParticles(effect,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                profile.particleCount(),
                0.4D,
                0.4D,
                0.4D,
                profile.particleSpeed());
    }

    public static void playSound(ServerWorld world, BlockPos pos, Identifier soundId, float volume, float pitch) {
        if (soundId == null) {
            return;
        }
        SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
        if (sound == null) {
            return;
        }
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, volume, pitch);
    }

    @Nullable
    private static ParticleEffect resolveParticle(@Nullable Identifier id) {
        if (id == null) {
            return null;
        }
        if (!Registries.PARTICLE_TYPE.containsId(id)) {
            return null;
        }
        ParticleType<?> type = Registries.PARTICLE_TYPE.get(id);
        return type instanceof ParticleEffect effect ? effect : null;
    }
}
