package net.cyberpunk042.mixin;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionMixinHelper;
import net.cyberpunk042.server.raycast.GrowthServerRaycast;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;


/**
 * Mixed bag of hooks for player/server interactions.
 * - `onPlayerInteractBlock` guard remains gameplay-critical for growth placement.
 * - `theVirusBlock$injectGrowthCollisionsIntoValidation` is the only collision mixin
 *   that is absolutely required: it feeds growth shapes into the anti-cheat validator
 *   so players can no longer phase through shells.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

	@Shadow
	public ServerPlayerEntity player;

	@Shadow
	private Vec3d requestedTeleportPos;

	@Shadow
	protected abstract void updateSequence(int sequence);

	@Shadow
	private static native boolean canPlace(ServerPlayerEntity player, ItemStack stack);

	/**
	 * Gameplay-critical: ensures Growth Blocks respect server-side raycasts and
	 * packet ordering before players interact. Not related to the collision fix.
	 */
	@Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$handleGrowthInteraction(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
		if (player == null || !player.isLoaded()) {
			return;
		}

		ServerWorld world = player.getWorld();
		BlockPos basePos = packet.getBlockHitResult().getBlockPos();
		BlockEntity blockEntity = world.getBlockEntity(basePos);
		if (!(blockEntity instanceof ProgressiveGrowthBlockEntity growth)) {
			return;
		}

		this.updateSequence(packet.getSequence());
		Hand hand = packet.getHand();
		ItemStack stack = player.getStackInHand(hand);
		if (!stack.isItemEnabled(world.getEnabledFeatures())) {
			ci.cancel();
			return;
		}

		player.updateLastActionTime();
		BlockHitResult serverHit = GrowthServerRaycast.serverRaycast(player, growth);
		if (serverHit == null) {
			ci.cancel();
			return;
		}

		BlockPos hitPos = serverHit.getBlockPos();
		if (!player.canInteractWithBlockAt(hitPos, 1.0D)) {
			ci.cancel();
			return;
		}

		int topY = world.getTopYInclusive();
		if (hitPos.getY() > topY) {
			player.sendMessageToClient(Text.translatable("build.tooHigh", topY).formatted(Formatting.RED), true);
			this.postGrowthBlockUpdates(world, hitPos, serverHit.getSide());
			ci.cancel();
			return;
		}

		if (this.requestedTeleportPos == null && world.canEntityModifyAt(player, hitPos)) {
			ActionResult actionResult = player.interactionManager.interactBlock(player, world, stack, hand, serverHit);
			if (actionResult.isAccepted()) {
				Criteria.ANY_BLOCK_USE.trigger(player, hitPos, stack.copy());
			}

			if (serverHit.getSide() == Direction.UP && !actionResult.isAccepted() && hitPos.getY() >= topY && canPlace(player, stack)) {
				player.sendMessageToClient(Text.translatable("build.tooHigh", topY).formatted(Formatting.RED), true);
			} else if (actionResult instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.SERVER) {
				player.swingHand(hand, true);
			}
		}

		this.postGrowthBlockUpdates(world, hitPos, serverHit.getSide());
		ci.cancel();
	}

	/**
	 * Critical collision fix: wraps the validator's `WorldView.getCollisions` call so
	 * the server sees growth shapes before running anti-cheat checks. Disable this
	 * and players will immediately regain the “sweet spot” glide exploit.
	 */
	@WrapOperation(
		method = "isEntityNotCollidingWithBlocks(Lnet/minecraft/world/WorldView;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;DDD)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/WorldView;getCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Lnet/minecraft/util/math/Vec3d;)Ljava/lang/Iterable;"
		)
	)
	private Iterable<VoxelShape> theVirusBlock$injectGrowthCollisionsIntoValidation(
			WorldView collisionView,
			@Nullable Entity entity,
			Box queryBox,
			Vec3d movementReference,
			Operation<Iterable<VoxelShape>> original) {
		net.cyberpunk042.util.SuperProfiler.start("Mixin:AntiCheat.collision");
		
		// Fast exit when no growth blocks exist - most common case
		if (!net.cyberpunk042.block.entity.GrowthCollisionTracker.hasAny()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:AntiCheat.collision");
			return original.call(collisionView, entity, queryBox, movementReference);
		}
		Iterable<VoxelShape> vanilla = original.call(collisionView, entity, queryBox, movementReference);
		if (GrowthCollisionDebug.disableAntiCheatCollisions()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:AntiCheat.collision");
			return vanilla;
		}
		if (!(entity instanceof ServerPlayerEntity) || !(collisionView instanceof World world) || world.isClient) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:AntiCheat.collision");
			return vanilla;
		}
		List<VoxelShape> extras = GrowthCollisionMixinHelper.gatherGrowthCollisions(entity, world, queryBox, vanilla);
		if (extras.isEmpty()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:AntiCheat.collision");
			return vanilla;
		}
		// Optimize merging
		if (vanilla == null || !vanilla.iterator().hasNext()) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:AntiCheat.collision");
			return extras;
		}
		List<VoxelShape> merged = new ArrayList<>();
		for (VoxelShape shape : vanilla) {
			merged.add(shape);
		}
		merged.addAll(extras);
		net.cyberpunk042.util.SuperProfiler.end("Mixin:AntiCheat.collision");
		return merged;
	}

	private void postGrowthBlockUpdates(ServerWorld world, BlockPos pos, Direction side) {
		ServerPlayNetworkHandler self = (ServerPlayNetworkHandler) (Object) this;
		self.sendPacket(new BlockUpdateS2CPacket(world, pos));
		self.sendPacket(new BlockUpdateS2CPacket(world, pos.offset(side)));
	}
}

