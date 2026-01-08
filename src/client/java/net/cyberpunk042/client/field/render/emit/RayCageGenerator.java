package net.cyberpunk042.client.field.render.emit;

import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.mesh.ray.RayContext;
import net.cyberpunk042.client.visual.mesh.ray.RayGeometryUtils;
import net.cyberpunk042.visual.shape.RayCurvature;
import net.cyberpunk042.visual.shape.SphereDeformation;

/**
 * Generates cage (lat/lon grid) geometry for 3D ray shapes.
 * 
 * <p>Extracted from RaysRenderer to follow Single Responsibility Principle.</p>
 * 
 * @see RaysRenderer
 */
public final class RayCageGenerator {
    
    private RayCageGenerator() {} // Utility class
    
    /**
     * Generates cage (lat/lon grid) for a single droplet shape.
     * 
     * @param builder MeshBuilder to add geometry to
     * @param context Ray context with position/orientation data
     * @param latCount Number of latitude rings
     * @param lonCount Number of longitude meridians
     */
    public static void generateDropletCage(
            MeshBuilder builder,
            RayContext context,
            int latCount, int lonCount) {
        
        // Get droplet parameters - use shapeLength for consistent sizing with solid rendering
        float length = context.length();
        float axialLength = context.shapeLength();
        float radius = axialLength * 0.5f;  // Base radius matches solid rendering (shapeLength * 0.5)
        float intensity = context.shapeIntensity();
        
        // Get ray endpoints
        float[] start = context.start();
        float[] end = context.end();
        
        // === APPLY CURVATURE TO POSITION ===
        // Curvature affects WHERE the cage is placed (center position)
        // Orientation is ALWAYS respected for which way the tip points
        RayCurvature curvature = context.curvature();
        float curvatureIntensity = context.curvatureIntensity();
        
        float[] center;
        
        if (curvature != null && curvature != RayCurvature.NONE 
            && curvatureIntensity > 0.001f) {
            // Place center at curved midpoint
            center = RayGeometryUtils.computeCurvedPosition(
                start, end, 0.5f, curvature, curvatureIntensity);
        } else {
            // No curvature
            center = new float[] {
                (start[0] + end[0]) * 0.5f,
                (start[1] + end[1]) * 0.5f,
                (start[2] + end[2]) * 0.5f
            };
        }
        
        // Orientation always comes from context (respects user's RayOrientation setting)
        float[] direction = context.orientationVector();
        
        // Build basis vectors for the droplet orientation
        float[] up = new float[] { 0, 1, 0 };
        if (Math.abs(direction[0] * up[0] + direction[1] * up[1] + direction[2] * up[2]) > 0.99f) {
            up = new float[] { 1, 0, 0 };
        }
        // Cross products to get orthonormal basis
        float[] u = normalize(cross(up, direction));
        float[] v = normalize(cross(direction, u));
        
        // Use SphereDeformation.DROPLET to get accurate radii
        SphereDeformation deformation = SphereDeformation.DROPLET;
        
        // === LATITUDE LINES (horizontal rings) ===
        for (int lat = 1; lat < latCount; lat++) {
            float theta = (float)(Math.PI * lat / latCount);
            
            // Get radius at this latitude from deformation
            float[] deformed = deformation.computeFullVertex(theta, 0, radius, intensity, axialLength);
            float ringRadius = (float)Math.sqrt(deformed[0] * deformed[0] + deformed[2] * deformed[2]);
            float axialPos = deformed[1];
            
            // Draw ring as line segments
            int segments = Math.max(lonCount * 2, 12);
            for (int i = 0; i < segments; i++) {
                float phi1 = (float)(2 * Math.PI * i / segments);
                float phi2 = (float)(2 * Math.PI * (i + 1) / segments);
                
                // Local coords (ring in XZ plane at height axialPos)
                float lx1 = (float)Math.cos(phi1) * ringRadius;
                float lz1 = (float)Math.sin(phi1) * ringRadius;
                float lx2 = (float)Math.cos(phi2) * ringRadius;
                float lz2 = (float)Math.sin(phi2) * ringRadius;
                
                // Transform to world coords using basis
                float[] p1 = transformToWorld(center, u, direction, v, lx1, axialPos, lz1);
                float[] p2 = transformToWorld(center, u, direction, v, lx2, axialPos, lz2);
                
                int idx1 = builder.addVertex(Vertex.pos(p1[0], p1[1], p1[2]));
                int idx2 = builder.addVertex(Vertex.pos(p2[0], p2[1], p2[2]));
                builder.line(idx1, idx2);
            }
        }
        
        // === LONGITUDE LINES (vertical meridians) ===
        for (int lon = 0; lon < lonCount; lon++) {
            float phi = (float)(2 * Math.PI * lon / lonCount);
            
            // Draw meridian from pole to pole
            int segments = Math.max(latCount * 2, 12);
            for (int i = 0; i < segments; i++) {
                float theta1 = (float)(Math.PI * i / segments);
                float theta2 = (float)(Math.PI * (i + 1) / segments);
                
                // Get deformed positions
                float[] d1 = deformation.computeFullVertex(theta1, phi, radius, intensity, axialLength);
                float[] d2 = deformation.computeFullVertex(theta2, phi, radius, intensity, axialLength);
                
                // Transform to world coords
                float[] p1 = transformToWorld(center, u, direction, v, d1[0], d1[1], d1[2]);
                float[] p2 = transformToWorld(center, u, direction, v, d2[0], d2[1], d2[2]);
                
                int idx1 = builder.addVertex(Vertex.pos(p1[0], p1[1], p1[2]));
                int idx2 = builder.addVertex(Vertex.pos(p2[0], p2[1], p2[2]));
                builder.line(idx1, idx2);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Transform local coords to world using basis vectors */
    private static float[] transformToWorld(float[] center, float[] u, float[] dir, float[] v, float lx, float ly, float lz) {
        return new float[] {
            center[0] + u[0] * lx + dir[0] * ly + v[0] * lz,
            center[1] + u[1] * lx + dir[1] * ly + v[1] * lz,
            center[2] + u[2] * lx + dir[2] * ly + v[2] * lz
        };
    }
    
    /** Cross product */
    private static float[] cross(float[] a, float[] b) {
        return new float[] {
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }
    
    /** Normalize vector */
    private static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len > 0.0001f) {
            return new float[] { v[0] / len, v[1] / len, v[2] / len };
        }
        return new float[] { 0, 1, 0 };
    }
}
