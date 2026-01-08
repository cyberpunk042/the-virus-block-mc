package net.cyberpunk042.client.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.field.influence.BindingConfig;
import net.cyberpunk042.field.influence.BindingSources;
import net.cyberpunk042.field.influence.InterpolationCurve;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;

/**
 * Client-side commands for managing field bindings.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/field binding list - List all active bindings</li>
 *   <li>/field binding add &lt;property&gt; &lt;source&gt; - Add a binding</li>
 *   <li>/field binding remove &lt;property&gt; - Remove a binding</li>
 *   <li>/field binding clear - Remove all bindings</li>
 *   <li>/field binding sources - List available binding sources</li>
 * </ul>
 */
public class FieldBindingCommands {
    
    /**
     * Builds the /field binding command tree.
     */
    public static LiteralArgumentBuilder<FabricClientCommandSource> buildBindingCommand() {
        return ClientCommandManager.literal("binding")
            .then(ClientCommandManager.literal("list")
                .executes(FieldBindingCommands::handleList))
            .then(ClientCommandManager.literal("sources")
                .executes(FieldBindingCommands::handleSources))
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("property", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        // Suggest common properties that can be bound
                        for (String prop : BINDABLE_PROPERTIES) {
                            builder.suggest(prop);
                        }
                        return builder.buildFuture();
                    })
                    .then(ClientCommandManager.argument("source", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            // Suggest available binding sources
                            for (String source : BindingSources.getAvailableIds()) {
                                builder.suggest(source);
                            }
                            return builder.buildFuture();
                        })
                        .executes(FieldBindingCommands::handleAddSimple)
                        .then(ClientCommandManager.argument("inputMin", FloatArgumentType.floatArg())
                            .then(ClientCommandManager.argument("inputMax", FloatArgumentType.floatArg())
                                .then(ClientCommandManager.argument("outputMin", FloatArgumentType.floatArg())
                                    .then(ClientCommandManager.argument("outputMax", FloatArgumentType.floatArg())
                                        .executes(FieldBindingCommands::handleAddFull)
                                        .then(ClientCommandManager.argument("curve", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                for (InterpolationCurve curve : InterpolationCurve.values()) {
                                                    builder.suggest(curve.name().toLowerCase());
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(FieldBindingCommands::handleAddWithCurve)))))))))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("property", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        // Suggest currently bound properties
                        FieldEditState state = FieldEditStateHolder.get();
                        for (var binding : state.getBindings()) {
                            builder.suggest(binding.property());
                        }
                        return builder.buildFuture();
                    })
                    .executes(FieldBindingCommands::handleRemove)))
            .then(ClientCommandManager.literal("clear")
                .executes(FieldBindingCommands::handleClear));
    }
    
    // Common properties that can be bound
    private static final String[] BINDABLE_PROPERTIES = {
        "radius",
        "fill.alpha",
        "appearance.primaryColor",
        "modifiers.alphaMultiplier",
        "modifiers.radiusMultiplier",
        "spin.speed",
        "pulse.amplitude"
    };
    
    private static int handleList(CommandContext<FabricClientCommandSource> ctx) {
        FieldEditState state = FieldEditStateHolder.get();
        var bindings = state.getBindings();
        
        if (bindings.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("§7No active bindings"));
            return 0;
        }
        
        ctx.getSource().sendFeedback(Text.literal("§6Active Bindings:"));
        for (var binding : bindings) {
            ctx.getSource().sendFeedback(Text.literal(String.format(
                "  §e%s §7← §b%s §7[%.1f-%.1f] → [%.1f-%.1f] %s",
                binding.property(),
                binding.source(),
                binding.inputMin(), binding.inputMax(),
                binding.outputMin(), binding.outputMax(),
                binding.curve().name().toLowerCase()
            )));
        }
        return bindings.size();
    }
    
    private static int handleSources(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("§6Available Binding Sources:"));
        for (String source : BindingSources.getAvailableIds()) {
            ctx.getSource().sendFeedback(Text.literal("  §b" + source));
        }
        return BindingSources.getAvailableIds().size();
    }
    
    private static int handleAddSimple(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        String source = StringArgumentType.getString(ctx, "source");
        
        if (!BindingSources.exists(source)) {
            ctx.getSource().sendError(Text.literal("Unknown binding source: " + source));
            return 0;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        state.addBinding(BindingConfig.builder()
            .property(property)
            .source(source)
            .build());
        
        ctx.getSource().sendFeedback(Text.literal(String.format(
            "§aAdded binding: §e%s §7← §b%s", property, source)));
        return 1;
    }
    
    private static int handleAddFull(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        String source = StringArgumentType.getString(ctx, "source");
        float inputMin = FloatArgumentType.getFloat(ctx, "inputMin");
        float inputMax = FloatArgumentType.getFloat(ctx, "inputMax");
        float outputMin = FloatArgumentType.getFloat(ctx, "outputMin");
        float outputMax = FloatArgumentType.getFloat(ctx, "outputMax");
        
        if (!BindingSources.exists(source)) {
            ctx.getSource().sendError(Text.literal("Unknown binding source: " + source));
            return 0;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        state.addBinding(BindingConfig.builder()
            .property(property)
            .source(source)
            .inputRange(inputMin, inputMax)
            .outputRange(outputMin, outputMax)
            .build());
        
        ctx.getSource().sendFeedback(Text.literal(String.format(
            "§aAdded binding: §e%s §7← §b%s §7[%.1f-%.1f] → [%.1f-%.1f]",
            property, source, inputMin, inputMax, outputMin, outputMax)));
        return 1;
    }
    
    private static int handleAddWithCurve(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        String source = StringArgumentType.getString(ctx, "source");
        float inputMin = FloatArgumentType.getFloat(ctx, "inputMin");
        float inputMax = FloatArgumentType.getFloat(ctx, "inputMax");
        float outputMin = FloatArgumentType.getFloat(ctx, "outputMin");
        float outputMax = FloatArgumentType.getFloat(ctx, "outputMax");
        String curveStr = StringArgumentType.getString(ctx, "curve");
        InterpolationCurve curve = InterpolationCurve.fromId(curveStr);
        
        if (!BindingSources.exists(source)) {
            ctx.getSource().sendError(Text.literal("Unknown binding source: " + source));
            return 0;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        state.addBinding(BindingConfig.builder()
            .property(property)
            .source(source)
            .inputRange(inputMin, inputMax)
            .outputRange(outputMin, outputMax)
            .curve(curve)
            .build());
        
        ctx.getSource().sendFeedback(Text.literal(String.format(
            "§aAdded binding: §e%s §7← §b%s §7[%.1f-%.1f] → [%.1f-%.1f] %s",
            property, source, inputMin, inputMax, outputMin, outputMax, curve.name().toLowerCase())));
        return 1;
    }
    
    private static int handleRemove(CommandContext<FabricClientCommandSource> ctx) {
        String property = StringArgumentType.getString(ctx, "property");
        FieldEditState state = FieldEditStateHolder.get();
        
        if (state.removeBinding(property)) {
            ctx.getSource().sendFeedback(Text.literal("§aRemoved binding for: §e" + property));
            return 1;
        } else {
            ctx.getSource().sendError(Text.literal("No binding found for: " + property));
            return 0;
        }
    }
    
    private static int handleClear(CommandContext<FabricClientCommandSource> ctx) {
        FieldEditState state = FieldEditStateHolder.get();
        int count = state.getBindings().size();
        state.clearBindings();
        ctx.getSource().sendFeedback(Text.literal("§aCleared " + count + " bindings"));
        return count;
    }
}
