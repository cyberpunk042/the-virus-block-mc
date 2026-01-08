package net.cyberpunk042.client.gui.shape;

import net.cyberpunk042.visual.shape.SphereAlgorithm;

import java.util.List;

/**
 * Data-driven widget specifications for shape controls.
 * 
 * <p>Each shape type declares its UI controls as a list of specs.
 * The {@link ShapeControlBuilder} converts these specs into actual widgets.</p>
 * 
 * <h3>Design Pattern: Specification Pattern + Factory</h3>
 * <ul>
 *   <li>Specs are pure data (immutable records)</li>
 *   <li>Builder interprets specs to create widgets</li>
 *   <li>Adding new shapes requires only adding specs, not code</li>
 * </ul>
 */
public final class ShapeWidgetSpec {
    
    private ShapeWidgetSpec() {} // Static utility class
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET SPECIFICATION RECORDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Specification for a labeled slider widget.
     * 
     * @param label Display label (e.g., "Radius", "Height")
     * @param stateKey State key for read/write (e.g., "sphere.radius")
     * @param min Minimum value
     * @param max Maximum value
     * @param format Display format (e.g., "%.2f", "%d", "%.0f°")
     * @param step Optional snap step (null for continuous, 1 for integers)
     * @param halfWidth If true, widget takes half the available width (for pairing)
     */
    public record SliderSpec(
        String label,
        String stateKey,
        float min,
        float max,
        String format,
        Float step,
        boolean halfWidth
    ) {
        /** Full-width slider factory */
        public static SliderSpec full(String label, String stateKey, float min, float max, String format) {
            return new SliderSpec(label, stateKey, min, max, format, null, false);
        }
        
        /** Full-width integer slider factory */
        public static SliderSpec fullInt(String label, String stateKey, int min, int max) {
            return new SliderSpec(label, stateKey, min, max, "%d", 1f, false);
        }
        
        /** Half-width slider factory (for pairing two sliders on one row) */
        public static SliderSpec half(String label, String stateKey, float min, float max, String format) {
            return new SliderSpec(label, stateKey, min, max, format, null, true);
        }
        
        /** Half-width integer slider factory */
        public static SliderSpec halfInt(String label, String stateKey, int min, int max) {
            return new SliderSpec(label, stateKey, min, max, "%d", 1f, true);
        }
        
        /** Half-width degree slider (0-360) */
        public static SliderSpec halfDegree(String label, String stateKey) {
            return new SliderSpec(label, stateKey, 0f, 360f, "%.0f°", null, true);
        }
        
        /** Half-width signed degree slider (-360 to 360) */
        public static SliderSpec halfDegreeSigned(String label, String stateKey) {
            return new SliderSpec(label, stateKey, -360f, 360f, "%.0f°", null, true);
        }
    }
    
    /**
     * Specification for a checkbox widget.
     * 
     * @param label Display label
     * @param stateKey State key for boolean value
     * @param tooltip Tooltip text
     * @param halfWidth If true, widget takes half width
     */
    public record CheckboxSpec(
        String label,
        String stateKey,
        String tooltip,
        boolean halfWidth
    ) {
        public static CheckboxSpec half(String label, String stateKey, String tooltip) {
            return new CheckboxSpec(label, stateKey, tooltip, true);
        }
        
        public static CheckboxSpec full(String label, String stateKey, String tooltip) {
            return new CheckboxSpec(label, stateKey, tooltip, false);
        }
    }
    
    /**
     * Specification for an enum dropdown widget.
     * 
     * @param <E> Enum type
     * @param label Display label
     * @param stateKey State key
     * @param enumClass The enum class
     * @param defaultValue Default enum value
     * @param halfWidth If true, takes half width
     */
    public record EnumDropdownSpec<E extends Enum<E>>(
        String label,
        String stateKey,
        Class<E> enumClass,
        E defaultValue,
        boolean halfWidth
    ) {
        public static <E extends Enum<E>> EnumDropdownSpec<E> half(
                String label, String stateKey, Class<E> enumClass, E defaultValue) {
            return new EnumDropdownSpec<>(label, stateKey, enumClass, defaultValue, true);
        }
        
        public static <E extends Enum<E>> EnumDropdownSpec<E> full(
                String label, String stateKey, Class<E> enumClass, E defaultValue) {
            return new EnumDropdownSpec<>(label, stateKey, enumClass, defaultValue, false);
        }
    }
    
