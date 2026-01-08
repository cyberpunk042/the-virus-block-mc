package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class CorruptedGlassBlock extends TransparentBlock {
	public static final EnumProperty<CorruptionStage> STAGE = EnumProperty.of("stage", CorruptionStage.class);
	public static final MapCodec<CorruptedGlassBlock> CODEC = createCodec(CorruptedGlassBlock::new);

	public CorruptedGlassBlock(Settings settings) {
		super(settings);
		this.setDefaultState(getDefaultState().with(STAGE, CorruptionStage.STAGE_1));
	}

	@Override
	protected MapCodec<? extends TransparentBlock> getCodec() {
		return CODEC;
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient) {
			if (world instanceof ServerWorld serverWorld && VirusWorldState.get(serverWorld).shieldFieldService().isShielding(pos)) {
				return super.onBreak(world, pos, state, player);
			}
			world.createExplosion(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 1.8F, World.ExplosionSourceType.BLOCK);
		}

		return super.onBreak(world, pos, state, player);
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

