package net.cyberpunk042.block.core;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.BoobytrapHelper;
import net.cyberpunk042.infection.BoobytrapHelper.Type;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class BacteriaBlock extends Block {

	public BacteriaBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		if (!world.isClient && world instanceof ServerWorld serverWorld) {
			scheduleNextPulse(serverWorld, pos, serverWorld.getRandom());
		}
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		scheduleNextPulse(world, pos, random);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		int attempts = world.getGameRules().getInt(TheVirusBlock.VIRUS_BACTERIA_SPREAD_ATTEMPTS);
		int radius = world.getGameRules().getInt(TheVirusBlock.VIRUS_BACTERIA_SPREAD_RADIUS);
		BoobytrapHelper.spread(world, pos, Type.BACTERIA, attempts, radius);
		scheduleNextPulse(world, pos, random);
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		if (!world.isClient && entity instanceof LivingEntity) {
			BoobytrapHelper.triggerExplosion((ServerWorld) world, pos, Type.BACTERIA, "contact");
			world.breakBlock(pos, false);
		}
		super.onSteppedOn(world, pos, state, entity);
	}

	private void scheduleNextPulse(ServerWorld world, BlockPos pos, Random random) {
		int interval = Math.max(10, world.getGameRules().getInt(TheVirusBlock.VIRUS_BACTERIA_PULSE_INTERVAL));
		int variance = Math.max(5, interval / 3);
		world.scheduleBlockTick(pos, this, interval + random.nextBetween(0, variance));
	}
}

