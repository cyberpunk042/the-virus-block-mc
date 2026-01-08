package net.cyberpunk042.init.nodes;

import net.cyberpunk042.block.virus.VirusBlockProtection;
import net.cyberpunk042.command.VirusCommand;
import net.cyberpunk042.infection.GlobalTerrainCorruption;
import net.cyberpunk042.infection.VirusInventoryAnnouncements;
import net.cyberpunk042.infection.VirusItemAlerts;
import net.cyberpunk042.infection.VirusTierBossBar;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.util.DelayedServerTasks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;

/**
 * Infection system initialization nodes.
 * 
 * <p>These nodes initialize the virus infection system components in proper order.
 */
public final class InfectionNodes {
    
    private InfectionNodes() {}
    
    /**
     * Virus block protection (anti-grief for virus blocks).
     */
    public static final InitNode BLOCK_PROTECTION = InitNode.simple(
        "block_protection", "Block Protection",
        () -> {
            VirusBlockProtection.init();
            return 1;
        }
    );
    
    /**
     * Virus tier boss bar display.
     */
    public static final InitNode TIER_BOSS_BAR = InitNode.simple(
        "tier_boss_bar", "Tier Boss Bar",
        () -> {
            VirusTierBossBar.init();
            return 1;
        }
    );
    
    /**
     * Inventory announcements for infection events.
     */
    public static final InitNode INVENTORY_ANNOUNCEMENTS = InitNode.simple(
        "inventory_announcements", "Inventory Announcements",
        () -> {
            VirusInventoryAnnouncements.init();
            return 1;
        }
    );
    
    /**
     * Global terrain corruption system.
     */
    public static final InitNode TERRAIN_CORRUPTION = InitNode.simple(
        "terrain_corruption", "Terrain Corruption",
        () -> {
            GlobalTerrainCorruption.init();
            return 1;
        }
    );
    
    /**
     * Virus item alerts.
     */
    public static final InitNode ITEM_ALERTS = InitNode.simple(
        "item_alerts", "Item Alerts",
        () -> {
            VirusItemAlerts.init();
            return 1;
        }
    );
    
    /**
     * Delayed server tasks utility.
     */
    public static final InitNode DELAYED_TASKS = InitNode.simple(
        "delayed_tasks", "Delayed Tasks",
        () -> {
            DelayedServerTasks.init();
            return 1;
        }
    );
    
    /**
     * Virus command registration.
     */
    public static final InitNode VIRUS_COMMANDS = InitNode.simple(
        "virus_commands", "Virus Commands",
        () -> {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> 
                VirusCommand.register(dispatcher));
            return 1;
        }
    );
    
    /**
     * Virus world state and tick handler.
     */
    public static final InitNode VIRUS_WORLD_STATE = InitNode.simple(
        "virus_world_state", "Virus World State",
        () -> {
            ServerWorldEvents.LOAD.register((server, world) -> {
                long start = System.nanoTime();
                VirusWorldState state = VirusWorldState.get(world);
                long getMs = (System.nanoTime() - start) / 1_000_000;
                
                long onLoadStart = System.nanoTime();
                state.onWorldLoad(world);
                long onLoadMs = (System.nanoTime() - onLoadStart) / 1_000_000;
                
                long totalMs = (System.nanoTime() - start) / 1_000_000;
                if (totalMs > 50) {
                    net.cyberpunk042.log.Logging.PROFILER.warn(
                        "[WorldLoad] SLOW: {}ms total | get={}ms, onLoad={}ms | dim={}",
                        totalMs, getMs, onLoadMs, world.getRegistryKey().getValue());
                }
            });
            
            ServerTickEvents.END_WORLD_TICK.register(world -> {
                String dim = world.getRegistryKey().getValue().getPath();
                
                net.cyberpunk042.util.SuperProfiler.start("VWS.get:" + dim);
                VirusWorldState state = VirusWorldState.get(world);
                net.cyberpunk042.util.SuperProfiler.end("VWS.get:" + dim);
                
                net.cyberpunk042.util.SuperProfiler.start("VWS.tick:" + dim);
                state.tick(world);
                net.cyberpunk042.util.SuperProfiler.end("VWS.tick:" + dim);
                
                net.cyberpunk042.util.SuperProfiler.start("BossBar:" + dim);
                VirusTierBossBar.update(world, state);
                net.cyberpunk042.util.SuperProfiler.end("BossBar:" + dim);
                
                net.cyberpunk042.util.SuperProfiler.start("Announce:" + dim);
                VirusInventoryAnnouncements.tick(world);
                net.cyberpunk042.util.SuperProfiler.end("Announce:" + dim);
            });
            
            return 1;
        }
    ).dependsOn("tier_boss_bar").dependsOn("inventory_announcements");
}
