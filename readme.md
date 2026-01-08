# 🦠 THE VIRUS BLOCK — Beginning of the End

**A Minecraft mod that lets you destroy your entire world… one mutation at a time.**  
Place a single block.  
Watch your dimension collapse into chaos.

Perfect for:
- Streamers  
- Challenge runs  
- SMP events  
- “Hardcore but getting worse every minute” experiences  

Once the **Virus Block** is placed, your dimension enters a progressive, escalating corruption arc that alters every part of Minecraft: mobs, blocks, worldgen, weather, cave physics, and even player behavior.

There is no going back.  
Only survival.

---

## 🧬 Core Concept

When the Virus Block is activated:

- Your dimension is flagged as **infected**  
- Mutation systems turn on  
- Chaos escalates in **five infection tiers**  
- The world becomes progressively more hostile, unstable, and unpredictable  

This mod transforms Minecraft into a living apocalypse simulator.

---

## 📈 Infection Tiers

The Virus spreads through **five escalating tiers**:

### Tier 1 — Mild Corruption

- Local block mutations around the Virus Block  
- Single-block flickers / texture glitches  
- Passive mobs stare, follow, or mirror players  
- Virus Aura: light poison ticks; stand too long on an Infectious Cube and it starts nibbling half-hearts  
- Carrying an Infectious Cube in your inventory for a few seconds gives intermittent **Hunger** + **Nausea**

### Tier 2 — Environmental Decay

- Random mutation pulses across loaded chunks  
- Fluids begin corrupting (murky water / tainted lava)  
- Small cave-ins during mining and the first **Collapse Surge** events  
- Hostile arrows occasionally fall from the sky  
- Boobytraps start appearing, but at a low density

### Tier 3 — Hostile Ecosystem

- Passive mobs become corrupted and aggressive  
- Terrain patches convert to corrupted biomes; surface carpets of corrupted sand/ice/snow spread quickly  
- TNT meteors / Skyfall begin  
- Void Tears become possible once Tier 3 extras are enabled  
- Boobytrap spread and bonus hostile spawns ramp up significantly

### Tier 4 — Reality Warping

- Random gravity collapses, sideways block falls, inversion events  
- Mobs gain random abilities: speed, crits, explosions, venom, knockback  
- Fluids misbehave in corrupted zones; Collapse Surge becomes lethal  
- Entity Duplication can trigger if Tier 4 extras stay enabled

### Tier 5 — The Endgame

- Biomes collapse or invert outright  
- Massive TNT/Arrow barrages, corruption waves, and entity duplication storms  
- Corrupted TNT now rolls volatile results (1/3 dud, 1/3 normal, 1/3 1.5× blast) to keep finales unpredictable  
- Bonus hostile mob spawns spike to their highest density thanks to tier-aware scaling  
- A guardian-beam shock field forms around every cocoon during the first half of Tier 5, repeatedly shoving intruders backward  
- Once the tier bar fills, the Virus enters **Apocalypse Mode** and begins charging the Singularity.

### 🔻 Apocalypse Mode & Singularity

After the last Tier 5 assault, the finale unfolds as a choreographed disaster:

1. **Fusing Countdown (30 seconds)**  
   - Bossbar switches to a purple “Total Collapse” warning.  
   - Virus Blocks crackle and flash white with the actual TNT fuse effect (we spawn a non-exploding primed TNT entity for the fuse animation), guardian beams intensify, and at T−15 s any remaining cocoon shell forcibly collapses.
2. **Singularity Beam + Reverse Collapse**  
   - A dark red (rainbow-tinted) beam punches from bedrock to build height.  
   - Chunk shells are queued from the horizon inward; every few ticks another batch implodes (alternating bottom→top vs top→bottom) while a sculk/portal veil and Warden booms sweep across the skyline.
3. **Core Ignition (“Big Bang”)**  
   - Once only the Virus chunk remains, the block transforms into a Singularity core, unleashes the largest explosion in the mod, and launches debris outward.
