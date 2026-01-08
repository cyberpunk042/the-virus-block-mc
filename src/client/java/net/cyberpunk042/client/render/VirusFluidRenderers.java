package net.cyberpunk042.client.render;

import net.cyberpunk042.config.ColorConfig;
import net.cyberpunk042.config.ColorConfig.ColorSlot;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public final class VirusFluidRenderers {
	private VirusFluidRenderers() {
	}

	public static void register() {
		final FluidRenderHandler vanillaLava = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.LAVA);
		final FluidRenderHandler vanillaWater = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER);
		if (vanillaLava == null || vanillaWater == null) {
			return;
		}

		FluidRenderHandler lavaHandler = new FluidRenderHandler() {
			@Override
			public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
				return vanillaLava.getFluidSprites(view, pos, state);
			}

			@Override
			public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
				if (VirusSkyClientState.areFluidsCorrupted()) {
					return ColorConfig.argb(ColorSlot.CORRUPTED_LAVA);
				}
				return vanillaLava.getFluidColor(view, pos, state);
			}
		};

		FluidRenderHandler waterHandler = new FluidRenderHandler() {
			@Override
			public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
				return vanillaWater.getFluidSprites(view, pos, state);
			}

			@Override
			public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
				if (VirusSkyClientState.areFluidsCorrupted()) {
					return ColorConfig.argb(ColorSlot.CORRUPTED_WATER);
				}
				return vanillaWater.getFluidColor(view, pos, state);
			}
		};

		FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, Fluids.FLOWING_LAVA, lavaHandler);
		FluidRenderHandlerRegistry.INSTANCE.register(Fluids.WATER, Fluids.FLOWING_WATER, waterHandler);
	}
}

