package net.cyberpunk042.client.visual.shader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;
import net.cyberpunk042.log.Logging;

import java.util.Set;

/**
 * Manages custom post-processing effects for testing depth buffer access.
 * 
 * <p>Modes:
 * <ul>
 *   <li>0 = OFF</li>
 *   <li>1 = Debug quadrants (depth, inverted, scene, blend)</li>
 *   <li>2 = Full-screen depth visualization</li>
 *   <li>3 = Passthrough (scene unchanged - confirms pipeline)</li>
 *   <li>4 = Red tint (confirms shader running)</li>
 *   <li>5 = SHOCKWAVE RING (terrain-conforming ring!)</li>
 * </ul>
 * 
 * <p>Commands:
 * <ul>
 *   <li>/depthtest - cycle through modes</li>
 *   <li>/depthtest [0-5] - set specific mode</li>
 * </ul>
 */
public class DepthTestShader {
    
    // Shader IDs for each mode
    private static final Identifier[] SHADER_IDS = {
        null, // Mode 0 = OFF
        Identifier.of("the-virus-block", "depth_test"),        // Mode 1 = Debug quadrants
        Identifier.of("the-virus-block", "depth_full"),        // Mode 2 = Full depth
        Identifier.of("the-virus-block", "depth_passthrough"), // Mode 3 = Passthrough
        Identifier.of("the-virus-block", "depth_redtint"),     // Mode 4 = Red tint
        Identifier.of("the-virus-block", "shockwave_ring")     // Mode 5 = SHOCKWAVE!
    };
    
    // Use the SAME external targets as the transparency shader
    // This ensures all framebuffers (including depth) are properly bound
    private static final Set<Identifier> REQUIRED_TARGETS = DefaultFramebufferSet.STAGES;
    
    // Current mode (0 = off)
    private static int mode = 0;
    private static final int MAX_MODE = 5;
    
    // Mode names for display
    private static final String[] MODE_NAMES = {
        "OFF",
        "Debug Quadrants",
        "Full Depth",
        "Passthrough", 
        "Red Tint",
        "§b§lSHOCKWAVE RING§r"
    };
    
    /**
     * Initializes the depth test shader system.
     */
    public static void init() {
        Logging.RENDER.topic("shader")
            .info("[DepthTestShader] Initialized. Use /depthtest to cycle modes.");
    }
    
    /**
     * Gets the current mode.
     */
    public static int getMode() {
        return mode;
    }
    
    /**
     * Sets the mode directly.
     */
    public static void setMode(int newMode) {
        mode = Math.max(0, Math.min(newMode, MAX_MODE));
        Logging.RENDER.topic("depth_test")
            .kv("mode", mode)
            .kv("name", MODE_NAMES[mode])
            .info("Depth test mode changed");
    }
    
    /**
     * Cycles to the next mode.
     */
    public static void cycleMode() {
        mode = (mode + 1) % (MAX_MODE + 1);
        Logging.RENDER.topic("depth_test")
            .kv("mode", mode)
            .kv("name", MODE_NAMES[mode])
            .info("Depth test mode cycled");
    }
    
    /**
     * Returns whether the shader is enabled (mode > 0).
     */
    public static boolean isEnabled() {
        return mode > 0;
    }
    
    /**
     * Gets the display name for the current mode.
     */
    public static String getModeName() {
        return MODE_NAMES[mode];
    }
    
    /**
     * Legacy toggle for compatibility.
     */
    public static void toggle() {
        cycleMode();
    }
    
    /**
     * Legacy setter for compatibility.
     */
    public static void setEnabled(boolean enable) {
        mode = enable ? 1 : 0;
    }
    
    /**
     * Loads and returns the post effect processor for the current mode.
     */
    public static PostEffectProcessor loadProcessor() {
        if (mode <= 0 || mode > MAX_MODE) {
            return null;
        }
        
        Identifier shaderId = SHADER_IDS[mode];
        if (shaderId == null) {
            return null;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        
        ShaderLoader shaderLoader = client.getShaderLoader();
        if (shaderLoader == null) {
            return null;
        }
        
        try {
            return shaderLoader.loadPostEffect(shaderId, REQUIRED_TARGETS);
        } catch (Exception e) {
            Logging.RENDER.topic("depth_test")
                .kv("shader", shaderId.toString())
                .kv("error", e.getMessage())
                .error("Failed to load post effect");
            return null;
        }
    }
}
