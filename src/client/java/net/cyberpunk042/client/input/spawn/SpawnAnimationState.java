package net.cyberpunk042.client.input.spawn;

import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/**
 * Internal state tracking for an orb that is being spawned/animated.
 * Package-private - only used by OrbSpawnManager.
 */
class SpawnAnimationState {
    
    /**
     * Animation phases.
     */
    enum Phase {
        FADE_IN,    // Orb appearing at spawn position
        TRAVEL,     // Orb moving from spawn to target
        ALIVE,      // Orb at destination, waiting for lifetime
        FADE_OUT    // Orb disappearing
    }
    
    // Identifiers
    final UUID orbId;
    final OrbSpawnConfig config;
    
    // Positions
    final Vec3d spawnPosition;
    final Vec3d targetPosition;
    final Vec3d referencePosition;  // Player pos at spawn time
    
    // For followModeAfterArrival: offset from player calculated at arrival
    // null until ALIVE phase starts
    Vec3d arrivalOffset = null;
    
    // Timing
    final long spawnTimeMs;
    Phase currentPhase;
    long phaseStartTimeMs;
    
    /**
     * Create a new spawn animation state.
     */
    SpawnAnimationState(
        UUID orbId,
        OrbSpawnConfig config,
        Vec3d spawnPosition,
        Vec3d targetPosition,
        Vec3d referencePosition
    ) {
        this.orbId = orbId;
        this.config = config;
        this.spawnPosition = spawnPosition;
        this.targetPosition = targetPosition;
        this.referencePosition = referencePosition;
        this.spawnTimeMs = System.currentTimeMillis();
        this.currentPhase = Phase.FADE_IN;
        this.phaseStartTimeMs = spawnTimeMs;
    }
    
    /**
     * Transition to the next phase.
     */
    void advancePhase() {
        phaseStartTimeMs = System.currentTimeMillis();
        currentPhase = switch (currentPhase) {
            case FADE_IN -> Phase.TRAVEL;
            case TRAVEL -> Phase.ALIVE;
            case ALIVE -> Phase.FADE_OUT;
            case FADE_OUT -> Phase.FADE_OUT; // Terminal state
        };
    }
    
    /**
     * Get elapsed time in current phase.
     */
    long getPhaseElapsedMs() {
        return System.currentTimeMillis() - phaseStartTimeMs;
    }
    
    /**
     * Get total elapsed time since spawn.
     */
    long getTotalElapsedMs() {
        return System.currentTimeMillis() - spawnTimeMs;
    }
    
    /**
     * Check if this animation is complete (ready for cleanup).
     */
    boolean isComplete() {
        if (currentPhase != Phase.FADE_OUT) return false;
        return getPhaseElapsedMs() >= config.fadeOutDurationMs();
    }
}
