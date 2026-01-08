package net.cyberpunk042.client.init.nodes;

import net.cyberpunk042.config.InfectionConfigRegistry;
import net.cyberpunk042.config.ModConfigBootstrap;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.registry.ModBlockEntities;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.client.render.BlockRenderLayer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

/**
 * Core client initialization nodes.
 */
public final class ClientCoreNodes {
    
    private ClientCoreNodes() {}
    
    /**
     * Client configuration loading.
     */
    public static final InitNode CONFIG = InitNode.simple(
        "client_config", "Client Configuration",
        () -> {
            ModConfigBootstrap.prepareClient();
            InfectionConfigRegistry.loadClient();
            // Initialize client-side profiler
            net.cyberpunk042.client.util.ClientProfiler.init();
            return 2;
        }
    );
    
    /**
     * Block render layers (translucent blocks, etc.).
     */
    public static final InitNode RENDER_LAYERS = InitNode.simple(
        "render_layers", "Render Layers",
        () -> {
            BlockRenderLayerMap.putBlocks(BlockRenderLayer.TRANSLUCENT,
                ModBlocks.CORRUPTED_GLASS,
                ModBlocks.CORRUPTED_ICE,
                ModBlocks.CORRUPTED_PACKED_ICE,
                ModBlocks.PROGRESSIVE_GROWTH_BLOCK);
            return 4;
        }
    );
    
    /**
     * Block entity renderers.
     */
    public static final InitNode BLOCK_ENTITY_RENDERERS = InitNode.simple(
        "block_entity_renderers", "Block Entity Renderers",
        () -> {
            BlockEntityRendererFactories.register(ModBlockEntities.SINGULARITY_BLOCK, 
                net.cyberpunk042.client.render.blockentity.SingularityBlockEntityRenderer::new);
            BlockEntityRendererFactories.register(ModBlockEntities.PROGRESSIVE_GROWTH, 
                net.cyberpunk042.client.render.blockentity.ProgressiveGrowthBlockEntityRenderer::new);
            net.cyberpunk042.client.render.item.ProgressiveGrowthItemRenderer.bootstrap();
            return 3;
        }
    );
}
