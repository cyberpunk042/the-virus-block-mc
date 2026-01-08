package net.cyberpunk042.util;

import net.cyberpunk042.registry.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class VirusEquipmentHelper {
	private VirusEquipmentHelper() {
	}

	public static boolean hasCompositeElytra(LivingEntity living) {
		return isEquipped(living, EquipmentSlot.CHEST, ModItems.COMPOSITE_ELYTRA);
	}

	public static boolean hasRubberBoots(LivingEntity living) {
		return isEquipped(living, EquipmentSlot.FEET, ModItems.RUBBER_BOOTS);
	}

	public static boolean hasHeavyPants(LivingEntity living) {
		return isEquipped(living, EquipmentSlot.LEGS, ModItems.HEAVY_PANTS);
	}

	public static boolean hasAugmentedHelmet(LivingEntity living) {
		return isEquipped(living, EquipmentSlot.HEAD, ModItems.AUGMENTED_HELMET);
	}

	public static boolean damageRubberBoots(ServerPlayerEntity player, int amount) {
		return damageEquipped(player, EquipmentSlot.FEET, ModItems.RUBBER_BOOTS, amount);
	}

	public static boolean damageHeavyPants(ServerPlayerEntity player, int amount) {
		return damageEquipped(player, EquipmentSlot.LEGS, ModItems.HEAVY_PANTS, amount);
	}

	private static boolean isEquipped(LivingEntity living, EquipmentSlot slot, Item target) {
		ItemStack stack = living.getEquippedStack(slot);
		return !stack.isEmpty() && stack.isOf(target);
	}

	private static boolean damageEquipped(ServerPlayerEntity player, EquipmentSlot slot, Item target, int amount) {
		ItemStack stack = player.getEquippedStack(slot);
		if (stack.isEmpty() || !stack.isOf(target)) {
			return false;
		}
		stack.damage(amount, player, slot);
		return true;
	}
}

