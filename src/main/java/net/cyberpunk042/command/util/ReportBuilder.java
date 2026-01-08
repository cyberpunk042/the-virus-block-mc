package net.cyberpunk042.command.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for consistent multi-line status/report output.
 * 
 * <p>Usage:
 * <pre>
 * ReportBuilder.create("Erosion Settings")
 *     .kv("water_drain_mode", collapse.waterDrainMode())
 *     .kv("water_drain_offset", collapse.waterDrainOffset())
 *     .section("Deferred", s -> s
 *         .kv("enabled", deferred.enabled())
 *         .kv("delay_ticks", deferred.initialDelayTicks()))
 *     .kv("collapse_particles", collapse.collapseParticles())
 *     .send(source);
 * </pre>
 */
public final class ReportBuilder {
    
    private final String title;
    private final List<Line> lines = new ArrayList<>();
    private int indent = 0;
    
    private ReportBuilder(String title) {
        this.title = title;
    }
    
    public static ReportBuilder create(String title) {
        return new ReportBuilder(title);
    }
    
    public static ReportBuilder untitled() {
        return new ReportBuilder(null);
    }
    
    /**
     * Add a key-value pair.
     */
    public ReportBuilder kv(String key, Object value) {
        lines.add(new KVLine(indent, key, formatValue(value)));
        return this;
    }
    
    /**
     * Add a key-value pair with custom formatting.
     */
    public ReportBuilder kv(String key, Object value, Formatting valueColor) {
        lines.add(new KVLine(indent, key, formatValue(value), valueColor));
        return this;
    }
    
    /**
     * Add a raw text line.
     */
    public ReportBuilder line(String text) {
        lines.add(new TextLine(indent, text, Formatting.GRAY));
        return this;
    }
    
    /**
     * Add a raw text line with color.
     */
    public ReportBuilder line(String text, Formatting color) {
        lines.add(new TextLine(indent, text, color));
        return this;
    }
    
    /**
     * Add an indented section with a header.
     */
    public ReportBuilder section(String header, Consumer<ReportBuilder> content) {
        lines.add(new TextLine(indent, header + ":", Formatting.YELLOW));
        indent++;
        content.accept(this);
        indent--;
        return this;
    }
    
    /**
     * Add a blank line.
     */
    public ReportBuilder blank() {
        lines.add(new BlankLine());
        return this;
    }
    
    /**
     * Send the report to the command source.
     */
    public void send(ServerCommandSource source) {
        // Title
        if (title != null && !title.isEmpty()) {
            source.sendFeedback(() -> Text.literal("═══ " + title + " ═══")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);
        }
        
        // Lines
        for (Line line : lines) {
            source.sendFeedback(line::build, false);
        }
    }
    
    private static String formatValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean b) return b ? "true" : "false";
        if (value instanceof Enum<?> e) return e.name().toLowerCase();
        return String.valueOf(value);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Line types
    // ─────────────────────────────────────────────────────────────────────────────
    
    private sealed interface Line permits KVLine, TextLine, BlankLine {
        Text build();
    }
    
    private record KVLine(int indent, String key, String value, Formatting valueColor) implements Line {
        KVLine(int indent, String key, String value) {
            this(indent, key, value, Formatting.WHITE);
        }
        
        @Override
        public Text build() {
            String prefix = "  ".repeat(indent) + " • ";
            return Text.literal(prefix).formatted(Formatting.GRAY)
                .append(Text.literal(key + ": ").formatted(Formatting.GRAY))
                .append(Text.literal(value).formatted(valueColor));
        }
    }
    
    private record TextLine(int indent, String text, Formatting color) implements Line {
        @Override
        public Text build() {
            String prefix = "  ".repeat(indent);
            return Text.literal(prefix + text).formatted(color);
        }
    }
    
    private record BlankLine() implements Line {
        @Override
        public Text build() {
            return Text.empty();
        }
    }
}
