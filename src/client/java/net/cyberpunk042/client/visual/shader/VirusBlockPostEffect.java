package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Manages the Virus Block post-effect shader.
 * 
 * <p>Provides toxic smoke and ESP outline effects for virus blocks.
 * The effect is always rendered when any virus blocks are in range;
 * ESP mode is enabled only when wearing the Augmented Helmet.</p>
 * 
 * <h2>Effects:</h2>
 * <ul>
 *   <li><b>Toxic Smoke</b> - Rising poison gas from exposed block faces (always on)</li>
 *   <li><b>Screen Poison</b> - 2D overlay when player is very close (always on)</li>
 *   <li><b>ESP Outline</b> - X-ray through walls, distance color-coded (helmet only)</li>
 * </ul>
 * 
 * @see VirusBlockUniformBinder
 */
public class VirusBlockPostEffect {
    
    private static final Identifier EFFECT_ID = Identifier.of("the-virus-block", "virus_block");
    
    private static boolean initialized = false;
    private static PostEffectProcessor processor = null;
    private static boolean loadFailed = false;
    private static final Matrix4f cachedInvViewProj = new Matrix4f();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Initializes the virus block post effect system.
     * Called during client initialization.
     */
    public static void init() {
        if (initialized) return;
        
        Logging.RENDER.topic("virus_block").info("Initializing VirusBlockPostEffect system");
        initialized = true;
        
        // Pre-enable smoke by default
        VirusBlockUniformBinder.setEnabled(true);
        VirusBlockUniformBinder.setSmokeEnabled(true);
        VirusBlockUniformBinder.setScreenPoisonEnabled(true);
        VirusBlockUniformBinder.setESPEnabled(true);  // Will only show when helmet equipped
    }
    
    /**
     * Loads the post effect processor.
     * Called lazily on first render or explicitly for pre-warming.
     */
    public static PostEffectProcessor loadProcessor() {
        if (loadFailed) return null;
        if (processor != null) return processor;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;
        
        var shaderLoader = client.getShaderLoader();
        if (shaderLoader == null) return null;
        
        try {
            Logging.RENDER.topic("virus_block").info("Loading virus_block post effect");
            
            processor = shaderLoader.loadPostEffect(
                EFFECT_ID, 
                net.minecraft.client.render.DefaultFramebufferSet.STAGES
            );
            
            Logging.RENDER.topic("virus_block")
                .kv("effect", EFFECT_ID.toString())
                .info("Successfully loaded virus block post effect");
            
            return processor;
            
        } catch (Exception e) {
            Logging.RENDER.topic("virus_block")
                .kv("effect", EFFECT_ID.toString())
                .kv("error", e.getMessage())
                .warn("Failed to load virus block post effect");
            loadFailed = true;
            return null;
        }
    }
    
    /**
     * Reloads the processor (e.g., after shader reload).
     */
    public static void reload() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
        loadFailed = false;
        loadProcessor();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE CONTROL
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() {
        return VirusBlockUniformBinder.isEnabled();
    }
    
    public static void setEnabled(boolean enabled) {
        VirusBlockUniformBinder.setEnabled(enabled);
    }
    
    public static void toggle() {
        boolean next = !isEnabled();
        setEnabled(next);
        Logging.RENDER.topic("virus_block").info("Toggled virus block effect: " + next);
    }
    
    public static void setESPEnabled(boolean enabled) {
        VirusBlockUniformBinder.setESPEnabled(enabled);
    }
    
    public static void setSmokeEnabled(boolean enabled) {
        VirusBlockUniformBinder.setSmokeEnabled(enabled);
    }
    
    public static void setScreenPoisonEnabled(boolean enabled) {
        VirusBlockUniformBinder.setScreenPoisonEnabled(enabled);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER ACCESSORS (delegates to UniformBinder)
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Smoke params
    public static void setSmokeHeight(float v) { VirusBlockUniformBinder.setSmokeHeight(v); }
    public static void setSmokeSpread(float v) { VirusBlockUniformBinder.setSmokeSpread(v); }
    public static void setSmokeDensity(float v) { VirusBlockUniformBinder.setSmokeDensity(v); }
    public static void setSmokeTurbulence(float v) { VirusBlockUniformBinder.setSmokeTurbulence(v); }
    public static void setSmokeRiseSpeed(float v) { VirusBlockUniformBinder.setSmokeRiseSpeed(v); }
    public static void setSmokeSwirl(float v) { VirusBlockUniformBinder.setSmokeSwirl(v); }
    public static void setSmokeNoiseScale(float v) { VirusBlockUniformBinder.setSmokeNoiseScale(v); }
    public static void setSmokeFadeHeight(float v) { VirusBlockUniformBinder.setSmokeFadeHeight(v); }
    public static void setSmokeColor(float r, float g, float b) { VirusBlockUniformBinder.setSmokeColor(r, g, b); }
    public static void setSmokeIntensity(float v) { VirusBlockUniformBinder.setSmokeIntensity(v); }
    
    // Screen poison params
    public static void setScreenPoisonTriggerDist(float v) { VirusBlockUniformBinder.setScreenPoisonTriggerDist(v); }
    public static void setScreenPoisonIntensity(float v) { VirusBlockUniformBinder.setScreenPoisonIntensity(v); }
    
    // ESP params
    public static void setESPCloseDist(float v) { VirusBlockUniformBinder.setESPCloseDist(v); }
    public static void setESPMediumDist(float v) { VirusBlockUniformBinder.setESPMediumDist(v); }
    public static void setESPPulseSpeed(float v) { VirusBlockUniformBinder.setESPPulseSpeed(v); }
    public static void setESPGlowPower(float v) { VirusBlockUniformBinder.setESPGlowPower(v); }
    public static void setESPGlowSize(float v) { VirusBlockUniformBinder.setESPGlowSize(v); }
    public static void setESPEdgeSharpness(float v) { VirusBlockUniformBinder.setESPEdgeSharpness(v); }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INVERSE VIEW-PROJECTION MATRIX
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void updateInvViewProj(Matrix4f invViewProj) {
        cachedInvViewProj.set(invViewProj);
    }
    
    public static Matrix4f getInvViewProj() {
        return new Matrix4f(cachedInvViewProj);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static String getStatusString() {
        return VirusBlockUniformBinder.getStatusString();
    }
    
    public static boolean isLoaded() {
        return processor != null && !loadFailed;
    }
    
    public static PostEffectProcessor getProcessor() {
        return processor;
    }
}
