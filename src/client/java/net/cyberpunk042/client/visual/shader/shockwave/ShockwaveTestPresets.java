package net.cyberpunk042.client.visual.shader.shockwave;

import net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.*;

/**
 * Test sequence presets for the Shockwave VFX system.
 * 
 * <p>Provides a 16-step visual validation sequence that cycles through
 * all major features: colors, ring counts, shapes, screen effects, etc.</p>
 */
public final class ShockwaveTestPresets {
    
    private ShockwaveTestPresets() {} // Utility class
    
    private static int testStep = -1;
    
    private static final String[] TEST_NAMES = {
        "1: Basic trigger (cyan)",
        "2: Red rings",
        "3: Green rings",
        "4: Multiple rings (5)",
        "5: Thick rings",
        "6: High intensity",
        "7: Blackout 50%",
        "8: Vignette effect",
        "9: Red tint",
        "10: All screen effects",
        "11: SPHERE shape (r=5)",
        "12: TORUS shape (20/3)",
        "13: POLYGON hex (6 sides)",
        "14: ORBITAL (4 spheres)",
        "RESET: Back to defaults"
    };
    
    /**
     * Configuration result from cycling to a new test step.
     */
    public record TestConfig(
        String name,
        RingParams ringParams,
        RingColor ringColor,
        ScreenEffects screenEffects,
        ShapeConfig shapeConfig,
        boolean shouldTrigger,
        boolean shouldDisable
    ) {}
    
    /**
     * Cycles to the next test configuration.
     * @return TestConfig with all parameters for the current step
     */
    public static TestConfig cycleNext() {
        testStep++;
        if (testStep >= TEST_NAMES.length) testStep = 0;
        
        // Start with defaults
        RingParams ringParams = RingParams.DEFAULT;
        RingColor ringColor = RingColor.DEFAULT;
        ScreenEffects screenEffects = ScreenEffects.NONE;
        ShapeConfig shapeConfig = ShapeConfig.POINT;
        boolean shouldTrigger = true;
        boolean shouldDisable = false;
        
        switch (testStep) {
            case 0 -> {
                // Basic trigger - default cyan
            }
            case 1 -> {
                // Red rings
                ringColor = new RingColor(1f, 0f, 0f, 1f);
            }
            case 2 -> {
                // Green rings
                ringColor = new RingColor(0f, 1f, 0f, 1f);
            }
            case 3 -> {
                // Multiple rings
                ringParams = ringParams.withCount(5).withSpacing(15f);
            }
            case 4 -> {
                // Thick rings
                ringParams = ringParams.withThickness(10f).withGlowWidth(15f);
            }
            case 5 -> {
                // High intensity
                ringParams = ringParams.withIntensity(2.5f);
            }
            case 6 -> {
                // Blackout with animation
                screenEffects = new ScreenEffects(0.5f, 0f, 0.5f, 1f, 1f, 1f, 0f);
            }
            case 7 -> {
                // Vignette with animation
                screenEffects = new ScreenEffects(0f, 0.8f, 0.3f, 1f, 1f, 1f, 0f);
            }
            case 8 -> {
                // Red tint with animation
                screenEffects = new ScreenEffects(0f, 0f, 0.5f, 1f, 0.3f, 0.3f, 0.7f);
            }
            case 9 -> {
                // All screen effects
                screenEffects = new ScreenEffects(0.3f, 0.5f, 0.4f, 1f, 0.5f, 0.8f, 0.5f);
                ringColor = new RingColor(1f, 0.5f, 0f, 1f); // Orange
            }
            case 10 -> {
                // Reset to defaults
                shouldTrigger = false;
                shouldDisable = true;
            }
            case 11 -> {
                // Sphere shape
                shapeConfig = ShapeConfig.sphere(5f);
            }
            case 12 -> {
                // Torus shape
                shapeConfig = ShapeConfig.torus(20f, 3f);
            }
            case 13 -> {
                // Polygon (hexagon)
                shapeConfig = ShapeConfig.polygon(6, 15f);
            }
            case 14 -> {
                // Orbital (4 spheres)
                shapeConfig = ShapeConfig.orbital(5f, 2f, 15f, 4);
            }
            default -> {
                // Reset to defaults  
                shapeConfig = ShapeConfig.POINT;
                shouldTrigger = false;
                shouldDisable = true;
            }
        }
        
        return new TestConfig(
            TEST_NAMES[testStep],
            ringParams,
            ringColor,
            screenEffects,
            shapeConfig,
            shouldTrigger,
            shouldDisable
        );
    }
    
    /**
     * Gets current test step name without cycling.
     */
    public static String getCurrentName() {
        if (testStep < 0 || testStep >= TEST_NAMES.length) {
            return "Use /shockwavegpu test to start";
        }
        return TEST_NAMES[testStep];
    }
    
    /**
     * Resets test sequence to beginning.
     */
    public static void reset() {
        testStep = -1;
    }
}
