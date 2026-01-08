#!/usr/bin/env python3
"""
Sphere Algorithm Prototyping & Verification

This script prototypes and verifies the 5 sphere tessellation algorithms:
1. LAT_LON - Latitude/longitude grid (current implementation)
2. UV_SPHERE - Standard UV-mapped sphere
3. ICO_SPHERE - Icosahedron subdivision
4. TYPE_A - Overlapping axis-aligned cubes (from mc_sphere_model_generator)
5. TYPE_E - Rotated rectangles forming hexadecagon (from mc_sphere_model_generator)

Usage:
    python sphere_algorithms.py --algorithm lat_lon --visualize
    python sphere_algorithms.py --algorithm all --export-java
    python sphere_algorithms.py --compare
"""

import math
import argparse
from dataclasses import dataclass, field
from typing import List, Tuple, Optional
import json

# ═══════════════════════════════════════════════════════════════════════════════
# DATA STRUCTURES
# ═══════════════════════════════════════════════════════════════════════════════

@dataclass
class Vertex:
    """3D vertex with position and normal."""
    x: float
    y: float
    z: float
    nx: float = 0.0
    ny: float = 0.0
    nz: float = 0.0
    
    def normalize(self) -> 'Vertex':
        """Normalize position to unit sphere and set normal = position."""
        length = math.sqrt(self.x**2 + self.y**2 + self.z**2)
        if length > 0:
            self.x /= length
            self.y /= length
            self.z /= length
            self.nx, self.ny, self.nz = self.x, self.y, self.z
        return self
    
    def scale(self, radius: float) -> 'Vertex':
        """Scale vertex position by radius."""
        return Vertex(
            self.x * radius, self.y * radius, self.z * radius,
            self.nx, self.ny, self.nz
        )
    
    def __repr__(self):
        return f"V({self.x:.3f}, {self.y:.3f}, {self.z:.3f})"


@dataclass
class Triangle:
    """Triangle defined by 3 vertex indices."""
    v0: int
    v1: int
    v2: int


@dataclass
class Quad:
    """Quad defined by 4 vertex indices (for TYPE_A/TYPE_E cubes)."""
    v0: int
    v1: int
    v2: int
    v3: int


@dataclass
class Mesh:
    """Simple mesh with vertices and triangles/quads."""
    vertices: List[Vertex] = field(default_factory=list)
    triangles: List[Triangle] = field(default_factory=list)
    quads: List[Quad] = field(default_factory=list)
    
    def stats(self) -> dict:
        return {
            "vertices": len(self.vertices),
            "triangles": len(self.triangles),
            "quads": len(self.quads),
            "total_faces": len(self.triangles) + len(self.quads)
        }


@dataclass
class CubeElement:
    """A cube element for TYPE_A/TYPE_E (not a mesh, but a rendering primitive)."""
    x1: float
    y1: float
    z1: float
    x2: float
    y2: float
    z2: float
    rotation_y: float = 0.0  # Y-axis rotation in degrees


# ═══════════════════════════════════════════════════════════════════════════════
# SHAPE PROFILES - Generalized radius functions
# ═══════════════════════════════════════════════════════════════════════════════

class ShapeProfile:
    """Defines how radius varies with height for generalized TYPE_A/TYPE_E."""
    
    @staticmethod
    def sphere(y_normalized: float, radius: float) -> float:
        """Sphere: r = R * cos(θ) where θ = arcsin(y/R)"""
        if abs(y_normalized) >= 1.0:
            return 0.0
        return radius * math.sqrt(1 - y_normalized * y_normalized)
    
    @staticmethod
    def ellipsoid(y_normalized: float, radius: float, 
                  scale_x: float = 1.0, scale_z: float = 1.0) -> tuple:
        """Ellipsoid: different radii for x and z axes"""
        if abs(y_normalized) >= 1.0:
            return (0.0, 0.0)
        base = math.sqrt(1 - y_normalized * y_normalized)
        return (radius * scale_x * base, radius * scale_z * base)
    
    @staticmethod
    def cylinder(y_normalized: float, radius: float) -> float:
        """Cylinder: constant radius"""
        return radius
    
    @staticmethod
    def cone(y_normalized: float, radius: float, tip_at_top: bool = True) -> float:
        """Cone: linear falloff"""
        t = (y_normalized + 1) / 2  # 0 at bottom, 1 at top
        if tip_at_top:
            return radius * (1 - t)
        else:
            return radius * t
    
    @staticmethod
    def paraboloid(y_normalized: float, radius: float) -> float:
        """Paraboloid: parabolic falloff (bowl shape)"""
        t = (y_normalized + 1) / 2  # 0 at bottom, 1 at top
        return radius * math.sqrt(max(0, 1 - t))
    
    @staticmethod
    def hourglass(y_normalized: float, radius: float, waist: float = 0.3) -> float:
        """Hourglass: pinched in middle"""
        # Distance from center (y=0)
        d = abs(y_normalized)
        # Blend between waist radius and full radius
        return radius * (waist + (1 - waist) * d)
    
    @staticmethod
    def torus_cross_section(angle: float, major_radius: float, minor_radius: float) -> tuple:
        """Torus: returns (center_offset, local_radius) for a cross-section"""
        # For a torus, the center of the tube traces a circle
        center_x = major_radius * math.cos(angle)
        center_z = major_radius * math.sin(angle)
        return ((center_x, center_z), minor_radius)


# ═══════════════════════════════════════════════════════════════════════════════
# ALGORITHM 1: LAT_LON (Current Implementation)
# ═══════════════════════════════════════════════════════════════════════════════

