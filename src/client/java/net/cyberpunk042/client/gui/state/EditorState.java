package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.log.Logging;

/**
 * Tracks current selection in the editor.
 * Separate from FieldEditState to keep selection logic isolated.
 */
public class EditorState {
    
    /** Currently selected layer index */
    private int selectedLayerIndex = 0;
    
    /** Currently selected primitive index within the layer */
    private int selectedPrimitiveIndex = 0;
    
    public EditorState() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Layer Selection
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getSelectedLayerIndex() {
        return selectedLayerIndex;
    }
    
    public void selectLayer(int index) {
        if (index != selectedLayerIndex) {
            selectedLayerIndex = index;
            selectedPrimitiveIndex = 0; // Reset primitive selection
            Logging.GUI.topic("editor").trace("Selected layer {}", index);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Primitive Selection
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getSelectedPrimitiveIndex() {
        return selectedPrimitiveIndex;
    }
    
    public void selectPrimitive(int index) {
        if (index != selectedPrimitiveIndex) {
            selectedPrimitiveIndex = index;
            Logging.GUI.topic("editor").trace("Selected primitive {}", index);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Utility
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void reset() {
        selectedLayerIndex = 0;
        selectedPrimitiveIndex = 0;
    }
}
