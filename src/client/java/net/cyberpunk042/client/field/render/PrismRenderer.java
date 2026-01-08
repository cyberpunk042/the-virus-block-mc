package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.PrismTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.PrismShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders prism primitives.
 * 
 * <p>Passes arrangement pattern and visibility mask to tessellator
 * for proper cell filtering and vertex arrangement.</p>
 * 
 * @see PrismShape
 * @see PrismTessellator
 */
public final class PrismRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "prism";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof PrismShape shape)) {
            return null;
        }
        
        // Get separate patterns for sides and caps
        VertexPattern sidesPattern = null;
        VertexPattern capPattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Sides use QUAD cells
            sidesPattern = arrangement.resolvePattern("sides", CellType.QUAD);
            // Caps use SECTOR cells
            capPattern = arrangement.resolvePattern("capTop", CellType.SECTOR);
            
            Logging.RENDER.topic("tessellate")
                .kv("sidesPattern", sidesPattern != null ? sidesPattern.toString() : "null")
                .kv("capPattern", capPattern != null ? capPattern.toString() : "null")
                .debug("[PRISM] Resolved per-part patterns");
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with separate patterns for sides and caps
        return PrismTessellator.tessellate(shape, sidesPattern, capPattern, visibility, wave, time);
    }
    
    /**
     * Emits prism cage: vertical edge lines + horizontal cap outlines.
     * 
     * <p>Uses {@link net.cyberpunk042.visual.fill.PrismCageOptions} for configuration.</p>
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
        
        if (!(primitive.shape() instanceof PrismShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        var cageOptions = fill != null ? fill.prismCage() : null;
        if (cageOptions == null) {
            cageOptions = net.cyberpunk042.visual.fill.PrismCageOptions.DEFAULT;
        }
        
        float radius = shape.radius();
        float halfHeight = shape.height() / 2f;
        int sides = shape.sides();
        int horzRings = cageOptions.horizontalRings();
        boolean showCaps = cageOptions.showCaps();
        
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        var builder = net.cyberpunk042.client.visual.mesh.MeshBuilder.lines();
        
        // === VERTICAL EDGE LINES (at each corner) ===
        for (int i = 0; i < sides; i++) {
            float angle = (float) (2 * Math.PI * i / sides);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            
            var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, -halfHeight, z);
            var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, halfHeight, z);
            
            if (applyWave) {
                v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
            }
            
            builder.line(builder.addVertex(v1), builder.addVertex(v2));
        }
        
        // === HORIZONTAL RINGS (including caps) ===
        for (int ring = 0; ring <= horzRings + 1; ring++) {
            float t = (float) ring / (horzRings + 1);
            float y = -halfHeight + t * shape.height();
            
            // Skip caps if not showing them
            if (!showCaps && (ring == 0 || ring == horzRings + 1)) continue;
            
            // Draw polygon outline at this height
            for (int i = 0; i < sides; i++) {
                float angle1 = (float) (2 * Math.PI * i / sides);
                float angle2 = (float) (2 * Math.PI * ((i + 1) % sides) / sides);
                
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
        
        var cageMesh = builder.build();
        var emitter = new net.cyberpunk042.client.visual.render.VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
}
