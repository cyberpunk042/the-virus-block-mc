package net.cyberpunk042;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.cyberpunk042.command.VirusDifficultyCommand;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.item.PurificationOption;
import net.cyberpunk042.network.PurificationTotemSelectPayload;
import net.cyberpunk042.network.VirusDifficultySelectPayload;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.registry.ModItems;
import net.cyberpunk042.screen.handler.PurificationTotemScreenHandler;
import net.cyberpunk042.screen.handler.VirusDifficultyScreenHandler;
import net.cyberpunk042.util.DelayedServerTasks;
import net.fabricmc.api.ModInitializer;
import net.cyberpunk042.log.Logging;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

public class TheVirusBlock implements ModInitializer {
	public static final String MOD_ID = "the-virus-block";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier SKY_TINT_PACKET = Identifier.of(MOD_ID, "sky_tint");
	public static final Identifier HORIZON_TINT_PACKET = Identifier.of(MOD_ID, "horizon_tint");
	public static final String CORRUPTION_PROJECTILE_TAG = MOD_ID + ".corruption_projectile";
	public static final String CORRUPTION_EXPLOSIVE_TAG = MOD_ID + ".corruption_explosive";
	public static final String VIRUS_DEFENSE_BEAM_TAG = MOD_ID + ".virus_defense_beam";
	private static final String STARTER_TAG = MOD_ID + ".starter_kit";
	private static final String STARTER_TOTEM_TAG = MOD_ID + ".starter_totem";
	public static final Identifier DIFFICULTY_SYNC_PACKET = Identifier.of(MOD_ID, "difficulty_sync");
	public static final Identifier DIFFICULTY_SELECT_PACKET = Identifier.of(MOD_ID, "difficulty_select");
	public static final Identifier VOID_TEAR_SPAWN_PACKET = Identifier.of(MOD_ID, "void_tear_spawn");
	public static final Identifier VOID_TEAR_BURST_PACKET = Identifier.of(MOD_ID, "void_tear_burst");
	public static final Identifier SHIELD_FIELD_SPAWN_PACKET = Identifier.of(MOD_ID, "shield_field_spawn");
	public static final Identifier SHIELD_FIELD_REMOVE_PACKET = Identifier.of(MOD_ID, "shield_field_remove");
	public static final Identifier FIELD_SPAWN_PACKET = Identifier.of(MOD_ID, "field_spawn");
	public static final Identifier FIELD_REMOVE_PACKET = Identifier.of(MOD_ID, "field_remove");
	public static final Identifier FIELD_UPDATE_PACKET = Identifier.of(MOD_ID, "field_update");
	public static final Identifier FIELD_DEFINITION_SYNC_PACKET = Identifier.of(MOD_ID, "field_definition_sync");
	public static final Identifier SINGULARITY_VISUAL_START_PACKET = Identifier.of(MOD_ID, "singularity_visual_start");
	public static final Identifier SINGULARITY_VISUAL_STOP_PACKET = Identifier.of(MOD_ID, "singularity_visual_stop");
	public static final Identifier SINGULARITY_BORDER_PACKET = Identifier.of(MOD_ID, "singularity_border");
	public static final Identifier SINGULARITY_SCHEDULE_PACKET = Identifier.of(MOD_ID, "singularity_schedule");
	public static final Identifier GROWTH_BEAM_PACKET = Identifier.of(MOD_ID, "growth_beam");
	public static final Identifier GROWTH_RING_FIELD_PACKET = Identifier.of(MOD_ID, "growth_ring_field");
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BLOCK_TELEPORT_ENABLED =
			GameRuleRegistry.register("virusBlockTeleportEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BLOCK_TELEPORT_RADIUS =
			GameRuleRegistry.register("virusBlockTeleportRadius", GameRules.Category.MISC, GameRuleFactory.createIntRule(16, 0, 64));
	public static final String CORRUPTED_WORM_TAG = MOD_ID + ":corrupted_worm";
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_PLAYER_RADIUS =
			GameRuleRegistry.register("virusSpreadPlayerRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(48, 8, 256));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_PLAYER_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadPlayerAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(24, 1, 1024));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_SOURCE_RADIUS =
			GameRuleRegistry.register("virusSpreadSourceRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(64, 8, 256));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_SOURCE_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadSourceAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(16, 1, 1024));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_VERTICAL_RADIUS =
			GameRuleRegistry.register("virusSpreadVerticalRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(16, 4, 80));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_INITIAL_RADIUS =
			GameRuleRegistry.register("virusSpreadInitialRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(96, 8, 512));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_INITIAL_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadInitialAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(150, 1, 8192));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SPREAD_CLEANSE_ATTEMPTS =
			GameRuleRegistry.register("virusSpreadCleanseAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(1500, 1, 16384));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_CORRUPTION_PROFILER =
			GameRuleRegistry.register("virusCorruptionProfiler", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MUTATION_ATTEMPTS =
			GameRuleRegistry.register("virusMutationAttemptsPerTick", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(64, 0, 256));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MUTATION_RADIUS =
			GameRuleRegistry.register("virusMutationRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(16, 4, 64));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_CHUNK_REWRITE_ON_LOAD =
			GameRuleRegistry.register("virusCorruptChunksOnLoad", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BOOBYTRAPS_ENABLED =
			GameRuleRegistry.register("virusBoobytrapsEnabled", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_CHANCE_INFECTED =
			GameRuleRegistry.register("virusBoobytrapChanceInfected", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(20, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_CHANCE_INFECTIOUS =
			GameRuleRegistry.register("virusBoobytrapChanceInfectious", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(15, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_CHANCE_BACTERIA =
			GameRuleRegistry.register("virusBoobytrapChanceBacteria", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(10, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_INFECTIOUS_SPREAD_ATTEMPTS =
			GameRuleRegistry.register("virusInfectiousSpreadAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(12, 0, 512));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_INFECTIOUS_SPREAD_RADIUS =
			GameRuleRegistry.register("virusInfectiousSpreadRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(6, 1, 64));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BACTERIA_SPREAD_ATTEMPTS =
			GameRuleRegistry.register("virusBacteriaSpreadAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(10, 0, 512));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BACTERIA_SPREAD_RADIUS =
			GameRuleRegistry.register("virusBacteriaSpreadRadius", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(5, 1, 64));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BACTERIA_PULSE_INTERVAL =
			GameRuleRegistry.register("virusBacteriaPulseInterval", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(80, 10, 400));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_INFECTED_BLOCK_EXPLODE_CHANCE =
			GameRuleRegistry.register("virusInfectedBlockExplodeChance", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(150, 0, 1000));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_CORRUPT_SAND_ENABLED =
			GameRuleRegistry.register("virusCorruptSandEnabled", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_CORRUPT_ICE_ENABLED =
			GameRuleRegistry.register("virusCorruptIceEnabled", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_CORRUPT_SNOW_ENABLED =
			GameRuleRegistry.register("virusCorruptSnowEnabled", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_SURFACE_CORRUPT_ATTEMPTS =
			GameRuleRegistry.register("virusSurfaceCorruptAttempts", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(80, 0, 4096));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_PLAYER_DAMAGE =
			GameRuleRegistry.register("virusBoobytrapPlayerDamage", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(8, 0, 40));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BOOBYTRAP_DAMAGE_PLAYERS_ONLY =
			GameRuleRegistry.register("virusBoobytrapDamagePlayersOnly", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_BOOBYTRAP_DAMAGE_BLOCKS =
			GameRuleRegistry.register("virusBoobytrapDamageBlocks", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_POWER_INFECTED =
			GameRuleRegistry.register("virusBoobytrapPowerInfected", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(4, 0, 10));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_POWER_INFECTIOUS =
			GameRuleRegistry.register("virusBoobytrapPowerInfectious", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(3, 0, 10));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_BOOBYTRAP_POWER_BACTERIA =
			GameRuleRegistry.register("virusBoobytrapPowerBacteria", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(3, 0, 10));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_DAMAGE =
			GameRuleRegistry.register("virusMatrixCubeDamage", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(8, 0, 40));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_MAX_ACTIVE =
			GameRuleRegistry.register("virusMatrixCubeMaxActive", GameRules.Category.MOBS, GameRuleFactory.createIntRule(200, 1, 500));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_SPAWN_INTERVAL =
			GameRuleRegistry.register("virusMatrixCubeSpawnInterval", GameRules.Category.MOBS, GameRuleFactory.createIntRule(40, 20, 6000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_MATRIX_CUBE_SPAWN_ATTEMPTS =
			GameRuleRegistry.register("virusMatrixCubeSpawnAttempts", GameRules.Category.MOBS, GameRuleFactory.createIntRule(20, 1, 200));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_TIER2_EVENTS_ENABLED =
			GameRuleRegistry.register("virusTier2EventsEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_TIER3_EXTRAS_ENABLED =
			GameRuleRegistry.register("virusTier3ExtrasEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_MUTATION_PULSE_ENABLED =
			GameRuleRegistry.register("virusEventMutationPulseEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_SKYFALL_ENABLED =
			GameRuleRegistry.register("virusEventSkyfallEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_COLLAPSE_SURGE_ENABLED =
			GameRuleRegistry.register("virusEventCollapseSurgeEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_PASSIVE_REVOLT_ENABLED =
			GameRuleRegistry.register("virusEventPassiveRevoltEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_MOB_BUFF_STORM_ENABLED =
			GameRuleRegistry.register("virusEventMobBuffStormEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_VIRUS_BLOOM_ENABLED =
			GameRuleRegistry.register("virusEventVirusBloomEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_VOID_TEAR_ENABLED =
			GameRuleRegistry.register("virusEventVoidTearEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_INVERSION_ENABLED =
			GameRuleRegistry.register("virusEventInversionEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_EVENT_ENTITY_DUPLICATION_ENABLED =
			GameRuleRegistry.register("virusEventEntityDuplicationEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_WORMS_ENABLED =
			GameRuleRegistry.register("virusWormsEnabled", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_MOB_FRIENDLY_FIRE =
			GameRuleRegistry.register("virusMobFriendlyFire", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_WORM_SPAWN_CHANCE =
			GameRuleRegistry.register("virusWormSpawnChance", GameRules.Category.MOBS, GameRuleFactory.createIntRule(6, 0, 1000));
	public static final GameRules.Key<GameRules.IntRule> VIRUS_WORM_TRAP_SPAWN_CHANCE =
			GameRuleRegistry.register("virusWormTrapSpawnChance", GameRules.Category.MOBS, GameRuleFactory.createIntRule(135, 0, 1000));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_LIQUID_MUTATION_ENABLED =
			GameRuleRegistry.register("virusLiquidMutationEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_DIFFICULTY_LOCKED =
			GameRuleRegistry.register("virusDifficultyLocked", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_ALLOW_FAST_FLIGHT =
			GameRuleRegistry.register("virusAllowFastFlight", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_VERBOSE_INVENTORY_ALERTS =
			GameRuleRegistry.register("virusVerboseInventoryAlerts", GameRules.Category.CHAT, GameRuleFactory.createBooleanRule(true));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_SINGULARITY_ALLOW_CHUNK_GENERATION =
			GameRuleRegistry.register("virusSingularityAllowChunkGeneration", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_SINGULARITY_ALLOW_OUTSIDE_BORDER_LOAD =
			GameRuleRegistry.register("virusSingularityAllowOutsideBorderLoad", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
	public static final GameRules.Key<GameRules.BooleanRule> VIRUS_SINGULARITY_COLLAPSE_ENABLED =
			GameRuleRegistry.register("virusSingularityCollapseEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

	@Override
	public void onInitialize() {
		// Wire State store to Init framework for observable progress
		net.cyberpunk042.state.StateInit.connect();
		
		// Execute staged initialization
		net.cyberpunk042.init.Init.orchestrator()
			// Stage 1: Core systems (no dependencies)
			.stage(net.cyberpunk042.init.InitStage.of("core", "Core Systems")
				.add(net.cyberpunk042.init.nodes.CoreNodes.SERVER_REF)
				.add(net.cyberpunk042.init.nodes.CoreNodes.CONFIG)
				.add(net.cyberpunk042.init.nodes.CoreNodes.LOGGING)
				.add(net.cyberpunk042.init.nodes.CoreNodes.COMMANDS))
			
			// Stage 2: Registries (blocks, items, entities, etc.)
			.stage(net.cyberpunk042.init.InitStage.of("registry", "Registries")
				.add(net.cyberpunk042.init.nodes.RegistryNodes.BLOCKS)
				.add(net.cyberpunk042.init.nodes.RegistryNodes.ITEMS)
				.add(net.cyberpunk042.init.nodes.RegistryNodes.BLOCK_ENTITIES)
				.add(net.cyberpunk042.init.nodes.RegistryNodes.ENTITIES)
				.add(net.cyberpunk042.init.nodes.RegistryNodes.EFFECTS)
				.add(net.cyberpunk042.init.nodes.RegistryNodes.ITEM_GROUPS)
				.add(net.cyberpunk042.init.nodes.RegistryNodes.SCREEN_HANDLERS))
			
			// Stage 3: Network (payloads + handlers)
			.stage(net.cyberpunk042.init.InitStage.of("network", "Networking")
				.add(net.cyberpunk042.init.nodes.NetworkNodes.S2C_PAYLOADS)
				.add(net.cyberpunk042.init.nodes.NetworkNodes.C2S_PAYLOADS)
				.add(net.cyberpunk042.init.nodes.NetworkNodes.GUI_PAYLOADS)
				.add(net.cyberpunk042.init.nodes.NetworkNodes.SERVER_HANDLERS))
			
			// Stage 4: Commands
			.stage(net.cyberpunk042.init.InitStage.of("commands", "Commands")
				.add(net.cyberpunk042.init.nodes.CommandNodes.LOG_COMMANDS)
				.add(net.cyberpunk042.init.nodes.CommandNodes.DEBUG_COMMANDS)
				.add(net.cyberpunk042.init.nodes.CommandNodes.GROWTH_COMMANDS))
			
			// Stage 5: Field system
			.stage(net.cyberpunk042.init.InitStage.of("field", "Field System")
				.add(net.cyberpunk042.init.nodes.FieldNodes.GROWTH_SCHEDULER)
				.add(net.cyberpunk042.init.nodes.FieldNodes.FIELD_COMMANDS)
				.add(net.cyberpunk042.init.nodes.FieldNodes.FIELD_RELOAD_LISTENER)
				.add(net.cyberpunk042.init.nodes.FieldNodes.FIELD_MANAGER_WIRING)
				.add(net.cyberpunk042.init.nodes.FieldNodes.FIELD_PLAYER_SYNC)
				.add(net.cyberpunk042.init.nodes.FieldNodes.COLOR_THEMES)
				.add(net.cyberpunk042.init.nodes.FieldNodes.FIELD_REGISTRY))
			
			// Stage 6: Infection system
			.stage(net.cyberpunk042.init.InitStage.of("infection", "Infection System")
				.add(net.cyberpunk042.init.nodes.InfectionNodes.BLOCK_PROTECTION)
				.add(net.cyberpunk042.init.nodes.InfectionNodes.TIER_BOSS_BAR)
				.add(net.cyberpunk042.init.nodes.InfectionNodes.INVENTORY_ANNOUNCEMENTS)
				.add(net.cyberpunk042.init.nodes.InfectionNodes.TERRAIN_CORRUPTION)
				.add(net.cyberpunk042.init.nodes.InfectionNodes.ITEM_ALERTS)
				.add(net.cyberpunk042.init.nodes.InfectionNodes.DELAYED_TASKS)
				.add(net.cyberpunk042.init.nodes.InfectionNodes.VIRUS_COMMANDS)
				.add(net.cyberpunk042.init.nodes.InfectionNodes.VIRUS_WORLD_STATE))
			
			.execute();
		
		// Register mod-specific server handlers (not in generic nodes)
		ServerPlayNetworking.registerGlobalReceiver(PurificationTotemSelectPayload.ID, (payload, context) ->
				context.player().getServer().execute(() -> handlePurificationSelection(context.player(), payload)));
		ServerPlayNetworking.registerGlobalReceiver(VirusDifficultySelectPayload.ID, (payload, context) ->
				context.player().getServer().execute(() -> handleDifficultySelection(context.player(), payload)));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				server.execute(() -> handlePlayerJoin(handler.player)));

		Logging.CONFIG.info("The Virus Block is primed. Containment is impossible.");
	}

	private static void handlePurificationSelection(ServerPlayerEntity player, PurificationTotemSelectPayload payload) {
		if (!(player.currentScreenHandler instanceof PurificationTotemScreenHandler handler)) {
			return;
		}
		if (handler.syncId != payload.syncId()) {
			return;
		}
		ServerWorld world = player.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		if (!state.infectionState().infected()) {
			player.sendMessage(Text.translatable("message.the-virus-block.purification_totem.inactive"), true);
			player.closeHandledScreen();
			return;
		}
		if (state.infectionState().dormant() && payload.option() == PurificationOption.NO_BOOBYTRAPS) {
			player.sendMessage(Text.translatable("message.the-virus-block.purification_totem.boobytraps_disabled"), true);
			player.closeHandledScreen();
			return;
		}
		ItemStack stack = player.getStackInHand(handler.getHand());
		if (!player.getAbilities().creativeMode) {
			if (!stack.isOf(ModItems.PURIFICATION_TOTEM)) {
				player.sendMessage(Text.translatable("message.the-virus-block.purification_totem.missing"), true);
				player.closeHandledScreen();
				return;
			}
			stack.decrement(1);
		}
		payload.option().apply(world, player, state);
		player.closeHandledScreen();
	}

	private static void handleDifficultySelection(ServerPlayerEntity player, VirusDifficultySelectPayload payload) {
		if (!(player.currentScreenHandler instanceof VirusDifficultyScreenHandler handler)) {
			return;
		}
		if (handler.syncId != payload.syncId()) {
			return;
		}
		ServerWorld world = player.getWorld();
		if (world.getGameRules().getBoolean(VIRUS_DIFFICULTY_LOCKED) && !player.hasPermissionLevel(2)) {
			player.sendMessage(Text.translatable("message.the-virus-block.difficulty.locked"), true);
			player.closeHandledScreen();
			return;
		}
		VirusWorldState state = VirusWorldState.get(world);
		state.setDifficulty(world, payload.difficulty());
		player.closeHandledScreen();
		player.sendMessage(Text.translatable("message.the-virus-block.difficulty.set", payload.difficulty().getDisplayName()), true);
		if (payload.difficulty() == VirusDifficulty.EASY || payload.difficulty() == VirusDifficulty.MEDIUM) {
			grantPurificationTotem(player);
		}
	}

	private static void handlePlayerJoin(ServerPlayerEntity player) {
		ServerWorld world = player.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		warnIfInfected(player, state);
		state.shieldFieldService().sendSnapshots(player);
		state.presentationCoord().syncProfileOnJoin(player);
		grantStarterKit(player);
		ensureFastFlight(player);
		maybePromptDifficulty(player);
		
		// Send shader time sync for multiplayer animation synchronization
		ServerPlayNetworking.send(player, net.cyberpunk042.network.shader.ShaderTimeSyncS2CPayload.now());
	}

	private static void grantStarterKit(ServerPlayerEntity player) {
		if (player.getCommandTags().contains(STARTER_TAG)) {
			return;
		}
		ItemStack stack = new ItemStack(ModBlocks.VIRUS_BLOCK.asItem());
		if (!player.giveItemStack(stack.copy())) {
			player.dropItem(stack, false);
		}
		player.addCommandTag(STARTER_TAG);
		player.sendMessage(Text.translatable("message.the-virus-block.starter.granted"), false);
		grantPurificationTotem(player);
	}

	private static void grantPurificationTotem(ServerPlayerEntity player) {
		if (player.getCommandTags().contains(STARTER_TOTEM_TAG)) {
			return;
		}
		ServerWorld world = player.getWorld();
		VirusDifficulty difficulty = VirusWorldState.get(world).tiers().difficulty();
		if (difficulty != VirusDifficulty.EASY && difficulty != VirusDifficulty.MEDIUM) {
			return;
		}
		ItemStack totem = new ItemStack(ModItems.PURIFICATION_TOTEM);
		if (!player.giveItemStack(totem.copy())) {
			player.dropItem(totem, false);
		}
		player.addCommandTag(STARTER_TOTEM_TAG);
		player.sendMessage(Text.translatable("message.the-virus-block.starter.totem"), false);
	}

	private static void maybePromptDifficulty(ServerPlayerEntity player) {
		ServerWorld world = player.getWorld();
		VirusWorldState state = VirusWorldState.get(world);
		if (state.tiers().hasShownDifficultyPrompt()) {
			return;
		}
		var server = player.getServer();
		if (server == null) {
			return;
		}
		DelayedServerTasks.schedule(server, 100, () -> {
			if (player.isDisconnected() || player.isRemoved()) {
				return;
			}
			showCenteredMessage(player, Text.literal("Virus Block Mod Detected..").formatted(net.minecraft.util.Formatting.DARK_PURPLE), 10, 40, 10);
			player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8F, 1.0F);
			DelayedServerTasks.schedule(server, 40, () -> {
				if (player.isDisconnected() || player.isRemoved()) {
					return;
				}
				showCenteredMessage(player, Text.literal("please be careful ..."), 5, 30, 5);
				DelayedServerTasks.schedule(server, 50, () -> {
					if (player.isDisconnected() || player.isRemoved()) {
						return;
					}
					if (VirusDifficultyCommand.openMenuFor(player)) {
						if (state.tiers().markDifficultyPromptShown()) {
							state.markDirty();
						}
					}
				});
			});
		});
	}

	private static void ensureFastFlight(ServerPlayerEntity player) {
		ServerWorld world = player.getWorld();
		if (!world.getGameRules().getBoolean(VIRUS_ALLOW_FAST_FLIGHT)) {
			return;
		}
		var server = player.getServer();
		if (server == null) {
			return;
		}
		GameRules.BooleanRule elytraRule = world.getGameRules().get(GameRules.DISABLE_ELYTRA_MOVEMENT_CHECK);
		if (!elytraRule.get()) {
			elytraRule.set(true, server);
		}
		GameRules.BooleanRule movementRule = world.getGameRules().get(GameRules.DISABLE_PLAYER_MOVEMENT_CHECK);
		if (!movementRule.get()) {
			movementRule.set(true, server);
		}
	}

	private static void showCenteredMessage(ServerPlayerEntity player, Text text, int fadeIn, int stay, int fadeOut) {
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
		player.networkHandler.sendPacket(new TitleS2CPacket(text));
	}

	private static void showActionBar(ServerPlayerEntity player, Text text) {
		player.sendMessage(text, true);
	}

	private static void warnIfInfected(ServerPlayerEntity player, VirusWorldState state) {
		if (!state.infectionState().infected()) {
			return;
		}
		showActionBar(player, Text.translatable("message.the-virus-block.infection.warning")
				.formatted(Formatting.DARK_RED));
	}
}