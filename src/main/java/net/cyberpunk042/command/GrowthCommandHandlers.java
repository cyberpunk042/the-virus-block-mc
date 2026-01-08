package net.cyberpunk042.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.command.util.CommandFormatters;
import net.cyberpunk042.command.util.CommandProtection;
import net.cyberpunk042.command.util.CommandTargetResolver;
import net.cyberpunk042.command.util.ConfigFileEditor;
import net.cyberpunk042.command.util.MutationParser;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.growth.scheduler.GrowthField;
import net.cyberpunk042.growth.scheduler.GrowthMutation;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.growth.scheduler.GrowthScheduler;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/**
 * Growth-specific command handlers.
 */
public final class GrowthCommandHandlers {

    public static final SimpleCommandExceptionType NO_GROWTH_STACK = 
            CommandTargetResolver.exception("Hold a progressive growth block in either hand.");
    public static final SimpleCommandExceptionType NO_BLOCK_TARGET = 
            CommandTargetResolver.exception("Look at a progressive growth block within reach.");
    public static final SimpleCommandExceptionType NO_TARGET = 
            CommandTargetResolver.exception("No growth block target found. Look at one or hold one in hand.");
    public static final DynamicCommandExceptionType UNKNOWN_DEFINITION = 
            new DynamicCommandExceptionType(id -> Text.literal("Unknown growth definition: " + id));

    private GrowthCommandHandlers() {}

    // === Target Resolution ===

    public static ProgressiveGrowthBlockEntity traceGrowthBlock(ServerPlayerEntity player) {
        return CommandTargetResolver.traceBlockEntity(player, ProgressiveGrowthBlockEntity.class);
    }

    public static ProgressiveGrowthBlockEntity requireGrowthBlock(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        return CommandTargetResolver.requireBlockEntity(player, ProgressiveGrowthBlockEntity.class, NO_BLOCK_TARGET);
    }

    public static CommandTargetResolver.StackTarget findHeldGrowthBlock(ServerPlayerEntity player) {
        return CommandTargetResolver.findHeldItem(player, stack -> 
                stack != null && !stack.isEmpty() && stack.isOf(ModBlocks.PROGRESSIVE_GROWTH_BLOCK.asItem()));
    }

    // === Apply Handlers ===

