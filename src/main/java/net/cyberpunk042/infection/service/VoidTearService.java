package net.cyberpunk042.infection.service;


import net.cyberpunk042.log.Logging;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.VirusEventType;
import net.cyberpunk042.infection.CorruptionProfiler;
import net.cyberpunk042.infection.state.InfectionState;
import net.cyberpunk042.network.VoidTearBurstPayload;
import net.cyberpunk042.network.VoidTearSpawnPayload;
import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.util.VirusEquipmentHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldEvents;
import net.minecraft.registry.tag.BlockTags;

public final class VoidTearService {

	private static final int VOID_TEAR_WARNING_DELAY = 20;
	private static final int VOID_TEAR_PULL_NOTICE_INTERVAL = 5;
	private static final int MAX_PLAYER_OFFSET_ATTEMPTS = 12;
	private static final int SAMPLE_ATTEMPTS = 6;
	private static final double TEAR_BURST_VERTICAL_BOOST = 0.25D;
	
	// Force application settings
	private static final int FORCE_APPLY_INTERVAL = 1;  // Apply force every tick
	private static final double MAX_VELOCITY = 1.5D;     // Cap velocity magnitude
	private static final double EFFECT_RADIUS = 15.0D;   // Pull from 15 blocks away
	private static final double ORBITAL_STRENGTH = 0.12D; // Tangential force for orbit
	private static final double PULL_STRENGTH = 0.18D;    // Radial pull strength
	private static final double PUSH_STRENGTH = 0.8D;     // Final burst push strength

	private final VirusWorldState host;
	private final List<VoidTearInstance> activeTears = new ArrayList<>();
	private final List<PendingVoidTear> pendingTears = new ArrayList<>();
	private long nextTearId;

	public VoidTearService(VirusWorldState host) {
		this.host = host;
	}

	public void burstAndClear() {
		ServerWorld world = host.world();
		for (VoidTearInstance tear : activeTears) {
			sendBurst(world, tear);
		}
		activeTears.clear();
		pendingTears.clear();
	}

	public void tick() {
		ServerWorld world = host.world();
		long now = world.getTime();
		Random random = world.getRandom();
		armPending(world, random, now);
		if (activeTears.isEmpty()) {
			return;
		}
		Iterator<VoidTearInstance> iterator = activeTears.iterator();
		while (iterator.hasNext()) {
			VoidTearInstance tear = iterator.next();
			if (now >= tear.expiryTick()) {
				applyForce(world, tear, true);
				sendBurst(world, tear);
				spawnBurstEffects(world, tear);
				iterator.remove();
				continue;
			}
			applyForce(world, tear, false);
			notifyPlayersDuring(world, tear, now);
			erodeBlocks(world, tear, random);
			spawnParticles(world, tear);
		}
	}

	public boolean maybeSpawn(BlockPos origin, InfectionTier tier, Random random) {
		ServerWorld world = host.world();
		if (!isVoidTearAllowed(tier) || random.nextFloat() > 0.2F) {
			return false;
		}
		boolean targetPlayer = random.nextFloat() < getPlayerVoidTearChance();
		BlockPos targetPos;
		if (targetPlayer) {
			targetPos = pickPlayerTearPos(world, random);
			if (targetPos == null) {
				return false;
			}
		} else {
			if (origin == null) {
				return false;
			}
			targetPos = sampleTearOffset(world, origin, random, 12, 6);
			if (targetPos == null) {
				return false;
			}
		}
		createVoidTear(world, targetPos, tier, targetPlayer ? "player" : "core");
		return true;
	}

	public boolean spawnViaCommand(BlockPos center) {
		ServerWorld world = host.world();
		if (center == null) {
			return false;
		}
		createVoidTear(world, center, host.tiers().currentTier(), "command");
		return true;
	}

