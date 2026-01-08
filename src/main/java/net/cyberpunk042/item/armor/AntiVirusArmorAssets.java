package net.cyberpunk042.item.armor;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class AntiVirusArmorAssets {
	public static final RegistryKey<EquipmentAsset> COMPOSITE = key("composite");
	public static final RegistryKey<EquipmentAsset> RUBBER = key("rubber");

	private AntiVirusArmorAssets() {
	}

	public static EquippableComponent build(EquipmentSlot slot, RegistryEntry<SoundEvent> sound, RegistryKey<EquipmentAsset> asset) {
		return EquippableComponent.builder(slot)
				.equipSound(sound)
				.model(asset)
				.build();
	}

	private static RegistryKey<EquipmentAsset> key(String name) {
		return RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(TheVirusBlock.MOD_ID, name));
	}
}

