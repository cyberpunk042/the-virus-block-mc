package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.RingTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.RingShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders ring primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see RingShape
 * @see RingTessellator
 */
public final class RingRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "ring";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof RingShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config with CellType validation
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Try to resolve pattern - if it fails, continue without pattern
            pattern = arrangement.resolvePattern("surface", shape.primaryCellType());
            // Don't return null if pattern fails - render without pattern instead
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with full config including wave
        return RingTessellator.tessellate(shape, pattern, visibility, wave, time);
    }
    
    /**
     * Emits ring cage: radial lines spanning inner to outer + concentric rings.
     * 
     * <p>Uses {@link net.cyberpunk042.visual.fill.RingCageOptions} for configuration.</p>
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
        
        if (!(primitive.shape() instanceof RingShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        var cageOptions = fill != null ? fill.ringCage() : null;
        if (cageOptions == null) {
            cageOptions = net.cyberpunk042.visual.fill.RingCageOptions.DEFAULT;
        }
        
        float innerRadius = shape.innerRadius();
        float outerRadius = shape.outerRadius();
        float halfHeight = shape.height() / 2f;
        int radialLines = cageOptions.radialLines();
        int concentricRings = cageOptions.concentricRings();
        boolean showInner = cageOptions.showInnerEdge();
        boolean showOuter = cageOptions.showOuterEdge();
        
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        var builder = net.cyberpunk042.client.visual.mesh.MeshBuilder.lines();
        
        // === RADIAL LINES (top and bottom faces) ===
        for (float y : new float[]{-halfHeight, halfHeight}) {
            for (int i = 0; i < radialLines; i++) {
                float angle = (float) (2 * Math.PI * i / radialLines);
                float ix = (float) Math.cos(angle) * innerRadius;
                float iz = (float) Math.sin(angle) * innerRadius;
                float ox = (float) Math.cos(angle) * outerRadius;
                float oz = (float) Math.sin(angle) * outerRadius;
                
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(ix, y, iz);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(ox, y, oz);
                
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                int idx1 = builder.addVertex(v1);
                int idx2 = builder.addVertex(v2);
                builder.line(idx1, idx2);
            }
        }
        
        // === CONCENTRIC RINGS (top and bottom) ===
        int ringSegments = Math.max(radialLines * 2, 32);
        for (float y : new float[]{-halfHeight, halfHeight}) {
            // Inner edge
            if (showInner) {
                emitCircle(builder, innerRadius, y, ringSegments, applyWave, waveConfig, time);
            }
            // Outer edge
            if (showOuter) {
                emitCircle(builder, outerRadius, y, ringSegments, applyWave, waveConfig, time);
            }
            // Intermediate rings
            for (int ring = 1; ring <= concentricRings; ring++) {
                float t = (float) ring / (concentricRings + 1);
                float radius = innerRadius + t * (outerRadius - innerRadius);
                emitCircle(builder, radius, y, ringSegments, applyWave, waveConfig, time);
            }
        }
        
        // === VERTICAL EDGES connecting top and bottom ===
        for (int i = 0; i < radialLines; i++) {
            float angle = (float) (2 * Math.PI * i / radialLines);
            // Inner vertical
            if (showInner) {
                float x = (float) Math.cos(angle) * innerRadius;
                float z = (float) Math.sin(angle) * innerRadius;
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, -halfHeight, z);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, halfHeight, z);
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                builder.line(builder.addVertex(v1), builder.addVertex(v2));
            }
            // Outer vertical
            if (showOuter) {
                float x = (float) Math.cos(angle) * outerRadius;
                float z = (float) Math.sin(angle) * outerRadius;
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, -halfHeight, z);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, halfHeight, z);
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
    
    private void emitCircle(net.cyberpunk042.client.visual.mesh.MeshBuilder builder, 
                           float radius, float y, int segments,
                           boolean applyWave, net.cyberpunk042.visual.animation.WaveConfig waveConfig, float time) {
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
            
            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;
            
            var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x1, y, z1);
            var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x2, y, z2);
            
            if (applyWave) {
                v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
            }
            
            builder.line(builder.addVertex(v1), builder.addVertex(v2));
        }
    }
}
