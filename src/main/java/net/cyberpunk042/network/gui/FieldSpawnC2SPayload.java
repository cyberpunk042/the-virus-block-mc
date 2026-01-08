package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client -> Server field spawn request.
 * 
 * <p>Sent when the player clicks "Spawn" in the Force sub-panel.</p>
 */
public record FieldSpawnC2SPayload(
    String configJson,
    int durationTicks,
    float offsetX,
    float offsetY,
    float offsetZ
) implements CustomPayload {
    
    public static final Id<FieldSpawnC2SPayload> ID = new Id<>(GuiPacketIds.FORCE_FIELD_SPAWN_C2S);
    
    public static final PacketCodec<RegistryByteBuf, FieldSpawnC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.configJson);
            buf.writeVarInt(payload.durationTicks);
            buf.writeFloat(payload.offsetX);
            buf.writeFloat(payload.offsetY);
            buf.writeFloat(payload.offsetZ);
        },
        buf -> new FieldSpawnC2SPayload(
            buf.readString(),
            buf.readVarInt(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    /**
     * Creates a spawn request with position offset from player.
     */
    public static FieldSpawnC2SPayload create(String configJson, int durationTicks, 
                                                    float offsetX, float offsetY, float offsetZ) {
        return new FieldSpawnC2SPayload(configJson, durationTicks, offsetX, offsetY, offsetZ);
    }
}
