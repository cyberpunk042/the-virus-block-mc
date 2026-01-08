package net.cyberpunk042.block.entity;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.registry.ModBlockEntities;
import net.cyberpunk042.network.SingularityVisualStartPayload;
import net.cyberpunk042.network.SingularityVisualStopPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SingularityBlockEntity extends BlockEntity {
	public static final int ORB_GROW_TICKS = 40; // 2s
	public static final int ORB_SHRINK_TICKS = 20; // 1s
	public static final int BEAM_DELAY_TICKS = 20;

	private SingularityVisualStage stage = SingularityVisualStage.DORMANT;
	private long stageStartTick;
	public SingularityBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SINGULARITY_BLOCK, pos, state);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, SingularityBlockEntity entity) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (entity.stage == SingularityVisualStage.DORMANT) {
			entity.startSequence(serverWorld);
		}
		entity.advanceStage(serverWorld);
	}

	public static void clientTick(World world, BlockPos pos, BlockState state, SingularityBlockEntity entity) {
		if (entity.stage == SingularityVisualStage.DORMANT) {
			entity.startSequence(world);
		}
	}

	public void startSequence(World world) {
		setStage(SingularityVisualStage.ORB_GROW, world.getTime(), world.isClient);
		if (world instanceof ServerWorld serverWorld) {
			broadcastStart(serverWorld, getPos());
		}
	}

	private void advanceStage(ServerWorld world) {
		if (stage == SingularityVisualStage.DORMANT) {
			return;
		}
		long age = world.getTime() - stageStartTick;
		if (stage == SingularityVisualStage.ORB_GROW && age >= ORB_GROW_TICKS) {
			setStage(SingularityVisualStage.ORB_SHRINK, world.getTime(), false);
		} else if (stage == SingularityVisualStage.ORB_SHRINK && age >= ORB_SHRINK_TICKS) {
			setStage(SingularityVisualStage.BEAM_CHARGE, world.getTime(), false);
		} else if (stage == SingularityVisualStage.BEAM_CHARGE && age >= BEAM_DELAY_TICKS) {
			setStage(SingularityVisualStage.BEAM, world.getTime(), false);
		}
	}

	public SingularityVisualStage getStage() {
		return stage;
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putString("Stage", stage.asString());
		view.putLong("StageStart", stageStartTick);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		stage = SingularityVisualStage.fromName(view.getString("Stage", SingularityVisualStage.DORMANT.asString()));
		stageStartTick = view.getLong("StageStart", 0L);
	}

	private void setStage(SingularityVisualStage newStage, long worldTime, boolean clientSide) {
		SingularityVisualStage previous = stage;
		stage = newStage;
		stageStartTick = worldTime;
		if (!clientSide) {
			Logging.SINGULARITY.topic("visual").info("Stage {} -> {} at {}", previous, newStage, getPos());
			markDirty();
			sync();
		}
	}

	private void sync() {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		BlockEntityUpdateS2CPacket packet = BlockEntityUpdateS2CPacket.create(this);
		if (packet != null) {
			for (ServerPlayerEntity player : PlayerLookup.tracking(this)) {
				player.networkHandler.sendPacket(packet);
			}
		}
		serverWorld.getChunkManager().markForUpdate(getPos());
	}

	private static void broadcastStart(ServerWorld world, BlockPos pos) {
		SingularityVisualStartPayload payload = new SingularityVisualStartPayload(pos);
		for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void broadcastStop(ServerWorld world, BlockPos pos) {
		SingularityVisualStopPayload payload = new SingularityVisualStopPayload(pos);
		for (ServerPlayerEntity player : PlayerLookup.tracking(world, pos)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void notifyStop(World world, BlockPos pos) {
		if (world instanceof ServerWorld serverWorld) {
			broadcastStop(serverWorld, pos);
		}
	}

	public enum SingularityVisualStage implements StringIdentifiable {
		DORMANT("dormant"),
		ORB_GROW("orb_grow"),
		ORB_SHRINK("orb_shrink"),
		BEAM_CHARGE("beam_charge"),
		BEAM("beam");

		private final String name;

		SingularityVisualStage(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}

		static SingularityVisualStage fromName(String name) {
			for (SingularityVisualStage value : values()) {
				if (value.name.equalsIgnoreCase(name)) {
					return value;
				}
			}
			return DORMANT;
		}
	}
}

