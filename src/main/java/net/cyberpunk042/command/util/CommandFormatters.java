package net.cyberpunk042.command.util;

import java.util.Locale;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * Reusable formatting utilities for command output.
 */
public final class CommandFormatters {

    private CommandFormatters() {}

    public static String formatDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "NaN";
        }
        if (Math.abs(value - Math.rint(value)) < 1.0E-4) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    public static String formatFloat(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return "NaN";
        }
        if (Math.abs(value - Math.round(value)) < 1.0E-3F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    public static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public static String formatBox(Box box) {
        return String.format(Locale.ROOT, "[%.2f, %.2f, %.2f -> %.2f, %.2f, %.2f]",
                box.minX, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ);
    }

    public static String formatDimension(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    public static String formatIdentifier(Identifier id) {
        return id == null ? "null" : id.toString();
    }

    public static String formatBoolean(boolean value) {
        return value ? "true" : "false";
    }

    public static String formatAny(Object value) {
        if (value == null) return "null";
        if (value instanceof Double d) return formatDouble(d);
        if (value instanceof Float f) return formatFloat(f);
        if (value instanceof BlockPos p) return formatPos(p);
        if (value instanceof Box b) return formatBox(b);
        if (value instanceof Identifier i) return formatIdentifier(i);
        if (value instanceof Boolean bool) return formatBoolean(bool);
        return String.valueOf(value);
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     * Examples: "5.2s", "2m 30s", "1h 15m 30s"
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    /**
     * Formats a duration with millisecond precision for short durations.
     */
    public static String formatDurationPrecise(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            return formatDuration(millis);
        }
    }


    /**
     * Formats a duration in game ticks to a human-readable string.
     * 20 ticks = 1 second.
     * Examples: "5s", "2m 30s", "1h 15m 30s", "2d 5h 30m"
     */
    public static String formatDurationTicks(long ticks) {
        long clamped = Math.max(0L, ticks);
        long totalSeconds = clamped / 20L;
        long days = totalSeconds / 86_400L;
        totalSeconds %= 86_400L;
        long hours = totalSeconds / 3_600L;
        totalSeconds %= 3_600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }

}
