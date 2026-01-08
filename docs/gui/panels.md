# Panels

> Packages: client.gui.panel, client.gui.panel.sub

**26 classes**

## Class Diagram

```mermaid
classDiagram
    class AbstractPanel {
        <<abstract>>
        +isFeatureSupported() boolean
        +getDisabledTooltip() String
        +setBounds(...) void
        +setBoundsQuiet(...) void
        +getBounds() Bounds
    }
    class ActionPanel {
        +init(...) void
        +tick() void
        +render(...) void
        +getWidgets() List
        +getHeight() int
    }
    class AdvancedPanel {
        +init(...) void
        +tick() void
        +render(...) void
        +mouseScrolled(...) boolean
        +onShapeTypeChanged() void
    }
    class DebugPanel {
        +init(...) void
        +tick() void
        +render(...) void
        +mouseScrolled(...) boolean
        +getTestFieldButton() ButtonWidget
    }
    class LayerPanel {
        +init(...) void
        +setOnLayerChanged(...) void
        +tick() void
        +render(...) void
        +getWidgets() List
    }
    class PrimitivePanel {
        +setOnPrimitiveChanged(...) void
        +init(...) void
        +refreshForLayerChange() void
        +tick() void
        +render(...) void
    }
    class ProfilesPanel {
        +setDualBounds(...) void
        +isDualMode() boolean
        +init(...) void
        +tick() void
        +render(...) void
    }
    class QuickPanel {
        +init(...) void
        +tick() void
        +render(...) void
        +getWidgets() List
    }
    class AppearanceSubPanel {
        +tick() void
        +render(...) void
        +getHeight() int
    }
    class ArrangeSubPanel {
        +getHeight() int
        +init(...) void
        +tick() void
        +render(...) void
        +getWidgets() List
    }
    class BeamSubPanel {
        +onStateChanged(...) void
        +render(...) void
        +tick() void
        +getHeight() int
    }
    class BindingsSubPanel {
        +init(...) void
        +getWidgets() List
        +render(...) void
        +mouseClicked(...) boolean
        +getHeight() int
    }
    class FieldVisualSubPanel {
        +tick() void
        +render(...) void
        +dispose() void
    }
    class FillSubPanel {
        +tick() void
        +render(...) void
        +onShapeChanged() void
        +getHeight() int
        +getWidgets() List
    }
    class ForceSubPanel {
        +setOnConfigureRequest(...) void
        +init(...) void
        +getSelectedConfig() ForceFieldConfig
        +setCustomConfig(...) void
        +tick() void
    }
    class LifecycleSubPanel {
        +tick() void
        +render(...) void
        +getHeight() int
    }
    class LinkingSubPanel {
        +tick() void
        +render(...) void
        +getHeight() int
        +getWidgets() List
    }
    class MagicCircleSubPanel {
        +tick() void
        +render(...) void
        +getHeight() int
    }
    class ModifiersSubPanel {
        +setWarningCallback(...) void
        +tick() void
        +render(...) void
        +onShapeChanged() void
        +getContentHeight() int
    }
    class PredictionSubPanel {
        +render(...) void
        +tick() void
        +getHeight() int
    }
    class ShapeSubPanel {
        +setWarningCallback(...) void
        +setShapeChangedCallback(...) void
        +init(...) void
        +rebuildForCurrentShape() void
        +tick() void
    }
    class ShockwaveSubPanel {
        +tick() void
        +render(...) void
        +getHeight() int
    }
    class TraceSubPanel {
        +init(...) void
        +render(...) void
        +tick() void
        +getHeight() int
    }
    class TransformSubPanel {
        +tick() void
        +render(...) void
        +getHeight() int
        +getWidgets() List
    }
    class TriggerSubPanel {
        +init(...) void
        +tick() void
        +render(...) void
        +getHeight() int
        +getWidgets() List
    }
    class VisibilitySubPanel {
        +tick() void
        +render(...) void
        +getHeight() int
    }
    class BoundPanel
    class Screen
    class Bounds
    class Presetpreset
    AbstractPanel --> Bounds : bounds
    AbstractPanel --> ExpandableSection : sections
    AbstractPanel --> FieldEditState : state
    AbstractPanel --> Screen : parent
    AbstractPanel <|-- ActionPanel
    AbstractPanel <|-- AdvancedPanel
    AbstractPanel <|-- ArrangeSubPanel
    AbstractPanel <|-- BindingsSubPanel
    AbstractPanel <|-- DebugPanel
    AbstractPanel <|-- ForceSubPanel
    AbstractPanel <|-- LayerPanel
    AbstractPanel <|-- PrimitivePanel
    AbstractPanel <|-- ProfilesPanel
    AbstractPanel <|-- QuickPanel
    AbstractPanel <|-- ShapeSubPanel
    AbstractPanel <|-- TraceSubPanel
    AbstractPanel <|-- TriggerSubPanel
    ActionPanel --> CyclingButtonWidget : autoSaveToggle
    ActionPanel --> CyclingButtonWidget : livePreviewToggle
    ActionPanel --> DrawContextcontext : uses
    AdvancedPanel --> AppearanceSubPanel : appearanceSubPanel
    AdvancedPanel --> ModifiersSubPanel : modifiersSubPanel
    AdvancedPanel --> ShapeSubPanel : shapeSubPanel
    AdvancedPanel --> TransformSubPanel : transformSubPanel
    AppearanceSubPanel --> ColorButton : primaryColorBtn
    AppearanceSubPanel --> ColorButton : secondaryColorBtn
    AppearanceSubPanel --> DrawContextcontext : uses
    ArrangeSubPanel --> CellType : currentCellType
    ArrangeSubPanel --> Tab : currentTab
    ArrangeSubPanel --> Tabtab : uses
    ArrangeSubPanel --> TextRenderer : textRenderer
    BeamSubPanel --> CyclingButtonWidget : enabledToggle
    BeamSubPanel --> CyclingButtonWidget : pulseToggle
    BeamSubPanel --> CyclingButtonWidget : variantDropdown
    BeamSubPanel --> CyclingButtonWidget : waveformDropdown
    BindingsSubPanel --> BindableProperty : selectedProperty
    BindingsSubPanel --> CyclingButtonWidget : propertyButton
    BindingsSubPanel --> GuiLayout : layout
    BindingsSubPanel --> InterpolationCurve : selectedCurve
    BoundPanel <|-- AppearanceSubPanel
    BoundPanel <|-- BeamSubPanel
    BoundPanel <|-- FieldVisualSubPanel
    BoundPanel <|-- FillSubPanel
    BoundPanel <|-- LifecycleSubPanel
    BoundPanel <|-- LinkingSubPanel
    BoundPanel <|-- MagicCircleSubPanel
    BoundPanel <|-- ModifiersSubPanel
    BoundPanel <|-- PredictionSubPanel
    BoundPanel <|-- ShockwaveSubPanel
    BoundPanel <|-- TransformSubPanel
    BoundPanel <|-- VisibilitySubPanel
    DebugPanel --> BeamSubPanel : beamPanel
    DebugPanel --> BindingsSubPanel : bindingsPanel
    DebugPanel --> LifecycleSubPanel : lifecyclePanel
    DebugPanel --> TriggerSubPanel : triggerPanel
    FieldVisualSubPanel --> ContentBuildercontent : uses
    FieldVisualSubPanel --> DrawContextcontext : uses
    FieldVisualSubPanel --> ParameterSpec : uses
    FieldVisualSubPanel --> ParameterSpecparam : uses
    FillSubPanel --> CageOptionsAdapter : cageAdapter
    FillSubPanel --> ContentBuildercontent : uses
    FillSubPanel --> CyclingButtonWidget : fragmentDropdown
    FillSubPanel --> DrawContextcontext : uses
    ForceSubPanel --> CyclingButtonWidget : locationDropdown
    ForceSubPanel --> CyclingButtonWidget : presetDropdown
    ForceSubPanel --> ForceFieldConfig : customConfig
    ForceSubPanel --> SpawnLocation : spawnLocation
    LayerPanel --> CyclingButtonWidget : blendModeDropdown
    LayerPanel --> DrawContextcontext : uses
    LayerPanel --> RunnableonLayerChanged : uses
    LifecycleSubPanel --> DrawContextcontext : uses
    LifecycleSubPanel --> LifecycleConfigpreset : uses
    LinkingSubPanel --> CyclingButtonWidget : targetDropdown
    LinkingSubPanel --> DrawContextcontext : uses
    MagicCircleSubPanel --> DrawContextcontext : uses
    ModifiersSubPanel --> BiConsumer : uses
    ModifiersSubPanel --> BiConsumer : warningCallback
    ModifiersSubPanel --> ContentBuilderc : uses
    ModifiersSubPanel --> ContentBuildercontent : uses
    PredictionSubPanel --> CyclingButtonWidget : enabledButton
    PredictionSubPanel --> CyclingButtonWidget : presetButton
    PredictionSubPanel --> DrawContextcontext : uses
    PredictionSubPanel --> Presetpreset : uses
    PrimitivePanel --> DrawContextcontext : uses
    PrimitivePanel --> Runnablecallback : uses
    ProfilesPanel --> ProfileActionService : actionService
    ProfilesPanel --> ProfileEntry : allProfiles
    ProfilesPanel --> ProfileEntry : filteredProfiles
    ProfilesPanel --> ProfilesPanelLayout : layout
    QuickPanel --> ColorButton : colorButton
    QuickPanel --> CyclingButtonWidget : shapeDropdown
    QuickPanel --> LayerPanel : layerPanel
    QuickPanel --> PrimitivePanel : primitivePanel
    ShapeSubPanel --> BiConsumer : warningCallback
    ShapeSubPanel --> CyclingButtonWidget : fragmentDropdown
    ShapeSubPanel --> CyclingButtonWidget : patternFaces
    ShapeSubPanel --> CyclingButtonWidget : shapeTypeDropdown
    ShockwaveSubPanel --> DrawContextcontext : uses
    TraceSubPanel --> DrawContextcontext : uses
    TransformSubPanel --> AxisMotionConfigconfig : uses
    TransformSubPanel --> ContentBuildercontent : uses
    TransformSubPanel --> DrawContextcontext : uses
    TransformSubPanel --> MotionModecurrentMode : uses
    TriggerSubPanel --> CyclingButtonWidget : triggerEffect
    TriggerSubPanel --> CyclingButtonWidget : triggerType
    TriggerSubPanel --> DrawContextcontext : uses
    VisibilitySubPanel --> CyclingButtonWidget : variantDropdown
    VisibilitySubPanel --> DrawContextcontext : uses
```

---
[Back to GUI System](../gui.md)
