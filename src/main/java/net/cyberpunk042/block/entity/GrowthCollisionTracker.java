package net.cyberpunk042.block.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class GrowthCollisionTracker {
	private static final ConcurrentHashMap<RegistryKey<World>, CopyOnWriteArraySet<ProgressiveGrowthBlockEntity>> ACTIVE =
			new ConcurrentHashMap<>();
	
	// Ultra-fast global check - if this is 0, skip ALL collision logic
	private static volatile int globalCount = 0;

	private GrowthCollisionTracker() {
	}
	
	/**
	 * Returns true if ANY growth blocks with collision are registered anywhere.
	 * This is an O(1) check suitable for hot paths like collision detection.
	 */
	public static boolean hasAny() {
		return globalCount > 0;
	}
	
	/**
	 * Returns true if any growth blocks with collision exist in the given world.
	 * Slightly more expensive than hasAny() but still very fast.
	 */
	public static boolean hasAnyInWorld(World world) {
		if (globalCount == 0) return false;
		if (!(world instanceof ServerWorld serverWorld)) return false;
		Set<ProgressiveGrowthBlockEntity> set = ACTIVE.get(serverWorld.getRegistryKey());
		return set != null && !set.isEmpty();
	}

	public static void register(ProgressiveGrowthBlockEntity entity) {
		if (!(entity.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		boolean added = ACTIVE
			.computeIfAbsent(serverWorld.getRegistryKey(), key -> new CopyOnWriteArraySet<>())
			.add(entity);
		if (added) {
			globalCount++;
		}
	}

	public static void unregister(ProgressiveGrowthBlockEntity entity) {
		if (!(entity.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		CopyOnWriteArraySet<ProgressiveGrowthBlockEntity> set = ACTIVE.get(serverWorld.getRegistryKey());
		if (set == null) {
			return;
		}
		boolean removed = set.remove(entity);
		if (removed) {
			globalCount--;
		}
		if (set.isEmpty()) {
			ACTIVE.remove(serverWorld.getRegistryKey(), set);
		}
	}

	public static Collection<ProgressiveGrowthBlockEntity> active(World world) {
		if (globalCount == 0) return Collections.emptyList(); // Fast path
		if (!(world instanceof ServerWorld serverWorld)) {
			return Collections.emptyList();
		}
		Set<ProgressiveGrowthBlockEntity> set = ACTIVE.get(serverWorld.getRegistryKey());
		return set != null ? set : Collections.emptyList();
	}

	public static void forEachActive(Consumer<ProgressiveGrowthBlockEntity> consumer) {
		if (consumer == null) {
			return;
		}
		for (Map.Entry<RegistryKey<World>, CopyOnWriteArraySet<ProgressiveGrowthBlockEntity>> entry : ACTIVE.entrySet()) {
			for (ProgressiveGrowthBlockEntity entity : entry.getValue()) {
				if (entity == null || entity.isRemoved()) {
					continue;
				}
				consumer.accept(entity);
			}
		}
	}
}

