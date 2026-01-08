package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload sent from server to client when a field is spawned.
 * 
 * <p>Contains all data needed to create a client-side FieldInstance.
 */
public record FieldSpawnPayload(
        long id,
        String definitionId,
        double x, double y, double z,
        float scale,
        float phase,
        int lifetimeTicks
) implements CustomPayload {
    
    public static final Id<FieldSpawnPayload> ID = new Id<>(TheVirusBlock.FIELD_SPAWN_PACKET);
    public static final PacketCodec<PacketByteBuf, FieldSpawnPayload> CODEC =
            PacketCodec.of(FieldSpawnPayload::write, FieldSpawnPayload::new);
    
    public FieldSpawnPayload(PacketByteBuf buf) {
        this(
            buf.readVarLong(),
            buf.readString(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt()
        );
    }
    
    private void write(PacketByteBuf buf) {
        buf.writeVarLong(id);
        buf.writeString(definitionId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(scale);
        buf.writeFloat(phase);
        buf.writeVarInt(lifetimeTicks);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    /**
     * Convenience: get definition as Identifier.
     */
    public Identifier definitionIdentifier() {
        return Identifier.tryParse(definitionId);
    }
}
