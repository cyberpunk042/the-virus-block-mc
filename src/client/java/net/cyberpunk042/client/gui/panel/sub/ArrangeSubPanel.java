package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.render.TestFieldRenderer;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.FragmentRegistry;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.pattern.ShuffleGenerator;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.cyberpunk042.visual.shape.Shape;

/**
 * Debug sub-panel for Arrangement patterns and Shuffle exploration.
 * 
 * <h2>Two Tabs</h2>
 * <ul>
 *   <li><b>Patterns</b> - Select named patterns for different shape parts</li>
 *   <li><b>Explorer</b> - Explore all vertex permutations with visual diagram</li>
 * </ul>
 */
public class ArrangeSubPanel extends AbstractPanel {
    
    private final TextRenderer textRenderer;
    
    // Tab state
    private enum Tab { PATTERNS, EXPLORER }
    private Tab currentTab = Tab.PATTERNS;
    
    // Shared state
    private String currentPart = "surface";  // Current part being shuffled (sides, capTop, etc.)
    private CellType currentCellType = CellType.QUAD;
    
    // Explorer state
    private int currentPermutation = 0;
    private String currentDescription = "";
    private int[] currentMapping = new int[]{0, 1, 2, 3};
    
    // Patterns tab state
    private boolean showPerPartControls = false;
    
    // Text fields
    private TextFieldWidget jumpField;
    private TextFieldWidget saveNameField;
    
    public ArrangeSubPanel(Screen parent, FieldEditState state, TextRenderer textRenderer) {
        super(parent, state);
        this.textRenderer = textRenderer;
        Logging.GUI.topic("panel").debug("ArrangeSubPanel created");
    }
    
    /**
     * Returns the total height of this panel's content.
     */
    public int getHeight() {
        return Math.max(contentHeight, 200);
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        
        if (bounds.isEmpty()) {
            setBoundsQuiet(new Bounds(0, 0, width, height));
        }
        
        initWidgets();
    }
    
    @Override
    protected void init() {
        initWidgets();
    }
    
    /**
     * Creates all widgets based on current tab.
     */
    private void initWidgets() {
        widgets.clear();
        
        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = GuiConstants.WIDGET_HEIGHT;
        int tabW = (w - GuiConstants.PADDING) / 2;
        
        // ═══════════════════════════════════════════════════════════════════
        // TAB BUTTONS (always visible)
        // ═══════════════════════════════════════════════════════════════════
        
        widgets.add(ButtonWidget.builder(
                Text.literal(currentTab == Tab.PATTERNS ? "§e[Patterns]" : "Patterns"),
                btn -> switchTab(Tab.PATTERNS))
            .dimensions(x, y, tabW, h - 2)
            .build());
        
        widgets.add(ButtonWidget.builder(
                Text.literal(currentTab == Tab.EXPLORER ? "§e[Explorer]" : "Explorer"),
                btn -> switchTab(Tab.EXPLORER))
            .dimensions(x + tabW + GuiConstants.PADDING, y, tabW, h - 2)
            .build());
        
        y += h + 4;
        
        // ═══════════════════════════════════════════════════════════════════
        // TAB CONTENT (mutually exclusive)
        // ═══════════════════════════════════════════════════════════════════
        
        if (currentTab == Tab.PATTERNS) {
            y = initPatternsTab(x, y, w, h);
        } else {
            y = initExplorerTab(x, y, w, h);
        }
        
        contentHeight = y - bounds.y() + GuiConstants.PADDING;
        
        Logging.GUI.topic("panel").debug("ArrangeSubPanel initialized, tab={}, widgets={}", 
            currentTab, widgets.size());
    }
    
