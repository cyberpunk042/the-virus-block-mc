# ForceConfigModal Implementation Plan - DETAILED

## Overview
Rewrite `ForceConfigModal.java` to use `CompactSelector` for zone/phase navigation.

---

## VISUAL DESIGN (LOCKED - NO CHANGES)

```
┌─────────────────────────────────────────────────────────────────┐
│  ⚡ Force Field Configuration                              [×]  │
├────────────────────────────────┬────────────────────────────────┤
│  [+] ◀│ Zone 1 │▶ [+]         │  [+] ◀│ Phase 1 │▶ [+]         │
├────────────────────────────────┼────────────────────────────────┤
│  Radius: ═══════════⬤══ 10.0  │  Start %: ═══════⬤════ 0%      │
│  Strength: ══⬤═════════ 0.15  │  End %: ══════════════⬤ 100%   │
│  Falloff: [Linear        ▼]   │  [●PULL] [○PUSH] [○HOLD]       │
│           [🗑 Delete Zone]     │  Multiplier: ════⬤═══ 1.0x     │
│                                │           [🗑 Delete Phase]    │
├────────────────────────────────┴────────────────────────────────┤
│  CONSTRAINTS                                                     │
│  Max Velocity: ═══⬤═══  Vert Boost: ═══⬤═══  Damping: ═⬤═══   │
├─────────────────────────────────────────────────────────────────┤
│                                          [Save]    [Cancel]      │
└─────────────────────────────────────────────────────────────────┘
```

---

## ARCHITECTURE (following LayerManager pattern)

```
ForceConfigModal
├── Inner class: ForceConfigState (manages zones/phases + selection)
│   ├── List<ZoneState> zones
│   ├── List<PhaseState> phases  
│   ├── int selectedZoneIndex
│   ├── int selectedPhaseIndex
│   ├── getSelectedZone() / getSelectedPhase()
│   ├── setSelectedZoneIndex(int) → clamp + notify
│   ├── setSelectedPhaseIndex(int) → clamp + notify
│   ├── addZone() / removeZone(int)
│   ├── addPhase() / removePhase(int)
│   └── Runnable onSelectionChange (triggers dialog.rebuild())
│
├── Inner class: ZoneState (mutable) - ALREADY EXISTS
│   ├── float radius, strength
│   └── String falloff
│
├── Inner class: PhaseState (mutable) - ALREADY EXISTS
│   ├── float startPercent, endPercent, multiplier
│   └── ForcePolarity polarity
│
├── Fields
│   ├── ModalDialog dialog
│   ├── ForceConfigState state (NEW - replaces direct lists)
│   ├── float maxVelocity, verticalBoost, damping (constraints)
│   └── Consumer<ForceFieldConfig> onSave
│
└── Methods
    ├── constructor: loads initial config → state
    ├── buildContent(Bounds, TextRenderer): builds UI from state
    │   ├── Left col: CompactSelector + zone controls
    │   ├── Right col: CompactSelector + phase controls
    │   └── Bottom: constraint sliders
    ├── save(): builds ForceFieldConfig from state
    └── getDialog(): returns dialog
```

---

## BINDING LOGIC - HOW IT WORKS

### The Key Insight (from LayerManager)
When selection changes:
1. Update selectedZoneIndex/selectedPhaseIndex
2. Clamp to valid range
3. Call onSelectionChange callback
4. Callback triggers dialog.rebuild()
5. buildContent() runs fresh, reads getSelectedZone()/getSelectedPhase()
6. Creates sliders with .initial(zone.radius) from current selection
7. Slider onChange captures zone object by reference

### Flow: User selects different zone
1. User clicks ▶ in zone CompactSelector
2. CompactSelector.onSelect callback fires
3. Callback does: state.setSelectedZoneIndex(newIdx)
4. setSelectedZoneIndex clamps + calls onSelectionChange
5. onSelectionChange calls dialog.rebuild()
6. buildContent() reads state.getSelectedZone() to get NEW zone
7. Creates sliders with .initial(newZone.radius) - shows new values

### Flow: User drags slider
1. User drags Radius slider
2. LabeledSlider.onChange fires with new value
3. Captured lambda `v -> zone.radius = v` runs
4. Updates the specific ZoneState object in the list
5. NO rebuild needed - value stored immediately

