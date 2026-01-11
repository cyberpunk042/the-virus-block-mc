#!/usr/bin/env python3
"""
HDR Pipeline Generator

Generates HDR post-effect JSON pipelines for different shader versions.
Supports simple (effect only) and full (effect + blur + glow) chains.

Usage:
    python3 generate_hdr_pipeline.py v7 --chain simple     # Effect only
    python3 generate_hdr_pipeline.py v7 --chain full       # Effect + blur + glow
    python3 generate_hdr_pipeline.py v5,v6,v7,v8 --chain full  # Multiple versions
"""

import argparse
import json
import sys
from pathlib import Path

# Base paths
BASE_DIR = Path(__file__).parent.parent
POST_EFFECT_DIR = BASE_DIR / "src/main/resources/assets/the-virus-block/post_effect"
SHADER_DIR = BASE_DIR / "src/main/resources/assets/the-virus-block/shaders/post"


def load_base_json(version: str) -> dict:
    """Load the base LDR JSON for a version."""
    json_path = POST_EFFECT_DIR / f"field_visual_{version}.json"
    if not json_path.exists():
        raise FileNotFoundError(f"Base JSON not found: {json_path}")
    with open(json_path) as f:
        return json.load(f)


def create_simple_chain(version: str, base_json: dict) -> dict:
    """
    Simple chain: Just swap the shader for HDR version.
    Effect → swap → blit → main
    
    HDR benefits:
    - RGBA16F intermediate buffer (via mixin)
    - No internal clamping (via HDR_MODE define)
    """
    hdr_json = base_json.copy()
    hdr_json["_comment"] = f"HDR Simple Chain - {version.upper()}"
    
    # Update first pass to use HDR shader
    for pass_obj in hdr_json["passes"]:
        shader = pass_obj.get("fragment_shader", "")
        if f"field_visual_{version}" in shader:
            pass_obj["fragment_shader"] = f"the-virus-block:post/hdr/field_visual_{version}_hdr"
            break
    
    return hdr_json


