package net.cyberpunk042.network.gui;

import net.cyberpunk042.log.Logging;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * G103: Server -> Client packet to open the Field Customizer GUI.
 * 
 * @param profileName Optional profile to load (empty = current)
 * @param debugUnlocked Whether debug mode is available
 */
public record GuiOpenS2CPayload(
    String profileName,
    boolean debugUnlocked
) implements CustomPayload {
    
    public static final Id<GuiOpenS2CPayload> ID = new Id<>(GuiPacketIds.GUI_OPEN_S2C);
    
    public static final PacketCodec<RegistryByteBuf, GuiOpenS2CPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.profileName);
            buf.writeBoolean(payload.debugUnlocked);
        },
        buf -> new GuiOpenS2CPayload(
            buf.readString(),
            buf.readBoolean()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public static void register() {
        Logging.GUI.topic("network").debug("Registered GuiOpenS2CPayload");
    }
}
