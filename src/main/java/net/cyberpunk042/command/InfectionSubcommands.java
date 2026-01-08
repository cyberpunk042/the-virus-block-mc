package net.cyberpunk042.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.command.util.CommandProtection;
import net.cyberpunk042.command.util.ListFormatter;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.SimpleVirusScheduler;
import net.cyberpunk042.infection.command.CommandFacade;
import net.cyberpunk042.infection.service.InfectionServices;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Subcommands for /virusblock infection - uses CommandKnob consistently.
 */
public final class InfectionSubcommands {
    
    private InfectionSubcommands() {}
    
    private static final SuggestionProvider<ServerCommandSource> SCENARIO_SUGGESTIONS = (ctx, builder) -> {
        CommandFacade facade = new CommandFacade(VirusWorldState.get(ctx.getSource().getWorld()));
        facade.registeredScenarioIds().forEach(id -> builder.suggest(id.toString()));
        return builder.buildFuture();
    };
    
    /**
     * Builds the /virusblock infection command tree.
     */
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        var cmd = CommandManager.literal("infection")
            .then(buildScenario())
            .then(buildScheduler())
            .then(buildProfile())
            .then(buildService())
            .then(CommandManager.literal("singularity")
                .then(CommandManager.literal("state")
                    .executes(ctx -> SingularitySubcommands.reportSingularity(ctx.getSource()))));
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildScenario() {
        var cmd = CommandManager.literal("scenario");
        
        // List scenarios (info action)
        cmd.then(CommandManager.literal("list")
            .executes(ctx -> listScenarios(ctx.getSource())));
        
        // Current scenario (info action)
        cmd.then(CommandManager.literal("current")
            .executes(ctx -> reportScenario(ctx.getSource())));
        
        // Set scenario (protected action with argument)
        cmd.then(CommandManager.literal("set")
            .then(CommandManager.argument("scenario", IdentifierArgumentType.identifier())
                .suggests(SCENARIO_SUGGESTIONS)
                .executes(ctx -> setScenario(ctx.getSource(),
                    IdentifierArgumentType.getIdentifier(ctx, "scenario")))));
        
        // Unbind scenario (protected action)
        CommandKnob.action("infection.scenario.unbind", "Unbind scenario")
            .successMessage("Scenario binding cleared; default scenario will be used.")
            .handler(src -> {
                CommandFacade facade = commandFacade(src);
                facade.unbindScenario(src.getWorld());
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildScheduler() {
        var cmd = CommandManager.literal("scheduler");
        
        // Status (info, no protection needed)
        cmd.then(CommandManager.literal("status")
            .executes(ctx -> reportSchedulerStatus(ctx.getSource())));
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildProfile() {
        var cmd = CommandManager.literal("profile");
        
        // Reload profiles (protected action)
        CommandKnob.action("infection.profile.reload", "Reload dimension profiles")
            .successMessage("Reloaded dimension profiles; scenarios will reattach next tick.")
            .handler(src -> {
                CommandFacade facade = commandFacade(src);
                facade.reloadProfiles(src.getWorld());
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildService() {
        var cmd = CommandManager.literal("service");
        
        // Reload services (protected action)
        CommandKnob.action("infection.service.reload", "Reload infection services")
            .successMessage("Reloaded infection service container configuration.")
            .handler(src -> {
                InfectionServices.reload();
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Handlers (with inline protection for parameterized commands)
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static int listScenarios(ServerCommandSource source) {
        CommandFacade facade = commandFacade(source);
        Set<Identifier> scenarios = facade.registeredScenarioIds();
        Identifier effective = facade.effectiveScenario(source.getWorld()).orElse(null);
        Identifier active = facade.activeScenarioId().orElse(null);
        
        return ListFormatter.<Identifier>create("Registered infection scenarios:")
            .emptyMessage("No infection scenarios are registered.")
            .items(scenarios.stream().sorted(Comparator.comparing(Identifier::toString)).toList(),
                id -> ListFormatter.entry(id.toString())
                    .tagIf(id.equals(effective), "dimension", net.minecraft.util.Formatting.AQUA)
                    .tagIf(id.equals(active), "active", net.minecraft.util.Formatting.GREEN))
            .send(source);
    }
    
    private static int reportScenario(ServerCommandSource source) {
        CommandFacade facade = commandFacade(source);
        Identifier effective = facade.effectiveScenario(source.getWorld()).orElse(null);
        Identifier active = facade.activeScenarioId().orElse(null);
        var explicit = facade.boundScenario(source.getWorld().getRegistryKey());
        if (effective == null) {
            CommandFeedback.error(source, "This dimension does not have an infection scenario registered.");
            return 0;
        }
        if (explicit.isPresent()) {
            CommandFeedback.info(source, "Scenario binding: " + explicit.get());
        } else {
            CommandFeedback.info(source, "Scenario binding: <default>");
        }
        CommandFeedback.info(source, "Effective scenario: " + effective);
        if (active != null) {
            CommandFeedback.info(source, "Active controller: " + active);
        } else {
            CommandFeedback.info(source, "No scenario is currently attached (host idle).");
        }
        return 1;
    }
    
    private static int setScenario(ServerCommandSource source, Identifier scenarioId) {
        // Protection check for scenario setting
        if (!CommandProtection.checkAndWarn(source, "infection.scenario.set")) {
            return 0;
        }
        
        CommandFacade facade = commandFacade(source);
        if (!facade.bindScenario(source.getWorld(), scenarioId)) {
            CommandFeedback.error(source, "Unknown infection scenario: " + scenarioId);
            return 0;
        }
        CommandFeedback.successBroadcast(source, 
            "Scenario for this dimension set to " + scenarioId + " (will take effect next tick).");
        return 1;
    }
    
    private static int reportSchedulerStatus(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        VirusWorldState state = VirusWorldState.get(world);
        var diagnostics = state.orchestrator().services().schedulerService().diagnostics();
        String label = String.format(Locale.ROOT,
            "Scheduler %s backlog=%d%s",
            diagnostics.implementation(),
            diagnostics.backlog(),
            diagnostics.usingFallback() ? " [default]" : "");
        CommandFeedback.info(source, label);
        
        List<SimpleVirusScheduler.TaskSnapshot> persisted = diagnostics.persistedTasks();
        if (persisted.isEmpty()) {
            CommandFeedback.info(source, "No persisted tasks queued.");
            return diagnostics.backlog();
        }
        source.sendFeedback(() -> Text.literal("Persisted tasks:"), false);
        int limit = Math.min(5, persisted.size());
        for (int i = 0; i < limit; i++) {
            SimpleVirusScheduler.TaskSnapshot snapshot = persisted.get(i);
            int remaining = Math.max(0, snapshot.remainingTicks());
            int index = i + 1;
            String line = String.format(Locale.ROOT, "  #%d %s (%d ticks)", index, snapshot.type(), remaining);
            source.sendFeedback(() -> Text.literal(line), false);
        }
        if (persisted.size() > limit) {
            int rem = persisted.size() - limit;
            CommandFeedback.info(source, " ... plus " + rem + " more task(s).");
        }
        return diagnostics.backlog();
    }
    
    private static CommandFacade commandFacade(ServerCommandSource source) {
        return new CommandFacade(VirusWorldState.get(source.getWorld()));
    }
}
