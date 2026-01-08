package net.cyberpunk042.log;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A log channel with level control, topic support, and chat forwarding.
 * Channels are created via {@link Channel#of} and registered in {@link Logging}.
 */
public class Channel implements ContextBuilder<Context> {
    
    private final String id;
    private final String label;
    private final LogLevel defaultLevel;
    
    private volatile LogLevel level;
    private volatile boolean chatForward = false;
    private final ConcurrentHashMap<String, LogLevel> topicLevels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Topic> topicCache = new ConcurrentHashMap<>();
    
    private Channel(String id, String label, LogLevel defaultLevel) {
        this.id = id;
        this.label = label;
        this.defaultLevel = defaultLevel;
        this.level = defaultLevel;
    }
    
    public static Channel of(String id, String label, LogLevel defaultLevel) {
        return new Channel(id, label, defaultLevel);
    }
    
    // ========== IDENTITY ==========
    
    public String id() { return id; }
    public String label() { return label; }
    public LogLevel defaultLevel() { return defaultLevel; }
    
    // ========== STATE ==========
    
    public LogLevel level() { return level; }
    public boolean chatForward() { return chatForward; }
    
    public void setLevel(LogLevel level) { this.level = level; }
    public void setChatForward(boolean forward) { this.chatForward = forward; }
    
    public void setTopicLevel(String topic, LogLevel level) {
        topicLevels.put(topic, level);
    }
    
    public void clearTopicLevel(String topic) {
        topicLevels.remove(topic);
    }
    
    public LogLevel effectiveLevel(String topic) {
        if (topic == null) return level;
        return topicLevels.getOrDefault(topic, level);
    }
    
    // ========== CHECKS ==========
    
    public boolean isEnabled() { return level != LogLevel.OFF; }
    public boolean is(LogLevel l) { return level.includes(l); }
    
    // ========== TOPIC ACCESS ==========
    
    public Topic topic(String name) {
        return topicCache.computeIfAbsent(name, n -> new Topic(this, n));
    }
    
    // ========== QUICK LOG (terminal) ==========
    
    public void trace(String msg, Object... args) { context().trace(msg, args); }
    public void trace(Supplier<String> s) { if (is(LogLevel.TRACE)) context().trace(s.get()); }
    
    public void debug(String msg, Object... args) { context().debug(msg, args); }
    public void debug(Supplier<String> s) { if (is(LogLevel.DEBUG)) context().debug(s.get()); }
    
    public void info(String msg, Object... args) { context().info(msg, args); }
    public void info(Supplier<String> s) { if (is(LogLevel.INFO)) context().info(s.get()); }
    
    public void warn(String msg, Object... args) { context().warn(msg, args); }
    public void warn(Supplier<String> s) { if (is(LogLevel.WARN)) context().warn(s.get()); }
    
    public void error(String msg, Object... args) { context().error(msg, args); }
    public void error(Supplier<String> s) { if (is(LogLevel.ERROR)) context().error(s.get()); }
    
    // ========== CONTEXT BUILDER ==========
    
    private Context context() { return new Context(this); }
    
    @Override public Context at(BlockPos pos) { return context().at(pos); }
    @Override public Context at(Vec3d pos) { return context().at(pos); }
    @Override public Context at(ChunkPos pos) { return context().at(pos); }
    @Override public Context entity(Entity entity) { return context().entity(entity); }
    @Override public Context player(PlayerEntity player) { return context().player(player); }
    @Override public Context world(World world) { return context().world(world); }
    @Override public Context id(Identifier id) { return context().id(id); }
    @Override public Context box(Box box) { return context().box(box); }
    @Override public Context kv(String key, Object value) { return context().kv(key, value); }
    @Override public Context exception(Throwable t) { return context().exception(t); }
    @Override public Context duration(long ticks) { return context().duration(ticks); }
    @Override public Context ms(long millis) { return context().ms(millis); }
    @Override public Context count(String name, int value) { return context().count(name, value); }
    @Override public Context percent(String name, double value) { return context().percent(name, value); }
    @Override public Context reason(String reason) { return context().reason(reason); }
    @Override public Context phase(String phase) { return context().phase(phase); }
    @Override public <V> Context list(String name, Iterable<V> items) { return context().list(name, items); }
    @Override public <V> Context list(String name, Iterable<V> items, Function<V, String> mapper) { return context().list(name, items, mapper); }
    @Override public <V> Context list(String name, V[] items) { return context().list(name, items); }
    @Override public <V> Context list(String name, V[] items, Function<V, String> mapper) { return context().list(name, items, mapper); }
    
    public FormattedContext formatted() { return new FormattedContext(context()); }
    
    // ========== SCOPED LOGGING ==========
    
    /**
     * Creates a deferred hierarchical log scope.
     * 
     * <p>Use with try-with-resources for automatic emission on close:</p>
     * <pre>
     * try (LogScope frame = Logging.FIELD.scope("render-frame")) {
     *     frame.branch("layer:0").kv("primitives", 5);
     * }
     * </pre>
     * 
     * @param name The scope name (root node name)
     * @return A new LogScope that emits at INFO level
     */
    public LogScope scope(String name) {
        return scope(name, LogLevel.INFO);
    }
    
    /**
     * Creates a deferred hierarchical log scope at specified level.
     * 
     * @param name The scope name (root node name)
     * @param level The log level to emit at
     * @return A new LogScope
     */
    public LogScope scope(String name, LogLevel level) {
        if (!this.level.includes(level)) {
            return LogScope.noop();
        }
        return new LogScope(this, name, level);
    }
    
    /**
     * Executes a lambda within a log scope, auto-emitting on completion.
     * 
     * <pre>
     * Logging.FIELD.scoped("render-frame", frame -> {
     *     frame.branch("layer:0").kv("primitives", 5);
     * });
     * </pre>
     * 
     * @param name The scope name
     * @param consumer Lambda to execute with the scope
     */
    public void scoped(String name, java.util.function.Consumer<LogScope> consumer) {
        scoped(name, LogLevel.INFO, consumer);
    }
    
    /**
     * Executes a lambda within a log scope at specified level.
     * 
     * @param name The scope name
     * @param level The log level
     * @param consumer Lambda to execute with the scope
     */
    public void scoped(String name, LogLevel level, java.util.function.Consumer<LogScope> consumer) {
        try (LogScope scope = scope(name, level)) {
            consumer.accept(scope);
        }
    }
    
    // ========== RESET ==========
    
    public void reset() {
        this.level = defaultLevel;
        this.chatForward = false;
        topicLevels.clear();
    }
}
