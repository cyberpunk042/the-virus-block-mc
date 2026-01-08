package net.cyberpunk042.command;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.command.util.CommandProtection;
import net.cyberpunk042.command.util.CommandTargetResolver;
import net.cyberpunk042.command.util.FieldCommandBuilder;
import net.cyberpunk042.command.util.MutationParser;
import net.cyberpunk042.command.util.RegistrySuggester;
import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.growth.scheduler.GrowthField;
import net.cyberpunk042.growth.scheduler.GrowthMutation;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Commands for managing progressive growth blocks.
 * Uses extracted utilities from command.util package.
 */
public final class GrowthBlockCommands {

    // === Field Specifications ===
    
    private static final Map<String, GrowthField> FIELD_LOOKUP = Arrays.stream(GrowthField.values())
            .collect(Collectors.toMap(field -> field.name().toLowerCase(Locale.ROOT), field -> field));

    private static final Map<GrowthField, String> JSON_KEY_MAP = buildJsonKeyMap();

    private static final List<FieldCommandBuilder.ProfileSpec<GrowthField, GrowthRegistry>> PROFILE_SPECS = List.of(
            new FieldCommandBuilder.ProfileSpec<>("growth", GrowthField.GROWTH_PROFILE, GrowthRegistry::growthProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("glow", GrowthField.GLOW_PROFILE, GrowthRegistry::glowProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("particle", GrowthField.PARTICLE_PROFILE, GrowthRegistry::particleProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("field", GrowthField.FIELD_PROFILE, GrowthRegistry::fieldProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("pull", GrowthField.PULL_PROFILE, GrowthRegistry::forceProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("push", GrowthField.PUSH_PROFILE, GrowthRegistry::forceProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("fuse", GrowthField.FUSE_PROFILE, GrowthRegistry::fuseProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("explosion", GrowthField.EXPLOSION_PROFILE, GrowthRegistry::explosionProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("opacity", GrowthField.OPACITY_PROFILE, GrowthRegistry::opacityProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("spin", GrowthField.SPIN_PROFILE, GrowthRegistry::spinProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("wobble", GrowthField.WOBBLE_PROFILE, GrowthRegistry::wobbleProfileIds),
            new FieldCommandBuilder.ProfileSpec<>("palette", GrowthField.PALETTE, registry -> Collections.emptyList())
    );

    private static final List<GrowthField> BOOLEAN_FIELDS = List.of(
            GrowthField.GROWTH_ENABLED,
            GrowthField.HAS_COLLISION,
            GrowthField.DOES_DESTRUCTION,
            GrowthField.HAS_FUSE);

    private static final List<GrowthField> INT_FIELDS = List.of(GrowthField.RATE_TICKS);

    private static final List<GrowthField> DOUBLE_FIELDS = List.of(
            GrowthField.RATE_SCALE,
            GrowthField.START_SCALE,
            GrowthField.TARGET_SCALE,
            GrowthField.MIN_SCALE,
            GrowthField.MAX_SCALE,
            GrowthField.TOUCH_DAMAGE);

    // === Utilities ===

    private static final RegistrySuggester<GrowthRegistry> SUGGESTER = new RegistrySuggester<>(() -> {
        InfectionServiceContainer c = InfectionServices.get();
        return c != null ? c.growth() : null;
    });

    private static final MutationParser<GrowthField, GrowthMutation> PARSER = new MutationParser<>(
            FIELD_LOOKUP,
            field -> mapFieldType(field.type()),
            GrowthMutation::new,
            (mutation, field, value, cleared) -> {
                if (cleared) {
                    mutation.clear(field);
                } else {
                    switch (field.type()) {
                        case BOOLEAN -> mutation.setBoolean(field, (Boolean) value);
                        case INT -> mutation.setInt(field, (Integer) value);
                        case DOUBLE -> mutation.setDouble(field, (Double) value);
                        case IDENTIFIER -> mutation.setIdentifier(field, (Identifier) value);
                    }
                }
            }
    );

    private static final FieldCommandBuilder<GrowthField, GrowthRegistry> FIELD_BUILDER = new FieldCommandBuilder<>(SUGGESTER);

    private GrowthBlockCommands() {}

    // === Registration ===

    public static void register() {
        CommandRegistrationCallback.EVENT.register(GrowthBlockCommands::registerInternal);
    }

    private static void registerInternal(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment env) {
        dispatcher.register(literal("growthblock")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("reload").executes(GrowthBlockCommands::reloadConfigs))
                .then(literal("list").executes(GrowthBlockCommands::listDefinitions))
                .then(buildApplyLiteral())
                .then(buildScheduleLiteral())
                .then(buildInspectLiteral())
                .then(buildDefaultsLiteral())
                .then(buildGiveLiteral()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> literal(String name) {
        return CommandManager.literal(name);
    }

    // === Command Builders ===

    private static LiteralArgumentBuilder<ServerCommandSource> buildApplyLiteral() {
        LiteralArgumentBuilder<ServerCommandSource> apply = literal("apply");
        
        // Attach profile and value sub-commands for auto target
        attachFieldCommands(apply, ApplyTarget.AUTO);
        
        // Block target
        LiteralArgumentBuilder<ServerCommandSource> blockLiteral = literal("block");
        attachFieldCommands(blockLiteral, ApplyTarget.BLOCK);
        blockLiteral.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
                .executes(ctx -> applyToBlock(ctx, StringArgumentType.getString(ctx, "mutations"))));
        apply.then(blockLiteral);

        // Hand target
        LiteralArgumentBuilder<ServerCommandSource> handLiteral = literal("hand");
        attachFieldCommands(handLiteral, ApplyTarget.HAND);
        handLiteral.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
                .executes(ctx -> applyToHand(ctx, StringArgumentType.getString(ctx, "mutations"))));
        apply.then(handLiteral);

        // Greedy string for auto
        apply.then(CommandManager.argument("mutations", StringArgumentType.greedyString())
                .executes(ctx -> applyAuto(ctx, StringArgumentType.getString(ctx, "mutations"))));
        
        return apply;
    }

    private static void attachFieldCommands(LiteralArgumentBuilder<ServerCommandSource> parent, ApplyTarget target) {
        FIELD_BUILDER.attachProfileCommands(parent, "profile", PROFILE_SPECS, 
                (ctx, field, value) -> setField(ctx, target, field, value));
        FIELD_BUILDER.attachValueCommands(parent, "value", BOOLEAN_FIELDS, INT_FIELDS, DOUBLE_FIELDS,
                f -> f.name().toLowerCase(Locale.ROOT),
                (ctx, field, value) -> setField(ctx, target, field, value));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildScheduleLiteral() {
        return literal("schedule")
                .then(literal("block")
                        .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                .then(CommandManager.argument("mutations", StringArgumentType.greedyString())
                                        .executes(ctx -> scheduleBlock(ctx,
                                                IntegerArgumentType.getInteger(ctx, "delay"),
                                                StringArgumentType.getString(ctx, "mutations"))))))
                .then(literal("pos")
                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                .then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
                                        .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                                .then(CommandManager.argument("mutations", StringArgumentType.greedyString())
                                                        .executes(ctx -> scheduleAtPosition(ctx,
                                                                DimensionArgumentType.getDimensionArgument(ctx, "dimension"),
                                                                BlockPosArgumentType.getLoadedBlockPos(ctx, "position"),
                                                                IntegerArgumentType.getInteger(ctx, "delay"),
                                                                StringArgumentType.getString(ctx, "mutations"))))))))
                .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                        .then(CommandManager.argument("mutations", StringArgumentType.greedyString())
                                .executes(ctx -> scheduleAuto(ctx,
                                        IntegerArgumentType.getInteger(ctx, "delay"),
                                        StringArgumentType.getString(ctx, "mutations")))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildInspectLiteral() {
        return literal("inspect")
                .then(literal("block").executes(GrowthBlockCommands::inspectBlock))
                .then(literal("hand").executes(GrowthBlockCommands::inspectHand))
                .executes(GrowthBlockCommands::inspectAuto);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildDefaultsLiteral() {
        return literal("defaults")
                .then(literal("show")
                        .then(CommandManager.argument("definition", IdentifierArgumentType.identifier())
                                .suggests((ctx, builder) -> SUGGESTER.suggest(ctx, builder, GrowthRegistry::definitionIds))
                                .executes(ctx -> showDefaults(ctx, IdentifierArgumentType.getIdentifier(ctx, "definition")))))
                .then(literal("set")
                        .then(CommandManager.argument("definition", IdentifierArgumentType.identifier())
                                .suggests((ctx, builder) -> SUGGESTER.suggest(ctx, builder, GrowthRegistry::definitionIds))
                                .then(CommandManager.argument("mutations", StringArgumentType.greedyString())
                                        .executes(ctx -> setDefaults(ctx,
                                                IdentifierArgumentType.getIdentifier(ctx, "definition"),
                                                StringArgumentType.getString(ctx, "mutations"))))))
                .then(literal("reload").executes(GrowthBlockCommands::reloadConfigs));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildGiveLiteral() {
        return literal("give")
                .then(CommandManager.argument("definition", IdentifierArgumentType.identifier())
                        .suggests((ctx, builder) -> SUGGESTER.suggest(ctx, builder, GrowthRegistry::definitionIds))
                        .executes(ctx -> giveToSource(ctx, IdentifierArgumentType.getIdentifier(ctx, "definition"), 1))
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> giveToSource(ctx, IdentifierArgumentType.getIdentifier(ctx, "definition"),
                                        IntegerArgumentType.getInteger(ctx, "count"))))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(ctx -> giveToPlayer(ctx,
                                        EntityArgumentType.getPlayer(ctx, "target"),
                                        IdentifierArgumentType.getIdentifier(ctx, "definition"),
                                        1))
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> giveToPlayer(ctx,
                                                EntityArgumentType.getPlayer(ctx, "target"),
                                                IdentifierArgumentType.getIdentifier(ctx, "definition"),
                                                IntegerArgumentType.getInteger(ctx, "count"))))));
    }

    // === Command Handlers ===

    private static int reloadConfigs(CommandContext<ServerCommandSource> ctx) {
        if (!CommandProtection.checkAndWarn(ctx.getSource(), "growth.reload")) {
            return 0;
        }
        InfectionServices.reload();
        ctx.getSource().sendFeedback(() -> Text.literal("Growth configs reloaded."), true);
        return 1;
    }

    private static int listDefinitions(CommandContext<ServerCommandSource> ctx) {
        GrowthRegistry registry = InfectionServices.get().growth();
        ctx.getSource().sendFeedback(() -> Text.literal("Loaded definitions: " + registry.definitionIds()), false);
        return registry.definitionIds().size();
    }

    private static int setField(CommandContext<ServerCommandSource> ctx, ApplyTarget target, GrowthField field, Object value) throws CommandSyntaxException {
        var parsed = PARSER.singleField(field, value);
        return applyParsed(ctx, target, parsed);
    }

    private static int applyAuto(CommandContext<ServerCommandSource> ctx, String raw) throws CommandSyntaxException {
        var parsed = PARSER.parse(raw);
        return GrowthCommandHandlers.applyAuto(ctx, parsed);
    }

    private static int applyToBlock(CommandContext<ServerCommandSource> ctx, String raw) throws CommandSyntaxException {
        var parsed = PARSER.parse(raw);
        ProgressiveGrowthBlockEntity block = GrowthCommandHandlers.requireGrowthBlock(ctx.getSource());
        return GrowthCommandHandlers.applyMutationToBlock(ctx, block, parsed);
    }

    private static int applyToHand(CommandContext<ServerCommandSource> ctx, String raw) throws CommandSyntaxException {
        var parsed = PARSER.parse(raw);
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        CommandTargetResolver.StackTarget target = GrowthCommandHandlers.findHeldGrowthBlock(player);
        if (target == null) {
            throw GrowthCommandHandlers.NO_GROWTH_STACK.create();
        }
        return GrowthCommandHandlers.applyMutationToStack(ctx, target, parsed);
    }

    private static int applyParsed(CommandContext<ServerCommandSource> ctx, ApplyTarget target, MutationParser.ParsedMutation<GrowthField, GrowthMutation> parsed) throws CommandSyntaxException {
        return switch (target) {
            case BLOCK -> {
                ProgressiveGrowthBlockEntity block = GrowthCommandHandlers.requireGrowthBlock(ctx.getSource());
                yield GrowthCommandHandlers.applyMutationToBlock(ctx, block, parsed);
            }
            case HAND -> {
                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                CommandTargetResolver.StackTarget stack = GrowthCommandHandlers.findHeldGrowthBlock(player);
                if (stack == null) throw GrowthCommandHandlers.NO_GROWTH_STACK.create();
                yield GrowthCommandHandlers.applyMutationToStack(ctx, stack, parsed);
            }
            case AUTO -> GrowthCommandHandlers.applyAuto(ctx, parsed);
        };
    }

    private static int inspectAuto(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return GrowthCommandHandlers.inspectAuto(ctx);
    }

    private static int inspectBlock(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ProgressiveGrowthBlockEntity block = GrowthCommandHandlers.requireGrowthBlock(ctx.getSource());
        return GrowthCommandHandlers.inspectBlock(ctx, block);
    }

    private static int inspectHand(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        CommandTargetResolver.StackTarget target = GrowthCommandHandlers.findHeldGrowthBlock(player);
        if (target == null) throw GrowthCommandHandlers.NO_GROWTH_STACK.create();
        return GrowthCommandHandlers.inspectHand(ctx, target);
    }

    private static int scheduleAuto(CommandContext<ServerCommandSource> ctx, int delay, String raw) throws CommandSyntaxException {
        var parsed = PARSER.parse(raw);
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ProgressiveGrowthBlockEntity block = GrowthCommandHandlers.traceGrowthBlock(player);
        if (block == null) throw GrowthCommandHandlers.NO_BLOCK_TARGET.create();
        return GrowthCommandHandlers.scheduleOnWorld(ctx, (ServerWorld) block.getWorld(), block.getPos(), delay, parsed);
    }

    private static int scheduleBlock(CommandContext<ServerCommandSource> ctx, int delay, String raw) throws CommandSyntaxException {
        var parsed = PARSER.parse(raw);
        ProgressiveGrowthBlockEntity block = GrowthCommandHandlers.requireGrowthBlock(ctx.getSource());
        return GrowthCommandHandlers.scheduleOnWorld(ctx, (ServerWorld) block.getWorld(), block.getPos(), delay, parsed);
    }

    private static int scheduleAtPosition(CommandContext<ServerCommandSource> ctx, ServerWorld world, BlockPos pos, int delay, String raw) throws CommandSyntaxException {
        var parsed = PARSER.parse(raw);
        return GrowthCommandHandlers.scheduleOnWorld(ctx, world, pos, delay, parsed);
    }

    private static int showDefaults(CommandContext<ServerCommandSource> ctx, Identifier definitionId) throws CommandSyntaxException {
        Path configRoot = InfectionServices.get().config().root();
        return GrowthCommandHandlers.showDefaults(ctx, definitionId, configRoot);
    }

    private static int setDefaults(CommandContext<ServerCommandSource> ctx, Identifier definitionId, String raw) throws CommandSyntaxException {
        var parsed = PARSER.parse(raw);
        Path configRoot = InfectionServices.get().config().root();
        return GrowthCommandHandlers.setDefaults(ctx, definitionId, parsed, JSON_KEY_MAP, configRoot);
    }

    private static int giveToSource(CommandContext<ServerCommandSource> ctx, Identifier definitionId, int count) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return GrowthCommandHandlers.giveToPlayer(ctx, player, definitionId, count);
    }

    private static int giveToPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, Identifier definitionId, int count) throws CommandSyntaxException {
        return GrowthCommandHandlers.giveToPlayer(ctx, target, definitionId, count);
    }

    // === Helpers ===

    private static Map<GrowthField, String> buildJsonKeyMap() {
        EnumMap<GrowthField, String> map = new EnumMap<>(GrowthField.class);
        map.put(GrowthField.GROWTH_ENABLED, "growth_enabled");
        map.put(GrowthField.RATE_TICKS, "rate");
        map.put(GrowthField.RATE_SCALE, "scale_by_rate");
        map.put(GrowthField.START_SCALE, "start");
        map.put(GrowthField.TARGET_SCALE, "target");
        map.put(GrowthField.MIN_SCALE, "min");
        map.put(GrowthField.MAX_SCALE, "max");
        map.put(GrowthField.HAS_COLLISION, "has_collision");
        map.put(GrowthField.DOES_DESTRUCTION, "does_destruction");
        map.put(GrowthField.HAS_FUSE, "has_fuse");
        map.put(GrowthField.TOUCH_DAMAGE, "touch_damage");
        map.put(GrowthField.GROWTH_PROFILE, "growth_profile");
        map.put(GrowthField.GLOW_PROFILE, "glow_profile");
        map.put(GrowthField.PARTICLE_PROFILE, "particle_profile");
        map.put(GrowthField.FIELD_PROFILE, "field_profile");
        map.put(GrowthField.PULL_PROFILE, "pull_profile");
        map.put(GrowthField.PUSH_PROFILE, "push_profile");
        map.put(GrowthField.FUSE_PROFILE, "fuse_profile");
        map.put(GrowthField.EXPLOSION_PROFILE, "explosion_profile");
        map.put(GrowthField.OPACITY_PROFILE, "opacity_profile");
        map.put(GrowthField.SPIN_PROFILE, "spin_profile");
        map.put(GrowthField.PALETTE, "palette");
        map.put(GrowthField.WOBBLE_PROFILE, "wobble_profile");
        return Map.copyOf(map);
    }

    private enum ApplyTarget { AUTO, BLOCK, HAND }

    private static MutationParser.ValueType mapFieldType(GrowthField.Type type) {
        return switch (type) {
            case BOOLEAN -> MutationParser.ValueType.BOOLEAN;
            case INT -> MutationParser.ValueType.INT;
            case DOUBLE -> MutationParser.ValueType.DOUBLE;
            case IDENTIFIER -> MutationParser.ValueType.IDENTIFIER;
        };
    }
}
