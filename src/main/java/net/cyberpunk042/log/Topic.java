package net.cyberpunk042.log;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A topic within a channel. Topics can have their own log level override.
 * Topics are cached per channel - same name returns same instance.
 */
public class Topic implements ContextBuilder<Context> {
    
    private final Channel channel;
    private final String name;
    
    Topic(Channel channel, String name) {
        this.channel = channel;
        this.name = name;
    }
    
    public String name() { return name; }
    public Channel channel() { return channel; }
    
    public LogLevel effectiveLevel() {
        return channel.effectiveLevel(name);
    }
    
    public boolean isEnabled() {
        return effectiveLevel() != LogLevel.OFF;
    }
    
    public boolean is(LogLevel level) {
        return effectiveLevel().includes(level);
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
    
    private Context context() {
        return new Context(channel, name, effectiveLevel());
    }
    
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
    
    public FormattedContext formatted() {
        return new FormattedContext(context());
    }
    
    // ========== SCOPED LOGGING ==========
    
    /**
     * Creates a deferred hierarchical log scope under this topic.
     * 
     * @param name The scope name
     * @return A new LogScope that emits at INFO level
     */
    public LogScope scope(String name) {
        return scope(name, LogLevel.INFO);
    }
    
    /**
     * Creates a deferred hierarchical log scope at specified level.
     * 
     * @param name The scope name
     * @param level The log level
     * @return A new LogScope
     */
    public LogScope scope(String name, LogLevel level) {
        if (!effectiveLevel().includes(level)) {
            return LogScope.noop();
        }
        return new LogScope(channel, this.name, name, level);
    }
    
    /**
     * Executes a lambda within a log scope, auto-emitting on completion.
     * 
     * @param name The scope name
     * @param consumer Lambda to execute
     */
    public void scoped(String name, java.util.function.Consumer<LogScope> consumer) {
        scoped(name, LogLevel.INFO, consumer);
    }
    
    /**
     * Executes a lambda within a log scope at specified level.
     * 
     * @param name The scope name
     * @param level The log level
     * @param consumer Lambda to execute
     */
    public void scoped(String name, LogLevel level, java.util.function.Consumer<LogScope> consumer) {
        try (LogScope scope = scope(name, level)) {
            consumer.accept(scope);
        }
    }
}
