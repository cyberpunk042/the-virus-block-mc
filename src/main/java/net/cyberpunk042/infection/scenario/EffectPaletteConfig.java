package net.cyberpunk042.infection.scenario;

import net.minecraft.util.Identifier;

record EffectPaletteConfig(
		Identifier id,
		SimpleEvent coreCharge,
		SimpleEvent coreDetonation,
		SimpleEvent ringCharge,
		RingPulseEvent ringPulse,
		DissipationEvent dissipation,
		CollapseVeilEvent collapseVeil) {

	record SimpleEvent(Particle particle, Sound sound) {
	}

	record Particle(Identifier id) {
	}

	record Sound(Identifier id, String category, float volume, float pitch, int intervalTicks) {
	}

	record RingPulseEvent(Particle primaryParticle,
			Particle secondaryParticle,
			Identifier debrisBlock,
			Sound ambientSound,
			Sound pulseSound) {
	}

	record DissipationEvent(Particle primaryParticle, Particle secondaryParticle, Sound sound) {
	}

	record CollapseVeilEvent(Particle primaryParticle, Particle secondaryParticle, Sound sound) {
	}
}
