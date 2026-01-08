package net.cyberpunk042.network;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public record GrowthBeamPayload(
		RegistryKey<World> worldKey,
		BlockPos origin,
		int targetEntityId,
		double targetX,
		double targetY,
		double targetZ,
		boolean pulling,
		float red,
		float green,
		float blue,
		int durationTicks) implements CustomPayload {

	public static final Id<GrowthBeamPayload> ID = new Id<>(TheVirusBlock.GROWTH_BEAM_PACKET);
	public static final PacketCodec<PacketByteBuf, GrowthBeamPayload> CODEC =
			PacketCodec.of(GrowthBeamPayload::write, GrowthBeamPayload::new);

	public GrowthBeamPayload(PacketByteBuf buf) {
		this(
				RegistryKey.of(RegistryKeys.WORLD, buf.readIdentifier()),
				buf.readBlockPos(),
				buf.readVarInt(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readBoolean(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readVarInt());
	}

	private void write(PacketByteBuf buf) {
		buf.writeIdentifier(worldKey.getValue());
		buf.writeBlockPos(origin);
		buf.writeVarInt(targetEntityId);
		buf.writeDouble(targetX);
		buf.writeDouble(targetY);
		buf.writeDouble(targetZ);
		buf.writeBoolean(pulling);
		buf.writeFloat(red);
		buf.writeFloat(green);
		buf.writeFloat(blue);
		buf.writeVarInt(durationTicks);
	}

	public Vec3d targetPosition() {
		return new Vec3d(targetX, targetY, targetZ);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

