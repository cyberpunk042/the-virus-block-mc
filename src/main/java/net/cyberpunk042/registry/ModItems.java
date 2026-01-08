package net.cyberpunk042.registry;

import java.util.function.Function;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.item.PurificationTotemItem;
import net.cyberpunk042.item.armor.AugmentedHelmetItem;
import net.cyberpunk042.item.armor.CompositeElytraItem;
import net.cyberpunk042.item.armor.HeavyPantsItem;
import net.cyberpunk042.item.armor.RubberBootsItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModItems {
	public static final Item PURIFICATION_TOTEM = register("purification_totem",
			settings -> new PurificationTotemItem(settings.maxCount(1)));
	public static final Item COMPOSITE_ELYTRA = register("composite_elytra",
			settings -> new CompositeElytraItem(settings.maxCount(1)));
	public static final Item RUBBER_BOOTS = register("rubber_boots",
			settings -> new RubberBootsItem(settings.maxCount(1)));
	public static final Item HEAVY_PANTS = register("heavy_pants",
			settings -> new HeavyPantsItem(settings.maxCount(1)));
	public static final Item AUGMENTED_HELMET = register("augmented_helmet",
			settings -> new AugmentedHelmetItem(settings.maxCount(1)));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Settings, Item> factory) {
		Identifier id = Identifier.of(TheVirusBlock.MOD_ID, name);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
		Item item = factory.apply(new Item.Settings().registryKey(key));
		return Registry.register(Registries.ITEM, key, item);
	}

	public static void bootstrap() {
		// intentionally empty
	}
}