4. **Orbital Ring Formation**  
   - Debris coalesces into a luminous ring circling the blast site; multiple columns are shredded every tick and a gravitational wind (`pullEntitiesTowardRing`) drags players/items toward the luminous halo so the spectacle never pauses.
5. **Dissipation & World Reset**  
   - Reverse-portal surges draw the ring inward, swallow lingering debris, and the infection forcibly resets (virus sources removed, starter Virus Block returned to players).

> **References:**  
> 1. `VirusWorldState.tickSingularity()` – drives the countdown, shell collapse, collapse sweep, core ignition, ring, and cleanup.  
> 2. `docs/singularity-plan.md` – design document outlining every phase.  
> 3. `VirusStatsCommand` – exposes remaining time and current Singularity state via `/virusstats`.  
> 4. `VirusCommand` – provides `/virusblock singularity start|abort|status` for admin/debug playback.

- Purification option 3 still halves the block’s max health, shortening the fight without rewinding progress.
- When the Singularity finishes dissipating, you spawn with the Virus Block again (starter kit) and the world has been cleansed.

---

## 🧩 Corrupted Block Variants

Every vanilla block can spawn a **corrupted variant**:

Examples:

- **Corrupted Stone:** behaves like sand  
- **Corrupted Glass:** explodes when broken  
- **Corrupted Dirt:** spawns bitey worms  
- **Corrupted Wood:** ignites when walked on  
- **Corrupted Iron Block:** magnetic pull on items  
- **Corrupted Ice:** creates hallucinations  

Over time, your world becomes an alien landscape.

---

## 🌪️ Global Mutation Events

These occur periodically or randomly:

- **Block Mutation Pulse**  
  Patches of blocks shift all at once into random variants.

- **Skyfall Event**  
  TNT / arrows / random objects rain down over players.

- **Passive Mob Revolt**  
  Cows, pigs, chickens, etc. become corrupted and hostile.

- **Mob Buff Storm**  
  All mobs gain random buffs (speed, strength, explosions, crits, etc.).

- **Liquid Mutation**  
  Water → sludge (slow + poison)  
  Lava → tainted lava (extra dangerous, smoke bursts)

- **Collapse Surge**  
  Stone caves collapse in chain reactions while mining.

- **Void Tears**  
  Temporary mini black holes that suck in nearby entities.

- **Inversion Event**  
  Falling blocks fall UP instead of down for a short duration.

- **Virus Bloom**  
  Plants and trees convert to corrupted flora instantly.

- **Entity Duplication**  
  Random mobs multiply into weaker clones for swarm chaos.

Every session becomes a unique apocalypse.

---

## 👁️ Virus Aura & Infectious Hazards

- Standing near the Virus Block applies poison, hunger, slowness, nausea, and armor corrosion. Intensity scales with tier.  
- Standing directly **on** an Infectious Cube now ticks damage continuously; Rubber Shoes extend the safe window slightly but the cube will still chew through your footwear first.  
- Carrying an Infectious Cube in your inventory rapidly builds viral load—once the threshold hits you’ll be pulsed with Hunger, Nausea, Weakness, Slowness, and Poison until you drop it or stash it.

Pro tip: cure cubes with milk (shapeless recipe) or quarantine them inside obsidian shells.

---

## 🛡️ Prototype Anti-Virus Gear

Four late-game armor pieces help you survive the apocalypse while keeping their vanilla enchant pools and trim compatibility:

- **Composite Elytra (Chest)** – Netherite chestplate defense + Elytra glide in a single item. Firework boosts are throttled for balance, but custom Elytra wings render on the player so you keep your silhouette. Repair with Netherite Ingots or Phantom Membranes.  
- **Rubber Shoes (Boots)** – Double durability leather boots tuned for boobytrap crawls: extra tolerance while standing on Infectious Cubes, softer explosion knockback, and reduced damage from trap payloads. Repair with Slimeballs.  
- **Heavy Pants (Leggings)** – Total immunity to Void Tear pull/damage/knockback. The tear eats the leggings’ durability instead of you, so bring spares.  
- **Augmented Helmet (Helmet)** – Emits periodic compass-style pings that point toward the nearest Virus Block or shield anchor, complete with electric spark particles and HUD chat hints.

