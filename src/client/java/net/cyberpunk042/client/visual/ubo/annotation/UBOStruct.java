package net.cyberpunk042.client.visual.ubo.annotation;

import java.lang.annotation.*;

/**
 * Marks a record as a UBO structure that can be serialized to GPU buffer.
 * 
 * <p>Records annotated with @UBOStruct can be passed to {@code ReflectiveUBOWriter.write()}
 * which will iterate their components in declaration order and write to the buffer.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @UBOStruct(name = "FieldVisualConfig", glslPath = "the-virus-block:shaders/post/field_visual.fsh")
 * public record FieldVisualUBO(
 *     @Vec4 PositionVec4 position,
 *     @Vec4 ColorVec4 color,
 *     @Mat4 Matrix4f viewProj
 * ) {}
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UBOStruct {
    
    /**
     * Name of the GLSL uniform block.
     * Used by GLSLValidator to locate the struct in the shader file.
     */
    String name() default "";
    
    /**
     * Path to GLSL file containing the uniform block.
     * Format: "namespace:path/to/shader.fsh"
     * Used by GLSLValidator for startup validation.
     * If empty, validation is skipped.
     */
    String glslPath() default "";
}
