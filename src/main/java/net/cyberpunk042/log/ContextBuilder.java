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

/**
 * Shared interface for context-building methods.
 * Implemented by Channel (-> Context), Topic (-> Context), Context (-> this).
 */
public interface ContextBuilder<T extends ContextBuilder<T>> {
    
    // Minecraft context
    T at(BlockPos pos);
    T at(Vec3d pos);
    T at(ChunkPos pos);
    T entity(Entity entity);
    T player(PlayerEntity player);
    T world(World world);
    T id(Identifier id);
    T box(Box box);
    
    // Generic context
    T kv(String key, Object value);
    T exception(Throwable throwable);
    T duration(long ticks);
    T ms(long millis);
    T count(String name, int value);
    T percent(String name, double value);
    T reason(String reason);
    T phase(String phase);
    
    // Collections (inline)
    <V> T list(String name, Iterable<V> items);
    <V> T list(String name, Iterable<V> items, Function<V, String> mapper);
    <V> T list(String name, V[] items);
    <V> T list(String name, V[] items, Function<V, String> mapper);
}
