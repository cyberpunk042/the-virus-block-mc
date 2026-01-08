package net.cyberpunk042.infection;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

public enum TierFeatureGroup {
	CORE(null),
	TIER2_EVENT(TheVirusBlock.VIRUS_TIER2_EVENTS_ENABLED),
	TIER3_EXTRA(TheVirusBlock.VIRUS_TIER3_EXTRAS_ENABLED);

	private final @Nullable GameRules.Key<GameRules.BooleanRule> toggleRule;

	TierFeatureGroup(@Nullable GameRules.Key<GameRules.BooleanRule> toggleRule) {
		this.toggleRule = toggleRule;
	}

	public boolean isGroupEnabled(ServerWorld world) {
		return toggleRule == null || world.getGameRules().getBoolean(toggleRule);
	}
}

