package net.cyberpunk042.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cyberpunk042.recipe.VirusFallbackRecipes;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;

@Mixin(ServerRecipeManager.class)
public class ServerRecipeManagerMixin {
	@Shadow
	private PreparedRecipes preparedRecipes;

	@Inject(method = "apply", at = @At("TAIL"))
	private void theVirusBlock$injectFallbackRecipes(PreparedRecipes preparedRecipes, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("RecipeMgr.apply");
		List<RecipeEntry<?>> mutable = new ArrayList<>(preparedRecipes.recipes());
		if (VirusFallbackRecipes.appendMissingRecipes(preparedRecipes, mutable)) {
			this.preparedRecipes = PreparedRecipes.of(mutable);
		}
		ctx.exit();
	}
}

