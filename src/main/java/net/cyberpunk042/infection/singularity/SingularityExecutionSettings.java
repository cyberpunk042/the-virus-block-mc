package net.cyberpunk042.infection.singularity;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.minecraft.server.world.ServerWorld;

/**
 * Convenience accessors for execution-related singularity settings.
 * Reads from {@link ServiceConfig} for collapse execution settings.
 */
public final class SingularityExecutionSettings {
	private static final ServiceConfig.SingularityExecution DEFAULT = new ServiceConfig.SingularityExecution();

	private SingularityExecutionSettings() {
	}

	private static ServiceConfig.SingularityExecution snapshot() {
		InfectionServiceContainer services = InfectionServices.container();
		if (services == null) {
			return DEFAULT;
		}
		ServiceConfig settings = services.settings();
		if (settings == null) {
			return DEFAULT;
		}
		ServiceConfig.Singularity singularity = settings.singularity;
		if (singularity == null) {
			return DEFAULT;
		}
		ServiceConfig.SingularityExecution execution = singularity.execution;
		return execution != null ? execution : DEFAULT;
	}

	public static boolean collapseEnabled() {
		return snapshot().collapseEnabled;
	}

	public static boolean collapseEnabled(ServerWorld world) {
		return collapseEnabled()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_COLLAPSE_ENABLED);
	}

	public static boolean allowChunkGeneration() {
		return snapshot().allowChunkGeneration;
	}

	public static boolean allowChunkGeneration(ServerWorld world) {
		return allowChunkGeneration()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION);
	}

	public static boolean allowOutsideBorderLoad() {
		return snapshot().allowOutsideBorderLoad;
	}

	public static boolean allowOutsideBorderLoad(ServerWorld world) {
		return allowOutsideBorderLoad()
				&& world.getGameRules().getBoolean(TheVirusBlock.VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD);
	}
}
