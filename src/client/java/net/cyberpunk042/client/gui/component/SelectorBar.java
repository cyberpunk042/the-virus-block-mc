package net.cyberpunk042.client.gui.component;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.widget.CompactSelector;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Selector bar component containing:
 * <ul>
 *   <li>Layer selector (with add/select/click actions)</li>
 *   <li>Primitive selector (with add/select/click actions)</li>
 * </ul>
 * 
 * <p>Each selector is a {@link CompactSelector} that allows navigation through
 * layers/primitives and triggers callbacks for selection changes.</p>
 */
public class SelectorBar implements ScreenComponent {
    
    private static final int SELECTOR_HEIGHT = 22;
    
    private Bounds bounds;
    private final TextRenderer textRenderer;
    private final List<ClickableWidget> widgets = new ArrayList<>();
    
    // Selectors
    private CompactSelector<String> layerSelector;
    private CompactSelector<String> primitiveSelector;
    
    // Callbacks
    private final Consumer<String> onLayerSelect;
    private final Consumer<String> onPrimitiveSelect;
    private final Consumer<String> onLayerClick;  // For modal
    private final Consumer<String> onPrimitiveClick;  // For modal
    private final Runnable onLayerAdd;
    private final Runnable onPrimitiveAdd;
    
    // Data suppliers
    private final java.util.function.Supplier<List<String>> layerNamesSupplier;
    private final java.util.function.Supplier<List<String>> primitiveNamesSupplier;
    private final java.util.function.IntSupplier selectedLayerIndex;
    private final java.util.function.IntSupplier selectedPrimitiveIndex;
    
    /**
     * Creates a selector bar.
     */
    public SelectorBar(
            TextRenderer textRenderer,
            java.util.function.Supplier<List<String>> layerNamesSupplier,
            java.util.function.Supplier<List<String>> primitiveNamesSupplier,
            java.util.function.IntSupplier selectedLayerIndex,
            java.util.function.IntSupplier selectedPrimitiveIndex,
            Consumer<String> onLayerSelect,
            Consumer<String> onPrimitiveSelect,
            Consumer<String> onLayerClick,
            Consumer<String> onPrimitiveClick,
            Runnable onLayerAdd,
            Runnable onPrimitiveAdd) {
        
        this.textRenderer = textRenderer;
        this.layerNamesSupplier = layerNamesSupplier;
        this.primitiveNamesSupplier = primitiveNamesSupplier;
        this.selectedLayerIndex = selectedLayerIndex;
        this.selectedPrimitiveIndex = selectedPrimitiveIndex;
        this.onLayerSelect = onLayerSelect;
        this.onPrimitiveSelect = onPrimitiveSelect;
        this.onLayerClick = onLayerClick;
        this.onPrimitiveClick = onPrimitiveClick;
        this.onLayerAdd = onLayerAdd;
        this.onPrimitiveAdd = onPrimitiveAdd;
    }
    
    @Override
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        rebuild();
    }
    
    @Override
    public void rebuild() {
        widgets.clear();
        if (bounds == null) return;
        
        // Layer selector at top
        Bounds layerBounds = bounds.sliceTop(SELECTOR_HEIGHT).inset(4, 2);
        List<String> layerNames = new ArrayList<>(layerNamesSupplier.get());
        
        layerSelector = new CompactSelector<>("LAYER:", textRenderer, layerNames, s -> s)
            .onSelect(onLayerSelect)
            .onAdd(onLayerAdd)
            .onItemClick(onLayerClick);
        layerSelector.setBounds(layerBounds);
        layerSelector.selectIndex(selectedLayerIndex.getAsInt());
        widgets.addAll(layerSelector.getWidgets());
        
        // Primitive selector below
        Bounds primBounds = bounds.withoutTop(SELECTOR_HEIGHT).sliceTop(SELECTOR_HEIGHT).inset(4, 2);
        List<String> primNames = new ArrayList<>(primitiveNamesSupplier.get());
        
        primitiveSelector = new CompactSelector<>("PRIM:", textRenderer, primNames, s -> s)
            .onSelect(onPrimitiveSelect)
            .onAdd(onPrimitiveAdd)
            .onItemClick(onPrimitiveClick);
        primitiveSelector.setBounds(primBounds);
        primitiveSelector.selectIndex(selectedPrimitiveIndex.getAsInt());
        widgets.addAll(primitiveSelector.getWidgets());
    }
    
    @Override
    public List<ClickableWidget> getWidgets() {
        return widgets;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Selectors handle their own rendering
    }
    
    /**
     * Refreshes the layer selector with current names and selection.
     */
    public void refreshLayers() {
        if (layerSelector != null && bounds != null) {
            Bounds layerBounds = bounds.sliceTop(SELECTOR_HEIGHT).inset(4, 2);
            List<String> layerNames = new ArrayList<>(layerNamesSupplier.get());
            layerSelector = new CompactSelector<>("LAYER:", textRenderer, layerNames, s -> s)
                .onSelect(onLayerSelect)
                .onAdd(onLayerAdd)
                .onItemClick(onLayerClick);
            layerSelector.setBounds(layerBounds);
            layerSelector.selectIndex(selectedLayerIndex.getAsInt());
            
            // Rebuild widget list (simplified - full rebuild)
            rebuild();
        }
    }
    
    /**
     * Refreshes the primitive selector with current names and selection.
     */
    public void refreshPrimitives() {
        if (primitiveSelector != null && bounds != null) {
            // Rebuild to update primitive list
            rebuild();
        }
    }
    
    /**
     * Refreshes both selectors with current state.
     */
    public void refresh() {
        rebuild();
    }
    
    /**
     * Selects a layer by index.
     */
    public void selectLayerIndex(int index) {
        if (layerSelector != null) {
            layerSelector.selectIndex(index);
        }
    }
    
    /**
     * Selects a primitive by index.
     */
    public void selectPrimitiveIndex(int index) {
        if (primitiveSelector != null) {
            primitiveSelector.selectIndex(index);
        }
    }
    
    /**
     * Returns the layer selector.
     */
    public CompactSelector<String> getLayerSelector() {
        return layerSelector;
    }
    
    /**
     * Returns the primitive selector.
     */
    public CompactSelector<String> getPrimitiveSelector() {
        return primitiveSelector;
    }
}
