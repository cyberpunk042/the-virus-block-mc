package net.cyberpunk042.infection.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe EffectBus implementation backed by a simple map of listeners.
 */
public final class SimpleEffectBus implements EffectBus {
	private final BusTelemetry telemetry;
	private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();

	public SimpleEffectBus() {
		this(null);
	}

	public SimpleEffectBus(BusTelemetry telemetry) {
		this.telemetry = telemetry;
	}

	@Override
	public <T> void register(Class<T> eventType, Consumer<T> handler) {
		listeners.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>()).add(handler);
		if (telemetry != null) {
			telemetry.onRegister(eventType, handler);
		}
	}

	@Override
	public <T> void unregister(Class<T> eventType, Consumer<T> handler) {
		List<Consumer<?>> list = listeners.get(eventType);
		if (list != null) {
			list.remove(handler);
			if (list.isEmpty()) {
				listeners.remove(eventType);
			}
		}
		if (telemetry != null) {
			telemetry.onUnregister(eventType, handler);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> void post(T event) {
		if (event == null) {
			return;
		}
		Class<?> eventClass = event.getClass();
		List<Consumer<?>> handlers = listeners.get(eventClass);
		if (handlers == null || handlers.isEmpty()) {
			return;
		}
		for (Consumer<?> handler : handlers) {
			((Consumer<T>) handler).accept(event);
		}
	}

	public interface BusTelemetry {
		void onRegister(Class<?> eventType, Consumer<?> handler);

		void onUnregister(Class<?> eventType, Consumer<?> handler);
	}
}

