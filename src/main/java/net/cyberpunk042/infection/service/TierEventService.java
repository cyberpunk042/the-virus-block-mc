package net.cyberpunk042.infection.service;

import java.util.List;
import java.util.Objects;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.entity.CorruptedTntEntity;
import net.cyberpunk042.infection.CorruptionProfiler;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.TierCookbook;
import net.cyberpunk042.infection.TierFeature;
import net.cyberpunk042.infection.TierFeatureGroup;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusEventType;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.mutation.BlockMutationHelper;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldEvents;
import net.minecraft.registry.tag.BlockTags;

public final class TierEventService {

	private final VirusWorldState host;
	private final VoidTearService voidTears;

	public TierEventService(VirusWorldState host, VoidTearService voidTears) {
		this.host = Objects.requireNonNull(host, "host");
		this.voidTears = Objects.requireNonNull(voidTears, "voidTears");
	}

	private boolean canTrigger(VirusEventType type, long cooldown) {
		double modifier = Math.max(0.1D, host.tiers().difficulty().getEventOddsMultiplier());
		long adjustedCooldown = Math.max(20L, MathHelper.floor(cooldown / modifier));
		long last = host.infectionState().eventHistory().getOrDefault(type, -adjustedCooldown);
		return host.infectionState().totalTicks() - last >= adjustedCooldown;
	}

	private void markEvent(VirusEventType type) {
		host.infectionState().eventHistory().put(type, host.infectionState().totalTicks());
		host.markDirty();
	}

	public void runTierEvents(InfectionTier tier) {
		ServerWorld world = host.world();
		if (host.singularityState().singularityState != SingularityState.DORMANT) {
			return;
		}
		boolean apocalypse = host.tiers().isApocalypseMode();
		boolean tier2Active = TierCookbook.anyEnabled(world, tier, apocalypse, TierFeatureGroup.TIER2_EVENT);
		boolean tier3Active = TierCookbook.anyEnabled(world, tier, apocalypse, TierFeatureGroup.TIER3_EXTRA);
		if (!tier2Active && !tier3Active) {
			return;
		}
		Random random = world.getRandom();
		BlockPos origin = host.infection().representativePos(world, random, host.sourceState());
		if (origin == null) {
			return;
		}

		if (tier2Active) {
			maybeMutationPulse(world, tier, random);
			maybeSkyfall(world, origin, tier, random);
			maybeCollapseSurge(world, origin, tier, random);
			maybePassiveRevolt(world, origin, tier, random);
			maybeMobBuffStorm(world, origin, tier, random);
			maybeVirusBloom(world, origin, tier, random);
		}

		if (tier3Active) {
			maybeVoidTear(world, origin, tier, random);
			maybeInversion(world, origin, tier, random);
			maybeEntityDuplication(world, origin, tier, random);
		}
	}

