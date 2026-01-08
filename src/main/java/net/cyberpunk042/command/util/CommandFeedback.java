package net.cyberpunk042.command.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Centralized command feedback utilities with consistent formatting.
 * 
 * <p>Usage:
 * <pre>
 * CommandFeedback.success(source, "Feature enabled");
 * CommandFeedback.error(source, "Invalid value");
 * CommandFeedback.info(source, "Current value: " + value);
 * CommandFeedback.warn(source, "This action cannot be undone");
 * </pre>
 */
public final class CommandFeedback {
    
    private CommandFeedback() {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Simple string feedback
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Sends a success message (green).
     */
    public static void success(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.GREEN), false);
    }
    
    /**
     * Sends a success message that broadcasts to ops.
     */
    public static void successBroadcast(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.GREEN), true);
    }
    
    /**
     * Sends an error message (red).
     */
    public static void error(ServerCommandSource source, String message) {
        source.sendError(Text.literal(message));
    }
    
    /**
     * Sends an info message (gray).
     */
    public static void info(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.GRAY), false);
    }
    
    /**
     * Sends a warning message (yellow).
     */
    public static void warn(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.YELLOW), false);
    }
    
    /**
     * Alias for {@link #warn(ServerCommandSource, String)}.
     */
    public static void warning(ServerCommandSource source, String message) {
        warn(source, message);
    }
    
    /**
     * Sends a highlight message (aqua).
     */
    public static void highlight(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.AQUA), false);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Text feedback (for complex formatting)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Sends a success Text.
     */
    public static void success(ServerCommandSource source, Text text) {
        source.sendFeedback(() -> text, false);
    }
    
    /**
     * Sends an error Text.
     */
    public static void error(ServerCommandSource source, Text text) {
        source.sendError(text);
    }
    
    /**
     * Sends an info Text.
     */
    public static void info(ServerCommandSource source, Text text) {
        source.sendFeedback(() -> text, false);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Common patterns
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Sends a "feature enabled/disabled" message.
     */
    public static void toggle(ServerCommandSource source, String featureName, boolean enabled) {
        String status = enabled ? "enabled" : "disabled";
        Formatting color = enabled ? Formatting.GREEN : Formatting.RED;
        source.sendFeedback(() -> Text.literal(featureName + " " + status).formatted(color), false);
    }
    
    /**
     * Sends a "value set to X" message.
     */
    public static void valueSet(ServerCommandSource source, String valueName, Object value) {
        source.sendFeedback(() -> Text.literal(valueName + " set to " + value).formatted(Formatting.GREEN), false);
    }
    
    /**
     * Sends a "current value is X" message.
     */
    public static void valueGet(ServerCommandSource source, String valueName, Object value) {
        source.sendFeedback(() -> Text.literal("Current " + valueName + ": " + value).formatted(Formatting.GRAY), false);
    }
    
    /**
     * Sends a "not found" error.
     */
    public static void notFound(ServerCommandSource source, String itemType, String identifier) {
        error(source, itemType + " not found: " + identifier);
    }
    
    /**
     * Sends an "invalid value" error.
     */
    public static void invalidValue(ServerCommandSource source, String valueName, Object value) {
        error(source, "Invalid " + valueName + ": " + value);
    }
    
    /**
     * Sends a "requires" error (e.g., "Requires an active scenario").
     */
    public static void requires(ServerCommandSource source, String requirement) {
        error(source, "Requires " + requirement);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Builder for complex messages
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Starts building a labeled message (e.g., "Label: value").
     */
    public static MutableText labeled(String label, Object value) {
        return Text.literal(label + ": ").formatted(Formatting.GRAY)
            .append(Text.literal(String.valueOf(value)).formatted(Formatting.WHITE));
    }
    
    /**
     * Creates a header text (gold, bold).
     */
    public static MutableText header(String text) {
        return Text.literal("═══ " + text + " ═══").formatted(Formatting.GOLD, Formatting.BOLD);
    }
    
    /**
     * Creates a subheader text (yellow).
     */
    public static MutableText subheader(String text) {
        return Text.literal("── " + text + " ──").formatted(Formatting.YELLOW);
    }
    
    /**
     * Creates a bullet point item.
     */
    public static MutableText bullet(String text) {
        return Text.literal("  • " + text).formatted(Formatting.GRAY);
    }
    
    /**
     * Creates a key-value pair for listings.
     */
    public static MutableText keyValue(String key, Object value, Formatting valueColor) {
        return Text.literal("  " + key + ": ").formatted(Formatting.GRAY)
            .append(Text.literal(String.valueOf(value)).formatted(valueColor));
    }
}
