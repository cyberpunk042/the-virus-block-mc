package net.cyberpunk042.client.gui.panel.sub;

import net.cyberpunk042.client.gui.builder.Bound;
import net.cyberpunk042.client.gui.builder.BoundPanel;
import net.cyberpunk042.client.gui.builder.ContentBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.util.GuiConstants;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.transform.Anchor;
import net.cyberpunk042.visual.transform.AxisMotionConfig;
import net.cyberpunk042.visual.transform.Billboard;
import net.cyberpunk042.visual.transform.Facing;
import net.cyberpunk042.visual.transform.MotionMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * TransformSubPanel - Unified Transform + Orbit controls.
 * 
 * <p>Uses {@link BoundPanel} with {@link ContentBuilder} for bidirectional
 * state synchronization. When switching primitives or loading profiles,
 * all widgets automatically update.</p>
 * 
 * <h3>Controls</h3>
 * <ul>
 *   <li>Anchor mode</li>
 *   <li>Offset (X/Y/Z)</li>
 *   <li>Rotation (X/Y/Z)</li>
 *   <li>Scale with toggle icons (R=ScaleWithRadius, ↻=InheritRotation)</li>
 *   <li>Facing, Billboard</li>
 *   <li>3D Orbit per-axis with mode-dependent secondary params</li>
 * </ul>
 */
public class TransformSubPanel extends BoundPanel {
    
    private static final int COMPACT_H = 16;
    private static final int GAP = 2;
    
    private final int startY;
    
    public TransformSubPanel(Screen parent, FieldEditState state, int startY) {
        super(parent, state);
        this.startY = startY;
        Logging.GUI.topic("panel").debug("TransformSubPanel created (BoundPanel version)");
    }
    
    @Override
    protected void buildContent() {
        ContentBuilder content = content(startY);
        
        // ═══════════════════════════════════════════════════════════════════════
        // ANCHOR
        // ═══════════════════════════════════════════════════════════════════════
        
        content.dropdown("Anchor", "transform.anchor", Anchor.class);
        
        // ═══════════════════════════════════════════════════════════════════════
        // OFFSET (X, Y, Z)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.vec3Row("X", "Y", "Z", "transform.offset", -10f, 10f);
        
        // ═══════════════════════════════════════════════════════════════════════
        // ROTATION (Rx, Ry, Rz)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.rotationRow("Rx", "Ry", "Rz", "transform.rotation");
        
        // ═══════════════════════════════════════════════════════════════════════
        // SCALE + Icon Toggles (R=ScaleWithRadius, ↻=InheritRotation)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.row()
            .slider("Scale", "transform.scale", 0.1f, 5f)
            .iconToggle("R", "transform.scaleWithRadius")
            .iconToggle("↻", "transform.inheritRotation")
            .end();
        
        // ═══════════════════════════════════════════════════════════════════════
        // FACING + BILLBOARD (side by side)
        // ═══════════════════════════════════════════════════════════════════════
        
        content.dropdownPair("Face", "transform.facing", Facing.class,
                            "Bill", "transform.billboard", Billboard.class);
        
        content.gap();
        
        // ═══════════════════════════════════════════════════════════════════════
        // 3D ORBIT - PER-AXIS CONTROLS (X, Y, Z)
        // ═══════════════════════════════════════════════════════════════════════
        
        buildAxisOrbit("X", "transform.orbit3d.x", content);
        buildAxisOrbit("Y", "transform.orbit3d.y", content);
        buildAxisOrbit("Z", "transform.orbit3d.z", content);
        
        // ═══════════════════════════════════════════════════════════════════════
        // SPIN ANIMATION (per-axis continuous rotation)
        // ═══════════════════════════════════════════════════════════════════════
        
        buildAxisSpin("X", "spin.speedX", "spin.oscillateX", "spin.rangeX", content);
        buildAxisSpin("Y", "spin.speedY", "spin.oscillateY", "spin.rangeY", content);
        buildAxisSpin("Z", "spin.speedZ", "spin.oscillateZ", "spin.rangeZ", content);
        
        contentHeight = content.getContentHeight();
    }
    