	private void createVoidTear(ServerWorld world, BlockPos basePos, InfectionTier tier, String source) {
		BlockPos tearPos = basePos.toImmutable();
		// Fixed values - no tier scaling
		double radius = EFFECT_RADIUS;
		double pullStrength = PULL_STRENGTH;
		double damageRadius = 3.0D;  // Fixed damage radius
		float damage = 2.0F;          // Fixed damage
		int durationTicks = 120;      // 6 seconds total
		long activationTick = world.getTime() + VOID_TEAR_WARNING_DELAY;
		long tearId = ++nextTearId;
		pendingTears.add(new PendingVoidTear(
				tearId,
				tearPos,
				radius,
				pullStrength,
				damageRadius * damageRadius,
				damage,
				activationTick,
				durationTicks,
				tier.getIndex(),
				source));
		warnPlayersNearTear(world, tearPos, radius);
	}

	private void armPending(ServerWorld world, Random random, long now) {
		Iterator<PendingVoidTear> iterator = pendingTears.iterator();
		while (iterator.hasNext()) {
			PendingVoidTear pending = iterator.next();
			if (now < pending.activationTick()) {
				continue;
			}
			iterator.remove();
			activate(world, pending, random);
		}
	}

	private void activate(ServerWorld world, PendingVoidTear pending, Random random) {
		BlockPos tearPos = pending.centerBlock();
		long expiry = pending.activationTick() + pending.durationTicks();
		VoidTearInstance tear = new VoidTearInstance(
				pending.id(),
				tearPos,
				Vec3d.ofCenter(tearPos),
				pending.radius(),
				pending.pullStrength(),
				pending.damageRadiusSq(),
				pending.damage(),
				expiry,
				pending.durationTicks(),
				pending.tierIndex());
		activeTears.add(tear);
		carveVoidTearChamber(world, tearPos, pending.radius());
		sendSpawn(world, tear);
		world.syncWorldEvent(WorldEvents.COMPOSTER_USED, tearPos, 0);
		int affected = applyForce(world, tear, false);
		erodeBlocks(world, tear, random);
		spawnParticles(world, tear);
		CorruptionProfiler.logTierEvent(world, VirusEventType.VOID_TEAR,
				tearPos,
				"source=" + pending.source() + " affected=" + affected + " radius=" + pending.radius() + " duration=100");
		Logging.SINGULARITY.info("[voidTear] source={} affected={} radius={} duration=100",
				pending.source(),
				affected,
				pending.radius());
	}

	private boolean isVoidTearAllowed(InfectionTier tier) {
		int tierIndex = tier.getIndex();
		int tierThreeIndex = InfectionTier.THREE.getIndex();
		if (tierIndex < tierThreeIndex) {
			return false;
		}
		if (tierIndex == tierThreeIndex) {
			return true;
		}
		VirusDifficulty difficulty = host.tiers().difficulty();
		return difficulty == VirusDifficulty.HARD || difficulty == VirusDifficulty.EXTREME;
	}

	private float getPlayerVoidTearChance() {
		return switch (host.tiers().difficulty()) {
			case EXTREME -> 0.4F;
			case HARD -> 0.25F;
			default -> 0.15F;
		};
	}

	private BlockPos pickPlayerTearPos(ServerWorld world, Random random) {
		List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
		if (players.isEmpty()) {
			return null;
		}
		ServerPlayerEntity player = players.get(random.nextInt(players.size()));
		return sampleTearOffset(world, player.getBlockPos(), random, 16, 8);
	}

