package net.cyberpunk042.client.visual.shader;

import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side scanner for virus/infected blocks.
 * 
 * <p>Provides block positions for the smoke effect (visible to all players).
 * Runs independently of helmet telemetry, scanning nearby chunks directly.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Call periodically (e.g., every 10 ticks)
 * VirusBlockScanner.tick();
 * 
 * // Get current positions
 * List<BlockPos> blocks = VirusBlockScanner.getNearbyBlocks();
 * </pre>
 */
public final class VirusBlockScanner {
    
    // Configuration
    private static final int SCAN_RADIUS = 128;       // Blocks to scan in each direction (128 = 256 diameter)
    private static final int SCAN_HEIGHT = 32;        // Vertical range
    private static final int MAX_BLOCKS = 32;         // Maximum blocks to track (matches shader)
    private static final int SCAN_INTERVAL = 20;      // Ticks between full scans (1 second)
    
    // State
    private static final List<BlockPos> nearbyBlocks = new CopyOnWriteArrayList<>();
    private static int tickCounter = 0;
    private static BlockPos lastScanCenter = null;
    private static long lastScanTime = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tick the scanner. Call from client tick event.
     * Only performs full scan every SCAN_INTERVAL ticks.
     */
    public static void tick() {
        tickCounter++;
        
        if (tickCounter % SCAN_INTERVAL != 0) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            nearbyBlocks.clear();
            return;
        }
        
        BlockPos playerPos = client.player.getBlockPos();
        
        // Skip if player hasn't moved much since last scan
        if (lastScanCenter != null && playerPos.isWithinDistance(lastScanCenter, 8)) {
            // Check if we need a time-based refresh
            long now = System.currentTimeMillis();
            if (now - lastScanTime < 2000) {  // 2 second minimum between full scans
                return;
            }
        }
        
        scan(client.world, playerPos);
    }
    
    /**
     * Force an immediate scan (e.g., when block is placed/broken).
     */
    public static void forceScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && client.world != null) {
            scan(client.world, client.player.getBlockPos());
        }
    }
    
    /**
     * Get the current list of nearby virus blocks.
     * Returns an unmodifiable view, sorted by distance.
     */
    public static List<BlockPos> getNearbyBlocks() {
        return Collections.unmodifiableList(nearbyBlocks);
    }
    
    /**
     * Check if any virus blocks are nearby.
     */
    public static boolean hasNearbyBlocks() {
        return !nearbyBlocks.isEmpty();
    }
    
    /**
     * Get distance to closest virus block, or Float.MAX_VALUE if none.
     */
    public static float getClosestDistance() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || nearbyBlocks.isEmpty()) {
            return Float.MAX_VALUE;
        }
        
        Vec3d playerPos = client.player.getPos();
        float closest = Float.MAX_VALUE;
        
        for (BlockPos pos : nearbyBlocks) {
            float dist = (float) playerPos.distanceTo(Vec3d.ofCenter(pos));
            if (dist < closest) {
                closest = dist;
            }
        }
        
        return closest;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL SCANNING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void scan(ClientWorld world, BlockPos center) {
        lastScanCenter = center;
        lastScanTime = System.currentTimeMillis();
        
        List<BlockPos> found = new ArrayList<>();
        Vec3d centerVec = Vec3d.ofCenter(center);
        
        // Scan a cubic region around the player
        int minX = center.getX() - SCAN_RADIUS;
        int maxX = center.getX() + SCAN_RADIUS;
        int minY = Math.max(world.getBottomY(), center.getY() - SCAN_HEIGHT);
        int maxY = Math.min(world.getTopYInclusive(), center.getY() + SCAN_HEIGHT);
        int minZ = center.getZ() - SCAN_RADIUS;
        int maxZ = center.getZ() + SCAN_RADIUS;
        
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);
                    BlockState state = world.getBlockState(mutable);
                    
                    if (isVirusBlock(state)) {
                        found.add(mutable.toImmutable());
                        
                        // Early exit if we hit max
                        if (found.size() >= MAX_BLOCKS * 2) {
                            break;
                        }
                    }
                }
                if (found.size() >= MAX_BLOCKS * 2) break;
            }
            if (found.size() >= MAX_BLOCKS * 2) break;
        }
        
        // Sort by distance and trim to MAX_BLOCKS
        found.sort((a, b) -> {
            double distA = centerVec.squaredDistanceTo(Vec3d.ofCenter(a));
            double distB = centerVec.squaredDistanceTo(Vec3d.ofCenter(b));
            return Double.compare(distA, distB);
        });
        
        if (found.size() > MAX_BLOCKS) {
            found = found.subList(0, MAX_BLOCKS);
        }
        
        // Update the list atomically
        nearbyBlocks.clear();
        nearbyBlocks.addAll(found);
    }
    
    /**
     * Check if a block state is a virus/infected block.
     */
    private static boolean isVirusBlock(BlockState state) {
        return state.isOf(ModBlocks.INFECTED_BLOCK);
    }
    
    /**
     * Clear all cached data (e.g., on disconnect).
     */
    public static void clear() {
        nearbyBlocks.clear();
        lastScanCenter = null;
        tickCounter = 0;
    }
    
    private VirusBlockScanner() {}
}
