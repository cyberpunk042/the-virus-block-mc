# Shader Effect Synchronization Service Proposal

## Problem Statement

When a shader effect is loaded before the world fully initializes, the effect can become
desynchronized due to:
1. Invalid camera/depth data at startup
2. Effects existing in the world before the player joins (server-side effects)
3. Time/TPS drift between client and server

This causes glitches like:
- Ground transposition not aligning correctly
- Ring effects appearing in wrong positions
- General visual desync until client restart

## Current Architecture

### Existing Components

1. **JoinWarmupManager** - Manages smooth loading on server join
   - Progress phases: Anti-Virus → Effects → Profiles → Shaders → Chunks
   - Has `shouldRenderFields()` gate
   - Could be extended for sync logic

2. **ShaderWarmupService** - Pre-compiles shaders during join
   - Called from JoinWarmupManager during "Loading Shaders" phase
   - Tracks `isWarmedUp()` and `isWarming()` state
   - Has `invalidate()` for resource reload

3. **ShaderAnimationManager** - Manages animation time
   - `tick()` updates animation time
   - Uses game time so animations pause when game is paused
   - Could be extended for server TPS sync

4. **FieldVisualRegistry** - Manages active field instances
   - Client-side registry of visual effects
   - Currently no server sync mechanism

5. **FieldVisualPostEffect** - Renders the actual shader effect
   - `currentField` tracks what's being rendered
   - `setCurrentField()` / `getCurrentField()` for per-frame binding

6. **Sync Payloads** (existing)
   - `FieldDefinitionSyncPayload` - syncs field definitions
   - `ProfileSyncS2CPayload` - syncs profiles
   - `DifficultySyncPayload` - syncs difficulty

## Proposed Solution

### New: ShaderEffectSyncService

A unified service that handles:

1. **Initialization State Tracking**
   - Track if effect was started before world was ready
   - Flag effects as "pending resync"

2. **World-Ready Detection**
   - Monitor when camera/depth data becomes valid
   - Check `MinecraftClient.getInstance().world != null`
   - Verify depth buffer has valid values

3. **Effect Resync on Ready**
   - When world becomes ready, iterate pending effects
   - Reset animation time offsets
   - Rebind UBO with fresh data
   - Clear "pending" flag

4. **Server → Client Sync**
   - Receive server-time offset on join
   - Adjust `ShaderAnimationManager.time` to match
   - Handle effects that existed before player joined

5. **TPS Alignment**
   - Receive server TPS updates
   - Scale animation speed accordingly
   - Handle TPS fluctuations gracefully

### Integration Points

1. **JoinWarmupManager.tick()**
   - Add `ShaderEffectSyncService.checkWorldReady()` call
   - Only proceed past shader phase when sync is complete

2. **ShaderAnimationManager.tick()**
   - Consult `ShaderEffectSyncService` for time offset
   - Apply TPS scaling if enabled

3. **FieldVisualRegistry.register()**
   - Check if world is ready
   - If not, mark instance as "pending sync"

4. **Network Layer**
   - New `ShaderTimeSyncS2CPayload` for time alignment
   - Could extend `FieldDefinitionSyncPayload` with timestamps

### API Design

```java
public final class ShaderEffectSyncService {
    private static boolean worldReady = false;
    private static float serverTimeOffset = 0.0f;
    private static final Set<UUID> pendingSyncFields = new HashSet<>();
    
    // Called from JoinWarmupManager.tick()
    public static void checkWorldReady() {
        if (worldReady) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && 
            client.player != null &&
            isDepthBufferValid()) {
            
            worldReady = true;
            resyncAllPendingEffects();
        }
    }
    
    // Called when registering an effect before world is ready
    public static void markPendingSync(UUID fieldId) {
        pendingSyncFields.add(fieldId);
    }
    
    // Called when server sends time sync packet
    public static void setServerTimeOffset(float offset) {
        serverTimeOffset = offset;
        ShaderAnimationManager.adjustTimeOffset(offset);
    }
    
    private static void resyncAllPendingEffects() {
        for (UUID id : pendingSyncFields) {
            FieldVisualInstance field = FieldVisualRegistry.get(id);
            if (field != null) {
                field.resetAnimationState();
            }
        }
        pendingSyncFields.clear();
    }
    
    public static void reset() {
        worldReady = false;
        serverTimeOffset = 0.0f;
        pendingSyncFields.clear();
    }
}
```

### Implementation Steps

1. **Phase 1: World-Ready Detection**
   - Create basic `ShaderEffectSyncService`
   - Add world-ready check
   - Integrate with `JoinWarmupManager`

2. **Phase 2: Effect Resync**
   - Track pending effects
   - Implement resync logic
   - Add `resetAnimationState()` to `FieldVisualInstance`

3. **Phase 3: Server Time Sync**
   - Create `ShaderTimeSyncS2CPayload`
   - Send on player join
   - Apply offset in `ShaderAnimationManager`

4. **Phase 4: TPS Alignment**
   - Track server TPS
   - Scale animation accordingly
   - Handle edge cases

## Files to Create/Modify

### New Files
- `src/client/java/net/cyberpunk042/client/visual/shader/util/ShaderEffectSyncService.java`
- `src/main/java/net/cyberpunk042/network/ShaderTimeSyncS2CPayload.java` (if server sync needed)

### Modified Files
- `JoinWarmupManager.java` - Add sync check
- `ShaderAnimationManager.java` - Add time offset/TPS support
- `FieldVisualRegistry.java` - Mark pending sync on register
- `FieldVisualInstance.java` - Add `resetAnimationState()` method
