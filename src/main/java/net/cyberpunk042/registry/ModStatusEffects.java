package net.cyberpunk042.registry;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.status.effect.PersonalShieldStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModStatusEffects {
	public static final RegistryEntry<StatusEffect> PERSONAL_SHIELD = register("personal_shield", new PersonalShieldStatusEffect());

	private ModStatusEffects() {
	}

	public static void bootstrap() {
		// no-op; class load ensures static init
	}

	private static RegistryEntry<StatusEffect> register(String name, StatusEffect effect) {
		Identifier id = Identifier.of(TheVirusBlock.MOD_ID, name);
		Registry.register(Registries.STATUS_EFFECT, id, effect);
		int rawId = Registries.STATUS_EFFECT.getRawId(effect);
		return Registries.STATUS_EFFECT.getEntry(rawId).orElseThrow();
	}
}

