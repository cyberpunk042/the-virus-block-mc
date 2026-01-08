package net.cyberpunk042.client.visual.shader;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Depth buffer diagnostic with VISUAL OUTPUT for ALL modes.
 * 
 * Mode 1: Depth Range - Shows histogram bar + min/max values
 * Mode 2: Center Pixel - Shows crosshair with depth value that changes color  
 * Mode 3: FBO Info - Shows FBO binding status as colored indicators
 * Mode 4: Full Visualization - Shows complete depth buffer as grayscale image
 */
public class DirectDepthRenderer {
    
    private static final Identifier DEPTH_TEXTURE_ID = Identifier.of("the-virus-block", "debug/depth_buffer");
    
    private static boolean initialized = false;
    private static int mode = 0;
    private static int frameCount = 0;
    
    // Depth capture resources
    private static NativeImage depthImage;
    private static NativeImageBackedTexture depthTexture;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean textureRegistered = false;
    
    // Stats for HUD display (all modes use these)
    private static float lastMinDepth = 1.0f;
    private static float lastMaxDepth = 0.0f;
    private static float lastCenterDepth = 1.0f;
    private static float lastCornerDepth = 1.0f;
    private static float lastRange = 0.0f;
    private static int lastReadFBO = 0;
    private static int lastDrawFBO = 0;
    private static boolean lastHasDepthTex = false;
    private static boolean lastHasDepthView = false;
    private static String lastDepthFormat = "?";
    private static int[] depthHistogram = new int[16];  // 16 buckets for histogram
    
    // Mode 5: Distance visualization stats
    private static float lastCenterDistance = 0.0f;
    private static float lastMinDistance = 0.0f;
    private static float lastMaxDistance = 0.0f;
    private static int lastSkyPixels = 0;
    private static boolean reversedZ = false;  // Toggle for reversed-Z depth buffer
    
    // Depth conversion constants
    private static final float NEAR_PLANE = 0.05f;
    private static float farPlane = 1000.0f;  // Will be updated based on render distance
    
    // Mode 6-8: Ring configuration
    private static float ringDistance = 15.0f;    // Distance in blocks for ring
    private static float ringThickness = 2.0f;    // Width of the ring in blocks
    private static int ringPixelCount = 0;        // How many pixels are in the ring
    
    // Mode 7: World point for ring origin
    private static org.joml.Vector3d ringWorldOrigin = new org.joml.Vector3d(0, 0, 0);
    private static boolean usePlayerAsOrigin = true;  // If true, use player position as origin
    
    // Mode 8: Animation state
    private static boolean animating = false;
    private static float animationRadius = 0.0f;
    private static float animationSpeed = 5.0f;  // blocks per second (slower to see)
    private static float maxAnimationRadius = 100.0f;
    private static long animationStartTime = 0;
    
    // Resolution control (for Mode 9 overlay only, Mode 7/8 always use full res)
    private static int resolutionDivisor = 1;  // Default to full resolution
    
    // Mode names
    private static final String[] MODE_NAMES = {
        "OFF",
        "Depth Range (Histogram)",
        "Center Crosshair",
        "FBO Info",
        "Full Depth Image",
        "Distance Visualization",
        "Ring at Fixed Distance",
        "Ring from World Point",       // Mode 7 - Grey background (WORKING)
        "Animated Expanding Ring",     // Mode 8 - Animated (WORKING)
        "OVERLAY Ring (Experimental)", // Mode 9 - draws ring pixels as overlay
        "TRANSPARENT Ring (New)"       // Mode 10 - transparent texture overlay
    };
    
    public static void init() {
        initialized = true;
        Logging.RENDER.topic("direct_depth").info("DirectDepthRenderer initialized");
    }
    
    public static boolean isEnabled() {
        return mode > 0;
    }
    
    public static int getMode() {
        return mode;
    }
    
    public static int getMaxMode() {
        return MODE_NAMES.length - 1;
    }
    
    public static void setMode(int newMode) {
        mode = Math.max(0, Math.min(newMode, getMaxMode()));
        frameCount = 0;
        Logging.RENDER.topic("direct_depth")
            .kv("mode", mode)
            .kv("name", MODE_NAMES[mode])
            .info("Direct depth mode changed");
    }
    
    public static void cycleMode() {
        mode = (mode + 1) % MODE_NAMES.length;
        frameCount = 0;
        Logging.RENDER.topic("direct_depth")
            .kv("mode", mode)
            .kv("name", MODE_NAMES[mode])
            .info("Direct depth mode cycled");
    }
    
    public static String getModeName() {
        return MODE_NAMES[mode];
    }
    
    /**
     * Captures depth buffer data - call from mixin after world render.
     */
    public static void render() {
        if (!isEnabled() || !initialized) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null) return;
        
        int width = framebuffer.textureWidth;
        int height = framebuffer.textureHeight;
        
        frameCount++;
        
        // Update far plane based on render distance
        if (client.options != null) {
            int renderDist = client.options.getViewDistance().getValue();
            farPlane = Math.max(192.0f, renderDist * 16.0f);
        }
        
