package net.cyberpunk042.client.visual.transform;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.transform.AnchorResolver;
import net.cyberpunk042.visual.transform.Facing;
import net.cyberpunk042.visual.transform.OrbitAnimator;
import net.cyberpunk042.visual.transform.Transform;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Applies a complete {@link Transform} to a {@link MatrixStack}.
 * 
 * <p>Application order:</p>
 * <ol>
 *   <li>Anchor offset (from {@link AnchorResolver})</li>
 *   <li>Additional offset</li>
 *   <li>Orbit offset (from {@link OrbitAnimator})</li>
 *   <li>Static rotation</li>
 *   <li>Facing rotation (from {@link FacingResolver})</li>
 *   <li>Billboard rotation (from {@link BillboardResolver})</li>
 *   <li>Scale</li>
 * </ol>
 * 
 * @see Transform
 * @see AnchorResolver
 * @see FacingResolver
 * @see BillboardResolver
 * @see OrbitAnimator
 */
public final class TransformApplier {
    
    private TransformApplier() {}
    
    // =========================================================================
    // Full Application
    // =========================================================================
    
    /**
     * Applies a complete transform to the matrix stack.
     * 
     * @param matrices The matrix stack to modify
     * @param transform The transform configuration
     * @param player The player (for anchor, facing)
     * @param camera The camera (for billboard, camera facing)
     * @param time Current time in ticks (for orbit)
     */
    public static void apply(MatrixStack matrices, Transform transform,
                            PlayerEntity player, Camera camera, float time) {
        if (transform == null) {
            transform = Transform.IDENTITY;
        }
        
        Logging.FIELD.topic("transform").trace("Applying transform: anchor={}, facing={}, billboard={}", 
            transform.anchor(), transform.facing(), transform.billboard());
        
        // === POSITION ===
        
        // 1. Anchor offset
        Vector3f anchorOffset = AnchorResolver.resolve(transform.anchor(), player);
        matrices.translate(anchorOffset.x, anchorOffset.y, anchorOffset.z);
        
        // 2. Additional offset
        if (transform.offset() != null) {
            Vector3f offset = transform.offset();
            matrices.translate(offset.x, offset.y, offset.z);
        }
        
        // 3. Orbit offset (priority: orbit3d > orbit)
        if (transform.hasOrbit3D()) {
            org.joml.Vector3f orbitOffset = transform.orbit3d().getOffset(time);
            matrices.translate(orbitOffset.x, orbitOffset.y, orbitOffset.z);
        } else if (transform.hasOrbit()) {
            Vector3f orbitOffset = OrbitAnimator.getOffset(transform.orbit(), time);
            matrices.translate(orbitOffset.x, orbitOffset.y, orbitOffset.z);
        }
        
        // === ROTATION ===
        
        // 4. Static rotation (if not inheriting or no layer rotation)
        if (transform.rotation() != null) {
            Vector3f rot = transform.rotation();
            // Apply in YXZ order (Minecraft standard)
            matrices.multiply(new Quaternionf().rotateY((float) Math.toRadians(rot.y)));
            matrices.multiply(new Quaternionf().rotateX((float) Math.toRadians(rot.x)));
            matrices.multiply(new Quaternionf().rotateZ((float) Math.toRadians(rot.z)));
        }
        
        // 5. Facing rotation (static directional orientation)
        if (FacingResolver.needsRotation(transform.facing())) {
            Quaternionf facingRot = FacingResolver.resolve(transform.facing());
            matrices.multiply(facingRot);
        }
        
        // 6. Billboard rotation
        BillboardResolver.apply(transform.billboard(), matrices, camera);
        
        // === SCALE ===
        
        // 7. Scale
        if (transform.scaleXYZ() != null) {
            Vector3f s = transform.scaleXYZ();
            matrices.scale(s.x, s.y, s.z);
        } else if (transform.scale() != 1.0f) {
            float s = transform.scale();
            matrices.scale(s, s, s);
        }
    }
    
    /**
     * Applies transform without player/camera context.
     * Only static properties are applied.
     * 
     * @param matrices The matrix stack
     * @param transform The transform
     */
    public static void applyStatic(MatrixStack matrices, Transform transform) {
        apply(matrices, transform, null, null, 0);
    }
    
}
