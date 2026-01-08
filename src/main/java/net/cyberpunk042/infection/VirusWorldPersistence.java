package net.cyberpunk042.infection;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.cyberpunk042.infection.api.SimpleVirusScheduler;
import net.cyberpunk042.infection.service.CollapseSnapshotService.SingularitySnapshot;
import net.cyberpunk042.infection.service.ShieldFieldService;
import net.minecraft.util.math.BlockPos;

/**
 * Handles serialization/deserialization of VirusWorldState.
 * Extracts all persistence-related records, codecs, and snapshot logic.
 */
public final class VirusWorldPersistence {

	// ─────────────────────────────────────────────────────────────────────────────
	// Flag constants for state encoding
	// ─────────────────────────────────────────────────────────────────────────────

	static final int FLAG_APOCALYPSE = 1 << 1;
	static final int FLAG_TERRAIN = 1 << 2;
	static final int FLAG_SHELLS = 1 << 3;
	static final int FLAG_CLEANSING = 1 << 4;

	// ─────────────────────────────────────────────────────────────────────────────
	// Codecs for nested records
	// ─────────────────────────────────────────────────────────────────────────────

	private static final Codec<ShieldFieldService.ShieldField> SHIELD_FIELD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.fieldOf("id").forGetter(ShieldFieldService.ShieldField::id),
			BlockPos.CODEC.fieldOf("center").forGetter(ShieldFieldService.ShieldField::center),
			Codec.DOUBLE.fieldOf("radius").forGetter(ShieldFieldService.ShieldField::radius),
			Codec.LONG.fieldOf("createdTick").forGetter(ShieldFieldService.ShieldField::createdTick)
	).apply(instance, ShieldFieldService.ShieldField::fromPersistence));

	private static final Codec<BoobytrapDefaults> BOOBYTRAP_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("captured").forGetter(BoobytrapDefaults::captured),
			Codec.BOOL.fieldOf("enabled").forGetter(BoobytrapDefaults::enabled),
			Codec.INT.fieldOf("spawn").forGetter(BoobytrapDefaults::spawn),
			Codec.INT.fieldOf("trap").forGetter(BoobytrapDefaults::trap)
	).apply(instance, BoobytrapDefaults::new));

	private static final Codec<SpreadSnapshot> SPREAD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.LONG.listOf().fieldOf("pillarChunks").forGetter(SpreadSnapshot::pillars),
			BlockPos.CODEC.listOf().fieldOf("virusSources").forGetter(SpreadSnapshot::sources)
	).apply(instance, SpreadSnapshot::new));

	// ─────────────────────────────────────────────────────────────────────────────
	// Main codec for VirusWorldState (used by PersistentState)
	// ─────────────────────────────────────────────────────────────────────────────

	public static final Codec<VirusWorldState> CODEC = VirusWorldSnapshot.CODEC.xmap(
			VirusWorldPersistence::fromSnapshot,
			VirusWorldPersistence::createSnapshot
	);

	// ─────────────────────────────────────────────────────────────────────────────
	// Snapshot creation
	// ─────────────────────────────────────────────────────────────────────────────

	static VirusWorldSnapshot createSnapshot(VirusWorldState state) {
		return new VirusWorldSnapshot(
				state.infectionState().infected(),
				state.infectionState().dormant(),
				state.tiers().index(),
				state.infectionState().totalTicks(),
				state.tiers().ticksInTier(),
				state.tiers().containmentLevel(),
				encodeFlags(state),
				HealthSnapshot.of(state),
				state.tiers().difficulty().getId(),
				state.tiers().hasShownDifficultyPrompt(),
				createBoobytrapSnapshot(state),
				createSpreadSnapshot(state),
				Map.copyOf(state.infectionState().eventHistory()),
				state.infectionState().lastMatrixCubeTick(),
				new ProfilesAndScheduler(state.presentationCoord().getProfileSnapshot(), state.orchestrator().services().schedulerService().snapshot()),
				new SingularityPersistenceTail(
						state.shieldFieldService().snapshot(),
						state.infection().getSingularitySnapshot(),
						state.singularity().chunkPreparationState().preGenComplete,
						state.singularity().chunkPreparationState().preGenMissingChunks,
						captureDamageAdaptation(state)));
	}

	private static Optional<BoobytrapDefaults> createBoobytrapSnapshot(VirusWorldState state) {
		if (!state.infectionState().boobytrapDefaultsCaptured()) {
			return Optional.empty();
		}
		return Optional.of(new BoobytrapDefaults(
				true,
				state.infectionState().defaultBoobytrapsEnabled(),
				state.infectionState().defaultWormSpawnChance(),
				state.infectionState().defaultWormTrapSpawnChance()));
	}

	private static Optional<SpreadSnapshot> createSpreadSnapshot(VirusWorldState state) {
		List<Long> pillars = new java.util.ArrayList<>(state.pillarChunks().size());
		state.pillarChunks().forEach((long value) -> pillars.add(value));
		List<BlockPos> sources = state.sources().snapshot(state.sourceState());
		if (pillars.isEmpty() && sources.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new SpreadSnapshot(pillars, sources));
	}

	/**
	 * Captures damage adaptation data for persistence.
	 */
	private static Map<String, Integer> captureDamageAdaptation(VirusWorldState state) {
		it.unimi.dsi.fastutil.objects.Object2IntMap<String> raw = state.infectionState().damageAdaptation();
		if (raw.isEmpty()) {
			return Map.of();
		}
		java.util.HashMap<String, Integer> result = new java.util.HashMap<>();
		raw.object2IntEntrySet().forEach(e -> result.put(e.getKey(), e.getIntValue()));
		return result;
	}

	private static VirusWorldState fromSnapshot(VirusWorldSnapshot snapshot) {
		VirusWorldState state = new VirusWorldState();
		snapshot.applyTo(state);
		return state;
	}

	static int encodeFlags(VirusWorldState state) {
		int flags = 0;
		if (state.tiers().isApocalypseMode()) {
			flags |= FLAG_APOCALYPSE;
		}
		if (state.infectionState().terrainCorrupted()) {
			flags |= FLAG_TERRAIN;
		}
		if (state.shell().isCollapsed()) {
			flags |= FLAG_SHELLS;
		}
		if (state.infectionState().cleansingActive()) {
			flags |= FLAG_CLEANSING;
		}
		return flags;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Nested records
	// ─────────────────────────────────────────────────────────────────────────────

	record BoobytrapDefaults(boolean captured, boolean enabled, int spawn, int trap) {
	}

	record SpreadSnapshot(List<Long> pillars, List<BlockPos> sources) {
	}

	record HealthSnapshot(double scale, double current) {
		private static final HealthSnapshot DEFAULT = new HealthSnapshot(1.0D, 0.0D);
		private static final Codec<HealthSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Codec.DOUBLE.optionalFieldOf("scale", 1.0D).forGetter(HealthSnapshot::scale),
				Codec.DOUBLE.optionalFieldOf("current", 0.0D).forGetter(HealthSnapshot::current)
		).apply(inst, HealthSnapshot::new));

		static HealthSnapshot of(VirusWorldState state) {
			return new HealthSnapshot(
					state.tiers().healthScale(),
					state.tiers().currentHealth());
		}
	}

	record SingularityPersistenceTail(
			List<ShieldFieldService.ShieldField> shields,
			Optional<SingularitySnapshot> snapshot,
			boolean preGenComplete,
			int preGenMissing,
			Map<String, Integer> damageAdaptation) {
	}

	record ProfilesAndScheduler(
			Map<String, String> profiles,
			List<SimpleVirusScheduler.TaskSnapshot> schedulerTasks) {
	}

	record VirusWorldSnapshot(
			boolean infected,
			boolean dormant,
			int tierIndex,
			long totalTicks,
			long ticksInTier,
			int containmentLevel,
			int stateFlags,
			HealthSnapshot health,
			String difficultyId,
			boolean difficultyPromptShown,
			Optional<BoobytrapDefaults> boobytrapDefaults,
			Optional<SpreadSnapshot> spreadData,
			Map<VirusEventType, Long> eventHistory,
			long lastMatrixCubeTick,
			ProfilesAndScheduler profilesAndScheduler,
			SingularityPersistenceTail persistenceTail) {

		static final Codec<VirusWorldSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.BOOL.optionalFieldOf("infected", false).forGetter(VirusWorldSnapshot::infected),
				Codec.BOOL.optionalFieldOf("dormant", false).forGetter(VirusWorldSnapshot::dormant),
				Codec.INT.optionalFieldOf("tierIndex", 0).forGetter(VirusWorldSnapshot::tierIndex),
				Codec.LONG.optionalFieldOf("totalTicks", 0L).forGetter(VirusWorldSnapshot::totalTicks),
				Codec.LONG.optionalFieldOf("ticksInTier", 0L).forGetter(VirusWorldSnapshot::ticksInTier),
				Codec.INT.optionalFieldOf("containmentLevel", 0).forGetter(VirusWorldSnapshot::containmentLevel),
				Codec.INT.optionalFieldOf("stateFlags", 0).forGetter(VirusWorldSnapshot::stateFlags),
				HealthSnapshot.CODEC.optionalFieldOf("health", HealthSnapshot.DEFAULT).forGetter(VirusWorldSnapshot::health),
				Codec.STRING.optionalFieldOf("difficulty", VirusDifficulty.HARD.getId()).forGetter(VirusWorldSnapshot::difficultyId),
				Codec.BOOL.optionalFieldOf("difficultyPromptShown", false).forGetter(VirusWorldSnapshot::difficultyPromptShown),
				BOOBYTRAP_CODEC.optionalFieldOf("boobytrapDefaults").forGetter(VirusWorldSnapshot::boobytrapDefaults),
				SPREAD_CODEC.optionalFieldOf("spreadData").forGetter(VirusWorldSnapshot::spreadData),
				Codec.unboundedMap(VirusEventType.CODEC, Codec.LONG).optionalFieldOf("eventHistory", Map.of()).forGetter(VirusWorldSnapshot::eventHistory),
				Codec.LONG.optionalFieldOf("lastMatrixCubeTick", 0L).forGetter(VirusWorldSnapshot::lastMatrixCubeTick),
				profilesAndScheduler(instance),
			instance.group(
					SHIELD_FIELD_CODEC.listOf().optionalFieldOf("shieldFields", List.of()).forGetter((VirusWorldSnapshot snapshot) -> snapshot.persistenceTail().shields()),
					SingularitySnapshot.CODEC.optionalFieldOf("singularity").forGetter((VirusWorldSnapshot snapshot) -> snapshot.persistenceTail().snapshot()),
					Codec.BOOL.optionalFieldOf("singularityPreGenComplete", false).forGetter((VirusWorldSnapshot snapshot) -> snapshot.persistenceTail().preGenComplete()),
					Codec.INT.optionalFieldOf("singularityPreGenMissing", 0).forGetter((VirusWorldSnapshot snapshot) -> snapshot.persistenceTail().preGenMissing()),
					Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("damageAdaptation", Map.of()).forGetter((VirusWorldSnapshot snapshot) -> snapshot.persistenceTail().damageAdaptation())
			).apply(instance, SingularityPersistenceTail::new)
		).apply(instance, VirusWorldSnapshot::new));

		void applyTo(VirusWorldState state) {
			state.infectionState().setInfected(infected);
			state.infectionState().setDormant(dormant);
			state.tiers().setIndex(tierIndex);
			state.infectionState().setTotalTicks(totalTicks);
			state.tiers().setTicksInTier(ticksInTier);
			state.tiers().setContainmentLevel(containmentLevel);
			state.tiers().setApocalypseMode((stateFlags & FLAG_APOCALYPSE) != 0);
			state.infectionState().setTerrainCorrupted((stateFlags & FLAG_TERRAIN) != 0);
			state.shell().maintenance().applyCollapsedFromSnapshot((stateFlags & FLAG_SHELLS) != 0);
			state.infectionState().setCleansingActive((stateFlags & FLAG_CLEANSING) != 0);
			state.tiers().setHealthScale(health.scale());
			state.tiers().setCurrentHealth(health.current());
			state.tiers().overwriteDifficulty(VirusDifficulty.fromId(difficultyId));
			state.tiers().setDifficultyPromptShown(difficultyPromptShown);
			boobytrapDefaults.ifPresent(def -> {
				state.infectionState().setBoobytrapDefaultsCaptured(def.captured());
				state.infectionState().setDefaultBoobytrapsEnabled(def.enabled());
				state.infectionState().setDefaultWormSpawnChance(def.spawn());
				state.infectionState().setDefaultWormTrapSpawnChance(def.trap());
			});
			spreadData.ifPresent(data -> {
				data.pillars().forEach(chunk -> state.pillarChunks().add(chunk.longValue()));
				state.sources().restoreSnapshot(state.sourceState(), data.sources());
			});
			state.infectionState().eventHistory().putAll(eventHistory);
			state.infectionState().setLastMatrixCubeTick(lastMatrixCubeTick);
			// Restore damage adaptation from persistence tail
			persistenceTail.damageAdaptation().forEach((key, value) -> {
				for (int i = 0; i < value; i++) {
					state.infectionState().recordDamageExposure(key);
				}
			});
			state.presentationCoord().applyProfileSnapshot(profilesAndScheduler.profiles());
			state.orchestrator().services().schedulerService().loadSnapshot(profilesAndScheduler.schedulerTasks());
			state.shieldFieldService().restoreSnapshot(persistenceTail.shields());
			state.shell().maintenance().applyRebuildPendingFromSnapshot(false);
			state.singularity().barrier().setActive(false);
			persistenceTail.snapshot().ifPresentOrElse(state.infection()::applySingularitySnapshot,
					() -> state.singularity().phase().clearSingularityState());
			state.singularity().chunkPreparationState().preGenComplete = persistenceTail.preGenComplete();
			state.singularity().chunkPreparationState().preGenMissingChunks = persistenceTail.preGenMissing();
		}

		private static App<RecordCodecBuilder.Mu<VirusWorldSnapshot>, ProfilesAndScheduler> profilesAndScheduler(RecordCodecBuilder.Instance<VirusWorldSnapshot> instance) {
			return instance.group(
					Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("singularityProfiles", Map.of()).forGetter(snapshot -> snapshot.profilesAndScheduler().profiles()),
					SimpleVirusScheduler.TaskSnapshot.CODEC.listOf().optionalFieldOf("schedulerTasks", List.of()).forGetter(snapshot -> snapshot.profilesAndScheduler().schedulerTasks())
			).apply(instance, ProfilesAndScheduler::new);
		}
	}

	private VirusWorldPersistence() {
		// Utility class
	}
}

