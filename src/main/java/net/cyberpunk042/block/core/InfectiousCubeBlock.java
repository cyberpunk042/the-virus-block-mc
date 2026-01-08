package net.cyberpunk042.block.core;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.BoobytrapHelper;
import net.cyberpunk042.infection.BoobytrapHelper.Type;
import net.cyberpunk042.util.SilkTouchFallbacks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class InfectiousCubeBlock extends Block {
	public static final MapCodec<InfectiousCubeBlock> CODEC = createCodec(InfectiousCubeBlock::new);

	public InfectiousCubeBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		int attempts = world.getGameRules().getInt(TheVirusBlock.VIRUS_INFECTIOUS_SPREAD_ATTEMPTS);
		int radius = world.getGameRules().getInt(TheVirusBlock.VIRUS_INFECTIOUS_SPREAD_RADIUS);
		BoobytrapHelper.spread(world, pos, Type.INFECTIOUS_CUBE, attempts, radius);
	}

	@Override
	public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack stack, boolean dropExperience) {
		if (SilkTouchFallbacks.dropSelfIfSilkTouch(this, world, pos, stack)) {
			return;
		}
		super.onStacksDropped(state, world, pos, stack, dropExperience);
	}
}

