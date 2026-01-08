package net.cyberpunk042.visual.pattern;

/**
 * A dynamically generated sector pattern from shuffle exploration.
 * 
 * <p>Unlike static {@link SectorPattern} enums, this allows runtime
 * exploration of all possible sector arrangements for discs.
 * 
 * @see ShuffleGenerator
 * @see SectorPattern
 */
public record DynamicSectorPattern(
    int skipInterval,
    int phaseOffset,
    boolean invertSelection,
    int shuffleIndex,
    String description
) implements VertexPattern {
    
    @Override
    public String id() {
        return "shuffle_sector_" + shuffleIndex;
    }
    
    @Override
    public CellType cellType() {
        return CellType.SECTOR;
    }
    
    @Override
    public boolean shouldRender(int index, int total) {
        // Apply skip interval and phase offset
        int adjusted = (index + phaseOffset) % total;
        boolean visible = adjusted % skipInterval == 0;
        return invertSelection ? !visible : visible;
    }
    
    @Override
    public int[][] getVertexOrder() {
        // Sectors are rendered as triangles from center
        // vertex 0 = center, 1 = edge start, 2 = edge end
        return new int[][]{{0, 1, 2}};
    }
    
    /**
     * Human-readable description of this pattern.
     */
    public String describe() {
        return String.format("skip=%d phase=%d %s", 
            skipInterval, phaseOffset, invertSelection ? "inverted" : "normal");
    }
    
    /**
     * Creates from a ShuffleGenerator arrangement.
     */
    public static DynamicSectorPattern fromArrangement(ShuffleGenerator.SectorArrangement arr) {
        return new DynamicSectorPattern(
            arr.skipInterval(),
            arr.phaseOffset(),
            arr.invertSelection(),
            arr.index(),
            arr.describe()
        );
    }
}