These items appear in the custom creative tab alongside the vanilla Elytra for easy testing.

---

## ⚔️ Ending the Infection

Apocalypse Mode is the last phase—there is no separate boss arena right now. Survive the Tier 5 chaos, keep accelerating the Virus Block’s progress, and once the bar flips to red health you can finally fight back:

- Every hit against the Virus Block removes health instead of rewinding progress.
- TNT, bed explosions, and projectiles all deal chunk-based damage; larger blasts chew away more HP.
- Purification totems remain powerful finishers—option 3 reduces max HP, option 4 burns current HP.
- When the health bar empties, the Virus Block disintegrates and the infection resets to zero sources.

---

## 🏛️ Player Tools & Interactions

### Accelerate / Delay Corruption

- Poke the Virus Block to speed up tier progress.  
- Wrap a Cured Infectious Cube in an obsidian cocoon to spawn an **Anti-Virus Field**—a beacon-style dome that stops spread, shields players from anomalies, and detonates violently if the virus reaches it.  

### Temporary Reversion — Purification Totem

- **No Boobytraps** – disables trap placement and throttles corrupted worms (virus enters “dormant” mode).  
- **No Shell** – collapses every defensive shell wrapped around the Virus Block. (Tier 5)
- **Half HP** – halves the Virus Block’s *maximum* health.  
- **Bleed HP** – halves the Virus Block’s *current* health immediately. (EASY Mode Only)

### End the Apocalypse

- Once Apocalypse Mode begins, the bossbar flips to red HP.  
- Hits, TNT, beds, and Purification option 4 drain health.  
- When HP reaches zero (or you mine the exposed block), the infection is wiped and the world calms down.

---

## 🧨 Why This Mod Rocks for Streamers

- Every world becomes a dynamic storyline  
- Random chaos = endless clips  
- Viewers can predict and watch the infection escalate  
- Passive mobs turning hostile is peak content  
- TNT meteors + block collapse chains = living highlight reels  
- World slowly turning alien = insane thumbnails  
- Built-in tension arc: “What tier are we at!?”  
- Endgame boss is fully cinematic and world-altering  

This is a “progressive chaos” mod designed for content creation and challenge runs.

---

## 🛠️ Development & Testing Roadmap

- The ongoing `VirusWorldState` refactor, architecture diagrams, and milestone breakdowns live in `docs/architecture_readme.md`. Check it whenever you need to understand which systems are being extracted next (Scenario Registry, controllers, planners, effect bus, etc.).
- Collision refactor notes (mixins, piston-inspired collision, remap pitfalls, log signatures) live in `docs/growth-block-collision.md`. Read it before touching the growth block mixins so you don’t repeat the sweet-spot issues or Loom remap failures.
- `docs/virusworld_state_refactor.md` captures the Strategy/Builder/Observer patterns behind that roadmap and is the canonical blueprint referenced by engineering discussions.
- Automated tests will land later in the roadmap; for now, **Jean runs manual QA playtests at each milestone** (controller extraction, planner service, engine integration, etc.) and records the results alongside release notes.
- If you are contributing code, add your scenario/feature notes to the architecture doc so the manual testing checklist stays accurate.

## 🧱 Growth Block Collision Mixins

Player collision with multi-block growth shells now flows through a layered set of mixins plus a shared helper. Together they almost fully solve the “players glide through growth blocks” bug by forcing every server-side collision query to merge the tracker’s world-space boxes:

