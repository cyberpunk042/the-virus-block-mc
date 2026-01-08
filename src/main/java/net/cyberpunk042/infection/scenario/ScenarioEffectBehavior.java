package net.cyberpunk042.infection.scenario;

import java.util.function.Consumer;

import net.cyberpunk042.infection.events.CollapseChunkVeilEvent;
import net.cyberpunk042.infection.events.CoreChargeTickEvent;
import net.cyberpunk042.infection.events.CoreDetonationEvent;
import net.cyberpunk042.infection.events.DissipationTickEvent;
import net.cyberpunk042.infection.events.RingChargeTickEvent;
import net.cyberpunk042.infection.events.RingPulseEvent;

record ScenarioEffectBehavior(
		Consumer<CoreChargeTickEvent> coreCharge,
		Consumer<CoreDetonationEvent> coreDetonation,
		Consumer<RingChargeTickEvent> ringCharge,
		Consumer<RingPulseEvent> ringPulse,
		Consumer<DissipationTickEvent> dissipation,
		Consumer<CollapseChunkVeilEvent> collapseVeil) {
}
