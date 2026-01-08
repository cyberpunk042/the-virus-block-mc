package net.cyberpunk042.infection.service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Convenience wrapper for reading/writing infection configuration files under
 * {@code config/the-virus-block}. Provides simple JSON helpers so new services
 * can persist their state without duplicating boilerplate.
 */
public final class ConfigService {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private final Path root;

	ConfigService(Path configDir) {
		this.root = configDir;
	}

	public Path root() {
		return root;
	}

	public Path resolve(String first, String... more) {
		return root.resolve(Path.of(first, more));
	}

	public <T> T readJson(String relativePath, Class<T> type, Supplier<T> defaults) {
		Path target = resolve(relativePath);
		if (!Files.exists(target)) {
			return defaults.get();
		}
		try (Reader reader = Files.newBufferedReader(target)) {
			T value = GSON.fromJson(reader, type);
			return value != null ? value : defaults.get();
		} catch (IOException ex) {
			return defaults.get();
		}
	}

	public void writeJson(String relativePath, Object value) {
		Objects.requireNonNull(value, "value");
		Path target = resolve(relativePath);
		try {
			if (target.getParent() != null) {
				Files.createDirectories(target.getParent());
			}
			try (Writer writer = Files.newBufferedWriter(target)) {
				GSON.toJson(value, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public boolean exists(String relativePath) {
		return Files.exists(resolve(relativePath));
	}
}

