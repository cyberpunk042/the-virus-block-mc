package net.cyberpunk042.client.visual;

import net.cyberpunk042.client.field.render.FieldRenderer;
import net.cyberpunk042.client.field.render.RenderOverrides;

import net.cyberpunk042.field.ClientFieldState;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldRegistry;
import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side manager for field rendering and state.
 * 
 * <p>Receives field state from server via {@link net.cyberpunk042.network.FieldSpawnPayload}
 * and handles:
 * <ul>
 *   <li>Storing synced field states</li>
 *   <li>Rendering all active fields</li>
 *   <li>Managing personal field tracker</li>
 * </ul>
 * 
 * <p>Hook into WorldRenderEvents.AFTER_ENTITIES for rendering.
 */
public final class ClientFieldManager {
    
    private static final ClientFieldManager INSTANCE = new ClientFieldManager();
    
    private final Map<Long, ClientFieldState> states = new ConcurrentHashMap<>();
    private final PersonalFieldTracker personalTracker = new PersonalFieldTracker();
    
    private ClientFieldManager() {}
    
    public static ClientFieldManager get() {
        return INSTANCE;
    }
    
    // =========================================================================
    // State management (called from network handlers)
    // =========================================================================
    
    /**
     * Adds or updates a field state (from server sync).
     */
    public void addOrUpdate(ClientFieldState state) {
        boolean isNew = !states.containsKey(state.id());
        states.put(state.id(), state);
        if (isNew) {
            Logging.RENDER.topic("field").debug(
                "Client added field {} at {}", state.id(), state.position());
        } else {
            Logging.RENDER.topic("field").trace(
                "Client updated field {} at {}", state.id(), state.position());
        }
    }
    
    /**
     * Gets a field state by ID.
     * @return the state or null if not found
     */
    public ClientFieldState get(long id) {
        return states.get(id);
    }
    
    /**
     * Removes a field state.
     */
    public void remove(long id) {
        ClientFieldState removed = states.remove(id);
        if (removed != null) {
            Logging.RENDER.topic("field").debug("Client removed field {}", id);
        }
    }
    
    /**
     * Clears all states (on disconnect).
     */
    public void clear() {
        int count = states.size();
        states.clear();
        personalTracker.setEnabled(false);
        if (count > 0) {
            Logging.RENDER.topic("field").info("Client cleared {} field states", count);
        }
    }
    
    /**
     * @return the number of active field states
     */
    public int count() {
        return states.size();
    }
    
    /**
     * @return all active field states (unmodifiable view)
     */
    public java.util.Collection<ClientFieldState> allStates() {
        return java.util.Collections.unmodifiableCollection(states.values());
    }
    
    // =========================================================================
    // Personal field
    // =========================================================================
    
    public PersonalFieldTracker personalTracker() {
        return personalTracker;
    }
    
    public void enablePersonalField(Identifier definitionId, float scale) {
        personalTracker.setDefinition(definitionId);
        personalTracker.setScale(scale);
        personalTracker.setEnabled(true);
        personalTracker.reset();
        Logging.RENDER.topic("field").info("Personal field enabled: {}", definitionId);
    }
    
    public void disablePersonalField() {
        personalTracker.setEnabled(false);
        Logging.RENDER.topic("field").info("Personal field disabled");
    }
    
    // =========================================================================
    // Tick
    // =========================================================================
    