def tessellate_lat_lon(radius: float = 1.0, lat_steps: int = 16, lon_steps: int = 32,
                       lat_start: float = 0.0, lat_end: float = 1.0,
                       lon_start: float = 0.0, lon_end: float = 1.0) -> Mesh:
    """
    Latitude/Longitude tessellation.
    
    Creates a grid where:
    - Latitude (theta): 0 = north pole, PI = south pole
    - Longitude (phi): 0 to 2*PI around Y axis
    
    Pros: Easy to understand, supports partial spheres, good for patterns
    Cons: Pole singularities (vertices cluster at poles)
    """
    mesh = Mesh()
    
    lat_range = lat_end - lat_start
    lon_range = lon_end - lon_start
    
    # Generate vertices
    vertex_grid = []
    for lat in range(lat_steps + 1):
        lat_norm = lat_start + (lat / lat_steps) * lat_range
        theta = lat_norm * math.pi  # 0 to PI
        
        row = []
        for lon in range(lon_steps + 1):
            lon_norm = lon_start + (lon / lon_steps) * lon_range
            phi = lon_norm * 2 * math.pi  # 0 to 2*PI
            
            # Spherical to Cartesian
            x = math.sin(theta) * math.cos(phi) * radius
            y = math.cos(theta) * radius
            z = math.sin(theta) * math.sin(phi) * radius
            
            # Normal = normalized position for sphere
            v = Vertex(x, y, z, x/radius, y/radius, z/radius)
            row.append(len(mesh.vertices))
            mesh.vertices.append(v)
        vertex_grid.append(row)
    
    # Generate triangles from quads
    for lat in range(lat_steps):
        for lon in range(lon_steps):
            tl = vertex_grid[lat][lon]
            tr = vertex_grid[lat][lon + 1]
            bl = vertex_grid[lat + 1][lon]
            br = vertex_grid[lat + 1][lon + 1]
            
            # Two triangles per quad
            mesh.triangles.append(Triangle(tl, tr, br))
            mesh.triangles.append(Triangle(tl, br, bl))
    
    return mesh


# ═══════════════════════════════════════════════════════════════════════════════
# ALGORITHM 2: UV_SPHERE (Variant of LAT_LON with different parameterization)
# ═══════════════════════════════════════════════════════════════════════════════

def tessellate_uv_sphere(radius: float = 1.0, segments: int = 32, rings: int = 16) -> Mesh:
    """
    UV Sphere tessellation.
    
    Similar to LAT_LON but uses standard UV mapping conventions:
    - Rings = horizontal divisions (like latitude)
    - Segments = vertical divisions (like longitude)
    
    The main difference is the vertex sharing at poles.
    """
    mesh = Mesh()
    
    # Top pole vertex
    mesh.vertices.append(Vertex(0, radius, 0, 0, 1, 0))
    top_pole = 0
    
    # Generate ring vertices (excluding poles)
    ring_start_indices = []
    for ring in range(1, rings):
        theta = math.pi * ring / rings
        y = math.cos(theta) * radius
        ring_radius = math.sin(theta) * radius
        
        ring_start_indices.append(len(mesh.vertices))
        for seg in range(segments):
            phi = 2 * math.pi * seg / segments
            x = ring_radius * math.cos(phi)
            z = ring_radius * math.sin(phi)
            
            # Normal
            nx = math.sin(theta) * math.cos(phi)
            ny = math.cos(theta)
            nz = math.sin(theta) * math.sin(phi)
            
            mesh.vertices.append(Vertex(x, y, z, nx, ny, nz))
    
    # Bottom pole vertex
    bottom_pole = len(mesh.vertices)
    mesh.vertices.append(Vertex(0, -radius, 0, 0, -1, 0))
    
    # Top cap triangles (pole to first ring)
    first_ring = ring_start_indices[0]
    for seg in range(segments):
        next_seg = (seg + 1) % segments
        mesh.triangles.append(Triangle(
            top_pole,
            first_ring + next_seg,
            first_ring + seg
        ))
    
    # Middle quads (between rings)
    for ring_idx in range(len(ring_start_indices) - 1):
        ring_a = ring_start_indices[ring_idx]
        ring_b = ring_start_indices[ring_idx + 1]
        
        for seg in range(segments):
            next_seg = (seg + 1) % segments
            
            # Quad as two triangles
            mesh.triangles.append(Triangle(
                ring_a + seg,
                ring_a + next_seg,
                ring_b + next_seg
            ))
            mesh.triangles.append(Triangle(
                ring_a + seg,
                ring_b + next_seg,
                ring_b + seg
            ))
    
    # Bottom cap triangles (last ring to pole)
    last_ring = ring_start_indices[-1]
    for seg in range(segments):
        next_seg = (seg + 1) % segments
        mesh.triangles.append(Triangle(
            last_ring + seg,
            last_ring + next_seg,
            bottom_pole
        ))
    
    return mesh


# ═══════════════════════════════════════════════════════════════════════════════
# ALGORITHM 3: ICO_SPHERE (Icosahedron Subdivision)
# ═══════════════════════════════════════════════════════════════════════════════

def tessellate_ico_sphere(radius: float = 1.0, subdivisions: int = 2) -> Mesh:
    """
    Icosphere tessellation via icosahedron subdivision.
    
    Starts with a 20-face icosahedron and subdivides each triangle.
    
    Pros: Uniform vertex distribution, no pole artifacts
    Cons: Harder to UV map, doesn't support partial spheres easily
    
    Subdivision levels:
    - 0: 12 vertices, 20 triangles (base icosahedron)
    - 1: 42 vertices, 80 triangles
    - 2: 162 vertices, 320 triangles
    - 3: 642 vertices, 1280 triangles
    - n: 10 * 4^n + 2 vertices, 20 * 4^n triangles
    """
    mesh = Mesh()
    
    # Golden ratio
    phi = (1 + math.sqrt(5)) / 2
    
    # Icosahedron vertices (normalized)
    ico_verts = [
        (-1,  phi, 0), ( 1,  phi, 0), (-1, -phi, 0), ( 1, -phi, 0),
        ( 0, -1,  phi), ( 0,  1,  phi), ( 0, -1, -phi), ( 0,  1, -phi),
        ( phi, 0, -1), ( phi, 0,  1), (-phi, 0, -1), (-phi, 0,  1),
    ]
    
    # Add normalized vertices
    for x, y, z in ico_verts:
        v = Vertex(x, y, z).normalize().scale(radius)
        mesh.vertices.append(v)
    
    # Icosahedron faces (20 triangles)
    ico_faces = [
        (0, 11, 5), (0, 5, 1), (0, 1, 7), (0, 7, 10), (0, 10, 11),
        (1, 5, 9), (5, 11, 4), (11, 10, 2), (10, 7, 6), (7, 1, 8),
        (3, 9, 4), (3, 4, 2), (3, 2, 6), (3, 6, 8), (3, 8, 9),
        (4, 9, 5), (2, 4, 11), (6, 2, 10), (8, 6, 7), (9, 8, 1),
    ]
    
    # Initial triangles
    triangles = [(v0, v1, v2) for v0, v1, v2 in ico_faces]
    
    # Subdivision cache: (v0, v1) -> midpoint index
    midpoint_cache = {}
    
    def get_midpoint(v0_idx: int, v1_idx: int) -> int:
        """Get or create midpoint vertex between two vertices."""
        key = tuple(sorted((v0_idx, v1_idx)))
        if key in midpoint_cache:
            return midpoint_cache[key]
        
        v0 = mesh.vertices[v0_idx]
        v1 = mesh.vertices[v1_idx]
        
        # Midpoint, then project to sphere surface
        mid = Vertex(
            (v0.x + v1.x) / 2,
            (v0.y + v1.y) / 2,
            (v0.z + v1.z) / 2
        ).normalize().scale(radius)
        
        idx = len(mesh.vertices)
        mesh.vertices.append(mid)
        midpoint_cache[key] = idx
        return idx
    
    # Subdivide
    for _ in range(subdivisions):
        new_triangles = []
        midpoint_cache.clear()
        
        for v0, v1, v2 in triangles:
            # Get midpoints
            a = get_midpoint(v0, v1)
            b = get_midpoint(v1, v2)
            c = get_midpoint(v2, v0)
            
            # Create 4 new triangles
            new_triangles.extend([
                (v0, a, c),
                (v1, b, a),
                (v2, c, b),
                (a, b, c),
            ])
        
        triangles = new_triangles
    
    # Convert to Triangle objects
    mesh.triangles = [Triangle(v0, v1, v2) for v0, v1, v2 in triangles]
    
    return mesh


