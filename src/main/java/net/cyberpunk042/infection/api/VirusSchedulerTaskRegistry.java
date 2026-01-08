package net.cyberpunk042.infection.api;


import net.cyberpunk042.log.Logging;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * Registry of persistent scheduler tasks. Tasks that wish to survive world
 * saves must register a factory up-front so the scheduler can reconstruct the
 * runnable from serialized data when the server restarts.
 */
public final class VirusSchedulerTaskRegistry {
	private static final Map<Identifier, TaskFactory> FACTORIES = new Object2ObjectOpenHashMap<>();

	private VirusSchedulerTaskRegistry() {
	}

	public static void register(Identifier id, TaskFactory factory) {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(factory, "factory");
		if (FACTORIES.containsKey(id)) {
			throw new IllegalStateException("Scheduler task already registered: " + id);
		}
		FACTORIES.put(id, factory);
	}

	public static Optional<VirusScheduler.PersistedTask> decode(Identifier id, NbtCompound data) {
		TaskFactory factory = FACTORIES.get(id);
		if (factory == null) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(factory.create(data == null ? new NbtCompound() : data));
		} catch (Exception ex) {
			Logging.CONFIG.error("[VirusScheduler] failed to decode task {}", id, ex);
			return Optional.empty();
		}
	}

	@FunctionalInterface
	public interface TaskFactory {
		@Nullable
		VirusScheduler.PersistedTask create(NbtCompound data);
	}
}

