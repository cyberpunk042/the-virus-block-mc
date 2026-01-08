package net.cyberpunk042.network.gui;

import net.cyberpunk042.log.Logging;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G104: Server -> Client profile synchronization.
 * 
 * <p>Sent when:</p>
 * <ul>
 *   <li>Profile save confirmed</li>
 *   <li>Profile loaded from server</li>
 *   <li>Initial sync on GUI open</li>
 * </ul>
 */
public record ProfileSyncS2CPayload(
    String profileName,
    String profileJson,
    boolean success,
    String message
) implements CustomPayload {
    
    public static final Id<ProfileSyncS2CPayload> ID = new Id<>(GuiPacketIds.PROFILE_SYNC_S2C);
    
    public static final PacketCodec<RegistryByteBuf, ProfileSyncS2CPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.profileName);
            buf.writeString(payload.profileJson);
            buf.writeBoolean(payload.success);
            buf.writeString(payload.message);
        },
        buf -> new ProfileSyncS2CPayload(
            buf.readString(),
            buf.readString(),
            buf.readBoolean(),
            buf.readString()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public static ProfileSyncS2CPayload success(String name, String json) {
        return new ProfileSyncS2CPayload(name, json, true, "");
    }
    
    public static ProfileSyncS2CPayload error(String message) {
        return new ProfileSyncS2CPayload("", "", false, message);
    }
}
