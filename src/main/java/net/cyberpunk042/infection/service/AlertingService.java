package net.cyberpunk042.infection.service;

import net.cyberpunk042.log.Logging;

/**
 * Central hook for emitting operator-facing alerts. Today it simply proxies to
 * the logging system, but the abstraction makes it easy to add webhooks,
 * Discord, etc. later without touching call sites.
 */
public final class AlertingService {

    public AlertingService() {
    }

    public void dispatch(String message, Object... args) {
        Logging.PROFILER.topic("alerts").warn("[alert] " + message, args);
    }
}
