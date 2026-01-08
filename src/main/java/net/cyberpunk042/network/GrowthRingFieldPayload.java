package net.cyberpunk042.network;

import java.util.ArrayList;
import java.util.List;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record GrowthRingFieldPayload(
		RegistryKey<World> worldKey,
		BlockPos origin,
		List<RingEntry> rings) implements CustomPayload {

	public static final Id<GrowthRingFieldPayload> ID = new Id<>(TheVirusBlock.GROWTH_RING_FIELD_PACKET);
	public static final PacketCodec<PacketByteBuf, GrowthRingFieldPayload> CODEC =
			PacketCodec.of(GrowthRingFieldPayload::write, GrowthRingFieldPayload::read);

	private static GrowthRingFieldPayload read(PacketByteBuf buf) {
		RegistryKey<World> world = RegistryKey.of(RegistryKeys.WORLD, buf.readIdentifier());
		BlockPos pos = buf.readBlockPos();
		int size = buf.readVarInt();
		List<RingEntry> entries = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Identifier fieldId = buf.readIdentifier();
			float radius = buf.readFloat();
			float width = buf.readFloat();
			int duration = buf.readVarInt();
			entries.add(new RingEntry(fieldId, radius, width, duration));
		}
		return new GrowthRingFieldPayload(world, pos, List.copyOf(entries));
	}

	private void write(PacketByteBuf buf) {
		buf.writeIdentifier(worldKey.getValue());
		buf.writeBlockPos(origin);
		buf.writeVarInt(rings.size());
		for (RingEntry entry : rings) {
			buf.writeIdentifier(entry.fieldProfileId());
			buf.writeFloat(entry.radius());
			buf.writeFloat(entry.width());
			buf.writeVarInt(entry.durationTicks());
		}
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public record RingEntry(Identifier fieldProfileId, float radius, float width, int durationTicks) {
	}
}

