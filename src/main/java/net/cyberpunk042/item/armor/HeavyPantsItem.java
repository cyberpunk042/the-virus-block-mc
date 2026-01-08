package net.cyberpunk042.item.armor;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.RepairableComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.sound.SoundEvents;

public class HeavyPantsItem extends Item {
	public HeavyPantsItem(Item.Settings settings) {
		super(applyProperties(settings));
	}

	private static Item.Settings applyProperties(Item.Settings settings) {
		return settings
				.maxCount(1)
				.fireproof()
				.armor(ArmorMaterials.NETHERITE, EquipmentType.LEGGINGS)
				.component(DataComponentTypes.EQUIPPABLE, AntiVirusArmorAssets.build(
						EquipmentType.LEGGINGS.getEquipmentSlot(),
						SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
						AntiVirusArmorAssets.COMPOSITE))
				.component(DataComponentTypes.REPAIRABLE,
						new RepairableComponent(RegistryEntryList.of(Items.NETHERITE_INGOT.getRegistryEntry())));
	}
}

