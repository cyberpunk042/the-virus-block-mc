package net.cyberpunk042.init;

/**
 * Standard node and stage IDs used across the mod.
 * 
 * <h2>Why Use This?</h2>
 * <ul>
 *   <li>Avoid typos in string IDs</li>
 *   <li>IDE autocomplete</li>
 *   <li>One place to see all registered nodes</li>
 *   <li>Easy to find usages</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Instead of:
 * if (Init.isReady("field_registry")) { ... }
 * 
 * // Use:
 * if (Init.isReady(InitIds.FIELD_REGISTRY)) { ... }
 * }</pre>
 */
public final class InitIds {
    
    private InitIds() {} // Static access only
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STAGES (Phases)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Core systems: payloads, handlers, Minecraft registries */
    public static final String STAGE_CORE = "core";
    
    /** Field system: definitions, themes, shapes */
    public static final String STAGE_FIELD = "field";
    
    /** Infection system: scenarios, growth */
    public static final String STAGE_INFECTION = "infection";
    
    /** GUI system: fragments, presets */
    public static final String STAGE_GUI = "gui";
    
    /** Client-side receivers */
    public static final String STAGE_RECEIVERS = "receivers";
    
    /** Client-side renderers */
    public static final String STAGE_RENDERERS = "renderers";
    
    /** Warmup/preloading phase */
    public static final String STAGE_WARMUP = "warmup";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE NODES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** PayloadTypeRegistry S2C + C2S */
    public static final String PAYLOADS = "payloads";
    
    /** Core S2C payloads (sky, horizon, etc.) */
    public static final String PAYLOADS_CORE = "payloads_core";
    
    /** Field-specific payloads */
    public static final String PAYLOADS_FIELD = "payloads_field";
    
    /** GUI-related payloads */
    public static final String PAYLOADS_GUI = "payloads_gui";
    
    /** ServerPlayNetworking handlers */
    public static final String HANDLERS = "handlers";
    
    /** ModBlocks, ModItems, etc. */
    public static final String MOD_REGISTRIES = "mod_registries";
    
    /** Log and command configs */
    public static final String CONFIG = "config";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FIELD NODES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** FieldRegistry definitions */
    public static final String FIELD_REGISTRY = "field_registry";
    
    /** ColorThemeRegistry */
    public static final String COLOR_THEMES = "color_themes";
    
    /** ShapeRegistry factories */
    public static final String SHAPE_REGISTRY = "shape_registry";
    
    /** Field profiles */
    public static final String PROFILES = "profiles";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INFECTION NODES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** ScenarioRegistry */
    public static final String SCENARIOS = "scenarios";
    
    /** GrowthRegistry (all profile types) */
    public static final String GROWTH = "growth";
    
    /** DimensionProfileRegistry */
    public static final String DIMENSIONS = "dimensions";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GUI NODES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** FragmentRegistry (shape/fill presets) */
    public static final String FRAGMENTS = "fragments";
    
    /** PresetRegistry (multi-scope presets) */
    public static final String PRESETS = "presets";
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CLIENT NODES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** ClientPlayNetworking receivers - core */
    public static final String RECEIVERS_CORE = "receivers_core";
    
    /** ClientPlayNetworking receivers - field */
    public static final String RECEIVERS_FIELD = "receivers_field";
    
    /** ClientPlayNetworking receivers - GUI */
    public static final String RECEIVERS_GUI = "receivers_gui";
    
    /** Block renderers */
    public static final String RENDERERS_BLOCK = "renderers_block";
    
    /** Entity renderers */
    public static final String RENDERERS_ENTITY = "renderers_entity";
    
    /** Effect renderers (singularity, void tear) */
    public static final String RENDERERS_EFFECT = "renderers_effect";
    
    /** ClientFieldManager setup */
    public static final String CLIENT_FIELD_MANAGER = "client_field_manager";
    
    /** ProfileManager (client-side) */
    public static final String PROFILE_MANAGER = "profile_manager";
    
    /** Tessellation warmup */
    public static final String WARMUP_TESSELLATION = "warmup_tessellation";
    
    /** Shader warmup */
    public static final String WARMUP_SHADERS = "warmup_shaders";
}