# ═══════════════════════════════════════════════════════════════════════════════
# ALGORITHM 4: TYPE_A (Overlapping Cubes - Accuracy)
# ═══════════════════════════════════════════════════════════════════════════════

def generate_type_a(radius: float = 1.0, vertical_layers: int = 8, 
                    horizontal_detail: int = 6, optimize: bool = True) -> List[CubeElement]:
    """
    Type A sphere approximation using overlapping axis-aligned cubes.
    
    From the original Python sphere generator:
    - Creates nested concentric cubes at each vertical layer
    - Each layer subdivides the quadrant into multiple overlapping cubes
    - Optimization: Reduces subdivisions at poles where geometry clusters
    
    The key insight: cubes are SYMMETRIC around origin, spanning from
    (-cx, -y, -cz) to (cx, y, cz), creating the full sphere in one go.
    
    This is NOT a mesh - it's a list of cube elements to render directly.
    
    Pros: Accurate sphere silhouette, good texture mapping
    Cons: Many overlapping faces, higher vertex count
    """
    cubes = []
    
    # More layers = smoother sphere
    vert_step = 90.0 / (vertical_layers + 1)
    
    for layer in range(1, vertical_layers + 1):
        level_deg = vert_step * layer
        level_rad = math.radians(level_deg)
        
        # At each layer height, compute the horizontal radius (sphere cross-section)
        layer_radius = radius * math.cos(level_rad)
        y = radius * math.sin(level_rad)
        
        # Adaptive detail (fewer cubes at poles where they cluster)
        if optimize:
            detail = max(1, round(horizontal_detail * layer_radius / radius))
        else:
            detail = horizontal_detail
        
        # FIXED: Include the full range by stepping through ALL quadrant angles
        horiz_step = 90.0 / detail
        
        # Generate cubes for each horizontal subdivision
        # Start from horiz_step (not 0) to avoid degenerate cubes
        for h in range(1, detail + 1):  # FIXED: include detail (was detail-1)
            deg = horiz_step * h
            rad = math.radians(deg)
            
            # Cube half-extents at this angle
            cx = layer_radius * math.cos(rad)
            cz = layer_radius * math.sin(rad)
            
            # Cube spans from negative to positive on all axes (symmetric)
            # This creates a box that, when viewed from any axis, shows the sphere profile
            cubes.append(CubeElement(-cx, -y, -cz, cx, y, cz))
    
    return cubes


# ═══════════════════════════════════════════════════════════════════════════════
# GENERALIZED TYPE_A - Works with any shape profile!
# ═══════════════════════════════════════════════════════════════════════════════

def generate_type_a_generalized(
    shape: str = "sphere",
    radius: float = 1.0,
    height: float = 2.0,
    vertical_layers: int = 8,
    horizontal_detail: int = 6,
    **kwargs
) -> List[CubeElement]:
    """
    Generalized TYPE_A algorithm that works with different shape profiles.
    
    Shapes:
    - sphere: Classic sphere (default)
    - ellipsoid: Stretched sphere (use scale_x, scale_z kwargs)
    - cylinder: Constant radius tube
    - cone: Linear taper (use tip_at_top kwarg)
    - paraboloid: Bowl/dish shape
    - hourglass: Pinched middle (use waist kwarg)
    
    Returns list of cube elements that create the shape silhouette.
    """
    cubes = []
    
    half_height = height / 2
    vert_step = height / (vertical_layers + 1)
    
    for layer in range(1, vertical_layers + 1):
        # Y position for this layer (-half_height to +half_height)
        y = -half_height + vert_step * layer
        y_normalized = y / half_height  # -1 to +1
        
        # Get radius at this height based on shape profile
        if shape == "sphere":
            layer_radius = ShapeProfile.sphere(y_normalized, radius)
        elif shape == "ellipsoid":
            rx, rz = ShapeProfile.ellipsoid(
                y_normalized, radius,
                kwargs.get('scale_x', 1.0),
                kwargs.get('scale_z', 1.0)
            )
            # For ellipsoid, we'll use the average for now (TODO: proper ellipse)
            layer_radius = (rx + rz) / 2
        elif shape == "cylinder":
            layer_radius = ShapeProfile.cylinder(y_normalized, radius)
        elif shape == "cone":
            layer_radius = ShapeProfile.cone(
                y_normalized, radius,
                kwargs.get('tip_at_top', True)
            )
        elif shape == "paraboloid":
            layer_radius = ShapeProfile.paraboloid(y_normalized, radius)
        elif shape == "hourglass":
            layer_radius = ShapeProfile.hourglass(
                y_normalized, radius,
                kwargs.get('waist', 0.3)
            )
        else:
            layer_radius = ShapeProfile.sphere(y_normalized, radius)
        
        if layer_radius <= 0.001:
            continue
        
        # Adaptive horizontal detail
        detail = max(1, round(horizontal_detail * layer_radius / radius))
        horiz_step = 90.0 / detail
        
        for h in range(1, detail + 1):
            deg = horiz_step * h
            rad = math.radians(deg)
            
            cx = layer_radius * math.cos(rad)
            cz = layer_radius * math.sin(rad)
            
            cubes.append(CubeElement(-cx, -y, -cz, cx, y, cz))
    
    return cubes


