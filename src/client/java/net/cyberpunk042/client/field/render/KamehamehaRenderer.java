package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.KamehamehaTessellator;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.CellType;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.KamehamehaShape;
import net.cyberpunk042.visual.shape.OrientationAxis;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;


/**
 * Renders Kamehameha primitives (energy beam with orb).
 * 
 * <p>Handles the composite orb + beam structure with separate
 * patterns for each component and multi-layer rendering for
 * core and aura effects.</p>
 * 
 * @see KamehamehaShape
 * @see KamehamehaTessellator
 */
public final class KamehamehaRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "kamehameha";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof KamehamehaShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("main", CellType.QUAD);
        }
        if (pattern == null) {
            pattern = QuadPattern.DEFAULT;
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        if (visibility == null) {
            visibility = VisibilityMask.FULL;
        }
        
        Logging.RENDER.topic("tessellate")
            .kv("orbRadius", shape.effectiveOrbRadius())
            .kv("beamLength", shape.effectiveBeamLength())
            .debug("[KAMEHAMEHA] Tessellating shape");
        
        return KamehamehaTessellator.tessellate(shape, pattern, visibility, time);
    }
    
    /**
     * Emits Kamehameha cage: rings around orb + lines along beam.
     */
    @Override
    protected void emitCage(MatrixStack matrices, VertexConsumer consumer,
                            Mesh mesh, int color, int light, FillConfig fill,
                            Primitive primitive, WaveConfig waveConfig, float time) {
        
        if (!(primitive.shape() instanceof KamehamehaShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        MeshBuilder builder = MeshBuilder.lines();
        
        // Cage lines around orb
        if (shape.isOrbVisible()) {
            tessellateOrbCage(builder, shape, 8, 4);
        }
        
        // Cage lines along beam
        if (shape.isBeamVisible()) {
            tessellateBeamCage(builder, shape, 8, 4);
        }
        
        // Emit the cage mesh
        Mesh cageMesh = builder.build();
        VertexEmitter emitter = new VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
    
    /**
     * Tessellates cage rings for the orb.
     */
    private void tessellateOrbCage(MeshBuilder builder, KamehamehaShape shape,
                                    int vertLines, int horzRings) {
        float radius = shape.effectiveOrbRadius();
        if (radius <= 0) return;
        
        // Get orientation for proper transform
        OrientationAxis axis = shape.orientationAxis() != null ? shape.orientationAxis() : OrientationAxis.POS_Z;
        float offset = shape.originOffset();
        
        int segments = 32;
        
        // Horizontal rings (latitude lines)
        for (int ring = 0; ring <= horzRings; ring++) {
            float t = (float) ring / horzRings;
            float theta = (float) (Math.PI * t);
            float localY = radius * (float) Math.cos(theta);
            float ringRadius = radius * (float) Math.sin(theta);
            
            if (ringRadius < 0.01f) continue;
            
            for (int i = 0; i < segments; i++) {
                float angle1 = (float) (2 * Math.PI * i / segments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
                
                float localX1 = (float) Math.cos(angle1) * ringRadius;
                float localZ1 = (float) Math.sin(angle1) * ringRadius;
                float localX2 = (float) Math.cos(angle2) * ringRadius;
                float localZ2 = (float) Math.sin(angle2) * ringRadius;
                
                // Transform to world space
                org.joml.Vector3f pos1 = axis.transformVertex(localX1, localY, localZ1, offset);
                org.joml.Vector3f pos2 = axis.transformVertex(localX2, localY, localZ2, offset);
                
                int idx1 = builder.addVertex(Vertex.pos(pos1.x, pos1.y, pos1.z));
                int idx2 = builder.addVertex(Vertex.pos(pos2.x, pos2.y, pos2.z));
                builder.line(idx1, idx2);
            }
        }
        
        // Vertical lines (longitude lines)
        for (int i = 0; i < vertLines; i++) {
            float phi = (float) (2 * Math.PI * i / vertLines);
            float cosPhi = (float) Math.cos(phi);
            float sinPhi = (float) Math.sin(phi);
            
            for (int lat = 0; lat < horzRings; lat++) {
                float theta1 = (float) (Math.PI * lat / horzRings);
                float theta2 = (float) (Math.PI * (lat + 1) / horzRings);
                
                float localY1 = radius * (float) Math.cos(theta1);
                float localY2 = radius * (float) Math.cos(theta2);
                float r1 = radius * (float) Math.sin(theta1);
                float r2 = radius * (float) Math.sin(theta2);
                
                float localX1 = cosPhi * r1;
                float localZ1 = sinPhi * r1;
                float localX2 = cosPhi * r2;
                float localZ2 = sinPhi * r2;
                
                // Transform to world space
                org.joml.Vector3f pos1 = axis.transformVertex(localX1, localY1, localZ1, offset);
                org.joml.Vector3f pos2 = axis.transformVertex(localX2, localY2, localZ2, offset);
                
                int idx1 = builder.addVertex(Vertex.pos(pos1.x, pos1.y, pos1.z));
                int idx2 = builder.addVertex(Vertex.pos(pos2.x, pos2.y, pos2.z));
                builder.line(idx1, idx2);
            }
        }
    }
    
    /**
     * Tessellates cage lines for the beam.
     */
    private void tessellateBeamCage(MeshBuilder builder, KamehamehaShape shape,
                                     int vertLines, int horzRings) {
        // Beam starts at 0 (True Center Anchor - passes through orb center)
        float startY = 0f;
        float length = shape.effectiveBeamLength();
        float baseRadius = shape.effectiveBeamBaseRadius();
        float tipRadius = shape.effectiveBeamTipRadius();
        
        if (length <= 0 || baseRadius <= 0) return;
        
        // Get orientation for proper transform
        OrientationAxis axis = shape.orientationAxis() != null ? shape.orientationAxis() : OrientationAxis.POS_Z;
        float offset = shape.originOffset();
        
        int segments = 32;
        
        // Vertical lines along beam
        for (int i = 0; i < vertLines; i++) {
            float angle = (float) (2 * Math.PI * i / vertLines);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            
            float localX1 = cos * baseRadius;
            float localZ1 = sin * baseRadius;
            float localX2 = cos * tipRadius;
            float localZ2 = sin * tipRadius;
            
            // Transform to world space
            org.joml.Vector3f pos1 = axis.transformVertex(localX1, startY, localZ1, offset);
            org.joml.Vector3f pos2 = axis.transformVertex(localX2, startY + length, localZ2, offset);
            
            int idx1 = builder.addVertex(Vertex.pos(pos1.x, pos1.y, pos1.z));
            int idx2 = builder.addVertex(Vertex.pos(pos2.x, pos2.y, pos2.z));
            builder.line(idx1, idx2);
        }
        
        // Horizontal rings along beam
        for (int ring = 0; ring <= horzRings; ring++) {
            float t = (float) ring / horzRings;
            float localY = startY + length * t;
            float r = baseRadius + (tipRadius - baseRadius) * t;
            
            if (r < 0.01f) continue;
            
            for (int i = 0; i < segments; i++) {
                float angle1 = (float) (2 * Math.PI * i / segments);
                float angle2 = (float) (2 * Math.PI * (i + 1) / segments);
                
                float localX1 = (float) Math.cos(angle1) * r;
                float localZ1 = (float) Math.sin(angle1) * r;
                float localX2 = (float) Math.cos(angle2) * r;
                float localZ2 = (float) Math.sin(angle2) * r;
                
                // Transform to world space
                org.joml.Vector3f pos1 = axis.transformVertex(localX1, localY, localZ1, offset);
                org.joml.Vector3f pos2 = axis.transformVertex(localX2, localY, localZ2, offset);
                
                int idx1 = builder.addVertex(Vertex.pos(pos1.x, pos1.y, pos1.z));
                int idx2 = builder.addVertex(Vertex.pos(pos2.x, pos2.y, pos2.z));
                builder.line(idx1, idx2);
            }
        }
    }
}
