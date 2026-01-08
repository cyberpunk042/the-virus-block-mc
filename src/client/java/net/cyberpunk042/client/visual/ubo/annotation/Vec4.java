package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Indicates this record component should be serialized as 4 floats (vec4).
 * 
 * <p>The annotated component must either:</p>
 * <ul>
 *   <li>Implement {@code Vec4Serializable} interface</li>
 *   <li>Be a {@code float[]} with at least 4 elements</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @UBOStruct
 * public record MyUBO(
 *     @Vec4 PositionVec4 position,    // PositionVec4 implements Vec4Serializable
 *     @Vec4 float[] rawColor          // float[4] array
 * ) {}
 * }</pre>
 * 
 * <p>Buffer layout: 16 bytes (4 floats × 4 bytes)</p>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Vec4 {}
