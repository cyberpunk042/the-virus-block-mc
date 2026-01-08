package net.cyberpunk042.infection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.server.world.ServerWorld;

public final class TierCookbook {
	private TierCookbook() {
	}

	public static boolean isEnabled(ServerWorld world, InfectionTier tier, boolean apocalypseMode, TierFeature feature) {
		if (!feature.getGroup().isGroupEnabled(world)) {
			return false;
		}
		if (!apocalypseMode && tier.isBelow(feature.getMinTier())) {
			return false;
		}
		return feature.getToggleRule().map(rule -> world.getGameRules().getBoolean(rule)).orElse(true);
	}

	public static boolean anyEnabled(ServerWorld world, InfectionTier tier, boolean apocalypseMode, TierFeatureGroup group) {
		for (TierFeature feature : TierFeature.values()) {
			if (feature.getGroup() == group && isEnabled(world, tier, apocalypseMode, feature)) {
				return true;
			}
		}
		return false;
	}

	public static EnumSet<TierFeature> activeFeatures(ServerWorld world, InfectionTier tier, boolean apocalypseMode) {
		EnumSet<TierFeature> set = EnumSet.noneOf(TierFeature.class);
		for (TierFeature feature : TierFeature.values()) {
			if (isEnabled(world, tier, apocalypseMode, feature)) {
				set.add(feature);
			}
		}
		return set;
	}

	public static EnumMap<InfectionTier, List<TierFeature>> defaultPlan() {
		EnumMap<InfectionTier, List<TierFeature>> map = new EnumMap<>(InfectionTier.class);
		for (InfectionTier tier : InfectionTier.values()) {
			map.put(tier, new ArrayList<>());
		}
		for (TierFeature feature : TierFeature.values()) {
			map.computeIfAbsent(feature.getMinTier(), key -> new ArrayList<>()).add(feature);
		}
		map.values().forEach(list -> list.sort(Comparator.comparing(TierFeature::getId)));
		return map;
	}
}

