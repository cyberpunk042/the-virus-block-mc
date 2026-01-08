# Field Primitives

Reusable primitive templates for use in layer primitives arrays.

## Usage

Reference in JSON using `$primitives/` syntax within a layer's primitives array:

```json
{
  "layers": [
    {
      "id": "main",
      "primitives": [
        "$primitives/glowing_sphere",
        "$primitives/wireframe_ring"
      ]
    }
  ]
}
```

This will load `config/the-virus-block/field_primitives/glowing_sphere.json` and `wireframe_ring.json`.

## JSON Format

A primitive template contains all primitive fields:

```json
{
  "id": "glowing_sphere",
  "type": "sphere",
  "shape": {
    "radius": 1.0
  },
  "appearance": {
    "color": "@primary",
    "glow": 0.8
  },
  "animation": {
    "spin": {
      "axis": "Y",
      "speed": 0.02
    }
  }
}
```

## Example Files

- `glowing_sphere.json` - Glowing sphere with spin
- `wireframe_ring.json` - Wireframe ring
- `pulsing_disc.json` - Pulsing disc

See `Primitive` interface for all available fields.


