package net.cyberpunk042.block.corrupted;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;

public class CorruptedCryingObsidianBlock extends Block {
	public static final MapCodec<CorruptedCryingObsidianBlock> CODEC = createCodec(CorruptedCryingObsidianBlock::new);

	public CorruptedCryingObsidianBlock(AbstractBlock.Settings settings) {
		super(settings.allowsSpawning((state, world, pos, entityType) -> false)
				.requiresTool()
				.strength(5.0F, 1200.0F)
				.sounds(net.minecraft.sound.BlockSoundGroup.GLASS));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}
}

