package net.cyberpunk042.item;

import net.cyberpunk042.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Utility helpers for Purification Totem logic.
 */
public final class PurificationTotemUtil {

	private PurificationTotemUtil() {
	}

	public static boolean isHolding(ServerPlayerEntity player) {
		return isTotem(player.getMainHandStack()) || isTotem(player.getOffHandStack());
	}

	private static boolean isTotem(ItemStack stack) {
		return stack.isOf(ModItems.PURIFICATION_TOTEM);
	}
}

