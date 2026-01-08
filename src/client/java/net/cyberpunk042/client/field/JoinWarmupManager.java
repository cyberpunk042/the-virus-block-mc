package net.cyberpunk042.client.field;

import net.cyberpunk042.client.visual.mesh.SphereTessellator;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.QuadPattern;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.cyberpunk042.visual.shape.SphereShape;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smooth loading bar on server join.
 * 
 * Progress is TICK-BASED for smoothness:
 * - 0-10% (0-20+ ticks): Anti-Virus Scan (adaptive based on FPS)
 * - 10-25% (next 30 ticks): Loading Effects (tessellation)
 * - 25-40% (next 25 ticks): Loading Profiles
 * - 40-50% (next 25 ticks): Compiling Shaders (GPU warmup)
 * - 50-100% (waits): Chunk loading (per-chunk progress)
 */
public final class JoinWarmupManager {
    
    // Timing (base values - Anti-Virus phase adapts based on frame time)
    private static final int ANTIVIRUS_TICKS_BASE = 40;  // 2.0 sec base for anti-virus scan (0-10%)
    private static final int ANTIVIRUS_TICKS_MAX = 60;   // 3.0 sec max if lag detected
    private static final int EFFECTS_TICKS = 30;         // 1.5 sec for effects (10-25%)
    private static final int PROFILES_TICKS = 25;        // 1.25 sec for profiles (25-40%)
    private static final int SHADERS_TICKS = 25;         // 1.25 sec for shaders (40-50%)
    private static final int MAX_CHUNK_WAIT = 600;       // 30 sec max wait for chunks
    
    // Chunk loading - adapts to render distance at warmup start
    private static volatile int chunkRadius = 8;         // Default, updated on join
    private static volatile int totalChunks = 289;       // (8*2+1)^2 = 289 default
    
    // Adaptive timing - extends Anti-Virus phase if user experienced lag
    private static volatile int antivirusTicks = ANTIVIRUS_TICKS_BASE;
    private static volatile long firstFrameNanos = 0;
    private static volatile boolean frameTimeCaptured = false;
    
    // State
    private static final AtomicBoolean warmupComplete = new AtomicBoolean(true);
    private static final AtomicInteger tickCount = new AtomicInteger(0);
    
    private static volatile boolean effectsStarted = false;
    private static volatile boolean effectsDone = false;
    private static volatile boolean profilesDone = false;
    private static volatile boolean shadersDone = false;
    private static volatile int chunksLoaded = 0;
    private static volatile int chunkWaitTicks = 0;
    
    // Stall detection - complete early if chunks stop arriving
    private static volatile int lastChunkCount = 0;
    private static volatile int stallTicks = 0;
    private static final int STALL_THRESHOLD = 40;  // 2 seconds of no progress = done
    
    private static CompletableFuture<Void> asyncTask = null;
    
    private JoinWarmupManager() {}
    
    private static volatile boolean hookRegistered = false;
    
