package net.cyberpunk042.util;

import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive performance profiler with clean ASCII output.
 * 
 * Features:
 * - Total tick time vs mod code time analysis
 * - Mixin profiling support
 * - Network packet tracking
 * - Per-handler breakdown
 * - Entity diagnostics (breakdown by type)
 * - Spike detection
 */
public final class SuperProfiler {
    
    // ============================================================================
    // TIMING DATA
    // ============================================================================
    
    private static final Map<String, AtomicLong> TOTAL_TIME = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> CALL_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, Long> MAX_SINGLE_CALL = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<String, Long>> START_TIMES = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // Network tracking
    private static final Map<String, AtomicLong> PACKET_COUNT = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> PACKET_BYTES = new ConcurrentHashMap<>();
    
    // Per-tick tracking
    private static long tickStartTime = 0;
    private static long totalTickTimeNs = 0;
    private static long modCodeTimeNs = 0;
    private static int tickCount = 0;
    private static int spikeCount = 0;
    private static long worstTickMs = 0;
    
    // Memory tracking
    private static long memoryAtStart = 0;
    private static long peakMemoryDelta = 0;
    
    // Config
    private static final long SPIKE_THRESHOLD_MS = 100;
    private static final long REPORT_INTERVAL_MS = 5000;
    private static long lastReportTime = 0;
    
    private static volatile boolean enabled = true;
    
    // Zombie entity cleanup - removes falling blocks that never tick
    private static volatile boolean zombieCleanupEnabled = true;
    private static int zombiesRemovedThisReport = 0;
    private static int totalZombiesRemoved = 0;
    
    private SuperProfiler() {}
    
