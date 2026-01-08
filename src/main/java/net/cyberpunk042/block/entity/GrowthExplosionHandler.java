package net.cyberpunk042.block.entity;

import java.util.List;

import net.cyberpunk042.growth.profile.GrowthExplosionProfile;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Handles explosion/burst sequences for growth blocks.
 */
public class GrowthExplosionHandler {

    // Burst state - held by the entity, but managed here
    public static class BurstState {
        public int burstExplosionsRemaining;
        public int burstDelayTicks;
        public int burstDelayInterval;
        public float burstRadius;
        public boolean burstCausesFire;
        public boolean burstBreaksBlocks;
        public double burstMaxDamage;
        public double burstDamageScaling = 1.0D;

        public boolean isActive() {
            return burstExplosionsRemaining > 0;
        }

        public void clear() {
            burstExplosionsRemaining = 0;
            burstDelayTicks = 0;
            burstDelayInterval = 0;
            burstRadius = 0.0F;
            burstCausesFire = false;
            burstBreaksBlocks = false;
            burstMaxDamage = 0.0D;
            burstDamageScaling = 1.0D;
        }
    }

    private GrowthExplosionHandler() {}

    public static void startBurstSequence(BurstState state, GrowthBlockDefinition definition, GrowthExplosionProfile explosion) {
        state.burstExplosionsRemaining = explosion.sanitizedAmount();
        state.burstDelayInterval = explosion.sanitizedAmountDelay();
        state.burstDelayTicks = 0;
        state.burstRadius = explosion.sanitizedRadius();
        state.burstCausesFire = explosion.causesFire();
        state.burstBreaksBlocks = definition.doesDestruction() && explosion.breaksBlocks();
        state.burstMaxDamage = explosion.sanitizedMaxDamage();
        state.burstDamageScaling = explosion.clampedDamageScaling();
    }

    /**
     * Ticks pending bursts.
     * @return true if the block was removed (no remaining charges)
     */
    public static boolean tickPendingBursts(
            ServerWorld world,
            BlockPos pos,
            BurstState state,
            int[] remainingChargesRef,
            Runnable onMarkDirty,
            Runnable onSync
    ) {
        if (!state.isActive()) {
            return false;
        }
        if (state.burstDelayTicks > 0) {
            state.burstDelayTicks--;
            return false;
        }
        executeBurstExplosion(world, pos, state);
        if (--state.burstExplosionsRemaining > 0) {
            state.burstDelayTicks = state.burstDelayInterval;
            return false;
        }
        return finalizeBurst(world, pos, state, remainingChargesRef, onMarkDirty, onSync);
    }

    public static void executeBurstExplosion(ServerWorld world, BlockPos pos, BurstState state) {
        World.ExplosionSourceType type = state.burstBreaksBlocks
                ? World.ExplosionSourceType.BLOCK
                : World.ExplosionSourceType.NONE;
        world.createExplosion(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                state.burstRadius,
                state.burstCausesFire,
                type);
        applyExplosionDamage(world, pos, state.burstRadius, state.burstMaxDamage, state.burstDamageScaling);
    }

    public static boolean finalizeBurst(
            ServerWorld world,
            BlockPos pos,
            BurstState state,
            int[] remainingChargesRef,
            Runnable onMarkDirty,
            Runnable onSync
    ) {
        remainingChargesRef[0] = Math.max(0, remainingChargesRef[0] - 1);
        state.clear();
        if (remainingChargesRef[0] <= 0) {
            world.removeBlock(pos, false);
            return true;
        }
        onMarkDirty.run();
        onSync.run();
        return false;
    }

    public static void applyExplosionDamage(ServerWorld world, BlockPos pos, double radius, double maxDamage, double scaling) {
        if (radius <= 0.0D || maxDamage <= 0.0D) {
            return;
        }
        Box box = new Box(pos).expand(radius);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive);
        if (targets.isEmpty()) {
            return;
        }
        Vec3d center = Vec3d.ofCenter(pos);
        for (LivingEntity living : targets) {
            double distance = living.getPos().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            double normalized = Math.max(0.0D, 1.0D - distance / radius);
            double falloff = Math.pow(normalized, scaling);
            double damageValue;
            if (Double.isInfinite(maxDamage)) {
                damageValue = falloff <= 0.0D ? 0.0D : Double.POSITIVE_INFINITY;
            } else {
                damageValue = maxDamage * falloff;
            }
            if (damageValue <= 0.0D) {
                continue;
            }
            float damageAmount = damageValue >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) damageValue;
            living.damage(world, world.getDamageSources().explosion(null, null), damageAmount);
        }
    }
}
