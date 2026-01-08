package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.adapter.MagicCircleConfig;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.visual.shader.MagicCirclePostEffect;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Magic Circle ground effect controls panel.
 * 
 * <p>Phase 1 UI with basic global controls. Future phases will add
 * per-layer customization.</p>
 */
public class MagicCircleSubPanel extends BoundPanel {
    
    private final int startY;
    
    public MagicCircleSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("MagicCircleSubPanel created");
    }
    
    @Override
    protected void buildContent() {
        // Sync adapter config to PostEffect when panel opens
        state.magicCircleAdapter().syncToPostEffect();
        
        ContentBuilder content = content(startY);
        
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;
        int y;
        
        // ═══════════════════════════════════════════════════════════════════════
        // ENABLE & TRIGGER
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Magic Circle");
        
        y = content.getCurrentY();
        
        // Enable toggle
        boolean enabled = (Boolean) state.get("magicCircle.enabled");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggle(x, y, halfW, 
            enabled ? "§aEnabled" : "Disabled",
            enabled, "Toggle magic circle rendering",
            v -> { state.set("magicCircle.enabled", v); }));
        
        // Follow player toggle
        boolean follow = (Boolean) state.get("magicCircle.followPlayer");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggle(x + halfW + GuiConstants.COMPACT_GAP, y, halfW,
            "Follow Player",
            follow, "Magic circle follows player position",
            v -> { state.set("magicCircle.followPlayer", v); }));
        content.advanceBy(22);
        
        // Spawn buttons
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("⚡ Spawn Here"), btn -> {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                var pos = client.player.getPos();
                state.set("magicCircle.centerX", (float) pos.x);
                state.set("magicCircle.centerY", (float) pos.y);
                state.set("magicCircle.centerZ", (float) pos.z);
                state.set("magicCircle.followPlayer", false);
                state.set("magicCircle.enabled", true);
                rebuildContent();
                notifyWidgetsChanged();
                
                net.cyberpunk042.client.gui.widget.ToastNotification.success(
                    String.format("Magic Circle @ %.0f, %.0f, %.0f", pos.x, pos.y, pos.z));
            }
        }).dimensions(x, y, halfW, 20).build());
        
        widgets.add(ButtonWidget.builder(Text.literal("🎯 At Cursor"), btn -> {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.player != null && client.world != null) {
                double maxDistance = 256.0;
                var cameraEntity = client.getCameraEntity();
                if (cameraEntity == null) cameraEntity = client.player;
                
                var start = cameraEntity.getCameraPosVec(1.0f);
                var look = cameraEntity.getRotationVec(1.0f);
                var end = start.add(look.multiply(maxDistance));
                
                var hit = client.world.raycast(new net.minecraft.world.RaycastContext(
                    start, end,
                    net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    cameraEntity
                ));
                
                if (hit != null && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                    var pos = hit.getPos();
                    state.set("magicCircle.centerX", (float) pos.x);
                    state.set("magicCircle.centerY", (float) pos.y);
                    state.set("magicCircle.centerZ", (float) pos.z);
                    state.set("magicCircle.followPlayer", false);
                    state.set("magicCircle.enabled", true);
                    rebuildContent();
                    notifyWidgetsChanged();
                    
                    net.cyberpunk042.client.gui.widget.ToastNotification.success(
                        String.format("Magic Circle @ %.0f, %.0f, %.0f", pos.x, pos.y, pos.z));
                }
            }
        }).dimensions(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 20).build());
        content.advanceBy(22);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // GEOMETRY
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Geometry");
        
        content.slider("Radius", "magicCircle.effectRadius").range(1f, 100f).format("%.1f").add();
        content.slider("Height Tol", "magicCircle.heightTolerance").range(0.1f, 10f).format("%.1f").add();
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // APPEARANCE
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Appearance");
        
        content.slider("Intensity", "magicCircle.intensity").range(0.1f, 5f).format("%.2f").add();
        content.slider("Glow Exp", "magicCircle.glowExponent").range(1f, 5f).format("%.1f").add();
        
        // Color RGB
        content.sliderTriple(
            "R", "magicCircle.primaryR", 0f, 1f,
            "G", "magicCircle.primaryG", 0f, 1f,
            "B", "magicCircle.primaryB", 0f, 1f
        );
        
        // Color presets
        y = content.getCurrentY();
        int thirdW = (w - GuiConstants.COMPACT_GAP * 2) / 3;
        
        widgets.add(ButtonWidget.builder(Text.literal("§eGold"), btn -> {
            state.set("magicCircle.primaryR", 1.0f);
            state.set("magicCircle.primaryG", 0.85f);
            state.set("magicCircle.primaryB", 0.3f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, thirdW, 20).build());
        
        widgets.add(ButtonWidget.builder(Text.literal("§bCyan"), btn -> {
            state.set("magicCircle.primaryR", 0.3f);
            state.set("magicCircle.primaryG", 0.9f);
            state.set("magicCircle.primaryB", 1.0f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x + thirdW + GuiConstants.COMPACT_GAP, y, thirdW, 20).build());
        
        widgets.add(ButtonWidget.builder(Text.literal("§dPurple"), btn -> {
            state.set("magicCircle.primaryR", 0.8f);
            state.set("magicCircle.primaryG", 0.3f);
            state.set("magicCircle.primaryB", 1.0f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x + (thirdW + GuiConstants.COMPACT_GAP) * 2, y, thirdW, 20).build());
        content.advanceBy(22);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // ANIMATION
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Animation");
        
        // Row 1: Rotation speed, Breathing speed, Breathing amount (3 cols)
        content.sliderTriple(
            "Speed", "magicCircle.rotationSpeed", 0f, 3f,
            "Breath", "magicCircle.breathingSpeed", 0f, 3f,
            "Amt", "magicCircle.breathingAmount", 0f, 0.1f
        );
        
        // Row 2: Animation Stage (half width) + Direction toggle (half width)
        y = content.getCurrentY();
        // Reuse halfW from top of method
        
        // Stage slider (left half)
        float stage = state.getFloat("magicCircle.animationStage");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.sliderCompact(
            x, y, halfW,
            "Stage:" + String.format("%.1f", stage),
            stage, 0f, 8f,
            "Animation stage (0-8)",
            v -> { state.set("magicCircle.animationStage", v.floatValue()); }
        ));
        
        // Direction toggle (right half) - "From Center" checkbox
        boolean fromCenter = (Boolean) state.get("magicCircle.animationFromCenter");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggleCompact(
            x + halfW + GuiConstants.COMPACT_GAP, y, halfW,
            fromCenter ? "↔ From Center" : "↔ From Outside",
            fromCenter,
            "Animation direction: center outward or outside inward",
            v -> { state.set("magicCircle.animationFromCenter", v); }
        ));
        content.advanceBy(20);
        
        // Row 3: Stage speed, Mode, Animate On Spawn toggle
        y = content.getCurrentY();
        int stageThirdW = (w - GuiConstants.COMPACT_GAP * 2) / 3;
        
        // Stage Speed slider
        float stageSpeed = state.getFloat("magicCircle.stageSpeed");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.sliderCompact(
            x, y, stageThirdW,
            "Spd:" + String.format("%.1f", stageSpeed),
            stageSpeed, 0.1f, 2f,
            "Seconds per stage",
            v -> { state.set("magicCircle.stageSpeed", v.floatValue()); }
        ));
        
        // Transition Mode dropdown
        int mode = (Integer) state.get("magicCircle.transitionMode");
        String[] modeNames = {"Instant", "Fade", "Scale", "Both"};
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<Integer>builder(
                v -> net.minecraft.text.Text.literal(modeNames[v]))
            .values(0, 1, 2, 3)
            .initially(mode)
            .omitKeyText()
            .build(x + stageThirdW + GuiConstants.COMPACT_GAP, y, stageThirdW, 16, 
                net.minecraft.text.Text.literal(""),
                (btn, v) -> { state.set("magicCircle.transitionMode", v); }));
        
        // Animate On Spawn toggle
        boolean animOnSpawn = (Boolean) state.get("magicCircle.animateOnSpawn");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggleCompact(
            x + (stageThirdW + GuiConstants.COMPACT_GAP) * 2, y, stageThirdW,
            "Animate",
            animOnSpawn,
            "Animate on spawn/despawn",
            v -> { state.set("magicCircle.animateOnSpawn", v); }
        ));
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER CONTROLS (Phase 2)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer Controls");
        
        // Each layer: [Toggle] [Intensity Slider] [Speed Slider]
        // This creates a 3-column layout per layer row
        for (int i = 0; i < MagicCircleConfig.LAYER_COUNT; i++) {
            final int layerIdx = i;
            final int layerNum = i + 1;
            String name = MagicCircleConfig.LAYER_NAMES[i];
            
            y = content.getCurrentY();
            
            // Toggle button (1/4 width)
            int toggleW = w / 4;
            boolean layerEnabled = (Boolean) state.get("magicCircle.layer" + layerNum + "Enable");
            widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggleCompact(
                x, y, toggleW - 2, 
                name,
                layerEnabled,
                "Toggle " + name,
                v -> { 
                    state.set("magicCircle.layer" + layerNum + "Enable", v); 
                }
            ));
            
            // Two compact sliders for intensity and speed (remaining 3/4 width)
            int sliderSpace = w - toggleW;
            int sliderW = (sliderSpace - GuiConstants.COMPACT_GAP * 2) / 2;
            int sliderX = x + toggleW + GuiConstants.COMPACT_GAP;
            
            // Intensity slider
            float intensityVal = state.getFloat("magicCircle.layer" + layerNum + "Intensity");
            widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.sliderCompact(
                sliderX, y, sliderW, 
                "I:" + String.format("%.1f", intensityVal),
                intensityVal, 0f, 2f,
                "Layer intensity (0-2)",
                v -> { state.set("magicCircle.layer" + layerNum + "Intensity", v.floatValue()); }
            ));
            
            // Speed slider
            sliderX += sliderW + GuiConstants.COMPACT_GAP;
            float speedVal = state.getFloat("magicCircle.layer" + layerNum + "Speed");
            widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.sliderCompact(
                sliderX, y, sliderW,
                "S:" + String.format("%.1f", speedVal),
                speedVal, -2f, 2f,
                "Rotation speed (-2 to 2)",
                v -> { state.set("magicCircle.layer" + layerNum + "Speed", v.floatValue()); }
            ));
            
            content.advanceBy(22);
        }
        
        // Quick layer actions
        content.advanceBy(4);
        y = content.getCurrentY();
        int qtrW = (w - GuiConstants.COMPACT_GAP * 3) / 4;
        
        widgets.add(ButtonWidget.builder(Text.literal("All On"), btn -> {
            for (int i = 1; i <= 8; i++) state.set("magicCircle.layer" + i + "Enable", true);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, qtrW, 16).build());
        
        widgets.add(ButtonWidget.builder(Text.literal("All Off"), btn -> {
            for (int i = 1; i <= 8; i++) state.set("magicCircle.layer" + i + "Enable", false);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x + qtrW + GuiConstants.COMPACT_GAP, y, qtrW, 16).build());
        
        widgets.add(ButtonWidget.builder(Text.literal("Reset I"), btn -> {
            for (int i = 1; i <= 8; i++) state.set("magicCircle.layer" + i + "Intensity", 1.0f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x + (qtrW + GuiConstants.COMPACT_GAP) * 2, y, qtrW, 16).build());
        
        widgets.add(ButtonWidget.builder(Text.literal("Reset S"), btn -> {
            // Reset to original rotation directions
            float[] defaultSpeeds = { 1f, 1f, 1f, 0f, -1f, -1f, 1f, -1f };
            for (int i = 0; i < 8; i++) state.set("magicCircle.layer" + (i+1) + "Speed", defaultSpeeds[i]);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x + (qtrW + GuiConstants.COMPACT_GAP) * 3, y, qtrW, 16).build());
        
        content.advanceBy(20);
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER GEOMETRY (Phase 3A)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer 4: Middle Ring");
        
        // Two sliders per row: Inner/Outer radius
        content.sliderPair(
            "Inner R", "magicCircle.layer4InnerRadius", 0f, 1f,
            "Outer R", "magicCircle.layer4OuterRadius", 0f, 1f
        );
        
        // Thickness and rotation offset
        content.sliderPair(
            "Thickness", "magicCircle.layer4Thickness", 0.0005f, 0.02f,
            "Rot Offset", "magicCircle.layer4RotOffset", 0f, 6.28f
        );
        
        // Reset button for Layer 4
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("Reset L4 Defaults"), btn -> {
            state.set("magicCircle.layer4InnerRadius", 0.5f);
            state.set("magicCircle.layer4OuterRadius", 0.55f);
            state.set("magicCircle.layer4Thickness", 0.002f);
            state.set("magicCircle.layer4RotOffset", 0.0f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, w, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        content.sectionHeader("Layer 7: Inner Radiation");
        
        // Two sliders per row: Inner/Outer radius
        content.sliderPair(
            "Inner R", "magicCircle.layer7InnerRadius", 0f, 1f,
            "Outer R", "magicCircle.layer7OuterRadius", 0f, 1f
        );
        
        // Spoke count (as slider, will treat as int) and thickness
        content.sliderPair(
            "Spokes", "magicCircle.layer7SpokeCount", 3f, 72f,
            "Thickness", "magicCircle.layer7Thickness", 0.0005f, 0.02f
        );
        
        // Rotation offset
        content.slider("Rot Offset", "magicCircle.layer7RotOffset").range(0f, 6.28f).format("%.2f").add();
        
        // Reset button for Layer 7
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("Reset L7 Defaults"), btn -> {
            state.set("magicCircle.layer7InnerRadius", 0.25f);
            state.set("magicCircle.layer7OuterRadius", 0.3f);
            state.set("magicCircle.layer7SpokeCount", 12);
            state.set("magicCircle.layer7Thickness", 0.005f);
            state.set("magicCircle.layer7RotOffset", 0.0f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, w, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER 2 GEOMETRY (Phase 3B) - Hexagram
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer 2: Hexagram");
        
        // Rect count and size
        content.sliderPair(
            "Rects", "magicCircle.layer2RectCount", 3f, 12f,
            "Size", "magicCircle.layer2RectSize", 0.1f, 1f
        );
        
        // Thickness and rotation offset
        content.sliderPair(
            "Thickness", "magicCircle.layer2Thickness", 0.0005f, 0.01f,
            "Rot Offset", "magicCircle.layer2RotOffset", 0f, 6.28f
        );
        
        // Snap rotation toggle + reset button
        y = content.getCurrentY();
        boolean snap2 = (Boolean) state.get("magicCircle.layer2SnapRotation");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggleCompact(
            x, y, w / 2 - 2,
            "Snap Rot",
            snap2,
            "Use angular snapping",
            v -> { state.set("magicCircle.layer2SnapRotation", v); }
        ));
        widgets.add(ButtonWidget.builder(Text.literal("Reset L2"), btn -> {
            state.set("magicCircle.layer2RectCount", 6);
            state.set("magicCircle.layer2RectSize", 0.601f);
            state.set("magicCircle.layer2Thickness", 0.0015f);
            state.set("magicCircle.layer2RotOffset", 0.0f);
            state.set("magicCircle.layer2SnapRotation", true);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x + w / 2 + 2, y, w / 2 - 2, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER 5 GEOMETRY (Phase 3B) - Inner Triangle
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer 5: Inner Triangle");
        
        // Rect count and size
        content.sliderPair(
            "Rects", "magicCircle.layer5RectCount", 3f, 12f,
            "Size", "magicCircle.layer5RectSize", 0.1f, 1f
        );
        
        // Thickness and rotation offset
        content.sliderPair(
            "Thickness", "magicCircle.layer5Thickness", 0.0005f, 0.01f,
            "Rot Offset", "magicCircle.layer5RotOffset", 0f, 6.28f
        );
        
        // Snap rotation toggle + reset button
        y = content.getCurrentY();
        boolean snap5 = (Boolean) state.get("magicCircle.layer5SnapRotation");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggleCompact(
            x, y, w / 2 - 2,
            "Snap Rot",
            snap5,
            "Use angular snapping",
            v -> { state.set("magicCircle.layer5SnapRotation", v); }
        ));
        widgets.add(ButtonWidget.builder(Text.literal("Reset L5"), btn -> {
            state.set("magicCircle.layer5RectCount", 3);
            state.set("magicCircle.layer5RectSize", 0.36f);
            state.set("magicCircle.layer5Thickness", 0.0015f);
            state.set("magicCircle.layer5RotOffset", 0.0f);
            state.set("magicCircle.layer5SnapRotation", true);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x + w / 2 + 2, y, w / 2 - 2, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER 3 GEOMETRY (Phase 3C) - Outer Dot Ring
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer 3: Outer Dots");
        
        // Dot count and orbit radius
        content.sliderPair(
            "Dots", "magicCircle.layer3DotCount", 4f, 36f,
            "Orbit R", "magicCircle.layer3OrbitRadius", 0.1f, 1f
        );
        
        // Ring geometry
        content.sliderPair(
            "Ring In", "magicCircle.layer3RingInner", 0f, 0.1f,
            "Ring Out", "magicCircle.layer3RingOuter", 0.01f, 0.2f
        );
        
        // Ring + Dot thickness
        content.sliderPair(
            "Ring Thk", "magicCircle.layer3RingThickness", 0.001f, 0.02f,
            "Dot Thk", "magicCircle.layer3DotThickness", 0.001f, 0.03f
        );
        
        // Dot radius and rotation offset
        content.sliderPair(
            "Dot R", "magicCircle.layer3DotRadius", 0f, 0.05f,
            "Rot Off", "magicCircle.layer3RotOffset", 0f, 6.28f
        );
        
        // Reset button for Layer 3
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("Reset L3 Defaults"), btn -> {
            state.set("magicCircle.layer3DotCount", 12);
            state.set("magicCircle.layer3OrbitRadius", 0.875f);
            state.set("magicCircle.layer3RingInner", 0.001f);
            state.set("magicCircle.layer3RingOuter", 0.05f);
            state.set("magicCircle.layer3RingThickness", 0.004f);
            state.set("magicCircle.layer3DotRadius", 0.001f);
            state.set("magicCircle.layer3DotThickness", 0.008f);
            state.set("magicCircle.layer3RotOffset", 0.262f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, w, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER 6 GEOMETRY (Phase 3C) - Inner Dot Ring
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer 6: Inner Dots");
        
        // Dot count and orbit radius
        content.sliderPair(
            "Dots", "magicCircle.layer6DotCount", 4f, 36f,
            "Orbit R", "magicCircle.layer6OrbitRadius", 0.1f, 1f
        );
        
        // Ring geometry
        content.sliderPair(
            "Ring In", "magicCircle.layer6RingInner", 0f, 0.1f,
            "Ring Out", "magicCircle.layer6RingOuter", 0.01f, 0.2f
        );
        
        // Ring + Dot thickness
        content.sliderPair(
            "Ring Thk", "magicCircle.layer6RingThickness", 0.001f, 0.02f,
            "Dot Thk", "magicCircle.layer6DotThickness", 0.0005f, 0.03f
        );
        
        // Dot radius and rotation offset
        content.sliderPair(
            "Dot R", "magicCircle.layer6DotRadius", 0f, 0.05f,
            "Rot Off", "magicCircle.layer6RotOffset", 0f, 6.28f
        );
        
        // Reset button for Layer 6
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("Reset L6 Defaults"), btn -> {
            state.set("magicCircle.layer6DotCount", 12);
            state.set("magicCircle.layer6OrbitRadius", 0.53f);
            state.set("magicCircle.layer6RingInner", 0.001f);
            state.set("magicCircle.layer6RingOuter", 0.035f);
            state.set("magicCircle.layer6RingThickness", 0.004f);
            state.set("magicCircle.layer6DotRadius", 0.001f);
            state.set("magicCircle.layer6DotThickness", 0.001f);
            state.set("magicCircle.layer6RotOffset", 0.262f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, w, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER 1 GEOMETRY (Phase 3D) - Outer Ring + Radiation
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer 1: Outer Ring");
        
        // Ring geometry
        content.sliderPair(
            "Ring In", "magicCircle.layer1RingInner", 0f, 1f,
            "Ring Out", "magicCircle.layer1RingOuter", 0f, 1f
        );
        
        // Ring thickness + Radiation inner
        content.sliderPair(
            "Ring Thk", "magicCircle.layer1RingThickness", 0.001f, 0.05f,
            "Rad In", "magicCircle.layer1RadInner", 0f, 1f
        );
        
        // Radiation outer + count
        content.sliderPair(
            "Rad Out", "magicCircle.layer1RadOuter", 0f, 1f,
            "Rad Cnt", "magicCircle.layer1RadCount", 3f, 72f
        );
        
        // Radiation thickness + rotation offset
        content.sliderPair(
            "Rad Thk", "magicCircle.layer1RadThickness", 0.0001f, 0.01f,
            "Rot Off", "magicCircle.layer1RotOffset", 0f, 6.28f
        );
        
        // Reset button for Layer 1
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("Reset L1 Defaults"), btn -> {
            state.set("magicCircle.layer1RingInner", 0.85f);
            state.set("magicCircle.layer1RingOuter", 0.9f);
            state.set("magicCircle.layer1RingThickness", 0.006f);
            state.set("magicCircle.layer1RadInner", 0.87f);
            state.set("magicCircle.layer1RadOuter", 0.88f);
            state.set("magicCircle.layer1RadCount", 36);
            state.set("magicCircle.layer1RadThickness", 0.0008f);
            state.set("magicCircle.layer1RotOffset", 0.0f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, w, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // LAYER 8 GEOMETRY (Phase 3D) - Spinning Core
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Layer 8: Core");
        
        // Breathing
        content.sliderPair(
            "Breath", "magicCircle.layer8BreathAmp", 0f, 0.2f,
            "Center", "magicCircle.layer8BreathCenter", 0.8f, 1.5f
        );
        
        // Orbital count + start radius
        content.sliderPair(
            "Orb Cnt", "magicCircle.layer8OrbitalCount", 1f, 12f,
            "Orb Start", "magicCircle.layer8OrbitalStart", 0.05f, 0.3f
        );
        
        // Orbital step + distance
        content.sliderPair(
            "Orb Step", "magicCircle.layer8OrbitalStep", 0f, 0.05f,
            "Orb Dist", "magicCircle.layer8OrbitalDist", 0f, 0.3f
        );
        
        // Orbital + center thickness
        content.sliderPair(
            "Orb Thk", "magicCircle.layer8OrbitalThickness", 0.0005f, 0.01f,
            "Ctr Thk", "magicCircle.layer8CenterThickness", 0.001f, 0.02f
        );
        
        // Center radius + rotation offset
        content.sliderPair(
            "Ctr R", "magicCircle.layer8CenterRadius", 0.01f, 0.1f,
            "Rot Off", "magicCircle.layer8RotOffset", 0f, 6.28f
        );
        
        // Reset button for Layer 8
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("Reset L8 Defaults"), btn -> {
            state.set("magicCircle.layer8BreathAmp", 0.04f);
            state.set("magicCircle.layer8BreathCenter", 1.1f);
            state.set("magicCircle.layer8OrbitalCount", 6);
            state.set("magicCircle.layer8OrbitalStart", 0.13f);
            state.set("magicCircle.layer8OrbitalStep", 0.01f);
            state.set("magicCircle.layer8OrbitalDist", 0.1f);
            state.set("magicCircle.layer8OrbitalThickness", 0.002f);
            state.set("magicCircle.layer8CenterRadius", 0.04f);
            state.set("magicCircle.layer8CenterThickness", 0.004f);
            state.set("magicCircle.layer8RotOffset", 0.0f);
            rebuildContent();
            notifyWidgetsChanged();
        }).dimensions(x, y, w, 16).build());
        content.advanceBy(20);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // STATUS
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Status");
        content.advanceBy(24);
        
        contentHeight = content.getContentHeight();
        Logging.GUI.topic("panel").debug("MagicCircleSubPanel built: {} widgets", widgets.size());
    }
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
        
        // Draw status text
        String status = MagicCirclePostEffect.getStatusString();
        int statusY = bounds.y() + contentHeight - 16 - scrollOffset;
        if (statusY > bounds.y() && statusY < bounds.bottom()) {
            context.drawTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                Text.literal("§7" + status), bounds.x() + GuiConstants.PADDING, statusY, 0xFFFFFF);
        }
    }
    
    public int getHeight() { return contentHeight; }
}