	private void maybeMutationPulse(ServerWorld world, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_MUTATION_PULSE)) {
			return;
		}
		if (!canTrigger(VirusEventType.MUTATION_PULSE, 400)) {
			return;
		}
		if (random.nextFloat() > 0.1F + tier.getIndex() * 0.03F) {
			return;
		}
		markEvent(VirusEventType.MUTATION_PULSE);
		int pulses = 2 + tier.getIndex();
		for (int i = 0; i < pulses; i++) {
			if (host.singularity().fusing().shouldSkipSpread()) {
				break;
			}
			BlockMutationHelper.mutateAroundSources(world, host.getVirusSources(), tier, host.tiers().isApocalypseMode());
		}
		BlockPos pulseOrigin = host.infection().representativePos(world, random, host.sourceState());
		if (pulseOrigin != null) {
			world.playSound(null, pulseOrigin, SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.AMBIENT, 1.2F, 0.6F + tier.getIndex() * 0.1F);
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.MUTATION_PULSE, pulseOrigin, "pulses=" + pulses);
	}

	private void maybeSkyfall(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_SKYFALL)) {
			return;
		}
		if (!canTrigger(VirusEventType.SKYFALL, 360)) {
			return;
		}
		if (random.nextFloat() > 0.08F + tier.getIndex() * 0.03F) {
			return;
		}
		List<ServerPlayerEntity> players = world.getPlayers(player -> player.squaredDistanceTo(origin.getX(), origin.getY(), origin.getZ()) < 4096.0D);
		players.removeIf(host.shieldFieldService()::isPlayerShielded);
		if (players.isEmpty()) {
			return;
		}
		markEvent(VirusEventType.SKYFALL);
		int volleys = 8 + tier.getIndex() * 4;
		int tntSpawned = 0;
		int arrowBarrageCount = 0;
		int totalArrows = 0;
		for (ServerPlayerEntity player : players) {
			for (int i = 0; i < volleys; i++) {
				double x = player.getX() + random.nextBetween(-6, 6);
				double z = player.getZ() + random.nextBetween(-6, 6);
				int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
				double y = top + 20.0D;

				ArrowEntity arrow = new ArrowEntity(world, x, y, z, new ItemStack(Items.ARROW), null);
				arrow.addCommandTag(TheVirusBlock.CORRUPTION_PROJECTILE_TAG);
				arrow.setVelocity(0.0D, -1.6D - random.nextDouble(), 0.0D);
				world.spawnEntity(arrow);

				if (tier.getIndex() >= 1 && random.nextFloat() < 0.35F + tier.getIndex() * 0.15F) {
					CorruptedTntEntity tnt = CorruptedTntEntity.spawn(world, x, y, z, null, Math.max(15, 50 - tier.getIndex() * 8));
					tnt.addCommandTag(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG);
					tntSpawned++;
				}
			}
			totalArrows += volleys;

			if (tier.getIndex() >= 2 && random.nextFloat() < 0.4F + tier.getIndex() * 0.1F) {
				spawnArrowBarrage(world, player, tier, random);
				arrowBarrageCount++;
			}
		}
		world.syncWorldEvent(WorldEvents.FIRE_EXTINGUISHED, origin, 0);
		CorruptionProfiler.logTierEvent(world, VirusEventType.SKYFALL, origin,
				"players=" + players.size() + " arrows=" + totalArrows + " tnt=" + tntSpawned + " barrages=" + arrowBarrageCount);
	}

	private void spawnArrowBarrage(ServerWorld world, ServerPlayerEntity player, InfectionTier tier, Random random) {
		int waves = 3 + tier.getIndex() * 2;
		for (int i = 0; i < waves; i++) {
			double offsetX = player.getX() + random.nextBetween(-2, 2) + random.nextDouble();
			double offsetZ = player.getZ() + random.nextBetween(-2, 2) + random.nextDouble();
			double y = player.getY() + 10.0D + i;
			ArrowEntity arrow = new ArrowEntity(world, offsetX, y, offsetZ, new ItemStack(Items.ARROW), null);
			arrow.addCommandTag(TheVirusBlock.CORRUPTION_PROJECTILE_TAG);
			arrow.setVelocity(0.0D, -1.9D - random.nextDouble() * 0.6D, 0.0D);
			world.spawnEntity(arrow);
		}
		if (tier.getIndex() >= 3) {
			CorruptedTntEntity tnt = CorruptedTntEntity.spawn(world, player.getX(), player.getY() + 12.0D, player.getZ(), null,
					Math.max(20, 40 - tier.getIndex() * 6));
			tnt.addCommandTag(TheVirusBlock.CORRUPTION_EXPLOSIVE_TAG);
		}
	}

	private void maybeCollapseSurge(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_COLLAPSE_SURGE)) {
			return;
		}
		if (!canTrigger(VirusEventType.COLLAPSE_SURGE, 900)) {
			return;
		}
		if (random.nextFloat() > 0.1F + tier.getIndex() * 0.02F) {
			return;
		}
		markEvent(VirusEventType.COLLAPSE_SURGE);
		int columns = 0;
		for (int i = 0; i < 6 + tier.getIndex() * 2; i++) {
			BlockPos target = origin.add(random.nextBetween(-8, 8), random.nextBetween(-4, 4), random.nextBetween(-8, 8));
			if (!world.isChunkLoaded(ChunkPos.toLong(target))) {
				continue;
			}
			BlockState state = world.getBlockState(target);
			if (!state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
				continue;
			}
			// FIX: Don't spawn FallingBlockEntity - it creates zombie entities that never despawn.
			// Instead, just break the block directly. This simulates the collapse without entity overhead.
			world.breakBlock(target, false);
			columns++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.COLLAPSE_SURGE, origin, "columns=" + columns);
	}

	private void maybePassiveRevolt(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_PASSIVE_REVOLT)) {
			return;
		}
		if (!canTrigger(VirusEventType.PASSIVE_REVOLT, 800)) {
			return;
		}
		List<AnimalEntity> animals = world.getEntitiesByClass(AnimalEntity.class, new Box(origin).expand(32.0D), Entity::isAlive);
		if (animals.isEmpty()) {
			return;
		}
		markEvent(VirusEventType.PASSIVE_REVOLT);
		int revolts = Math.min(animals.size(), 2 + tier.getIndex());
		int converted = 0;
		for (int i = 0; i < revolts; i++) {
			AnimalEntity animal = animals.get(random.nextInt(animals.size()));
			BlockPos pos = animal.getBlockPos();
			animal.discard();
			MobEntity zombie = EntityType.ZOMBIE.spawn(world, entity -> {
				entity.refreshPositionAndAngles(pos, random.nextFloat() * 360.0F, 0.0F);
				entity.setCustomName(Text.translatable("entity.the-virus-block.corrupted_passive").formatted(Formatting.DARK_RED));
			}, pos, SpawnReason.EVENT, true, false);
			VirusMobAllyHelper.mark(zombie);
			converted++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.PASSIVE_REVOLT, origin, "converted=" + converted);
	}

	private void maybeMobBuffStorm(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_MOB_BUFF_STORM)) {
			return;
		}
		if (!canTrigger(VirusEventType.MOB_BUFF_STORM, 700)) {
			return;
		}
		List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, new Box(origin).expand(48.0D), Entity::isAlive);
		if (hostiles.isEmpty()) {
			return;
		}
		markEvent(VirusEventType.MOB_BUFF_STORM);
		int buffed = 0;
		for (HostileEntity mob : hostiles) {
			StatusEffectInstance effect = randomMobEffect(random, tier);
			mob.addStatusEffect(effect);
			buffed++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.MOB_BUFF_STORM, origin, "buffed=" + buffed);
	}

	private StatusEffectInstance randomMobEffect(Random random, InfectionTier tier) {
		int amplifier = tier.getIndex() >= 3 ? 1 : 0;
		int duration = 20 * (80 + random.nextBetween(0, 160));
		return switch (random.nextInt(5)) {
			case 0 -> new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier, false, true);
			case 1 -> new StatusEffectInstance(StatusEffects.STRENGTH, duration, amplifier, false, true);
			case 2 -> new StatusEffectInstance(StatusEffects.RESISTANCE, duration, amplifier, false, true);
			case 3 -> new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, false, true);
			default -> new StatusEffectInstance(StatusEffects.REGENERATION, duration, amplifier, false, true);
		};
	}

	private void maybeVirusBloom(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_VIRUS_BLOOM)) {
			return;
		}
		if (!canTrigger(VirusEventType.VIRUS_BLOOM, 900)) {
			return;
		}
		if (random.nextFloat() > 0.12F) {
			return;
		}
		markEvent(VirusEventType.VIRUS_BLOOM);
		int radius = 26 + tier.getIndex() * 6;
		BlockMutationHelper.corruptFlora(world, origin, radius);
		CorruptionProfiler.logTierEvent(world, VirusEventType.VIRUS_BLOOM, origin, "radius=" + radius);
	}

	private void maybeVoidTear(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_VOID_TEAR)) {
			return;
		}
		if (!canTrigger(VirusEventType.VOID_TEAR, 200)) {
			return;
		}
		if (voidTears.maybeSpawn(origin, tier, random)) {
			markEvent(VirusEventType.VOID_TEAR);
		}
	}

	private void maybeInversion(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_INVERSION)) {
			return;
		}
		if (!canTrigger(VirusEventType.INVERSION, 1100)) {
			return;
		}
		if (random.nextFloat() > 0.08F) {
			return;
		}
		markEvent(VirusEventType.INVERSION);
		Box box = new Box(origin).expand(32.0D);
		int affected = 0;
		for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, Entity::isAlive)) {
			entity.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 80 + tier.getIndex() * 20, 0));
			affected++;
		}
		CorruptionProfiler.logTierEvent(world, VirusEventType.INVERSION, origin, "affected=" + affected);
	}

	private void maybeEntityDuplication(ServerWorld world, BlockPos origin, InfectionTier tier, Random random) {
		if (!TierCookbook.isEnabled(world, tier, host.tiers().isApocalypseMode(), TierFeature.EVENT_ENTITY_DUPLICATION)) {
			return;
		}
		if (!canTrigger(VirusEventType.ENTITY_DUPLICATION, 1200)) {
			return;
		}
		List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, new Box(origin).expand(32.0D + tier.getIndex() * 4.0D), Entity::isAlive);
		if (mobs.isEmpty()) {
			return;
		}
		if (random.nextFloat() > 0.12F) {
			return;
		}
		markEvent(VirusEventType.ENTITY_DUPLICATION);
		MobEntity target = mobs.get(random.nextInt(mobs.size()));
		BlockPos spawnPos = target.getBlockPos().add(random.nextBetween(-2, 2), 0, random.nextBetween(-2, 2));
		Entity spawned = target.getType().spawn(world, entity -> {
			if (entity instanceof MobEntity mob) {
				mob.refreshPositionAndAngles(spawnPos, target.getYaw(), target.getPitch());
				mob.setHealth(Math.max(2.0F, target.getHealth() * 0.5F));
			}
		}, spawnPos, SpawnReason.EVENT, false, false);
		if (spawned != null && spawned instanceof MobEntity mob) {
			VirusMobAllyHelper.mark(mob);
			CorruptionProfiler.logTierEvent(world, VirusEventType.ENTITY_DUPLICATION, spawnPos, "type=" + spawned.getType());
		}
	}
}

