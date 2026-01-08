package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.gui.state.PipelineTracer;
import net.cyberpunk042.client.render.util.BeaconBeamRenderer;
import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.field.BeamConfig;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.color.ColorResolver;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renders beam effects for field definitions using beacon-style rendering.
 * 
 * <p>Uses {@link BeaconBeamRenderer} for simple, efficient beam rendering
 * with scrolling texture effect. Supports:
 * <ul>
 *   <li>Inner/outer radius for beam thickness</li>
 *   <li>Color (resolved via ColorResolver)</li>
 *   <li>Height</li>
 *   <li>Glow (affects alpha)</li>
 *   <li>Optional pulse animation</li>
 * </ul>
 * 
 * @see BeamConfig
 * @see BeaconBeamRenderer
 */
public final class BeamRenderer {
    
    private BeamRenderer() {}
    
    /**
     * Renders a beam effect for a field.
     * 
     * @param matrices Matrix stack (positioned at field center)
     * @param consumers Vertex consumer provider
     * @param beam Beam configuration
     * @param resolver Color resolver
     * @param worldTime World time for animation
     * @param tickDelta Partial tick for smooth animation
     * @param alpha Overall alpha (0-1)
     */
    public static void render(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            BeamConfig beam,
            ColorResolver resolver,
            long worldTime,
            float tickDelta,
            float alpha) {
        
        if (!beam.enabled() || alpha <= 0.01f) {
            return;
        }
        
        Logging.FIELD.topic("render").debug("[BEAM] Rendering: inner={}, outer={}, height={}, color={}",
            beam.innerRadius(), beam.outerRadius(), beam.height(), beam.color());
        
        // CP5: Beam values received by renderer
        PipelineTracer.trace(PipelineTracer.B1_BEAM_ENABLED, 5, "render", String.valueOf(beam.enabled()));
        PipelineTracer.trace(PipelineTracer.B2_BEAM_INNER_RADIUS, 5, "render", String.valueOf(beam.innerRadius()));
        PipelineTracer.trace(PipelineTracer.B3_BEAM_OUTER_RADIUS, 5, "render", String.valueOf(beam.outerRadius()));
        PipelineTracer.trace(PipelineTracer.B4_BEAM_COLOR, 5, "render", beam.color());
        PipelineTracer.trace(PipelineTracer.B5_BEAM_HEIGHT, 5, "render", String.valueOf(beam.height()));
        PipelineTracer.trace(PipelineTracer.B6_BEAM_GLOW, 5, "render", String.valueOf(beam.glow()));
        PipelineTracer.trace(PipelineTracer.B7_BEAM_PULSE, 5, "render", beam.pulse() != null ? "active" : "null");
        
        // Resolve colors (same color for inner/outer, inner is just more transparent)
        int outerColor = resolveBeamColor(beam.color(), resolver);
        int innerColor = outerColor; // Same base color
        
        // Apply alpha and glow
        float effectiveAlpha = alpha;
        if (beam.glow() > 0) {
            effectiveAlpha = Math.min(1.0f, effectiveAlpha + beam.glow() * 0.3f);
        }
        
        // Apply pulse animation if present
        float pulseScale = 1.0f;
        if (beam.pulse() != null && beam.pulse().isActive()) {
            pulseScale = AnimationApplier.getPulseValue(beam.pulse(), worldTime + tickDelta);
            effectiveAlpha *= AnimationApplier.getPulseAlpha(beam.pulse(), worldTime + tickDelta);
        }
        
        // Apply alpha to colors
        int a = (int) (effectiveAlpha * 255) & 0xFF;
        outerColor = (outerColor & 0x00FFFFFF) | (a << 24);
        innerColor = (innerColor & 0x00FFFFFF) | ((a * 3 / 4) << 24); // Inner slightly transparent
        
        // Scale radii by pulse
        float innerR = beam.innerRadius() * pulseScale;
        float outerR = beam.outerRadius() * pulseScale;
        
        // Height: 0 or negative means infinite (pierce the sky)
        int height;
        if (beam.height() <= 0) {
            height = 1024; // Effectively infinite - extends well beyond sky
        } else {
            height = (int) beam.height();
        }
        
        // CP6: Beam values to emitter
        PipelineTracer.trace(PipelineTracer.B1_BEAM_ENABLED, 6, "emit", "rendering");
        PipelineTracer.trace(PipelineTracer.B2_BEAM_INNER_RADIUS, 6, "emit", String.valueOf(innerR));
        PipelineTracer.trace(PipelineTracer.B3_BEAM_OUTER_RADIUS, 6, "emit", String.valueOf(outerR));
        PipelineTracer.trace(PipelineTracer.B4_BEAM_COLOR, 6, "emit", "0x" + Integer.toHexString(outerColor));
        PipelineTracer.trace(PipelineTracer.B5_BEAM_HEIGHT, 6, "emit", String.valueOf(height));
        PipelineTracer.trace(PipelineTracer.B6_BEAM_GLOW, 6, "emit", "applied");
        PipelineTracer.trace(PipelineTracer.B7_BEAM_PULSE, 6, "emit", "scale=" + pulseScale);
        
        // Use BeaconBeamRenderer for actual rendering
        BeaconBeamRenderer.render(
            matrices,
            consumers,
            innerR,
            outerR,
            height,
            worldTime,
            tickDelta,
            innerColor,
            outerColor
        );
        
        // CP7: Beam vertices emitted
        PipelineTracer.trace(PipelineTracer.B1_BEAM_ENABLED, 7, "vtx", "complete");
        PipelineTracer.trace(PipelineTracer.B2_BEAM_INNER_RADIUS, 7, "vtx", "complete");
        PipelineTracer.trace(PipelineTracer.B3_BEAM_OUTER_RADIUS, 7, "vtx", "complete");
        PipelineTracer.trace(PipelineTracer.B4_BEAM_COLOR, 7, "vtx", "complete");
        PipelineTracer.trace(PipelineTracer.B5_BEAM_HEIGHT, 7, "vtx", "complete");
        PipelineTracer.trace(PipelineTracer.B6_BEAM_GLOW, 7, "vtx", "complete");
        PipelineTracer.trace(PipelineTracer.B7_BEAM_PULSE, 7, "vtx", "complete");
    }
    
    /**
     * Resolves a beam color reference.
     */
    private static int resolveBeamColor(String colorRef, ColorResolver resolver) {
        if (colorRef == null || colorRef.isEmpty()) {
            return 0xFFFFFFFF; // Default white
        }
        
        if (resolver != null) {
            return resolver.resolve(colorRef);
        }
        
        // Try parsing as hex
        if (colorRef.startsWith("#")) {
            try {
                String hex = colorRef.substring(1);
                if (hex.length() == 6) {
                    return 0xFF000000 | Integer.parseUnsignedInt(hex, 16);
                } else if (hex.length() == 8) {
                    return Integer.parseUnsignedInt(hex, 16);
                }
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        
        return 0xFFFFFFFF;
    }
}
