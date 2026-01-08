package net.cyberpunk042.util;

import net.minecraft.block.Block;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Provides runtime silk-touch drops when datapack loot tables fail to load.
 */
public final class SilkTouchFallbacks {
	private SilkTouchFallbacks() {
	}

	public static boolean dropSelfIfSilkTouch(Block block, ServerWorld world, BlockPos pos, ItemStack tool) {
		if (world == null || tool == null || tool.isEmpty()) {
			return false;
		}
		RegistryEntry.Reference<Enchantment> silkEntry = world.getRegistryManager()
				.getOrThrow(RegistryKeys.ENCHANTMENT)
				.getOrThrow(Enchantments.SILK_TOUCH);
		if (silkEntry == null || !hasSilkTouch(tool, silkEntry)) {
			return false;
		}
		Block.dropStack(world, pos, new ItemStack(block));
		return true;
	}

	private static boolean hasSilkTouch(ItemStack stack, RegistryEntry<Enchantment> reference) {
		ItemEnchantmentsComponent applied = stack.getEnchantments();
		return applied != null && applied.getLevel(reference) > 0;
	}
}


