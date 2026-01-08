package net.cyberpunk042.block.core;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

public class CuredInfectiousCubeBlock extends Block {
	public static final MapCodec<CuredInfectiousCubeBlock> CODEC = createCodec(CuredInfectiousCubeBlock::new);

	public CuredInfectiousCubeBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (world instanceof ServerWorld serverWorld) {
			VirusWorldState.get(serverWorld).shieldFieldService().evaluateCandidate(serverWorld, pos);
		}
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify) {
		super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
		if (world instanceof ServerWorld serverWorld) {
			VirusWorldState.get(serverWorld).shieldFieldService().evaluateCandidate(serverWorld, pos);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		if (!world.getBlockState(pos).isOf(state.getBlock())) {
			VirusWorldState.get(world).shieldFieldService().removeShieldAt(world, pos);
		}
		super.onStateReplaced(state, world, pos, moved);
	}
}

