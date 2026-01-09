# 🎯 Augmented Helmet HUD - Implementation Plan

> **Goal**: Create an immersive augmented reality experience when wearing the Augmented Helmet
> **Status**: Planning Phase
> **Created**: 2026-01-08

---

## 📋 Overview

Transform the Augmented Helmet from a simple "ping in chat" system to a full augmented reality experience with:

1. **HUD Panel** - Sci-fi styled threat scanner in top-right corner
2. **Visual Effects** - Visor overlay, scan lines, subtle AR feel
3. **Threat Highlighting** - See virus blocks through walls (limited range)
4. **Smooth Animations** - Interpolated direction arrow, pulsing effects

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AUGMENTED HELMET SYSTEM                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  SERVER SIDE                              CLIENT SIDE                        │
│  ════════════                             ═══════════                        │
│                                                                              │
│  ┌──────────────────────┐                ┌──────────────────────────────┐   │
│  │ HelmetTelemetryService│──[NETWORK]───▶│ HelmetHudPayload (Receiver)  │   │
│  │ (existing + enhanced) │               └──────────────┬───────────────┘   │
│  │                       │                              │                    │
│  │ • Calculates nearest  │                              ▼                    │
│  │ • Counts threats      │               ┌──────────────────────────────┐   │
│  │ • Computes direction  │               │ HelmetHudState (Singleton)   │   │
│  │ • Sends packets       │               │ • Stores latest data         │   │
│  └──────────────────────┘               │ • Interpolates animations    │   │
│                                          │ • Fade in/out logic          │   │
│                                          └──────────────┬───────────────┘   │
│                                                         │                    │
│                                                         ▼                    │
│                                          ┌──────────────────────────────┐   │
│                                          │ AugmentedHelmetOverlay       │   │
│                                          │ (HudRenderCallback)          │   │
│                                          │                              │   │
│                                          │ • Dark sci-fi panel          │   │
│                                          │ • Direction arrow            │   │
│                                          │ • Distance + signal bar      │   │
│                                          │ • Threat count               │   │
│                                          └──────────────┬───────────────┘   │
│                                                         │                    │
│                                                         ▼                    │
│                                          ┌──────────────────────────────┐   │
│                                          │ HelmetVisorEffect            │   │
│                                          │ (Optional Post-Processing)   │   │
│                                          │                              │   │
│                                          │ • Visor vignette             │   │
│                                          │ • Scan lines                 │   │
│                                          │ • Threat highlighting        │   │
│                                          └──────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Phase 1: Core HUD Panel

**Goal**: Working HUD panel that shows when helmet is equipped

### Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `HelmetHudState.java` | `client/helmet/` | Client-side state singleton |
| `AugmentedHelmetOverlay.java` | `client/helmet/` | HUD renderer |
| `HelmetHudPayload.java` | `network/` | Network packet (server→client) |

### Tasks

- [ ] **1.1** Create `HelmetHudState` class
  - Singleton pattern
  - Fields: `yawToTarget`, `distance`, `signalStrength`, `sourceCount`, `hasTarget`
  - Interpolation helpers for smooth animations
  - Timestamp for fade logic

- [ ] **1.2** Create `HelmetHudPayload` record
  - Implement `CustomPayload` interface
  - Fields: direction angle, distance, strength, count, hasTarget
  - Register in `ModPayloads` / `FieldNetworking`

- [ ] **1.3** Create `AugmentedHelmetOverlay` renderer
  - Register with `HudRenderCallback`
  - Top-right positioning with GUI scale adaptation
  - Check if player wearing helmet (client-side)
  - Dark semi-transparent panel
  - Direction arrow (Unicode or custom drawn)
  - Distance text
  - Signal strength bar
  - Threat count

- [ ] **1.4** Modify `HelmetTelemetryService`
  - Calculate direction angle to target
  - Calculate signal strength (inverse of distance, clamped)
  - Send `HelmetHudPayload` alongside existing ping

- [ ] **1.5** Register client receiver for payload
  - Update `HelmetHudState` on receive

### Visual Design (Phase 1)

```
╔══════════════════════════════════════╗
║ ◈ THREAT SCANNER            [ACTIVE] ║ ← Cyan header
╠══════════════════════════════════════╣
║                                      ║
║              ▲                       ║ ← Direction arrow
║             /|\                      ║   (rotates to point at threat)
║              │                       ║
║                                      ║
║  NEAREST:  47m  NORTH                ║ ← Distance + compass
║  SIGNAL:   ████████░░░░  STRONG      ║ ← Strength bar
║  THREATS:  2 sources detected        ║ ← Count
║                                      ║
╚══════════════════════════════════════╝
```

