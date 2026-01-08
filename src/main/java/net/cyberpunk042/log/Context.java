package net.cyberpunk042.log;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fluent builder for log messages with contextual fields.
 * Single-use: chain methods, then call terminal (info/warn/error/debug/trace).
 */
public class Context implements ContextBuilder<Context> {
    
    private final Channel channel;
    private final String topicName;
    private final LogLevel effectiveLevel;
    private final List<String> fields = new ArrayList<>(4);
    private Throwable exception = null;
    private boolean forceChat = false;
    
    Context(Channel channel) {
        this.channel = channel;
        this.topicName = null;
        this.effectiveLevel = channel.level();
    }
    
    Context(Channel channel, String topicName, LogLevel effectiveLevel) {
        this.channel = channel;
        this.topicName = topicName;
        this.effectiveLevel = effectiveLevel;
    }
    
    // ========== MINECRAFT CONTEXT ==========
    
    @Override
    public Context at(BlockPos pos) {
        if (pos != null) fields.add("pos=[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]");
        return this;
    }
    
    @Override
    public Context at(Vec3d pos) {
        if (pos != null) fields.add(String.format("pos=[%.1f,%.1f,%.1f]", pos.x, pos.y, pos.z));
        return this;
    }
    
    @Override
    public Context at(ChunkPos pos) {
        if (pos != null) fields.add("chunk=[" + pos.x + "," + pos.z + "]");
        return this;
    }
    
    @Override
    public Context entity(Entity entity) {
        if (entity != null) fields.add("entity=" + entity.getName().getString());
        return this;
    }
    
    @Override
    public Context player(PlayerEntity player) {
        if (player != null) fields.add("player=" + player.getName().getString());
        return this;
    }
    
    @Override
    public Context world(World world) {
        if (world != null) fields.add("world=" + world.getRegistryKey().getValue().getPath());
        return this;
    }
    
    @Override
    public Context id(Identifier id) {
        if (id != null) fields.add("id=" + id.toString());
        return this;
    }
    
    @Override
    public Context box(Box box) {
        if (box != null) {
            fields.add(String.format("box=[%.0f,%.0f,%.0f->%.0f,%.0f,%.0f]",
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
        }
        return this;
    }
    
    // ========== GENERIC CONTEXT ==========
    
    @Override
    public Context kv(String key, Object value) {
        if (key != null && value != null) fields.add(key + "=" + LogFormat.format(value));
        return this;
    }
    
    @Override
    public Context exception(Throwable throwable) {
        this.exception = throwable;
        return this;
    }
    
    @Override
    public Context duration(long ticks) {
        fields.add("duration=" + formatTicks(ticks));
        return this;
    }
    
    @Override
    public Context ms(long millis) {
        fields.add("time=" + millis + "ms");
        return this;
    }
    
    @Override
    public Context count(String name, int value) {
        fields.add(name + "=" + value);
        return this;
    }
    
    @Override
    public Context percent(String name, double value) {
        fields.add(name + "=" + String.format("%.1f%%", value * 100));
        return this;
    }
    
    @Override
    public Context reason(String reason) {
        if (reason != null) fields.add("reason=" + reason);
        return this;
    }
    
    @Override
    public Context phase(String phase) {
        if (phase != null) fields.add("phase=" + phase);
        return this;
    }
    
    /**
     * Force this message to be sent to chat, regardless of channel's chatForward setting.
     * Use for critical errors that players MUST see in-game.
     */
    public Context alwaysChat() {
        this.forceChat = true;
        return this;
    }
    
    // ========== COLLECTIONS ==========
    
    @Override
    public <V> Context list(String name, Iterable<V> items) {
        return list(name, items, Object::toString);
    }
    
    @Override
    public <V> Context list(String name, Iterable<V> items, Function<V, String> mapper) {
        if (name != null && items != null) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (V item : items) joiner.add(mapper.apply(item));
            fields.add(name + "=" + joiner);
        }
        return this;
    }
    
    @Override
    public <V> Context list(String name, V[] items) {
        return list(name, items, Object::toString);
    }
    
    @Override
    public <V> Context list(String name, V[] items, Function<V, String> mapper) {
        if (name != null && items != null) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (V item : items) joiner.add(mapper.apply(item));
            fields.add(name + "=" + joiner);
        }
        return this;
    }
    
    // ========== TERMINAL METHODS ==========
    
    public void trace(String message, Object... args) { log(LogLevel.TRACE, message, args); }
    public void trace(Supplier<String> s) { if (effectiveLevel.includes(LogLevel.TRACE)) log(LogLevel.TRACE, s.get()); }
    
    public void debug(String message, Object... args) { log(LogLevel.DEBUG, message, args); }
    public void debug(Supplier<String> s) { if (effectiveLevel.includes(LogLevel.DEBUG)) log(LogLevel.DEBUG, s.get()); }
    
    public void info(String message, Object... args) { log(LogLevel.INFO, message, args); }
    public void info(Supplier<String> s) { if (effectiveLevel.includes(LogLevel.INFO)) log(LogLevel.INFO, s.get()); }
    
    public void warn(String message, Object... args) { log(LogLevel.WARN, message, args); }
    public void warn(Supplier<String> s) { if (effectiveLevel.includes(LogLevel.WARN)) log(LogLevel.WARN, s.get()); }
    
    public void error(String message, Object... args) { log(LogLevel.ERROR, message, args); }
    public void error(Supplier<String> s) { if (effectiveLevel.includes(LogLevel.ERROR)) log(LogLevel.ERROR, s.get()); }
    
    public FormattedContext formatted() { return new FormattedContext(this); }
    
    // ========== INTERNAL ==========
    
    void log(LogLevel level, String message, Object... args) {
        if (!effectiveLevel.includes(level)) return;
        String fullMessage = buildMessage(message, args);
        LogOutput.emit(channel, topicName, level, fullMessage, exception, forceChat);
    }
    
    private String buildMessage(String message, Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(channel.label());
        if (topicName != null) sb.append(":").append(topicName);
        sb.append("] ");
        for (String field : fields) sb.append(field).append(" ");
        sb.append(args.length > 0 ? formatSLF4J(message, args) : message);
        return sb.toString();
    }
    
    private String formatSLF4J(String pattern, Object... args) {
        if (pattern == null) return "";
        StringBuilder result = new StringBuilder();
        int argIndex = 0, i = 0;
        while (i < pattern.length()) {
            if (i < pattern.length() - 1 && pattern.charAt(i) == '{' && pattern.charAt(i + 1) == '}') {
                result.append(argIndex < args.length ? LogFormat.format(args[argIndex++]) : "{}");
                i += 2;
            } else {
                result.append(pattern.charAt(i++));
            }
        }
        return result.toString();
    }
    
    private static String formatTicks(long ticks) {
        if (ticks < 20) return ticks + "t";
        if (ticks < 20 * 60) return String.format("%.1fs", ticks / 20.0);
        if (ticks < 20 * 60 * 60) return String.format("%.1fm", ticks / (20.0 * 60));
        return String.format("%.1fh", ticks / (20.0 * 60 * 60));
    }
    
    // Accessors for FormattedContext
    Channel channel() { return channel; }
    String topicName() { return topicName; }
    LogLevel effectiveLevel() { return effectiveLevel; }
    List<String> fields() { return fields; }
    Throwable exception() { return exception; }
    boolean forceChat() { return forceChat; }
}
