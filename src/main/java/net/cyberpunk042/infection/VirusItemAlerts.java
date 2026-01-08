package net.cyberpunk042.infection;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class VirusItemAlerts {
	private static volatile boolean initialized = false;
	
	private VirusItemAlerts() {
	}

	public static void init() {
		if (initialized) return;
		initialized = true;
		
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(world instanceof ServerWorld serverWorld)) {
				return;
			}
			if (!(entity instanceof ItemEntity item)) {
				return;
			}
			if (!item.getStack().isOf(ModBlocks.VIRUS_BLOCK.asItem())) {
				return;
			}
			if (item.age > 0) {
				return;
			}
			Entity owner = item.getOwner();
			if (owner instanceof ServerPlayerEntity player) {
				broadcastDrop(serverWorld, player);
			}
		});
	}

	public static void broadcastBurn(ServerWorld world) {
		if (!shouldAlert(world)) {
			return;
		}
		Text toast = Text.translatable("message.the-virus-block.inventory.burned");
		world.getServer().getPlayerManager().broadcast(toast, false);
	}

	public static void broadcastPickup(ServerWorld world, ServerPlayerEntity player) {
		if (!shouldAlert(world)) {
			return;
		}
		Text message = Text.translatable("message.the-virus-block.inventory.pickup", player.getDisplayName());
		world.getServer().getPlayerManager().broadcast(message, false);
	}

	private static void broadcastDrop(ServerWorld world, ServerPlayerEntity dropper) {
		if (!shouldAlert(world)) {
			return;
		}
		Text message = Text.translatable("message.the-virus-block.inventory.drop", dropper.getDisplayName());
		world.getServer().getPlayerManager().broadcast(message, false);
	}

	private static boolean shouldAlert(ServerWorld world) {
		if (VirusWorldState.get(world).infectionState().infected()) {
			return false;
		}
		return world.getGameRules().getBoolean(TheVirusBlock.VIRUS_VERBOSE_INVENTORY_ALERTS);
	}
}

