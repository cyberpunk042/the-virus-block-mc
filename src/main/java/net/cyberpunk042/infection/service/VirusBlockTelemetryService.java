package net.cyberpunk042.infection.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.network.VirusBlockTelemetryPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Sends virus block positions to ALL players on the server.
 * 
 * <p>This is independent of the helmet - all players receive virus block
 * data so shaders can render effects.</p>
 */
public final class VirusBlockTelemetryService {

    private static final int UPDATE_INTERVAL = 10;  // Every 10 ticks (0.5 seconds)
    private static final int MAX_POSITIONS = 32;    // Max positions to send per packet
    
    private final VirusWorldState host;
    private int tickCounter = 0;

    public VirusBlockTelemetryService(VirusWorldState host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    public void tick() {
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        ServerWorld world = host.world();
        List<BlockPos> sources = host.hasVirusSources() 
            ? new ArrayList<>(host.getVirusSources())
            : new ArrayList<>();
        
        // Send to ALL alive players on server (even empty packets to keep connection alive)
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isSpectator())) {
            sendPayload(player, sources);
        }
    }

    private void sendPayload(ServerPlayerEntity player, List<BlockPos> sources) {
        Vec3d playerPos = player.getEyePos();
        
        // Sort by distance to player
        sources.sort(Comparator.comparingDouble(pos -> 
            pos.toCenterPos().squaredDistanceTo(playerPos)));
        
        // Get closest distance
        float closestDist = sources.isEmpty() ? Float.MAX_VALUE : 
            (float) sources.get(0).toCenterPos().distanceTo(playerPos);
        
        // Take nearest positions up to max
        List<BlockPos> nearbyPositions = sources.subList(0, 
            Math.min(sources.size(), MAX_POSITIONS));
        
        VirusBlockTelemetryPayload payload = new VirusBlockTelemetryPayload(
            closestDist,
            sources.size(),
            nearbyPositions
        );
        
        ServerPlayNetworking.send(player, payload);
    }
}
