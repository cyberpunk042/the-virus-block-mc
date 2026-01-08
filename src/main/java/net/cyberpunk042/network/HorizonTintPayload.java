package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record HorizonTintPayload(boolean enabled, float intensity, int argb) implements CustomPayload {
	public static final Id<HorizonTintPayload> ID = new Id<>(TheVirusBlock.HORIZON_TINT_PACKET);
	public static final PacketCodec<PacketByteBuf, HorizonTintPayload> CODEC = PacketCodec.of(HorizonTintPayload::write, HorizonTintPayload::new);

	public HorizonTintPayload(PacketByteBuf buf) {
		this(buf.readBoolean(), buf.readFloat(), buf.readInt());
	}

	private void write(PacketByteBuf buf) {
		buf.writeBoolean(enabled);
		buf.writeFloat(intensity);
		buf.writeInt(argb);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

