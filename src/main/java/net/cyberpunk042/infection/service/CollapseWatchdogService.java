package net.cyberpunk042.infection.service;

import java.util.Objects;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;

/**
 * Centralizes collapse watchdog plumbing (scheduler/fuse/collapse watchdogs) and
 * diagnostic helpers such as telemetry/presentation fallbacks.
 */
public final class CollapseWatchdogService {

    private static final SingularityTelemetryService FALLBACK_TELEMETRY = new SingularityTelemetryService();

    private final SingularityWatchdogController watchdogController = new SingularityWatchdogController();
    private final SingularityPresentationService presentationFallback = new SingularityPresentationService();

    public CollapseWatchdogService(VirusWorldState host) {
        Objects.requireNonNull(host, "host");
    }

    public SingularityWatchdogController controller() {
        return watchdogController;
    }

    public void resetCollapseStallTicks() {
        watchdogController.resetCollapseStallTicks();
    }

    public int collapseStallTicks() {
        return watchdogController.collapseStallTicks();
    }

    public int diagnosticsSampleInterval() {
        return SingularityDiagnostics.sampleIntervalTicks();
    }

    public SingularityPresentationService presentationService() {
        InfectionServiceContainer c = InfectionServices.container();
        return c != null ? c.presentation() : presentationFallback;
    }

    public SingularityTelemetryService telemetryService() {
        InfectionServiceContainer c = InfectionServices.container();
        return c != null ? c.telemetry() : FALLBACK_TELEMETRY;
    }
}
