package net.cyberpunk042.mixin;


import net.cyberpunk042.log.Logging;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionMixinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * Diagnostic mixin for the Yarn-mapped `Entity` class.
 * None of its injections are required for collision correctness;
 * they simply log and mirror vanilla collision state so we can
 * verify that dev builds still hit the same call sites as runtime jars.
 */
@Mixin(Entity.class)
public abstract class EntityGrowthCollisionMixin {
	static {
		if (GrowthCollisionDebug.isEnabled()) {
			Logging.CALLBACKS.info("[GrowthCollision] Entity mixin initialized");
		}
	}

	@Inject(
		method = "findCollisionsForMovement(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/util/math/Box;)Ljava/util/List;",
		at = @At("RETURN"),
		cancellable = true
	)
	/**
	 * Instrumentation only: mirrors the runtime/intermediary hook so we can
	 * confirm the Yarn mapping still points at the correct helper.
	 */
	private static void theVirusBlock$appendGrowthCollisions(
			@Nullable Entity entity,
			World world,
			List<VoxelShape> original,
			Box queryBox,
			CallbackInfoReturnable<List<VoxelShape>> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("Entity.findCollisions");
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

}

