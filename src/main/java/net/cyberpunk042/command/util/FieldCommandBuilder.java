package net.cyberpunk042.command.util;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

/**
 * Builds command trees dynamically from field specifications.
 * 
 * @param <F> Field enum type
 * @param <R> Registry type for suggestions
 */
public class FieldCommandBuilder<F extends Enum<F>, R> {

    public record ProfileSpec<F, R>(
            String literal,
            F field,
            Function<R, Collection<Identifier>> suggester
    ) {}

    @FunctionalInterface
    public interface FieldHandler<F> {
        int handle(CommandContext<ServerCommandSource> ctx, F field, Object value) throws CommandSyntaxException;
    }

    private final RegistrySuggester<R> suggester;

    public FieldCommandBuilder(RegistrySuggester<R> suggester) {
        this.suggester = suggester;
    }

    /**
     * Attach profile selection commands (identifier fields with suggestions).
     */
    public void attachProfileCommands(
            LiteralArgumentBuilder<ServerCommandSource> parent,
            String rootLiteral,
            List<ProfileSpec<F, R>> specs,
            FieldHandler<F> handler
    ) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(rootLiteral);
        
        for (ProfileSpec<F, R> spec : specs) {
            root.then(CommandManager.literal(spec.literal())
                    .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                            .suggests((ctx, builder) -> suggester.suggest(ctx, builder, spec.suggester()))
                            .executes(ctx -> handler.handle(ctx, spec.field(),
                                    IdentifierArgumentType.getIdentifier(ctx, "id")))));
        }
        
        parent.then(root);
    }

    /**
     * Attach typed value commands (boolean, int, double fields).
     */
    public void attachValueCommands(
            LiteralArgumentBuilder<ServerCommandSource> parent,
            String rootLiteral,
            List<F> booleanFields,
            List<F> intFields,
            List<F> doubleFields,
            Function<F, String> literalizer,
            FieldHandler<F> handler
    ) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal(rootLiteral);
        
        for (F field : booleanFields) {
            root.then(CommandManager.literal(literalizer.apply(field))
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> handler.handle(ctx, field,
                                    BoolArgumentType.getBool(ctx, "value")))));
        }
        
        for (F field : intFields) {
            root.then(CommandManager.literal(literalizer.apply(field))
                    .then(CommandManager.argument("value", IntegerArgumentType.integer())
                            .executes(ctx -> handler.handle(ctx, field,
                                    IntegerArgumentType.getInteger(ctx, "value")))));
        }
        
        for (F field : doubleFields) {
            root.then(CommandManager.literal(literalizer.apply(field))
                    .then(CommandManager.argument("value", DoubleArgumentType.doubleArg())
                            .executes(ctx -> handler.handle(ctx, field,
                                    DoubleArgumentType.getDouble(ctx, "value")))));
        }
        
        parent.then(root);
    }

    /**
     * Create a literal builder.
     */
    public static LiteralArgumentBuilder<ServerCommandSource> literal(String name) {
        return CommandManager.literal(name);
    }
}
