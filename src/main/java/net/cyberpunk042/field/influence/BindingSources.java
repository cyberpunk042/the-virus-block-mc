package net.cyberpunk042.field.influence;

import net.cyberpunk042.log.Logging;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of available binding sources.
 * 
 * <p>Per ARCHITECTURE §12.1, provides 12 sources:
 * <ul>
 *   <li>player.health, player.health_percent</li>
 *   <li>player.armor, player.food, player.speed</li>
 *   <li>player.is_sprinting, player.is_sneaking, player.is_flying, player.is_invisible</li>
 *   <li>player.in_combat, player.damage_taken</li>
 *   <li>field.age</li>
 * </ul>
 * 
 * @see BindingSource
 * @see BindingResolver
 */
public final class BindingSources {
    
    private static final Map<String, BindingSource> SOURCES = new HashMap<>();
    
    // =========================================================================
    // Player Health & Stats
    // =========================================================================
    
    public static final BindingSource PLAYER_HEALTH = register(new BindingSource() {
        @Override public String getId() { return "player.health"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null ? player.getHealth() : 0; 
        }
        @Override public boolean isBoolean() { return false; }
    });
    
    public static final BindingSource PLAYER_HEALTH_PERCENT = register(new BindingSource() {
        @Override public String getId() { return "player.health_percent"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null ? player.getHealth() / player.getMaxHealth() : 0; 
        }
        @Override public boolean isBoolean() { return false; }
    });
    
    public static final BindingSource PLAYER_ARMOR = register(new BindingSource() {
        @Override public String getId() { return "player.armor"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null ? player.getArmor() : 0; 
        }
        @Override public boolean isBoolean() { return false; }
    });
    
    public static final BindingSource PLAYER_FOOD = register(new BindingSource() {
        @Override public String getId() { return "player.food"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null ? player.getHungerManager().getFoodLevel() : 0; 
        }
        @Override public boolean isBoolean() { return false; }
    });
    
    public static final BindingSource PLAYER_SPEED = register(new BindingSource() {
        @Override public String getId() { return "player.speed"; }
        @Override public float getValue(PlayerEntity player) { 
            if (player == null) return 0;
            return (float) player.getVelocity().horizontalLength();
        }
        @Override public boolean isBoolean() { return false; }
    });
    
    // =========================================================================
    // Player State Booleans
    // =========================================================================
    
    public static final BindingSource PLAYER_SPRINTING = register(new BindingSource() {
        @Override public String getId() { return "player.is_sprinting"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null && player.isSprinting() ? 1 : 0; 
        }
        @Override public boolean isBoolean() { return true; }
    });
    
    public static final BindingSource PLAYER_SNEAKING = register(new BindingSource() {
        @Override public String getId() { return "player.is_sneaking"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null && player.isSneaking() ? 1 : 0; 
        }
        @Override public boolean isBoolean() { return true; }
    });
    
    public static final BindingSource PLAYER_FLYING = register(new BindingSource() {
        @Override public String getId() { return "player.is_flying"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null && player.getAbilities().flying ? 1 : 0; 
        }
        @Override public boolean isBoolean() { return true; }
    });
    
    public static final BindingSource PLAYER_INVISIBLE = register(new BindingSource() {
        @Override public String getId() { return "player.is_invisible"; }
        @Override public float getValue(PlayerEntity player) { 
            return player != null && player.isInvisible() ? 1 : 0; 
        }
        @Override public boolean isBoolean() { return true; }
    });
    
    // =========================================================================
    // Combat Sources (require CombatTracker)
    // =========================================================================
    
    public static final BindingSource PLAYER_IN_COMBAT = register(new BindingSource() {
        @Override public String getId() { return "player.in_combat"; }
        @Override public float getValue(PlayerEntity player) { 
            // Delegates to CombatTracker
            return CombatTracker.isInCombat(player) ? 1 : 0;
        }
        @Override public boolean isBoolean() { return true; }
    });
    
    public static final BindingSource PLAYER_DAMAGE_TAKEN = register(new BindingSource() {
        @Override public String getId() { return "player.damage_taken"; }
        @Override public float getValue(PlayerEntity player) { 
            // Delegates to CombatTracker
            return CombatTracker.getDamageTakenDecayed(player);
        }
        @Override public boolean isBoolean() { return false; }
    });
    
    // =========================================================================
    // Field Sources
    // =========================================================================
    
    public static final BindingSource FIELD_AGE = register(new BindingSource() {
        @Override public String getId() { return "field.age"; }
        @Override public float getValue(PlayerEntity player) { 
            // Field age is passed separately, not from player
            // This is a placeholder - actual value comes from BindingResolver context
            return 0;
        }
        @Override public boolean isBoolean() { return false; }
    });
    
    // =========================================================================
    // Registry Methods (F145)
    // =========================================================================
    
    private static BindingSource register(BindingSource source) {
        SOURCES.put(source.getId(), source);
        return source;
    }
    
    /**
     * Gets a binding source by ID.
     * 
     * @param id Source ID (e.g., "player.health")
     * @return Optional containing the source, or empty if not found
     */
    public static Optional<BindingSource> get(String id) {
        return Optional.ofNullable(SOURCES.get(id));
    }
    
    /**
     * Gets a binding source, returning default value on invalid source (F146).
     * 
     * @param id Source ID
     * @return The source, or null with warning logged
     */
    @Nullable
    public static BindingSource getOrWarn(String id) {
        BindingSource source = SOURCES.get(id);
        if (source == null) {
            Logging.FIELD.topic("binding").warn("Unknown binding source '{}', defaulting to 0", id);
        }
        return source;
    }
    
    /**
     * Checks if a source ID is valid.
     */
    public static boolean exists(String id) {
        return SOURCES.containsKey(id);
    }
    
    /**
     * Gets all available source IDs.
     */
    public static java.util.Set<String> getAvailableIds() {
        return java.util.Collections.unmodifiableSet(SOURCES.keySet());
    }
    
    private BindingSources() {}
}
