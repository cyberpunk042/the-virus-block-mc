package net.cyberpunk042.block.virus;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.entity.VirusBlockEntity;
import net.cyberpunk042.infection.VirusDamageClassifier;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.item.PurificationOption;
import net.cyberpunk042.registry.ModBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class VirusBlock extends BlockWithEntity {
	private static final int LOCK_MESSAGE_INTERVAL = 20;
	private static final double VULNERABLE_EXPLOSION_FRACTION = 0.01D;
	private static final double VULNERABLE_CONTACT_FRACTION = 0.0008333333333333334D;

	public static final MapCodec<VirusBlock> CODEC = createCodec(VirusBlock::new);

	public VirusBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);

		if (world instanceof ServerWorld serverWorld) {
			VirusWorldState virusState = VirusWorldState.get(serverWorld);
			virusState.sources().registerSource(virusState.sourceState(), pos);
			
			// Start the infection event if not already infected
			if (!virusState.infectionState().infected()) {
				virusState.infectionLifecycle().startInfection(pos);
			} else {
				// Additional virus block: double the health (x2, x3, x4, etc.)
				virusState.tiers().increaseHealthScaleForAdditionalSource();
				virusState.markDirty();
			}
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		if (!world.getBlockState(pos).isOf(state.getBlock())) {
			VirusWorldState infection = VirusWorldState.get(world);
			infection.sources().unregisterSource(infection.sourceState(), pos);
			
			// If that was the last source, end the infection
			if (infection.sources().isEmpty(infection.sourceState()) && infection.infectionState().infected()) {
				infection.infectionLifecycle().endInfection(); // This triggers cleanse
			}
		}

		super.onStateReplaced(state, world, pos, moved);
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (stack.isEmpty()) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		boolean handledItem = stack.isOf(Items.NETHER_STAR) || stack.isOf(Items.DRAGON_BREATH)
				|| stack.isOf(Items.OBSIDIAN) || stack.isOf(Items.CRYING_OBSIDIAN)
				|| stack.isOf(Items.TOTEM_OF_UNDYING);
		if (!handledItem) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		VirusWorldState infection = VirusWorldState.get(serverWorld);

		if (stack.isOf(Items.NETHER_STAR) || stack.isOf(Items.DRAGON_BREATH)) {
			boolean advanced = infection.tierProgression().forceAdvanceTier();
			if (advanced) {
				world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1.0F, 0.6F);
			}
		} else if (stack.isOf(Items.OBSIDIAN) || stack.isOf(Items.CRYING_OBSIDIAN)) {
			infection.tierProgression().applyContainmentCharge(stack.isOf(Items.CRYING_OBSIDIAN) ? 2 : 1);
			world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 0.7F, 0.9F);
		} else if (stack.isOf(Items.TOTEM_OF_UNDYING) && player instanceof ServerPlayerEntity serverPlayer) {
			PurificationOption.NO_BOOBYTRAPS.apply(serverWorld, serverPlayer, infection);
		}

		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
		}

		return ActionResult.SUCCESS;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new VirusBlockEntity(pos, state);
	}

	@Override
	protected void onExploded(BlockState state, ServerWorld world, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> stackMerger) {
		VirusWorldState infection = VirusWorldState.get(world);
		Entity source = explosion.getEntity();
		if (source != null && source.getCommandTags().contains(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG)) {
			return;
		}

		// Classify based on the explosion's entity
		String damageKey = VirusDamageClassifier.classifyExplosion(source);
		System.out.println("[Explosion DEBUG] Entity=" + (source != null ? source.getType().toString() : "null") + " -> damageKey=" + damageKey);

		if (infection.tiers().isApocalypseMode()) {
			System.out.println("[Explosion DEBUG] Apocalypse mode - applying damage with key: " + damageKey);
			infection.tierProgression().bleedHealth(VULNERABLE_EXPLOSION_FRACTION, damageKey);
		} else {
			System.out.println("[Explosion DEBUG] Not apocalypse - just disturbing");
			infection.disturbance().disturbByPlayer();
		}
	}

	@Override
	public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		super.onProjectileHit(world, state, hit, projectile);
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		VirusWorldState infection = VirusWorldState.get(serverWorld);
		if (projectile.getCommandTags().contains(TheVirusBlock.CORRUPTION_PROJECTILE_TAG)) {
			return;
		}

		// Classify as projectile damage for viral adaptation
		String damageKey = VirusDamageClassifier.KEY_PROJECTILE;

		if (infection.tiers().isApocalypseMode()) {
			infection.tierProgression().bleedHealth(VULNERABLE_CONTACT_FRACTION, damageKey);
		} else {
			infection.disturbance().disturbByPlayer();
		}
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : validateTicker(type, ModBlockEntities.VIRUS_BLOCK, VirusBlockEntity::tick);
	}

	@Override
	protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
		if (player.getWorld() instanceof ServerWorld serverWorld) {
			VirusWorldState infection = VirusWorldState.get(serverWorld);
			boolean vulnerable = infection.tiers().isApocalypseMode();

		if (vulnerable) {
				if (!player.isCreative()) {
					// Classify melee damage by tool type for viral adaptation
					String damageKey = classifyMeleeDamage(player);
					System.out.println("[Melee DEBUG] Player attacking with damageKey=" + damageKey);
					infection.tierProgression().bleedHealth(VULNERABLE_CONTACT_FRACTION, damageKey);
				}
			} else if (!player.isCreative()) {
				infection.disturbance().disturbByPlayer();
				if (player instanceof ServerPlayerEntity serverPlayer) {
					VirusBlockProtection.recordBlockedAttempt(serverPlayer, serverWorld);
					if (serverPlayer.age % LOCK_MESSAGE_INTERVAL == 0) {
						serverPlayer.sendMessage(Text.translatable("message.the-virus-block.block_locked"), true);
					}
				}
				return 0.0F;
			}
		}

		return super.calcBlockBreakingDelta(state, player, world, pos);
	}

	/**
	 * Classifies melee damage from the player's held item.
	 */
	private static String classifyMeleeDamage(PlayerEntity player) {
		ItemStack held = player.getMainHandStack();
		if (held.isEmpty()) {
			return VirusDamageClassifier.KEY_MELEE_FIST;
		}
		String itemId = net.minecraft.registry.Registries.ITEM.getId(held.getItem()).toString();
		return VirusDamageClassifier.KEY_MELEE_PREFIX + itemId;
	}
}

