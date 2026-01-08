package net.cyberpunk042.client.gui.preview;

import net.cyberpunk042.client.field.render.FieldRenderer;
import net.cyberpunk042.field.FieldDefinition;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Handles offscreen framebuffer rendering for field previews.
 * 
 * <p>This class encapsulates all OpenGL state management and FBO operations
 * for rendering a {@link FieldDefinition} to an offscreen buffer, then
 * blitting the result to the GUI.
 * 
 * <h3>Rendering Pipeline</h3>
 * <ol>
 *   <li>Save current GL state (viewport)</li>
 *   <li>Bind offscreen FBO and set viewport</li>
 *   <li>Render opaque background quad</li>
 *   <li>Render field using {@link FieldRenderer}</li>
 *   <li>Capture FBO to dynamic texture</li>
 *   <li>Restore GL state</li>
 *   <li>Blit texture to GUI</li>
 * </ol>
 * 
 * @see FieldPreviewRenderer
 * @see PreviewFramebuffer
 */
@Environment(EnvType.CLIENT)
public class PreviewFboRenderer {
    
    // Rendering constants
    private static final int FBO_SCALE = 4;           // Render at 4x resolution
    private static final float CAMERA_DISTANCE = 2.5f; // Fixed camera distance
    private static final int BG_COLOR = 0x1E1E2E;     // Dark background (RGB)
    
    // Managed resources
    private PreviewFramebuffer framebuffer;
    
    // Transient state during render
    private int[] savedViewport = new int[4];
    private int fboWidth;
    private int fboHeight;
    private float cameraDistance;
    
    /**
     * Renders a field definition to the GUI at the specified bounds.
     * 
     * @param context Draw context for GUI rendering
     * @param definition Field definition to render
     * @param scale Scale factor from pulse animation
     * @param rotationX Pitch rotation in degrees
     * @param rotationY Yaw rotation in degrees
     * @param x1 Left bound
     * @param y1 Top bound
     * @param x2 Right bound
     * @param y2 Bottom bound
     * @param timeTicks Animation time in ticks
     * @param alpha Alpha multiplier for field
     */
    public void render(DrawContext context, FieldDefinition definition,
                       float scale, float rotationX, float rotationY,
                       int x1, int y1, int x2, int y2,
                       float timeTicks, float alpha) {
        // Calculate dimensions
        int width = x2 - x1;
        int height = y2 - y1;
        if (width <= 0 || height <= 0) return;
        
        this.fboWidth = width * FBO_SCALE;
        this.fboHeight = height * FBO_SCALE;
        
        // Ensure framebuffer is ready
        if (!ensureFramebuffer(context, x1, y1, x2, y2)) {
            return;
        }
        
        // Save GL state
        saveGlState();
        
        // Setup and bind FBO
        setupFbo();
        
        // Calculate camera distance with aspect ratio correction
        float aspect = (float) fboWidth / (float) fboHeight;
        float aspectFactor = aspect < 1.0f ? 1.0f / aspect : 1.0f;
        this.cameraDistance = CAMERA_DISTANCE * aspectFactor;
        
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        
        // Render background and field
        renderBackground(immediate);
        renderField(immediate, definition, scale, rotationX, rotationY, timeTicks, alpha);
        
        // Capture and restore state
        captureAndRestore(client);
        
        // Blit to GUI
        blitToGui(context, x1, y1, x2, y2);
    }
    
