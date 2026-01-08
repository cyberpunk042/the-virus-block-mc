package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MoleculeTessellator;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.shape.MoleculeShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;

/**
 * Renderer for MoleculeShape primitives.
 * 
 * <p>Uses the standard AbstractPrimitiveRenderer implementation with
 * MoleculeTessellator for mesh generation.</p>
 * 
 * @see MoleculeShape
 * @see MoleculeTessellator
 */
public class MoleculeRenderer extends AbstractPrimitiveRenderer {
    
    @Override
    public String shapeType() {
        return "molecule";
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof MoleculeShape shape)) {
            Logging.FIELD.topic("render").warn("[MOLECULE] Shape is NOT MoleculeShape: {}", 
                primitive.shape() != null ? primitive.shape().getClass().getSimpleName() : "null");
            return null;
        }
        
        Logging.FIELD.topic("render").trace("[MOLECULE] Tessellating: atomCount={}, atomRadius={}", 
            shape.atomCount(), shape.atomRadius());
        
        // Get pattern from arrangement config
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            pattern = arrangement.resolvePattern("main", shape.primaryCellType());
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // Tessellate molecule
        Mesh mesh = MoleculeTessellator.tessellate(shape, pattern, visibility, wave, time);
        if (mesh == null || mesh.isEmpty()) {
            Logging.FIELD.topic("render").warn("[MOLECULE] Tessellation returned empty mesh!");
        } else {
            Logging.FIELD.topic("render").debug("[MOLECULE] Mesh: {} vertices", mesh.vertices().size());
        }
        return mesh;
    }
}
