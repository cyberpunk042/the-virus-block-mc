package net.cyberpunk042.block.entity;

import net.cyberpunk042.growth.profile.GrowthForceProfile;
import net.cyberpunk042.growth.profile.GrowthFuseProfile;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.events.GrowthBeamEvent;
import net.cyberpunk042.infection.events.GrowthForceEvent;
import net.cyberpunk042.infection.events.GrowthForceEvent.ForceType;
import net.cyberpunk042.infection.events.GrowthFuseEvent;
import net.cyberpunk042.network.GrowthBeamPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Publishes growth-related events to the event bus and sends network payloads.
 */
public class GrowthEventPublisher {

    public GrowthEventPublisher() {}

    public void postForceEvent(ServerWorld world, BlockPos pos, GrowthForceProfile profile, boolean pulling, double radius, double strength) {
        ForceType type = pulling ? ForceType.PULL : ForceType.PUSH;
        VirusWorldState state = VirusWorldState.get(world);
        state.orchestrator().services().effectBus().post(new GrowthForceEvent(world, pos.toImmutable(), type, radius, strength, profile));
    }

    public void postBeamEvent(ServerWorld world, BlockPos pos, LivingEntity target, GrowthForceProfile profile, boolean pulling) {
        VirusWorldState state = VirusWorldState.get(world);
        Vec3d targetPos = target.getBoundingBox().getCenter();
        float[] color = profile.beamColorFloats();
        int duration = Math.max(10, profile.sanitizedInterval());
        state.orchestrator().services().effectBus().post(new GrowthBeamEvent(
                world,
                pos.toImmutable(),
                target.getId(),
                targetPos,
                pulling,
                color,
                duration));
        sendBeamPayload(world, pos, targetPos, target, pulling, color, duration);
    }

    public void postFuseEvent(ServerWorld world, BlockPos pos, GrowthFuseEvent.Stage stage, int ticksRemaining, GrowthFuseProfile profile) {
        VirusWorldState state = VirusWorldState.get(world);
        state.orchestrator().services().effectBus().post(new GrowthFuseEvent(world, pos.toImmutable(), stage, ticksRemaining, profile));
    }

    public void sendBeamPayload(ServerWorld world, BlockPos pos, Vec3d targetPos, LivingEntity target, boolean pulling, float[] color, int duration) {
        float red = color.length > 0 ? color[0] : 1.0F;
        float green = color.length > 1 ? color[1] : 0.5F;
        float blue = color.length > 2 ? color[2] : 0.5F;
        GrowthBeamPayload payload = new GrowthBeamPayload(
                world.getRegistryKey(),
                pos.toImmutable(),
                target.getId(),
                targetPos.x,
                targetPos.y,
                targetPos.z,
                pulling,
                red,
                green,
                blue,
                duration);
        for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
