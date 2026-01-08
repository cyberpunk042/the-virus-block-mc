package net.cyberpunk042.command.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Fluent builder for consistent list command output.
 * 
 * <p>Usage:
 * <pre>
 * ListFormatter.create("Registered scenarios:")
 *     .emptyMessage("No scenarios registered.")
 *     .items(scenarios, id -> {
 *         var entry = ListFormatter.entry(id.toString());
 *         if (id.equals(active)) entry.tag("active", Formatting.GREEN);
 *         if (id.equals(effective)) entry.tag("current", Formatting.AQUA);
 *         return entry;
 *     })
 *     .showCount(true)
 *     .send(source);
 * </pre>
 */
public final class ListFormatter<T> {
    
    private final String header;
    private String emptyMessage = "No items.";
    private boolean showCount = false;
    private final List<Entry> entries = new ArrayList<>();
    
    private ListFormatter(String header) {
        this.header = header;
    }
    
    public static <T> ListFormatter<T> create(String header) {
        return new ListFormatter<>(header);
    }
    
    public ListFormatter<T> emptyMessage(String message) {
        this.emptyMessage = message;
        return this;
    }
    
    public ListFormatter<T> showCount(boolean show) {
        this.showCount = show;
        return this;
    }
    
    /**
     * Add items using a mapper function that creates Entry objects.
     */
    public ListFormatter<T> items(Collection<T> items, Function<T, Entry> mapper) {
        for (T item : items) {
            entries.add(mapper.apply(item));
        }
        return this;
    }
    
    /**
     * Add items with simple string representation.
     */
    public ListFormatter<T> itemsSimple(Collection<T> items, Function<T, String> mapper) {
        for (T item : items) {
            entries.add(entry(mapper.apply(item)));
        }
        return this;
    }
    
    /**
     * Add a single entry.
     */
    public ListFormatter<T> add(Entry entry) {
        entries.add(entry);
        return this;
    }
    
    /**
     * Send the formatted list to the command source.
     * @return The number of items (useful for command return value)
     */
    public int send(ServerCommandSource source) {
        if (entries.isEmpty()) {
            source.sendFeedback(() -> Text.literal(emptyMessage).formatted(Formatting.GRAY), false);
            return 0;
        }
        
        // Header
        String headerText = showCount ? header + " (" + entries.size() + ")" : header;
        source.sendFeedback(() -> Text.literal(headerText).formatted(Formatting.YELLOW), false);
        
        // Items
        for (Entry entry : entries) {
            source.sendFeedback(entry::build, false);
        }
        
        return entries.size();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Entry builder
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static Entry entry(String text) {
        return new Entry(text);
    }
    
    public static class Entry {
        private final String text;
        private final List<Tag> tags = new ArrayList<>();
        private Formatting textColor = Formatting.WHITE;
        
        private Entry(String text) {
            this.text = text;
        }
        
        public Entry color(Formatting color) {
            this.textColor = color;
            return this;
        }
        
        public Entry tag(String label) {
            return tag(label, Formatting.GRAY);
        }
        
        public Entry tag(String label, Formatting color) {
            tags.add(new Tag(label, color));
            return this;
        }
        
        public Entry tagIf(boolean condition, String label) {
            if (condition) tag(label);
            return this;
        }
        
        public Entry tagIf(boolean condition, String label, Formatting color) {
            if (condition) tag(label, color);
            return this;
        }
        
        Text build() {
            MutableText result = Text.literal(" • ").formatted(Formatting.GRAY)
                .append(Text.literal(text).formatted(textColor));
            
            for (Tag tag : tags) {
                result.append(Text.literal(" [" + tag.label + "]").formatted(tag.color));
            }
            
            return result;
        }
        
        private record Tag(String label, Formatting color) {}
    }
}
