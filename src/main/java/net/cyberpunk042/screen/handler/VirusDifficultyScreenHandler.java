package net.cyberpunk042.screen.handler;

import net.cyberpunk042.screen.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class VirusDifficultyScreenHandler extends ScreenHandler {
	public VirusDifficultyScreenHandler(int syncId, PlayerInventory inventory) {
		super(ModScreenHandlers.VIRUS_DIFFICULTY, syncId);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}
}

