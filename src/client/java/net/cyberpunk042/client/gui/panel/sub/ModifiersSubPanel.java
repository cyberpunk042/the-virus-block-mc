package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.visual.animation.*;
import net.cyberpunk042.client.gui.builder.Bound;
import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.shape.RayCompatibilityHint;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.client.gui.util.GuiWidgets;
import net.cyberpunk042.client.gui.widget.LabeledSlider;
import net.cyberpunk042.log.Logging;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * ModifiersSubPanel - Animation and effects using BoundPanel.
 * 
 * <p>Controls modifiers like bobbing, breathing, wobble, wave, precession,
 * and ray-specific animations (flow, motion).</p>
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for bidirectional
 * state synchronization. Widgets automatically update on profile load,
 * primitive switch, etc.</p>
 */
public class ModifiersSubPanel extends BoundPanel {
    
    private static final int COMPACT_H = 16;
    
    private final int startY;
    
    // Warning callback for ray compatibility hints
    private java.util.function.BiConsumer<String, Integer> warningCallback;
    private String currentWarning = null;
    private int currentWarningColor = 0;
    
    public ModifiersSubPanel(Screen parent, FieldEditState state) {
        super(parent, state);
        this.startY = GuiConstants.PADDING;
        Logging.GUI.topic("panel").debug("ModifiersSubPanel created (BoundPanel version)");
    }
    
    /**
     * Sets the callback for displaying ray compatibility warnings.
     * @param callback BiConsumer accepting (warningText, color) - null text clears warning
     */
    public void setWarningCallback(java.util.function.BiConsumer<String, Integer> callback) {
        this.warningCallback = callback;
    }

