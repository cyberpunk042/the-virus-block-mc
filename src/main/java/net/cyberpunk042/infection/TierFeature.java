package net.cyberpunk042.infection;

import java.util.Locale;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.world.GameRules;

public enum TierFeature {
	LIQUID_CORRUPTION("liquid_corruption", InfectionTier.TWO, TierFeatureGroup.CORE, TheVirusBlock.VIRUS_LIQUID_MUTATION_ENABLED),
	CORRUPT_SAND("corrupt_sand", InfectionTier.TWO, TierFeatureGroup.CORE, TheVirusBlock.VIRUS_CORRUPT_SAND_ENABLED),
	CORRUPT_ICE("corrupt_ice", InfectionTier.TWO, TierFeatureGroup.CORE, TheVirusBlock.VIRUS_CORRUPT_ICE_ENABLED),
	CORRUPT_SNOW("corrupt_snow", InfectionTier.TWO, TierFeatureGroup.CORE, TheVirusBlock.VIRUS_CORRUPT_SNOW_ENABLED),

	EVENT_MUTATION_PULSE("mutation_pulse", InfectionTier.THREE, TierFeatureGroup.TIER2_EVENT, TheVirusBlock.VIRUS_EVENT_MUTATION_PULSE_ENABLED),
	EVENT_SKYFALL("skyfall", InfectionTier.ONE, TierFeatureGroup.TIER2_EVENT, TheVirusBlock.VIRUS_EVENT_SKYFALL_ENABLED),
	EVENT_COLLAPSE_SURGE("collapse_surge", InfectionTier.TWO, TierFeatureGroup.TIER2_EVENT, TheVirusBlock.VIRUS_EVENT_COLLAPSE_SURGE_ENABLED),
	EVENT_PASSIVE_REVOLT("passive_revolt", InfectionTier.THREE, TierFeatureGroup.TIER2_EVENT, TheVirusBlock.VIRUS_EVENT_PASSIVE_REVOLT_ENABLED),
	EVENT_MOB_BUFF_STORM("mob_buff_storm", InfectionTier.FOUR, TierFeatureGroup.TIER2_EVENT, TheVirusBlock.VIRUS_EVENT_MOB_BUFF_STORM_ENABLED),
	EVENT_VIRUS_BLOOM("virus_bloom", InfectionTier.FIVE, TierFeatureGroup.TIER2_EVENT, TheVirusBlock.VIRUS_EVENT_VIRUS_BLOOM_ENABLED),

	EVENT_VOID_TEAR("void_tear", InfectionTier.THREE, TierFeatureGroup.TIER3_EXTRA, TheVirusBlock.VIRUS_EVENT_VOID_TEAR_ENABLED),
	EVENT_INVERSION("inversion", InfectionTier.FOUR, TierFeatureGroup.TIER3_EXTRA, TheVirusBlock.VIRUS_EVENT_INVERSION_ENABLED),
	EVENT_ENTITY_DUPLICATION("entity_duplication", InfectionTier.FOUR, TierFeatureGroup.TIER3_EXTRA, TheVirusBlock.VIRUS_EVENT_ENTITY_DUPLICATION_ENABLED);

	private final String id;
	private final InfectionTier minTier;
	private final TierFeatureGroup group;
	private final @Nullable GameRules.Key<GameRules.BooleanRule> toggleRule;

	TierFeature(String id, InfectionTier minTier, TierFeatureGroup group, @Nullable GameRules.Key<GameRules.BooleanRule> toggleRule) {
		this.id = id.toLowerCase(Locale.ROOT);
		this.minTier = minTier;
		this.group = group;
		this.toggleRule = toggleRule;
	}

	public String getId() {
		return id;
	}

	public InfectionTier getMinTier() {
		return minTier;
	}

	public TierFeatureGroup getGroup() {
		return group;
	}

	public Optional<GameRules.Key<GameRules.BooleanRule>> getToggleRule() {
		return Optional.ofNullable(toggleRule);
	}
}