### Technical Notes

- **GUI Scale Adaptation**: Use `client.getWindow().getScaleFactor()` and apply slight offset multiplier
- **Top-Right Position**: `x = screenWidth - panelWidth - margin`
- **Client Helmet Check**: Create `ClientVirusEquipmentHelper` that checks local player inventory

---

## 📦 Phase 2: Smooth Animations & Polish

**Goal**: Fluid, responsive feel with subtle sci-fi touches

### Tasks

- [ ] **2.1** Direction Arrow Animation
  - Lerp current angle toward target angle
  - Smooth rotation (not instant snap)
  - Consider using delta time for frame-independent animation

- [ ] **2.2** Signal Bar Animation
  - Smooth bar width transitions
  - Pulsing glow effect on bar

- [ ] **2.3** Panel Effects
  - Subtle border pulse (every few seconds)
  - Optional: occasional "glitch" frame (1-2 frame distortion)
  - Fade in when helmet equipped, fade out when removed

- [ ] **2.4** Text Styling
  - Custom colors per element
  - "CRITICAL" red text when threat < 10m
  - Blinking effect for close threats

- [ ] **2.5** Sound Cues (Optional)
  - Subtle beep when new threat detected
  - Pitch change based on proximity

---

## 📦 Phase 3: Visor Overlay Effect

**Goal**: Make the screen feel like you're looking through an AR visor

### Approach Options

| Option | Pros | Cons |
|--------|------|------|
| **Texture Overlay** | Simple, no shader needed | Limited effects |
| **Post-Process Shader** | Full control, can do scan lines + vignette | More complex |
| **DrawContext Only** | No new assets needed | Limited to rectangles/gradients |

### Tasks

- [ ] **3.1** Create Visor Vignette
  - Subtle darkening at screen edges
  - Slightly rounded corners feel
  - Semi-transparent overlay

- [ ] **3.2** Scan Lines Effect (Optional)
  - Very faint horizontal lines
  - Subtle enough to not be annoying
  - Toggle option in config?

- [ ] **3.3** HUD Frame Elements
  - Corner brackets/decorations
  - Thin lines at screen edges
  - Small status indicators (helmet icon, battery?)

### Visual Concept

```
┌────────────────────────────────────────────────────────────────┐
│ ╔══╗                                                     ╔══╗  │
│ ║  ║                    GAME VIEW                        ║  ║  │
│ ╚══╝                                                     ╚══╝  │
│                                                                │
│   (vignette darkening around edges)                            │
│                                                                │
│                                                                │
│ ╔══╗                                                     ╔══╗  │
│ ║  ║                                          [HUD PANEL]║  ║  │
│ ╚══╝                                                     ╚══╝  │
└────────────────────────────────────────────────────────────────┘
        └── Corner brackets for "visor frame" feel ──┘
```

---

## 📦 Phase 4: Threat Highlighting (Advanced AR)

**Goal**: See virus blocks through walls with a glowing outline

### Approach

This is the most ambitious feature. We can leverage the existing depth buffer work:

1. **Know virus block positions** (from server packet)
2. **Project to screen space** (using camera matrices)
3. **Render highlight** at those screen positions

### Options

| Approach | Description | Difficulty |
|----------|-------------|------------|
| **Screen-Space Marker** | Draw 2D icon/arrow at projected position | Easy |
| **World-Space Glow** | Render glowing sphere at virus position | Medium |
| **Depth-Aware Outline** | Use depth buffer to draw "through walls" | Hard |

### Tasks

- [ ] **4.1** Send Virus Positions in Payload
  - Add `List<BlockPos>` or `Vec3d[]` to payload (limited to nearby, max 5?)

- [ ] **4.2** Project World to Screen
  - Use camera matrices to convert world pos → screen pos
  - Handle off-screen (show arrow at edge pointing to it?)

- [ ] **4.3** Render Screen-Space Markers
  - Small diamond/triangle icon at projected position
  - Color based on distance (red = close)
  - Pulse animation

- [ ] **4.4** (Advanced) Depth-Aware Highlighting
  - If virus is behind geometry, show X-ray style outline
  - Use existing DirectDepthRenderer techniques
  - Limited range (maybe 30-50 blocks max)

### Visual Concept

```
                    Regular game view
                          
    ┌────────────────────────────────────────┐
    │                                        │
    │         █████                          │
    │         █   █    ◆ ← Threat marker     │
    │         █████   (virus block behind    │
    │                   this wall)           │
    │    ◇                                   │
    │    ↑                                   │
    │  (another threat, further away)        │
    │                                        │
    └────────────────────────────────────────┘
```

---

