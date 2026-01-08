package net.cyberpunk042.screen;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.screen.handler.PurificationTotemScreenHandler;
import net.cyberpunk042.screen.handler.VirusDifficultyScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
	public static final ScreenHandlerType<PurificationTotemScreenHandler> PURIFICATION_TOTEM =
			Registry.register(Registries.SCREEN_HANDLER, Identifier.of(TheVirusBlock.MOD_ID, "purification_totem"),
					new ScreenHandlerType<>(PurificationTotemScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
	public static final ScreenHandlerType<VirusDifficultyScreenHandler> VIRUS_DIFFICULTY =
			Registry.register(Registries.SCREEN_HANDLER, Identifier.of(TheVirusBlock.MOD_ID, "virus_difficulty"),
					new ScreenHandlerType<>(VirusDifficultyScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

	private ModScreenHandlers() {
	}

	public static void bootstrap() {
		// just loads static fields
	}
}

