package net.cyberpunk042.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.client.gui.util.PresetRegistry;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Commands for applying fragments and presets.
 * 
 * <h2>Fragment Command</h2>
 * <pre>
 * /field fragment <category> <name>
 * /field fragment fill wireframe_thin
 * /field fragment appearance ethereal_glow
 * </pre>
 * 
 * <h2>Preset Command</h2>
 * <pre>
 * /field preset <name>
 * /field preset tech_grid
 * </pre>
 */
public final class FieldFragmentCommands {
    
    private FieldFragmentCommands() {}
    
    private static final List<String> FRAGMENT_CATEGORIES = List.of(
        "shape", "fill", "visibility", "appearance", "animation", 
        "transform", "arrangement", "orbit", "follow", "prediction"
    );
    
    /**
     * Register fragment and preset commands under the /field parent.
     */
    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> field) {
        buildFragmentCommand(field);
        buildPresetCommand(field);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FRAGMENT COMMAND
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildFragmentCommand(LiteralArgumentBuilder<FabricClientCommandSource> field) {
        var fragment = ClientCommandManager.literal("fragment");
        
        // /field fragment <category> <name>
        fragment.then(ClientCommandManager.argument("category", StringArgumentType.word())
            .suggests((ctx, builder) -> {
                for (String cat : FRAGMENT_CATEGORIES) {
                    builder.suggest(cat);
                }
                return builder.buildFuture();
            })
            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String category = StringArgumentType.getString(ctx, "category");
                    suggestFragmentNames(category, builder);
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    String category = StringArgumentType.getString(ctx, "category");
                    String name = StringArgumentType.getString(ctx, "name");
                    return applyFragment(ctx.getSource(), category, name);
                })));
        
        // /field fragment (list categories)
        fragment.executes(ctx -> {
            ctx.getSource().sendFeedback(Text.literal("Fragment categories: " + 
                String.join(", ", FRAGMENT_CATEGORIES)).formatted(Formatting.AQUA));
            return 1;
        });
        
        field.then(fragment);
    }
    
    private static void suggestFragmentNames(String category, SuggestionsBuilder builder) {
        List<String> names = switch (category.toLowerCase()) {
            case "shape" -> {
                // Shape fragments are per-type, suggest common ones
                var state = FieldEditStateHolder.get();
                String shapeType = state != null ? state.getString("shapeType") : "sphere";
                yield FragmentRegistry.listShapeFragments(shapeType);
            }
            case "fill" -> FragmentRegistry.listFillFragments();
            case "visibility" -> FragmentRegistry.listVisibilityFragments();
            case "arrangement" -> FragmentRegistry.listArrangementFragments();
            case "animation" -> FragmentRegistry.listAnimationFragments();
            case "follow" -> FragmentRegistry.listFollowFragments();
            case "appearance" -> FragmentRegistry.listAppearanceFragments();
            case "orbit" -> FragmentRegistry.listOrbitFragments();
            case "transform" -> FragmentRegistry.listTransformFragments();
            case "prediction" -> FragmentRegistry.listPredictionFragments();
            default -> List.of();
        };
        
        for (String name : names) {
            if (!name.equals("Default") && !name.equals("Custom")) {
                builder.suggest(name);
            }
        }
    }
    
    private static int applyFragment(FabricClientCommandSource source, String category, String name) {
        FieldEditState state = FieldEditStateHolder.get();
        if (state == null) {
            source.sendError(Text.literal("Field state not available"));
            return 0;
        }
        
        // Build the ref path
        String ref = "$field_" + category + "s/" + name;
        FragmentRegistry.applyFragment(state, category, ref);
        
        source.sendFeedback(Text.literal("Applied " + category + " fragment: " + name)
            .formatted(Formatting.GREEN));
        return 1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRESET COMMAND
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildPresetCommand(LiteralArgumentBuilder<FabricClientCommandSource> field) {
        var preset = ClientCommandManager.literal("preset");
        
        // /field preset <name>
        preset.then(ClientCommandManager.argument("name", StringArgumentType.word())
            .suggests((ctx, builder) -> {
                for (String name : PresetRegistry.listPresets()) {
                    if (!name.equals("None")) {
                        builder.suggest(name);
                    }
                }
                return builder.buildFuture();
            })
            .executes(ctx -> {
                String name = StringArgumentType.getString(ctx, "name");
                return applyPreset(ctx.getSource(), name);
            }));
        
        // /field preset (list available)
        preset.executes(ctx -> {
            var names = PresetRegistry.listPresets();
            ctx.getSource().sendFeedback(Text.literal("Available presets: " + 
                String.join(", ", names)).formatted(Formatting.AQUA));
            return 1;
        });
        
        field.then(preset);
    }
    
    private static int applyPreset(FabricClientCommandSource source, String name) {
        FieldEditState state = FieldEditStateHolder.get();
        if (state == null) {
            source.sendError(Text.literal("Field state not available"));
            return 0;
        }
        
        var presetOpt = PresetRegistry.getPreset(name);
        if (presetOpt.isEmpty()) {
            source.sendError(Text.literal("Preset not found: " + name));
            return 0;
        }
        
        PresetRegistry.applyPreset(state, name);
        
        var preset = presetOpt.get();
        source.sendFeedback(Text.literal("Applied preset: " + preset.name())
            .formatted(Formatting.GREEN));
        if (!preset.description().isEmpty()) {
            source.sendFeedback(Text.literal("  " + preset.description())
                .formatted(Formatting.GRAY));
        }
        return 1;
    }
}