## 📦 Phase 5: Configuration & Polish

### Tasks

- [ ] **5.1** Config Options
  - Enable/disable HUD
  - Enable/disable visor effects
  - Enable/disable scan lines
  - Opacity settings

- [ ] **5.2** Key Binding (Optional)
  - Toggle helmet HUD visibility
  - Or always show when equipped

- [ ] **5.3** Accessibility
  - High contrast mode option
  - Larger text mode

---

## 🔧 Technical Considerations

### GUI Scale Adaptation

```java
// Get base scale factor
double guiScale = client.getWindow().getScaleFactor();

// Apply slight adaptation (not 1:1, just a bit)
float adaptFactor = 1.0f + (float)(guiScale - 2.0) * 0.1f;

// Clamp to reasonable range
adaptFactor = MathHelper.clamp(adaptFactor, 0.85f, 1.15f);

// Apply to panel dimensions
int panelWidth = (int)(BASE_PANEL_WIDTH * adaptFactor);
```

### Client-Side Helmet Detection

```java
public static boolean hasAugmentedHelmetClient(PlayerEntity player) {
    ItemStack headSlot = player.getEquippedStack(EquipmentSlot.HEAD);
    return !headSlot.isEmpty() && headSlot.isOf(ModItems.AUGMENTED_HELMET);
}
```

Note: `ModItems` access requires the item to be registered on both sides, which it is.

### Network Payload Design

```java
public record HelmetHudPayload(
    float yawToTarget,      // Angle in degrees (0-360)
    float distance,         // Distance in blocks
    float signalStrength,   // 0.0 - 1.0
    int sourceCount,        // Number of threats in range
    boolean hasTarget       // Whether any target exists
) implements CustomPayload {
    
    public static final Id<HelmetHudPayload> ID = 
        new Id<>(Identifier.of("the-virus-block", "helmet_hud"));
    
    // ... codec, etc
}
```

---

## 📊 Dependency Graph

```
Phase 1 ─────┬────▶ Phase 2 (animations)
             │
             └────▶ Phase 3 (visor effects)
                          │
                          ▼
                    Phase 4 (highlighting)
                          │
                          ▼
                    Phase 5 (config/polish)
```

Phases 2, 3, 4 can be worked on somewhat in parallel after Phase 1 is complete.

---

## ✅ Success Criteria

- [ ] HUD panel visible when helmet equipped
- [ ] Direction arrow points toward nearest threat
- [ ] Distance and signal strength displayed
- [ ] Smooth animations (no jittering)
- [ ] Works with different GUI scales
- [ ] Subtle AR visor effect (not distracting)
- [ ] Performance: No noticeable FPS impact

---

## 🎨 Color Palette (Dark Sci-Fi Theme)

| Element | Color | Hex |
|---------|-------|-----|
| Panel Background | Near-black, semi-transparent | `#000000C0` (with alpha) |
| Header Bar | Dark cyan | `#004444` |
| Accent/Header Text | Bright cyan | `#00FFFF` |
| Primary Text | White | `#FFFFFF` |
| Secondary Text | Light gray | `#AAAAAA` |
| Warning | Orange | `#FFAA00` |
| Critical/Danger | Red | `#FF4444` |
| Signal Bar (Strong) | Cyan→Green gradient | `#00FFFF` → `#00FF00` |
| Signal Bar (Weak) | Yellow→Red gradient | `#FFFF00` → `#FF0000` |

---

## ✅ Design Decisions (Finalized 2026-01-08)

| Decision | Choice | Notes |
|----------|--------|-------|
| **Packet Frequency** | 10/sec (every 2 ticks) | With auto-drop on latency to avoid making lag worse |
| **Chat Messages** | **REMOVE** | HUD replaces the deprecated chat approach entirely |
| **Threat Highlighting** | 2D markers (Easy) | Screen-projected position markers for nearby threats |
| **Visor Intensity** | Strong | But configurable via CommandKnob for user preference |
| **File Organization** | `client/helmet/` package | New dedicated package for helmet HUD system |

### Latency Auto-Drop Logic

```java
// Pseudo-code for adaptive packet sending
if (serverTickTime > LATENCY_THRESHOLD_MS) {
    // Skip this packet to reduce network pressure
    skipCount++;
    if (skipCount >= MAX_SKIP) {
        // Force send at least every MAX_SKIP cycles
        sendPacket();
        skipCount = 0;
    }
} else {
    sendPacket();
    skipCount = 0;
}
```

### Client-Side Configuration (HelmetHudConfig)

Since the helmet HUD is **client-side**, we need a client config system (not server CommandKnob).

**Pattern**: Similar to `GuiConfigPersistence` but with client commands for runtime adjustment.

