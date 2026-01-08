package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload sent from server to client to update a field's position/state.
 * 
 * <p>Includes optional shuffle override for live editing.
 * Shuffle is sent as type+index so client can reconstruct DynamicQuadPattern.
 * 
 * <p>Also includes follow mode and prediction settings for personal shields.
 */
public record FieldUpdatePayload(
        long id,
        double x, double y, double z,
        float alpha,
        String shuffleType,  // "quad", "segment", "sector", "edge", "static:XXX", or empty
        int shuffleIndex,    // Index into shuffle permutations (-1 for static patterns)
        // Follow mode and prediction
        String followMode,   // "snap", "smooth", "glide"
        boolean predictionEnabled,
        int predictionLeadTicks,
        float predictionMaxDistance,
        float predictionLookAhead,
        float predictionVerticalBoost
) implements CustomPayload {
    
    public static final Identifier PACKET_ID = Identifier.of(TheVirusBlock.MOD_ID, "field_update");
    public static final Id<FieldUpdatePayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<PacketByteBuf, FieldUpdatePayload> CODEC =
            PacketCodec.of(FieldUpdatePayload::write, FieldUpdatePayload::read);
    
    public static FieldUpdatePayload read(PacketByteBuf buf) {
        return new FieldUpdatePayload(
            buf.readVarLong(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readFloat(),
            buf.readString(),
            buf.readVarInt(),
            // Follow mode and prediction
            buf.readString(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat()
        );
    }
    
    private void write(PacketByteBuf buf) {
        buf.writeVarLong(id);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(alpha);
        buf.writeString(shuffleType != null ? shuffleType : "");
        buf.writeVarInt(shuffleIndex);
        // Follow mode and prediction
        buf.writeString(followMode != null ? followMode : "snap");
        buf.writeBoolean(predictionEnabled);
        buf.writeVarInt(predictionLeadTicks);
        buf.writeFloat(predictionMaxDistance);
        buf.writeFloat(predictionLookAhead);
        buf.writeFloat(predictionVerticalBoost);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    /**
     * Creates an update without shuffle override.
     */
    public static FieldUpdatePayload position(long id, double x, double y, double z, float alpha) {
        return new FieldUpdatePayload(id, x, y, z, alpha, "", -1, "snap", false, 0, 0, 0, 0);
    }
    
    /**
     * Creates an update with shuffle override.
     * @param shuffleType "quad", "segment", "sector", or "edge"
     * @param shuffleIndex Index into the shuffle permutations
     */
    public static FieldUpdatePayload withShuffle(long id, double x, double y, double z, float alpha, 
                                                  String shuffleType, int shuffleIndex) {
        return new FieldUpdatePayload(id, x, y, z, alpha, shuffleType, shuffleIndex, "snap", false, 0, 0, 0, 0);
    }
    
    /**
     * Creates an update with all options.
     */
    public static FieldUpdatePayload full(long id, double x, double y, double z, float alpha,
                                          String shuffleType, int shuffleIndex,
                                          String followMode, boolean predictionEnabled,
                                          int predictionLeadTicks, float predictionMaxDistance,
                                          float predictionLookAhead, float predictionVerticalBoost) {
        return new FieldUpdatePayload(id, x, y, z, alpha, shuffleType, shuffleIndex,
            followMode, predictionEnabled, predictionLeadTicks, predictionMaxDistance,
            predictionLookAhead, predictionVerticalBoost);
    }
    
    /**
     * Checks if this update has a shuffle override.
     */
    public boolean hasShuffleOverride() {
        return shuffleType != null && !shuffleType.isEmpty();
    }
    
    /**
     * Checks if this is a static pattern (vs dynamic shuffle).
     */
    public boolean isStaticPattern() {
        return shuffleType != null && shuffleType.startsWith("static:");
    }
}
