package net.cyberpunk042.client.input.spawn;

import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.visual.effect.FieldVisualConfig;
import net.cyberpunk042.client.visual.effect.FieldVisualInstance;
import net.cyberpunk042.client.visual.effect.FieldVisualPostEffect;
import net.cyberpunk042.client.visual.effect.FieldVisualRegistry;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages orb spawn animations ("coming into existence").
 * 
 * <p>Handles the full lifecycle: spawn → fade in → travel → alive → fade out → cleanup</p>
 */
public final class OrbSpawnManager {
    
    private static final String LOG_TOPIC = "orb_spawn";
    
    /**
     * Active spawn animations, keyed by orb ID.
     */
    private static final Map<UUID, SpawnAnimationState> activeAnimations = new ConcurrentHashMap<>();
    
    private OrbSpawnManager() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Spawn an orb with animation.
     * 
     * @param config Spawn configuration
     * @param referencePos Reference position (usually player position)
     * @return UUID of the spawned orb
     */
    public static UUID spawnOrb(OrbSpawnConfig config, Vec3d referencePos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }
        
        // Clean up any existing spawn orbs first (only one spawn orb at a time)
        for (UUID existingId : activeAnimations.keySet()) {
            FieldVisualRegistry.unregister(existingId);
            FieldVisualPostEffect.removeProcessor(existingId);
        }
        activeAnimations.clear();
        
        // Get player look direction for spawn direction calculation
        Vec3d lookDir = client.player.getRotationVec(1.0f);
        
        // Calculate spawn position
        Vec3d spawnDirection = config.originMode().getSpawnDirection(lookDir);
        Vec3d spawnPosition = referencePos.add(spawnDirection.multiply(config.spawnDistance()));
        
        // Calculate target position based on target mode
        Vec3d targetPosition;
        if (config.targetMode() == TargetMode.TRUE_TARGET && config.trueTargetCoords() != null) {
            targetPosition = config.trueTargetCoords();
        } else {
            // RELATIVE mode: target is between spawn and player
            // targetDistance is how far FROM PLAYER the orb stops
            // At 100%: targetDist=0, orb reaches player
            // At 0%: targetDist=spawnDist, orb stays at spawn
            // The target is along the LINE from spawn to player
            Vec3d toPlayer = referencePos.subtract(spawnPosition).normalize();
            float distanceFromSpawn = config.spawnDistance() - config.targetDistance();
            targetPosition = spawnPosition.add(toPlayer.multiply(distanceFromSpawn));
        }
        
        // Get visual config from adapter
        FieldVisualConfig visualConfig = getVisualConfig();
        float orbRadius = getOrbRadius();
        
        // Create orb ID
        UUID orbId = UUID.randomUUID();
        
        // Create orb instance at spawn position with 0 opacity
        FieldVisualInstance orb = new FieldVisualInstance(
            orbId,
            client.player.getUuid(),
            spawnPosition,
            orbRadius,
            "sphere",
            visualConfig
        );
        orb.setSpawnOpacity(0f);  // Start invisible
        orb.setSpawnAnimationOrb(true);  // Mark as spawn orb - NOT affected by global follow
        
        // Register orb
        FieldVisualRegistry.register(orb);
        FieldVisualPostEffect.setEnabled(true);
        
        // Create animation state
        SpawnAnimationState state = new SpawnAnimationState(
            orbId, config, spawnPosition, targetPosition, referencePos
        );
        activeAnimations.put(orbId, state);
        
        Logging.GUI.topic(LOG_TOPIC)
            .kv("orbId", orbId.toString().substring(0, 8))
            .kv("origin", config.originMode().name())
            .kv("spawnPos", formatVec(spawnPosition))
            .kv("targetPos", formatVec(targetPosition))
            .info("Orb spawn started");
        
