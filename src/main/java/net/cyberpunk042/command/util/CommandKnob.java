package net.cyberpunk042.command.util;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Unified command builder system with built-in protection.
 * 
 * <p>Every command knob has:
 * <ul>
 *   <li>A path (required) - for protection lookup</li>
 *   <li>A display name (required) - for feedback</li>
 *   <li>A default value (required) - auto-registered</li>
 *   <li>A handler (required) - executes the change</li>
 *   <li>Protection check (automatic) - via CommandProtection</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * // Toggle
 * CommandKnob.toggle("singularity.collapse", "Singularity collapse")
 *     .defaultValue(true)
 *     .handler((source, enabled) -> getFacade(source).setCollapseEnabled(enabled))
 *     .attach(parent);
 * 
 * // Value
 * CommandKnob.value("erosion.outline", "Outline thickness")
 *     .range(1, 16)
 *     .unit("blocks")
 *     .defaultValue(1)
 *     .handler((source, value) -> getFacade(source).setOutlineThickness(value))
 *     .attach(parent);
 * 
 * // Enum
 * CommandKnob.enumValue("erosion.fill_mode", "Fill mode", CollapseFillMode.class)
 *     .idMapper(CollapseFillMode::id)
 *     .parser(CollapseFillMode::fromId)
 *     .defaultValue(CollapseFillMode.AIR)
 *     .handler((source, mode) -> getFacade(source).setFillMode(mode))
 *     .attach(parent);
 * </pre>
 */
public final class CommandKnob {
    
    private CommandKnob() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Factory Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a toggle (enable/disable) knob.
     */
    public static ToggleBuilder toggle(String path, String displayName) {
        return new ToggleBuilder(path, displayName);
    }
    
    /**
     * Create an integer value knob.
     */
    public static ValueBuilder value(String path, String displayName) {
        return new ValueBuilder(path, displayName);
    }
    
    /**
     * Create an enum value knob.
     */
    public static <E extends Enum<E>> EnumBuilder<E> enumValue(String path, String displayName, Class<E> enumClass) {
        return new EnumBuilder<>(path, displayName, enumClass);
    }
    
