# Field Presets (Multi-Scope)

Presets are **partial field configurations** that can modify multiple aspects at once.
Unlike Fragments (single-scope), Presets can:

- Add/modify layers
- Add/modify primitives
- Set properties across multiple categories (shape, fill, animation, etc.)
- Create cohesive "looks" or "effects" with one selection

## Hierarchy

```
Profile    = Complete field definition (replaces everything)
Preset     = Partial merge (can add layers, set multiple properties)
Fragment   = Single-scope $ref target (shape OR fill OR animation)
```

## JSON Structure

```json
{
  "name": "Display Name",
  "description": "What this preset does",
  "merge": {
    "layers": [...],           // Optional: add/modify layers
    "appearance": {...},       // Optional: appearance settings
    "animation": {...},        // Optional: animation settings
    "fill": {...},             // Optional: fill settings
    "visibility": {...},       // Optional: visibility mask
    "arrangement": {...},      // Optional: arrangement patterns
    "beam": {...},             // Optional: debug beam settings
    "follow": {...}            // Optional: follow mode settings
  }
}
```

## Merge Behavior

- Properties in the preset **override** current values
- Layers with matching names are **merged**, new layers are **appended**
- Primitives follow the same merge logic
- Unspecified properties are **left unchanged**

## Creating Custom Presets

1. Create a `.json` file in this folder
2. Define the `name` and `description`
3. Add a `merge` object with the properties you want to set
4. The preset will appear in the GUI dropdown automatically

