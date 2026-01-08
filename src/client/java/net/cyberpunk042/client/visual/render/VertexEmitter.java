package net.cyberpunk042.client.visual.render;

import net.cyberpunk042.client.visual.animation.AnimationApplier;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.PrimitiveType;
import net.cyberpunk042.client.visual.mesh.TravelEffectComputer;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.visual.animation.TravelEffectConfig;
import net.cyberpunk042.visual.animation.WaveConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import net.cyberpunk042.visual.appearance.ColorContext;

/**
 * Emits mesh vertices to Minecraft's VertexConsumer.
 * Handles transformation, color, and lighting.
 * 
 * <p>Usage:
 * <pre>
 * VertexEmitter emitter = new VertexEmitter(matrices, consumer);
 * emitter.color(0x80FF0000);  // Semi-transparent red
 * emitter.emit(sphereMesh);
 * </pre>
 */
public final class VertexEmitter {
    
    private final MatrixStack.Entry entry;
    private final VertexConsumer consumer;
    private final Matrix4f positionMatrix;
    private final Matrix3f normalMatrix;
    
    private int color = 0xFFFFFFFF;
    private int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private int overlay = OverlayTexture.DEFAULT_UV;
    
    // Wave animation support
    private WaveConfig waveConfig = null;
    private float waveTime = 0f;
    
    // Travel effect support (directional alpha animation)
    private TravelEffectConfig travelEffect = null;
    private float travelPhase = 0f;
    private float[] travelBoundsMin = null;  // [minX, minY, minZ] for position-based t
    private float[] travelBoundsMax = null;  // [maxX, maxY, maxZ]
    private float shapeBaseMinAlpha = 0f;    // Shape's minimum alpha at base (t=0)
    private float shapeTipMinAlpha = 0f;     // Shape's minimum alpha at tip (t=1)
    
    // Per-vertex color support
    private ColorContext colorContext = null;
    private int cellIndex = 0;
    
    /**
     * Creates an emitter with the current matrix stack entry and consumer.
     */
    public VertexEmitter(MatrixStack.Entry entry, VertexConsumer consumer) {
        this.entry = entry;
        this.consumer = consumer;
        this.positionMatrix = entry.getPositionMatrix();
        this.normalMatrix = entry.getNormalMatrix();
    }
    
    /**
     * Creates an emitter from a matrix stack (uses peek()).
     */
    public VertexEmitter(MatrixStack matrices, VertexConsumer consumer) {
        this(matrices.peek(), consumer);
    }
    
    // =========================================================================
    // Configuration
    // =========================================================================
    
    /**
     * Sets the color as ARGB int.
     */
    public VertexEmitter color(int argb) {
        this.color = argb;
        return this;
    }
    
    /**
     * Sets the color from RGBA components (0-255).
     */
    public VertexEmitter color(int r, int g, int b, int a) {
        this.color = (a << 24) | (r << 16) | (g << 8) | b;
        return this;
    }
    
    /**
     * Sets the color from RGBA components (0.0-1.0).
     */
    public VertexEmitter color(float r, float g, float b, float a) {
        return color(
            (int) (r * 255) & 0xFF,
            (int) (g * 255) & 0xFF,
            (int) (b * 255) & 0xFF,
            (int) (a * 255) & 0xFF
        );
    }
    
    /**
     * Sets the alpha only (preserves RGB).
     */
    public VertexEmitter alpha(float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        this.color = (this.color & 0x00FFFFFF) | (a << 24);
        return this;
    }
    
    /**
     * Sets the light level.
     */
    public VertexEmitter light(int light) {
        this.light = light;
        return this;
    }
    
    /**
     * Sets full brightness.
     */
    public VertexEmitter fullBright() {
        this.light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        return this;
    }
    
    /**
     * Sets the overlay.
     */
    public VertexEmitter overlay(int overlay) {
        this.overlay = overlay;
        return this;
    }
    
