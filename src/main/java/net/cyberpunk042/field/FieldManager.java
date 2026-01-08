package net.cyberpunk042.field;

import net.cyberpunk042.field.instance.FieldInstance;
import net.cyberpunk042.field.instance.PersonalFieldInstance;
import net.cyberpunk042.field.instance.AnchoredFieldInstance;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Server-side manager for active field instances.
 */
public final class FieldManager {
    
    private static final Map<ServerWorld, FieldManager> MANAGERS = new WeakHashMap<>();
    private static final AtomicLong ID_COUNTER = new AtomicLong(1);
    
    private final ServerWorld world;
    private final Map<Long, FieldInstance> instances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerFields = new ConcurrentHashMap<>();
    
    private Consumer<FieldInstance> onSpawn;
    private Consumer<Long> onRemove;
    private Consumer<FieldInstance> onUpdate;
    
    private FieldManager(ServerWorld world) {
        this.world = world;
    }
    
    public static FieldManager get(ServerWorld world) {
        return MANAGERS.computeIfAbsent(world, FieldManager::new);
    }
    
    public static void remove(ServerWorld world) {
        MANAGERS.remove(world);
    }
    
    public void onSpawn(Consumer<FieldInstance> callback) { this.onSpawn = callback; }
    public void onRemove(Consumer<Long> callback) { this.onRemove = callback; }
    public void onUpdate(Consumer<FieldInstance> callback) { this.onUpdate = callback; }
    
    public AnchoredFieldInstance spawnAt(Identifier definitionId, Vec3d position, float scale, int lifetimeTicks) {
        FieldDefinition def = FieldRegistry.get(definitionId);
        if (def == null) {
            Logging.REGISTRY.topic("field").warn("Unknown definition: {}", definitionId);
            return null;
        }
        
        long id = ID_COUNTER.getAndIncrement();
        AnchoredFieldInstance instance = new AnchoredFieldInstance(
            id, definitionId, def.type(), BlockPos.ofFloored(position));
        instance.setScale(scale);
        if (lifetimeTicks > 0) instance.setMaxLifeTicks(lifetimeTicks);
        instance.setPhase(world.random.nextFloat() * (float) Math.PI * 2);
        
        instances.put(id, instance);
        if (onSpawn != null) onSpawn.accept(instance);
        
        Logging.REGISTRY.topic("field").info(
            "Spawned field {} at {} (lifetime={})", definitionId, position, lifetimeTicks);
        return instance;
    }
    
    public AnchoredFieldInstance spawnAtBlock(Identifier definitionId, BlockPos pos, float scale) {
        FieldDefinition def = FieldRegistry.get(definitionId);
        if (def == null) {
            Logging.REGISTRY.topic("field").warn("Unknown definition: {}", definitionId);
            return null;
        }
        
        long id = ID_COUNTER.getAndIncrement();
        AnchoredFieldInstance instance = new AnchoredFieldInstance(id, definitionId, def.type(), pos);
        instance.setScale(scale);
        instance.setPhase(world.random.nextFloat() * (float) Math.PI * 2);
        
        instances.put(id, instance);
        if (onSpawn != null) onSpawn.accept(instance);
        
        Logging.REGISTRY.topic("field").info(
            "Spawned field {} at block {} (scale={})", definitionId, pos, scale);
        return instance;
    }
    
    public PersonalFieldInstance spawnForPlayer(Identifier definitionId, UUID playerUuid, float scale) {
        Long existing = playerFields.get(playerUuid);
        if (existing != null) remove(existing);
        
        FieldDefinition def = FieldRegistry.get(definitionId);
        if (def == null) return null;
        
        long id = ID_COUNTER.getAndIncrement();
        PersonalFieldInstance instance = new PersonalFieldInstance(
            id, definitionId, def.type(), playerUuid, Vec3d.ZERO);
        instance.setScale(scale);
        instance.setPhase(world.random.nextFloat() * (float) Math.PI * 2);
        
        instances.put(id, instance);
        playerFields.put(playerUuid, id);
        if (onSpawn != null) onSpawn.accept(instance);
        return instance;
    }
    
    public boolean remove(long id) {
        FieldInstance instance = instances.remove(id);
        if (instance == null) return false;
        
        instance.remove();
        if (instance.ownerUuid() != null) playerFields.remove(instance.ownerUuid());
        if (onRemove != null) onRemove.accept(id);
        return true;
    }
    
    public boolean removePlayerField(UUID playerUuid) {
        Long id = playerFields.remove(playerUuid);
        return id != null && remove(id);
    }
    
    public FieldInstance get(long id) { return instances.get(id); }
    
    public FieldInstance getPlayerField(UUID playerUuid) {
        Long id = playerFields.get(playerUuid);
        return id != null ? instances.get(id) : null;
    }
    
    public Collection<FieldInstance> all() {
        return Collections.unmodifiableCollection(instances.values());
    }
    
    /**
     * Gets a specific field instance by ID.
     */
    public FieldInstance getInstance(long id) {
        return instances.get(id);
    }
    
    public int count() { return instances.size(); }
    
