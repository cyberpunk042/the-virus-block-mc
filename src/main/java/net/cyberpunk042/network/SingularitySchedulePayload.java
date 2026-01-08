package net.cyberpunk042.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SingularitySchedulePayload(List<RingEntry> rings) implements CustomPayload {
	public static final Id<SingularitySchedulePayload> ID = new Id<>(TheVirusBlock.SINGULARITY_SCHEDULE_PACKET);
	public static final PacketCodec<PacketByteBuf, SingularitySchedulePayload> CODEC =
			PacketCodec.of(SingularitySchedulePayload::write, SingularitySchedulePayload::new);

	public SingularitySchedulePayload(PacketByteBuf buf) {
		this(readEntries(buf));
	}

	private void write(PacketByteBuf buf) {
		buf.writeCollection(rings, (packet, entry) -> {
			packet.writeVarInt(entry.index());
			packet.writeVarInt(entry.chunkCount());
			packet.writeVarInt(entry.sideLength());
			packet.writeDouble(entry.activationRadius());
			packet.writeVarInt(entry.tickInterval());
		});
	}

	public List<RingEntry> rings() {
		return Collections.unmodifiableList(rings);
	}

	private static List<RingEntry> readEntries(PacketByteBuf buf) {
		return buf.readCollection(ArrayList::new, packet -> new RingEntry(
				packet.readVarInt(),
				packet.readVarInt(),
				packet.readVarInt(),
				packet.readDouble(),
				packet.readVarInt()
		));
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public record RingEntry(int index, int chunkCount, int sideLength, double activationRadius, int tickInterval) {
	}
}

