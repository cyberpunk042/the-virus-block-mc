package net.cyberpunk042.config;

import net.cyberpunk042.infection.service.InfectionServices;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Forces static initialization of config modules before the registry attempts
 * to load them. This approach keeps config classes self-contained while still
 * providing a central hook for mod/bootstrap entry points.
 */
public final class ModConfigBootstrap {
	private static boolean commonPrepared;
	private static boolean clientPrepared;

	private ModConfigBootstrap() {
	}

	public static synchronized void prepareCommon() {
		if (commonPrepared) {
			return;
		}
		commonPrepared = true;
		InfectionServices.initialize(FabricLoader.getInstance().getConfigDir().resolve("the-virus-block"));
		initialize(SingularityConfig.class);
	}

	public static synchronized void prepareClient() {
		if (clientPrepared) {
			return;
		}
		clientPrepared = true;
		prepareCommon();
		initialize(ColorConfig.class);
	}

	private static void initialize(Class<?> clazz) {
		try {
			Class.forName(clazz.getName(), true, clazz.getClassLoader());
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Unable to initialize config class " + clazz.getName(), ex);
		}
	}
}

