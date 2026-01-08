package net.cyberpunk042.mixin;

import net.cyberpunk042.field.influence.FieldEvent;
import net.cyberpunk042.field.influence.TriggerEventDispatcher;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F156: Mixin for ServerPlayerEntity to dispatch respawn events.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
	
	/**
	 * F156: Dispatch PLAYER_RESPAWN event when a player respawns.
	 * 
	 * <p>Note: This targets ServerPlayerEntity.copyFrom() which is called during respawn.
	 * We inject at the end to ensure the player is fully respawned.
	 */
	@Inject(method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V", at = @At("TAIL"), require = 1)
	private void theVirusBlock$dispatchRespawnEvent(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("ServerPlayer.copyFrom");
		ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
		// Only dispatch if this is a respawn (not just copying data while alive)
		if (!alive) {
			TriggerEventDispatcher.dispatch(FieldEvent.PLAYER_RESPAWN, self, null);
		}
		ctx.exit();
	}
}


