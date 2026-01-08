package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server -> Client shockwave trigger notification.
 * 
 * <p>Sent to all players in range when a shockwave is triggered.
 * Clients receive this and trigger their local ShockwavePostEffect.</p>
 */
public record ShockwaveTriggerS2CPayload(
    float worldX,              // Shockwave origin X
    float worldY,              // Shockwave origin Y
    float worldZ,              // Shockwave origin Z
    String configJson          // Shockwave visual config (from ShockwaveAdapter)
) implements CustomPayload {
    
    public static final Id<ShockwaveTriggerS2CPayload> ID = new Id<>(GuiPacketIds.SHOCKWAVE_TRIGGER_S2C);
    
    public static final PacketCodec<RegistryByteBuf, ShockwaveTriggerS2CPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeFloat(payload.worldX);
            buf.writeFloat(payload.worldY);
            buf.writeFloat(payload.worldZ);
            buf.writeString(payload.configJson);
        },
        buf -> new ShockwaveTriggerS2CPayload(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readString()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    /**
     * Creates a shockwave trigger broadcast.
     */
    public static ShockwaveTriggerS2CPayload create(float worldX, float worldY, float worldZ, String configJson) {
        return new ShockwaveTriggerS2CPayload(worldX, worldY, worldZ, configJson);
    }
}
