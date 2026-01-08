package net.cyberpunk042.block.corrupted;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CorruptedWoodBlock extends Block {
	public CorruptedWoodBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		super.onSteppedOn(world, pos, state, entity);

		if (!world.isClient && entity instanceof LivingEntity livingEntity) {
			livingEntity.setOnFireFor(2);

			BlockPos firePos = pos.up();
			if (world.getBlockState(firePos).isAir()) {
				world.setBlockState(firePos, Blocks.FIRE.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
	}
}

