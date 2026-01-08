package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusDifficulty;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CustomPayload;

public record DifficultySyncPayload(VirusDifficulty difficulty) implements CustomPayload {
	public static final Id<DifficultySyncPayload> ID = new Id<>(TheVirusBlock.DIFFICULTY_SYNC_PACKET);
	public static final PacketCodec<PacketByteBuf, DifficultySyncPayload> CODEC = PacketCodec.of(DifficultySyncPayload::write, DifficultySyncPayload::new);

	public DifficultySyncPayload(PacketByteBuf buf) {
		this(VirusDifficulty.fromId(buf.readString()));
	}

	private void write(PacketByteBuf buf) {
		buf.writeString(difficulty().getId());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

