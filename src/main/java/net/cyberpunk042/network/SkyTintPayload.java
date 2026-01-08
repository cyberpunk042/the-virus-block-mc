package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SkyTintPayload(boolean skyCorrupted, boolean fluidsCorrupted) implements CustomPayload {
	public static final Id<SkyTintPayload> ID = new Id<>(TheVirusBlock.SKY_TINT_PACKET);
	public static final PacketCodec<PacketByteBuf, SkyTintPayload> CODEC = PacketCodec.of(SkyTintPayload::write, SkyTintPayload::new);

	public SkyTintPayload(PacketByteBuf buf) {
		this(buf.readBoolean(), buf.readBoolean());
	}

	private void write(PacketByteBuf buf) {
		buf.writeBoolean(skyCorrupted);
		buf.writeBoolean(fluidsCorrupted);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}


