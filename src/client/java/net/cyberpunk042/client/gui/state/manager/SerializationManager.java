package net.cyberpunk042.client.gui.state.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.StateAccessor;
import net.cyberpunk042.client.gui.state.adapter.PrimitiveAdapter;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.log.Logging;

import java.util.List;

/**
 * Manages state serialization to/from JSON.
 * 
 * <p>Handles the complete state including:
 * <ul>
 *   <li>@StateField fields on FieldEditState itself</li>
 *   <li>All adapter state (shape, animation, fill, etc.)</li>
 *   <li>Layer and primitive data</li>
 *   <li>Selection state</li>
 * </ul>
 * 
 * <p>Uses {@link StateAccessor#toJson(Object)} internally for reflection-based
 * serialization of @StateField annotated fields.</p>
 */
public class SerializationManager extends AbstractManager {
    
    private final List<PrimitiveAdapter> adapters;
    private final List<FieldLayer> fieldLayers;
    private final List<Integer> selectedPrimitivePerLayer;
    private final LayerManager layerManager;
    
    public SerializationManager(FieldEditState state, 
                                 List<PrimitiveAdapter> adapters,
                                 List<FieldLayer> fieldLayers,
                                 List<Integer> selectedPrimitivePerLayer,
                                 LayerManager layerManager) {
        super(state);
        this.adapters = adapters;
        this.fieldLayers = fieldLayers;
        this.selectedPrimitivePerLayer = selectedPrimitivePerLayer;
        this.layerManager = layerManager;
    }
    
    /**
     * Serializes the complete state to JSON.
     * 
     * @return Full JSON representation including adapters and layers
     */
    public JsonObject toJson() {
        // Start with @StateField fields from FieldEditState
        JsonObject json = StateAccessor.toJson(state());
        
        // Add adapter state under their category names
        for (PrimitiveAdapter adapter : adapters) {
            String category = adapter.category();
            JsonObject adapterJson = StateAccessor.toJson(adapter);
            if (adapterJson.size() > 0) {
                json.add(category, adapterJson);
            }
        }
        
        // IMPORTANT: Save current adapter state to primitive before serializing layers
        layerManager.saveSelectedPrimitive();
        
        // Add layers
        JsonArray layersArray = new JsonArray();
        for (FieldLayer layer : fieldLayers) {
            layersArray.add(layer.toJson());
        }
        json.add("layers", layersArray);
        
        // Add selection state
        json.addProperty("selectedLayerIndex", layerManager.getSelectedLayerIndex());
        json.addProperty("selectedPrimitiveIndex", layerManager.getSelectedPrimitiveIndex());
        
        Logging.GUI.topic("serial").trace("Serialized state: {} fields, {} adapters, {} layers",
            json.size(), adapters.size(), fieldLayers.size());
        
        return json;
    }
    
    /**
     * Deserializes JSON into the state.
     * 
     * @param json The JSON to load
     */
    public void fromJson(JsonObject json) {
        // Load basic state fields
        StateAccessor.fromJson(state(), json);
        
        // Load adapter state
        for (PrimitiveAdapter adapter : adapters) {
            String category = adapter.category();
            if (json.has(category) && json.get(category).isJsonObject()) {
                StateAccessor.fromJson(adapter, json.getAsJsonObject(category));
            }
        }
        
        // Load layers
        if (json.has("layers") && json.get("layers").isJsonArray()) {
            fieldLayers.clear();
            selectedPrimitivePerLayer.clear();
            JsonArray layersArray = json.getAsJsonArray("layers");
            for (int i = 0; i < layersArray.size(); i++) {
                fieldLayers.add(FieldLayer.fromJson(layersArray.get(i).getAsJsonObject()));
                selectedPrimitivePerLayer.add(0);
            }
        }
        
        // Load selection state
        if (json.has("selectedLayerIndex")) {
            layerManager.setSelectedLayerIndex(json.get("selectedLayerIndex").getAsInt());
        }
        if (json.has("selectedPrimitiveIndex")) {
            layerManager.setSelectedPrimitiveIndex(json.get("selectedPrimitiveIndex").getAsInt());
        }
        
        Logging.GUI.topic("serial").trace("Deserialized state from JSON");
    }
    
    /**
     * Serializes to pretty-printed JSON string.
     */
    public String toJsonString() {
        return toJson().toString();
    }
    
    /**
     * Deserializes from JSON string.
     */
    public void fromJsonString(String jsonStr) {
        JsonObject json = com.google.gson.JsonParser.parseString(jsonStr).getAsJsonObject();
        fromJson(json);
    }
    
    @Override
    public void reset() {
        // Nothing to reset - delegates reset to adapters individually
    }
}
