package net.cyberpunk042.registry;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
	public static final ItemGroup VIRUS_GROUP = Registry.register(
			Registries.ITEM_GROUP,
			Identifier.of(TheVirusBlock.MOD_ID, "core"),
			ItemGroup.create(ItemGroup.Row.TOP, 0)
					.icon(() -> new ItemStack(ModBlocks.VIRUS_BLOCK))
					.displayName(Text.translatable("itemGroup.the-virus-block"))
					.entries((displayContext, entries) -> {
						entries.add(ModBlocks.VIRUS_BLOCK);
						entries.add(ModBlocks.SINGULARITY_BLOCK);
						entries.add(ModBlocks.PROGRESSIVE_GROWTH_BLOCK);
						entries.add(ModBlocks.CORRUPTED_STONE);
						entries.add(ModBlocks.CORRUPTED_GLASS);
						entries.add(ModBlocks.CORRUPTED_DIRT);
						entries.add(ModBlocks.CORRUPTED_SAND);
						entries.add(ModBlocks.CORRUPTED_TNT);
						entries.add(ModBlocks.INFECTED_GRASS);
						entries.add(ModBlocks.CORRUPTED_WOOD);
						entries.add(ModBlocks.CORRUPTED_SNOW_BLOCK);
						entries.add(ModBlocks.CORRUPTED_SNOW);
						entries.add(ModBlocks.CORRUPTED_IRON);
						entries.add(ModBlocks.CORRUPTED_ICE);
						entries.add(ModBlocks.CORRUPTED_PACKED_ICE);
						entries.add(ModBlocks.CORRUPTED_CRYING_OBSIDIAN);
						entries.add(ModBlocks.CORRUPTED_DIAMOND);
						entries.add(ModBlocks.CORRUPTED_GOLD);
						entries.add(ModBlocks.MATRIX_CUBE);
						entries.add(ModBlocks.INFECTED_BLOCK);
						entries.add(ModBlocks.INFECTIOUS_CUBE);
						entries.add(ModBlocks.CURED_INFECTIOUS_CUBE);
						entries.add(ModBlocks.BACTERIA);
						entries.add(ModItems.PURIFICATION_TOTEM);
						entries.add(Items.ELYTRA);
						entries.add(Items.COMPASS);
						entries.add(ModItems.COMPOSITE_ELYTRA);
						entries.add(ModItems.RUBBER_BOOTS);
						entries.add(ModItems.HEAVY_PANTS);
						entries.add(ModItems.AUGMENTED_HELMET);
					})
					.build()
	);

	private ModItemGroups() {
	}

	public static void bootstrap() {
	}
}

