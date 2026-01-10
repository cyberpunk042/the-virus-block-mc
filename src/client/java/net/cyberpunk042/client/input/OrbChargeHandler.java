package net.cyberpunk042.client.input;

import net.cyberpunk042.client.gui.state.FieldEditStateHolder;
import net.cyberpunk042.client.gui.state.adapter.FieldVisualAdapter;
import net.cyberpunk042.client.visual.effect.FieldVisualConfig;
import net.cyberpunk042.client.visual.effect.FieldVisualInstance;
import net.cyberpunk042.client.visual.effect.FieldVisualPostEffect;
import net.cyberpunk042.client.visual.effect.FieldVisualRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * C key orb charge and throw - INDEPENDENT of GUI toggle.
 * 
 * Hold C: Orb appears at player, grows from 0 to configured radius over 3s
 * After 3s: Orb sustains at full size
 * Release C: Orb throws toward crosshair
 */
public class OrbChargeHandler {
    
    private static KeyBinding chargeKey;
    
    // State
    private static boolean isCharging = false;
    private static boolean isThrowing = false;
    private static long chargeStartTime = 0;
    private static long throwStartTime = 0;
    private static UUID orbId = null;
    
    // For cooldown message
    private static long lastCooldownMessageTime = 0;
    private static final long COOLDOWN_MESSAGE_INTERVAL_MS = 500;
    
    // Config
    private static final float MAX_CHARGE_MS = 3000f;
    private static final float THROW_DURATION_MS = 2000f;  // Slightly slower for more impact
    
    // Throw animation
    private static Vec3d throwStart;
    private static Vec3d throwTarget;
    
    // Charge progress (0-1) for HUD
    private static float chargeProgress = 0f;
    
    public static void register() {
        chargeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.the-virus-block.charge_orb",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.the-virus-block.effects"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(OrbChargeHandler::tick);
        
        // Register HUD renderer for charge bar
        HudRenderCallback.EVENT.register(OrbChargeHandler::renderChargeBar);
    }
    
    private static void tick(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) {
            if (isCharging) cancelCharge();
            return;
        }
        
        boolean keyDown = chargeKey.isPressed();
        
        // If throwing and user presses C, show message
        if (keyDown && isThrowing && !isCharging) {
            long now = System.currentTimeMillis();
            if (now - lastCooldownMessageTime > COOLDOWN_MESSAGE_INTERVAL_MS) {
                client.player.sendMessage(Text.literal("§c⚡ Orb in flight!"), true);
                lastCooldownMessageTime = now;
            }
            return;
        }
        
        // Start charging
        if (keyDown && !isCharging && !isThrowing) {
            startCharge(client);
        }
        // Continue charging
        else if (keyDown && isCharging) {
            updateCharge(client);
        }
        // Release - throw
        else if (!keyDown && isCharging) {
            releaseThrow(client);
        }
        
        // Update throw animation
        if (isThrowing) {
            updateThrow(client);
        }
        
