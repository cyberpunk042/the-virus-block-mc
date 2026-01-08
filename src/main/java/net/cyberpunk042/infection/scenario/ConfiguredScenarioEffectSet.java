package net.cyberpunk042.infection.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.events.CollapseChunkVeilEvent;
import net.cyberpunk042.infection.events.CoreChargeTickEvent;
import net.cyberpunk042.infection.events.CoreDetonationEvent;
import net.cyberpunk042.infection.events.DissipationTickEvent;
import net.cyberpunk042.infection.events.RingChargeTickEvent;
import net.cyberpunk042.infection.events.RingPulseEvent;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.Identifier;

final class ConfiguredScenarioEffectSet implements ScenarioEffectSet {
	private final Identifier scenarioId;
	private final Identifier paletteId;
	private final ScenarioEffectBehavior behavior;
	private final List<Registration<?>> registrations = new ArrayList<>();
	private EffectBus bus;

	ConfiguredScenarioEffectSet(Identifier scenarioId, Identifier paletteId, ScenarioEffectBehavior behavior) {
		this.scenarioId = scenarioId;
		this.paletteId = paletteId;
		this.behavior = behavior;
	}

	@Override
	public void install(EffectBus bus) {
		this.bus = bus;
		Logging.EFFECTS.info("[scenario:{} palette:{}] install effect-set {}",
				scenarioId, paletteId, behavior.getClass().getSimpleName());
		register(CoreChargeTickEvent.class, behavior.coreCharge());
		register(CoreDetonationEvent.class, behavior.coreDetonation());
		register(RingChargeTickEvent.class, behavior.ringCharge());
		register(RingPulseEvent.class, behavior.ringPulse());
		register(DissipationTickEvent.class, behavior.dissipation());
		register(CollapseChunkVeilEvent.class, behavior.collapseVeil());
	}

	private <T> void register(Class<T> type, Consumer<T> handler) {
		if (handler == null) {
			return;
		}
		bus.register(type, handler);
		registrations.add(new Registration<>(type, handler));
		Logging.EFFECTS.info("[scenario:{} palette:{}] register {}", scenarioId, paletteId, type.getSimpleName());
	}

	@Override
	public void close() {
		for (Registration<?> registration : registrations) {
			unregister(registration);
		}
		registrations.clear();
		Logging.EFFECTS.info("[scenario:{} palette:{}] effect-set closed", scenarioId, paletteId);
	}

	private <T> void unregister(Registration<T> registration) {
		bus.unregister(registration.type(), registration.handler());
	}

	private record Registration<T>(Class<T> type, Consumer<T> handler) {
	}
}

