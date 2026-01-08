package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.world.World;

@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin {
	@Shadow
	@Final
	private ServerBossBar bossBar;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void theVirusBlock$init(EntityType<? extends WitherEntity> type, World world, CallbackInfo ci) {
		hideBossBar();
	}

	@Inject(method = "mobTick", at = @At("TAIL"))
	private void theVirusBlock$mobTick(CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Wither.mobTick");
		hideBossBar();
		ctx.exit();
	}

	private void hideBossBar() {
		bossBar.setVisible(false);
		bossBar.clearPlayers();
	}
}


