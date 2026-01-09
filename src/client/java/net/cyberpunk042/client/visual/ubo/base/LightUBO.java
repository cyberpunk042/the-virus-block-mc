package net.cyberpunk042.client.visual.ubo.base;

import net.cyberpunk042.client.visual.ubo.annotation.UBOStruct;
import net.cyberpunk042.client.visual.ubo.annotation.Vec4;

/**
 * Light UBO - Scene Lighting Data.
 * 
 * <h2>Domain</h2>
 * <p>The fifth layer of the UBO onion. Contains scene lighting information
 * for effects that need to interact with lights.</p>
 * 
 * <h2>Primary Use Case: Tornado Effect</h2>
 * <p>The Tornado effect requires two-point volumetric lighting with:
 * <ul>
 *   <li>Warm light (orange) on one side</li>
 *   <li>Cool light (green/cyan) on the other side</li>
 *   <li>Configurable positions, colors, and intensities</li>
 * </ul>
 * </p>
 * 
 * <h2>Contents</h2>
 * <ul>
 *   <li><b>header:</b> Light count, ambient color</li>
 *   <li><b>light0-3:</b> Up to 4 point lights with position, color, direction</li>
 * </ul>
 * 
 * <h2>Binding</h2>
 * <p>Binding index: {@code 4}</p>
 * 
 * <h2>Size</h2>
 * <p>208 bytes (13 vec4)</p>
 */
@UBOStruct(name = "LightDataUBO")
public record LightUBO(
    /** Header: lightCount, ambientR, ambientG, ambientB */
    @Vec4 HeaderVec4 header,
    
    // Light 0 (Tornado: Warm/Orange)
    @Vec4 LightPosVec4 light0Position,
    @Vec4 LightColorVec4 light0Color,
    @Vec4 LightDirVec4 light0Direction,
    
    // Light 1 (Tornado: Cool/Cyan)
    @Vec4 LightPosVec4 light1Position,
    @Vec4 LightColorVec4 light1Color,
    @Vec4 LightDirVec4 light1Direction,
    
    // Light 2 (Reserved)
    @Vec4 LightPosVec4 light2Position,
    @Vec4 LightColorVec4 light2Color,
    @Vec4 LightDirVec4 light2Direction,
    
    // Light 3 (Reserved)
    @Vec4 LightPosVec4 light3Position,
    @Vec4 LightColorVec4 light3Color,
    @Vec4 LightDirVec4 light3Direction
) {
    /** Maximum number of lights supported */
    public static final int MAX_LIGHTS = 4;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VEC4 RECORDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Light header: lightCount, ambientR, ambientG, ambientB */
    public record HeaderVec4(float lightCount, float ambientR, float ambientG, float ambientB) {
        public static HeaderVec4 of(int lightCount, float ambR, float ambG, float ambB) {
            return new HeaderVec4((float) lightCount, ambR, ambG, ambB);
        }
        public static final HeaderVec4 NO_LIGHTS = new HeaderVec4(0, 0.01f, 0.01f, 0.01f);
    }
    
    /** Light position: x, y, z, strength */
    public record LightPosVec4(float x, float y, float z, float strength) {
        public static LightPosVec4 of(float x, float y, float z, float strength) {
            return new LightPosVec4(x, y, z, strength);
        }
        public static final LightPosVec4 ZERO = new LightPosVec4(0, 0, 0, 0);
    }
    
    /** Light color: r, g, b, attenuation */
    public record LightColorVec4(float r, float g, float b, float attenuation) {
        public static LightColorVec4 of(float r, float g, float b, float atten) {
            return new LightColorVec4(r, g, b, atten);
        }
        /** Warm orange (Tornado Light 0 default) */
        public static final LightColorVec4 WARM_ORANGE = new LightColorVec4(0.6f, 0.25f, 0.15f, 1f);
        /** Cool cyan (Tornado Light 1 default) */
        public static final LightColorVec4 COOL_CYAN = new LightColorVec4(0.1f, 1f, 0.6f, 1f);
        public static final LightColorVec4 ZERO = new LightColorVec4(0, 0, 0, 0);
    }
    
    /** Light direction: x, y, z, angle (for spotlights) */
    public record LightDirVec4(float x, float y, float z, float angle) {
        /** Point light (no direction) */
        public static final LightDirVec4 POINT = new LightDirVec4(0, -1, 0, 0);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Default lights (ambient only, no active lights) */
    public static LightUBO defaultLights() {
        return ambientOnly(0.01f, 0.01f, 0.002f);
    }
    
    /** Ambient only, no active point lights */
    public static LightUBO ambientOnly(float ambR, float ambG, float ambB) {
        return new LightUBO(
            HeaderVec4.of(0, ambR, ambG, ambB),
            LightPosVec4.ZERO, LightColorVec4.ZERO, LightDirVec4.POINT,
            LightPosVec4.ZERO, LightColorVec4.ZERO, LightDirVec4.POINT,
            LightPosVec4.ZERO, LightColorVec4.ZERO, LightDirVec4.POINT,
            LightPosVec4.ZERO, LightColorVec4.ZERO, LightDirVec4.POINT
        );
    }
    
    /**
     * Creates a LightUBO configured for Tornado effect.
     * <p>Two-point volumetric lighting with warm and cool colors.</p>
     */
    public static LightUBO forTornado(
            float centerX, float centerY, float centerZ,
            float light0OffsetX, float light0OffsetY, float light0OffsetZ, float light0Strength,
            float light1OffsetX, float light1OffsetY, float light1OffsetZ, float light1Strength,
            float ambR, float ambG, float ambB
    ) {
        return new LightUBO(
            HeaderVec4.of(2, ambR, ambG, ambB),
            
            // Light 0: Warm orange
            LightPosVec4.of(centerX + light0OffsetX, centerY + light0OffsetY, centerZ + light0OffsetZ, light0Strength),
            LightColorVec4.WARM_ORANGE,
            LightDirVec4.POINT,
            
            // Light 1: Cool cyan
            LightPosVec4.of(centerX + light1OffsetX, centerY + light1OffsetY, centerZ + light1OffsetZ, light1Strength),
            LightColorVec4.COOL_CYAN,
            LightDirVec4.POINT,
            
            // Light 2-3: Unused
            LightPosVec4.ZERO, LightColorVec4.ZERO, LightDirVec4.POINT,
            LightPosVec4.ZERO, LightColorVec4.ZERO, LightDirVec4.POINT
        );
    }
}
