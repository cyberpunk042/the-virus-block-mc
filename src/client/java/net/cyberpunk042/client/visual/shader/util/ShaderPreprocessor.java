package net.cyberpunk042.client.visual.shader.util;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GLSL Shader Preprocessor with #include support.
 * 
 * <p>Processes shader source before OpenGL compilation, replacing
 * {@code #include "filename.glsl"} directives with file contents.</p>
 * 
 * <h3>Usage in shaders:</h3>
 * <pre>
 * #include "include/sdf_library.glsl"
 * #include "include/raymarching.glsl"
 * </pre>
 * 
 * <h3>Include paths:</h3>
 * <p>Paths are relative to the shader's directory. For a shader at
 * {@code shaders/post/shockwave_ring.fsh}, the include path
 * {@code include/math.glsl} resolves to {@code shaders/post/include/math.glsl}.</p>
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li>Recursive includes (includes can include other files)</li>
 *   <li>Circular dependency detection (prevents infinite loops)</li>
 *   <li>Line number tracking for error messages</li>
 *   <li>Only processes files from "the-virus-block" namespace</li>
 * </ul>
 */
public final class ShaderPreprocessor {
    
    private ShaderPreprocessor() {} // Utility class
    
    // Pattern to match: #include "path/to/file.glsl" with optional trailing comment
    private static final Pattern INCLUDE_PATTERN = 
        Pattern.compile("^\\s*#include\\s+\"([^\"]+)\"\\s*(?://.*)?$", Pattern.MULTILINE);
    
    // Namespace to process (only our mod's shaders)
    private static final String OUR_NAMESPACE = "the-virus-block";
    
    // Cache for processed shader sources (key: identifier path, value: processed source)
    private static final Map<String, String> PREPROCESSED_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Process shader source, resolving all #include directives.
     * 
     * @param source Raw shader source with #include directives
     * @param baseIdentifier Identifier of the shader file (for resolving relative paths)
     * @return Processed shader source with includes resolved
     */
    public static String process(String source, Identifier baseIdentifier) {
        // Only process our namespace
        if (!OUR_NAMESPACE.equals(baseIdentifier.getNamespace())) {
            return source;
        }
        
        // Check cache first
        String cacheKey = baseIdentifier.toString();
        String cached = PREPROCESSED_CACHE.get(cacheKey);
        if (cached != null) {
            Logging.RENDER.topic("shader_preprocess")
                .kv("shader", baseIdentifier.getPath())
                .debug("Using cached preprocessed source");
            return cached;
        }
        
        // Track included files to prevent circular dependencies
        Set<String> includedFiles = new HashSet<>();
        includedFiles.add(baseIdentifier.getPath());
        
        try {
            String processed = processIncludes(source, baseIdentifier, includedFiles, 0);
            // Cache the result
            PREPROCESSED_CACHE.put(cacheKey, processed);
            return processed;
        } catch (Exception e) {
            Logging.RENDER.topic("shader_preprocess")
                .kv("shader", baseIdentifier.toString())
                .kv("error", e.getMessage())
                .error("Failed to process shader includes");
            return source; // Return original on error
        }
    }
    
    /**
     * Clears the preprocessed shader cache.
     * Called on resource pack reload to ensure new shader versions are loaded.
     */
    public static void invalidateCache() {
        int count = PREPROCESSED_CACHE.size();
        PREPROCESSED_CACHE.clear();
        Logging.RENDER.topic("shader_preprocess")
            .kv("cleared", count)
            .info("Preprocessor cache invalidated");
    }
    
    /**
     * Recursively process includes in the source.
     */
    private static String processIncludes(String source, Identifier baseId, 
                                          Set<String> includedFiles, int depth) 
            throws IOException {
        
        // Prevent too deep nesting (safety limit)
        if (depth > 10) {
            throw new IOException("Include depth exceeded (>10 levels) - possible circular dependency");
        }
        
        Matcher matcher = INCLUDE_PATTERN.matcher(source);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String includePath = matcher.group(1);
            
            // Resolve relative path from base shader
            String resolvedPath = resolvePath(baseId.getPath(), includePath);
            
            // Skip if already included (mimics include guard behavior)
            // This is NOT an error - it's normal for multiple files to include the same dependency
            if (includedFiles.contains(resolvedPath)) {
                // Replace with comment, content is already in the output
                matcher.appendReplacement(result, "// (already included: " + includePath + ")");
                continue;
            }
            
            // Mark as included BEFORE loading to prevent true circular dependencies
            includedFiles.add(resolvedPath);
            
            // Load included file
            Identifier includeId = Identifier.of(OUR_NAMESPACE, resolvedPath);
            String includeContent = loadShaderFile(includeId);
            
            if (includeContent == null) {
                Logging.RENDER.topic("shader_preprocess")
                    .kv("include", includePath)
                    .kv("resolved", resolvedPath)
                    .warn("Include file not found, keeping directive as comment");
                matcher.appendReplacement(result, "// INCLUDE NOT FOUND: " + includePath);
                continue;
            }
            
            // Recursively process includes in the included file
            String processedContent = processIncludes(includeContent, includeId, includedFiles, depth + 1);
            
            // Add markers for debugging
            String replacement = "\n// ═══ BEGIN INCLUDE: " + includePath + " ═══\n" +
                                 processedContent +
                                 "\n// ═══ END INCLUDE: " + includePath + " ═══\n";
            
            // Escape replacement string for regex
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Resolve include path relative to base shader path.
     * 
     * <p>NOTE: Minecraft's ShaderLoader strips the "shaders/" prefix from identifiers,
     * so we receive paths like "post/shockwave_ring" instead of "shaders/post/shockwave_ring".
     * We must prepend "shaders/" when loading include files from resources.</p>
     * 
     * <p>This method also handles relative paths like "../core/constants.glsl" by
     * normalizing the result to remove ".." components.</p>
     * 
     * @param basePath Path like "post/shockwave_ring" (without shaders/ prefix)
     * @param includePath Path like "include/math.glsl" or "../core/constants.glsl"
     * @return Resolved path like "shaders/post/include/math.glsl"
     */
    private static String resolvePath(String basePath, String includePath) {
        // Prepend "shaders/" since ShaderLoader strips it from the identifier
        String fullBasePath = basePath;
        if (!basePath.startsWith("shaders/")) {
            fullBasePath = "shaders/" + basePath;
        }
        
        // Get directory of base file
        int lastSlash = fullBasePath.lastIndexOf('/');
        String baseDir = (lastSlash >= 0) ? fullBasePath.substring(0, lastSlash + 1) : "shaders/";
        
        // Combine and normalize path
        String combined = baseDir + includePath;
        return normalizePath(combined);
    }
    
    /**
     * Normalize a path by resolving ".." and "." components.
     * 
     * @param path Path that may contain ".." or "." components
     * @return Normalized path
     */
    private static String normalizePath(String path) {
        // Split into components
        String[] parts = path.split("/");
        java.util.LinkedList<String> stack = new java.util.LinkedList<>();
        
        for (String part : parts) {
            if (part.equals("..")) {
                if (!stack.isEmpty() && !stack.getLast().equals("..")) {
                    stack.removeLast();
                }
            } else if (!part.equals(".") && !part.isEmpty()) {
                stack.addLast(part);
            }
        }
        
        return String.join("/", stack);
    }
    
    /**
     * Load shader file content from resources.
     * 
     * @param identifier Resource identifier
     * @return File content as string, or null if not found
     */
    private static String loadShaderFile(Identifier identifier) {
        var client = MinecraftClient.getInstance();
        if (client == null) return null;
        
        var resourceManager = client.getResourceManager();
        if (resourceManager == null) return null;
        
        try {
            var resource = resourceManager.getResource(identifier);
            if (resource.isEmpty()) return null;
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if a shader source contains any #include directives.
     * Useful for fast-path optimization.
     */
    public static boolean hasIncludes(String source) {
        return INCLUDE_PATTERN.matcher(source).find();
    }
}