	private BlockPos sampleTearOffset(ServerWorld world, BlockPos base, Random random, int horizontalRange, int verticalRange) {
		if (base == null) {
			return null;
		}
		BlockPos candidate = base;
		for (int attempts = 0; attempts < MAX_PLAYER_OFFSET_ATTEMPTS; attempts++) {
			int offsetX = random.nextBetween(-horizontalRange, horizontalRange);
			int offsetY = random.nextBetween(-verticalRange, verticalRange);
			int offsetZ = random.nextBetween(-horizontalRange, horizontalRange);
			BlockPos test = base.add(offsetX, offsetY, offsetZ);
			int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, test.getX(), test.getZ());
			int clampedY = MathHelper.clamp(test.getY(), world.getBottomY() + 4, topY - 4);
			test = new BlockPos(test.getX(), clampedY, test.getZ());
			if (!world.isChunkLoaded(ChunkPos.toLong(test))) {
				continue;
			}
			return test;
		}
		return candidate;
	}

	private void warnPlayersNearTear(ServerWorld world, BlockPos center, double radius) {
		double warnRange = Math.max(20.0D, radius * 2.5D);
		double warnRangeSq = warnRange * warnRange;
		Vec3d centerVec = Vec3d.ofCenter(center);
		Text warning = Text.translatable("message.the-virus-block.void_tear.warning").formatted(Formatting.LIGHT_PURPLE);
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.squaredDistanceTo(centerVec) <= warnRangeSq) {
				player.sendMessage(warning, true);
			}
		}
	}

	private void notifyPlayersDuring(ServerWorld world, VoidTearInstance tear, long now) {
		if ((now + tear.id()) % VOID_TEAR_PULL_NOTICE_INTERVAL != 0) {
			return;
		}
		double notifyRadius = Math.max(8.0D, tear.radius() * 1.6D);
		double notifyRadiusSq = notifyRadius * notifyRadius;
		Vec3d center = tear.centerVec();
		long ticksRemaining = tear.expiryTick() - now;
		double duration = Math.max(1.0D, tear.durationTicks());
		double lifeFrac = MathHelper.clamp(1.0D - (ticksRemaining / duration), 0.0D, 1.0D);
		boolean pushPhase = finalBlastPhase(lifeFrac);
		boolean pullPhase = !pushPhase && lifeFrac < 0.5D;
		if (!pushPhase && !pullPhase) {
			return;
		}
		Text message = pushPhase
				? Text.translatable("message.the-virus-block.void_tear.push").formatted(Formatting.LIGHT_PURPLE)
				: Text.translatable("message.the-virus-block.void_tear.pull").formatted(Formatting.LIGHT_PURPLE);
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			if (player.squaredDistanceTo(center) <= notifyRadiusSq) {
				player.sendMessage(message, true);
			}
		}
	}

	private boolean finalBlastPhase(double lifeFraction) {
		// Only final blast on expiry, pull phase is 0-85%
		return lifeFraction >= 0.85D;
	}

	private int applyForce(ServerWorld world, VoidTearInstance tear, boolean finalBlast) {
		long now = world.getTime();
		double effectRadius = Math.max(6.0D, tear.radius() * 1.4D);
		Box box = new Box(tear.centerBlock()).expand(effectRadius);
		int affected = 0;
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, Entity::isAlive)) {
			if (host.shieldFieldService().isShielding(living.getBlockPos())) {
				continue;
			}
			Vec3d center = tear.centerVec();
			Vec3d offset;
			long ticksRemaining = tear.expiryTick() - now;
			double duration = Math.max(1.0D, tear.durationTicks());
			double lifeFrac = MathHelper.clamp(1.0D - (ticksRemaining / duration), 0.0D, 1.0D);
			boolean pushPhase = finalBlast || finalBlastPhase(lifeFrac);
			boolean pullPhase = !pushPhase && lifeFrac < 0.5D;
			if (!pullPhase && !pushPhase) {
				continue;
			}
			offset = pushPhase ? living.getPos().subtract(center) : center.subtract(living.getPos());
			double distanceSq = offset.lengthSquared();
			if (distanceSq < 1.0E-4) {
				continue;
			}
			double distance = Math.sqrt(distanceSq);
			if (distance > effectRadius) {
				continue;
			}
			if (living instanceof ServerPlayerEntity player && VirusEquipmentHelper.hasHeavyPants(player)) {
				accumulateHeavyPantsWear(player);
				continue;
			}
			double proximity = 1.0D - Math.min(1.0D, distance / effectRadius);
			Vec3d direction = offset.normalize();
			double baseStrength = tear.pullStrength() * (pushPhase ? 4.2D : 3.2D);
			double strength = baseStrength * (0.15D + 0.85D * proximity);
			Vec3d impulse = direction.multiply(strength);
			double vertical = MathHelper.clamp(impulse.y + (pushPhase ? TEAR_BURST_VERTICAL_BOOST * 0.5D : 0.0D),
					-0.25D,
					0.25D);
			living.addVelocity(impulse.x, vertical, impulse.z);
			living.velocityModified = true;
			living.velocityDirty = true;
			living.fallDistance = 0.0F;
			affected++;
			if (pullPhase && distanceSq <= tear.damageRadiusSq()) {
				living.damage(world, world.getDamageSources().explosion(null, null), tear.damage());
			}
		}
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box, entity -> !entity.isRemoved())) {
			Vec3d delta = tear.centerVec().subtract(item.getPos());
			double distance = delta.length();
			if (distance < 0.1D) {
				continue;
			}
			double proximity = 1.0D - Math.min(1.0D, distance / effectRadius);
			if (proximity <= 0.0D) {
				continue;
			}
			Vec3d impulse = delta.normalize().multiply(tear.pullStrength() * 0.5D * proximity);
			item.addVelocity(impulse.x, impulse.y * 0.1D, impulse.z);
			item.velocityDirty = true;
		}
		return affected;
	}

	private void erodeBlocks(ServerWorld world, VoidTearInstance tear, Random random) {
		int attempts = Math.max(2, 2 + tear.tierIndex());
		double radiusSq = tear.radius() * tear.radius();
		for (int i = 0; i < attempts; i++) {
			BlockPos target = sampleBlock(random, tear, radiusSq);
			if (!world.isChunkLoaded(ChunkPos.toLong(target))) {
				continue;
			}
			if (host.shieldFieldService().isShielding(target)) {
				continue;
			}
			BlockState state = world.getBlockState(target);
			if (state.isAir()
					|| state.getHardness(world, target) < 0.0F
					|| host.singularity().fusing().isVirusCoreBlock(target, state)
					|| state.isOf(ModBlocks.SINGULARITY_BLOCK)) {
				continue;
			}
			if (state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
				world.setBlockState(target, ModBlocks.CORRUPTED_STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
			} else {
				world.setBlockState(target, ModBlocks.CORRUPTED_GLASS.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
	}

	private BlockPos sampleBlock(Random random, VoidTearInstance tear, double radiusSq) {
		for (int tries = 0; tries < SAMPLE_ATTEMPTS; tries++) {
			double dx = (random.nextDouble() * 2.0D - 1.0D) * tear.radius();
			double dy = (random.nextDouble() * 2.0D - 1.0D) * tear.radius();
			double dz = (random.nextDouble() * 2.0D - 1.0D) * tear.radius();
			if (dx * dx + dy * dy + dz * dz > radiusSq) {
				continue;
			}
			return BlockPos.ofFloored(tear.centerVec().x + dx, tear.centerVec().y + dy, tear.centerVec().z + dz);
		}
		return tear.centerBlock();
	}

	private void spawnParticles(ServerWorld world, VoidTearInstance tear) {
		world.spawnParticles(
				ParticleTypes.REVERSE_PORTAL,
				tear.centerVec().x,
				tear.centerVec().y,
				tear.centerVec().z,
				24,
				tear.radius() * 0.2D,
				tear.radius() * 0.2D,
				tear.radius() * 0.2D,
				0.02D);
	}

	private void spawnBurstEffects(ServerWorld world, VoidTearInstance tear) {
		world.spawnParticles(
				ParticleTypes.SONIC_BOOM,
				tear.centerVec().x,
				tear.centerVec().y,
				tear.centerVec().z,
				2,
				tear.radius() * 0.1D,
				tear.radius() * 0.1D,
				tear.radius() * 0.1D,
				0.0D);
		world.playSound(
				null,
				tear.centerBlock(),
				SoundEvents.ENTITY_WITHER_BREAK_BLOCK,
				SoundCategory.HOSTILE,
				0.8F,
				0.5F);
	}

	private void sendSpawn(ServerWorld world, VoidTearInstance tear) {
		VoidTearSpawnPayload payload = new VoidTearSpawnPayload(
				tear.id(),
				tear.centerVec().x,
				tear.centerVec().y,
				tear.centerVec().z,
				(float) tear.radius(),
				tear.durationTicks(),
				tear.tierIndex());
		host.collapseModule().watchdog().presentationService().broadcastPayload(host.world(), payload);
	}

	private void sendBurst(ServerWorld world, VoidTearInstance tear) {
		VoidTearBurstPayload payload = new VoidTearBurstPayload(
				tear.id(),
				tear.centerVec().x,
				tear.centerVec().y,
				tear.centerVec().z,
				(float) tear.radius());
		host.collapseModule().watchdog().presentationService().broadcastPayload(host.world(), payload);
	}

	private void carveVoidTearChamber(ServerWorld world, BlockPos center, double radius) {
		int carveRadius = Math.max(2, MathHelper.ceil(radius * 0.325D));
		int depth = Math.max(2, carveRadius + 1);
		int radiusSq = carveRadius * carveRadius;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int dx = -carveRadius; dx <= carveRadius; dx++) {
			for (int dz = -carveRadius; dz <= carveRadius; dz++) {
				if (dx * dx + dz * dz > radiusSq) {
					continue;
				}
				for (int dy = 0; dy <= depth; dy++) {
					mutable.set(center.getX() + dx, center.getY() - dy, center.getZ() + dz);
					if (!world.isChunkLoaded(ChunkPos.toLong(mutable)) || host.shieldFieldService().isShielding(mutable)) {
						continue;
					}
					BlockState state = world.getBlockState(mutable);
					if (!state.isAir() && !isVoidTearProtected(state)) {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}
				for (int dy = 1; dy <= 2; dy++) {
					mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (!world.isChunkLoaded(ChunkPos.toLong(mutable)) || host.shieldFieldService().isShielding(mutable)) {
						continue;
					}
					BlockState state = world.getBlockState(mutable);
					if (!state.isAir() && !isVoidTearProtected(state)) {
						world.setBlockState(mutable, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
					}
				}
			}
		}
	}

	private boolean isVoidTearProtected(BlockState state) {
		return state.isOf(Blocks.OBSIDIAN)
				|| state.isOf(Blocks.CRYING_OBSIDIAN)
				|| state.isOf(Blocks.NETHER_PORTAL)
				|| state.isOf(Blocks.END_PORTAL)
				|| state.isOf(Blocks.END_PORTAL_FRAME)
				// Protect virus core blocks from void tear destruction
				|| state.isOf(ModBlocks.VIRUS_BLOCK)
				|| state.isOf(ModBlocks.SINGULARITY_BLOCK);
	}

	private void accumulateHeavyPantsWear(ServerPlayerEntity player) {
		InfectionState state = host.infectionState();
		Object2DoubleMap<UUID> heavyPantsWear = state.heavyPantsVoidWear();
		UUID uuid = player.getUuid();
		double accumulated = heavyPantsWear.getOrDefault(uuid, 0.0D) + host.collapseConfig().heavyPantsVoidTearWear();
		int damage = (int) accumulated;
		if (damage > 0) {
			VirusEquipmentHelper.damageHeavyPants(player, damage);
			accumulated -= damage;
		}
		heavyPantsWear.put(uuid, accumulated);
	}

	private record VoidTearInstance(
			long id,
			BlockPos centerBlock,
			Vec3d centerVec,
			double radius,
			double pullStrength,
			double damageRadiusSq,
			float damage,
			long expiryTick,
			int durationTicks,
			int tierIndex) {
	}

	private record PendingVoidTear(
			long id,
			BlockPos centerBlock,
			double radius,
			double pullStrength,
			double damageRadiusSq,
			float damage,
			long activationTick,
			int durationTicks,
			int tierIndex,
			String source) {
	}
}

