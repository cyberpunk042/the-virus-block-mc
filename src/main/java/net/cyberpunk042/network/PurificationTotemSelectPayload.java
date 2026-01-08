package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.item.PurificationOption;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PurificationTotemSelectPayload(int syncId, PurificationOption option) implements CustomPayload {
	public static final Id<PurificationTotemSelectPayload> ID = new Id<>(Identifier.of(TheVirusBlock.MOD_ID, "purification_totem_select"));
	public static final PacketCodec<PacketByteBuf, PurificationTotemSelectPayload> CODEC = PacketCodec.ofStatic(
			(PacketByteBuf buf, PurificationTotemSelectPayload payload) -> {
				buf.writeVarInt(payload.syncId());
				buf.writeEnumConstant(payload.option());
			},
			(PacketByteBuf buf) -> new PurificationTotemSelectPayload(buf.readVarInt(), buf.readEnumConstant(PurificationOption.class)));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

