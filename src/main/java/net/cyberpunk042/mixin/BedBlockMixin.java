package net.cyberpunk042.mixin;

import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

	@Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$explodeInInfection(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit,
												 CallbackInfoReturnable<ActionResult> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("BedBlock.onUse");
		if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
			ctx.exit();
			return;
		}
		BedBlock bedBlock = (BedBlock) (Object) this;
		BlockPos headPos = pos;
		BlockState headState = state;
		if (state.get(BedBlock.PART) != BedPart.HEAD) {
			headPos = pos.offset(state.get(BedBlock.FACING));
			headState = world.getBlockState(headPos);
			if (!headState.isOf(bedBlock)) {
				ctx.exit();
				return;
			}
		}
		VirusWorldState infection = VirusWorldState.get(serverWorld);
		if (!infection.combat().isWithinAura(headPos)) {
			ctx.exit();
			return;
		}
		if (infection.shieldFieldService().isShielding(headPos)) {
			ctx.exit();
			return;
		}
		explode(serverWorld, headPos, headState, bedBlock);
		ctx.exit();
		cir.setReturnValue(ActionResult.SUCCESS_SERVER);
	}

	private static void explode(ServerWorld world, BlockPos headPos, BlockState headState, BedBlock bedBlock) {
		world.removeBlock(headPos, false);
		BlockPos footPos = headPos.offset(headState.get(BedBlock.FACING).getOpposite());
		if (world.getBlockState(footPos).isOf(bedBlock)) {
			world.removeBlock(footPos, false);
		}
		Vec3d center = Vec3d.ofCenter(headPos);
		world.createExplosion(null, world.getDamageSources().badRespawnPoint(center), null, center, 5.0F, true, World.ExplosionSourceType.BLOCK);
	}
}

