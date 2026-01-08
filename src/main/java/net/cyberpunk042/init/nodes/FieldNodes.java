package net.cyberpunk042.init.nodes;

import net.cyberpunk042.command.field.FieldCommand;
import net.cyberpunk042.command.field.FieldTestCommand;
import net.cyberpunk042.field.FieldManager;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.growth.scheduler.GrowthScheduler;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.FieldNetworking;
import net.cyberpunk042.visual.color.ColorThemeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Field system initialization nodes.
 * 
 * <p>These nodes initialize the field system in proper order with dependencies.
 */
public final class FieldNodes {
    
    private FieldNodes() {}
    
    /**
     * Growth scheduler (needs to run before field system).
     */
    public static final InitNode GROWTH_SCHEDULER = InitNode.simple(
        "growth_scheduler", "Growth Scheduler",
        () -> {
            GrowthScheduler.registerSchedulerTasks();
            return 1;
        }
    );
    
    /**
     * Field command registration.
     */
    public static final InitNode FIELD_COMMANDS = InitNode.simple(
        "field_commands", "Field Commands",
        () -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
                FieldCommand.register(dispatcher);
                FieldTestCommand.register(dispatcher);
            });
            Logging.REGISTRY.topic("field").info("Registered /field and /fieldtest command trees");
            return 2;
        }
    );
    
    /**
     * Field definition resource reload listener.
     */
    public static final InitNode FIELD_RELOAD_LISTENER = InitNode.simple(
        "field_reload_listener", "Field Reload Listener",
        () -> {
            ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of("the-virus-block", "field_definitions");
                    }
                    
                    @Override
                    public void reload(ResourceManager manager) {
                        Logging.REGISTRY.topic("field").info("Reloading field definitions from resources...");
                        FieldRegistry.clear();
                        FieldRegistry.registerDefaults();
                        var loader = new net.cyberpunk042.field.loader.FieldLoader();
                        loader.load(manager);
                        int loaded = FieldRegistry.count();
                        Logging.REGISTRY.topic("field").info("Loaded {} field definitions from JSON", loaded);
                    }
                }
            );
            Logging.REGISTRY.topic("field").info("Registered field definition reload listener");
            return 1;
        }
    );
    
    /**
     * Field manager tick and network wiring.
     */
    public static final InitNode FIELD_MANAGER_WIRING = InitNode.simple(
        "field_manager_wiring", "Field Manager Wiring",
        () -> {
            Set<ServerWorld> wiredWorlds = Collections.newSetFromMap(new WeakHashMap<>());
            
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                String dim = world.getRegistryKey().getValue().getPath();
                net.cyberpunk042.util.SuperProfiler.start("FieldMgr:" + dim);
                FieldManager manager = FieldManager.get(world);
                if (manager != null) {
                    if (!wiredWorlds.contains(world)) {
                        manager.onSpawn(instance -> FieldNetworking.sendSpawn(world, instance));
                        manager.onRemove(id -> FieldNetworking.sendRemove(world, id));
                        manager.onUpdate(instance -> FieldNetworking.sendUpdate(world, instance));
                        wiredWorlds.add(world);
                    }
                    manager.tick();
                }
                net.cyberpunk042.util.SuperProfiler.end("FieldMgr:" + dim);
            });
            
            Logging.REGISTRY.topic("field").info("Wired FieldManager to FieldNetworking");
            return 1;
        }
    );
    
    /**
     * Field sync on player join.
     */
    public static final InitNode FIELD_PLAYER_SYNC = InitNode.simple(
        "field_player_sync", "Field Player Sync",
        () -> {
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                ServerPlayerEntity player = handler.player;
                server.execute(() -> {
                    FieldNetworking.syncDefinitionsTo(player);
                    if (player.getWorld() != null) {
                        FieldManager manager = FieldManager.get(player.getWorld());
                        if (manager != null) {
                            FieldNetworking.syncAllTo(player, manager.all());
                        }
                    }
                });
            });
            return 1;
        }
    );
    
    /**
     * Color theme registry loading.
     */
    public static final InitNode COLOR_THEMES = InitNode.simple(
        "color_themes", "Color Themes",
        () -> {
            int themeCount = ColorThemeRegistry.count();
            Logging.REGISTRY.topic("field").info("Color themes loaded: {}", themeCount);
            return themeCount;
        }
    );
    
    /**
     * Field registry defaults.
     */
    public static final InitNode FIELD_REGISTRY = InitNode.reloadable(
        "field_registry", "Field Registry",
        () -> {
            FieldRegistry.registerDefaults();
            int count = FieldRegistry.count();
            Logging.FIELD.topic("registry").info("Registry: {} fields loaded", count);
            return count;
        },
        () -> {
            FieldRegistry.reload();
            return FieldRegistry.count();
        }
    );
}