    /**
     * Create an action command (no value, just executes).
     * Used for reload, spawn, clear operations that should still be protectable.
     */
    public static ActionBuilder action(String path, String displayName) {
        return new ActionBuilder(path, displayName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Action Builder (for commands without values)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class ActionBuilder {
        private final String path;
        private final String displayName;
        private Function<ServerCommandSource, Boolean> handler;
        private BiPredicate<ServerCommandSource, Object> scenarioCheck;
        private String successMessage;
        
        private ActionBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public ActionBuilder handler(Function<ServerCommandSource, Boolean> handler) {
            this.handler = handler;
            return this;
        }
        
        public ActionBuilder scenarioRequired(BiPredicate<ServerCommandSource, Object> check) {
            this.scenarioCheck = check;
            return this;
        }
        
        public ActionBuilder successMessage(String message) {
            this.successMessage = message;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<ServerCommandSource> parent) {
            validate();
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<ServerCommandSource> build() {
            validate();
            String cmdName = extractCommandName(path);
            return CommandManager.literal(cmdName)
                .executes(ctx -> execute(ctx.getSource()));
        }
        
        private int execute(ServerCommandSource source) {
            // Protection check
            if (!CommandProtection.checkAndWarn(source, path)) {
                return 0;
            }
            
            // Scenario check
            if (scenarioCheck != null && !scenarioCheck.test(source, null)) {
                return 0;
            }
            
            // Execute handler
            if (!handler.apply(source)) {
                CommandFeedback.error(source, "Action failed.");
                return 0;
            }
            
            if (successMessage != null) {
                CommandFeedback.successBroadcast(source, successMessage);
            } else {
                CommandFeedback.successBroadcast(source, displayName + " completed.");
            }
            return 1;
        }
        
        private void validate() {
            if (handler == null) throw new IllegalStateException("Action '" + path + "' missing handler()");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Toggle Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class ToggleBuilder {
        private final String path;
        private final String displayName;
        private Boolean defaultValue;
        private BiFunction<ServerCommandSource, Boolean, Boolean> handler;
        private BiPredicate<ServerCommandSource, Object> scenarioCheck;
        
        private ToggleBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public ToggleBuilder defaultValue(boolean value) {
            this.defaultValue = value;
            CommandKnobDefaults.register(path, value);
            return this;
        }
        
        public ToggleBuilder handler(BiFunction<ServerCommandSource, Boolean, Boolean> handler) {
            this.handler = handler;
            return this;
        }
        
        public ToggleBuilder scenarioRequired(BiPredicate<ServerCommandSource, Object> check) {
            this.scenarioCheck = check;
            return this;
        }
        
        /**
         * Build and attach to a parent command.
         */
        public void attach(LiteralArgumentBuilder<ServerCommandSource> parent) {
            validate();
            parent.then(build());
        }
        
        /**
         * Build the command node (for manual attachment).
         */
        public LiteralArgumentBuilder<ServerCommandSource> build() {
            validate();
            String cmdName = extractCommandName(path);
            return CommandManager.literal(cmdName)
                .then(CommandManager.literal("enable")
                    .executes(ctx -> execute(ctx.getSource(), true)))
                .then(CommandManager.literal("disable")
                    .executes(ctx -> execute(ctx.getSource(), false)));
        }
        
        private int execute(ServerCommandSource source, boolean enabled) {
            // Protection check
            if (!CommandProtection.checkAndWarn(source, path)) {
                return 0;
            }
            
            // Scenario check
            if (scenarioCheck != null && !scenarioCheck.test(source, null)) {
                return 0;
            }
            
            // Execute handler
            if (!handler.apply(source, enabled)) {
                CommandFeedback.error(source, "Failed to update settings.");
                return 0;
            }
            
            CommandFeedback.toggle(source, displayName, enabled);
            return 1;
        }
        
        private void validate() {
            if (defaultValue == null) throw new IllegalStateException("Toggle knob '" + path + "' missing defaultValue()");
            if (handler == null) throw new IllegalStateException("Toggle knob '" + path + "' missing handler()");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Value Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class ValueBuilder {
        private final String path;
        private final String displayName;
        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;
        private String unit;
        private Integer defaultValue;
        private BiFunction<ServerCommandSource, Integer, Boolean> handler;
        private BiPredicate<ServerCommandSource, Object> scenarioCheck;
        
        private ValueBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public ValueBuilder range(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }
        
        public ValueBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }
        
        public ValueBuilder defaultValue(int value) {
            this.defaultValue = value;
            CommandKnobDefaults.register(path, value);
            return this;
        }
        
        public ValueBuilder handler(BiFunction<ServerCommandSource, Integer, Boolean> handler) {
            this.handler = handler;
            return this;
        }
        
        public ValueBuilder scenarioRequired(BiPredicate<ServerCommandSource, Object> check) {
            this.scenarioCheck = check;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<ServerCommandSource> parent) {
            validate();
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<ServerCommandSource> build() {
            validate();
            String cmdName = extractCommandName(path);
            // Use effective limits (respects removeConfigLimiter)
            int effectiveMin = CommandKnobConfig.effectiveMin(min);
            int effectiveMax = CommandKnobConfig.effectiveMax(max);
            return CommandManager.literal(cmdName)
                .then(CommandManager.argument("value", IntegerArgumentType.integer(effectiveMin, effectiveMax))
                    .executes(ctx -> execute(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value"))));
        }
        
        private int execute(ServerCommandSource source, int value) {
            if (!CommandProtection.checkAndWarn(source, path)) {
                return 0;
            }
            
            // Warn if using unsafe value (outside normal limits but allowed by limiter removal)
            if (CommandKnobConfig.isLimiterRemoved()) {
                CommandKnobConfig.warnPlayerUnsafeValue(source, displayName, value, min, max);
            }
            
            if (scenarioCheck != null && !scenarioCheck.test(source, null)) {
                return 0;
            }
            
            if (!handler.apply(source, value)) {
                CommandFeedback.error(source, "Failed to update settings.");
                return 0;
            }
            
            String formatted = unit != null ? value + " " + unit : String.valueOf(value);
            CommandFeedback.valueSet(source, displayName, formatted);
            return value;
        }
        
        private void validate() {
            if (defaultValue == null) throw new IllegalStateException("Value knob '" + path + "' missing defaultValue()");
            if (handler == null) throw new IllegalStateException("Value knob '" + path + "' missing handler()");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Enum Builder
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final class EnumBuilder<E extends Enum<E>> {
        private final String path;
        private final String displayName;
        private final Class<E> enumClass;
        private Function<E, String> idMapper;
        private Function<String, E> parser;
        private E defaultValue;
        private BiFunction<ServerCommandSource, E, Boolean> handler;
        private BiPredicate<ServerCommandSource, Object> scenarioCheck;
        
        private EnumBuilder(String path, String displayName, Class<E> enumClass) {
            this.path = path;
            this.displayName = displayName;
            this.enumClass = enumClass;
            // Default: lowercase name
            this.idMapper = e -> e.name().toLowerCase();
            this.parser = name -> {
                for (E e : enumClass.getEnumConstants()) {
                    if (e.name().equalsIgnoreCase(name)) return e;
                }
                return null;
            };
        }
        
        public EnumBuilder<E> idMapper(Function<E, String> mapper) {
            this.idMapper = mapper;
            return this;
        }
        
        public EnumBuilder<E> parser(Function<String, E> parser) {
            this.parser = parser;
            return this;
        }
        
        public EnumBuilder<E> defaultValue(E value) {
            this.defaultValue = value;
            CommandKnobDefaults.register(path, idMapper.apply(value));
            return this;
        }
        
        public EnumBuilder<E> handler(BiFunction<ServerCommandSource, E, Boolean> handler) {
            this.handler = handler;
            return this;
        }
        
        public EnumBuilder<E> scenarioRequired(BiPredicate<ServerCommandSource, Object> check) {
            this.scenarioCheck = check;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<ServerCommandSource> parent) {
            validate();
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<ServerCommandSource> build() {
            validate();
            String cmdName = extractCommandName(path);
            return CommandManager.literal(cmdName)
                .then(CommandManager.argument("value", StringArgumentType.word())
                    .suggests(EnumSuggester.of(enumClass, idMapper))
                    .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "value"))));
        }
        
        private int execute(ServerCommandSource source, String rawValue) {
            if (!CommandProtection.checkAndWarn(source, path)) {
                return 0;
            }
            
            E value = parser.apply(rawValue);
            if (value == null) {
                CommandFeedback.error(source, "Unknown value: " + rawValue);
                return 0;
            }
            
            if (scenarioCheck != null && !scenarioCheck.test(source, null)) {
                return 0;
            }
            
            if (!handler.apply(source, value)) {
                CommandFeedback.error(source, "Failed to update settings.");
                return 0;
            }
            
            CommandFeedback.successBroadcast(source, displayName + " set to " + idMapper.apply(value) + ".");
            return 1;
        }
        
        private void validate() {
            if (defaultValue == null) throw new IllegalStateException("Enum knob '" + path + "' missing defaultValue()");
            if (handler == null) throw new IllegalStateException("Enum knob '" + path + "' missing handler()");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Float/Double Value Builder (for field configs, etc.)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a float/double value knob.
     */
    public static FloatBuilder floatValue(String path, String displayName) {
        return new FloatBuilder(path, displayName);
    }
    
    public static final class FloatBuilder {
        private final String path;
        private final String displayName;
        private float min = -Float.MAX_VALUE;
        private float max = Float.MAX_VALUE;
        private String unit;
        private Float defaultValue;
        private BiFunction<ServerCommandSource, Float, Boolean> handler;
        private BiPredicate<ServerCommandSource, Object> scenarioCheck;
        
        private FloatBuilder(String path, String displayName) {
            this.path = path;
            this.displayName = displayName;
        }
        
        public FloatBuilder range(float min, float max) {
            this.min = min;
            this.max = max;
            return this;
        }
        
        public FloatBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }
        
        public FloatBuilder defaultValue(float value) {
            this.defaultValue = value;
            CommandKnobDefaults.register(path, value);
            return this;
        }
        
        public FloatBuilder handler(BiFunction<ServerCommandSource, Float, Boolean> handler) {
            this.handler = handler;
            return this;
        }
        
        public FloatBuilder scenarioRequired(BiPredicate<ServerCommandSource, Object> check) {
            this.scenarioCheck = check;
            return this;
        }
        
        public void attach(LiteralArgumentBuilder<ServerCommandSource> parent) {
            validate();
            parent.then(build());
        }
        
        public LiteralArgumentBuilder<ServerCommandSource> build() {
            validate();
            String cmdName = extractCommandName(path);
            // Use effective limits (respects removeConfigLimiter)
            float effectiveMin = CommandKnobConfig.isLimiterRemoved() ? -Float.MAX_VALUE : min;
            float effectiveMax = CommandKnobConfig.isLimiterRemoved() ? Float.MAX_VALUE : max;
            return CommandManager.literal(cmdName)
                .then(CommandManager.argument("value", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(effectiveMin, effectiveMax))
                    .executes(ctx -> execute(ctx.getSource(), com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value"))));
        }
        
        private int execute(ServerCommandSource source, float value) {
            if (!CommandProtection.checkAndWarn(source, path)) {
                return 0;
            }
            
            // Warn if using unsafe value
            if (CommandKnobConfig.isLimiterRemoved() && (value < min || value > max)) {
                String range = "[" + min + " - " + max + "]";
                CommandFeedback.warning(source, 
                    "UNSAFE VALUE: " + displayName + " = " + value + " is outside normal range " + range);
                CommandFeedback.warning(source, "This may cause crashes or undefined behavior!");
            }
            
            if (scenarioCheck != null && !scenarioCheck.test(source, null)) {
                return 0;
            }
            
            if (!handler.apply(source, value)) {
                CommandFeedback.error(source, "Failed to update settings.");
                return 0;
            }
            
            String formatted = unit != null ? value + " " + unit : String.valueOf(value);
            CommandFeedback.valueSet(source, displayName, formatted);
            return 1;
        }
        
        private void validate() {
            if (defaultValue == null) throw new IllegalStateException("Float knob '" + path + "' missing defaultValue()");
            if (handler == null) throw new IllegalStateException("Float knob '" + path + "' missing handler()");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Extract command name from path (last segment).
     * e.g., "singularity.collapse" -> "collapse"
     */
    private static String extractCommandName(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
