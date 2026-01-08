package net.cyberpunk042.client.command.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom argument type for field paths with bracket notation.
 * 
 * <p>Supports paths like:</p>
 * <ul>
 *   <li>{@code layer[0].primitive[0].fill.mode}</li>
 *   <li>{@code layer[main].primitive[sphere].radius}</li>
 *   <li>{@code orbit.radius} (uses selected layer/primitive)</li>
 *   <li>{@code fill.mode} (shorthand for current primitive)</li>
 * </ul>
 * 
 * <p>Provides intelligent autocomplete at each path segment.</p>
 */
public class FieldPathArgument implements ArgumentType<FieldPath> {
    
    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\[(\\d+|\\w+)]");
    private static final SimpleCommandExceptionType INVALID_PATH = 
        new SimpleCommandExceptionType(Text.literal("Invalid field path"));
    
    // Known property paths (leaf nodes)
    private static final Set<String> PRIMITIVE_PROPERTIES = Set.of(
        "fill.mode", "fill.wireThickness", "fill.doubleSided",
        "appearance.color", "appearance.alpha", "appearance.glow", "appearance.emissive",
        "mask.type", "mask.count", "mask.thickness",
        "shapeType", "radius",
        // Shape-specific
        "sphere.latSteps", "sphere.lonSteps",
        "ring.innerRadius", "ring.outerRadius", "ring.segments", "ring.y", "ring.height", "ring.twist",
        "disc.innerRadius", "disc.segments", "disc.y",
        "prism.sides", "prism.height", "prism.y", "prism.twist", "prism.capTop", "prism.capBottom",
        "cylinder.height", "cylinder.segments", "cylinder.y", "cylinder.capTop", "cylinder.capBottom",
        "polyhedron.type", "polyhedron.subdivisions",
        // Animation
        "spin.speedX", "spin.speedY", "spin.speedZ",
        "spin.oscillateX", "spin.oscillateY", "spin.oscillateZ",
        "spin.rangeX", "spin.rangeY", "spin.rangeZ",
        "pulse.speed", "pulse.amplitude", "pulse.enabled",
        "alphaPulse.speed", "alphaPulse.minAlpha", "alphaPulse.maxAlpha", "alphaPulse.enabled",
        "wobble.speed", "wobble.amplitude", "wobble.enabled",
        "wave.speed", "wave.amplitude", "wave.wavelength", "wave.enabled",
        "colorCycle.speed", "colorCycle.enabled"
    );
    
    private static final Set<String> LAYER_PROPERTIES = Set.of(
        "blendMode", "order", "alpha", "name"
    );
    
    private static final Set<String> ROOT_PROPERTIES = Set.of(
        "orbit.radius", "orbit.speed", "orbit.axis", "orbit.enabled", "orbit.phase",
        "transform.scale", "transform.anchor",
        "followEnabled", "followMode",
        "predictionEnabled", "prediction.leadTicks", "prediction.maxDistance", "prediction.lookAhead"
    );
    
    private FieldPathArgument() {}
    
    public static FieldPathArgument path() {
        return new FieldPathArgument();
    }
    
    public static FieldPath getPath(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, FieldPath.class);
    }
    
    @Override
    public FieldPath parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        
        // Read until whitespace
        StringBuilder pathBuilder = new StringBuilder();
        while (reader.canRead() && reader.peek() != ' ') {
            pathBuilder.append(reader.read());
        }
        
        String rawPath = pathBuilder.toString();
        if (rawPath.isEmpty()) {
            throw INVALID_PATH.createWithContext(reader);
        }
        
        return FieldPath.parse(rawPath);
    }
    
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
            CommandContext<S> context, SuggestionsBuilder builder) {
        
        String input = builder.getRemaining().toLowerCase();
        FieldEditState state = FieldEditStateHolder.get();
        
        // Parse what's already typed
        List<String> suggestions = new ArrayList<>();
        
        if (input.isEmpty() || !input.contains(".")) {
            // Suggest top-level options
            suggestions.add("layer[");
            for (String prop : ROOT_PROPERTIES) {
                if (prop.startsWith(input)) {
                    suggestions.add(prop);
                }
            }
            // Also suggest primitive properties (shorthand)
            for (String prop : PRIMITIVE_PROPERTIES) {
                if (prop.startsWith(input)) {
                    suggestions.add(prop);
                }
            }
        } else if (input.startsWith("layer[")) {
            // Suggest layer indices/names
            int bracketEnd = input.indexOf(']');
            if (bracketEnd < 0) {
                // Still typing layer index
                if (state != null) {
                    for (int i = 0; i < state.getLayerCount(); i++) {
                        String suggestion = "layer[" + i + "]";
                        if (suggestion.startsWith(input)) {
                            suggestions.add(suggestion);
                        }
                    }
                }
            } else if (!input.contains(".primitive[")) {
                // After layer[N], suggest .primitive[ or layer properties
                String prefix = input.substring(0, bracketEnd + 1);
                suggestions.add(prefix + ".primitive[");
                for (String prop : LAYER_PROPERTIES) {
                    suggestions.add(prefix + "." + prop);
                }
            } else {
                // After layer[N].primitive[
                int primBracketStart = input.indexOf(".primitive[") + 11;
                int primBracketEnd = input.indexOf(']', primBracketStart);
                if (primBracketEnd < 0) {
                    // Still typing primitive index
                    String prefix = input.substring(0, primBracketStart);
                    if (state != null) {
                        int layerIdx = extractLayerIndex(input);
                        for (int i = 0; i < state.getPrimitiveCount(layerIdx); i++) {
                            suggestions.add(prefix + i + "]");
                        }
                    }
                } else {
                    // After layer[N].primitive[M], suggest properties
                    String prefix = input.substring(0, primBracketEnd + 1);
                    for (String prop : PRIMITIVE_PROPERTIES) {
                        String full = prefix + "." + prop;
                        if (full.startsWith(input)) {
                            suggestions.add(full);
                        }
                    }
                }
            }
        } else {
            // Partial property path - suggest completions
            for (String prop : PRIMITIVE_PROPERTIES) {
                if (prop.startsWith(input)) {
                    suggestions.add(prop);
                }
            }
            for (String prop : ROOT_PROPERTIES) {
                if (prop.startsWith(input)) {
                    suggestions.add(prop);
                }
            }
        }
        
        for (String suggestion : suggestions) {
            builder.suggest(suggestion);
        }
        
        return builder.buildFuture();
    }
    
    private int extractLayerIndex(String path) {
        Matcher m = Pattern.compile("layer\\[(\\d+)]").matcher(path);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }
    
    @Override
    public Collection<String> getExamples() {
        return List.of(
            "layer[0].primitive[0].fill.mode",
            "orbit.radius",
            "spin.speed",
            "appearance.alpha"
        );
    }
}

