package net.cyberpunk042.network.shader;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-client payload for shader time synchronization.
 * 
 * <p>Sent on player join and periodically to keep shader animations
 * synchronized across all clients on the same server.</p>
 * 
 * <p>Clients use this to calculate an offset from their local time,
 * then smoothly interpolate toward synced time for animations.</p>
 */
public record ShaderTimeSyncS2CPayload(long serverTimeMs) implements CustomPayload {
    
    public static final Identifier PACKET_ID = Identifier.of(TheVirusBlock.MOD_ID, "shader_time_sync");
    public static final Id<ShaderTimeSyncS2CPayload> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<PacketByteBuf, ShaderTimeSyncS2CPayload> CODEC = 
        PacketCodec.of(ShaderTimeSyncS2CPayload::write, ShaderTimeSyncS2CPayload::read);
    
    /**
     * Creates a payload with current server time.
     */
    public static ShaderTimeSyncS2CPayload now() {
        return new ShaderTimeSyncS2CPayload(System.currentTimeMillis());
    }
    
    /**
     * Read from network buffer.
     */
    public static ShaderTimeSyncS2CPayload read(PacketByteBuf buf) {
        return new ShaderTimeSyncS2CPayload(buf.readLong());
    }
    
    /**
     * Write to network buffer.
     */
    private void write(PacketByteBuf buf) {
        buf.writeLong(serverTimeMs);
    }
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
