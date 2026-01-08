package net.cyberpunk042.command.field;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.cyberpunk042.command.util.CommandKnob;
import net.cyberpunk042.field.instance.FollowConfig;
import net.cyberpunk042.log.Logging;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/**
 * /field personal subcommands - Configure the player's personal field (shield).
 * 
 * <p>These settings control how the personal field behaves in-game:
 * <ul>
 *   <li>enabled - Toggle personal field visibility</li>
 *   <li>visual - Toggle visual rendering</li>
 *   <li>follow - Follow mode settings (leadOffset, responsiveness, lookAhead)</li>
 * </ul>
 * 
 * <p>Settings are persisted via CommandKnob to JSON config.</p>
 */
public final class PersonalSubcommand {
    
    private PersonalSubcommand() {}
    
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        var cmd = CommandManager.literal("personal")
            .then(buildFollow());
        
        // Knob-based settings
        CommandKnob.toggle("field.personal.enabled", "Personal field")
            .defaultValue(false)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Personal field: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.toggle("field.personal.visual", "Personal field visual")
            .defaultValue(true)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Personal visual: {}", v);
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
    
    private static LiteralArgumentBuilder<ServerCommandSource> buildFollow() {
        var cmd = CommandManager.literal("follow");
        
        CommandKnob.toggle("field.personal.follow.enabled", "Follow enabled")
            .defaultValue(true)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Follow enabled: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.floatValue("field.personal.follow.leadOffset", "Lead/trail offset")
            .range(-1.0f, 1.0f)
            .unit("")
            .defaultValue(0.0f)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Lead offset: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.floatValue("field.personal.follow.responsiveness", "Responsiveness")
            .range(0.1f, 1.0f)
            .unit("")
            .defaultValue(0.5f)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Responsiveness: {}", v);
                return true;
            })
            .attach(cmd);
        
        CommandKnob.floatValue("field.personal.follow.lookAhead", "Look ahead offset")
            .range(0.0f, 0.5f)
            .unit("blocks")
            .defaultValue(0.0f)
            .handler((src, v) -> {
                Logging.COMMANDS.info("Look ahead: {}", v);
                return true;
            })
            .attach(cmd);
        
        return cmd;
    }
}
