package net.cyberpunk042.infection.service;

import java.util.ArrayList;
import java.util.List;

import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.scenario.ScenarioEffectSet;
import net.cyberpunk042.log.Logging;

/**
 * Tracks effect-set registrations per scenario so {@link EffectBus} usage stays
 * coordinated. Eventually this can grow into a full effect manager (pooling,
 * async dispatch, etc.), but for now it keeps lifecycle bookkeeping out of
 * {@code VirusWorldState}.
 */
public final class EffectService {
	private final List<ScenarioEffectSet> activeSets = new ArrayList<>();

	public void registerSet(ScenarioEffectSet set, EffectBus bus) {
		if (set == null) {
			return;
		}
		set.install(bus);
		activeSets.add(set);
		Logging.EFFECTS.info("[effectService] installed {}", set.getClass().getSimpleName());
	}

	public void unregisterSet(ScenarioEffectSet set) {
		if (set == null) {
			return;
		}
		if (activeSets.remove(set)) {
			try {
				set.close();
			} catch (Exception ignored) {
			}
			Logging.EFFECTS.info("[effectService] removed {}", set.getClass().getSimpleName());
		}
	}

	public void clearSets() {
		for (ScenarioEffectSet set : activeSets) {
			try {
				set.close();
			} catch (Exception ignored) {
			}
			Logging.EFFECTS.info("[effectService] removed {}", set.getClass().getSimpleName());
		}
		activeSets.clear();
	}

	public void close() {
		clearSets();
	}
}