    /**
     * Builds a single axis spin row: Speed slider + Oscillate toggle + conditional Range slider.
     */
    private void buildAxisSpin(String axis, String speedPath, String oscPath, String rangePath, ContentBuilder content) {
        boolean oscillating = state.getBool(oscPath);
        
        if (oscillating) {
            // [SpinX: ▬▬▬▬▬▬] [Osc ☑] [Rng°: ▬▬▬▬]
            int fullWidth = panelWidth - GuiConstants.PADDING * 2;
            int speedWidth = (int)(fullWidth * 0.45f);
            int oscWidth = 40;
            int rangeWidth = fullWidth - speedWidth - oscWidth - GAP * 2;
            int x = GuiConstants.PADDING;
            int y = content.getCurrentY();
            
            // Speed slider
            float currentSpeed = state.getFloat(speedPath);
            var speedSlider = net.cyberpunk042.client.gui.widget.LabeledSlider.builder("Spin" + axis)
                .position(x, y)
                .width(speedWidth)
                .range(-360f, 360f)
                .initial(currentSpeed)
                .format("%.0f°/s")
                .onChange(v -> state.set(speedPath, v))
                .build();
            widgets.add(speedSlider);
            bindings.add(new Bound<>(speedSlider,
                () -> state.getFloat(speedPath), v -> state.set(speedPath, v),
                v -> v, v -> v, speedSlider::setValue));
            
            // Oscillate toggle
            var oscToggle = CyclingButtonWidget.<Boolean>builder(b -> Text.literal(b ? "Osc ✓" : "Osc"))
                .values(List.of(false, true))
                .initially(true)
                .omitKeyText()
                .build(x + speedWidth + GAP, y, oscWidth, COMPACT_H, Text.empty(),
                    (btn, val) -> {
                        state.set(oscPath, val);
                        rebuildContent();
                        notifyWidgetsChanged();
                    });
            widgets.add(oscToggle);
            
            // Range slider (only when oscillating)
            float currentRange = state.getFloat(rangePath);
            var rangeSlider = net.cyberpunk042.client.gui.widget.LabeledSlider.builder("Rng°")
                .position(x + speedWidth + oscWidth + GAP * 2, y)
                .width(rangeWidth)
                .range(1f, 360f)
                .initial(currentRange > 0 ? currentRange : 360f)
                .format("%.0f°")
                .onChange(v -> state.set(rangePath, v))
                .build();
            widgets.add(rangeSlider);
            bindings.add(new Bound<>(rangeSlider,
                () -> state.getFloat(rangePath), v -> state.set(rangePath, v),
                v -> v, v -> v, rangeSlider::setValue));
            
            content.advanceRow();
        } else {
            // [SpinX: ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬] [Osc ☐]
            int fullWidth = panelWidth - GuiConstants.PADDING * 2;
            int oscWidth = 40;
            int speedWidth = fullWidth - oscWidth - GAP;
            int x = GuiConstants.PADDING;
            int y = content.getCurrentY();
            
            // Speed slider (wider when no range)
            float currentSpeed = state.getFloat(speedPath);
            var speedSlider = net.cyberpunk042.client.gui.widget.LabeledSlider.builder("Spin" + axis)
                .position(x, y)
                .width(speedWidth)
                .range(-360f, 360f)
                .initial(currentSpeed)
                .format("%.0f°/s")
                .onChange(v -> state.set(speedPath, v))
                .build();
            widgets.add(speedSlider);
            bindings.add(new Bound<>(speedSlider,
                () -> state.getFloat(speedPath), v -> state.set(speedPath, v),
                v -> v, v -> v, speedSlider::setValue));
            
            // Oscillate toggle
            var oscToggle = CyclingButtonWidget.<Boolean>builder(b -> Text.literal(b ? "Osc ✓" : "Osc"))
                .values(List.of(false, true))
                .initially(false)
                .omitKeyText()
                .build(x + speedWidth + GAP, y, oscWidth, COMPACT_H, Text.empty(),
                    (btn, val) -> {
                        state.set(oscPath, val);
                        rebuildContent();
                        notifyWidgetsChanged();
                    });
            widgets.add(oscToggle);
            
            content.advanceRow();
        }
    }
    
    /**
     * Builds a complete per-axis orbit section with mode-dependent widgets.
     */
    private void buildAxisOrbit(String axis, String basePath, ContentBuilder content) {
        var orbit3d = state.transform() != null ? state.transform().orbit3d() : null;
        AxisMotionConfig config = orbit3d != null ? switch(axis) {
            case "X" -> orbit3d.x();
            case "Y" -> orbit3d.y();
            case "Z" -> orbit3d.z();
            default -> null;
        } : null;
        
        MotionMode currentMode = config != null ? config.mode() : MotionMode.NONE;
        
        // Row 1: Mode dropdown (manual - triggers rebuild) + Radius slider
        buildModeAndRadiusRow(axis, basePath, config, content);
        
        // Row 2: Speed + Phase
        content.sliderPair(
            "Spd", basePath + ".frequency", -3f, 3f,
            "Ph°", basePath + ".phase", 0f, 360f
        );
        
        // Row 3+ (conditional): Secondary params for complex modes
        content.when(currentMode.needsSecondaryParams(), secondary -> {
            buildSecondaryParams(axis, basePath, config, currentMode, secondary);
        });
        
        // EPICYCLIC tilts
        content.when(currentMode == MotionMode.EPICYCLIC, tiltContent -> {
            tiltContent.vec3Row("TiltX", "TiltY", "TiltZ", 
                basePath + ".orbit2Tilt", -180f, 180f, "%.0f°");
        });
    }
    
