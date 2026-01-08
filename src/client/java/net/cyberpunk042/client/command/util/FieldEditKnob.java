package net.cyberpunk042.client.command.util;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.cyberpunk042.client.gui.render.TestFieldRenderer;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.visual.validation.ValueRange;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Client-side command builder for /field edit commands.
 * 
 * <p>Creates commands that directly update {@link FieldEditState} without
 * server round-trips. Uses dot notation paths that map to {@code StateAccessor}.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * var orbit = ClientCommandManager.literal("orbit");
 * 
 * FieldEditKnob.toggle("orbit.enabled", "Orbit")
 *     .defaultValue(false)
 *     .attach(orbit);
 * 
 * FieldEditKnob.floatValue("orbit.radius", "Orbit radius")
 *     .range(0.1f, 50f)
 *     .unit("blocks")
 *     .defaultValue(1.0f)
 *     .attach(orbit);
 * 
 * FieldEditKnob.enumValue("orbit.axis", "Orbit axis", Axis.class)
 *     .defaultValue(Axis.Y)
 *     .attach(orbit);
 * </pre>
 * 
 * @see FieldEditState
 * @see FieldEditStateHolder
 */
public final class FieldEditKnob {
    
    private FieldEditKnob() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a float parameter knob.
     * @param path State path (e.g., "orbit.radius")
     * @param displayName Human-readable name for feedback
     */
    public static FloatBuilder floatValue(String path, String displayName) {
        return new FloatBuilder(path, displayName);
    }
    
    /**
     * Create an integer parameter knob.
     * @param path State path (e.g., "ring.segments")
     * @param displayName Human-readable name for feedback
     */
    public static IntBuilder intValue(String path, String displayName) {
        return new IntBuilder(path, displayName);
    }
    
    /**
     * Create a boolean toggle knob.
     * @param path State path (e.g., "orbit.enabled")
     * @param displayName Human-readable name for feedback
     */
    public static ToggleBuilder toggle(String path, String displayName) {
        return new ToggleBuilder(path, displayName);
    }
    
    /**
     * Create an enum selection knob.
     * @param path State path (e.g., "orbit.axis")
     * @param displayName Human-readable name for feedback
     * @param enumClass The enum class
     */
    public static <E extends Enum<E>> EnumBuilder<E> enumValue(String path, String displayName, Class<E> enumClass) {
        return new EnumBuilder<>(path, displayName, enumClass);
    }
    
