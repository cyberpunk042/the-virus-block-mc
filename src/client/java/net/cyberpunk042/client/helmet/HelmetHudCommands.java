package net.cyberpunk042.client.helmet;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client commands for configuring the Augmented Helmet HUD.
 * 
 * <h2>Commands</h2>
 * <pre>
 * /helmethud                     - Show current status
 * /helmethud visor <true|false>  - Enable/disable visor effects
 * /helmethud scanlines <true|false>
 * /helmethud vignette <0.0-1.0>  - Set vignette strength
 * /helmethud intensity <0.0-1.0> - Set overall visor intensity
 * /helmethud hud <true|false>    - Enable/disable HUD panel
 * /helmethud scale <0.5-2.0>     - Set HUD scale
 * /helmethud opacity <0.3-1.0>   - Set HUD opacity
 * /helmethud markers <true|false>
 * /helmethud markercount <0-50>  - Max markers (0 = unlimited)
 * /helmethud markerrange <0-500> - Marker render range (0 = unlimited)
 * /helmethud reset               - Reset all to defaults
 * </pre>
 */
public final class HelmetHudCommands {
    
    private HelmetHudCommands() {}
    
    /**
     * Registers all helmet HUD client commands.
     */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("helmethud")
                    // Status (no args)
                    .executes(ctx -> {
                        showStatus(ctx.getSource()::sendFeedback);
                        return 1;
                    })
                    
