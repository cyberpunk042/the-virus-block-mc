package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Syncs a field definition from server to client.
 * Sent when player joins or when definitions are reloaded.
 * 
 * <p>Contains the full JSON so client can reconstruct the definition.
 */
public record FieldDefinitionSyncPayload(
        String definitionId,
        String definitionJson
) implements CustomPayload {
    
    public static final Id<FieldDefinitionSyncPayload> ID = new Id<>(TheVirusBlock.FIELD_DEFINITION_SYNC_PACKET);
    public static final PacketCodec<PacketByteBuf, FieldDefinitionSyncPayload> CODEC =
            PacketCodec.of(FieldDefinitionSyncPayload::write, FieldDefinitionSyncPayload::read);
    
    public static FieldDefinitionSyncPayload read(PacketByteBuf buf) {
        return new FieldDefinitionSyncPayload(
            buf.readString(),
            buf.readString()
        );
    }
    
    private void write(PacketByteBuf buf) {
        buf.writeString(definitionId);
        buf.writeString(definitionJson);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public Identifier definitionIdentifier() {
        return Identifier.tryParse(definitionId);
    }
}

