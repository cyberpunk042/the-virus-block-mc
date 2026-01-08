# Initialization Architecture Refactor Plan

## Problem Analysis

### What We Learned From Debugging
1. **Scattered Registration**: Payload types were registered in multiple places (TheVirusBlock, FieldSystemInit, FieldNetworking), creating confusion and potential conflicts
2. **Inconsistent Patterns**: Some payloads used `TheVirusBlock.PACKET_ID` (works), others defined their own `Identifier.of()` (didn't work)
3. **Silent Failures**: Log levels were set to WARN/OFF, hiding critical initialization info
4. **Hidden Dependencies**: Receiver registration depends on payload type registration, but this wasn't explicit
5. **Dead Code**: `FieldNetworking.registerPayloads()` was never called but sat there causing confusion

### Patterns That Work
- `ShieldFieldSpawnPayload`: Uses centralized `TheVirusBlock.SHIELD_FIELD_SPAWN_PACKET`
- Registration order: PayloadTypeRegistry (in ModInitializer) → ClientPlayNetworking.registerGlobalReceiver (in ClientModInitializer)
- Clear logging at INFO level during init

## Design Principles

### 1. Single Source of Truth
Each piece of configuration/registration should exist in exactly ONE place.

### 2. Explicit Dependencies
If B depends on A, this should be declared, not assumed.

### 3. Fail Fast, Fail Loud
Any initialization failure should crash immediately with a clear error message.

### 4. Self-Documenting
Initialization logs should tell the complete story of what was registered.

### 5. Phased Execution
Clear phases with defined boundaries prevent race conditions.

## Proposed Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         COMMON (src/main/java)                       │
├─────────────────────────────────────────────────────────────────────┤
│  TheVirusBlock.onInitialize()                                        │
│    └── ModInitManager.initServer()                                   │
│          ├── Phase 1: PayloadRegistry.registerAll()                  │
│          │     └── All S2C and C2S payload types                     │
│          ├── Phase 2: CommandRegistry.registerAll()                  │
│          ├── Phase 3: FieldSystemInit.initServerLogic()              │
│          └── Phase 4: GuiPacketRegistration.registerServerHandlers() │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT (src/client/java)                      │
├─────────────────────────────────────────────────────────────────────┤
│  TheVirusBlockClient.onInitializeClient()                            │
│    └── ModInitManager.initClient()                                   │
│          ├── Phase 1: ReceiverRegistry.registerAll()                 │
│          │     └── All ClientPlayNetworking receivers                │
│          ├── Phase 2: RendererRegistry.registerAll()                 │
│          │     └── TestFieldRenderer, ClientFieldManager, etc.       │
│          ├── Phase 3: EventRegistry.registerAll()                    │
│          │     └── Tick, render, connection events                   │
│          └── Phase 4: WarmupTasks.runAll()                           │
│                └── Profile loading, tessellation warmup              │
└─────────────────────────────────────────────────────────────────────┘
```

## Key Components

### 1. PayloadRegistry (Common)
```java
public final class PayloadRegistry {
    private static final Set<Identifier> registered = new HashSet<>();
    
    public static void registerAll() {
        // All S2C payloads
        register(FieldSpawnPayload.ID, FieldSpawnPayload.CODEC);
        register(ShieldFieldSpawnPayload.ID, ShieldFieldSpawnPayload.CODEC);
        // ... all others
        
        // All C2S payloads
        
        // ... all others
        
        Logging.REGISTRY.info("Registered {} payload types", registered.size());
    }
    
    private static <T extends CustomPayload> void register(Id<T> id, PacketCodec<?, T> codec) {
        if (registered.contains(id.id())) {
            throw new IllegalStateException("Duplicate payload registration: " + id.id());
        }
        PayloadTypeRegistry.playS2C().register(id, codec);
        registered.add(id.id());
        Logging.REGISTRY.info("  ✓ S2C: {}", id.id());
    }
}
```

### 2. ReceiverRegistry (Client)
```java
public final class ReceiverRegistry {
    private static final Set<Identifier> registered = new HashSet<>();
    
    public static void registerAll() {
        // Field system receivers
        register(FieldSpawnPayload.ID, FieldClientHandlers::handleSpawn);
        register(ShieldFieldSpawnPayload.ID, FieldClientHandlers::handleLegacyShieldSpawn);
        // ... all others
        
        Logging.REGISTRY.info("Registered {} receivers", registered.size());
    }
    
    private static <T extends CustomPayload> void register(
            Id<T> id, 
            BiConsumer<T, ClientPlayNetworking.Context> handler) {
        ClientPlayNetworking.registerGlobalReceiver(id, (payload, context) -> {
            Logging.NETWORK.debug("Received: {} (id={})", id.id(), payload);
            context.client().execute(() -> handler.accept(payload, context));
        });
        registered.add(id.id());
        Logging.REGISTRY.info("  ✓ Receiver: {}", id.id());
    }
}
```

### 3. Centralized Packet IDs
```java
// In TheVirusBlock.java - ALL packet IDs in one place
public static final class PacketIds {
    // Field system
    public static final Identifier FIELD_SPAWN = id("field_spawn");
    public static final Identifier FIELD_REMOVE = id("field_remove");
    public static final Identifier FIELD_UPDATE = id("field_update");
    public static final Identifier FIELD_DEFINITION_SYNC = id("field_definition_sync");
    
    // Legacy/Anti-virus
    public static final Identifier SHIELD_FIELD_SPAWN = id("shield_field_spawn");
    public static final Identifier SHIELD_FIELD_REMOVE = id("shield_field_remove");
    
    // ... all others
    
    private static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
```

## Migration Strategy

### Phase 1: Centralize Packet IDs (Low Risk)
1. Create `TheVirusBlock.PacketIds` inner class
2. Move all Identifier definitions there
3. Update all payload classes to use `PacketIds.X`

### Phase 2: Create Registry Classes (Medium Risk)
1. Create `PayloadRegistry` in common code
2. Create `ReceiverRegistry` in client code
3. Move registrations from scattered locations to these classes

### Phase 3: Cleanup (Low Risk)
1. Remove dead code (`FieldNetworking.registerPayloads()`)
2. Remove duplicate registrations
3. Update log levels to be consistent (INFO for init, DEBUG for runtime)

### Phase 4: Add Validation (Optional)
1. Add startup checks that verify all S2C payloads have receivers
2. Add checks for duplicate registrations
3. Add summary report of what was initialized

## Cleanup Candidates

### Files to Modify
- `TheVirusBlock.java` - Remove scattered PayloadTypeRegistry calls, add PacketIds
- `TheVirusBlockClient.java` - Delegate to ReceiverRegistry
- `FieldSystemInit.java` - Remove network registration, focus on server logic
- `FieldClientInit.java` - Remove receiver registration, delegate to ReceiverRegistry
- `GuiPacketRegistration.java` - Clean up, focus on server handlers only

### Files to Delete/Deprecate
- `FieldNetworking.registerPayloads()` - Never called, dead code

## Expected Benefits
1. **Debuggability**: One place to check for registration issues
2. **Consistency**: All payloads follow the same pattern
3. **Maintainability**: Adding new payloads is straightforward
4. **Visibility**: Clear logs show exactly what's registered
5. **Safety**: Duplicate detection prevents conflicts

## Next Steps
1. [ ] Test current fix (FieldSpawnPayload using TheVirusBlock.FIELD_SPAWN_PACKET)
2. [ ] If working, proceed with Phase 1 (Centralize Packet IDs)
3. [ ] Create registry classes
4. [ ] Migrate existing code
5. [ ] Clean up dead code
