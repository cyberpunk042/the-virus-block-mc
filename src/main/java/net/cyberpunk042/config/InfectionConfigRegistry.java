package net.cyberpunk042.config;


import net.cyberpunk042.log.Logging;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Small facade that keeps track of every JSON-backed configuration module
 * exposed by the mod. Each config registers a {@link ConfigHandle} so the
 * registry can load/save the entire stack (or client-only slices) from a
 * single entry point.
 */
public final class InfectionConfigRegistry {
	private static final Map<String, ConfigHandle> HANDLES = new LinkedHashMap<>();

	private InfectionConfigRegistry() {
	}

	public static void register(ConfigHandle handle) {
		Objects.requireNonNull(handle, "handle");
		ConfigHandle previous = HANDLES.putIfAbsent(handle.id(), handle);
		if (previous != null) {
			throw new IllegalStateException("Duplicate config id: " + handle.id());
		}
	}

	public static void loadCommon() {
		loadInternal(ConfigScope.COMMON);
	}

	public static void loadClient() {
		loadCommon();
		loadInternal(ConfigScope.CLIENT_ONLY);
	}

	public static void save(String id) {
		ConfigHandle handle = HANDLES.get(id);
		if (handle == null) {
			return;
		}
		try {
			handle.saver().run();
		} catch (Exception ex) {
			Logging.CONFIG.error("Failed to save config {}", id, ex);
		}
	}

	private static void loadInternal(ConfigScope scope) {
		HANDLES.values().stream()
				.filter(handle -> handle.scope() == scope)
				.forEach(handle -> {
					try {
						handle.loader().run();
					} catch (Exception ex) {
						Logging.CONFIG.error("Failed to load config {}", handle.id(), ex);
					}
				});
	}

	public enum ConfigScope {
		COMMON,
		CLIENT_ONLY
	}

	public record ConfigHandle(String id, ConfigScope scope, Runnable loader, Runnable saver) {
		public ConfigHandle {
			if (id == null || id.isBlank()) {
				throw new IllegalArgumentException("Config id must be set");
			}
			Objects.requireNonNull(scope, "scope");
			Objects.requireNonNull(loader, "loader");
			Objects.requireNonNull(saver, "saver");
		}

		public static ConfigHandle common(String id, Runnable loader, Runnable saver) {
			return new ConfigHandle(id, ConfigScope.COMMON, loader, saver);
		}

		public static ConfigHandle clientOnly(String id, Runnable loader, Runnable saver) {
			return new ConfigHandle(id, ConfigScope.CLIENT_ONLY, loader, saver);
		}
	}
}

