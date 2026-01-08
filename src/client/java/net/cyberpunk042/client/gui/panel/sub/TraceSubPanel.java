package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.panel.AbstractPanel;
import net.cyberpunk042.client.gui.preview.PreviewConfig;
import net.cyberpunk042.client.gui.state.DefinitionBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.PipelineTracer;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.client.gui.widget.ToastNotification;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline Trace sub-panel for debugging the render pipeline.
 * 
 * <p>Allows enabling/disabling pipeline tracing and viewing results.</p>
 */
public class TraceSubPanel extends AbstractPanel {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private ButtonWidget toggleBtn;
    private ButtonWidget dumpBtn;
    private ButtonWidget clearBtn;
    private ButtonWidget dumpStateBtn;
    private ButtonWidget dumpDefinitionBtn;
    private LabeledSlider previewDetailSlider;
    
    private String lastSummary = "No traces yet";
    private List<String> lastResults = new ArrayList<>();
    
    public TraceSubPanel(Screen parent, FieldEditState state) {
        super(parent, state);
    }
    
    @Override
    public void init(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
        widgets.clear();
        
        int y = GuiConstants.PADDING;
        int btnWidth = width - GuiConstants.PADDING * 2;
        int halfWidth = btnWidth / 2 - 2;
        int x = GuiConstants.PADDING;
        
        // Toggle button
        toggleBtn = GuiWidgets.button(x, y, btnWidth, getToggleLabel(),
            "Enable/disable pipeline tracing",
            this::toggleTrace);
        widgets.add(toggleBtn);
        y += 24;
        
        // Dump button
        dumpBtn = GuiWidgets.button(x, y, halfWidth, "📋 Dump to Log",
            "Output all traces to game log",
            this::dumpTraces);
        widgets.add(dumpBtn);
        
        // Clear button  
        clearBtn = GuiWidgets.button(x + halfWidth + 4, y, halfWidth, "🗑 Clear",
            "Clear all recorded traces",
            this::clearTraces);
        widgets.add(clearBtn);
        y += 28;
        
        // === NEW DEBUG BUTTONS ===
        // Dump State JSON button
        dumpStateBtn = GuiWidgets.button(x, y, halfWidth, "📄 State JSON",
            "Dump raw FieldEditState to log",
            this::dumpStateJson);
        widgets.add(dumpStateBtn);
        
        // Dump Definition JSON button
        dumpDefinitionBtn = GuiWidgets.button(x + halfWidth + 4, y, halfWidth, "📦 Def JSON",
            "Dump converted FieldDefinition to log",
            this::dumpDefinitionJson);
        widgets.add(dumpDefinitionBtn);
        y += 28;
        
        // === PREVIEW SETTINGS ===
        // Preview Detail slider
        previewDetailSlider = LabeledSlider.builder("Preview Detail")
            .position(x, y)
            .width(btnWidth)
            .range(PreviewConfig.MIN_DETAIL, PreviewConfig.MAX_DETAIL)
            .initial(PreviewConfig.getDetail())
            .format("%d")
            .step(1f)
            .onChange(v -> PreviewConfig.setDetail(v.intValue()))
            .build();
        widgets.add(previewDetailSlider);
        y += 24;
    }
    
    private String getToggleLabel() {
        return PipelineTracer.isEnabled() 
            ? "§a⚡ TRACING ON §7- values are being recorded"
            : "§7⚡ Tracing OFF §8- click to enable";
    }
    
    private void toggleTrace() {
        if (PipelineTracer.isEnabled()) {
            PipelineTracer.disable();
            ToastNotification.info("Pipeline tracing disabled");
        } else {
            PipelineTracer.clear();
            PipelineTracer.enable();
            ToastNotification.success("Pipeline tracing ENABLED - change values in GUI");
        }
        toggleBtn.setMessage(Text.literal(getToggleLabel()));
    }
    
    private void dumpTraces() {
        PipelineTracer.dump();
        lastSummary = PipelineTracer.summary();
        ToastNotification.info("Traces dumped to log: " + lastSummary);
        Logging.GUI.topic("trace").info("=== PIPELINE TRACE SUMMARY ===");
        Logging.GUI.topic("trace").info(lastSummary);
    }
    
    private void clearTraces() {
        PipelineTracer.clear();
        lastSummary = "Traces cleared";
        lastResults.clear();
        ToastNotification.info("Traces cleared");
    }
    
