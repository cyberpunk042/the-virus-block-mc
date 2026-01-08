package net.cyberpunk042.mixin;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.field.influence.CombatTracker;
import net.cyberpunk042.field.influence.FieldEvent;
import net.cyberpunk042.field.influence.TriggerEventDispatcher;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.registry.ModStatusEffects;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	private static final int CORRUPTED_REGEN_DURATION = 80;
	private static final int CORRUPTED_REGEN_REFRESH_THRESHOLD = 20;

	private LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}


	@Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
	private void theVirusBlock$adjustDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (source.isIn(DamageTypeTags.IS_FIRE) && self instanceof PlayerEntity player && player.isInLava()) {
			VirusWorldState state = VirusWorldState.get(world);
			if (state.tiers().areLiquidsCorrupted(world)) {
				player.extinguish();
				theVirusBlock$grantCorruptedRegen(player);
				cir.setReturnValue(false);
				return;
			}
		}

		Entity attackerEntity = source.getAttacker();
		if (!(attackerEntity instanceof LivingEntity attacker)) {
			return;
		}

		if (attacker.getWorld() != self.getWorld()) {
			return;
		}
		if (!world.getGameRules().getBoolean(TheVirusBlock.VIRUS_MOB_FRIENDLY_FIRE)
				&& self instanceof MobEntity && attacker instanceof MobEntity
				&& VirusMobAllyHelper.isAlly(self) && VirusMobAllyHelper.isAlly(attacker)) {
			((MobEntity) attacker).setTarget(null);
			cir.setReturnValue(false);
			return;
		}
	}

	@ModifyVariable(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
			at = @At("HEAD"), argsOnly = true, index = 3)
	private float theVirusBlock$personalShieldBeamResistance(float amount, ServerWorld world, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (amount <= 0.0F) {
			return amount;
		}
		if (self.hasStatusEffect(ModStatusEffects.PERSONAL_SHIELD)
				&& source.getAttacker() instanceof GuardianEntity guardian
				&& guardian.getCommandTags().contains(TheVirusBlock.VIRUS_DEFENSE_BEAM_TAG)) {
			return amount * 0.25F;
		}
		return amount;
	}

	/**
	 * F156: Dispatch PLAYER_DAMAGE event when a player takes damage.
	 * F166: Hook CombatTracker for binding sources.
	 */
	@Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
			at = @At(value = "RETURN"), require = 1)
	private void theVirusBlock$dispatchDamageEvent(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		// Only dispatch if damage was actually applied (return value is true) and entity is a player
		if (cir.getReturnValue() && self instanceof PlayerEntity player) {
			// F156: Trigger event for visual effects
			TriggerEventDispatcher.dispatch(FieldEvent.PLAYER_DAMAGE, player, amount);
			// F166: Track combat state for bindings
			CombatTracker.onDamageTaken(player, amount);
		}
	}

	/**
	 * F156: Dispatch PLAYER_HEAL event when a player heals.
	 */
	@Inject(method = "heal(F)V", at = @At("TAIL"), require = 1)
	private void theVirusBlock$dispatchHealEvent(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof PlayerEntity player && amount > 0.0F) {
			TriggerEventDispatcher.dispatch(FieldEvent.PLAYER_HEAL, player, amount);
		}
	}

	@Inject(method = "baseTick", at = @At("TAIL"))
	private void theVirusBlock$lavaBlessing(CallbackInfo ci) {
		net.cyberpunk042.util.SuperProfiler.start("Mixin:LivingEntity.tick");
		
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof PlayerEntity player) || !(player.getWorld() instanceof ServerWorld serverWorld)) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:LivingEntity.tick");
			return;
		}

		// Only do work if player is in lava or water
		boolean inLava = player.isInLava();
		boolean inWater = player.isTouchingWater() || player.isSubmergedInWater();
		if (!inLava && !inWater) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:LivingEntity.tick");
			return; // Fast exit if not in liquid
		}
		
		VirusWorldState state = VirusWorldState.get(serverWorld);
		boolean liquidsCorrupted = state.tiers().areLiquidsCorrupted(serverWorld);
		if (!liquidsCorrupted) {
			net.cyberpunk042.util.SuperProfiler.end("Mixin:LivingEntity.tick");
			return;
		}
		
		if (inLava) {
			player.extinguish();
			theVirusBlock$grantCorruptedRegen(player);
		}

		if (inWater && player instanceof ServerPlayerEntity serverPlayer) {
			int tier = state.tiers().currentTier().getIndex();
			theVirusBlock$degradeArmorInWater(serverPlayer, tier, serverWorld);
		}
		
		net.cyberpunk042.util.SuperProfiler.end("Mixin:LivingEntity.tick");
	}

	private void theVirusBlock$degradeArmorInWater(ServerPlayerEntity player, int tier, ServerWorld world) {
		if (player.age % 20 != 0) {
			return;
		}
		float chance = Math.min(0.95F, 0.2F + tier * 0.05F);
		if (player.getRandom().nextFloat() > chance) {
			return;
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (!slot.isArmorSlot()) {
				continue;
			}
			ItemStack stack = player.getEquippedStack(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (isNetheriteArmor(stack)) {
				continue;
			}
			stack.damage(1 + tier, world, player, item -> player.sendEquipmentBreakStatus(item, slot));
		}
	}

	private static boolean isNetheriteArmor(ItemStack stack) {
		return stack.isOf(Items.NETHERITE_HELMET)
				|| stack.isOf(Items.NETHERITE_CHESTPLATE)
				|| stack.isOf(Items.NETHERITE_LEGGINGS)
				|| stack.isOf(Items.NETHERITE_BOOTS);
	}

	private static void theVirusBlock$grantCorruptedRegen(PlayerEntity player) {
		StatusEffectInstance current = player.getStatusEffect(StatusEffects.REGENERATION);
		if (current != null && current.getDuration() > CORRUPTED_REGEN_REFRESH_THRESHOLD) {
			return;
		}
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, CORRUPTED_REGEN_DURATION, 0, false, true));
	}
}