    /**
     * Create a string parameter knob.
     * @param path State path (e.g., "appearance.color")
     * @param displayName Human-readable name for feedback
     */
    public static StringValueBuilder stringValue(String path, String displayName) {
        return new StringValueBuilder(path, displayName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Extracts command name from path: "orbit.radius" → "radius" */
    static String extractCommandName(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
    
    /** Updates state and triggers preview refresh */
    static void updateState(String path, Object value) {
        FieldEditState state = FieldEditStateHolder.get();
        if (state != null) {
            state.set(path, value);
            state.markDirty();
            TestFieldRenderer.markDirty();
        }
    }
    
    /** Sends success feedback */
    static void feedback(FabricClientCommandSource source, String displayName, String value) {
        source.sendFeedback(Text.literal(displayName + " set to: ")
            .formatted(Formatting.GREEN)
            .append(Text.literal(value).formatted(Formatting.WHITE)));
    }
    
    /** Sends current value feedback */
    static void showCurrent(FabricClientCommandSource source, String displayName, Object value, String unit) {
        String formatted = unit != null ? value + " " + unit : String.valueOf(value);
        source.sendFeedback(Text.literal(displayName + ": ")
            .formatted(Formatting.GRAY)
            .append(Text.literal(formatted).formatted(Formatting.AQUA)));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FLOAT BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class FloatBuilder {
        private final String path;
        private final String displayName;
        private float min = -Float.MAX_VALUE;
        private float max = Float.MAX_VALUE;
        private String unit;
        private float defaultValue = 0f;
        
        private FloatBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public FloatBuilder range(float min, float max) {
            this.min = min;
            this.max = max;
            return this;
        }
        
        /**
         * Set range from ValueRange (extracts min, max, AND unit automatically).
         */
        public FloatBuilder range(ValueRange vr) {
            this.min = vr.min();
            this.max = vr.max();
            this.unit = vr.unit();
            return this;
        }
        
        public FloatBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }
        
        public FloatBuilder defaultValue(float value) {
            this.defaultValue = value;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<FabricClientCommandSource> parent) {
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<FabricClientCommandSource> build() {
            String cmdName = extractCommandName(path);
            return ClientCommandManager.literal(cmdName)
                .executes(ctx -> {
                    FieldEditState state = FieldEditStateHolder.get();
                    float current = state != null ? state.getFloat(path) : defaultValue;
                    showCurrent(ctx.getSource(), displayName, current, unit);
                    return 1;
                })
                .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(min, max))
                    .executes(ctx -> {
                        float value = FloatArgumentType.getFloat(ctx, "value");
                        updateState(path, value);
                        String formatted = unit != null ? value + " " + unit : String.valueOf(value);
                        feedback(ctx.getSource(), displayName, formatted);
                        return 1;
                    }));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INT BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class IntBuilder {
        private final String path;
        private final String displayName;
        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;
        private String unit;
        private int defaultValue = 0;
        
        private IntBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public IntBuilder range(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }
        
        /**
         * Set range from ValueRange (extracts min, max, AND unit automatically).
         */
        public IntBuilder range(ValueRange vr) {
            this.min = (int) vr.min();
            this.max = (int) vr.max();
            this.unit = vr.unit();
            return this;
        }
        
        public IntBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }
        
        public IntBuilder defaultValue(int value) {
            this.defaultValue = value;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<FabricClientCommandSource> parent) {
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<FabricClientCommandSource> build() {
            String cmdName = extractCommandName(path);
            return ClientCommandManager.literal(cmdName)
                .executes(ctx -> {
                    FieldEditState state = FieldEditStateHolder.get();
                    int current = state != null ? state.getInt(path) : defaultValue;
                    showCurrent(ctx.getSource(), displayName, current, unit);
                    return 1;
                })
                .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(min, max))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        updateState(path, value);
                        String formatted = unit != null ? value + " " + unit : String.valueOf(value);
                        feedback(ctx.getSource(), displayName, formatted);
                        return 1;
                    }));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TOGGLE BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class ToggleBuilder {
        private final String path;
        private final String displayName;
        private boolean defaultValue = false;
        
        private ToggleBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public ToggleBuilder defaultValue(boolean value) {
            this.defaultValue = value;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<FabricClientCommandSource> parent) {
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<FabricClientCommandSource> build() {
            String cmdName = extractCommandName(path);
            return ClientCommandManager.literal(cmdName)
                .executes(ctx -> {
                    FieldEditState state = FieldEditStateHolder.get();
                    boolean current = state != null ? state.getBool(path) : defaultValue;
                    showCurrent(ctx.getSource(), displayName, current ? "enabled" : "disabled", null);
                    return 1;
                })
                .then(ClientCommandManager.literal("enable")
                    .executes(ctx -> {
                        updateState(path, true);
                        feedback(ctx.getSource(), displayName, "enabled");
                        return 1;
                    }))
                .then(ClientCommandManager.literal("disable")
                    .executes(ctx -> {
                        updateState(path, false);
                        feedback(ctx.getSource(), displayName, "disabled");
                        return 1;
                    }))
                .then(ClientCommandManager.literal("toggle")
                    .executes(ctx -> {
                        FieldEditState state = FieldEditStateHolder.get();
                        boolean current = state != null && state.getBool(path);
                        boolean newValue = !current;
                        updateState(path, newValue);
                        feedback(ctx.getSource(), displayName, newValue ? "enabled" : "disabled");
                        return 1;
                    }));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ENUM BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class EnumBuilder<E extends Enum<E>> {
        private final String path;
        private final String displayName;
        private final Class<E> enumClass;
        private E defaultValue;
        private Function<E, String> idMapper = e -> e.name().toLowerCase();
        private Function<String, E> parser;
        
        private EnumBuilder(String path, String displayName, Class<E> enumClass) {
            this.path = path;
            this.displayName = displayName;
            this.enumClass = enumClass;
            // Default parser
            this.parser = id -> {
                for (E e : enumClass.getEnumConstants()) {
                    if (e.name().equalsIgnoreCase(id)) return e;
                }
                return null;
            };
        }
        
        public EnumBuilder<E> defaultValue(E value) {
            this.defaultValue = value;
            return this;
        }
        
        public EnumBuilder<E> idMapper(Function<E, String> mapper) {
            this.idMapper = mapper;
            return this;
        }
        
        public EnumBuilder<E> parser(Function<String, E> parser) {
            this.parser = parser;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<FabricClientCommandSource> parent) {
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<FabricClientCommandSource> build() {
            String cmdName = extractCommandName(path);
            
            // Create suggestion provider
            SuggestionProvider<FabricClientCommandSource> suggestions = (ctx, builder) -> 
                CommandSource.suggestMatching(
                    Arrays.stream(enumClass.getEnumConstants())
                        .map(idMapper)
                        .collect(Collectors.toList()),
                    builder);
            
            return ClientCommandManager.literal(cmdName)
                .executes(ctx -> {
                    FieldEditState state = FieldEditStateHolder.get();
                    String current = state != null ? state.getString(path) : 
                        (defaultValue != null ? idMapper.apply(defaultValue) : "none");
                    showCurrent(ctx.getSource(), displayName, current, null);
                    return 1;
                })
                .then(ClientCommandManager.argument("value", StringArgumentType.word())
                    .suggests(suggestions)
                    .executes(ctx -> {
                        String input = StringArgumentType.getString(ctx, "value");
                        E parsed = parser.apply(input);
                        if (parsed == null) {
                            ctx.getSource().sendError(Text.literal("Unknown value: " + input));
                            return 0;
                        }
                        String id = idMapper.apply(parsed);
                        updateState(path, id);
                        feedback(ctx.getSource(), displayName, id);
                        return 1;
                    }));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STRING BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class StringValueBuilder {
        private final String path;
        private final String displayName;
        private String defaultValue = "";
        private SuggestionProvider<FabricClientCommandSource> suggestions;
        
        private StringValueBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public StringValueBuilder defaultValue(String value) {
            this.defaultValue = value;
            return this;
        }
        
        public StringValueBuilder suggestions(SuggestionProvider<FabricClientCommandSource> suggestions) {
            this.suggestions = suggestions;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<FabricClientCommandSource> parent) {
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<FabricClientCommandSource> build() {
            String cmdName = extractCommandName(path);
            var argBuilder = ClientCommandManager.argument("value", StringArgumentType.word());
            
            if (suggestions != null) {
                argBuilder.suggests(suggestions);
            }
            
            return ClientCommandManager.literal(cmdName)
                .executes(ctx -> {
                    FieldEditState state = FieldEditStateHolder.get();
                    String current = state != null ? state.getString(path) : defaultValue;
                    showCurrent(ctx.getSource(), displayName, current, null);
                    return 1;
                })
                .then(argBuilder
                    .executes(ctx -> {
                        String value = StringArgumentType.getString(ctx, "value");
                        updateState(path, value);
                        feedback(ctx.getSource(), displayName, value);
                        return 1;
                    }));
        }
    }
}

