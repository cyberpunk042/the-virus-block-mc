package net.cyberpunk042.client.gui.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.cyberpunk042.client.gui.state.adapter.*;
import net.cyberpunk042.client.gui.state.manager.*;
import net.cyberpunk042.client.visual.effect.EffectType;
import net.cyberpunk042.field.FieldLayer;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.field.loader.SimplePrimitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.Shape;
import net.cyberpunk042.visual.shape.SphereShape;

import java.util.*;

/**
 * G02: Manages the shared editing state for both GUI and /field commands.
 * 
 * <p>Uses adapter-based architecture for <b>data</b> (shape, animation, fill, etc.)
 * and manager-based architecture for <b>operations</b> (layers, profiles, bindings).</p>
 * 
 * <p><b>Architecture:</b></p>
 * <pre>
 * FieldEditState (coordinator)
 *   ├── ADAPTERS (data)
 *   │   ├── ShapeAdapter     - shape configs
 *   │   ├── AnimationAdapter - animation configs
 *   │   ├── FillAdapter      - fill config
 *   │   ├── TransformAdapter - transform
 *   │   ├── AppearanceAdapter - colors, glow
 *   │   ├── VisibilityAdapter - mask
 *   │   └── ArrangementAdapter - pattern
 *   │
 *   └── MANAGERS (operations)
 *       ├── LayerManager     - layer/primitive CRUD
 *       ├── ProfileManager   - profiles, snapshots
 *       ├── BindingsManager  - dynamic bindings
 *       └── TriggerManager   - trigger testing
 * </pre>
 * 
 * @see PrimitiveAdapter
 * @see StateManager
 */
public class FieldEditState {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ADAPTERS (handle data categories)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Per-primitive adapters (stored in each primitive)
    private final ShapeAdapter shapeAdapter = new ShapeAdapter();
    private final AnimationAdapter animationAdapter = new AnimationAdapter();
    private final FillAdapter fillAdapter = new FillAdapter();
    private final TransformAdapter transformAdapter = new TransformAdapter();
    private final AppearanceAdapter appearanceAdapter = new AppearanceAdapter();
    private final VisibilityAdapter visibilityAdapter = new VisibilityAdapter();
    private final ArrangementAdapter arrangementAdapter = new ArrangementAdapter();
    private final LinkAdapter linkAdapter = new LinkAdapter();
    private final TriggerAdapter triggerAdapter = new TriggerAdapter();
    
    // Field-level adapters (stored at field definition level, not per-primitive)
    private final ShockwaveAdapter shockwaveAdapter = new ShockwaveAdapter();
    private final FieldVisualAdapter fieldVisualAdapter = new FieldVisualAdapter();
    private final MagicCircleAdapter magicCircleAdapter = new MagicCircleAdapter();
    
    // List of per-primitive adapters only
    private final List<PrimitiveAdapter> adapters = List.of(
        shapeAdapter, animationAdapter, fillAdapter, transformAdapter,
        appearanceAdapter, visibilityAdapter, arrangementAdapter, linkAdapter, triggerAdapter
    );
    
    // Map includes BOTH primitive adapters AND field-level adapters for path routing
    private final Map<String, AbstractAdapter> adapterByCategory;
    
