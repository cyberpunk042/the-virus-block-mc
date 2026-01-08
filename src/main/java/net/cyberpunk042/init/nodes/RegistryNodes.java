package net.cyberpunk042.init.nodes;

import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.registry.ModBlockEntities;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModEntities;
import net.cyberpunk042.registry.ModItemGroups;
import net.cyberpunk042.registry.ModItems;
import net.cyberpunk042.registry.ModStatusEffects;
import net.cyberpunk042.screen.ModScreenHandlers;

/**
 * Minecraft registry bootstrap nodes.
 * 
 * <p>These register blocks, items, entities, etc. with Minecraft's registries.
 */
public final class RegistryNodes {
    
    private RegistryNodes() {}
    
    /**
     * Block registry.
     */
    public static final InitNode BLOCKS = InitNode.simple(
        "blocks", "Blocks",
        () -> {
            ModBlocks.bootstrap();
            return 1; // Could count actual blocks registered
        }
    );
    
    /**
     * Item registry.
     */
    public static final InitNode ITEMS = InitNode.simple(
        "items", "Items",
        () -> {
            ModItems.bootstrap();
            return 1;
        }
    ).dependsOn("blocks"); // Items often reference blocks
    
    /**
     * Block entity registry.
     */
    public static final InitNode BLOCK_ENTITIES = InitNode.simple(
        "block_entities", "Block Entities",
        () -> {
            ModBlockEntities.bootstrap();
            return 1;
        }
    ).dependsOn("blocks");
    
    /**
     * Entity registry.
     */
    public static final InitNode ENTITIES = InitNode.simple(
        "entities", "Entities",
        () -> {
            ModEntities.bootstrap();
            return 1;
        }
    );
    
    /**
     * Status effect registry.
     */
    public static final InitNode EFFECTS = InitNode.simple(
        "effects", "Status Effects",
        () -> {
            ModStatusEffects.bootstrap();
            return 1;
        }
    );
    
    /**
     * Item group (creative tab) registry.
     */
    public static final InitNode ITEM_GROUPS = InitNode.simple(
        "item_groups", "Item Groups",
        () -> {
            ModItemGroups.bootstrap();
            return 1;
        }
    ).dependsOn("items");
    
    /**
     * Screen handler registry.
     */
    public static final InitNode SCREEN_HANDLERS = InitNode.simple(
        "screen_handlers", "Screen Handlers",
        () -> {
            ModScreenHandlers.bootstrap();
            return 1;
        }
    );
}
