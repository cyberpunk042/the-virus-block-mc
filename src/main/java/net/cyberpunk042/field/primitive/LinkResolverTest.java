package net.cyberpunk042.field.primitive;

import net.cyberpunk042.field.loader.SimplePrimitive;
import net.cyberpunk042.visual.animation.Animation;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.fill.FillConfig;
import net.cyberpunk042.visual.pattern.ArrangementConfig;
import net.cyberpunk042.visual.shape.RingShape;
import net.cyberpunk042.visual.shape.SphereShape;
import net.cyberpunk042.visual.transform.Transform;
import net.cyberpunk042.visual.visibility.VisibilityMask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for primitive linking functionality.
 * 
 * <p>Tests F114 and F115 from TODO_LIST.md:
 * <ul>
 *   <li>F114: Test ring links to sphere radius + offset</li>
 *   <li>F115: Test invalid link (forward reference) logs error</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <p>Run with: {@code java net.cyberpunk042.field.primitive.LinkResolverTest}</p>
 * 
 * @see LinkResolver
 * @see PrimitiveLink
 */
public class LinkResolverTest {
    
    /**
     * F114: Test that a ring can link to a sphere's radius with an offset.
     * 
     * <p>Creates:
     * <ul>
     *   <li>Sphere with radius 2.0</li>
     *   <li>Ring that links to sphere with offset 0.5</li>
     * </ul>
     * 
     * <p>Expected: Ring's resolved radius should be 2.0 + 0.5 = 2.5
     */
    public static boolean testF114_RingLinksToSphereRadius() {
        System.out.println("=== F114: Ring links to sphere radius + offset ===");
        
        // Create sphere primitive with radius 2.0
        SphereShape sphereShape = SphereShape.ofRadius(2.0f);
        SimplePrimitive sphere = SimplePrimitive.of("sphere_1", "sphere", sphereShape);
        
        // Create ring primitive that links to sphere with offset 0.5
        RingShape ringShape = RingShape.at(0.8f, 1.0f, 0.0f); // Initial values, will be overridden
        PrimitiveLink link = PrimitiveLink.radiusMatch("sphere_1", 0.5f);
        SimplePrimitive ring = new SimplePrimitive(
            "ring_1", "ring", ringShape,
            Transform.IDENTITY,
            FillConfig.SOLID,
            VisibilityMask.FULL,
            ArrangementConfig.DEFAULT,
            Appearance.DEFAULT,
            Animation.NONE,
            link
        );
        
        // Build primitive index (only sphere, since ring comes after)
        Map<String, Primitive> index = new HashMap<>();
        index.put("sphere_1", sphere);
        
        // Resolve links for ring
        LinkResolver.ResolvedValues resolved = LinkResolver.resolveLinks(ring, index);
        
        // Verify radius resolution
        float expectedRadius = 2.0f + 0.5f; // sphere radius + offset
        boolean hasRadius = resolved.hasRadius();
        float actualRadius = resolved.radius();
        
        System.out.println("  Sphere radius: " + sphereShape.radius());
        System.out.println("  Link offset: " + link.radiusOffset());
        System.out.println("  Expected resolved radius: " + expectedRadius);
        System.out.println("  Has radius: " + hasRadius);
        System.out.println("  Actual resolved radius: " + actualRadius);
        
        boolean passed = hasRadius && Math.abs(actualRadius - expectedRadius) < 0.001f;
        
        if (passed) {
            System.out.println("  ✅ PASS: Ring correctly links to sphere radius with offset");
        } else {
            System.out.println("  ❌ FAIL: Expected radius " + expectedRadius + ", got " + actualRadius);
        }
        
        return passed;
    }
    
