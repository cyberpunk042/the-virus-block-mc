package net.cyberpunk042.recipe;


import net.cyberpunk042.log.Logging;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Builds runtime fallback recipes for when the datapack fails to load.
 */
public final class VirusFallbackRecipes {
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> CURED_CUBE_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "cured_infectious_cube"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_NORTH_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_EAST_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem_east"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_SOUTH_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem_south"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> PURIFICATION_TOTEM_WEST_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem_west"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> COMPOSITE_ELYTRA_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "composite_elytra"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> RUBBER_BOOTS_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "rubber_boots"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> HEAVY_PANTS_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "heavy_pants"));
	private static final RegistryKey<net.minecraft.recipe.Recipe<?>> AUGMENTED_HELMET_ID =
			RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(TheVirusBlock.MOD_ID, "augmented_helmet"));

	private VirusFallbackRecipes() {
	}

	public static boolean appendMissingRecipes(PreparedRecipes current, List<RecipeEntry<?>> entries) {
		boolean changed = false;
		if (current.get(CURED_CUBE_ID) == null) {
			entries.add(createCuredCubeRecipe());
			Logging.CONFIG.warn("Injected fallback recipe for {} because datapack entry was missing", CURED_CUBE_ID.getValue());
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_NORTH_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_NORTH_ID, " C ", "GTI", " D "));
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_EAST_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_EAST_ID, " G ", "DTC", " I "));
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_SOUTH_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_SOUTH_ID, " D ", "ITG", " C "));
			changed = true;
		}
		if (current.get(PURIFICATION_TOTEM_WEST_ID) == null) {
			entries.add(createTotemRecipe(PURIFICATION_TOTEM_WEST_ID, " I ", "CTD", " G "));
			changed = true;
		}
		if (current.get(COMPOSITE_ELYTRA_ID) == null) {
			entries.add(createCompositeElytraRecipe());
			changed = true;
		}
		if (current.get(RUBBER_BOOTS_ID) == null) {
			entries.add(createRubberBootsRecipe());
			changed = true;
		}
		if (current.get(HEAVY_PANTS_ID) == null) {
			entries.add(createHeavyPantsRecipe());
			changed = true;
		}
		if (current.get(AUGMENTED_HELMET_ID) == null) {
			entries.add(createAugmentedHelmetRecipe());
			changed = true;
		}
		if (changed) {
			Logging.CONFIG.warn("Purification Totem fallback recipes were injected because the datapack failed to provide them.");
		}
		return changed;
	}

	private static RecipeEntry<ShapelessRecipe> createCuredCubeRecipe() {
		ShapelessRecipe recipe = new ShapelessRecipe(
				"",
				CraftingRecipeCategory.MISC,
				new ItemStack(ModBlocks.CURED_INFECTIOUS_CUBE),
				List.of(
						Ingredient.ofItems(ModBlocks.INFECTIOUS_CUBE),
						Ingredient.ofItems(Items.MILK_BUCKET)));
		return new RecipeEntry<>(CURED_CUBE_ID, recipe);
	}

	private static RecipeEntry<ShapedRecipe> createTotemRecipe(RegistryKey<net.minecraft.recipe.Recipe<?>> id, String... pattern) {
		Map<Character, Ingredient> key = Map.of(
				'C', Ingredient.ofItems(ModBlocks.CURED_INFECTIOUS_CUBE),
				'G', Ingredient.ofItems(ModBlocks.CORRUPTED_GOLD),
				'I', Ingredient.ofItems(ModBlocks.CORRUPTED_IRON),
				'D', Ingredient.ofItems(ModBlocks.CORRUPTED_DIAMOND),
				'T', Ingredient.ofItems(Items.TOTEM_OF_UNDYING));
		RawShapedRecipe raw = RawShapedRecipe.create(key, Arrays.asList(pattern));
		ShapedRecipe recipe = new ShapedRecipe(
				"",
				CraftingRecipeCategory.MISC,
				raw,
				new ItemStack(ModItems.PURIFICATION_TOTEM));
		return new RecipeEntry<>(id, recipe);
	}

	private static RecipeEntry<ShapelessRecipe> createCompositeElytraRecipe() {
		ShapelessRecipe recipe = new ShapelessRecipe(
				"",
				CraftingRecipeCategory.MISC,
				new ItemStack(ModItems.COMPOSITE_ELYTRA),
				List.of(Ingredient.ofItems(Items.ELYTRA), Ingredient.ofItems(Items.NETHERITE_CHESTPLATE)));
		return new RecipeEntry<>(COMPOSITE_ELYTRA_ID, recipe);
	}

	private static RecipeEntry<ShapedRecipe> createRubberBootsRecipe() {
		Map<Character, Ingredient> key = Map.of(
				'S', Ingredient.ofItems(Items.SLIME_BALL),
				'T', Ingredient.ofItems(Items.STRING));
		RawShapedRecipe raw = RawShapedRecipe.create(key, Arrays.asList("S S", "S S", "TTT"));
		ShapedRecipe recipe = new ShapedRecipe(
				"",
				CraftingRecipeCategory.MISC,
				raw,
				new ItemStack(ModItems.RUBBER_BOOTS));
		return new RecipeEntry<>(RUBBER_BOOTS_ID, recipe);
	}

	private static RecipeEntry<ShapedRecipe> createHeavyPantsRecipe() {
		Map<Character, Ingredient> key = Map.of(
				'C', Ingredient.ofItems(Items.CRYING_OBSIDIAN),
				'N', Ingredient.ofItems(Items.NETHERITE_LEGGINGS));
		RawShapedRecipe raw = RawShapedRecipe.create(key, Arrays.asList("CCC", "CNC", "CCC"));
		ShapedRecipe recipe = new ShapedRecipe(
				"",
				CraftingRecipeCategory.MISC,
				raw,
				new ItemStack(ModItems.HEAVY_PANTS));
		return new RecipeEntry<>(HEAVY_PANTS_ID, recipe);
	}

	private static RecipeEntry<ShapedRecipe> createAugmentedHelmetRecipe() {
		Map<Character, Ingredient> key = Map.of(
				'G', Ingredient.ofItems(Items.GOLD_INGOT),
				'L', Ingredient.ofItems(Items.LEATHER),
				'R', Ingredient.ofItems(Items.REDSTONE),
				'H', Ingredient.ofItems(Items.NETHERITE_HELMET),
				'S', Ingredient.ofItems(Items.STONE),
				'C', Ingredient.ofItems(Items.COMPASS));
		RawShapedRecipe raw = RawShapedRecipe.create(key, Arrays.asList("GLG", "RHR", "SCS"));
		ShapedRecipe recipe = new ShapedRecipe(
				"",
				CraftingRecipeCategory.MISC,
				raw,
				new ItemStack(ModItems.AUGMENTED_HELMET));
		return new RecipeEntry<>(AUGMENTED_HELMET_ID, recipe);
	}
}

