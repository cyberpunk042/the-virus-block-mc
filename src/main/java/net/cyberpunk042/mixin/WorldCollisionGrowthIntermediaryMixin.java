package net.cyberpunk042.mixin;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.collision.GrowthCollisionMixinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

@Mixin(targets = "net.minecraft.class_1941", remap = false)
public interface WorldCollisionGrowthIntermediaryMixin {

	@Inject(
			method = "method_71395(Lnet/minecraft/class_1297;Lnet/minecraft/class_238;Lnet/minecraft/class_243;)Ljava/lang/Iterable;",
			at = @At("RETURN"),
			cancellable = true)
	default void theVirusBlock$mergeGrowthCollisions(
			@Nullable Entity entity,
			Box queryBox,
			Vec3d movementReference,
			CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("World.getCollisions.Int");
		// Fast exit when no growth blocks exist
		if (!net.cyberpunk042.block.entity.GrowthCollisionTracker.hasAny()) {
			ctx.exit();
			return;
		}
		if (!(this instanceof World world) || world.isClient || entity == null) {
			ctx.exit();
			return;
		}
		Iterable<VoxelShape> vanilla = cir.getReturnValue();
		List<VoxelShape> extras = GrowthCollisionMixinHelper.gatherGrowthCollisions(entity, world, queryBox, vanilla);
		if (extras.isEmpty()) {
			ctx.exit();
			return;
		}
		// Avoid allocation when vanilla is empty
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
		ctx.exit();
	}

	@Inject(
			method = "method_20812(Lnet/minecraft/class_1297;Lnet/minecraft/class_238;)Ljava/lang/Iterable;",
			at = @At("RETURN"),
			cancellable = true)
	default void theVirusBlock$mergeGrowthBlockCollisions(
			@Nullable Entity entity,
			Box queryBox,
			CallbackInfoReturnable<Iterable<VoxelShape>> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("World.getBlockColl.Int");
		// Fast exit when no growth blocks exist
		if (!net.cyberpunk042.block.entity.GrowthCollisionTracker.hasAny()) {
			ctx.exit();
			return;
		}
		if (!(this instanceof World world) || world.isClient || entity == null) {
			ctx.exit();
			return;
		}
		Iterable<VoxelShape> vanilla = cir.getReturnValue();
		List<VoxelShape> extras = GrowthCollisionMixinHelper.gatherGrowthCollisions(entity, world, queryBox, vanilla);
		if (extras.isEmpty()) {
			ctx.exit();
			return;
		}
		// Avoid allocation when vanilla is empty
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
		ctx.exit();
	}
}

