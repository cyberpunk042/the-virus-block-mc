package net.cyberpunk042.entity;

import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class VirusFuseEntity extends TntEntity {
	private static final int LOOP_FUSE_TICKS = 20;
	private static final int TOTAL_FUSE_TICKS = 20 * 20;
	private static final int SLIDE_START_TICKS = 5 * 20;
	private static final double SLIDE_DISTANCE = 15.0D;
	private static final TrackedData<BlockPos> TRACKED_OWNER_POS = DataTracker.registerData(VirusFuseEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
	private static final TrackedData<Float> TRACKED_SLIDE = DataTracker.registerData(VirusFuseEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private BlockPos ownerPos = BlockPos.ORIGIN;
	private int ticksAlive;

	public VirusFuseEntity(EntityType<? extends TntEntity> type, World world) {
		super(type, world);
		this.setFuse(LOOP_FUSE_TICKS);
		this.setSilent(true);
		this.setInvulnerable(true);
		this.setNoGravity(true);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(TRACKED_OWNER_POS, BlockPos.ORIGIN);
		builder.add(TRACKED_SLIDE, 0.0F);
	}

	public VirusFuseEntity(World world, BlockPos ownerPos) {
		this(ModEntities.VIRUS_FUSE, world);
		setOwnerPos(ownerPos);
	}

	public void setOwnerPos(BlockPos pos) {
		if (pos == null) {
			return;
		}
		BlockPos immutable = pos.toImmutable();
		if (immutable.equals(this.ownerPos)) {
			return;
		}
		this.ownerPos = immutable;
		this.dataTracker.set(TRACKED_OWNER_POS, this.ownerPos);
		refreshPositionAndAngles(ownerPos.getX() + 0.5D, ownerPos.getY(), ownerPos.getZ() + 0.5D, 0.0F, 0.0F);
	}

	public BlockPos getOwnerPos() {
		return ownerPos;
	}

	@Override
	public BlockState getBlockState() {
		return ModBlocks.VIRUS_BLOCK.getDefaultState();
	}

	@Override
	public void tick() {
		ticksAlive++;
		World world = this.getWorld();
		if (world.isClient) {
			clientTick(world);
			return;
		}
		serverTick((ServerWorld) world);
	}

	private double computeSlideOffset() {
		if (ticksAlive < SLIDE_START_TICKS) {
			return 0.0D;
		}
		double progress = (double)(ticksAlive - SLIDE_START_TICKS) / Math.max(1, TOTAL_FUSE_TICKS - SLIDE_START_TICKS);
		return Math.min(progress, 1.0D) * SLIDE_DISTANCE;
	}

	private void serverTick(ServerWorld serverWorld) {
		if (ownerPos == null || !serverWorld.isChunkLoaded(ChunkPos.toLong(ownerPos))) {
			discard();
			return;
		}
		double offset = computeSlideOffset();
		this.dataTracker.set(TRACKED_OWNER_POS, ownerPos);
		this.dataTracker.set(TRACKED_SLIDE, (float) offset);
		refreshPositionAndAngles(ownerPos.getX() + 0.5D, ownerPos.getY() + offset, ownerPos.getZ() + 0.5D, 0.0F, 0.0F);
		updateFuseLoop();
		serverWorld.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	private void clientTick(World world) {
		BlockPos trackedPos = this.dataTracker.get(TRACKED_OWNER_POS);
		float offset = this.dataTracker.get(TRACKED_SLIDE);
		if (!trackedPos.equals(BlockPos.ORIGIN)) {
			this.ownerPos = trackedPos;
			this.setPosition(trackedPos.getX() + 0.5D, trackedPos.getY() + offset, trackedPos.getZ() + 0.5D);
		}
	}

	private void updateFuseLoop() {
		int fuse = this.getFuse() - 1;
		if (fuse <= 0) {
			fuse = LOOP_FUSE_TICKS;
		}
		this.setFuse(fuse);
	}
}