        // Capture data based on mode
        switch (mode) {
            case 1 -> captureDepthRange(width, height);
            case 2 -> captureCenterPixel(width, height);
            case 3 -> captureFBOInfo(client, framebuffer);
            case 4 -> captureFullDepth(width, height);
            case 5 -> captureDistanceVisualization(width, height);
            case 6 -> captureRingAtDistance(width, height);
            case 7 -> captureRingFromWorldPoint(client, width, height);
            case 8 -> captureAnimatedRing(client, width, height);
            case 9 -> captureOverlayRing(client, width, height);
            case 10 -> captureTransparentRing(client, width, height);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 1: Depth Range - Builds histogram
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureDepthRange(int width, int height) {
        int sampleWidth = width / 10;
        int sampleHeight = height / 10;
        
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(sampleWidth * sampleHeight);
        GL11.glReadPixels(0, 0, sampleWidth, sampleHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Reset histogram
        for (int i = 0; i < 16; i++) depthHistogram[i] = 0;
        
        float minDepth = 1.0f, maxDepth = 0.0f;
        int totalPixels = sampleWidth * sampleHeight;
        
        for (int i = 0; i < totalPixels; i++) {
            float depth = depthBuffer.get(i);
            if (depth < minDepth) minDepth = depth;
            if (depth > maxDepth) maxDepth = depth;
        }
        
        lastMinDepth = minDepth;
        lastMaxDepth = maxDepth;
        lastRange = maxDepth - minDepth;
        
        // Build histogram based on actual range
        depthBuffer.rewind();
        float range = lastRange > 0.0001f ? lastRange : 0.0001f;
        for (int i = 0; i < totalPixels; i++) {
            float depth = depthBuffer.get(i);
            float normalized = (depth - minDepth) / range;
            int bucket = Math.min(15, Math.max(0, (int)(normalized * 16)));
            depthHistogram[bucket]++;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 2: Center Pixel - Samples center and corners
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureCenterPixel(int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(centerX, centerY, 1, 1, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        lastCenterDepth = depthBuffer.get(0);
        
        // Also sample corner
        FloatBuffer cornerBuffer = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(10, 10, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, cornerBuffer);
        if (GL11.glGetError() == GL11.GL_NO_ERROR) {
            lastCornerDepth = cornerBuffer.get(0);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 3: FBO Info - Checks framebuffer binding
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureFBOInfo(MinecraftClient client, Framebuffer framebuffer) {
        IntBuffer currentReadFbo = BufferUtils.createIntBuffer(1);
        IntBuffer currentDrawFbo = BufferUtils.createIntBuffer(1);
        
        GL11.glGetIntegerv(GL30.GL_READ_FRAMEBUFFER_BINDING, currentReadFbo);
        GL11.glGetIntegerv(GL30.GL_DRAW_FRAMEBUFFER_BINDING, currentDrawFbo);
        
        lastReadFBO = currentReadFbo.get(0);
        lastDrawFBO = currentDrawFbo.get(0);
        
        @Nullable GpuTexture depthTex = framebuffer.getDepthAttachment();
        @Nullable GpuTextureView depthView = framebuffer.getDepthAttachmentView();
        
        lastHasDepthTex = depthTex != null;
        lastHasDepthView = depthView != null;
        lastDepthFormat = depthTex != null ? depthTex.getFormat().toString() : "N/A";
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 4: Full Depth Image
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureFullDepth(int width, int height) {
        int captureWidth = width / 4;
        int captureHeight = height / 4;
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // First pass: find range
        float minDepth = 1.0f, maxDepth = 0.0f;
        for (int i = 0; i < captureWidth * captureHeight; i++) {
            float depth = depthBuffer.get(i);
            if (depth < minDepth) minDepth = depth;
            if (depth > maxDepth) maxDepth = depth;
        }
        lastMinDepth = minDepth;
        lastMaxDepth = maxDepth;
        lastRange = maxDepth - minDepth;
        
        // Second pass: convert to image
        depthBuffer.rewind();
        float range = lastRange > 0.0001f ? lastRange : 0.0001f;
        
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                float depth = depthBuffer.get(srcY * captureWidth + x);
                
                float normalized = 1.0f - ((depth - minDepth) / range);
                normalized = (float) Math.pow(normalized, 0.5);
                
                int gray = Math.max(0, Math.min(255, (int)(normalized * 255)));
                depthImage.setColorArgb(x, y, (255 << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }
        
        depthTexture.upload();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VISUAL RENDERING - All modes show something visual
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void renderOverlay(DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;
        
        try {
            switch (mode) {
                case 1 -> renderMode1_Histogram(context, client, screenWidth, screenHeight);
                case 2 -> renderMode2_Crosshair(context, client, screenWidth, screenHeight);
                case 3 -> renderMode3_FBOInfo(context, client, screenWidth, screenHeight);
                case 4 -> renderMode4_FullImage(context, client, screenWidth, screenHeight);
                case 5 -> renderMode5_DistanceVis(context, client, screenWidth, screenHeight);
                case 6 -> renderMode6_RingAtDistance(context, client, screenWidth, screenHeight);
                case 7 -> renderMode7_WorldPointRing(context, client, screenWidth, screenHeight);
                case 8 -> renderMode8_AnimatedRing(context, client, screenWidth, screenHeight);
                case 9 -> renderMode9_OverlayRing(context, client, screenWidth, screenHeight);
                case 10 -> renderMode10_TransparentRing(context, client, screenWidth, screenHeight);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    private static void renderMode1_Histogram(DrawContext context, MinecraftClient client, int sw, int sh) {
        int x = 10, y = 10;
        int barWidth = 200, barHeight = 60;
        
        // Background
        context.fill(x - 2, y - 2, x + barWidth + 4, y + barHeight + 40, 0xC0000000);
        
        // Find max bucket for scaling
        int maxBucket = 1;
        for (int i = 0; i < 16; i++) {
            if (depthHistogram[i] > maxBucket) maxBucket = depthHistogram[i];
        }
        
        // Draw histogram bars
        int bucketWidth = barWidth / 16;
        for (int i = 0; i < 16; i++) {
            int bucketHeight = (int)((float)depthHistogram[i] / maxBucket * barHeight);
            int bx = x + i * bucketWidth;
            int by = y + barHeight - bucketHeight;
            
            // Color gradient: green (near) to red (far)
            int r = (int)(255 * (i / 15.0f));
            int g = (int)(255 * (1 - i / 15.0f));
            int color = 0xFF000000 | (r << 16) | (g << 8);
            
            context.fill(bx, by, bx + bucketWidth - 1, y + barHeight, color);
        }
        
        // Border
        context.fill(x - 1, y - 1, x + barWidth + 1, y, 0xFFFFFFFF);
        context.fill(x - 1, y + barHeight, x + barWidth + 1, y + barHeight + 1, 0xFFFFFFFF);
        
        // Labels
        context.drawText(client.textRenderer, 
            String.format("Min: %.6f  Max: %.6f", lastMinDepth, lastMaxDepth),
            x, y + barHeight + 5, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Range: %.6f", lastRange),
            x, y + barHeight + 18, 0xAAFFAA, true);
    }
    
    private static void renderMode2_Crosshair(DrawContext context, MinecraftClient client, int sw, int sh) {
        int cx = sw / 2;
        int cy = sh / 2;
        int size = 20;
        
        // Color based on depth: green = near, red = far
        float depthFactor = Math.min(1.0f, Math.max(0.0f, (lastCenterDepth - 0.99f) / 0.01f));
        int r = (int)(255 * depthFactor);
        int g = (int)(255 * (1 - depthFactor));
        int crossColor = 0xFF000000 | (r << 16) | (g << 8);
        
        // Draw crosshair
        context.fill(cx - size, cy - 1, cx + size, cy + 2, crossColor);
        context.fill(cx - 1, cy - size, cx + 2, cy + size, crossColor);
        
        // Circle outline
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI * 2 / 32;
            int px = cx + (int)(Math.cos(angle) * 15);
            int py = cy + (int)(Math.sin(angle) * 15);
            context.fill(px, py, px + 2, py + 2, crossColor);
        }
        
        // Info box
        int bx = 10, by = 10;
        context.fill(bx - 2, by - 2, bx + 180, by + 50, 0xC0000000);
        
        context.drawText(client.textRenderer, "CENTER DEPTH", bx, by, 0xFFFF00, true);
        context.drawText(client.textRenderer, 
            String.format("Raw: %.6f", lastCenterDepth),
            bx, by + 12, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Corner: %.6f", lastCornerDepth),
            bx, by + 24, 0xAAAAAA, true);
        context.drawText(client.textRenderer,
            String.format("Diff: %.6f", Math.abs(lastCenterDepth - lastCornerDepth)),
            bx, by + 36, 0xAAFFAA, true);
    }
    
    private static void renderMode3_FBOInfo(DrawContext context, MinecraftClient client, int sw, int sh) {
        int x = 10, y = 10;
        int w = 220, h = 100;
        
        // Background
        context.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xC0000000);
        
        // Title
        context.drawText(client.textRenderer, "FRAMEBUFFER INFO", x, y, 0xFFFF00, true);
        
        // FBO IDs with status indicators
        int statusColor = (lastReadFBO == lastDrawFBO) ? 0xFF00FF00 : 0xFFFF0000;
        context.fill(x, y + 14, x + 8, y + 22, statusColor);
        context.drawText(client.textRenderer, 
            String.format("Read FBO: %d  Draw FBO: %d", lastReadFBO, lastDrawFBO),
            x + 12, y + 14, 0xFFFFFF, true);
        
        // Depth texture status
        int depthTexColor = lastHasDepthTex ? 0xFF00FF00 : 0xFFFF0000;
        context.fill(x, y + 28, x + 8, y + 36, depthTexColor);
        context.drawText(client.textRenderer,
            "Depth Texture: " + (lastHasDepthTex ? "YES" : "NO"),
            x + 12, y + 28, 0xFFFFFF, true);
        
        // Depth view status
        int depthViewColor = lastHasDepthView ? 0xFF00FF00 : 0xFFFF0000;
        context.fill(x, y + 42, x + 8, y + 50, depthViewColor);
        context.drawText(client.textRenderer,
            "Depth View: " + (lastHasDepthView ? "YES" : "NO"),
            x + 12, y + 42, 0xFFFFFF, true);
        
        // Format
        context.drawText(client.textRenderer,
            "Format: " + lastDepthFormat,
            x, y + 58, 0xAAFFAA, true);
        
        // Summary
        boolean allGood = lastHasDepthTex && lastHasDepthView && (lastReadFBO == lastDrawFBO);
        int summaryColor = allGood ? 0xFF00FF00 : 0xFFFF8800;
        context.drawText(client.textRenderer,
            allGood ? "Status: ALL GOOD" : "Status: CHECK ISSUES",
            x, y + 78, summaryColor, true);
    }
    
    private static void renderMode4_FullImage(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        int x = 10, y = 10;
        int displayWidth = lastWidth;
        int displayHeight = lastHeight;
        
        // Background
        context.fill(x - 2, y - 2, x + displayWidth + 2, y + displayHeight + 30, 0xC0000000);
        
        // Depth image
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            x, y,
            0.0f, 0.0f,
            displayWidth, displayHeight,
            displayWidth, displayHeight
        );
        
        // Stats
        context.drawText(client.textRenderer,
            String.format("Depth: %.4f - %.4f (range: %.6f)", lastMinDepth, lastMaxDepth, lastRange),
            x, y + displayHeight + 4, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            "White=Near  Black=Far",
            x, y + displayHeight + 16, 0xAAAAAA, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 5: Distance Visualization
    // Converts depth to world-space distance and visualizes as color gradient
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureDistanceVisualization(int width, int height) {
        int captureWidth = Math.max(1, width / resolutionDivisor);
        int captureHeight = Math.max(1, height / resolutionDivisor);
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Stats tracking
        float minDist = Float.MAX_VALUE;
        float maxDist = 0.0f;
        int skyPixels = 0;
        int centerIdx = (captureHeight / 2) * captureWidth + (captureWidth / 2);
        float centerDepth = 0.0f;
        
        // First pass: analyze and convert to distances
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                int idx = srcY * captureWidth + x;
                float depth = depthBuffer.get(idx);
                
                // Check for center pixel
                if (idx == centerIdx) {
                    centerDepth = depth;
                }
                
                // Sky detection (depth very close to 1.0)
                if (depth >= 0.9999f) {
                    skyPixels++;
                    // Sky = black
                    depthImage.setColorArgb(x, y, 0xFF000000);
                    continue;
                }
                
                // Convert depth to distance
                float distance = depthToDistance(depth);
                
                if (distance < minDist) minDist = distance;
                if (distance > maxDist) maxDist = distance;
                
                // Convert distance to color
                int color = distanceToColor(distance);
                depthImage.setColorArgb(x, y, color);
            }
        }
        
        // Update stats
        lastCenterDistance = depthToDistance(centerDepth);
        lastMinDistance = (minDist == Float.MAX_VALUE) ? 0.0f : minDist;
        lastMaxDistance = maxDist;
        lastSkyPixels = skyPixels;
        
        depthTexture.upload();
        
        // Log diagnostics every second
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("mode5_distance")
                .kv("nearPlane", NEAR_PLANE)
                .kv("farPlane", farPlane)
                .kv("reversedZ", reversedZ)
                .kv("centerDepth", String.format("%.6f", centerDepth))
                .kv("centerDist", String.format("%.2f", lastCenterDistance))
                .kv("minDist", String.format("%.2f", lastMinDistance))
                .kv("maxDist", String.format("%.2f", lastMaxDistance))
                .kv("skyPixels", skyPixels)
                .info("Distance analysis");
        }
    }
    
    /**
     * Converts depth buffer value to world-space distance in blocks.
     */
    private static float depthToDistance(float depth) {
        // Handle edge cases
        if (depth <= 0.0f) return NEAR_PLANE;
        if (depth >= 1.0f) return farPlane;
        
        // Apply reversed-Z if enabled
        float d = reversedZ ? (1.0f - depth) : depth;
        
        // Standard perspective depth linearization:
        // z_linear = (far * near) / (far - depth * (far - near))
        float distance = (farPlane * NEAR_PLANE) / (farPlane - d * (farPlane - NEAR_PLANE));
        
        // Clamp to valid range
        return Math.max(NEAR_PLANE, Math.min(farPlane, distance));
    }
    
    /**
     * Converts distance in blocks to a color on the gradient.
     * Red = very close (0-5), Orange (5-15), Yellow (15-30), 
     * Green (30-60), Cyan (60-100), Blue (100+)
     */
    private static int distanceToColor(float distance) {
        int r, g, b;
        
        if (distance < 5.0f) {
            // Red zone (0-5 blocks)
            float t = distance / 5.0f;
            r = 255;
            g = (int)(t * 100);
            b = 0;
        } else if (distance < 15.0f) {
            // Orange zone (5-15 blocks)
            float t = (distance - 5.0f) / 10.0f;
            r = 255;
            g = 100 + (int)(t * 155);
            b = 0;
        } else if (distance < 30.0f) {
            // Yellow zone (15-30 blocks)
            float t = (distance - 15.0f) / 15.0f;
            r = 255 - (int)(t * 55);
            g = 255;
            b = 0;
        } else if (distance < 60.0f) {
            // Green zone (30-60 blocks)
            float t = (distance - 30.0f) / 30.0f;
            r = 0;
            g = 255 - (int)(t * 55);
            b = (int)(t * 200);
        } else if (distance < 100.0f) {
            // Cyan zone (60-100 blocks)
            float t = (distance - 60.0f) / 40.0f;
            r = 0;
            g = 200 - (int)(t * 100);
            b = 200 + (int)(t * 55);
        } else {
            // Blue zone (100+ blocks)
            r = 0;
            g = 100;
            b = 255;
        }
        
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
    
    private static void renderMode5_DistanceVis(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        int x = 10, y = 10;
        int displayWidth = lastWidth;
        int displayHeight = lastHeight;
        
        // FULLSCREEN stretched display
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            0, 0,  // No margin
            0.0f, 0.0f,
            sw, sh,  // Stretch to screen size
            lastWidth, lastHeight  // Texture size
        );
        
        // Stats overlay in top-left corner
        int statX = 5, statY = 25;
        context.fill(statX - 2, statY - 2, statX + 180, statY + 85, 0xA0000000);
        
        context.drawText(client.textRenderer, "Mode 5: Distance", statX, statY, 0x00FFFF, true);
        context.drawText(client.textRenderer,
            String.format("Center: %.1f blocks", lastCenterDistance),
            statX, statY + 12, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Range: %.1f - %.1f", lastMinDistance, lastMaxDistance),
            statX, statY + 24, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            String.format("Sky: %d | Far: %.0f", lastSkyPixels, farPlane),
            statX, statY + 36, 0xAAAAFF, true);
        
        // Legend
        int legendY = statY + 50;
        for (int i = 0; i < 150; i++) {
            int color = distanceToColor((i / 150.0f) * 100.0f);
            context.fill(statX + i, legendY, statX + i + 1, legendY + 10, color);
        }
        context.drawText(client.textRenderer, "0", statX, legendY + 12, 0xFFFFFF, true);
        context.drawText(client.textRenderer, "50", statX + 70, legendY + 12, 0xFFFFFF, true);
        context.drawText(client.textRenderer, "100+", statX + 130, legendY + 12, 0xFFFFFF, true);
    }
    
    /**
     * Toggle reversed-Z mode for testing.
     */
    public static void toggleReversedZ() {
        reversedZ = !reversedZ;
        Logging.RENDER.topic("direct_depth")
            .kv("reversedZ", reversedZ)
            .info("Reversed-Z toggled");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 6: Ring at Fixed Distance from Camera
    // Highlights all pixels at a specific distance
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureRingAtDistance(int width, int height) {
        int captureWidth = Math.max(1, width / resolutionDivisor);
        int captureHeight = Math.max(1, height / resolutionDivisor);
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Stats
        int skyPixels = 0;
        int localRingPixels = 0;
        float innerRing = ringDistance - ringThickness / 2.0f;
        float outerRing = ringDistance + ringThickness / 2.0f;
        
        // Process pixels
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                int idx = srcY * captureWidth + x;
                float depth = depthBuffer.get(idx);
                
                // Sky detection
                if (depth >= 0.9999f) {
                    skyPixels++;
                    depthImage.setColorArgb(x, y, 0xFF000000);
                    continue;
                }
                
                // Convert to distance
                float distance = depthToDistance(depth);
                
                // Check if within ring
                if (distance >= innerRing && distance <= outerRing) {
                    localRingPixels++;
                    // Bright cyan for ring
                    depthImage.setColorArgb(x, y, 0xFF00FFFF);
                } else {
                    // Dim gray for background
                    float brightness = 1.0f - Math.min(1.0f, distance / 100.0f);
                    int gray = (int)(brightness * 80);
                    depthImage.setColorArgb(x, y, (255 << 24) | (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        
        ringPixelCount = localRingPixels;
        lastSkyPixels = skyPixels;
        
        depthTexture.upload();
        
        // Log diagnostics
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("mode6_ring")
                .kv("ringDistance", String.format("%.1f", ringDistance))
                .kv("ringThickness", String.format("%.1f", ringThickness))
                .kv("ringPixels", ringPixelCount)
                .kv("skyPixels", skyPixels)
                .info("Ring analysis");
        }
    }
    
    private static void renderMode6_RingAtDistance(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        // FULLSCREEN stretched display
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            0, 0,
            0.0f, 0.0f,
            sw, sh,
            lastWidth, lastHeight
        );
        
        // Stats overlay
        int statX = 5, statY = 25;
        context.fill(statX - 2, statY - 2, statX + 200, statY + 60, 0xA0000000);
        
        context.drawText(client.textRenderer, "Mode 6: Ring (Camera Dist)", statX, statY, 0x00FFFF, true);
        context.drawText(client.textRenderer,
            String.format("Ring: %.1f blocks (±%.1f)", ringDistance, ringThickness / 2),
            statX, statY + 12, 0x00FFFF, true);
        context.drawText(client.textRenderer,
            String.format("Ring pixels: %d", ringPixelCount),
            statX, statY + 24, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            "/directdepth ring <dist> <thick>",
            statX, statY + 40, 0x888888, true);
    }
    
    /**
     * Set ring distance and thickness for modes 6-8.
     */
    public static void setRingParams(float distance, float thickness) {
        ringDistance = Math.max(1.0f, distance);
        ringThickness = Math.max(0.5f, thickness);
        Logging.RENDER.topic("direct_depth")
            .kv("ringDistance", ringDistance)
            .kv("ringThickness", ringThickness)
            .info("Ring parameters updated");
    }
    
    public static float getRingDistance() {
        return ringDistance;
    }
    
    public static float getRingThickness() {
        return ringThickness;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 7: Ring from World Point
    // Uses player position or custom world point as ring origin
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureRingFromWorldPoint(MinecraftClient client, int width, int height) {
        // ALWAYS capture at FULL resolution to get correct coordinates
        // Use resolutionDivisor to skip pixels during processing for speed
        int captureWidth = width;
        int captureHeight = height;
        
        // Get camera position
        var camera = client.gameRenderer.getCamera();
        if (camera == null) return;
        
        var cameraPos = camera.getPos();
        
        // Get ring origin (player feet or custom)
        org.joml.Vector3d origin;
        if (usePlayerAsOrigin && client.player != null) {
            origin = new org.joml.Vector3d(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
            );
        } else {
            origin = ringWorldOrigin;
        }
        
        // Ensure texture exists
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            if (client.getTextureManager() != null) {
                client.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, 
            GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        if (GL11.glGetError() != GL11.GL_NO_ERROR) return;
        
        // Get camera direction vectors
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        // Forward direction
        double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double fy = -Math.sin(pitchRad);
        double fz = Math.cos(yawRad) * Math.cos(pitchRad);
        
        // Right direction
        double rx = Math.cos(yawRad);
        double rz = Math.sin(yawRad);
        
        // Up direction (cross product)
        double ux = fy * rz;
        double uy = fz * rx - fx * rz;
        double uz = -fy * rx;
        
        // FOV
        double fov = Math.toRadians(client.options.getFov().getValue());
        double aspectRatio = (double) captureWidth / captureHeight;
        
        int skyPixels = 0;
        int localRingPixels = 0;
        float innerRing = ringDistance - ringThickness / 2.0f;
        float outerRing = ringDistance + ringThickness / 2.0f;
        
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                float depth = depthBuffer.get(srcY * captureWidth + x);
                
                if (depth >= 0.9999f) {
                    skyPixels++;
                    depthImage.setColorArgb(x, y, 0xFF000000);  // Black for sky
                    continue;
                }
                
                // Convert depth to eye-space distance
                float eyeDist = depthToDistance(depth);
                
                // Calculate ray direction for this pixel
                double ndcX = (2.0 * x / captureWidth - 1.0) * aspectRatio * Math.tan(fov / 2);
                double ndcY = (1.0 - 2.0 * y / captureHeight) * Math.tan(fov / 2);
                
                // Ray direction = forward + ndcX * right + ndcY * up
                double dirX = fx + ndcX * rx + ndcY * ux;
                double dirY = fy + ndcY * uy;
                double dirZ = fz + ndcX * rz + ndcY * uz;
                
                // Normalize
                double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                dirX /= len; dirY /= len; dirZ /= len;
                
                // World position = camera + direction * distance
                double worldX = cameraPos.x + dirX * eyeDist;
                double worldY = cameraPos.y + dirY * eyeDist;
                double worldZ = cameraPos.z + dirZ * eyeDist;
                
                // Distance from origin
                double dx = worldX - origin.x;
                double dy = worldY - origin.y;
                double dz = worldZ - origin.z;
                float distFromOrigin = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // Check if within ring (with expanded glow zone)
                float glowThickness = ringThickness * 3.0f;  // Glow extends beyond main ring
                float glowInner = ringDistance - glowThickness / 2.0f;
                float glowOuter = ringDistance + glowThickness / 2.0f;
                
                if (distFromOrigin >= glowInner && distFromOrigin <= glowOuter) {
                    float distFromCenter = Math.abs(distFromOrigin - ringDistance);
                    
                    // Calculate core intensity (sharp center)
                    float coreIntensity = 0.0f;
                    if (distFromOrigin >= innerRing && distFromOrigin <= outerRing) {
                        localRingPixels++;
                        coreIntensity = 1.0f - (distFromCenter / (ringThickness / 2));
                        coreIntensity = Math.max(0.0f, coreIntensity);
                    }
                    
                    // Calculate glow intensity (soft outer)
                    float glowIntensity = 1.0f - (distFromCenter / (glowThickness / 2));
                    glowIntensity = (float) Math.pow(Math.max(0.0f, glowIntensity), 2.0);  // Square for falloff
                    
                    // Color: Cyan glow with white hot core
                    float r = coreIntensity * 1.0f + glowIntensity * 0.2f;
                    float g = coreIntensity * 1.0f + glowIntensity * 0.9f;
                    float b = coreIntensity * 1.0f + glowIntensity * 1.0f;
                    
                    // Clamp and convert
                    int ir = (int) Math.min(255, r * 255);
                    int ig = (int) Math.min(255, g * 255);
                    int ib = (int) Math.min(255, b * 255);
                    int alpha = (int) Math.min(255, (coreIntensity * 255 + glowIntensity * 150));
                    
                    depthImage.setColorArgb(x, y, (alpha << 24) | (ir << 16) | (ig << 8) | ib);
                } else {
                    // Dim grey background showing distance
                    float normalized = Math.min(1.0f, distFromOrigin / 50.0f);
                    int gray = (int)((1.0f - normalized) * 60);  // Darker background
                    depthImage.setColorArgb(x, y, (255 << 24) | (gray << 16) | (gray << 8) | gray);
                }
            }
        }
        
        ringPixelCount = localRingPixels;
        lastSkyPixels = skyPixels;
        depthTexture.upload();
        
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("mode7_world")
                .kv("origin", String.format("%.1f, %.1f, %.1f", origin.x, origin.y, origin.z))
                .kv("ringDist", ringDistance)
                .kv("ringPixels", ringPixelCount)
                .info("World ring analysis");
        }
    }
    
    private static void renderMode7_WorldPointRing(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        // FULLSCREEN stretched display (pipeline handles alpha blending)
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            0, 0,
            0.0f, 0.0f,
            sw, sh,
            lastWidth, lastHeight
        );
        
        // Stats overlay
        int statX = 5, statY = 25;
        context.fill(statX - 2, statY - 2, statX + 200, statY + 70, 0xA0000000);
        
        context.drawText(client.textRenderer, "Mode 7: Ring (World Point)", statX, statY, 0xFF00FF, true);
        context.drawText(client.textRenderer, "Origin: Player Position", statX, statY + 12, 0xFF00FF, true);
        context.drawText(client.textRenderer,
            String.format("Ring: %.1f blocks (±%.1f)", ringDistance, ringThickness / 2),
            statX, statY + 24, 0xFFFFFF, true);
        context.drawText(client.textRenderer,
            String.format("Ring pixels: %d", ringPixelCount),
            statX, statY + 36, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            "Magenta = Ring | Gray = World",
            statX, statY + 52, 0x888888, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 8: Animated Expanding Ring
    // Full shockwave simulation with expanding radius
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureAnimatedRing(MinecraftClient client, int width, int height) {
        // Update animation
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            animationRadius = elapsed * animationSpeed;
            if (animationRadius > maxAnimationRadius) {
                animating = false;
                animationRadius = maxAnimationRadius;
            }
        }
        
        // Temporarily override ring distance with animation radius
        float savedRingDist = ringDistance;
        ringDistance = animationRadius;
        
        // Reuse Mode 7's logic
        captureRingFromWorldPoint(client, width, height);
        
        // Restore
        ringDistance = savedRingDist;
    }
    
    private static void renderMode8_AnimatedRing(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        // FULLSCREEN stretched display (pipeline handles alpha blending)
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            0, 0,
            0.0f, 0.0f,
            sw, sh,
            lastWidth, lastHeight
        );
        
        // Stats overlay
        int statX = 5, statY = 25;
        context.fill(statX - 2, statY - 2, statX + 220, statY + 90, 0xA0000000);
        
        context.drawText(client.textRenderer, "Mode 8: SHOCKWAVE", statX, statY, 0xFF00FF, true);
        context.drawText(client.textRenderer,
            String.format("Radius: %.1f / %.1f blocks", animationRadius, maxAnimationRadius),
            statX, statY + 12, animating ? 0x00FF00 : 0xFFFF00, true);
        context.drawText(client.textRenderer,
            String.format("Speed: %.1f blocks/sec", animationSpeed),
            statX, statY + 24, 0xAAAAFF, true);
        context.drawText(client.textRenderer,
            String.format("Status: %s", animating ? "EXPANDING" : "STOPPED"),
            statX, statY + 36, animating ? 0x00FF00 : 0xFF0000, true);
        context.drawText(client.textRenderer,
            String.format("Ring pixels: %d", ringPixelCount),
            statX, statY + 48, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            "/directdepth trigger | speed <n>",
            statX, statY + 64, 0x888888, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 9: OVERLAY Ring - Draws ONLY ring pixels, game visible behind!
    // This is the proper shockwave effect
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Store ring pixel positions instead of generating a texture
    private static int[] overlayRingX = new int[50000];
    private static int[] overlayRingY = new int[50000];
    private static float[] overlayRingIntensity = new float[50000];
    private static int overlayRingCount = 0;
    private static int lastSampleDiv = 4;  // Store for render function
    
    private static void captureOverlayRing(MinecraftClient client, int width, int height) {
        // Update animation (if animating)
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            animationRadius = elapsed * animationSpeed;
            if (animationRadius > maxAnimationRadius) {
                animating = false;
                // Don't reset to 0 - keep at max so user can see it
            }
        }
        
        // If no radius set, nothing to show
        if (animationRadius <= 0) {
            overlayRingCount = 0;
            return;
        }
        
        // Use the configurable resolution divisor
        // Default 1 = full res, higher = faster but blockier
        int sampleDiv = Math.max(1, resolutionDivisor);
        lastSampleDiv = sampleDiv;  // Save for render function
        int sampleWidth = width / sampleDiv;
        int sampleHeight = height / sampleDiv;
        
        // Get camera
        var camera = client.gameRenderer.getCamera();
        if (camera == null) {
            overlayRingCount = 0;
            return;
        }
        
        var cameraPos = camera.getPos();
        org.joml.Vector3d origin = new org.joml.Vector3d(
            client.player != null ? client.player.getX() : cameraPos.x,
            client.player != null ? client.player.getY() : cameraPos.y,
            client.player != null ? client.player.getZ() : cameraPos.z
        );
        
        // Camera orientation
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        float fov = client.options.getFov().getValue();
        float aspect = (float)width / height;
        
        // Read depth at sample resolution
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(sampleWidth * sampleHeight);
        GL11.glReadPixels(0, 0, sampleWidth, sampleHeight, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        float currentRadius = animationRadius;
        float innerRing = currentRadius - ringThickness / 2;
        float outerRing = currentRadius + ringThickness / 2;
        
        int count = 0;
        int maxPixels = overlayRingX.length;
        
        for (int sy = 0; sy < sampleHeight && count < maxPixels; sy++) {
            for (int sx = 0; sx < sampleWidth && count < maxPixels; sx++) {
                float depth = depthBuffer.get(sy * sampleWidth + sx);
                if (depth > 0.9999f) continue;  // Skip sky
                
                // Convert to NDC
                float ndcX = (sx / (float)sampleWidth) * 2.0f - 1.0f;
                float ndcY = (sy / (float)sampleHeight) * 2.0f - 1.0f;
                
                // Linearize depth
                float near = 0.05f;
                float eyeDist = (2.0f * near * farPlane) / (farPlane + near - depth * (farPlane - near));
                
                // Calculate world position
                float tanHalfFov = (float) Math.tan(Math.toRadians(fov / 2.0));
                float yawRad = (float) Math.toRadians(-yaw);
                float pitchRad = (float) Math.toRadians(-pitch);
                
                float localX = ndcX * tanHalfFov * aspect;
                float localY = ndcY * tanHalfFov;
                float localZ = -1.0f;
                
                float cosPitch = (float) Math.cos(pitchRad);
                float sinPitch = (float) Math.sin(pitchRad);
                float cosYaw = (float) Math.cos(yawRad);
                float sinYaw = (float) Math.sin(yawRad);
                
                float tempY = localY * cosPitch - localZ * sinPitch;
                float tempZ = localY * sinPitch + localZ * cosPitch;
                localY = tempY;
                localZ = tempZ;
                
                float tempX = localX * cosYaw + localZ * sinYaw;
                tempZ = -localX * sinYaw + localZ * cosYaw;
                localX = tempX;
                localZ = tempZ;
                
                float len = (float) Math.sqrt(localX * localX + localY * localY + localZ * localZ);
                float dirX = localX / len;
                float dirY = localY / len;
                float dirZ = localZ / len;
                
                double worldX = cameraPos.x + dirX * eyeDist;
                double worldY = cameraPos.y + dirY * eyeDist;
                double worldZ = cameraPos.z + dirZ * eyeDist;
                
                // Distance from origin
                double dx = worldX - origin.x;
                double dy = worldY - origin.y;
                double dz = worldZ - origin.z;
                float distFromOrigin = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                // Check if within ring
                if (distFromOrigin >= innerRing && distFromOrigin <= outerRing) {
                    // Map sample position to SCREEN position (scale by sampleDiv)
                    overlayRingX[count] = sx * sampleDiv;
                    overlayRingY[count] = height - (sy * sampleDiv) - sampleDiv;  // Flip Y
                    
                    // Calculate intensity (brightest at ring center)
                    float ringCenter = currentRadius;
                    float distFromCenter = Math.abs(distFromOrigin - ringCenter);
                    overlayRingIntensity[count] = 1.0f - (distFromCenter / (ringThickness / 2));
                    
                    count++;
                }
            }
        }
        
        overlayRingCount = count;
    }
    
    private static void renderMode9_OverlayRing(DrawContext context, MinecraftClient client, int sw, int sh) {
        // DON'T draw a texture - draw individual rectangles for ring pixels!
        
        int pixelSize = Math.max(1, lastSampleDiv);  // Match what capture used
        
        for (int i = 0; i < overlayRingCount; i++) {
            int x = overlayRingX[i];
            int y = overlayRingY[i];
            float intensity = overlayRingIntensity[i];
            
            // Color: Magenta with intensity-based brightness
            int r = (int)(255 * intensity);
            int g = (int)(50 * intensity);
            int b = (int)(255 * intensity);
            int alpha = (int)(220 * intensity);  // More opaque
            int color = (alpha << 24) | (r << 16) | (g << 8) | b;
            
            context.fill(x, y, x + pixelSize, y + pixelSize, color);
        }
        
        // Stats overlay
        int statX = 5, statY = 25;
        context.fill(statX - 2, statY - 2, statX + 260, statY + 100, 0xA0000000);
        
        context.drawText(client.textRenderer, "Mode 9: OVERLAY SHOCKWAVE", statX, statY, 0xFF00FF, true);
        context.drawText(client.textRenderer,
            String.format("Radius: %.1f / %.1f blocks", animationRadius, maxAnimationRadius),
            statX, statY + 12, animating ? 0x00FF00 : 0xFFFF00, true);
        context.drawText(client.textRenderer,
            String.format("Speed: %.1f | Resolution: 1/%d", animationSpeed, resolutionDivisor),
            statX, statY + 24, 0xAAAAFF, true);
        context.drawText(client.textRenderer,
            String.format("Ring pixels: %d | Status: %s", overlayRingCount, animating ? "ANIMATING" : "STATIC"),
            statX, statY + 36, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            "Commands:", statX, statY + 52, 0x888888, true);
        context.drawText(client.textRenderer,
            "/directdepth trigger | radius <n> | speed <n>",
            statX, statY + 64, 0x666666, true);
        context.drawText(client.textRenderer,
            "/directdepth resolution <n> (1=full, 4=fast)",
            statX, statY + 76, 0x666666, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODE 10: TRANSPARENT TEXTURE Ring - Same as Mode 7/8 but with transparency
    // This is an experimental mode trying to achieve game-visible overlay
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void captureTransparentRing(MinecraftClient client, int width, int height) {
        // Use FULL resolution always (same as Mode 7/8)
        int captureWidth = width;
        int captureHeight = height;
        
        // Get camera position
        var camera = client.gameRenderer.getCamera();
        if (camera == null) return;
        
        var cameraPos = camera.getPos();
        
        // Get ring origin (player position)
        org.joml.Vector3d origin;
        if (usePlayerAsOrigin && client.player != null) {
            origin = new org.joml.Vector3d(
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
            );
        } else {
            origin = new org.joml.Vector3d(cameraPos.x, cameraPos.y, cameraPos.z);
        }
        
        // Animation update
        if (animating) {
            float elapsed = (System.currentTimeMillis() - animationStartTime) / 1000.0f;
            animationRadius = elapsed * animationSpeed;
            if (animationRadius > maxAnimationRadius) {
                animating = false;
            }
        }
        
        // Use animation radius for the ring
        float currentRingDist = animating || animationRadius > 0 ? animationRadius : ringDistance;
        
        // Ensure texture exists at correct size (same pattern as Mode 7)
        if (depthImage == null || lastWidth != captureWidth || lastHeight != captureHeight) {
            if (depthImage != null) depthImage.close();
            depthImage = new NativeImage(NativeImage.Format.RGBA, captureWidth, captureHeight, false);
            lastWidth = captureWidth;
            lastHeight = captureHeight;
            
            if (depthTexture != null) depthTexture.close();
            depthTexture = new NativeImageBackedTexture(DEPTH_TEXTURE_ID::toString, depthImage);
            
            var client2 = MinecraftClient.getInstance();
            if (client2.getTextureManager() != null) {
                client2.getTextureManager().registerTexture(DEPTH_TEXTURE_ID, depthTexture);
                textureRegistered = true;
            }
        }
        
        // Read depth buffer
        FloatBuffer depthBuffer = BufferUtils.createFloatBuffer(captureWidth * captureHeight);
        GL11.glReadPixels(0, 0, captureWidth, captureHeight, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthBuffer);
        
        // Camera orientation (same math as Mode 7)
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
        double aspectRatio = (double) captureWidth / captureHeight;
        
        int localRingPixels = 0;
        float innerRing = currentRingDist - ringThickness / 2.0f;
        float outerRing = currentRingDist + ringThickness / 2.0f;
        
        for (int y = 0; y < captureHeight; y++) {
            for (int x = 0; x < captureWidth; x++) {
                int srcY = captureHeight - 1 - y;
                float depth = depthBuffer.get(srcY * captureWidth + x);
                
                // Sky = fully transparent
                if (depth >= 0.9999f) {
                    depthImage.setColorArgb(x, y, 0x00000000);
                    continue;
                }
                
                // Convert depth to eye-space distance
                float eyeDist = depthToDistance(depth);
                
                // Calculate ray direction
                double ndcX = (2.0 * x / captureWidth - 1.0) * aspectRatio * Math.tan(fov / 2);
                double ndcY = (1.0 - 2.0 * y / captureHeight) * Math.tan(fov / 2);
                
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
                
                // Check if within ring
                if (distFromOrigin >= innerRing && distFromOrigin <= outerRing) {
                    localRingPixels++;
                    // Intensity gradient from ring center
                    float distFromCenter = Math.abs(distFromOrigin - currentRingDist);
                    float intensity = 1.0f - (distFromCenter / (ringThickness / 2));
                    intensity = Math.max(0.0f, Math.min(1.0f, intensity));
                    
                    // Magenta with alpha
                    int alpha = (int)(200 * intensity);
                    int r = (int)(255 * intensity);
                    int g = (int)(80 * intensity);
                    int b = (int)(255 * intensity);
                    depthImage.setColorArgb(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
                } else {
                    // NOT in ring = fully transparent
                    depthImage.setColorArgb(x, y, 0x00000000);
                }
            }
        }
        
        ringPixelCount = localRingPixels;
        depthTexture.upload();
    }
    
    private static void renderMode10_TransparentRing(DrawContext context, MinecraftClient client, int sw, int sh) {
        if (!textureRegistered || depthTexture == null) return;
        
        // Draw texture stretched to fullscreen
        // GUI_TEXTURED should handle alpha blending
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            DEPTH_TEXTURE_ID,
            0, 0,
            0.0f, 0.0f,
            sw, sh,
            lastWidth, lastHeight
        );
        
        // Stats overlay
        int statX = 5, statY = 25;
        context.fill(statX - 2, statY - 2, statX + 260, statY + 80, 0xA0000000);
        
        context.drawText(client.textRenderer, "Mode 10: TRANSPARENT Ring (Experimental)", statX, statY, 0xFF00FF, true);
        context.drawText(client.textRenderer,
            String.format("Radius: %.1f blocks | Ring pixels: %d", 
                animating || animationRadius > 0 ? animationRadius : ringDistance, ringPixelCount),
            statX, statY + 12, 0xAAFFAA, true);
        context.drawText(client.textRenderer,
            animating ? "Status: ANIMATING" : "Status: STATIC",
            statX, statY + 24, animating ? 0x00FF00 : 0xFFFF00, true);
        context.drawText(client.textRenderer,
            "/directdepth trigger | radius <n>",
            statX, statY + 40, 0x666666, true);
        context.drawText(client.textRenderer,
            "If broken: /directdepth 8 (working mode)",
            statX, statY + 52, 0xFF6666, true);
    }
    
    /**
     * Start the shockwave animation.
     */
    public static void triggerAnimation() {
        animating = true;
        animationRadius = 0.0f;
        animationStartTime = System.currentTimeMillis();
        Logging.RENDER.topic("direct_depth")
            .kv("speed", animationSpeed)
            .kv("maxRadius", maxAnimationRadius)
            .info("Animation triggered");
    }
    
    /**
     * Set animation speed.
     */
    public static void setAnimationSpeed(float speed) {
        animationSpeed = Math.max(1.0f, speed);
        Logging.RENDER.topic("direct_depth")
            .kv("animationSpeed", animationSpeed)
            .info("Animation speed updated");
    }
    
    /**
     * Set max animation radius.
     */
    public static void setMaxRadius(float radius) {
        maxAnimationRadius = Math.max(10.0f, radius);
        Logging.RENDER.topic("direct_depth")
            .kv("maxAnimationRadius", maxAnimationRadius)
            .info("Max radius updated");
    }
    
    /**
     * Set current radius directly (static mode - no animation).
     */
    public static void setRadius(float radius) {
        animating = false;  // Stop any animation
        animationRadius = Math.max(0.0f, radius);
        Logging.RENDER.topic("direct_depth")
            .kv("radius", animationRadius)
            .info("Static radius set");
    }
    
    /**
     * Set resolution divisor for pixelation effect.
     * 1 = full resolution, 2 = half, 4 = quarter, etc.
     */
    public static void setResolution(int divisor) {
        resolutionDivisor = Math.max(1, Math.min(32, divisor));
        // Force texture recreation on next frame
        lastWidth = 0;
        lastHeight = 0;
        Logging.RENDER.topic("direct_depth")
            .kv("resolutionDivisor", resolutionDivisor)
            .info("Resolution updated");
    }
    
    public static int getResolution() {
        return resolutionDivisor;
    }
    
    public static void dispose() {
        if (depthTexture != null) {
            depthTexture.close();
            depthTexture = null;
        }
        if (depthImage != null) {
            depthImage.close();
            depthImage = null;
        }
        textureRegistered = false;
    }
}
