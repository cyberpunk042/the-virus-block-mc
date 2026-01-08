package net.cyberpunk042.infection.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.profile.CollapseSyncProfile;
import net.cyberpunk042.infection.profile.DimensionProfile;
import net.cyberpunk042.network.DifficultySyncPayload;
import net.cyberpunk042.network.SingularitySchedulePayload;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Coordinates presentation/broadcast helpers that used to live on {@link VirusWorldState}.
 */
public final class PresentationCoordinatorService {

	private final VirusWorldState host;

	public PresentationCoordinatorService(VirusWorldState host) {
		this.host = host;
	}

	public void broadcastCollapseSchedule() {
		ServerWorld world = host.world();
		if (!isCollapseActive()) {
			return;
		}
		SingularitySchedulePayload payload = buildSchedulePayload();
		if (payload == null) {
			return;
		}
		presentation().broadcastPayload(host.world(),
				payload,
				player -> player.isAlive()
						&& !player.isSpectator()
						&& getCollapseProfile(player) == CollapseSyncProfile.CINEMATIC);
	}

	public void sendCollapseSchedule(ServerPlayerEntity player) {
		if (player == null || !isCollapseActive()) {
			return;
		}
		SingularitySchedulePayload payload = buildSchedulePayload();
		if (payload == null) {
			return;
		}
		presentation().sendPayload(player, payload);
	}

	public void updateBossBars() {
		ServerWorld world = host.world();
		presentation().updateBossBars(host.world(), host);
	}

	public void syncDifficulty() {
		ServerWorld world = host.world();
		DifficultySyncPayload payload = new DifficultySyncPayload(host.tiers().difficulty());
		presentation().broadcastPayload(host.world(), payload);
	}

