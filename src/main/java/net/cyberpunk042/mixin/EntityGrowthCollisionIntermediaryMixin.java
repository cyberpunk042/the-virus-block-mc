package net.cyberpunk042.mixin;


import net.cyberpunk042.log.Logging;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionMixinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * Production/runtime twin to {@link EntityGrowthCollisionMixin}.
 * Keeps the intermediary descriptor hooking in sync with dev builds
 * but provides diagnostics only; disabling it would not break the fix.
 */
@Mixin(targets = "net.minecraft.class_1297", remap = false)
public abstract class EntityGrowthCollisionIntermediaryMixin {
	@Inject(
		method = "method_59920(Lnet/minecraft/class_1297;Lnet/minecraft/class_1937;Ljava/util/List;Lnet/minecraft/class_238;)Ljava/util/List;",
		at = @At("RETURN"),
		cancellable = true
	)
	/**
	 * Instrumentation only: confirms Loom remap parity and logs when the
	 * server build invokes the helper.
	 */
	private static void theVirusBlock$appendGrowthCollisionsIntermediary(
			@Nullable Entity entity,
			World world,
			List<VoxelShape> original,
			Box queryBox,
			CallbackInfoReturnable<List<VoxelShape>> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Entity.findCollisions.Int");
		// Fast exit when no growth blocks exist
		if (!net.cyberpunk042.block.entity.GrowthCollisionTracker.hasAny()) {
			ctx.exit();
			return;
		}
		if (world.isClient) {
			ctx.exit();
			return;
		}
		GrowthCollisionMixinHelper.appendGrowthCollisions(entity, world, original, queryBox, cir);
		ctx.exit();
	}

	@Inject(
		method = "method_20736(Lnet/minecraft/class_1297;Lnet/minecraft/class_243;Lnet/minecraft/class_238;Lnet/minecraft/class_1937;Ljava/util/List;)Lnet/minecraft/class_243;",
		at = @At("HEAD"),
		remap = false
	)
	/**
	 * Instrumentation only: runtime equivalent of the Yarn sweep logger.
	 * Lets us diff dev vs. prod movement traces.
	 */
	private static void theVirusBlock$logPlayerMovementSweepIntermediary(
			@Nullable Entity entity,
			Vec3d movement,
			Box queryBox,
			World world,
			List<VoxelShape> vanilla,
			CallbackInfoReturnable<Vec3d> cir) {
		if (!GrowthCollisionDebug.isEnabled()) {
			return;
		}
		if (entity instanceof ServerPlayerEntity player) {
			Logging.CALLBACKS.info(
					"[GrowthCollision:sweep] (intermediary) player={} movement={} query={} baseCollisions={}",
					player.getGameProfile().getName(),
					movement,
					queryBox,
					vanilla != null ? vanilla.size() : -1);
		} else if (entity instanceof PlayerEntity player && world.isClient) {
			Logging.CALLBACKS.info(
					"[GrowthCollision:sweep-client] (intermediary) player={} movement={} query={}",
					player.getGameProfile().getName(),
					movement,
					queryBox);
		}
	}
}

