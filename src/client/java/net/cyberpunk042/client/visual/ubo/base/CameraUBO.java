package net.cyberpunk042.client.visual.ubo.base;

import net.cyberpunk042.client.visual.ubo.annotation.UBOStruct;
import net.cyberpunk042.client.visual.ubo.annotation.Vec4;
import net.cyberpunk042.client.visual.ubo.annotation.Mat4;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Camera UBO - View Definition and Matrices.
 * 
 * <h2>Domain</h2>
 * <p>The second layer of the UBO onion. Contains everything needed to define
 * the camera view: position, orientation, projection parameters, and matrices.</p>
 * 
 * <h2>Contents</h2>
 * <ul>
 *   <li><b>position:</b> Camera world position (xyz) + reserved (w)</li>
 *   <li><b>forward:</b> Camera forward vector (xyz) + aspect ratio (w)</li>
 *   <li><b>up:</b> Camera up vector (xyz) + field of view in radians (w)</li>
 *   <li><b>clip:</b> Near plane (x), far plane (y), isFlying flag (z), reserved (w)</li>
 *   <li><b>viewProj:</b> Combined view-projection matrix</li>
 *   <li><b>invViewProj:</b> Inverse of view-projection matrix (for ray reconstruction)</li>
 *   <li><b>reserved:</b> Reserved for future expansion (e.g., PrevViewProj for motion vectors)</li>
 * </ul>
 * 
 * <h2>Update Frequency</h2>
 * <p><b>Every frame</b> (or on change if camera is static)</p>
 * 
 * <h2>GLSL Usage</h2>
 * <pre>{@code
 * #include "include/ubo/camera.glsl"
 * 
 * void main() {
 *     vec3 camPos = CameraPosition.xyz;
 *     vec3 forward = CameraForward.xyz;
 *     float aspect = CameraForward.w;
 *     vec3 up = CameraUp.xyz;
 *     float fov = CameraUp.w;
 *     float near = CameraClip.x;
 *     float far = CameraClip.y;
 *     bool flying = CameraClip.z > 0.5;
 *     
 *     // Ray reconstruction
 *     vec4 worldPos = InvViewProj * clipPos;
 *     vec3 worldDir = normalize(worldPos.xyz / worldPos.w - camPos);
 * }
 * }</pre>
 * 
 * <h2>Binding</h2>
 * <p>Binding index: {@code 1} (see {@link net.cyberpunk042.client.visual.ubo.UBORegistry#CAMERA_BINDING})</p>
 * 
 * <h2>Size</h2>
 * <p>224 bytes (4 vec4 + 2 mat4 + 2 reserved vec4)</p>
 * 
 * @see net.cyberpunk042.client.visual.ubo.UBORegistry
 */
@UBOStruct(name = "CameraDataUBO")
public record CameraUBO(
    /** Camera world position: xyz=pos, w=reserved */
    @Vec4 PositionVec4 position,
    
    /** Camera forward direction: xyz=forward, w=aspect ratio */
    @Vec4 ForwardVec4 forward,
    
    /** Camera up direction: xyz=up, w=fov (radians) */
    @Vec4 UpVec4 up,
    
    /** Clip planes: x=near, y=far, z=isFlying, w=reserved */
    @Vec4 ClipVec4 clip,
    
    /** View-projection matrix (world to clip) */
    @Mat4 Matrix4f viewProj,
    
    /** Inverse view-projection (clip to world, for ray reconstruction) */
    @Mat4 Matrix4f invViewProj,
    
    /** Reserved slot 1 (future: PrevViewProj for motion vectors) */
    @Vec4 ReservedVec4 reserved1,
    
    /** Reserved slot 2 */
    @Vec4 ReservedVec4 reserved2
) {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VEC4 RECORDS - Clean pattern, auto-detected by ReflectiveUBOWriter
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Camera position: x, y, z, reserved */
    public record PositionVec4(float x, float y, float z, float w) {
        public static PositionVec4 from(Vector3f pos) {
            return new PositionVec4(pos.x, pos.y, pos.z, 0f);
        }
        public static PositionVec4 from(float x, float y, float z) {
            return new PositionVec4(x, y, z, 0f);
        }
    }
    
    /** Camera forward: x, y, z, aspect ratio */
    public record ForwardVec4(float x, float y, float z, float aspect) {
        public static ForwardVec4 from(Vector3f forward, float aspect) {
            return new ForwardVec4(forward.x, forward.y, forward.z, aspect);
        }
    }
    
    /** Camera up: x, y, z, fov (radians) */
    public record UpVec4(float x, float y, float z, float fov) {
        public static UpVec4 from(Vector3f up, float fov) {
            return new UpVec4(up.x, up.y, up.z, fov);
        }
    }
    
    /** Clip planes: near, far, isFlying, reserved */
    public record ClipVec4(float near, float far, float isFlying, float reserved) {
        public static ClipVec4 from(float near, float far, boolean isFlying) {
            return new ClipVec4(near, far, isFlying ? 1f : 0f, 0f);
        }
    }
    
    /** Reserved vec4 (all zeros) */
    public record ReservedVec4(float x, float y, float z, float w) {
        public static final ReservedVec4 ZERO = new ReservedVec4(0, 0, 0, 0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Creates a CameraUBO from camera state.
     */
    public static CameraUBO from(
            Vector3f position,
            Vector3f forward,
            Vector3f up,
            float fov,
            float aspect,
            float near,
            float far,
            boolean isFlying,
            Matrix4f viewProj,
            Matrix4f invViewProj
    ) {
        return new CameraUBO(
            PositionVec4.from(position),
            ForwardVec4.from(forward, aspect),
            UpVec4.from(up, fov),
            ClipVec4.from(near, far, isFlying),
            viewProj,
            invViewProj,
            ReservedVec4.ZERO,
            ReservedVec4.ZERO
        );
    }
    
    /**
     * Creates a CameraUBO from individual float values.
     */
    public static CameraUBO from(
            float posX, float posY, float posZ,
            float fwdX, float fwdY, float fwdZ,
            float upX, float upY, float upZ,
            float fov, float aspect,
            float near, float far,
            boolean isFlying,
            Matrix4f viewProj,
            Matrix4f invViewProj
    ) {
        return new CameraUBO(
            new PositionVec4(posX, posY, posZ, 0),
            new ForwardVec4(fwdX, fwdY, fwdZ, aspect),
            new UpVec4(upX, upY, upZ, fov),
            ClipVec4.from(near, far, isFlying),
            viewProj,
            invViewProj,
            ReservedVec4.ZERO,
            ReservedVec4.ZERO
        );
    }
}
