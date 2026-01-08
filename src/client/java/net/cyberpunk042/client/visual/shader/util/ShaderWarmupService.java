package net.cyberpunk042.client.visual.shader.util;

import net.cyberpunk042.client.visual.effect.FieldVisualConfig;
import net.cyberpunk042.client.visual.effect.FieldVisualInstance;
import net.cyberpunk042.client.visual.effect.FieldVisualPostEffect;
import net.cyberpunk042.client.visual.effect.FieldVisualRegistry;
import net.cyberpunk042.client.visual.effect.ShaderKey;
import net.cyberpunk042.client.visual.shader.ShockwavePostEffect;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pre-compiles shader programs during join warm-up.
 * 
 * <p>This service eliminates first-enable lag by forcing GPU shader compilation
 * during the loading screen rather than on first effect activation.</p>
 * 
 * <h3>Warm-up Strategy (Effect Registry):</h3>
 * <ul>
 *   <li>Loads the VERSION-SPECIFIC standalone shader for the current field</li>
 *   <li>Falls back to V7 standalone if no field exists yet</li>
 *   <li>Also warms shockwave shaders</li>
 * </ul>
 * 
 * <h3>Integration:</h3>
 * <p>Called from {@link net.cyberpunk042.client.field.JoinWarmupManager} during
 * the "Loading Shaders" phase. Must be executed on the render thread.</p>
 */
public final class ShaderWarmupService {
    
    private static final String LOG_TOPIC = "shader_warmup";
    
    /** Shockwave effects to warm up */
    private static final Identifier[] SHOCKWAVE_EFFECTS = {
        Identifier.of("the-virus-block", "shockwave_ring"),
        Identifier.of("the-virus-block", "shockwave_glow")
    };
    
    private static final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private static final AtomicBoolean warming = new AtomicBoolean(false);
    
    private ShaderWarmupService() {} // Utility class
    
    /**
     * Triggers shader compilation by loading version-specific field effect processors.
     * 
     * <p><b>MUST be called from the render thread.</b></p>
     * 
     * @return Number of shaders successfully warmed up
     */
    public static int warmup() {
        if (warmedUp.get()) {
            Logging.RENDER.topic(LOG_TOPIC).debug("Already warmed up, skipping");
            return 3; // Approximate count
        }
        
        if (!warming.compareAndSet(false, true)) {
            Logging.RENDER.topic(LOG_TOPIC).debug("Warmup already in progress");
            return 0;
        }
        
        Logging.RENDER.topic(LOG_TOPIC).info("Starting shader warm-up (version-specific)...");
        long startTime = System.nanoTime();
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            warming.set(false);
            return 0;
        }
        
        int success = 0;
        
        // === WARM UP ALL FIELD VISUAL VERSIONS ===
        // Dynamically get all registered versions from schema registry
        var energyOrbVersions = net.cyberpunk042.client.gui.schema.EffectSchemaRegistry
            .versionsFor(net.cyberpunk042.client.visual.effect.EffectType.ENERGY_ORB);
        
        for (int version : energyOrbVersions) {
            try {
                long effectStart = System.nanoTime();
                
                // Create a minimal config for this version using factory + mutation
                var config = FieldVisualConfig.defaultEnergyOrb().withVersion(version);
                
                // Temporarily enable to allow loading
                boolean wasEnabled = FieldVisualPostEffect.isEnabled();
                FieldVisualPostEffect.setEnabled(true);
                
                // Load version-specific processor (this triggers compilation)
                var processor = FieldVisualPostEffect.loadProcessor(config);
                
                // Restore original state
                FieldVisualPostEffect.setEnabled(wasEnabled);
                
                long effectMs = (System.nanoTime() - effectStart) / 1_000_000;
                
                if (processor != null) {
                    success++;
                    ShaderKey key = ShaderKey.fromConfig(config);
                    Logging.RENDER.topic(LOG_TOPIC)
                        .kv("version", "V" + version)
                        .kv("shader", key.toShaderId().getPath())
                        .kv("time_ms", effectMs)
                        .debug("Field visual V{} compiled", version);
                }
                
            } catch (Exception e) {
                Logging.RENDER.topic(LOG_TOPIC)
                    .kv("version", version)
                    .kv("error", e.getMessage())
                    .warn("Failed to warm up field visual V{}", version);
            }
        }
        
