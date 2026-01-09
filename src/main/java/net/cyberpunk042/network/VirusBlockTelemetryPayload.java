package net.cyberpunk042.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Payload containing virus block positions for all players.
 * 
 * <p>Sent to ALL players on the server regardless of equipment.</p>
 */
public record VirusBlockTelemetryPayload(
    float closestDistance,
    int totalCount,
    List<BlockPos> nearbyPositions
) implements CustomPayload {

    public static final int MAX_POSITIONS = 32;
    
    public static final Id<VirusBlockTelemetryPayload> ID = new Id<>(
        Identifier.of("the-virus-block", "virus_block_telemetry")
    );

    public static final PacketCodec<RegistryByteBuf, VirusBlockTelemetryPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.FLOAT, VirusBlockTelemetryPayload::closestDistance,
            PacketCodecs.INTEGER, VirusBlockTelemetryPayload::totalCount,
            BlockPos.PACKET_CODEC.collect(PacketCodecs.toList(MAX_POSITIONS)), 
                VirusBlockTelemetryPayload::nearbyPositions,
            VirusBlockTelemetryPayload::new
        );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public static VirusBlockTelemetryPayload empty() {
        return new VirusBlockTelemetryPayload(Float.MAX_VALUE, 0, new ArrayList<>());
    }
}
