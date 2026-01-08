package net.cyberpunk042.command.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Reusable utilities for reading/writing JSON config files.
 */
public final class ConfigFileEditor {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static final DynamicCommandExceptionType FILE_MISSING = 
            new DynamicCommandExceptionType(id -> Text.literal("Could not locate JSON file for: " + id));
    public static final DynamicCommandExceptionType IO_ERROR = 
            new DynamicCommandExceptionType(msg -> Text.literal("Config I/O error: " + msg));

    public record FieldChange<F>(F field, Object value, boolean cleared) {}

    private ConfigFileEditor() {}

    /**
     * Find a config file by scanning a directory for a matching ID.
     */
    @Nullable
    public static Path findConfigFile(
            Path directory,
            Identifier targetId,
            BiPredicate<Path, Identifier> matcher
    ) throws IOException {
        if (!Files.exists(directory)) {
            return null;
        }
        // Try direct path first
        Path guess = directory.resolve(targetId.getPath() + ".json");
        if (Files.exists(guess) && matcher.test(guess, targetId)) {
            return guess;
        }
        // Scan directory
        try (var stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> matcher.test(path, targetId))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Check if a JSON file contains the given ID.
     */
    public static boolean matchesId(Path path, Identifier targetId, String idField) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) return false;
            JsonObject json = parsed.getAsJsonObject();
            if (!json.has(idField)) return false;
            Identifier fileId = Identifier.tryParse(json.get(idField).getAsString());
            return targetId.equals(fileId);
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Read a JSON file as JsonObject.
     */
    public static JsonObject readJson(Path path) throws CommandSyntaxException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw IO_ERROR.create(path + " is not a JSON object");
            }
            return parsed.getAsJsonObject();
        } catch (IOException ex) {
            throw IO_ERROR.create(path + ": " + ex.getMessage());
        }
    }

    /**
     * Write a JsonObject to a file.
     */
    public static void writeJson(Path path, JsonObject json) throws CommandSyntaxException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(json, writer);
        } catch (IOException ex) {
            throw IO_ERROR.create(path + ": " + ex.getMessage());
        }
    }

    /**
     * Apply field changes to a JSON object.
     * 
     * @param json The JSON object to modify
     * @param changes List of field changes
     * @param keyMap Map from field enum to JSON key name
     * @return true if any changes were made
     */
    public static <F> boolean applyChanges(
            JsonObject json,
            List<FieldChange<F>> changes,
            Map<F, String> keyMap
    ) {
        boolean changed = false;
        for (FieldChange<F> change : changes) {
            String key = keyMap.get(change.field());
            if (key == null) continue;
            
            if (change.cleared()) {
                if (json.has(key)) {
                    json.remove(key);
                    changed = true;
                }
                continue;
            }
            
            Object value = change.value();
            if (value instanceof Boolean bool) {
                if (!json.has(key) || json.get(key).getAsBoolean() != bool) {
                    json.addProperty(key, bool);
                    changed = true;
                }
            } else if (value instanceof Integer integer) {
                if (!json.has(key) || json.get(key).getAsInt() != integer) {
                    json.addProperty(key, integer);
                    changed = true;
                }
            } else if (value instanceof Double dbl) {
                if (!json.has(key) || json.get(key).getAsDouble() != dbl) {
                    json.addProperty(key, dbl);
                    changed = true;
                }
            } else if (value instanceof Identifier id) {
                String text = id.toString();
                if (!json.has(key) || !json.get(key).getAsString().equals(text)) {
                    json.addProperty(key, text);
                    changed = true;
                }
            } else if (value instanceof String str) {
                if (!json.has(key) || !json.get(key).getAsString().equals(str)) {
                    json.addProperty(key, str);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Format a config path relative to root.
     */
    public static String formatPath(Path path, Path root) {
        try {
            return root.relativize(path).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return path.toString();
        }
    }
}
