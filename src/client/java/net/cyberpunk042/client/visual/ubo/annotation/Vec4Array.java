package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Indicates this record component is an array of vec4 values.
 * 
 * <p>The annotated component must be an array type where each element
 * can be serialized as a vec4 (4 floats).</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @UBOStruct
 * public record MyUBO(
 *     @Vec4Array(count = 32) PositionVec4[] positions
 * ) {}
 * }</pre>
 * 
 * <p>Buffer layout: count × 16 bytes</p>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Vec4Array {
    /**
     * Number of vec4 elements in the array.
     * The actual array may be smaller; remaining slots are zero-filled.
     */
    int count();
}
