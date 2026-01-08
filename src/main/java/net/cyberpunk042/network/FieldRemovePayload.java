package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload sent from server to client when a field is removed.
 */
public record FieldRemovePayload(long id) implements CustomPayload {
    
    public static final Identifier PACKET_ID = Identifier.of(TheVirusBlock.MOD_ID, "field_remove");
    public static final Id<FieldRemovePayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<PacketByteBuf, FieldRemovePayload> CODEC =
            PacketCodec.of(FieldRemovePayload::write, FieldRemovePayload::read);
    
    public static FieldRemovePayload read(PacketByteBuf buf) {
        return new FieldRemovePayload(buf.readVarLong());
    }
    
    private void write(PacketByteBuf buf) {
        buf.writeVarLong(id);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
