package net.cyberpunk042.client.visual.ubo;

import com.mojang.blaze3d.buffers.Std140Builder;
import net.cyberpunk042.client.visual.ubo.annotation.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.RecordComponent;

/**
 * Writes any {@code @UBOStruct} record to a Std140 buffer using reflection.
 * 
 * <p>Field order is determined by record component declaration order,
 * which is guaranteed by Java to match source order. This eliminates
 * the need for manual slot numbering.</p>
 * 
 * <h3>Supported Annotations</h3>
 * <ul>
 *   <li>{@code @Vec4} - writes 4 floats (16 bytes)</li>
 *   <li>{@code @Mat4} - writes 16 floats (64 bytes)</li>
 *   <li>{@code @Floats(count=N)} - writes N floats with optional padding</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * FieldVisualUBO ubo = FieldVisualUBO.from(config, position, camera, ...);
 * ReflectiveUBOWriter.write(builder, ubo);
 * }</pre>
 * 
 * @see UBOStruct
 * @see Vec4
 * @see Mat4
 * @see Floats
 */
public final class ReflectiveUBOWriter {
    
    private ReflectiveUBOWriter() {} // Utility class
    
    /**
     * Calculates the buffer size in bytes for a UBO struct.
     * 
     * @param uboClass the record class annotated with @UBOStruct
     * @return total size in bytes
     */
    public static int calculateBufferSize(Class<?> uboClass) {
        int bytes = 0;
        
        for (RecordComponent comp : uboClass.getRecordComponents()) {
            if (comp.isAnnotationPresent(Vec4.class)) {
                bytes += 16;  // 4 floats × 4 bytes
            } else if (comp.isAnnotationPresent(Mat4.class)) {
                bytes += 64;  // 16 floats × 4 bytes
            } else if (comp.isAnnotationPresent(Vec4Array.class)) {
                Vec4Array arr = comp.getAnnotation(Vec4Array.class);
                bytes += arr.count() * 16;  // count × 4 floats × 4 bytes
            } else if (comp.isAnnotationPresent(Floats.class)) {
                Floats floats = comp.getAnnotation(Floats.class);
                bytes += floats.count() * 4;
                if (floats.pad() && floats.count() % 4 != 0) {
                    // Pad to next vec4 boundary
                    int remainder = floats.count() % 4;
                    bytes += (4 - remainder) * 4;
                }
            }
            // Components without annotations are skipped
        }
        
        return bytes;
    }
    
    /**
     * Writes a UBO struct record to the buffer.
     * 
     * <p>Iterates through record components in declaration order and writes
     * each one according to its annotation.</p>
     * 
     * @param builder the Std140Builder to write to
     * @param record the UBO struct record to serialize
     * @throws RuntimeException if writing fails
     */
    public static void write(Std140Builder builder, Object record) {
        try {
            for (RecordComponent comp : record.getClass().getRecordComponents()) {
                Object value = comp.getAccessor().invoke(record);
                
                if (comp.isAnnotationPresent(Vec4.class)) {
                    writeVec4(builder, value, comp.getName());
                } else if (comp.isAnnotationPresent(Mat4.class)) {
                    writeMatrix(builder, (Matrix4f) value);
                } else if (comp.isAnnotationPresent(Vec4Array.class)) {
                    Vec4Array arrAnnotation = comp.getAnnotation(Vec4Array.class);
                    writeVec4Array(builder, value, arrAnnotation.count(), comp.getName());
                } else if (comp.isAnnotationPresent(Floats.class)) {
                    writeFloats(builder, value, comp.getAnnotation(Floats.class), comp.getName());
                }
                // Components without annotations are intentionally skipped
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to write UBO: " + record.getClass().getSimpleName() + 
                " - " + e.getMessage(), 
                e
            );
        }
    }
    
    /**
     * Writes a value as vec4 (4 floats).
     */
    private static void writeVec4(Std140Builder builder, Object value, String componentName) {
        if (value instanceof Vec4Serializable v4) {
            builder.putFloat(v4.slot0());
            builder.putFloat(v4.slot1());
            builder.putFloat(v4.slot2());
            builder.putFloat(v4.slot3());
        } else if (value instanceof float[] arr) {
            if (arr.length < 4) {
                throw new IllegalArgumentException(
                    "float[] for @Vec4 component '" + componentName + 
                    "' has only " + arr.length + " elements, need 4"
                );
            }
            builder.putFloat(arr[0]);
            builder.putFloat(arr[1]);
            builder.putFloat(arr[2]);
            builder.putFloat(arr[3]);
        } else {
            throw new IllegalArgumentException(
                "Cannot write component '" + componentName + "' as Vec4: " + 
                value.getClass().getSimpleName() +
                ". Must implement Vec4Serializable or be float[4]."
            );
        }
    }
    
