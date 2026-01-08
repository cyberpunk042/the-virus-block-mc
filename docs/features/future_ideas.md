# Future Feature Ideas

A collection of promising ideas from brainstorming sessions.

---

## 🏆 TOP PICKS

### 1. Pocket Dimension (Safe Haven)

**Concept:** A personal/shared dimension players can enter to pause the virus.

**Mechanics:**
- Right-click a special item (Pocket Key? Sanctuary Stone?) to enter
- Personal dimension - each player has their own OR shared team dimension
- When ALL online players are inside pocket dimensions, the virus PAUSES
- Creates strategic coordination: "Everyone get to safety!"
- Dimension is small, can be upgraded/customized
- Cannot bring virus-related items inside (purification)
- Exiting returns you to where you entered

**Technical considerations:**
- Custom dimension type
- Player tracking for "all players in safety" check
- Tick pausing for virus world state
- Entry/exit teleportation

**Why it's good:**
- Gives players breathing room
- Creates coordination gameplay
- Safe space for planning/crafting
- Natural pacing mechanism

---

### 2. The Glitch (Reality Breakdown)

**Concept:** The virus corrupts the game itself, not just the world.

**Possible safe implementations:**
- Texture swaps (blocks look like other blocks temporarily)
- Fake particles that don't affect gameplay
- UI corruption (hearts/hunger display wrong but mechanics correct)
- Sound glitches (wrong sounds play)
- Fake chat messages from "the virus"
- HUD elements appearing/disappearing
- Screen shake/color shifts
- Fake "Achievement Unlocked" popups with creepy messages

**What to AVOID (crash risk):**
- Actual coordinate manipulation
- Breaking block states
- Modifying entity data incorrectly
- Changing actual game rules mid-tick

