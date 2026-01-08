package net.cyberpunk042.log;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Rich formatted output builder.
 * Returned by Channel/Topic/Context.formatted().
 */
public class FormattedContext {
    
    private final Context parent;
    private final List<String> sections = new ArrayList<>();
    
    FormattedContext(Context parent) {
        this.parent = parent;
    }
    
    /**
     * Force this message to be sent to chat, regardless of channel's chatForward setting.
     * Use for critical errors that players MUST see in-game.
     */
    public FormattedContext alwaysChat() {
        parent.alwaysChat();
        return this;
    }
    
    // ========== STRUCTURE ==========
    
    public FormattedContext heading(String text) {
        sections.add("\n=== " + text + " ===");
        return this;
    }
    
    public FormattedContext blank() {
        sections.add("");
        return this;
    }
    
    public FormattedContext line(String text) {
        sections.add("\n" + text);
        return this;
    }
    
    // ========== CUSTOM INJECTION ==========
    
    public FormattedContext raw(String text) {
        sections.add(text);
        return this;
    }
    
    public FormattedContext block(Supplier<String> supplier) {
        sections.add(supplier.get());
        return this;
    }
    
    public FormattedContext section(LogSection section) {
        sections.add(section.render());
        return this;
    }
    
    public FormattedContext transform(Function<String, String> transformer) {
        if (!sections.isEmpty()) {
            int last = sections.size() - 1;
            sections.set(last, transformer.apply(sections.get(last)));
        }
        return this;
    }
    
    // ========== SUB-BUILDERS ==========
    
    public FormattedPairs pairs() {
        return new FormattedPairs(this);
    }
    
    public FormattedTable table(String title) {
        return new FormattedTable(this, title);
    }
    
    public <T> FormattedContext list(String title, Iterable<T> items) {
        return list(title, items, Object::toString);
    }
    
    public <T> FormattedContext list(String title, Iterable<T> items, Function<T, String> mapper) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(title).append(":");
        for (T item : items) {
            sb.append("\n  - ").append(mapper.apply(item));
        }
        sections.add(sb.toString());
        return this;
    }
    
    public FormattedTree tree(String title) {
        return new FormattedTree(this, title);
    }
    
    // ========== TERMINAL ==========
    
    public void trace() { log(LogLevel.TRACE); }
    public void debug() { log(LogLevel.DEBUG); }
    public void info() { log(LogLevel.INFO); }
    public void warn() { log(LogLevel.WARN); }
    public void error() { log(LogLevel.ERROR); }
    
    private void log(LogLevel level) {
        StringBuilder sb = new StringBuilder();
        for (String section : sections) sb.append(section);
        parent.log(level, sb.toString());
    }
    
    void addSection(String content) {
        sections.add(content);
    }
}