    /**
     * Builds Mode dropdown + Radius slider row.
     * Mode dropdown is manual because it triggers a full rebuild.
     */
    private void buildModeAndRadiusRow(String axis, String basePath, 
            AxisMotionConfig config, ContentBuilder content) {
        
        int fullWidth = panelWidth - GuiConstants.PADDING * 2;
        int modeWidth = 90;
        int radiusWidth = fullWidth - modeWidth - GAP;
        int x = GuiConstants.PADDING;
        int y = content.getCurrentY();  // Use current Y, not content height
        
        MotionMode currentMode = config != null ? config.mode() : MotionMode.NONE;
        
        // Mode dropdown - manual widget, triggers rebuild on change
        CyclingButtonWidget<MotionMode> modeDropdown = CyclingButtonWidget.<MotionMode>builder(
                mode -> Text.literal(axis + ":" + mode.name()))
            .values(MotionMode.values())
            .initially(currentMode)
            .omitKeyText()
            .build(x, y, modeWidth, COMPACT_H, Text.literal(""),
                (btn, val) -> {
                    state.set(basePath + ".mode", val.name());
                    Logging.GUI.topic("transform").info("Orbit {} mode: {}", axis, val.name());
                    rebuildContent();
                    notifyWidgetsChanged();
                });
        widgets.add(modeDropdown);
        
        // Radius slider - manually positioned next to mode dropdown
        float initialRad = config != null ? config.amplitude() : 1.0f;
        var radSlider = net.cyberpunk042.client.gui.widget.LabeledSlider.builder("Rad")
            .position(x + modeWidth + GAP, y)
            .width(radiusWidth)
            .range(0f, 10f)
            .initial(initialRad)
            .format("%.1f")
            .onChange(v -> state.set(basePath + ".amplitude", v))
            .build();
        widgets.add(radSlider);
        
        // Register binding for radius slider sync
        bindings.add(new Bound<>(
            radSlider,
            () -> state.getFloat(basePath + ".amplitude"),
            v -> state.set(basePath + ".amplitude", v),
            v -> v,
            v -> v,
            radSlider::setValue
        ));
        
        // Advance content Y by one row for manual widgets
        content.advanceRow();
    }
    
    /**
     * Builds mode-dependent secondary parameter widgets.
     */
    private void buildSecondaryParams(String axis, String basePath, 
            AxisMotionConfig config, MotionMode currentMode, ContentBuilder content) {
        
        // Amplitude2 label based on mode
        String amp2Label = switch (currentMode) {
            case HELIX -> "Wave";
            case EPICYCLIC -> "Rad2";
            case WOBBLE -> "Tilt";
            case FLOWER -> "Petal";
            case ORBIT_BOUNCE -> "Bnc";
            case ELLIPTIC -> "Ratio";
            default -> "Amp2";
        };
        
        // Frequency2 label based on mode
        String freq2Label = switch (currentMode) {
            case FLOWER -> "Petals";
            case WOBBLE -> "Rock";
            case HELIX -> "Coils";
            case ORBIT_BOUNCE -> "Bnc/s";
            case EPICYCLIC -> "Spd2";
            case PENDULUM -> "Wbl";
            default -> "Freq2";
        };
        
        // Frequency2 ranges
        float freq2Min = switch (currentMode) {
            case EPICYCLIC, HELIX -> -20f;
            case WOBBLE, PENDULUM -> -5f;
            case ORBIT_BOUNCE -> -3f;
            default -> 0.1f;
        };
        float freq2Max = switch (currentMode) {
            case FLOWER, HELIX, EPICYCLIC -> 20f;
            case WOBBLE, PENDULUM -> 5f;
            case ORBIT_BOUNCE -> 3f;
            default -> 20f;
        };
        
        // Amplitude2 range
        float amp2Min = currentMode == MotionMode.ORBIT_BOUNCE ? -10f : 0f;
        float amp2Max = 10f;
        
        // Determine if we need 3 controls (PENDULUM: Swing, EPICYCLIC: Ph2)
        boolean hasThirdControl = (currentMode == MotionMode.PENDULUM || currentMode == MotionMode.EPICYCLIC);
        
        if (hasThirdControl) {
            // Three controls: Amp2 | Freq2 | Swing/Ph2
            if (currentMode == MotionMode.PENDULUM) {
                content.row()
                    .slider(amp2Label, basePath + ".amplitude2", amp2Min, amp2Max)
                    .slider(freq2Label, basePath + ".frequency2", freq2Min, freq2Max)
                    .slider("Swing°", basePath + ".swingAngle", 1f, 1000f)
                    .end();
            } else {
                // EPICYCLIC: Ph2 (degrees)
                content.row()
                    .slider(amp2Label, basePath + ".amplitude2", amp2Min, amp2Max)
                    .slider(freq2Label, basePath + ".frequency2", freq2Min, freq2Max)
                    .sliderDegrees("Ph2°", basePath + ".phase2")
                    .end();
            }
        } else {
            // Two controls: Amp2 | Freq2
            content.sliderPair(
                amp2Label, basePath + ".amplitude2", amp2Min, amp2Max,
                freq2Label, basePath + ".frequency2", freq2Min, freq2Max
            );
        }
    }
    
    @Override
    protected boolean needsRebuildOnFragmentApply() {
        // Transform presets might change orbit modes
        return true;
    }
    
    @Override
    public void tick() {
        // No tick behavior needed
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use parent's scroll-aware rendering
        renderWithScroll(context, mouseX, mouseY, delta);
    }
    
    public int getHeight() {
        return contentHeight;
    }
    
    public List<ClickableWidget> getWidgets() {
        return new ArrayList<>(widgets);
    }
}