def type_a_to_mesh(cubes: List[CubeElement]) -> Mesh:
    """Convert TYPE_A cube elements to a renderable mesh (for visualization).
    
    For proper visualization, we only render the OUTER faces of each cube
    that would be visible when rendered with depth testing.
    
    Note: Uses correct CCW winding for outward-facing normals.
    """
    mesh = Mesh()
    
    for cube in cubes:
        base_idx = len(mesh.vertices)
        
        # 8 corners of the cube
        corners = [
            (cube.x1, cube.y1, cube.z1),  # 0: 000
            (cube.x2, cube.y1, cube.z1),  # 1: 100
            (cube.x1, cube.y2, cube.z1),  # 2: 010
            (cube.x2, cube.y2, cube.z1),  # 3: 110
            (cube.x1, cube.y1, cube.z2),  # 4: 001
            (cube.x2, cube.y1, cube.z2),  # 5: 101
            (cube.x1, cube.y2, cube.z2),  # 6: 011
            (cube.x2, cube.y2, cube.z2),  # 7: 111
        ]
        
        for x, y, z in corners:
            mesh.vertices.append(Vertex(x, y, z))
        
        # 6 faces with CCW winding
        faces = [
            (0, 2, 3, 1),  # -Z face
            (4, 5, 7, 6),  # +Z face
            (0, 4, 6, 2),  # -X face
            (1, 3, 7, 5),  # +X face
            (0, 1, 5, 4),  # -Y face
            (2, 6, 7, 3),  # +Y face
        ]
        
        for v0, v1, v2, v3 in faces:
            mesh.triangles.append(Triangle(base_idx + v0, base_idx + v1, base_idx + v2))
            mesh.triangles.append(Triangle(base_idx + v0, base_idx + v2, base_idx + v3))
    
    return mesh


def generalized_silhouette(cubes: List[CubeElement], max_radius: float, resolution: int = 32) -> Mesh:
    """
    Generate silhouette mesh for any set of cubes (generalized shapes).
    Works by sampling the outer boundary from all directions.
    """
    mesh = Mesh()
    
    lat_steps = resolution
    lon_steps = resolution * 2
    
    # Find Y bounds of the cubes
    y_min = min(cube.y1 for cube in cubes)
    y_max = max(cube.y2 for cube in cubes)
    y_range = y_max - y_min
    
    vertex_grid = []
    for lat in range(lat_steps + 1):
        # Map to Y range of the shape
        t = lat / lat_steps
        y_sample = y_min + t * y_range
        
        row = []
        for lon in range(lon_steps + 1):
            phi = 2 * math.pi * lon / lon_steps
            
            # Horizontal direction
            dx = math.cos(phi)
            dz = math.sin(phi)
            
            # Find farthest cube extent in this horizontal direction at this Y
            max_dist = 0
            for cube in cubes:
                # Check if this cube spans our Y level
                if cube.y1 <= y_sample <= cube.y2 or cube.y1 <= -y_sample <= cube.y2:
                    # Project corners onto the direction
                    for cx in [cube.x1, cube.x2]:
                        for cz in [cube.z1, cube.z2]:
                            dot = cx * dx + cz * dz
                            if dot > max_dist:
                                max_dist = dot
            
            # Create vertex
            x = max_dist * dx
            z = max_dist * dz
            
            row.append(len(mesh.vertices))
            mesh.vertices.append(Vertex(x, y_sample, z, dx, 0, dz))
        vertex_grid.append(row)
    
    # Generate triangles
    for lat in range(lat_steps):
        for lon in range(lon_steps):
            tl = vertex_grid[lat][lon]
            tr = vertex_grid[lat][lon + 1]
            bl = vertex_grid[lat + 1][lon]
            br = vertex_grid[lat + 1][lon + 1]
            
            mesh.triangles.append(Triangle(tl, tr, br))
            mesh.triangles.append(Triangle(tl, br, bl))
    
    return mesh


def type_a_silhouette(radius: float = 1.0, resolution: int = 32) -> Mesh:
    """
    Generate the INTENDED silhouette of TYPE_A - what it looks like 
    when properly rendered with depth testing.
    
    This samples the outer boundary by finding the maximum extent
    at each angle - essentially ray-marching from outside.
    """
    mesh = Mesh()
    
    # Generate cubes
    cubes = generate_type_a(radius, vertical_layers=8, horizontal_detail=6)
    
    # For each direction, find the outermost point
    # We'll use a latitude/longitude grid to sample the silhouette
    lat_steps = resolution
    lon_steps = resolution * 2
    
    vertex_grid = []
    for lat in range(lat_steps + 1):
        theta = math.pi * lat / lat_steps
        row = []
        for lon in range(lon_steps + 1):
            phi = 2 * math.pi * lon / lon_steps
            
            # Ray direction
            dx = math.sin(theta) * math.cos(phi)
            dy = math.cos(theta)
            dz = math.sin(theta) * math.sin(phi)
            
            # Find farthest intersection with any cube
            max_dist = 0
            for cube in cubes:
                # Check if ray from origin in direction (dx,dy,dz) hits this cube
                # For axis-aligned box, we can compute the extent in this direction
                # The cube spans from (x1,y1,z1) to (x2,y2,z2)
                # For symmetric cubes around origin, the extent is max of the corners
                
                # Project each corner onto the ray direction
                for corner_signs in [(-1,-1,-1), (-1,-1,1), (-1,1,-1), (-1,1,1),
                                     (1,-1,-1), (1,-1,1), (1,1,-1), (1,1,1)]:
                    cx = cube.x2 if corner_signs[0] > 0 else cube.x1
                    cy = cube.y2 if corner_signs[1] > 0 else cube.y1
                    cz = cube.z2 if corner_signs[2] > 0 else cube.z1
                    
                    # Dot product gives distance along ray
                    dot = cx * dx + cy * dy + cz * dz
                    if dot > max_dist:
                        max_dist = dot
            
            # Clamp to sphere radius (the cubes might extend beyond)
            max_dist = min(max_dist, radius * 1.1)
            
            # Create vertex at this point on the silhouette
            x = max_dist * dx
            y = max_dist * dy
            z = max_dist * dz
            
            row.append(len(mesh.vertices))
            mesh.vertices.append(Vertex(x, y, z, dx, dy, dz))
        vertex_grid.append(row)
    
    # Generate triangles
    for lat in range(lat_steps):
        for lon in range(lon_steps):
            tl = vertex_grid[lat][lon]
            tr = vertex_grid[lat][lon + 1]
            bl = vertex_grid[lat + 1][lon]
            br = vertex_grid[lat + 1][lon + 1]
            
            mesh.triangles.append(Triangle(tl, tr, br))
            mesh.triangles.append(Triangle(tl, br, bl))
    
    return mesh


