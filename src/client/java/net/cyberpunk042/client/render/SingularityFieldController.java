package net.cyberpunk042.client.render;

import net.cyberpunk042.client.visual.ClientFieldManager;
import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldType;
import net.cyberpunk042.log.Logging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls singularity visual effects using the new field system.
 * 
 * <p>Manages the complex phased animation of singularity visuals:
 * <ul>
 *   <li><b>Primary sphere</b>: Grows, then shrinks</li>
 *   <li><b>Core sphere</b>: Appears during primary shrink, stays</li>
 *   <li><b>Beam</b>: Black initially, turns red when core appears</li>
 * </ul>
 * 
 * <p>Each component is a separate {@link ClientFieldState} managed by
 * {@link ClientFieldManager}.
 */
public final class SingularityFieldController {
    
    // Field definition IDs
    private static final Identifier DEF_PRIMARY = Identifier.of("the-virus-block", "singularity_primary");
    private static final Identifier DEF_CORE = Identifier.of("the-virus-block", "singularity_core");
    private static final Identifier DEF_BEAM_BLACK = Identifier.of("the-virus-block", "singularity_beam_black");
    private static final Identifier DEF_BEAM_RED = Identifier.of("the-virus-block", "singularity_beam_red");
    
    // Timing constants
    private static final int PRIMARY_GROW_TICKS = 60;
    private static final int PRIMARY_SHRINK_TICKS = 80;
    private static final int CORE_OVERLAP_TICKS = 20;
    private static final int CORE_GROW_TICKS = 40;
    private static final int BEAM_DELAY_TICKS = 10;
    
    // Scale constants
    private static final float PRIMARY_MIN_SCALE = 0.3f;
    private static final float PRIMARY_MAX_SCALE = 2.5f;
    private static final float CORE_MIN_SCALE = 0.1f;
    private static final float CORE_MAX_SCALE = 0.8f;
    
    // Active singularities
    private static final Map<BlockPos, SingularityState> ACTIVE = new ConcurrentHashMap<>();
    
    // ID generation
    private static long nextId = Long.MIN_VALUE / 2;
    
    private SingularityFieldController() {}
    
    /**
     * Adds a singularity at the given position.
     */
    public static void add(BlockPos pos) {
        BlockPos key = pos.toImmutable();
        if (ACTIVE.containsKey(key)) {
            return;
        }
        
        SingularityState state = new SingularityState(key);
        ACTIVE.put(key, state);
        
        // Create primary sphere field
        createPrimaryField(state);
        
        Logging.RENDER.topic("singularity").debug(
            "Added singularity at {}", pos);
    }
    
    /**
     * Removes a singularity at the given position.
     */
    public static void remove(BlockPos pos) {
        SingularityState state = ACTIVE.remove(pos);
        if (state != null) {
            // Remove all associated fields
            ClientFieldManager mgr = ClientFieldManager.get();
            if (state.primaryFieldId != 0) mgr.remove(state.primaryFieldId);
            if (state.coreFieldId != 0) mgr.remove(state.coreFieldId);
            if (state.beamFieldId != 0) mgr.remove(state.beamFieldId);
            
            Logging.RENDER.topic("singularity").debug(
                "Removed singularity at {}", pos);
        }
    }
    
    /**
     * Ticks all active singularities.
     * Call from client tick event.
     */
    public static void tick(MinecraftClient client) {
        if (client.isPaused()) {
            return;
        }
        
        for (SingularityState state : ACTIVE.values()) {
            state.tick(client);
        }
    }
    
    /**
     * Clears all singularities.
     */
    public static void clear() {
        ClientFieldManager mgr = ClientFieldManager.get();
        for (SingularityState state : ACTIVE.values()) {
            if (state.primaryFieldId != 0) mgr.remove(state.primaryFieldId);
            if (state.coreFieldId != 0) mgr.remove(state.coreFieldId);
            if (state.beamFieldId != 0) mgr.remove(state.beamFieldId);
        }
        ACTIVE.clear();
    }
    
    private static void createPrimaryField(SingularityState state) {
        state.primaryFieldId = nextId++;
        Vec3d pos = Vec3d.ofCenter(state.pos);
        
        ClientFieldState field = ClientFieldState.atPosition(
                state.primaryFieldId, DEF_PRIMARY, FieldType.SHIELD, pos)
            .withScale(PRIMARY_MIN_SCALE)
            .withPhase(state.phaseOffset);
        
        ClientFieldManager.get().addOrUpdate(field);
    }
    
    private static void createCoreField(SingularityState state) {
        state.coreFieldId = nextId++;
        Vec3d pos = Vec3d.ofCenter(state.pos);
        
        ClientFieldState field = ClientFieldState.atPosition(
                state.coreFieldId, DEF_CORE, FieldType.SHIELD, pos)
            .withScale(CORE_MIN_SCALE)
            .withPhase(state.phaseOffset);
        
        ClientFieldManager.get().addOrUpdate(field);
    }
    