        return orbId;
    }
    
    /**
     * Manually despawn an orb (triggers fade out).
     */
    public static void despawnOrb(UUID orbId) {
        SpawnAnimationState state = activeAnimations.get(orbId);
        if (state != null && state.currentPhase != SpawnAnimationState.Phase.FADE_OUT) {
            state.currentPhase = SpawnAnimationState.Phase.FADE_OUT;
            state.phaseStartTimeMs = System.currentTimeMillis();
            
            Logging.GUI.topic(LOG_TOPIC)
                .kv("orbId", orbId.toString().substring(0, 8))
                .info("Orb despawn triggered");
        }
    }
    
    /**
     * Despawn all active orbs.
     */
    public static void despawnAll() {
        for (UUID orbId : activeAnimations.keySet()) {
            despawnOrb(orbId);
        }
    }
    
    /**
     * Check if any orbs are currently animating.
     */
    public static boolean hasActiveAnimations() {
        return !activeAnimations.isEmpty();
    }
    
    /**
     * Get count of active orbs.
     */
    public static int getActiveCount() {
        return activeAnimations.size();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TICK - Called every frame
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Update all active spawn animations.
     * Called from ClientTickEvents.
     */
    public static void tick(MinecraftClient client) {
        if (client.player == null || activeAnimations.isEmpty()) {
            return;
        }
        
        // Trace logging for debugging
        
        // Process each animation
        var iterator = activeAnimations.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID orbId = entry.getKey();
            SpawnAnimationState state = entry.getValue();
            
            // Get the orb instance
            FieldVisualInstance orb = FieldVisualRegistry.get(orbId);
            if (orb == null) {
                iterator.remove();
                continue;
            }
            
            // Process based on current phase
            switch (state.currentPhase) {
                case FADE_IN -> tickFadeIn(state, orb);
                case TRAVEL -> tickTravel(state, orb, client);
                case ALIVE -> tickAlive(state, orb, client);
                case FADE_OUT -> tickFadeOut(state, orb);
            }
            
            // Check for completion
            if (state.isComplete()) {
                FieldVisualRegistry.unregister(orbId);
                iterator.remove();
                
                Logging.GUI.topic(LOG_TOPIC)
                    .kv("orbId", orbId.toString().substring(0, 8))
                    .info("Orb animation complete, cleaned up");
                
                // NOTE: Don't disable FieldVisualPostEffect here - preview orb may still be active
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PHASE HANDLERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void tickFadeIn(SpawnAnimationState state, FieldVisualInstance orb) {
        long elapsed = state.getPhaseElapsedMs();
        long duration = state.config.fadeInDurationMs();
        
        if (elapsed >= duration) {
            // Fade in complete
            orb.setSpawnOpacity(1f);
            state.advancePhase();
        } else {
            // Interpolate opacity
            float progress = (float) elapsed / duration;
            float opacity = state.config.easingCurve().apply(progress);
            orb.setSpawnOpacity(opacity);
        }
    }
    
    private static void tickTravel(SpawnAnimationState state, FieldVisualInstance orb, MinecraftClient client) {
        long elapsed = state.getPhaseElapsedMs();
        long duration = state.config.interpolationDurationMs();
        
        if (elapsed >= duration) {
            // Travel complete
            orb.updatePosition(state.targetPosition);
            state.advancePhase();
        } else {
            // Interpolate position
            float progress = (float) elapsed / duration;
            float eased = state.config.easingCurve().apply(progress);
            
            Vec3d currentPos = state.spawnPosition.lerp(state.targetPosition, eased);
            orb.updatePosition(currentPos);
        }
    }
    
    private static void tickAlive(SpawnAnimationState state, FieldVisualInstance orb, MinecraftClient client) {
        // Handle follow mode ONLY if enabled
        if (state.config.followModeAfterArrival() && client.player != null) {
            // Calculate arrival offset on first tick of ALIVE phase
            if (state.arrivalOffset == null) {
                Vec3d playerPos = client.player.getBoundingBox().getCenter();
                // Offset = target position relative to current player position
                state.arrivalOffset = state.targetPosition.subtract(playerPos);
            }
            
            // Apply fixed offset to current player position
            Vec3d playerPos = client.player.getBoundingBox().getCenter();
            Vec3d newPos = playerPos.add(state.arrivalOffset);
            orb.updatePosition(newPos);
        }
        // If followModeAfterArrival is false, orb stays at state.targetPosition (no updates)
        
        // Check lifetime
        long lifetime = state.config.lifetimeMs();
        if (lifetime > 0) {
            long elapsed = state.getPhaseElapsedMs();
            if (elapsed >= lifetime) {
                // Lifetime expired, start fade out
                state.advancePhase();
            }
        }
        // If lifetime == 0, orb stays alive forever (until manual despawn)
    }
    
    private static void tickFadeOut(SpawnAnimationState state, FieldVisualInstance orb) {
        long elapsed = state.getPhaseElapsedMs();
        long duration = state.config.fadeOutDurationMs();
        
        if (elapsed >= duration) {
            orb.setSpawnOpacity(0f);
            // isComplete() will return true and cleanup will happen
        } else {
            // Interpolate opacity (reverse)
            float progress = (float) elapsed / duration;
            float opacity = 1f - state.config.easingCurve().apply(progress);
            orb.setSpawnOpacity(opacity);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static FieldVisualConfig getVisualConfig() {
        var editState = FieldEditStateHolder.get();
        if (editState != null) {
            return editState.fieldVisualAdapter().buildConfig();
        }
        return FieldVisualConfig.defaultEnergyOrb();
    }
    
    private static float getOrbRadius() {
        var editState = FieldEditStateHolder.get();
        if (editState != null) {
            Object val = editState.fieldVisualAdapter().get("previewRadius");
            if (val instanceof Number n) return n.floatValue();
        }
        return 3.0f;
    }
    
    private static String formatVec(Vec3d v) {
        return String.format("%.1f,%.1f,%.1f", v.x, v.y, v.z);
    }
}