# ═══════════════════════════════════════════════════════════════════════════════
# ALGORITHM 5: TYPE_E (Rotated Rectangles - Efficiency)
# ═══════════════════════════════════════════════════════════════════════════════

def generate_type_e(radius: float = 1.0, vertical_layers: int = 8,
                    quadrant_subdivision: int = 5) -> List[CubeElement]:
    """
    Type E sphere approximation using rotated rectangles.
    
    From the original Python sphere generator:
    - Creates rotated rectangles forming polygonal cross-sections
    - quadrant_subdivision=1 → Square (4-sided, 1 cube at 45°)
    - quadrant_subdivision=3 → Octagon (8-sided, 4 rectangles)
    - quadrant_subdivision=5 → Hexadecagon (16-sided, 8 rectangles)
    
    The rectangles span from -y to +y (symmetric), creating a full sphere.
    At each layer, thin rotated slabs approximate the circular cross-section.
    
    This is NOT a mesh - it's a list of cube elements with Y-rotation.
    
    Pros: Much fewer elements than TYPE_A, efficient rendering
    Cons: Less accurate silhouette, harder to texture
    """
    if quadrant_subdivision not in [1, 3, 5]:
        print(f"Warning: quadrant_subdivision should be 1, 3, or 5. Got {quadrant_subdivision}, using 5.")
        quadrant_subdivision = 5
    
    cubes = []
    
    # Use more layers for smoother result
    vert_step = 90.0 / (vertical_layers + 1)
    
    for layer in range(1, vertical_layers + 1):
        level_deg = vert_step * layer
        level_rad = math.radians(level_deg)
        
        # Horizontal radius at this layer (sphere cross-section)
        layer_radius = radius * math.cos(level_rad)
        # Height from equator
        y = radius * math.sin(level_rad)
        
        if quadrant_subdivision == 1:
            # Square: single cube rotated 45°
            side = layer_radius * math.sin(math.pi / 4)
            cubes.append(CubeElement(-side, -y, -side, side, y, side, rotation_y=45.0))
            
        elif quadrant_subdivision == 3:
            # Octagon: 4 rectangles
            side = layer_radius * math.tan(math.pi / 8)
            
            # Z-aligned at 0° and 45°
            for angle in [0, 45]:
                cubes.append(CubeElement(-side, -y, -layer_radius, side, y, layer_radius, rotation_y=angle))
            
            # X-aligned at 0° and 45°
            for angle in [0, 45]:
                cubes.append(CubeElement(-layer_radius, -y, -side, layer_radius, y, side, rotation_y=angle))
                
        elif quadrant_subdivision == 5:
            # Hexadecagon: 8 rectangles per layer
            side = layer_radius * math.tan(math.pi / 16)
            
            # Z-aligned rectangles at 5 rotations (-45° to 45° in 22.5° steps)
            for angle in [-45, -22.5, 0, 22.5, 45]:
                cubes.append(CubeElement(-side, -y, -layer_radius, side, y, layer_radius, rotation_y=angle))
            
            # X-aligned rectangles at 3 rotations
            for angle in [-22.5, 0, 22.5]:
                cubes.append(CubeElement(-layer_radius, -y, -side, layer_radius, y, side, rotation_y=angle))
    
    return cubes


def type_e_to_mesh(cubes: List[CubeElement]) -> Mesh:
    """Convert TYPE_E cube elements to a renderable mesh (with rotation applied).
    
    Note: Uses correct CCW winding for outward-facing normals.
    """
    mesh = Mesh()
    
    for cube in cubes:
        base_idx = len(mesh.vertices)
        
        # Apply Y rotation
        cos_r = math.cos(math.radians(cube.rotation_y))
        sin_r = math.sin(math.radians(cube.rotation_y))
        
        def rotate_y(x, z):
            return (x * cos_r - z * sin_r, x * sin_r + z * cos_r)
        
        # 8 corners indexed same as TYPE_A
        corners = [
            (cube.x1, cube.y1, cube.z1),  # 0: 000
            (cube.x2, cube.y1, cube.z1),  # 1: 100
            (cube.x1, cube.y2, cube.z1),  # 2: 010
            (cube.x2, cube.y2, cube.z1),  # 3: 110
            (cube.x1, cube.y1, cube.z2),  # 4: 001
            (cube.x2, cube.y1, cube.z2),  # 5: 101
            (cube.x1, cube.y2, cube.z2),  # 6: 011
            (cube.x2, cube.y2, cube.z2),  # 7: 111
        ]
        
        # Apply Y rotation to each corner
        for x, y, z in corners:
            rx, rz = rotate_y(x, z)
            mesh.vertices.append(Vertex(rx, y, rz))
        
        # 6 faces with CCW winding (same as TYPE_A)
        faces = [
            (0, 2, 3, 1),  # -Z face
            (4, 5, 7, 6),  # +Z face
            (0, 4, 6, 2),  # -X face
            (1, 3, 7, 5),  # +X face
            (0, 1, 5, 4),  # -Y face
            (2, 6, 7, 3),  # +Y face
        ]
        
        for v0, v1, v2, v3 in faces:
            mesh.triangles.append(Triangle(base_idx + v0, base_idx + v1, base_idx + v2))
            mesh.triangles.append(Triangle(base_idx + v0, base_idx + v2, base_idx + v3))
    
    return mesh