    @Override
    protected void buildContent() {
        ContentBuilder content = content(startY);
        
        // Check if current shape is Rays
        boolean isRaysShape = "rays".equalsIgnoreCase(state.getString("shapeType"));
        
        // ═══════════════════════════════════════════════════════════════════════
        // FIELD MODIFIERS - Bobbing & Breathing
        // ═══════════════════════════════════════════════════════════════════════
        
        content.sliderPair(
            "Bobbing", "modifiers.bobbing", 0f, 1f,
            "Breathing", "modifiers.breathing", 0f, 1f
        );
        
        // ═══════════════════════════════════════════════════════════════════════
        // COLOR CYCLE
        // ═══════════════════════════════════════════════════════════════════════
        
        buildColorCycleSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // WOBBLE - Random position jitter
        // ═══════════════════════════════════════════════════════════════════════
        
        buildWobbleSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // WAVE DEFORMATION
        // ═══════════════════════════════════════════════════════════════════════
        
        buildWaveSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // PRECESSION - Axis wobble
        // ═══════════════════════════════════════════════════════════════════════
        
        buildPrecessionSection(content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAY FLOW - Alpha/visibility animation (Rays only)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(isRaysShape, c -> buildRayFlowSection(c));
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAY MOTION - Geometry animation (Rays only)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(isRaysShape, c -> buildRayMotionSection(c));
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAY WIGGLE - Undulation/deformation (Rays only)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(isRaysShape, c -> buildRayWiggleSection(c));
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAY TWIST - Axial rotation (Rays only)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(isRaysShape, c -> buildRayTwistSection(c));
        
        // ═══════════════════════════════════════════════════════════════════════
        // RAY COMPATIBILITY CHECK - Show warnings for incompatible configurations
        // ═══════════════════════════════════════════════════════════════════════
        
        if (isRaysShape) {
            // Compute compatibility and display inline warning if any
            checkRayCompatibility();
            if (currentWarning != null && !currentWarning.isEmpty()) {
                content.gap();
                content.label("⚠ " + currentWarning, currentWarningColor);
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // TRAVEL EFFECT - General travel effect for any shape (non-rays)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.when(!isRaysShape, c -> buildTravelEffectSection(c));
        
        contentHeight = content.getContentHeight();
        Logging.GUI.topic("panel").debug("ModifiersSubPanel built: {} widgets, isRays={}, warning={}",
            widgets.size(), isRaysShape, currentWarning);
    }
    
    /**
     * Checks ray animation configuration for compatibility issues and sends warnings.
     */
    private void checkRayCompatibility() {
        RayCompatibilityHint.compute(state, (warning, color) -> {
            currentWarning = warning;
            currentWarningColor = color;
            if (warningCallback != null) {
                warningCallback.accept(warning, color);
            }
        });
    }
    
    // =========================================================================
    // COLOR CYCLE SECTION
    // =========================================================================
    
    private void buildColorCycleSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        boolean active = state.colorCycle().isActive();
        
        // Checkboxes: Color Cycle | Smooth Blend
        CyclingButtonWidget<Boolean> cycleToggle = GuiWidgets.toggle(
            x, y, halfW, "Color Cycle", active, "Enable color cycling",
            v -> {
                if (!v) {
                    state.set("colorCycle.speed", 0f);
                } else {
                    if (state.colorCycle().colors() == null || state.colorCycle().colors().isEmpty()) {
                        state.set("colorCycle", ColorCycleConfig.builder()
                            .colors("#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#8B00FF")
                            .speed(1.0f).blend(true).build());
                    } else if (state.getFloat("colorCycle.speed") == 0f) {
                        state.set("colorCycle.speed", 1f);
                    }
                }
            });
        widgets.add(cycleToggle);
        
        CyclingButtonWidget<Boolean> blendToggle = GuiWidgets.toggle(
            x + halfW + GuiConstants.PADDING, y, halfW, "Smooth Blend",
            state.getBool("colorCycle.blend"), "Smooth color transitions",
            v -> state.set("colorCycle.blend", v));
        widgets.add(blendToggle);
        content.advanceRow();
        
        // Speed slider
        content.slider("Speed", "colorCycle.speed").range(0.1f, 5f).format("%.1f").add();
    }
    
    // =========================================================================
    // WOBBLE SECTION
    // =========================================================================
    
    private void buildWobbleSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        WobbleConfig wobble = state.wobble();
        boolean active = wobble != null && wobble.isActive();
        
        CyclingButtonWidget<Boolean> toggle = GuiWidgets.toggle(
            x, y, w, "Wobble", active, "Random position jitter",
            v -> {
                if (!v) {
                    state.set("wobble", WobbleConfig.NONE);
                } else {
                    WobbleConfig current = state.wobble();
                    if (current == null || current.amplitude() == null || !current.isActive()) {
                        state.set("wobble.amplitude", new org.joml.Vector3f(0.1f, 0.05f, 0.1f));
                        if (current == null || current.speed() <= 0) {
                            state.set("wobble.speed", 1.0f);
                        }
                    }
                }
            });
        widgets.add(toggle);
        content.advanceRow();
        
        // Amplitude + Speed
        content.sliderPair(
            "Amplitude", "wobble.amplitude", 0f, 1f,
            "Speed", "wobble.speed", 0.1f, 5f
        );
    }
    
    // =========================================================================
    // WAVE SECTION
    // =========================================================================
    
    private void buildWaveSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        WaveConfig wave = state.wave();
        boolean active = wave != null && wave.isActive();
        
        CyclingButtonWidget<Boolean> toggle = GuiWidgets.toggle(
            x, y, w, "Wave Deformation", active, "Surface wave animation",
            v -> { if (!v) state.set("wave", WaveConfig.NONE); });
        widgets.add(toggle);
        content.advanceRow();
        
        // Amplitude + Frequency
        content.sliderPair(
            "Amplitude", "wave.amplitude", 0f, 1f,
            "Frequency", "wave.frequency", 0.1f, 10f
        );
        
        // Speed + Direction dropdown
        // Create Speed slider at left half
        LabeledSlider speedSlider = LabeledSlider.builder("Speed")
            .position(x, content.getCurrentY()).width(halfW)
            .range(0.1f, 5f).initial(state.getFloat("wave.speed")).format("%.1f")
            .onChange(v -> state.set("wave.speed", v))
            .build();
        widgets.add(speedSlider);
        
        // Direction dropdown at right half
        CyclingButtonWidget<Axis> dirDropdown = CyclingButtonWidget.<Axis>builder(
                a -> Text.literal("Dir: " + a.name()))
            .values(Axis.values())
            .initially(wave != null && wave.direction() != null ? wave.direction() : Axis.Y)
            .omitKeyText()
            .build(x + halfW + GuiConstants.PADDING, content.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("wave.direction", val.name()));
        widgets.add(dirDropdown);
        content.advanceRow();
    }
    
