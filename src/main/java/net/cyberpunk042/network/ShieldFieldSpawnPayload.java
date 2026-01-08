package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ShieldFieldSpawnPayload(long id, double x, double y, double z, float radius) implements CustomPayload {
	public static final Id<ShieldFieldSpawnPayload> ID = new Id<>(TheVirusBlock.SHIELD_FIELD_SPAWN_PACKET);
	public static final PacketCodec<PacketByteBuf, ShieldFieldSpawnPayload> CODEC =
			PacketCodec.of(ShieldFieldSpawnPayload::write, ShieldFieldSpawnPayload::new);

	public ShieldFieldSpawnPayload(PacketByteBuf buf) {
		this(buf.readVarLong(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
	}

	private void write(PacketByteBuf buf) {
		buf.writeVarLong(id);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(radius);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

