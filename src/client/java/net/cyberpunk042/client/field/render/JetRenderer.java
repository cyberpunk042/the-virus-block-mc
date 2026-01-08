package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.animation.WaveDeformer;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.mesh.JetTessellator;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.fill.CylinderCageOptions;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.JetShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;


/**
 * Renders jet primitives (cones/tubes for relativistic jets).
 * 
 * <p>Handles tessellation with per-part patterns and provides specialized
 * cage rendering for jet geometry.</p>
 * 
 * @see JetShape
 * @see JetTessellator
 */
public final class JetRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "jet";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof JetShape shape)) {
            return null;
        }
        
        // Get separate patterns for walls and caps
        VertexPattern wallPattern = null;
        VertexPattern capPattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Walls use QUAD cells
            wallPattern = arrangement.resolvePattern("outerWall", CellType.QUAD);
            // Caps also use QUAD cells (Ring-style: quads between inner and outer radius)
            capPattern = arrangement.resolvePattern("capBase", CellType.QUAD);
            
            Logging.RENDER.topic("tessellate")
                .kv("wallPattern", wallPattern != null ? wallPattern.toString() : "null")
                .kv("capPattern", capPattern != null ? capPattern.toString() : "null")
                .debug("[JET] Resolved per-part patterns");
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate with separate patterns for walls and caps
        return JetTessellator.tessellate(shape, wallPattern, capPattern, visibility, wave, time);
    }
    
    /**
     * Emits jet cage: vertical lines along length + horizontal rings.
     * 
     * <p>Uses cylinder-style cage options for configuration.</p>
     */
    @Override
    protected void emitCage(MatrixStack matrices, VertexConsumer consumer,
                            Mesh mesh, int color, int light, FillConfig fill,
                            Primitive primitive, WaveConfig waveConfig, float time) {
        
        if (!(primitive.shape() instanceof JetShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        var cageOptions = fill != null ? fill.cylinderCage() : null;
        if (cageOptions == null) {
            cageOptions = CylinderCageOptions.DEFAULT;
        }
        
        float halfGap = shape.gap() / 2;
        int vertLines = cageOptions.verticalLines();
        int horzRings = cageOptions.horizontalRings();
        boolean showCaps = cageOptions.showCaps();
        int segments = shape.segments();
        
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        
        MeshBuilder builder = MeshBuilder.lines();
        
        // Tessellate cage for top jet
        tessellateJetCage(builder, shape, true, halfGap, vertLines, horzRings,
            showCaps, segments, applyWave, waveConfig, time);
        
        // Tessellate cage for bottom jet if dual
        if (shape.dualJets()) {
            tessellateJetCage(builder, shape, false, halfGap, vertLines, horzRings,
                showCaps, segments, applyWave, waveConfig, time);
        }
        
        // Emit the cage mesh
        Mesh cageMesh = builder.build();
        VertexEmitter emitter = new VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
    
    /**
     * Tessellates cage lines for a single jet segment.
     */
    private void tessellateJetCage(MeshBuilder builder, JetShape shape,
                                    boolean isTop, float halfGap,
                                    int vertLines, int horzRings, boolean showCaps,
                                    int segments, boolean applyWave,
                                    WaveConfig waveConfig, float time) {
        float length = shape.length();
        float baseR = shape.baseRadius();
        float tipR = isTop ? shape.topTipRadius() : shape.bottomTipRadius();
        
        float yBase = isTop ? halfGap : -halfGap;
        float yTip = isTop ? (halfGap + length) : -(halfGap + length);
        
        // === VERTICAL LINES (along the cone/tube length) ===
        for (int i = 0; i < vertLines; i++) {
            float angle = (float) (2 * Math.PI * i / vertLines);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            float x1 = cos * baseR;
            float z1 = sin * baseR;
            float x2 = cos * tipR;
            float z2 = sin * tipR;
            
            Vertex v1 = Vertex.pos(x1, yBase, z1);
            Vertex v2 = Vertex.pos(x2, yTip, z2);
            
            if (applyWave) {
                v1 = WaveDeformer.applyToVertex(v1, waveConfig, time);
                v2 = WaveDeformer.applyToVertex(v2, waveConfig, time);
            }
            
            int idx1 = builder.addVertex(v1);
            int idx2 = builder.addVertex(v2);
            builder.line(idx1, idx2);
        }
        
        // === HORIZONTAL RINGS ===
        int ringSegments = Math.max(segments, 32);
        int totalRings = horzRings + (showCaps ? 2 : 0);
        
        for (int ring = 0; ring <= horzRings + 1; ring++) {
            // t = 0 at base, t = 1 at tip
            float t = ring / (float) (horzRings + 1);
            float y = yBase + (yTip - yBase) * t;
            float r = baseR + (tipR - baseR) * t;
            
            // Skip caps if not showing them
            if (!showCaps && (ring == 0 || ring == horzRings + 1)) continue;
            // Skip tip ring if radius is 0 (pointed cone)
            if (ring == horzRings + 1 && tipR == 0) continue;
            
            for (int i = 0; i < ringSegments; i++) {
                float angle1 = (float) (2 * Math.PI * i / ringSegments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / ringSegments);
                
                float x1 = (float) Math.cos(angle1) * r;
                float z1 = (float) Math.sin(angle1) * r;
                float x2 = (float) Math.cos(angle2) * r;
                float z2 = (float) Math.sin(angle2) * r;
                
                Vertex v1 = Vertex.pos(x1, y, z1);
                Vertex v2 = Vertex.pos(x2, y, z2);
                
                if (applyWave) {
                    v1 = WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                int idx1 = builder.addVertex(v1);
                int idx2 = builder.addVertex(v2);
                builder.line(idx1, idx2);
            }
        }
        
        // === INNER WALLS (for hollow jets) ===
        if (shape.hollow() && shape.innerBaseRadius() > 0) {
            float innerBaseR = shape.innerBaseRadius();
            float innerTipR = shape.innerTipRadius();
            
            // Inner vertical lines
            for (int i = 0; i < vertLines; i++) {
                float angle = (float) (2 * Math.PI * i / vertLines);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                
                float x1 = cos * innerBaseR;
                float z1 = sin * innerBaseR;
                float x2 = cos * innerTipR;
                float z2 = sin * innerTipR;
                
                Vertex v1 = Vertex.pos(x1, yBase, z1);
                Vertex v2 = Vertex.pos(x2, yTip, z2);
                
                if (applyWave) {
                    v1 = WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                int idx1 = builder.addVertex(v1);
                int idx2 = builder.addVertex(v2);
                builder.line(idx1, idx2);
            }
        }
    }
}