        // Also warm up Geodesic
        try {
            long effectStart = System.nanoTime();
            var geodesicConfig = FieldVisualConfig.geodesic();
            
            boolean wasEnabled = FieldVisualPostEffect.isEnabled();
            FieldVisualPostEffect.setEnabled(true);
            var processor = FieldVisualPostEffect.loadProcessor(geodesicConfig);
            FieldVisualPostEffect.setEnabled(wasEnabled);
            
            long effectMs = (System.nanoTime() - effectStart) / 1_000_000;
            if (processor != null) {
                success++;
                Logging.RENDER.topic(LOG_TOPIC)
                    .kv("shader", "field_visual_geodesic")
                    .kv("time_ms", effectMs)
                    .debug("Geodesic shader compiled");
            }
        } catch (Exception e) {
            Logging.RENDER.topic(LOG_TOPIC)
                .kv("error", e.getMessage())
                .warn("Failed to warm up geodesic shader");
        }
        
        Logging.RENDER.topic(LOG_TOPIC)
            .kv("versions", energyOrbVersions.size() + 1)  // +1 for geodesic
            .kv("success", success)
            .info("All field visual shaders pre-compiled");
        
        // === WARM UP SHOCKWAVE SHADERS ===
        ShaderLoader shaderLoader = client.getShaderLoader();
        if (shaderLoader != null) {
            for (Identifier effectId : SHOCKWAVE_EFFECTS) {
                try {
                    long effectStart = System.nanoTime();
                    var processor = shaderLoader.loadPostEffect(
                        effectId, 
                        net.minecraft.client.render.DefaultFramebufferSet.STAGES
                    );
                    long effectMs = (System.nanoTime() - effectStart) / 1_000_000;
                    
                    if (processor != null) {
                        success++;
                        Logging.RENDER.topic(LOG_TOPIC)
                            .kv("effect", effectId.getPath())
                            .kv("time_ms", effectMs)
                            .info("Shockwave shader compiled");
                    }
                } catch (Exception e) {
                    Logging.RENDER.topic(LOG_TOPIC)
                        .kv("effect", effectId.getPath())
                        .kv("error", e.getMessage())
                        .warn("Failed to warm up shockwave shader");
                }
            }
        }
        
        // === WARM UP MAGIC CIRCLE SHADER ===
        if (shaderLoader != null) {
            try {
                long effectStart = System.nanoTime();
                var magicCircleProcessor = shaderLoader.loadPostEffect(
                    Identifier.of("the-virus-block", "magic_circle"),
                    net.minecraft.client.render.DefaultFramebufferSet.STAGES
                );
                long effectMs = (System.nanoTime() - effectStart) / 1_000_000;
                
                if (magicCircleProcessor != null) {
                    success++;
                    Logging.RENDER.topic(LOG_TOPIC)
                        .kv("shader", "magic_circle")
                        .kv("time_ms", effectMs)
                        .debug("Magic circle shader compiled");
                }
            } catch (Exception e) {
                Logging.RENDER.topic(LOG_TOPIC)
                    .kv("error", e.getMessage())
                    .warn("Failed to warm up magic circle shader");
            }
        }

        long totalMs = (System.nanoTime() - startTime) / 1_000_000;
        Logging.RENDER.topic(LOG_TOPIC)
            .kv("success", success)
            .kv("time_ms", totalMs)
            .info("Shader warm-up complete");
        
        warmedUp.set(true);
        warming.set(false);
        return success;
    }
    
    /**
     * Gets the current field's config for warmup, if available.
     */
    private static FieldVisualConfig getCurrentFieldConfig() {
        // Use origin as camera pos since we just need any field
        var fields = FieldVisualRegistry.getFieldsToRender(net.minecraft.util.math.Vec3d.ZERO);
        if (fields != null && !fields.isEmpty()) {
            FieldVisualInstance field = fields.get(0);
            return field.getConfig();
        }
        return null;
    }
    
    /**
     * Returns whether shader warm-up has completed.
     */
    public static boolean isWarmedUp() {
        return warmedUp.get();
    }
    
    /**
     * Returns whether warm-up is currently in progress.
     */
    public static boolean isWarming() {
        return warming.get();
    }
    
    /**
     * Resets warm-up state. Called on resource reload.
     */
    public static void invalidate() {
        warmedUp.set(false);
        // Clear processor cache so new shaders get loaded
        FieldVisualPostEffect.clearProcessorCache();
        // Also invalidate preprocessor cache
        ShaderPreprocessor.invalidateCache();
        Logging.RENDER.topic(LOG_TOPIC).info("Shader warm-up state invalidated");
    }
}
