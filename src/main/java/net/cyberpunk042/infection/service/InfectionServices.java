package net.cyberpunk042.infection.service;

import java.nio.file.Path;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

/**
 * Global bootstrap for {@link InfectionServiceContainer}. This is initialized
 * once during mod startup so subsequent code can safely retrieve shared
 * services without re-reading config.
 */
public final class InfectionServices {
	private static InfectionServiceContainer INSTANCE;
	private static Path CONFIG_ROOT;

	private InfectionServices() {
	}

	public static synchronized void initialize(Path configDir) {
		if (INSTANCE != null) {
			return;
		}
		CONFIG_ROOT = Objects.requireNonNull(configDir, "configDir");
		rebuild();
	}

	public static synchronized void reload() {
		if (CONFIG_ROOT == null) {
			throw new IllegalStateException("InfectionServices has not been initialized");
		}
		rebuild();
	}

	private static void rebuild() {
		INSTANCE = InfectionServiceContainer.builder(CONFIG_ROOT).build();
	}

	public static InfectionServiceContainer get() {
		return Objects.requireNonNull(INSTANCE, "InfectionServices has not been initialized");
	}

	/**
	 * Returns the service container, or null if not yet initialized.
	 */
	public static @Nullable InfectionServiceContainer container() {
		return INSTANCE;
	}
}

