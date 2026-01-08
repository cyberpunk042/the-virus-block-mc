package net.cyberpunk042.item.armor;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.RepairableComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.sound.SoundEvents;

public class RubberBootsItem extends Item {
	public RubberBootsItem(Item.Settings settings) {
		super(applyProperties(settings));
	}

	private static Item.Settings applyProperties(Item.Settings settings) {
		int baseDurability = EquipmentType.BOOTS.getMaxDamage(ArmorMaterials.LEATHER.durability());
		return settings
				.maxCount(1)
				.armor(ArmorMaterials.LEATHER, EquipmentType.BOOTS)
				.component(DataComponentTypes.EQUIPPABLE, AntiVirusArmorAssets.build(
						EquipmentType.BOOTS.getEquipmentSlot(),
						SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
						AntiVirusArmorAssets.RUBBER))
				.component(DataComponentTypes.MAX_DAMAGE, baseDurability * 2)
				.component(DataComponentTypes.REPAIRABLE,
						new RepairableComponent(RegistryEntryList.of(Items.SLIME_BALL.getRegistryEntry())));
	}
}

