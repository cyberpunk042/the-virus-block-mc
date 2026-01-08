package net.cyberpunk042.field.instance;

import net.cyberpunk042.log.Logging;

import net.cyberpunk042.field.FieldType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * A field instance anchored to a block position.
 * 
 * <p>Used for:
 * <ul>
 *   <li>Growth block fields</li>
 *   <li>Shield generator fields</li>
 *   <li>Any block-based field effect</li>
 * </ul>
 * 
 * <p>The field remains centered on the block until removed.
 */
public class AnchoredFieldInstance extends FieldInstance {
    
    private final BlockPos anchorPos;
    private BlockEntity blockEntity;
    
    public AnchoredFieldInstance(long id, Identifier definitionId, FieldType type,
                                  BlockPos anchorPos) {
        super(id, definitionId, type, Vec3d.ofCenter(anchorPos));
        this.anchorPos = anchorPos;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public BlockPos anchorPos() {
        return anchorPos;
    }
    
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public void setBlockEntity(BlockEntity be) {
        if (be != null) {
            Logging.REGISTRY.topic("anchored").debug(
                "Block entity attached at {}: {}", anchorPos, be.getClass().getSimpleName());
        } else if (this.blockEntity != null) {
            Logging.REGISTRY.topic("anchored").debug(
                "Block entity detached at {}", anchorPos);
        }
        this.blockEntity = be;
    }
    
    /**
     * Checks if the anchor block still exists.
     * Called to determine if the field should be removed.
     */
    public boolean isAnchorValid() {
        // This would need a World reference to check
        // For now, just return true (check externally)
        return true;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    protected void tickInstance() {
        // Anchored fields don't move, but we could add effects here
        // like pulsing, rotation animation, etc.
    }
    
    @Override
    public void onRemoved() {
        Logging.REGISTRY.topic("anchored").debug(
            "Anchored field removed at {}", anchorPos);
        // Clear block entity reference
        this.blockEntity = null;
    }
}
