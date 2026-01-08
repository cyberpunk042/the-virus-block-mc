package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;
import java.util.ArrayList;

/**
 * G109: Server -> Client list of available server profiles.
 */
public record ServerProfilesS2CPayload(
    List<String> profileNames
) implements CustomPayload {
    
    public static final Id<ServerProfilesS2CPayload> ID = new Id<>(GuiPacketIds.SERVER_PROFILES_S2C);
    
    public static final PacketCodec<RegistryByteBuf, ServerProfilesS2CPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeVarInt(payload.profileNames.size());
            for (String name : payload.profileNames) {
                buf.writeString(name);
            }
        },
        buf -> {
            int size = buf.readVarInt();
            List<String> names = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                names.add(buf.readString());
            }
            return new ServerProfilesS2CPayload(names);
        }
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
