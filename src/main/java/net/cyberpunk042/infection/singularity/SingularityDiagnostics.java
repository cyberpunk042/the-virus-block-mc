package net.cyberpunk042.infection.singularity;

import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.infection.service.ServiceConfig;

/**
 * Central accessors for collapse diagnostics so legacy {@code SingularityConfig}
 * toggles can be retired cleanly.
 */
public final class SingularityDiagnostics {
	private static final ServiceConfig.Diagnostics FALLBACK = new ServiceConfig.Diagnostics();

	private SingularityDiagnostics() {
	}

	private static ServiceConfig.Diagnostics diagnostics() {
		InfectionServiceContainer container = InfectionServices.container();
		if (container == null) {
			return FALLBACK;
		}
		ServiceConfig settings = container.settings();
		if (settings == null) {
			return FALLBACK;
		}
		ServiceConfig.Diagnostics diagnostics = settings.diagnostics;
		return diagnostics != null ? diagnostics : FALLBACK;
	}

	public static boolean enabled() {
		return diagnostics().enabled;
	}

	public static boolean logChunkSamples() {
		ServiceConfig.Diagnostics diagnostics = diagnostics();
		return diagnostics.enabled && diagnostics.logChunkSamples;
	}

	public static boolean logBypasses() {
		ServiceConfig.Diagnostics diagnostics = diagnostics();
		return diagnostics.enabled && diagnostics.logBypasses;
	}

	public static int sampleIntervalTicks() {
		int interval = diagnostics().logSampleIntervalTicks;
		return interval > 0 ? interval : 20;
	}

	public static ServiceConfig.LogSpamSettings logSpamSettings() {
		ServiceConfig.Diagnostics diagnostics = diagnostics();
		ServiceConfig.LogSpamSettings logSpam = diagnostics.logSpam;
		return logSpam != null ? logSpam : new ServiceConfig.LogSpamSettings();
	}
}

