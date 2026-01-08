package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.gui.state.PipelineTracer;
import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.animation.TravelEffectConfig;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.ColorContext;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.FillMode;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.animation.WaveConfig;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Base implementation for primitive renderers.
 * 
 * <p>Provides common functionality:
 * <ul>
 *   <li>Color resolution from appearance</li>
 *   <li>Fill mode handling (solid, wireframe, cage)</li>
 *   <li>Mesh emission via VertexEmitter</li>
 * </ul>
 * 
 * <p>Subclasses only need to implement {@link #tessellate(Primitive)} to
 * create the mesh for their specific shape.
 * 
 * @see PrimitiveRenderer
 * @see VertexEmitter
 */
public abstract class AbstractPrimitiveRenderer implements PrimitiveRenderer {
    
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver resolver,
            RenderOverrides overrides) {
        
        Logging.FIELD.topic("render").trace("[APR_ENTRY] primitive={}, type={}", 
            primitive.id(), primitive.type());
        
        // === PHASE 1: Tessellate ===
        // Get wave config for CPU deformation (if active)
        Animation anim = primitive.animation();
        WaveConfig wave = (anim != null) ? anim.wave() : null;
        Mesh mesh = tessellate(primitive, wave, time);
        if (mesh == null || mesh.isEmpty()) {
            Logging.FIELD.topic("render").warn(
                "[APR_EXIT] Empty mesh for primitive '{}', skipping", primitive.id());
            return;
        }
        
        Logging.FIELD.topic("render").trace("[APR] Mesh OK: {} vertices", mesh.vertexCount());
        
        // === PHASE 2: Resolve Color (includes animation effects) ===
        Appearance app = primitive.appearance();
        
        // CP4: ALL appearance segments
        if (app != null) {
            PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 4, "prim.color", app.color());
            PipelineTracer.trace(PipelineTracer.A2_ALPHA, 4, "prim.alpha", app.alpha() != null ? String.valueOf(app.alpha().max()) : "1.0");
            PipelineTracer.trace(PipelineTracer.A3_GLOW, 4, "prim.glow", String.valueOf(app.glow()));
            PipelineTracer.trace(PipelineTracer.A4_EMISSIVE, 4, "prim.emissive", String.valueOf(app.emissive()));
            PipelineTracer.trace(PipelineTracer.A5_SATURATION, 4, "prim.saturation", String.valueOf(app.saturation()));
            PipelineTracer.trace(PipelineTracer.A6_SECONDARY_COLOR, 4, "prim.secondary", app.secondaryColor());
        }
        
        // CP4: ALL shape segments
        if (primitive.shape() != null) {
            PipelineTracer.trace(PipelineTracer.S1_SHAPE_TYPE, 4, "prim.shape", primitive.shape().getClass().getSimpleName());
            traceShapeAtCP4(primitive.shape());
        }
        
        // CP4: ALL fill segments (F1-F6)
        if (primitive.fill() != null) {
            var f = primitive.fill();
            PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 4, "prim.fill", f.mode().name());
            PipelineTracer.trace(PipelineTracer.F2_WIRE_THICKNESS, 4, "prim.wire", String.valueOf(f.wireThickness()));
            PipelineTracer.trace(PipelineTracer.F3_DOUBLE_SIDED, 4, "prim.double", String.valueOf(f.doubleSided()));
            PipelineTracer.trace(PipelineTracer.F4_DEPTH_TEST, 4, "prim.depth", String.valueOf(f.depthTest()));
            PipelineTracer.trace(PipelineTracer.F5_DEPTH_WRITE, 4, "prim.depthW", String.valueOf(f.depthWrite()));
            PipelineTracer.trace(PipelineTracer.F6_CAGE_OPTIONS, 4, "prim.cage", f.cage() != null ? "present" : "null");
        }
        
        // CP4: ALL transform segments (T1-T13)
        if (primitive.transform() != null && primitive.transform() != net.cyberpunk042.visual.transform.Transform.IDENTITY) {
            var t = primitive.transform();
            PipelineTracer.trace(PipelineTracer.T1_OFFSET, 4, "prim.offset", t.offset() != null ? t.offset().toString() : "null");
            PipelineTracer.trace(PipelineTracer.T2_ROTATION, 4, "prim.rotation", t.rotation() != null ? t.rotation().toString() : "null");
            PipelineTracer.trace(PipelineTracer.T3_SCALE, 4, "prim.scale", String.valueOf(t.scale()));
            PipelineTracer.trace(PipelineTracer.T4_SCALE_XYZ, 4, "prim.scaleXYZ", t.scaleXYZ() != null ? t.scaleXYZ().toString() : "null");
            PipelineTracer.trace(PipelineTracer.T5_ANCHOR, 4, "prim.anchor", t.anchor() != null ? t.anchor().name() : "null");
            PipelineTracer.trace(PipelineTracer.T6_BILLBOARD, 4, "prim.billboard", t.billboard() != null ? t.billboard().name() : "null");
            PipelineTracer.trace(PipelineTracer.T7_ORBIT, 4, "prim.orbit", t.orbit() != null ? "active" : "null");
            PipelineTracer.trace(PipelineTracer.T8_INHERIT_ROTATION, 4, "prim.inheritRot", String.valueOf(t.inheritRotation()));
            PipelineTracer.trace(PipelineTracer.T9_SCALE_WITH_RADIUS, 4, "prim.scaleWithRad", String.valueOf(t.scaleWithRadius()));
            PipelineTracer.trace(PipelineTracer.T10_FACING, 4, "prim.facing", t.facing() != null ? t.facing().name() : "null");
            PipelineTracer.trace(PipelineTracer.T11_UP_VECTOR, 4, "prim.up", t.up() != null ? t.up().name() : "null");
            if (t.orbit() != null) {
                PipelineTracer.trace(PipelineTracer.T12_ORBIT_RADIUS, 4, "prim.orbitR", String.valueOf(t.orbit().radius()));
                PipelineTracer.trace(PipelineTracer.T13_ORBIT_SPEED, 4, "prim.orbitS", String.valueOf(t.orbit().speed()));
            }
        }
        
        // CP4: ALL visibility segments (V1-V14)
        if (primitive.visibility() != null) {
            var v = primitive.visibility();
            PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 4, "prim.mask", v.mask().name());
            PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 4, "prim.count", String.valueOf(v.count()));
            PipelineTracer.trace(PipelineTracer.V3_MASK_THICKNESS, 4, "prim.thick", String.valueOf(v.thickness()));
            PipelineTracer.trace(PipelineTracer.V4_MASK_OFFSET, 4, "prim.offset", String.valueOf(v.offset()));
            PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 4, "prim.anim", String.valueOf(v.animate()));
            PipelineTracer.trace(PipelineTracer.V6_MASK_ANIM_SPEED, 4, "prim.speed", String.valueOf(v.animSpeed()));
            PipelineTracer.trace(PipelineTracer.V7_MASK_INVERT, 4, "prim.invert", String.valueOf(v.invert()));
            PipelineTracer.trace(PipelineTracer.V8_MASK_FEATHER, 4, "prim.feather", String.valueOf(v.feather()));
            PipelineTracer.trace(PipelineTracer.V9_MASK_DIRECTION, 4, "prim.dir", v.direction());
            PipelineTracer.trace(PipelineTracer.V10_MASK_FALLOFF, 4, "prim.falloff", String.valueOf(v.falloff()));
            PipelineTracer.trace(PipelineTracer.V11_GRADIENT_START, 4, "prim.gradStart", String.valueOf(v.gradientStart()));
            PipelineTracer.trace(PipelineTracer.V12_GRADIENT_END, 4, "prim.gradEnd", String.valueOf(v.gradientEnd()));
            PipelineTracer.trace(PipelineTracer.V13_CENTER_X, 4, "prim.centerX", String.valueOf(v.centerX()));
            PipelineTracer.trace(PipelineTracer.V14_CENTER_Y, 4, "prim.centerY", String.valueOf(v.centerY()));
        }
        
        // CP4: ALL animation segments (reuse anim from line 55)
        if (anim != null) {
            if (anim.spin() != null) {
                PipelineTracer.trace(PipelineTracer.N1_SPIN_SPEED, 4, "prim.spinY", String.valueOf(anim.spin().speedY()));
            }
            if (anim.pulse() != null) {
                PipelineTracer.trace(PipelineTracer.N3_PULSE_SPEED, 4, "prim.pulse", String.valueOf(anim.pulse().speed()));
                PipelineTracer.trace(PipelineTracer.N4_PULSE_SCALE, 4, "prim.pScale", String.valueOf(anim.pulse().scale()));
                PipelineTracer.trace(PipelineTracer.N5_PULSE_MODE, 4, "prim.pMode", anim.pulse().mode().name());
            }
            if (anim.alphaPulse() != null) {
                PipelineTracer.trace(PipelineTracer.N6_ALPHA_PULSE_SPEED, 4, "prim.aSpeed", String.valueOf(anim.alphaPulse().speed()));
                PipelineTracer.trace(PipelineTracer.N7_ALPHA_PULSE_MIN, 4, "prim.aMin", String.valueOf(anim.alphaPulse().min()));
                PipelineTracer.trace(PipelineTracer.N8_ALPHA_PULSE_MAX, 4, "prim.aMax", String.valueOf(anim.alphaPulse().max()));
            }
            if (anim.wave() != null) {
                PipelineTracer.trace(PipelineTracer.N9_WAVE_SPEED, 4, "prim.wFreq", String.valueOf(anim.wave().frequency()));
                PipelineTracer.trace(PipelineTracer.N10_WAVE_AMPLITUDE, 4, "prim.wAmp", String.valueOf(anim.wave().amplitude()));
            }
            if (anim.wobble() != null) {
                PipelineTracer.trace(PipelineTracer.N11_WOBBLE_SPEED, 4, "prim.wobble", String.valueOf(anim.wobble().speed()));
            }
            if (anim.colorCycle() != null) {
                PipelineTracer.trace(PipelineTracer.N12_COLOR_CYCLE, 4, "prim.cycle", anim.colorCycle().isActive() ? "active" : "inactive");
                PipelineTracer.trace(PipelineTracer.N16_COLOR_CYCLE_SPEED, 4, "prim.cycleSpeed", String.valueOf(anim.colorCycle().speed()));
            }
        }
        
        int color = resolveColor(primitive, resolver, overrides, time);
        
        // CP5: Renderer resolved value
        PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 5, "resolved", "0x" + Integer.toHexString(color));
        
        // === PHASE 2.5: Apply Animated Mask Alpha ===
        // Note: For animated masks, we apply alpha modulation at render time
        // since tessellation is cached. This affects overall primitive alpha.
        if (primitive.visibility() != null && primitive.visibility().animate()) {
            // Apply average mask alpha based on animation time
            // This is an approximation - true per-vertex mask would require re-tessellation
            float maskAlpha = 0.5f + 0.5f * (float)Math.sin(time * primitive.visibility().animSpeed() * 0.1f);
            color = net.cyberpunk042.visual.color.ColorMath.multiplyAlpha(color, maskAlpha);
        }
        
        // === PHASE 3: Get Wave Config for Vertex Displacement ===
        Animation animation = primitive.animation();
        net.cyberpunk042.visual.animation.WaveConfig waveConfig = 
            (animation != null && animation.hasWave()) ? animation.wave() : null;
        
        // === PHASE 4: Emit Based on Fill Mode ===
        FillConfig fill = primitive.fill();
        FillMode mode = fill != null ? fill.mode() : FillMode.SOLID;
        
        // CP5-CP6: Fill mode and emitter
        PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 5, "renderer.mode", mode.name());
        PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 6, "emitColor", "0x" + Integer.toHexString(color));
        
        switch (mode) {
            case SOLID -> {
                PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 6, "emit", "SOLID");
                // Create ColorContext for per-vertex coloring if needed
                Appearance appearance = primitive.appearance();
                ColorContext colorCtx = null;
                if (appearance != null && appearance.isPerVertex()) {
                    // For MESH_* modes, we need UNBLENDED primary and secondary colors
                    // The `color` variable is already blended by resolveColor(), so we resolve fresh
                    int primaryColor = color; // fallback
                    int secondaryColor = color; // fallback
                    
                    // Resolve primary color (unblended)
                    String primaryRef = appearance.color();
                    if (primaryRef != null && resolver != null) {
                        primaryColor = resolver.resolve(primaryRef);
                    }
                    
                    // Resolve secondary color (unblended)
                    String secondaryRef = appearance.secondaryColor();
                    if (secondaryRef != null && resolver != null) {
                        secondaryColor = resolver.resolve(secondaryRef);
                    } else if (secondaryRef != null && secondaryRef.startsWith("#")) {
                        try {
                            secondaryColor = 0xFF000000 | Integer.parseInt(secondaryRef.substring(1), 16);
                        } catch (NumberFormatException ignored) {}
                    } else {
                        // No secondary specified - use primary
                        secondaryColor = primaryColor;
                    }
                    
                    // Get shape dimensions for gradient calculation
                    float shapeRadius = getShapeRadius(primitive);
                    float shapeHeight = getShapeHeight(primitive);
                    colorCtx = ColorContext.from(appearance, primaryColor, secondaryColor, time, shapeRadius, shapeHeight);
                }
                emitSolid(matrices, consumer, mesh, color, light, waveConfig, time, colorCtx, animation, primitive.shape());
            }
            case WIREFRAME -> emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            case CAGE -> emitCage(matrices, consumer, mesh, color, light, fill, primitive, waveConfig, time);
            case POINTS -> emitPoints(matrices, consumer, mesh, color, light, fill, waveConfig, time);
        }
        
        Logging.FIELD.topic("render").trace("[APR] DONE: Rendered {} primitive '{}': vertices={}, mode={}",
            shapeType(), primitive.id(), mesh.vertexCount(), mode);
    }
    
    // =========================================================================
    // Abstract Methods - Subclasses Implement
    // =========================================================================
    
    /**
     * Tessellates the primitive's shape into a mesh.
     * 
     * @param primitive The primitive containing the shape
     * @param wave Wave config for CPU deformation (may be null or inactive)
     * @param time Current time in ticks for wave animation
     * @return The tessellated mesh, or null if invalid
     */
    protected abstract Mesh tessellate(Primitive primitive, WaveConfig wave, float time);
    
    /**
     * Traces shape-specific values at CP4.
     */
    private void traceShapeAtCP4(net.cyberpunk042.visual.shape.Shape shape) {
        if (shape instanceof net.cyberpunk042.visual.shape.SphereShape s) {
            PipelineTracer.trace(PipelineTracer.S2_RADIUS, 4, "sphere.r", String.valueOf(s.radius()));
            PipelineTracer.trace(PipelineTracer.S3_LAT_STEPS, 4, "sphere.lat", String.valueOf(s.latSteps()));
            PipelineTracer.trace(PipelineTracer.S4_LON_STEPS, 4, "sphere.lon", String.valueOf(s.lonSteps()));
            PipelineTracer.trace(PipelineTracer.S5_ALGORITHM, 4, "sphere.algo", s.algorithm().name());
        } else if (shape instanceof net.cyberpunk042.visual.shape.RingShape r) {
            PipelineTracer.trace(PipelineTracer.S6_INNER_RADIUS, 4, "ring.inner", String.valueOf(r.innerRadius()));
            PipelineTracer.trace(PipelineTracer.S7_OUTER_RADIUS, 4, "ring.outer", String.valueOf(r.outerRadius()));
            PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 4, "ring.h", String.valueOf(r.height()));
            PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 4, "ring.seg", String.valueOf(r.segments()));
        } else if (shape instanceof net.cyberpunk042.visual.shape.CylinderShape c) {
            PipelineTracer.trace(PipelineTracer.S2_RADIUS, 4, "cyl.r", String.valueOf(c.radius()));
            PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 4, "cyl.h", String.valueOf(c.height()));
            PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 4, "cyl.seg", String.valueOf(c.segments()));
        } else if (shape instanceof net.cyberpunk042.visual.shape.PrismShape p) {
            PipelineTracer.trace(PipelineTracer.S2_RADIUS, 4, "prism.r", String.valueOf(p.radius()));
            PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 4, "prism.h", String.valueOf(p.height()));
            PipelineTracer.trace(PipelineTracer.S10_SIDES, 4, "prism.sides", String.valueOf(p.sides()));
        } else if (shape instanceof net.cyberpunk042.visual.shape.PolyhedronShape poly) {
            PipelineTracer.trace(PipelineTracer.S2_RADIUS, 4, "poly.r", String.valueOf(poly.radius()));
            PipelineTracer.trace(PipelineTracer.S11_POLY_TYPE, 4, "poly.type", poly.polyType().name());
        }
    }
    
    // =========================================================================
    // Color Resolution
    // =========================================================================
    
    /**
     * Resolves the render color from appearance, animation, and overrides.
     * 
     * <p>Color resolution priority:
     * <ol>
     *   <li>Override color (from RenderOverrides)</li>
     *   <li>Animation color cycle (if active)</li>
     *   <li>Appearance color reference (resolved via ColorResolver)</li>
     *   <li>Direct hex color in appearance</li>
     *   <li>Default white</li>
     * </ol>
     */
    protected int resolveColor(Primitive primitive, ColorResolver resolver, RenderOverrides overrides, float time) {
        // Check for color override first
        if (overrides != null && overrides.colorOverride() != null) {
            return overrides.colorOverride();
        }
        
        int baseColor;
        
        // Check for animation color cycle - uses ColorHelper.lerp for smooth blending
        Animation animation = primitive.animation();
        if (animation != null && animation.hasColorCycle()) {
            baseColor = AnimationApplier.getColorCycle(animation.colorCycle(), time);
        } else {
            // Normal color resolution from appearance
            Appearance appearance = primitive.appearance();
            if (appearance == null) {
                return 0xFFFFFFFF; // Default white
            }
            
            // Resolve color reference (e.g., "primary" → actual color)
            String colorRef = appearance.color();
            
            if (colorRef != null && resolver != null) {
                baseColor = resolver.resolve(colorRef);
                Logging.FIELD.topic("color").debug("Resolved via resolver: {} → #{}", colorRef, Integer.toHexString(baseColor));
            } else if (colorRef != null && colorRef.startsWith("#")) {
                // Direct hex color
                baseColor = parseHexColor(colorRef);
                Logging.FIELD.topic("color").debug("Parsed hex directly: {} → #{}", colorRef, Integer.toHexString(baseColor));
            } else if (colorRef != null && colorRef.startsWith("0x")) {
                baseColor = Integer.parseUnsignedInt(colorRef.substring(2), 16);
                Logging.FIELD.topic("color").debug("Parsed 0x format: {} → #{}", colorRef, Integer.toHexString(baseColor));
            } else {
                baseColor = 0xFFFFFFFF;
                Logging.FIELD.topic("color").debug("No valid colorRef (was: {}), using white", colorRef);
            }
            
            // Apply appearance color modifiers (saturation, brightness, hueShift)
            baseColor = applyAppearanceModifiers(baseColor, appearance, resolver);
            
            // Apply CYCLING color mode (animates through color spectrum over time)
            if (appearance.isCycling()) {
                // Cycling mode: interpolates through ColorSet based on time + timePhase
                float t = ((time / 20f) + appearance.timePhase()) % 1f;
                if (t < 0) t += 1f;
                baseColor = appearance.effectiveColorSet().interpolateSpectrum(t);
            }
            
            // TODO: RANDOM and MESH_* modes require per-vertex color calculation
            // These will be implemented in tessellators, not here
        }
        
        // Apply alpha from appearance (AlphaRange - use max value)
        Appearance appearance = primitive.appearance();
        float alpha = (appearance != null && appearance.alpha() != null) 
            ? appearance.alpha().max() : 1.0f;
        
        // Apply alpha pulse animation (from AlphaPulseConfig)
        if (animation != null && animation.hasAlphaPulse()) {
            alpha *= AnimationApplier.getAlphaPulse(animation.alphaPulse(), time);
        }
        
        // Apply pulse alpha (when PulseConfig.mode == ALPHA)
        if (animation != null && animation.hasPulse()) {
            alpha *= AnimationApplier.getPulseAlpha(animation.pulse(), time);
        }
        
        if (overrides != null) {
            alpha *= overrides.alphaMultiplier();
        }
        
        // Combine base color with alpha
        int a = (int) (alpha * 255) & 0xFF;
        return (baseColor & 0x00FFFFFF) | (a << 24);
    }
    
    /**
     * @deprecated Use {@link #resolveColor(Primitive, ColorResolver, RenderOverrides, float)} instead
     */
    @Deprecated
    protected int resolveColor(Primitive primitive, ColorResolver resolver, RenderOverrides overrides) {
        return resolveColor(primitive, resolver, overrides, 0f);
    }
    
    /**
     * Parses a hex color string (#RRGGBB or #AARRGGBB).
     */
    protected int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return 0xFFFFFFFF;
        
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (cleaned.length() == 6) {
                // RGB only, add full alpha
                return 0xFF000000 | Integer.parseUnsignedInt(cleaned, 16);
            } else if (cleaned.length() == 8) {
                // ARGB
                return Integer.parseUnsignedInt(cleaned, 16);
            }
        } catch (NumberFormatException e) {
            Logging.FIELD.topic("render").warn("Invalid hex color: {}", hex);
        }
        return 0xFFFFFFFF;
    }
    
    /**
     * Applies appearance color modifiers (saturation, brightness, hueShift, secondaryColor).
     * 
     * @param baseColor Base ARGB color
     * @param appearance Appearance config
     * @param resolver Color resolver for secondary color references
     * @return Modified ARGB color
     */
    protected int applyAppearanceModifiers(int baseColor, Appearance appearance, ColorResolver resolver) {
        int color = baseColor;
        
        // Apply saturation (1.0 = no change, 0.0 = grayscale, 2.0 = hyper-saturated)
        if (appearance.saturation() != 1.0f) {
            color = net.cyberpunk042.visual.color.ColorMath.multiplySaturation(color, appearance.saturation());
        }
        
        // Apply brightness (1.0 = no change)
        if (appearance.brightness() != 1.0f) {
            color = net.cyberpunk042.visual.color.ColorMath.multiplyBrightness(color, appearance.brightness());
        }
        
        // Apply hue shift (degrees)
        if (appearance.hueShift() != 0.0f) {
            color = net.cyberpunk042.visual.color.ColorMath.shiftHue(color, appearance.hueShift());
        }
        
        // Apply secondary color blending
        if (appearance.hasSecondaryColor()) {
            String secondaryRef = appearance.secondaryColor();
            int secondaryColor = 0xFFFFFFFF;
            
            if (resolver != null) {
                secondaryColor = resolver.resolve(secondaryRef);
            } else if (secondaryRef != null && secondaryRef.startsWith("#")) {
                secondaryColor = parseHexColor(secondaryRef);
            }
            
            color = net.cyberpunk042.visual.color.ColorMath.blend(color, secondaryColor, appearance.colorBlend());
            
            Logging.FIELD.topic("render").trace(
                "Applied secondary color blend: {} @ {:.2f}", secondaryRef, appearance.colorBlend());
        }
        
        return color;
    }
    
    /**
     * Gets the radius of a primitive's shape for gradient calculation.
     */
    protected float getShapeRadius(Primitive primitive) {
        if (primitive.shape() == null) return 1f;
        var shape = primitive.shape();
        if (shape instanceof net.cyberpunk042.visual.shape.SphereShape s) return s.radius();
        if (shape instanceof net.cyberpunk042.visual.shape.CylinderShape c) return c.radius();
        if (shape instanceof net.cyberpunk042.visual.shape.RingShape r) return r.outerRadius();
        if (shape instanceof net.cyberpunk042.visual.shape.PrismShape p) return p.radius();
        if (shape instanceof net.cyberpunk042.visual.shape.CapsuleShape c) return c.radius();
        if (shape instanceof net.cyberpunk042.visual.shape.ConeShape c) return c.bottomRadius();
        if (shape instanceof net.cyberpunk042.visual.shape.TorusShape t) return t.majorRadius() + t.minorRadius();
        if (shape instanceof net.cyberpunk042.visual.shape.PolyhedronShape p) return p.radius();
        return 1f;
    }
    
    /**
     * Gets the height of a primitive's shape for gradient calculation.
     */
    protected float getShapeHeight(Primitive primitive) {
        if (primitive.shape() == null) return 1f;
        var shape = primitive.shape();
        if (shape instanceof net.cyberpunk042.visual.shape.CylinderShape c) return c.height();
        if (shape instanceof net.cyberpunk042.visual.shape.RingShape r) return r.height();
        if (shape instanceof net.cyberpunk042.visual.shape.PrismShape p) return p.height();
        if (shape instanceof net.cyberpunk042.visual.shape.CapsuleShape c) return c.height();
        if (shape instanceof net.cyberpunk042.visual.shape.ConeShape c) return c.height();
        if (shape instanceof net.cyberpunk042.visual.shape.JetShape j) return j.length();
        if (shape instanceof net.cyberpunk042.visual.shape.SphereShape s) return s.radius() * 2;
        return 1f;
    }
    
    // =========================================================================
    // Emission Methods
    // =========================================================================
    
    /**
     * Emits mesh as solid triangles/quads with optional wave animation and travel effect.
     * 
     * @param matrices Transform stack
     * @param consumer Vertex output
     * @param mesh Source mesh
     * @param color ARGB color (used if colorCtx is null or not per-vertex)
     * @param light Light level
     * @param waveConfig Wave animation config (null for no wave)
     * @param time Current time for wave animation
     * @param colorCtx ColorContext for per-vertex coloring (null for uniform color)
     * @param animation Animation config for travel effects (null for no travel effect)
     * @param shape Shape for bounds calculation (used for travel effect on non-spherical shapes)
     */
    protected void emitSolid(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time,
            ColorContext colorCtx,
            Animation animation,
            net.cyberpunk042.visual.shape.Shape shape) {
        
        // CP7: Final vertex emission - ALL segments that reach the vertex stage
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int vertexCount = mesh.vertexCount();
        
        // Appearance segments - trace actual vertex color
        PipelineTracer.trace(PipelineTracer.A1_PRIMARY_COLOR, 7, "emitted", "0x" + Integer.toHexString(color));
        PipelineTracer.trace(PipelineTracer.A2_ALPHA, 7, "emitted", String.valueOf((float)a / 255f));
        PipelineTracer.trace(PipelineTracer.A3_GLOW, 7, "emitted", "post-process");
        PipelineTracer.trace(PipelineTracer.A4_EMISSIVE, 7, "emitted", "post-process");
        PipelineTracer.trace(PipelineTracer.A5_SATURATION, 7, "emitted", "applied");
        PipelineTracer.trace(PipelineTracer.A6_SECONDARY_COLOR, 7, "emitted", "blend");
        
        // Shape segments - emitted as mesh
        PipelineTracer.trace(PipelineTracer.S1_SHAPE_TYPE, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S2_RADIUS, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S3_LAT_STEPS, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S4_LON_STEPS, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S5_ALGORITHM, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S6_INNER_RADIUS, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S7_OUTER_RADIUS, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S8_HEIGHT, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S9_SEGMENTS, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.S10_SIDES, 7, "emitted", "mesh");
        
        // Fill segments - mode applied
        PipelineTracer.trace(PipelineTracer.F1_FILL_MODE, 7, "emitted", "applied");
        PipelineTracer.trace(PipelineTracer.F2_WIRE_THICKNESS, 7, "emitted", "applied");
        PipelineTracer.trace(PipelineTracer.F3_DOUBLE_SIDED, 7, "emitted", "applied");
        PipelineTracer.trace(PipelineTracer.F4_DEPTH_TEST, 7, "emitted", "applied");
        PipelineTracer.trace(PipelineTracer.F5_DEPTH_WRITE, 7, "emitted", "applied");
        PipelineTracer.trace(PipelineTracer.F6_CAGE_OPTIONS, 7, "emitted", "applied");
        
        // Transform segments - applied via matrix
        PipelineTracer.trace(PipelineTracer.T1_OFFSET, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T2_ROTATION, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T3_SCALE, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T4_SCALE_XYZ, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T5_ANCHOR, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T6_BILLBOARD, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T7_ORBIT, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T8_INHERIT_ROTATION, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T9_SCALE_WITH_RADIUS, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T10_FACING, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T11_UP_VECTOR, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T12_ORBIT_RADIUS, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.T13_ORBIT_SPEED, 7, "emitted", "matrix");
        
        // Visibility segments - applied to mesh
        PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V3_MASK_THICKNESS, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V4_MASK_OFFSET, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V6_MASK_ANIM_SPEED, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V7_MASK_INVERT, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V8_MASK_FEATHER, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V9_MASK_DIRECTION, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V10_MASK_FALLOFF, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V11_GRADIENT_START, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V12_GRADIENT_END, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V13_CENTER_X, 7, "emitted", "mesh");
        PipelineTracer.trace(PipelineTracer.V14_CENTER_Y, 7, "emitted", "mesh");
        
        // Animation segments - applied via matrix/color
        PipelineTracer.trace(PipelineTracer.N1_SPIN_SPEED, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.N2_SPIN_AXIS, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.N3_PULSE_SPEED, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.N4_PULSE_SCALE, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.N5_PULSE_MODE, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.N6_ALPHA_PULSE_SPEED, 7, "emitted", "alpha");
        PipelineTracer.trace(PipelineTracer.N7_ALPHA_PULSE_MIN, 7, "emitted", "alpha");
        PipelineTracer.trace(PipelineTracer.N8_ALPHA_PULSE_MAX, 7, "emitted", "alpha");
        PipelineTracer.trace(PipelineTracer.N9_WAVE_SPEED, 7, "emitted", "wave");
        PipelineTracer.trace(PipelineTracer.N10_WAVE_AMPLITUDE, 7, "emitted", "wave");
        PipelineTracer.trace(PipelineTracer.N11_WOBBLE_SPEED, 7, "emitted", "matrix");
        PipelineTracer.trace(PipelineTracer.N12_COLOR_CYCLE, 7, "emitted", "color");
        PipelineTracer.trace(PipelineTracer.N16_COLOR_CYCLE_SPEED, 7, "emitted", "color");
        
        VertexEmitter emitter = new VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        
        // Apply per-vertex coloring if context provided
        if (colorCtx != null) {
            emitter.colorContext(colorCtx);
        }
        
        // Apply wave animation if configured
        if (waveConfig != null && waveConfig.isActive()) {
            emitter.wave(waveConfig, time);
        }
        
        // Apply travel effect if configured (general travel for any shape)
        if (animation != null && animation.hasTravelEffect()) {
            TravelEffectConfig travelEffect = animation.travelEffect();
            // Calculate travel phase from time and speed
            float travelSpeed = travelEffect.speed();
            float travelPhase = (time * travelSpeed * 0.05f) % 1f;  // 0.05 = speed scaling factor
            
            // Check if shape is spherical (use normal-based) or not (use position-based)
            boolean isSphericalShape = shape instanceof net.cyberpunk042.visual.shape.SphereShape;
            
            if (isSphericalShape) {
                // Spheres: use normal-based t calculation
                emitter.travelEffect(travelEffect, travelPhase);
            } else if (shape instanceof net.cyberpunk042.visual.shape.JetShape jet) {
                // Jet shapes: use position-based with shape-level minAlpha gradient
                org.joml.Vector3f bounds = shape.getBounds();
                float[] boundsMin = new float[] { -bounds.x / 2, -bounds.y / 2, -bounds.z / 2 };
                float[] boundsMax = new float[] { bounds.x / 2, bounds.y / 2, bounds.z / 2 };
                emitter.travelEffect(travelEffect, travelPhase, boundsMin, boundsMax,
                    jet.baseMinAlpha(), jet.tipMinAlpha());
            } else if (shape instanceof net.cyberpunk042.visual.shape.KamehamehaShape kamehameha) {
                // Kamehameha shapes: use position-based with beam minAlpha gradient
                org.joml.Vector3f bounds = shape.getBounds();
                float[] boundsMin = new float[] { -bounds.x / 2, -bounds.y / 2, -bounds.z / 2 };
                float[] boundsMax = new float[] { bounds.x / 2, bounds.y / 2, bounds.z / 2 };
                // Use beam alpha gradient (orb is at base, tip is at end of beam)
                emitter.travelEffect(travelEffect, travelPhase, boundsMin, boundsMax,
                    kamehameha.beamBaseMinAlpha(), kamehameha.beamTipMinAlpha());
            } else if (shape != null) {
                // Other non-spherical shapes: use position-based t calculation with bounds
                org.joml.Vector3f bounds = shape.getBounds();
                // Bounds are half-extents from center, so full extent is -half to +half
                float[] boundsMin = new float[] { -bounds.x / 2, -bounds.y / 2, -bounds.z / 2 };
                float[] boundsMax = new float[] { bounds.x / 2, bounds.y / 2, bounds.z / 2 };
                emitter.travelEffect(travelEffect, travelPhase, boundsMin, boundsMax);
            } else {
                // Fallback to normal-based
                emitter.travelEffect(travelEffect, travelPhase);
            }
        }
        
        emitter.emit(mesh);
    }
    
    /**
     * Emits mesh as wireframe lines with optional wave animation.
     */
    protected void emitWireframe(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        float thickness = fill != null ? fill.wireThickness() : 1.0f;
        
        // For wireframe with wave, we need instance-based emitter
        if (waveConfig != null && waveConfig.isActive()) {
            VertexEmitter emitter = new VertexEmitter(matrices, consumer);
            emitter.color(color).light(light).wave(waveConfig, time);
            emitter.emitWireframe(mesh, thickness);
        } else {
            VertexEmitter.emitWireframe(
                matrices.peek(), consumer, mesh, color, thickness, light);
        }
    }
    
    /**
     * Emits mesh as cage (lat/lon grid for spheres, edges for polyhedra).
     * Default implementation falls back to wireframe.
     */
    protected void emitCage(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill,
            Primitive primitive,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        // Default: same as wireframe
        // Subclasses can override for shape-specific cage rendering
        emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
    }
    
    /**
     * Emits mesh vertices as points using tiny billboarded quads with optional wave animation.
     * 
     * <p>Minecraft/OpenGL doesn't support GL_POINTS natively for our use case,
     * so we fake it with tiny camera-facing quads (2 triangles each).
     * 
     * @param matrices Transform stack
     * @param consumer Vertex output
     * @param mesh Source mesh (only vertices used)
     * @param color ARGB color
     * @param light Light value
     * @param fill Fill config containing pointSize
     * @param waveConfig Wave animation config (null for no wave)
     * @param time Current time for wave animation
     */
    protected void emitPoints(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        // Get point size from fill config (convert from GUI units to rendering units)
        float pointSize = fill != null ? fill.pointSize() / 100f : 0.02f;
        emitPoints(matrices, consumer, mesh, color, light, pointSize, waveConfig, time);
    }
    
    /**
     * Emits mesh vertices as points with configurable size and optional wave animation.
     */
    protected void emitPoints(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            float pointSize,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        if (mesh.vertices().isEmpty()) return;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        
        // Extract ARGB components
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        float half = pointSize / 2f;
        boolean hasWave = waveConfig != null && waveConfig.isActive();
        
        // For each vertex, emit a tiny billboarded quad
        for (Vertex v : mesh.vertices()) {
            float x = v.x();
            float y = v.y();
            float z = v.z();
            
            // Apply wave displacement if configured
            if (hasWave) {
                float[] displaced = AnimationApplier.applyWaveToVertex(waveConfig, x, y, z, time);
                x = displaced[0];
                y = displaced[1];
                z = displaced[2];
            }
            
            // Create a camera-facing quad (billboard)
            // Using XY plane offset - will be approximately billboarded for most views
            // For true billboarding, would need camera direction
            
            // Quad corners (tiny square centered on vertex)
            //  2---3
            //  |   |
            //  0---1
            
            // Triangle 1: 0-1-2
            emitBillboardVertex(consumer, matrix, normalMatrix, x - half, y - half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x + half, y - half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x - half, y + half, z, 0, 0, 1, r, g, b, a, light);
            
            // Triangle 2: 1-3-2
            emitBillboardVertex(consumer, matrix, normalMatrix, x + half, y - half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x + half, y + half, z, 0, 0, 1, r, g, b, a, light);
            emitBillboardVertex(consumer, matrix, normalMatrix, x - half, y + half, z, 0, 0, 1, r, g, b, a, light);
        }
        
        Logging.FIELD.topic("render").trace("Emitted {} points as billboarded quads", mesh.vertices().size());
    }
    
    /**
     * Helper method to emit a single vertex for billboarded quads.
     * 
     * @param consumer Vertex consumer
     * @param matrix Position transformation matrix
     * @param normalMatrix Normal transformation matrix
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param nx Normal X
     * @param ny Normal Y
     * @param nz Normal Z
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @param a Alpha component (0-255)
     * @param light Light level
     */
    private void emitBillboardVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normalMatrix,
            float x, float y, float z,
            float nx, float ny, float nz,
            int r, int g, int b, int a,
            int light) {
        
        // Transform position
        org.joml.Vector4f pos = new org.joml.Vector4f(x, y, z, 1.0f);
        pos.mul(matrix);
        
        // Transform normal
        org.joml.Vector3f normal = new org.joml.Vector3f(nx, ny, nz);
        normal.mul(normalMatrix).normalize();
        
        // Emit vertex
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .texture(0, 0)  // No texture for points
            .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(normal.x(), normal.y(), normal.z());
    }
}

