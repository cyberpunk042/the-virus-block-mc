package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record SingularityVisualStopPayload(BlockPos pos) implements CustomPayload {
	public static final Id<SingularityVisualStopPayload> ID = new Id<>(TheVirusBlock.SINGULARITY_VISUAL_STOP_PACKET);
	public static final PacketCodec<PacketByteBuf, SingularityVisualStopPayload> CODEC =
			PacketCodec.of(SingularityVisualStopPayload::write, SingularityVisualStopPayload::new);

	public SingularityVisualStopPayload(PacketByteBuf buf) {
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

