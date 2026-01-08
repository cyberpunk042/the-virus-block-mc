package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G106: Client -> Server profile load request.
 */
public record ProfileLoadC2SPayload(
    String profileName
) implements CustomPayload {
    
    public static final Id<ProfileLoadC2SPayload> ID = new Id<>(GuiPacketIds.PROFILE_LOAD_C2S);
    
    public static final PacketCodec<RegistryByteBuf, ProfileLoadC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> buf.writeString(payload.profileName),
        buf -> new ProfileLoadC2SPayload(buf.readString())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