def create_full_chain(version: str, base_json: dict) -> dict:
    """
    Full chain: Effect + blur + glow composite.
    
    Pass 1: Effect → swap (HDR, no internal clamping)
    Pass 2: Blit swap → main (show effect)
    Pass 3: Blur H swap → blur_h
    Pass 4: Blur V blur_h → blur_v
    Pass 5: Glow add blur_v → main
    
    HDR benefits:
    - All passes use RGBA16F
    - Blur operates on HDR values (smoother falloff)
    - Glow preserves bright highlights
    """
    # Start with base structure
    hdr_json = {
        "_comment": f"HDR Full Chain - {version.upper()} with blur and glow",
        "targets": {
            "swap": {},
            "blur_h": {},
            "blur_v": {}
        },
        "passes": []
    }
    
    # Copy first pass from base and update shader
    base_pass = base_json["passes"][0].copy()
    base_pass["_comment"] = "Pass 1: HDR Effect → swap"
    base_pass["fragment_shader"] = f"the-virus-block:post/hdr/field_visual_{version}_hdr"
    base_pass["output"] = "swap"  # Output to intermediate buffer, not main
    hdr_json["passes"].append(base_pass)
    
    # Pass 2: Blit swap → main
    blit_pass = {
        "_comment": "Pass 2: Blit effect to main",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "minecraft:post/blit",
        "inputs": [{"sampler_name": "In", "target": "swap"}],
        "output": "minecraft:main",
        "uniforms": {
            "BlitConfig": [
                {"name": "ColorModulate", "type": "vec4", "value": [1.0, 1.0, 1.0, 1.0]}
            ]
        }
    }
    hdr_json["passes"].append(blit_pass)
    
    # Pass 3: Horizontal blur
    blur_h_pass = {
        "_comment": "Pass 3: Horizontal Blur",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
        "inputs": [{"sampler_name": "In", "target": "swap"}],
        "output": "blur_h",
        "uniforms": {
            "BlurParams": [
                {"name": "DirectionX", "type": "float", "value": 1.0},
                {"name": "DirectionY", "type": "float", "value": 0.0},
                {"name": "BlurPad1", "type": "float", "value": 0.0},
                {"name": "BlurPad2", "type": "float", "value": 0.0}
            ],
            "HdrConfig": [
                {"name": "BlurRadius", "type": "float", "value": 1.0},
                {"name": "GlowIntensity", "type": "float", "value": 1.0},
                {"name": "HdrPad1", "type": "float", "value": 0.0},
                {"name": "HdrPad2", "type": "float", "value": 0.0}
            ]
        }
    }
    hdr_json["passes"].append(blur_h_pass)
    
    # Pass 4: Vertical blur
    blur_v_pass = {
        "_comment": "Pass 4: Vertical Blur",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
        "inputs": [{"sampler_name": "In", "target": "blur_h"}],
        "output": "blur_v",
        "uniforms": {
            "BlurParams": [
                {"name": "DirectionX", "type": "float", "value": 0.0},
                {"name": "DirectionY", "type": "float", "value": 1.0},
                {"name": "BlurPad1", "type": "float", "value": 0.0},
                {"name": "BlurPad2", "type": "float", "value": 0.0}
            ],
            "HdrConfig": [
                {"name": "BlurRadius", "type": "float", "value": 1.0},
                {"name": "GlowIntensity", "type": "float", "value": 1.0},
                {"name": "HdrPad1", "type": "float", "value": 0.0},
                {"name": "HdrPad2", "type": "float", "value": 0.0}
            ]
        }
    }
    hdr_json["passes"].append(blur_v_pass)
    
    # Pass 5: Glow composite
    glow_pass = {
        "_comment": "Pass 5: Add glow to scene",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/glow_add",
        "inputs": [
            {"sampler_name": "Scene", "target": "minecraft:main"},
            {"sampler_name": "Glow", "target": "blur_v"}
        ],
        "output": "minecraft:main",
        "uniforms": {
            "FieldVisualConfig": [
                {"name": "CenterX", "type": "float", "value": 0.0},
                {"name": "CenterY", "type": "float", "value": 64.0},
                {"name": "CenterZ", "type": "float", "value": 0.0},
                {"name": "Radius", "type": "float", "value": 3.0},
                {"name": "PrimaryR", "type": "float", "value": 1.0},
                {"name": "PrimaryG", "type": "float", "value": 0.5},
                {"name": "PrimaryB", "type": "float", "value": 0.0},
                {"name": "PrimaryA", "type": "float", "value": 1.0},
                {"name": "SecondaryR", "type": "float", "value": 1.0},
                {"name": "SecondaryG", "type": "float", "value": 0.8},
                {"name": "SecondaryB", "type": "float", "value": 0.0},
                {"name": "SecondaryA", "type": "float", "value": 1.0},
                {"name": "TertiaryR", "type": "float", "value": 1.0},
                {"name": "TertiaryG", "type": "float", "value": 1.0},
                {"name": "TertiaryB", "type": "float", "value": 1.0},
                {"name": "TertiaryA", "type": "float", "value": 0.0},
                {"name": "HighlightR", "type": "float", "value": 1.0},
                {"name": "HighlightG", "type": "float", "value": 1.0},
                {"name": "HighlightB", "type": "float", "value": 1.0},
                {"name": "HighlightA", "type": "float", "value": 0.0},
                {"name": "RayColorR", "type": "float", "value": 1.0},
                {"name": "RayColorG", "type": "float", "value": 0.6},
                {"name": "RayColorB", "type": "float", "value": 0.0},
                {"name": "RayColorA", "type": "float", "value": 1.0},
                {"name": "Phase", "type": "float", "value": 0.0},
                {"name": "AnimSpeed", "type": "float", "value": 1.0},
                {"name": "Intensity", "type": "float", "value": 1.0},
                {"name": "EffectType", "type": "float", "value": 0.0}
            ]
        }
    }
    hdr_json["passes"].append(glow_pass)
    
    return hdr_json


def generate_pipeline(version: str, chain_type: str) -> Path:
    """Generate a pipeline JSON for the given version and chain type."""
    base_json = load_base_json(version)
    
    if chain_type == "simple":
        hdr_json = create_simple_chain(version, base_json)
    elif chain_type == "full":
        hdr_json = create_full_chain(version, base_json)
    else:
        raise ValueError(f"Unknown chain type: {chain_type}")
    
    # Write output
    output_path = POST_EFFECT_DIR / f"field_visual_{version}_hdr.json"
    with open(output_path, 'w') as f:
        json.dump(hdr_json, f, indent=4)
    
    return output_path


def main():
    parser = argparse.ArgumentParser(description="Generate HDR post-effect pipelines")
    parser.add_argument("versions", help="Comma-separated list of versions (e.g., v5,v6,v7,v8)")
    parser.add_argument("--chain", choices=["simple", "full"], default="simple",
                        help="Chain type: simple (effect only) or full (effect + blur + glow)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print what would be generated without writing files")
    
    args = parser.parse_args()
    versions = [v.strip() for v in args.versions.split(",")]
    
    print(f"Generating {args.chain} HDR pipelines for: {', '.join(versions)}")
    
    for version in versions:
        try:
            if args.dry_run:
                print(f"  Would generate: field_visual_{version}_hdr.json ({args.chain} chain)")
            else:
                output_path = generate_pipeline(version, args.chain)
                print(f"  Generated: {output_path.name}")
        except FileNotFoundError as e:
            print(f"  ERROR: {e}", file=sys.stderr)
        except Exception as e:
            print(f"  ERROR generating {version}: {e}", file=sys.stderr)
    
    print("Done!")


if __name__ == "__main__":
    main()
