package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.cyberpunk042.log.Logging;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Shockwave Glow Renderer - Hybrid CPU+GPU approach.
 * 
 * <p>This system combines:</p>
 * <ul>
 *   <li><b>CPU:</b> Reads depth buffer, calculates ring positions, generates mask texture</li>
 *   <li><b>GPU:</b> Applies corona-style glow effect to the mask</li>
 * </ul>
 * 
 * <p>This bypasses the 1.21.6 limitation where post-effect shaders can't access depth.</p>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * [glReadPixels] → [CPU Ring Detection] → [Mask Texture] → [GPU Glow Shader] → [Screen]
 * </pre>
 */
public class ShockwaveGlowRenderer {
    
    // Texture IDs
    private static final Identifier MASK_TEXTURE_ID = Identifier.of("the-virus-block", "shockwave_mask");
    
    // Mask texture
    private static NativeImage maskImage = null;
    private static NativeImageBackedTexture maskTexture = null;
    private static boolean textureRegistered = false;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    
    // Animation state
    private static boolean animating = false;
    private static float animationRadius = 0.0f;
    private static float animationSpeed = 10.0f;  // blocks per second
    private static float maxAnimationRadius = 100.0f;
    private static long animationStartTime = 0;
    
    // Ring parameters
    private static float ringThickness = 5.0f;
    
    // Glow parameters (for future GPU shader)
    private static float glowPower = 2.0f;
    private static float glowIntensity = 2.5f;
    private static float[] glowColor = {0.2f, 0.9f, 1.0f};  // Cyan
    
    // Enable state
    private static boolean enabled = false;
    
    /**
     * Initialize the renderer.
     */
    public static void init() {
        Logging.RENDER.topic("shockwave_glow")
            .info("ShockwaveGlowRenderer initialized");
    }
    
    /**
     * Check if enabled.
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Toggle enable state.
     */
    public static void toggle() {
        enabled = !enabled;
        Logging.RENDER.topic("shockwave_glow")
            .kv("enabled", enabled)
            .info("ShockwaveGlowRenderer toggled");
    }
    
    /**
     * Trigger the shockwave animation.
     */
    public static void trigger() {
        enabled = true;
        animating = true;
        animationRadius = 0.0f;
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("shockwave_glow")
            .kv("speed", animationSpeed)
            .kv("maxRadius", maxAnimationRadius)
            .info("Shockwave triggered");
    }
    
    /**
     * Set static radius (no animation).
     */
    public static void setRadius(float radius) {
        enabled = true;
        animating = false;
        animationRadius = Math.max(0.0f, radius);
        Logging.RENDER.topic("shockwave_glow")
            .kv("radius", animationRadius)
            .info("Static radius set");
    }
    
    /**
     * Capture the depth buffer and generate the mask texture.
     * Called from render thread before rendering.
     */
    public static void captureAndGenerateMask(MinecraftClient client, int width, int height) {
        if (!enabled) return;
        
        // Update animation
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            animationRadius = elapsed * animationSpeed;
            if (animationRadius > maxAnimationRadius) {
                animating = false;
            }
        }
        
        // Skip if radius is 0
        if (animationRadius <= 0.0f) return;
        
        // Get camera
        var camera = client.gameRenderer.getCamera();
        if (camera == null) return;
        var cameraPos = camera.getPos();
        
