package net.cyberpunk042.infection.service;

import java.util.function.Predicate;

import net.cyberpunk042.infection.VirusTierBossBar;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.network.SingularityBorderPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Handles HUD/Bossbar synchronization and singularity border payload dispatch so
 * {@link VirusWorldState} no longer has to poke networking helpers directly.
 */
public class SingularityHudService {
	public void updateBossBars(ServerWorld world, VirusWorldState state) {
		if (world == null || state == null) {
			return;
		}
		VirusTierBossBar.update(world, state);
	}

	public void syncBorder(ServerWorld world, BorderSyncData data) {
		if (world == null || data == null) {
			return;
		}
		SingularityBorderPayload payload = new SingularityBorderPayload(
				data.active(),
				data.centerX(),
				data.centerZ(),
				data.initialDiameter(),
				data.currentDiameter(),
				data.targetDiameter(),
				data.duration(),
				data.elapsed(),
				data.phase());
		broadcastPayload(world, payload, ALIVE_PLAYERS);
	}

	public void broadcastPayload(ServerWorld world, CustomPayload payload) {
		broadcastPayload(world, payload, ALIVE_PLAYERS);
	}

	public void broadcastPayload(ServerWorld world, CustomPayload payload, Predicate<ServerPlayerEntity> filter) {
		if (world == null || payload == null) {
			return;
		}
		Predicate<ServerPlayerEntity> predicate = filter != null ? filter : ALIVE_PLAYERS;
		for (ServerPlayerEntity player : world.getPlayers(predicate)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public void sendPayload(ServerPlayerEntity player, CustomPayload payload) {
		if (player == null || payload == null) {
			return;
		}
		ServerPlayNetworking.send(player, payload);
	}

	private static final Predicate<ServerPlayerEntity> ALIVE_PLAYERS = PlayerEntity::isAlive;

	public record BorderSyncData(
			boolean active,
			double centerX,
			double centerZ,
			double initialDiameter,
			double currentDiameter,
			double targetDiameter,
			long duration,
			long elapsed,
			String phase) {
	}
}

