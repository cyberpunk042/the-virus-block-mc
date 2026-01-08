package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.ConeTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.ConeShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders cone and frustum primitives.
 * 
 * @see ConeShape
 * @see ConeTessellator
 */
public final class ConeRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "cone";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof ConeShape shape)) {
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
        
        return ConeTessellator.tessellate(shape, pattern, visibility, wave, time);
    }
    
    /**
     * Emits cone cage: radial lines from apex to base + horizontal rings.
     * 
     * <p>Uses {@link net.cyberpunk042.visual.fill.ConeCageOptions} for configuration.</p>
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
        
        if (!(primitive.shape() instanceof ConeShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        var cageOptions = fill != null ? fill.coneCage() : null;
        if (cageOptions == null) {
            cageOptions = net.cyberpunk042.visual.fill.ConeCageOptions.DEFAULT;
        }
        
        float baseRadius = shape.bottomRadius();
        float topRadius = shape.topRadius();  // 0 for true cone
        float halfHeight = shape.height() / 2f;
        int radialLines = cageOptions.radialLines();
        int horzRings = cageOptions.horizontalRings();
        boolean showBase = cageOptions.showBase();
        
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        var builder = net.cyberpunk042.client.visual.mesh.MeshBuilder.lines();
        
        // === RADIAL LINES (from apex/top to base) ===
        for (int i = 0; i < radialLines; i++) {
            float angle = (float) (2 * Math.PI * i / radialLines);
            float bx = (float) Math.cos(angle) * baseRadius;
            float bz = (float) Math.sin(angle) * baseRadius;
            float tx = (float) Math.cos(angle) * topRadius;
            float tz = (float) Math.sin(angle) * topRadius;
            
            var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(bx, -halfHeight, bz);
            var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(tx, halfHeight, tz);
            
            if (applyWave) {
                v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
            }
            
            int idx1 = builder.addVertex(v1);
            int idx2 = builder.addVertex(v2);
            builder.line(idx1, idx2);
        }
        
        // === HORIZONTAL RINGS ===
        int ringSegments = Math.max(radialLines * 2, 32);
        for (int ring = 0; ring <= horzRings; ring++) {
            float t = (float) ring / Math.max(horzRings, 1);
            float y = -halfHeight + t * shape.height();
            float radius = baseRadius + t * (topRadius - baseRadius);
            
            // Skip base circle if not showing
            if (!showBase && ring == 0) continue;
            // Skip top if it's a point (true cone)
            if (ring == horzRings && topRadius < 0.001f) continue;
            
            for (int i = 0; i < ringSegments; i++) {
                float angle1 = (float) (2 * Math.PI * i / ringSegments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / ringSegments);
                
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
                
                int idx1 = builder.addVertex(v1);
                int idx2 = builder.addVertex(v2);
                builder.line(idx1, idx2);
            }
        }
        
        var cageMesh = builder.build();
        var emitter = new net.cyberpunk042.client.visual.render.VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
}