**File**: `client/helmet/HelmetHudConfig.java`

```java
public final class HelmetHudConfig {
    private static final HelmetHudConfig INSTANCE = new HelmetHudConfig();
    
    // Visor effects
    private boolean visorEnabled = true;
    private float visorIntensity = 1.0f;  // 0.0 - 1.0
    private boolean scanlinesEnabled = true;
    private float scanlinesOpacity = 0.15f;  // 0.0 - 0.5
    private float vignetteStrength = 0.4f;  // 0.0 - 1.0
    
    // HUD panel
    private boolean hudEnabled = true;
    private float hudScale = 1.0f;  // 0.5 - 2.0
    private float hudOpacity = 0.85f;  // 0.3 - 1.0
    
    public static HelmetHudConfig get() { return INSTANCE; }
    
    // Getters/setters with validation...
}
```

**Client Command Registration** (in `TheVirusBlockClient`):

```java
// /helmethud visor <enable|disable>
// /helmethud intensity <0.0-1.0>
// /helmethud scanlines <enable|disable>
// /helmethud vignette <0.0-1.0>
// /helmethud scale <0.5-2.0>
```

| Command | Effect |
|---------|--------|
| `/helmethud visor enable` | Enable visor overlay |
| `/helmethud visor disable` | Disable visor overlay |
| `/helmethud intensity 0.5` | Set visor intensity to 50% |
| `/helmethud scanlines enable` | Enable scan line effect |
| `/helmethud vignette 0.3` | Set vignette strength |
| `/helmethud scale 1.2` | Scale HUD panel 120% |
| `/helmethud opacity 0.7` | Set HUD panel opacity |
| `/helmethud reset` | Reset all to defaults |

---

## 📝 Finalized Answers

| Question | Answer | Config |
|----------|--------|--------|
| **Max Threat Range** | Unlimited (but configurable) | `helmet.detection.range` |
| **Marker Count** | Show all (but configurable max) | `helmet.markers.maxCount` |
| **Marker Render Range** | Limited range for markers | `helmet.markers.renderRange` |

### Configurable Limits (for performance tuning)

```java
// In HelmetHudConfig - easy to adjust
private int maxMarkerCount = 10;        // Max markers to render (0 = unlimited)
private float markerRenderRange = 100f; // Only show markers within this range
private float detectionRange = 0f;      // 0 = unlimited detection
```

This way if 100+ sources are in view, we can:
1. Cap markers shown to nearest 10
2. Only render markers within 100 blocks
3. Still report total count in HUD text

---

## 🚀 Next Steps

1. Review and refine this plan together
2. Begin Phase 1 implementation
3. Test with actual gameplay
4. Iterate on visual design based on feedback

---

## 📁 Complete File List

### New Files to Create

| # | File | Location | Purpose |
|---|------|----------|---------|
| 1 | `HelmetHudConfig.java` | `client/helmet/` | Client-side config singleton + persistence |
| 2 | `HelmetHudState.java` | `client/helmet/` | Runtime state with interpolation |
| 3 | `AugmentedHelmetOverlay.java` | `client/helmet/` | HUD panel renderer |
| 4 | `HelmetVisorEffect.java` | `client/helmet/` | Visor overlay (vignette, scanlines) |
| 5 | `HelmetHudPayload.java` | `network/` | Server→Client HUD data packet |
| 6 | `HelmetHudCommands.java` | `client/command/` | Client commands for `/helmethud` |

### Files to Modify

| File | Changes |
|------|---------|
| `HelmetTelemetryService.java` | Add packet sending, remove chat messages |
| `FieldNetworking.java` | Register `HelmetHudPayload` |
| `ClientFieldNodes.java` | Register HUD render callback + receivers |
| `TheVirusBlockClient.java` | Register `/helmethud` client commands |

### Implementation Order

```
1. HelmetHudPayload.java        ─┐
2. HelmetHudConfig.java          │
3. HelmetHudState.java           ├─► Phase 1 (Core)
4. AugmentedHelmetOverlay.java   │
5. Modify HelmetTelemetryService ┘

6. Smooth animations             ─► Phase 2 (Polish)
7. HelmetVisorEffect.java        ─► Phase 3 (AR Visor)
8. 2D threat markers             ─► Phase 4 (Highlighting)
9. HelmetHudCommands.java        ─► Phase 5 (Config)
```

---

## 📋 Checklist for Start

Before starting implementation:

- [x] Plan reviewed and approved
- [x] Design decisions finalized
- [x] File structure decided
- [ ] Phase 1 ready to begin

**Ready to implement when you give the green light! 🚀**
