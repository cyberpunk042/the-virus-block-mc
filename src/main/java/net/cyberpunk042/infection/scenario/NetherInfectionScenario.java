package net.cyberpunk042.infection.scenario;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.api.VirusWorldContext;
import net.minecraft.util.Identifier;

/**
 * Placeholder Nether scenario that currently mirrors the Overworld behavior.
 * Having a dedicated scenario instance lets us inject Nether-specific effect
 * sets / controllers later without touching {@link net.cyberpunk042.infection.VirusWorldState}.
 */
public final class NetherInfectionScenario extends AbstractDimensionInfectionScenario {
	public static final Identifier ID = Identifier.of(TheVirusBlock.MOD_ID, "nether");

	@Override
	public Identifier id() {
		return ID;
	}

	@Override
	protected ScenarioEffectSet createEffectSet(VirusWorldContext context) {
		Identifier paletteId = context.singularity().profile().effects().effectPalette();
		EffectPaletteConfig palette = EffectPaletteRegistry.resolve(paletteId);
		ScenarioEffectBehavior behavior = ScenarioEffectPalettes.fromPalette(palette, copyEffects(context), copyAudio(context));
		return new ConfiguredScenarioEffectSet(id(), paletteId, behavior);
	}

}

