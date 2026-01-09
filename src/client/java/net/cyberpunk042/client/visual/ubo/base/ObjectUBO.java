package net.cyberpunk042.client.visual.ubo.base;

import net.cyberpunk042.client.visual.ubo.annotation.UBOStruct;
import net.cyberpunk042.client.visual.ubo.annotation.Vec4;

/**
 * Object UBO - Per-Instance Identity and Transform.
 * 
 * <h2>Domain</h2>
 * <p>The third layer of the UBO onion. Contains data that varies per object/instance
 * when rendering multiple instances of the same effect.</p>
 * 
 * <h2>Current Status</h2>
 * <p><b>STUB - Reserved for Future Use.</b></p>
 * <p>Currently, effects render one instance at a time. This UBO is reserved for 
 * future multi-instance optimizations using instanced rendering.</p>
 * 
 * <h2>Future Contents</h2>
 * <ul>
 *   <li><b>identity:</b> objectId, objectType, flags</li>
 *   <li><b>transform:</b> Position offset and scale</li>
 * </ul>
 * 
 * <h2>Binding</h2>
 * <p>Binding index: {@code 2}</p>
 * 
 * <h2>Size</h2>
 * <p>32 bytes (2 vec4)</p>
 */
@UBOStruct(name = "ObjectDataUBO")
public record ObjectUBO(
    /** Object identity: objectId, objectType, flags, reserved */
    @Vec4 IdentityVec4 identity,
    
    /** Object transform: offsetX, offsetY, offsetZ, scale */
    @Vec4 TransformVec4 transform
) {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VEC4 RECORDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Object identity: objectId, objectType, flags, reserved */
    public record IdentityVec4(float objectId, float objectType, float flags, float reserved) {
        public static final IdentityVec4 DEFAULT = new IdentityVec4(0, 0, 0, 0);
        
        public static IdentityVec4 of(int objectId, int objectType, int flags) {
            return new IdentityVec4((float) objectId, (float) objectType, (float) flags, 0);
        }
    }
    
    /** Object transform: offsetX, offsetY, offsetZ, scale */
    public record TransformVec4(float offsetX, float offsetY, float offsetZ, float scale) {
        public static final TransformVec4 DEFAULT = new TransformVec4(0, 0, 0, 1f);
        
        public static TransformVec4 of(float x, float y, float z, float scale) {
            return new TransformVec4(x, y, z, scale);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Default instance (no transform, id 0) */
    public static ObjectUBO defaultInstance() {
        return new ObjectUBO(IdentityVec4.DEFAULT, TransformVec4.DEFAULT);
    }
    
    /** Creates ObjectUBO for a specific instance */
    public static ObjectUBO from(int objectId, int objectType, float x, float y, float z, float scale) {
        return new ObjectUBO(
            IdentityVec4.of(objectId, objectType, 0),
            TransformVec4.of(x, y, z, scale)
        );
    }
}