                    // Visor toggle
                    .then(ClientCommandManager.literal("visor")
                        .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean value = BoolArgumentType.getBool(ctx, "enabled");
                                HelmetHudConfig.get().setVisorEnabled(value);
                                feedback(ctx.getSource()::sendFeedback, "Visor effects", value ? "enabled" : "disabled");
                                return 1;
                            })))
                    
                    // Scanlines toggle
                    .then(ClientCommandManager.literal("scanlines")
                        .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean value = BoolArgumentType.getBool(ctx, "enabled");
                                HelmetHudConfig.get().setScanlinesEnabled(value);
                                feedback(ctx.getSource()::sendFeedback, "Scan lines", value ? "enabled" : "disabled");
                                return 1;
                            })))
                    
                    // Vignette strength
                    .then(ClientCommandManager.literal("vignette")
                        .then(ClientCommandManager.argument("strength", FloatArgumentType.floatArg(0f, 1f))
                            .executes(ctx -> {
                                float value = FloatArgumentType.getFloat(ctx, "strength");
                                HelmetHudConfig.get().setVignetteStrength(value);
                                feedback(ctx.getSource()::sendFeedback, "Vignette strength", String.format("%.2f", value));
                                return 1;
                            })))
                    
                    // Visor intensity
                    .then(ClientCommandManager.literal("intensity")
                        .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0f, 1f))
                            .executes(ctx -> {
                                float value = FloatArgumentType.getFloat(ctx, "value");
                                HelmetHudConfig.get().setVisorIntensity(value);
                                feedback(ctx.getSource()::sendFeedback, "Visor intensity", String.format("%.2f", value));
                                return 1;
                            })))
                    
                    // HUD toggle
                    .then(ClientCommandManager.literal("hud")
                        .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean value = BoolArgumentType.getBool(ctx, "enabled");
                                HelmetHudConfig.get().setHudEnabled(value);
                                feedback(ctx.getSource()::sendFeedback, "HUD panel", value ? "enabled" : "disabled");
                                return 1;
                            })))
                    
                    // HUD scale
                    .then(ClientCommandManager.literal("scale")
                        .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.5f, 2f))
                            .executes(ctx -> {
                                float value = FloatArgumentType.getFloat(ctx, "value");
                                HelmetHudConfig.get().setHudScale(value);
                                feedback(ctx.getSource()::sendFeedback, "HUD scale", String.format("%.2f", value));
                                return 1;
                            })))
                    
                    // HUD opacity
                    .then(ClientCommandManager.literal("opacity")
                        .then(ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.3f, 1f))
                            .executes(ctx -> {
                                float value = FloatArgumentType.getFloat(ctx, "value");
                                HelmetHudConfig.get().setHudOpacity(value);
                                feedback(ctx.getSource()::sendFeedback, "HUD opacity", String.format("%.2f", value));
                                return 1;
                            })))
                    
                    // Markers toggle
                    .then(ClientCommandManager.literal("markers")
                        .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean value = BoolArgumentType.getBool(ctx, "enabled");
                                HelmetHudConfig.get().setMarkersEnabled(value);
                                feedback(ctx.getSource()::sendFeedback, "Threat markers", value ? "enabled" : "disabled");
                                return 1;
                            })))
                    
                    // Marker count
                    .then(ClientCommandManager.literal("markercount")
                        .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(0, 50))
                            .executes(ctx -> {
                                int value = IntegerArgumentType.getInteger(ctx, "count");
                                HelmetHudConfig.get().setMaxMarkerCount(value);
                                String display = value == 0 ? "unlimited" : String.valueOf(value);
                                feedback(ctx.getSource()::sendFeedback, "Max marker count", display);
                                return 1;
                            })))
                    
                    // Marker range
                    .then(ClientCommandManager.literal("markerrange")
                        .then(ClientCommandManager.argument("range", FloatArgumentType.floatArg(0f, 500f))
                            .executes(ctx -> {
                                float value = FloatArgumentType.getFloat(ctx, "range");
                                HelmetHudConfig.get().setMarkerRenderRange(value);
                                String display = value == 0 ? "unlimited" : String.format("%.0f blocks", value);
                                feedback(ctx.getSource()::sendFeedback, "Marker render range", display);
                                return 1;
                            })))
                    
                    // Reset
                    .then(ClientCommandManager.literal("reset")
                        .executes(ctx -> {
                            HelmetHudConfig.get().reset();
                            ctx.getSource().sendFeedback(Text.literal("[Helmet HUD] ")
                                .formatted(Formatting.AQUA)
                                .append(Text.literal("All settings reset to defaults.")
                                    .formatted(Formatting.WHITE)));
                            return 1;
                        }))
            );
        });
    }
    
    private static void showStatus(java.util.function.Consumer<Text> feedback) {
        HelmetHudConfig config = HelmetHudConfig.get();
        
        feedback.accept(Text.literal("═══ Helmet HUD Status ═══").formatted(Formatting.AQUA));
        
        // Visor section
        feedback.accept(Text.literal("Visor Effects:").formatted(Formatting.GRAY));
        statusLine(feedback, "  Enabled", config.isVisorEnabled());
        statusLine(feedback, "  Intensity", String.format("%.2f", config.getVisorIntensity()));
        statusLine(feedback, "  Scanlines", config.isScanlinesEnabled());
        statusLine(feedback, "  Vignette", String.format("%.2f", config.getVignetteStrength()));
        
        // HUD section
        feedback.accept(Text.literal("HUD Panel:").formatted(Formatting.GRAY));
        statusLine(feedback, "  Enabled", config.isHudEnabled());
        statusLine(feedback, "  Scale", String.format("%.2f", config.getHudScale()));
        statusLine(feedback, "  Opacity", String.format("%.2f", config.getHudOpacity()));
        
        // Markers section
        feedback.accept(Text.literal("Threat Markers:").formatted(Formatting.GRAY));
        statusLine(feedback, "  Enabled", config.isMarkersEnabled());
        statusLine(feedback, "  Max Count", config.getMaxMarkerCount() == 0 ? "unlimited" : String.valueOf(config.getMaxMarkerCount()));
        statusLine(feedback, "  Render Range", config.getMarkerRenderRange() == 0 ? "unlimited" : String.format("%.0f", config.getMarkerRenderRange()));
        
        feedback.accept(Text.literal("Use /helmethud <setting> <value> to change.").formatted(Formatting.DARK_GRAY));
    }
    
    private static void statusLine(java.util.function.Consumer<Text> feedback, String label, boolean value) {
        feedback.accept(Text.literal(label + ": ")
            .formatted(Formatting.GRAY)
            .append(Text.literal(value ? "ON" : "OFF")
                .formatted(value ? Formatting.GREEN : Formatting.RED)));
    }
    
    private static void statusLine(java.util.function.Consumer<Text> feedback, String label, String value) {
        feedback.accept(Text.literal(label + ": ")
            .formatted(Formatting.GRAY)
            .append(Text.literal(value)
                .formatted(Formatting.WHITE)));
    }
    
    private static void feedback(java.util.function.Consumer<Text> consumer, String setting, String value) {
        consumer.accept(Text.literal("[Helmet HUD] ")
            .formatted(Formatting.AQUA)
            .append(Text.literal(setting + " set to ")
                .formatted(Formatting.WHITE))
            .append(Text.literal(value)
                .formatted(Formatting.YELLOW)));
    }
}
