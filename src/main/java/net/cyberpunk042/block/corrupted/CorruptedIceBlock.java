package net.cyberpunk042.block.corrupted;

import net.minecraft.block.BlockState;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class CorruptedIceBlock extends TranslucentBlock {
	public CorruptedIceBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		super.onSteppedOn(world, pos, state, entity);

		if (!world.isClient && entity instanceof LivingEntity livingEntity) {
			livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0));
			livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextFloat() < 0.25F) {
			double x = pos.getX() + random.nextDouble();
			double y = pos.getY() + 0.8D + random.nextDouble() * 0.5D;
			double z = pos.getZ() + random.nextDouble();
			world.addParticleClient(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0D, 0.015D, 0.0D);
		}
	}
}

