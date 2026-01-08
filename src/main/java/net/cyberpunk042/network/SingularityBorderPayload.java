package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SingularityBorderPayload(
		boolean active,
		double centerX,
		double centerZ,
		double initialDiameter,
		double currentDiameter,
		double targetDiameter,
		long duration,
		long elapsed,
		String phase) implements CustomPayload {

	public static final Id<SingularityBorderPayload> ID = new Id<>(TheVirusBlock.SINGULARITY_BORDER_PACKET);
	public static final PacketCodec<PacketByteBuf, SingularityBorderPayload> CODEC =
			PacketCodec.of(SingularityBorderPayload::write, SingularityBorderPayload::new);

	public SingularityBorderPayload(PacketByteBuf buf) {
		this(
				buf.readBoolean(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readVarLong(),
				buf.readVarLong(),
				buf.readString(32)
		);
	}

	private void write(PacketByteBuf buf) {
		buf.writeBoolean(active);
		buf.writeDouble(centerX);
		buf.writeDouble(centerZ);
		buf.writeDouble(initialDiameter);
		buf.writeDouble(currentDiameter);
		buf.writeDouble(targetDiameter);
		buf.writeVarLong(duration);
		buf.writeVarLong(elapsed);
		buf.writeString(phase);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

