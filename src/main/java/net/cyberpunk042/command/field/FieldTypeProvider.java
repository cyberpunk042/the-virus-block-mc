package net.cyberpunk042.command.field;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Provider interface for field type-specific command handling.
 * 
 * <p>Each field type (SHIELD, PERSONAL, etc.) can have a provider that handles
 * type-specific subcommands and operations.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Register a provider
 * FieldTypeProviders.register(FieldType.SHIELD, new ShieldTypeProvider());
 * 
 * // Build subcommand tree
 * LiteralArgumentBuilder cmd = provider.buildSubcommand();
 * </pre>
 * 
 * @see FieldCommand
 * @see FieldType
 */
public interface FieldTypeProvider {
    
    /**
     * Returns the field type this provider handles.
     */
    FieldType getType();
    
    /**
     * Builds the subcommand tree for this field type.
     * 
     * <p>The returned command builder will be registered under /field/{type}/...
     * 
     * @return command builder with type-specific subcommands
     */
    LiteralArgumentBuilder<ServerCommandSource> buildSubcommand();
    
    /**
     * Returns all available definitions for this field type.
     */
    List<FieldDefinition> getDefinitions();
    
    /**
     * Gets a specific definition by ID.
     * 
     * @param id the definition identifier
     * @return the definition, or empty if not found
     */
    Optional<FieldDefinition> getDefinition(Identifier id);
    
    /**
     * Spawns a field for a player.
     * 
     * @param player     the player to spawn the field for
     * @param definition the field definition to use
     * @return true if spawn was successful
     */
    boolean spawnForPlayer(ServerPlayerEntity player, FieldDefinition definition);
    
    /**
     * Removes all fields of this type for a player.
     * 
     * @param player the player to remove fields from
     * @return number of fields removed
     */
    int removeForPlayer(ServerPlayerEntity player);
    
    /**
     * Called when definitions are reloaded.
     * Providers can clear caches here.
     */
    default void onReload() {
        // Default no-op
    }
    
    /**
     * Returns a display name for this type (for UI/feedback).
     */
    default String getDisplayName() {
        return getType().id().substring(0, 1).toUpperCase() + getType().id().substring(1);
    }
    
    /**
     * Whether this type supports the spawn command.
     */
    default boolean supportsSpawn() {
        return true;
    }
    
    /**
     * Whether this type supports the remove command.
     */
    default boolean supportsRemove() {
        return true;
    }
}

