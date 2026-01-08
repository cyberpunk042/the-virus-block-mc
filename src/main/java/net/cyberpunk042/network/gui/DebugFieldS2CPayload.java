package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G108: Server -> Client debug field confirmation/update.
 */
public record DebugFieldS2CPayload(
    String fieldJson,
    boolean active,
    String status
) implements CustomPayload {
    
    public static final Id<DebugFieldS2CPayload> ID = new Id<>(GuiPacketIds.DEBUG_FIELD_S2C);
    
    public static final PacketCodec<RegistryByteBuf, DebugFieldS2CPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.fieldJson);
            buf.writeBoolean(payload.active);
            buf.writeString(payload.status);
        },
        buf -> new DebugFieldS2CPayload(
            buf.readString(),
            buf.readBoolean(),
            buf.readString()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
