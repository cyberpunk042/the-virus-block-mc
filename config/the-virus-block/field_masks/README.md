# Field Masks

Reusable visibility mask configurations for primitives.

## Usage

Reference in JSON using `$masks/` syntax:

```json
{
  "visibility": "$masks/horizontal_bands"
}
```

This will load `config/the-virus-block/field_masks/horizontal_bands.json`

## Example Files

- `horizontal_bands.json` - Horizontal band pattern
- `vertical_stripes.json` - Vertical stripe pattern
- `checker.json` - Checkerboard pattern

## JSON Format

```json
{
  "mask": "BANDS",
  "count": 4,
  "thickness": 0.5
}
```

See `VisibilityMask` for all available fields.


