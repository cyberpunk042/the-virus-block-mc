package net.cyberpunk042.command.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Generic enum-based suggestion provider.
 * Reduces boilerplate for enum tab-completion in commands.
 * 
 * <p>Usage:
 * <pre>
 * // Simple lowercase names
 * .suggests(EnumSuggester.of(MyEnum.class))
 * 
 * // Custom name mapper
 * .suggests(EnumSuggester.of(MyEnum.class, e -> e.getDisplayName()))
 * 
 * // With filter
 * .suggests(EnumSuggester.of(MyEnum.class, e -> e.isAvailable()))
 * </pre>
 */
public final class EnumSuggester {
    
    private EnumSuggester() {}
    
    /**
     * Creates a suggestion provider for an enum using lowercase names.
     */
    public static <E extends Enum<E>> SuggestionProvider<ServerCommandSource> of(Class<E> enumClass) {
        return of(enumClass, e -> e.name().toLowerCase());
    }
    
    /**
     * Creates a suggestion provider for an enum with a custom name mapper.
     */
    public static <E extends Enum<E>> SuggestionProvider<ServerCommandSource> of(
            Class<E> enumClass, 
            Function<E, String> nameMapper) {
        return (context, builder) -> {
            for (E value : enumClass.getEnumConstants()) {
                builder.suggest(nameMapper.apply(value));
            }
            return builder.buildFuture();
        };
    }
    
    /**
     * Creates a suggestion provider for an enum with filtering.
     */
    public static <E extends Enum<E>> SuggestionProvider<ServerCommandSource> ofFiltered(
            Class<E> enumClass,
            Function<E, String> nameMapper,
            java.util.function.Predicate<E> filter) {
        return (context, builder) -> {
            for (E value : enumClass.getEnumConstants()) {
                if (filter.test(value)) {
                    builder.suggest(nameMapper.apply(value));
                }
            }
            return builder.buildFuture();
        };
    }
    
    /**
     * Parses an enum value from a string argument (case-insensitive).
     * @return The enum value, or null if not found
     */
    public static <E extends Enum<E>> E parse(Class<E> enumClass, String value) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
    
    /**
     * Parses an enum value with a custom name mapper.
     * @return The enum value, or null if not found
     */
    public static <E extends Enum<E>> E parse(
            Class<E> enumClass, 
            String value, 
            Function<E, String> nameMapper) {
        for (E e : enumClass.getEnumConstants()) {
            if (nameMapper.apply(e).equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
