package net.cyberpunk042.block.core;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.infection.GlobalTerrainCorruption;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class InfectedGrassBlock extends Block {
	public static final MapCodec<InfectedGrassBlock> CODEC = createCodec(InfectedGrassBlock::new);

	public InfectedGrassBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (!world.isClient && world instanceof ServerWorld serverWorld) {
			VirusWorldState infection = VirusWorldState.get(serverWorld);
			if (infection.infectionState().infected()) {
				GlobalTerrainCorruption.trigger(serverWorld, pos);
			}
		}
	}
}

