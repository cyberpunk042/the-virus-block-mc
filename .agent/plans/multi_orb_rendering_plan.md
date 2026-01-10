# Multi-Orb Rendering Plan

## THE MISSION
1. Preview orb renders (controlled by toggle)
2. Spawned orb renders (independent of toggle)
3. Both render AT THE SAME TIME
4. Toggle affects ONLY preview orb, NOT spawned orb

---

## CORE PRINCIPLE
**Each field needs its OWN processor instance.**
You cannot use the same processor to render two shaders in parallel.

---

## ORBS ARE DIFFERENT

### TEST/PREVIEW ORB
- **Created by**: FieldVisualAdapter.syncToEffect()
- **ID stored in**: adapter.previewFieldId
- **Controlled by**: GUI toggle
- **GUI affects**: YES

### SPAWNED ORB
- **Created by**: OrbSpawnManager.spawnOrb()
- **ID stored in**: OrbSpawnManager.activeAnimations
- **Controlled by**: Spawn lifecycle
- **GUI affects**: NEVER

---

## PROCESSOR STRATEGY

**Cache by FIELD ID (UUID), not shader key:**

```java
Map<UUID, PostEffectProcessor> PROCESSOR_CACHE
```

- Field registered → Create processor, cache by UUID
- Field unregistered → Remove processor from cache
- Each field has OWN processor instance

---

## GUI TOGGLE RULES

**Toggle callback:**
1. Set `enabled` flag in adapter
2. Call `syncToEffect()` 
3. **NEVER call FieldVisualPostEffect.setEnabled()**

**syncToEffect():**
1. enabled=false → Unregister previewFieldId ONLY
2. enabled=true → Register preview orb
3. **NEVER touch spawned orbs**
4. **NEVER call global setEnabled(false)**

---

## IMPLEMENTATION STEPS

### Step 1: Change PROCESSOR_CACHE key
- From: `Map<ShaderKey, PostEffectProcessor>`
- To: `Map<UUID, PostEffectProcessor>`

### Step 2: Create processor on field registration
- In FieldVisualRegistry.register() or when loading processor
- Store mapping: fieldId → processor

### Step 3: Remove processor on field unregistration
- In FieldVisualRegistry.unregister()
- Clean up processor from cache

### Step 4: Remove ALL global setEnabled() from adapter
- Already partially done
- Verify no setEnabled(false) calls remain that affect spawn orbs

### Step 5: Restore loop in WorldRendererFieldVisualMixin
- For each field: load/get processor, call render()

### Step 6: PostEffectPassMixin field lookup
- Need way to get field from processor instance
- Options:
  - Map<PostEffectProcessor, UUID>
  - Or store current field in processor somehow

---

## DO NOT DEVIATE
- Each field = own processor
- Preview and spawn are SEPARATE
- GUI only touches preview
- Spawn orbs are INDEPENDENT
