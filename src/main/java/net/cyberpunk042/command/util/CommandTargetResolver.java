package net.cyberpunk042.command.util;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Reusable utilities for resolving command targets (blocks, items).
 */
public final class CommandTargetResolver {

    public static final double DEFAULT_TRACE_DISTANCE = 8.0D;

    public record StackTarget(ItemStack stack, Hand hand) {}

    private CommandTargetResolver() {}

    /**
     * Raycast to find a block entity of the given type.
     */
    @Nullable
    public static <T extends BlockEntity> T traceBlockEntity(ServerPlayerEntity player, Class<T> type) {
        return traceBlockEntity(player, DEFAULT_TRACE_DISTANCE, type);
    }

    @Nullable
    public static <T extends BlockEntity> T traceBlockEntity(ServerPlayerEntity player, double distance, Class<T> type) {
        HitResult hit = player.raycast(distance, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockEntity entity = player.getWorld().getBlockEntity(blockHit.getBlockPos());
            if (type.isInstance(entity)) {
                return type.cast(entity);
            }
        }
        return null;
    }

    /**
     * Find an item in either hand matching the predicate.
     */
    @Nullable
    public static StackTarget findHeldItem(ServerPlayerEntity player, Predicate<ItemStack> predicate) {
        ItemStack main = player.getMainHandStack();
        if (predicate.test(main)) {
            return new StackTarget(main, Hand.MAIN_HAND);
        }
        ItemStack off = player.getOffHandStack();
        if (predicate.test(off)) {
            return new StackTarget(off, Hand.OFF_HAND);
        }
        return null;
    }

    /**
     * Require a block entity or throw.
     */
    public static <T extends BlockEntity> T requireBlockEntity(
            ServerPlayerEntity player,
            Class<T> type,
            SimpleCommandExceptionType noTargetException
    ) throws CommandSyntaxException {
        T entity = traceBlockEntity(player, type);
        if (entity == null) {
            throw noTargetException.create();
        }
        return entity;
    }

    /**
     * Require a held item or throw.
     */
    public static StackTarget requireHeldItem(
            ServerPlayerEntity player,
            Predicate<ItemStack> predicate,
            SimpleCommandExceptionType noStackException
    ) throws CommandSyntaxException {
        StackTarget target = findHeldItem(player, predicate);
        if (target == null) {
            throw noStackException.create();
        }
        return target;
    }

    /**
     * Create a simple exception type with a message.
     */
    public static SimpleCommandExceptionType exception(String message) {
        return new SimpleCommandExceptionType(Text.literal(message));
    }
}
