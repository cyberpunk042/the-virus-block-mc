package net.cyberpunk042.infection;

import java.util.Locale;
import java.util.function.Supplier;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public enum VirusDifficulty {
	// Recalibrated difficulties - EASY should be relaxed, EXTREME should be intense but playable
	// Format: id, icon, durationMult, knockbackMult, bleedOption, healthPenalty, wormSpawn, eventOdds, guardianDmg, matrixDmg, corruptionSpread, texture
	EASY("easy", Items.TOTEM_OF_UNDYING::getDefaultStack, 2.0D, 0.0D, true, 0, 0.3D, 0.5D, 0.4D, 0.5D, 0.3D, "difficulty_easy"),
	MEDIUM("medium", Items.SHIELD::getDefaultStack, 1.4D, 0.4D, false, 0, 0.6D, 0.75D, 0.7D, 0.75D, 0.6D, "difficulty_medium"),
	HARD("hard", Items.NETHERITE_SWORD::getDefaultStack, 1.0D, 1.0D, false, 0, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, "difficulty_hard"),
	EXTREME("extreme", Items.WITHER_SKELETON_SKULL::getDefaultStack, 0.8D, 1.3D, false, -2, 1.4D, 1.2D, 1.4D, 1.3D, 1.5D, "difficulty_extreme");

	private final String id;
	private final Supplier<ItemStack> iconSupplier;
	private final double durationMultiplier;
	private final double knockbackMultiplier;
	private final boolean allowsBleedOption;
	private final int maxHealthPenaltyPerTier; // negative value reduces health per tier
	private final double wormSpawnMultiplier;
	private final double eventOddsMultiplier;
	private final double guardianBeamDamageMultiplier;
	private final double matrixCubeDamageMultiplier;
	private final double corruptionSpreadMultiplier;
	private final Identifier iconTexture;

	VirusDifficulty(String id,
	                Supplier<ItemStack> iconSupplier,
	                double durationMultiplier,
	                double knockbackMultiplier,
	                boolean allowsBleedOption,
	                int maxHealthPenaltyPerTier,
	                double wormSpawnMultiplier,
	                double eventOddsMultiplier,
	                double guardianBeamDamageMultiplier,
	                double matrixCubeDamageMultiplier,
	                double corruptionSpreadMultiplier,
	                String iconTexturePath) {
		this.id = id.toLowerCase(Locale.ROOT);
		this.iconSupplier = iconSupplier;
		this.durationMultiplier = durationMultiplier;
		this.knockbackMultiplier = knockbackMultiplier;
		this.allowsBleedOption = allowsBleedOption;
		this.maxHealthPenaltyPerTier = maxHealthPenaltyPerTier;
		this.wormSpawnMultiplier = wormSpawnMultiplier;
		this.eventOddsMultiplier = eventOddsMultiplier;
		this.guardianBeamDamageMultiplier = guardianBeamDamageMultiplier;
		this.matrixCubeDamageMultiplier = matrixCubeDamageMultiplier;
		this.corruptionSpreadMultiplier = corruptionSpreadMultiplier;
		this.iconTexture = Identifier.of(TheVirusBlock.MOD_ID, "textures/gui/" + iconTexturePath + ".png");
	}

	public String getId() {
		return id;
	}

	public Text getDisplayName() {
		return Text.translatable("difficulty.the-virus-block." + id + ".name");
	}

	public Text getDescription() {
		return Text.translatable("difficulty.the-virus-block." + id + ".desc");
	}

	public ItemStack createIcon() {
		return iconSupplier.get().copy();
	}

	public double getDurationMultiplier() {
		return durationMultiplier;
	}

	public double getKnockbackMultiplier() {
		return knockbackMultiplier;
	}

	public boolean allowsBleedOption() {
		return allowsBleedOption;
	}

	public int getHealthPenaltyForTier(int tierIndex) {
		return maxHealthPenaltyPerTier * Math.max(0, tierIndex + 1);
	}

	public double getWormSpawnMultiplier() {
		return wormSpawnMultiplier;
	}

	public double getEventOddsMultiplier() {
		return eventOddsMultiplier;
	}

	public double getGuardianBeamDamageMultiplier() {
		return guardianBeamDamageMultiplier;
	}

	public double getMatrixCubeDamageMultiplier() {
		return matrixCubeDamageMultiplier;
	}

	public double getCorruptionSpreadMultiplier() {
		return corruptionSpreadMultiplier;
	}

	public float getTeleportChance() {
		return switch (this) {
			case EASY -> 0.10F;    // Very predictable
			case MEDIUM -> 0.20F;  // Occasionally moves
			case HARD -> 0.40F;    // Moves around more
			case EXTREME -> 0.60F; // Very mobile
		};
	}

	public Identifier getIconTexture() {
		return iconTexture;
	}

	public static VirusDifficulty fromId(String id) {
		for (VirusDifficulty diff : values()) {
			if (diff.id.equalsIgnoreCase(id)) {
				return diff;
			}
		}
		return HARD;
	}
}