        // Get origin (player position)
        org.joml.Vector3d origin;
        if (client.player != null) {
            origin = new org.joml.Vector3d(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
            );
        } else {
            origin = new org.joml.Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);
        }
        
        // Ensure mask texture exists
        if (maskImage == null || lastWidth != width || lastHeight != height) {
            if (maskImage != null) maskImage.close();
            maskImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);
            lastWidth = width;
            lastHeight = height;
            
            if (maskTexture != null) maskTexture.close();
            maskTexture = new NativeImageBackedTexture(MASK_TEXTURE_ID::toString, maskImage);
            
            if (client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(MASK_TEXTURE_ID, maskTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(width * height);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        // Camera orientation
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);
        
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        
        double ux = fy * rz;
        double uy = fz * rx - fx * rz;
        double uz = -fy * rx;
        
        double fov = Math.toRadians(client.options.getFov().getValue());
        double aspectRatio = (double) width / height;
        
        float innerRing = animationRadius - ringThickness / 2.0f;
        float outerRing = animationRadius + ringThickness / 2.0f;
        float glowThickness = ringThickness * 3.0f;
        float glowInner = animationRadius - glowThickness / 2.0f;
        float glowOuter = animationRadius + glowThickness / 2.0f;
        
        // Generate mask
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcY = height - 1 - y;
                float depth = depthBuffer.get(srcY * width + x);
                
                // Sky = transparent
                if (depth >= 0.9999f) {
                    maskImage.setColorArgb(x, y, 0x00000000);
                    continue;
                }
                
                // Convert depth to distance
                float near = 0.05f;
                float far = 256.0f;
                float eyeDist = (2.0f * near * far) / (far + near - depth * (far - near));
                
                // Ray direction
                double ndcX = (2.0 * x / width - 1.0) * aspectRatio * Math.tan(fov / 2);
                double ndcY = (1.0 - 2.0 * y / height) * Math.tan(fov / 2);
                
                double dirX = fx + ndcX * rx + ndcY * ux;
                double dirY = fy + ndcY * uy;
                double dirZ = fz + ndcX * rz + ndcY * uz;
                
                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= len; dirY /= len; dirZ /= len;
                
                // World position
                double worldX = cameraPos.x + dirX * eyeDist;
                double worldY = cameraPos.y + dirY * eyeDist;
                double worldZ = cameraPos.z + dirZ * eyeDist;
                
                // Distance from origin
                double dx = worldX - origin.x;
                double dy = worldY - origin.y;
                double dz = worldZ - origin.z;
                float distFromOrigin = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // Calculate mask intensity
                if (distFromOrigin >= glowInner && distFromOrigin <= glowOuter) {
                    float distFromCenter = Math.abs(distFromOrigin - animationRadius);
                    
                    // Core intensity
                    float coreIntensity = 0.0f;
                    if (distFromOrigin >= innerRing && distFromOrigin <= outerRing) {
                        coreIntensity = 1.0f - (distFromCenter / (ringThickness / 2));
                        coreIntensity = Math.max(0.0f, coreIntensity);
                    }
                    
                    // Glow intensity
                    float glowIntens = 1.0f - (distFromCenter / (glowThickness / 2));
                    glowIntens = (float) Math.pow(Math.max(0.0f, glowIntens), 2.0);
                    
                    // Store in mask: R=core, G=glow, B=0, A=combined
                    int ir = (int)(coreIntensity * 255);
                    int ig = (int)(glowIntens * 255);
                    int alpha = Math.max(ir, ig);
                    
                    maskImage.setColorArgb(x, y, (alpha << 24) | (ir << 16) | (ig << 8) | 0);
                } else {
                    maskImage.setColorArgb(x, y, 0x00000000);
                }
            }
        }
        
        maskTexture.upload();
    }
    
    /**
     * Render the glow effect.
     * For now: simple texture display. Later: GPU shader.
     */
    public static void render(DrawContext context, MinecraftClient client, int screenWidth, int screenHeight) {
        if (!enabled || !textureRegistered || maskTexture == null) return;
        
        // TODO: Replace with custom shader render
        // For now, just draw the mask with additive-ish blending
        context.drawTexture(
            net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
            MASK_TEXTURE_ID,
            0, 0,
            0.0f, 0.0f,
            screenWidth, screenHeight,
            lastWidth, lastHeight
        );
        
        // Stats
        int statX = 5, statY = 25;
        context.fill(statX - 2, statY - 2, statX + 200, statY + 60, 0xA0000000);
        context.drawText(client.textRenderer, "SHOCKWAVE GLOW (Hybrid)", statX, statY, 0x00FFFF, true);
        context.drawText(client.textRenderer,
            String.format("Radius: %.1f blocks", animationRadius),
            statX, statY + 12, animating ? 0x00FF00 : 0xFFFF00, true);
        context.drawText(client.textRenderer,
            animating ? "ANIMATING" : "STATIC",
            statX, statY + 24, animating ? 0x00FF00 : 0xAAAAAA, true);
        context.drawText(client.textRenderer,
            "/shockwave trigger | radius <n>",
            statX, statY + 40, 0x666666, true);
    }
    
    /**
     * Dispose resources.
     */
    public static void dispose() {
        if (maskImage != null) {
            maskImage.close();
            maskImage = null;
        }
        if (maskTexture != null) {
            maskTexture.close();
            maskTexture = null;
        }
        textureRegistered = false;
    }
}
