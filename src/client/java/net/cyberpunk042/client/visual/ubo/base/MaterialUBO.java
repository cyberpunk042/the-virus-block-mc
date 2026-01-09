package net.cyberpunk042.client.visual.ubo.base;

import net.cyberpunk042.client.visual.ubo.annotation.UBOStruct;
import net.cyberpunk042.client.visual.ubo.annotation.Vec4;

/**
 * Material UBO - Per-Material Surface Constants.
 * 
 * <h2>Domain</h2>
 * <p>The fourth layer of the UBO onion. Contains surface properties that define
 * how an object/effect interacts with light and rendering.</p>
 * 
 * <h2>Current Status</h2>
 * <p><b>STUB - Reserved for Future Use.</b></p>
 * <p>Currently, material-like properties are embedded in effect configs.
 * This UBO is reserved for a future unified material system.</p>
 * 
 * <h2>Future Contents</h2>
 * <ul>
 *   <li><b>albedo:</b> Base color (RGBA)</li>
 *   <li><b>properties:</b> roughness, metallic, emission, reserved</li>
 * </ul>
 * 
 * <h2>Binding</h2>
 * <p>Binding index: {@code 3}</p>
 * 
 * <h2>Size</h2>
 * <p>32 bytes (2 vec4)</p>
 */
@UBOStruct(name = "MaterialDataUBO")
public record MaterialUBO(
    /** Material albedo: r, g, b, alpha */
    @Vec4 AlbedoVec4 albedo,
    
    /** Material properties: roughness, metallic, emission, reserved */
    @Vec4 PropertiesVec4 properties
) {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VEC4 RECORDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Material albedo: r, g, b, alpha */
    public record AlbedoVec4(float r, float g, float b, float a) {
        public static final AlbedoVec4 WHITE = new AlbedoVec4(1, 1, 1, 1);
        
        public static AlbedoVec4 of(float r, float g, float b, float a) {
            return new AlbedoVec4(r, g, b, a);
        }
    }
    
    /** Material properties: roughness, metallic, emission, reserved */
    public record PropertiesVec4(float roughness, float metallic, float emission, float reserved) {
        public static final PropertiesVec4 DEFAULT = new PropertiesVec4(0.5f, 0, 0, 0);
        
        public static PropertiesVec4 of(float roughness, float metallic, float emission) {
            return new PropertiesVec4(roughness, metallic, emission, 0);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Default material (white, rough, non-metallic) */
    public static MaterialUBO defaultMaterial() {
        return new MaterialUBO(AlbedoVec4.WHITE, PropertiesVec4.DEFAULT);
    }
    
    /** Creates MaterialUBO with specified properties */
    public static MaterialUBO from(float r, float g, float b, float a,
            float roughness, float metallic, float emission) {
        return new MaterialUBO(
            AlbedoVec4.of(r, g, b, a),
            PropertiesVec4.of(roughness, metallic, emission)
        );
    }
}