    /**
     * Sets wave animation configuration.
     * When set, vertices will be displaced by wave animation during emission.
     * 
     * @param wave Wave configuration
     * @param time Current time in ticks
     * @return this emitter for chaining
     */
    public VertexEmitter wave(WaveConfig wave, float time) {
        this.waveConfig = wave;
        this.waveTime = time;
        return this;
    }
    
    /**
     * Clears wave animation (no displacement).
     */
    public VertexEmitter noWave() {
        this.waveConfig = null;
        return this;
    }
    
    /**
     * Sets ColorContext for per-vertex color calculation.
     * When set, color is calculated per-vertex based on position and mode.
     * 
     * @param ctx Color context containing mode, colors, and direction
     * @return this emitter for chaining
     */
    public VertexEmitter colorContext(ColorContext ctx) {
        this.colorContext = ctx;
        this.cellIndex = 0;
        return this;
    }
    
    /**
     * Clears the color context (uses uniform color from color() instead).
     */
    public VertexEmitter noColorContext() {
        this.colorContext = null;
        return this;
    }
    
    /**
     * Sets travel effect configuration for directional alpha animation.
     * When set, vertex alpha is modulated based on position along the travel axis.
     * 
     * <p>For spherical shapes, use this method without bounds - it will use vertex
     * normals to compute position (works because normals point radially outward).</p>
     * 
     * @param config Travel effect configuration
     * @param phase Current animation phase (0-1, varies over time)
     * @return this emitter for chaining
     */
    public VertexEmitter travelEffect(TravelEffectConfig config, float phase) {
        this.travelEffect = config;
        this.travelPhase = phase;
        this.travelBoundsMin = null;
        this.travelBoundsMax = null;
        return this;
    }
    
    /**
     * Sets travel effect with explicit bounds for position-based t calculation.
     * 
     * <p>Use this method for non-spherical shapes (jets, cylinders, prisms, etc.)
     * where vertex normals don't point radially from center. The bounds define
     * the extents of the shape along each axis for proper t normalization.</p>
     * 
     * @param config Travel effect configuration
     * @param phase Current animation phase (0-1, varies over time)
     * @param boundsMin Minimum extents [minX, minY, minZ]
     * @param boundsMax Maximum extents [maxX, maxY, maxZ]
     * @return this emitter for chaining
     */
    public VertexEmitter travelEffect(TravelEffectConfig config, float phase, 
                                       float[] boundsMin, float[] boundsMax) {
        this.travelEffect = config;
        this.travelPhase = phase;
        this.travelBoundsMin = boundsMin;
        this.travelBoundsMax = boundsMax;
        this.shapeBaseMinAlpha = 0f;
        this.shapeTipMinAlpha = 0f;
        return this;
    }
    
    /**
     * Sets travel effect with bounds AND shape-level min alpha gradient.
     * 
     * <p>Use this for shapes like jets or beams that have a base-to-tip alpha
     * gradient where the travel effect should not go below the interpolated floor.</p>
     * 
     * @param config Travel effect configuration
     * @param phase Current animation phase (0-1)
     * @param boundsMin Minimum extents [minX, minY, minZ]
     * @param boundsMax Maximum extents [maxX, maxY, maxZ]
     * @param baseMinAlpha Minimum alpha at base (t=0) - travel can't go below this
     * @param tipMinAlpha Minimum alpha at tip (t=1) - travel can't go below this
     * @return this emitter for chaining
     */
    public VertexEmitter travelEffect(TravelEffectConfig config, float phase, 
                                       float[] boundsMin, float[] boundsMax,
                                       float baseMinAlpha, float tipMinAlpha) {
        this.travelEffect = config;
        this.travelPhase = phase;
        this.travelBoundsMin = boundsMin;
        this.travelBoundsMax = boundsMax;
        this.shapeBaseMinAlpha = baseMinAlpha;
        this.shapeTipMinAlpha = tipMinAlpha;
        return this;
    }
    
