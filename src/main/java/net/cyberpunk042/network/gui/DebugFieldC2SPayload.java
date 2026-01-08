package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G107: Client -> Server debug field update.
 * 
 * <p>Sent when the player modifies the DEBUG field in real-time.</p>
 */
public record DebugFieldC2SPayload(
    String fieldJson,
    boolean spawn,
    boolean despawn
) implements CustomPayload {
    
    public static final Id<DebugFieldC2SPayload> ID = new Id<>(GuiPacketIds.DEBUG_FIELD_C2S);
    
    public static final PacketCodec<RegistryByteBuf, DebugFieldC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.fieldJson);
            buf.writeBoolean(payload.spawn);
            buf.writeBoolean(payload.despawn);
        },
        buf -> new DebugFieldC2SPayload(
            buf.readString(),
            buf.readBoolean(),
            buf.readBoolean()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public static DebugFieldC2SPayload updateField(String json) {
        return new DebugFieldC2SPayload(json, false, false);
    }
    
    public static DebugFieldC2SPayload spawnField(String json) {
        return new DebugFieldC2SPayload(json, true, false);
    }
    
    public static DebugFieldC2SPayload despawnField() {
        return new DebugFieldC2SPayload("", false, true);
    }
}
