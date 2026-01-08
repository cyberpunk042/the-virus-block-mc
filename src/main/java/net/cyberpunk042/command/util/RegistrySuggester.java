package net.cyberpunk042.command.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

/**
 * Reusable suggestion providers for command tab-completion.
 * 
 * @param <R> Registry type to extract suggestions from
 */
public class RegistrySuggester<R> {

    private final Supplier<R> registrySupplier;

    public RegistrySuggester(Supplier<R> registrySupplier) {
        this.registrySupplier = registrySupplier;
    }

    /**
     * Suggest identifiers from a registry extractor.
     */
    public CompletableFuture<Suggestions> suggest(
            CommandContext<ServerCommandSource> ctx,
            SuggestionsBuilder builder,
            Function<R, Collection<Identifier>> extractor
    ) {
        return suggestFiltered(ctx, builder, extractor, null);
    }

    /**
     * Suggest identifiers with optional filter.
     */
    public CompletableFuture<Suggestions> suggestFiltered(
            CommandContext<ServerCommandSource> ctx,
            SuggestionsBuilder builder,
            Function<R, Collection<Identifier>> extractor,
            @Nullable Predicate<String> filter
    ) {
        R registry = registrySupplier.get();
        Collection<Identifier> ids = registry != null ? extractor.apply(registry) : Collections.emptyList();
        String remaining = builder.getRemainingLowerCase();
        
        ids.stream()
                .map(Identifier::toString)
                .filter(id -> remaining.isEmpty() || id.toLowerCase(Locale.ROOT).startsWith(remaining))
                .filter(id -> filter == null || filter.test(id))
                .sorted()
                .forEach(builder::suggest);
        
        return builder.buildFuture();
    }

    /**
     * Suggest from a simple collection of identifiers.
     */
    public static CompletableFuture<Suggestions> suggestIdentifiers(
            SuggestionsBuilder builder,
            Collection<Identifier> ids
    ) {
        String remaining = builder.getRemainingLowerCase();
        ids.stream()
                .map(Identifier::toString)
                .filter(id -> remaining.isEmpty() || id.toLowerCase(Locale.ROOT).startsWith(remaining))
                .sorted()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * Suggest from a simple collection of strings.
     */
    public static CompletableFuture<Suggestions> suggestStrings(
            SuggestionsBuilder builder,
            Collection<String> values
    ) {
        String remaining = builder.getRemainingLowerCase();
        values.stream()
                .filter(v -> remaining.isEmpty() || v.toLowerCase(Locale.ROOT).startsWith(remaining))
                .sorted()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
