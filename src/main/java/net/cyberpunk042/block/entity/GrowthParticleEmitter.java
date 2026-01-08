package net.cyberpunk042.block.entity;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.growth.profile.GrowthParticleProfile;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Handles ambient particle and sound emission for growth blocks.
 */
public class GrowthParticleEmitter {

    public static class EmitterState {
        public int ambientCooldown;
        public int ambientSoundCooldown;
    }

    private GrowthParticleEmitter() {}

    public static void emitAmbientParticles(
            ServerWorld world,
            BlockPos pos,
            GrowthParticleProfile profile,
            double currentScale,
            EmitterState state
    ) {
        if (profile == null) {
            return;
        }
        boolean spawnParticles = profile.sanitizedCount() > 0 && profile.particleId() != null;
        boolean playSound = profile.soundId() != null;
        if (!spawnParticles && !playSound) {
            state.ambientCooldown = 0;
            state.ambientSoundCooldown = 0;
            return;
        }

        if (spawnParticles) {
            if (state.ambientCooldown-- <= 0) {
                state.ambientCooldown = profile.sanitizedInterval();
                spawnConfiguredParticles(world, pos, profile, currentScale);
            }
        } else {
            state.ambientCooldown = 0;
        }

        if (playSound) {
            if (state.ambientSoundCooldown-- <= 0) {
                playSound(world, pos, profile.soundId(), 0.6F, 1.0F);
                state.ambientSoundCooldown = profile.sanitizedSoundInterval();
            }
        } else {
            state.ambientSoundCooldown = 0;
        }
    }

    public static void spawnConfiguredParticles(ServerWorld world, BlockPos pos, GrowthParticleProfile profile, double currentScale) {
        ParticleEffect effect = resolveParticle(profile.particleId());
        if (effect == null) {
            return;
        }
        int count = profile.sanitizedCount();
        if (count <= 0) {
            return;
        }
        double scaleFactor = profile.followScale() ? Math.max(0.1D, currentScale) : 1.0D;
        Vec3d base = Vec3d.ofCenter(pos)
                .add(profile.offsetX(), profile.offsetY(), profile.offsetZ());
        for (int i = 0; i < count; i++) {
            Vec3d offset = computeParticleOffset(world, profile, scaleFactor);
            Vec3d spawn = base.add(offset);
            world.spawnParticles(effect,
                    spawn.x,
                    spawn.y,
                    spawn.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    profile.sanitizedSpeed());
        }
    }

    public static Vec3d computeParticleOffset(ServerWorld world, GrowthParticleProfile profile, double scaleFactor) {
        double radius = profile.clampedRadius() * scaleFactor;
        double height = profile.clampedHeight() * scaleFactor;
        double jitterX = (world.random.nextDouble() * 2.0D - 1.0D) * profile.clampedJitterX();
        double jitterY = (world.random.nextDouble() * 2.0D - 1.0D) * profile.clampedJitterY();
        double jitterZ = (world.random.nextDouble() * 2.0D - 1.0D) * profile.clampedJitterZ();
        Vec3d base = switch (profile.shape()) {
            case SHELL -> randomDirection(world).multiply(radius);
            case RING -> {
                double angle = world.random.nextDouble() * Math.PI * 2.0D;
                double y = (world.random.nextDouble() - 0.5D) * height;
                yield new Vec3d(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            }
            case COLUMN -> {
                double angle = world.random.nextDouble() * Math.PI * 2.0D;
                double r = world.random.nextDouble() * radius;
                double y = (world.random.nextDouble() - 0.5D) * height;
                yield new Vec3d(Math.cos(angle) * r, y, Math.sin(angle) * r);
            }
            case SPHERE -> randomPointInSphere(world, radius);
            default -> randomPointInSphere(world, radius);
        };
        return base.add(jitterX, jitterY, jitterZ);
    }

    public static Vec3d randomPointInSphere(ServerWorld world, double radius) {
        for (int attempts = 0; attempts < 8; attempts++) {
            double x = (world.random.nextDouble() * 2.0D - 1.0D) * radius;
            double y = (world.random.nextDouble() * 2.0D - 1.0D) * radius;
            double z = (world.random.nextDouble() * 2.0D - 1.0D) * radius;
            if (x * x + y * y + z * z <= radius * radius) {
                return new Vec3d(x, y, z);
            }
        }
        return randomDirection(world).multiply(radius);
    }

    public static Vec3d randomDirection(ServerWorld world) {
        double theta = world.random.nextDouble() * Math.PI * 2.0D;
        double phi = Math.acos(2.0D * world.random.nextDouble() - 1.0D);
        double sinPhi = Math.sin(phi);
        return new Vec3d(
                sinPhi * Math.cos(theta),
                Math.cos(phi),
                sinPhi * Math.sin(theta));
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
    public static ParticleEffect resolveParticle(@Nullable Identifier id) {
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
