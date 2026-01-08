package net.cyberpunk042.client.init.nodes;

import net.cyberpunk042.client.color.CorruptedColorProviders;
import net.cyberpunk042.client.render.CorruptedFireTextures;
import net.cyberpunk042.client.render.VirusFluidRenderers;
import net.cyberpunk042.client.render.entity.CorruptedWormRenderer;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.client.render.entity.TntEntityRenderer;

/**
 * Client visual/render initialization nodes.
 */
public final class ClientVisualNodes {
    
    private ClientVisualNodes() {}
    
    /**
     * Block color providers (corrupted blocks tinting).
     */
    public static final InitNode COLOR_PROVIDERS = InitNode.simple(
        "color_providers", "Color Providers",
        () -> {
            CorruptedColorProviders.register();
            return 1;
        }
    );
    
    /**
     * Fire texture overlays.
     */
    public static final InitNode FIRE_TEXTURES = InitNode.simple(
        "fire_textures", "Fire Textures",
        () -> {
            CorruptedFireTextures.bootstrap();
            return 1;
        }
    );
    
    /**
     * Fluid renderers.
     */
    public static final InitNode FLUID_RENDERERS = InitNode.simple(
        "fluid_renderers", "Fluid Renderers",
        () -> {
            VirusFluidRenderers.register();
            return 1;
        }
    );
    
    /**
     * Entity renderers.
     */
    public static final InitNode ENTITY_RENDERERS = InitNode.simple(
        "entity_renderers", "Entity Renderers",
        () -> {
            EntityRendererRegistry.register(ModEntities.FALLING_MATRIX_CUBE, FallingBlockEntityRenderer::new);
            EntityRendererRegistry.register(ModEntities.CORRUPTED_WORM, CorruptedWormRenderer::new);
            EntityRendererRegistry.register(ModEntities.CORRUPTED_TNT, TntEntityRenderer::new);
            EntityRendererRegistry.register(ModEntities.VIRUS_FUSE, TntEntityRenderer::new);
            return 4;
        }
    );
}