    /**
     * Writes an array of vec4 values.
     * 
     * @param builder the Std140Builder to write to
     * @param value the array (must be Object[] with Vec4Serializable elements)
     * @param count the expected number of vec4 elements
     * @param componentName the component name for error messages
     */
    private static void writeVec4Array(Std140Builder builder, Object value, int count, String componentName) {
        if (value == null) {
            // Write zeros for all slots
            for (int i = 0; i < count; i++) {
                builder.putFloat(0f);
                builder.putFloat(0f);
                builder.putFloat(0f);
                builder.putFloat(0f);
            }
            return;
        }
        
        if (!(value instanceof Object[] arr)) {
            throw new IllegalArgumentException(
                "Cannot write component '" + componentName + "' as Vec4Array: " +
                value.getClass().getSimpleName() +
                ". Must be an array of Vec4Serializable."
            );
        }
        
        // Write actual elements
        int written = 0;
        for (int i = 0; i < arr.length && i < count; i++) {
            Object elem = arr[i];
            if (elem instanceof Vec4Serializable v4) {
                builder.putFloat(v4.slot0());
                builder.putFloat(v4.slot1());
                builder.putFloat(v4.slot2());
                builder.putFloat(v4.slot3());
                written++;
            } else if (elem == null) {
                builder.putFloat(0f);
                builder.putFloat(0f);
                builder.putFloat(0f);
                builder.putFloat(0f);
                written++;
            } else {
                throw new IllegalArgumentException(
                    "Element " + i + " of component '" + componentName + 
                    "' is not Vec4Serializable: " + elem.getClass().getSimpleName()
                );
            }
        }
        
        // Fill remaining slots with zeros
        for (int i = written; i < count; i++) {
            builder.putFloat(0f);
            builder.putFloat(0f);
            builder.putFloat(0f);
            builder.putFloat(0f);
        }
    }
    
    /**
     * Writes a Matrix4f as mat4 (16 floats in column-major order).
     */
    private static void writeMatrix(Std140Builder builder, Matrix4f mat) {
        if (mat == null) {
            // Identity matrix fallback
            writeIdentityMatrix(builder);
            return;
        }
        
        // Write column-major (GLSL std140 expects columns as vec4s)
        Vector4f col = new Vector4f();
        for (int c = 0; c < 4; c++) {
            mat.getColumn(c, col);
            builder.putFloat(col.x);
            builder.putFloat(col.y);
            builder.putFloat(col.z);
            builder.putFloat(col.w);
        }
    }
    
    /**
     * Writes identity matrix as fallback for null matrices.
     */
    private static void writeIdentityMatrix(Std140Builder builder) {
        // Column 0
        builder.putFloat(1f);
        builder.putFloat(0f);
        builder.putFloat(0f);
        builder.putFloat(0f);
        // Column 1
        builder.putFloat(0f);
        builder.putFloat(1f);
        builder.putFloat(0f);
        builder.putFloat(0f);
        // Column 2
        builder.putFloat(0f);
        builder.putFloat(0f);
        builder.putFloat(1f);
        builder.putFloat(0f);
        // Column 3
        builder.putFloat(0f);
        builder.putFloat(0f);
        builder.putFloat(0f);
        builder.putFloat(1f);
    }
    
    /**
     * Writes N floats with optional padding.
     */
    private static void writeFloats(Std140Builder builder, Object value, Floats annotation, String componentName) {
        float[] arr;
        
        if (value instanceof float[] fa) {
            arr = fa;
        } else if (value instanceof Float f) {
            arr = new float[] { f };
        } else {
            throw new IllegalArgumentException(
                "Cannot write component '" + componentName + "' as Floats: " +
                value.getClass().getSimpleName() +
                ". Must be float[] or Float."
            );
        }
        
        int count = annotation.count();
        if (arr.length < count) {
            throw new IllegalArgumentException(
                "float[] for @Floats(count=" + count + ") component '" + componentName + 
                "' has only " + arr.length + " elements"
            );
        }
        
        // Write the requested number of floats
        for (int i = 0; i < count; i++) {
            builder.putFloat(arr[i]);
        }
        
        // Pad to vec4 boundary if requested
        if (annotation.pad() && count % 4 != 0) {
            int remainder = count % 4;
            int paddingCount = 4 - remainder;
            for (int i = 0; i < paddingCount; i++) {
                builder.putFloat(0f);
            }
        }
    }
}
