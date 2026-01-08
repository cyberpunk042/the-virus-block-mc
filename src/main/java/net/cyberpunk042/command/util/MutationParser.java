package net.cyberpunk042.command.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Reusable parser for field=value mutation syntax.
 * 
 * @param <F> Field enum type
 * @param <M> Mutation container type
 */
public class MutationParser<F extends Enum<F>, M> {

    public static final SimpleCommandExceptionType EMPTY_MUTATION = 
            new SimpleCommandExceptionType(Text.literal("Provide at least one field=value pair."));
    public static final DynamicCommandExceptionType UNKNOWN_FIELD = 
            new DynamicCommandExceptionType(name -> Text.literal("Unknown field: " + name));
    public static final DynamicCommandExceptionType INVALID_VALUE = 
            new DynamicCommandExceptionType(value -> Text.literal("Invalid value: " + value));
    public static final DynamicCommandExceptionType MALFORMED_PAIR = 
            new DynamicCommandExceptionType(token -> Text.literal("Expected field=value but found: " + token));

    public record ParsedMutation<F, M>(M mutation, String summary, List<FieldChange<F>> changes) {}
    public record FieldChange<F>(F field, Object value, boolean cleared) {}

    /** Value types for field=value parsing. */
    public enum ValueType { BOOLEAN, INT, DOUBLE, IDENTIFIER }

    private final Map<String, F> fieldLookup;
    private final Function<F, ValueType> typeResolver;
    private final MutationFactory<F, M> mutationFactory;

    @FunctionalInterface
    public interface MutationFactory<F, M> {
        M create();
    }

    @FunctionalInterface
    public interface MutationApplicator<F, M> {
        void apply(M mutation, F field, Object value, boolean cleared);
    }

    private final MutationApplicator<F, M> applicator;

    public MutationParser(
            Map<String, F> fieldLookup,
            Function<F, ValueType> typeResolver,
            MutationFactory<F, M> mutationFactory,
            MutationApplicator<F, M> applicator
    ) {
        this.fieldLookup = fieldLookup;
        this.typeResolver = typeResolver;
        this.mutationFactory = mutationFactory;
        this.applicator = applicator;
    }

    public ParsedMutation<F, M> parse(String raw) throws CommandSyntaxException {
        if (raw == null || raw.trim().isEmpty()) {
            throw EMPTY_MUTATION.create();
        }
        
        M mutation = mutationFactory.create();
        StringJoiner summary = new StringJoiner(", ");
        List<FieldChange<F>> changes = new ArrayList<>();
        
        for (String token : raw.trim().split("\\s+")) {
            if (token.isEmpty()) continue;
            
            int separator = token.indexOf('=');
            if (separator < 0) {
                throw MALFORMED_PAIR.create(token);
            }
            
            String key = token.substring(0, separator);
            String value = token.substring(separator + 1);
            
            F field = resolveField(key);
            FieldChange<F> change = applyFieldValue(mutation, field, value);
            changes.add(change);
            
            if (change.cleared()) {
                summary.add(field.name().toLowerCase(Locale.ROOT) + "=<cleared>");
            } else {
                summary.add(field.name().toLowerCase(Locale.ROOT) + "=" + value);
            }
        }
        
        return new ParsedMutation<>(mutation, summary.toString(), List.copyOf(changes));
    }

    public ParsedMutation<F, M> singleField(F field, Object value) throws CommandSyntaxException {
        M mutation = mutationFactory.create();
        applicator.apply(mutation, field, value, false);
        FieldChange<F> change = new FieldChange<>(field, value, false);
        String summary = field.name().toLowerCase(Locale.ROOT) + "=" + CommandFormatters.formatAny(value);
        return new ParsedMutation<>(mutation, summary, List.of(change));
    }

    private F resolveField(String raw) throws CommandSyntaxException {
        if (raw == null || raw.isEmpty()) {
            throw UNKNOWN_FIELD.create(raw);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        F field = fieldLookup.get(normalized);
        if (field == null) {
            throw UNKNOWN_FIELD.create(raw);
        }
        return field;
    }

    private FieldChange<F> applyFieldValue(M mutation, F field, String rawValue) throws CommandSyntaxException {
        String value = rawValue == null ? "" : rawValue.trim();
        
        if (isClearToken(value)) {
            applicator.apply(mutation, field, null, true);
            return new FieldChange<>(field, null, true);
        }
        
        try {
            ValueType type = typeResolver.apply(field);
            Object parsed = switch (type) {
                case BOOLEAN -> parseBoolean(value);
                case INT -> Integer.parseInt(value);
                case DOUBLE -> Double.parseDouble(value);
                case IDENTIFIER -> {
                    Identifier id = Identifier.tryParse(value);
                    if (id == null) throw INVALID_VALUE.create(value + " (expected identifier)");
                    yield id;
                }
            };
            applicator.apply(mutation, field, parsed, false);
            return new FieldChange<>(field, parsed, false);
        } catch (NumberFormatException ex) {
            throw INVALID_VALUE.create(value + " (for " + field.name().toLowerCase(Locale.ROOT) + ")");
        }
    }

    public static boolean parseBoolean(String value) throws CommandSyntaxException {
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> throw INVALID_VALUE.create(value + " (expected boolean)");
        };
    }

    public static boolean isClearToken(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.isEmpty()
                || normalized.equals("clear")
                || normalized.equals("none")
                || normalized.equals("null")
                || normalized.equals("-");
    }
}
