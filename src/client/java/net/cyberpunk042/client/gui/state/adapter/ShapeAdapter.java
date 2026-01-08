package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.shape.*;

import java.util.Set;

/**
 * Adapter for shape-related state.
 * 
 * <p>Handles all shape types (sphere, ring, prism, cylinder, 
 * polyhedron, torus, capsule, cone) plus the current shape type selector.</p>
 * 
 * <h3>Path Access</h3>
 * <ul>
 *   <li>{@code shapeType} - Current shape type string</li>
 *   <li>{@code sphere.radius}, {@code sphere.latSteps}, etc.</li>
 *   <li>{@code ring.innerRadius}, {@code ring.outerRadius}, etc.</li>
 *   <li>... similar for all other shapes</li>
 * </ul>
 */
@StateCategory("shape")
public class ShapeAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE RECORDS (one per shape type)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private SphereShape sphere = SphereShape.DEFAULT;
    @StateField private RingShape ring = RingShape.DEFAULT;
    @StateField private PrismShape prism = PrismShape.builder().build();
    @StateField private CylinderShape cylinder = CylinderShape.builder().build();
    @StateField private PolyhedronShape polyhedron = PolyhedronShape.DEFAULT;
    @StateField private TorusShape torus = TorusShape.DEFAULT;
    @StateField private CapsuleShape capsule = CapsuleShape.DEFAULT;
    @StateField private ConeShape cone = ConeShape.DEFAULT;
    @StateField private JetShape jet = JetShape.DEFAULT;
    @StateField private RaysShape rays = RaysShape.DEFAULT;
    @StateField private KamehamehaShape kamehameha = KamehamehaShape.DEFAULT;
    @StateField private MoleculeShape molecule = MoleculeShape.DEFAULT;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE SELECTOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    @StateField private String shapeType = "sphere";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMITIVE ADAPTER IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public String category() {
        return "shape";
    }
    
    @Override
    public void loadFrom(Primitive source) {
        if (source == null) return;
        
        // Load shape type
        this.shapeType = source.type() != null ? source.type() : "sphere";
        
        // Load shape into the correct field based on type
        Shape shape = source.shape();
        if (shape != null) {
            dispatchShape(shape);
        }
        
        Logging.GUI.topic("adapter").trace("ShapeAdapter loaded: type={}", shapeType);
    }
    
    private void dispatchShape(Shape shape) {
        switch (shape) {
            case SphereShape s -> this.sphere = s;
            case RingShape r -> this.ring = r;
            case PrismShape p -> this.prism = p;
            case CylinderShape c -> this.cylinder = c;
            case PolyhedronShape p -> this.polyhedron = p;
            case TorusShape t -> this.torus = t;
            case CapsuleShape c -> this.capsule = c;
            case ConeShape c -> this.cone = c;
            case JetShape j -> this.jet = j;
            case RaysShape r -> this.rays = r;
            case KamehamehaShape k -> this.kamehameha = k;
            case MoleculeShape m -> this.molecule = m;
            default -> Logging.GUI.topic("adapter").warn("Unknown shape type: {}", shape.getClass().getSimpleName());
        }
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        builder.type(shapeType);
        builder.shape(currentShape());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE ACCESS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get the currently selected shape based on shapeType.
     */
    public Shape currentShape() {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> sphere;
            case "ring" -> ring;
            case "prism" -> prism;
            case "cylinder" -> cylinder;
            case "polyhedron" -> polyhedron;
            case "cube" -> PolyhedronShape.subdivided(PolyType.CUBE, polyhedron.radius(), polyhedron.subdivisions());
            case "octahedron" -> PolyhedronShape.subdivided(PolyType.OCTAHEDRON, polyhedron.radius(), polyhedron.subdivisions());
            case "icosahedron" -> PolyhedronShape.subdivided(PolyType.ICOSAHEDRON, polyhedron.radius(), polyhedron.subdivisions());
            case "tetrahedron" -> PolyhedronShape.subdivided(PolyType.TETRAHEDRON, polyhedron.radius(), polyhedron.subdivisions());
            case "dodecahedron" -> PolyhedronShape.subdivided(PolyType.DODECAHEDRON, polyhedron.radius(), polyhedron.subdivisions());
            case "torus" -> torus;
            case "capsule" -> capsule;
            case "cone" -> cone;
            case "jet" -> jet;
            case "rays" -> rays;
            case "kamehameha" -> kamehameha;
            case "molecule" -> molecule;
            default -> {
                Logging.GUI.topic("adapter").warn("Unknown shapeType '{}', defaulting to sphere", shapeType);
                yield sphere;
            }
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TYPED ACCESSORS (for direct access without reflection)
    // ═══════════════════════════════════════════════════════════════════════════
    
    public String shapeType() { return shapeType; }
    public void setShapeType(String type) { this.shapeType = type; }
    
    public SphereShape sphere() { return sphere; }
    public void setSphere(SphereShape sphere) { this.sphere = sphere; }
    
    public RingShape ring() { return ring; }
    public void setRing(RingShape ring) { this.ring = ring; }
    
    public PrismShape prism() { return prism; }
    public void setPrism(PrismShape prism) { this.prism = prism; }
    
    public CylinderShape cylinder() { return cylinder; }
    public void setCylinder(CylinderShape cylinder) { this.cylinder = cylinder; }
    
    public PolyhedronShape polyhedron() { return polyhedron; }
    public void setPolyhedron(PolyhedronShape polyhedron) { this.polyhedron = polyhedron; }
    
    public TorusShape torus() { return torus; }
    public void setTorus(TorusShape torus) { this.torus = torus; }
    
    public CapsuleShape capsule() { return capsule; }
    public void setCapsule(CapsuleShape capsule) { this.capsule = capsule; }
    
    public ConeShape cone() { return cone; }
    public void setCone(ConeShape cone) { this.cone = cone; }
    
    public JetShape jet() { return jet; }
    public void setJet(JetShape jet) { this.jet = jet; }
    
    public RaysShape rays() { return rays; }
    public void setRays(RaysShape rays) { this.rays = rays; }
    
    public KamehamehaShape kamehameha() { return kamehameha; }
    public void setKamehameha(KamehamehaShape kamehameha) { this.kamehameha = kamehameha; }
    
    public MoleculeShape molecule() { return molecule; }
    public void setMolecule(MoleculeShape molecule) { this.molecule = molecule; }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Override
    public void reset() {
        this.sphere = SphereShape.DEFAULT;
        this.ring = RingShape.DEFAULT;
        this.prism = PrismShape.builder().build();
        this.cylinder = CylinderShape.builder().build();
        this.polyhedron = PolyhedronShape.DEFAULT;
        this.torus = TorusShape.DEFAULT;
        this.capsule = CapsuleShape.DEFAULT;
        this.cone = ConeShape.DEFAULT;
        this.jet = JetShape.DEFAULT;
        this.rays = RaysShape.DEFAULT;
        this.kamehameha = KamehamehaShape.DEFAULT;
        this.molecule = MoleculeShape.DEFAULT;
        this.shapeType = "sphere";
    }
}
