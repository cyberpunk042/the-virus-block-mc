package net.cyberpunk042.client.command.util;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a parsed field path like {@code layer[0].primitive[0].fill.mode}.
 * 
 * <p>Handles the translation between bracket notation and StateAccessor paths.</p>
 */
public record FieldPath(
    Integer layerIndex,
    Integer primitiveIndex,
    String propertyPath
) {
    
    private static final Pattern LAYER_PATTERN = Pattern.compile("^layer\\[(\\d+|\\w+)]");
    private static final Pattern PRIMITIVE_PATTERN = Pattern.compile("\\.primitive\\[(\\d+|\\w+)]");
    
    /**
     * Parse a path string into a FieldPath.
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "layer[0].primitive[0].fill.mode"} → layer=0, primitive=0, property="fill.mode"</li>
     *   <li>{@code "orbit.radius"} → layer=null, primitive=null, property="orbit.radius"</li>
     *   <li>{@code "fill.mode"} → layer=null, primitive=null, property="fill.mode" (uses current)</li>
     * </ul>
     */
    public static FieldPath parse(String raw) {
        Integer layerIdx = null;
        Integer primIdx = null;
        String remaining = raw;
        
        // Extract layer[N]
        Matcher layerMatcher = LAYER_PATTERN.matcher(remaining);
        if (layerMatcher.find()) {
            String layerVal = layerMatcher.group(1);
            layerIdx = parseIndexOrName(layerVal, true);
            remaining = remaining.substring(layerMatcher.end());
            if (remaining.startsWith(".")) {
                remaining = remaining.substring(1);
            }
        }
        
        // Extract primitive[N]
        Matcher primMatcher = PRIMITIVE_PATTERN.matcher("." + remaining);
        if (primMatcher.find()) {
            String primVal = primMatcher.group(1);
            primIdx = parseIndexOrName(primVal, false);
            remaining = remaining.substring(primMatcher.end() - 1); // -1 for the . we added
            if (remaining.startsWith(".")) {
                remaining = remaining.substring(1);
            }
        }
        
        // Remove leading "primitive[N]." if present at start
        if (remaining.startsWith("primitive[")) {
            Matcher m = Pattern.compile("^primitive\\[(\\d+|\\w+)]\\.?").matcher(remaining);
            if (m.find()) {
                primIdx = parseIndexOrName(m.group(1), false);
                remaining = remaining.substring(m.end());
            }
        }
        
        return new FieldPath(layerIdx, primIdx, remaining);
    }
    
    private static Integer parseIndexOrName(String val, boolean isLayer) {
        // Try numeric first
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            // It's a name - look it up in state
            FieldEditState state = FieldEditStateHolder.get();
            if (state == null) return 0;
            
            if (isLayer) {
                return state.findLayerByName(val);
            } else {
                int layerIdx = state.getSelectedLayerIndex();
                return state.findPrimitiveById(layerIdx, val);
            }
        }
    }
    
    /**
     * Get the effective layer index (from path or current selection).
     */
    public int effectiveLayerIndex() {
        if (layerIndex != null) return layerIndex;
        FieldEditState state = FieldEditStateHolder.get();
        return state != null ? state.getSelectedLayerIndex() : 0;
    }
    
    /**
     * Get the effective primitive index (from path or current selection).
     */
    public int effectivePrimitiveIndex() {
        if (primitiveIndex != null) return primitiveIndex;
        FieldEditState state = FieldEditStateHolder.get();
        return state != null ? state.getSelectedPrimitiveIndex() : 0;
    }
    
    /**
     * Convert to a StateAccessor-compatible path.
     * 
     * <p>This builds the full path including layer/primitive prefixes if needed.</p>
     */
    public String toStatePath() {
        // For now, we use the simple property path and let the state
        // figure out the context from selectedLayerIndex/selectedPrimitiveIndex
        return propertyPath;
    }
    
    /**
     * Apply the layer/primitive selection to state before accessing property.
     */
    public void applyContext() {
        FieldEditState state = FieldEditStateHolder.get();
        if (state == null) return;
        
        if (layerIndex != null) {
            state.setSelectedLayerIndex(layerIndex);
        }
        if (primitiveIndex != null) {
            state.setSelectedPrimitiveIndex(primitiveIndex);
        }
    }
    
    /**
     * Get full display path for feedback.
     */
    public String displayPath() {
        StringBuilder sb = new StringBuilder();
        if (layerIndex != null) {
            sb.append("layer[").append(layerIndex).append("]");
            if (primitiveIndex != null || !propertyPath.isEmpty()) {
                sb.append(".");
            }
        }
        if (primitiveIndex != null) {
            sb.append("primitive[").append(primitiveIndex).append("]");
            if (!propertyPath.isEmpty()) {
                sb.append(".");
            }
        }
        sb.append(propertyPath);
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return displayPath();
    }
}

