package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.PolyhedronTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.PolyhedronShape;

/**
 * Renders polyhedron primitives (Platonic solids).
 * 
 * <p>Supports:
 * <ul>
 *   <li>Tetrahedron (4 faces)</li>
 *   <li>Cube (6 faces)</li>
 *   <li>Octahedron (8 faces)</li>
 *   <li>Dodecahedron (12 faces)</li>
 *   <li>Icosahedron (20 faces)</li>
 * </ul>
 * 
 * <h3>Cage vs Wireframe</h3>
 * <p>For polyhedral shapes, <b>cage mode produces the same output as wireframe</b>.
 * This is by design: the natural edges of a polyhedron (e.g., 12 edges of a cube,
 * 30 edges of a dodecahedron) ARE the structural "cage". Unlike curved surfaces
 * (sphere, torus) where a cage shows parametric grid lines distinct from tessellation
 * edges, polyhedra have no such secondary structure.</p>
 * 
 * <p>If subdivisions are applied (geodesic sphere from icosahedron), a future
 * enhancement could show only the original base polyhedron edges as the cage.</p>
 * 
 * <p>Uses {@link PolyhedronTessellator}'s builder pattern for tessellation.</p>
 * 
 * @see PolyhedronShape
 * @see PolyhedronTessellator
 */
public final class PolyhedronRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "polyhedron";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
            .kv("primType", primitive.type())
            .kv("shapeClass", primitive.shape() != null ? primitive.shape().getClass().getSimpleName() : "null")
            .debug("[POLY] Entering tessellate");
        
        if (!(primitive.shape() instanceof PolyhedronShape shape)) {
            net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
                .reason("Shape mismatch")
                .kv("expected", "PolyhedronShape")
                .kv("actual", primitive.shape() != null ? primitive.shape().getClass().getName() : "null")
                .warn("[POLY] Shape is not PolyhedronShape!");
            return null;
        }
        
        net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
            .kv("polyType", shape.polyType())
            .kv("radius", shape.radius())
            .debug("[POLY] Tessellating polyhedron");
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("faces", shape.primaryCellType());
            net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
                .kv("pattern", pattern != null ? pattern.toString() : "null")
                .debug("[POLY] Resolved pattern for faces");
        }
        
        // Use PolyhedronTessellator's builder pattern with pattern support
        // Detail level 0 = no subdivision (raw Platonic solid)
        Mesh mesh = PolyhedronTessellator.builder()
            .polyType(shape.polyType())
            .radius(shape.radius())
            .subdivisions(shape.subdivisions())
            .pattern(pattern)
            .build()
            .tessellate(0, wave, time);
        
        net.cyberpunk042.log.Logging.RENDER.topic("tessellate")
            .kv("vertexCount", mesh != null ? mesh.vertexCount() : 0)
            .kv("isEmpty", mesh != null ? mesh.isEmpty() : true)
            .debug("[POLY] Tessellation result");
        
        return mesh;
    }
    
    /**
     * Cage rendering for polyhedra intentionally uses wireframe.
     * 
     * <p>For Platonic solids, the natural edges ARE the structural cage.
     * There is no distinct "cage" separate from wireframe because:</p>
     * <ul>
     *   <li>Cube has 12 edges that define its shape</li>
     *   <li>Tetrahedron has 6 edges</li>
     *   <li>Octahedron has 12 edges</li>
     *   <li>Icosahedron has 30 edges</li>
     *   <li>Dodecahedron has 30 edges</li>
     * </ul>
     * 
     * <p>Unlike curved surfaces (sphere, torus) where cage shows parametric
     * grid lines distinct from tessellation edges, polyhedra have no such
     * secondary parametric structure.</p>
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
        // Polyhedra: cage = wireframe by design (edges ARE the structure)
        emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
    }
}