    /**
     * Marker for ending a row (forces next widget to new line).
     * Use this when you have an odd number of half-width widgets.
     */
    public record RowBreak() {}
    
    /**
     * Specification for a section label/header.
     */
    public record SectionHeader(String text) {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHAPE SPECIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns widget specs for the given shape type.
     * 
     * @param shapeType Shape type id (e.g., "sphere", "cylinder")
     * @return List of widget specifications in display order
     */
    public static List<Object> forShape(String shapeType) {
        return switch (shapeType.toLowerCase()) {
            case "sphere" -> SPHERE_SPECS;
            case "ring" -> RING_SPECS;
            case "disc" -> DISC_SPECS;
            case "prism" -> PRISM_SPECS;
            case "cylinder", "beam" -> CYLINDER_SPECS;
            case "cube", "tetrahedron", "octahedron", "dodecahedron", "icosahedron" -> POLYHEDRON_SPECS;
            case "torus" -> TORUS_SPECS;
            case "capsule" -> CAPSULE_SPECS;
            case "cone" -> CONE_SPECS;
            case "jet" -> JET_SPECS;
            case "rays" -> RAYS_SPECS;
            case "kamehameha" -> KAMEHAMEHA_SPECS;
            case "molecule" -> MOLECULE_SPECS;
            default -> List.of();
        };
    }
    
    // SPHERE
    // ───────────────────────────────────────────────────────────────────────────
    
