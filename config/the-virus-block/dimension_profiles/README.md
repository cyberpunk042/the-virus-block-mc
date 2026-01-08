# Dimension Profile Reference

Each JSON file in this folder defines how the Virus Block collapse behaves inside a specific dimension. Profiles share the same knobs, so you can copy properties between files and only override the values that need to change.

## Root Keys

**id** – Fully qualified resource location that ties the profile to a dimension. Keep it unique and stable; servers serialize this string when syncing active collapse states to clients.

- Format: `namespace:path` (e.g., `the-virus-block:overworld`, `the-virus-block:nether`).
- Match the infection scenario’s identifier so `/virusblock infection scenario` can locate it.

**collapse** – Primary group that governs how the shrinking wall is computed, paced, and communicated. Every field inside this object affects performance or fairness, so changes should be tested on a private server before going live.

- Omit nothing: missing keys fall back to `DimensionProfile.Collapse.defaults()`, which mirrors the Overworld profile.

**effects** – Cosmetic overrides that recolor the beam and swap particle types. They serve as a quick visual hint for players entering a given dimension.

- Leave it out to share the default magenta beam and sculk particles.

**physics** – Tunables for the knockback and suction forces that accompany the moving barrier. These values directly impact combat readability.

- Omitted blocks reuse the Overworld pull (0.35) and push radius (12).

## Collapse Knobs

**columns_per_tick** – Number of column slices processed per server tick. Higher numbers accelerate erosion but raise CPU cost, while smaller numbers make the wall look chunkier.

- `1-4` – Cinematic pacing for story moments or underpowered hardware.
- `5-8` – Balanced default (Overworld ships at 8).
- `9-12` – Emergency throughput; only use when you can spare extra CPU/GPU time.

**tick_interval** – Delay, in ticks, between collapse passes. Lower intervals feel relentless; larger ones create discrete pulses that give players time to react.

- `1-5` – Continuous grind; useful for PVP arenas.
- `10-20` – Default cadence that feels pulsed yet threatening.
- `>20` – Long pauses for puzzle beats or scripted cutscenes.

**max_radius_chunks** – Maximum collapse radius expressed in chunks from the origin. This defines the playable bubble and clamps any scripted expansion that might overshoot.

- `4-6` – Small dungeons or test arenas.
- `8-12` – Default overworld footprint (12 ≈ 192 block radius).
- `16+` – Mega events; pair with higher pregeneration budgets.

**mode** – Strategy used when selecting which columns to eat away. The default `erode` mode works from the outside inward, but custom modes can opt into spirals or checkerboard passes if implemented.

- `erode` – Canon ring-first erosion that matches the classic collapse.
- `slab` – Reserved for Nether experiments that chew horizontal layers (documented in `docs/virusworld_state_refactor.md`).
- `<custom>` – Scenario-specific hooks; unrecognized tokens quietly fall back to `erode`.

**ring_start_delay_ticks** – Time between the server enabling collapse logic and the first visible ring pulse. Bumping this up gives players a grace period to loot or reposition.

- `<=20` – Instant punishment.
- `40` – Stock grace period before the beam forms.
- `>=100` – Narrative breathing room between fuse collapse and wall motion.

**ring_duration_ticks** – Lifespan of each pulse once it spawns. Longer durations blend into a continuous beam, while short bursts look like discrete shockwaves.

- `<=80` – Distinct pulses (flash, fade, flash).
- `120-200` – Seamless beam with mild flicker (default is 200).
- `>=240` – Slow, lingering waves for dramatic reveals.

**barrier_start_radius** – Radius in blocks where the barrier initially spawns. Use a value slightly larger than `max_radius_chunks * 16` to avoid spurious chunk edges.

- `0` is invalid; Overworld defaults to `120`.
- Match or exceed the pregeneration radius to keep visuals in sync.
- Going beyond `256` begins to stretch particle meshes noticeably.

**barrier_end_radius** – Smallest permitted radius. Setting it near zero forces the wall to converge, whereas larger values leave a safe core for scripted finales.

- `0.5` – Canon zero-point; treats the last chunk as touching.
- `8-16` – Leaves a tiny stage for a boss arena.
- `>=32` – Keeps a town or loot vault intact after collapse.

**barrier_duration_ticks** – How long the barrier interpolates from start to end radius. Setting it below the total runtime makes the wall snap inward abruptly when the timer expires.

- `<=600` – Rapid pinch suitable for short matches.
- `1000` – Default; matches the scheduler’s collapse length.
- `>=1600` – Slow burn for multi-phase raids.

**barrier_auto_reset** – Boolean toggle that re-expands the barrier once it reaches the end radius. Enable it for cyclical events; leave it `false` for one-way collapses.

- `false` – Default; barrier stops and stays collapsed.
- `true` – Border expands outward again after `barrier_reset_delay_ticks`.

**barrier_reset_delay_ticks** – Cooldown before an auto-reset begins its outward sweep. Players can exploit this delay to regroup, so align it with raid pacing.

- `0` – Immediate rebound once the barrier finishes.
- `200` – Default breather between collapse cycles.
- `>=600` – Long intermission before the next sweep.

**chunk_pregen_enabled** – When `true`, the server pregenerates chunks within the specified radius, preventing hitching the moment the wall touches previously unseen terrain.

- `true` – Default safety net.
- `false` – Use when the map is already pregenerated or disk IO is precious.