    // ============================================================================
    // INITIALIZATION
    // ============================================================================
    
    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(SuperProfiler::onTickStart);
        ServerTickEvents.END_SERVER_TICK.register(SuperProfiler::onTickEnd);
        Logging.PROFILER.info("[SuperProfiler] Performance monitoring enabled");
    }
    
    // ============================================================================
    // TICK LIFECYCLE
    // ============================================================================
    
    private static void onTickStart(MinecraftServer server) {
        if (!enabled) return;
        tickStartTime = System.nanoTime();
        memoryAtStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
    
    private static void onTickEnd(MinecraftServer server) {
        if (!enabled) return;
        long tickTimeNs = System.nanoTime() - tickStartTime;
        long tickTimeMs = tickTimeNs / 1_000_000;
        
        totalTickTimeNs += tickTimeNs;
        tickCount++;
        
        // Memory tracking
        long memoryNow = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryDelta = memoryNow - memoryAtStart;
        if (memoryDelta > peakMemoryDelta) {
            peakMemoryDelta = memoryDelta;
        }
        
        // Spike detection
        if (tickTimeMs > SPIKE_THRESHOLD_MS) {
            spikeCount++;
            if (tickTimeMs > worstTickMs) {
                worstTickMs = tickTimeMs;
            }
            long modMs = modCodeTimeNs / 1_000_000;
            Logging.PROFILER.warn("[SPIKE] {}ms tick | Mod: {}ms | Vanilla: {}ms",
                tickTimeMs, modMs, tickTimeMs - modMs);
        }
        
        // Periodic report
        long now = System.currentTimeMillis();
        if (now - lastReportTime >= REPORT_INTERVAL_MS) {
            generateReport(server);
            lastReportTime = now;
        }
        
        modCodeTimeNs = 0;
    }
    
    // ============================================================================
    // PROFILING API
    // ============================================================================
    
    public static void start(String label) {
        if (!enabled) return;
        START_TIMES.get().put(label, System.nanoTime());
    }
    
    public static void end(String label) {
        if (!enabled) return;
        Long startTime = START_TIMES.get().remove(label);
        if (startTime == null) return;
        
        long elapsed = System.nanoTime() - startTime;
        TOTAL_TIME.computeIfAbsent(label, k -> new AtomicLong()).addAndGet(elapsed);
        CALL_COUNT.computeIfAbsent(label, k -> new AtomicLong()).incrementAndGet();
        modCodeTimeNs += elapsed;
        
        long elapsedMs = elapsed / 1_000_000;
        MAX_SINGLE_CALL.compute(label, (k, v) -> v == null ? elapsedMs : Math.max(v, elapsedMs));
    }
    
    // ============================================================================
    // NETWORK TRACKING
    // ============================================================================
    
    public static void trackPacket(String packetName, int bytes) {
        if (!enabled) return;
        PACKET_COUNT.computeIfAbsent(packetName, k -> new AtomicLong()).incrementAndGet();
        PACKET_BYTES.computeIfAbsent(packetName, k -> new AtomicLong()).addAndGet(bytes);
    }
    
    // ============================================================================
    // REPORT GENERATION
    // ============================================================================
    
    private static void generateReport(MinecraftServer server) {
        if (tickCount == 0) return;
        
        long avgTickMs = totalTickTimeNs / tickCount / 1_000_000;
        long modTotalMs = TOTAL_TIME.values().stream().mapToLong(AtomicLong::get).sum() / 1_000_000;
        long unaccountedMs = (totalTickTimeNs / 1_000_000) - modTotalMs;
        double tps = tickCount / (REPORT_INTERVAL_MS / 1000.0);
        double modPercent = totalTickTimeNs > 0 ? (modTotalMs * 100.0 / (totalTickTimeNs / 1_000_000)) : 0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("+==========================================================================+\n");
        sb.append("|                    SUPER PROFILER - PERFORMANCE REPORT                  |\n");
        sb.append("+==========================================================================+\n");
        sb.append(String.format("| SUMMARY                                                                  |\n"));
        sb.append(String.format("|   Ticks: %-4d  |  Avg: %-4dms  |  TPS: %-5.1f  |  Spikes: %-3d           |\n",
            tickCount, avgTickMs, tps, spikeCount));
        sb.append(String.format("|   Worst: %-4dms  |  Peak Mem: %-5.1fMB                                  |\n",
            worstTickMs, peakMemoryDelta / 1024.0 / 1024.0));
        sb.append("+--------------------------------------------------------------------------+\n");
        sb.append(String.format("| TIME BREAKDOWN                                                           |\n"));
        sb.append(String.format("|   Mod Code:    %5dms (%5.1f%%)                                         |\n", modTotalMs, modPercent));
        sb.append(String.format("|   Unaccounted: %5dms (%5.1f%%) <- Vanilla/Other mods/Mixins            |\n", unaccountedMs, 100.0 - modPercent));
        
        // Handler breakdown
        sb.append("+--------------------------------------------------------------------------+\n");
        sb.append("| TOP HANDLERS (sorted by total time)                                      |\n");
        sb.append("+--------------------------------------------------------------------------+\n");
        
        List<Map.Entry<String, AtomicLong>> sorted = new ArrayList<>(TOTAL_TIME.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()));
        
        int shown = 0;
        for (Map.Entry<String, AtomicLong> entry : sorted) {
            if (shown >= 12) break;
            String label = entry.getKey();
            long totalNs = entry.getValue().get();
            long count = CALL_COUNT.getOrDefault(label, new AtomicLong(1)).get();
            long maxMs = MAX_SINGLE_CALL.getOrDefault(label, 0L);
            long totalMs = totalNs / 1_000_000;
            long avgUs = count > 0 ? (totalNs / count / 1_000) : 0;
            
            if (totalMs > 0 || shown < 8) {
                String shortLabel = label.length() > 22 ? label.substring(0, 19) + "..." : label;
                sb.append(String.format("|   %-22s %4dms  %5d calls  %5dus avg  %3dms max |\n",
                    shortLabel, totalMs, count, avgUs, maxMs));
                shown++;
            }
        }
        
        if (sorted.isEmpty()) {
            sb.append("|   (no profiled code ran)                                                 |\n");
        }
        
        // Network stats (simple tracking from SuperProfiler)
        if (!PACKET_COUNT.isEmpty()) {
            sb.append("+--------------------------------------------------------------------------+\n");
            sb.append("| SIMPLE PACKET COUNTS                                                     |\n");
            sb.append("+--------------------------------------------------------------------------+\n");
            
            List<Map.Entry<String, AtomicLong>> packetsSorted = new ArrayList<>(PACKET_COUNT.entrySet());
            packetsSorted.sort((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()));
            
            int packetShown = 0;
            for (Map.Entry<String, AtomicLong> entry : packetsSorted) {
                if (packetShown >= 5) break;
                String name = entry.getKey();
                long count = entry.getValue().get();
                long bytes = PACKET_BYTES.getOrDefault(name, new AtomicLong(0)).get();
                String shortName = name.length() > 30 ? name.substring(0, 27) + "..." : name;
                sb.append(String.format("|   %-30s  %5d packets  %7.1fKB           |\n",
                    shortName, count, bytes / 1024.0));
                packetShown++;
            }
        }
        
        // Detailed network stats from NetworkProfiler
        sb.append(NetworkProfiler.generateReport());
        
        // World diagnostics
        sb.append("+--------------------------------------------------------------------------+\n");
        sb.append("| WORLD DIAGNOSTICS                                                        |\n");
        sb.append("+--------------------------------------------------------------------------+\n");
        
        for (ServerWorld world : server.getWorlds()) {
            String dimName = world.getRegistryKey().getValue().getPath();
            if (dimName.length() > 12) dimName = dimName.substring(0, 9) + "...";
            
            // Accurate entity counting with breakdown
            int players = 0, mobs = 0, items = 0, projectiles = 0, other = 0;
            int removed = 0, alive = 0;  // Track entity state
            Map<String, Integer> typeCounts = new HashMap<>();
            // Falling block diagnostic
            int fallingBelowVoid = 0;
            int fallingAboveWorld = 0;
            int fallingNormal = 0;
            int fallingAgeOver600 = 0;
            int fallingUnloadedChunk = 0;
            int fallingStuck = 0;         // Velocity near zero but not landed
            int fallingCorruptedStone = 0; // Carrying corrupted stone specifically
            
            // Entity lifecycle diagnostic counters (for all types)
            int entitiesInUnloadedChunks = 0;
            int itemsOldAge = 0;         // Items older than 6000 ticks (default despawn)
            int mobsNoDespawn = 0;       // Mobs that can't despawn (persistent)
            int mobsOldAge = 0;          // Mobs older than 600 ticks but still alive
            int wormsCount = 0;
            int wormsOldAge = 0;         // Worms older than 600 ticks
            
            // UUID uniqueness check - detect true entity duplication
            java.util.Set<java.util.UUID> seenUuids = new java.util.HashSet<>();
            int duplicateUuids = 0;
            
            try {
                for (Entity entity : world.iterateEntities()) {
                    // Track if entity is actually "alive" vs removed/pending
                    if (entity.isRemoved()) {
                        removed++;
                        continue;  // Skip removed entities from main count
                    }
                    alive++;
                    
                    // Check for duplicate UUIDs (true entity duplication bug)
                    if (!seenUuids.add(entity.getUuid())) {
                        duplicateUuids++;
                    }
                    
                    if (entity instanceof PlayerEntity) players++;
                    else if (entity instanceof MobEntity) mobs++;
                    else if (entity instanceof ItemEntity) items++;
                    else if (entity instanceof ProjectileEntity) projectiles++;
                    else other++;
                    
                    String type = entity.getType().getTranslationKey()
                        .replace("entity.minecraft.", "")
                        .replace("entity.the-virus-block.", "virus:");
                    typeCounts.merge(type, 1, Integer::sum);
                    
                    // CHECK: Is this entity in a loaded chunk?
                    net.minecraft.util.math.ChunkPos entityChunk = new net.minecraft.util.math.ChunkPos(entity.getBlockPos());
                    boolean isChunkLoaded = world.isChunkLoaded(entityChunk.toLong());
                    // Check if chunk has a world chunk (fully loaded, entities can tick)
                    boolean hasWorldChunk = world.getChunkManager() instanceof net.minecraft.server.world.ServerChunkManager scm
                        && scm.getWorldChunk(entityChunk.x, entityChunk.z) != null;
                    if (!isChunkLoaded || !hasWorldChunk) {
                        entitiesInUnloadedChunks++;
                    }
                    
                    // Track ItemEntity age issues
                    if (entity instanceof net.minecraft.entity.ItemEntity item) {
                        int itemAge = item.getItemAge();
                        if (itemAge > 6000) { // Default despawn time
                            itemsOldAge++;
                            // Sample first 3 old items for detailed analysis
                            if (itemsOldAge <= 3) {
                                net.minecraft.util.math.BlockPos pos = item.getBlockPos();
                                net.cyberpunk042.log.Logging.PROFILER.warn(
                                    "[ItemDiag] age={} pos=({},{},{}) stack={} inLoadedChunk={}",
                                    itemAge, pos.getX(), pos.getY(), pos.getZ(),
                                    item.getStack().getItem().getTranslationKey(),
                                    world.isChunkLoaded(entityChunk.toLong())
                                );
                            }
                        }
                    }
                    
                    // Track MobEntity despawn issues
                    if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
                        if (mob.isPersistent() || mob.cannotDespawn()) {
                            mobsNoDespawn++;
                        }
                        if (mob.age > 600) {
                            mobsOldAge++;
                        }
                    }
                    
                    // Track CorruptedWormEntity specifically
                    if (entity instanceof net.cyberpunk042.entity.CorruptedWormEntity worm) {
                        wormsCount++;
                        if (worm.age > 600) {
                            wormsOldAge++;
                        }
                    }
                    
                    // Special tracking for falling blocks - deep diagnostic
                    if (entity instanceof net.minecraft.entity.FallingBlockEntity fb) {
                        double y = fb.getY();
                        int age = fb.age;
                        
                        // Access timeFalling via accessor
                        int timeFalling = ((net.cyberpunk042.mixin.FallingBlockEntityAccessor) fb).virus$getTimeFalling();
                        
                        // ZOMBIE CLEANUP: Detect and remove entities that never ticked
                        // A zombie entity has age=0 AND timeFalling=0, meaning tick() was never called
                        if (age == 0 && timeFalling == 0) {
                            net.minecraft.util.math.Vec3d vel = fb.getVelocity();
                            boolean velocityZero = vel.lengthSquared() < 0.0001;
                            

                            
                            if (zombieCleanupEnabled && velocityZero) {
                                fb.discard();
                                zombiesRemovedThisReport++;
                                totalZombiesRemoved++;
                                continue; // Don't count this entity in stats since we removed it
                            }
                        }
                        
                        if (y < world.getBottomY() - 64) {
                            fallingBelowVoid++;
                        } else if (y > world.getTopYInclusive() + 64) {
                            fallingAboveWorld++;
                        } else {
                            fallingNormal++;
                        }
                        if (age > 600) {
                            fallingAgeOver600++;
                        }
                        // Check removal state
                        if (fb.isRemoved()) {
                            fallingUnloadedChunk++;
                        }
                        
                        // Count stuck entities (velocity near zero but still in world)
                        net.minecraft.util.math.Vec3d vel = fb.getVelocity();
                        if (vel.lengthSquared() < 0.0001) { // Velocity essentially zero
                            fallingStuck++;
                        }
                        
                        // Count corrupted stone carriers
                        net.minecraft.block.BlockState carried = fb.getBlockState();
                        if (carried.getBlock() instanceof net.cyberpunk042.block.corrupted.CorruptedStoneBlock) {
                            fallingCorruptedStone++;
                        }
                        

                    }
                }
            } catch (Exception e) { /* ignore */ }
            
            int totalEntities = players + mobs + items + projectiles + other;
            int loadedChunks = world.getChunkManager().getLoadedChunkCount();
            
            // Virus sources
            int virusSources = 0;
            boolean infected = false;
            try {
                var state = net.cyberpunk042.infection.VirusWorldState.get(world);
                virusSources = state.hasVirusSources() ? state.getVirusSources().size() : 0;
                infected = state.infectionState().infected();
            } catch (Exception e) { /* ignore */ }
            
            sb.append(String.format("|   %-12s Chunks:%-4d Ent:%-4d (P:%-2d M:%-3d I:%-3d Proj:%-2d Oth:%-3d)|\n",
                dimName, loadedChunks, totalEntities, players, mobs, items, projectiles, other));
            if (removed > 0) {
                sb.append(String.format("|               *** GHOST ENTITIES: %d removed but still in iterator! ***|\n", removed));
            }
            if (duplicateUuids > 0) {
                sb.append(String.format("|               *** DUPLICATE UUIDs: %d entities have same UUID! ***|\n", duplicateUuids));
            }
            if (entitiesInUnloadedChunks > 0) {
                sb.append(String.format("|               *** IN NON-TICKING CHUNKS: %d ***|\n", entitiesInUnloadedChunks));
            }
            sb.append(String.format("|               Infected: %-5s  Sources: %-3d                            |\n",
                infected, virusSources));
            
            // Falling block diagnostic output
            int totalFalling = fallingBelowVoid + fallingAboveWorld + fallingNormal;
            if (totalFalling > 0) {
                sb.append(String.format("|               FALLING: %d (void:%d above:%d normal:%d age>600:%d)|\n",
                    totalFalling, fallingBelowVoid, fallingAboveWorld, fallingNormal, fallingAgeOver600));
                sb.append(String.format("|                 stuck:%d corrupted_stone:%d removed:%d           |\n",
                    fallingStuck, fallingCorruptedStone, fallingUnloadedChunk));
            }
            
            // ENTITY LIFECYCLE DIAGNOSTICS - find the systemic issue
            if (itemsOldAge > 0 || mobsOldAge > 0 || wormsCount > 0) {
                sb.append("|             --- ENTITY LIFECYCLE DIAGNOSTIC ---                       |\n");
                if (itemsOldAge > 0) {
                    sb.append(String.format("|               Items age>6000 (should despawn): %d                  |\n", 
                        itemsOldAge));
                }
                if (mobsOldAge > 0 || mobsNoDespawn > 0) {
                    sb.append(String.format("|               Mobs age>600: %d  persistent/noDespawn: %d           |\n", 
                        mobsOldAge, mobsNoDespawn));
                }
                if (wormsCount > 0) {
                    sb.append(String.format("|               Worms: %d (age>600: %d)                              |\n", 
                        wormsCount, wormsOldAge));
                }
            }
            
            // Top entity types if many entities (helps identify what these entities are)
            if (totalEntities > 10 && !typeCounts.isEmpty()) {
                sb.append("|               Top types:                                               |\n");
                List<Map.Entry<String, Integer>> topTypes = typeCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(5)
                    .toList();
                for (var entry : topTypes) {
                    String typeName = entry.getKey().length() > 20 
                        ? entry.getKey().substring(0, 17) + "..." : entry.getKey();
                    sb.append(String.format("|                 -> %-20s: %4d                          |\n",
                        typeName, entry.getValue()));
                }
            }
        }
        
        sb.append("+==========================================================================+");
        
        Logging.PROFILER.warn(sb.toString());
        
        reset();
    }
    
    private static void reset() {
        TOTAL_TIME.clear();
        CALL_COUNT.clear();
        MAX_SINGLE_CALL.clear();
        PACKET_COUNT.clear();
        PACKET_BYTES.clear();
        NetworkProfiler.reset();
        totalTickTimeNs = 0;
        modCodeTimeNs = 0;
        tickCount = 0;
        spikeCount = 0;
        worstTickMs = 0;
        peakMemoryDelta = 0;
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) reset();
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
}
