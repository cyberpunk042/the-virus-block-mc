# 🦠 The Virus Block - AI Instructions

> **Tech Stack**: Minecraft 1.21.6 Fabric mod with Yarn mappings

## Quick Orientation

Read the documentation in order of depth:
1. `docs/README.md` - System overview
2. `docs/ARCHITECTURE.md` - Complete architecture diagram & data flow
3. `docs/{system}.md` - Detailed class diagrams per system (field, gui, visual, rendering, etc.)

<mission>
[The current mission is to analyse why ProfilesPanel is that massive, looking for OOP principle opportunities and patterns for [abstract] classes or design pattern application, like a good engineer. I aim for 400 lines max, and I want a plan to reach it. Let me know if you have questions or if you need help.]
This mod creates customizable **force fields** in Minecraft with advanced visual effects.
The core concept is a **FieldDefinition** that describes layers of geometric primitives
with animations, colors, and triggers - all editable through a sophisticated GUI.
</mission>

## Key Concepts

| Concept | Description |
|---------|-------------|
| **FieldDefinition** | The serializable data model (JSON-compatible) defining a complete field |
| **FieldLayer** | A layer containing primitives with transform, animation, and blend mode |
| **Primitive** | A shape (sphere, cube, etc.) with fill, color, and visibility settings |
| **FieldEditState** | Client-side state management bridging GUI ↔ FieldDefinition |
| **FieldInstance** | A live field attached to an entity in the world |

## FieldDefinition Composition

A `FieldDefinition` is the central data structure. Understanding its components is key:

```
FieldDefinition
├── id: String              # Unique identifier (e.g., "shield_default")
├── type: FieldType         # Category: SHIELD, PERSONAL, AURA, etc.
├── baseRadius: float       # Base scale before modifiers
├── themeId: String?        # Color theme reference (e.g., "energy_blue")
│
├── layers: FieldLayer[]    # Ordered list of visual layers
│   └── FieldLayer
│       ├── id: String
│       ├── primitives: Primitive[]    # Shapes in this layer
│       │   └── Primitive
│       │       ├── shape: Shape       # Geometry (sphere, cube, etc.)
│       │       ├── fill: FillConfig   # Solid, wireframe, gradient
│       │       ├── transform: Transform
│       │       └── visibility: VisibilityMask
│       ├── transform: Transform       # Layer-level offset/scale
│       ├── animation: Animation       # Spin, pulse, wave
│       └── blendMode: BlendMode       # Additive, multiply, etc.
│
├── modifiers: Modifiers?   # Global visual modifiers
│   ├── radiusMultiplier, strengthMultiplier, alphaMultiplier
│   ├── tilt, swirl, spinMultiplier
│   └── visualScale
│
├── follow: FollowConfig?   # How field follows attached entity
│   ├── enabled, responsiveness
│   └── leadOffset, trailOffset
│
├── beam: BeamConfig?       # Central beam effect
│   ├── enabled, innerRadius, outerRadius
│   └── color, pulse
│
├── bindings: Map<String, BindingConfig>  # Reactive property bindings
│   └── e.g., "alpha" → { source: "player.health_percent", range: [0.3, 1.0] }
│
├── triggers: TriggerConfig[]   # Event-triggered effects
│   └── e.g., { event: ON_DAMAGE, effect: PULSE, intensity: 0.5 }
│
└── lifecycle: LifecycleConfig? # Spawn/despawn animations
    └── fadeIn, fadeOut, decay
```

## Architecture at a Glance

```
GUI (FieldCustomizerScreen)
    ↓
FieldEditState (Adapters bridge UI ↔ Data)
    ↓
FieldDefinition (Serializable model)
    ↓
FieldRegistry → FieldManager → FieldInstance
    ↓
FieldRenderer → GPU
```

## Code Organization

```
src/main/java/net/cyberpunk042/     # Server + Common code
├── field/                          # Core field system
├── visual/                         # Shapes, patterns, fills, colors
├── block/                          # Custom blocks
└── infection/                      # Virus spreading logic

src/client/java/net/cyberpunk042/client/   # Client-only code
├── gui/                            # Full GUI system
│   ├── screen/                     # Main screens
│   ├── panel/                      # UI panels
│   ├── state/                      # State management & adapters
│   └── widget/                     # Custom widgets
├── field/render/                   # Field rendering
└── visual/                         # Mesh building, tessellators
```

## Common Tasks

### Adding a new shape property
1. Add field to `Shape.java` or specific shape class
2. Add adapter method in `ShapeAdapter.java`
3. Add UI control in `ShapeSubPanel.java`
4. Handle in tessellator if geometry changes

### Adding a new panel/sub-panel
1. Extend `AbstractPanel` or `AbstractSubPanel`
2. Register in `LayoutManager` category
3. Add state adapter if needed

### Modifying field serialization
1. Update `FieldDefinition` record (or nested records)
2. Update `fromJson`/`toJson` methods
3. Update `FieldEditState` sync logic

## Gotchas

- **Records are immutable** - Use `withX()` builder pattern for modifications
- **Client vs Server** - GUI and rendering are client-only, field logic is common
- **State sync** - `FieldEditState` must sync to `FieldDefinition` before save/network
- **Yarn mappings** - Use Yarn names (e.g., `MinecraftClient`, `MatrixStack`)

---

*For detailed class diagrams and relationships, see `docs/`*
