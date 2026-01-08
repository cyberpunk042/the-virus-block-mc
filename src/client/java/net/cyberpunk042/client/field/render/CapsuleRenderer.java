package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.CapsuleTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.CapsuleShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renders capsule (cylinder with hemispherical caps) primitives.
 * 
 * @see CapsuleShape
 * @see CapsuleTessellator
 */
public final class CapsuleRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "capsule";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof CapsuleShape shape)) {
            return null;
        }
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("cylinder", shape.primaryCellType());
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        return CapsuleTessellator.tessellate(shape, pattern, visibility, wave, time);
    }
    /**
     * Emits capsule cage: hemisphere caps + cylinder body lines.
     * 
     * <p>Longitude lines curve along the hemispherical caps, not straight through.
     * Uses sphere-like options for overall configuration.</p>
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
        
        if (!(primitive.shape() instanceof CapsuleShape shape)) {
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Use sphere cage options for overall configuration
        var cageOptions = fill != null ? fill.sphereCage() : null;
        if (cageOptions == null) {
            cageOptions = net.cyberpunk042.visual.fill.SphereCageOptions.DEFAULT;
        }
        
        float radius = shape.radius();
        float cylinderHeight = shape.cylinderHeight();
        float halfCylHeight = cylinderHeight / 2f;
        int latCount = cageOptions.latitudeCount();
        int lonCount = cageOptions.longitudeCount();
        int hemiSteps = Math.max(4, latCount / 2);  // Steps for hemisphere curve
        
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        var builder = net.cyberpunk042.client.visual.mesh.MeshBuilder.lines();
        
        // === LONGITUDE LINES (curved along capsule surface) ===
        for (int i = 0; i < lonCount; i++) {
            float lonAngle = (float) (2 * Math.PI * i / lonCount);
            float baseX = (float) Math.cos(lonAngle);
            float baseZ = (float) Math.sin(lonAngle);
            
            // Draw bottom hemisphere arc (from pole to equator)
            for (int j = 0; j < hemiSteps; j++) {
                float lat1 = (float) (Math.PI / 2 * j / hemiSteps);
                float lat2 = (float) (Math.PI / 2 * (j + 1) / hemiSteps);
                
                float y1 = -halfCylHeight - radius * (float) Math.cos(lat1);
                float r1 = radius * (float) Math.sin(lat1);
                float y2 = -halfCylHeight - radius * (float) Math.cos(lat2);
                float r2 = radius * (float) Math.sin(lat2);
                
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(baseX * r1, y1, baseZ * r1);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(baseX * r2, y2, baseZ * r2);
                
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                builder.line(builder.addVertex(v1), builder.addVertex(v2));
            }
            
            // Draw cylinder vertical line (if cylinder has height)
            if (cylinderHeight > 0.001f) {
                float x = baseX * radius;
                float z = baseZ * radius;
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, -halfCylHeight, z);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(x, halfCylHeight, z);
                
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                builder.line(builder.addVertex(v1), builder.addVertex(v2));
            }
            
            // Draw top hemisphere arc (from equator to pole)
            for (int j = 0; j < hemiSteps; j++) {
                float lat1 = (float) (Math.PI / 2 * j / hemiSteps);
                float lat2 = (float) (Math.PI / 2 * (j + 1) / hemiSteps);
                
                float y1 = halfCylHeight + radius * (float) Math.cos(lat1);
                float r1 = radius * (float) Math.sin(lat1);
                float y2 = halfCylHeight + radius * (float) Math.cos(lat2);
                float r2 = radius * (float) Math.sin(lat2);
                
                // Reverse direction: from full radius down to pole
                var v1 = net.cyberpunk042.client.visual.mesh.Vertex.pos(baseX * r1, y1, baseZ * r1);
                var v2 = net.cyberpunk042.client.visual.mesh.Vertex.pos(baseX * r2, y2, baseZ * r2);
                
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                builder.line(builder.addVertex(v1), builder.addVertex(v2));
            }
        }
        
        // === HORIZONTAL RINGS on cylinder body ===
        int bodyRings = Math.max(2, latCount / 2);
        int ringSegments = Math.max(lonCount * 2, 32);
        for (int ring = 0; ring <= bodyRings; ring++) {
            float t = (float) ring / bodyRings;
            float y = -halfCylHeight + t * cylinderHeight;
            
            emitHorizontalRing(builder, radius, y, ringSegments, applyWave, waveConfig, time);
        }
        
        // === LATITUDE LINES on top hemisphere ===
        int hemiRings = Math.max(2, latCount / 2);
        for (int ring = 1; ring < hemiRings; ring++) {
            float latAngle = (float) (Math.PI / 2 * ring / hemiRings);
            float ringY = halfCylHeight + radius * (float) Math.sin(latAngle);
            float ringRadius = radius * (float) Math.cos(latAngle);
            
            emitHorizontalRing(builder, ringRadius, ringY, ringSegments, applyWave, waveConfig, time);
        }
        
        // === LATITUDE LINES on bottom hemisphere ===
        for (int ring = 1; ring < hemiRings; ring++) {
            float latAngle = (float) (Math.PI / 2 * ring / hemiRings);
            float ringY = -halfCylHeight - radius * (float) Math.sin(latAngle);
            float ringRadius = radius * (float) Math.cos(latAngle);
            
            emitHorizontalRing(builder, ringRadius, ringY, ringSegments, applyWave, waveConfig, time);
        }
        
        var cageMesh = builder.build();
        var emitter = new net.cyberpunk042.client.visual.render.VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
    
    private void emitHorizontalRing(net.cyberpunk042.client.visual.mesh.MeshBuilder builder,
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