---

## IMPLEMENTATION STEPS

### Step 1: Add ForceConfigState inner class (NEW)

```java
/**
 * Manages zones, phases, and selection state.
 * Follows LayerManager pattern for selection management.
 */
private class ForceConfigState {
    private final List<ZoneState> zones = new ArrayList<>();
    private final List<PhaseState> phases = new ArrayList<>();
    private int selectedZoneIndex = 0;
    private int selectedPhaseIndex = 0;
    private Runnable onSelectionChange;
    
    void setOnSelectionChange(Runnable callback) {
        this.onSelectionChange = callback;
    }
    
    // Zone selection
    ZoneState getSelectedZone() {
        if (zones.isEmpty()) return null;
        return zones.get(Math.min(selectedZoneIndex, zones.size() - 1));
    }
    
    int getSelectedZoneIndex() { return selectedZoneIndex; }
    
    void setSelectedZoneIndex(int index) {
        selectedZoneIndex = Math.max(0, Math.min(index, zones.size() - 1));
        if (onSelectionChange != null) onSelectionChange.run();
    }
    
    // Phase selection
    PhaseState getSelectedPhase() {
        if (phases.isEmpty()) return null;
        return phases.get(Math.min(selectedPhaseIndex, phases.size() - 1));
    }
    
    int getSelectedPhaseIndex() { return selectedPhaseIndex; }
    
    void setSelectedPhaseIndex(int index) {
        selectedPhaseIndex = Math.max(0, Math.min(index, phases.size() - 1));
        if (onSelectionChange != null) onSelectionChange.run();
    }
    
    // Zone CRUD
    List<ZoneState> getZones() { return zones; }
    
    void addZone() {
        float minRadius = zones.stream().map(z -> z.radius).min(Float::compare).orElse(10f);
        zones.add(new ZoneState(Math.max(1, minRadius * 0.5f), 0.2f, "linear"));
        selectedZoneIndex = zones.size() - 1;
        if (onSelectionChange != null) onSelectionChange.run();
    }
    
    void removeZone(int index) {
        if (zones.size() > 1 && index >= 0 && index < zones.size()) {
            zones.remove(index);
            selectedZoneIndex = Math.min(selectedZoneIndex, zones.size() - 1);
            if (onSelectionChange != null) onSelectionChange.run();
        }
    }
    
    // Phase CRUD
    List<PhaseState> getPhases() { return phases; }
    
    void addPhase() {
        float lastEnd = phases.isEmpty() ? 0 : phases.get(phases.size() - 1).endPercent;
        phases.add(new PhaseState(lastEnd, 100, ForcePolarity.PUSH, 1.0f));
        selectedPhaseIndex = phases.size() - 1;
        if (onSelectionChange != null) onSelectionChange.run();
    }
    
    void removePhase(int index) {
        if (phases.size() > 1 && index >= 0 && index < phases.size()) {
            phases.remove(index);
            selectedPhaseIndex = Math.min(selectedPhaseIndex, phases.size() - 1);
            if (onSelectionChange != null) onSelectionChange.run();
        }
    }
    
    // Load from config
    void loadFrom(ForceFieldConfig config) {
        zones.clear();
        phases.clear();
        
        for (ForceZone zone : config.zones()) {
            zones.add(new ZoneState(zone.radius(), zone.strength(), zone.falloffName()));
        }
        if (zones.isEmpty()) zones.add(new ZoneState(10f, 0.1f, "linear"));
        
        for (ForcePhase phase : config.phases()) {
            phases.add(new PhaseState(phase.startPercent(), phase.endPercent(), 
                                      phase.polarity(), phase.strengthMultiplier()));
        }
        if (phases.isEmpty()) phases.add(new PhaseState(0, 100, ForcePolarity.PULL, 1.0f));
        
        selectedZoneIndex = 0;
        selectedPhaseIndex = 0;
    }
}
```

### Step 2: Update constructor to use ForceConfigState

Replace:
```java
// Old
private List<ZoneState> zones = new ArrayList<>();
private List<PhaseState> phases = new ArrayList<>();
```

With:
```java
// New
private final ForceConfigState state = new ForceConfigState();
```

