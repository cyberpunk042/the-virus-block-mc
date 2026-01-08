package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "isSuitableFor", at = @At("RETURN"), cancellable = true)
	private void theVirusBlock$ensureCorruptedMetalsArePickaxeable(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("ItemStack.isSuitable");
		if (state == null || cir.getReturnValue()) {
			ctx.exit();
			return;
		}
		ItemStack self = (ItemStack) (Object) this;
		if (!self.isIn(ItemTags.PICKAXES)) {
			ctx.exit();
			return;
		}
		Block block = state.getBlock();
		if (block == ModBlocks.CORRUPTED_GOLD
				|| block == ModBlocks.CORRUPTED_DIAMOND
				|| block == ModBlocks.CORRUPTED_IRON
				|| block == ModBlocks.CORRUPTED_CRYING_OBSIDIAN) {
			cir.setReturnValue(true);
		}
		ctx.exit();
	}
}

