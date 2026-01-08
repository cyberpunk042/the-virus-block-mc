package net.cyberpunk042.client.field.render;

import net.cyberpunk042.client.gui.state.PipelineTracer;
import net.cyberpunk042.client.visual.mesh.Mesh;
import net.cyberpunk042.client.visual.mesh.MeshBuilder;
import net.cyberpunk042.client.visual.mesh.SphereTessellator;
import net.cyberpunk042.client.visual.mesh.Vertex;
import net.cyberpunk042.client.visual.mesh.GeometryMath;
import net.cyberpunk042.client.visual.render.VertexEmitter;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.fill.SphereCageOptions;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.pattern.VertexPattern;
import net.cyberpunk042.visual.color.ColorResolver;
import net.cyberpunk042.visual.shape.SphereAlgorithm;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.visibility.VisibilityMask;
import net.cyberpunk042.log.Logging;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders sphere primitives.
 * 
 * <p>Supports multiple rendering algorithms via {@link SphereAlgorithm}:</p>
 * <ul>
 *   <li><b>LAT_LON, UV_SPHERE, ICO_SPHERE</b>: Mesh-based tessellation</li>
 *   <li><b>TYPE_A</b>: Direct rendering with overlapping cubes (accurate)</li>
 *   <li><b>TYPE_E</b>: Direct rendering with rotated rectangles (efficient)</li>
 * </ul>
 * 
 * <p>TYPE_A and TYPE_E use depth testing magic - overlapping geometry creates
 * a smooth sphere silhouette from any viewing angle.</p>
 * 
 * @see SphereShape
 * @see SphereAlgorithm
 * @see SphereTessellator
 * @see SphereCageOptions
 */
public final class SphereRenderer extends AbstractPrimitiveRenderer {
    
    // Default parameters for TYPE_A/TYPE_E rendering
    private static final int DEFAULT_VERTICAL_LAYERS = 8;
    private static final int DEFAULT_HORIZONTAL_DETAIL = 6;
    
    @Override
    public String shapeType() {
        return "sphere";
    }
    
    /**
     * Override render to handle TYPE_A/TYPE_E direct rendering.
     * 
     * <p>TYPE_A and TYPE_E algorithms use direct vertex emission (overlapping cubes
     * or rotated rectangles) rather than mesh tessellation, so we intercept the
     * render call and use the specialized rendering methods.</p>
     */
    @Override
    public void render(
            Primitive primitive,
            MatrixStack matrices,
            VertexConsumer consumer,
            int light,
            float time,
            ColorResolver resolver,
            RenderOverrides overrides) {
        
        if (!(primitive.shape() instanceof SphereShape shape)) {
            // Fall back to normal rendering for non-sphere shapes
            super.render(primitive, matrices, consumer, light, time, resolver, overrides);
            return;
        }
        
        // Check if this algorithm requires direct rendering
        if (requiresDirectRendering(shape)) {
            Logging.FIELD.topic("render").trace("[SPHERE] Using direct rendering for algorithm: {}", 
                shape.algorithm());
            
            // Resolve color
            int color = resolveColor(primitive, resolver, overrides, time);
            
            // Use direct rendering methods
            if (shape.algorithm() == SphereAlgorithm.TYPE_A) {
                renderTypeA(matrices, consumer, shape.radius(), color, light);
            } else if (shape.algorithm() == SphereAlgorithm.TYPE_E) {
                renderTypeE(matrices, consumer, shape.radius(), color, light);
            }
            return;
        }
        
        // Normal mesh-based rendering for LAT_LON, UV_SPHERE, ICO_SPHERE
        super.render(primitive, matrices, consumer, light, time, resolver, overrides);
    }
    