    // =========================================================================
    // PRECESSION SECTION
    // =========================================================================
    
    private void buildPrecessionSection(ContentBuilder content) {
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();
        int w = panelWidth - GuiConstants.PADDING * 2;
        
        PrecessionConfig prec = state.precession();
        boolean active = prec != null && prec.isActive();
        
        CyclingButtonWidget<Boolean> toggle = GuiWidgets.toggle(
            x, y, w, "Precession", active, "Axis wobble (lighthouse effect)",
            v -> {
                if (!v) state.set("precession", null);
                else state.set("precession", PrecessionConfig.DEFAULT);
            });
        widgets.add(toggle);
        content.advanceRow();
        
        // Tilt + Speed
        content.sliderPair(
            "Tilt°", "precession.tiltAngle", 1f, 90f,
            "Speed", "precession.speed", 0.01f, 2f
        );
    }
    
    // =========================================================================
    // TRAVEL EFFECT SECTION (Any shape except Rays)
    // =========================================================================
    
    private void buildTravelEffectSection(ContentBuilder c) {
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Section header
        c.gap();
        c.sectionHeader("Travel Effect");
        c.infoText("Directional alpha sweep (relativistic jet)");
        
        TravelEffectConfig travelEffect = state.travelEffect();
        
        // === TRAVEL MODE ===
        net.cyberpunk042.visual.energy.EnergyTravel curMode = 
            travelEffect != null ? travelEffect.effectiveMode() : net.cyberpunk042.visual.energy.EnergyTravel.NONE;
        
        CyclingButtonWidget<net.cyberpunk042.visual.energy.EnergyTravel> modeDropdown = 
            CyclingButtonWidget.<net.cyberpunk042.visual.energy.EnergyTravel>builder(
                    m -> Text.literal("Mode: " + m.displayName()))
                .values(net.cyberpunk042.visual.energy.EnergyTravel.values())
                .initially(curMode)
                .omitKeyText()
                .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                    (btn, val) -> state.set("travelEffect.mode", val));
        widgets.add(modeDropdown);
        
        // Direction axis dropdown
        Axis curDir = travelEffect != null ? travelEffect.effectiveDirection() : Axis.Y;
        CyclingButtonWidget<Axis> dirDropdown = CyclingButtonWidget.<Axis>builder(
                a -> Text.literal("Axis: " + a.name()))
            .values(Axis.values())
            .initially(curDir)
            .omitKeyText()
            .build(x + halfW + GuiConstants.PADDING, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("travelEffect.direction", val));
        widgets.add(dirDropdown);
        c.advanceRow();
        
        // === TRAVEL DIRECTION MODE (Linear, Radial, Angular, Spherical) ===
        TravelDirection curTravelDir = travelEffect != null ? travelEffect.effectiveTravelDirection() : TravelDirection.LINEAR;
        CyclingButtonWidget<TravelDirection> travelDirDropdown = CyclingButtonWidget.<TravelDirection>builder(
                td -> Text.literal("Type: " + td.displayName()))
            .values(TravelDirection.values())
            .initially(curTravelDir)
            .omitKeyText()
            .build(x, c.getCurrentY(), w, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("travelEffect.travelDirection", val));
        widgets.add(travelDirDropdown);
        c.advanceRow();
        
        // === SPEED + BLEND MODE ===
        LabeledSlider speedSlider = LabeledSlider.builder("Speed")
            .position(x, c.getCurrentY()).width(halfW)
            .range(0.01f, 3f).initial(travelEffect != null ? travelEffect.speed() : 1f).format("%.2f")
            .onChange(v -> state.set("travelEffect.speed", v))
            .build();
        widgets.add(speedSlider);
        