    private static void createBeamField(SingularityState state, boolean red) {
        if (state.beamFieldId != 0) {
            ClientFieldManager.get().remove(state.beamFieldId);
        }
        
        state.beamFieldId = nextId++;
        Vec3d pos = Vec3d.ofCenter(state.pos).add(0, 0.5, 0);
        Identifier defId = red ? DEF_BEAM_RED : DEF_BEAM_BLACK;
        
        ClientFieldState field = ClientFieldState.atPosition(
                state.beamFieldId, defId, FieldType.SHIELD, pos)
            .withScale(1.0f);
        
        ClientFieldManager.get().addOrUpdate(field);
    }
    
    /**
     * State for a single singularity visual.
     */
    private static final class SingularityState {
        final BlockPos pos;
        final float phaseOffset;
        
        // Field IDs
        long primaryFieldId;
        long coreFieldId;
        long beamFieldId;
        
        // Phase tracking
        PrimaryPhase primaryPhase = PrimaryPhase.GROW;
        CorePhase corePhase = CorePhase.DORMANT;
        int primaryAge;
        int coreAge;
        int totalAge;
        
        boolean beamCreated;
        boolean coreCuePlayed;
        
        SingularityState(BlockPos pos) {
            this.pos = pos.toImmutable();
            int hash = Long.hashCode(pos.asLong());
            this.phaseOffset = (hash & 0xFFFF) / 65535.0f * MathHelper.TAU;
        }
        
        void tick(MinecraftClient client) {
            totalAge++;
            
            updatePrimary(client);
            updateCore();
            updateBeam();
        }
        
        void updatePrimary(MinecraftClient client) {
            ClientFieldState field = ClientFieldManager.get().get(primaryFieldId);
            if (field == null) return;
            
            switch (primaryPhase) {
                case GROW -> {
                    float progress = MathHelper.clamp(primaryAge / (float) PRIMARY_GROW_TICKS, 0, 1);
                    float scale = MathHelper.lerp(progress, PRIMARY_MIN_SCALE, PRIMARY_MAX_SCALE);
                    float alpha = MathHelper.lerp(progress, 0.5f, 0.95f);
                    
                    field.withScale(scale).withAlpha(alpha);
                    primaryAge++;
                    
                    if (primaryAge >= PRIMARY_GROW_TICKS) {
                        primaryPhase = PrimaryPhase.SHRINK;
                        primaryAge = 0;
                    }
                }
                case SHRINK -> {
                    // Start core when nearing end of shrink
                    int remaining = PRIMARY_SHRINK_TICKS - primaryAge;
                    if (remaining <= CORE_OVERLAP_TICKS && corePhase == CorePhase.DORMANT) {
                        startCore(client);
                    }
                    
                    float progress = MathHelper.clamp(primaryAge / (float) PRIMARY_SHRINK_TICKS, 0, 1);
                    float scale = MathHelper.lerp(progress, PRIMARY_MAX_SCALE, PRIMARY_MIN_SCALE);
                    float alpha = MathHelper.lerp(progress, 0.95f, 0.3f);
                    
                    field.withScale(scale).withAlpha(alpha);
                    primaryAge++;
                    
                    if (primaryAge >= PRIMARY_SHRINK_TICKS) {
                        primaryPhase = PrimaryPhase.INACTIVE;
                        field.withAlpha(0);
                    }
                }
                case INACTIVE -> {
                    if (corePhase == CorePhase.DORMANT) {
                        startCore(client);
                    }
                }
            }
        }
        
        void startCore(MinecraftClient client) {
            if (corePhase != CorePhase.DORMANT) return;
            
            corePhase = CorePhase.GROW;
            coreAge = 0;
            createCoreField(this);
            
            // Switch beam to red
            if (beamCreated) {
                createBeamField(this, true);
            }
            
            // Play sound
            if (!coreCuePlayed && client.world != null && client.player != null) {
                Vec3d pos = Vec3d.ofCenter(this.pos);
                client.world.playSound(client.player, pos.x, pos.y, pos.z,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 3.0f, 0.65f);
                coreCuePlayed = true;
            }
        }
        
        void updateCore() {
            if (coreFieldId == 0) return;
            ClientFieldState field = ClientFieldManager.get().get(coreFieldId);
            if (field == null) return;
            
            switch (corePhase) {
                case GROW -> {
                    float progress = MathHelper.clamp(coreAge / (float) CORE_GROW_TICKS, 0, 1);
                    float scale = MathHelper.lerp(progress, CORE_MIN_SCALE, CORE_MAX_SCALE);
                    float alpha = MathHelper.lerp(progress, 0.3f, 1.0f);
                    
                    field.withScale(scale).withAlpha(alpha);
                    coreAge++;
                    
                    if (coreAge >= CORE_GROW_TICKS) {
                        corePhase = CorePhase.IDLE;
                    }
                }
                case IDLE -> {
                    field.withScale(CORE_MAX_SCALE).withAlpha(1.0f);
                }
                case DORMANT -> {}
            }
        }
        
        void updateBeam() {
            // Create beam after delay
            if (!beamCreated && totalAge >= BEAM_DELAY_TICKS && primaryPhase != PrimaryPhase.INACTIVE) {
                boolean isRed = corePhase != CorePhase.DORMANT;
                createBeamField(this, isRed);
                beamCreated = true;
            }
        }
    }
    
    private enum PrimaryPhase {
        GROW, SHRINK, INACTIVE
    }
    
    private enum CorePhase {
        DORMANT, GROW, IDLE
    }
}

