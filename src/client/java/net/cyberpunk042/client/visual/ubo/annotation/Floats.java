package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Indicates this record component should be serialized as N floats.
 * 
 * <p>Useful for vec3, vec2, or single floats that don't need a full vec4.
 * Note that std140 layout may require padding after non-vec4 types.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @UBOStruct
 * public record MyUBO(
 *     @Floats(count = 3, pad = true) float[] position,  // vec3 with padding to vec4
 *     @Floats(count = 1) Float intensity                // single float
 * ) {}
 * }</pre>
 * 
 * <p>The annotated component must be either:</p>
 * <ul>
 *   <li>{@code float[]} with at least {@code count} elements</li>
 *   <li>{@code Float} (when count = 1)</li>
 * </ul>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Floats {
    
    /**
     * Number of floats to write from the component.
     */
    int count();
    
    /**
     * Whether to add std140 padding after writing.
     * When true, pads to the next vec4 boundary (16-byte alignment).
     * Useful for vec3 which requires padding in std140 layout.
     * Default is false.
     */
    boolean pad() default false;
}
