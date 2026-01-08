package net.cyberpunk042.infection.scenario;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.api.VirusWorldContext;
import net.minecraft.util.Identifier;

/**
 * Default scenario bound to the Overworld dimension. Behavior still matches the
 * legacy {@link net.cyberpunk042.infection.VirusWorldState} tick loop, but the
 * scenario now owns the singularity controller so future refactors can swap it
 * without touching the host.
 */
public final class OverworldInfectionScenario extends AbstractDimensionInfectionScenario {
	public static final Identifier ID = Identifier.of(TheVirusBlock.MOD_ID, "overworld");

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

