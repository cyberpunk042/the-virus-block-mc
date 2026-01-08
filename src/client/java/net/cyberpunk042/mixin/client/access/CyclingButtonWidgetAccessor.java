package net.cyberpunk042.mixin.client.access;

import net.minecraft.client.gui.widget.CyclingButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for CyclingButtonWidget's cycle method.
 */
@Mixin(CyclingButtonWidget.class)
public interface CyclingButtonWidgetAccessor {
    
    @Invoker("cycle")
    void invokeCycle(int amount);
}
