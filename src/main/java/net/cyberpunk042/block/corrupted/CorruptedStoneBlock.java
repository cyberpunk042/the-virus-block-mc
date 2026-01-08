package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;

public class CorruptedStoneBlock extends FallingBlock {
	public static final EnumProperty<CorruptionStage> STAGE = EnumProperty.of("stage", CorruptionStage.class);
	public static final MapCodec<CorruptedStoneBlock> CODEC = createCodec(CorruptedStoneBlock::new);

	public CorruptedStoneBlock(Settings settings) {
		super(settings);
		this.setDefaultState(getDefaultState().with(STAGE, CorruptionStage.STAGE_1));
	}

	@Override
	protected MapCodec<? extends FallingBlock> getCodec() {
		return CODEC;
	}

	@Override
	public int getColor(BlockState state, BlockView world, BlockPos pos) {
		return state.getMapColor(world, pos).color;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(STAGE);
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		CorruptionStage targetStage = VirusWorldState.get(world).tiers().currentTier().getIndex() >= 2
				? CorruptionStage.STAGE_2
				: CorruptionStage.STAGE_1;
		if (state.get(STAGE) != targetStage) {
			world.setBlockState(pos, state.with(STAGE, targetStage), Block.NOTIFY_LISTENERS);
		}
	}
}