**Why it's good:**
- Meta-horror (the virus knows it's in a game)
- Unique to gaming medium
- Unsettling without being unfair

---

### 3. Afterimage Haunting (Phantom Block)

**Concept:** A ghostly afterimage of the virus block periodically manifests near players and attacks them with varied, random assault patterns.

**Mechanics:**
- Random spawn timer (every 2-10 minutes based on tier)
- Afterimage appears 10-20 blocks away from player, visible but translucent
- Cannot be damaged or destroyed - it's a phantom
- Stays for 5-15 seconds before fading
- During manifestation, launches RANDOM attacks at the player:

**Attack Types (randomly selected):**
- **Psychic Pulse:** Nausea + brief blindness, no physical damage
- **Void Beam:** Straight-line damage beam, must dodge sideways
- **Corruption Blast:** AOE explosion at player's feet, knockback
- **Spectral Arrows:** Volley of homing arrows, slow but persistent
- **Gravity Pull:** Yanks player toward the afterimage
- **Gravity Push:** Shoves player away violently
- **Mind Freeze:** Player can't move for 1-2 seconds
- **Item Scramble:** Swaps held item with random inventory slot
- **Echo Scream:** Plays heartbeat LOUD, direction confusion

Possibly reuse the progressive growth block to make it wobble, shrink, grow, fuse, explode, etc. !?

**Technical considerations:**
- Render as translucent entity, not solid block
- Client-side particle effects for visual flair
- Attack patterns as weighted random selection
- Distance from actual virus block affects frequency (further = more common)
- Timer persists across dimensions

**Why it's good:**
- The virus hunts you ANYWHERE
- Unpredictable - players can't memorize patterns
- Constant tension even in "safe" areas
- Reinforces the virus as a persistent, intelligent threat
- Great for jump scares without being cheap

---

### 4. Eternal Return (The Virus Never Dies)

**Concept:** Defeating the virus is never permanent. It will find its way back to you.

**Mechanics:**

**Time-Based Return:**
- After cleansing the virus, a hidden timer starts
- Every real-time hour after victory, chance of return increases by 5%
- At 24 hours: 100% chance of virus block appearing in inventory
- Appears during any inventory interaction (crafting, chest, etc.)
- Player gets a warning: "Something stirs in your belongings..."

**Dimensional Return:**
- Entering the Nether after defeating the virus: 25% chance per entry
- Entering the End: 50% chance per entry
- The virus sees dimension travel as weakness to exploit

**Escalating Curse:**
- First defeat: When it returns, you get 1 virus block
- Second defeat: Returns with 2 virus blocks
- Third defeat: Returns with 3 virus blocks
- And so on... infinitely
- Blocks appear in DIFFERENT inventory slots (can't just ignore one slot)

**Additional Escalation:**
- Each return, the virus starts at a HIGHER tier (min Tier 2 on 2nd, Tier 3 on 3rd, etc.)
- Cap at starting Tier 4 after 5th defeat

**Counter-play:**
- Special "Sealing Ritual" that requires rare items can delay return
- Staying in Pocket Dimension pauses the return timer
- Destroying blocks in creative doesn't count as "defeating" (no return trigger)

**Why it's good:**
- True roguelike tension - victory is temporary
- Escalating difficulty creates endgame challenge
- Forces players to LIVE with the virus, not just defeat it
- Multiple virus blocks = multiply the chaos
- Explains lore: the virus is bound to you specifically

---

### 5. Cinematic Death (Death Delay)

**Concept:** When you die to the virus, you don't just respawn. You're FORCED to watch your own death unfold in a dramatic, randomized cinematic sequence.

**Mechanics:**

**The Death Sequence:**
- Player dies → enters "Death Observer" state
- Camera pulls out to third-person, slowly orbiting the body
- Death animation plays (random from pool, 5-15 seconds)
- Player cannot skip, cannot move camera, must watch
- After animation, slow fade to black, THEN respawn

**Random Death Animations:**
- **Corruption Embrace:** Virus tendrils wrap around body, slowly pull it into the ground
- **Crystallization:** Body turns to corrupted crystal, shatters into particles
- **Void Consumption:** Body dissolves into void particles, spiraling away
- **Echo Split:** Body splits into multiple ghostly copies that fade one by one
- **Puppet Strings:** Body rises like a marionette, dances grotesquely, then collapses
- **Implosion:** Body crumples inward, compresses to a point, vanishes
- **Time Decay:** Body rapidly ages, crumbles to dust
- **Glitch Death:** Body flickers between states, corrupts visually, despawns like a glitched entity
- **Absorption:** Tendrils from nearby virus block reach out, visibly drain the body
- **Rebirth Failure:** Body tries to get up several times, failing each time, then goes still

**Audio:**
- Heartbeat slows during sequence
- Distorted music plays
- Final beat = respawn

**Technical considerations:**
- Lock player input during sequence
- Override death screen
- Spawn invisible "death cam" entity at body
- Play particle/entity animations client-side
- Keep body entity alive during animation, then remove

**Multiplayer twist:**
- Other players can SEE your death sequence
- Creates shared horror moments
- "I watched him die... it was horrible"

**Why it's good:**
- Death has WEIGHT
- Each death is memorable
- Horror atmosphere amplified
- Encourages players to NOT die (extra motivation)
- Unique to this mod - no other mod does this

---

### 6. Color Drain (Random Event)

**Concept:** A random atmospheric event where the world temporarily loses all color, except for the virus and corruption-related elements.

**Mechanics:**

**Trigger:**
- Random chance every 5-15 minutes during active infection
- Higher chance at higher tiers
- Tier 5+: guaranteed at least once per hour

**The Event:**
- World suddenly desaturates to grayscale over 1-2 seconds
- Duration: 15-30 seconds
- ONLY virus blocks, corrupted blocks, and infection-related particles retain their purple/red color
- Creates stark visual contrast: gray world, vivid infection

**Visual Details:**
- Grayscale shader applied to everything
- Virus block glows MORE intensely during drain
- Corruption particles become extra vibrant
- Players appear as gray silhouettes
- Sky goes gray, sun/moon become white circles

**Gameplay Implications:**
- Helps players LOCATE infection sources (follow the color)
- Disorienting but useful
- Can happen during combat (challenge)
- Announces virus "mood" - it's flexing

**Audio Accompaniment:**
- Muffled sound effect when starting
- Low hum during duration
- Pop/release sound when color returns

**Technical considerations:**
- Client-side shader effect
- Server sends trigger packet
- Exclusion list for colored rendering (virus blocks, particles)
- Smooth transition in/out

**Why it's good:**
- Visually stunning
- Practical use (finding infection)
- Atmospheric horror
- Low implementation cost, high impact
- Unique to this mod

---

### 7. Chromatic Aberration Surge (Visual Effect)

**Concept:** The virus causes your vision to fragment - colors separate at edges, creating a disorienting RGB split effect that intensifies during dangerous moments.

**Mechanics:**

**Trigger Conditions:**
- Proximity to virus block (closer = stronger)
- During damage taken
- During tier events
- Random surges at Tier 4+
- Afterimage phantom attacks

**Intensity Levels:**
- **Subtle (Level 1):** Slight color fringing at screen edges, barely noticeable
- **Moderate (Level 2):** Clear RGB separation, noticeable on all edges
- **Intense (Level 3):** Heavy aberration, center of screen stable but edges are fragmented rainbows
- **Critical (Level 4):** Full-screen separation, red/green/blue images visibly offset from each other

**Visual Details:**
- RGB channels offset in different directions
- Red shifts left, Blue shifts right, Green stays centered (classic aberration)
- Offset distance scales with intensity
- Moving your view makes the effect MORE noticeable
- Standing still = effect stabilizes slightly

**Dynamic Behavior:**
- Pulses with the heartbeat rhythm
- Spikes during damage events
- Builds gradually as you stay in infected zones
- Decays when leaving infected areas (10-15 second fade)

**Gameplay Implications:**
- Harder to aim precisely during surges
- Indicator of danger level (worse = closer to threat)
- Creates visceral discomfort (players want to escape)
- Can be partially countered by standing still

**Technical considerations:**
- Post-processing shader effect
- Server sends intensity value (0.0 - 1.0)
- Client interpolates for smooth transitions
- Combine with existing sky corruption effects
- Performance: single-pass shader, minimal GPU impact

**Audio Pairing:**
- High-pitched ringing at high intensities
- Heartbeat becomes distorted during surge
- Static noise at critical levels

**Why it's good:**
- Visceral horror feedback
- Clear danger indicator
- Unique visual style
- Works with other effects (Color Drain + Chromatic = terrifying)
- Used in AAA horror games (proven effective)

---

### 8. Weakness Window (Combat Mechanic)

**Concept:** Infected mobs and the virus itself have brief vulnerability windows. Learning the "tells" and timing your attacks during these windows rewards skill with massive damage multipliers.

**Mechanics:**

**How Windows Work:**
- Mobs have attack cycles with specific phases
- One phase is the "weakness window" (usually post-attack recovery)
- Attacking during window = 3x damage (or more)
- Attacking outside window = normal damage
- Window duration varies by mob type (0.5s - 2s)

**Visual Tells (Learn These):**
- **Zombie variants:** Window opens when they lunge and miss. Arms forward = vulnerable back.
- **Skeletons:** Brief pause after shooting an arrow. Bow lowered = strike NOW.
- **Spiders:** After a pounce lands/misses. On ground recovering.
- **Creepers:** The moment BEFORE explosion starts (risky but devastating).
- **Endermen:** After teleporting. Brief disorientation.
- **Virus Mobs (custom):** Unique patterns per mob type.

**Virus Block Weakness:**
- The virus block itself has windows during tier events
- Specific events create vulnerability moments
- Skyfall aftermath = window
- Post-mutation pulse = window
- Learn the patterns, exploit them

**Visual Feedback:**
- Subtle particle effect when window opens (brief glow/flash)
- Hit during window = distinct sound (satisfying crunch)
- Damage numbers appear larger/colored for window hits
- Miss the window = normal feedback

**Skill Progression:**
- Early game: windows are hard to notice
- Experienced players: read tells instantly
- Mastery: predict windows before they open

**Window Modifiers:**
- Higher tiers = shorter windows (harder timing)
- Apocalypse mode = windows are nearly invisible
- Some buffs extend window duration
- Some debuffs (from virus) make windows invisible

**Technical considerations:**
- Mob state machine tracks current phase
- Window flag set during specific states
- Damage multiplier applied during flag
- Client receives subtle visual cue packet
- Performance: minimal overhead (boolean check)

**Why it's good:**
- Rewards skill and observation
- Makes combat feel like a dance
- Creates mastery curve
- Differentiates experienced players
- Souls-like feel without being unfair

---

### 9. Name Titles (Achievement System)

**Concept:** Players earn visible titles above their nameplates based on achievements and milestones during infection fights. These titles are badges of honor that tell your story at a glance.

**Mechanics:**

**How Titles Work:**
- Earn titles by completing specific challenges
- Only ONE title displayed at a time (player chooses)
- Titles appear above nameplate in multiplayer
- Some titles are permanent, some are temporary
- Titles persist across server sessions

**Title Categories:**

**Combat Titles:**
- **"The Untouchable"** - Survive Tier 5 without taking damage for 10 minutes
- **"Virus Slayer"** - Cleanse the infection 5 times
- **"Deathless"** - Complete a cleanse without dying once
- **"Berserker"** - Kill 100 infected mobs in a single tier
- **"Glass Cannon"** - Defeat singularity with no armor equipped

**Survival Titles:**
- **"Last Standing"** - Be the only survivor in multiplayer when virus peaks
- **"Nomad"** - Travel 10,000 blocks during active infection
- **"Hoarder"** - Maintain 64 stacks of items throughout infection
- **"Minimalist"** - Cleanse with less than 9 inventory slots used

**Skill Titles:**
- **"Perfect Timing"** - Land 50 weakness window hits in one session
- **"Parry Master"** - Block 100 projectiles
- **"Speed Demon"** - Cleanse in under 1 real-time hour

**Humor Titles:**
- **"Professional Coward"** - Run away from 100 encounters
- **"Hug Enthusiast"** - Touched by virus 1000 times
- **"Explodey"** - Die to explosions 50 times

**Rare Titles:**
- **"The Chosen"** - First player on server to cleanse
- **"Legacy"** - Cleanse 10 times on same world
- **"Eternal Struggle"** - Face returning virus 5 times

**Technical considerations:**
- Titles stored in player NBT data
- Server validates title claims
- Title selection in custom UI
- Display via nameplate mixin
- Sync titles to all clients in render range

**Why it's good:**
- Visible bragging rights
- Goal-oriented gameplay
- Community recognition
- Encourages varied playstyles
- Low implementation cost

---

### 10. Speedrun Timer (Competitive Feature)

**Concept:** A visible, accurate timer that tracks infection duration from start to cleanse. Supports competitive speedrunning with split tracking and potential leaderboard integration.

**Mechanics:**

**Timer Features:**
- Starts automatically when infection begins
- Displays in corner of screen (toggleable)
- Stops when infection is cleansed
- Shows hours:minutes:seconds:milliseconds
- Persists through deaths (doesn't reset)

**Split Tracking:**
- Records time when each tier is reached
- Shows current split vs best split
- Green = ahead of personal best
- Red = behind personal best
- Split history saved per world

**Categories:**
- **Any%** - Just cleanse, any method
- **No Deaths** - Cleanse without dying
- **All Tiers** - Experience every tier before cleansing
- **Low%** - Minimal resources used
- **Glitchless** - No exploit usage

**Display Modes:**
- **Minimal** - Just current time
- **Splits** - Current time + split comparisons
- **Full** - Detailed breakdown with per-tier stats
- **Hidden** - Timer runs but not displayed (for casual play)

**Post-Run Stats:**
- Total time
- Time per tier
- Deaths count
- Damage taken/dealt
- Resources consumed
- Compare to previous runs

**Leaderboard Integration (Optional):**
- Submit times to server leaderboard
- View top times for the world seed
- Verification via replay recording
- Separate leaderboards per category

**Technical considerations:**
- World-persistent timer stored in infection state
- Client-side display with server sync
- Split data saved in player/world NBT
- Export runs to shareable format
- Integration with common speedrun timer formats

**Why it's good:**
- Adds replayability
- Community competition
- Content creator appeal
- Measures improvement
- Supports modpack competitive scenes

---

### 11. Void Eclipse (World Event)

**Concept:** The sun goes black. A terrifying world event where the virus temporarily consumes the light itself, plunging the world into total darkness with intensified mob spawning.

**Mechanics:**

**Trigger Conditions:**
- Random chance at Tier 4+ (rare)
- Guaranteed once during singularity phase
- Can be forced by player action (specific ritual?)
- Lasts 2-5 minutes real-time

**Visual Effects:**
- Sun turns pitch BLACK with dark purple corona
- Sky fades from normal → deep purple → near-black
- Stars become visible during "day"
- Moon (if visible) also darkens
- World light level drops to 0-1 everywhere
- Only artificial lights work (and they flicker)

**Gameplay Effects:**
- Hostile mobs spawn EVERYWHERE regardless of light level
- Normal mob cap temporarily tripled
- Passive mobs panic and run
- Visibility reduced to ~8 blocks even with torches
- Heartbeat intensifies dramatically
- Player night vision potions have reduced effectiveness

**Special Spawns During Eclipse:**
- Endermen spawn in Overworld abnormally
- Phantoms spawn regardless of sleep state
- Rare "Void Walker" entities only appear during eclipse
- All spawned mobs have slight damage/speed buff

**Warning Signs (30 seconds before):**
- Sun starts dimming
- Birds flee (particle effect)
- Ambient sound goes silent
- Heartbeat pitch drops
- "The light fails..." message

**Audio Design:**
- Deep, resonant bass drone
- Wind sounds (despite no wind particles)
- Distant screaming/wailing ambient
- Heartbeat becomes slow, heavy, ominous

**Player Strategies:**
- Rush to shelter
- Light EVERYTHING (diminishing returns but helps)
- Defend a single point
- Use this time to track virus (it glows during eclipse)
- Prepare for the end (stock up, plan)

**Eclipse End:**
- Light gradually returns over 15 seconds
- Mobs spawned during eclipse burn in returning sun
- Brief "golden hour" with no new spawns
- Calm before continuation

**Technical considerations:**
- Override sky light calculation temporarily
- Custom sky renderer during event
- Mob spawning rule bypass
- Server-synced event state
- Performance: mob cap still respected, just raised

**Why it's good:**
- Unforgettable visual
- Creates urgent survival pressure
- Rewards preparation
- Cinematic horror moment
- Virus feels truly powerful

---

### 12. Mercy System (Dynamic Assistance)

**Concept:** The more you die, the more the game helps you. A hidden assistance system that gradually provides struggling players with cured items, protective gear, and buffs - making the experience accessible without explicit difficulty settings.

**Mechanics:**

**Death Counter Tracking:**
- Hidden counter tracks deaths during current infection
- Counter persists through respawns but resets on cleanse
- Each milestone unlocks new assistance tier
- Players never see the exact number (preserves dignity)

**Assistance Tiers:**

**Tier 1 (3-5 deaths):**
- Random Cured Infectious Cubes appear in inventory periodically
- Slight health regeneration buff in safe zones
- Message: "Something stirs in your favor..."

**Tier 2 (6-10 deaths):**
- Rubber Boots spawn in inventory (force field resistance)
- Heavy Pants spawn (knockback resistance)
- Mobs deal 10% less damage
- Message: "The world offers protection..."

**Tier 3 (11-15 deaths):**
- Personal Shield activates briefly after respawn (5 seconds invulnerability)
- +2 max hearts permanently for this run
- Healing items appear in inventory periodically
- Message: "You are not alone..."

**Tier 4 (16-20 deaths):**
- Constant weak regeneration effect
- Netherite-tier protective gear appears gradually
- Tier progression slows by 25%
- Message: "The balance shifts..."

**Tier 5 (21+ deaths):**
- Strong personal shield (absorbs 50% damage)
- Full protective gear set if not already equipped
- Mobs target you less frequently
- Heartbeat range drastically reduced
- Message: "Even the virus shows mercy..."

**Items That May Appear:**
- Cured Infectious Cubes
- Rubber Boots (force field immunity)
- Heavy Pants (knockback resistance)
- Protective Helmets (psychic damage reduction)
- Healing Potions
- Purification Totems
- Shield Generators

**Buff Escalation:**
- Max health increases (+2 hearts per tier)
- Damage resistance increases (5% per tier)
- Regeneration rate increases
- Cooldowns on deaths reduced
- Hunger drain slowed

**Design Philosophy:**
- NEVER punish players for dying
- Invisible system preserves player agency
- No "easy mode" stigma
- Hardcore players can ignore (gear is optional)
- Accessible without being mandatory

**Technical considerations:**
- Death counter stored in player NBT per world
- Assistance items spawned on respawn event
- Buff effects applied as hidden status effects
- Reset on infection cleanse
- Optional server config to disable (for hardcore servers)

**Why it's good:**
- Inclusive game design
- Prevents frustration spiral
- No explicit "easy mode" button
- Rewards persistence (keep trying!)
- Matches modern game accessibility standards

---

## � TESSELLATED FIELD IDEAS

### 13. Color Gradient Across Faces

**Concept:** Each tessellated face has a slightly different color, creating beautiful gradient effects across the entire shape.

**Mechanics:**
- Color assigned per-face based on position (angle, height, distance from center)
- Different shapes create different gradient patterns:
  - **Sphere:** Smooth pole-to-pole gradient, equator band effects
  - **Prism:** Vertical stripes, cap vs side differentiation
  - **Icosahedron:** Each of 20 faces distinct, creates gem-like faceting
  - **Cylinder:** Radial gradient around circumference
  - **Cone:** Base to tip gradient
  - **Torus:** Inner vs outer ring differentiation

**Gradient Modes:**
- **Polar:** Based on Y-axis position (top to bottom)
- **Radial:** Based on distance from center
- **Angular:** Based on angle around Y-axis (rainbow wheel)
- **Normal-based:** Color from face normal direction (RGB = XYZ)
- **Random seed:** Each face gets random color from palette

**Technical:** Pass face index/normal to shader, compute gradient in fragment shader.

---

### 14. Configurable Face Animation Sequences

**Concept:** Faces flash, pulse, or animate in configurable patterns - treating the tessellation as a programmable display.

**Animation Types:**
- **Wave:** Color wave ripples across faces from origin point
- **Random Flash:** Faces flash randomly with configurable density
- **Sequential:** Faces light up in order (spiral, row-by-row, random)
- **Breathing:** All faces pulse together in sync with heartbeat
- **Chase:** Pattern that moves around the shape continuously

**Configuration:**
- Speed, direction, colors, blend modes
- Per-field-definition customizable
- Can be tied to game events (tier changes, damage, etc.)

---

### 15. Projectile Deflection Shield (Debug → Feature)

**Concept:** Tessellated field that actively deflects projectiles, with deflection angle determined by the face angle.

**Mechanics:**
- Projectile hits a triangular face
- Face normal determines reflection direction
- Projectile bounces at physically accurate angle
- Works for: arrows, fireballs, wind charges, etc.

**Advanced Options:**
- Absorption mode (projectile destroyed, no bounce)
- Velocity reduction on bounce (energy loss)
- Face damage on deflection (shield durability per-face)
- Deflect explosions: blast force redirected by face normals

**Current State:** Debug feature - needs promotion to full gameplay mechanic.

---

### 16. Rotating Constellation Field

**Concept:** Lines connect vertices like constellation stars, rotating slowly to create mesmerizing celestial effects.

**How To Achieve:**
1. **Base mesh:** Your normal tessellated shape (icosahedron, etc.)
2. **Constellation layer:** Additional render pass drawing lines between non-adjacent vertices
3. **Pattern generation:** 
   - Connect every Nth vertex
   - Skip connections to create star patterns
   - Multiple overlay patterns at different rotations
4. **Animation:** Slowly rotate the constellation pattern independent of the mesh
   - Or: mesh rotates one way, constellation rotates opposite way
5. **Visual effect:** Particles/glow at vertices where lines meet

**Technical Approach:**
- Generate constellation edge list at field creation
- Render as separate line geometry with alpha
- Apply rotation matrix to vertex positions
- Vertex shader handles rotation, fragment handles glow

**Variations:**
- Constellation appears only during specific events
- Pattern complexity increases with tier
- Player can "draw" their own constellation by touching vertices

---

### 17. Holographic Texture Mapping

**Concept:** Project images, symbols, or patterns onto the tessellated surface for sci-fi/holographic effects.

**Is it possible?** Yes! Via shader-based texture projection.

**Implementation Approaches:**

**Triplanar Projection (Recommended - works for all shapes):**
```glsl
// Sample texture from 3 directions, blend based on normal
vec4 xProj = texture(holoTex, position.yz);
vec4 yProj = texture(holoTex, position.xz);
vec4 zProj = texture(holoTex, position.xy);
vec3 blend = abs(normal);
blend /= (blend.x + blend.y + blend.z);
color = xProj * blend.x + yProj * blend.y + zProj * blend.z;
```

**Spherical Projection (Good for spheres/icosahedrons):**
```glsl
vec2 uv = vec2(atan(pos.z, pos.x) / 6.28 + 0.5, pos.y * 0.5 + 0.5);
color = texture(holoTex, uv);
```

**Use Cases:**
- Virus warning symbols on shields
- Player emblems on personal fields
- Animated warning patterns
- Sci-fi grid overlays

**Complexity:** Medium - requires texture binding and UV generation in shader.

---

### 18. Field Appearance Animation

**Concept:** Animate field spawning with smooth scale, opacity, or reveal transitions.

**Animation Types:**

**1. Scale Animation (0 → 1):**
```glsl
float eased = 1.0 - pow(1.0 - spawnProgress, 3.0); // Ease-out
vec3 scaledPos = position * eased;
```

**2. Opacity Fade:**
```glsl
float alpha = spawnProgress * targetOpacity;
```

**3. Scale + Opacity Combo (Recommended):**
```glsl
float eased = smoothstep(0.0, 1.0, spawnProgress);
vec3 pos = position * eased;
float alpha = eased * targetOpacity;
```

**4. Face-by-Face Reveal:**
- Each face has activation delay
- Spiral outward, random, or wave pattern
- Creates "assembling" effect

**5. Edge Trace Animation:**
- Edges draw like pen strokes
- Faces fill after edges complete
- Very sci-fi aesthetic

**6. Vertex Explosion → Collapse:**
- Vertices start scattered
- Animate toward correct positions
- "Materializing from chaos" look

**7. Unfolding Origami:**
- Faces start at center
- Unfold outward to final positions
- Like paper folding in reverse

**Parameters to expose:**
- `spawnDuration` (seconds)
- `animationType` (enum)
- `easingFunction` (linear, easeOut, elasticOut, etc.)
- `delayPattern` (spiral, random, bottom-up, etc.)

---

### 19. Kaleidoscope Mode

**Concept:** Mirror/reflect patterns across segmented wedges, creating mesmerizing symmetrical animations.

**How It Works:**
Kaleidoscopes divide a circle into wedges and mirror the pattern in each wedge.

**Shader Implementation:**
```glsl
// Convert to polar
float angle = atan(position.z, position.x);
float radius = length(position.xz);

// Fold into wedge (6 segments = 60° each)
int segments = 6;
float wedgeSize = 6.28318 / float(segments);
float foldedAngle = mod(angle, wedgeSize);

// Mirror every other segment
if (mod(floor(angle / wedgeSize), 2.0) == 1.0) {
    foldedAngle = wedgeSize - foldedAngle;
}

// Sample pattern using folded coordinates
vec2 kaleidoUV = vec2(foldedAngle / wedgeSize, radius);
color = texture(patternTex, kaleidoUV);
```

**Variations:**
- **Static:** Pattern doesn't move
- **Rotating:** Wedge boundaries slowly rotate
- **Animated Source:** Pattern in source wedge animates, all mirrors follow
- **Variable Segments:** Increase segments over time (3 → 6 → 12)

**Parameters:**
- `segmentCount` (3, 4, 6, 8, 12...)
- `rotationSpeed`
- `patternSource` (texture, noise, gradient)

**Visual Result:** 6-fold (or N-fold) symmetry like a real kaleidoscope toy.

**Complexity:** Medium - the folding math is straightforward once understood.

---

### 20. Animated Noise Texture

**Concept:** Perlin/Simplex noise flows across the surface like living electricity. Creates organic, breathing effect.

**Difficulty:** ⭐⭐ Easy - Standard shader technique

**Implementation:**
```glsl
// In fragment shader
uniform float time;
uniform float scale;
uniform float speed;

// Using simplex or perlin noise function
float noise = snoise(position * scale + time * speed);

// Apply to color
vec3 baseColor = vec3(0.2, 0.0, 0.4); // Purple base
vec3 glowColor = vec3(0.8, 0.2, 1.0); // Bright purple glow
color = mix(baseColor, glowColor, noise);
```

**Variations:**
- **Flowing:** Noise moves in one direction (flow field)
- **Pulsing:** Noise intensity oscillates with time
- **Turbulent:** Multiple noise octaves for chaotic look
- **Directional:** Flow follows face normals

**Parameters:**
- `noiseScale` - Size of noise pattern
- `flowSpeed` - Animation speed
- `octaves` - Complexity (1 = simple, 4+ = turbulent)

---

### 21. Klein Bottle Shape

**Concept:** 4D shape projected into 3D - creates mind-bending geometry where inside becomes outside.

**Difficulty:** ⭐⭐⭐ Medium - Unusual math but same technique as other shapes

**The "4D" Part Explained:**
A Klein Bottle is just a mesh defined by parametric equations. The "4D" aspect is conceptual - in 3D it self-intersects, which looks strange but renders normally.

**Parametric Equations:**
```java
// Generate vertices for Klein Bottle
// u, v range from 0 to 2π
for (float u = 0; u < TWO_PI; u += step) {
    for (float v = 0; v < TWO_PI; v += step) {
        float r = 4.0f * (1.0f - cos(u) / 2.0f);
        
        float x, y, z;
        if (u < PI) {
            x = 6 * cos(u) * (1 + sin(u)) + r * cos(u) * cos(v);
            y = 16 * sin(u) + r * sin(u) * cos(v);
        } else {
            x = 6 * cos(u) * (1 + sin(u)) + r * cos(v + PI);
            y = 16 * sin(u);
        }
        z = r * sin(v);
        
        vertices.add(new Vec3d(x, y, z));
    }
}
// Connect into triangles like any other parametric surface
```

**Rendering Challenges:**
- Self-intersection needs alpha blending or special handling
- Normals are tricky at transition points
- Consider two-sided rendering

---

### 22. Hyperbolic Surface (Saddle Shape)

**Concept:** Surface that curves in opposite directions - creates elegant mathematical forms.

**Difficulty:** ⭐⭐⭐ Medium - Standard parametric surface

**Hyperbolic Paraboloid (Simple Saddle):**
```java
// "Pringle chip" shape
// x, y range from -1 to 1
for (float x = -1; x <= 1; x += step) {
    for (float y = -1; y <= 1; y += step) {
        float z = x * x - y * y; // Saddle equation
        vertices.add(new Vec3d(x, y, z));
    }
}
```

**Pseudosphere (More Complex):**
```java
// Tractricoid shape - looks like a horn/funnel
// u from 0 to 2π, v from 0.01 to ~3
for (float u = 0; u < TWO_PI; u += step) {
    for (float v = 0.01f; v < 3.0f; v += step) {
        float x = cos(u) / cosh(v);
        float y = sin(u) / cosh(v);
        float z = v - tanh(v);
        vertices.add(new Vec3d(x, y, z));
    }
}
```

**Visual Appeal:**
- Elegant mathematical curves
- Great for force field applications (saddle points in physics!)
- Unusual profile catches eye

---

### 23. DNA Helix Shape

**Concept:** Double helix spiral - two intertwined strands with connecting "rungs."

**Difficulty:** ⭐⭐ Easy - Simple parametric curve

**Implementation:**
```java
float radius = 1.0f;
float twistRate = 2.0f; // Rotations per unit length
float length = 10.0f;
float rungInterval = 0.5f;

List<Vec3d> strand1 = new ArrayList<>();
List<Vec3d> strand2 = new ArrayList<>();
List<Pair<Vec3d, Vec3d>> rungs = new ArrayList<>();

for (float t = 0; t < length; t += step) {
    // First strand
    float angle1 = t * twistRate;
    strand1.add(new Vec3d(
        radius * cos(angle1),
        t, // Y is the vertical axis
        radius * sin(angle1)
    ));
    
    // Second strand (offset by π)
    float angle2 = angle1 + PI;
    strand2.add(new Vec3d(
        radius * cos(angle2),
        t,
        radius * sin(angle2)
    ));
    
    // Add rung at intervals
    if (t % rungInterval < step) {
        rungs.add(new Pair<>(strand1.getLast(), strand2.getLast()));
    }
}
```

**Rendering Options:**
- **Wireframe:** Just the curves + rungs as lines
- **Tube geometry:** Extrude circles along each strand
- **Ribbon:** Flat strips that twist along the helix
- **Points:** Vertices as glowing orbs

**Variations:**
- Triple/Quadruple helix
- Variable radius along length
- Animated twist (DNA "unzipping")
- Glowing rungs that pulse

---

### 24. Rainbow Pattern

**Concept:** Hue-cycling color pattern that creates vivid rainbow effects across the tessellated surface.

**Difficulty:** ⭐⭐ Easy - Simple hue calculation

**Implementation:**
```glsl
// In fragment shader
uniform float time;
uniform float speed;

// Get angle around Y-axis (or use UV, or position-based)
float angle = atan(position.z, position.x);

// Normalize to 0-1 range
float hue = (angle / 6.28318) + 0.5;

// Optional: animate the hue shift
hue = fract(hue + time * speed);

// Convert HSV to RGB (hue, full saturation, full value)
vec3 rainbow = hsv2rgb(vec3(hue, 1.0, 1.0));
color = rainbow;
```

**HSV to RGB Helper:**
```glsl
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
```

**Pattern Variations:**
- **Radial Rainbow:** Hue based on distance from center
- **Vertical Rainbow:** Hue based on Y position (top to bottom)
- **Angular Rainbow:** Hue based on angle around axis (pie slices)
- **Spiral Rainbow:** Combine angle + height for spiral bands
- **Animated Sweep:** Rainbow rotates around the shape

**Parameters:**
- `rainbowSpeed` - Animation speed (0 = static)
- `saturation` - 0.0 (grayscale) to 1.0 (vivid)
- `brightness` - Overall intensity
- `direction` - Radial, vertical, angular, spiral

---

### 25. Spiral Path Pattern

**Concept:** Color bands spiral from pole to pole like a barber pole. Creates dynamic, eye-catching stripes.

**Difficulty:** ⭐⭐ Easy - Combine angle and height

**Implementation:**
```glsl
// In fragment shader
uniform float spiralTightness; // How many wraps pole-to-pole
uniform float time;
uniform float speed;

// Get position in polar-ish coordinates
float angle = atan(position.z, position.x); // -π to π
float height = position.y; // Normalized -1 to 1 ideally

// Combine angle and height for spiral
float spiral = angle + height * spiralTightness;

// Animate rotation
spiral += time * speed;

// Create bands
float band = sin(spiral * bandCount) * 0.5 + 0.5;

// Two-color stripe
vec3 color1 = vec3(1.0, 0.0, 0.0); // Red
vec3 color2 = vec3(1.0, 1.0, 1.0); // White
color = mix(color1, color2, step(0.5, band)); // Hard edge
// Or: color = mix(color1, color2, band); // Soft gradient
```

**Variations:**
- **Barber Pole:** Red/white/blue classic stripes
- **Candy Cane:** Red/white diagonal stripes
- **Rainbow Spiral:** HSV hue follows spiral path
- **Double Helix:** Two intertwined spiral bands
- **Animated Spin:** Spiral rotates around the shape

**Parameters:**
- `spiralTightness` - Number of rotations pole-to-pole
- `bandCount` - How many color bands
- `rotationSpeed` - Animation speed
- `colors[]` - Array of colors to cycle through

**Shape Considerations:**
- **Sphere:** Classic spiral from bottom to top
- **Cylinder:** Spiral wraps around sides
- **Cone:** Spiral tightens toward apex
- **Torus:** Spiral follows the tube around the ring

---

### 26. Voronoi Cells Pattern

**Concept:** Organic cell-like divisions across the surface. Creates natural, biological textures.

**Difficulty:** ⭐⭐⭐ Medium - Requires Voronoi noise function

**Implementation:**
```glsl
// Voronoi noise function
vec2 voronoi(vec3 p, float scale) {
    vec3 sp = p * scale;
    vec3 cell = floor(sp);
    
    float minDist = 1e10;
    vec3 closestPoint;
    
    // Check 3x3x3 neighborhood
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 neighbor = cell + vec3(x, y, z);
                // Random point within cell
                vec3 point = neighbor + hash33(neighbor);
                float dist = length(sp - point);
                if (dist < minDist) {
                    minDist = dist;
                    closestPoint = point;
                }
            }
        }
    }
    
    return vec2(minDist, hash13(closestPoint)); // Distance and cell ID
}

// Usage
vec2 v = voronoi(position, cellScale);
float cellEdge = smoothstep(0.02, 0.05, v.x); // Edge detection
vec3 cellColor = hsv2rgb(vec3(v.y, 0.7, 0.9)); // Random color per cell
color = mix(edgeColor, cellColor, cellEdge);
```

**Variations:**
- **Cell coloring:** Each cell a random/gradient color
- **Edge highlighting:** Glowing lines between cells
- **Cracked appearance:** Damage visualization
- **Animated cells:** Cell centers slowly drift

**Parameters:**
- `cellScale` - Size of cells
- `edgeWidth` - Thickness of borders
- `edgeColor` - Color of cell boundaries
- `cellColorMode` - Random, gradient, uniform

---

### 27. Starburst Pattern

**Concept:** Lines radiating from poles outward like sun rays. Creates dramatic, energetic visuals.

**Difficulty:** ⭐⭐ Easy - Angle-based pattern

**Implementation:**
```glsl
// In fragment shader
uniform int rayCount;
uniform float rayWidth;
uniform float time;

// Calculate angle from pole (using X-Z plane)
float angle = atan(position.z, position.x);

// Create rays (angular divisions)
float ray = abs(sin(angle * float(rayCount)));

// Sharpen edges
ray = smoothstep(1.0 - rayWidth, 1.0, ray);

// Optional animation: rotate rays
float animatedAngle = angle + time * rotationSpeed;
float animatedRay = abs(sin(animatedAngle * float(rayCount)));

// Apply to color
vec3 rayColor = vec3(1.0, 0.9, 0.3); // Gold
vec3 gapColor = vec3(0.2, 0.1, 0.0); // Dark
color = mix(gapColor, rayColor, ray);
```

**Variations:**
- **Sun burst:** Rays emanate from center
- **Polar rays:** Rays from both top and bottom poles
- **Asymmetric:** Different ray counts from each pole
- **Gradient rays:** Rays fade toward equator
- **Animated spin:** Rays rotate around axis

**Parameters:**
- `rayCount` - Number of rays (8, 12, 16, 24...)
- `rayWidth` - Thickness of each ray (0.0 - 1.0)
- `rayFalloff` - How quickly rays fade from pole
- `rotationSpeed` - Animation speed

---

### 28. Dots/Stipple Pattern (Halftone)

**Concept:** Pattern of dots across surface creating halftone/stipple effect. Retro printing aesthetic.

**Difficulty:** ⭐⭐ Easy - Grid-based dot pattern

**Implementation:**
```glsl
// In fragment shader
uniform float dotScale;
uniform float dotSize;
uniform float time;

// Create grid coordinates
vec2 uv = position.xz * dotScale; // Or use spherical mapping
vec2 grid = fract(uv) - 0.5;

// Distance from grid center = dot
float dot = length(grid);

// Dot size can vary by position (halftone density)
float density = (position.y + 1.0) * 0.5; // 0-1 based on height
float threshold = dotSize * density;

// Create dot
float inDot = 1.0 - smoothstep(threshold - 0.02, threshold, dot);

// Colors
vec3 dotColor = vec3(0.0); // Black dots
vec3 bgColor = vec3(1.0); // White background
color = mix(bgColor, dotColor, inDot);
```

**Variations:**
- **Classic halftone:** Dot size varies with shading
- **Pop art:** Bold uniform dots
- **Noise-modulated:** Random dot sizes
- **Animated dots:** Dots pulse or drift
- **Multi-color:** Different colored dots

**Parameters:**
- `dotScale` - Density of dot grid
- `dotSize` - Base size of dots
- `dotColor` - Dot color
- `densityMode` - Uniform, gradient, noise-based

---

### 29. Advanced Multi-Axis Orbit

**Concept:** Orbital rotation around 1, 2, or 3 axes simultaneously for complex, unpredictable motion.

**Difficulty:** ⭐⭐⭐ Medium - Requires proper rotation composition

**Implementation:**
```java
// Orbit configuration
public class OrbitConfig {
    public float xAxisSpeed = 0.0f;  // Rotation around X (pitch)
    public float yAxisSpeed = 1.0f;  // Rotation around Y (yaw) - most common
    public float zAxisSpeed = 0.0f;  // Rotation around Z (roll)
    
    public float xAxisRadius = 0.0f; // Orbit radius in X
    public float yAxisRadius = 0.0f; // Orbit radius in Y
    public float zAxisRadius = 1.0f; // Orbit radius in Z
    
    public float phaseOffset = 0.0f; // Starting phase
}

// Calculate orbit position
public Vec3d calculateOrbitPosition(float time, Vec3d center, OrbitConfig config) {
    // Calculate angles for each axis
    float angleX = time * config.xAxisSpeed + config.phaseOffset;
    float angleY = time * config.yAxisSpeed + config.phaseOffset;
    float angleZ = time * config.zAxisSpeed + config.phaseOffset;
    
    // Compose rotation matrices
    Matrix3f rotX = new Matrix3f().rotateX(angleX);
    Matrix3f rotY = new Matrix3f().rotateY(angleY);
    Matrix3f rotZ = new Matrix3f().rotateZ(angleZ);
    
    // Combined rotation
    Matrix3f combined = rotZ.mul(rotY).mul(rotX);
    
    // Apply to offset vector
    Vec3d offset = new Vec3d(config.xAxisRadius, config.yAxisRadius, config.zAxisRadius);
    Vec3f rotated = combined.transform(offset.toVector3f());
    
    return center.add(rotated.x, rotated.y, rotated.z);
}
```

**Orbit Types:**

**Single Axis (Simple):**
```java
// Classic horizontal orbit
config.yAxisSpeed = 1.0f;  // Only Y rotation
config.zAxisRadius = 2.0f; // Radius in XZ plane
```

**Dual Axis (Tilted Orbit):**
```java
// Tilted elliptical orbit
config.yAxisSpeed = 1.0f;   // Primary rotation
config.xAxisSpeed = 0.3f;   // Secondary tilt
config.zAxisRadius = 2.0f;
```

**Triple Axis (Chaotic/Precession):**
```java
// Tumbling, unpredictable motion
config.xAxisSpeed = 0.7f;
config.yAxisSpeed = 1.0f;
config.zAxisSpeed = 0.4f;
// Use irrational ratios for non-repeating patterns
```

**Advanced Features:**
- **Eccentricity:** Elliptical vs circular orbits
- **Precession:** Orbit plane slowly rotates
- **Phase sync:** Multiple orbiters in formation
- **Speed variation:** Accelerate/decelerate through orbit

**Preset Orbits:**
- `CIRCULAR` - Simple single-axis
- `ELLIPTICAL` - Dual-axis elongated
- `FIGURE_8` - Lissajous pattern
- `CHAOTIC` - Triple-axis with irrational ratios
- `PRECESSING` - Slowly wobbling orbit plane

---

Another idea would be to inject into the seed generation and pregenerate around the seed center certain structure hidden and stuff, like reminant of past virus of whatnot or added treasures and such. increasing the side of ore vein and such...

## 📋 IDEAS TO EXPLORE

*(Add more here as we find them)*

---

## ❌ REJECTED IDEAS

*(Track what didn't work and why)*

