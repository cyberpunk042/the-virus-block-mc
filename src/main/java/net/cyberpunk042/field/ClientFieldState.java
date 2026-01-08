package net.cyberpunk042.field;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Client-side field state for rendering.
 * 
 * <p>This is a lightweight representation of field data received from the server.
 * It contains only what's needed for rendering, not full server-side logic.
 * 
 * <p><b>Note:</b> For server-side field instances with full behavior,
 * see {@link net.cyberpunk042.field.instance.FieldInstance}.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Create from network payload
 * ClientFieldState state = ClientFieldState.atPosition(id, defId, type, pos)
 *     .withScale(scale)
 *     .withPhase(phase);
 * 
 * // Add to client manager
 * ClientFieldManager.get().addOrUpdate(state);
 * </pre>
 */
public final class ClientFieldState {
    
    /** Number of ticks for fade-out at end of life */
    private static final int FADE_TICKS = 20;
    
    private final long id;
    private final Identifier definitionId;
    private final FieldType type;
    
    // Position (one of these is set)
    private Vec3d position;
    private BlockPos blockPos;
    private UUID ownerUuid;
    
    // Runtime state
    private int age;
    private int maxLifeTicks;
    private float alpha;
    private float scale;
    private float phase;
    private boolean removed;
    
    // Shuffle override (for live editing)
    private String shuffleType;
    private int shuffleIndex = -1;
    
    // Follow mode and prediction (for personal shields)
    // null = no follow (use world position), "snap"/"smooth"/"glide" = follow player
    private String followMode = null;
    private boolean predictionEnabled = false;
    private int predictionLeadTicks = 0;
    private float predictionMaxDistance = 0;
    private float predictionLookAhead = 0;
    private float predictionVerticalBoost = 0;
    
    // Interpolated position (for smooth/glide modes) - with prev for render-frame interpolation
    private Vec3d previousInterpolatedPosition;  // Position from last tick
    private Vec3d interpolatedPosition;          // Position updated this tick

    
    // =========================================================================
    // Constructors
    // =========================================================================
    
    private ClientFieldState(long id, Identifier definitionId, FieldType type) {
        this.id = id;
        this.definitionId = definitionId;
        this.type = type;
        this.age = 0;
        this.maxLifeTicks = -1; // -1 = infinite
        this.alpha = 1.0f;
        this.scale = 1.0f;
        this.phase = 0;
        this.removed = false;
    }
    
    /**
     * Creates a world-positioned field state.
     */
    public static ClientFieldState atPosition(long id, Identifier definitionId, FieldType type, Vec3d position) {
        ClientFieldState state = new ClientFieldState(id, definitionId, type);
        state.position = position;
        return state;
    }
    
    /**
     * Creates a block-anchored field state.
     */
    public static ClientFieldState atBlock(long id, Identifier definitionId, FieldType type, BlockPos blockPos) {
        ClientFieldState state = new ClientFieldState(id, definitionId, type);
        state.blockPos = blockPos;
        state.position = Vec3d.ofCenter(blockPos);
        return state;
    }
    
    /**
     * Creates a player-attached field state.
     */
    public static ClientFieldState forPlayer(long id, Identifier definitionId, FieldType type, UUID playerUuid) {
        ClientFieldState state = new ClientFieldState(id, definitionId, type);
        state.ownerUuid = playerUuid;
        state.position = Vec3d.ZERO;
        return state;
    }
    
    // =========================================================================
    // Getters
    // =========================================================================
    
    public long id() { return id; }
    public Identifier definitionId() { return definitionId; }
    public FieldType type() { return type; }
    public Vec3d position() { return position; }
    public @Nullable BlockPos blockPos() { return blockPos; }
    public @Nullable UUID ownerUuid() { return ownerUuid; }
    public int age() { return age; }
    public int maxLifeTicks() { return maxLifeTicks; }
    public float alpha() { return alpha; }
    public float scale() { return scale; }
    public float phase() { return phase; }
    public boolean isRemoved() { return removed; }
    public @Nullable String shuffleType() { return shuffleType; }
    public int shuffleIndex() { return shuffleIndex; }
    public boolean hasShuffleOverride() { return shuffleType != null && !shuffleType.isEmpty(); }
    public boolean isStaticPattern() { return shuffleType != null && shuffleType.startsWith("static:"); }
    
