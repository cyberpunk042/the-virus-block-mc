package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record SingularityVisualStartPayload(BlockPos pos) implements CustomPayload {
	public static final Id<SingularityVisualStartPayload> ID = new Id<>(TheVirusBlock.SINGULARITY_VISUAL_START_PACKET);
	public static final PacketCodec<PacketByteBuf, SingularityVisualStartPayload> CODEC =
			PacketCodec.of(SingularityVisualStartPayload::write, SingularityVisualStartPayload::new);

	public SingularityVisualStartPayload(PacketByteBuf buf) {
		this(buf.readBlockPos());
	}

	private void write(PacketByteBuf buf) {
		buf.writeBlockPos(pos);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