    /**
     * Dumps the raw FieldEditState as JSON to the log.
     * This shows the "before" state - what the GUI has set.
     */
    private void dumpStateJson() {
        try {
            JsonObject json = state.toJson(); // Uses SerializationManager - includes adapters + layers
            String pretty = GSON.toJson(json);
            
            Logging.GUI.topic("debug").info("=== RAW FIELD EDIT STATE ===\n" +
                "Fill mode: " + state.fill().mode().name() + "\n" +
                "Fill config: " + state.fill() + "\n" +
                "--- Full State JSON ---\n" + pretty);
            
            ToastNotification.success("State JSON dumped to log");
        } catch (Exception e) {
            Logging.GUI.topic("debug").error("Failed to dump state JSON", e);
            ToastNotification.error("Failed: " + e.getMessage());
        }
    }
    
    /**
     * Dumps the converted FieldDefinition as JSON to the log.
     * This shows the "after" state - what gets rendered.
     */
    private void dumpDefinitionJson() {
        try {
            FieldDefinition def = DefinitionBuilder.fromState(state);
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== CONVERTED FIELD DEFINITION ===\n");
            sb.append("Definition ID: ").append(def.id()).append("\n");
            sb.append("Layer count: ").append(def.layers() != null ? def.layers().size() : 0).append("\n");
            
            // Dump each layer and primitive with fill info
            if (def.layers() != null) {
                for (int i = 0; i < def.layers().size(); i++) {
                    var layer = def.layers().get(i);
                    sb.append("  Layer[").append(i).append("]: ").append(layer.id())
                      .append(" (primitives=").append(layer.primitives() != null ? layer.primitives().size() : 0).append(")\n");
                    
                    if (layer.primitives() != null) {
                        for (int j = 0; j < layer.primitives().size(); j++) {
                            var prim = layer.primitives().get(j);
                            var fill = prim.fill();
                            sb.append("    Primitive[").append(j).append("]: id=").append(prim.id())
                              .append(", type=").append(prim.type()).append("\n");
                            sb.append("    → fill.mode=").append(fill != null ? fill.mode().name() : "NULL")
                              .append(", wireThickness=").append(fill != null ? fill.wireThickness() : "N/A")
                              .append(", doubleSided=").append(fill != null ? fill.doubleSided() : "N/A").append("\n");
                            sb.append("    → shape=").append(prim.shape() != null ? prim.shape().getClass().getSimpleName() : "NULL").append("\n");
                        }
                    }
                }
            }
            
            // Also dump full JSON
            JsonObject json = def.toJson();
            String pretty = GSON.toJson(json);
            sb.append("--- Full Definition JSON ---\n").append(pretty);
            
            Logging.GUI.topic("debug").info(sb.toString());
            
            ToastNotification.success("Definition JSON dumped to log");
        } catch (Exception e) {
            Logging.GUI.topic("debug").error("Failed to dump definition JSON", e);
            ToastNotification.error("Failed: " + e.getMessage());
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        var tr = MinecraftClient.getInstance().textRenderer;
        int y = GuiConstants.PADDING;
        
        // Render all widgets (buttons and sliders)
        for (var widget : widgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
        
        // Status area - adjusted for new buttons + slider
        y = 116;
        
        // Tracing status
        String status = PipelineTracer.isEnabled() ? "§a● RECORDING" : "§7○ Idle";
        context.drawTextWithShadow(tr, Text.literal("Status: " + status), 
            GuiConstants.PADDING, y, 0xFFFFFF);
        y += 12;
        
        // Summary
        context.drawTextWithShadow(tr, Text.literal("§7" + lastSummary), 
            GuiConstants.PADDING, y, 0xAAAAAA);
        y += 16;
        
        // Instructions
        context.drawTextWithShadow(tr, Text.literal("§8─────────────────────"), 
            GuiConstants.PADDING, y, 0x444444);
        y += 12;
        
        context.drawTextWithShadow(tr, Text.literal("§7Debug Buttons:"), 
            GuiConstants.PADDING, y, 0xAAAAAA);
        y += 11;
        context.drawTextWithShadow(tr, Text.literal("§8• State JSON = raw GUI state"), 
            GuiConstants.PADDING, y, 0x888888);
        y += 10;
        context.drawTextWithShadow(tr, Text.literal("§8• Def JSON = converted for render"), 
            GuiConstants.PADDING, y, 0x888888);
        y += 14;
        
        // Fill mode quick display
        context.drawTextWithShadow(tr, Text.literal("§7Current fill: §f" + state.fill().mode().name()), 
            GuiConstants.PADDING, y, 0xAAAAAA);
    }
    
    @Override
    public void tick() {
        // Update toggle button label in case state changed externally
        if (toggleBtn != null) {
            toggleBtn.setMessage(Text.literal(getToggleLabel()));
        }
    }
    
    @Override
    public int getHeight() {
        return 250; // Increased for new slider
    }
}
