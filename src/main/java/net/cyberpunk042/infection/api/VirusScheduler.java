package net.cyberpunk042.infection.api;

import java.util.List;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * Per-scenario task queue that will eventually replace the ad-hoc runnable
 * lists spread through {@link net.cyberpunk042.infection.VirusWorldState}.
 * <p>
 * For now it provides the minimum set of hooks so that the world host can
 * thread the dependency through the singularity state machine without behavior
 * changes.
 */
public interface VirusScheduler {
	void schedule(int delayTicks, Runnable task);

	void tick();

	void clear();

	boolean isEmpty();

	List<? extends PersistedTaskSnapshot> snapshot();

	void loadSnapshot(List<? extends PersistedTaskSnapshot> snapshots);

	static VirusScheduler immediate() {
		return NoopVirusScheduler.INSTANCE;
	}

	interface PersistedTaskSnapshot {
	}

	interface PersistedTask extends Runnable {
		Identifier type();

		NbtCompound save();
	}
}

