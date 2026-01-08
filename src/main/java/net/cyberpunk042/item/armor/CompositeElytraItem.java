package net.cyberpunk042.item.armor;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.RepairableComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;

public class CompositeElytraItem extends Item {
	public CompositeElytraItem(Item.Settings settings) {
		super(applyProperties(settings));
	}

	private static Item.Settings applyProperties(Item.Settings settings) {
		RegistryEntryList<Item> repairs = RegistryEntryList.of(
				Items.NETHERITE_INGOT.getRegistryEntry(),
				Items.PHANTOM_MEMBRANE.getRegistryEntry());
		return settings
				.maxCount(1)
				.fireproof()
				.armor(ArmorMaterials.NETHERITE, EquipmentType.CHESTPLATE)
				.component(DataComponentTypes.EQUIPPABLE, AntiVirusArmorAssets.build(
						EquipmentType.CHESTPLATE.getEquipmentSlot(),
						SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
						AntiVirusArmorAssets.COMPOSITE))
				.component(DataComponentTypes.GLIDER, Unit.INSTANCE)
				.component(DataComponentTypes.REPAIRABLE, new RepairableComponent(repairs));
	}
}

