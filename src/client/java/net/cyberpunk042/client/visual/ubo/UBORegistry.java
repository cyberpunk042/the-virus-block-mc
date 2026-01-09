package net.cyberpunk042.client.visual.ubo;

/**
 * Centralized UBO Registry - Single Source of Truth for All UBO Bindings.
 * 
 * <h2>Purpose</h2>
 * <p>This registry defines all UBO binding indices used throughout the shader system.
 * All binding calls MUST use constants from this registry - no magic numbers allowed.</p>
 * 
 * <h2>Binding Convention</h2>
 * <ul>
 *   <li><b>0–9:</b> Base UBOs (engine-level, always available)</li>
 *   <li><b>10–19:</b> Pass/Post UBOs (reserved for future)</li>
 *   <li><b>20–29:</b> Effect Config UBOs (preset/style, rarely updated)</li>
 *   <li><b>30–39:</b> Effect Runtime UBOs (per-frame instance state)</li>
 * </ul>
 * 
 * <h2>Domain Ownership (Onion Architecture)</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────┐
 * │                    Frame (0)                         │  ← Global per-frame
 * │  ┌─────────────────────────────────────────────┐    │
 * │  │                Camera (1)                    │    │  ← View definition
 * │  │  ┌─────────────────────────────────────┐    │    │
 * │  │  │             Object (2)               │    │    │  ← Per-instance
 * │  │  │  ┌─────────────────────────────┐    │    │    │
 * │  │  │  │         Material (3)         │    │    │    │  ← Surface props
 * │  │  │  │  ┌─────────────────────┐    │    │    │    │
 * │  │  │  │  │      Light (4)       │    │    │    │    │  ← Scene lights
 * │  │  │  │  │  ┌─────────────┐    │    │    │    │    │
 * │  │  │  │  │  │EffectConfig │    │    │    │    │    │  ← Preset/style
 * │  │  │  │  │  │   (20+)     │    │    │    │    │    │
 * │  │  │  │  │  └─────────────┘    │    │    │    │    │
 * │  │  │  │  └─────────────────────┘    │    │    │    │
 * │  │  │  └─────────────────────────────┘    │    │    │
 * │  │  └─────────────────────────────────────┘    │    │
 * │  └─────────────────────────────────────────────┘    │
 * └─────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Update Frequency</h2>
 * <ul>
 *   <li><b>Frame:</b> Every frame (smallest payload, ring-buffer candidate)</li>
 *   <li><b>Camera:</b> Every frame or on change</li>
 *   <li><b>Object:</b> Per draw/instance</li>
 *   <li><b>Material:</b> Rarely (cached)</li>
 *   <li><b>Light:</b> On light changes (batched)</li>
 *   <li><b>EffectConfig:</b> On preset change (rare)</li>
 *   <li><b>EffectRuntime:</b> Per frame if CPU-driven</li>
 * </ul>
 * 
 * @see net.cyberpunk042.client.visual.ubo.base.FrameUBO
 * @see net.cyberpunk042.client.visual.ubo.base.CameraUBO
 * @see net.cyberpunk042.client.visual.ubo.base.ObjectUBO
 * @see net.cyberpunk042.client.visual.ubo.base.MaterialUBO
 * @see net.cyberpunk042.client.visual.ubo.base.LightUBO
 */
public final class UBORegistry {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BASE UBOs (0–9) - Engine Level, Always Available
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Frame UBO binding index.
     * <p>Contains global per-frame data: time, deltaTime, frameIndex, layoutVersion.</p>
     * <p><b>Update frequency:</b> Every frame</p>
     * <p><b>GLSL block:</b> {@code layout(std140) uniform FrameData}</p>
     */
    public static final int FRAME_BINDING = 0;
    
    /**
     * Camera UBO binding index.
     * <p>Contains view definition: position, forward, up, matrices, fov, aspect, near/far.</p>
     * <p><b>Update frequency:</b> Every frame (or on change)</p>
     * <p><b>GLSL block:</b> {@code layout(std140) uniform CameraData}</p>
     */
    public static final int CAMERA_BINDING = 1;
    
    /**
     * Object UBO binding index.
     * <p>Contains per-instance data: identity, transform, objectId.</p>
     * <p><b>Update frequency:</b> Per draw/instance</p>
     * <p><b>GLSL block:</b> {@code layout(std140) uniform ObjectData}</p>
     * <p><b>Status:</b> Reserved for future multi-instance effects</p>
     */
    public static final int OBJECT_BINDING = 2;
    
    /**
     * Material UBO binding index.
     * <p>Contains per-material constants: surface properties, textures references.</p>
     * <p><b>Update frequency:</b> Rarely (cached)</p>
     * <p><b>GLSL block:</b> {@code layout(std140) uniform MaterialData}</p>
     * <p><b>Status:</b> Reserved for future material system</p>
     */
    public static final int MATERIAL_BINDING = 3;
    
