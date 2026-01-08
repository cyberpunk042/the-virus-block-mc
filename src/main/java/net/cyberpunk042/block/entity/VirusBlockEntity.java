package net.cyberpunk042.block.entity;

import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.item.PurificationTotemUtil;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class VirusBlockEntity extends BlockEntity {
	private int auraTick;
	private int heartbeatCooldown;
	private boolean registered = false; // Track if already registered
	private final Object2LongMap<UUID> auraCooldowns = new Object2LongOpenHashMap<>();

	public VirusBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VIRUS_BLOCK, pos, state);
	}

	public static void tick(World world, BlockPos pos, BlockState state, VirusBlockEntity entity) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		// Only register once, not every tick
		if (!entity.registered) {
			VirusWorldState infection = VirusWorldState.get(serverWorld);
			infection.sources().registerSource(infection.sourceState(), pos);
			entity.registered = true;
		}

		entity.auraTick++;

		if (entity.auraTick % 5 == 0) {
			VirusWorldState infection = VirusWorldState.get(serverWorld);
			entity.applyAura(serverWorld, pos, infection);
		}

		if (entity.auraTick % 20 == 0) {
			serverWorld.spawnParticles(ParticleTypes.PORTAL, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 4, 0.4D, 0.2D, 0.4D, 0.0D);
		}

		// Heartbeat sound - "The Infection Sings"
		entity.tickHeartbeat(serverWorld, pos);
	}

	/**
	 * Emits a rhythmic heartbeat sound that intensifies with infection tier.
	 * <ul>
	 *   <li>Tier 1: every 60 ticks (3 sec) - slow, ominous</li>
	 *   <li>Tier 5: every 20 ticks (1 sec) - rapid, threatening</li>
	 *   <li>Apocalypse: every 10 ticks (0.5 sec) - frantic</li>
	 * </ul>
	 */
	private void tickHeartbeat(ServerWorld world, BlockPos pos) {
		// Always decrement cooldown
		if (heartbeatCooldown > 0) {
			heartbeatCooldown--;
			return;
		}

		// Get infection state
		VirusWorldState infection = VirusWorldState.get(world);
		boolean isInfected = infection.infectionState().infected();
		
		// Debug every 3 seconds
		if (auraTick % 60 == 0) {
			System.out.println("[Heartbeat DEBUG] pos=" + pos + " infected=" + isInfected);
		}
		
		// Must be infected for heartbeat
		if (!isInfected) {
			return;
		}

		InfectionTier tier = infection.tiers().currentTier();
		boolean apocalypse = infection.tiers().isApocalypseMode();

		// Set next heartbeat interval
		int interval = apocalypse ? 10 : Math.max(20, 60 - tier.getIndex() * 10);
		heartbeatCooldown = interval;

		// Volume and pitch - HIGH volume for long range
		float volume = 5.0F; // Very loud for long range
		float pitch = 0.8F;

		// Play the sound
		world.playSound(null, pos, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, volume, pitch);
	}

	private void applyAura(ServerWorld world, BlockPos pos, VirusWorldState infection) {
		InfectionTier tier = infection.tiers().currentTier();
		double radius = infection.combat().getActiveAuraRadius();

		long now = world.getTime();
		Box area = new Box(pos).expand(radius);
		for (ServerPlayerEntity player : world.getEntitiesByClass(ServerPlayerEntity.class, area, ServerPlayerEntity::isAlive)) {
			if (PurificationTotemUtil.isHolding(player)) {
				continue;
			}
			if (!shouldAffect(player, infection, now)) {
				continue;
			}

			applyStatusEffects(world.getRandom(), player, tier, infection);
			if (player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= radius * radius) {
				degradeArmor(world, player, tier);
			}
		}
	}

	private boolean shouldAffect(LivingEntity target, VirusWorldState infection, long now) {
		long ready = auraCooldowns.getOrDefault(target.getUuid(), 0L);
		if (now < ready) {
			return false;
		}

		long cooldown = Math.max(40L, 160L - infection.tiers().currentTier().getIndex() * 20L - (infection.tiers().isApocalypseMode() ? 20L : 0L));
		auraCooldowns.put(target.getUuid(), now + cooldown);

		if (auraTick % 200 == 0) {
			auraCooldowns.object2LongEntrySet().removeIf(entry -> entry.getLongValue() + 400L < now);
		}

		return true;
	}

	private static void applyStatusEffects(Random random, LivingEntity target, InfectionTier tier, VirusWorldState infection) {
		int tierIndex = tier.getIndex();
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60 + tierIndex * 20, tierIndex >= 3 ? 1 : 0, false, true));

		if (tierIndex >= 1 && target instanceof ServerPlayerEntity player) {
			forceHungerDrop(player, tierIndex, infection.tiers().isApocalypseMode());
		}

		if (tierIndex >= 2) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, tierIndex >= 4 ? 2 : 1, false, true));
		}

		if (tierIndex >= 3) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, true));
		}

		if (tierIndex >= 4) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 1, false, true));
		}

		if (infection.tiers().isApocalypseMode() && random.nextFloat() < 0.2F) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 50, 0));
		}
	}

	private static void forceHungerDrop(ServerPlayerEntity player, int tierIndex, boolean apocalypseMode) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		float exhaustion = 1.0F + tierIndex * 0.3F + (apocalypseMode ? 0.5F : 0.0F);
		player.addExhaustion(exhaustion);
	}

	private static void degradeArmor(ServerWorld world, LivingEntity living, InfectionTier tier) {
		Random random = world.getRandom();
		if (random.nextFloat() > 0.12F + tier.getIndex() * 0.03F) {
			return;
		}

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!slot.isArmorSlot()) {
				continue;
			}

			ItemStack stack = living.getEquippedStack(slot);
			if (stack.isEmpty()) {
				continue;
			}

			stack.damage(
					1 + tier.getIndex(),
					world,
					living instanceof ServerPlayerEntity player ? player : null,
					item -> living.sendEquipmentBreakStatus(item, slot));
		}
	}
}

