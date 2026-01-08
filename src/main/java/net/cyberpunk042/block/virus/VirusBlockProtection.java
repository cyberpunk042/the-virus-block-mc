package net.cyberpunk042.block.virus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class VirusBlockProtection {
	private static final int COOLDOWN_TICKS = 10;

	private static final Map<UUID, Long> blockedUntil = new ConcurrentHashMap<>();
	
	private static volatile boolean initialized = false;

	private VirusBlockProtection() {
	}

	public static void init() {
		if (initialized) return;
		initialized = true;
		
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerWorld serverWorld)
					|| !(player instanceof ServerPlayerEntity serverPlayer)
					|| !(state.isOf(ModBlocks.VIRUS_BLOCK) || state.isOf(ModBlocks.SINGULARITY_BLOCK))) {
				return true;
			}

			VirusWorldState infection = VirusWorldState.get(serverWorld);
			if (shouldBlockBreak(serverWorld, serverPlayer, infection)) {
				resyncBlock(serverPlayer, serverWorld, pos, state);
				return false;
			}

			return true;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> blockedUntil.remove(handler.player.getUuid()));
	}

	public static void recordBlockedAttempt(ServerPlayerEntity player, ServerWorld world) {
		blockedUntil.put(player.getUuid(), world.getTime() + COOLDOWN_TICKS);
	}

	private static boolean shouldBlockBreak(ServerWorld world, ServerPlayerEntity player, VirusWorldState infection) {
		long now = world.getTime();
		UUID uuid = player.getUuid();
		Long lock = blockedUntil.get(uuid);
		if (lock != null) {
			if (now < lock) {
				return true;
			}
			// Remove expired entry to prevent unbounded map growth
			blockedUntil.remove(uuid);
		}

		boolean blocked = !player.isCreative() && !infection.tiers().isApocalypseMode();
		if (blocked) {
			recordBlockedAttempt(player, world);
		}

		return blocked;
	}

	private static void resyncBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
		world.getChunkManager().markForUpdate(pos);
		player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, state));
	}
}