    /**
     * Disposes managed resources. Call when the preview is no longer needed.
     */
    public void dispose() {
        if (framebuffer != null) {
            framebuffer.close();
            framebuffer = null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE: Setup Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean ensureFramebuffer(DrawContext context, int x1, int y1, int x2, int y2) {
        if (framebuffer == null) {
            framebuffer = new PreviewFramebuffer();
        }
        framebuffer.ensureSize(fboWidth, fboHeight);
        
        if (framebuffer.getFramebuffer() == null) {
            context.fill(x1, y1, x2, y2, 0xFFFF0000);  // Red = no framebuffer
            return false;
        }
        return true;
    }
    
    private void saveGlState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
    }
    
    private void setupFbo() {
        framebuffer.prepare();
        Framebuffer fb = framebuffer.getFramebuffer();
        
        // Bind framebuffer using Iris method (for shader compatibility)
        try {
            var bindMethod = fb.getClass().getMethod("iris$bindFramebuffer");
            bindMethod.invoke(fb);
        } catch (Exception e) {
            // Fallback handled by PreviewFramebuffer
        }
        
        // Set viewport to FBO dimensions
        GL11.glViewport(0, 0, fboWidth, fboHeight);
        
        // Clear with opaque dark background
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glClearColor(0.12f, 0.12f, 0.18f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE: Rendering Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void renderBackground(VertexConsumerProvider.Immediate immediate) {
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(0, 0, -cameraDistance);
        
        RenderLayer layer = RenderLayer.getEntityTranslucent(
            Identifier.of("minecraft", "textures/misc/white.png")
        );
        var consumer = immediate.getBuffer(layer);
        
        // Background quad parameters
        float size = 50f;
        float z = -5f;
        int r = (BG_COLOR >> 16) & 0xFF;
        int g = (BG_COLOR >> 8) & 0xFF;
        int b = BG_COLOR & 0xFF;
        int a = 0xFF;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        var entry = matrices.peek();
        var matrix = entry.getPositionMatrix();
        
        // Emit quad vertices
        consumer.vertex(matrix, -size, -size, z)
            .color(r, g, b, a).texture(0f, 0f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light)
            .normal(entry, 0f, 0f, 1f);
        consumer.vertex(matrix, -size, size, z)
            .color(r, g, b, a).texture(0f, 1f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light)
            .normal(entry, 0f, 0f, 1f);
        consumer.vertex(matrix, size, size, z)
            .color(r, g, b, a).texture(1f, 1f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light)
            .normal(entry, 0f, 0f, 1f);
        consumer.vertex(matrix, size, -size, z)
            .color(r, g, b, a).texture(1f, 0f)
            .overlay(OverlayTexture.DEFAULT_UV).light(light)
            .normal(entry, 0f, 0f, 1f);
        
        immediate.draw();
        matrices.pop();
    }
    
    private void renderField(VertexConsumerProvider.Immediate immediate,
                             FieldDefinition definition,
                             float scale, float rotationX, float rotationY,
                             float timeTicks, float alpha) {
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        
        // View transform
        matrices.translate(0, 0, -cameraDistance);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationY));
        matrices.scale(scale, scale, scale);
        
        // Render field
        FieldRenderer.render(
            matrices,
            immediate,
            definition,
            Vec3d.ZERO,
            1.0f,
            timeTicks,
            alpha
        );
        
        immediate.draw();
        matrices.pop();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE: Capture and Restore
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void captureAndRestore(MinecraftClient client) {
        // Capture FBO content to texture
        PreviewDynamicTexture.getInstance().captureFromBoundFbo(fboWidth, fboHeight);
        
        // Unbind FBO (restore main framebuffer)
        try {
            var mainFb = client.getFramebuffer();
            var unbindMethod = mainFb.getClass().getMethod("iris$bindFramebuffer");
            unbindMethod.invoke(mainFb);
        } catch (Exception e) {
            // Fallback: bind default framebuffer
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        
        // Restore viewport
        GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    }
    
    private void blitToGui(DrawContext context, int x1, int y1, int x2, int y2) {
        PreviewDynamicTexture texture = PreviewDynamicTexture.getInstance();
        
        if (!texture.isReady()) {
            context.fill(x1, y1, x2, y2, 0xFFFF0000);  // Red = not ready
            return;
        }
        
        try {
            int width = x2 - x1;
            int height = y2 - y1;
            
            // Opaque background to prevent world bleed-through
            context.fill(x1, y1, x2, y2, 0xFF000000 | BG_COLOR);
            
            // Blit captured texture
            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                PreviewDynamicTexture.getTextureId(),
                x1, y1,
                0.0f, 0.0f,
                width, height,
                width, height
            );
        } catch (Exception e) {
            context.fill(x1, y1, x2, y2, 0xFFFF8800);  // Orange = error
        }
    }
}
