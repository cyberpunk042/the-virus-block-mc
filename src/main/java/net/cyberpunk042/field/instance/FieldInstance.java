package net.cyberpunk042.field.instance;

import net.cyberpunk042.log.Logging;

import net.cyberpunk042.field.FieldType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Base class for active field instances.
 * 
 * <h2>Instance Hierarchy</h2>
 * <pre>
 * FieldInstance (abstract)
 * ├── PersonalFieldInstance - Attached to a player
 * └── AnchoredFieldInstance - Attached to a block position
 * </pre>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Created via {@link net.cyberpunk042.field.FieldManager}</li>
 *   <li>Ticked each game tick</li>
 *   <li>Removed when expired or manually despawned</li>
 * </ol>
 */
public abstract class FieldInstance {
    
    protected final long id;
    protected final Identifier definitionId;
    protected final FieldType type;
    protected Vec3d position;
    protected float scale;
    protected float phase;
    protected float alpha;
    protected int age;
    protected int maxLifeTicks;
    protected boolean alive;
    protected boolean removed;
    
    // F162: Lifecycle state tracking
    protected LifecycleState lifecycleState = LifecycleState.SPAWNING;
    // F163: Fade progress (0.0 → 1.0 during transitions)
    protected float fadeProgress = 0.0f;
    
    // Cached definition to avoid registry lookup every tick
    private transient net.cyberpunk042.field.FieldDefinition cachedDef;
    
    protected FieldInstance(long id, Identifier definitionId, FieldType type, Vec3d position) {
        this.id = id;
        this.definitionId = definitionId;
        this.type = type;
        this.position = position;
        this.scale = 1.0f;
        this.phase = 0;
        this.alpha = 1.0f;
        this.age = 0;
        this.maxLifeTicks = -1; // -1 = infinite
        this.alive = true;
        this.removed = false;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────────
    
    public long id() { return id; }
    public Identifier definitionId() { return definitionId; }
    public FieldType type() { return type; }
    public Vec3d position() { return position; }
    public float scale() { return scale; }
    public float phase() { return phase; }
    public float alpha() { return alpha; }
    public int age() { return age; }
    public int maxLifeTicks() { return maxLifeTicks; }
    public boolean isAlive() { return alive && !removed; }
    public boolean isRemoved() { return removed; }
    
    // F162-F163: Lifecycle state accessors
    public LifecycleState lifecycleState() { return lifecycleState; }
    public float fadeProgress() { return fadeProgress; }
    public boolean isSpawning() { return lifecycleState == LifecycleState.SPAWNING; }
    public boolean isActive() { return lifecycleState == LifecycleState.ACTIVE; }
    public boolean isDespawning() { return lifecycleState == LifecycleState.DESPAWNING; }
    public boolean isComplete() { return lifecycleState == LifecycleState.COMPLETE; }
    
    // Definition caching to avoid registry lookup every tick
    public net.cyberpunk042.field.FieldDefinition cachedDefinition() { return cachedDef; }
    public void cacheDefinition(net.cyberpunk042.field.FieldDefinition def) { this.cachedDef = def; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Modifiers
    // ─────────────────────────────────────────────────────────────────────────────
    
    public void setPosition(Vec3d pos) {
        this.position = pos;
    }
    
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    public void setPhase(float phase) {
        this.phase = phase;
    }
    
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0, Math.min(1, alpha));
    }
    
    public void setMaxLifeTicks(int ticks) {
        this.maxLifeTicks = ticks;
    }
    
    // F162: Lifecycle state transitions
    public void setLifecycleState(LifecycleState state) {
        if (this.lifecycleState != state) {
            Logging.FIELD.topic("lifecycle").debug(
                "Field {} state: {} → {}", id, this.lifecycleState, state);
            this.lifecycleState = state;
            this.fadeProgress = 0.0f; // Reset progress on state change
        }
    }
    
    // F163: Fade progress
    public void setFadeProgress(float progress) {
        this.fadeProgress = Math.max(0f, Math.min(1f, progress));
    }
    
    /**
     * Advances to ACTIVE state (called when spawn animation completes).
     */
    public void activate() {
        if (lifecycleState == LifecycleState.SPAWNING) {
            setLifecycleState(LifecycleState.ACTIVE);
            setFadeProgress(1.0f);
        }
    }
    
    /**
     * Begins despawn animation.
     */
    public void beginDespawn() {
        if (lifecycleState == LifecycleState.ACTIVE || lifecycleState == LifecycleState.SPAWNING) {
            setLifecycleState(LifecycleState.DESPAWNING);
        }
    }
    
    /**
     * Marks lifecycle as complete (ready for removal).
     */
    public void complete() {
        setLifecycleState(LifecycleState.COMPLETE);
        this.alive = false;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Called each tick. Returns true if the instance should be removed.
     */
    public boolean tick() {
        if (removed) {
            return true;
        }
        
        age++;
        
        // Check lifetime
        if (maxLifeTicks > 0 && age >= maxLifeTicks) {
            remove();
            return true;
        }
        
        // Update phase for animation
        phase += 0.05f;
        if (phase > Math.PI * 2) {
            phase -= (float)(Math.PI * 2);
        }
        
        // Subclass-specific tick
        tickInstance();
        
        return !alive || removed;
    }
    
    /**
     * Subclass-specific tick logic.
     */
    protected abstract void tickInstance();
    
    /**
     * Marks this instance for removal.
     */
    public void remove() {
        Logging.REGISTRY.topic("instance").debug(
            "Field {} removed (type={}, age={})", id, type.id(), age);
        this.alive = false;
        this.removed = true;
    }
    
    /**
     * Called when the instance is actually removed from the manager.
     */
    public void onRemoved() {
        // Override in subclasses for cleanup
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Owner (for personal fields)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Returns the owner UUID if this is a personal field, null otherwise.
     */
    public UUID ownerUuid() {
        return null;
    }
    
    /**
     * Returns the anchor block position if this is an anchored field, null otherwise.
     */
    public BlockPos anchorPos() {
        return null;
    }
}
