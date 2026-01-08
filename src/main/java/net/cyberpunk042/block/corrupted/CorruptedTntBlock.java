package net.cyberpunk042.block.corrupted;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.entity.CorruptedTntEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;

public class CorruptedTntBlock extends TntBlock {
	private static final BooleanProperty UNSTABLE = Properties.UNSTABLE;

	public CorruptedTntBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!oldState.isOf(state.getBlock())) {
			if (world.isReceivingRedstonePower(pos) && prime(world, pos)) {
				world.removeBlock(pos, false);
			}
		}
	}

	@Override
	protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
		if (world.isReceivingRedstonePower(pos) && prime(world, pos)) {
			world.removeBlock(pos, false);
		}
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient() && !player.getAbilities().creativeMode && state.get(UNSTABLE)) {
			prime(world, pos, player);
		}
		return super.onBreak(world, pos, state, player);
	}

	@Override
	public void onDestroyedByExplosion(ServerWorld world, BlockPos pos, Explosion explosion) {
		if (world.getGameRules().getBoolean(GameRules.TNT_EXPLODES)) {
			LivingEntity igniter = explosion.getCausingEntity() instanceof LivingEntity living ? living : null;
			CorruptedTntEntity entity = CorruptedTntEntity.spawn(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, igniter, 80);
			int fuse = entity.getFuse();
			entity.setFuse((short) (world.random.nextInt(Math.max(1, fuse / 4)) + Math.max(1, fuse / 8)));
		}
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!stack.isOf(Items.FLINT_AND_STEEL) && !stack.isOf(Items.FIRE_CHARGE)) {
			return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
		}
		if (prime(world, pos, player)) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL_AND_REDRAW);
			Item item = stack.getItem();
			if (stack.isOf(Items.FLINT_AND_STEEL)) {
				stack.damage(1, player, LivingEntity.getSlotForHand(hand));
			} else {
				stack.decrementUnlessCreative(1, player);
			}
			player.incrementStat(Stats.USED.getOrCreateStat(item));
		} else if (world instanceof ServerWorld serverWorld && !serverWorld.getGameRules().getBoolean(GameRules.TNT_EXPLODES)) {
			player.sendMessage(Text.translatable("block.minecraft.tnt.disabled"), true);
			return ActionResult.PASS;
		}
		return ActionResult.SUCCESS;
	}

	@Override
	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		if (world instanceof ServerWorld serverWorld) {
			BlockPos blockPos = hit.getBlockPos();
			Entity owner = projectile.getOwner();
			if (projectile.isOnFire()
					&& projectile.canModifyAt(serverWorld, blockPos)
					&& prime(world, blockPos, owner instanceof LivingEntity living ? living : null)) {
				world.removeBlock(blockPos, false);
			}
		}
	}

	private static boolean prime(World world, BlockPos pos) {
		return prime(world, pos, null);
	}

	private static boolean prime(World world, BlockPos pos, @Nullable LivingEntity igniter) {
		if (world instanceof ServerWorld serverWorld && serverWorld.getGameRules().getBoolean(GameRules.TNT_EXPLODES)) {
			CorruptedTntEntity.spawn(serverWorld, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, igniter, 80);
			world.playSound(null, pos, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.emitGameEvent(igniter, GameEvent.PRIME_FUSE, pos);
			return true;
		}
		if (world instanceof ServerWorld serverWorld) {
			serverWorld.getPlayers(player -> player.hasPermissionLevel(2))
					.forEach(player -> player.sendMessage(Text.translatable("block.minecraft.tnt.disabled"), true));
		}
		return false;
	}
}
