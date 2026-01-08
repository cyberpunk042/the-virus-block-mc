package net.cyberpunk042.client.gui.render;

import net.cyberpunk042.client.field.render.FieldRenderer;
import net.cyberpunk042.client.gui.state.DefinitionBuilder;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Renders the test field preview when active.
 * 
 * <p>Uses the standard FieldRenderer pipeline with the definition
 * built from FieldEditState. This replaces the old SimplifiedFieldRenderer.</p>
 */
public final class TestFieldRenderer {
    
    private static boolean initialized = false;
    
    private TestFieldRenderer() {}
    
    /**
     * Registers the test field renderer with world render events.
     * Call once during client initialization.
     */
    public static void init() {
        if (initialized) {
            Logging.GUI.topic("render").debug("TestFieldRenderer already initialized, skipping");
            return;
        }
        initialized = true;
        WorldRenderEvents.AFTER_ENTITIES.register(TestFieldRenderer::render);
        Logging.GUI.topic("render").info("TestFieldRenderer initialized");
    }
    
    /**
     * Main render callback.
     */
    private static void render(WorldRenderContext context) {
        // Check if test field is active
        if (!FieldEditStateHolder.isTestFieldActive()) {
            return;
        }
        
        // Skip if personal field is visible - it now uses the live definition 
        // with follow mode, so we don't need to render separately
        if (net.cyberpunk042.client.visual.ClientFieldManager.get().personalTracker().isVisible()) {
            return;
        }
        
        FieldEditState state = FieldEditStateHolder.get();
        if (state == null) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        float worldTime = context.world().getTime() + tickDelta;
        
        // Calculate position - use LERPED player position for smooth rendering
        Vec3d playerPos = player.getLerpedPos(tickDelta).add(0, 1.0, 0); // Center at chest height
        Vec3d renderPos = playerPos.subtract(camPos);
        
        // Build definition from current state
        FieldDefinition definition = DefinitionBuilder.fromState(state);
        
        if (definition == null || definition.layers() == null || definition.layers().isEmpty()) {
            return;
        }
        
        // Render using the standard FieldRenderer
        FieldRenderer.render(
            matrices,
            consumers,
            definition,
            renderPos,
            1.0f,       // scale
            worldTime,  // time
            1.0f        // alpha
        );
    }
    
    /**
     * Force mesh rebuild (no-op in new renderer, meshes are built on demand).
     * Kept for API compatibility during transition.
     */
    public static void markDirty() {
        // No-op - FieldRenderer rebuilds meshes as needed
    }
}