    /**
     * Clears travel effect (no directional alpha modulation).
     */
    public VertexEmitter noTravelEffect() {
        this.travelEffect = null;
        this.travelBoundsMin = null;
        this.travelBoundsMax = null;
        this.shapeBaseMinAlpha = 0f;
        this.shapeTipMinAlpha = 0f;
        return this;
    }
    
    // =========================================================================
    // Mesh emission
    // =========================================================================
    
    /**
     * Emits all vertices from a mesh.
     */
    public void emit(Mesh mesh) {
        if (mesh.isEmpty()) {
            return;
        }
        
        switch (mesh.primitiveType()) {
            case TRIANGLES -> emitTriangles(mesh);
            case QUADS -> emitQuads(mesh);
            case LINES -> emitLines(mesh);
            default -> throw new UnsupportedOperationException(
                "Unsupported primitive type: " + mesh.primitiveType());
        }
    }
    
    private void emitTriangles(Mesh mesh) {
        mesh.forEachTriangle((v0, v1, v2) -> {
            emitVertex(v0);
            emitVertex(v1);
            emitVertex(v2);
            cellIndex++; // Increment cell index for per-cell coloring
        });
    }
    
    private void emitQuads(Mesh mesh) {
        // Render quads as 2 triangles (Minecraft expects triangles)
        mesh.forEachQuad((v0, v1, v2, v3) -> {
            // First triangle: v0, v1, v2
            emitVertex(v0);
            emitVertex(v1);
            emitVertex(v2);
            // Second triangle: v0, v2, v3
            emitVertex(v0);
            emitVertex(v2);
            emitVertex(v3);
            cellIndex++; // Increment cell index for per-cell coloring
        });
    }
    
    private void emitLines(Mesh mesh) {
        mesh.forEachLine((v0, v1) -> {
            // Lines in Minecraft require special handling
            // Each line vertex needs position, color, and normal
            emitLineVertex(v0, v1);
            emitLineVertex(v1, v0);
        });
    }
    
    // =========================================================================
    // Single vertex emission
    // =========================================================================
    