- `EntityGrowthCollisionMixin` (Yarn) and `EntityGrowthCollisionIntermediaryMixin` (runtime) hook `Entity.findCollisionsForMovement`/`method_59920`, append growth `VoxelShape`s, and emit `[GrowthCollision:hook|sweep|move]` logs so we can prove players actually enter the vanilla collision stack.
- `CollisionViewGrowthCollisionMixin` mirrors piston behavior by extending `CollisionView.getCollisions(...)` whenever the view is a `World` instance and the entity is a player, giving us `[GrowthCollision:cv]` breadcrumbs for every server sweep.
- `ServerPlayNetworkHandlerMixin` wraps the `WorldView.getCollisions(...)` call inside `isEntityNotCollidingWithBlocks`. This was the breakthrough: once the anti-cheat validation sees the merged `Iterable<VoxelShape>`, server players can no longer slip through even when the client tries to predictively phase.
- `GrowthCollisionMixinHelper` centralizes the tracker scan, logs why shapes were skipped, throttles debug box spam, and merges the vanilla/external collision lists consistently.

With the `WorldView` wrap in place the collisions “stick” for survival/creative players during testing; any remaining oddities trace back to tracker coverage (e.g., growth block entities not registering) rather than missing mixins. See `docs/growth-block-collision.md` for the debugging timeline, log signatures, and future maintenance checklist.

Need the old `[GrowthCollision:*]` traces again? Start the JVM with `-Dthevirusblock.growthCollisionDebug=true` to re-enable every collision log; otherwise they stay silent to avoid console spam.  
Trying to isolate the collision pipeline? Add `-Dthevirusblock.disableCollisionViewHook=true` to temporarily disable the generic `CollisionView` mixin so only the `ServerPlayNetworkHandler` wrap remains active.

When debug logging is enabled you will now see `[GrowthCollision:debug] … scenario=TOP|BOTTOM|SIDE_*` lines that spell out the exact `worldBox` vs `query` overlap for each contact type—capture one snippet per scenario when reproducing sticky collisions. Still seeing rubber-banding while walking on top? Set `-Dthevirusblock.growthTopClearance=0.15` (default `0.02`, adjustable up to `0.25`) so the helper treats anyone within that many blocks of the roof as “above” the shell and skips duplicate collisions. Prefer runtime experiments? Use `/growthcollision shape shell|solid` (permission level ≥2) to swap between the new thin-shell collider and the original solid column without restarting, and `/growthcollision dump` to log every active growth block’s bounding boxes on demand.

---

## 🧪 Debug / Admin Commands

- `/virusstats` – Quick diagnostics (time since mod start, infection uptime, ETA to final wave).  
- `/virustiers` – Shows the current tier, apocalypse state, and Tier Cookbook flags (great for tuning gamerules).  
- `/virusboobytraps [radiusChunks]` – Lists nearby boobytraps (defaults to 8 chunks).  
- `/virusblock waves friendlyfire <enable|disable|status>` – Toggle whether virus-spawned waves hurt one another.  
- `/virusblock teleport <enable|disable|radius|status>` – Control automatic Virus Block teleports.

### Tier Feature Defaults

- **Tier 2:** liquid corruption, corrupted sand/ice/snow surfaces, Mutation Pulse, Skyfall, Collapse Surge, Passive Revolt, Mob Buff Storm, Virus Bloom.
- **Tier 3:** Void Tear, Inversion.
- **Tier 4:** Entity Duplication.
- **Tier 5:** (Reserved – apocalypse add-ons only.)

## 🍳 Crafting Recipes

