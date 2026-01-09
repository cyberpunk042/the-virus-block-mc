# Ownership Checklist - Parameter Addition Guardrail

> **Purpose:** Answer these questions before adding ANY new parameter.
> **Rule:** If you can't answer all questions, stop and think harder.

---

## The Checklist

### 1. Domain Ownership

**Question:** Which domain owns this parameter?

| Domain | Owns Parameters That... |
|--------|------------------------|
| Frame | Are global, per-frame, engine-level (time, frameIndex, globalDebugFlags) |
| Camera | Define the view (position, vectors, matrices, fov, aspect, near/far) |
| Object | Are per-instance identity (future: objectId, instance transform) |
| Material | Are per-material constants (future: surface properties) |
| Light | Define scene lighting (future: light positions, colors, intensities) |
| EffectConfig | Are preset/style knobs (colors, noise, shape, thresholds) |
| EffectRuntime | Are per-frame instance state (center if moving, fade progress, phase) |
| Debug | Are development-only diagnostics |

**Write your answer:** _______________

---

### 2. Update Frequency

**Question:** How often does this parameter change?

| Frequency | When to Use | Examples |
|-----------|-------------|----------|
| per-frame | Every single frame | time, camera position, animation phase |
| on-change | When user/preset changes | colors, noise settings, intensity |
| rare | Almost never | layout version, effect type |
| never | Reserved slots | padding, future expansion |

**Write your answer:** _______________

---

### 3. Scope

**Question:** Is this parameter...

- [ ] **Global** (same for all effects)? → Frame or Camera
- [ ] **Per-effect-type** (same for all instances of Energy Orb)? → EffectConfig
- [ ] **Per-instance** (different for each field on screen)? → EffectRuntime or Object

**Write your answer:** _______________

---

### 4. Derivability

**Question:** Can this be derived from existing values?

- Can it be computed from `time` + other params? → Don't add it
- Is it a scaled version of something else? → Consider a multiplier instead
- Is it redundant with another param? → Merge them

**If derivable, stop here. Don't add the parameter.**

---

### 5. Naming

**Question:** Does a parameter with similar meaning already exist?

Check the [Parameter Dictionary](field_visual_param_dictionary.md) first.

- If yes → Reuse existing or clarify naming
- If no → Add to dictionary before implementing

**Write new name:** _______________

---

### 6. Reserved Lanes

**Question:** Is there a reserved slot available in the target section?

- [ ] Yes → Use reserved slot (no layout change needed)
- [ ] No → Add reserved lanes first, then use one

**Slot to use:** _______________

---

### 7. Version Bump

**Question:** Does this require a version bump?

| Situation | Version Bump? |
|-----------|---------------|
| Filling a reserved slot | No |
| Reordering existing params | Yes |
| Removing a param | Yes |
| Changing param semantics | Yes (or new version/flag) |

**Write your answer:** _______________

---

### 8. Shader Impact

**Question:** Which shaders need updating?

- [ ] FrameUBO users (all shaders)
- [ ] CameraUBO users (all effect shaders)
- [ ] FieldVisualConfig users (field_visual_*.fsh)
- [ ] VirusBlockConfig users (virus_block.fsh)
- [ ] Other: _______________

---

### 9. Documentation

**Question:** Have you updated...

- [ ] Parameter Dictionary
- [ ] UBO Registry (if new slot/binding)
- [ ] GLSL block declaration
- [ ] Java record
- [ ] This checklist (if new domain/pattern)

---

## Final Approval

Before implementing:

- [ ] All questions answered
- [ ] Domain is clear
- [ ] No duplication
- [ ] Reserved lane available (or planned)
- [ ] Documentation updated

**Reviewer sign-off (if applicable):** _______________

---

> **Anti-pattern detected?** If you're adding camera data to EffectConfig, STOP. That's the old mega-UBO mistake.
