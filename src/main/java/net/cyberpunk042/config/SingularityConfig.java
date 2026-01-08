package net.cyberpunk042.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import net.cyberpunk042.config.InfectionConfigRegistry.ConfigHandle;

/**
 * Client-facing configuration for singularity collapse execution.
 * Generates {@code config/the-virus-block/singularity.json}.
 * 
 * <p>The collapse system uses a simple radius-based fill loop that processes
 * blocks in expanding/contracting rings around the singularity center.
 */
public final class SingularityConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
			.resolve("the-virus-block")
			.resolve("singularity.json");

	private static SingularitySettings settings = new SingularitySettings();

	static {
		InfectionConfigRegistry.register(ConfigHandle.common("singularity", SingularityConfig::load, SingularityConfig::save));
	}

	private SingularityConfig() {
	}

	public static synchronized void load() {
		boolean dirty = false;
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				SingularitySettings loaded = GSON.fromJson(reader, SingularitySettings.class);
				if (loaded != null) {
					settings = loaded;
				}
			} catch (IOException ignored) {
			}
		} else {
			dirty = true;
		}
		if (settings.execution == null) {
			settings.execution = new ExecutionSettings();
			dirty = true;
		}
		if (dirty) {
			save();
		}
	}

	public static synchronized void save() {
		try {
			if (CONFIG_PATH.getParent() != null) {
				Files.createDirectories(CONFIG_PATH.getParent());
			}
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(settings, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public static void reload() {
		load();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Accessors
	// ─────────────────────────────────────────────────────────────────────────────

	public static ExecutionSettings execution() {
		return settings.execution != null ? settings.execution : new ExecutionSettings();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Settings classes
	// ─────────────────────────────────────────────────────────────────────────────

	private static final class SingularitySettings {
		ExecutionSettings execution = new ExecutionSettings();
	}

	/**
	 * Execution settings for singularity collapse.
	 * 
	 * <p>The collapse processor uses a simple radius-based fill loop that
	 * clears blocks in expanding or contracting rings around the singularity center.
	 * Configuration for fill shapes, modes, and water drainage are in the
	 * dimension profile settings.
	 */
	public static final class ExecutionSettings {
		// Reserved for future settings
	}
}
