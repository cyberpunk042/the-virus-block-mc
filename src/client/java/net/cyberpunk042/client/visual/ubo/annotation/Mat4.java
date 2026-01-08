package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Indicates this record component should be serialized as 16 floats (mat4).
 * 
 * <p>The annotated component must be an {@code org.joml.Matrix4f}.
 * The matrix is written in column-major order as required by GLSL std140 layout.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @UBOStruct
 * public record MyUBO(
 *     @Mat4 Matrix4f invViewProj,
 *     @Mat4 Matrix4f viewProj
 * ) {}
 * }</pre>
 * 
 * <p>Buffer layout: 64 bytes (16 floats × 4 bytes = 4 vec4 slots)</p>
 * 
 * <p>If the matrix is null, an identity matrix is written as fallback.</p>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Mat4 {}
