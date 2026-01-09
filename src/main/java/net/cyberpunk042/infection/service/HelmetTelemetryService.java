package net.cyberpunk042.infection.service;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.network.HelmetHudPayload;
import net.cyberpunk042.util.VirusEquipmentHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks augmented helmet HUD data for players wearing the helmet.
 * 
 * <p>Sends {@link HelmetHudPayload} to clients at 10Hz (every 2 ticks) with
 * auto-drop on high latency to avoid making lag worse.</p>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Direction and distance to nearest threat</li>
 *   <li>Signal strength (inverse of distance)</li>
 *   <li>Total threat count</li>
 *   <li>Nearby positions for 2D markers</li>
 *   <li>Particle trail on ping interval</li>
 * </ul>
 */
public final class HelmetTelemetryService {

	// HUD update interval (every 2 ticks = 10Hz)
	private static final int HUD_UPDATE_INTERVAL = 2;
	
	// Particle ping interval (every 80 ticks = 4 seconds)
	private static final int PARTICLE_PING_INTERVAL = 80;
	
	private static final double HELMET_PING_MAX_PARTICLE_DISTANCE = 32.0D;
	
	// Latency threshold for packet dropping (ms) - be generous to avoid NO SIGNAL
	private static final long LATENCY_THRESHOLD_MS = 500;
	private static final int MAX_SKIP_CYCLES = 2;  // Force send at least every 2 cycles
	
	private final VirusWorldState host;
	private int hudTickCounter = 0;
	private int skipCount = 0;

	public HelmetTelemetryService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void tick() {
		ServerWorld world = host.world();
		Object2IntMap<UUID> helmetTimers = host.infectionState().helmetPingTimers();
		Object2DoubleMap<UUID> heavyPantsWear = host.infectionState().heavyPantsVoidWear();
		
		// If no infection, clear state and skip
		if (!host.infectionState().infected()) {
			if (!helmetTimers.isEmpty()) helmetTimers.clear();
			if (!heavyPantsWear.isEmpty()) heavyPantsWear.clear();
			return;
		}
		
		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (players.isEmpty()) {
			if (!helmetTimers.isEmpty()) helmetTimers.clear();
			return;
		}
		
		hudTickCounter++;
		boolean shouldSendHud = hudTickCounter >= HUD_UPDATE_INTERVAL;
		if (shouldSendHud) {
			hudTickCounter = 0;
		}
		
		// Check latency for adaptive packet dropping
		boolean forcePacket = skipCount >= MAX_SKIP_CYCLES;
		if (forcePacket) {
			skipCount = 0;
		}
		
		int trackedCount = 0;
		for (ServerPlayerEntity player : players) {
			// Skip spectators, but allow creative mode (useful for testing)
			if (player.isSpectator() || !VirusEquipmentHelper.hasAugmentedHelmet(player)) {
				helmetTimers.removeInt(player.getUuid());
				continue;
			}
			
			UUID uuid = player.getUuid();
			trackedCount++;
			int ticks = helmetTimers.getOrDefault(uuid, 0) + 1;
			helmetTimers.put(uuid, ticks);
			
			// Send HUD update (10Hz with latency check)
			if (shouldSendHud) {
				boolean dropPacket = shouldDropPacket(player) && !forcePacket;
				if (dropPacket) {
					skipCount++;
				} else {
					sendHudPayload(player);
					skipCount = 0;
				}
			}
			
			// Particle ping (removed - using client-side 3D outlines now)
			if (ticks >= PARTICLE_PING_INTERVAL) {
				helmetTimers.put(uuid, 0);
				// spawnParticlePing(world, player);
			}
		}
		
		// Clean stale entries
		if (helmetTimers.size() > trackedCount) {
			Set<UUID> activeUuids = new HashSet<>(trackedCount);
			for (ServerPlayerEntity p : players) {
				if (!p.isSpectator() && VirusEquipmentHelper.hasAugmentedHelmet(p)) {
					activeUuids.add(p.getUuid());
				}
			}
			helmetTimers.object2IntEntrySet().removeIf(entry -> !activeUuids.contains(entry.getKey()));
		}
	}

	/**
	 * Sends the HUD payload to a player.
	 * Only contains helmet-specific data (yaw, signal strength).
	 */
	private void sendHudPayload(ServerPlayerEntity player) {
		Vec3d playerPos = player.getEyePos();
		
		List<BlockPos> sources = new ArrayList<>(host.getVirusSources());
		int totalCount = sources.size();
		
		if (sources.isEmpty()) {
			ServerPlayNetworking.send(player, HelmetHudPayload.empty());
			return;
		}
		
		// Sort by distance to find nearest
		sources.sort(Comparator.comparingDouble(pos -> 
			pos.toCenterPos().squaredDistanceTo(playerPos)));
		
		// Nearest target
		BlockPos nearest = sources.get(0);
		Vec3d targetVec = Vec3d.ofCenter(nearest);
		Vec3d delta = targetVec.subtract(playerPos);
		float distance = (float) delta.length();
		
		// Calculate yaw angle for direction arrow
		float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
		if (yaw < 0) yaw += 360;
		
		// Signal strength for helmet indicator
		float signalStrength = HelmetHudPayload.computeSignalStrength(distance);
		
		HelmetHudPayload payload = new HelmetHudPayload(
			yaw,
			signalStrength,
			totalCount,
			true
		);
		
		ServerPlayNetworking.send(player, payload);
	}
	
	/**
	 * Spawns particle trail on ping interval.
	 */
	private void spawnParticlePing(ServerWorld world, ServerPlayerEntity player) {
		BlockPos target = findNearestVirusSource(player.getBlockPos());
		if (target == null) {
			return;
		}
		
		Vec3d eye = player.getEyePos();
		Vec3d delta = Vec3d.ofCenter(target).subtract(eye);
		double distance = delta.length();
		
		if (distance < 0.5D) {
			return;
		}
		
		double visualDistance = Math.min(distance, HELMET_PING_MAX_PARTICLE_DISTANCE);
		spawnHelmetTrail(world, eye, delta, visualDistance);
		
		// Play subtle ping sound
		world.playSound(null, player.getX(), player.getEyeY(), player.getZ(), 
			SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 0.25F, 1.8F);
	}

	@Nullable
	private BlockPos findNearestVirusSource(BlockPos origin) {
		if (!host.hasVirusSources()) {
			return null;
		}
		Vec3d originVec = Vec3d.ofCenter(origin);
		BlockPos closest = null;
		double best = Double.MAX_VALUE;
		for (BlockPos source : host.getVirusSources()) {
			double distanceSq = source.toCenterPos().squaredDistanceTo(originVec);
			if (distanceSq < best) {
				best = distanceSq;
				closest = source;
			}
		}
		return closest;
	}

	private void spawnHelmetTrail(ServerWorld world, Vec3d eye, Vec3d delta, double maxDistance) {
		if (maxDistance <= 0.0D) {
			return;
		}
		Vec3d direction = delta.normalize();
		int steps = Math.max(4, MathHelper.floor(maxDistance / 2.0D));
		double step = maxDistance / steps;
		for (int i = 1; i <= steps; i++) {
			Vec3d point = eye.add(direction.multiply(step * i));
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}
	
	/**
	 * Checks if packet should be dropped due to high latency.
	 */
	private boolean shouldDropPacket(ServerPlayerEntity player) {
		int ping = player.networkHandler.getLatency();
		return ping > LATENCY_THRESHOLD_MS;
	}
}
