package net.cyberpunk042.network.gui;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client -> Server shockwave field spawn request.
 * 
 * <p>Sent when the player clicks "At Cursor" in the Shockwave sub-panel.
 * Spawns a field with shockwave effect at an absolute world position.</p>
 */
public record ShockwaveFieldSpawnC2SPayload(
    String fieldJson,          // Complete field definition JSON (includes shockwave config)
    float worldX,              // Absolute world X position
    float worldY,              // Absolute world Y position
    float worldZ,              // Absolute world Z position
    String sourcePrimitiveRef  // Reference to source primitive (e.g., "0.0" for layer 0, prim 0)
) implements CustomPayload {
    
    public static final Id<ShockwaveFieldSpawnC2SPayload> ID = new Id<>(GuiPacketIds.SHOCKWAVE_FIELD_SPAWN_C2S);
    
    public static final PacketCodec<RegistryByteBuf, ShockwaveFieldSpawnC2SPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.fieldJson);
            buf.writeFloat(payload.worldX);
            buf.writeFloat(payload.worldY);
            buf.writeFloat(payload.worldZ);
            buf.writeString(payload.sourcePrimitiveRef != null ? payload.sourcePrimitiveRef : "");
        },
        buf -> new ShockwaveFieldSpawnC2SPayload(
            buf.readString(),
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
     * Creates a shockwave spawn request at absolute world coordinates.
     * 
     * @param fieldJson Complete field definition JSON (includes shockwave config)
     * @param worldX Absolute world X
     * @param worldY Absolute world Y
     * @param worldZ Absolute world Z
     * @param sourcePrimitiveRef Reference to source primitive (e.g., "0.0")
     */
    public static ShockwaveFieldSpawnC2SPayload create(String fieldJson, 
            float worldX, float worldY, float worldZ, String sourcePrimitiveRef) {
        return new ShockwaveFieldSpawnC2SPayload(fieldJson, worldX, worldY, worldZ, sourcePrimitiveRef);
    }
}