| Recipe | Inputs | Notes |
| --- | --- | --- |
| **Cured Infectious Cube** | Shapeless: 1× Infectious Cube + 1× Milk Bucket | Produces 1× Cured Infectious Cube. Combine anywhere in the crafting grid. |
| **Purification Totem** | Shaped “cross” pattern: Top = Cured Infectious Cube, Left = Corrupted Gold, Center = Totem of Undying, Right = Corrupted Iron, Bottom = Corrupted Diamond | There are four JSON variants covering every rotation; the center slot must always be a Totem of Undying. Consumed on use. |
| **Composite Elytra** | Elytra + Netherite Chestplate + (2) Crying Obsidian | Combines Elytra glide with netherite-tier armor. Repair using Netherite Ingots or Phantom Membranes. |
| **Rubber Shoes** | 2× Slime Block + 3× String (shaped “U”) | High-durability anti-boobytrap boots that extend Infectious Cube tolerance and dampen knockback. Repair with Slimeballs. |
| **Heavy Pants** | Netherite Leggings surrounded by 8× Crying Obsidian | Grants absolute Void Tear immunity while draining legging durability instead of player HP. |
| **Augmented Helmet** | Netherite Helmet + Compass + Gold Ingot + Leather + Redstone + Stone (shaped) | Adds periodic virus-tracking pings that guide you to the nearest Virus Block or shield anchor. |

*(Drop your PNGs—`cured_infectious_cube.png`, `purification_totem.png`, etc.—into the repo’s `docs/` folder if you want the README to display the crafting layouts directly.)*

## 🛠️ Installation

> Placeholder instructions — adjust versions when implementing.

1. Install **Fabric Loader** (0.18.x or later)  
2. Install **Fabric API**  
3. Drop `virusblock-x.x.x.jar` into your `mods` folder  
4. Launch the game

Optional but recommended:

- Use shaders for maximum visual freakiness  
- Recommended performance mods: Sodium, Indium, Iris

---

## ⚙️ Configuration Reference

Most tuning happens through gamerules. Here are the high-impact ones (defaults in parentheses):

| Gamerule | Default | Effect |
| --- | --- | --- |
| `virusSurfaceCorruptAttempts` | `640` | Attempts per tick for Tier‑2 surface mutation (sand/ice/snow carpets). Raise for faster biome takeovers. |
| `virusLiquidMutationEnabled` | `true` | Enables corrupted water/lava once Tier 2 is reached. |
| `virusCorruptSandEnabled` / `virusCorruptIceEnabled` / `virusCorruptSnowEnabled` | `true` | Opt individual surface conversions in/out. |
| `virusTier2EventsEnabled` | `true` | Master toggle for Tier‑2 global events (Mutation Pulse, Skyfall, etc.). |
| `virusTier3ExtrasEnabled` | `true` | Unlocks Tier‑3+ extras (Void Tear, Inversion, Entity Duplication). |
| `virusEventMutationPulseEnabled` … `virusEventEntityDuplicationEnabled` | `true` | Fine-grained switches for each event (Mutation Pulse, Skyfall, Collapse Surge, Passive Revolt, Mob Buff Storm, Virus Bloom, Void Tear, Inversion, Entity Duplication). Use these if you want to cherry-pick specific events without disabling the whole tier. |
| `virusMatrixCubeMaxActive` | `200` | Cap on simultaneous Matrix Cubes raining from the sky. |
| `virusBoobytrapsEnabled` | `true` | Controls spontaneous boobytrap placement/explosions. |
| `virusWormsEnabled` | `true` | Allows corrupted dirt/boobytraps to spawn corrupted worms. |
| `virusSingularityAllowChunkGeneration` | `false` | Runtime switch; actual default comes from `config/the-virus-block/services.json -> singularity.execution`. Only when both the config and this gamerule are `true` will the collapse generate/load missing chunks. |
| `virusSingularityAllowOutsideBorderLoad` | `false` | Runtime switch; combined with the config, it determines whether chunks outside the active border can be touched. |
| `virusSingularityCollapseEnabled` | `true` | Live override for the collapse itself; both this gamerule and the config flag must remain `true` for the singularity collapse to progress. |

> **Server Config:** `config/the-virus-block/services.json` defines the hard defaults for chunk generation/outside-border loading, diagnostics, fuse timings, guardian FX, etc., while `config/the-virus-block/dimension_profiles/<dimension>.json` captures per-dimension collapse physics/FX. The gamerules above simply mirror those values at runtime.

### Singularity Services (`config/the-virus-block/services.json`)

