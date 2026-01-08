package net.cyberpunk042.client.gui.state.manager;

import net.cyberpunk042.client.gui.state.ChangeType;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.adapter.PrimitiveAdapter;
import net.cyberpunk042.client.gui.state.adapter.PrimitiveBuilder;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.loader.SimplePrimitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.layer.BlendMode;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.shape.SphereShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages layer and primitive CRUD operations plus selection/sync.
 * 
 * <p>Extracted from FieldEditState to follow Single Responsibility Principle.
 * All layer manipulation (add, remove, swap, rename, visibility, alpha, blend mode)
 * and primitive manipulation within layers is handled here.</p>
 */
public class LayerManager extends AbstractManager {
    
    // Document state (references to FieldEditState's lists)
    private final List<FieldLayer> fieldLayers;
    private final List<Integer> selectedPrimitivePerLayer;
    private final List<PrimitiveAdapter> adapters;
    
    // Selection state
    private int selectedLayerIndex = 0;
    
    // Callback for selection change notification
    private Runnable selectionChangeCallback;
    
    public LayerManager(FieldEditState state, List<FieldLayer> fieldLayers, 
                        List<Integer> selectedPrimitivePerLayer, List<PrimitiveAdapter> adapters) {
        super(state);
        this.fieldLayers = fieldLayers;
        this.selectedPrimitivePerLayer = selectedPrimitivePerLayer;
        this.adapters = adapters;
    }
    
    public void setSelectionChangeCallback(Runnable callback) {
        this.selectionChangeCallback = callback;
    }
    
