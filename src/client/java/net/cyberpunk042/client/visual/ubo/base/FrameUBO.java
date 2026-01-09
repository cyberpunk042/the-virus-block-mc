package net.cyberpunk042.client.visual.ubo.base;

import net.cyberpunk042.client.visual.ubo.annotation.UBOStruct;
import net.cyberpunk042.client.visual.ubo.annotation.Vec4;

/**
 * Frame UBO - Global Per-Frame Data.
 * 
 * <h2>Domain</h2>
 * <p>This is the outermost layer of the UBO onion architecture.
 * It contains data that is truly global and changes every frame.</p>
 * 
 * <h2>Contents</h2>
 * <ul>
 *   <li><b>time:</b> Accumulated time in seconds (for animation)</li>
 *   <li><b>deltaTime:</b> Time since last frame in seconds</li>
 *   <li><b>frameIndex:</b> Monotonically increasing frame counter</li>
 *   <li><b>layoutVersion:</b> UBO layout version for mismatch detection</li>
 * </ul>
 * 
 * <h2>Update Frequency</h2>
 * <p><b>Every frame.</b> This is the smallest UBO and the best candidate
 * for ring-buffering and persistent mapping if performance tuning is needed.</p>
 * 
 * <h2>GLSL Usage</h2>
 * <pre>{@code
 * #include "include/ubo/frame.glsl"
 * 
 * void main() {
 *     float time = FrameTime.x;
 *     float dt = FrameTime.y;
 *     float frame = FrameTime.z;
 *     float version = FrameTime.w;
 * }
 * }</pre>
 * 
 * <h2>Binding</h2>
 * <p>Binding index: {@code 0} (see {@link net.cyberpunk042.client.visual.ubo.UBORegistry#FRAME_BINDING})</p>
 * 
 * <h2>Size</h2>
 * <p>16 bytes (1 vec4)</p>
 * 
 * @see net.cyberpunk042.client.visual.ubo.UBORegistry
 */
@UBOStruct(name = "FrameDataUBO")
public record FrameUBO(
    /**
     * Frame time vector.
     * <ul>
     *   <li><b>x:</b> time - Accumulated time in seconds since game start</li>
     *   <li><b>y:</b> deltaTime - Time since last frame (typically 0.016 at 60fps)</li>
     *   <li><b>z:</b> frameIndex - Frame counter (wraps at max float precision)</li>
     *   <li><b>w:</b> layoutVersion - UBO layout version (1.0 = initial)</li>
     * </ul>
     */
    @Vec4 FrameTimeVec4 frameTime
) {
    /** Current layout version for all base UBOs */
    public static final float CURRENT_LAYOUT_VERSION = 1.0f;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VEC4 RECORD - Clean pattern, no Vec4Serializable boilerplate needed
    // ReflectiveUBOWriter auto-detects record components by order
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Frame time vec4 components.
     * 
     * @param time       Accumulated time in seconds
     * @param deltaTime  Time since last frame
     * @param frameIndex Frame counter
     * @param layoutVersion UBO layout version
     */
    public record FrameTimeVec4(
        float time,
        float deltaTime,
        float frameIndex,
        float layoutVersion
    ) {
        /**
         * Creates FrameTimeVec4 with current layout version.
         */
        public static FrameTimeVec4 of(float time, float deltaTime, int frameIndex) {
            return new FrameTimeVec4(time, deltaTime, (float) frameIndex, CURRENT_LAYOUT_VERSION);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a FrameUBO from current frame state.
     * 
     * @param time Accumulated time in seconds
     * @param deltaTime Time since last frame
     * @param frameIndex Current frame number
     * @return New FrameUBO ready for binding
     */
    public static FrameUBO from(float time, float deltaTime, int frameIndex) {
        return new FrameUBO(FrameTimeVec4.of(time, deltaTime, frameIndex));
    }
}