    // Follow mode and prediction getters
    public String followMode() { return followMode; }
    public boolean predictionEnabled() { return predictionEnabled; }
    public int predictionLeadTicks() { return predictionLeadTicks; }
    public float predictionMaxDistance() { return predictionMaxDistance; }
    public float predictionLookAhead() { return predictionLookAhead; }
    public float predictionVerticalBoost() { return predictionVerticalBoost; }
    public Vec3d interpolatedPosition() { return interpolatedPosition != null ? interpolatedPosition : position; }
    
    /**
     * Gets the interpolated render position for smooth frame-by-frame movement.
     * 
     * <p>This interpolates between the position from the last tick and the current
     * tick position, eliminating visual stuttering when rendering at higher FPS
     * than the tick rate.
     * 
     * @param tickDelta Partial tick progress (0.0 = start of tick, 1.0 = end of tick)
     * @return Smoothly interpolated position for rendering
     */
    public Vec3d getRenderPosition(float tickDelta) {
        Vec3d current = interpolatedPosition != null ? interpolatedPosition : position;
        Vec3d previous = previousInterpolatedPosition != null ? previousInterpolatedPosition : current;
        
        // First frame or no previous: use current directly
        if (previous.equals(Vec3d.ZERO) && !current.equals(Vec3d.ZERO)) {
            return current;
        }
        
        // Lerp between previous and current for smooth sub-tick movement
        return previous.lerp(current, tickDelta);
    }

    
    public boolean hasLifetime() { return maxLifeTicks > 0; }
    public boolean isPlayerAttached() { return ownerUuid != null; }
    public boolean isBlockAttached() { return blockPos != null; }
    
    // =========================================================================
    // Setters / State updates
    // =========================================================================
    
    public ClientFieldState withPosition(Vec3d position) {
        this.position = position;
        return this;
    }
    
    public ClientFieldState withScale(float scale) {
        this.scale = scale;
        return this;
    }
    
    public ClientFieldState withAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }
    
    public ClientFieldState withPhase(float phase) {
        this.phase = phase;
        return this;
    }
    
    public ClientFieldState withLifetime(int ticks) {
        this.maxLifeTicks = ticks;
        return this;
    }
    
    public ClientFieldState withShuffleOverride(String type, int index) {
        this.shuffleType = type;
        this.shuffleIndex = index;
        return this;
    }
    
    public ClientFieldState withFollowMode(String mode) {
        this.followMode = mode != null ? mode : "snap";
        return this;
    }
    
    public ClientFieldState withPrediction(boolean enabled, int leadTicks, float maxDistance, 
                                            float lookAhead, float verticalBoost) {
        this.predictionEnabled = enabled;
        this.predictionLeadTicks = leadTicks;
        this.predictionMaxDistance = maxDistance;
        this.predictionLookAhead = lookAhead;
        this.predictionVerticalBoost = verticalBoost;
        return this;
    }
    
    public ClientFieldState withInterpolatedPosition(Vec3d pos) {
        // Save previous for render-frame interpolation
        this.previousInterpolatedPosition = this.interpolatedPosition;
        this.interpolatedPosition = pos;
        return this;
    }
    
    /**
     * Ticks the state.
     * @return true if the state should be removed
     */
    public boolean tick() {
        if (removed) {
            return true;
        }
        
        age++;
        
        if (maxLifeTicks > 0 && age >= maxLifeTicks) {
            removed = true;
            return true;
        }
        
        return false;
    }
    
    public void remove() {
        this.removed = true;
    }
    
    /**
     * Gets effective alpha (considering fade-out at end of life).
     */
    public float effectiveAlpha() {
        if (maxLifeTicks <= 0) {
            return alpha;
        }
        
        int remaining = maxLifeTicks - age;
        if (remaining > FADE_TICKS) {
            return alpha;
        }
        
        return alpha * (remaining / (float) FADE_TICKS);
    }
}
