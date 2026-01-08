package net.cyberpunk042.client.field.render.emit;

import net.cyberpunk042.visual.fill.FillMode;

/**
 * Factory for creating EmitStrategy instances.
 * 
 * <p>Based on FillMode, selects the appropriate emission strategy:
 * - SOLID/TRANSLUCENT: EmitTriangleStrategy (filled triangles)
 * - WIREFRAME: EmitLineStrategy (edges as lines)
 * - CAGE: EmitCageStrategy (triangle edges as lines)
 * </p>
 * 
 * @see net.cyberpunk042.visual.fill.FillMode
 */
public final class EmitStrategyFactory {
    
    private EmitStrategyFactory() {}
    
    /**
     * Get the appropriate strategy for a fill mode.
     */
    public static EmitStrategy get(FillMode mode) {
        if (mode == null) {
            return EmitTriangleStrategy.INSTANCE;
        }
        
        return switch (mode) {
            case SOLID -> EmitTriangleStrategy.INSTANCE;
            case WIREFRAME -> EmitLineStrategy.INSTANCE;
            case CAGE -> EmitCageStrategy.INSTANCE;
            case POINTS -> EmitTriangleStrategy.INSTANCE;  // Fallback for now
        };
    }
}
