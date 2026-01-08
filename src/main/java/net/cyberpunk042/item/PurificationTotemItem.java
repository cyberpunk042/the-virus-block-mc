package net.cyberpunk042.item;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.screen.handler.PurificationTotemScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class PurificationTotemItem extends Item {
	public PurificationTotemItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.SUCCESS;
		}
		VirusWorldState state = VirusWorldState.get(serverWorld);
		if (!state.infectionState().infected()) {
			user.sendMessage(Text.translatable("message.the-virus-block.purification_totem.inactive"), true);
			return ActionResult.FAIL;
		}

		user.openHandledScreen(new Factory(hand));
		return ActionResult.SUCCESS;
	}

	private static class Factory implements NamedScreenHandlerFactory {
		private final Hand hand;

		Factory(Hand hand) {
			this.hand = hand;
		}

		@Override
		public Text getDisplayName() {
			return Text.translatable("screen.the-virus-block.purification_totem.title");
		}

		@Override
		public PurificationTotemScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
			return new PurificationTotemScreenHandler(syncId, playerInventory, hand);
		}
	}
}

