# Implementation Summary: Two New Features

## Date: 2025-12-21

---

## Feature 1: "The Infection Sings" - Heartbeat Sound System ♥

### Overview
Every virus source block now emits a rhythmic heartbeat sound that intensifies as the infection progresses.

### Behavior
- **Tier 1 (index 0)**: Heartbeat every 60 ticks (3 seconds) - slow, ominous
- **Tier 2-4**: Progressively faster (50, 40, 30, 20 ticks)
- **Tier 5 (index 4)**: Heartbeat every 20 ticks (1 second) - rapid, threatening  
- **Apocalypse Mode**: Heartbeat every 10 ticks (0.5 seconds) - frantic

### Audio Properties
- **Sound**: `block.sculk_sensor.clicking` (vanilla) - has a pulse-like quality
- **Volume**: 0.3 at Tier 1, +0.1 per tier, +0.2 in apocalypse (max 1.0)
- **Pitch**: 0.6 → 1.0 (normal), 1.4 (apocalypse - more urgent)

### Files Modified
- `src/main/java/net/cyberpunk042/block/entity/VirusBlockEntity.java`
  - Added `heartbeatCooldown` field
  - Added `tickHeartbeat()` method

---

## Feature 2: "Viral Adaptation" - Damage Resistance Memory 🧬

### Overview
The virus tracks what hurts it and develops resistance over time. Players must vary their attack strategies.

### Mechanics
- **1st hit**: 0% resistance → records exposure
- **2nd hit**: 25% resistance
- **3rd hit**: 50% resistance  
- **4th hit**: 75% resistance
- **5th+ hit**: 100% resistance (immune)

### Damage Categories Tracked
| Key | Description |
|-----|-------------|
| `BED` | Bed explosions |
| `TNT` | TNT explosions |
| `EXPLOSION` | Other explosions (creepers, etc.) |
| `MELEE:minecraft:diamond_pickaxe` | Per-item melee weapons/tools |
| `MELEE:fist` | Bare-handed attacks |
| `PROJECTILE` | Arrow and projectile damage |
| `OTHER` | Fallback for uncategorized |

### Player Feedback
- **Adaptation message**: "The virus adapts! {type} now deals {%}% less damage." (purple)
- **Immunity message**: "The virus is immune to {type}!" (dark red)
- Displayed in action bar to all players

### Persistence
- Damage adaptation is saved/loaded with world data
- Stored in `VirusWorldPersistence` as `Map<String, Integer>`

### Files Created
- `src/main/java/net/cyberpunk042/infection/VirusDamageClassifier.java`
  - Damage source classification utility
  - Display name formatting for player messages

### Files Modified
- `src/main/java/net/cyberpunk042/infection/state/InfectionState.java`
  - Added `damageAdaptation` map
  - Added `recordDamageExposure()`, `getDamageResistance()`, `getExposureCount()`, `resetDamageAdaptation()`

- `src/main/java/net/cyberpunk042/infection/InfectionOperations.java`
  - Added overloaded `applyHealthDamage(world, amount, damageKey)`
  - Added `notifyAdaptation()` and `notifyImmunity()` methods

- `src/main/java/net/cyberpunk042/infection/service/TierProgressionService.java`
  - Added overloaded `bleedHealth(fraction, damageKey)`

- `src/main/java/net/cyberpunk042/block/virus/VirusBlock.java`
  - Updated `onExploded()` to classify explosion damage
  - Updated `onProjectileHit()` to track projectile damage
  - Updated `calcBlockBreakingDelta()` to track melee damage per tool

- `src/main/java/net/cyberpunk042/infection/VirusWorldPersistence.java`
  - Added `damageAdaptation` field to snapshot
  - Added `captureDamageAdaptation()` helper
  - Updated codec and `applyTo()` for persistence

- `src/main/resources/assets/the-virus-block/lang/en_us.json`
  - Added `message.the-virus-block.adaptation`
  - Added `message.the-virus-block.immunity`

---

## Testing Notes

### Heartbeat Testing
1. Place a virus block in the world
2. Listen for the heartbeat sound
3. Use `/virus tier set 5` to advance tiers and verify the tempo increases
4. Use `/virus apocalypse` to verify frantic heartbeat

### Adaptation Testing
1. Enter apocalypse mode (virus block is vulnerable)
2. Hit the virus block with a diamond pickaxe
3. Verify message: "The virus adapts! Diamond Pickaxe now deals 25% less damage."
4. Hit 3 more times with the same tool
5. Verify message: "The virus is immune to Diamond Pickaxe!"
6. Switch to a different tool - should deal full damage again
7. Save and reload world - verify resistances persist

---

## Future Enhancements

- **Custom heartbeat sound**: Add a dedicated `virus_heartbeat.ogg` for more immersive audio
- **Visual pulse**: Sync particle effects with heartbeat
- **Resistance decay**: Slowly lose resistance over time if not damaged by that type
- **Adaptation notification particles**: Spawn particles when virus adapts