	public void pushPlayersFromBlock(BlockPos formingPos,
			int radius,
			double strengthScale,
			boolean spawnGuardian) {
		ServerWorld world = host.world();
		double difficultyKnockback = host.tiers().difficulty().getKnockbackMultiplier();
		if (difficultyKnockback <= 0.0D) {
			return;
		}
		GuardianFxService.GuardianResult result = presentation().pushPlayers(host.world(),
				formingPos,
				radius,
				strengthScale,
				spawnGuardian,
				difficultyKnockback,
				PlayerEntity::isAlive,
				host.orchestrator().services().effectBusOrNoop());
		if (result.anyAffected()) {
			world.playSound(null,
					formingPos,
					SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),
					SoundCategory.HOSTILE,
					1.25F,
					0.55F);
		}
	}

	public boolean isChunkWithinBroadcastRadius(ChunkPos chunk, int radius) {
		ServerWorld world = host.world();
		double thresholdSq = radius * (double) radius;
		double centerX = chunk.getCenterX();
		double centerZ = chunk.getCenterZ();
		for (ServerPlayerEntity player : world.getPlayers(player -> player.isAlive() && !player.isSpectator())) {
			if (!shouldPlayerTriggerBroadcast(player)) {
				continue;
			}
			double dx = player.getX() - centerX;
			double dz = player.getZ() - centerZ;
			if (dx * dx + dz * dz <= thresholdSq) {
				return true;
			}
		}
		return false;
	}

	public void pushEntitiesTowardRing(double radius, BlockPos centerPos) {
		ServerWorld world = host.world();
		if (centerPos == null) {
			return;
		}
		Vec3d center = Vec3d.ofCenter(centerPos);
		Box range = new Box(centerPos).expand(radius);
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, range, Entity::isAlive)) {
			Vec3d delta = center.subtract(living.getPos());
			double distance = delta.length();
			if (distance < 0.1D) {
				continue;
			}
			double modifier = MathHelper.clamp(1.0D - (distance / (radius + 4.0D)), 0.0D, 1.0D);
			if (modifier <= 0.0D) {
				continue;
			}
			Vec3d impulse = delta.normalize().multiply(host.collapseConfig().getRingPullStrength() * modifier);
			living.addVelocity(impulse.x, impulse.y * 0.2D, impulse.z);
			living.velocityDirty = true;
			living.velocityModified = true;
		}
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, range, entity -> !entity.isRemoved())) {
			Vec3d delta = center.subtract(item.getPos());
			double distance = delta.length();
			if (distance < 0.1D) {
				continue;
			}
			double modifier = MathHelper.clamp(1.0D - (distance / (radius + 4.0D)), 0.0D, 1.0D);
			if (modifier <= 0.0D) {
				continue;
			}
			Vec3d impulse = delta.normalize().multiply(host.collapseConfig().getRingPullStrength() * 0.5D * modifier);
			item.addVelocity(impulse.x, impulse.y * 0.1D, impulse.z);
			item.velocityDirty = true;
		}
	}

	public void syncProfileOnJoin(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		if (getCollapseProfile(player) == CollapseSyncProfile.CINEMATIC && isCollapseActive()) {
			sendCollapseSchedule(player);
		}
	}

	private SingularityPresentationService presentation() {
		return host.collapseModule().watchdog().presentationService();
	}

	private SingularitySchedulePayload buildSchedulePayload() {
		// Ring visualization removed - return empty schedule
		List<SingularitySchedulePayload.RingEntry> rings = new ArrayList<>();
		if (rings.isEmpty()) {
			return null;
		}
		return new SingularitySchedulePayload(rings);
	}

	private int resolveTickIntervalForSide(int sideLength) {
		for (DimensionProfile.Collapse.RadiusDelay delay : host.collapseConfig().configuredRadiusDelays()) {
			if (sideLength <= delay.side()) {
				return Math.max(1, delay.ticks());
			}
		}
		return host.collapseConfig().configuredCollapseTickInterval();
	}

	private boolean isCollapseActive() {
		return host.singularityState().singularityState == SingularityState.FUSING
				|| host.singularityState().singularityState == SingularityState.COLLAPSE
				|| host.singularityState().singularityState == SingularityState.CORE
				|| host.singularityState().singularityState == SingularityState.RING
				|| host.singularityState().singularityState == SingularityState.DISSIPATION;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Profile Management (extracted from VirusWorldState)
	// ─────────────────────────────────────────────────────────────────────────────

	public CollapseSyncProfile getCollapseProfile(ServerPlayerEntity player) {
		return getCollapseProfile(player.getUuid());
	}

	private CollapseSyncProfile getCollapseProfile(UUID uuid) {
		CollapseSyncProfile profile = host.singularityState().singularityPlayerProfiles.get(uuid);
		return profile != null ? profile : host.collapseConfig().configuredDefaultSyncProfile();
	}

	public void setCollapseProfile(ServerPlayerEntity player, CollapseSyncProfile profile) {
		if (profile == null) {
			profile = host.collapseConfig().configuredDefaultSyncProfile();
		}
		UUID uuid = player.getUuid();
		CollapseSyncProfile defaultProfile = host.collapseConfig().configuredDefaultSyncProfile();
		CollapseSyncProfile previous = host.singularityState().singularityPlayerProfiles.get(uuid);
		if (profile == defaultProfile) {
			if (previous != null) {
				host.singularityState().singularityPlayerProfiles.remove(uuid);
				host.markDirty();
			}
		} else if (previous != profile) {
			host.singularityState().singularityPlayerProfiles.put(uuid, profile);
			host.markDirty();
		}
		if (profile == CollapseSyncProfile.CINEMATIC) {
			sendCollapseSchedule(player);
		}
	}

	public boolean shouldPlayerTriggerBroadcast(ServerPlayerEntity player) {
		return getCollapseProfile(player) != CollapseSyncProfile.MINIMAL;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Profile Snapshot (for persistence)
	// ─────────────────────────────────────────────────────────────────────────────

	public Map<String, String> getProfileSnapshot() {
		if (host.singularityState().singularityPlayerProfiles.isEmpty()) {
			return Map.of();
		}
		Map<String, String> snapshot = new HashMap<>();
		ObjectIterator<Object2ObjectMap.Entry<UUID, CollapseSyncProfile>> iterator =
				host.singularityState().singularityPlayerProfiles.object2ObjectEntrySet().iterator();
		while (iterator.hasNext()) {
			Object2ObjectMap.Entry<UUID, CollapseSyncProfile> entry = iterator.next();
			snapshot.put(entry.getKey().toString(), entry.getValue().id());
		}
		return snapshot;
	}

	public void applyProfileSnapshot(Map<String, String> entries) {
		host.singularityState().singularityPlayerProfiles.clear();
		if (entries == null || entries.isEmpty()) {
			return;
		}
		entries.forEach(this::applyProfileSnapshotEntry);
	}

	private void applyProfileSnapshotEntry(String uuidString, String profileId) {
		try {
			UUID uuid = UUID.fromString(uuidString);
			CollapseSyncProfile profile = CollapseSyncProfile.fromId(profileId);
			if (profile != null && profile != host.collapseConfig().configuredDefaultSyncProfile()) {
				host.singularityState().singularityPlayerProfiles.put(uuid, profile);
			}
		} catch (IllegalArgumentException ignored) {
		}
	}
}