    private void notifySelectionChanged() {
        if (selectionChangeCallback != null) selectionChangeCallback.run();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE SYNC (via adapters)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Loads the selected primitive's values into all adapters.
     */
    public void loadSelectedPrimitive() {
        Primitive prim = getSelectedPrimitive();
        if (prim == null) {
            Logging.GUI.topic("state").warn("No primitive selected to load");
            return;
        }
        
        for (PrimitiveAdapter adapter : adapters) {
            adapter.loadFrom(prim);
        }
        
        Logging.GUI.topic("state").debug("Loaded primitive '{}' via adapters", prim.id());
    }
    
    /**
     * Saves all adapter data back to the currently selected primitive.
     */
    public void saveSelectedPrimitive() {
        int layerIdx = selectedLayerIndex;
        int primIdx = getSelectedPrimitiveIndex();
        
        if (layerIdx < 0 || layerIdx >= fieldLayers.size()) return;
        FieldLayer layer = fieldLayers.get(layerIdx);
        if (primIdx < 0 || primIdx >= layer.primitives().size()) return;
        
        Primitive oldPrim = layer.primitives().get(primIdx);
        
        // Build new primitive via adapters
        PrimitiveBuilder builder = new PrimitiveBuilder();
        builder.id(oldPrim.id());
        
        for (PrimitiveAdapter adapter : adapters) {
            adapter.saveTo(builder);
        }
        
        SimplePrimitive newPrim = builder.build();
        
        // Replace in layer
        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        updatedPrimitives.set(primIdx, newPrim);
        
        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIdx, updatedLayer);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SELECTION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getSelectedLayerIndex() { return selectedLayerIndex; }
    
    public void setSelectedLayerIndex(int index) {
        saveSelectedPrimitive();
        selectedLayerIndex = Math.max(0, Math.min(index, fieldLayers.size() - 1));
        ensurePrimitiveSelection(selectedLayerIndex);
        loadSelectedPrimitive();
        notifySelectionChanged();
        state.notifyStateChanged(ChangeType.LAYER_SWITCHED);
    }
    
    public int getSelectedPrimitiveIndex() {
        if (selectedPrimitivePerLayer.isEmpty()) return 0;
        if (selectedLayerIndex < 0 || selectedLayerIndex >= selectedPrimitivePerLayer.size()) return 0;
        return selectedPrimitivePerLayer.get(selectedLayerIndex);
    }
    
    public void setSelectedPrimitiveIndex(int index) {
        saveSelectedPrimitive();
        ensurePrimitiveSelection(selectedLayerIndex);
        int clamped = clampPrimitiveIndex(selectedLayerIndex, index);
        selectedPrimitivePerLayer.set(selectedLayerIndex, clamped);
        loadSelectedPrimitive();
        notifySelectionChanged();
        state.notifyStateChanged(ChangeType.PRIMITIVE_SWITCHED);
    }
    
    public FieldLayer getSelectedLayer() {
        if (fieldLayers.isEmpty()) return null;
        return fieldLayers.get(selectedLayerIndex);
    }
    
    public Primitive getSelectedPrimitive() {
        FieldLayer layer = getSelectedLayer();
        if (layer == null || layer.primitives().isEmpty()) return null;
        int primIdx = getSelectedPrimitiveIndex();
        if (primIdx < 0 || primIdx >= layer.primitives().size()) return null;
        return layer.primitives().get(primIdx);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER CRUD
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getCount() { return fieldLayers.size(); }
    public List<FieldLayer> getAll() { return fieldLayers; }
    
    public int add() {
        if (fieldLayers.size() >= 10) return -1;
        String name = "Layer " + (fieldLayers.size() + 1);
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        FieldLayer newLayer = FieldLayer.of(name, List.of(defaultPrimitive));
        fieldLayers.add(newLayer);
        selectedPrimitivePerLayer.add(0);
        markDirty();
        return fieldLayers.size() - 1;
    }
    
    public int addWithName(String name) {
        if (fieldLayers.size() >= 10) return -1;
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        FieldLayer newLayer = FieldLayer.of(name, List.of(defaultPrimitive));
        fieldLayers.add(newLayer);
        selectedPrimitivePerLayer.add(0);
        markDirty();
        return fieldLayers.size() - 1;
    }
    
    public boolean remove(int index) {
        if (fieldLayers.size() <= 1 || index < 0 || index >= fieldLayers.size()) return false;
        fieldLayers.remove(index);
        if (index < selectedPrimitivePerLayer.size()) {
            selectedPrimitivePerLayer.remove(index);
        }
        if (selectedLayerIndex >= fieldLayers.size()) {
            selectedLayerIndex = Math.max(0, fieldLayers.size() - 1);
        } else if (selectedLayerIndex > index) {
            selectedLayerIndex--;
        }
        markDirty();
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    public float getAlpha(int index) {
        if (index < 0 || index >= fieldLayers.size()) return 1.0f;
        return fieldLayers.get(index).alpha();
    }
    
    public void setAlpha(int index, float alpha) {
        if (index < 0 || index >= fieldLayers.size()) return;
        FieldLayer layer = fieldLayers.get(index);
        fieldLayers.set(index, new FieldLayer(layer.id(), layer.primitives(), layer.transform(), 
            layer.animation(), alpha, layer.visible(), layer.blendMode()));
        markDirty();
    }
    
    public String getBlendMode(int index) {
        if (index < 0 || index >= fieldLayers.size()) return "NORMAL";
        return fieldLayers.get(index).blendMode().name();
    }
    
    public void setBlendMode(int index, String mode) {
        if (index < 0 || index >= fieldLayers.size()) return;
        FieldLayer layer = fieldLayers.get(index);
        fieldLayers.set(index, new FieldLayer(layer.id(), layer.primitives(), layer.transform(),
            layer.animation(), layer.alpha(), layer.visible(), BlendMode.valueOf(mode)));
        markDirty();
    }
    
    public boolean isVisible(int index) {
        if (index < 0 || index >= fieldLayers.size()) return true;
        return fieldLayers.get(index).visible();
    }
    
    public boolean toggleVisibility(int index) {
        if (index < 0 || index >= fieldLayers.size()) return true;
        FieldLayer layer = fieldLayers.get(index);
        boolean newVisible = !layer.visible();
        fieldLayers.set(index, new FieldLayer(layer.id(), layer.primitives(), layer.transform(),
            layer.animation(), layer.alpha(), newVisible, layer.blendMode()));
        markDirty();
        return newVisible;
    }
    
    public boolean swap(int a, int b) {
        if (a < 0 || a >= fieldLayers.size() || b < 0 || b >= fieldLayers.size()) return false;
        FieldLayer temp = fieldLayers.get(a);
        fieldLayers.set(a, fieldLayers.get(b));
        fieldLayers.set(b, temp);
        markDirty();
        return true;
    }
    
    public String getName(int index) {
        if (index < 0 || index >= fieldLayers.size()) return "";
        return fieldLayers.get(index).id();
    }
    
    public void rename(int index, String newName) {
        if (index < 0 || index >= fieldLayers.size()) return;
        FieldLayer layer = fieldLayers.get(index);
        fieldLayers.set(index, new FieldLayer(newName, layer.primitives(), layer.transform(),
            layer.animation(), layer.alpha(), layer.visible(), layer.blendMode()));
        markDirty();
    }
    
    public int findByName(String name) {
        for (int i = 0; i < fieldLayers.size(); i++) {
            if (fieldLayers.get(i).id().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE CRUD
    // ═══════════════════════════════════════════════════════════════════════════
    
    public List<? extends Primitive> getPrimitives(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return List.of();
        return fieldLayers.get(layerIndex).primitives();
    }
    
    public int getPrimitiveCount(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return 0;
        return fieldLayers.get(layerIndex).primitives().size();
    }
    
    public int addPrimitive(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return -1;
        FieldLayer layer = fieldLayers.get(layerIndex);
        String id = "primitive_" + (layer.primitives().size() + 1);
        
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive newPrimitive = SimplePrimitive.of(id, "sphere", defaultShape);
        
        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        updatedPrimitives.add(newPrimitive);
        
        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIndex, updatedLayer);
        markDirty();
        return updatedPrimitives.size() - 1;
    }
    
    public boolean removePrimitive(int layerIndex, int primitiveIndex) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return false;
        FieldLayer layer = fieldLayers.get(layerIndex);
        if (layer.primitives().size() <= 1 || primitiveIndex < 0 || primitiveIndex >= layer.primitives().size()) return false;
        
        List<Primitive> updatedPrimitives = new ArrayList<>(layer.primitives());
        updatedPrimitives.remove(primitiveIndex);
        
        FieldLayer updatedLayer = new FieldLayer(
            layer.id(), updatedPrimitives, layer.transform(), layer.animation(),
            layer.alpha(), layer.visible(), layer.blendMode()
        );
        fieldLayers.set(layerIndex, updatedLayer);
        clampPrimitiveSelection(layerIndex);
        markDirty();
        return true;
    }
    
    public String getPrimitiveName(int layerIdx, int primIdx) {
        if (layerIdx < 0 || layerIdx >= fieldLayers.size()) return "";
        FieldLayer layer = fieldLayers.get(layerIdx);
        if (primIdx < 0 || primIdx >= layer.primitives().size()) return "";
        return layer.primitives().get(primIdx).id();
    }
    
    public void renamePrimitive(int layerIdx, int primIdx, String newName) {
        if (layerIdx < 0 || layerIdx >= fieldLayers.size()) return;
        FieldLayer layer = fieldLayers.get(layerIdx);
        if (primIdx < 0 || primIdx >= layer.primitives().size()) return;
        List<Primitive> prims = new ArrayList<>(layer.primitives());
        Primitive old = prims.get(primIdx);
        prims.set(primIdx, SimplePrimitive.of(newName, old.type(), old.shape()));
        fieldLayers.set(layerIdx, new FieldLayer(layer.id(), prims, layer.transform(),
            layer.animation(), layer.alpha(), layer.visible(), layer.blendMode()));
        markDirty();
    }
    
    public int findPrimitiveById(int layerIdx, String id) {
        if (layerIdx < 0 || layerIdx >= fieldLayers.size()) return -1;
        List<? extends Primitive> prims = fieldLayers.get(layerIdx).primitives();
        for (int i = 0; i < prims.size(); i++) {
            if (prims.get(i).id().equalsIgnoreCase(id)) return i;
        }
        return -1;
    }
    
    public boolean swapPrimitives(int layerIdx, int a, int b) {
        if (layerIdx < 0 || layerIdx >= fieldLayers.size()) return false;
        FieldLayer layer = fieldLayers.get(layerIdx);
        List<Primitive> prims = new ArrayList<>(layer.primitives());
        if (a < 0 || a >= prims.size() || b < 0 || b >= prims.size()) return false;
        Primitive temp = prims.get(a);
        prims.set(a, prims.get(b));
        prims.set(b, temp);
        fieldLayers.set(layerIdx, new FieldLayer(layer.id(), prims, layer.transform(),
            layer.animation(), layer.alpha(), layer.visible(), layer.blendMode()));
        markDirty();
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SELECTION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void ensurePrimitiveSelection(int layerIndex) {
        while (selectedPrimitivePerLayer.size() <= layerIndex) {
            selectedPrimitivePerLayer.add(0);
        }
    }
    
    private int clampPrimitiveIndex(int layerIndex, int index) {
        if (layerIndex < 0 || layerIndex >= fieldLayers.size()) return 0;
        List<? extends Primitive> primitives = fieldLayers.get(layerIndex).primitives();
        return Math.max(0, Math.min(index, Math.max(0, primitives.size() - 1)));
    }
    
    private void clampPrimitiveSelection(int layerIndex) {
        if (selectedPrimitivePerLayer.isEmpty()) return;
        int clamped = clampPrimitiveIndex(layerIndex, getSelectedPrimitiveIndex());
        selectedPrimitivePerLayer.set(layerIndex, clamped);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void reset() {
        fieldLayers.clear();
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        fieldLayers.add(FieldLayer.of("Layer 1", List.of(defaultPrimitive)));
        selectedLayerIndex = 0;
        selectedPrimitivePerLayer.clear();
        selectedPrimitivePerLayer.add(0);
    }
    
    /**
     * Loads layers from a FieldDefinition.
     * Replaces current layers with those from the definition.
     */
    public void loadFromDefinition(net.cyberpunk042.field.FieldDefinition definition) {
        if (definition == null) return;
        
        // Clear existing layers
        fieldLayers.clear();
        selectedPrimitivePerLayer.clear();
        
        // Load layers from definition
        if (definition.layers() != null && !definition.layers().isEmpty()) {
            for (net.cyberpunk042.field.FieldLayer layer : definition.layers()) {
                fieldLayers.add(layer);
                selectedPrimitivePerLayer.add(0);
            }
        }
        
        // If no layers, add a default
        if (fieldLayers.isEmpty()) {
            Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
            Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
            fieldLayers.add(net.cyberpunk042.field.FieldLayer.of("Layer 1", List.of(defaultPrimitive)));
            selectedPrimitivePerLayer.add(0);
        }
        
        // Reset selection
        selectedLayerIndex = 0;
        if (!selectedPrimitivePerLayer.isEmpty()) {
            selectedPrimitivePerLayer.set(0, 0);
        }
        
        // Load the first primitive's settings into adapters
        loadSelectedPrimitive();
        
        // Notify listeners
        notifySelectionChanged();
    }
}
