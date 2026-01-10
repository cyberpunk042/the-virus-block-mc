package net.cyberpunk042.client.input.spawn;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration for orb spawn animation.
 * 
 * @param originMode Direction the orb comes from
 * @param targetMode How targeting works (RELATIVE or TRUE_TARGET)
 * @param spawnDistance How far away the orb spawns (defaults to proximityDarken)
 * @param targetDistance Where the orb stops for RELATIVE mode (defaults to 50% of spawnDistance)
 * @param trueTargetCoords Exact world coordinates for TRUE_TARGET mode (null if RELATIVE)
 * @param interpolationDurationMs How long the travel takes in milliseconds
 * @param easingCurve Movement curve type
 * @param fadeInDurationMs How long the orb takes to appear (smooth fade in)
 * @param fadeOutDurationMs How long the orb takes to disappear (smooth fade out)
 * @param lifetimeMs How long the orb exists after arriving (0 = infinite, max = 172800000 / 2 days)
 * @param followModeAfterArrival Whether the orb follows the player after arriving
 */
public record OrbSpawnConfig(
    SpawnOriginMode originMode,
    TargetMode targetMode,
    float spawnDistance,
    float targetDistance,
    @Nullable Vec3d trueTargetCoords,
    long interpolationDurationMs,
    EasingCurve easingCurve,
    long fadeInDurationMs,
    long fadeOutDurationMs,
    long lifetimeMs,
    boolean followModeAfterArrival
) {
    /**
     * Default configuration.
     * Spawn distance should typically be read from proximityDarken.
     */
    public static final OrbSpawnConfig DEFAULT = new OrbSpawnConfig(
        SpawnOriginMode.FROM_ABOVE,
        TargetMode.RELATIVE,
        1000f,          // spawnDistance (should be overridden with proximityDarken)
        500f,           // targetDistance (50% of spawn)
        null,           // trueTargetCoords (not used in RELATIVE mode)
        10000L,         // 10 seconds travel time
        EasingCurve.EASE_OUT,
        500L,           // 500ms fade in
        500L,           // 500ms fade out
        0L,             // 0 = infinite lifetime
        true            // follow player after arrival
    );
    
    /**
     * Maximum allowed lifetime (2 days in milliseconds).
     */
    public static final long MAX_LIFETIME_MS = 172_800_000L;
    
    /**
     * Create a config with spawn distance based on proximityDarken.
     */
    public static OrbSpawnConfig withProximityDarken(float proximityDarken) {
        return new OrbSpawnConfig(
            SpawnOriginMode.FROM_ABOVE,
            TargetMode.RELATIVE,
            proximityDarken,
            proximityDarken * 0.5f,  // 50% of spawn distance
            null,
            10000L,
            EasingCurve.EASE_OUT,
            500L,
            500L,
            0L,
            true
        );
    }
    
    /**
     * Builder-style method to set origin mode.
     */
    public OrbSpawnConfig withOriginMode(SpawnOriginMode mode) {
        return new OrbSpawnConfig(mode, targetMode, spawnDistance, targetDistance, 
            trueTargetCoords, interpolationDurationMs, easingCurve, 
            fadeInDurationMs, fadeOutDurationMs, lifetimeMs, followModeAfterArrival);
    }
    
    /**
     * Builder-style method to set target mode.
     */
    public OrbSpawnConfig withTargetMode(TargetMode mode) {
        return new OrbSpawnConfig(originMode, mode, spawnDistance, targetDistance, 
            trueTargetCoords, interpolationDurationMs, easingCurve, 
            fadeInDurationMs, fadeOutDurationMs, lifetimeMs, followModeAfterArrival);
    }
    
    /**
     * Builder-style method to set true target coordinates.
     */
    public OrbSpawnConfig withTrueTarget(Vec3d coords) {
        return new OrbSpawnConfig(originMode, TargetMode.TRUE_TARGET, spawnDistance, targetDistance, 
            coords, interpolationDurationMs, easingCurve, 
            fadeInDurationMs, fadeOutDurationMs, lifetimeMs, followModeAfterArrival);
    }
    
    /**
     * Builder-style method to set easing curve.
     */
    public OrbSpawnConfig withEasing(EasingCurve curve) {
        return new OrbSpawnConfig(originMode, targetMode, spawnDistance, targetDistance, 
            trueTargetCoords, interpolationDurationMs, curve, 
            fadeInDurationMs, fadeOutDurationMs, lifetimeMs, followModeAfterArrival);
    }
    
    /**
     * Builder-style method to set lifetime.
     */
    public OrbSpawnConfig withLifetime(long lifetimeMs) {
        long clamped = Math.max(0, Math.min(MAX_LIFETIME_MS, lifetimeMs));
        return new OrbSpawnConfig(originMode, targetMode, spawnDistance, targetDistance, 
            trueTargetCoords, interpolationDurationMs, easingCurve, 
            fadeInDurationMs, fadeOutDurationMs, clamped, followModeAfterArrival);
    }
}
