package net.cyberpunk042.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.profile.ProfileManager;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.field.profile.Profile;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Commands for managing user profiles and viewing status.
 * 
 * <h2>Profile Commands</h2>
 * <pre>
 * /field profile list           - List all profiles
 * /field profile load <name>    - Load a profile
 * /field profile save [name]    - Save current state
 * /field profile               - Show current profile
 * </pre>
 * 
 * <h2>Status Command</h2>
 * <pre>
 * /field status                - Show field state summary
 * </pre>
 */
public final class FieldProfileCommands {
    
    private FieldProfileCommands() {}
    
    /**
     * Register profile and status commands under the /field parent.
     */
    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> field) {
        buildProfileCommands(field);
        buildStatusCommand(field);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PROFILE COMMANDS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildProfileCommands(LiteralArgumentBuilder<FabricClientCommandSource> field) {
        var profile = ClientCommandManager.literal("profile");
        
        // /field profile list
        profile.then(ClientCommandManager.literal("list")
            .executes(ctx -> listProfiles(ctx.getSource())));
        
        // /field profile load <name>
        profile.then(ClientCommandManager.literal("load")
            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    var mgr = ProfileManager.getInstance();
                    for (var p : mgr.getAllProfiles()) {
                        builder.suggest(p.id());
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    return loadProfile(ctx.getSource(), name);
                })));
        
        // /field profile save [name]
        profile.then(ClientCommandManager.literal("save")
            .executes(ctx -> saveProfile(ctx.getSource(), null))
            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    return saveProfile(ctx.getSource(), name);
                })));
        
        // /field profile (show current)
        profile.executes(ctx -> {
            FieldEditState state = FieldEditStateHolder.get();
            if (state != null) {
                ctx.getSource().sendFeedback(Text.literal("Current profile: " + 
                    state.getCurrentProfileName()).formatted(Formatting.AQUA));
            }
            return 1;
        });
        
        field.then(profile);
    }
    
    private static int listProfiles(FabricClientCommandSource source) {
        var mgr = ProfileManager.getInstance();
        var profiles = mgr.getAllProfiles();
        
        if (profiles.isEmpty()) {
            source.sendFeedback(Text.literal("No profiles found").formatted(Formatting.YELLOW));
            return 1;
        }
        
        source.sendFeedback(Text.literal("Profiles (" + profiles.size() + "):").formatted(Formatting.AQUA));
        for (var p : profiles) {
            String sourceTag = switch (p.source()) {
                case BUNDLED -> "[bundled]";
                case LOCAL -> "[local]";
                case SERVER -> "[server]";
            };
            source.sendFeedback(Text.literal("  " + p.id() + " " + sourceTag)
                .formatted(Formatting.GRAY));
        }
        return 1;
    }
    
    private static int loadProfile(FabricClientCommandSource source, String name) {
        FieldEditState state = FieldEditStateHolder.get();
        if (state == null) {
            source.sendError(Text.literal("Field state not available"));
            return 0;
        }
        
        var mgr = ProfileManager.getInstance();
        var profileOpt = mgr.getProfile(name);
        
        if (profileOpt.isEmpty()) {
            source.sendError(Text.literal("Profile not found: " + name));
            return 0;
        }
        
        var profile = profileOpt.get();
        // Convert JsonObject to String for fromProfileJson
        state.fromProfileJson(profile.toJson().toString());
        state.setCurrentProfile(profile.id(), profile.source() == ProfileSource.SERVER);
        state.saveSnapshot();
        
        source.sendFeedback(Text.literal("Loaded profile: " + profile.name())
            .formatted(Formatting.GREEN));
        return 1;
    }
    
    private static int saveProfile(FabricClientCommandSource source, String name) {
        FieldEditState state = FieldEditStateHolder.get();
        if (state == null) {
            source.sendError(Text.literal("Field state not available"));
            return 0;
        }
        
        String profileName = name != null ? name : state.getCurrentProfileName();
        if (profileName == null || profileName.isEmpty()) {
            profileName = "custom_" + System.currentTimeMillis();
        }
        
        // Create profile from current state
        String jsonStr = state.toProfileJson(profileName);
        var json = com.google.gson.JsonParser.parseString(jsonStr).getAsJsonObject();
        
        var profile = Profile.fromJson(json, ProfileSource.LOCAL);
        
        var mgr = ProfileManager.getInstance();
        mgr.saveProfile(profile);
        
        state.setCurrentProfile(profileName, false);
        state.saveSnapshot();
        
        source.sendFeedback(Text.literal("Saved profile: " + profileName)
            .formatted(Formatting.GREEN));
        return 1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS COMMAND
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void buildStatusCommand(LiteralArgumentBuilder<FabricClientCommandSource> field) {
        field.then(ClientCommandManager.literal("status")
            .executes(ctx -> {
                FieldEditState state = FieldEditStateHolder.get();
                if (state == null) {
                    ctx.getSource().sendError(Text.literal("Field state not available"));
                    return 0;
                }
                
                ctx.getSource().sendFeedback(Text.literal("═══ Field Status ═══").formatted(Formatting.GOLD));
                ctx.getSource().sendFeedback(Text.literal("Profile: " + state.getCurrentProfileName())
                    .formatted(Formatting.AQUA));
                ctx.getSource().sendFeedback(Text.literal("Shape: " + state.getString("shapeType") + 
                    " (r=" + String.format("%.1f", state.getFloat("radius")) + ")")
                    .formatted(Formatting.WHITE));
                ctx.getSource().sendFeedback(Text.literal("Layers: " + state.getLayerCount() + 
                    " | Primitives: " + state.getPrimitiveCount(state.getSelectedLayerIndex()))
                    .formatted(Formatting.WHITE));
                ctx.getSource().sendFeedback(Text.literal("Dirty: " + (state.isDirty() ? "Yes" : "No"))
                    .formatted(state.isDirty() ? Formatting.YELLOW : Formatting.GREEN));
                
                return 1;
            }));
    }
}

