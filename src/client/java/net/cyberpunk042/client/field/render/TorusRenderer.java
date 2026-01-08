package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.TorusTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.TorusShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders torus (donut) primitives.
 * 
 * @see TorusShape
 * @see TorusTessellator
 */
public final class TorusRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "torus";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof TorusShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("surface", shape.primaryCellType());
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        return TorusTessellator.tessellate(shape, pattern, visibility, wave, time);
    }
    
    /**
     * Emits torus cage: major rings (around hole) + minor rings (around tube).
     * 
     * <p>Uses {@link net.cyberpunk042.visual.fill.TorusCageOptions} for configuration.</p>
     */
    @Override
    protected void emitCage(
            net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumer consumer,
            net.cyberpunk042.client.visual.mesh.Mesh mesh,
            int color,
            int light,
            net.cyberpunk042.visual.fill.FillConfig fill,
            Primitive primitive,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        if (!(primitive.shape() instanceof TorusShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        var cageOptions = fill != null ? fill.torusCage() : null;
        if (cageOptions == null) {
            cageOptions = net.cyberpunk042.visual.fill.TorusCageOptions.DEFAULT;
        }
        
        float majorRadius = shape.majorRadius();  // distance from center to tube center
        float minorRadius = shape.minorRadius();  // tube radius
        int majorRings = cageOptions.majorRings();
        int minorRings = cageOptions.minorRings();
        
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        var builder = net.cyberpunk042.client.visual.mesh.MeshBuilder.lines();
        
        // === MAJOR RINGS (circles around the hole, at different points on tube) ===
        // These are circles in the XZ plane at y positions determined by minor angle
        for (int minor = 0; minor < minorRings; minor++) {
            float minorAngle = (float) (2 * Math.PI * minor / minorRings);
            float tubeY = (float) Math.sin(minorAngle) * minorRadius;
            float tubeOffset = (float) Math.cos(minorAngle) * minorRadius;
            float ringRadius = majorRadius + tubeOffset;
            
            int segments = majorRings * 2;
            for (int i = 0; i < segments; i++) {
                float angle1 = (float) (2 * Math.PI * i / segments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
                
                float x1 = (float) Math.cos(angle1) * ringRadius;
                float z1 = (float) Math.sin(angle1) * ringRadius;
                float x2 = (float) Math.cos(angle2) * ringRadius;
                float z2 = (float) Math.sin(angle2) * ringRadius;
                
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x1, tubeY, z1);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x2, tubeY, z2);
                
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                builder.line(builder.addVertex(v1), builder.addVertex(v2));
            }
        }
        
        // === MINOR RINGS (circles around tube cross-section, at different major angles) ===
        for (int major = 0; major < majorRings; major++) {
            float majorAngle = (float) (2 * Math.PI * major / majorRings);
            float cx = (float) Math.cos(majorAngle) * majorRadius;
            float cz = (float) Math.sin(majorAngle) * majorRadius;
            
            // Minor ring is a circle in the plane perpendicular to the major circle
            int segments = minorRings * 2;
            for (int i = 0; i < segments; i++) {
                float minorAngle1 = (float) (2 * Math.PI * i / segments);
                float minorAngle2 = (float) (2 * Math.PI * (i + 1) / segments);
                
                // Points on the tube cross-section
                float dx1 = (float) Math.cos(minorAngle1) * minorRadius;
                float dy1 = (float) Math.sin(minorAngle1) * minorRadius;
                float dx2 = (float) Math.cos(minorAngle2) * minorRadius;
                float dy2 = (float) Math.sin(minorAngle2) * minorRadius;
                
                // Transform to world coordinates (radial direction from center)
                float x1 = cx + dx1 * (float) Math.cos(majorAngle);
                float z1 = cz + dx1 * (float) Math.sin(majorAngle);
                float x2 = cx + dx2 * (float) Math.cos(majorAngle);
                float z2 = cz + dx2 * (float) Math.sin(majorAngle);
                
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x1, dy1, z1);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x2, dy2, z2);
                
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                builder.line(builder.addVertex(v1), builder.addVertex(v2));
            }
        }
        
        var cageMesh = builder.build();
        var emitter = new net.cyberpunk042.client.visual.render.VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
}
