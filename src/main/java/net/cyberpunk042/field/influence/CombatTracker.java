package net.cyberpunk042.field.influence;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Tracks combat state for binding sources.
 * 
 * <p>Per CLASS_DIAGRAM §16:
 * <ul>
 *   <li>Tracks last damage taken/dealt timestamps</li>
 *   <li>Provides decaying damage amount</li>
 *   <li>In-combat = within 100 ticks of damage</li>
 * </ul>
 */
public final class CombatTracker {
    
    private static final int COMBAT_TIMEOUT_TICKS = 100; // 5 seconds
    private static final float DECAY_FACTOR = 0.95f;
    
    // Per-player combat data
    private static final Map<UUID, CombatData> PLAYER_DATA = new WeakHashMap<>();
    
    private record CombatData(
        long lastDamageTaken,
        long lastDamageDealt,
        float lastDamageAmount,
        float decayedDamage
    ) {
        static CombatData empty() {
            return new CombatData(0, 0, 0, 0);
        }
        
        CombatData withDamageTaken(long time, float amount) {
            return new CombatData(time, lastDamageDealt, amount, amount);
        }
        
        CombatData withDamageDealt(long time) {
            return new CombatData(lastDamageTaken, time, lastDamageAmount, decayedDamage);
        }
        
        CombatData withDecay(float newDecayed) {
            return new CombatData(lastDamageTaken, lastDamageDealt, lastDamageAmount, newDecayed);
        }
    }
    
    /**
     * Whether the player is in combat (damaged within timeout).
     */
    public static boolean isInCombat(PlayerEntity player) {
        if (player == null || player.getWorld() == null) return false;
        
        CombatData data = PLAYER_DATA.getOrDefault(player.getUuid(), CombatData.empty());
        long currentTime = player.getWorld().getTime();
        
        return (currentTime - data.lastDamageTaken) < COMBAT_TIMEOUT_TICKS ||
               (currentTime - data.lastDamageDealt) < COMBAT_TIMEOUT_TICKS;
    }
    
    /**
     * Gets the decayed damage amount.
     */
    public static float getDamageTakenDecayed(PlayerEntity player) {
        if (player == null) return 0;
        CombatData data = PLAYER_DATA.getOrDefault(player.getUuid(), CombatData.empty());
        return data.decayedDamage;
    }
    
    /**
     * Called when player takes damage.
     */
    public static void onDamageTaken(PlayerEntity player, float amount) {
        if (player == null || player.getWorld() == null) return;
        
        CombatData data = PLAYER_DATA.getOrDefault(player.getUuid(), CombatData.empty());
        PLAYER_DATA.put(player.getUuid(), data.withDamageTaken(player.getWorld().getTime(), amount));
    }
    
    /**
     * Called when player deals damage.
     */
    public static void onDamageDealt(PlayerEntity player) {
        if (player == null || player.getWorld() == null) return;
        
        CombatData data = PLAYER_DATA.getOrDefault(player.getUuid(), CombatData.empty());
        PLAYER_DATA.put(player.getUuid(), data.withDamageDealt(player.getWorld().getTime()));
    }
    
    /**
     * Called every tick to decay damage.
     */
    public static void tick(PlayerEntity player) {
        if (player == null) return;
        
        CombatData data = PLAYER_DATA.get(player.getUuid());
        if (data != null && data.decayedDamage > 0.01f) {
            PLAYER_DATA.put(player.getUuid(), data.withDecay(data.decayedDamage * DECAY_FACTOR));
        }
    }
    
    private CombatTracker() {}
}
