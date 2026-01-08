package net.cyberpunk042.infection.service;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.state.InfectionState;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.util.VirusEquipmentHelper;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Handles per-player exposure effects for standing on or carrying infectious blocks.
 */
public final class InfectionExposureService {

	private static final int INFECTIOUS_CONTACT_THRESHOLD = 40;
	private static final int INFECTIOUS_CONTACT_INTERVAL = 20;
	private static final int INFECTIOUS_INVENTORY_THRESHOLD = 20;
	private static final int INFECTIOUS_INVENTORY_INTERVAL = 60;
	private static final int INFECTIOUS_INVENTORY_WARNING_COOLDOWN = 200;
	private static final int RUBBER_CONTACT_THRESHOLD_BONUS = 60;
	private static final int RUBBER_CONTACT_INTERVAL_BONUS = 10;
	private static final float RUBBER_CONTACT_DAMAGE = 0.5F;

	private final VirusWorldState host;

	public InfectionExposureService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void tickContact() {
		ServerWorld world = host.world();
		InfectionState state = host.infectionState();
		Object2IntMap<UUID> contactTicks = state.infectiousContactTicks();
		Set<UUID> active = new HashSet<>();
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.isSpectator() || player.isCreative()) {
				contactTicks.removeInt(player.getUuid());
				continue;
			}
			BlockPos feet = player.getBlockPos().down();
			if (!world.getBlockState(feet).isOf(ModBlocks.INFECTIOUS_CUBE)) {
				contactTicks.removeInt(player.getUuid());
				continue;
			}
			UUID uuid = player.getUuid();
			active.add(uuid);
			int ticks = contactTicks.getOrDefault(uuid, 0) + 1;
			contactTicks.put(uuid, ticks);
			boolean wearingBoots = VirusEquipmentHelper.hasRubberBoots(player);
			int threshold = INFECTIOUS_CONTACT_THRESHOLD + (wearingBoots ? RUBBER_CONTACT_THRESHOLD_BONUS : 0);
			int interval = INFECTIOUS_CONTACT_INTERVAL + (wearingBoots ? RUBBER_CONTACT_INTERVAL_BONUS : 0);
			if (threshold > 0 && ticks == threshold) {
				player.sendMessage(Text.translatable("message.the-virus-block.infectious_contact_warning"), true);
			}
			if (ticks > threshold && (ticks - threshold) % interval == 0) {
				float damage = wearingBoots ? RUBBER_CONTACT_DAMAGE : 1.0F;
				player.damage(world, world.getDamageSources().magic(), damage);
				world.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.ENTITY_SILVERFISH_HURT, SoundCategory.BLOCKS, 0.6F, 1.2F);
				if (wearingBoots) {
					VirusEquipmentHelper.damageRubberBoots(player, 1);
				}
			}
		}
		if (contactTicks.isEmpty()) {
			return;
		}
		contactTicks.object2IntEntrySet().removeIf(entry -> !active.contains(entry.getKey()));
	}

	public void tickInventory() {
		ServerWorld world = host.world();
		ItemStack infectiousStack = ModBlocks.INFECTIOUS_CUBE.asItem().getDefaultStack();
		InfectionState state = host.infectionState();
		Object2IntMap<UUID> inventoryTicks = state.infectiousInventoryTicks();
		Object2LongMap<UUID> warnCooldowns = state.infectiousInventoryWarnCooldowns();
		Set<UUID> retained = new HashSet<>();
		long now = world.getTime();
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			UUID uuid = player.getUuid();
			if (player.isSpectator() || player.isCreative()
					|| player.getInventory().count(infectiousStack.getItem()) <= 0) {
				inventoryTicks.removeInt(uuid);
				warnCooldowns.removeLong(uuid);
				continue;
			}
			retained.add(uuid);
			int ticks = inventoryTicks.getOrDefault(uuid, 0) + 1;
			inventoryTicks.put(uuid, ticks);
			if (INFECTIOUS_INVENTORY_THRESHOLD > 0 && ticks == INFECTIOUS_INVENTORY_THRESHOLD) {
				long nextAllowed = warnCooldowns.getOrDefault(uuid, 0L);
				if (now >= nextAllowed) {
					player.sendMessage(Text.translatable("message.the-virus-block.infectious_inventory_warning"), true);
					warnCooldowns.put(uuid, now + INFECTIOUS_INVENTORY_WARNING_COOLDOWN);
				}
			}
			if (ticks > INFECTIOUS_INVENTORY_THRESHOLD
					&& (ticks - INFECTIOUS_INVENTORY_THRESHOLD) % INFECTIOUS_INVENTORY_INTERVAL == 0) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 120, 1));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 80, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0));
			}
		}
		if (retained.isEmpty()) {
			inventoryTicks.clear();
			warnCooldowns.clear();
		} else {
			inventoryTicks.object2IntEntrySet().removeIf(entry -> !retained.contains(entry.getKey()));
			warnCooldowns.object2LongEntrySet().removeIf(entry -> !retained.contains(entry.getKey()));
		}
	}
}