    /**
     * Ticks all client-side state.
     * Call from client tick event.
     */
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Apply follow mode to test fields that should follow the player
            for (ClientFieldState state : states.values()) {
                tickFollowMode(state, client.player);
            }
        }
        
        states.values().removeIf(ClientFieldState::tick);
        if (client.player != null) {
            personalTracker.tick(client.player);
        }
    }
    
    /**
     * Applies follow mode interpolation to a field state.
     * Fields with follow mode enabled will smoothly track the player's position.
     * Prediction is applied to ALL modes including snap.
     */
    private void tickFollowMode(ClientFieldState state, net.minecraft.entity.player.PlayerEntity player) {
        String mode = state.followMode();
        if (mode == null || mode.isEmpty()) {
            mode = "snap"; // Default to snap
        }
        
        // Get target position (player center)
        Vec3d targetPos = player.getBoundingBox().getCenter();
        
        // Apply prediction if enabled (applies to ALL modes including snap)
        if (state.predictionEnabled()) {
            targetPos = applyPrediction(player, targetPos, state);
        }
        
        // For snap mode, immediately set to target position
        if ("snap".equals(mode)) {
            state.withInterpolatedPosition(targetPos);
            return;
        }
        
        // Get current interpolated position (or use target if first tick)
        Vec3d currentPos = state.interpolatedPosition();
        if (currentPos == null || currentPos.equals(Vec3d.ZERO)) {
            state.withInterpolatedPosition(targetPos);
            return;
        }
        
        // Interpolate based on follow mode
        float lerpFactor = switch (mode.toLowerCase()) {
            case "smooth" -> 0.35f;
            case "glide" -> 0.2f;
            default -> 1.0f; // snap
        };
        
        Vec3d nextPos = currentPos.lerp(targetPos, lerpFactor);
        state.withInterpolatedPosition(nextPos);
    }
    
    /**
     * Applies prediction offset based on player velocity and look direction.
     */
    private Vec3d applyPrediction(net.minecraft.entity.player.PlayerEntity player, Vec3d base, ClientFieldState state) {
        // Apply velocity prediction
        int leadTicks = state.predictionLeadTicks();
        Vec3d predicted = base.add(player.getVelocity().multiply(leadTicks));
        
        // Apply look-ahead offset
        float lookAhead = state.predictionLookAhead();
        if (Math.abs(lookAhead) > 0.001f) {
            Vec3d look = player.getRotationVector();
            predicted = predicted.add(look.multiply(lookAhead));
        }
        
        // Apply vertical boost
        float verticalBoost = state.predictionVerticalBoost();
        if (Math.abs(verticalBoost) > 0.001f) {
            predicted = predicted.add(0.0, verticalBoost, 0.0);
        }
        
        // Clamp to max distance
        float maxDist = state.predictionMaxDistance();
        if (maxDist > 0) {
            Vec3d delta = predicted.subtract(base);
            double dist = delta.length();
            if (dist > maxDist) {
                predicted = base.add(delta.normalize().multiply(maxDist));
            }
        }
        
        return predicted;
    }
    
    /**
     * Applies prediction at render time using smoothed values.
     * 
     * <p>Unlike tick-time prediction, this uses the player's lerped position
     * and interpolated velocity estimation to avoid sudden jumps between frames.
     * 
     * @param player The player entity
     * @param base Base position (already lerped to tickDelta)
     * @param state Field state with prediction config
     * @param tickDelta Partial tick progress
     * @return Smoothly predicted position
     */
    private Vec3d applyRenderTimePrediction(net.minecraft.entity.player.PlayerEntity player, 
                                             Vec3d base, ClientFieldState state, float tickDelta) {
        // Get velocity - this is still tick-based, but we'll scale it smoothly
        Vec3d velocity = player.getVelocity();
        
        // For smoother velocity, lerp between 0 velocity and current based on tickDelta
        // This dampens sudden velocity changes mid-tick
        int leadTicks = state.predictionLeadTicks();
        
        // Scale lead by tickDelta to smoothly ramp prediction as tick progresses
        // This prevents "jumps" at tick boundaries
        float smoothLead = leadTicks * (0.5f + tickDelta * 0.5f);
        Vec3d predicted = base.add(velocity.multiply(smoothLead));
        
        // Apply look-ahead with interpolated rotation
        float lookAhead = state.predictionLookAhead();
        if (Math.abs(lookAhead) > 0.001f) {
            // getRotationVec(tickDelta) handles interpolation internally
            Vec3d look = player.getRotationVec(tickDelta);
            predicted = predicted.add(look.multiply(lookAhead));
        }
        
        // Apply vertical boost
        float verticalBoost = state.predictionVerticalBoost();
        if (Math.abs(verticalBoost) > 0.001f) {
            predicted = predicted.add(0.0, verticalBoost, 0.0);
        }
        
        // Clamp to max distance
        float maxDist = state.predictionMaxDistance();
        if (maxDist > 0) {
            Vec3d delta = predicted.subtract(base);
            double dist = delta.length();
            if (dist > maxDist) {
                predicted = base.add(delta.normalize().multiply(maxDist));
            }
        }
        
        return predicted;
    }

    
    // =========================================================================
    // Rendering
    // =========================================================================
    
    /**
     * Renders all active fields.
     * Call from WorldRenderEvents.AFTER_ENTITIES.
     */
    public void render(WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        
        // Skip rendering during warmup phase (prevents lag on join)
        if (!net.cyberpunk042.client.field.JoinWarmupManager.shouldRenderFields()) {
            return;
        }
        
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();
        
        float tickDelta = MinecraftClient.getInstance()
            .getRenderTickCounter()
            .getTickProgress(false);
        float worldTime = context.world().getTime() + tickDelta;
        
        // Render synced states (includes fields spawned via /fieldtest)
        for (ClientFieldState state : states.values()) {
            renderState(matrices, consumers, state, camPos, worldTime, tickDelta);
        }
        
        // Render personal field
        if (personalTracker.isVisible()) {
            renderPersonalField(matrices, consumers, camPos, worldTime, tickDelta);
        }
    }
    
    private void renderState(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            ClientFieldState state,
            Vec3d camPos,
            float worldTime,
            float tickDelta) {
        
        FieldDefinition def = FieldRegistry.get(state.definitionId());
        if (def == null) {
            Logging.RENDER.topic("field").trace(
                "No definition for field {}: {}", state.id(), state.definitionId());
            return;
        }
        
        // Calculate field position - use render-time computation for smoothness
        String followMode = state.followMode();
        Vec3d fieldPos;
        
        if (followMode != null && !followMode.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                // Use the player's RENDER position (lerped between ticks) for smooth tracking
                Vec3d playerRenderPos = client.player.getLerpedPos(tickDelta);
                Vec3d playerCenter = playerRenderPos.add(0, client.player.getHeight() / 2.0, 0);
                
                // Apply prediction at render-time for smooth results
                if (state.predictionEnabled()) {
                    fieldPos = applyRenderTimePrediction(client.player, playerCenter, state, tickDelta);
                } else {
                    fieldPos = playerCenter;
                }
                
                // For smooth/glide modes, still apply tick-rate interpolation on top
                if (!"snap".equals(followMode)) {
                    // Blend between tick-interpolated and render-computed position
                    Vec3d tickInterp = state.getRenderPosition(tickDelta);
                    float blendFactor = "glide".equals(followMode) ? 0.3f : 0.5f;
                    fieldPos = tickInterp.lerp(fieldPos, blendFactor);
                }
            } else {
                fieldPos = state.getRenderPosition(tickDelta);
            }
        } else {
            // Use raw position if no follow mode
            fieldPos = state.position();
        }
        
        Vec3d pos = fieldPos.subtract(camPos);
        float alpha = state.effectiveAlpha();
        float time = worldTime + state.phase();
        
        // Check for shuffle override and create RenderOverrides if needed
        RenderOverrides overrides = null;
        if (state.hasShuffleOverride()) {
            // Reconstruct the pattern from shuffle type + index
            net.cyberpunk042.visual.pattern.VertexPattern pattern = reconstructPattern(
                state.shuffleType(), state.shuffleIndex());
            
            if (pattern != null) {
                overrides = RenderOverrides.withPattern(pattern);
            }
            
            // DIAGNOSTIC: Log pattern override being applied
            if ((int) worldTime % 60 == 0) {
                Logging.RENDER.topic("shuffle").info(
                    "Rendering with shuffle override: {}:{} -> {}", 
                    state.shuffleType(), state.shuffleIndex(), 
                    pattern != null ? pattern.id() : "null");
            }
        }
        
        // Use new FieldRenderer with optional overrides
        FieldRenderer.render(
            matrices,
            consumers,
            def,
            pos,
            state.scale(),
            time,
            alpha,
            overrides
        );
    }
    
    /**
     * Reconstructs a VertexPattern from shuffle type + index, or static pattern ID.
     * Uses ShuffleGenerator to get the same permutation as the server.
     */
    private static net.cyberpunk042.visual.pattern.VertexPattern reconstructPattern(String type, int index) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        
        // Check for static pattern (prefixed with "static:")
        if (type.startsWith("static:")) {
            String patternId = type.substring(7); // Remove "static:" prefix
            return net.cyberpunk042.visual.pattern.VertexPattern.fromString(patternId);
        }
        
        // Dynamic shuffle pattern (index must be >= 0)
        if (index < 0) {
            return null;
        }
        
        return switch (type.toLowerCase()) {
            case "quad" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getQuad(index);
                yield net.cyberpunk042.visual.pattern.DynamicQuadPattern.fromArrangement(arr);
            }
            case "segment" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getSegment(index);
                yield net.cyberpunk042.visual.pattern.DynamicSegmentPattern.fromArrangement(arr);
            }
            case "sector" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getSector(index);
                yield net.cyberpunk042.visual.pattern.DynamicSectorPattern.fromArrangement(arr);
            }
            case "edge" -> {
                var arr = net.cyberpunk042.visual.pattern.ShuffleGenerator.getEdge(index);
                yield net.cyberpunk042.visual.pattern.DynamicEdgePattern.fromArrangement(arr);
            }
            default -> null;
        };
    }
    
    private void renderPersonalField(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Vec3d camPos,
            float worldTime,
            float tickDelta) {
        
        // Use LIVE definition from current edit state if GUI is active,
        // otherwise fall back to saved definition from registry
        FieldDefinition def;
        if (net.cyberpunk042.client.gui.state.FieldEditStateHolder.isTestFieldActive()) {
            var state = net.cyberpunk042.client.gui.state.FieldEditStateHolder.get();
            def = state != null 
                ? net.cyberpunk042.client.gui.state.DefinitionBuilder.fromState(state) 
                : personalTracker.definition();
        } else {
            def = personalTracker.definition();
        }
        if (def == null) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d fieldPos;
        
        if (client.player != null) {
            // Use the player's RENDER position (lerped between ticks) for smooth tracking
            Vec3d playerRenderPos = client.player.getLerpedPos(tickDelta);
            Vec3d playerCenter = playerRenderPos.add(0, client.player.getHeight() / 2.0 + 0.05, 0);
            
            // Check if follow is enabled in the definition
            net.cyberpunk042.field.instance.FollowConfig follow = def.follow();
            if (follow != null && follow.enabled() && !follow.isLocked()) {
                // Use render-time follow for smooth results
                fieldPos = applyRenderTimeFollow(client.player, playerCenter, follow, tickDelta);
            } else {
                fieldPos = playerCenter;
            }
        } else {
            // Fallback to interpolated position
            fieldPos = personalTracker.getRenderPosition(tickDelta);
        }
        
        Vec3d pos = fieldPos.subtract(camPos);
        float time = worldTime + personalTracker.getPhase();
        
        FieldRenderer.render(
            matrices,
            consumers,
            def,
            pos,
            personalTracker.getScale(),
            time,
            1.0f
        );
    }
    
    /**
     * Applies follow offset at render time for personal fields using smoothed values.
     */
    private Vec3d applyRenderTimeFollow(net.minecraft.entity.player.PlayerEntity player,
                                        Vec3d base, 
                                        net.cyberpunk042.field.instance.FollowConfig follow,
                                        float tickDelta) {
        Vec3d result = base;
        
        // Apply look-ahead
        float lookAhead = follow.lookAhead();
        if (Math.abs(lookAhead) > 0.001f) {
            Vec3d look = player.getRotationVec(tickDelta);
            result = result.add(look.multiply(lookAhead));
        }
        
        // Apply lead/trail offset
        float leadOffset = follow.leadOffset();
        if (Math.abs(leadOffset) > 0.01f) {
            Vec3d velocity = player.getVelocity();
            
            // Ignore Y velocity when on ground
            if (player.isOnGround()) {
                velocity = new Vec3d(velocity.x, 0.0, velocity.z);
            }
            
            double speed = velocity.length();
            if (speed > 0.01) {
                result = result.add(velocity.normalize().multiply(leadOffset * speed * 5.0));
            }
        }
        
        return result;
    }
}

