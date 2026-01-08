package net.cyberpunk042.infection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public final class VirusInventoryAnnouncements {
	private static final int CHAT_INTERVAL_TICKS = 1500;
	private static final int TOAST_INTERVAL_TICKS = 1000;
	private static final Map<ServerWorld, Set<BlockEntity>> TRACKED_CONTAINERS = new WeakHashMap<>();
	
	private static volatile boolean initialized = false;

	private VirusInventoryAnnouncements() {
	}

	public static void init() {
		if (initialized) return;
		initialized = true;
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (!(world instanceof ServerWorld serverWorld) || !(blockEntity instanceof Inventory)) {
				return;
			}
			TRACKED_CONTAINERS.computeIfAbsent(serverWorld,
					key -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(blockEntity);
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
			if (!(world instanceof ServerWorld serverWorld) || !(blockEntity instanceof Inventory)) {
				return;
			}
			Set<BlockEntity> set = TRACKED_CONTAINERS.get(serverWorld);
			if (set != null) {
				set.remove(blockEntity);
				if (set.isEmpty()) {
					TRACKED_CONTAINERS.remove(serverWorld);
				}
			}
		});
	}

	public static void tick(ServerWorld world) {
		if (world.getRegistryKey() != World.OVERWORLD) {
			return;
		}
		if (VirusWorldState.get(world).infectionState().infected()) {
			return;
		}
		long time = world.getTime();
		boolean chatWindow = time % CHAT_INTERVAL_TICKS == 0;
		boolean toastWindow = time % TOAST_INTERVAL_TICKS == 0;
		if (!chatWindow && !toastWindow) {
			return;
		}
		MinecraftServer server = world.getServer();
		if (server == null) {
			return;
		}
		GameRules.BooleanRule verbose = world.getGameRules().get(TheVirusBlock.VIRUS_VERBOSE_INVENTORY_ALERTS);
		if (!verbose.get()) {
			return;
		}
		Map<String, Integer> playerCounts = countPerPlayer(server);
		Map<String, Integer> containerCounts = countPerContainer(world);
		if (playerCounts.isEmpty() && containerCounts.isEmpty()) {
			return;
		}
		if (chatWindow) {
			dispatchChatAlert(server, playerCounts, containerCounts);
		}
		if (toastWindow) {
			dispatchToast(world, sumCounts(playerCounts), sumCounts(containerCounts));
		}
	}

	private static void dispatchChatAlert(MinecraftServer server, Map<String, Integer> playerCounts, Map<String, Integer> containerCounts) {
		MutableText message = Text.literal("[Virus Alert] ").formatted(Formatting.DARK_AQUA);
		if (!playerCounts.isEmpty()) {
			message.append(Text.translatable("message.the-virus-block.inventory.verbose.players",
					formatEntries(playerCounts)).formatted(Formatting.GOLD));
		}
		if (!containerCounts.isEmpty()) {
			if (!playerCounts.isEmpty()) {
				message.append(Text.literal(" | ").formatted(Formatting.GRAY));
			}
			message.append(Text.translatable("message.the-virus-block.inventory.verbose.containers",
					formatEntries(containerCounts)).formatted(Formatting.YELLOW));
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			player.sendMessage(message, false);
		}
	}

	private static void dispatchToast(ServerWorld world, int playerTotal, int containerTotal) {
		Text title = Text.empty();
		Text subtitle = Text.empty();
		if (playerTotal > 0) {
			title = playerTotal == 1
					? Text.translatable("message.the-virus-block.inventory.single")
					: Text.translatable("message.the-virus-block.inventory.plural", playerTotal);
		}
		if (containerTotal > 0) {
			Text containerText = containerTotal == 1
					? Text.translatable("message.the-virus-block.inventory.container.single")
					: Text.translatable("message.the-virus-block.inventory.container.plural", containerTotal);
			if (title.getString().isEmpty()) {
				title = containerText;
			} else {
				subtitle = containerText;
			}
		}
		if (title.getString().isEmpty()) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			player.sendMessage(title.copy().formatted(Formatting.GRAY), true);
			if (!subtitle.getString().isEmpty()) {
				player.sendMessage(subtitle.copy().formatted(Formatting.DARK_GRAY), true);
			}
		}
	}

	private static Map<String, Integer> countPerPlayer(MinecraftServer server) {
		Item virusItem = ModBlocks.VIRUS_BLOCK.asItem();
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			int total = player.getInventory().count(virusItem);
			if (total > 0) {
				counts.put(player.getGameProfile().getName(), total);
			}
		}
		return counts;
	}

	private static Map<String, Integer> countPerContainer(ServerWorld world) {
		Item virusItem = ModBlocks.VIRUS_BLOCK.asItem();
		Map<String, Integer> counts = new LinkedHashMap<>();
		Set<BlockEntity> tracked = TRACKED_CONTAINERS.get(world);
		if (tracked == null || tracked.isEmpty()) {
			return counts;
		}
		for (BlockEntity blockEntity : tracked) {
			if (blockEntity.isRemoved() || !(blockEntity instanceof Inventory inventory)) {
				continue;
			}
			String label = blockEntity.getCachedState().getBlock().getName().getString();
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (!stack.isEmpty() && stack.isOf(virusItem)) {
					counts.merge(label, stack.getCount(), Integer::sum);
				}
			}
		}
		return counts;
	}

	private static String formatEntries(Map<String, Integer> counts) {
		return counts.entrySet().stream()
				.map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
				.collect(Collectors.joining(", "));
	}

	private static int sumCounts(Map<String, Integer> counts) {
		return counts.values().stream().mapToInt(Integer::intValue).sum();
	}
}