    public void tick() {
        if (instances.isEmpty()) return; // Fast exit when no fields
        
        Iterator<Map.Entry<Long, FieldInstance>> it = instances.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, FieldInstance> entry = it.next();
            FieldInstance instance = entry.getValue();
            
            // Apply force physics if this field has a forceConfig
            // Use cached definition from instance to avoid registry lookup every tick
            FieldDefinition def = instance.cachedDefinition();
            if (def == null) {
                def = FieldRegistry.get(instance.definitionId());
                instance.cacheDefinition(def);
            }
            
            if (def == null) {
                // Log once per second (every 20 ticks) to avoid spam
                if (instance.age() % 20 == 0) {
                    Logging.REGISTRY.topic("force").debug(
                        "No definition found for field {} (defId={})", 
                        instance.id(), instance.definitionId());
                }
            } else if (def.forceConfig() != null) {
                applyFieldForces(instance, def);
            }
            
            if (instance.tick()) {
                it.remove();
                if (instance.ownerUuid() != null) playerFields.remove(instance.ownerUuid());
                if (onRemove != null) onRemove.accept(entry.getKey());
            }
        }
    }
    
    /**
     * Applies force physics to entities near the field.
     */
    private void applyFieldForces(FieldInstance instance, FieldDefinition def) {
        var forceConfig = def.forceConfig();
        float maxRadius = forceConfig.maxRadius() * instance.scale();
        
        // Build search box around field position
        Vec3d center = instance.position();
        net.minecraft.util.math.Box searchBox = new net.minecraft.util.math.Box(
            center.x - maxRadius, center.y - maxRadius, center.z - maxRadius,
            center.x + maxRadius, center.y + maxRadius, center.z + maxRadius
        );
        
        // Calculate normalized time for phase effects
        float normalizedTime = instance.maxLifeTicks() > 0 
            ? (float) instance.age() / instance.maxLifeTicks() 
            : 0.5f; // Default to middle of cycle for infinite fields
        
        // Create force field calculator
        net.cyberpunk042.field.force.field.RadialForceField forceField = 
            new net.cyberpunk042.field.force.field.RadialForceField(forceConfig);
        
        // Find and affect all living entities (server-authoritative forces)
        for (net.minecraft.entity.Entity entity : world.getEntitiesByClass(
                net.minecraft.entity.LivingEntity.class, searchBox, 
                e -> e.isAlive() && !e.isSpectator())) {
            
            applyForceToEntity((net.minecraft.entity.LivingEntity) entity, center, 
                forceField, forceConfig, normalizedTime, instance.age(), instance.maxLifeTicks());
        }
    }
    
    /**
     * Applies force to a single entity.
     */
    private void applyForceToEntity(net.minecraft.entity.LivingEntity entity, Vec3d center,
                                    net.cyberpunk042.field.force.field.ForceField forceField,
                                    net.cyberpunk042.field.force.ForceFieldConfig config,
                                    float normalizedTime, int ticksElapsed, int durationTicks) {
        // Create force context
        net.cyberpunk042.field.force.core.ForceContext context = 
            net.cyberpunk042.field.force.core.ForceContext.forEntity(
                entity, center, normalizedTime, ticksElapsed, durationTicks);
        
        // Check if in range
        double distance = context.distance();
        if (!forceField.affectsDistance(distance)) {
            // Restore entity state when leaving force field
            restoreEntityState(entity);
            return;
        }
        
        // Calculate force
        Vec3d force = forceField.calculateForce(context);
        if (force.lengthSquared() < 0.0001) {
            return;
        }
        
        // Log force application (once per second to avoid spam)
        if (ticksElapsed % 20 == 0) {
            Logging.REGISTRY.topic("force").debug(
                "Applying force to {} at dist={:.1f}: force=({:.3f},{:.3f},{:.3f}) time={:.1f}%", 
                entity.getName().getString(), distance, 
                force.x, force.y, force.z, normalizedTime * 100);
        }
        
        // Get current velocity - NO DAMPING (let gravity naturally decelerate)
        Vec3d currentVel = entity.getVelocity();
        
        // Add force directly
        Vec3d newVel = currentVel.add(force);
        
        // NO HARD VELOCITY CAP - gravity will naturally limit via orbital mechanics
        // The gravity floor in RadialForceField ensures entities can't escape
        
        // Only cap extreme values to prevent physics bugs
        double maxExtreme = 10.0;  // Minecraft physics breaks above this
        if (newVel.length() > maxExtreme) {
            newVel = newVel.normalize().multiply(maxExtreme);
        }
        
        // Apply
        entity.setVelocity(newVel);
        entity.velocityModified = true;
        entity.velocityDirty = true;
        
        // Make entity appear to float/fly rather than walk
        entity.fallDistance = 0.0f;
        
        // Disable gravity while in force field (entity is being controlled by force)
        if (!entity.hasNoGravity()) {
            entity.setNoGravity(true);
        }
    }
    
    /**
     * Called when entity leaves the force field - restore normal state.
     */
    private void restoreEntityState(net.minecraft.entity.LivingEntity entity) {
        if (entity.hasNoGravity()) {
            entity.setNoGravity(false);
        }
    }
    
    public void clear() {
        instances.clear();
        playerFields.clear();
    }
}