# ═══════════════════════════════════════════════════════════════════════════════
# COMPARISON & ANALYSIS
# ═══════════════════════════════════════════════════════════════════════════════

def compare_algorithms(radius: float = 1.0):
    """Compare all algorithms and print statistics."""
    print("\n" + "="*70)
    print("SPHERE ALGORITHM COMPARISON")
    print("="*70)
    print(f"Radius: {radius}")
    print("-"*70)
    
    results = []
    
    # LAT_LON
    mesh = tessellate_lat_lon(radius, lat_steps=16, lon_steps=32)
    stats = mesh.stats()
    stats["algorithm"] = "LAT_LON"
    stats["params"] = "lat=16, lon=32"
    results.append(stats)
    
    # UV_SPHERE
    mesh = tessellate_uv_sphere(radius, segments=32, rings=16)
    stats = mesh.stats()
    stats["algorithm"] = "UV_SPHERE"
    stats["params"] = "seg=32, rings=16"
    results.append(stats)
    
    # ICO_SPHERE (various subdivisions)
    for subdiv in [1, 2, 3]:
        mesh = tessellate_ico_sphere(radius, subdivisions=subdiv)
        stats = mesh.stats()
        stats["algorithm"] = f"ICO_SPHERE"
        stats["params"] = f"subdiv={subdiv}"
        results.append(stats)
    
    # TYPE_A
    cubes = generate_type_a(radius, vertical_layers=6, horizontal_detail=4)
    mesh = type_a_to_mesh(cubes)
    stats = mesh.stats()
    stats["algorithm"] = "TYPE_A"
    stats["params"] = f"layers=6, detail=4 ({len(cubes)} cubes)"
    results.append(stats)
    
    # TYPE_E
    cubes = generate_type_e(radius, vertical_layers=6, quadrant_subdivision=5)
    mesh = type_e_to_mesh(cubes)
    stats = mesh.stats()
    stats["algorithm"] = "TYPE_E"
    stats["params"] = f"layers=6, subdiv=5 ({len(cubes)} cubes)"
    results.append(stats)
    
    # Print table
    print(f"{'Algorithm':<15} {'Params':<30} {'Vertices':>10} {'Triangles':>10}")
    print("-"*70)
    for r in results:
        print(f"{r['algorithm']:<15} {r['params']:<30} {r['vertices']:>10} {r['triangles']:>10}")
    
    print("\n" + "="*70)
    print("NOTES:")
    print("-"*70)
    print("• LAT_LON/UV_SPHERE: Good for patterns, pole singularities")
    print("• ICO_SPHERE: Uniform distribution, no poles, good for smooth spheres")
    print("• TYPE_A: Accurate silhouette, many overlapping faces (MC block models)")
    print("• TYPE_E: Efficient, fewer faces, uses rotation (MC block models)")
    print("="*70)


# ═══════════════════════════════════════════════════════════════════════════════
# VISUALIZATION (Optional - requires matplotlib)
# ═══════════════════════════════════════════════════════════════════════════════

def visualize_algorithm(algorithm: str, radius: float = 1.0, save_to_file: bool = True):
    """Visualize a sphere algorithm using matplotlib."""
    try:
        # Use non-interactive backend for file saving (works without display)
        import matplotlib
        if save_to_file:
            matplotlib.use('Agg')
        import matplotlib.pyplot as plt
        from mpl_toolkits.mplot3d import Axes3D
        from mpl_toolkits.mplot3d.art3d import Poly3DCollection
    except ImportError:
        print("matplotlib not available. Install with: pip install matplotlib")
        return
    
    # Generate mesh based on algorithm
    if algorithm == "lat_lon":
        mesh = tessellate_lat_lon(radius, lat_steps=12, lon_steps=24)
        title = "LAT_LON Sphere"
    elif algorithm == "uv_sphere":
        mesh = tessellate_uv_sphere(radius, segments=24, rings=12)
        title = "UV_SPHERE"
    elif algorithm == "ico_sphere":
        mesh = tessellate_ico_sphere(radius, subdivisions=2)
        title = "ICO_SPHERE (subdiv=2)"
    elif algorithm == "type_a":
        # Show the SILHOUETTE - what TYPE_A actually produces when rendered properly
        mesh = type_a_silhouette(radius, resolution=24)
        cubes = generate_type_a(radius, vertical_layers=8, horizontal_detail=6)
        title = f"TYPE_A Silhouette ({len(cubes)} cubes → smooth surface)"
    elif algorithm == "type_a_raw":
        # Show raw cubes for debugging
        cubes = generate_type_a(radius, vertical_layers=6, horizontal_detail=4)
        mesh = type_a_to_mesh(cubes)
        title = f"TYPE_A Raw ({len(cubes)} overlapping cubes)"
    elif algorithm == "type_e":
        cubes = generate_type_e(radius, vertical_layers=6, quadrant_subdivision=5)
        mesh = type_e_to_mesh(cubes)
        title = f"TYPE_E ({len(cubes)} cubes)"
    # === GENERALIZED SHAPES ===
    elif algorithm == "cylinder":
        cubes = generate_type_a_generalized("cylinder", radius, height=2.0, vertical_layers=8)
        mesh = generalized_silhouette(cubes, radius * 1.1, 24)
        title = f"CYLINDER via TYPE_A ({len(cubes)} cubes)"
    elif algorithm == "cone":
        cubes = generate_type_a_generalized("cone", radius, height=2.0, vertical_layers=10)
        mesh = generalized_silhouette(cubes, radius * 1.1, 24)
        title = f"CONE via TYPE_A ({len(cubes)} cubes)"
    elif algorithm == "paraboloid":
        cubes = generate_type_a_generalized("paraboloid", radius, height=2.0, vertical_layers=10)
        mesh = generalized_silhouette(cubes, radius * 1.1, 24)
        title = f"PARABOLOID via TYPE_A ({len(cubes)} cubes)"
    elif algorithm == "hourglass":
        cubes = generate_type_a_generalized("hourglass", radius, height=2.0, vertical_layers=10, waist=0.3)
        mesh = generalized_silhouette(cubes, radius * 1.1, 24)
        title = f"HOURGLASS via TYPE_A ({len(cubes)} cubes)"
    else:
        print(f"Unknown algorithm: {algorithm}")
        return
    
    # Plot
    fig = plt.figure(figsize=(10, 8))
    ax = fig.add_subplot(111, projection='3d')
    
    # For raw cube views, use wireframe (better visualization of cube structure)
    use_wireframe = algorithm in ['type_a_raw', 'type_e']
    
    # Extract triangle vertices
    triangles_3d = []
    for tri in mesh.triangles:
        v0 = mesh.vertices[tri.v0]
        v1 = mesh.vertices[tri.v1]
        v2 = mesh.vertices[tri.v2]
        triangles_3d.append([
            [v0.x, v0.y, v0.z],
            [v1.x, v1.y, v1.z],
            [v2.x, v2.y, v2.z]
        ])
    
    # Add collection with appropriate style
    if use_wireframe:
        # Wireframe only - shows cube structure clearly
        collection = Poly3DCollection(triangles_3d, alpha=0.1, linewidth=0.3, 
                                       edgecolor='blue', facecolor='lightblue')
    else:
        # Solid rendering for traditional sphere algorithms
        collection = Poly3DCollection(triangles_3d, alpha=0.7, linewidth=0.5, 
                                       edgecolor='darkblue', facecolor='cyan')
    ax.add_collection3d(collection)
    
    # Set limits
    ax.set_xlim([-radius*1.2, radius*1.2])
    ax.set_ylim([-radius*1.2, radius*1.2])
    ax.set_zlim([-radius*1.2, radius*1.2])
    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')
    ax.set_title(f"{title}\n{mesh.stats()}")
    
    plt.tight_layout()
    
    if save_to_file:
        output_path = f"scripts/sphere_{algorithm}.png"
        plt.savefig(output_path, dpi=150, bbox_inches='tight')
        print(f"Saved: {output_path}")
        plt.close()
    else:
        plt.show()