    @Override
    protected Mesh tessellate(Primitive primitive, net.cyberpunk042.visual.animation.WaveConfig wave, float time) {
        if (!(primitive.shape() instanceof SphereShape shape)) {
            Logging.FIELD.topic("render").warn("[SPHERE] Shape is NOT SphereShape: {}", 
                primitive.shape() != null ? primitive.shape().getClass().getSimpleName() : "null");
            return null;
        }
        
        Logging.FIELD.topic("render").trace("[SPHERE] Tessellating: radius={}, algo={}, wave={}", 
            shape.radius(), shape.algorithm(), wave != null && wave.isActive() ? "active" : "none");
        
        // Get pattern from arrangement config with CellType validation
        VertexPattern pattern = null;
        ArrangementConfig arrangement = primitive.arrangement();
        if (arrangement != null) {
            // Validate pattern is compatible with sphere's QUAD cells
            Logging.FIELD.topic("render").trace("[SPHERE] Arrangement: {}, resolving pattern for CellType={}", 
                arrangement, shape.primaryCellType());
            pattern = arrangement.resolvePattern("main", shape.primaryCellType());
            if (pattern == null) {
                Logging.FIELD.topic("render").warn("[SPHERE] Pattern validation FAILED! Using null pattern (skip)");
                // Don't return null - try without pattern
                // return null;
            }
        } else {
            Logging.FIELD.topic("render").trace("[SPHERE] No arrangement config, using null pattern");
        }
        
        // Get visibility mask
        VisibilityMask visibility = primitive.visibility();
        
        // CP5: Visibility/Mask values at tessellation
        if (visibility != null) {
            PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 5, "tessellate", visibility.mask().name());
            PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 5, "tessellate", String.valueOf(visibility.count()));
            PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 5, "tessellate", String.valueOf(visibility.animate()));
        }
        
        // Tessellate with full config including wave deformation
        Mesh mesh = SphereTessellator.tessellate(shape, pattern, visibility, wave, time);
        if (mesh == null || mesh.isEmpty()) {
            Logging.FIELD.topic("render").warn("[SPHERE] Tessellation returned empty mesh!");
        } else {
            Logging.FIELD.topic("render").debug("[SPHERE] Mesh: {} vertices", mesh.vertices().size());
            // CP6-CP7: Visibility applied to mesh
            PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 6, "mesh", "applied");
            PipelineTracer.trace(PipelineTracer.V1_MASK_TYPE, 7, "vertices", String.valueOf(mesh.vertexCount()));
            PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 6, "mesh", "applied");
            PipelineTracer.trace(PipelineTracer.V2_MASK_COUNT, 7, "vertices", String.valueOf(mesh.vertexCount()));
            PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 6, "mesh", "applied");
            PipelineTracer.trace(PipelineTracer.V5_MASK_ANIMATE, 7, "vertices", String.valueOf(mesh.vertexCount()));
        }
        return mesh;
    }
    
    /**
     * Emits sphere as cage (latitude/longitude grid lines) with optional wave animation.
     * 
     * <p>Uses {@link SphereCageOptions} for configuration:
     * <ul>
     *   <li>latitudeCount - Number of horizontal rings</li>
     *   <li>longitudeCount - Number of vertical meridians</li>
     *   <li>showEquator - Highlight the equator line</li>
     *   <li>showPoles - Add small pole markers</li>
     * </ul>
     * 
     * <p>Uses {@link MathHelper#sin} and {@link MathHelper#cos} for fast
     * lookup-table based trigonometry.</p>
     */
    @Override
    protected void emitCage(
            MatrixStack matrices,
            VertexConsumer consumer,
            Mesh mesh,
            int color,
            int light,
            FillConfig fill,
            Primitive primitive,
            net.cyberpunk042.visual.animation.WaveConfig waveConfig,
            float time) {
        
        if (!(primitive.shape() instanceof SphereShape shape)) {
            // Fallback to wireframe if not a sphere
            emitWireframe(matrices, consumer, mesh, color, light, fill, waveConfig, time);
            return;
        }
        
        // Get cage options or defaults
        SphereCageOptions cageOptions = fill != null ? fill.sphereCage() : null;
        if (cageOptions == null) {
            cageOptions = SphereCageOptions.DEFAULT;
        }
        
        float radius = shape.radius();
        int latCount = cageOptions.latitudeCount();
        int lonCount = cageOptions.longitudeCount();
        float thickness = fill != null ? fill.wireThickness() : 1.0f;
        
        // Check if CPU wave deformation should be applied
        boolean applyWave = waveConfig != null && waveConfig.isActive() && waveConfig.isCpuMode();
        
        // Build cage mesh with lines
        MeshBuilder builder = MeshBuilder.lines();
        
        // === LATITUDE LINES (horizontal rings) ===
        for (int lat = 1; lat < latCount; lat++) {
            float theta = GeometryMath.PI * lat / latCount;  // 0 to PI
            float ringRadius = MathHelper.sin(theta) * radius;
            float y = MathHelper.cos(theta) * radius;
            
            // Draw ring as line segments
            int segments = Math.max(lonCount * 2, 16);
            for (int i = 0; i < segments; i++) {
                float phi1 = GeometryMath.TWO_PI * i / segments;
                float phi2 = GeometryMath.TWO_PI * (i + 1) / segments;
                
                float x1 = MathHelper.cos(phi1) * ringRadius;
                float z1 = MathHelper.sin(phi1) * ringRadius;
                float x2 = MathHelper.cos(phi2) * ringRadius;
                float z2 = MathHelper.sin(phi2) * ringRadius;
                
                // Create vertices and apply wave deformation if active
                Vertex v1 = Vertex.pos(x1, y, z1);
                Vertex v2 = Vertex.pos(x2, y, z2);
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                int idx1 = builder.addVertex(v1);
                int idx2 = builder.addVertex(v2);
                builder.line(idx1, idx2);
            }
        }
        
        // === LONGITUDE LINES (vertical meridians) ===
        for (int lon = 0; lon < lonCount; lon++) {
            float phi = GeometryMath.TWO_PI * lon / lonCount;
            
            // Draw meridian as line segments from pole to pole
            int segments = Math.max(latCount * 2, 16);
            for (int i = 0; i < segments; i++) {
                float theta1 = GeometryMath.PI * i / segments;
                float theta2 = GeometryMath.PI * (i + 1) / segments;
                
                // Spherical to cartesian
                float x1 = MathHelper.sin(theta1) * MathHelper.cos(phi) * radius;
                float y1 = MathHelper.cos(theta1) * radius;
                float z1 = MathHelper.sin(theta1) * MathHelper.sin(phi) * radius;
                
                float x2 = MathHelper.sin(theta2) * MathHelper.cos(phi) * radius;
                float y2 = MathHelper.cos(theta2) * radius;
                float z2 = MathHelper.sin(theta2) * MathHelper.sin(phi) * radius;
                
                // Create vertices and apply wave deformation if active
                Vertex v1 = Vertex.pos(x1, y1, z1);
                Vertex v2 = Vertex.pos(x2, y2, z2);
                if (applyWave) {
                    v1 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v1, waveConfig, time);
                    v2 = net.cyberpunk042.client.visual.animation.WaveDeformer.applyToVertex(v2, waveConfig, time);
                }
                
                int idx1 = builder.addVertex(v1);
                int idx2 = builder.addVertex(v2);
                builder.line(idx1, idx2);
            }
        }
        
        // Emit the cage mesh
        Mesh cageMesh = builder.build();
        VertexEmitter emitter = new VertexEmitter(matrices, consumer);
        emitter.color(color).light(light);
        emitter.emit(cageMesh);
    }
    
    // =========================================================================
    // TYPE_A Direct Rendering (Overlapping Cubes)
    // =========================================================================
    
    /**
     * Renders a sphere using TYPE_A algorithm - overlapping axis-aligned cubes.
     * 
     * <p>Creates nested concentric cubes at each vertical layer. Different aspect
     * ratios per layer create smooth appearance when rendered with depth testing.</p>
     * 
     * <p>Best for: static models, close-up viewing, accurate silhouette.</p>
     * 
     * @param matrices MatrixStack for transformation
     * @param consumer Vertex consumer
     * @param radius Sphere radius
     * @param color ARGB color
     * @param light Light level
     */
    public static void renderTypeA(MatrixStack matrices, VertexConsumer consumer,
                                    float radius, int color, int light) {
        renderTypeA(matrices, consumer, radius, DEFAULT_VERTICAL_LAYERS, 
                    DEFAULT_HORIZONTAL_DETAIL, color, light);
    }
    
    /**
     * Renders TYPE_A sphere with custom parameters.
     * 
     * @param matrices MatrixStack for transformation
     * @param consumer Vertex consumer
     * @param radius Sphere radius
     * @param verticalLayers Number of vertical layers (4-10 recommended)
     * @param horizontalDetail Horizontal detail per layer (2-6 recommended)
     * @param color ARGB color
     * @param light Light level
     */
    public static void renderTypeA(MatrixStack matrices, VertexConsumer consumer,
                                    float radius, int verticalLayers, int horizontalDetail,
                                    int color, int light) {
        float vertStep = 90.0f / (verticalLayers + 1);
        
        for (int layer = 1; layer <= verticalLayers; layer++) {
            float levelDeg = vertStep * layer;
            float levelRad = (float) Math.toRadians(levelDeg);
            
            float layerRadius = radius * MathHelper.cos(levelRad);
            float y = radius * MathHelper.sin(levelRad);
            
            // Adaptive detail (fewer cubes at poles) - optimization
            int detail = Math.max(1, Math.round(horizontalDetail * layerRadius / radius));
            float horizStep = 90.0f / detail;
            
            for (int h = 1; h <= detail; h++) {
                float deg = horizStep * h;
                float rad = (float) Math.toRadians(deg);
                
                float cx = layerRadius * MathHelper.cos(rad);
                float cz = layerRadius * MathHelper.sin(rad);
                
                // Render cube from (-cx, -y, -cz) to (cx, y, cz)
                renderCube(matrices, consumer, -cx, -y, -cz, cx, y, cz, color, light);
            }
        }
    }
    
    // =========================================================================
    // TYPE_E Direct Rendering (Rotated Rectangles)
    // =========================================================================
    
    /**
     * Renders a sphere using TYPE_E algorithm - rotated thin rectangles.
     * 
     * <p>Creates rotated rectangles forming 16-sided (hexadecagon) cross-sections
     * at each vertical layer. Much fewer elements than TYPE_A.</p>
     * 
     * <p>Best for: distant objects, animated spheres, performance.</p>
     * 
     * @param matrices MatrixStack for transformation
     * @param consumer Vertex consumer
     * @param radius Sphere radius
     * @param color ARGB color
     * @param light Light level
     */
    public static void renderTypeE(MatrixStack matrices, VertexConsumer consumer,
                                    float radius, int color, int light) {
        renderTypeE(matrices, consumer, radius, DEFAULT_VERTICAL_LAYERS, color, light);
    }
    
    /**
     * Renders TYPE_E sphere with custom vertical layers.
     * 
     * @param matrices MatrixStack for transformation
     * @param consumer Vertex consumer
     * @param radius Sphere radius
     * @param verticalLayers Number of vertical layers (4-10 recommended)
     * @param color ARGB color
     * @param light Light level
     */
    public static void renderTypeE(MatrixStack matrices, VertexConsumer consumer,
                                    float radius, int verticalLayers, int color, int light) {
        float vertStep = 90.0f / (verticalLayers + 1);
        float sideRatio = (float) Math.tan(Math.PI / 16.0);  // Hexadecagon
        
        for (int layer = 1; layer <= verticalLayers; layer++) {
            float levelDeg = vertStep * layer;
            float levelRad = (float) Math.toRadians(levelDeg);
            
            float layerRadius = radius * MathHelper.cos(levelRad);
            float y = radius * MathHelper.sin(levelRad);
            float side = layerRadius * sideRatio;
            
            // Z-aligned rectangles at 5 rotations (-45° to 45° in 22.5° steps)
            for (float angle = -45f; angle < 67.5f; angle += 22.5f) {
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                renderCube(matrices, consumer, -side, -y, -layerRadius, side, y, layerRadius, color, light);
                matrices.pop();
            }
            
            // X-aligned rectangles at 3 rotations
            for (float angle = -22.5f; angle < 45f; angle += 22.5f) {
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                renderCube(matrices, consumer, -layerRadius, -y, -side, layerRadius, y, side, color, light);
                matrices.pop();
            }
        }
    }
    
    // =========================================================================
    // Cube Rendering (shared by TYPE_A and TYPE_E)
    // =========================================================================
    
    /**
     * Renders an axis-aligned cube/box.
     */
    private static void renderCube(MatrixStack matrices, VertexConsumer consumer,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    int color, int light) {
        Matrix4f pos = matrices.peek().getPositionMatrix();
        Matrix3f norm = matrices.peek().getNormalMatrix();
        
        // Extract color components
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        
        // 6 faces (each as a quad = 4 vertices)
        // -Z face (front)
        quad(consumer, pos, norm, 
             x2, y1, z1,  x1, y1, z1,  x1, y2, z1,  x2, y2, z1,
             0, 0, -1, r, g, b, a, light);
        
        // +Z face (back)
        quad(consumer, pos, norm,
             x1, y1, z2,  x2, y1, z2,  x2, y2, z2,  x1, y2, z2,
             0, 0, 1, r, g, b, a, light);
        
        // -X face (left)
        quad(consumer, pos, norm,
             x1, y1, z1,  x1, y1, z2,  x1, y2, z2,  x1, y2, z1,
             -1, 0, 0, r, g, b, a, light);
        
        // +X face (right)
        quad(consumer, pos, norm,
             x2, y1, z2,  x2, y1, z1,  x2, y2, z1,  x2, y2, z2,
             1, 0, 0, r, g, b, a, light);
        
        // -Y face (bottom)
        quad(consumer, pos, norm,
             x1, y1, z1,  x2, y1, z1,  x2, y1, z2,  x1, y1, z2,
             0, -1, 0, r, g, b, a, light);
        
        // +Y face (top)
        quad(consumer, pos, norm,
             x1, y2, z2,  x2, y2, z2,  x2, y2, z1,  x1, y2, z1,
             0, 1, 0, r, g, b, a, light);
    }
    
    /**
     * Emits a quad (4 vertices).
     */
    private static void quad(VertexConsumer consumer, Matrix4f pos, Matrix3f norm,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4,
                              float nx, float ny, float nz,
                              float r, float g, float b, float a,
                              int light) {
        vertex(consumer, pos, norm, x1, y1, z1, nx, ny, nz, 0, 1, r, g, b, a, light);
        vertex(consumer, pos, norm, x2, y2, z2, nx, ny, nz, 1, 1, r, g, b, a, light);
        vertex(consumer, pos, norm, x3, y3, z3, nx, ny, nz, 1, 0, r, g, b, a, light);
        vertex(consumer, pos, norm, x4, y4, z4, nx, ny, nz, 0, 0, r, g, b, a, light);
    }
    
    /**
     * Emits a single vertex.
     */
    private static void vertex(VertexConsumer consumer, Matrix4f pos, Matrix3f norm,
                                float x, float y, float z,
                                float nx, float ny, float nz,
                                float u, float v,
                                float r, float g, float b, float a,
                                int light) {
        consumer.vertex(pos, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(0)  // No overlay
                .light(light)
                .normal(nx, ny, nz);  // 1.21.6 API: just floats, no matrix
    }
    
    // =========================================================================
    // Algorithm Detection
    // =========================================================================
    
    /**
     * Checks if a sphere shape requires direct rendering (TYPE_A/TYPE_E).
     */
    public static boolean requiresDirectRendering(SphereShape shape) {
        SphereAlgorithm algo = shape.algorithm();
        return algo == SphereAlgorithm.TYPE_A || algo == SphereAlgorithm.TYPE_E;
    }
}
