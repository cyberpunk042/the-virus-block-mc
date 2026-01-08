package net.cyberpunk042.client.visual.ubo;

/**
 * Interface for records that can be serialized as a vec4 (4 floats).
 * 
 * <p>Implementing this interface allows any record to be written to a UBO
 * when its usage site is annotated with {@code @Vec4}.</p>
 * 
 * <p>The slot names are slot0-3 to be generic and not assume meaning
 * (could be xyzw, rgba, stpq, etc.).</p>
 * 
 * <p>Example implementation:</p>
 * <pre>{@code
 * public record PositionVec4(float x, float y, float z, float w) implements Vec4Serializable {
 *     @Override public float slot0() { return x; }
 *     @Override public float slot1() { return y; }
 *     @Override public float slot2() { return z; }
 *     @Override public float slot3() { return w; }
 * }
 * }</pre>
 * 
 * <p>Then used in a UBO struct:</p>
 * <pre>{@code
 * @UBOStruct
 * public record MyUBO(
 *     @Vec4 PositionVec4 position  // Will call slot0-3 to serialize
 * ) {}
 * }</pre>
 */
public interface Vec4Serializable {
    
    /**
     * First float value (typically x, r, s, or similar).
     */
    float slot0();
    
    /**
     * Second float value (typically y, g, t, or similar).
     */
    float slot1();
    
    /**
     * Third float value (typically z, b, p, or similar).
     */
    float slot2();
    
    /**
     * Fourth float value (typically w, a, q, or similar).
     */
    float slot3();
    
    /**
     * Convenience method to get all 4 values as an array.
     * Default implementation builds from slot0-3.
     */
    default float[] toFloatArray() {
        return new float[] { slot0(), slot1(), slot2(), slot3() };
    }
}
