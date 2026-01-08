package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * TEST: Renders a fullscreen overlay using VertexConsumer + RenderLayer.
 * 
 * <p>This approach SHOULD trigger CustomUniformBinder via bindDefaultUniforms(),
 * which is how FresnelParams works for field rendering.
 * 
 * <p>Command: /shockwavetest
 */
public final class ShockwaveTestRenderer {
    
    private static boolean enabled = false;
    private static float testRadius = 20.0f;
    private static int frameCount = 0;
    
    // Toggle between DrawContext mode (works) and VertexConsumer mode (testing)
    private static boolean useVertexConsumer = false;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void toggle() {
        enabled = !enabled;
        Logging.RENDER.topic("shockwave_test")
            .kv("enabled", enabled)
            .info("ShockwaveTestRenderer toggled");
    }
    
    public static void setRadius(float r) {
        testRadius = r;
        enabled = true;
        Logging.RENDER.topic("shockwave_test")
            .kv("radius", r)
            .info("Test radius set");
    }
    
    public static float getRadius() {
        return testRadius;
    }
    
    public static void toggleMode() {
        useVertexConsumer = !useVertexConsumer;
        Logging.RENDER.topic("shockwave_test")
            .kv("mode", useVertexConsumer ? "VertexConsumer" : "DrawContext")
            .info("Render mode toggled");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void init() {
        ShockwaveTestPipeline.init();
        Logging.RENDER.topic("shockwave_test")
            .info("ShockwaveTestRenderer initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Render a test fullscreen overlay.
     * Called from ShockwaveTestMixin at end of HUD render.
     */
    public static void render(DrawContext context) {
        if (!enabled) return;
        
        frameCount++;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        
        if (useVertexConsumer) {
            // VertexConsumer mode - this should trigger uniform binding!
            renderWithVertexConsumer(client, sw, sh);
        } else {
            // DrawContext mode - simple, proven to work
            renderWithDrawContext(context, client, sw, sh);
        }
    }
    
    /**
     * Simple DrawContext rendering (proven to work).
     */
    private static void renderWithDrawContext(DrawContext context, MinecraftClient client, int sw, int sh) {
        // Log every 60 frames
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("shockwave_test")
                .kv("radius", testRadius)
                .kv("mode", "DrawContext")
                .info("Rendering");
        }
        
        // Magenta border
        int borderSize = 10;
        int color = 0x99FF00FF;
        context.fill(0, 0, sw, borderSize, color);
        context.fill(0, sh - borderSize, sw, sh, color);
        context.fill(0, 0, borderSize, sh, color);
        context.fill(sw - borderSize, 0, sw, sh, color);
        
        // Radius text
        String text = String.format("DrawContext - RADIUS: %.1f", testRadius);
        int textWidth = client.textRenderer.getWidth(text);
        context.drawText(client.textRenderer, text, (sw - textWidth) / 2, sh / 2, 0xFFFF00FF, true);
    }
    
    /**
     * VertexConsumer rendering - this SHOULD trigger CustomUniformBinder!
     */
    private static void renderWithVertexConsumer(MinecraftClient client, int sw, int sh) {
        // Log every 60 frames
        if (frameCount % 60 == 1) {
            Logging.RENDER.topic("shockwave_test")
                .kv("radius", testRadius)
                .kv("mode", "VertexConsumer")
                .info("Rendering - check if [shockwave_bind] appears!");
        }
        
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        
        // Use entity translucent layer - this goes through RenderPipeline
        RenderLayer layer = RenderLayer.getEntityTranslucent(
            Identifier.of("minecraft", "textures/misc/white.png")
        );
        
        VertexConsumer consumer = immediate.getBuffer(layer);
        var entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        
        // Draw a green border (different from DrawContext magenta)
        int r = 0, g = 255, b = 0, a = 180;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int borderSize = 15;
        
        // Top border quad
        emitQuad(consumer, matrix, entry, 0, 0, sw, borderSize, r, g, b, a, light);
        
        // Bottom border quad
        emitQuad(consumer, matrix, entry, 0, sh - borderSize, sw, sh, r, g, b, a, light);
        
        // Left border quad
        emitQuad(consumer, matrix, entry, 0, 0, borderSize, sh, r, g, b, a, light);
        
        // Right border quad  
        emitQuad(consumer, matrix, entry, sw - borderSize, 0, sw, sh, r, g, b, a, light);
        
        // Flush the buffer - this triggers the actual draw call with uniform binding!
        immediate.draw(layer);
        
        matrices.pop();
    }
    
    private static void emitQuad(VertexConsumer consumer, Matrix4f matrix, MatrixStack.Entry entry,
                                  float x1, float y1, float x2, float y2,
                                  int r, int g, int b, int a, int light) {
        // Quad with 4 vertices (CCW winding)
        consumer.vertex(matrix, x1, y1, 0).color(r, g, b, a)
            .texture(0f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
        consumer.vertex(matrix, x1, y2, 0).color(r, g, b, a)
            .texture(0f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
        consumer.vertex(matrix, x2, y2, 0).color(r, g, b, a)
            .texture(1f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
        consumer.vertex(matrix, x2, y1, 0).color(r, g, b, a)
            .texture(1f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0f, 0f, 1f);
    }
    
    private ShockwaveTestRenderer() {}
}