And in constructor:
```java
state.loadFrom(initialConfig);
state.setOnSelectionChange(() -> dialog.rebuild());
```

### Step 3: Update buildContent() to use state + CompactSelector

See full implementation below.

### Step 4: Update save() to use state

Replace references to `zones` and `phases` with `state.getZones()` and `state.getPhases()`.

---

## buildContent() - COMPLETE NEW IMPLEMENTATION

```java
private List<ClickableWidget> buildContent(Bounds bounds, TextRenderer tr) {
    List<ClickableWidget> widgets = new ArrayList<>();
    
    int padding = GuiConstants.PADDING;
    int rowH = GuiConstants.WIDGET_HEIGHT;
    
    // Calculate two-column layout
    int contentW = bounds.width() - padding * 2;
    int colW = (contentW - padding) / 2;
    
    int leftX = bounds.x() + padding;
    int rightX = leftX + colW + padding;
    int topY = bounds.y() + padding;
    
    // ═══════════════════════════════════════════════════════════════════
    // LEFT COLUMN: ZONES
    // ═══════════════════════════════════════════════════════════════════
    int leftY = topY;
    
    // Zone selector (CompactSelector)
    List<String> zoneNames = new ArrayList<>();
    for (int i = 0; i < state.getZones().size(); i++) {
        zoneNames.add("Zone " + (i + 1));
    }
    if (zoneNames.isEmpty()) zoneNames.add("(none)");
    
    CompactSelector<String> zoneSelector = new CompactSelector<>("", tr, zoneNames, s -> s)
        .onSelect(name -> {
            int idx = zoneNames.indexOf(name);
            if (idx >= 0 && idx < state.getZones().size()) {
                state.setSelectedZoneIndex(idx);
            }
        })
        .onAdd(state::addZone)
        .selectIndex(state.getSelectedZoneIndex());
    zoneSelector.setBounds(new Bounds(leftX, leftY, colW, 20));
    widgets.addAll(zoneSelector.getWidgets());
    leftY += 24;
    
    // Zone controls (only if zone exists)
    ZoneState zone = state.getSelectedZone();
    if (zone != null) {
        // Radius
        widgets.add(LabeledSlider.builder("Radius")
            .position(leftX, leftY).width(colW)
            .range(0.5f, 50f).initial(zone.radius).format("%.1f")
            .onChange(v -> zone.radius = v)
            .build());
        leftY += rowH + padding;
        
        // Strength
        widgets.add(LabeledSlider.builder("Strength")
            .position(leftX, leftY).width(colW)
            .range(0.01f, 1.0f).initial(zone.strength).format("%.3f")
            .onChange(v -> zone.strength = v)
            .build());
        leftY += rowH + padding;
        
        // Falloff dropdown
        widgets.add(CyclingButtonWidget.<String>builder(s -> Text.literal("Falloff: " + s))
            .values(FALLOFF_OPTIONS)
            .initially(capitalize(zone.falloff))
            .build(leftX, leftY, colW, rowH, Text.literal("Falloff"),
                   (btn, val) -> zone.falloff = val.toLowerCase()));
        leftY += rowH + padding;
        
        // Delete button (only if >1 zone)
        if (state.getZones().size() > 1) {
            widgets.add(GuiWidgets.button(leftX, leftY, colW, "🗑 Delete Zone", 
                "Remove this zone", () -> state.removeZone(state.getSelectedZoneIndex())));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // RIGHT COLUMN: PHASES
    // ═══════════════════════════════════════════════════════════════════
    int rightY = topY;
    
    // Phase selector (CompactSelector)
    List<String> phaseNames = new ArrayList<>();
    for (int i = 0; i < state.getPhases().size(); i++) {
        phaseNames.add("Phase " + (i + 1));
    }
    if (phaseNames.isEmpty()) phaseNames.add("(none)");
    
    CompactSelector<String> phaseSelector = new CompactSelector<>("", tr, phaseNames, s -> s)
        .onSelect(name -> {
            int idx = phaseNames.indexOf(name);
            if (idx >= 0 && idx < state.getPhases().size()) {
                state.setSelectedPhaseIndex(idx);
            }
        })
        .onAdd(state::addPhase)
        .selectIndex(state.getSelectedPhaseIndex());
    phaseSelector.setBounds(new Bounds(rightX, rightY, colW, 20));
    widgets.addAll(phaseSelector.getWidgets());
    rightY += 24;
    
    // Phase controls (only if phase exists)
    PhaseState phase = state.getSelectedPhase();
    if (phase != null) {
        // Start %
        widgets.add(LabeledSlider.builder("Start %")
            .position(rightX, rightY).width(colW)
            .range(0, 100).initial(phase.startPercent).format("%.0f%%").step(5)
            .onChange(v -> phase.startPercent = v)
            .build());
        rightY += rowH + padding;
        
        // End %
        widgets.add(LabeledSlider.builder("End %")
            .position(rightX, rightY).width(colW)
            .range(0, 100).initial(phase.endPercent).format("%.0f%%").step(5)
            .onChange(v -> phase.endPercent = v)
            .build());
        rightY += rowH + padding;
        
        // Polarity buttons (3 side by side)
        int polarityBtnW = (colW - padding * 2) / 3;
        for (int i = 0; i < ForcePolarity.values().length; i++) {
            ForcePolarity pol = ForcePolarity.values()[i];
            int btnX = rightX + i * (polarityBtnW + padding);
            boolean selected = phase.polarity == pol;
            String label = (selected ? "● " : "○ ") + pol.name();
            
            final ForcePolarity finalPol = pol;
            final PhaseState finalPhase = phase;
            widgets.add(ButtonWidget.builder(Text.literal(label), btn -> {
                    finalPhase.polarity = finalPol;
                    dialog.rebuild();
                })
                .dimensions(btnX, rightY, polarityBtnW, rowH)
                .build());
        }
        rightY += rowH + padding;
        
        // Multiplier
        widgets.add(LabeledSlider.builder("Multiplier")
            .position(rightX, rightY).width(colW)
            .range(0.1f, 5.0f).initial(phase.multiplier).format("%.1fx")
            .onChange(v -> phase.multiplier = v)
            .build());
        rightY += rowH + padding;
        
        // Delete button (only if >1 phase)
        if (state.getPhases().size() > 1) {
            widgets.add(GuiWidgets.button(rightX, rightY, colW, "🗑 Delete Phase",
                "Remove this phase", () -> state.removePhase(state.getSelectedPhaseIndex())));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BOTTOM: CONSTRAINTS (full width)
    // ═══════════════════════════════════════════════════════════════════
    int bottomY = bounds.y() + bounds.height() - 60;
    int thirdW = (contentW - padding * 2) / 3;
    
    // Max Velocity
    widgets.add(LabeledSlider.builder("Max Vel")
        .position(leftX, bottomY).width(thirdW)
        .range(0.1f, 3.0f).initial(maxVelocity).format("%.2f")
        .onChange(v -> maxVelocity = v)
        .build());
    
    // Vertical Boost
    widgets.add(LabeledSlider.builder("V.Boost")
        .position(leftX + thirdW + padding, bottomY).width(thirdW)
        .range(0f, 1.0f).initial(verticalBoost).format("%.2f")
        .onChange(v -> verticalBoost = v)
        .build());
    
    // Damping
    widgets.add(LabeledSlider.builder("Damping")
        .position(leftX + thirdW * 2 + padding * 2, bottomY).width(thirdW)
        .range(0f, 0.5f).initial(damping).format("%.3f")
        .onChange(v -> damping = v)
        .build());
    
    return widgets;
}
```

---

## TESTING CHECKLIST

### Zone Navigation
- [ ] Zone selector shows "Zone 1" initially
- [ ] Clicking ▶ navigates to Zone 2 (if exists)
- [ ] Clicking ◀ navigates back to Zone 1
- [ ] When navigating, sliders update to show new zone's values

### Zone Add/Delete
- [ ] Clicking [+] adds new zone and selects it
- [ ] Delete button only shows if >1 zone
- [ ] Delete removes zone and adjusts selection

### Zone Editing
- [ ] Radius slider updates zone.radius
- [ ] Strength slider updates zone.strength
- [ ] Falloff dropdown updates zone.falloff

### Phase (same tests as zones)

### Constraints
- [ ] Max Velocity, Vertical Boost, Damping sliders work

### Save/Cancel
- [ ] Save builds config from state
- [ ] Cancel closes without saving
