package net.cyberpunk042.status.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public final class PersonalShieldStatusEffect extends StatusEffect {
	public PersonalShieldStatusEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0x7AE4FF);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return false;
	}
}

