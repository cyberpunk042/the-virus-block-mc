package net.cyberpunk042.client.gui.shape;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;

import java.util.function.BiConsumer;

/**
 * Calculates and reports performance hints for shape configurations.
 * 
 * <p>Different shapes have different complexity thresholds. This class
 * encapsulates the logic for determining when to show warnings about
 * high polygon counts or segment counts.</p>
 * 
 * <h3>Warning Levels</h3>
 * <ul>
 *   <li><b>None:</b> Value is below warning threshold</li>
 *   <li><b>Medium:</b> Value is between warn and high thresholds</li>
 *   <li><b>High:</b> Value exceeds high threshold</li>
 * </ul>
 */
public final class ShapePerformanceHint {
    
    private ShapePerformanceHint() {} // Static utility
    
    /**
     * Performance threshold configuration for a shape type.
     * 
     * @param warnThreshold Value at which to show medium warning
     * @param highThreshold Value at which to show high/critical warning
     * @param labelFormat Format string for the warning label (e.g., "~%d tris")
     */
    public record Threshold(int warnThreshold, int highThreshold, String labelFormat) {}
    
    /**
     * Computes and sends a performance warning based on shape configuration.
     * 
     * @param state The field edit state to read values from
     * @param shapeType The current shape type
     * @param callback Callback to receive (warningText, color) or (null, 0) to clear
     */
    public static void compute(FieldEditState state, String shapeType, 
                               BiConsumer<String, Integer> callback) {
        if (callback == null) return;
        
        String shape = shapeType.toLowerCase();
        
        switch (shape) {
            case "sphere" -> {
                int lat = state.getInt("sphere.latSteps");
                int lon = state.getInt("sphere.lonSteps");
                int tess = lat * lon;
                sendWarning(callback, tess, 640, 1280, "~" + tess + " tris");
            }
            
            case "ring" -> {
                int seg = state.getInt("ring.segments");
                sendWarning(callback, seg, 256, 512, "~" + seg + " segs");
            }
            
            case "disc" -> {
                int seg = state.getInt("disc.segments");
                int rings = state.getInt("disc.rings");
                int total = seg * Math.max(1, rings);
                sendWarning(callback, total, 256, 512, "~" + total + " cells");
            }
            
            case "prism" -> {
                int sides = state.getInt("prism.sides");
                int hSegs = state.getInt("prism.heightSegments");
                int total = sides * Math.max(1, hSegs);
                sendWarning(callback, total, 64, 128, "~" + total + " faces");
            }
            
            case "cylinder", "beam" -> {
                int seg = state.getInt("cylinder.segments");
                int hSegs = state.getInt("cylinder.heightSegments");
                int total = seg * Math.max(1, hSegs);
                sendWarning(callback, total, 128, 256, "~" + total + " faces");
            }
            
            case "cube" -> {
                int sub = state.getInt("polyhedron.subdivisions");
                int tess = (int) (6 * Math.pow(4, sub));  // 6 faces, each subdivided
                sendWarning(callback, tess, 200, 800, "~" + tess + " faces");
            }
            
            case "tetrahedron" -> {
                int sub = state.getInt("polyhedron.subdivisions");
                int tess = (int) (4 * Math.pow(4, sub));  // 4 faces
                sendWarning(callback, tess, 200, 800, "~" + tess + " faces");
            }
            
            case "octahedron" -> {
                int sub = state.getInt("polyhedron.subdivisions");
                int tess = (int) (8 * Math.pow(4, sub));  // 8 faces
                sendWarning(callback, tess, 200, 800, "~" + tess + " faces");
            }
            
            case "dodecahedron" -> {
                int sub = state.getInt("polyhedron.subdivisions");
                int tess = (int) (12 * Math.pow(4, sub));  // 12 faces
                sendWarning(callback, tess, 200, 800, "~" + tess + " faces");
            }
            
            case "icosahedron" -> {
                int sub = state.getInt("polyhedron.subdivisions");
                int tess = (int) (20 * Math.pow(4, sub));  // 20 faces
                sendWarning(callback, tess, 200, 800, "~" + tess + " faces");
            }
            
            case "torus" -> {
                int major = state.getInt("torus.majorSegments");
                int minor = state.getInt("torus.minorSegments");
                int total = major * minor * 2;  // Quads -> 2 tris each
                sendWarning(callback, total, 512, 1024, "~" + total + " tris");
            }
            
            case "capsule" -> {
                int seg = state.getInt("capsule.segments");
                int total = seg * seg * 2;  // Rough approximation
                sendWarning(callback, total, 256, 512, "~" + total + " tris");
            }
            
            case "cone" -> {
                int seg = state.getInt("cone.segments");
                sendWarning(callback, seg, 32, 64, "~" + seg + " segs");
            }
            
            default -> callback.accept(null, 0);  // Clear warning
        }
    }
    
    /**
     * Sends a warning based on value thresholds.
     */
    private static void sendWarning(BiConsumer<String, Integer> callback,
                                    int value, int warn, int high, String label) {
        if (value >= high) {
            callback.accept("⚠ High: " + label, GuiConstants.ERROR);
        } else if (value >= warn) {
            callback.accept("⚠ Med: " + label, GuiConstants.WARNING);
        } else {
            callback.accept(null, 0);  // Clear
        }
    }
}
