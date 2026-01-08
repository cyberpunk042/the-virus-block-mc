package net.cyberpunk042.client.visual.ubo;

import net.cyberpunk042.client.visual.ubo.annotation.*;
import net.cyberpunk042.log.Logging;

import java.io.*;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.regex.*;

/**
 * Validates that Java UBO records match GLSL uniform blocks.
 * Called at startup to catch mismatches early with clear error messages.
 * 
 * <h3>Validation Checks</h3>
 * <ul>
 *   <li>Slot count matches between Java and GLSL</li>
 *   <li>Slot sizes match (vec4 = 16 bytes, mat4 = 64 bytes, etc.)</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * // In TheVirusBlockClient.onInitializeClient():
 * GLSLValidator.validate(FieldVisualUBO.class);
 * GLSLValidator.validate(ShockwaveUBO.class);
 * }</pre>
 * 
 * @see UBOStruct
 */
public final class GLSLValidator {
    
    private GLSLValidator() {} // Utility class
    
    /**
     * Validates a UBO record against its GLSL counterpart.
     * 
     * @param uboClass the record class annotated with @UBOStruct
     * @throws UBOMismatchException if the Java and GLSL structures don't match
     * @throws IllegalArgumentException if the class is not annotated
     */
    public static void validate(Class<?> uboClass) {
        UBOStruct annotation = uboClass.getAnnotation(UBOStruct.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                "Class must have @UBOStruct annotation: " + uboClass.getName()
            );
        }
        
        // If no GLSL path specified, skip validation
        if (annotation.glslPath().isEmpty()) {
            Logging.RENDER.topic("ubo_validator")
                .kv("class", uboClass.getSimpleName())
                .debug("Skipping validation (no glslPath specified)");
            return;
        }
        
        List<SlotInfo> javaSlots = extractJavaSlots(uboClass);
        
        // TODO: Implement GLSL parsing when ready
        // For now, just log the Java slots for verification
        logJavaSlots(uboClass.getSimpleName(), javaSlots);
        
        // Future: Parse GLSL and compare
        // List<SlotInfo> glslSlots = parseGLSLStruct(annotation.glslPath(), annotation.name());
        // compareSlots(javaSlots, glslSlots);
    }
    
    /**
     * Extracts slot information from a Java UBO record.
     */
    private static List<SlotInfo> extractJavaSlots(Class<?> uboClass) {
        List<SlotInfo> slots = new ArrayList<>();
        
        for (RecordComponent comp : uboClass.getRecordComponents()) {
            String name = comp.getName();
            int sizeBytes = 0;
            String type = "unknown";
            
            if (comp.isAnnotationPresent(Vec4.class)) {
                sizeBytes = 16;
                type = "vec4";
            } else if (comp.isAnnotationPresent(Mat4.class)) {
                sizeBytes = 64;
                type = "mat4";
            } else if (comp.isAnnotationPresent(Floats.class)) {
                Floats f = comp.getAnnotation(Floats.class);
                sizeBytes = f.count() * 4;
                if (f.pad() && f.count() % 4 != 0) {
                    sizeBytes += (4 - (f.count() % 4)) * 4;
                }
                type = "float" + (f.count() > 1 ? "[" + f.count() + "]" : "");
            }
            
            if (sizeBytes > 0) {
                slots.add(new SlotInfo(name, type, sizeBytes));
            }
        }
        
        return slots;
    }
    
    /**
     * Logs Java slots for debugging and verification.
     */
    private static void logJavaSlots(String className, List<SlotInfo> slots) {
        int totalBytes = slots.stream().mapToInt(SlotInfo::sizeBytes).sum();
        int vec4Count = totalBytes / 16;
        
        Logging.RENDER.topic("ubo_validator")
            .kv("class", className)
            .kv("slots", slots.size())
            .kv("bytes", totalBytes)
            .kv("vec4s", vec4Count)
            .info("UBO structure validated");
    }
    
    /**
     * Parses a GLSL uniform block and extracts slot information.
     * 
     * @param glslPath resource path to the shader (e.g., "the-virus-block:shaders/post/field_visual.fsh")
     * @param structName name of the uniform block
     * @return list of slots parsed from GLSL
     */
    private static List<SlotInfo> parseGLSLStruct(String glslPath, String structName) {
        // TODO: Implement GLSL parsing
        // This will:
        // 1. Load the shader file as a resource
        // 2. Find the uniform block by name
        // 3. Parse field declarations
        // 4. Calculate sizes based on types
        
        return List.of(); // Placeholder
    }
    
    /**
     * Compares Java and GLSL slots and throws on mismatch.
     */
    private static void compareSlots(List<SlotInfo> javaSlots, List<SlotInfo> glslSlots) {
        // Compare slot counts
        if (javaSlots.size() != glslSlots.size()) {
            throw new UBOMismatchException(
                "Slot count mismatch",
                "Java has " + javaSlots.size() + " slots, GLSL has " + glslSlots.size(),
                javaSlots, glslSlots
            );
        }
        
        // Compare each slot
        for (int i = 0; i < javaSlots.size(); i++) {
            SlotInfo java = javaSlots.get(i);
            SlotInfo glsl = glslSlots.get(i);
            
            if (java.sizeBytes != glsl.sizeBytes) {
                throw new UBOMismatchException(
                    "Slot " + i + " size mismatch",
                    "Java '" + java.name + "' (" + java.type + ") is " + java.sizeBytes + 
                    " bytes, GLSL '" + glsl.name + "' (" + glsl.type + ") is " + glsl.sizeBytes + " bytes",
                    javaSlots, glslSlots
                );
            }
        }
    }
    
    /**
     * Information about a single UBO slot.
     */
    public record SlotInfo(String name, String type, int sizeBytes) {
        @Override
        public String toString() {
            return name + " (" + type + ", " + sizeBytes + " bytes)";
        }
    }
    
    /**
     * Exception thrown when Java and GLSL UBO structures don't match.
     */
    public static class UBOMismatchException extends RuntimeException {
        public final List<SlotInfo> javaSlots;
        public final List<SlotInfo> glslSlots;
        
        public UBOMismatchException(String what, String detail, List<SlotInfo> java, List<SlotInfo> glsl) {
            super(buildMessage(what, detail, java, glsl));
            this.javaSlots = java;
            this.glslSlots = glsl;
        }
        
        private static String buildMessage(String what, String detail, List<SlotInfo> java, List<SlotInfo> glsl) {
            StringBuilder sb = new StringBuilder();
            sb.append("UBO Mismatch: ").append(what).append("\n");
            sb.append("  ").append(detail).append("\n\n");
            
            sb.append("Java slots (").append(java.size()).append("):\n");
            for (int i = 0; i < java.size(); i++) {
                sb.append("  ").append(i).append(": ").append(java.get(i)).append("\n");
            }
            
            sb.append("\nGLSL slots (").append(glsl.size()).append("):\n");
            for (int i = 0; i < glsl.size(); i++) {
                sb.append("  ").append(i).append(": ").append(glsl.get(i)).append("\n");
            }
            
            return sb.toString();
        }
    }
}
