package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record VoidTearSpawnPayload(long id, double x, double y, double z, float radius,
                                   int durationTicks, int tierIndex) implements CustomPayload {
	public static final Id<VoidTearSpawnPayload> ID = new Id<>(TheVirusBlock.VOID_TEAR_SPAWN_PACKET);
	public static final PacketCodec<PacketByteBuf, VoidTearSpawnPayload> CODEC =
			PacketCodec.of(VoidTearSpawnPayload::write, VoidTearSpawnPayload::new);

	public VoidTearSpawnPayload(PacketByteBuf buf) {
		this(buf.readVarLong(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
				buf.readFloat(), buf.readVarInt(), buf.readVarInt());
	}

	private void write(PacketByteBuf buf) {
		buf.writeVarLong(id);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(radius);
		buf.writeVarInt(durationTicks);
		buf.writeVarInt(tierIndex);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
