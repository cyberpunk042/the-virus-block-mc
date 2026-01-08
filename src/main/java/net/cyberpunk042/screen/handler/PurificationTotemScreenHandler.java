package net.cyberpunk042.screen.handler;

import net.cyberpunk042.screen.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;

public class PurificationTotemScreenHandler extends ScreenHandler {
	private final Hand hand;

	public PurificationTotemScreenHandler(int syncId, PlayerInventory inventory) {
		this(syncId, inventory, Hand.MAIN_HAND);
	}

	public PurificationTotemScreenHandler(int syncId, PlayerInventory inventory, Hand hand) {
		super(ModScreenHandlers.PURIFICATION_TOTEM, syncId);
		this.hand = hand;
	}

	public Hand getHand() {
		return hand;
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

