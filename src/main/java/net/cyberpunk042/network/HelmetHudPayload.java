package net.cyberpunk042.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server→Client payload for Augmented Helmet HUD data.
 * 
 * <p>Sent at 10Hz only to players wearing the helmet.
 * Contains helmet-specific visualization data (direction arrow, signal).</p>
 * 
 * <p>Virus block positions and distances are sent separately via
 * {@link VirusBlockTelemetryPayload} to ALL players.</p>
 */
public record HelmetHudPayload(
    float yawToNearest,      // Direction angle for visor arrow (0-360°)
    float signalStrength,    // Signal strength for visor indicator (0.0-1.0)
    int totalCount,          // Total threats for display count
    boolean hasTarget        // Whether any threat exists
) implements CustomPayload {
    
    public static final Id<HelmetHudPayload> ID = 
        new Id<>(Identifier.of("the-virus-block", "helmet_hud"));
    
    public static final PacketCodec<RegistryByteBuf, HelmetHudPayload> CODEC = PacketCodec.of(
        HelmetHudPayload::write,
        HelmetHudPayload::read
    );
    
    private void write(RegistryByteBuf buf) {
        buf.writeFloat(yawToNearest);
        buf.writeFloat(signalStrength);
        buf.writeVarInt(totalCount);
        buf.writeBoolean(hasTarget);
    }
    
    private static HelmetHudPayload read(RegistryByteBuf buf) {
        float yaw = buf.readFloat();
        float strength = buf.readFloat();
        int total = buf.readVarInt();
        boolean hasTarget = buf.readBoolean();
        
        return new HelmetHudPayload(yaw, strength, total, hasTarget);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    /**
     * Creates an empty payload (no targets detected).
     */
    public static HelmetHudPayload empty() {
        return new HelmetHudPayload(0f, 0f, 0, false);
    }
    
    /**
     * Computes signal strength from distance.
     * Uses logarithmic scale for better visualization at long range.
     */
    public static float computeSignalStrength(float distance) {
        if (distance <= 0) return 1.0f;
        if (distance <= 10) return 1.0f;
        
        float logDist = (float)Math.log10(distance);
        float strength = 1.0f - (logDist - 1f) / 3.5f;
        return Math.max(0.05f, Math.min(1.0f, strength));
    }
}