    public static int applyAuto(CommandContext<ServerCommandSource> ctx, MutationParser.ParsedMutation<GrowthField, GrowthMutation> parsed) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ProgressiveGrowthBlockEntity block = traceGrowthBlock(player);
        if (block != null) {
            return applyMutationToBlock(ctx, block, parsed);
        }
        CommandTargetResolver.StackTarget target = findHeldGrowthBlock(player);
        if (target != null) {
            return applyMutationToStack(ctx, target, parsed);
        }
        throw NO_TARGET.create();
    }

    public static int applyMutationToBlock(CommandContext<ServerCommandSource> ctx, ProgressiveGrowthBlockEntity block, MutationParser.ParsedMutation<GrowthField, GrowthMutation> parsed) {
        if (!CommandProtection.checkAndWarn(ctx.getSource(), "growth.apply")) {
            return 0;
        }
        boolean changed = block.applyMutation(parsed.mutation());
        if (!changed) {
            ctx.getSource().sendFeedback(() -> Text.literal("No override changes were applied; the block already matches the requested values."), false);
            return 0;
        }
        BlockPos pos = block.getPos();
        ctx.getSource().sendFeedback(() -> Text.literal("Applied " + parsed.summary() + " to growth block at " + CommandFormatters.formatPos(pos) + "."), true);
        return 1;
    }

    public static int applyMutationToStack(CommandContext<ServerCommandSource> ctx, CommandTargetResolver.StackTarget target, MutationParser.ParsedMutation<GrowthField, GrowthMutation> parsed) {
        GrowthOverrides overrides = ProgressiveGrowthBlock.readOverrides(target.stack());
        boolean changed = overrides.applyMutation(parsed.mutation());
        if (!changed) {
            ctx.getSource().sendFeedback(() -> Text.literal("No override changes were applied to the held stack."), false);
            return 0;
        }
        ProgressiveGrowthBlock.applyOverrides(target.stack(), overrides);
        ctx.getSource().sendFeedback(() -> Text.literal("Applied " + parsed.summary() + " to the " + target.hand().name().toLowerCase(Locale.ROOT) + " stack."), true);
        return 1;
    }

    // === Inspect Handlers ===

    public static int inspectAuto(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ProgressiveGrowthBlockEntity block = traceGrowthBlock(player);
        if (block != null) {
            return inspectBlock(ctx, block);
        }
        CommandTargetResolver.StackTarget target = findHeldGrowthBlock(player);
        if (target != null) {
            return inspectHand(ctx, target);
        }
        throw NO_TARGET.create();
    }

    public static int inspectBlock(CommandContext<ServerCommandSource> ctx, ProgressiveGrowthBlockEntity block) {
        GrowthOverrides overrides = block.overridesSnapshot();
        GrowthProfileDescriber.reportDefinition(ctx.getSource(),
                "Block at " + CommandFormatters.formatPos(block.getPos()),
                block.getDefinitionId(),
                block.definitionSnapshot(),
                overrides);
        Box renderWorld = block.getWorldRenderBounds();
        final VoxelShape hitShape = block.worldShape(ProgressiveGrowthBlock.ShapeType.OUTLINE);
        final boolean hitEmpty = hitShape == null || hitShape.isEmpty();
        final Box hitWorld = hitEmpty ? Box.of(Vec3d.ofCenter(block.getPos()), 0.0D, 0.0D, 0.0D) : hitShape.getBoundingBox();
        ctx.getSource().sendFeedback(() -> Text.literal("Render bounds: " + CommandFormatters.formatBox(renderWorld)), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Hit bounds: " + CommandFormatters.formatBox(hitWorld) + " empty=" + hitEmpty), false);
        return 1;
    }

    public static int inspectHand(CommandContext<ServerCommandSource> ctx, CommandTargetResolver.StackTarget target) {
        GrowthRegistry registry = InfectionServices.get().growth();
        Identifier definitionId = ProgressiveGrowthBlock.readDefinitionId(target.stack());
        if (definitionId == null) {
            definitionId = registry.defaultDefinition().id();
        }
        GrowthBlockDefinition base = registry.definition(definitionId);
        GrowthOverrides overrides = ProgressiveGrowthBlock.readOverrides(target.stack());
        GrowthBlockDefinition applied = overrides.isEmpty() ? base : overrides.apply(base, registry);
        GrowthProfileDescriber.reportDefinition(ctx.getSource(),
                "Hand (" + target.hand().name().toLowerCase(Locale.ROOT) + ")",
                definitionId,
                applied,
                overrides);
        return 1;
    }

    // === Schedule Handlers ===

    public static int scheduleOnWorld(CommandContext<ServerCommandSource> ctx, ServerWorld world, BlockPos pos, int delay, MutationParser.ParsedMutation<GrowthField, GrowthMutation> parsed) {
        if (!CommandProtection.checkAndWarn(ctx.getSource(), "growth.schedule")) {
            return 0;
        }
        GrowthScheduler.schedule(world, pos, parsed.mutation(), delay);
        String delayText = delay == 1 ? "1 tick" : delay + " ticks";
        ctx.getSource().sendFeedback(() -> Text.literal("Scheduled " + parsed.summary()
                + " at " + CommandFormatters.formatPos(pos)
                + " in " + CommandFormatters.formatDimension(world)
                + " after " + delayText + "."), true);
        return 1;
    }

    // === Give Handlers ===

    public static int giveToPlayer(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target, Identifier definitionId, int count) throws CommandSyntaxException {
        if (!CommandProtection.checkAndWarn(ctx.getSource(), "growth.give")) {
            return 0;
        }
        GrowthRegistry registry = InfectionServices.get().growth();
        GrowthBlockDefinition definition = requireDefinition(registry, definitionId);
        int clamped = Math.min(64, Math.max(1, count));
        ItemStack stack = new ItemStack(ModBlocks.PROGRESSIVE_GROWTH_BLOCK, clamped);
        ProgressiveGrowthBlock.applyDefinitionId(stack, definition.id());
        ItemStack gift = stack.copy();
        boolean inserted = target.getInventory().insertStack(gift);
        if (!inserted) {
            target.dropItem(gift, false);
        }
        ctx.getSource().sendFeedback(() -> Text.literal("Gave " + stack.getCount() + " growth block(s) with definition " + definition.id() + " to " + target.getGameProfile().getName()), true);
        return stack.getCount();
    }

    // === Defaults Handlers ===

    public static int showDefaults(CommandContext<ServerCommandSource> ctx, Identifier definitionId, Path configRoot) throws CommandSyntaxException {
        GrowthRegistry registry = InfectionServices.get().growth();
        GrowthBlockDefinition definition = requireDefinition(registry, definitionId);
        GrowthProfileDescriber.reportDefinition(ctx.getSource(), "Defaults", definitionId, definition, GrowthOverrides.empty());
        try {
            Path file = findDefinitionFile(definitionId, configRoot);
            if (file != null) {
                ctx.getSource().sendFeedback(() -> Text.literal("JSON: " + ConfigFileEditor.formatPath(file, configRoot)), false);
            }
        } catch (IOException ex) {
            ctx.getSource().sendError(Text.literal("Warning: unable to inspect JSON file (" + ex.getMessage() + ")"));
        }
        return 1;
    }

    public static int setDefaults(CommandContext<ServerCommandSource> ctx, Identifier definitionId, MutationParser.ParsedMutation<GrowthField, GrowthMutation> parsed, Map<GrowthField, String> jsonKeyMap, Path configRoot) throws CommandSyntaxException {
        if (!CommandProtection.checkAndWarn(ctx.getSource(), "growth.defaults.set")) {
            return 0;
        }
        GrowthRegistry registry = InfectionServices.get().growth();
        requireDefinition(registry, definitionId);
        Path file = requireDefinitionFile(definitionId, configRoot);
        JsonObject json = ConfigFileEditor.readJson(file);
        
        var changes = parsed.changes().stream()
                .map(c -> new ConfigFileEditor.FieldChange<>(c.field(), c.value(), c.cleared()))
                .toList();
        
        boolean changed = ConfigFileEditor.applyChanges(json, changes, jsonKeyMap);
        if (!changed) {
            ctx.getSource().sendFeedback(() -> Text.literal("No JSON changes were applied to " + ConfigFileEditor.formatPath(file, configRoot) + "."), false);
            return 0;
        }
        ConfigFileEditor.writeJson(file, json);
        InfectionServices.reload();
        ctx.getSource().sendFeedback(
                () -> Text.literal("Updated " + ConfigFileEditor.formatPath(file, configRoot) + " (" + parsed.summary() + ") and reloaded growth registry."),
                true);
        return 1;
    }

    // === Helpers ===

    public static GrowthBlockDefinition requireDefinition(GrowthRegistry registry, Identifier definitionId) throws CommandSyntaxException {
        if (registry == null || definitionId == null || !registry.hasDefinition(definitionId)) {
            throw UNKNOWN_DEFINITION.create(String.valueOf(definitionId));
        }
        return registry.definition(definitionId);
    }

    public static Path findDefinitionFile(Identifier definitionId, Path configRoot) throws IOException {
        Path dir = configRoot.resolve("growth_blocks");
        return ConfigFileEditor.findConfigFile(dir, definitionId, (path, id) -> ConfigFileEditor.matchesId(path, id, "id"));
    }

    public static Path requireDefinitionFile(Identifier definitionId, Path configRoot) throws CommandSyntaxException {
        try {
            Path file = findDefinitionFile(definitionId, configRoot);
            if (file == null) {
                throw ConfigFileEditor.FILE_MISSING.create(String.valueOf(definitionId));
            }
            return file;
        } catch (IOException ex) {
            throw ConfigFileEditor.IO_ERROR.create(ex.getMessage());
        }
    }
}
