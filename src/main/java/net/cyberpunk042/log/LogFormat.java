package net.cyberpunk042.log;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static utility for formatting values in logs.
 * Includes type registry and builder factories.
 */
public final class LogFormat {
    
    @SuppressWarnings("rawtypes")
    private static final Map<Class, LogFormatter> FORMATTERS = new ConcurrentHashMap<>();
    
    static {
        // Built-in formatters
        register(BlockPos.class, bp -> "[" + bp.getX() + "," + bp.getY() + "," + bp.getZ() + "]");
        register(ChunkPos.class, cp -> "[" + cp.x + "," + cp.z + "]");
        register(Vec3d.class, v -> String.format("[%.1f,%.1f,%.1f]", v.x, v.y, v.z));
        register(Identifier.class, Identifier::toString);
        register(Entity.class, e -> e.getName().getString());
    }
    
    @SuppressWarnings("unchecked")
    public static <T> void register(Class<T> type, LogFormatter<T> formatter) {
        FORMATTERS.put(type, formatter);
    }
    
    @SuppressWarnings("unchecked")
    public static String format(Object value) {
        if (value == null) return "null";
        LogFormatter formatter = FORMATTERS.get(value.getClass());
        if (formatter != null) {
            return formatter.format(value);
        }
        // Check superclasses
        for (Map.Entry<Class, LogFormatter> entry : FORMATTERS.entrySet()) {
            if (entry.getKey().isInstance(value)) {
                return entry.getValue().format(value);
            }
        }
        return value.toString();
    }
    
    // ========== QUICK FORMATTERS ==========
    
    public static String duration(long ticks) {
        if (ticks < 20) return ticks + "t";
        if (ticks < 20 * 60) return String.format("%.1fs", ticks / 20.0);
        if (ticks < 20 * 60 * 60) return String.format("%.1fm", ticks / (20.0 * 60));
        return String.format("%.1fh", ticks / (20.0 * 60 * 60));
    }
    
    public static String durationMs(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        return String.format("%.1fm", ms / 60000.0);
    }
    
    public static String heading(String text) {
        return "=== " + text + " ===";
    }
    
    public static String custom(Supplier<String> supplier) {
        return supplier.get();
    }
    
    public static String raw(String text) {
        return text;
    }
    
    // ========== LIST FORMATTING ==========
    
    public static <T> String list(Iterable<T> items) {
        return list(items, Object::toString);
    }
    
    public static <T> String list(Iterable<T> items, Function<T, String> mapper) {
        StringJoiner joiner = new StringJoiner("\n  - ", "\n  - ", "");
        for (T item : items) joiner.add(mapper.apply(item));
        return joiner.toString();
    }
    
    public static <T> String list(T[] items, Function<T, String> mapper) {
        return list(Arrays.asList(items), mapper);
    }
    
    public static ListBuilder listBuilder() {
        return new ListBuilder();
    }
    
    // ========== PAIR FORMATTING ==========
    