# ═══════════════════════════════════════════════════════════════════════════════
# JAVA CODE GENERATION
# ═══════════════════════════════════════════════════════════════════════════════

def generate_java_icosphere():
    """Generate Java code for IcoSphere tessellation."""
    java_code = '''
// ═══════════════════════════════════════════════════════════════════════════════
// ICO_SPHERE TESSELLATION - Generated from Python prototype
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tessellates a sphere using icosahedron subdivision.
 * 
 * @param radius Sphere radius
 * @param subdivisions Number of subdivision levels (0-4 recommended)
 * @return Generated mesh
 */
public static Mesh tessellateIcoSphere(float radius, int subdivisions) {
    MeshBuilder builder = MeshBuilder.triangles();
    
    // Golden ratio
    final float PHI = (1.0f + (float) Math.sqrt(5.0)) / 2.0f;
    
    // Icosahedron vertices (12 vertices)
    float[][] icoVerts = {
        {-1,  PHI, 0}, { 1,  PHI, 0}, {-1, -PHI, 0}, { 1, -PHI, 0},
        { 0, -1,  PHI}, { 0,  1,  PHI}, { 0, -1, -PHI}, { 0,  1, -PHI},
        { PHI, 0, -1}, { PHI, 0,  1}, {-PHI, 0, -1}, {-PHI, 0,  1},
    };
    
    // Add normalized vertices
    int[] vertexIndices = new int[12];
    for (int i = 0; i < 12; i++) {
        float x = icoVerts[i][0];
        float y = icoVerts[i][1];
        float z = icoVerts[i][2];
        float len = (float) Math.sqrt(x*x + y*y + z*z);
        vertexIndices[i] = builder.addVertex(Vertex.spherical(
            x/len * radius, y/len * radius, z/len * radius));
    }
    
    // Icosahedron faces (20 triangles)
    int[][] icoFaces = {
        {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
        {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
        {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
        {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1},
    };
    
    // Build initial triangle list
    List<int[]> triangles = new ArrayList<>();
    for (int[] face : icoFaces) {
        triangles.add(new int[]{vertexIndices[face[0]], 
                                vertexIndices[face[1]], 
                                vertexIndices[face[2]]});
    }
    
    // Subdivision with midpoint cache
    Map<Long, Integer> midpointCache = new HashMap<>();
    
    for (int level = 0; level < subdivisions; level++) {
        List<int[]> newTriangles = new ArrayList<>();
        midpointCache.clear();
        
        for (int[] tri : triangles) {
            int a = getMidpoint(builder, midpointCache, tri[0], tri[1], radius);
            int b = getMidpoint(builder, midpointCache, tri[1], tri[2], radius);
            int c = getMidpoint(builder, midpointCache, tri[2], tri[0], radius);
            
            newTriangles.add(new int[]{tri[0], a, c});
            newTriangles.add(new int[]{tri[1], b, a});
            newTriangles.add(new int[]{tri[2], c, b});
            newTriangles.add(new int[]{a, b, c});
        }
        
        triangles = newTriangles;
    }
    
    // Add triangles to mesh
    for (int[] tri : triangles) {
        builder.triangle(tri[0], tri[1], tri[2]);
    }
    
    return builder.build();
}

private static int getMidpoint(MeshBuilder builder, Map<Long, Integer> cache, 
                               int v0, int v1, float radius) {
    long key = Math.min(v0, v1) * 100000L + Math.max(v0, v1);
    if (cache.containsKey(key)) {
        return cache.get(key);
    }
    
    Vertex vert0 = builder.getVertex(v0);
    Vertex vert1 = builder.getVertex(v1);
    
    float mx = (vert0.x() + vert1.x()) / 2;
    float my = (vert0.y() + vert1.y()) / 2;
    float mz = (vert0.z() + vert1.z()) / 2;
    
    // Project to sphere surface
    float len = (float) Math.sqrt(mx*mx + my*my + mz*mz);
    mx = mx/len * radius;
    my = my/len * radius;
    mz = mz/len * radius;
    
    int idx = builder.addVertex(Vertex.spherical(mx, my, mz));
    cache.put(key, idx);
    return idx;
}
'''
    return java_code