```jsonc
{
  "singularity": {
    "execution": {
      "collapseEnabled": true,
      "allowChunkGeneration": true,
      "allowOutsideBorderLoad": true,
      "multithreaded": false,
      "workerCount": 4,
      "mode": "ring_slice"
    },
    "collapseBarDelayTicks": 60,
    "collapseCompleteHoldTicks": 40,
    "coreChargeTicks": 80,
    "resetDelayTicks": 160
  },
  "diagnostics": {
    "enabled": true,
    "logChunkSamples": true,
    "logBypasses": true,
    "logSampleIntervalTicks": 20,
    "logSpam": {
      "enableSpamDetection": true,
      "perSecondThreshold": 10,
      "perMinuteThreshold": 200,
      "suppressWhenTriggered": true
    }
  },
  "fuse": {
    "explosionDelayTicks": 400,
    "shellCollapseTicks": 20,
    "pulseIntervalTicks": 8
  }
}
```

### Dimension Profile (`config/the-virus-block/dimension_profiles/overworld.json`)

```jsonc
{
  "collapse": {
    "columns_per_tick": 8,
    "tick_interval": 20,
    "max_radius_chunks": 12,
    "mode": "erode",
    "ring_start_delay_ticks": 40,
    "ring_duration_ticks": 200,
    "barrier_start_radius": 120.0,
    "barrier_end_radius": 0.5,
    "barrier_duration_ticks": 1000,
    "chunk_pregen_enabled": true,
    "chunk_preload_enabled": true,
    "drain_water_ahead": true,
    "water_drain_offset": 1,
    "collapse_particles": false,
    "fill_mode": "air",
    "fill_shape": "outline",
    "outline_thickness": 2,
    "use_native_fill": true,
    "respect_protected_blocks": true,
    "radius_delays": [
      { "side": 1, "ticks": 150 },
      { "side": 3, "ticks": 100 },
      { "side": 9, "ticks": 40 },
      { "side": 15, "ticks": 20 }
    ]
  }
}
```

- `singularity.execution.allowChunkGeneration` / `allowOutsideBorderLoad` guard the chunk manager mixins; if either is `false`, the collapse skips new terrain regardless of gamerules.
- `diagnostics.enabled` enables per-tick collapse summaries (columns processed, skips, water cells cleared). `logChunkSamples` controls whether `SingularityChunkContext` flushes chunk/border digests, `logBypasses` controls bypass spam, and `logSampleIntervalTicks` throttles those messages.
- `drain_water_ahead` / `water_drain_offset` (dimension profile) control the proactive fluid pass that clears water/lava a few blocks ahead of the erosion front so oceans don’t explode into waterfalls mid-collapse.
- `singularity.execution.multithreaded` toggles the collapse worker scheduler. When enabled, slices are processed on background threads sized according to `workerCount` (rounded to multiples of four so you can request 4/8/12 on a 12-core machine).
- `respect_protected_blocks` keeps bedrock/negative-hardness blocks intact even during chunk wipes; turn it off for pure obliteration.
- `collapse.mode` selects between the two modern destruction paths: `"erode"` (outer shell in passes) and `"slab"`/future variants. The execution enum (`singularity.execution.mode`) currently selects between the ring slice implementations.
- `collapse_particles` gates the ash/sound spam for each cleared block; leave it `false` when profiling.
- `fill_mode` picks the `/fill` behavior used by the collapse: `"air"` replaces blocks directly, `"destroy"` mimics `/fill … destroy` (breaks blocks, spawns drops).
- `fill_shape` decides how each chunk is iterated while it is being hollowed out. `"matrix"` sweeps in XYZ order, while `"column"`, `"row"`, `"vector"`, and `"outline"` bias the carve for specific visuals or performance profiles; `outline_thickness` widens the outline strategy.
- `use_native_fill` lets you toggle between the high-speed direct block replacement path (`false`) and a vanilla-style fill (`true`) that fires block updates and respects drops at the cost of extra TPS.
- `singularity.execution.workerCount` is ignored until `multithreaded` is `true`, but once threading is on we reduce the value to the nearest multiple of four and clamp it to `availableCores - 1`.
- `collapse.tick_interval` is the per-batch cooldown in ticks; raising it slows the collapse cadence without touching throughput math.
- `singularity.execution.collapseEnabled` is the master kill-switch: when `false`, fusing still detonates after its timer but no singularity block is spawned and no terrain is chewed away. The gamerule `virusSingularityCollapseEnabled` can override this live without reopening the config.
- `fuse.explosionDelayTicks`, `fuse.shellCollapseTicks`, and `fuse.pulseIntervalTicks` control the fuse timeline (delay before detonation, shell collapse timing, ambient pulse cadence).
- `collapse.view_distance_chunks` / `simulation_distance_chunks` optionally override the vanilla player-manager distances during the collapse (set each to `0` to leave vanilla behaviour). The overrides are logged on the `singularity` channel and automatically restored once the event finishes.
- `collapse.broadcast_mode` / `broadcast_radius_blocks` control how aggressively we stream collapse block updates to clients. `immediate` mirrors vanilla; `delayed`/`summary` skip packets for chunks farther than the configured radius (in blocks) and replay them later when a player gets close (`summary` also logs a short digest).
- `collapse.radius_delays` define the tick cooldown for rings at or below the listed side length so you can slow the final rings without editing code.
- `chunk_pregen_*`/`chunk_preload_*` fields tune (or disable) the pre-generation/preload cadence and chunk budgets.
- `barrier_start_radius`, `barrier_end_radius`, and `barrier_duration_ticks` determine how far out the protective border spawns, how tight it shrinks, and how fast the interpolation runs.

