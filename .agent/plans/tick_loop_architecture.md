# Tick Loop Architecture Analysis

## Reference: Classic Game Loop Pattern

```
┌────────────────────────────────────────────────────────────────────┐
│                           START GAME                               │
└─────────────────────────────────┬──────────────────────────────────┘
                                  ▼
┌────────────────────────────────────────────────────────────────────┐
│                     INITIALIZE GAME STATE                          │
│  - Load resources (once)                                           │
│  - Create cached contexts                                          │
│  - Register event handlers                                         │
└─────────────────────────────────┬──────────────────────────────────┘
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        GAME LOOP (per tick)                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │ PROCESS INPUT   │→ │ UPDATE STATE    │→ │ RENDER (client) │     │
│  │ - Events        │  │ - Physics       │  │ - Draw frames   │     │
│  │ - Commands      │  │ - AI            │  │ - Animations    │     │
│  │ - Packets       │  │ - Timers        │  └─────────────────┘     │
│  └─────────────────┘  └─────────────────┘                          │
└─────────────────────────────────┬──────────────────────────────────┘
                                  ▼
                          [Exit condition?]
                                  ▼
┌────────────────────────────────────────────────────────────────────┐
│                      CLEANUP AND EXIT                              │
│  - Save state                                                      │
│  - Release resources                                               │
└────────────────────────────────────────────────────────────────────┘
```

---

## Current Tick Handler Registration Points

### Server Tick (END_SERVER_TICK)
| Handler | Location | Purpose |
|---------|----------|---------|
| `TickProfiler::onTick` | TickProfiler.java:30 | Logs performance data |
| `DelayedServerTasks::run` | DelayedServerTasks.java:25 | Executes scheduled tasks |

### World Tick (END_WORLD_TICK) - Runs 3x per tick (3 dimensions)
| Handler | Location | Purpose |
|---------|----------|---------|
| `VirusWorldState.tick()` | InfectionNodes.java:127 | Core infection logic |
| `VirusTierBossBar.update()` | InfectionNodes.java:137 | UI updates |
| `VirusInventoryAnnouncements.tick()` | InfectionNodes.java:141 | Announcements |
| `FieldManager.tick()` | FieldNodes.java:100 | Force field physics |
| `GlobalTerrainCorruption.tickWorld()` | GlobalTerrainCorruption.java:38 | Terrain corruption |

---

## Issues Identified & Fixed

### ✅ Fixed: Object Allocation Every Tick
**Before:**
```java
private VirusWorldContext createVirusWorldContext(ServerWorld world) {
    return new BasicVirusWorldContext(...);  // NEW OBJECT EVERY TICK!
}
```

**After:**
```java
private VirusWorldContext createVirusWorldContext(ServerWorld world) {
    if (cachedVirusContext == null || cachedVirusContext.world() != world) {
        cachedVirusContext = new BasicVirusWorldContext(...);  // Once per world
    }
    return cachedVirusContext;
}
```

### ✅ Fixed: O(n²) in Boss Bar Player Sync
**Before:** `bar.getPlayers().contains(player)` inside loop = O(n²)
**After:** Pre-built HashSet for O(1) lookups

### ✅ Fixed: Collision Mixins Missing Early Exit
**Before:** No early exit when infection inactive
**After:** `if (!GrowthCollisionTracker.hasAny()) return;` as first line

### ✅ Fixed: Mob Mixins Expensive Calls Every Tick
**Before:** `VirusWorldState.get()` called even when condition is false
**After:** Check cheap condition (`isOnFire()`) before expensive call

---

## Idempotency Analysis

### ✅ Idempotent Operations (Safe to retry)
| Operation | Reason |
|-----------|--------|
| `VirusWorldState.get(world)` | Returns cached PersistentState |
| `FieldRegistry.get(id)` | Returns cached definition |
| `GrowthCollisionTracker.hasAny()` | Reads volatile counter |
| `DimensionProfileRegistry.resolve()` | Returns cached profile |

### ⚠️ Non-Idempotent Operations (Once only)
| Operation | Guard |
|-----------|-------|
| `GlobalTerrainCorruption.init()` | `if (initialized) return;` |
| `VirusTierBossBar.init()` | `if (initialized) return;` |
| `DelayedServerTasks.init()` | `if (initialized) return;` |
| Event handler registration | Runs in InitNode (once) |

### 🔴 Potentially Expensive Per-Tick Operations
| Operation | Mitigation |
|-----------|------------|
| `world.getPlayers()` | Returns live list, cheap |
| `FieldManager.tick()` loops | Has fast exit when empty |
| `VirusInventoryAnnouncements` | Only runs on 1000/1500 tick intervals |

---

## Ideal Tick Loop Structure

```java
public class InfectionTickLoop {
    // Cached state - initialized once
    private VirusWorldContext context;
    private SingularityContext singularityContext;
    
    public void init(ServerWorld world) {
        // Called once on world load
        context = createContext(world);
        singularityContext = createSingularityContext(world);
    }
    
    public void tick(ServerWorld world) {
        // Fast exits first
        if (!shouldTick(world)) return;
        
        // Update cached context's world reference
        context.updateWorld(world);
        
        // Ordered phases
        tickScheduler();           // Process delayed tasks
        tickScenario(context);     // Core scenario logic
        tickPhases(context);       // Singularity phases
        tickFields(world);         // Force fields
        tickUI(world);             // Boss bars, announcements
    }
    
    public void shutdown(ServerWorld world) {
        // Cleanup
        context = null;
        singularityContext = null;
    }
}
```

---

## Performance Metrics to Monitor

```java
// Use TickProfiler with these labels:
TickProfiler.start("VirusWorldState.get");   // Should be <1μs
TickProfiler.start("VirusWorldState.tick");  // Target: <500μs
TickProfiler.start("FieldManager.tick");     // Target: <100μs per field
TickProfiler.start("Mixin:Collision");       // Should be 0 when no growth
```

---

## Summary of Session Optimizations

1. **Collision System** - O(1) early exit via `GrowthCollisionTracker.hasAny()`
2. **Spatial Filtering** - BlockPos distance check before VoxelShape operations
3. **Context Caching** - Single allocation per world instead of per tick
4. **Boss Bar Sync** - HashSet for O(1) contains check
5. **Mob Mixins** - Cheap condition checks before expensive calls
6. **Field Definition Caching** - Instance-level cache to avoid registry lookup

**Expected Impact:** When infection is NOT active, mod overhead should be near-zero.
