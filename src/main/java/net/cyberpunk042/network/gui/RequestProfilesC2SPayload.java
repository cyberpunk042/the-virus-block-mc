package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G110: Client -> Server request for available server profiles.
 */
public record RequestProfilesC2SPayload() implements CustomPayload {
    
    public static final Id<RequestProfilesC2SPayload> ID = new Id<>(GuiPacketIds.REQUEST_PROFILES_C2S);
    
    public static final PacketCodec<RegistryByteBuf, RequestProfilesC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {},
        buf -> new RequestProfilesC2SPayload()
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