    /**
     * Light UBO binding index.
     * <p>Contains scene lighting: light positions, colors, intensities, types.</p>
     * <p><b>Update frequency:</b> On light changes (batched)</p>
     * <p><b>GLSL block:</b> {@code layout(std140) uniform LightData}</p>
     * <p><b>Note:</b> Required for Tornado effect (two-point volumetric lighting)</p>
     */
    public static final int LIGHT_BINDING = 4;
    
    // Reserved: 5-9 for future base UBOs
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PASS/POST UBOs (10–19) - Reserved
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Reserved for future pass-specific data
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EFFECT CONFIG UBOs (20–29) - Preset/Style, Rarely Updated
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Field Visual Config UBO binding index.
     * <p>Contains preset/style parameters for Energy Orb, Geodesic, and related effects.</p>
     * <p><b>Update frequency:</b> On preset change (rare)</p>
     */
    public static final int FIELD_VISUAL_CONFIG_BINDING = 20;
    
    /**
     * Virus Block Config UBO binding index.
     * <p>Contains preset/style for virus block visual effects.</p>
     */
    public static final int VIRUS_BLOCK_CONFIG_BINDING = 21;
    
    /**
     * Shockwave Config UBO binding index.
     * <p>Contains preset/style for shockwave ring effects.</p>
     */
    public static final int SHOCKWAVE_CONFIG_BINDING = 22;
    
    /**
     * Magic Circle Config UBO binding index.
     * <p>Contains preset/style for magic circle effects.</p>
     */
    public static final int MAGIC_CIRCLE_CONFIG_BINDING = 23;
    
    /**
     * Tornado Config UBO binding index.
     * <p>Contains preset/style for tornado volumetric effect.</p>
     */
    public static final int TORNADO_CONFIG_BINDING = 24;
    
    // Reserved: 25-29 for future effect configs
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EFFECT RUNTIME UBOs (30–39) - Per-Frame Instance State
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Field Visual Runtime UBO binding index.
     * <p>Contains per-frame instance state: center, radius, phase, intensity.</p>
     * <p><b>Update frequency:</b> Per frame (if CPU-driven)</p>
     */
    public static final int FIELD_VISUAL_RUNTIME_BINDING = 30;
    
    /**
     * Shockwave Runtime UBO binding index.
     * <p>Contains per-frame state: ring radius, progress, animation phase.</p>
     */
    public static final int SHOCKWAVE_RUNTIME_BINDING = 31;
    
    // Reserved: 32-39 for future effect runtimes
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GLSL BLOCK NAMES - Must Match Shader Declarations
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** GLSL block name for Frame UBO */
    public static final String FRAME_BLOCK_NAME = "FrameData";
    
    /** GLSL block name for Camera UBO */
    public static final String CAMERA_BLOCK_NAME = "CameraData";
    
    /** GLSL block name for Object UBO */
    public static final String OBJECT_BLOCK_NAME = "ObjectData";
    
    /** GLSL block name for Material UBO */
    public static final String MATERIAL_BLOCK_NAME = "MaterialData";
    
    /** GLSL block name for Light UBO */
    public static final String LIGHT_BLOCK_NAME = "LightData";
    
    /** GLSL block name for Field Visual Config UBO */
    public static final String FIELD_VISUAL_CONFIG_BLOCK_NAME = "FieldVisualConfig";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXPECTED SIZES (Bytes) - For Validation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Expected size of FrameUBO in bytes (1 vec4 = 16 bytes) */
    public static final int FRAME_EXPECTED_SIZE = 16;
    
    /** Expected size of CameraUBO in bytes (4 vec4 + 2 mat4 + 2 reserved = 224 bytes) */
    public static final int CAMERA_EXPECTED_SIZE = 224;
    
    /** Expected size of ObjectUBO in bytes (2 vec4 = 32 bytes) */
    public static final int OBJECT_EXPECTED_SIZE = 32;
    
    /** Expected size of MaterialUBO in bytes (2 vec4 = 32 bytes) */
    public static final int MATERIAL_EXPECTED_SIZE = 32;
    
    /** Expected size of LightUBO in bytes (header + 4 lights * 3 vec4 = 208 bytes) */
    public static final int LIGHT_EXPECTED_SIZE = 208;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validates that a calculated size matches expected size.
     * 
     * @param blockName Name of the UBO block (for error messages)
     * @param calculatedSize Size calculated from record components
     * @param expectedSize Expected size from this registry
     * @throws IllegalStateException if sizes don't match
     */
    public static void validateSize(String blockName, int calculatedSize, int expectedSize) {
        if (calculatedSize != expectedSize) {
            throw new IllegalStateException(String.format(
                "[UBO] Size mismatch for %s: calculated=%d, expected=%d. " +
                "Check @Vec4/@Mat4 annotations and record field order.",
                blockName, calculatedSize, expectedSize
            ));
        }
    }
    
    /**
     * Logs UBO registration for debugging.
     * 
     * @param blockName Name of the UBO block
     * @param binding Binding index
     * @param size Size in bytes
     */
    public static void logRegistration(String blockName, int binding, int size) {
        System.out.printf("[UBO] %s: binding=%d, size=%d bytes%n", blockName, binding, size);
    }
    
    // Private constructor - utility class
    private UBORegistry() {}
}
