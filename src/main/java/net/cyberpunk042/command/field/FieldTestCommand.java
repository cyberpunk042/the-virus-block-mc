package net.cyberpunk042.command.field;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.cyberpunk042.command.util.CommandFeedback;
import net.cyberpunk042.command.util.ReportBuilder;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.pattern.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;

/**
 * Debug commands for vertex pattern and shuffle exploration.
 * 
 * <h2>Commands</h2>
 * <pre>
 * /fieldtest vertex [pattern]        - List or set vertex pattern
 * /fieldtest shuffle                 - Show current shuffle state
 * /fieldtest shuffle next|prev       - Navigate shuffle patterns
 * /fieldtest shuffle jump <idx>      - Jump to specific index
 * /fieldtest shuffle type <type>     - Switch shuffle type
 * </pre>
 * 
 * @see FieldEditSubcommand for GUI-synced editing
 */
public final class FieldTestCommand {

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE (vertex/shuffle only)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static VertexPattern editPattern = QuadPattern.DEFAULT;
    private static ShuffleType shuffleType = ShuffleType.QUAD;
    private static int shuffleIndex = 0;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHUFFLE TYPE ENUM
    // ═══════════════════════════════════════════════════════════════════════════
    
    public enum ShuffleType {
        QUAD("quad", "Quad patterns (spheres/prisms)"),
        SEGMENT("segment", "Segment patterns (rings)"),
        SECTOR("sector", "Sector patterns (discs)"),
        EDGE("edge", "Edge patterns (wireframes)"),
        TRIANGLE("triangle", "Triangle patterns (polyhedra)");
        
        private final String id;
        private final String description;
        
        ShuffleType(String id, String description) {
            this.id = id;
            this.description = description;
        }
        
        public String id() { return id; }
        public String description() { return description; }
        
        public int count() {
            return switch (this) {
                case QUAD -> ShuffleGenerator.quadCount();
                case SEGMENT -> ShuffleGenerator.segmentCount();
                case SECTOR -> ShuffleGenerator.sectorCount();
                case EDGE -> ShuffleGenerator.edgeCount();
                case TRIANGLE -> ShuffleGenerator.triangleCount();
            };
        }
    }
    
    private FieldTestCommand() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SUGGESTION PROVIDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final SuggestionProvider<ServerCommandSource> VERTEX_SUGGESTIONS = (ctx, builder) -> {
        for (String id : QuadPattern.ids()) builder.suggest(id);
        for (String id : SegmentPattern.ids()) builder.suggest(id);
        for (String id : SectorPattern.ids()) builder.suggest(id);
        for (String id : EdgePattern.ids()) builder.suggest(id);
        return builder.buildFuture();
    };
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var cmd = CommandManager.literal("fieldtest")
            .requires(source -> source.hasPermissionLevel(2))
            .then(buildVertexCommand())
            .then(buildShuffleCommands());
        
        dispatcher.register(cmd);
        Logging.COMMANDS.info("Registered /fieldtest (vertex/shuffle)");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VERTEX COMMAND
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildVertexCommand() {
        return CommandManager.literal("vertex")
            .executes(ctx -> {
                ReportBuilder.create("Vertex Patterns")
                    .section("Quad", s -> { for (String id : QuadPattern.ids()) s.line("  " + id); })
                    .section("Segment", s -> { for (String id : SegmentPattern.ids()) s.line("  " + id); })
                    .section("Sector", s -> { for (String id : SectorPattern.ids()) s.line("  " + id); })
                    .section("Edge", s -> { for (String id : EdgePattern.ids()) s.line("  " + id); })
                    .kv("Current", editPattern.id(), Formatting.GREEN)
                    .send(ctx.getSource());
                return 1;
            })
            .then(CommandManager.argument("pattern", StringArgumentType.word())
                .suggests(VERTEX_SUGGESTIONS)
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "pattern");
                    VertexPattern pattern = VertexPattern.fromString(name);
                    if (pattern == null) {
                        CommandFeedback.error(ctx.getSource(), "Unknown pattern: " + name);
                        return 0;
                    }
                    editPattern = pattern;
                    CommandFeedback.valueSet(ctx.getSource(), "Vertex pattern", pattern.id());
                    return 1;
                }));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHUFFLE COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildShuffleCommands() {
        return CommandManager.literal("shuffle")
            .executes(ctx -> {
                CommandFeedback.info(ctx.getSource(), 
                    "[" + shuffleType.id() + " " + (shuffleIndex + 1) + "/" + shuffleType.count() + "]");
                return 1;
            })
            .then(CommandManager.literal("next").executes(ctx -> {
                shuffleIndex = (shuffleIndex + 1) % shuffleType.count();
                return applyShuffleAndShow(ctx.getSource());
            }))
            .then(CommandManager.literal("prev").executes(ctx -> {
                shuffleIndex = (shuffleIndex - 1 + shuffleType.count()) % shuffleType.count();
                return applyShuffleAndShow(ctx.getSource());
            }))
            .then(CommandManager.literal("jump")
                .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                    .executes(ctx -> {
                        int idx = IntegerArgumentType.getInteger(ctx, "index");
                        if (idx >= shuffleType.count()) {
                            CommandFeedback.error(ctx.getSource(), "Max index: " + (shuffleType.count() - 1));
                            return 0;
                        }
                        shuffleIndex = idx;
                        return applyShuffleAndShow(ctx.getSource());
                    })))
            .then(CommandManager.literal("type")
                .then(CommandManager.literal("quad").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.QUAD)))
                .then(CommandManager.literal("segment").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.SEGMENT)))
                .then(CommandManager.literal("sector").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.SECTOR)))
                .then(CommandManager.literal("edge").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.EDGE)))
                .then(CommandManager.literal("triangle").executes(ctx -> setShuffle(ctx.getSource(), ShuffleType.TRIANGLE))));
    }
    
    private static int setShuffle(ServerCommandSource source, ShuffleType type) {
        shuffleType = type;
        shuffleIndex = 0;
        CommandFeedback.success(source, "Switched to " + type.id() + " (" + type.count() + " patterns)");
        return applyShuffleAndShow(source);
    }
    
    private static int applyShuffleAndShow(ServerCommandSource source) {
        String desc = switch (shuffleType) {
            case QUAD -> {
                var arr = ShuffleGenerator.getQuad(shuffleIndex);
                editPattern = DynamicQuadPattern.fromArrangement(arr);
                yield arr.describe();
            }
            case SEGMENT -> {
                var arr = ShuffleGenerator.getSegment(shuffleIndex);
                editPattern = DynamicSegmentPattern.fromArrangement(arr);
                yield arr.describe();
            }
            case SECTOR -> {
                var arr = ShuffleGenerator.getSector(shuffleIndex);
                editPattern = DynamicSectorPattern.fromArrangement(arr);
                yield arr.describe();
            }
            case EDGE -> {
                var arr = ShuffleGenerator.getEdge(shuffleIndex);
                editPattern = DynamicEdgePattern.fromArrangement(arr);
                yield arr.describe();
            }
            case TRIANGLE -> {
                var arr = ShuffleGenerator.getTriangle(shuffleIndex);
                editPattern = DynamicTrianglePattern.fromArrangement(arr);
                yield arr.describe();
            }
        };
        
        CommandFeedback.highlight(source, 
            String.format("[%s #%d/%d] %s", shuffleType.id().toUpperCase(), shuffleIndex + 1, shuffleType.count(), desc));
        return 1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC ACCESSOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static VertexPattern getEditPattern() { return editPattern; }
}
