package net.cyberpunk042.mixin.client;

import net.cyberpunk042.mixin.client.access.CyclingButtonWidgetAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to add right-click backward cycling support to CyclingButtonWidget.
 * Left-click cycles forward (vanilla behavior), right-click cycles backward.
 * 
 * We target ClickableWidget.mouseClicked because that's where the method is defined,
 * then check if `this` is a CyclingButtonWidget.
 */
@Mixin(ClickableWidget.class)
public abstract class CyclingButtonWidgetMixin {
    
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void theVirusBlock$onRightClickCyclingButton(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Only apply to CyclingButtonWidget instances
        if (!((Object) this instanceof CyclingButtonWidget<?>)) {
            return;
        }
        
        ClickableWidget self = (ClickableWidget) (Object) this;
        
        if (self.active && self.visible && self.isMouseOver(mouseX, mouseY)) {
            if (button == 1) { // Right-click: cycle backward
                self.playDownSound(MinecraftClient.getInstance().getSoundManager());
                // Use the accessor to invoke the cycle method
                ((CyclingButtonWidgetAccessor) this).invokeCycle(-1);
                cir.setReturnValue(true);
            }
        }
    }
}