    /**
     * SPHERE base specs - geometry only.
     * Deformation, Horizon, and Corona sections are added CONDITIONALLY
     * in ShapeSubPanel based on current state values.
     */
    private static final List<Object> SPHERE_SPECS = List.of(
        // Row 1: Radius + Algorithm
        SliderSpec.half("Radius", "sphere.radius", 0.1f, 20.0f, "%.2f"),
        EnumDropdownSpec.half("Algo", "sphere.algorithm", SphereAlgorithm.class, SphereAlgorithm.LAT_LON),
        
        // Row 2: Lat + Lon steps
        SliderSpec.halfInt("Lat", "sphere.latSteps", 4, 512),
        SliderSpec.halfInt("Lon", "sphere.lonSteps", 4, 512),
        
        // Row 3: Lat Start + End (fraction 0-1)
        SliderSpec.half("Start", "sphere.latStart", 0f, 1f, "%.2f"),
        SliderSpec.half("End", "sphere.latEnd", 0f, 1f, "%.2f")
        
        // NOTE: The following sections are added CONDITIONALLY in ShapeSubPanel:
        // - Deformation section (Deform dropdown + type-specific sliders)
        // - Planet Terrain section (only when deformation == PLANET)
        // - Horizon Effect section (Enable checkbox + conditional sliders)
        // - Corona Effect section (Enable checkbox + conditional sliders)
        // - QuadPattern dropdown (for all sphere types)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // RING
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> RING_SPECS = List.of(
        // Row 1: Inner R + Outer R
        SliderSpec.half("Inner R", "ring.innerRadius", 0.0f, 10.0f, "%.2f"),
        SliderSpec.half("Outer R", "ring.outerRadius", 0.0f, 20.0f, "%.2f"),
        
        // Row 2: Segments + Height
        SliderSpec.halfInt("Segs", "ring.segments", 3, 512),
        SliderSpec.half("Height", "ring.height", 0f, 10f, "%.2f"),
        
        // Row 3: Y Pos + Taper
        SliderSpec.half("Y Pos", "ring.y", -5f, 5f, "%.2f"),
        SliderSpec.half("Taper", "ring.taper", 0f, 2f, "%.2f"),
        
        // Row 4: Twist + (break)
        SliderSpec.halfDegreeSigned("Twist", "ring.twist"),
        new RowBreak(),
        
        // Row 5: Arc Start + End
        SliderSpec.halfDegree("Arc St", "ring.arcStart"),
        SliderSpec.halfDegree("Arc End", "ring.arcEnd")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // DISC
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> DISC_SPECS = List.of(
        // Row 1: Radius + Inner Radius
        SliderSpec.half("Radius", "disc.radius", 0.0f, 20.0f, "%.2f"),
        SliderSpec.half("Inner R", "disc.innerRadius", 0f, 10f, "%.2f"),
        
        // Row 2: Segments + Rings
        SliderSpec.halfInt("Segs", "disc.segments", 3, 512),
        SliderSpec.halfInt("Rings", "disc.rings", 1, 100),
        
        // Row 3: Y Position (full width)
        SliderSpec.full("Y Pos", "disc.y", -5f, 5f, "%.2f"),
        
        // Row 4: Arc Start + End
        SliderSpec.halfDegree("Arc St", "disc.arcStart"),
        SliderSpec.halfDegree("Arc End", "disc.arcEnd")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // PRISM
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> PRISM_SPECS = List.of(
        // Row 1: Sides + Radius
        SliderSpec.halfInt("Sides", "prism.sides", 3, 64),
        SliderSpec.half("Radius", "prism.radius", 0.1f, 20.0f, "%.2f"),
        
        // Row 2: Height + Top Radius
        SliderSpec.half("Height", "prism.height", 0.1f, 20.0f, "%.2f"),
        SliderSpec.half("Top R", "prism.topRadius", 0.0f, 20.0f, "%.2f"),
        
        // Row 3: Twist + Height Segments
        SliderSpec.halfDegreeSigned("Twist", "prism.twist"),
        SliderSpec.halfInt("H Segs", "prism.heightSegments", 1, 100),
        
        // Row 4: Cap Top + Cap Bottom
        CheckboxSpec.half("Cap Top", "prism.capTop", "Render top cap"),
        CheckboxSpec.half("Cap Bottom", "prism.capBottom", "Render bottom cap")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // CYLINDER
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> CYLINDER_SPECS = List.of(
        // Row 1: Radius + Top Radius
        SliderSpec.half("Radius", "cylinder.radius", 0.1f, 20.0f, "%.2f"),
        SliderSpec.half("Top R", "cylinder.topRadius", 0f, 20f, "%.2f"),
        
        // Row 2: Height + Segments
        SliderSpec.half("Height", "cylinder.height", 0.1f, 256f, "%.2f"),
        SliderSpec.halfInt("Segs", "cylinder.segments", 3, 256),
        
        // Row 3: Arc + Height Segments
        SliderSpec.halfDegree("Arc", "cylinder.arc"),
        SliderSpec.halfInt("H Segs", "cylinder.heightSegments", 1, 100),
        
        // Row 4: Cap Top + Cap Bottom
        CheckboxSpec.half("Cap Top", "cylinder.capTop", "Render top cap"),
        CheckboxSpec.half("Cap Bot", "cylinder.capBottom", "Render bottom cap")
        
        // Note: "Open Ended" checkbox is special logic, handled in ShapeSubPanel
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // POLYHEDRON (Shared by cube, tetrahedron, octahedron, dodecahedron, icosahedron)
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> POLYHEDRON_SPECS = List.of(
        // Full width size
        SliderSpec.full("Size", "polyhedron.radius", 0.5f, 10f, "%.1f"),
        
        // Full width subdivisions
        SliderSpec.fullInt("Subdivisions", "polyhedron.subdivisions", 0, 4)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // TORUS
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> TORUS_SPECS = List.of(
        SliderSpec.full("Ring Radius", "torus.majorRadius", 0.5f, 5f, "%.2f"),
        SliderSpec.full("Tube Radius", "torus.minorRadius", 0.1f, 2f, "%.2f"),
        new SliderSpec("Ring Segments", "torus.majorSegments", 8, 64, "%d", 4f, false),
        new SliderSpec("Tube Segments", "torus.minorSegments", 4, 32, "%d", 2f, false)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // CAPSULE
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> CAPSULE_SPECS = List.of(
        SliderSpec.full("Radius", "capsule.radius", 0.1f, 3f, "%.2f"),
        SliderSpec.full("Height", "capsule.height", 0.5f, 10f, "%.1f"),
        new SliderSpec("Segments", "capsule.segments", 8, 64, "%d", 4f, false)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // CONE
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> CONE_SPECS = List.of(
        SliderSpec.full("Bottom Radius", "cone.bottomRadius", 0.1f, 5f, "%.2f"),
        SliderSpec.full("Top Radius", "cone.topRadius", 0f, 5f, "%.2f"),
        SliderSpec.full("Height", "cone.height", 0.5f, 10f, "%.1f"),
        new SliderSpec("Segments", "cone.segments", 8, 64, "%d", 4f, false)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // JET (Relativistic Jets)
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> JET_SPECS = List.of(
        // === GEOMETRY ===
        new SectionHeader("Geometry"),
        
        // Row 1: Length + Base Radius
        SliderSpec.half("Length", "jet.length", 0.1f, 10f, "%.2f"),
        SliderSpec.half("Base R", "jet.baseRadius", 0.05f, 3f, "%.2f"),
        
        // Row 2: Segments + Length Segments
        SliderSpec.halfInt("Segs", "jet.segments", 4, 64),
        SliderSpec.halfInt("Len Segs", "jet.lengthSegments", 1, 32),
        
        // === TIP RADII ===
        new SectionHeader("Tip Radii"),
        
        // Row 3: Top Tip Radius + Bottom Tip Radius
        SliderSpec.half("Top Tip R", "jet.topTipRadius", 0f, 3f, "%.2f"),
        SliderSpec.half("Bot Tip R", "jet.bottomTipRadius", 0f, 3f, "%.2f"),
        
        // === CONFIGURATION ===
        new SectionHeader("Configuration"),
        
        // Row 4: Dual Jets checkbox + Gap slider
        CheckboxSpec.half("Dual Jets", "jet.dualJets", "Emit jets from both poles"),
        SliderSpec.half("Gap", "jet.gap", 0f, 2f, "%.2f"),
        
        // Note: Hollow checkbox + inner radii are handled in ShapeSubPanel (conditional)
        
        // === CAPS ===
        new SectionHeader("Caps"),
        
        // Row 5: Cap Base + Cap Tip
        CheckboxSpec.half("Cap Base", "jet.capBase", "Close base end"),
        CheckboxSpec.half("Cap Tip", "jet.capTip", "Close tip end (if not pointed)"),
        
        // === ALPHA GRADIENT ===
        new SectionHeader("Alpha Gradient"),
        
        // Row: Base Alpha + Base Min Alpha
        SliderSpec.half("Base α", "jet.baseAlpha", 0f, 1f, "%.2f"),
        SliderSpec.half("Base Min α", "jet.baseMinAlpha", 0f, 1f, "%.2f"),
        
        // Row: Tip Alpha + Tip Min Alpha
        SliderSpec.half("Tip α", "jet.tipAlpha", 0f, 1f, "%.2f"),
        SliderSpec.half("Tip Min α", "jet.tipMinAlpha", 0f, 1f, "%.2f")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // RAYS (Common controls for ALL ray types)
    // Note: LINE-only and 3D-only controls are added conditionally in ShapeSubPanel
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> RAYS_SPECS = List.of(
        // === GEOMETRY (ALL ray types) ===
        new SectionHeader("Ray Geometry"),
        
        // Row: Ray Length (ALL ray types use this)
        SliderSpec.full("Ray Len", "rays.rayLength", 0.1f, 10f, "%.2f"),
        
        // === DISTRIBUTION (ALL ray types) ===
        new SectionHeader("Distribution"),
        
        // Row: Count + Arrangement
        SliderSpec.halfInt("Count", "rays.count", 1, 1000),
        EnumDropdownSpec.half("Arrange", "rays.arrangement", 
            net.cyberpunk042.visual.shape.RayArrangement.class, 
            net.cyberpunk042.visual.shape.RayArrangement.RADIAL),
        
        // Row: Distribution mode + Inner Radius
        EnumDropdownSpec.half("Distrib", "rays.distribution",
            net.cyberpunk042.visual.shape.RayDistribution.class,
            net.cyberpunk042.visual.shape.RayDistribution.UNIFORM),
        SliderSpec.half("Inner R", "rays.innerRadius", 0f, 10f, "%.2f"),
        
        // Row: Outer Radius + Layers
        SliderSpec.half("Outer R", "rays.outerRadius", 0.1f, 20f, "%.2f"),
        SliderSpec.halfInt("Layers", "rays.layers", 1, 16),
        
        // Row: Layer Spacing + Layer Mode
        SliderSpec.half("Layer Sp", "rays.layerSpacing", 0.1f, 10f, "%.2f"),
        EnumDropdownSpec.half("Layer Mode", "rays.layerMode",
            net.cyberpunk042.visual.shape.RayLayerMode.class,
            net.cyberpunk042.visual.shape.RayLayerMode.VERTICAL),
        
        // Row: Unified End (for radial/shell/spiral layers) + break
        CheckboxSpec.half("Unified End", "rays.unifiedEnd", "All layers converge to same inner radius"),
        new RowBreak(),
        
        // Row: Randomness + Length Variation
        SliderSpec.half("Random", "rays.randomness", 0f, 1f, "%.2f"),
        SliderSpec.half("Len Var", "rays.lengthVariation", 0f, 1f, "%.2f")
        
        // NOTE: The following are added CONDITIONALLY in ShapeSubPanel:
        // 
        // LINE-ONLY: Ray Width, Fade Start/End, Segments/Seg Gap, Line Shape, Amplitude/Frequency
        // 3D-ONLY: Orientation, Intensity, Shape Length, Quad Pattern
        // ALL TYPES: Ray Type dropdown, Field Curvature (both at bottom of panel)
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // KAMEHAMEHA (Energy beam with charging orb)
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> KAMEHAMEHA_SPECS = List.of(
        // === COMBINED PROGRESS [0-2] ===
        // 0.0-1.0 = Orb charges, 1.0-2.0 = Beam extends
        SliderSpec.full("Progress", "kamehameha.combinedProgress", 0f, 2f, "%.2f"),
        
        // === ORB CONFIGURATION ===
        new SectionHeader("Orb"),
        
        // Row: Orb Radius + Segments
        SliderSpec.half("Orb R", "kamehameha.orbRadius", 0.1f, 2f, "%.2f"),
        SliderSpec.halfInt("Orb Segs", "kamehameha.orbSegments", 4, 48),
        
        // Row: Orb Transition + Alpha
        EnumDropdownSpec.half("Trans", "kamehameha.orbTransition",
            net.cyberpunk042.visual.shape.TransitionStyle.class,
            net.cyberpunk042.visual.shape.TransitionStyle.FADE_AND_SCALE),
        SliderSpec.half("Orb α", "kamehameha.orbAlpha", 0f, 1f, "%.2f"),
        
        // === BEAM CONFIGURATION ===
        new SectionHeader("Beam"),
        
        // Row: Beam Length + Transition
        SliderSpec.half("Length", "kamehameha.beamLength", 0f, 20f, "%.1f"),
        EnumDropdownSpec.half("Trans", "kamehameha.beamTransition",
            net.cyberpunk042.visual.shape.TransitionStyle.class,
            net.cyberpunk042.visual.shape.TransitionStyle.SCALE),
        
        // Row: Beam Ratio + Taper
        SliderSpec.half("Ratio", "kamehameha.beamRatio", 0.05f, 1f, "%.2f"),
        SliderSpec.half("Taper", "kamehameha.beamTaper", 0f, 1.5f, "%.2f"),
        
        // Row: Length Intensity + Width Intensity
        SliderSpec.half("Len Int", "kamehameha.beamLengthIntensity", 0f, 3f, "%.1f"),
        SliderSpec.half("Wid Int", "kamehameha.beamWidthIntensity", 0f, 3f, "%.1f"),
        
        // Row: Twist + (break)
        SliderSpec.halfDegreeSigned("Twist", "kamehameha.beamTwist"),
        new RowBreak(),
        
        // Row: Beam Segments + Length Segments
        SliderSpec.halfInt("Segs", "kamehameha.beamSegments", 4, 48),
        SliderSpec.halfInt("Len Segs", "kamehameha.beamLengthSegments", 1, 32),
        
        // === BEAM ALPHA GRADIENT ===
        new SectionHeader("Beam Alpha"),
        
        // Row: Base Alpha + Base Min Alpha
        SliderSpec.half("Base α", "kamehameha.beamBaseAlpha", 0f, 1f, "%.2f"),
        SliderSpec.half("Base Min α", "kamehameha.beamBaseMinAlpha", 0f, 1f, "%.2f"),
        
        // Row: Tip Alpha + Tip Min Alpha
        SliderSpec.half("Tip α", "kamehameha.beamTipAlpha", 0f, 1f, "%.2f"),
        SliderSpec.half("Tip Min α", "kamehameha.beamTipMinAlpha", 0f, 1f, "%.2f"),
        
        // === TIP STYLE ===
        new SectionHeader("Tip"),
        CheckboxSpec.full("Dome Tip", "kamehameha.hasDomeTip", "Use hemisphere dome tip (unchecked = flat cut)"),
        
        // === ORIENTATION ===
        new SectionHeader("Orientation"),
        
        // Row: Orientation Axis + Origin Offset
        EnumDropdownSpec.half("Axis", "kamehameha.orientationAxis",
            net.cyberpunk042.visual.shape.OrientationAxis.class,
            net.cyberpunk042.visual.shape.OrientationAxis.POS_Z),
        SliderSpec.half("Offset", "kamehameha.originOffset", -5f, 5f, "%.2f")
    );
    
    // ───────────────────────────────────────────────────────────────────────────
    // MOLECULE (Metaball-style spheres with connecting tubes)
    // ───────────────────────────────────────────────────────────────────────────
    
    private static final List<Object> MOLECULE_SPECS = List.of(
        // === ATOMS ===
        new SectionHeader("Atoms"),
        
        // Row: Atom Count + Distribution
        SliderSpec.halfInt("Count", "molecule.atomCount", 2, 12),
        EnumDropdownSpec.half("Layout", "molecule.distribution",
            net.cyberpunk042.visual.shape.AtomDistribution.class,
            net.cyberpunk042.visual.shape.AtomDistribution.FIBONACCI),
        
        // Row: Atom Radius + Atom Distance
        SliderSpec.half("Atom R", "molecule.atomRadius", 0.1f, 1f, "%.2f"),
        SliderSpec.half("Distance", "molecule.atomDistance", 0.3f, 2f, "%.2f"),
        
        // Row: Size Variation + Seed
        SliderSpec.half("Size Var", "molecule.sizeVariation", 0f, 1f, "%.2f"),
        SliderSpec.halfInt("Seed", "molecule.seed", 0, 999),
        
        // === CONNECTORS ===
        new SectionHeader("Connectors"),
        
        // Row: Neck Radius + Neck Pinch
        SliderSpec.half("Neck R", "molecule.neckRadius", 0.02f, 0.5f, "%.2f"),
        SliderSpec.half("Pinch", "molecule.neckPinch", 0f, 1f, "%.2f"),
        
        // Row: Connection Distance (full width)
        SliderSpec.full("Connect Dist", "molecule.connectionDistance", 0.5f, 3f, "%.2f"),
        
        // === SCALE ===
        new SectionHeader("Scale"),
        SliderSpec.full("Scale", "molecule.scale", 0.1f, 10f, "%.2f")
    );
}