def generate_java_type_a():
    """Generate Java code for TYPE_A direct rendering."""
    java_code = '''
// ═══════════════════════════════════════════════════════════════════════════════
// TYPE_A SPHERE RENDERING - Generated from Python prototype
// Overlapping axis-aligned cubes for accurate sphere approximation
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Renders a TYPE_A sphere using overlapping cubes.
 * 
 * @param matrices MatrixStack for transformation
 * @param buffer Vertex consumer
 * @param radius Sphere radius
 * @param verticalLayers Number of vertical layers (4-8 recommended)
 * @param horizontalDetail Horizontal detail per layer (2-4 recommended)
 * @param color ARGB color
 * @param light Light level
 */
public static void renderTypeA(MatrixStack matrices, VertexConsumer buffer,
                                float radius, int verticalLayers, int horizontalDetail,
                                int color, int light) {
    double vertStep = 90.0 / (verticalLayers + 1);
    
    for (int layer = 1; layer <= verticalLayers; layer++) {
        double levelDeg = vertStep * layer;
        double levelRad = Math.toRadians(levelDeg);
        
        float layerRadius = (float) (radius * Math.cos(levelRad));
        float y = (float) (radius * Math.sin(levelRad));
        
        // Adaptive detail (fewer cubes at poles) - optimization
        int detail = Math.max(1, Math.round(horizontalDetail * layerRadius / radius));
        double horizStep = 90.0 / detail;
        
        for (int h = 1; h < detail; h++) {
            double deg = horizStep * h;
            double rad = Math.toRadians(deg);
            
            float cx = (float) (layerRadius * Math.cos(rad));
            float cz = (float) (layerRadius * Math.sin(rad));
            
            // Render cube from (-cx, -y, -cz) to (cx, y, cz)
            renderCube(matrices, buffer, -cx, -y, -cz, cx, y, cz, color, light);
        }
    }
}
'''
    return java_code


def generate_java_type_e():
    """Generate Java code for TYPE_E direct rendering."""
    java_code = '''
// ═══════════════════════════════════════════════════════════════════════════════
// TYPE_E SPHERE RENDERING - Generated from Python prototype
// Rotated rectangles forming hexadecagon cross-sections
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Renders a TYPE_E sphere using rotated rectangles.
 * 
 * More efficient than TYPE_A with fewer overlapping faces.
 * Uses hexadecagon (16-sided) cross-sections.
 * 
 * @param matrices MatrixStack for transformation
 * @param buffer Vertex consumer
 * @param radius Sphere radius
 * @param verticalLayers Number of vertical layers (4-8 recommended)
 * @param color ARGB color
 * @param light Light level
 */
public static void renderTypeE(MatrixStack matrices, VertexConsumer buffer,
                                float radius, int verticalLayers,
                                int color, int light) {
    double vertStep = 90.0 / (verticalLayers + 1);
    float sideRatio = (float) Math.tan(Math.PI / 16.0);  // Hexadecagon
    
    for (int layer = 1; layer <= verticalLayers; layer++) {
        double levelDeg = vertStep * layer;
        double levelRad = Math.toRadians(levelDeg);
        
        float layerRadius = (float) (radius * Math.cos(levelRad));
        float y = (float) (radius * Math.sin(levelRad));
        float side = layerRadius * sideRatio;
        
        // Z-aligned rectangles at various rotations (-45° to 45°)
        for (float angle = -45f; angle < 67.5f; angle += 22.5f) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
            renderCube(matrices, buffer, -side, -y, -layerRadius, side, y, layerRadius, color, light);
            matrices.pop();
        }
        
        // X-aligned rectangles
        for (float angle = -22.5f; angle < 45f; angle += 22.5f) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
            renderCube(matrices, buffer, -layerRadius, -y, -side, layerRadius, y, side, color, light);
            matrices.pop();
        }
    }
}
'''
    return java_code


# ═══════════════════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════════════════

def main():
    parser = argparse.ArgumentParser(description='Sphere Algorithm Prototyping')
    parser.add_argument('--algorithm', '-a', type=str, default='all',
                        choices=['lat_lon', 'uv_sphere', 'ico_sphere', 'type_a', 'type_a_raw', 'type_e', 
                                 'cylinder', 'cone', 'paraboloid', 'hourglass', 'all'],
                        help='Algorithm to test (type_a shows silhouette, type_a_raw shows overlapping cubes)')
    parser.add_argument('--visualize', '-v', action='store_true',
                        help='Visualize the algorithm (requires matplotlib)')
    parser.add_argument('--save', '-s', action='store_true',
                        help='Save visualization to PNG file (works without display)')
    parser.add_argument('--compare', '-c', action='store_true',
                        help='Compare all algorithms')
    parser.add_argument('--export-java', '-j', action='store_true',
                        help='Export Java code for the algorithm')
    parser.add_argument('--radius', '-r', type=float, default=1.0,
                        help='Sphere radius')
    
    args = parser.parse_args()
    
    if args.compare or args.algorithm == 'all':
        compare_algorithms(args.radius)
    
    if args.visualize or args.save:
        save_to_file = args.save or True  # Default to saving since display often doesn't work
        if args.algorithm == 'all':
            for algo in ['lat_lon', 'uv_sphere', 'ico_sphere', 'type_a', 'type_e']:
                visualize_algorithm(algo, args.radius, save_to_file)
            # Also generate raw type_a for comparison
            visualize_algorithm('type_a_raw', args.radius, save_to_file)
        else:
            visualize_algorithm(args.algorithm, args.radius, save_to_file)
    
    if args.export_java:
        print("\n" + "="*70)
        print("JAVA CODE GENERATION")
        print("="*70)
        
        if args.algorithm in ['ico_sphere', 'all']:
            print("\n--- ICO_SPHERE ---")
            print(generate_java_icosphere())
        
        if args.algorithm in ['type_a', 'all']:
            print("\n--- TYPE_A ---")
            print(generate_java_type_a())
        
        if args.algorithm in ['type_e', 'all']:
            print("\n--- TYPE_E ---")
            print(generate_java_type_e())


if __name__ == '__main__':
    main()