    private void switchTab(Tab tab) {
        if (currentTab != tab) {
            currentTab = tab;
            Logging.GUI.topic("panel").debug("ArrangeSubPanel switching to tab: {}", tab);
            initWidgets();
            notifyWidgetsChanged();  // Using inherited method
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PATTERNS TAB
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int initPatternsTab(int x, int y, int w, int h) {
        int labelW = 60;
        int controlW = w - labelW - GuiConstants.PADDING;
        
        // Cell Type selector
        widgets.add(CyclingButtonWidget.<CellType>builder(ct -> Text.literal(ct.name()))
            .values(CellType.values())
            .initially(currentCellType)
            .build(x + labelW, y, controlW, h, Text.literal("Cell"),
                (btn, val) -> {
                    currentCellType = val;
                    initWidgets();
                    notifyWidgetsChanged();
                }));
        y += h + 2;
        
        // Pattern selector
        List<String> patterns = getPatternsForCellType(currentCellType);
        String currentPattern = getPatternFromState(patterns.get(0));
        
        widgets.add(CyclingButtonWidget.<String>builder(Text::literal)
            .values(patterns)
            .initially(patterns.contains(currentPattern) ? currentPattern : patterns.get(0))
            .build(x + labelW, y, controlW, h, Text.literal("Pattern"),
                (btn, val) -> applyPatternToState(val)));
        y += h + 4;
        
        // Per-Part toggle
        widgets.add(ButtonWidget.builder(
                Text.literal(showPerPartControls ? "▼ Per-Part Overrides" : "▶ Per-Part Overrides"),
                btn -> {
                    showPerPartControls = !showPerPartControls;
                    initWidgets();
                    notifyWidgetsChanged();
                })
            .dimensions(x, y, w, h)
            .build());
        y += h + 2;
        
        // Per-Part dropdowns (only when expanded)
        if (showPerPartControls) {
            String shapeType = state.getString("shapeType").toLowerCase();
            
            if (shapeType.equals("sphere")) {
                widgets.add(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.poles", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Poles"),
                        (btn, val) -> applyPartPattern("poles", val)));
                y += h + 2;
                
                widgets.add(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.equator", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Equator"),
                        (btn, val) -> applyPartPattern("equator", val)));
                y += h + 2;
            } else if (shapeType.equals("prism") || shapeType.equals("cylinder")) {
                widgets.add(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.capTop", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Caps"),
                        (btn, val) -> applyPartPattern("caps", val)));
                y += h + 2;
                
                widgets.add(CyclingButtonWidget.<String>builder(Text::literal)
                    .values(patterns)
                    .initially(getStringOrDefault("arrangement.sides", patterns.get(0)))
                    .build(x + labelW, y, controlW, h, Text.literal("Sides"),
                        (btn, val) -> applyPartPattern("sides", val)));
                y += h + 2;
            }
        }
        
        return y;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLORER TAB
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int initExplorerTab(int x, int y, int w, int h) {
        // Initial update before building widgets
        updateShuffleDisplay();
        
        // Get valid parts for current shape
        String[] validParts = getValidPartsForShape();
        
        // Ensure currentPart is valid for this shape
        boolean partFound = false;
        for (String part : validParts) {
            if (part.equals(currentPart)) {
                partFound = true;
                break;
            }
        }
        if (!partFound && validParts.length > 0) {
            currentPart = validParts[0];
        }
        
        // Get CellType for current part
        CellType partCellType = getCellTypeForPart(currentPart);
        if (partCellType != null && partCellType != currentCellType) {
            currentCellType = partCellType;
            currentPermutation = 0;
            updateShuffleDisplay();
        }
        
        // Part selector with label
        int labelW = 35;  // Width for "Part:" and "Cell:" labels
        int fieldW = (w - labelW - GuiConstants.COMPACT_GAP) / 2;
        
        // Draw "Part:" label (will be rendered in render())
        // Part selector button (shows "sides", "capTop", "capBottom", etc.)
        widgets.add(CyclingButtonWidget.<String>builder(part -> Text.literal("Part: " + part))
            .values(validParts)
            .initially(currentPart)
            .omitKeyText()
            .build(x, y, w / 2 - 2, h, Text.literal(""),
                (btn, val) -> {
                    currentPart = val;
                    // Update CellType to match this part
                    CellType newCellType = getCellTypeForPart(val);
                    if (newCellType != null) {
                        currentCellType = newCellType;
                    }
                    currentPermutation = 0;
                    updateShuffleDisplay();
                    initWidgets();
                    notifyWidgetsChanged();
                }));
        
        // CellType display button (shows "Cell: QUAD", etc.)
        widgets.add(CyclingButtonWidget.<CellType>builder(ct -> Text.literal("Cell: " + ct.name()))
            .values(currentCellType)  // Single value - effectively read-only
            .initially(currentCellType)
            .omitKeyText()
            .build(x + w / 2 + 2, y, w / 2 - 2, h, Text.literal(""),
                (btn, val) -> {}));  // No action - display only
        y += h + 8;
        
        // Space for visual diagram (rendered in render())
        y += 80;
        
        // Navigation buttons: ◀ | 🎲 | ▶ (compact)
        int compactH = GuiConstants.COMPACT_HEIGHT;
        int btnW = (w - GuiConstants.COMPACT_GAP * 2) / 3;
        
        widgets.add(ButtonWidget.builder(Text.literal("◀"), btn -> {
                currentPermutation--;
                if (currentPermutation < 0) {
                    currentPermutation = getTotal() - 1;
                }
                updateShuffleDisplay();
                applyCurrentPermutationAsPattern();
            })
            .dimensions(x, y, btnW, compactH)
            .build());
        
        widgets.add(ButtonWidget.builder(Text.literal("🎲"), btn -> {
                currentPermutation = (int)(Math.random() * getTotal());
                updateShuffleDisplay();
                applyCurrentPermutationAsPattern();
            })
            .dimensions(x + btnW + GuiConstants.COMPACT_GAP, y, btnW, compactH)
            .build());
        
        widgets.add(ButtonWidget.builder(Text.literal("▶"), btn -> {
                currentPermutation++;
                if (currentPermutation >= getTotal()) {
                    currentPermutation = 0;
                }
                updateShuffleDisplay();
                applyCurrentPermutationAsPattern();
            })
            .dimensions(x + (btnW + GuiConstants.COMPACT_GAP) * 2, y, btnW, compactH)
            .build());
        
        y += compactH + GuiConstants.COMPACT_GAP;
        
        // Jump to # (compact)
        int jumpFieldW = w - 40;
        jumpField = new TextFieldWidget(textRenderer, x, y, jumpFieldW, compactH, Text.literal("Jump"));
        jumpField.setPlaceholder(Text.literal("#..."));
        jumpField.setMaxLength(6);
        widgets.add(jumpField);
        
        widgets.add(ButtonWidget.builder(Text.literal("→"), btn -> {
                if (jumpField == null) return;
                try {
                    int target = Integer.parseInt(jumpField.getText()) - 1;
                    if (target >= 0 && target < getTotal()) {
                        currentPermutation = target;
                        updateShuffleDisplay();
                        applyCurrentPermutationAsPattern();
                    } else {
                        Logging.GUI.topic("arrange").warn("Jump target out of range: 1-{}", getTotal());
                    }
                } catch (NumberFormatException e) {
                    Logging.GUI.topic("arrange").warn("Invalid jump target - enter a number");
                }
            })
            .dimensions(x + jumpFieldW + GuiConstants.COMPACT_GAP, y, 30, compactH)
            .build());
        
        y += compactH + GuiConstants.COMPACT_GAP;
        
        // Save as pattern (compact)
        int saveFieldW = w - 50;
        saveNameField = new TextFieldWidget(textRenderer, x, y, saveFieldW, compactH, Text.literal("Name"));
        saveNameField.setPlaceholder(Text.literal("name.."));
        saveNameField.setMaxLength(32);
        widgets.add(saveNameField);
        
        widgets.add(ButtonWidget.builder(Text.literal("💾"), btn -> saveAsPattern())
            .dimensions(x + saveFieldW + GuiConstants.COMPACT_GAP, y, 40, compactH)
            .build());
        
        y += compactH + 8;
        
        return y;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private String getPatternFromState(String defaultValue) {
        ArrangementConfig arr = state.arrangement();
        if (arr != null && arr.defaultPattern() != null) {
            return arr.defaultPattern();
        }
        return defaultValue;
    }
    
    private void applyPatternToState(String patternName) {
        ArrangementConfig current = state.arrangement();
        ArrangementConfig updated = current.toBuilder()
            .defaultPattern(patternName)
            .build();
        state.set("arrangement", updated);
        TestFieldRenderer.markDirty();  // Force mesh rebuild
        Logging.GUI.topic("arrange").info("Pattern applied: {}", patternName);
    }
    
    private void applyPartPattern(String part, String patternName) {
        ArrangementConfig current = state.arrangement();
        ArrangementConfig.Builder builder = current.toBuilder();
        
        // Map part name to the correct builder method
        switch (part) {
            // Sphere parts
            case "main" -> builder.main(patternName);
            case "poles" -> builder.poles(patternName);
            case "equator" -> builder.equator(patternName);
            case "hemisphereTop" -> builder.hemisphereTop(patternName);
            case "hemisphereBottom" -> builder.hemisphereBottom(patternName);
            // Ring/Disc parts
            case "surface" -> builder.surface(patternName);
            case "innerEdge" -> builder.innerEdge(patternName);
            case "outerEdge" -> builder.outerEdge(patternName);
            case "edge", "discEdge" -> builder.discEdge(patternName);
            // Prism/Cylinder parts
            case "sides" -> builder.sides(patternName);
            case "capTop" -> builder.capTop(patternName);
            case "capBottom" -> builder.capBottom(patternName);
            case "caps" -> builder.capTop(patternName).capBottom(patternName);  // Both caps
            case "edges", "prismEdges" -> builder.prismEdges(patternName);
            // Polyhedron parts
            case "faces" -> builder.faces(patternName);
            case "polyEdges" -> builder.polyEdges(patternName);
            case "vertices" -> builder.vertices(patternName);
            // Default: set as default pattern
            default -> builder.defaultPattern(patternName);
        }
        
        state.set("arrangement", builder.build());
        TestFieldRenderer.markDirty();  // Force mesh rebuild
        Logging.GUI.topic("arrange").debug("Part pattern applied: {} = {}", part, patternName);
    }
    
    private void applyCurrentPermutationAsPattern() {
        String patternName = "shuffle_" + currentCellType.name().toLowerCase() + "_" + currentPermutation;
        
        // Apply to the specific part, not just default
        applyPartPattern(currentPart, patternName);
        
        // Enhanced logging for easy tracking - format: [ARRANGE] Part:CellType:#N/Total = Description
        Logging.GUI.topic("arrange").info("[ARRANGE] {}:{}:#{}/{} = {}", 
            currentPart.toUpperCase(),
            currentCellType.name(), 
            currentPermutation + 1, 
            getTotal(),
            currentDescription);
    }
    
    private void saveAsPattern() {
        if (saveNameField == null) return;
        String name = saveNameField.getText().trim();
        if (name.isEmpty()) {
            Logging.GUI.topic("arrange").warn("Cannot save pattern: name is empty");
            return;
        }
        
        Logging.GUI.topic("arrange").info("Pattern saved: {} = {}#{}", name, currentCellType, currentPermutation);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private List<String> getPatternsForCellType(CellType cellType) {
        List<String> fromRegistry = FragmentRegistry.listArrangementFragments();
        if (!fromRegistry.isEmpty() && !fromRegistry.get(0).equals("Default")) {
            return fromRegistry;
        }
        
         return switch (cellType) {
            // Use actual defined patterns from QuadPattern enum
            case QUAD -> List.of("filled_1", "triangle_1", "triangle_2", "tooth_1", "parallelogram_1", "wave_1", "stripe_1");
            case SEGMENT -> List.of("full", "alternating", "sparse");
            case SECTOR -> List.of("full", "half", "quarters");
            case EDGE -> List.of("full", "latitude", "longitude");
            case TRIANGLE -> List.of("full", "alternating", "inverted");
        };
    }
    
    /**
     * Gets valid part names for the current shape.
     * This populates the Part selector dropdown.
     */
    private String[] getValidPartsForShape() {
        Shape shape = state.currentShape();
        
        if (shape != null && shape.getParts() != null && !shape.getParts().isEmpty()) {
            String[] parts = shape.getParts().keySet().toArray(new String[0]);
            Logging.GUI.topic("arrange").debug("[PARTS] Shape '{}' has parts: {}", 
                state.getString("shapeType"), java.util.Arrays.toString(parts));
            return parts;
        }
        
        // Fallback for shapes without explicit parts
        Logging.GUI.topic("arrange").debug("[PARTS] No parts defined, using 'surface'");
        return new String[]{"surface"};
    }
    
    /**
     * Gets the CellType for a specific part name.
     */
    private CellType getCellTypeForPart(String partName) {
        Shape shape = state.currentShape();
        
        if (shape != null && shape.getParts() != null) {
            CellType ct = shape.getParts().get(partName);
            if (ct != null) {
                return ct;
            }
        }
        
        // Fallback to shape's primary cell type
        if (shape != null) {
            return shape.primaryCellType();
        }
        
        return CellType.QUAD;  // Ultimate fallback
    }
    
    /**
     * Gets valid CellTypes for the current shape based on its parts.
     * This filters the CellType selector to only show relevant options.
     */
    private CellType[] getValidCellTypesForShape() {
        String shapeType = state.getString("shapeType");
        Shape shape = state.currentShape();
        
        if (shape != null && shape.getParts() != null) {
            // Collect unique cell types from shape's parts
            Set<CellType> cellTypes = new HashSet<>();
            for (CellType ct : shape.getParts().values()) {
                cellTypes.add(ct);
            }
            // Also add primary cell type
            cellTypes.add(shape.primaryCellType());
            
            // Log for debugging
            Logging.GUI.topic("arrange").debug("[CELLTYPE] Shape '{}' supports: {}", shapeType, cellTypes);
            
            return cellTypes.toArray(new CellType[0]);
        }
        
        // Fallback: return all cell types
        Logging.GUI.topic("arrange").debug("[CELLTYPE] No shape, showing all CellTypes");
        return CellType.values();
    }
    
    private int getTotal() {
        return switch (currentCellType) {
            case QUAD -> ShuffleGenerator.quadCount();
            case SEGMENT -> ShuffleGenerator.segmentCount();
            case SECTOR -> ShuffleGenerator.sectorCount();
            case EDGE -> ShuffleGenerator.edgeCount();
            case TRIANGLE -> ShuffleGenerator.triangleCount();
        };
    }
    
    private void updateShuffleDisplay() {
        currentDescription = switch (currentCellType) {
            case QUAD -> ShuffleGenerator.getQuad(currentPermutation).describe();
            case SEGMENT -> ShuffleGenerator.getSegment(currentPermutation).describe();
            case SECTOR -> ShuffleGenerator.getSector(currentPermutation).describe();
            case EDGE -> ShuffleGenerator.getEdge(currentPermutation).describe();
            case TRIANGLE -> ShuffleGenerator.getTriangle(currentPermutation).describe();
        };
        
        currentMapping = extractMapping(currentCellType, currentPermutation);
        
        Logging.GUI.topic("arrange").debug("Shuffle: {}#{} = {}", 
            currentCellType, currentPermutation, currentDescription);
    }
    
    private int[] extractMapping(CellType cellType, int permutation) {
        return switch (cellType) {
            case QUAD -> {
                var quad = ShuffleGenerator.getQuad(permutation);
                int[] mapping = new int[4];
                if (quad.tri1() != null) {
                    for (int i = 0; i < Math.min(3, quad.tri1().length); i++) {
                        mapping[i] = quad.tri1()[i].index;
                    }
                }
                if (quad.tri2() != null && quad.tri2().length > 0) {
                    mapping[3] = quad.tri2()[0].index;
                }
                yield mapping;
            }
            case TRIANGLE -> new int[]{0, 1, 2};
            case SEGMENT -> new int[]{0, 1};
            case SECTOR -> new int[]{0, 1, 2};
            case EDGE -> new int[]{0, 1};
        };
    }
    
    private String getStringOrDefault(String path, String defaultValue) {
        try {
            String value = state.getString(path);
            return value != null && !value.isEmpty() ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    @Override
    public void tick() {
        // TextFieldWidget handles cursor blink internally
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render widgets
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
        
        // Render custom content
        int x = bounds.x();
        int y = bounds.y() + GuiConstants.WIDGET_HEIGHT + 6;
        
        if (currentTab == Tab.PATTERNS) {
            renderPatternsLabels(context, x, y);
        } else {
            renderExplorerContent(context, x, y);
        }
    }
    
    private void renderPatternsLabels(DrawContext context, int x, int y) {
        context.drawTextWithShadow(textRenderer, "Cell:", x, y + 6, 0xFFAAAAAA);
        y += GuiConstants.WIDGET_HEIGHT + 2;
        context.drawTextWithShadow(textRenderer, "Pattern:", x, y + 6, 0xFFAAAAAA);
        
        if (showPerPartControls) {
            y += GuiConstants.WIDGET_HEIGHT + 4 + GuiConstants.WIDGET_HEIGHT + 2;
            String shapeType = state.getString("shapeType").toLowerCase();
            
            if (shapeType.equals("sphere")) {
                context.drawTextWithShadow(textRenderer, "Poles:", x, y + 6, 0xFF888888);
                y += GuiConstants.WIDGET_HEIGHT + 2;
                context.drawTextWithShadow(textRenderer, "Equator:", x, y + 6, 0xFF888888);
            } else if (shapeType.equals("prism") || shapeType.equals("cylinder")) {
                context.drawTextWithShadow(textRenderer, "Caps:", x, y + 6, 0xFF888888);
                y += GuiConstants.WIDGET_HEIGHT + 2;
                context.drawTextWithShadow(textRenderer, "Sides:", x, y + 6, 0xFF888888);
            }
        }
    }
    
    private void renderExplorerContent(DrawContext context, int x, int y) {
        int w = bounds.width();
        
        // Cell label
        context.drawTextWithShadow(textRenderer, "Cell:", x, y + 6, 0xFFAAAAAA);
        y += GuiConstants.WIDGET_HEIGHT + 8;
        
        // Visual diagram + info panel
        int diagramW = (w - 8) / 2;
        int diagramH = 70;
        int infoX = x + diagramW + 8;
        int infoW = w - diagramW - 8;
        
        // Diagram
        context.fill(x, y, x + diagramW, y + diagramH, 0x44222233);
        context.drawBorder(x, y, diagramW, diagramH, 0xFF444466);
        renderPermutationDiagram(context, x + 4, y + 4, diagramW - 8, diagramH - 8);
        
        // Info panel
        context.fill(infoX, y, infoX + infoW, y + diagramH, 0x44223322);
        context.drawBorder(infoX, y, infoW, diagramH, 0xFF446644);
        
        int infoY = y + 4;
        String mappingStr = formatMapping(currentMapping);
        context.drawTextWithShadow(textRenderer, mappingStr, infoX + 4, infoY, 0xFF88CCFF);
        infoY += 14;
        
        String desc = currentDescription.length() > 20 
            ? currentDescription.substring(0, 18) + "..." 
            : currentDescription;
        context.drawTextWithShadow(textRenderer, desc, infoX + 4, infoY, 0xFFCCCCCC);
        
        // Counter
        y += diagramH + 4;
        String counter = String.format("#%d / %d", currentPermutation + 1, getTotal());
        context.drawCenteredTextWithShadow(textRenderer, counter, x + w / 2, y, 0xFFFFFFAA);
    }
    
    private void renderPermutationDiagram(DrawContext context, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int size = Math.min(w, h) / 2 - 4;
        
        int color = 0xFF00CCFF;
        int lineColor = 0xFF4488AA;
        
        switch (currentCellType) {
            case QUAD -> {
                int[] px = {cx - size, cx + size, cx + size, cx - size};
                int[] py = {cy - size/2, cy - size/2, cy + size/2, cy + size/2};
                
                for (int i = 0; i < 4; i++) {
                    int next = (i + 1) % 4;
                    drawLine(context, px[i], py[i], px[next], py[next], lineColor);
                }
                
                for (int i = 0; i < 4; i++) {
                    context.fill(px[i] - 6, py[i] - 6, px[i] + 6, py[i] + 6, color);
                    context.drawCenteredTextWithShadow(textRenderer, String.valueOf(i), px[i], py[i] - 3, 0xFFFFFFFF);
                }
            }
            case TRIANGLE -> {
                int[] px = {cx, cx - size, cx + size};
                int[] py = {cy - size/2, cy + size/2, cy + size/2};
                
                for (int i = 0; i < 3; i++) {
                    int next = (i + 1) % 3;
                    drawLine(context, px[i], py[i], px[next], py[next], lineColor);
                }
                
                for (int i = 0; i < 3; i++) {
                    context.fill(px[i] - 6, py[i] - 6, px[i] + 6, py[i] + 6, color);
                    context.drawCenteredTextWithShadow(textRenderer, String.valueOf(i), px[i], py[i] - 3, 0xFFFFFFFF);
                }
            }
            default -> {
                context.drawCenteredTextWithShadow(textRenderer, currentCellType.name(), cx, cy - 5, color);
                context.drawCenteredTextWithShadow(textRenderer, formatMapping(currentMapping), cx, cy + 8, 0xFFAAAAAA);
            }
        }
    }
    
    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int) length);
        
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int px = (int) (x1 + dx * t);
            int py = (int) (y1 + dy * t);
            context.fill(px, py, px + 1, py + 1, color);
        }
    }
    
    private String formatMapping(int[] mapping) {
        if (mapping == null || mapping.length == 0) return "—";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mapping.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i).append("→").append(mapping[i]);
        }
        return sb.toString();
    }
    
    public List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() {
        return new ArrayList<>(widgets);
    }
}
