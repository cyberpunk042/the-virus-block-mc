package net.cyberpunk042.mixin;

import net.cyberpunk042.field.influence.FieldEvent;
import net.cyberpunk042.field.influence.TriggerEventDispatcher;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F156: Mixin for PlayerEntity to dispatch death events.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	
	/**
	 * F156: Dispatch PLAYER_DEATH event when a player dies.
	 */
	@Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"), require = 1)
	private void theVirusBlock$dispatchDeathEvent(DamageSource damageSource, CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Player.onDeath");
		PlayerEntity self = (PlayerEntity) (Object) this;
		TriggerEventDispatcher.dispatch(FieldEvent.PLAYER_DEATH, self, damageSource);
		ctx.exit();
	}
}

