package net.cyberpunk042.visual.pattern;

import net.cyberpunk042.visual.pattern.QuadPattern.Corner;

/**
 * A dynamically generated quad pattern from shuffle exploration.
 * 
 * <p>Unlike static {@link QuadPattern} enums, this allows runtime
 * exploration of all possible vertex arrangements.
 * 
 * <p>Uses semantic {@link Corner} enum for readability.
 * 
 * @see ShuffleGenerator
 * @see QuadPattern
 */
public record DynamicQuadPattern(
    Corner[] triangle1,
    Corner[] triangle2,
    int shuffleIndex,
    String description
) implements VertexPattern {
    
    @Override
    public String id() {
        return "shuffle_quad_" + shuffleIndex;
    }
    
    @Override
    public CellType cellType() {
        return CellType.QUAD;
    }
    
    @Override
    public boolean shouldRender(int index, int total) {
        return true;  // Always render, just different vertex order
    }
    
    @Override
    public int[][] getVertexOrder() {
        int[] t1 = new int[]{triangle1[0].index, triangle1[1].index, triangle1[2].index};
        if (triangle2 == null) {
            return new int[][]{t1};
        }
        int[] t2 = new int[]{triangle2[0].index, triangle2[1].index, triangle2[2].index};
        return new int[][]{t1, t2};
    }
    
    /**
     * Human-readable description of this pattern.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(triangle1[0].shortName()).append("→");
        sb.append(triangle1[1].shortName()).append("→");
        sb.append(triangle1[2].shortName());
        
        if (triangle2 != null) {
            sb.append(" | ");
            sb.append(triangle2[0].shortName()).append("→");
            sb.append(triangle2[1].shortName()).append("→");
            sb.append(triangle2[2].shortName());
        }
        return sb.toString();
    }
    
    /**
     * Creates from a ShuffleGenerator arrangement.
     */
    public static DynamicQuadPattern fromArrangement(ShuffleGenerator.QuadArrangement arr) {
        return new DynamicQuadPattern(arr.tri1(), arr.tri2(), arr.index(), arr.describe());
    }
}
