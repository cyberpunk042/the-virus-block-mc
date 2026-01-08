package net.cyberpunk042.entity;

import java.util.Objects;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModStatusEffects;
import net.cyberpunk042.mixin.FallingBlockEntityAccessor;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class FallingMatrixCubeEntity extends FallingBlockEntity {
	private static final int MAX_FALL_TICKS = 600;

	private boolean registeredWithTracker;

	public FallingMatrixCubeEntity(EntityType<? extends FallingBlockEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.dropItem = false;  // Prevent vanilla from spawning items - we handle lifecycle manually
		this.registeredWithTracker = false;
	}

	public FallingMatrixCubeEntity(ServerWorld world, BlockPos origin, BlockState carriedState) {
		this(ModEntities.FALLING_MATRIX_CUBE, world);
		this.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
		((FallingBlockEntityAccessor) this).virus$setBlockState(Objects.requireNonNullElse(carriedState, ModBlocks.MATRIX_CUBE.getDefaultState()));
	}

	@Override
	public void tick() {
		if (!this.getWorld().isClient && !handleServerTick((ServerWorld) this.getWorld())) {
			return;
		}
		super.tick();
	}

	private boolean handleServerTick(ServerWorld world) {
		ensureRegistered(world);
		if (VirusWorldState.get(world).shieldFieldService().isShielding(this.getBlockPos())) {
			despawn(world);
			return false;
		}

		BlockPos below = this.getBlockPos().down();
		if (!world.isChunkLoaded(ChunkPos.toLong(below))) {
			return true;
		}
		if (shouldSettle(world, below)) {
			settle(world);
			return false;
		}

		BlockState stateBelow = world.getBlockState(below);
		if (stateBelow.isOf(Blocks.BEDROCK)) {
			world.setBlockState(below, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			return true;
		}
		if (hitsCoreOrShell(world, below, stateBelow)) {
			despawn(world);
			return false;
		}
		if (isProtected(world, below, stateBelow)) {
			settle(world);
			return false;
		}

		if (!stateBelow.isAir()) {
			world.breakBlock(below, false);
		}
		dealDamage(world, below);
		return true;
	}

	private boolean shouldSettle(ServerWorld world, BlockPos below) {
		return this.age > MAX_FALL_TICKS;
	}

	private void settle(ServerWorld world) {
		BlockPos landing = this.getBlockPos();
		if (landing.getY() < world.getBottomY()) {
			unregister(world);
			this.discard();
			return;
		}
		BlockState carried = this.getBlockState();
		BlockState current = world.getBlockState(landing);
		if (hitsCoreOrShell(world, landing, current)) {
			despawn(world);
			return;
		}
		if (isUnbreakable(world, landing, current)) {
			unregister(world);
			this.discard();
			return;
		}
		if (!current.isAir() && !isProtected(world, landing, current)) {
			world.breakBlock(landing, false);
		}
		world.setBlockState(landing, carried, Block.NOTIFY_LISTENERS);
		if (world.getBlockEntity(landing) instanceof MatrixCubeBlockEntity cube) {
			cube.markSettled();
		}
		world.playSound(null, landing, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.0F, 0.6F);
		unregister(world);
		this.discard();
	}

	private static boolean isProtected(ServerWorld world, BlockPos pos, BlockState state) {
		if (state.isAir()) {
			return false;
		}
		if (state.isOf(ModBlocks.MATRIX_CUBE)) {
			return true;
		}
		return isUnbreakable(world, pos, state);
	}

	private static boolean hitsCoreOrShell(ServerWorld world, BlockPos pos, BlockState state) {
		if (state.isAir() && VirusWorldState.get(world).singularity().fusing().isFuseClearedBlock(pos)) {
			return true;
		}
		Block block = state.getBlock();
		return block == ModBlocks.VIRUS_BLOCK
				|| block == ModBlocks.SINGULARITY_BLOCK
				|| block == ModBlocks.CORRUPTED_STONE
				|| block == ModBlocks.CORRUPTED_CRYING_OBSIDIAN
				|| block == ModBlocks.CORRUPTED_DIAMOND
				|| block == ModBlocks.CORRUPTED_GOLD
				|| block == ModBlocks.CORRUPTED_IRON
				|| block == ModBlocks.CURED_INFECTIOUS_CUBE
				|| block == Blocks.OBSIDIAN
				|| block == Blocks.CRYING_OBSIDIAN;
	}

	private void despawn(ServerWorld world) {
		unregister(world);
		world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.HOSTILE, 0.8F, 0.5F);
		this.discard();
	}

	private static boolean isUnbreakable(ServerWorld world, BlockPos pos, BlockState state) {
		return state.isOf(Blocks.BEDROCK)
				|| state.isOf(Blocks.OBSIDIAN)
				|| state.isOf(Blocks.CRYING_OBSIDIAN)
				|| state.isOf(ModBlocks.CURED_INFECTIOUS_CUBE)
				|| state.getHardness(world, pos) < 0.0F;
	}

	private void dealDamage(ServerWorld world, BlockPos pos) {
		int damage = Math.max(0, world.getGameRules().getInt(TheVirusBlock.VIRUS_MATRIX_CUBE_DAMAGE));
		if (damage <= 0) {
			return;
		}
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, new Box(pos).expand(0.5), LivingEntity::isAlive)) {
			if (living.hasStatusEffect(ModStatusEffects.PERSONAL_SHIELD)) {
				continue;
			}
			living.damage(world, world.getDamageSources().fallingBlock(this), damage);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
			unregister(serverWorld);
		}
		super.remove(reason);
	}

	public void markRegistered() {
		this.registeredWithTracker = true;
	}

	private void ensureRegistered(ServerWorld world) {
		if (!registeredWithTracker) {
			MatrixCubeBlockEntity.register(world, this.getUuid());
			registeredWithTracker = true;
		}
	}

	private void unregister(ServerWorld world) {
		if (registeredWithTracker) {
			MatrixCubeBlockEntity.unregister(world, this.getUuid());
			registeredWithTracker = false;
		}
	}
}

