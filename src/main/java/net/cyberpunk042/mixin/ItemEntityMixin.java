package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cyberpunk042.infection.VirusItemAlerts;
import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
	@Unique
	private boolean theVirusBlock$burnAlertSent;
	@Unique
	private boolean theVirusBlock$trackPickup;
	@Unique
	private boolean theVirusBlock$pickupAlertSent;

	private ItemEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
			at = @At("HEAD"))
	private void theVirusBlock$onDamage(ServerWorld world, DamageSource source, float amount,
	                                    CallbackInfoReturnable<Boolean> cir) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("ItemEntity.damage");
		if (theVirusBlock$burnAlertSent) {
			ctx.exit();
			return;
		}
		if (!source.isIn(DamageTypeTags.IS_FIRE)) {
			ctx.exit();
			return;
		}
		ItemStack stack = ((ItemEntity) (Object) this).getStack();
		if (!stack.isOf(ModBlocks.VIRUS_BLOCK.asItem())) {
			ctx.exit();
			return;
		}
		VirusItemAlerts.broadcastBurn(world);
		theVirusBlock$burnAlertSent = true;
		ctx.exit();
	}

	@Inject(method = "onPlayerCollision(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
	private void theVirusBlock$preparePickup(PlayerEntity player, CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("ItemEntity.prepPickup");
		if (getWorld().isClient() || theVirusBlock$pickupAlertSent) {
			theVirusBlock$trackPickup = false;
			ctx.exit();
			return;
		}
		ItemStack stack = ((ItemEntity) (Object) this).getStack();
		theVirusBlock$trackPickup = stack.isOf(ModBlocks.VIRUS_BLOCK.asItem());
		ctx.exit();
	}

	@Inject(method = "onPlayerCollision(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("TAIL"))
	private void theVirusBlock$announcePickup(PlayerEntity player, CallbackInfo ci) {
		var ctx = net.cyberpunk042.util.MixinProfiler.enter("ItemEntity.announce");
		if (!theVirusBlock$trackPickup || theVirusBlock$pickupAlertSent || getWorld().isClient()) {
			ctx.exit();
			return;
		}
		if (!this.isRemoved()) {
			ctx.exit();
			return;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			ctx.exit();
			return;
		}
		World world = getWorld();
		if (!(world instanceof ServerWorld serverWorld)) {
			ctx.exit();
			return;
		}
		VirusItemAlerts.broadcastPickup(serverWorld, serverPlayer);
		theVirusBlock$pickupAlertSent = true;
		theVirusBlock$trackPickup = false;
		ctx.exit();
	}
}