    public static String pairs(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            sb.append("\n  ").append(e.getKey()).append(": ").append(format(e.getValue()));
        }
        return sb.toString();
    }
    
    public static PairBuilder pairs() {
        return new PairBuilder();
    }
    
    // ========== TABLE FORMATTING ==========
    
    public static TableBuilder table() {
        return new TableBuilder();
    }
    
    // ========== TREE FORMATTING ==========
    
    public static TreeBuilder tree(String root) {
        return new TreeBuilder(root);
    }
    
    private LogFormat() {}
    
    // ========== BUILDERS ==========
    
    public static class ListBuilder {
        private final List<String> items = new ArrayList<>();
        private String bullet = "- ";
        private String indent = "  ";
        
        public ListBuilder bullet(String b) { bullet = b; return this; }
        public ListBuilder indent(String i) { indent = i; return this; }
        public ListBuilder add(String item) { items.add(item); return this; }
        public <T> ListBuilder add(T item, Function<T, String> mapper) { items.add(mapper.apply(item)); return this; }
        public <T> ListBuilder addAll(Iterable<T> it, Function<T, String> mapper) {
            for (T t : it) items.add(mapper.apply(t));
            return this;
        }
        
        public String build() {
            StringBuilder sb = new StringBuilder();
            for (String item : items) {
                sb.append("\n").append(indent).append(bullet).append(item);
            }
            return sb.toString();
        }
    }
    
    public static class PairBuilder {
        private final List<String[]> pairs = new ArrayList<>();
        private int padding = 0;
        
        public PairBuilder add(String key, Object value) {
            pairs.add(new String[] { key, format(value) });
            padding = Math.max(padding, key.length());
            return this;
        }
        public PairBuilder addAll(Map<String, ?> map) {
            for (Map.Entry<String, ?> e : map.entrySet()) add(e.getKey(), e.getValue());
            return this;
        }
        public PairBuilder separator() { pairs.add(null); return this; }
        
        public String build() {
            StringBuilder sb = new StringBuilder();
            for (String[] pair : pairs) {
                if (pair == null) {
                    sb.append("\n  ").append("-".repeat(padding + 10));
                } else {
                    sb.append("\n  ").append(String.format("%-" + padding + "s", pair[0])).append(": ").append(pair[1]);
                }
            }
            return sb.toString();
        }
    }
    
    public static class TableBuilder {
        private String[] headers;
        private final List<Object[]> rows = new ArrayList<>();
        private TableStyle style = TableStyle.ASCII;
        private final Map<Integer, Function<Object, String>> colFormatters = new HashMap<>();
        
        public TableBuilder headers(String... h) { headers = h; return this; }
        public TableBuilder row(Object... cells) { rows.add(cells); return this; }
        public <T> TableBuilder rows(Iterable<T> items, Function<T, Object[]> mapper) {
            for (T item : items) rows.add(mapper.apply(item));
            return this;
        }
        public TableBuilder style(TableStyle s) { style = s; return this; }
        public TableBuilder colFormatter(int col, Function<Object, String> f) { colFormatters.put(col, f); return this; }
        
        public String build() {
            if (headers == null || headers.length == 0) return "";
            
            int[] widths = new int[headers.length];
            for (int i = 0; i < headers.length; i++) widths[i] = headers[i].length();
            for (Object[] row : rows) {
                for (int i = 0; i < row.length && i < widths.length; i++) {
                    String cell = formatCell(i, row[i]);
                    widths[i] = Math.max(widths[i], cell.length());
                }
            }
            
            StringBuilder sb = new StringBuilder();
            
            // Header
            sb.append("\n").append(style.vertical);
            for (int i = 0; i < headers.length; i++) {
                sb.append(" ").append(String.format("%-" + widths[i] + "s", headers[i])).append(" ").append(style.vertical);
            }
            
            // Separator
            sb.append("\n").append(style.topLeft);
            for (int i = 0; i < headers.length; i++) {
                sb.append(style.horizontal.repeat(widths[i] + 2));
                sb.append(i < headers.length - 1 ? style.horizontal : style.topRight);
            }
            
            // Rows
            for (Object[] row : rows) {
                sb.append("\n").append(style.vertical);
                for (int i = 0; i < headers.length; i++) {
                    String cell = i < row.length ? formatCell(i, row[i]) : "";
                    sb.append(" ").append(String.format("%-" + widths[i] + "s", cell)).append(" ").append(style.vertical);
                }
            }
            
            return sb.toString();
        }
        
        private String formatCell(int col, Object value) {
            Function<Object, String> f = colFormatters.get(col);
            return f != null ? f.apply(value) : format(value);
        }
    }
    
    public static class TreeBuilder {
        private final String root;
        private final List<Object> nodes = new ArrayList<>();
        private int depth = 0;
        
        TreeBuilder(String root) { this.root = root; }
        
        public TreeBuilder branch(String name) {
            nodes.add(new int[] { depth, 1 }); // 1 = branch
            nodes.add(name);
            depth++;
            return this;
        }
        
        public TreeBuilder leaf(String name) {
            nodes.add(new int[] { depth, 0 }); // 0 = leaf
            nodes.add(name);
            return this;
        }
        
        public TreeBuilder up() {
            if (depth > 0) depth--;
            return this;
        }
        
        public String build() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n").append(root);
            for (int i = 0; i < nodes.size(); i += 2) {
                int[] meta = (int[]) nodes.get(i);
                String name = (String) nodes.get(i + 1);
                sb.append("\n").append("  ".repeat(meta[0] + 1));
                sb.append(meta[1] == 1 ? "├─ " : "└─ ").append(name);
            }
            return sb.toString();
        }
    }
}
