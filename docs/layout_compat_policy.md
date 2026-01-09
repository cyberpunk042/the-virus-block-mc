# Layout Compatibility Policy

> **Purpose:** Define rules for maintaining backward compatibility when UBO layouts change.
> **Rule:** Follow this policy for every layout modification.

---

## Core Principle

**Prefer adding to reserved lanes over reordering.**

Reserved lanes exist specifically so you can add parameters without breaking existing shaders.

---

## Compatibility Levels

### Level 1: No-Break Change (Preferred)

**What:** Fill a reserved slot with a new parameter.

**Requirements:**
- Use existing reserved vec4/float slot
- Don't move any other parameters
- New parameter gets a default value that preserves old behavior

**Version bump:** Not required

**Example:**
```java
// Before
float Reserved1;  // Slot 23.z

// After
float NewFeatureFlag;  // Slot 23.z (was Reserved1)
```

---

### Level 2: Additive Change

**What:** Add new slots at the end of a section, within reserved space.

**Requirements:**
- Add only at section end
- Update section reserved count
- New parameter defaults preserve old behavior

**Version bump:** Recommended (for debugging), not required if reserved space existed

**Example:**
```java
// Before (section ends at slot 48)
vec4 Slot48;
vec4 Reserved[2];  // Slots 49-50

// After (used one reserved)
vec4 Slot48;
vec4 NewSlot49;
vec4 Reserved[1];  // Slot 50
```

---

### Level 3: Reorder/Remove Change (Breaking)

**What:** Move, rename, or remove existing parameters.

**Requirements:**
- Bump layout version
- Update ALL shaders that use this UBO
- Update Java record
- Update GLSL block
- Run all golden tests

**Version bump:** Required

**Migration options:**
1. **Big bang:** Update all shaders at once
2. **Dual layout:** Support both layouts temporarily via version check

**Example:**
```glsl
// In shader
if (LayoutVersion > 1.5) {
    // New layout
    float newParam = Slot5.x;
} else {
    // Old layout (deprecated, remove after migration)
    float newParam = 1.0;  // Default
}
```

---

## Reserved Lane Guidelines

### How Many to Reserve

| Section Type | Min Reserved | Reason |
|--------------|--------------|--------|
| Header | 2 vec4 | Version, flags, future metadata |
| Colors | 1 vec4 | Future palette expansion |
| Animation | 2 vec4 | New animation modes |
| Shape/Core | 2 vec4 | New shape parameters |
| Noise | 2 vec4 | New noise algorithms |
| Corona/Rays | 2 vec4 | Visual expansion |
| Version-specific (V2, V8) | 4 vec4 | Version evolution |

### Naming Convention

```java
float Reserved1;  // Clear but generic
float _pad0;      // Short but unclear
float futureUse1; // Descriptive
```

Use `ReservedN` for consistency.

---

## Version Number Scheme

### LayoutVersion (in Frame or Config header)

| Version | Meaning |
|---------|---------|
| 1.0 | Initial layout |
| 1.1 | Reserved slots filled |
| 2.0 | Breaking layout change |
| 2.1 | Reserved slots filled after 2.0 |

### EffectVersion (in Effect Config)

| Version | Meaning |
|---------|---------|
| 1.0 | V1 Energy Orb |
| 2.0 | V2 Detail parameters |
| 5.0 | V5 Flames |
| 8.0 | V8 Electric Aura |

---

## Migration Checklist

For any layout change:

### No-Break (Level 1)
- [ ] Update Java record
- [ ] Update GLSL block
- [ ] Set meaningful default
- [ ] Update parameter dictionary

### Additive (Level 2)
- [ ] All of Level 1
- [ ] Update reserved count
- [ ] Consider version bump

### Breaking (Level 3)
- [ ] All of Level 2
- [ ] Bump layout version
- [ ] Update ALL affected shaders
- [ ] Run golden tests
- [ ] Update documentation
- [ ] Commit message mentions breaking change

---

## Deprecation Process

If removing a parameter:

1. Mark as deprecated (via comment or log)
2. Keep for at least one version
3. Replace with reserved slot
4. Remove references in next breaking change

---

## Compatibility Matrix

Track which shader versions work with which layout versions:

| Shader | Layout 1.0 | Layout 1.1 | Layout 2.0 |
|--------|------------|------------|------------|
| V1 | ✅ | ✅ | ❌ |
| V2 | ✅ | ✅ | ❌ |
| V8 | ✅ | ✅ | ✅ |
| Geodesic | ✅ | ✅ | ✅ |

Update this matrix when layouts change.

---

> **Golden Rule:** If you're unsure whether a change is breaking, assume it is and follow Level 3 process.