    /**
     * Emits a single vertex with full transform and optional wave displacement.
     * 
     * <p>If wave animation is configured via {@link #wave(WaveConfig, float)},
     * the vertex position will be displaced before transformation.</p>
     */
    public void emitVertex(Vertex vertex) {
        float vx = vertex.x();
        float vy = vertex.y();
        float vz = vertex.z();
        
        // Apply wave displacement if configured
        if (waveConfig != null && waveConfig.isActive()) {
            float[] displaced = AnimationApplier.applyWaveToVertex(
                waveConfig, vx, vy, vz, waveTime);
            vx = displaced[0];
            vy = displaced[1];
            vz = displaced[2];
        }
        
        // Transform position
        Vector4f pos = new Vector4f(vx, vy, vz, 1.0f);
        pos.mul(positionMatrix);
        
        // Transform normal
        Vector3f normal = new Vector3f(vertex.nx(), vertex.ny(), vertex.nz());
        normal.mul(normalMatrix);
        
        // Calculate color - use ColorContext for per-vertex coloring, otherwise use uniform color
        int vertexColor;
        if (colorContext != null && colorContext.isPerVertex()) {
            // Per-vertex color calculation using local (pre-transform) coordinates
            vertexColor = colorContext.calculateColor(vertex.x(), vertex.y(), vertex.z(), cellIndex);
        } else {
            vertexColor = this.color;
        }
        
        // Apply travel effect to alpha if configured
        if (travelEffect != null && travelEffect.isActive()) {
            float t;
            
            if (travelBoundsMin != null && travelBoundsMax != null) {
                // Position-based t calculation (for jets, cylinders, etc.)
                // Use the LOCAL (pre-transform) vertex position
                t = TravelEffectComputer.computeT(
                    vertex.x(), vertex.y(), vertex.z(),
                    travelEffect, travelBoundsMin, travelBoundsMax);
            } else {
                // Normal-based t calculation (for spheres and centered shapes)
                // Works because normals point radially outward from center
                t = TravelEffectComputer.computeTForSphere(
                    vertex.nx(), vertex.ny(), vertex.nz(), travelEffect);
            }
            
            // Compute interpolated shape-level minAlpha floor
            // This is the minimum alpha at this vertex's position along the shape
            float shapeMinAlpha = shapeBaseMinAlpha + (shapeTipMinAlpha - shapeBaseMinAlpha) * t;
            
            // Get base alpha from color
            int baseAlpha = (vertexColor >> 24) & 0xFF;
            float baseAlphaF = baseAlpha / 255f;
            
            // Compute travel-modified alpha
            float modifiedAlpha = TravelEffectComputer.computeAlpha(
                baseAlphaF, t, travelEffect, travelPhase);
            
            // Apply the shape-level minAlpha floor
            // The final alpha cannot go below the interpolated shape minimum
            modifiedAlpha = Math.max(modifiedAlpha, shapeMinAlpha);
            
            // Replace alpha in vertexColor
            int newAlpha = (int)(modifiedAlpha * 255) & 0xFF;
            vertexColor = (vertexColor & 0x00FFFFFF) | (newAlpha << 24);
        }
        
        // Apply per-vertex alpha from tessellator (e.g., Kamehameha alpha gradient)
        // This multiplies with the existing alpha from color/travel effect
        float vertexAlpha = vertex.alpha();
        if (vertexAlpha < 1.0f) {
            int currentAlpha = (vertexColor >> 24) & 0xFF;
            int newAlpha = (int)(currentAlpha * vertexAlpha) & 0xFF;
            vertexColor = (vertexColor & 0x00FFFFFF) | (newAlpha << 24);
        }
        
        // Decompose color
        int a = (vertexColor >> 24) & 0xFF;
        int r = (vertexColor >> 16) & 0xFF;
        int g = (vertexColor >> 8) & 0xFF;
        int b = vertexColor & 0xFF;
        
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, a)
            .texture(vertex.u(), vertex.v())
            .overlay(overlay)
            .light(light)
            .normal(normal.x(), normal.y(), normal.z());
    }
    
    /**
     * Emits a line vertex (for LINES render layer).
     * <p>Supports ColorContext for per-vertex coloring, wave animation, and per-vertex alpha.
     */
    private void emitLineVertex(Vertex v, Vertex other) {
        float vx = v.x();
        float vy = v.y();
        float vz = v.z();
        
        // Apply wave displacement if configured
        if (waveConfig != null && waveConfig.isActive()) {
            float[] displaced = AnimationApplier.applyWaveToVertex(
                waveConfig, vx, vy, vz, waveTime);
            vx = displaced[0];
            vy = displaced[1];
            vz = displaced[2];
        }
        
        // Transform position
        Vector4f pos = new Vector4f(vx, vy, vz, 1.0f);
        pos.mul(positionMatrix);
        
        // For lines, normal is the direction
        Vector3f dir = new Vector3f(
            other.x() - v.x(),
            other.y() - v.y(),
            other.z() - v.z()
        );
        dir.normalize();
        dir.mul(normalMatrix);
        
        // Calculate color - use ColorContext for per-vertex coloring, otherwise use uniform color
        int vertexColor;
        if (colorContext != null && colorContext.isPerVertex()) {
            // Per-vertex color calculation using local (pre-transform) coordinates
            vertexColor = colorContext.calculateColor(v.x(), v.y(), v.z(), cellIndex);
        } else {
            vertexColor = this.color;
        }
        
        // Apply per-vertex alpha from vertex (for FADE edge transition)
        int baseAlpha = (vertexColor >> 24) & 0xFF;
        int finalAlpha = (int) (baseAlpha * v.alpha()) & 0xFF;
        
        int r = (vertexColor >> 16) & 0xFF;
        int g = (vertexColor >> 8) & 0xFF;
        int b = vertexColor & 0xFF;
        
        consumer.vertex(pos.x(), pos.y(), pos.z())
            .color(r, g, b, finalAlpha)
            .normal(dir.x(), dir.y(), dir.z());
    }
    
    // =========================================================================
    // Direct vertex emission (without Mesh)
    // =========================================================================
    
    /**
     * Emits a single vertex at the given position with current settings.
     * <p>Alpha defaults to 1.0 (fully opaque).
     */
    public void vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        emitVertex(new Vertex(x, y, z, nx, ny, nz, u, v, 1.0f));
    }
    
    /**
     * Emits a triangle from 3 vertices.
     */
    public void triangle(Vertex v0, Vertex v1, Vertex v2) {
        emitVertex(v0);
        emitVertex(v1);
        emitVertex(v2);
    }
    
    /**
     * Emits a quad from 4 vertices.
     */
    public void quad(Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        emitVertex(v0);
        emitVertex(v1);
        emitVertex(v2);
        emitVertex(v3);
    }
    
    // =========================================================================
    // Static utilities
    // =========================================================================
    
    /**
     * Creates an ARGB color from components.
     */
    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Creates an ARGB color from float components.
     */
    public static int argb(float a, float r, float g, float b) {
        return argb(
            (int) (a * 255) & 0xFF,
            (int) (r * 255) & 0xFF,
            (int) (g * 255) & 0xFF,
            (int) (b * 255) & 0xFF
        );
    }
    
    /**
     * Applies alpha to an existing color.
     */
    public static int withAlpha(int rgb, float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        return (rgb & 0x00FFFFFF) | (a << 24);
    }
    
    /**
     * Linearly interpolates between two colors.
     */
    public static int lerpColor(int start, int end, float t) {
        int sa = (start >> 24) & 0xFF;
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        
        int ea = (end >> 24) & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;
        
        int a = (int) (sa + (ea - sa) * t) & 0xFF;
        int r = (int) (sr + (er - sr) * t) & 0xFF;
        int g = (int) (sg + (eg - sg) * t) & 0xFF;
        int b = (int) (sb + (eb - sb) * t) & 0xFF;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // =========================================================================
    // Wireframe emission
    // =========================================================================
    
    /**
     * Emits a mesh as wireframe (edges only).
     * 
     * <p>Line thickness is handled by the render layer via RenderPhase.LineWidth.
     * This method just emits proper line vertices.</p>
     * 
     * @param mesh the mesh to render as wireframe
     * @param thickness line thickness (handled by render layer, passed for API compatibility)
     */
    public void emitWireframe(Mesh mesh, float thickness) {
        if (mesh.isEmpty()) {
            return;
        }
        
        PrimitiveType primType = mesh.primitiveType();
        
        if (primType == PrimitiveType.QUADS) {
            // For quads, draw only the 4 edges (not the internal diagonal)
            mesh.forEachQuad((v0, v1, v2, v3) -> {
                emitEdge(v0, v1);
                emitEdge(v1, v2);
                emitEdge(v2, v3);
                emitEdge(v3, v0);
            });
        } else {
            // For triangles, draw all 3 edges per triangle
            mesh.forEachTriangle((v0, v1, v2) -> {
                emitEdge(v0, v1);
                emitEdge(v1, v2);
                emitEdge(v2, v0);
            });
        }
    }
    
    /**
     * Emits a single edge (2 vertices = 1 line) with consistent normals.
     * Both vertices use the same normal direction (v0 toward v1).
     */
    private void emitEdge(Vertex v0, Vertex v1) {
        // Direction from v0 to v1 - CONSISTENT for both vertices
        float dx = v1.x() - v0.x();
        float dy = v1.y() - v0.y();
        float dz = v1.z() - v0.z();
        float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        
        // Transform normal through matrix
        Vector3f dir = new Vector3f(dx, dy, dz);
        dir.mul(normalMatrix);
        
        // Calculate color
        int vertexColor;
        if (colorContext != null && colorContext.isPerVertex()) {
            // Use midpoint for consistent color on both ends
            float midX = (v0.x() + v1.x()) * 0.5f;
            float midY = (v0.y() + v1.y()) * 0.5f;
            float midZ = (v0.z() + v1.z()) * 0.5f;
            vertexColor = colorContext.calculateColor(midX, midY, midZ, cellIndex);
        } else {
            vertexColor = this.color;
        }
        
        int a = (vertexColor >> 24) & 0xFF;
        int r = (vertexColor >> 16) & 0xFF;
        int g = (vertexColor >> 8) & 0xFF;
        int b = vertexColor & 0xFF;
        
        // Emit first vertex (v0)
        float x0 = v0.x(), y0 = v0.y(), z0 = v0.z();
        if (waveConfig != null && waveConfig.isActive()) {
            float[] displaced = AnimationApplier.applyWaveToVertex(waveConfig, x0, y0, z0, waveTime);
            x0 = displaced[0];
            y0 = displaced[1];
            z0 = displaced[2];
        }
        Vector4f pos0 = new Vector4f(x0, y0, z0, 1.0f);
        pos0.mul(positionMatrix);
        consumer.vertex(pos0.x(), pos0.y(), pos0.z())
            .color(r, g, b, a)
            .normal(dir.x(), dir.y(), dir.z());
        
        // Emit second vertex (v1)
        float x1 = v1.x(), y1 = v1.y(), z1 = v1.z();
        if (waveConfig != null && waveConfig.isActive()) {
            float[] displaced = AnimationApplier.applyWaveToVertex(waveConfig, x1, y1, z1, waveTime);
            x1 = displaced[0];
            y1 = displaced[1];
            z1 = displaced[2];
        }
        Vector4f pos1 = new Vector4f(x1, y1, z1, 1.0f);
        pos1.mul(positionMatrix);
        consumer.vertex(pos1.x(), pos1.y(), pos1.z())
            .color(r, g, b, a)
            .normal(dir.x(), dir.y(), dir.z());
    }
    
    
    /**
     * Static convenience method for emitting wireframe.
     */
    public static void emitWireframe(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Mesh mesh,
            int argb,
            float thickness,
            int light) {
        VertexEmitter emitter = new VertexEmitter(entry, consumer);
        emitter.color(argb).light(light).emitWireframe(mesh, thickness);
    }
    
    /**
     * Static wireframe emission per ARCHITECTURE.md signature.
     * 
     * <p>Per ARCHITECTURE.md (lines 290-297):
     * <pre>
     * VertexEmitter.emitWireframe(consumer, mesh, matrix, argb, thickness, light);
     * </pre>
     */
    public static void emitWireframe(
            VertexConsumer consumer,
            Mesh mesh,
            Matrix4f matrix,
            int argb,
            float thickness,
            int light) {
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // Handle different primitive types for proper wireframe
        PrimitiveType primType = mesh.primitiveType();
        
        if (primType == PrimitiveType.QUADS) {
            // For quads, draw only the 4 edges (not the internal diagonal)
            mesh.forEachQuad((v0, v1, v2, v3) -> {
                emitLineSegment(consumer, matrix, v0, v1, r, g, b, a);
                emitLineSegment(consumer, matrix, v1, v2, r, g, b, a);
                emitLineSegment(consumer, matrix, v2, v3, r, g, b, a);
                emitLineSegment(consumer, matrix, v3, v0, r, g, b, a);
            });
        } else {
            // For triangles, draw all 3 edges per triangle
            mesh.forEachTriangle((v0, v1, v2) -> {
                emitLineSegment(consumer, matrix, v0, v1, r, g, b, a);
                emitLineSegment(consumer, matrix, v1, v2, r, g, b, a);
                emitLineSegment(consumer, matrix, v2, v0, r, g, b, a);
            });
        }
    }
    
    private static void emitLineSegment(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vertex v0, Vertex v1,
            int r, int g, int b, int a) {
        
        // Direction for line normal
        float dx = v1.x() - v0.x();
        float dy = v1.y() - v0.y();
        float dz = v1.z() - v0.z();
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        
        consumer.vertex(matrix, v0.x(), v0.y(), v0.z())
            .color(r, g, b, a)
            .normal(dx, dy, dz);
        
        consumer.vertex(matrix, v1.x(), v1.y(), v1.z())
            .color(r, g, b, a)
            .normal(dx, dy, dz);
    }
    
    // =========================================================================
    // Static API (per ARCHITECTURE.md)
    // =========================================================================
    
    /**
     * Emits a quad from 4 corner positions.
     * 
     * <p>Per ARCHITECTURE.md specification:
     * <pre>
     * VertexEmitter.emitQuad(consumer, matrix, corners, argb, light);
     * </pre>
     * 
     * @param consumer the vertex consumer to emit to
     * @param matrix   transformation matrix
     * @param corners  4 corner positions [v0, v1, v2, v3]
     * @param argb     color as ARGB int
     * @param light    light level
     */
    public static void emitQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vector3f[] corners,
            int argb,
            int light) {
        
        if (corners == null || corners.length != 4) {
            throw new IllegalArgumentException("Quad requires exactly 4 corners");
        }
        
        // Decompose color
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // Calculate face normal from corners
        Vector3f edge1 = new Vector3f(corners[1]).sub(corners[0]);
        Vector3f edge2 = new Vector3f(corners[3]).sub(corners[0]);
        Vector3f normal = new Vector3f();
        edge1.cross(edge2, normal).normalize();
        
        // Emit 4 vertices
        for (int i = 0; i < 4; i++) {
            Vector4f pos = new Vector4f(corners[i].x, corners[i].y, corners[i].z, 1.0f);
            pos.mul(matrix);
            
            consumer.vertex(pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .texture(i == 0 || i == 3 ? 0 : 1, i < 2 ? 0 : 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal.x(), normal.y(), normal.z());
        }
    }
    
    /**
     * Emits an entire mesh with transformation and color.
     * 
     * <p>Per ARCHITECTURE.md specification:
     * <pre>
     * VertexEmitter.emitMesh(consumer, mesh, matrix, argb, light);
     * </pre>
     * 
     * @param consumer the vertex consumer to emit to
     * @param mesh     the mesh to emit
     * @param matrix   transformation matrix
     * @param argb     color as ARGB int
     * @param light    light level
     */
    public static void emitMesh(
            VertexConsumer consumer,
            Mesh mesh,
            Matrix4f matrix,
            int argb,
            int light) {
        
        if (mesh == null || mesh.isEmpty()) {
            return;
        }
        
        // Decompose color
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        
        // Create a simple normal matrix (assuming uniform scale)
        Matrix3f normalMatrix = new Matrix3f(matrix);
        
        // Consumer for each vertex
        java.util.function.Consumer<Vertex> emitVertex = (v) -> {
            // Transform position
            Vector4f pos = new Vector4f(v.x(), v.y(), v.z(), 1.0f);
            pos.mul(matrix);
            
            // Transform normal
            Vector3f normal = new Vector3f(v.nx(), v.ny(), v.nz());
            normal.mul(normalMatrix).normalize();
            
            consumer.vertex(pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .texture(v.u(), v.v())
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal.x(), normal.y(), normal.z());
        };
        
        // Emit based on primitive type
        switch (mesh.primitiveType()) {
            case TRIANGLES -> mesh.forEachTriangle((v0, v1, v2) -> {
                emitVertex.accept(v0);
                emitVertex.accept(v1);
                emitVertex.accept(v2);
            });
            case QUADS -> mesh.forEachQuad((v0, v1, v2, v3) -> {
                emitVertex.accept(v0);
                emitVertex.accept(v1);
                emitVertex.accept(v2);
                emitVertex.accept(v3);
            });
            default -> throw new UnsupportedOperationException(
                "Static emitMesh does not support: " + mesh.primitiveType());
        }
    }

}
