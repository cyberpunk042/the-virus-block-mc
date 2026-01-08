package net.cyberpunk042.command.util;

import java.util.List;
import java.util.function.Function;

import net.minecraft.util.Identifier;

/**
 * Generic utility for describing profiles as formatted strings.
 */
public final class ProfileDescriber {

    public record Field<T>(String name, Function<T, ?> extractor) {}

    private ProfileDescriber() {}

    /**
     * Describe a profile using its ID and a list of field extractors.
     */
    public static <T> String describe(
            T profile,
            Function<T, Identifier> idGetter,
            List<Field<T>> fields
    ) {
        if (profile == null) {
            return "unavailable";
        }
        
        StringBuilder sb = new StringBuilder();
        Identifier id = idGetter.apply(profile);
        sb.append(id != null ? id.toString() : "unknown");
        
        for (Field<T> field : fields) {
            sb.append(" ").append(field.name()).append("=");
            Object value = field.extractor().apply(profile);
            sb.append(CommandFormatters.formatAny(value));
        }
        
        return sb.toString();
    }

    /**
     * Describe a profile with a prefix label.
     */
    public static <T> String describeLabeled(
            String label,
            T profile,
            Function<T, Identifier> idGetter,
            List<Field<T>> fields
    ) {
        return label + ": " + describe(profile, idGetter, fields);
    }

    /**
     * Describe a nested sub-object (like a layer).
     */
    public static <T> String describeNested(
            String name,
            T object,
            List<Field<T>> fields
    ) {
        if (object == null) {
            return name + "=null";
        }
        
        StringBuilder sb = new StringBuilder(name).append("(");
        boolean first = true;
        for (Field<T> field : fields) {
            if (!first) sb.append(" ");
            first = false;
            sb.append(field.name()).append("=");
            Object value = field.extractor().apply(object);
            sb.append(CommandFormatters.formatAny(value));
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Create a field extractor.
     */
    public static <T> Field<T> field(String name, Function<T, ?> extractor) {
        return new Field<>(name, extractor);
    }

    /**
     * Describe an optional value with a default fallback.
     */
    public static String describeOptional(Object value, String defaultText) {
        if (value == null) {
            return defaultText;
        }
        return CommandFormatters.formatAny(value);
    }

    /**
     * Build a description incrementally.
     */
    public static class Builder<T> {
        private final StringBuilder sb = new StringBuilder();
        private final T profile;
        private boolean first = true;

        public Builder(T profile, Identifier id) {
            this.profile = profile;
            sb.append(id != null ? id.toString() : "unknown");
        }

        public Builder<T> add(String name, Function<T, ?> extractor) {
            sb.append(" ").append(name).append("=");
            sb.append(CommandFormatters.formatAny(extractor.apply(profile)));
            return this;
        }

        public Builder<T> addRaw(String name, Object value) {
            sb.append(" ").append(name).append("=");
            sb.append(CommandFormatters.formatAny(value));
            return this;
        }

        public String build() {
            return sb.toString();
        }
    }

    public static <T> Builder<T> builder(T profile, Identifier id) {
        return new Builder<>(profile, id);
    }
}
