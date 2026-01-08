package net.cyberpunk042.infection.api;


import net.cyberpunk042.log.Logging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * Tick-based scheduler that executes {@link Runnable} tasks after the requested
 * delay. Tasks are executed on the server thread during {@link #tick()} and
 * exceptions are logged but do not halt subsequent tasks. Tasks that implement
 * {@link VirusScheduler.PersistedTask} are snapshotted so they can be restored
 * across server restarts.
 */
public final class SimpleVirusScheduler implements VirusScheduler, VirusSchedulerMetrics {
	private final List<ScheduledTask> tasks = new ArrayList<>();

	@Override
	public synchronized void schedule(int delayTicks, Runnable task) {
		Objects.requireNonNull(task, "task");
		int clampedDelay = Math.max(0, delayTicks);
		if (clampedDelay == 0) {
			runSafely(task);
			return;
		}
		VirusScheduler.PersistedTask persisted = task instanceof VirusScheduler.PersistedTask pt ? pt : null;
		tasks.add(new ScheduledTask(clampedDelay, task, persisted));
	}

	@Override
	public synchronized void tick() {
		if (tasks.isEmpty()) {
			return;
		}
		Iterator<ScheduledTask> iterator = tasks.iterator();
		while (iterator.hasNext()) {
			ScheduledTask scheduled = iterator.next();
			if (scheduled.remaining > 0) {
				scheduled.remaining--;
			}
			if (scheduled.remaining <= 0) {
				iterator.remove();
				runSafely(scheduled.task);
			}
		}
	}

	@Override
	public synchronized void clear() {
		tasks.clear();
	}

	@Override
	public synchronized boolean isEmpty() {
		return tasks.isEmpty();
	}

	@Override
	public synchronized List<TaskSnapshot> snapshot() {
		if (tasks.isEmpty()) {
			return List.of();
		}
		List<TaskSnapshot> snapshots = new ArrayList<>();
		for (ScheduledTask scheduled : tasks) {
			if (scheduled.persisted == null) {
				continue;
			}
			NbtCompound data = scheduled.persisted.save();
			snapshots.add(new TaskSnapshot(scheduled.persisted.type(), scheduled.remaining, data));
		}
		return snapshots.isEmpty() ? List.of() : Collections.unmodifiableList(snapshots);
	}

	@Override
	public synchronized int pendingTasks() {
		return tasks.size();
	}

	@Override
	public synchronized void loadSnapshot(List<? extends PersistedTaskSnapshot> snapshots) {
		if (snapshots == null || snapshots.isEmpty()) {
			return;
		}
		for (PersistedTaskSnapshot raw : snapshots) {
			if (!(raw instanceof TaskSnapshot snapshot)) {
				continue;
			}
			Optional<VirusScheduler.PersistedTask> task = VirusSchedulerTaskRegistry.decode(snapshot.type(), snapshot.payload());
			if (task.isEmpty()) {
				Logging.CONFIG.warn("[VirusScheduler] Missing task factory for {}", snapshot.type());
				continue;
			}
			tasks.add(new ScheduledTask(Math.max(0, snapshot.remainingTicks()), task.get(), task.get()));
		}
	}

	private static void runSafely(Runnable task) {
		try {
			task.run();
		} catch (Throwable error) {
			Logging.CONFIG.error("[VirusScheduler] task failed", error);
		}
	}

	private static final class ScheduledTask {
		private int remaining;
		private final Runnable task;
		@Nullable
		private final VirusScheduler.PersistedTask persisted;

		private ScheduledTask(int delay, Runnable task, @Nullable VirusScheduler.PersistedTask persisted) {
			this.remaining = delay;
			this.task = task;
			this.persisted = persisted;
		}
	}

	public record TaskSnapshot(Identifier type, int remainingTicks, NbtCompound payload)
			implements VirusScheduler.PersistedTaskSnapshot {
		public static final Codec<TaskSnapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Identifier.CODEC.fieldOf("type").forGetter(TaskSnapshot::type),
				Codec.INT.fieldOf("remaining").forGetter(TaskSnapshot::remainingTicks),
				NbtCompound.CODEC.optionalFieldOf("data", new NbtCompound()).forGetter(TaskSnapshot::payload)
		).apply(inst, TaskSnapshot::new));
	}
}

