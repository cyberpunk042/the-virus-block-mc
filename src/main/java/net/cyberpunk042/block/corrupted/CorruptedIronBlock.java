package net.cyberpunk042.block.corrupted;

import java.util.List;

import net.cyberpunk042.util.SilkTouchFallbacks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class CorruptedIronBlock extends Block {
	public CorruptedIronBlock(Settings settings) {
		super(settings.allowsSpawning((state, world, pos, entityType) -> false)
				.requiresTool()
				.strength(2.0F, 6.0F)
				.sounds(net.minecraft.sound.BlockSoundGroup.METAL));
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		super.randomTick(state, world, pos, random);

		if (random.nextFloat() < 0.4F) {
			List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(5.0D), entity -> true);
			Vec3d center = Vec3d.ofCenter(pos);

			for (ItemEntity item : items) {
				if (!item.isAlive()) {
					continue;
				}

				Vec3d pull = center.subtract(item.getPos());
				if (pull.lengthSquared() < 1.0E-4D) {
					continue;
				}

				Vec3d velocity = pull.normalize().multiply(0.2D + random.nextDouble() * 0.15D);
				item.addVelocity(velocity.x, velocity.y, velocity.z);
			}
		}
	}

	@Override
	public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack stack, boolean dropExperience) {
		if (SilkTouchFallbacks.dropSelfIfSilkTouch(this, world, pos, stack)) {
			return;
		}
		super.onStacksDropped(state, world, pos, stack, dropExperience);
	}
}

