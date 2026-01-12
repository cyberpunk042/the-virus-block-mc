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


def create_godrays_chain(version: str, base_json: dict) -> dict:
    """
    God Rays chain: HDR Effect + god ray passes.
    
    Pass 1: Effect → swap (HDR, procedural rays skipped when god rays enabled)
    Pass 2: Blit swap → main
    Pass 3: God Rays Mask → god_mask (depth-based occlusion)
    Pass 4: God Rays Accum → god_accum (radial blur toward light)
    Pass 5: God Rays Blur H → god_blur_h
    Pass 6: God Rays Blur V → god_blur_v
    Pass 7: God Rays Composite → main (blend god rays with scene)
    
    NOTE: When GodRayEnabled=0, the god ray passes early-out with passthrough.
    HDR glow is achieved naturally through unclamped values - no separate glow pass needed.
    """
    # Extract FieldVisualConfig uniforms from base pass
    base_pass = base_json["passes"][0].copy()
    field_visual_uniforms = base_pass.get("uniforms", {})
    
    # ═══════════════════════════════════════════════════════════════════════════
    # INJECT GOD RAY UNIFORMS (Slots 50-54)
    # These are dynamically updated by PostEffectPassMixin but must be declared
    # in the JSON for Minecraft to create the uniform block with correct size.
    # ═══════════════════════════════════════════════════════════════════════════
    god_ray_uniforms = [
        # Slot 50: God Ray params
        {"name": "GodRayEnabled", "type": "float", "value": 0.0},
        {"name": "GodRayDecay", "type": "float", "value": 0.97},
        {"name": "GodRayExposure", "type": "float", "value": 0.02},
        {"name": "GodRaySamples", "type": "float", "value": 96.0},
        # Slot 51: God Ray mask params
        {"name": "GodRayThreshold", "type": "float", "value": 0.5},
        {"name": "GodRaySkyEnabled", "type": "float", "value": 0.0},
        {"name": "GodRaySoftness", "type": "float", "value": 0.3},
        {"name": "GodRayMaskReserved", "type": "float", "value": 0.0},
        # Slot 52: God Ray style params
        {"name": "GodRayEnergyMode", "type": "float", "value": 0.0},
        {"name": "GodRayColorMode", "type": "float", "value": 0.0},
        {"name": "GodRayDistributionMode", "type": "float", "value": 0.0},
        {"name": "GodRayArrangementMode", "type": "float", "value": 0.0},
        # Slot 53: God Ray color 2 (for gradient mode)
        {"name": "GodRayColor2R", "type": "float", "value": 1.0},
        {"name": "GodRayColor2G", "type": "float", "value": 0.9},
        {"name": "GodRayColor2B", "type": "float", "value": 0.7},
        {"name": "GodRayGradientPower", "type": "float", "value": 1.0},
        # Slot 54: God Ray noise params
        {"name": "GodRayNoiseScale", "type": "float", "value": 8.0},
        {"name": "GodRayNoiseSpeed", "type": "float", "value": 0.5},
        {"name": "GodRayNoiseIntensity", "type": "float", "value": 0.5},
        {"name": "GodRayAngularBias", "type": "float", "value": 0.0},
        # Slot 55: God Ray curvature (vortex/spiral/pinwheel)
        {"name": "GodRayCurvatureMode", "type": "float", "value": 1.0},  # 1 = vortex
        {"name": "GodRayCurvatureStrength", "type": "float", "value": 0.3},
        {"name": "GodRayCurvatureSpeed", "type": "float", "value": 1.0},
        {"name": "GodRayCurvatureReserved", "type": "float", "value": 0.0},
        # Slot 56: God Ray flicker (animation modes)
        {"name": "GodRayFlickerMode", "type": "float", "value": 0.0},
        {"name": "GodRayFlickerIntensity", "type": "float", "value": 0.3},
        {"name": "GodRayFlickerFrequency", "type": "float", "value": 2.0},
        {"name": "GodRayWaveDistribution", "type": "float", "value": 0.0},
        # Slot 57: God Ray travel (chase/scroll effects)
        {"name": "GodRayTravelMode", "type": "float", "value": 0.0},
        {"name": "GodRayTravelSpeed", "type": "float", "value": 1.0},
        {"name": "GodRayTravelCount", "type": "float", "value": 3.0},
        {"name": "GodRayTravelWidth", "type": "float", "value": 0.2},
    ]
    
    # Append god ray uniforms to FieldVisualConfig if not already present
    if "FieldVisualConfig" in field_visual_uniforms:
        existing_names = {u["name"] for u in field_visual_uniforms["FieldVisualConfig"]}
        for u in god_ray_uniforms:
            if u["name"] not in existing_names:
                field_visual_uniforms["FieldVisualConfig"].append(u)
    
    # Build the pipeline
    hdr_json = {
        "_comment": f"HDR God Rays Chain - {version.upper()} with volumetric light shafts",
        "targets": {
            "swap": {},
            "god_mask": {},
            "god_accum": {},
            "god_blur_h": {},
            "god_blur_v": {}
        },
        "passes": []
    }
    
    # ═══════════════════════════════════════════════════════════════════════════
    # Pass 1: Effect → swap
    # ═══════════════════════════════════════════════════════════════════════════
    base_pass["_comment"] = "Pass 1: HDR Effect → swap (procedural rays skipped when god rays enabled)"
    base_pass["fragment_shader"] = f"the-virus-block:post/hdr/field_visual_{version}_hdr"
    base_pass["output"] = "swap"
    hdr_json["passes"].append(base_pass)
    
    # ═══════════════════════════════════════════════════════════════════════════
    # Pass 2: Blit → main
    # ═══════════════════════════════════════════════════════════════════════════
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
    
    # ═══════════════════════════════════════════════════════════════════════════
    # Pass 3: God Rays Mask (depth → occlusion)
    # ═══════════════════════════════════════════════════════════════════════════
    god_mask_pass = {
        "_comment": "Pass 3: God Rays Mask - brightness + depth to occlusion",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/god_rays_mask",
        "inputs": [
            {"sampler_name": "Scene", "target": "minecraft:main"},
            {
                "sampler_name": "Depth",
                "target": "minecraft:main",
                "use_depth_buffer": True
            }
        ],
        "output": "god_mask",
        "uniforms": field_visual_uniforms  # Needs FieldVisualConfig for threshold + sky toggle
    }
    hdr_json["passes"].append(god_mask_pass)
    
    # ═══════════════════════════════════════════════════════════════════════════
    # Pass 4: God Rays Accumulate (radial blur toward light)
    # ═══════════════════════════════════════════════════════════════════════════
    god_accum_pass = {
        "_comment": "Pass 4: God Rays Accumulate - radial blur toward light source",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/god_rays_accum",
        "inputs": [
            {"sampler_name": "Occlusion", "target": "god_mask"}
        ],
        "output": "god_accum",
        "uniforms": field_visual_uniforms  # Needs FieldVisualConfig for position + god ray params
    }
    hdr_json["passes"].append(god_accum_pass)
    
    # ═══════════════════════════════════════════════════════════════════════════
    # Pass 5-6: God Rays Blur (soften the rays)
    # ═══════════════════════════════════════════════════════════════════════════
    god_blur_h_pass = {
        "_comment": "Pass 5: God Rays Blur H",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
        "inputs": [{"sampler_name": "In", "target": "god_accum"}],
        "output": "god_blur_h",
        "uniforms": {
            "BlurParams": [
                {"name": "DirectionX", "type": "float", "value": 1.0},
                {"name": "DirectionY", "type": "float", "value": 0.0},
                {"name": "BlurPad1", "type": "float", "value": 0.0},
                {"name": "BlurPad2", "type": "float", "value": 0.0}
            ]
        }
    }
    hdr_json["passes"].append(god_blur_h_pass)
    
    god_blur_v_pass = {
        "_comment": "Pass 6: God Rays Blur V",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/gaussian_blur",
        "inputs": [{"sampler_name": "In", "target": "god_blur_h"}],
        "output": "god_blur_v",
        "uniforms": {
            "BlurParams": [
                {"name": "DirectionX", "type": "float", "value": 0.0},
                {"name": "DirectionY", "type": "float", "value": 1.0},
                {"name": "BlurPad1", "type": "float", "value": 0.0},
                {"name": "BlurPad2", "type": "float", "value": 0.0}
            ]
        }
    }
    hdr_json["passes"].append(god_blur_v_pass)
    
    # ═══════════════════════════════════════════════════════════════════════════
    # Pass 7: God Rays Composite (blend with scene)
    # ═══════════════════════════════════════════════════════════════════════════
    god_composite_pass = {
        "_comment": "Pass 7: God Rays Composite - blend god rays with scene",
        "vertex_shader": "minecraft:post/blit",
        "fragment_shader": "the-virus-block:post/hdr/god_rays_composite",
        "inputs": [
            {"sampler_name": "Scene", "target": "minecraft:main"},
            {"sampler_name": "GodRays", "target": "god_blur_v"}
        ],
        "output": "minecraft:main",
        "uniforms": field_visual_uniforms  # Needs FieldVisualConfig for ray color + god ray enabled check
    }
    hdr_json["passes"].append(god_composite_pass)
    
    return hdr_json


def generate_pipeline(version: str, chain_type: str) -> Path:
    """Generate a pipeline JSON for the given version and chain type."""
    base_json = load_base_json(version)
    
    if chain_type == "simple":
        hdr_json = create_simple_chain(version, base_json)
    elif chain_type == "godrays":
        hdr_json = create_godrays_chain(version, base_json)
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
    parser.add_argument("--chain", choices=["simple", "godrays"], default="simple",
                        help="Chain type: simple (HDR effect only), godrays (HDR + volumetric light shafts)")
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
