package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.util.SilkTouchFallbacks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class CorruptedDiamondBlock extends Block {
	public static final MapCodec<CorruptedDiamondBlock> CODEC = createCodec(CorruptedDiamondBlock::new);

	public CorruptedDiamondBlock(AbstractBlock.Settings settings) {
		super(settings.allowsSpawning((state, world, pos, entityType) -> false)
				.requiresTool()
				.strength(3.0F, 8.0F)
				.sounds(net.minecraft.sound.BlockSoundGroup.METAL));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack stack, boolean dropExperience) {
		if (SilkTouchFallbacks.dropSelfIfSilkTouch(this, world, pos, stack)) {
			return;
		}
		super.onStacksDropped(state, world, pos, stack, dropExperience);
	}
}