#### Diagnostics Commands

Operators can tune these knobs live via `/virusblock singularity diagnostics …`:

- `enable|disable` – master switch for diagnostics sampling/logging.
- `chunksamples enable|disable` – toggles per-tick chunk/border summaries.
- `bypasses enable|disable` – toggles bypass stack logging.
- `interval <ticks>` – adjusts the sampling cooldown.
- `spam enable|disable` – gates the per-channel spam watchdog.
- `spam persecond <count>` / `spam perminute <count>` – updates rate thresholds.
- `spam suppress enable|disable` – decides whether spammy templates are muted after an alert.

#### Collapse Sync Commands

Use `/virusblock singularity viewdistance <chunks>` or `/virusblock singularity simulationdistance <chunks>` to update the overrides at runtime (set `0` to disable). The commands persist the values back into the active dimension profile JSON; the overrides are applied the moment a collapse arms and are restored automatically afterward.  
Use `/virusblock singularity broadcast mode <immediate|delayed|summary>` and `/virusblock singularity broadcast radius <blocks>` to switch the collapse broadcast profile without editing the config.

### Example Preset (`configs/virus_contained.mcfunction`)

```mcfunction
# Tier-2 visuals only, low chaos
/gamerule virusTier3ExtrasEnabled false
/gamerule virusTier2EventsEnabled true
/gamerule virusSurfaceCorruptAttempts 320
/gamerule virusLiquidMutationEnabled true
/gamerule virusBoobytrapsEnabled false
/gamerule virusMatrixCubeMaxActive 80
/gamerule virusWormsEnabled false
```

Drop this `.mcfunction` into a datapack and run `/function configs:virus_contained` to apply it instantly on a server.

 ----------------------------------------------------------------------------------------
 Thanks to shadertoy for offering such a resources and thanks to "DigvijaysinhG, flight404, foxes, tdhooper, Ark, and nimitz" for having shared their beautiful equations with the world. (And thanks to those who inspired them)

Thank you for all the Open Source Community for your support and for making this mod possible.
Ortibal Railgun pushed me to try and integrate visual effects into this world and it pushed me.
My imagination lead me to explore mesh and energy fields and customize them to the utmost extent.