**chunk_pregen_radius_blocks** – Outer radius in blocks for chunk pregeneration. Zero means “match the barrier start radius,” which is adequate unless the arena needs scenic backdrops.

- `0` – Derive from `barrier_start_radius`.
- `>0` – Manually overdraw distant silhouettes or skyboxes.

**chunk_pregen_chunks_per_tick** – Throughput cap for pregeneration. Keep it conservative to avoid saturating the disk pipeline when starting large events.

- `4` – Bare minimum for lightly populated servers.
- `8` – Default; safe on most hosts.
- `>=12` – Fast staging; monitor server watchdogs.

**chunk_preload_enabled** – Enables synchronous chunk loading as the barrier advances. This is cheaper than full pregeneration but still prevents players from seeing void strips.

- `true` – Default; ensures chunks stay resident during collapse.
- `false` – Skip if you manually ticket chunks or lean on /forceload.

**chunk_preload_chunks_per_tick** – Maximum number of chunks queued per tick when preloading. Tune alongside `chunk_preload_enabled` to match your TPS headroom.

- `2` – Gentle pacing for low-end hosts.
- `4` – Default sweet spot.
- `>=8` – Aggressive; use only when TPS headroom is proven.

**view_distance_chunks** – Optional override for player view distance inside the collapse zone. Leave at `0` to inherit the server-default slider.

- `0` – Respect the vanilla gamerule (`viewDistance`).
- `6-10` – Force tighter bubble so FPS stays high.
- `>=12` – Showcase panoramas; increases GPU cost.

**simulation_distance_chunks** – Similar override for simulation distance (mob AI, block updates). Lower numbers save CPU if the collapse area is mostly decorative.

- `0` – Use vanilla.
- `4-6` – Keep nearby mobs active while culling the outskirts.
- `>=8` – Needed only when redstone or farms sit near the border.

**broadcast_mode** – Determines how collapse updates reach nearby clients. This flag is mirrored by `/virusblock singularity broadcast mode`.

- `immediate` – Send every block/map update the tick it happens; ideal for small fights.
- `delayed` – Buffer updates for chunks beyond `broadcast_radius_blocks` until a tracked player enters the radius.
- `summary` – Same buffering as `delayed` plus `[Singularity] broadcast=...` log lines so ops can audit what was skipped.

**broadcast_radius_blocks** – Sphere radius (in blocks) within which players receive synchronized collapse data. Use wider radii for tall arenas so spectators stay in sync.

- `0` – Disable distance filtering; everyone sees updates.
- `32-96` – Default bands that cover the active arena.
- `>=128` – Stadium-scale viewing decks.

**default_sync_profile** – Name of a server-defined sync profile that controls how much data accompanies each broadcast. This ties into the network package registry under `net.cyberpunk042.infection.sync`.

- `full` – Bossbar, guardian laser, collapse HUD, and every particle; best for raid leaders.
- `cinematic` – Adds predictive schedule payloads (`SingularitySchedulePayload`) so storytelling clients receive full ring timing even if chunks are buffered.
- `minimal` – Suppresses non-essential notifications; good for spectators or staff recording footage.

**radius_delays** – Array describing per-side slowdown timers as the wall closes. Each entry delays collapse on one square “side” of the arena, letting you bias progression.

- `side` – Index of the side ring; lower numbers represent the outer perimeter (common table: 1, 3, 9, 15).
- `ticks` – Length of the pause before that side advances again (150/100/40/20 mimic Overworld pacing).
- Extend the table with larger `side` values when megastructures exceed 15 chunks per edge.

## Effect Knobs

**beam_color** – Hex string in `#RRGGBBAA` format that recolors the collapse laser. Vibrant colors read best against biome fog; avoid fully transparent alpha.

- `#C600FFFF` – Overworld magenta with opaque alpha.
- `#FF6A00FF` – Nether ember beam.
- Custom codes like `#00BCD4CC` work as long as they stay opaque enough to read against particles.

**veil_particles** – Namespaced particle ID sprayed across the shrinking veil. Choose particles that already exist client-side to avoid missing-texture clouds.

- `minecraft:sculk_soul` – Default spectral motes.
- `minecraft:ash` – Works nicely in Nether profiles.
- `the-virus-block:*` – Custom particle factories are supported; ensure the asset ships to both client and server.

**ring_particles** – Particle ID used for the traveling wavefront. High-density particles like `portal` can overwhelm GPUs in tight spaces, so profile on low-end hardware.

- `minecraft:portal` – Stock shimmering ring.
- `minecraft:lava` – Fits molten arenas but sprays more sprites.
- Light-use particles (`minecraft:dragon_breath`, `minecraft:dust_color_transition`) help when the beam already fills the screen.

## Physics Knobs

**ring_pull_strength** – Scalar applied to inward suction when the shockwave passes. Small decimals create gentle nudges; anything above `0.5` can juggle players uncontrollably.

- `0.1-0.25` – Cinematic tug without affecting combat.
- `0.3-0.4` – Default danger zone; noticeable but manageable.
- `>=0.5` – Harsh yank that may fling players into the beam; combine with high knockback only if that chaos is intentional.

**push_radius** – Distance from the ring at which outward knockback engages. Larger radii make melee combat near the barrier riskier but also prevent cheese tactics with bows.

- `8` – Tight bubble directly on the ring.
- `10-12` – Default; discourages point-blank camping.
- `>=16` – Arena-wide shove that keeps kite-heavy fights honest.