    {
        Map<String, AbstractAdapter> map = new HashMap<>();
        for (PrimitiveAdapter adapter : adapters) {
            map.put(adapter.category(), (AbstractAdapter) adapter);
        }
        // Add field-level adapters
        map.put(shockwaveAdapter.category(), shockwaveAdapter);
        map.put("fieldVisual", fieldVisualAdapter);
        map.put(magicCircleAdapter.category(), magicCircleAdapter);
        
        // Register adapters under all expected path prefixes for backward compatibility
        // Panels use paths like "spin.speed", "sphere.radius", "mask.count"
        
        // Animation types -> AnimationAdapter
        map.put("spin", animationAdapter);
        map.put("pulse", animationAdapter);
        map.put("alphaPulse", animationAdapter);
        map.put("wobble", animationAdapter);
        map.put("wave", animationAdapter);
        map.put("colorCycle", animationAdapter);
        map.put("precession", animationAdapter);
        map.put("travelEffect", animationAdapter);
        map.put("rayFlow", animationAdapter);
        map.put("rayMotion", animationAdapter);
        map.put("rayWiggle", animationAdapter);
        map.put("rayTwist", animationAdapter);
        
        // Shape types -> ShapeAdapter
        map.put("shapeType", shapeAdapter);
        map.put("sphere", shapeAdapter);
        map.put("ring", shapeAdapter);
        map.put("prism", shapeAdapter);
        map.put("cylinder", shapeAdapter);
        map.put("polyhedron", shapeAdapter);
        map.put("torus", shapeAdapter);
        map.put("capsule", shapeAdapter);
        map.put("cone", shapeAdapter);
        map.put("jet", shapeAdapter);
        map.put("rays", shapeAdapter);
        map.put("kamehameha", shapeAdapter);
        map.put("molecule", shapeAdapter);
        
        // Link properties -> LinkAdapter (panel uses paths like "primitiveId", "radiusOffset")
        map.put("primitiveId", linkAdapter);
        map.put("radiusOffset", linkAdapter);
        map.put("phaseOffset", linkAdapter);
        map.put("mirrorAxis", linkAdapter);
        map.put("followLinked", linkAdapter);
        map.put("scaleWithLinked", linkAdapter);
        
        // Trigger UI state -> TriggerAdapter
        map.put("triggerType", triggerAdapter);
        map.put("triggerEffect", triggerAdapter);
        map.put("triggerIntensity", triggerAdapter);
        map.put("triggerDuration", triggerAdapter);
        
        // Arrangement paths -> ArrangementAdapter
        map.put("defaultPattern", arrangementAdapter);
        map.put("poles", arrangementAdapter);
        map.put("equator", arrangementAdapter);
        map.put("surface", arrangementAdapter);
        map.put("sides", arrangementAdapter);
        map.put("capTop", arrangementAdapter);
        map.put("capBottom", arrangementAdapter);
        
        // Fill paths -> FillAdapter (for paths like fill.mode, fill.wireThickness)
        // Already covered by category "fill"
        
        // Visibility/Mask paths -> VisibilityAdapter
        map.put("mask", visibilityAdapter);
        
        // Transform paths -> TransformAdapter
        map.put("anchor", transformAdapter);
        map.put("facing", transformAdapter);
        map.put("billboard", transformAdapter);
        map.put("orbit", transformAdapter);
        
        adapterByCategory = Collections.unmodifiableMap(map);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MANAGERS (handle operation categories)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final LayerManager layerManager;
    private final ProfileManager profileManager;
    private final BindingsManager bindingsManager;
    private final TriggerManager triggerManager;
    private final SerializationManager serializationManager;
    
    // Manager accessors
    public LayerManager layers() { return layerManager; }
    public ProfileManager profiles() { return profileManager; }
    public BindingsManager bindings() { return bindingsManager; }
    public TriggerManager triggers() { return triggerManager; }
    public SerializationManager serialization() { return serializationManager; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DOCUMENT (layers - shared with LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final List<FieldLayer> fieldLayers = new ArrayList<>();
    private final List<Integer> selectedPrimitivePerLayer = new ArrayList<>();
    
    /** Listener called when layer or primitive selection changes */
    private Runnable selectionChangeListener;
    
    {
        // Initialize with one default layer
        Shape defaultShape = SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        Primitive defaultPrimitive = SimplePrimitive.of("primitive_1", "sphere", defaultShape);
        FieldLayer defaultLayer = FieldLayer.of("Layer 1", List.of(defaultPrimitive));
        fieldLayers.add(defaultLayer);
        selectedPrimitivePerLayer.add(0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GUI SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField public boolean livePreviewEnabled = true;
    @StateField public boolean autoSaveEnabled = false;
    @StateField public boolean debugUnlocked = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIRTY TRACKING & LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean isDirty = false;
    private final List<Runnable> changeListeners = new ArrayList<>();
    
    public boolean isDirty() { return isDirty; }
    public void markDirty() { isDirty = true; notifyListeners(); }
    public void clearDirty() { isDirty = false; }
    
    public void addChangeListener(Runnable listener) { changeListeners.add(listener); }
    public void removeChangeListener(Runnable listener) { changeListeners.remove(listener); }
    
    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                Logging.GUI.topic("state").error("Error in state change listener", e);
            }
        }
    }
    
    public void setSelectionChangeListener(Runnable listener) {
        this.selectionChangeListener = listener;
    }
    
    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.run();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE CHANGE LISTENERS (for GUI panel binding system)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final List<StateChangeListener> stateListeners = new ArrayList<>();
    
    /**
     * Registers a listener for state changes.
     * 
     * <p>Listeners are notified when:
     * <ul>
     *   <li>Profile is loaded</li>
     *   <li>Layer/primitive selection changes</li>
     *   <li>Fragment/preset is applied</li>
     *   <li>Properties change from external source</li>
     * </ul>
     * 
     * @param listener The listener to add
     * @see StateChangeListener
     * @see ChangeType
     */
    public void addStateListener(StateChangeListener listener) {
        if (listener != null && !stateListeners.contains(listener)) {
            stateListeners.add(listener);
            Logging.GUI.topic("state").trace("Added state listener: {}", listener.getClass().getSimpleName());
        }
    }
    
    /**
     * Removes a previously registered state listener.
     * 
     * @param listener The listener to remove
     */
    public void removeStateListener(StateChangeListener listener) {
        if (stateListeners.remove(listener)) {
            Logging.GUI.topic("state").trace("Removed state listener: {}", listener.getClass().getSimpleName());
        }
    }
    
    /**
     * Notifies all registered listeners of a state change.
     * 
     * <p>Called by managers and external systems when state changes
     * in a way that widgets need to know about.</p>
     * 
     * @param type The type of change that occurred
     * @see ChangeType
     */
    public void notifyStateChanged(ChangeType type) {
        if (stateListeners.isEmpty()) return;
        
        Logging.GUI.topic("state").debug("Notifying {} listeners of {}", stateListeners.size(), type);
        
        // Copy to avoid ConcurrentModificationException if listener modifies list
        List<StateChangeListener> listeners = new ArrayList<>(stateListeners);
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChanged(type);
            } catch (Exception e) {
                Logging.GUI.topic("state").error("Error in state listener {}: {}", 
                    listener.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATH-BASED ACCESS (routes to adapters)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Set a value by path, routing to the appropriate adapter.
     * <p>Examples: "spin.speed", "pulse.scale", "mask.count", "fill.mode"</p>
     */
    public void set(String path, Object value) {
        String[] parts = path.split("\\.", 2);
        String category = parts[0];
        
        AbstractAdapter adapter = adapterByCategory.get(category);
        if (adapter != null) {
            // Pass the FULL path to the adapter - it handles internal navigation
            adapter.set(path, value);
        } else {
            // Try direct field on this class
            StateAccessor.set(this, path, value);
        }
        markDirty();
    }
    
    /**
     * Get a value by path, routing to the appropriate adapter.
     */
    public Object get(String path) {
        String[] parts = path.split("\\.", 2);
        String category = parts[0];
        
        AbstractAdapter adapter = adapterByCategory.get(category);
        if (adapter != null) {
            // Pass the FULL path to the adapter - it handles internal navigation
            return adapter.get(path);
        }
        return StateAccessor.get(this, path);
    }
    
    // Typed getters
    public int getInt(String path) {
        Object v = get(path);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }
    
    public float getFloat(String path) {
        Object v = get(path);
        return v instanceof Number ? ((Number) v).floatValue() : 0f;
    }
    
    public float getFloatOr(String path, float defaultValue) {
        Object v = get(path);
        return v instanceof Number ? ((Number) v).floatValue() : defaultValue;
    }
    
    public boolean getBool(String path) {
        Object v = get(path);
        return v instanceof Boolean && (Boolean) v;
    }
    
    public String getString(String path) {
        Object v = get(path);
        if (v == null) return null;
        if (v instanceof Enum<?>) return ((Enum<?>) v).name();
        return v.toString();
    }
    
    public org.joml.Vector3f getVec3f(String path) {
        Object v = get(path);
        if (v instanceof org.joml.Vector3f) return (org.joml.Vector3f) v;
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ADAPTER ACCESS (internal)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public ShapeAdapter shapeAdapter() { return shapeAdapter; }
    public AnimationAdapter animationAdapter() { return animationAdapter; }
    public FillAdapter fillAdapterObj() { return fillAdapter; }
    public TransformAdapter transformAdapterObj() { return transformAdapter; }
    public AppearanceAdapter appearanceAdapterObj() { return appearanceAdapter; }
    public VisibilityAdapter visibilityAdapterObj() { return visibilityAdapter; }
    public ArrangementAdapter arrangementAdapterObj() { return arrangementAdapter; }
    public ShockwaveAdapter shockwaveAdapter() { return shockwaveAdapter; }
    public FieldVisualAdapter fieldVisualAdapter() { return fieldVisualAdapter; }
    public MagicCircleAdapter magicCircleAdapter() { return magicCircleAdapter; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BACKWARD-COMPATIBLE CONFIG ACCESSORS (used by panels and DefinitionBuilder)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Shape delegates
    public net.cyberpunk042.visual.shape.SphereShape sphere() { return shapeAdapter.sphere(); }
    public net.cyberpunk042.visual.shape.RingShape ring() { return shapeAdapter.ring(); }
    public net.cyberpunk042.visual.shape.PrismShape prism() { return shapeAdapter.prism(); }
    public net.cyberpunk042.visual.shape.CylinderShape cylinder() { return shapeAdapter.cylinder(); }
    public net.cyberpunk042.visual.shape.PolyhedronShape polyhedron() { return shapeAdapter.polyhedron(); }
    public net.cyberpunk042.visual.shape.TorusShape torus() { return shapeAdapter.torus(); }
    public net.cyberpunk042.visual.shape.CapsuleShape capsule() { return shapeAdapter.capsule(); }
    public net.cyberpunk042.visual.shape.ConeShape cone() { return shapeAdapter.cone(); }
    public net.cyberpunk042.visual.shape.JetShape jet() { return shapeAdapter.jet(); }
    public net.cyberpunk042.visual.shape.RaysShape rays() { return shapeAdapter.rays(); }
    public net.cyberpunk042.visual.shape.KamehamehaShape kamehameha() { return shapeAdapter.kamehameha(); }
    public net.cyberpunk042.visual.shape.MoleculeShape molecule() { return shapeAdapter.molecule(); }
    public Shape currentShape() { return shapeAdapter.currentShape(); }
    
    // Animation delegates
    public net.cyberpunk042.visual.animation.SpinConfig spin() { return animationAdapter.spin(); }
    public net.cyberpunk042.visual.animation.PulseConfig pulse() { return animationAdapter.pulse(); }
    public net.cyberpunk042.visual.animation.AlphaPulseConfig alphaPulse() { return animationAdapter.alphaPulse(); }
    public net.cyberpunk042.visual.animation.WobbleConfig wobble() { return animationAdapter.wobble(); }
    public net.cyberpunk042.visual.animation.WaveConfig wave() { return animationAdapter.wave(); }
    public net.cyberpunk042.visual.animation.ColorCycleConfig colorCycle() { return animationAdapter.colorCycle(); }
    public net.cyberpunk042.visual.animation.PrecessionConfig precession() { return animationAdapter.precession(); }
    public net.cyberpunk042.visual.animation.TravelEffectConfig travelEffect() { return animationAdapter.travelEffect(); }
    public net.cyberpunk042.visual.animation.RayFlowConfig rayFlow() { return animationAdapter.rayFlow(); }
    public net.cyberpunk042.visual.animation.RayMotionConfig rayMotion() { return animationAdapter.rayMotion(); }
    public net.cyberpunk042.visual.animation.RayWiggleConfig rayWiggle() { return animationAdapter.rayWiggle(); }
    public net.cyberpunk042.visual.animation.RayTwistConfig rayTwist() { return animationAdapter.rayTwist(); }
    
    // Fill/Transform/Visibility/Arrangement - return the actual config, not adapter
    public net.cyberpunk042.visual.fill.FillConfig fill() { return fillAdapter.fill(); }
    public net.cyberpunk042.visual.transform.Transform transform() { return transformAdapter.transform(); }
    public net.cyberpunk042.visual.visibility.VisibilityMask mask() { return visibilityAdapter.mask(); }
    public net.cyberpunk042.visual.pattern.ArrangementConfig arrangement() { return arrangementAdapter.arrangement(); }
    public AppearanceState appearance() { return appearanceAdapter.appearance(); }
    public net.cyberpunk042.field.primitive.PrimitiveLink link() { return linkAdapter.currentLink(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE SYNC (via adapters)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE SYNC DELEGATES (→ LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void loadSelectedPrimitive() { layerManager.loadSelectedPrimitive(); }
    public void saveSelectedPrimitive() { layerManager.saveSelectedPrimitive(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SELECTION DELEGATES (→ LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getSelectedLayerIndex() { return layerManager.getSelectedLayerIndex(); }
    public void setSelectedLayerIndex(int index) { layerManager.setSelectedLayerIndex(index); }
    public int getSelectedPrimitiveIndex() { return layerManager.getSelectedPrimitiveIndex(); }
    public void setSelectedPrimitiveIndex(int index) { layerManager.setSelectedPrimitiveIndex(index); }
    public FieldLayer getSelectedLayer() { return layerManager.getSelectedLayer(); }
    public Primitive getSelectedPrimitive() { return layerManager.getSelectedPrimitive(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER CRUD DELEGATES (→ LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getLayerCount() { return layerManager.getCount(); }
    public List<FieldLayer> getFieldLayers() { return layerManager.getAll(); }
    public int addLayer() { return layerManager.add(); }
    public boolean removeLayer(int index) { return layerManager.remove(index); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE CRUD DELEGATES (→ LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public int getPrimitiveCount(int layerIndex) { return layerManager.getPrimitiveCount(layerIndex); }
    public int addPrimitive(int layerIndex) { return layerManager.addPrimitive(layerIndex); }
    public boolean removePrimitive(int layerIndex, int primitiveIndex) { return layerManager.removePrimitive(layerIndex, primitiveIndex); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void reset() {
        // Reset all adapters
        for (PrimitiveAdapter adapter : adapters) {
            adapter.reset();
        }
        
        // Reset managers
        layerManager.reset();
        profileManager.reset();
        bindingsManager.reset();
        triggerManager.reset();
        
        // Reset definition-level fields to defaults
        beam = net.cyberpunk042.field.BeamConfig.NONE;
        follow = net.cyberpunk042.field.instance.FollowConfig.DEFAULT;
        modifiers = net.cyberpunk042.field.Modifiers.DEFAULT;
        lifecycle = net.cyberpunk042.field.influence.LifecycleConfig.DEFAULT;
        forceConfig = null;
        
        // Reset field-level adapters (not in adapters list since they aren't PrimitiveAdapters)
        shockwaveAdapter.reset();
        magicCircleAdapter.reset();
        fieldVisualAdapter.clearPreview();
        
        markDirty();
        Logging.GUI.topic("state").info("FieldEditState reset to defaults");
        
        // Notify listeners of full reset (widgets should reinitialize)
        notifyStateChanged(ChangeType.FULL_RESET);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELD DEFINITION INTEGRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Loads shockwave config from a FieldDefinition.
     * Call this after loading the definition to sync the FX state.
     */
    public void loadShockwaveFromDefinition(net.cyberpunk042.field.FieldDefinition def) {
        if (def != null && def.shockwave() != null) {
            shockwaveAdapter.loadFromJson(def.shockwave());
            Logging.GUI.topic("state").debug("Loaded shockwave config from field definition: {}", def.id());
        } else {
            shockwaveAdapter.reset();
        }
    }
    
    /**
     * Loads field visual config from a FieldDefinition.
     * Call this after loading the definition to sync the FX state.
     */
    public void loadFieldVisualFromDefinition(net.cyberpunk042.field.FieldDefinition def) {
        if (def != null && def.fieldVisual() != null) {
            fieldVisualAdapter.loadFromJson(def.fieldVisual());
            Logging.GUI.topic("state").debug("Loaded field visual config from field definition: {}", def.id());
            // Notify listeners so UI refreshes and shader gets updated
            notifyStateChanged(ChangeType.PROFILE_LOADED);
        } else {
            // Reset to defaults then apply schema defaults
            fieldVisualAdapter.reset();
            applyFieldVisualSchemaDefaults();
        }
    }
    
    /**
     * Applies schema defaults to the field visual adapter based on current effect type and version.
     * Called when creating a new effect or resetting to defaults.
     */
    public void applyFieldVisualSchemaDefaults() {
        EffectType effectType = fieldVisualAdapter.effectType();
        int version = (int) fieldVisualAdapter.get("fieldVisual.version");
        
        var schema = net.cyberpunk042.client.gui.schema.EffectSchemaRegistry.get(effectType, version);
        if (schema != null) {
            fieldVisualAdapter.applySchemaDefaults(schema);
            Logging.GUI.topic("state").debug("Applied schema defaults for {} V{}", effectType, version);
        } else {
            Logging.GUI.topic("state").debug("No schema found for {} V{}", effectType, version);
        }
    }
    
    // JSON SERIALIZATION (delegates to SerializationManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public JsonObject toJson() {
        return serializationManager.toJson();
    }
    
    public void fromJson(JsonObject json) {
        serializationManager.fromJson(json);
        clearDirty();
    }
    
    public String toStateJson() {
        return serializationManager.toJsonString();
    }
    
    public void fromStateJson(String jsonStr) {
        serializationManager.fromJsonString(jsonStr);
        clearDirty();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE ENTRY (type alias for import compatibility)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Re-export ProfileEntry so existing imports work: import FieldEditState.ProfileEntry
    public record ProfileEntry(
        String name, 
        boolean isServer, 
        String description,
        net.cyberpunk042.field.category.ProfileSource source
    ) {
        public ProfileEntry(String name, boolean isServer) { 
            this(name, isServer, "", isServer 
                ? net.cyberpunk042.field.category.ProfileSource.SERVER 
                : net.cyberpunk042.field.category.ProfileSource.LOCAL); 
        }
        public ProfileEntry(String name, boolean isServer, String description) { 
            this(name, isServer, description, isServer 
                ? net.cyberpunk042.field.category.ProfileSource.SERVER 
                : net.cyberpunk042.field.category.ProfileSource.LOCAL); 
        }
        public boolean isBundled() { 
            return source == net.cyberpunk042.field.category.ProfileSource.BUNDLED; 
        }
        public boolean isLocal() { 
            return source == net.cyberpunk042.field.category.ProfileSource.LOCAL; 
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILES MANAGEMENT DELEGATES (→ ProfileManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public List<ProfileEntry> getProfiles() { 
        // Convert ProfileManager.ProfileEntry to FieldEditState.ProfileEntry
        return profileManager.getProfiles().stream()
            .map(e -> new ProfileEntry(e.name(), e.isServer(), e.description(), e.source()))
            .collect(java.util.stream.Collectors.toList());
    }
    public String getCurrentProfileName() { return profileManager.getCurrentName(); }
    public void setCurrentProfileName(String name) { profileManager.setCurrentName(name); }
    public void updateServerProfiles(List<String> names) { profileManager.updateServerProfiles(names); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ADDITIONAL BACKWARD-COMPATIBLE ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    // shapeType accessor (used by many panels)
    public String getShapeType() { return shapeAdapter.shapeType(); }
    public void setShapeType(String type) { shapeAdapter.setShapeType(type); markDirty(); }
    
    // shapeType field access (used by FillSubPanel) - MUST return live value from adapter!
    // This was a bug: it was a static field initialized to "sphere" that never updated
    public String getShapeTypeField() { return shapeAdapter.shapeType(); }
    
    // ProfileSnapshot type alias for import compatibility
    public static class ProfileSnapshot extends ProfileManager.ProfileSnapshot {
        public ProfileSnapshot(String jsonStr) { super(jsonStr); }
    }
    
    public ProfileSnapshot getProfileSnapshot() { return new ProfileSnapshot(profileManager.getSnapshotJson()); }
    public void setSnapshot(String json) { profileManager.setSnapshot(json); }
    public void saveSnapshot() { profileManager.saveSnapshot(); }
    public void restoreFromSnapshot() { profileManager.restoreFromSnapshot(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPE-BASED GET/SET (backward compat)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public <T> T get(Class<T> type) { return StateAccessor.get(this, type); }
    public <T> T get(String name, Class<T> type) { return StateAccessor.get(this, name, type); }
    public <T> void set(T value) { StateAccessor.set(this, value); markDirty(); }
    
    public boolean setFromString(String path, String value) {
        try {
            Object current = get(path);
            Object parsed = parseValue(value, current);
            set(path, parsed);
            return true;
        } catch (Exception e) {
            Logging.GUI.topic("state").warn("Failed to set {} = {}: {}", path, value, e.getMessage());
            return false;
        }
    }
    
    private Object parseValue(String value, Object current) {
        if (current == null) return value;
        Class<?> type = current.getClass();
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Float.class || type == float.class) return Float.parseFloat(value);
        if (type == Double.class || type == double.class) return Double.parseDouble(value);
        return value;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BINDINGS DELEGATES (backward compat → BindingsManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public List<net.cyberpunk042.field.influence.BindingConfig> getBindings() { return bindingsManager.getAll(); }
    public void addBinding(net.cyberpunk042.field.influence.BindingConfig binding) { bindingsManager.add(binding); }
    public boolean removeBinding(String property) { return bindingsManager.remove(property); }
    public void clearBindings() { bindingsManager.clear(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER DELEGATES (backward compat → LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public float getLayerAlpha(int index) { return layerManager.getAlpha(index); }
    public void setLayerAlpha(int index, float alpha) { layerManager.setAlpha(index, alpha); }
    public String getLayerBlendMode(int index) { return layerManager.getBlendMode(index); }
    public void setLayerBlendMode(int index, String mode) { layerManager.setBlendMode(index, mode); }
    public boolean isLayerVisible(int index) { return layerManager.isVisible(index); }
    public boolean toggleLayerVisibility(int index) { return layerManager.toggleVisibility(index); }
    public boolean swapLayers(int a, int b) { return layerManager.swap(a, b); }
    public int findLayerByName(String name) { return layerManager.findByName(name); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE DELEGATES (backward compat → LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean swapPrimitives(int layerIdx, int a, int b) { return layerManager.swapPrimitives(layerIdx, a, b); }
    public String getPrimitiveName(int layerIdx, int primIdx) { return layerManager.getPrimitiveName(layerIdx, primIdx); }
    public int findPrimitiveById(int layerIdx, String id) { return layerManager.findPrimitiveById(layerIdx, id); }
    public int addPrimitiveWithId(int layerIdx, String type) {
        int idx = addPrimitive(layerIdx);
        if (idx >= 0) shapeAdapter.setShapeType(type);
        return idx;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE DELEGATES (backward compat → ProfileManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void setCurrentProfile(String name, boolean isServer) { profileManager.setCurrent(name, isServer); }
    public String toProfileJson(String profileName) { return profileManager.toJson(profileName); }
    public void fromProfileJson(String json) { profileManager.fromJson(json); }
    public String getCurrentShapeFragmentName() { return profileManager.getCurrentShapeFragmentName(); }
    public String getCurrentFillFragmentName() { return profileManager.getCurrentFillFragmentName(); }
    public String getCurrentAnimationFragmentName() { return profileManager.getCurrentAnimationFragmentName(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TRIGGER DELEGATES (backward compat → TriggerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void fireTestTrigger(net.cyberpunk042.field.influence.TriggerConfig config) { triggerManager.fire(config); }
    public net.cyberpunk042.field.influence.TriggerConfig getActiveTestTrigger() { return triggerManager.getActive(); }
    public float getTestTriggerProgress() { return triggerManager.getProgress(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LAYER/PRIMITIVE NAMING DELEGATES (backward compat → LayerManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void renameLayer(int index, String newName) { layerManager.rename(index, newName); }
    public void renamePrimitive(int layerIdx, int primIdx, String newName) { layerManager.renamePrimitive(layerIdx, primIdx, newName); }
    public String getLayerName(int index) { return layerManager.getName(index); }
    public int addLayerWithName(String name) { 
        int idx = layerManager.addWithName(name);
        if (idx >= 0) selectedPrimitivePerLayer.add(0);
        return idx;
    }
    public List<? extends Primitive> getPrimitivesForLayer(int index) { return layerManager.getPrimitives(index); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UNDO/REDO (stubs - user requested removal)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public void undo() { Logging.GUI.topic("state").debug("Undo not implemented in adapter architecture"); }
    public void redo() { Logging.GUI.topic("state").debug("Redo not implemented in adapter architecture"); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE HELPERS DELEGATES (backward compat → ProfileManager)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public boolean isCurrentProfileServerSourced() { return profileManager.isCurrentServerSourced(); }
    public void setServerProfiles(List<String> names) { profileManager.updateServerProfiles(names); }
    public boolean factoryReset() { return profileManager.factoryReset(); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEFINITION-LEVEL FIELDS (backward compat)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField @DefinitionField("beam") 
    private net.cyberpunk042.field.BeamConfig beam = net.cyberpunk042.field.BeamConfig.NONE;
    
    @StateField @DefinitionField("follow") 
    private net.cyberpunk042.field.instance.FollowConfig follow = net.cyberpunk042.field.instance.FollowConfig.DEFAULT;
    
    @StateField @DefinitionField("modifiers") 
    private net.cyberpunk042.field.Modifiers modifiers = net.cyberpunk042.field.Modifiers.DEFAULT;
    
    @StateField @DefinitionField("lifecycle") 
    private net.cyberpunk042.field.influence.LifecycleConfig lifecycle = net.cyberpunk042.field.influence.LifecycleConfig.DEFAULT;
    
    @StateField @DefinitionField("forceConfig") 
    private net.cyberpunk042.field.force.ForceFieldConfig forceConfig = null;
    
    public net.cyberpunk042.field.BeamConfig beam() { return beam; }
    public net.cyberpunk042.field.instance.FollowConfig follow() { return follow; }
    public net.cyberpunk042.field.Modifiers modifiers() { return modifiers; }
    public net.cyberpunk042.field.influence.LifecycleConfig lifecycle() { return lifecycle; }
    public net.cyberpunk042.field.force.ForceFieldConfig forceConfig() { return forceConfig; }
    
    public FieldEditState() {
        // Initialize managers with required references
        this.layerManager = new LayerManager(this, fieldLayers, selectedPrimitivePerLayer, adapters);
        this.profileManager = new ProfileManager(this);
        this.bindingsManager = new BindingsManager(this);
        this.triggerManager = new TriggerManager(this);
        this.serializationManager = new SerializationManager(this, adapters, fieldLayers, selectedPrimitivePerLayer, layerManager);
        
        // Wire selection change callback
        this.layerManager.setSelectionChangeCallback(this::notifySelectionChanged);
        
        Logging.GUI.topic("state").debug("FieldEditState created with adapter + manager architecture");
    }
    
    /**
     * Loads state from a FieldDefinition (saved profile).
     * Delegates to ProfileManager (proper Single Responsibility).
     */
    public void loadFromDefinition(net.cyberpunk042.field.FieldDefinition definition) {
        profileManager.loadFromDefinition(definition);
    }
}