        // Clear charge progress if not charging
        if (!isCharging) {
            chargeProgress = 0f;
        }
    }
    
    private static void startCharge(MinecraftClient client) {
        isCharging = true;
        chargeStartTime = System.currentTimeMillis();
        chargeProgress = 0f;
        
        // Create orb at player with tiny radius
        Vec3d playerPos = client.player.getBoundingBox().getCenter();
        orbId = UUID.randomUUID();
        
        // Get config from adapter if available, else use default
        FieldVisualConfig config = getConfig();
        
        FieldVisualInstance orb = new FieldVisualInstance(
            orbId,
            client.player.getUuid(),
            playerPos,
            0.01f,  // Start infinitely small
            "sphere",
            config
        );
        
        // Mark as independent orb (not affected by GUI toggle)
        orb.setSpawnAnimationOrb(true);
        
        // Register - no need for global setFollowFieldId, each orb manages its own position
        FieldVisualRegistry.register(orb);
        FieldVisualPostEffect.setEnabled(true);
    }
    
    private static void updateCharge(MinecraftClient client) {
        if (orbId == null) return;
        
        // Calculate charge progress (0 to 1 over 3 seconds)
        long elapsed = System.currentTimeMillis() - chargeStartTime;
        chargeProgress = Math.min(1.0f, elapsed / MAX_CHARGE_MS);
        
        // Ease out for smooth growth
        float eased = 1.0f - (float)Math.pow(1.0 - chargeProgress, 3);
        
        // Get target radius from config
        float targetRadius = getTargetRadius();
        
        // Interpolate: tiny -> full radius
        float currentRadius = 0.01f + (targetRadius - 0.01f) * eased;
        
        // Update orb position and radius
        Vec3d playerPos = client.player.getBoundingBox().getCenter();
        FieldVisualRegistry.updatePosition(orbId, playerPos);
        FieldVisualRegistry.updateRadius(orbId, currentRadius);
    }
    
    private static void releaseThrow(MinecraftClient client) {
        isCharging = false;
        chargeProgress = 0f;
        if (orbId == null) return;
        
        // Get current orb position
        FieldVisualInstance orb = FieldVisualRegistry.get(orbId);
        throwStart = orb != null ? orb.getWorldCenter() : client.player.getBoundingBox().getCenter();
        
        // Target = where player is looking (crosshair) at throwRange distance
        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookDir = client.player.getRotationVec(1.0f);
        float throwRange = getThrowRange();
        throwTarget = eyePos.add(lookDir.multiply(throwRange));
        
        // Start throw - orb now manages its own position via FieldVisualRegistry.updatePosition
        isThrowing = true;
        throwStartTime = System.currentTimeMillis();
    }
    
    private static void updateThrow(MinecraftClient client) {
        if (orbId == null) {
            isThrowing = false;
            return;
        }
        
        long elapsed = System.currentTimeMillis() - throwStartTime;
        float progress = Math.min(1.0f, elapsed / THROW_DURATION_MS);
        
        // Ease out for smooth deceleration
        float eased = 1.0f - (float)Math.pow(1.0 - progress, 3);
        
        // Interpolate position
        Vec3d currentPos = throwStart.lerp(throwTarget, eased);
        FieldVisualRegistry.updatePosition(orbId, currentPos);
        
        // Fade out near end
        if (progress > 0.7f) {
            float fade = (progress - 0.7f) / 0.3f;
            FieldVisualInstance orb = FieldVisualRegistry.get(orbId);
            if (orb != null) {
                float baseIntensity = getIntensity();
                FieldVisualConfig newConfig = orb.getConfig().withIntensity(baseIntensity * (1.0f - fade));
                orb.updateConfig(newConfig);
            }
        }
        
        // Finish
        if (progress >= 1.0f) {
            finishThrow();
        }
    }
    
    private static void finishThrow() {
        isThrowing = false;
        if (orbId != null) {
            FieldVisualRegistry.unregister(orbId);
            orbId = null;
        }
        // Don't call setEnabled(false) - other orbs may still be rendering
    }
    
    private static void cancelCharge() {
        isCharging = false;
        chargeProgress = 0f;
        if (orbId != null) {
            FieldVisualRegistry.unregister(orbId);
            orbId = null;
        }
        // Don't call setEnabled(false) - other orbs may still be rendering
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HUD RENDERING - Charge Bar
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static void renderChargeBar(DrawContext context, RenderTickCounter tickCounter) {
        if (!isCharging || chargeProgress <= 0f) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Draw charge bar centered below crosshair
        int barWidth = 80;
        int barHeight = 6;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight / 2 + 20;
        
        // Background (dark)
        context.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0x80000000);
        
        // Progress bar
        int filledWidth = (int)(barWidth * chargeProgress);
        
        // Color: cyan -> white as it charges
        int r = (int)(100 + 155 * chargeProgress);
        int g = (int)(200 + 55 * chargeProgress);
        int b = 255;
        int color = 0xFF000000 | (r << 16) | (g << 8) | b;
        
        context.fill(x, y, x + filledWidth, y + barHeight, color);
        
        // Glow effect when fully charged
        if (chargeProgress >= 1.0f) {
            // Pulsing glow
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 100.0) * 0.3 + 0.7);
            int glowAlpha = (int)(255 * pulse);
            int glowColor = (glowAlpha << 24) | 0x00FFFF;
            context.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, glowColor);
        }
        
        // Label
        String label = chargeProgress >= 1.0f ? "§b⚡ READY!" : String.format("§7Charging... §f%d%%", (int)(chargeProgress * 100));
        int textX = x + barWidth / 2 - client.textRenderer.getWidth(label) / 2;
        context.drawText(client.textRenderer, label, textX, y + barHeight + 4, 0xFFFFFF, true);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIG HELPERS - Read from adapter if available, else use defaults
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static FieldVisualConfig getConfig() {
        var state = FieldEditStateHolder.get();
        if (state != null) {
            return state.fieldVisualAdapter().buildConfig();
        }
        return FieldVisualConfig.defaultEnergyOrb();
    }
    
    private static float getTargetRadius() {
        var state = FieldEditStateHolder.get();
        if (state != null) {
            Object val = state.fieldVisualAdapter().get("fieldVisual.previewRadius");
            if (val instanceof Number n) return n.floatValue();
        }
        return 3.0f;
    }
    
    private static float getThrowRange() {
        var state = FieldEditStateHolder.get();
        if (state != null) {
            Object val = state.fieldVisualAdapter().get("fieldVisual.throwRange");
            if (val instanceof Number n) return n.floatValue();
        }
        return 30.0f;
    }
    
    private static float getIntensity() {
        var state = FieldEditStateHolder.get();
        if (state != null) {
            Object val = state.fieldVisualAdapter().get("fieldVisual.intensity");
            if (val instanceof Number n) return n.floatValue();
        }
        return 1.2f;
    }
}
