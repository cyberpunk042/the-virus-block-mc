package net.cyberpunk042.mixin;


import net.cyberpunk042.log.Logging;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionMixinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;

/**
 * Support/observability mixin for any `CollisionView` that is also a `World`.
 * Helpful for logging and for catching non-player callers, but not the root fix—
 * the `ServerPlayNetworkHandler` wrap is what enforces collisions server-side.
 */
@Mixin(CollisionView.class)
public interface CollisionViewGrowthCollisionMixin {
	@Inject(
		method = "getCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Lnet/minecraft/util/math/Vec3d;)Ljava/lang/Iterable;",
		at = @At("RETURN"),
		cancellable = true
	)
	default void theVirusBlock$appendGrowthCollisions(
			@Nullable Entity entity,
			Box queryBox,
			Vec3d movementReference,
			CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
		// Count all calls (even early exits) for profiling
		net.cyberpunk042.util.SuperProfiler.start("Mixin:Collision.getAll");
		
		// Ultra-fast early exit - if no growth blocks exist globally, skip everything
		if (!net.cyberpunk042.block.entity.GrowthCollisionTracker.hasAny()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getAll");
			return;
		}
		if (entity == null || !(this instanceof World world) || world.isClient) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getAll");
			return;
		}
		Iterable<VoxelShape> vanilla = cir.getReturnValue();
		List<VoxelShape> extras = GrowthCollisionMixinHelper.gatherGrowthCollisions(entity, world, queryBox, vanilla);
		if (extras.isEmpty()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getAll");
			return;
		}

		// Avoid allocation if possible
		if (vanilla == null || !vanilla.iterator().hasNext()) {
			cir.setReturnValue(extras);
		} else {
			List<VoxelShape> merged = new ArrayList<>();
			for (VoxelShape shape : vanilla) {
				merged.add(shape);
			}
			merged.addAll(extras);
			cir.setReturnValue(merged);
		}
		net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getAll");
	}

	@Inject(
		method = "getBlockCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/lang/Iterable;",
		at = @At("RETURN"),
		cancellable = true
	)
	default void theVirusBlock$appendGrowthBlockCollisions(
			@Nullable Entity entity,
			Box queryBox,
			CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
		net.cyberpunk042.util.SuperProfiler.start("Mixin:Collision.getBlock");
		
		// Ultra-fast early exit
		if (!net.cyberpunk042.block.entity.GrowthCollisionTracker.hasAny()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getBlock");
			return;
		}
		if (entity == null || !(this instanceof World world) || world.isClient) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getBlock");
			return;
		}
		Iterable<VoxelShape> vanilla = cir.getReturnValue();
		List<VoxelShape> extras = GrowthCollisionMixinHelper.gatherGrowthCollisions(entity, world, queryBox, vanilla);
		if (extras.isEmpty()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getBlock");
			return;
		}

		// Avoid allocation if possible
		if (vanilla == null || !vanilla.iterator().hasNext()) {
			cir.setReturnValue(extras);
		} else {
			List<VoxelShape> merged = new ArrayList<>();
			for (VoxelShape shape : vanilla) {
				merged.add(shape);
			}
			merged.addAll(extras);
			cir.setReturnValue(merged);
		}
		net.cyberpunk042.util.SuperProfiler.end("Mixin:Collision.getBlock");
	}
}