    /**
     * F115: Test that forward references are detected and logged.
     * 
     * <p>Creates:
     * <ul>
     *   <li>Ring that tries to link to "sphere_1" (forward reference)</li>
     *   <li>Sphere declared AFTER the ring</li>
     * </ul>
     * 
     * <p>Expected: Validation should detect forward reference and return error
     */
    public static boolean testF115_ForwardReferenceDetection() {
        System.out.println("\n=== F115: Invalid link (forward reference) logs error ===");
        
        // Create ring that links to sphere (but sphere comes later)
        RingShape ringShape = RingShape.at(0.8f, 1.0f, 0.0f);
        PrimitiveLink link = PrimitiveLink.radiusMatch("sphere_1", 0.0f);
        SimplePrimitive ring = new SimplePrimitive(
            "ring_1", "ring", ringShape,
            Transform.IDENTITY,
            FillConfig.SOLID,
            VisibilityMask.FULL,
            ArrangementConfig.DEFAULT,
            Appearance.DEFAULT,
            Animation.NONE,
            link
        );
        
        // Create sphere (declared AFTER ring - forward reference!)
        SphereShape sphereShape = SphereShape.ofRadius(2.0f);
        SimplePrimitive sphere = SimplePrimitive.of("sphere_1", "sphere", sphereShape);
        
        // List in order: ring first, then sphere (forward reference)
        List<Primitive> primitives = new ArrayList<>();
        primitives.add(ring);
        primitives.add(sphere);
        
        // Validate links
        List<String> errors = LinkResolver.validate(primitives);
        
        System.out.println("  Primitives order: ring_1 (links to sphere_1), sphere_1");
        System.out.println("  Validation errors found: " + errors.size());
        for (String error : errors) {
            System.out.println("    - " + error);
        }
        
        // Check for forward reference error
        boolean hasForwardRefError = errors.stream()
            .anyMatch(e -> e.contains("Forward reference") && e.contains("ring_1") && e.contains("sphere_1"));
        
        boolean passed = !errors.isEmpty() && hasForwardRefError;
        
        if (passed) {
            System.out.println("  ✅ PASS: Forward reference correctly detected and logged");
        } else {
            System.out.println("  ❌ FAIL: Forward reference not detected or wrong error message");
            if (errors.isEmpty()) {
                System.out.println("    Expected at least one error, but got none");
            } else {
                System.out.println("    Expected error containing 'Forward reference', but got:");
                errors.forEach(e -> System.out.println("      " + e));
            }
        }
        
        return passed;
    }
    
    /**
     * Additional test: Verify that valid links (backward references) work correctly.
     */
    public static boolean testValidBackwardReference() {
        System.out.println("\n=== Additional: Valid backward reference ===");
        
        // Create sphere first
        SphereShape sphereShape = SphereShape.ofRadius(2.0f);
        SimplePrimitive sphere = SimplePrimitive.of("sphere_1", "sphere", sphereShape);
        
        // Create ring that links to sphere (valid - sphere comes before)
        RingShape ringShape = RingShape.at(0.8f, 1.0f, 0.0f);
        PrimitiveLink link = PrimitiveLink.radiusMatch("sphere_1", 0.5f);
        SimplePrimitive ring = new SimplePrimitive(
            "ring_1", "ring", ringShape,
            Transform.IDENTITY,
            FillConfig.SOLID,
            VisibilityMask.FULL,
            ArrangementConfig.DEFAULT,
            Appearance.DEFAULT,
            Animation.NONE,
            link
        );
        
        // List in correct order: sphere first, then ring
        List<Primitive> primitives = new ArrayList<>();
        primitives.add(sphere);
        primitives.add(ring);
        
        // Validate links
        List<String> errors = LinkResolver.validate(primitives);
        
        System.out.println("  Primitives order: sphere_1, ring_1 (links to sphere_1)");
        System.out.println("  Validation errors: " + errors.size());
        
        boolean passed = errors.isEmpty();
        
        if (passed) {
            System.out.println("  ✅ PASS: Valid backward reference accepted");
        } else {
            System.out.println("  ❌ FAIL: Valid backward reference incorrectly rejected");
            errors.forEach(e -> System.out.println("    - " + e));
        }
        
        return passed;
    }
    
    /**
     * Main entry point for running tests.
     */
    public static void main(String[] args) {
        System.out.println("LinkResolver Tests");
        System.out.println("==================\n");
        
        boolean f114 = testF114_RingLinksToSphereRadius();
        boolean f115 = testF115_ForwardReferenceDetection();
        boolean additional = testValidBackwardReference();
        
        System.out.println("\n==================");
        System.out.println("Test Results:");
        System.out.println("  F114 (Ring links to sphere): " + (f114 ? "✅ PASS" : "❌ FAIL"));
        System.out.println("  F115 (Forward reference): " + (f115 ? "✅ PASS" : "❌ FAIL"));
        System.out.println("  Additional (Backward ref): " + (additional ? "✅ PASS" : "❌ FAIL"));
        
        boolean allPassed = f114 && f115 && additional;
        System.out.println("\nOverall: " + (allPassed ? "✅ ALL TESTS PASSED" : "❌ SOME TESTS FAILED"));
        
        System.exit(allPassed ? 0 : 1);
    }
}

