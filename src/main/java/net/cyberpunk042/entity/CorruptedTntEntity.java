package net.cyberpunk042.entity;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;
import net.minecraft.entity.MovementType;

public class CorruptedTntEntity extends TntEntity {
	public CorruptedTntEntity(EntityType<? extends TntEntity> type, World world) {
		super(type, world);
	}

	public CorruptedTntEntity(World world, double x, double y, double z, @Nullable LivingEntity igniter) {
		this(ModEntities.CORRUPTED_TNT, world);
		this.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
		double angle = world.random.nextDouble() * 2.0D * Math.PI;
		this.setVelocity(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);
		this.setFuse(80);
	}

	@Override
	public BlockState getBlockState() {
		return ModBlocks.CORRUPTED_TNT.getDefaultState();
	}

	public static CorruptedTntEntity spawn(ServerWorld world, double x, double y, double z, @Nullable LivingEntity igniter, int fuse) {
		CorruptedTntEntity entity = new CorruptedTntEntity(world, x, y, z, igniter);
		entity.setFuse(fuse);
		world.spawnEntity(entity);
		return entity;
	}

	@Override
	public void tick() {
		if (!this.hasNoGravity()) {
			this.setVelocity(this.getVelocity().add(0.0D, -0.04D, 0.0D));
		}
		this.move(MovementType.SELF, this.getVelocity());
		this.setVelocity(this.getVelocity().multiply(0.98D));
		if (this.isOnGround()) {
			this.setVelocity(this.getVelocity().multiply(0.7D, -0.5D, 0.7D));
		}
		int fuse = this.getFuse() - 1;
		this.setFuse(fuse);
		if (fuse <= 0) {
			this.discard();
			if (!this.getWorld().isClient()) {
				explodeCustom();
			}
		} else {
			this.updateWaterState();
			if (this.getWorld() instanceof ServerWorld serverWorld) {
				serverWorld.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	private void explodeCustom() {
		World world = this.getWorld();
		ServerWorld serverWorld = (ServerWorld) world;
		double roll = serverWorld.random.nextDouble();
		if (roll < 1.0D / 3.0D) {
			serverWorld.playSound(
					null,
					getX(),
					getY(),
					getZ(),
					SoundEvents.BLOCK_FIRE_EXTINGUISH,
					SoundCategory.BLOCKS,
					0.6F,
					0.8F + serverWorld.random.nextFloat() * 0.3F);
		} else {
			float power = roll < (2.0D / 3.0D) ? 4.0F : 6.0F;
			serverWorld.createExplosion(this, getX(), getBodyY(0.0625D), getZ(), power, ExplosionSourceType.TNT);
		}
	}
}

