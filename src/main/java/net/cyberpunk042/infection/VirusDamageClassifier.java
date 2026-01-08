package net.cyberpunk042.infection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;

/**
 * Classifies damage sources into categories for viral adaptation tracking.
 * 
 * <p>The virus tracks damage by category and develops resistance over time.
 * Categories are designed to balance gameplay:
 * <ul>
 *   <li>BED - Intentional bed explosions (a classic strategy)</li>
 *   <li>TNT - TNT explosions</li>
 *   <li>EXPLOSION - Other explosions (end crystals, creepers, etc.)</li>
 *   <li>MELEE:{item_id} - Melee attacks with specific weapons/tools</li>
 *   <li>PROJECTILE - Arrow/projectile damage</li>
 *   <li>OTHER - Fallback for uncategorized damage</li>
 * </ul>
 * 
 * @see net.cyberpunk042.infection.state.InfectionState#recordDamageExposure(String)
 */
public final class VirusDamageClassifier {

	/** Damage key for bed explosions. */
	public static final String KEY_BED = "BED";
	
	/** Damage key for TNT explosions. */
	public static final String KEY_TNT = "TNT";
	
	/** Damage key for other explosions. */
	public static final String KEY_EXPLOSION = "EXPLOSION";
	
	/** Damage key prefix for melee attacks. */
	public static final String KEY_MELEE_PREFIX = "MELEE:";
	
	/** Damage key for bare-handed attacks. */
	public static final String KEY_MELEE_FIST = "MELEE:fist";
	
	/** Damage key for projectile attacks. */
	public static final String KEY_PROJECTILE = "PROJECTILE";
	
	/** Damage key for uncategorized damage. */
	public static final String KEY_OTHER = "OTHER";

	private VirusDamageClassifier() {}

	/**
	 * Classifies a damage source into a category key for adaptation tracking.
	 * 
	 * @param source The damage source (may be null)
	 * @param attacker The attacking entity (may be null)
	 * @return A category key string, never null
	 */
	public static String classify(@Nullable DamageSource source, @Nullable Entity attacker) {
		if (source == null) {
			return KEY_OTHER;
		}

		// Check for explosions first
		if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
			return classifyExplosion(attacker);
		}

		// Check for projectiles
		if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
			return KEY_PROJECTILE;
		}

		// Check for player melee attacks
		if (attacker instanceof PlayerEntity player) {
			return classifyMelee(player);
		}

		return KEY_OTHER;
	}

	/**
	 * Classifies an explosion based on its source entity.
	 * 
	 * @param source The entity that caused the explosion (may be null)
	 * @return A category key: BED for null entity, TNT for TntEntity, EXPLOSION otherwise
	 */
	public static String classifyExplosion(@Nullable Entity source) {
		// TNT entity = TNT explosion
		if (source instanceof TntEntity) {
			return KEY_TNT;
		}
		
		// Null entity typically means bed or respawn anchor explosion
		// (these don't have a source entity in vanilla)
		if (source == null) {
			return KEY_BED;
		}
		
		// Other explosions: creepers, crystals, ghast fireballs, etc.
		return KEY_EXPLOSION;
	}

	/**
	 * Classifies melee damage from a player, tracking the specific tool/weapon used.
	 */
	private static String classifyMelee(PlayerEntity player) {
		ItemStack held = player.getMainHandStack();
		if (held.isEmpty()) {
			return KEY_MELEE_FIST;
		}
		
		// Track resistance per unique item type
		// e.g., "MELEE:minecraft:diamond_pickaxe"
		String itemId = Registries.ITEM.getId(held.getItem()).toString();
		return KEY_MELEE_PREFIX + itemId;
	}

	/**
	 * Returns a human-readable display name for a damage key.
	 * Used for player feedback messages.
	 */
	public static String getDisplayName(String damageKey) {
		if (damageKey == null) {
			return "Unknown";
		}
		
		return switch (damageKey) {
			case KEY_BED -> "Bed Explosion";
			case KEY_TNT -> "TNT";
			case KEY_EXPLOSION -> "Explosion";
			case KEY_PROJECTILE -> "Projectiles";
			case KEY_MELEE_FIST -> "Bare Hands";
			case KEY_OTHER -> "Unknown";
			default -> {
				if (damageKey.startsWith(KEY_MELEE_PREFIX)) {
					// Extract item name: "MELEE:minecraft:diamond_sword" -> "diamond_sword"
					String itemId = damageKey.substring(KEY_MELEE_PREFIX.length());
					int lastColon = itemId.lastIndexOf(':');
					if (lastColon >= 0) {
						itemId = itemId.substring(lastColon + 1);
					}
					// Convert underscores to spaces and capitalize
					yield formatItemName(itemId);
				}
				yield damageKey;
			}
		};
	}

	/**
	 * Formats an item ID into a readable name.
	 * e.g., "diamond_pickaxe" -> "Diamond Pickaxe"
	 */
	private static String formatItemName(String itemId) {
		if (itemId == null || itemId.isEmpty()) {
			return "Unknown Item";
		}
		
		String[] words = itemId.split("_");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			if (!word.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(Character.toUpperCase(word.charAt(0)));
				if (word.length() > 1) {
					sb.append(word.substring(1));
				}
			}
		}
		return sb.toString();
	}
}
