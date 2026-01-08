package net.cyberpunk042.block.singularity;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.block.entity.SingularityBlockEntity;
import net.cyberpunk042.registry.ModBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SingularityBlock extends BlockWithEntity {
	public static final MapCodec<SingularityBlock> CODEC = createCodec(SingularityBlock::new);
	private static final VoxelShape OUTLINE_SHAPE = VoxelShapes.cuboid(-0.125D, 0.0D, -0.125D, 1.125D, 1.125D, 1.125D);
	private static final VoxelShape COLLISION_SHAPE = VoxelShapes.cuboid(-0.0625D, 0.0D, -0.0625D, 1.0625D, 1.0625D, 1.0625D);

	public SingularityBlock(Settings settings) {
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
		return OUTLINE_SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return COLLISION_SHAPE;
	}

	@Override
	public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE_SHAPE;
	}

	@Override
	public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
		return OUTLINE_SHAPE;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new SingularityBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient
				? validateTicker(type, ModBlockEntities.SINGULARITY_BLOCK, SingularityBlockEntity::clientTick)
				: validateTicker(type, ModBlockEntities.SINGULARITY_BLOCK, SingularityBlockEntity::serverTick);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (world.getBlockEntity(pos) instanceof SingularityBlockEntity entity) {
			entity.startSequence(world);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		SingularityBlockEntity.notifyStop(world, pos);
		super.onStateReplaced(state, world, pos, moved);
	}
}

