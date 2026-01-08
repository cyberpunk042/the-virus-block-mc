package net.cyberpunk042.client.color;

import java.util.Map;

import net.cyberpunk042.client.render.VirusSkyClientState;
import net.cyberpunk042.config.ColorConfig;
import net.cyberpunk042.config.ColorConfig.ColorSlot;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public final class CorruptedColorProviders {
	private static final Map<Block, ColorSlot> COLOR_MAP = Map.of(
			ModBlocks.CORRUPTED_CRYING_OBSIDIAN, ColorSlot.CORRUPTED_CRYING_OBSIDIAN,
			ModBlocks.CORRUPTED_DIAMOND, ColorSlot.CORRUPTED_DIAMOND,
			ModBlocks.CORRUPTED_GOLD, ColorSlot.CORRUPTED_GOLD
	);

	private CorruptedColorProviders() {
	}

	public static void register() {
		COLOR_MAP.forEach((block, slot) ->
				ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> ColorConfig.argb(slot), block));
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (!VirusSkyClientState.areFluidsCorrupted()) {
				return 0xFFFFFFFF;
			}
			if (state.isOf(Blocks.SOUL_FIRE) || state.isOf(Blocks.SOUL_CAMPFIRE)) {
				return ColorConfig.argb(ColorSlot.CORRUPTED_SOUL_FIRE);
			}
			return ColorConfig.argb(ColorSlot.CORRUPTED_FIRE);
		}, Blocks.FIRE, Blocks.CAMPFIRE, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE);
	}
}

