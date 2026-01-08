package net.cyberpunk042.block.matrix;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModBlockEntities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MatrixCubeBlock extends BlockWithEntity {
	public static final MapCodec<MatrixCubeBlock> CODEC = createCodec(MatrixCubeBlock::new);
	private static final VoxelShape SHAPE = Block.createCuboidShape(1, 1, 1, 15, 15, 15);

	public MatrixCubeBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new MatrixCubeBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : validateTicker(type, ModBlockEntities.MATRIX_CUBE, MatrixCubeBlockEntity::tick);
	}

	@Override
	public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		super.onProjectileHit(world, state, hit, projectile);
		if (!world.isClient && world instanceof ServerWorld serverWorld) {
			if (VirusWorldState.get(serverWorld).shieldFieldService().isShielding(hit.getBlockPos())) {
				return;
			}
			serverWorld.createExplosion(projectile, hit.getBlockPos().getX() + 0.5, hit.getBlockPos().getY() + 0.5, hit.getBlockPos().getZ() + 0.5, 2.5F, World.ExplosionSourceType.BLOCK);
			world.breakBlock(hit.getBlockPos(), false);
		}
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient) {
			if (world instanceof ServerWorld serverWorld && VirusWorldState.get(serverWorld).shieldFieldService().isShielding(pos)) {
				return super.onBreak(world, pos, state, player);
			}
			world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 1.2F, World.ExplosionSourceType.BLOCK);
		}
		return super.onBreak(world, pos, state, player);
	}
}

