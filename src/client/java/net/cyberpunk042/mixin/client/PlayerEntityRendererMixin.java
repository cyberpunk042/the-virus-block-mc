package net.cyberpunk042.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cyberpunk042.client.render.feature.CompositeElytraFeatureRenderer;
import net.cyberpunk042.mixin.client.access.LivingEntityRendererAccessor;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void thevirus$addCompositeLayer(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
		PlayerEntityRenderer self = (PlayerEntityRenderer) (Object) this;
		((LivingEntityRendererAccessor) self).thevirus$addFeature(new CompositeElytraFeatureRenderer(self, ctx.getEntityModels()));
	}
}