        net.cyberpunk042.visual.energy.TravelBlendMode curBlend = 
            travelEffect != null && travelEffect.blendMode() != null
                ? travelEffect.blendMode()
                : net.cyberpunk042.visual.energy.TravelBlendMode.REPLACE;
        CyclingButtonWidget<net.cyberpunk042.visual.energy.TravelBlendMode> blendDropdown = 
            CyclingButtonWidget.<net.cyberpunk042.visual.energy.TravelBlendMode>builder(
                    m -> Text.literal("Blend: " + m.displayName()))
                .values(net.cyberpunk042.visual.energy.TravelBlendMode.values())
                .initially(curBlend)
                .omitKeyText()
                .build(x + halfW + GuiConstants.PADDING, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                    (btn, val) -> state.set("travelEffect.blendMode", val));
        widgets.add(blendDropdown);
        c.advanceRow();
        
        // === MIN ALPHA + INTENSITY ===
        c.sliderPair(
            "MinA", "travelEffect.minAlpha", 0f, 1f,
            "Effect", "travelEffect.intensity", 0f, 1f
        );
        
        // === COUNT + WIDTH ===
        c.sliderPair(
            "Count", "travelEffect.count", 1f, 10f,
            "Width", "travelEffect.width", 0.05f, 0.5f
        );
    }
    
    // =========================================================================
    // RAY FLOW SECTION (Rays only)
    // =========================================================================
    
    private void buildRayFlowSection(ContentBuilder c) {
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Section header
        c.gap();
        c.sectionHeader("Ray Flow");
        c.infoText("Animation timing (mode is on Shape panel)");
        
        RayFlowConfig flow = state.rayFlow();
        
        // === RADIATIVE ANIMATION (enable/speed only - mode is in Shape) ===
        boolean radiativeEnabled = flow != null && flow.radiativeEnabled();
        CyclingButtonWidget<Boolean> radiativeToggle = GuiWidgets.toggle(
            x, c.getCurrentY(), halfW, "Radiative",
            radiativeEnabled, "Enable radiative animation",
            v -> state.set("rayFlow.radiativeEnabled", v));
        widgets.add(radiativeToggle);
        
        LabeledSlider radiativeSpeedSlider = LabeledSlider.builder("Speed")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.01f, 2f).initial(flow != null ? flow.radiativeSpeed() : 1f).format("%.2f")
            .onChange(v -> state.set("rayFlow.radiativeSpeed", v))
            .build();
        widgets.add(radiativeSpeedSlider);
        c.advanceRow();
        
        // === TRAVEL ANIMATION ===
        c.infoText("Travel (chase/scroll along rays)");
        
        net.cyberpunk042.visual.energy.EnergyTravel curTravel = 
            flow != null ? flow.effectiveTravel() : net.cyberpunk042.visual.energy.EnergyTravel.NONE;
        
        CyclingButtonWidget<net.cyberpunk042.visual.energy.EnergyTravel> travelDropdown = 
            CyclingButtonWidget.<net.cyberpunk042.visual.energy.EnergyTravel>builder(
                    m -> Text.literal("Trav: " + m.displayName()))
                .values(net.cyberpunk042.visual.energy.EnergyTravel.values())
                .initially(curTravel)
                .omitKeyText()
                .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                    (btn, val) -> state.set("rayFlow.travel", val));
        widgets.add(travelDropdown);
        
        LabeledSlider travelSpeedSlider = LabeledSlider.builder("TSpd")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.01f, 2f).initial(flow != null ? flow.travelSpeed() : 1f).format("%.2f")
            .onChange(v -> state.set("rayFlow.travelSpeed", v))
            .build();
        widgets.add(travelSpeedSlider);
        c.advanceRow();
        
        // Chase count + width
        c.sliderPair(
            "Count", "rayFlow.chaseCount", 1f, 10f,
            "Width", "rayFlow.chaseWidth", 0.05f, 0.5f
        );
        
        // === TRAVEL BLEND MODE + CONTROLS ===
        net.cyberpunk042.visual.energy.TravelBlendMode curBlendMode = 
            flow != null && flow.travelBlendMode() != null 
                ? flow.travelBlendMode() 
                : net.cyberpunk042.visual.energy.TravelBlendMode.REPLACE;
        
        CyclingButtonWidget<net.cyberpunk042.visual.energy.TravelBlendMode> blendDropdown = 
            CyclingButtonWidget.<net.cyberpunk042.visual.energy.TravelBlendMode>builder(
                    m -> Text.literal("Blend: " + m.displayName()))
                .values(net.cyberpunk042.visual.energy.TravelBlendMode.values())
                .initially(curBlendMode)
                .omitKeyText()
                .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                    (btn, val) -> state.set("rayFlow.travelBlendMode", val));
        widgets.add(blendDropdown);
        
        LabeledSlider minAlphaSlider = LabeledSlider.builder("MinA")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0f, 1f).initial(flow != null ? flow.travelMinAlpha() : 0f).format("%.2f")
            .onChange(v -> state.set("rayFlow.travelMinAlpha", v))
            .build();
        widgets.add(minAlphaSlider);
        c.advanceRow();
        
        c.slider("Effect", "rayFlow.travelIntensity").range(0f, 1f).format("%.2f").add();
        
        // === FLICKER ANIMATION ===
        c.infoText("Flicker (twinkle/strobe effects)");
        
        net.cyberpunk042.visual.energy.EnergyFlicker curFlicker = 
            flow != null ? flow.effectiveFlicker() : net.cyberpunk042.visual.energy.EnergyFlicker.NONE;
        
        CyclingButtonWidget<net.cyberpunk042.visual.energy.EnergyFlicker> flickerDropdown = 
            CyclingButtonWidget.<net.cyberpunk042.visual.energy.EnergyFlicker>builder(
                    m -> Text.literal("Flkr: " + m.displayName()))
                .values(net.cyberpunk042.visual.energy.EnergyFlicker.values())
                .initially(curFlicker)
                .omitKeyText()
                .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                    (btn, val) -> state.set("rayFlow.flicker", val));
        widgets.add(flickerDropdown);
        
        LabeledSlider intensitySlider = LabeledSlider.builder("Int")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0f, 1f).initial(flow != null ? flow.flickerIntensity() : 0.3f).format("%.1f")
            .onChange(v -> state.set("rayFlow.flickerIntensity", v))
            .build();
        widgets.add(intensitySlider);
        c.advanceRow();
        
        c.slider("Freq", "rayFlow.flickerFrequency").range(1f, 20f).format("%.1f").add();
    }
    
    // =========================================================================
    // RAY MOTION SECTION (Rays only)
    // =========================================================================
    
    private void buildRayMotionSection(ContentBuilder c) {
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Section header
        c.gap();
        c.sectionHeader("Ray Motion");
        c.infoText("Geometry movement (orbit, drift...)");
        
        RayMotionConfig motion = state.rayMotion();
        MotionMode curMode = motion != null ? motion.mode() : MotionMode.NONE;
        
        // Mode dropdown + Speed
        CyclingButtonWidget<MotionMode> modeDropdown = CyclingButtonWidget.<MotionMode>builder(
                m -> Text.literal("Mot: " + m.displayName()))
            .values(MotionMode.values())
            .initially(curMode)
            .omitKeyText()
            .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("rayMotion.mode", val));
        widgets.add(modeDropdown);
        
        LabeledSlider speedSlider = LabeledSlider.builder("Spd")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.01f, 2f).initial(motion != null ? motion.speed() : 0.5f).format("%.2f")
            .onChange(v -> state.set("rayMotion.speed", v))
            .build();
        widgets.add(speedSlider);
        c.advanceRow();
        
        // Direction X, Y
        c.sliderPair(
            "Dir X", "rayMotion.directionX", -1f, 1f,
            "Dir Y", "rayMotion.directionY", -1f, 1f
        );
        
        // Direction Z, Amplitude
        c.sliderPair(
            "Dir Z", "rayMotion.directionZ", -1f, 1f,
            "Amp", "rayMotion.amplitude", 0f, 1f
        );
        
        c.slider("Frequency", "rayMotion.frequency").range(0.1f, 10f).format("%.1f").add();
    }
    
    // =========================================================================
    // RAY WIGGLE SECTION (Rays only)
    // =========================================================================
    
    private void buildRayWiggleSection(ContentBuilder c) {
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Section header
        c.gap();
        c.sectionHeader("Ray Wiggle");
        c.infoText("Undulation (needs multi-segment rays)");
        
        RayWiggleConfig wiggle = state.rayWiggle();
        WiggleMode curMode = wiggle != null ? wiggle.mode() : WiggleMode.NONE;
        
        // Mode dropdown + Speed
        CyclingButtonWidget<WiggleMode> modeDropdown = CyclingButtonWidget.<WiggleMode>builder(
                m -> Text.literal("Wgl: " + m.displayName()))
            .values(WiggleMode.values())
            .initially(curMode)
            .omitKeyText()
            .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("rayWiggle.mode", val));
        widgets.add(modeDropdown);
        
        LabeledSlider speedSlider = LabeledSlider.builder("Spd")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.01f, 3f).initial(wiggle != null ? wiggle.speed() : 0.5f).format("%.2f")
            .onChange(v -> state.set("rayWiggle.speed", v))
            .build();
        widgets.add(speedSlider);
        c.advanceRow();
        
        // Amplitude + Frequency
        c.sliderPair(
            "Amp", "rayWiggle.amplitude", 0f, 0.5f,
            "Freq", "rayWiggle.frequency", 0.5f, 20f
        );
        
        // Phase offset
        c.slider("Phase", "rayWiggle.phaseOffset").range(0f, 360f).format("%.0f°").add();
    }
    
    // =========================================================================
    // RAY TWIST SECTION (Rays only)
    // =========================================================================
    
    private void buildRayTwistSection(ContentBuilder c) {
        int x = GuiConstants.PADDING;
        int w = panelWidth - GuiConstants.PADDING * 2;
        int halfW = (w - GuiConstants.PADDING) / 2;
        
        // Section header
        c.gap();
        c.sectionHeader("Ray Twist");
        c.infoText("Axial rotation (needs 3D shapes)");
        
        RayTwistConfig twist = state.rayTwist();
        TwistMode curMode = twist != null ? twist.mode() : TwistMode.NONE;
        
        // Mode dropdown + Speed
        CyclingButtonWidget<TwistMode> modeDropdown = CyclingButtonWidget.<TwistMode>builder(
                m -> Text.literal("Twst: " + m.displayName()))
            .values(TwistMode.values())
            .initially(curMode)
            .omitKeyText()
            .build(x, c.getCurrentY(), halfW, COMPACT_H, Text.literal(""),
                (btn, val) -> state.set("rayTwist.mode", val));
        widgets.add(modeDropdown);
        
        LabeledSlider speedSlider = LabeledSlider.builder("Spd")
            .position(x + halfW + GuiConstants.PADDING, c.getCurrentY()).width(halfW)
            .range(0.01f, 2f).initial(twist != null ? twist.speed() : 0.5f).format("%.2f")
            .onChange(v -> state.set("rayTwist.speed", v))
            .build();
        widgets.add(speedSlider);
        c.advanceRow();
        
        // Amount (degrees) + Phase offset
        c.sliderPair(
            "Amount°", "rayTwist.amount", 0f, 720f,
            "Phase°", "rayTwist.phaseOffset", 0f, 360f
        );
    }
    
    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    
    @Override
    public void tick() {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    @Override
    protected boolean needsRebuildOnFragmentApply() {
        // Ray sections depend on shape type
        return true;
    }
    
    /** Called when shape type changes - rebuilds widgets */
    public void onShapeChanged() {
        rebuildContent();
    }
    
    public int getContentHeight() {
        return contentHeight;
    }
    
    public List<ClickableWidget> getWidgets() {
        return new ArrayList<>(widgets);
    }
}
