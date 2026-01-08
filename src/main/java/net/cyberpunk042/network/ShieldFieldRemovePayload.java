package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ShieldFieldRemovePayload(long id) implements CustomPayload {
	public static final Id<ShieldFieldRemovePayload> ID = new Id<>(TheVirusBlock.SHIELD_FIELD_REMOVE_PACKET);
	public static final PacketCodec<PacketByteBuf, ShieldFieldRemovePayload> CODEC =
			PacketCodec.of(ShieldFieldRemovePayload::write, ShieldFieldRemovePayload::new);

	public ShieldFieldRemovePayload(PacketByteBuf buf) {
		this(buf.readVarLong());
	}

	private void write(PacketByteBuf buf) {
		buf.writeVarLong(id);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

