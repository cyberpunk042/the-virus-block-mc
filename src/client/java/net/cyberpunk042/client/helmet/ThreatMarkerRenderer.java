package net.cyberpunk042.client.helmet;

import net.cyberpunk042.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

/**
 * Renders 2D threat markers on screen for virus sources.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Projects world positions to screen coordinates</li>
 *   <li>Renders diamond markers at projected positions</li>
 *   <li>Edge indicators for off-screen threats</li>
 *   <li>Color-coded by distance</li>
 *   <li>Respects config limits (max count, render range)</li>
 * </ul>
 */
public final class ThreatMarkerRenderer {
    
    private ThreatMarkerRenderer() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final int MARKER_SIZE = 8;
    private static final int EDGE_MARGIN = 30;
    
    // Colors
    private static final int COLOR_CLOSE = 0xFFFF2222;    // Red (< 20 blocks)
    private static final int COLOR_MEDIUM = 0xFFFFAA00;   // Orange (20-50 blocks)
    private static final int COLOR_FAR = 0xFF00FFFF;      // Cyan (> 50 blocks)
    private static final int COLOR_EDGE = 0xFFFFFFFF;     // White for edge indicators
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN RENDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Renders threat markers on screen.
     */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.gameRenderer == null) return;
        
        HelmetHudConfig config = HelmetHudConfig.get();
        if (!config.isMarkersEnabled()) return;
        
        if (!hasAugmentedHelmet(client)) return;
        
        HelmetHudState state = HelmetHudState.get();
        if (!state.hasData()) return;
        
        // Get positions from virus block telemetry (server-provided, all players)
        var telemetryState = net.cyberpunk042.client.visual.shader.VirusBlockTelemetryState.get();
        List<BlockPos> positions = telemetryState.getNearbyPositions();
        
        // DEBUG: Draw a test marker at screen center to verify rendering works
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float alpha = state.getFadeAlpha() * config.getHudOpacity();
        
        // DEBUG: Show positions count in bottom-left
        String debugText = "Markers: " + positions.size() + " positions";
        context.drawText(client.textRenderer, debugText, 10, screenHeight - 20, 0xFFFF00FF, true);
        
        if (positions.isEmpty()) return;
        
        Vec3d cameraPos = client.player.getEyePos();
        Camera camera = client.gameRenderer.getCamera();
        
        // Get projection and view matrices
        Matrix4f projectionMatrix = client.gameRenderer.getBasicProjectionMatrix(
            client.options.getFov().getValue().floatValue());
        Matrix4f viewMatrix = new Matrix4f().rotation(camera.getRotation());
        
        // Combined matrix
        Matrix4f mvpMatrix = new Matrix4f(projectionMatrix).mul(viewMatrix);
        
        int maxMarkers = config.getMaxMarkerCount() > 0 ? config.getMaxMarkerCount() : positions.size();
        float maxRange = config.getMarkerRenderRange();
        
        int rendered = 0;
        for (BlockPos pos : positions) {
            if (rendered >= maxMarkers) break;
            
            Vec3d worldPos = Vec3d.ofCenter(pos);
            double distance = cameraPos.distanceTo(worldPos);
            
            // Skip if beyond render range
            if (maxRange > 0 && distance > maxRange) continue;
            
            // Project to screen
            ScreenPos screenPos = worldToScreen(worldPos, cameraPos, mvpMatrix, screenWidth, screenHeight);
            
            if (screenPos != null) {
                int color = getDistanceColor(distance);
                
                if (screenPos.onScreen) {
                    // Render marker at position
                    renderMarker(context, screenPos.x, screenPos.y, distance, color, alpha);
                } else {
                    // Render edge indicator
                    renderEdgeIndicator(context, screenPos.x, screenPos.y, 
                        screenWidth, screenHeight, color, alpha);
                }
                rendered++;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WORLD TO SCREEN PROJECTION
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static ScreenPos worldToScreen(Vec3d worldPos, Vec3d cameraPos, 
                                            Matrix4f mvpMatrix, int screenWidth, int screenHeight) {
        // Convert to camera-relative coordinates
        Vector3f relPos = new Vector3f(
            (float)(worldPos.x - cameraPos.x),
            (float)(worldPos.y - cameraPos.y),
            (float)(worldPos.z - cameraPos.z)
        );
        
        // Apply MVP matrix
        Vector4f clipPos = new Vector4f(relPos.x, relPos.y, relPos.z, 1.0f);
        mvpMatrix.transform(clipPos);
        
        // Behind camera check
        if (clipPos.w <= 0) {
            // Behind camera - calculate edge position
            float angle = (float)Math.atan2(relPos.x, relPos.z);
            int edgeX = screenWidth / 2 + (int)(Math.sin(angle) * (screenWidth / 2 - EDGE_MARGIN));
            int edgeY = EDGE_MARGIN;  // Top edge for behind targets
            return new ScreenPos(edgeX, edgeY, false);
        }
        
        // Perspective divide
        float ndcX = clipPos.x / clipPos.w;
        float ndcY = clipPos.y / clipPos.w;
        
        // Convert to screen coordinates
        int screenX = (int)((ndcX + 1.0f) * 0.5f * screenWidth);
        int screenY = (int)((1.0f - ndcY) * 0.5f * screenHeight);  // Flip Y
        
        // Check if on screen
        boolean onScreen = screenX >= EDGE_MARGIN && screenX <= screenWidth - EDGE_MARGIN
                        && screenY >= EDGE_MARGIN && screenY <= screenHeight - EDGE_MARGIN;
        
        if (!onScreen) {
            // Clamp to edge
            screenX = MathHelper.clamp(screenX, EDGE_MARGIN, screenWidth - EDGE_MARGIN);
            screenY = MathHelper.clamp(screenY, EDGE_MARGIN, screenHeight - EDGE_MARGIN);
        }
        
        return new ScreenPos(screenX, screenY, onScreen);
    }
    
    private record ScreenPos(int x, int y, boolean onScreen) {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARKER RENDERING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderMarker(DrawContext context, int x, int y, double distance, 
                                      int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        if (a < 10) return;
        
        int c = (a << 24) | (color & 0x00FFFFFF);
        int cGlow = ((a / 3) << 24) | (color & 0x00FFFFFF);
        
        int size = MARKER_SIZE;
        int half = size / 2;
        
        // Glow (larger, dimmer)
        drawDiamond(context, x, y, half + 3, cGlow);
        
        // Main diamond
        drawDiamond(context, x, y, half, c);
        
        // Distance text (small, below marker)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null && distance < 100) {
            String distText = (int)distance + "m";
            int textWidth = client.textRenderer.getWidth(distText);
            int textColor = (a << 24) | 0xFFFFFF;
            context.drawText(client.textRenderer, distText, 
                x - textWidth / 2, y + half + 4, textColor, true);
        }
    }
    
    private static void drawDiamond(DrawContext context, int cx, int cy, int radius, int color) {
        // Diamond shape using 4 triangular fills (approximated with rects)
        for (int i = 0; i <= radius; i++) {
            int width = radius - i;
            // Top half
            context.fill(cx - width, cy - i, cx + width + 1, cy - i + 1, color);
            // Bottom half
            context.fill(cx - width, cy + i, cx + width + 1, cy + i + 1, color);
        }
    }
    
    private static void renderEdgeIndicator(DrawContext context, int x, int y,
                                             int screenWidth, int screenHeight,
                                             int color, float alpha) {
        int a = (int)(200 * alpha);
        if (a < 10) return;
        
        int c = (a << 24) | (color & 0x00FFFFFF);
        
        // Determine which edge and draw arrow
        int arrowSize = 6;
        
        if (y <= EDGE_MARGIN) {
            // Top edge - arrow pointing up
            drawArrowUp(context, x, EDGE_MARGIN, arrowSize, c);
        } else if (y >= screenHeight - EDGE_MARGIN) {
            // Bottom edge - arrow pointing down
            drawArrowDown(context, x, screenHeight - EDGE_MARGIN, arrowSize, c);
        } else if (x <= EDGE_MARGIN) {
            // Left edge - arrow pointing left
            drawArrowLeft(context, EDGE_MARGIN, y, arrowSize, c);
        } else {
            // Right edge - arrow pointing right
            drawArrowRight(context, screenWidth - EDGE_MARGIN, y, arrowSize, c);
        }
    }
    
    private static void drawArrowUp(DrawContext context, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            context.fill(x - i, y + i, x + i + 1, y + i + 1, color);
        }
    }
    
    private static void drawArrowDown(DrawContext context, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            context.fill(x - i, y - i, x + i + 1, y - i + 1, color);
        }
    }
    
    private static void drawArrowLeft(DrawContext context, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            context.fill(x + i, y - i, x + i + 1, y + i + 1, color);
        }
    }
    
    private static void drawArrowRight(DrawContext context, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            context.fill(x - i, y - i, x - i + 1, y + i + 1, color);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static int getDistanceColor(double distance) {
        if (distance < 30) return COLOR_CLOSE;   // Red = danger zone
        if (distance < 100) return COLOR_MEDIUM; // Orange = caution
        return COLOR_FAR;                        // Cyan = safe distance
    }
    
    private static boolean hasAugmentedHelmet(MinecraftClient client) {
        if (client.player == null) return false;
        ItemStack headSlot = client.player.getEquippedStack(EquipmentSlot.HEAD);
        return !headSlot.isEmpty() && headSlot.isOf(ModItems.AUGMENTED_HELMET);
    }
    
    /**
     * Registers the threat marker renderer.
     */
    public static void init() {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
            ThreatMarkerRenderer::render);
    }
}
