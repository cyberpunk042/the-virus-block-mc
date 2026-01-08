package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G105: Client -> Server profile save request.
 * 
 * <p>Used for two purposes:</p>
 * <ul>
 *   <li>Apply profile to player's shield (saveToServer=false)</li>
 *   <li>OP saving profile to server_profiles (saveToServer=true, requires OP)</li>
 * </ul>
 */
public record ProfileSaveC2SPayload(
    String profileName,
    String profileJson,
    boolean saveToServer
) implements CustomPayload {
    
    public static final Id<ProfileSaveC2SPayload> ID = new Id<>(GuiPacketIds.PROFILE_SAVE_C2S);
    
    public static final PacketCodec<RegistryByteBuf, ProfileSaveC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.profileName);
            buf.writeString(payload.profileJson);
            buf.writeBoolean(payload.saveToServer);
        },
        buf -> new ProfileSaveC2SPayload(
            buf.readString(),
            buf.readString(),
            buf.readBoolean()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    /**
     * Create payload to apply profile to player's shield.
     */
    public static ProfileSaveC2SPayload applyToShield(String name, String json) {
        return new ProfileSaveC2SPayload(name, json, false);
    }
    
    /**
     * Create payload for OP to save profile to server.
     */
    public static ProfileSaveC2SPayload saveToServer(String name, String json) {
        return new ProfileSaveC2SPayload(name, json, true);
    }
}