    public static void init() {
        if (hookRegistered) return;
        hookRegistered = true;
        
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Logging.RENDER.topic("warmup").info("Player joined - starting smooth warmup");
            startWarmup();
        });
        Logging.RENDER.topic("warmup").info("JoinWarmupManager initialized");
    }
    
    private static void startWarmup() {
        // Reset all state
        warmupComplete.set(false);
        tickCount.set(0);
        effectsStarted = false;
        effectsDone = false;
        profilesDone = false;
        shadersDone = false;
        chunksLoaded = 0;
        chunkWaitTicks = 0;
        lastChunkCount = 0;
        stallTicks = 0;
        
        // Reset adaptive Anti-Virus timing
        antivirusTicks = ANTIVIRUS_TICKS_BASE;
        firstFrameNanos = System.nanoTime();
        frameTimeCaptured = false;
        
        // Adapt to actual render distance (chunks to wait for)
        var client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            int renderDistance = client.options.getViewDistance().getValue();
            chunkRadius = Math.min(renderDistance, 16);  // Cap at 16, stall detection handles server limits
            totalChunks = (chunkRadius * 2 + 1) * (chunkRadius * 2 + 1);
            Logging.RENDER.topic("warmup").info("Chunk target: {} chunks (render distance: {})", totalChunks, renderDistance);
        }
        
        // Start async warmup (runs in background while bar animates)
        asyncTask = CompletableFuture.runAsync(() -> {
            try {
                // Warmup effects (may complete faster than the animation)
                warmupSphere(15.0f, 24, 32);
                warmupSphere(8.0f, 16, 24);
                warmupSphere(3.0f, 10, 14);
                warmupPatterns();
                effectsDone = true;
                Logging.RENDER.topic("warmup").debug("Effects tessellation done");
                
                // Load profiles
                net.cyberpunk042.client.profile.ProfileManager.getInstance().loadAll();
                profilesDone = true;
                Logging.RENDER.topic("warmup").debug("Profiles loaded");
                
                // Compile shaders (must run on render thread for OpenGL context)
                net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                    net.cyberpunk042.client.visual.shader.util.ShaderWarmupService.warmup();
                    shadersDone = true;
                    Logging.RENDER.topic("warmup").debug("Shaders compiled");
                });
                
            } catch (Exception e) {
                Logging.RENDER.topic("warmup").error("Warmup error: {}", e.getMessage());
                effectsDone = true;
                profilesDone = true;
                shadersDone = true;
            }
        });
        
        effectsStarted = true;
    }
    
    private static void warmupSphere(float radius, int lat, int lon) {
        SphereShape shape = SphereShape.builder()
            .radius(radius)
            .latSteps(lat)
            .lonSteps(lon)
            .algorithm(SphereAlgorithm.LAT_LON)
            .build();
        SphereTessellator.tessellate(shape);
        SphereTessellator.tessellate(shape, QuadPattern.STRIPE_1, null, null, 0);
    }
    
    private static void warmupPatterns() {
        for (QuadPattern p : QuadPattern.values()) {
            p.getVertexOrder();
        }
    }
    
    /**
     * Called every tick. This drives the smooth progress animation.
     */
    public static void tick() {
        if (warmupComplete.get()) {
            return;
        }
        
        int ticks = tickCount.incrementAndGet();
        
        // === ADAPTIVE ANTI-VIRUS PHASE ===
        // On tick 1, measure how long the first frame took to render.
        // If it was slow (lag spike), the user didn't see the message - extend display time.
        if (ticks == 1 && !frameTimeCaptured) {
            long frameDurationMs = (System.nanoTime() - firstFrameNanos) / 1_000_000;
            frameTimeCaptured = true;
            
            // If frame took >100ms (bad), extend Anti-Virus phase so user can read it
            // 100ms = smooth, 500ms+ = bad lag spike
            if (frameDurationMs > 500) {
                // Severe lag - show for 3 seconds
                antivirusTicks = ANTIVIRUS_TICKS_MAX;
                Logging.RENDER.topic("warmup").info("Lag spike detected ({}ms) - extending Anti-Virus display", frameDurationMs);
            } else if (frameDurationMs > 200) {
                // Moderate lag - show for 2 seconds
                antivirusTicks = 40;
            } else if (frameDurationMs > 100) {
                // Minor lag - show for 1.5 seconds
                antivirusTicks = 30;
            }
            // Otherwise keep base 20 ticks (1 second)
        }
        
        // Phase 0: Anti-Virus (0-10%) - adaptive ticks
        // Phase 1: Effects (10-25%) - 30 ticks
        // Phase 2: Profiles (25-40%) - 25 ticks
        // Phase 3: Shaders (40-50%) - 25 ticks
        // Phase 4: Chunks (50-100%) - wait for actual chunks
        
        int totalPreChunk = antivirusTicks + EFFECTS_TICKS + PROFILES_TICKS + SHADERS_TICKS;
        if (ticks <= totalPreChunk) {
            // Pre-chunk phases: smooth animation from 0-50%
            // The async work runs in parallel but we animate smoothly regardless
            return;
        }
        
        // After 100 ticks, we're at 50%+ and now we honestly wait for chunks
        chunkWaitTicks++;
        chunksLoaded = countLoadedChunks();
        
        // Stall detection: if chunk count hasn't increased, increment stall counter
        if (chunksLoaded > lastChunkCount) {
            lastChunkCount = chunksLoaded;
            stallTicks = 0;  // Reset - still loading
        } else {
            stallTicks++;
        }
        
        // Check completion conditions
        boolean allChunksLoaded = chunksLoaded >= totalChunks;
        boolean chunkTimeout = chunkWaitTicks >= MAX_CHUNK_WAIT;
        boolean loadingStalled = stallTicks >= STALL_THRESHOLD && chunksLoaded > 50;  // Min 50 chunks
        
        if (allChunksLoaded) {
            Logging.RENDER.topic("warmup").info("All {} chunks loaded!", totalChunks);
            completeWarmup();
        } else if (loadingStalled) {
            // Server view-distance likely lower than our target - that's fine
            Logging.RENDER.topic("warmup").info("Chunk loading complete (server limit) - {} loaded", chunksLoaded);
            completeWarmup();
        } else if (chunkTimeout) {
            Logging.RENDER.topic("warmup").warn("Chunk timeout - {} of {} loaded", chunksLoaded, totalChunks);
            completeWarmup();
        }
    }
    
    /**
     * Counts FULLY loaded chunks in 5x5 around player.
     * Uses ChunkStatus.FULL to ensure chunk has terrain data.
     */
    private static int countLoadedChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return 0;
        }
        
        ChunkPos center = client.player.getChunkPos();
        int count = 0;
        
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = center.x + dx;
                int cz = center.z + dz;
                
                // Use getChunk with ChunkStatus.FULL to check for fully loaded chunks
                // This returns null if chunk is not at FULL status
                var chunk = client.world.getChunkManager().getChunk(
                    cx, cz, 
                    net.minecraft.world.chunk.ChunkStatus.FULL, 
                    false  // Don't create if missing
                );
                
                if (chunk != null) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private static void completeWarmup() {
        warmupComplete.set(true);
        asyncTask = null;
        Logging.RENDER.topic("warmup").info("Warmup complete - fields will render");
        
        MinecraftClient.getInstance().execute(() -> {
            net.cyberpunk042.client.gui.widget.ToastNotification.success("Ready!");
        });
    }
    
    public static boolean shouldRenderFields() {
        return warmupComplete.get();
    }
    
    /**
     * Returns smooth progress 0.0 to 1.0.
     * 
     * Progress is TICK-BASED with adaptive Anti-Virus phase:
     * - Ticks 0-N: 0% to 10% (Anti-Virus scan, N=20-60 based on FPS)
     * - Next 30: 10% to 25% (Effects)
     * - Next 25: 25% to 40% (Profiles)
     * - Next 25: 40% to 50% (Shaders)
     * - After: 50% + chunk progress to 100%
     */
    public static float getWarmupProgress() {
        if (warmupComplete.get()) {
            return 1.0f;
        }
        
        int ticks = tickCount.get();
        
        // Phase 0: Anti-Virus (0-10%) - adaptive duration
        if (ticks <= antivirusTicks) {
            return (float) ticks / antivirusTicks * 0.10f;
        }
        
        // Phase 1: Effects (10-25%)
        int effectsEnd = antivirusTicks + EFFECTS_TICKS;
        if (ticks <= effectsEnd) {
            int effectTicks = ticks - antivirusTicks;
            return 0.10f + (float) effectTicks / EFFECTS_TICKS * 0.15f;
        }
        
        // Phase 2: Profiles (25-40%)
        int profilesEnd = effectsEnd + PROFILES_TICKS;
        if (ticks <= profilesEnd) {
            int profileTicks = ticks - effectsEnd;
            return 0.25f + (float) profileTicks / PROFILES_TICKS * 0.15f;
        }
        
        // Phase 3: Shaders (40-50%)
        int totalPreChunk = profilesEnd + SHADERS_TICKS;
        if (ticks <= totalPreChunk) {
            int shaderTicks = ticks - profilesEnd;
            return 0.40f + (float) shaderTicks / SHADERS_TICKS * 0.10f;
        }
        
        // Phase 4: Chunks (50-100%)
        float chunkProgress = (float) chunksLoaded / totalChunks;
        return 0.50f + chunkProgress * 0.50f;
    }
    
    /**
     * Returns the current stage label.
     */
    public static String getCurrentStageLabel() {
        if (warmupComplete.get()) {
            return "Ready!";
        }
        
        int ticks = tickCount.get();
        
        // Phase 0: Anti-Virus
        if (ticks <= antivirusTicks) {
            return "§b🛡 Anti-Virus Scan...";
        }
        
        // Phase 1: Effects
        int effectsEnd = antivirusTicks + EFFECTS_TICKS;
        if (ticks <= effectsEnd) {
            return "Loading Effects...";
        }
        
        // Phase 2: Profiles
        int profilesEnd = effectsEnd + PROFILES_TICKS;
        if (ticks <= profilesEnd) {
            return "Loading Profiles...";
        }
        
        // Phase 3: Shaders
        int totalPreChunk = profilesEnd + SHADERS_TICKS;
        if (ticks <= totalPreChunk) {
            return "Compiling Shaders...";
        }
        
        // Phase 4: Chunks
        return String.format("Loading Chunks (%d/%d)...", chunksLoaded, totalChunks);
    }
    
    public static boolean isWarmingUp() {
        return !warmupComplete.get();
    }
}
