package net.cyberpunk042.block.entity;


import net.cyberpunk042.log.Logging;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock.ShapeType;
import net.cyberpunk042.growth.profile.GrowthFieldProfile;
import net.cyberpunk042.growth.profile.GrowthExplosionProfile;
import net.cyberpunk042.growth.profile.GrowthForceProfile;
import net.cyberpunk042.growth.profile.GrowthFuseProfile;
import net.cyberpunk042.growth.profile.GrowthGlowProfile;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.growth.profile.GrowthOpacityProfile;
import net.cyberpunk042.growth.profile.GrowthParticleProfile;
import net.cyberpunk042.growth.scheduler.GrowthMutation;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.growth.profile.GrowthSpinProfile;
import net.cyberpunk042.growth.profile.GrowthWobbleProfile;
import net.cyberpunk042.infection.events.GrowthFuseEvent;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.registry.ModBlockEntities;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * Runtime state for {@link ProgressiveGrowthBlock}. Handles scale animation,
 * fuse timing, force fields, and touch damage. Rendering will read the synced
 * scale + profiles client-side.
 */
public class ProgressiveGrowthBlockEntity extends BlockEntity {
    private static final double MIN_SCALE_SPAN = 1.0E-4D;
    
    // Core state
    private Identifier definitionId = GrowthBlockDefinition.defaults().id();
    private double currentScale = GrowthBlockDefinition.defaults().startScale();
    private double previousScale = currentScale;
    private double targetScale = GrowthBlockDefinition.defaults().targetScale();
    private int scaleCooldown;
    private int pullCooldown;
    private int pushCooldown;
    private GrowthOverrides overrides = GrowthOverrides.empty();
    private boolean needsClientSync;
    private int lastLightLevel;
    
    // Fuse state
    private boolean fuseArmed;
    private int fuseTicks;
    private int collapseTicks;
    private int collapseDuration;
    private double collapseStartScale;
    private double collapseEndScale;
    private int remainingCharges = GrowthExplosionProfile.defaults().sanitizedCharges();
    
    // Wobble state
    private double wobbleOffsetX;
    private double wobbleOffsetY;
    private double wobbleOffsetZ;
    private boolean lastWobbleEnabled = false;
    
    // Shape state
    private VoxelShape outlineShape = VoxelShapes.fullCube();
    private VoxelShape collisionShape = VoxelShapes.empty();
    @Nullable
    private List<Box> collisionPanels = null;
    private Box renderBounds = new Box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private boolean loggedEmptyShape;
    private double lastLoggedCollisionScale = -1.0D;
    private long lastCollisionDebugTick = Long.MIN_VALUE;
    private boolean lastHasCollision = GrowthBlockDefinition.defaults().hasCollision();
    private boolean collisionTrackerRegistered;
    
    // Damage cooldowns
    private final Object2LongMap<UUID> touchCooldowns = new Object2LongOpenHashMap<>();
    private final Object2LongMap<UUID> forceDamageCooldowns = new Object2LongOpenHashMap<>();
    
    // Tracking
    private boolean lastGrowthEnabled = GrowthBlockDefinition.defaults().growthEnabled();
    
    // Delegated state
    private final GrowthExplosionHandler.BurstState burstState = new GrowthExplosionHandler.BurstState();
    private final GrowthParticleEmitter.EmitterState emitterState = new GrowthParticleEmitter.EmitterState();
    private final GrowthEventPublisher eventPublisher = new GrowthEventPublisher();

