package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusDifficulty;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record VirusDifficultySelectPayload(int syncId, VirusDifficulty difficulty) implements CustomPayload {
	public static final Id<VirusDifficultySelectPayload> ID = new Id<>(TheVirusBlock.DIFFICULTY_SELECT_PACKET);
	public static final PacketCodec<PacketByteBuf, VirusDifficultySelectPayload> CODEC = PacketCodec.of(VirusDifficultySelectPayload::write, VirusDifficultySelectPayload::new);

	public VirusDifficultySelectPayload(PacketByteBuf buf) {
		this(buf.readVarInt(), VirusDifficulty.fromId(buf.readString()));
	}

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(syncId);
		buf.writeString(difficulty.getId());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

