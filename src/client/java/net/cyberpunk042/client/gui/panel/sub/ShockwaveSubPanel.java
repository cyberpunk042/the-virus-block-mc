package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.adapter.ShockwaveConfig;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.visual.shader.ShockwavePostEffect;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.ShapeType;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.OriginMode;
import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.EasingType;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Shockwave visual effect controls panel.
 * 
 * <p>Complete UI for all 59 shockwave parameters organized into sections.
 * All controls are bound to shockwave.* paths via the ShockwaveAdapter,
 * which handles syncing to ShockwavePostEffect for live preview.</p>
 */
public class ShockwaveSubPanel extends BoundPanel {
    
    private final int startY;
    
    public ShockwaveSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("ShockwaveSubPanel created");
    }
    
    @Override
    protected void buildContent() {
        // CRITICAL: Sync adapter config to PostEffect when panel opens.
        // This ensures the visual effect matches what the GUI shows (defaults).
        // Without this, the GUI shows "Orbital" but PostEffect might still have old values.
        state.shockwaveAdapter().syncToPostEffect();
        
        ContentBuilder content = content(startY);
        
        // Used by trigger buttons
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.COMPACT_GAP) / 2;
        int y;
        
        // ═══════════════════════════════════════════════════════════════════════
        // CONFIGURATION: Preset + Source (combined row)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Configuration");
        
        // Get shockwave fragments from FragmentRegistry
        java.util.List<String> fragments = net.cyberpunk042.client.gui.util.FragmentRegistry.listShockwaveFragments();
        
        // Source primitive selector - build list of available primitives
        java.util.List<String> primitiveOptions = new java.util.ArrayList<>();
        primitiveOptions.add("None");
        
        var fieldLayers = state.getFieldLayers();
        if (fieldLayers != null) {
            for (int li = 0; li < fieldLayers.size(); li++) {
                var layer = fieldLayers.get(li);
                var prims = layer.primitives();
                for (int pi = 0; pi < prims.size(); pi++) {
                    var prim = prims.get(pi);
                    String label = String.format("L%d.P%d:%s", li, pi, prim.type().toString().substring(0, Math.min(4, prim.type().toString().length())));
                    primitiveOptions.add(label);
                }
            }
        }
        
        String currentRef = state.shockwaveAdapter().shapeSourceRef();
        int initialIndex = 0;
        if (currentRef != null) {
            for (int i = 1; i < primitiveOptions.size(); i++) {
                String opt = primitiveOptions.get(i);
                if (opt.startsWith("L" + currentRef.replace(".", ".P"))) {
                    initialIndex = i;
                    break;
                }
            }
        }
        
        // Row 1: Preset + Source + ShapeType (3 columns, dynamic)
        final int fInitialIndex = initialIndex;
        final java.util.List<String> fOptions = primitiveOptions;
        final java.util.List<net.cyberpunk042.field.FieldLayer> fLayers = fieldLayers;
        
        int thirdW = (w - GuiConstants.COMPACT_GAP * 2) / 3;
        int x2 = x + thirdW + GuiConstants.COMPACT_GAP;
        int x3 = x + (thirdW + GuiConstants.COMPACT_GAP) * 2;
        y = content.getCurrentY();
        
        // Column 1: Preset (if available) or "No Presets" info toggle
        if (fragments.size() > 2) {
            // Get the current preset name from adapter (persists across rebuilds)
            String currentPreset = (String) state.get("shockwave.currentPresetName");
            if (currentPreset == null || !fragments.contains(currentPreset)) {
                currentPreset = "Default";
            }
            final String fCurrentPreset = currentPreset;
            
            widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
                name -> Text.literal(name))
                .values(fragments)
                .initially(fCurrentPreset)
                .build(x, y, thirdW, 20, Text.literal("Preset"), (btn, value) -> {
                    // Store the selected preset name FIRST
                    state.set("shockwave.currentPresetName", value);
                    
                    if (!"Default".equals(value) && !"Custom".equals(value)) {
                        net.cyberpunk042.client.gui.util.FragmentRegistry.applyShockwaveFragment(state, value);
                        state.markDirty();
                    }
                    rebuildContent();
                    notifyWidgetsChanged();
                }));
        } else {
            // No presets - show disabled info button
            widgets.add(ButtonWidget.builder(Text.literal("§7No Preset"), btn -> {})
                .dimensions(x, y, thirdW, 20).build());
        }
        
        // Column 2: Source selector
        widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<String>builder(
            v -> Text.literal(v))
            .values(fOptions)
            .initially(fOptions.get(fInitialIndex))
            .build(x2, y, thirdW, 20, Text.literal("Src"), (btn, value) -> {
                if (value.equals("None")) {
                    state.set("shockwave.shapeSourceRef", null);
                } else {
                    String prefix = value.split(":")[0].trim();
                    int layerIdx = Integer.parseInt(prefix.substring(1, prefix.indexOf('.')));
                    int primIdx = Integer.parseInt(prefix.substring(prefix.indexOf('P') + 1));
                    state.set("shockwave.shapeSourceRef", layerIdx + "." + primIdx);
                    
                    if (fLayers != null && layerIdx < fLayers.size()) {
                        var layer = fLayers.get(layerIdx);
                        if (primIdx < layer.primitives().size()) {
                            state.shockwaveAdapter().syncFromPrimitive(layer.primitives().get(primIdx));
                        }
                    }
                }
                rebuildContent();
                notifyWidgetsChanged();
            }));
        
        // Column 3: ShapeType (disabled when source primitive is linked)
        boolean hasSourcePrimitive = state.shockwaveAdapter().shapeSourceRef() != null;
        if (hasSourcePrimitive) {
            // Show disabled button when primitive is linked (shape comes from primitive)
            widgets.add(ButtonWidget.builder(Text.literal("§7Linked"), btn -> {})
                .dimensions(x3, y, thirdW, 20).build());
        } else {
            // Active shape selector when no primitive linked
            ShapeType currentType = (ShapeType) state.get("shockwave.shapeType");
            widgets.add(net.minecraft.client.gui.widget.CyclingButtonWidget.<ShapeType>builder(
                t -> Text.literal(t.name()))
                .values(ShapeType.values())
                .initially(currentType != null ? currentType : ShapeType.SPHERE)
                .build(x3, y, thirdW, 20, Text.literal("Shape"), (btn, v) -> {
                    state.set("shockwave.shapeType", v);
                    rebuildContent();  // Refresh UI to show/hide polygon sides slider
                    notifyWidgetsChanged();
                }));
        }
        content.advanceBy(22);
        
        // Row 2: Scale + Follow (2 columns)
        // hasSourcePrimitive already declared above
        y = content.getCurrentY();
        
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.slider(x, y, halfW,
            "Scale", 0.0f, 2.0f, (Float)state.get("shockwave.globalScale"), "%.2fx", "Global effect scale",
            v -> state.set("shockwave.globalScale", v)));
        
        if (hasSourcePrimitive) {
            if (!((Boolean) state.get("shockwave.followPosition"))) {
                state.set("shockwave.followPosition", true);
            }
            widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggle(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, "§aFollow",
                true, "Linked to primitive", v -> {}));
        } else {
            boolean follow = (Boolean) state.get("shockwave.followPosition");
            widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggle(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, "Follow",
                follow, "Follow position updates",
                v -> state.set("shockwave.followPosition", v)));
        }
        content.advanceBy(22);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // TRIGGER BUTTONS
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Trigger");
        
        y = content.getCurrentY();
        widgets.add(ButtonWidget.builder(Text.literal("⚡ Trigger"), btn -> {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                // If source primitive is linked, use its position
                String sourceRef = state.shockwaveAdapter().shapeSourceRef();
                if (sourceRef != null && fLayers != null) {
                    // Parse ref format: "layerIdx.primIdx"
                    String[] parts = sourceRef.split("\\.");
                    if (parts.length == 2) {
                        int layerIdx = Integer.parseInt(parts[0]);
                        int primIdx = Integer.parseInt(parts[1]);
                        if (layerIdx < fLayers.size()) {
                            var layer = fLayers.get(layerIdx);
                            if (primIdx < layer.primitives().size()) {
                                var prim = layer.primitives().get(primIdx);
                                var pos = prim.transform().offset();
                                ShockwavePostEffect.setTargetPosition(pos.x, pos.y, pos.z);
                                ShockwavePostEffect.trigger();
                                return;
                            }
                        }
                    }
                }
                // No linked primitive - use player position
                var playerPos = client.player.getPos();
                ShockwavePostEffect.setTargetPosition((float)playerPos.x, (float)playerPos.y, (float)playerPos.z);
                ShockwavePostEffect.trigger();
            }
        }).dimensions(x, y, halfW, 20).build());
        widgets.add(ButtonWidget.builder(Text.literal("🎯 At Cursor"), btn -> {
            // 256-block raycast to spawn shockwave at cursor position
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
                    
                    // Apply Y offset from config
                    float yOffset = state.shockwaveAdapter().getConfig().cursorYOffset();
                    
                    // Get current field state and source primitive ref
                    String fieldJson = state.toStateJson();
                    String sourceRef = state.shockwaveAdapter().shapeSourceRef();
                    
                    // Spawn field at cursor position + Y offset via server
                    net.cyberpunk042.client.network.GuiPacketSender.spawnShockwaveField(
                        fieldJson, (float)pos.x, (float)pos.y + yOffset, (float)pos.z, sourceRef);
                    
                    // Server will broadcast ShockwaveTriggerS2CPayload to all players in range
                    // including us, so no need to trigger locally
                    
                    net.cyberpunk042.client.gui.widget.ToastNotification.success(
                        String.format("Shockwave @ %.0f, %.0f, %.0f", pos.x, pos.y + yOffset, pos.z));
                } else {
                    // No hit - fallback to camera mode
                    ShockwavePostEffect.setOriginMode(OriginMode.CAMERA);
                    ShockwavePostEffect.trigger();
                    net.cyberpunk042.client.gui.widget.ToastNotification.info("No block hit - using camera mode");
                }
            }
        }).dimensions(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, 20).build());
        content.advanceBy(22);
        
        // Y Offset for At Cursor spawn
        content.slider("Y Offset", "shockwave.cursorYOffset").range(-10f, 20f).format("%.1f").add();
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // 1. SHOCKWAVE RINGS - All ring controls consolidated
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeaderWithReset("Rings", this::resetRingsSection);
        
        // Row 1: Count | Spacing | Thickness
        content.sliderTriple(
            "RngCnt", "shockwave.ringCount", 1f, 100f,
            "RngSpc", "shockwave.ringSpacing", 1f, 200f,
            "RngThk", "shockwave.ringThickness", 0.5f, 50f
        );
        
        // Row 2: MaxRadius | Glow | Intensity
        content.sliderTriple(
            "MaxR", "shockwave.ringMaxRadius", 10f, 2000f,
            "RngGlo", "shockwave.ringGlowWidth", 1f, 100f,
            "RngInt", "shockwave.ringIntensity", 0.1f, 5f
        );
        
        // Row 3: R | G | B (color)
        content.sliderTriple(
            "R", "shockwave.ringColorR", 0f, 1f,
            "G", "shockwave.ringColorG", 0f, 1f,
            "B", "shockwave.ringColorB", 0f, 1f
        );
        
        // Row 4: Opacity | RingSpeed | Combined toggle
        y = content.getCurrentY();
        int thirdW2 = (w - GuiConstants.COMPACT_GAP * 2) / 3;
        int xMid = x + thirdW2 + GuiConstants.COMPACT_GAP;
        int x3Row4 = xMid + thirdW2 + GuiConstants.COMPACT_GAP;
        
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.slider(x, y, thirdW2,
            "Opac", 0f, 1f, (Float)state.get("shockwave.ringColorOpacity"), "%.2f", "Ring opacity",
            v -> state.set("shockwave.ringColorOpacity", v)));
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.slider(xMid, y, thirdW2,
            "RngSpd", 1f, 100f, (Float)state.get("shockwave.ringSpeed"), "%.1f", "Ring expansion speed",
            v -> state.set("shockwave.ringSpeed", v)));
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggle(x3Row4, y, thirdW2, "Combined",
            (Boolean)state.get("shockwave.combinedMode"), "Combined mode: single shockwave from center",
            v -> state.set("shockwave.combinedMode", v)));
        content.advanceBy(22);
        
        // Row 5: Polygon Sides (only shown when ShapeType=POLYGON and manual mode)
        ShapeType currentShapeType = (ShapeType) state.get("shockwave.shapeType");
        if (currentShapeType == ShapeType.POLYGON && !hasSourcePrimitive) {
            y = content.getCurrentY();
            widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.slider(x, y, w,
                "Sides", 3f, 32f, ((Number)state.get("shockwave.polygonSides")).floatValue(), "%.0f", "Number of polygon sides (3-32)",
                v -> state.set("shockwave.polygonSides", v.intValue())));
            content.advanceBy(22);
        }
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // 2. ORBITAL SPHERES - Only shown when ShapeType=ORBITAL
        // ═══════════════════════════════════════════════════════════════════════
        
        // Only show orbitals and beams in ORBITAL mode
        boolean isOrbitalMode = currentShapeType == ShapeType.ORBITAL;
        
        if (isOrbitalMode) {
        content.sectionHeaderWithReset("Orbital", this::resetOrbitalSection);
        
        // Geometry: Count | MainR | OrbR (3 sliders)
        content.sliderTriple(
            "OrbCnt", "shockwave.orbitalCount", 1f, 100f,
            "MainR", "shockwave.mainRadius", 0f, 500f,
            "OrbR", "shockwave.orbitalRadius", 0.5f, 200f
        );
        
        // Geometry: OrbitDist | BlendRadius | CoronaWidth (3 sliders)
        content.sliderTriple(
            "OrbDst", "shockwave.orbitDistance", 1f, 500f,
            "Blend", "shockwave.blendRadius", -50f, 50f,
            "OrbCorW", "shockwave.orbitalCoronaWidth", 0.5f, 50f
        );
        
        // Body RGB (3 sliders)
        content.sliderTriple(
            "BodyR", "shockwave.orbitalBodyR", 0f, 1f,
            "BodyG", "shockwave.orbitalBodyG", 0f, 1f,
            "BodyB", "shockwave.orbitalBodyB", 0f, 1f
        );
        
        // Corona RGB (3 sliders)
        content.sliderTriple(
            "CorR", "shockwave.orbitalCoronaR", 0f, 1f,
            "CorG", "shockwave.orbitalCoronaG", 0f, 1f,
            "CorB", "shockwave.orbitalCoronaB", 0f, 1f
        );
        
        // Corona effects: Alpha | Intensity | RimPower (3 sliders)
        content.sliderTriple(
            "OrbAlp", "shockwave.orbitalCoronaA", 0f, 1f,
            "OrbInt", "shockwave.orbitalCoronaIntensity", 0.1f, 10f,
            "OrbRmP", "shockwave.orbitalRimPower", 0.5f, 10f
        );
        
        // RimFalloff + OrbitalSpeed
        content.sliderPair(
            "OrbRmF", "shockwave.orbitalRimFalloff", 0.5f, 5f,
            "OrbSpd", "shockwave.orbitalSpeed", -0.5f, 0.5f
        );
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // BEAMS
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeaderWithReset("Beams", this::resetBeamSection);
        
        // Row 1: Height | Width | Taper
        // Note: Width=0 uses WidthScale proportionally, Width>0 overrides with absolute
        content.sliderTriple(
            "Height", "shockwave.beamHeight", 0f, 1000f,
            "Width", "shockwave.beamWidth", 0f, 50f,
            "Taper", "shockwave.beamTaper", 0f, 3f
        );
        
        // Row 2: WidthScale | CoronaWidth | CoronaAlpha (3 sliders)
        // Note: WidthScale is only used when Width is 0 (else Width takes priority)
        content.sliderTriple(
            "WScl", "shockwave.beamWidthScale", 0.01f, 2f,
            "BmCorW", "shockwave.beamCoronaWidth", 0.5f, 50f,
            "BmAlph", "shockwave.beamCoronaA", 0f, 1f
        );
        
        // Body RGB (3 sliders)
        content.sliderTriple(
            "BmBdyR", "shockwave.beamBodyR", 0f, 1f,
            "BmBdyG", "shockwave.beamBodyG", 0f, 1f,
            "BmBdyB", "shockwave.beamBodyB", 0f, 1f
        );
        
        // Corona RGB (3 sliders)
        content.sliderTriple(
            "BmCorR", "shockwave.beamCoronaR", 0f, 1f,
            "BmCorG", "shockwave.beamCoronaG", 0f, 1f,
            "BmCorB", "shockwave.beamCoronaB", 0f, 1f
        );
        
        // Corona effects: Intensity | RimPower | RimFalloff (3 sliders)
        content.sliderTriple(
            "BmInt", "shockwave.beamCoronaIntensity", 0.1f, 10f,
            "BmRmP", "shockwave.beamRimPower", 0.5f, 10f,
            "BmRmF", "shockwave.beamRimFalloff", 0.5f, 5f
        );
        
        content.gap();
        } // end isOrbitalMode
        
        // ═══════════════════════════════════════════════════════════════════════
        // ANIMATION
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeaderWithReset("Animation", this::resetAnimationSection);
        
        // Row 1: Orbital Spawn | Retract | OrbDelay (3 sliders)
        content.sliderTriple(
            "OrbSpn", "shockwave.orbitalSpawnDuration", 100f, 10000f,
            "OrbRet", "shockwave.orbitalRetractDuration", 100f, 10000f,
            "OrbDly", "shockwave.orbitalSpawnDelay", 0f, 5000f
        );
        
        // Row 2: Beam Grow | Shrink | Hold (3 sliders)
        content.sliderTriple(
            "BmGrow", "shockwave.beamGrowDuration", 100f, 5000f,
            "BmShrk", "shockwave.beamShrinkDuration", 100f, 5000f,
            "BmHold", "shockwave.beamHoldDuration", 0f, 10000f
        );
        
        // Row 3: Beam Width/Length Grow Factors + Delay (3 sliders)
        content.sliderTriple(
            "WGrow", "shockwave.beamWidthGrowFactor", 0f, 1f,
            "LGrow", "shockwave.beamLengthGrowFactor", 0f, 1f,
            "BmDly", "shockwave.beamStartDelay", 0f, 5000f
        );
        
        // Row 4: Retract Delay + Auto Retract toggle (sliderPair + toggle)
        y = content.getCurrentY();
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.slider(x, y, halfW,
            "RetrDly", 0f, 5000f, (Float)state.get("shockwave.retractDelay"), "%.0f", "Delay before retract",
            v -> state.set("shockwave.retractDelay", v)));
        boolean autoRetr = (Boolean) state.get("shockwave.autoRetractOnRingEnd");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggle(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, "AutoRet",
            autoRetr, "Retract when rings end",
            v -> state.set("shockwave.autoRetractOnRingEnd", v)));
        content.advanceBy(22);
        
        // Row 5: Orbital Easing dropdowns (2-column)
        y = content.getCurrentY();
        EasingType orbSpawnEase = (EasingType) state.get("shockwave.orbitalSpawnEasing");
        EasingType orbRetractEase = (EasingType) state.get("shockwave.orbitalRetractEasing");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.enumDropdown(x, y, halfW,
            "OrbSpnEase", EasingType.class, orbSpawnEase, "Orbital spawn easing curve",
            v -> state.set("shockwave.orbitalSpawnEasing", v)));
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.enumDropdown(x + halfW + GuiConstants.COMPACT_GAP, y, halfW,
            "OrbRetEase", EasingType.class, orbRetractEase, "Orbital retract easing curve",
            v -> state.set("shockwave.orbitalRetractEasing", v)));
        content.advanceBy(22);
        
        // Row 6: Beam Easing dropdowns (2-column)
        y = content.getCurrentY();
        EasingType bmGrowEase = (EasingType) state.get("shockwave.beamGrowEasing");
        EasingType bmShrinkEase = (EasingType) state.get("shockwave.beamShrinkEasing");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.enumDropdown(x, y, halfW,
            "BmGrowEase", EasingType.class, bmGrowEase, "Beam grow easing curve",
            v -> state.set("shockwave.beamGrowEasing", v)));
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.enumDropdown(x + halfW + GuiConstants.COMPACT_GAP, y, halfW,
            "BmShrinkEase", EasingType.class, bmShrinkEase, "Beam shrink easing curve",
            v -> state.set("shockwave.beamShrinkEasing", v)));
        content.advanceBy(22);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // SCREEN EFFECTS
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Screen Effects");
        
        // Row 1: Blackout | Vignette | VigRadius (3 sliders)
        content.sliderTriple(
            "Black", "shockwave.blackout", 0f, 1f,
            "Vign", "shockwave.vignetteAmount", 0f, 1f,
            "VigR", "shockwave.vignetteRadius", 0f, 1f
        );
        
        // Row 2: TintAmount | Tint R | G | B (use triple for RGB)
        content.sliderTriple(
            "TintR", "shockwave.tintR", 0f, 1f,
            "TintG", "shockwave.tintG", 0f, 1f,
            "TintB", "shockwave.tintB", 0f, 1f
        );
        
        // Row 3: TintAmount + Contract toggle
        y = content.getCurrentY();
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.slider(x, y, halfW,
            "TintAmt", 0f, 1f, (Float)state.get("shockwave.tintAmount"), "%.2f", "Screen tint strength",
            v -> state.set("shockwave.tintAmount", v)));
        boolean contract = (Boolean) state.get("shockwave.ringContractMode");
        widgets.add(net.cyberpunk042.client.gui.util.GuiWidgets.toggle(x + halfW + GuiConstants.COMPACT_GAP, y, halfW, "Contract",
            contract, "Rings contract instead of expand",
            v -> state.set("shockwave.ringContractMode", v)));
        content.advanceBy(22);
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // STATUS
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sectionHeader("Status");
        content.advanceBy(24);
        
        contentHeight = content.getContentHeight();
        Logging.GUI.topic("panel").debug("ShockwaveSubPanel built: {} widgets", widgets.size());
    }
    
    // Note: All sliders and controls now use ContentBuilder bindings with shockwave.* paths
    // The ShockwaveAdapter handles syncing to ShockwavePostEffect
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
        
        // Draw status text
        String status = ShockwavePostEffect.getStatusString();
        int statusY = bounds.y() + contentHeight - 16 - scrollOffset;
        if (statusY > bounds.y() && statusY < bounds.bottom()) {
            context.drawTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                Text.literal("§7" + status), bounds.x() + GuiConstants.PADDING, statusY, 0xFFFFFF);
        }
    }
    
    public int getHeight() { return contentHeight; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION RESET HELPERS
    // Reset specific sections to their DEFAULT values from ShockwaveConfig
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void resetRingsSection() {
        var d = ShockwaveConfig.DEFAULT;
        state.set("shockwave.ringCount", (float) d.ringCount());
        state.set("shockwave.ringSpacing", d.ringSpacing());
        state.set("shockwave.ringThickness", d.ringThickness());
        state.set("shockwave.ringMaxRadius", d.ringMaxRadius());
        state.set("shockwave.ringGlowWidth", d.ringGlowWidth());
        state.set("shockwave.ringIntensity", d.ringIntensity());
        state.set("shockwave.ringColorR", d.ringColorR());
        state.set("shockwave.ringColorG", d.ringColorG());
        state.set("shockwave.ringColorB", d.ringColorB());
        state.set("shockwave.ringColorOpacity", d.ringColorOpacity());
        state.set("shockwave.ringSpeed", d.ringSpeed());
        state.set("shockwave.combinedMode", d.combinedMode());
        rebuildContent();
        notifyWidgetsChanged();
    }
    
    private void resetOrbitalSection() {
        var d = ShockwaveConfig.DEFAULT;
        state.set("shockwave.orbitalCount", (float) d.orbitalCount());
        state.set("shockwave.mainRadius", d.mainRadius());
        state.set("shockwave.orbitalRadius", d.orbitalRadius());
        state.set("shockwave.orbitDistance", d.orbitDistance());
        state.set("shockwave.blendRadius", d.blendRadius());
        state.set("shockwave.orbitalCoronaWidth", d.orbitalCoronaWidth());
        state.set("shockwave.orbitalBodyR", d.orbitalBodyR());
        state.set("shockwave.orbitalBodyG", d.orbitalBodyG());
        state.set("shockwave.orbitalBodyB", d.orbitalBodyB());
        state.set("shockwave.orbitalCoronaR", d.orbitalCoronaR());
        state.set("shockwave.orbitalCoronaG", d.orbitalCoronaG());
        state.set("shockwave.orbitalCoronaB", d.orbitalCoronaB());
        state.set("shockwave.orbitalCoronaA", d.orbitalCoronaA());
        state.set("shockwave.orbitalCoronaIntensity", d.orbitalCoronaIntensity());
        state.set("shockwave.orbitalRimPower", d.orbitalRimPower());
        state.set("shockwave.orbitalRimFalloff", d.orbitalRimFalloff());
        rebuildContent();
        notifyWidgetsChanged();
    }
    
    private void resetBeamSection() {
        var d = ShockwaveConfig.DEFAULT;
        state.set("shockwave.beamHeight", d.beamHeight());
        state.set("shockwave.beamWidth", d.beamWidth());
        state.set("shockwave.beamWidthScale", d.beamWidthScale());
        state.set("shockwave.beamTaper", d.beamTaper());
        state.set("shockwave.beamBodyR", d.beamBodyR());
        state.set("shockwave.beamBodyG", d.beamBodyG());
        state.set("shockwave.beamBodyB", d.beamBodyB());
        state.set("shockwave.beamCoronaR", d.beamCoronaR());
        state.set("shockwave.beamCoronaG", d.beamCoronaG());
        state.set("shockwave.beamCoronaB", d.beamCoronaB());
        state.set("shockwave.beamCoronaA", d.beamCoronaA());
        state.set("shockwave.beamCoronaWidth", d.beamCoronaWidth());
        state.set("shockwave.beamCoronaIntensity", d.beamCoronaIntensity());
        state.set("shockwave.beamRimPower", d.beamRimPower());
        state.set("shockwave.beamRimFalloff", d.beamRimFalloff());
        rebuildContent();
        notifyWidgetsChanged();
    }
    
    private void resetAnimationSection() {
        var d = ShockwaveConfig.DEFAULT;
        state.set("shockwave.orbitalSpeed", d.orbitalSpeed());
        state.set("shockwave.orbitalSpawnDuration", d.orbitalSpawnDuration());
        state.set("shockwave.orbitalRetractDuration", d.orbitalRetractDuration());
        state.set("shockwave.beamGrowDuration", d.beamGrowDuration());
        state.set("shockwave.beamShrinkDuration", d.beamShrinkDuration());
        state.set("shockwave.beamHoldDuration", d.beamHoldDuration());
        state.set("shockwave.orbitalSpawnDelay", d.orbitalSpawnDelay());
        state.set("shockwave.beamStartDelay", d.beamStartDelay());
        state.set("shockwave.retractDelay", d.retractDelay());
        state.set("shockwave.autoRetractOnRingEnd", d.autoRetractOnRingEnd());
        rebuildContent();
        notifyWidgetsChanged();
    }
}