    public ProgressiveGrowthBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROGRESSIVE_GROWTH, pos, state);
        touchCooldowns.defaultReturnValue(0L);
        forceDamageCooldowns.defaultReturnValue(0L);
        this.previousScale = currentScale;
        GrowthBlockDefinition definition = resolveDefinition();
        resetTransientState(definition);
        rebuildShapes(definition);
        refreshCollisionTracker(definition.hasCollision());
        if (state.contains(ProgressiveGrowthBlock.LIGHT_LEVEL)) {
            lastLightLevel = state.get(ProgressiveGrowthBlock.LIGHT_LEVEL);
        } else {
            lastLightLevel = 0;
        }
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, ProgressiveGrowthBlockEntity entity) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        entity.tickServer(serverWorld);
    }

    public static void clientTick(World world, BlockPos pos, BlockState state, ProgressiveGrowthBlockEntity entity) {
        entity.tickClient();
    }

    private void tickServer(ServerWorld world) {
        GrowthBlockDefinition definition = resolveDefinition();
        GrowthGlowProfile glowProfile = resolveGlowProfile();
        updateBlockLightLevel(glowProfile.lightLevel());
        boolean dirty = false;
        boolean shapeDirty = false;
        boolean growthEnabled = definition.growthEnabled();
        boolean allowScaleTick = growthEnabled || fuseArmed;
        boolean hasCollision = definition.hasCollision();
        refreshCollisionTracker(hasCollision);

        if (!growthEnabled && lastGrowthEnabled && !fuseArmed) {
            previousScale = currentScale;
            scaleCooldown = definition.sanitizedRate();
        }

        if (allowScaleTick) {
            if (scaleCooldown-- <= 0) {
                previousScale = currentScale;
                double before = currentScale;
                stepScaleTowardsTarget(definition);
                scaleCooldown = definition.sanitizedRate();
                if (Math.abs(before - currentScale) > 1.0E-4) {
                    dirty = true;
                    shapeDirty = true;
                }
            }
        } else {
            scaleCooldown = definition.sanitizedRate();
        }

        GrowthFuseProfile fuseProfile = resolveFuseProfile();
        if (!definition.hasFuse()) {
            if (fuseArmed || burstState.isActive()) {
                disarmFuse();
                dirty = true;
            }
        } else if (!fuseArmed && !burstState.isActive() && remainingCharges > 0
                && fuseProfile.trigger() == GrowthFuseProfile.Trigger.AUTO
                && shouldAutoArm(definition, fuseProfile)) {
            armFuse(world, definition, fuseProfile);
            dirty = true;
        }

        FuseResult fuseResult = fuseArmed ? tickFuse(world, definition, fuseProfile) : FuseResult.NONE;
        if (fuseResult.removed()) {
            return;
        }
        if (fuseResult.dirty()) {
            dirty = true;
        }
        if (fuseResult.shapeDirty()) {
            shapeDirty = true;
        }

        GrowthForceProfile pullProfile = resolveForceProfile(definition.pullProfileId(), false);
        GrowthForceProfile pushProfile = resolveForceProfile(definition.pushProfileId(), true);

        if (growthEnabled && GrowthForceHandler.isForceActive(pullProfile)) {
            if (pullCooldown-- <= 0) {
                GrowthForceHandler.applyForce(world, pos, definition, pullProfile, true,
                        growthProgress(definition), forceDamageCooldowns, eventPublisher);
                pullCooldown = pullProfile.sanitizedInterval();
            }
        } else {
            pullCooldown = Math.max(0, Math.min(pullCooldown, definition.sanitizedRate()));
        }

        if (growthEnabled && GrowthForceHandler.isForceActive(pushProfile)) {
            if (pushCooldown-- <= 0) {
                GrowthForceHandler.applyForce(world, pos, definition, pushProfile, false,
                        growthProgress(definition), forceDamageCooldowns, eventPublisher);
                pushCooldown = pushProfile.sanitizedInterval();
            }
        } else {
            pushCooldown = Math.max(0, Math.min(pushCooldown, definition.sanitizedRate()));
        }

        GrowthWobbleProfile wobbleProfile = resolveWobbleProfile(definition.wobbleProfileId());
        boolean wobbleEnabled = wobbleProfile.enabled();
        if (wobbleEnabled != lastWobbleEnabled) {
            wobbleOffsetX = 0.0D;
            wobbleOffsetY = 0.0D;
            wobbleOffsetZ = 0.0D;
            shapeDirty = true;
            lastWobbleEnabled = wobbleEnabled;
        }

        if (hasCollision != lastHasCollision) {
            shapeDirty = true;
            lastHasCollision = hasCollision;
        }

        if (updateWobble(wobbleProfile, world.getTime())) {
            dirty = true;
            shapeDirty = true;
        }

        if (shapeDirty) {
            rebuildShapes(definition);
        }

        applyVolumeTouchDamage(world, definition);
        if (definition.doesDestruction()) {
            destroyIntersectingBlocks(world, definition);
        }
        
        GrowthParticleEmitter.emitAmbientParticles(world, pos, resolveParticleProfile(), currentScale, emitterState);
        
        int[] chargesRef = { remainingCharges };
        if (GrowthExplosionHandler.tickPendingBursts(world, pos, burstState, chargesRef, this::markDirty, () -> { scheduleClientSync(); sync(); })) {
            remainingCharges = chargesRef[0];
            return;
        }
        remainingCharges = chargesRef[0];

        lastGrowthEnabled = growthEnabled;

        if (dirty) {
            markDirty();
            sync();
        }
    }

    private void tickClient() {
        GrowthBlockDefinition definition = resolveDefinition();
        GrowthWobbleProfile wobbleProfile = resolveWobbleProfile(definition.wobbleProfileId());
        previousScale = currentScale;
        double before = currentScale;
        stepScaleTowardsTarget(definition);
        boolean shapeDirty = Math.abs(before - currentScale) > 1.0E-4;
        boolean hasCollision = definition.hasCollision();
        if (hasCollision != lastHasCollision) {
            lastHasCollision = hasCollision;
            shapeDirty = true;
        }
        if (updateWobble(wobbleProfile, world != null ? world.getTime() : 0L)) {
            shapeDirty = true;
        }
        if (shapeDirty) {
            rebuildShapes(definition);
        }
    }

    private void stepScaleTowardsTarget(GrowthBlockDefinition definition) {
        double min = definition.minScale();
        double max = sanitizedMaxScale(definition);
        double diff = targetScale - currentScale;
        if (Math.abs(diff) < 1.0E-4) {
            currentScale = MathHelper.clamp(currentScale, min, max);
            return;
        }
        double maxStep = 0.05D * definition.clampedRateScale();
        double step = Math.copySign(Math.min(Math.abs(diff), maxStep), diff);
        currentScale = MathHelper.clamp(currentScale + step, min, max);
    }

    // === Fuse Methods ===

    private void armFuse(ServerWorld world, GrowthBlockDefinition definition, GrowthFuseProfile profile) {
        fuseArmed = true;
        fuseTicks = profile.sanitizedExplosionDelay();
        collapseDuration = profile.sanitizedShellCollapse();
        collapseTicks = collapseDuration;
        collapseStartScale = currentScale;
        collapseEndScale = profile.collapseTargetScaleOrDefault(definition);
        eventPublisher.postFuseEvent(world, pos, GrowthFuseEvent.Stage.ARMED, fuseTicks, profile);
    }

    private FuseResult tickFuse(ServerWorld world, GrowthBlockDefinition definition, GrowthFuseProfile profile) {
        if (!fuseArmed) {
            return FuseResult.NONE;
        }
        if (fuseTicks-- <= 0) {
            boolean removed = triggerDetonation(world, definition);
            return new FuseResult(true, true, removed);
        }
        boolean shapeDirty = false;
        if (fuseTicks % Math.max(1, profile.sanitizedPulseInterval()) == 0) {
            emitFusePulse(world, profile);
        }
        if (collapseTicks > 0 && collapseDuration > 0) {
            double fraction = 1.0D - (double) collapseTicks / (double) collapseDuration;
            double collapseTarget = MathHelper.lerp(fraction, collapseStartScale, collapseEndScale);
            targetScale = Math.min(targetScale, collapseTarget);
            collapseTicks--;
            shapeDirty = true;
        }
        return new FuseResult(true, shapeDirty, false);
    }

    private boolean triggerDetonation(ServerWorld world, GrowthBlockDefinition definition) {
        fuseArmed = false;
        fuseTicks = 0;
        collapseTicks = 0;
        collapseDuration = 0;
        GrowthExplosionHandler.startBurstSequence(burstState, definition, resolveExplosionProfile());
        eventPublisher.postFuseEvent(world, pos, GrowthFuseEvent.Stage.DETONATED, 0, resolveFuseProfile());
        int[] chargesRef = { remainingCharges };
        boolean removed = GrowthExplosionHandler.tickPendingBursts(world, pos, burstState, chargesRef, this::markDirty, () -> { scheduleClientSync(); sync(); });
        remainingCharges = chargesRef[0];
        return removed;
    }

    private void emitFusePulse(ServerWorld world, GrowthFuseProfile profile) {
        ParticleEffect effect = GrowthParticleEmitter.resolveParticle(profile.particleId());
        if (effect != null) {
            world.spawnParticles(effect, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 4, 0.15D, 0.15D, 0.15D, 0.01D);
        }
        GrowthForceHandler.playSound(world, pos, profile.soundId(), 0.7F, 1.0F);
        eventPublisher.postFuseEvent(world, pos, GrowthFuseEvent.Stage.PULSE, fuseTicks, profile);
    }

    private void disarmFuse() {
        if (!fuseArmed) {
            burstState.clear();
            return;
        }
        fuseArmed = false;
        fuseTicks = 0;
        collapseTicks = 0;
        collapseDuration = 0;
        collapseStartScale = currentScale;
        collapseEndScale = currentScale;
        burstState.clear();
    }

    private boolean shouldAutoArm(GrowthBlockDefinition definition, GrowthFuseProfile profile) {
        if (remainingCharges <= 0 || burstState.isActive() || profile.trigger() != GrowthFuseProfile.Trigger.AUTO) {
            return false;
        }
        return growthProgress(definition) >= profile.clampedAutoProgress();
    }

    // === Wobble Methods ===

    private boolean updateWobble(GrowthWobbleProfile wobble, long time) {
        if (wobble == null) {
            return false;
        }
        Vec3d sample = sampleWobble(time, wobble);
        boolean changed = Math.abs(sample.x - wobbleOffsetX) > 1.0E-4
                || Math.abs(sample.y - wobbleOffsetY) > 1.0E-4
                || Math.abs(sample.z - wobbleOffsetZ) > 1.0E-4;
        if (changed) {
            wobbleOffsetX = sample.x;
            wobbleOffsetY = sample.y;
            wobbleOffsetZ = sample.z;
        }
        return changed;
    }

    private Vec3d sampleWobble(double time, GrowthWobbleProfile wobble) {
        if (!wobble.enabled()) {
            return Vec3d.ZERO;
        }
        double scaleFactor = MathHelper.clamp(currentScale, 0.2D, 1.4D);
        double amplitudeScale = Math.max(0.35D, 1.1D - scaleFactor * 0.35D);
        double x = Math.sin(time * wobble.clampedSpeedX()) * wobble.clampedAmplitudeX() * amplitudeScale;
        double z = Math.cos(time * wobble.clampedSpeedZ()) * wobble.clampedAmplitudeZ() * amplitudeScale;
        double y = Math.sin(time * wobble.clampedSpeedY()) * wobble.clampedAmplitudeY() * amplitudeScale;
        return new Vec3d(x, y, z);
    }

    public Vec3d getRenderWobble(GrowthBlockDefinition definition, float tickDelta) {
        if (world == null) {
            return Vec3d.ZERO;
        }
        GrowthWobbleProfile wobble = resolveWobbleProfile(definition.wobbleProfileId());
        if (!wobble.enabled()) {
            return Vec3d.ZERO;
        }
        double time = world.getTime() + tickDelta;
        return sampleWobble(time, wobble);
    }

    // === Shape Methods (delegated) ===

    private void rebuildShapes(GrowthBlockDefinition definition) {
        double clampedWobbleX = wobbleOffsetX;
        double clampedWobbleZ = wobbleOffsetZ;
        double minScale = Math.max(0.05D, definition.minScale());
        double maxScale = Math.max(minScale, sanitizedMaxScale(definition));
        double clamped = MathHelper.clamp(currentScale, minScale, maxScale);
        if (clamped >= 1.0D) {
            clampedWobbleX = 0.0D;
            clampedWobbleZ = 0.0D;
            wobbleOffsetX = 0.0D;
            wobbleOffsetZ = 0.0D;
        }
        
        GrowthShapeBuilder.ShapeResult result = GrowthShapeBuilder.build(
                definition, currentScale, clampedWobbleX, wobbleOffsetY, clampedWobbleZ, pos);
        
        this.outlineShape = result.outlineShape();
        this.collisionShape = result.collisionShape();
        this.collisionPanels = result.collisionPanels();
        this.renderBounds = result.renderBounds();
        
        GrowthShapeBuilder.debugCollisionShape(world, pos, definition, clamped, lastLoggedCollisionScale,
                outlineShape, collisionShape, renderBounds, collisionPanels, lastCollisionDebugTick);
        lastLoggedCollisionScale = clamped;
        if (world instanceof ServerWorld) {
            lastCollisionDebugTick = world.getTime();
        }
        
        if (!loggedEmptyShape && outlineShape.isEmpty()) {
            loggedEmptyShape = true;
            Logging.GROWTH.warn("[GrowthEntity] Outline shape empty for {} at {}", definition.id(), pos);
        }
    }

    // === Damage Methods ===

    private void applyVolumeTouchDamage(ServerWorld world, GrowthBlockDefinition definition) {
        double damage = definition.touchDamage();
        if (damage <= 0.0D) {
            return;
        }
        Box damageBox = outlineShape.getBoundingBox().offset(pos);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, damageBox, LivingEntity::isAlive);
        if (targets.isEmpty()) {
            return;
        }
        for (LivingEntity living : targets) {
            damageLivingEntity(world, living, definition);
        }
    }

    private void destroyIntersectingBlocks(ServerWorld world, GrowthBlockDefinition definition) {
        Box box = outlineShape.getBoundingBox().offset(pos);
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.ceil(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.ceil(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.ceil(box.maxZ);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    if (mutable.equals(pos)) continue;
                    BlockState state = world.getBlockState(mutable);
                    if (state.isAir()) continue;
                    if (state.getHardness(world, mutable) < 0.0F) continue;
                    world.breakBlock(mutable, false);
                }
            }
        }
    }

    private void damageLivingEntity(ServerWorld world, LivingEntity living, GrowthBlockDefinition definition) {
        double damage = definition.touchDamage();
        if (damage <= 0.0D) return;
        long now = world.getTime();
        long ready = touchCooldowns.getOrDefault(living.getUuid(), 0L);
        if (now < ready) return;
        touchCooldowns.put(living.getUuid(), now + 10L);
        living.damage(world, world.getDamageSources().magic(), (float) damage);
    }

    // === Manual Fuse Methods ===

    private boolean matchesManualTrigger(GrowthFuseProfile profile, ManualFuseCause cause) {
        return switch (profile.trigger()) {
            case RIGHT_CLICK, ITEM_USE -> cause == ManualFuseCause.INTERACT;
            case ATTACK -> cause == ManualFuseCause.ATTACK;
            default -> false;
        };
    }

    private boolean validateArmItem(GrowthFuseProfile profile, ItemStack stack) {
        boolean needsItem = profile.requiresItem() || profile.trigger() == GrowthFuseProfile.Trigger.ITEM_USE;
        if (!needsItem) return true;
        if (stack.isEmpty()) return false;
        if (profile.allowedItems().isEmpty()) return true;
        Identifier heldId = Registries.ITEM.getId(stack.getItem());
        return profile.allowedItems().contains(heldId);
    }

    private void consumeArmItem(PlayerEntity player, ItemStack stack, GrowthFuseProfile profile) {
        boolean needsItem = profile.requiresItem() || profile.trigger() == GrowthFuseProfile.Trigger.ITEM_USE;
        if (!needsItem || stack.isEmpty()) return;
        if (profile.consumeItem()) {
            stack.decrement(1);
        } else if (stack.isDamageable()) {
            stack.damage(1, player);
        }
    }

    public void onEntityCollision(Entity entity) {
        if (!(entity instanceof LivingEntity living) || world == null || world.isClient) return;
        GrowthBlockDefinition definition = resolveDefinition();
        damageLivingEntity((ServerWorld) world, living, definition);
    }

    public boolean canManualFusePreview(ManualFuseCause cause, ItemStack stack) {
        GrowthBlockDefinition definition = resolveDefinition();
        if (!definition.hasFuse() || fuseArmed || remainingCharges <= 0 || burstState.isActive()) return false;
        GrowthFuseProfile profile = resolveFuseProfile();
        return matchesManualTrigger(profile, cause) && validateArmItem(profile, stack);
    }

    public boolean handleManualFuse(ServerWorld world, PlayerEntity player, Hand hand, ManualFuseCause cause) {
        GrowthBlockDefinition definition = resolveDefinition();
        if (!definition.hasFuse() || fuseArmed || remainingCharges <= 0 || burstState.isActive()) return false;
        GrowthFuseProfile profile = resolveFuseProfile();
        if (!matchesManualTrigger(profile, cause)) return false;
        ItemStack stack = hand != null ? player.getStackInHand(hand) : ItemStack.EMPTY;
        if (!validateArmItem(profile, stack)) return false;
        consumeArmItem(player, stack, profile);
        armFuse(world, definition, profile);
        markDirty();
        scheduleClientSync();
        sync();
        return true;
    }

    // === Shape Accessors ===

    public VoxelShape shape(ShapeType type) {
        return switch (type) {
            case COLLISION -> collisionShape == null ? VoxelShapes.empty() : collisionShape;
            default -> outlineShape == null ? VoxelShapes.empty() : outlineShape;
        };
    }

    public VoxelShape worldShape(ShapeType type) {
        VoxelShape local = shape(type);
        return local.isEmpty() ? local : local.offset(pos.getX(), pos.getY(), pos.getZ());
    }

    // === Collision Tracker ===

    private void refreshCollisionTracker(boolean hasCollision) {
        if (!(world instanceof ServerWorld)) return;
        if (hasCollision && !collisionTrackerRegistered) {
            GrowthCollisionTracker.register(this);
            collisionTrackerRegistered = true;
        } else if (!hasCollision && collisionTrackerRegistered) {
            GrowthCollisionTracker.unregister(this);
            collisionTrackerRegistered = false;
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (collisionTrackerRegistered) {
            GrowthCollisionTracker.unregister(this);
            collisionTrackerRegistered = false;
        }
    }

    // === Accessors ===

    public Identifier getDefinitionId() { return definitionId; }
    @Nullable public List<Box> getCollisionPanels() { return collisionPanels; }
    public Box getRenderBounds() { return renderBounds; }
    public Box getInteractionBounds() { return outlineShape.isEmpty() ? Box.of(Vec3d.ofCenter(pos), 0.0D, 0.0D, 0.0D) : outlineShape.getBoundingBox(); }
    public Box getWorldRenderBounds() { return renderBounds.offset(pos); }
    public boolean hasCollision() { return lastHasCollision; }
    public boolean isFuseArmed() { return fuseArmed; }
    public int getFuseTicks() { return fuseTicks; }
    public float getRenderScale(float tickDelta) { return (float) MathHelper.lerp(tickDelta, previousScale, currentScale); }
    public GrowthBlockDefinition definitionSnapshot() { return resolveDefinition(); }

    public void forceRebuildCollisionShape() {
        GrowthBlockDefinition definition = resolveDefinition();
        rebuildShapes(definition);
    }

    public void setDefinitionId(Identifier id) {
        if (id == null || id.equals(this.definitionId)) return;
        this.definitionId = id;
        GrowthBlockDefinition definition = resolveDefinition();
        resetTransientState(definition);
        rebuildShapes(definition);
        markDirty();
        scheduleClientSync();
        sync();
    }

    public void replaceOverrides(GrowthOverrides newOverrides) {
        this.overrides = newOverrides == null ? GrowthOverrides.empty() : newOverrides;
        GrowthBlockDefinition definition = resolveDefinition();
        rebuildShapes(definition);
        lastGrowthEnabled = definition.growthEnabled();
        lastWobbleEnabled = resolveWobbleProfile(definition.wobbleProfileId()).enabled();
        lastHasCollision = definition.hasCollision();
        markDirty();
        scheduleClientSync();
        sync();
    }

    public GrowthOverrides overridesSnapshot() {
        if (overrides == null || overrides.isEmpty()) return GrowthOverrides.empty();
        return GrowthOverrides.fromNbt(overrides.toNbt());
    }

    public boolean applyMutation(GrowthMutation mutation) {
        if (mutation == null || mutation.isEmpty()) return false;
        if (overrides == null) overrides = GrowthOverrides.empty();
        boolean changed = overrides.applyMutation(mutation);
        if (!changed) return false;
        GrowthBlockDefinition definition = resolveDefinition();
        rebuildShapes(definition);
        lastGrowthEnabled = definition.growthEnabled();
        lastWobbleEnabled = resolveWobbleProfile(definition.wobbleProfileId()).enabled();
        lastHasCollision = definition.hasCollision();
        markDirty();
        scheduleClientSync();
        sync();
        return true;
    }

    // === Profile Resolution ===

    private GrowthBlockDefinition resolveDefinition() {
        GrowthRegistry registry = registry();
        GrowthBlockDefinition definition = registry == null ? GrowthBlockDefinition.defaults() : registry.definition(definitionId);
        if (overrides != null && !overrides.isEmpty()) {
            definition = overrides.apply(definition, registry);
        }
        double min = definition.minScale();
        double max = sanitizedMaxScale(definition);
        double baseTarget = MathHelper.clamp(definition.targetScale(), min, max);
        boolean canResetTarget = !fuseArmed && collapseTicks <= 0 && !burstState.isActive();
        targetScale = canResetTarget ? baseTarget : MathHelper.clamp(targetScale, min, max);
        currentScale = MathHelper.clamp(currentScale, min, max);
        return definition;
    }

    private double growthProgress(GrowthBlockDefinition definition) {
        double min = definition.minScale();
        double max = sanitizedMaxScale(definition);
        double span = Math.max(MIN_SCALE_SPAN, max - min);
        return MathHelper.clamp((currentScale - min) / span, 0.0D, 1.0D);
    }

    private GrowthForceProfile resolveForceProfile(@Nullable Identifier id, boolean push) {
        GrowthRegistry registry = registry();
        GrowthForceProfile fallback = push ? GrowthForceProfile.defaultsPush() : GrowthForceProfile.defaultsPull();
        return registry == null ? fallback : registry.forceProfile(id, fallback);
    }

    public GrowthFieldProfile resolveFieldProfile() {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthFieldProfile.defaults() : registry.fieldProfile(resolveDefinition().fieldProfileId());
    }

    public GrowthFuseProfile resolveFuseProfile() {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthFuseProfile.defaults() : registry.fuseProfile(resolveDefinition().fuseProfileId());
    }

    public GrowthExplosionProfile resolveExplosionProfile() {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthExplosionProfile.defaults() : registry.explosionProfile(resolveDefinition().explosionProfileId());
    }

    public GrowthGlowProfile resolveGlowProfile() {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthGlowProfile.defaults() : registry.glowProfile(resolveDefinition().glowProfileId());
    }

    private void updateBlockLightLevel(int desiredLevel) {
        int clamped = MathHelper.clamp(desiredLevel, 0, 15);
        if (clamped == lastLightLevel) return;
        if (world == null) { lastLightLevel = clamped; return; }
        BlockState state = getCachedState();
        if (!state.contains(ProgressiveGrowthBlock.LIGHT_LEVEL)) { lastLightLevel = clamped; return; }
        if (state.get(ProgressiveGrowthBlock.LIGHT_LEVEL) == clamped) { lastLightLevel = clamped; return; }
        world.setBlockState(pos, state.with(ProgressiveGrowthBlock.LIGHT_LEVEL, clamped), Block.NOTIFY_LISTENERS);
        lastLightLevel = clamped;
    }

    public GrowthOpacityProfile resolveOpacityProfile() {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthOpacityProfile.defaults() : registry.opacityProfile(resolveDefinition().opacityProfileId());
    }

    public GrowthSpinProfile resolveSpinProfile() {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthSpinProfile.defaults() : registry.spinProfile(resolveDefinition().spinProfileId());
    }

    public GrowthWobbleProfile resolveWobbleProfile() {
        return resolveWobbleProfile(resolveDefinition().wobbleProfileId());
    }

    private GrowthWobbleProfile resolveWobbleProfile(@Nullable Identifier id) {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthWobbleProfile.none() : registry.wobbleProfile(id);
    }

    private GrowthParticleProfile resolveParticleProfile() {
        GrowthRegistry registry = registry();
        return registry == null ? GrowthParticleProfile.defaults() : registry.particleProfile(resolveDefinition().particleProfileId());
    }

    @Nullable
    private GrowthRegistry registry() {
        InfectionServiceContainer c = InfectionServices.container();
        return c != null ? c.growth() : null;
    }

    private double sanitizedMaxScale(GrowthBlockDefinition definition) {
        double min = definition.minScale();
        double max = definition.maxScale();
        return (max - min < MIN_SCALE_SPAN) ? min + MIN_SCALE_SPAN : max;
    }

    private void resetTransientState(GrowthBlockDefinition definition) {
        double min = definition.minScale();
        double max = sanitizedMaxScale(definition);
        currentScale = MathHelper.clamp(currentScale, min, max);
        previousScale = currentScale;
        targetScale = MathHelper.clamp(targetScale, min, max);
        scaleCooldown = definition.sanitizedRate();
        pullCooldown = 0;
        pushCooldown = 0;
        emitterState.ambientCooldown = 0;
        emitterState.ambientSoundCooldown = 0;
        wobbleOffsetX = 0.0D;
        wobbleOffsetY = 0.0D;
        wobbleOffsetZ = 0.0D;
        disarmFuse();
        forceDamageCooldowns.clear();
        lastGrowthEnabled = definition.growthEnabled();
        lastWobbleEnabled = resolveWobbleProfile(definition.wobbleProfileId()).enabled();
        lastHasCollision = definition.hasCollision();
        collapseEndScale = currentScale;
        remainingCharges = resolveExplosionProfile().sanitizedCharges();
        burstState.clear();
    }

    // === Records/Enums ===

    private record FuseResult(boolean dirty, boolean shapeDirty, boolean removed) {
        static final FuseResult NONE = new FuseResult(false, false, false);
    }

    public enum ManualFuseCause { INTERACT, ATTACK }

    // === NBT ===

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putString("DefinitionId", definitionId.toString());
        view.putDouble("CurrentScale", currentScale);
        view.putDouble("TargetScale", targetScale);
        view.putBoolean("FuseArmed", fuseArmed);
        view.putInt("FuseTicks", fuseTicks);
        view.putInt("CollapseTicks", collapseTicks);
        view.putInt("CollapseDuration", collapseDuration);
        view.putDouble("CollapseStartScale", collapseStartScale);
        view.putDouble("CollapseEndScale", collapseEndScale);
        view.putInt("ScaleCooldown", scaleCooldown);
        view.putInt("PullCooldown", pullCooldown);
        view.putInt("PushCooldown", pushCooldown);
        view.putInt("AmbientCooldown", emitterState.ambientCooldown);
        view.putInt("AmbientSoundCooldown", emitterState.ambientSoundCooldown);
        view.putDouble("WobbleOffsetX", wobbleOffsetX);
        view.putDouble("WobbleOffsetY", wobbleOffsetY);
        view.putDouble("WobbleOffsetZ", wobbleOffsetZ);
        view.putInt("RemainingCharges", remainingCharges);
        view.putInt("BurstExplosionsRemaining", burstState.burstExplosionsRemaining);
        view.putInt("BurstDelayTicks", burstState.burstDelayTicks);
        view.putInt("BurstDelayInterval", burstState.burstDelayInterval);
        view.putFloat("BurstRadius", burstState.burstRadius);
        view.putBoolean("BurstCausesFire", burstState.burstCausesFire);
        view.putBoolean("BurstBreaksBlocks", burstState.burstBreaksBlocks);
        view.putDouble("BurstMaxDamage", burstState.burstMaxDamage);
        view.putDouble("BurstDamageScaling", burstState.burstDamageScaling);
        view.putString("Overrides", overrides.toSnbt());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.definitionId = Identifier.tryParse(view.getString("DefinitionId", definitionId.toString()));
        this.currentScale = view.getDouble("CurrentScale", currentScale);
        this.targetScale = view.getDouble("TargetScale", targetScale);
        this.fuseArmed = view.getBoolean("FuseArmed", false);
        this.fuseTicks = view.getInt("FuseTicks", 0);
        this.collapseTicks = view.getInt("CollapseTicks", 0);
        this.collapseDuration = view.getInt("CollapseDuration", 0);
        this.collapseStartScale = view.getDouble("CollapseStartScale", currentScale);
        this.collapseEndScale = view.getDouble("CollapseEndScale", currentScale);
        this.emitterState.ambientSoundCooldown = view.getInt("AmbientSoundCooldown", 0);
        this.wobbleOffsetX = view.getDouble("WobbleOffsetX", 0.0D);
        this.wobbleOffsetY = view.getDouble("WobbleOffsetY", 0.0D);
        this.wobbleOffsetZ = view.getDouble("WobbleOffsetZ", 0.0D);
        int profileCharges = resolveExplosionProfile().sanitizedCharges();
        this.remainingCharges = MathHelper.clamp(view.getInt("RemainingCharges", profileCharges), 0, profileCharges);
        this.burstState.burstExplosionsRemaining = view.getInt("BurstExplosionsRemaining", 0);
        this.burstState.burstDelayTicks = view.getInt("BurstDelayTicks", 0);
        this.burstState.burstDelayInterval = view.getInt("BurstDelayInterval", 0);
        this.burstState.burstRadius = view.getFloat("BurstRadius", 0.0F);
        this.burstState.burstCausesFire = view.getBoolean("BurstCausesFire", false);
        this.burstState.burstBreaksBlocks = view.getBoolean("BurstBreaksBlocks", false);
        this.burstState.burstMaxDamage = view.getDouble("BurstMaxDamage", 0.0D);
        this.burstState.burstDamageScaling = view.getDouble("BurstDamageScaling", 1.0D);
        String overridesRaw = view.getString("Overrides", "");
        this.overrides = GrowthOverrides.fromSnbt(overridesRaw);
        GrowthBlockDefinition definition = resolveDefinition();
        this.scaleCooldown = view.getInt("ScaleCooldown", definition.sanitizedRate());
        this.pullCooldown = view.getInt("PullCooldown", 0);
        this.pushCooldown = view.getInt("PushCooldown", 0);
        this.emitterState.ambientCooldown = view.getInt("AmbientCooldown", 0);
        lastGrowthEnabled = definition.growthEnabled();
        lastWobbleEnabled = resolveWobbleProfile(definition.wobbleProfileId()).enabled();
        lastHasCollision = definition.hasCollision();
        rebuildShapes(definition);
    }

    private void sync() {
        if (!(world instanceof ServerWorld serverWorld) || !needsClientSync) return;
        needsClientSync = false;
        BlockEntityUpdateS2CPacket packet = BlockEntityUpdateS2CPacket.create(this);
        for (ServerPlayerEntity player : PlayerLookup.tracking(this)) {
            player.networkHandler.sendPacket(packet);
        }
        if (Logging.GROWTH.is(net.cyberpunk042.log.LogLevel.DEBUG)) {
            Logging.GROWTH.debug("[GrowthSync] {} sending def={} scale={} fuse={} target={}",
                    pos, definitionId, String.format("%.3f", currentScale), fuseArmed, String.format("%.3f", targetScale));
        }
        serverWorld.getChunkManager().markForUpdate(pos);
        serverWorld.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    private void scheduleClientSync() {
        this.needsClientSync = true;
    }
    
    public void snapToTargetScale() {
        GrowthBlockDefinition definition = resolveDefinition();
        double min = definition.minScale();
        double max = sanitizedMaxScale(definition);
        double clamped = MathHelper.clamp(definition.targetScale(), min, max);
        this.currentScale = clamped;
        this.previousScale = clamped;
        this.targetScale = clamped;
        this.scaleCooldown = 0;
    }
}
