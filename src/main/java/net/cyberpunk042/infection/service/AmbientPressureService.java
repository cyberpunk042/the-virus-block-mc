package net.cyberpunk042.infection.service;

import java.util.List;
import java.util.Objects;
import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.util.VirusMobAllyHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;

/**
 * Applies ambient pressure effects (bonus spawns + health penalties) while the infection is active.
 */
public final class AmbientPressureService {

	private static final Identifier EXTREME_HEALTH_MODIFIER_ID =
			Identifier.of(TheVirusBlock.MOD_ID, "extreme_health_penalty");

	// Mob caps for ambient pressure spawning (to prevent lag)
	private static final int CAP_ZOMBIE = 50;
	private static final int CAP_SKELETON = 50;
	private static final int CAP_SPIDER = 50;
	private static final int CAP_HUSK = 30;
	private static final int CAP_DROWNED = 30;
	private static final int CAP_ENDERMAN = 20;

	private final VirusWorldState host;

	public AmbientPressureService(VirusWorldState host) {
		this.host = Objects.requireNonNull(host, "host");
	}

	public void tick(InfectionTier tier) {
		ServerWorld world = host.world();
		boostAmbientSpawns(world, tier);
		applyDifficultyRules(tier);
	}

	public void applyDifficultyRules(InfectionTier tier) {
		ServerWorld world = host.world();
		applyDifficultyRulesInternal(world, tier);
	}

	private void boostAmbientSpawns(ServerWorld world, InfectionTier tier) {
		if (host.singularityState().singularityState != SingularityState.DORMANT) {
			return;
		}
		if (!world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
			return;
		}
		
		// Only spawn every 100 ticks (5 seconds) instead of every tick
		if (host.infectionState().totalTicks() % 100 != 0) {
			return;
		}
		
		VirusDifficulty difficulty = host.tiers().difficulty();
		int difficultyScale = switch (difficulty) {
			case EASY -> 0;
			case MEDIUM -> 1;
			case HARD -> 2;
			case EXTREME -> 3;
		};
		if (difficultyScale <= 0) {
			return;
		}
		int tierIndex = tier.getIndex();
		// No mob spawning at tier 1 - start at tier 2
		if (tierIndex < 1) {
			return;
		}
		// Use tier multiplier with a very low base to keep numbers reasonable
		float tierMultiplier = tier.getMobSpawnMultiplier();
		float baseAttempts = 0.5F * difficultyScale; // Much lower base than before
		float scaledAttempts = baseAttempts * tierMultiplier;
		if (host.tiers().isApocalypseMode()) {
			scaledAttempts += 1.0F;
		}
		int attempts = Math.max(1, Math.min(4, Math.round(scaledAttempts))); // Cap at 4
		
		Random random = world.getRandom();
		List<ServerPlayerEntity> anchors = world.getPlayers(player -> player.isAlive()
				&& !player.isSpectator()
				&& host.combat().isWithinAura(player.getBlockPos()));
		if (anchors.isEmpty()) {
			return;
		}
		for (int i = 0; i < attempts; i++) {
			ServerPlayerEntity anchor = anchors.get(random.nextInt(anchors.size()));
			BlockPos spawnPos = findBoostSpawnPos(world, anchor.getBlockPos(), 16 + tierIndex * 6, random);
			if (spawnPos == null) {
				continue;
			}
			EntityType<? extends MobEntity> type = pickBoostMobType(tierIndex, random);
			if (type == null) {
				continue;
			}
			// Check mob cap before spawning
			if (isAtMobCap(world, type)) {
				continue;
			}
			MobEntity spawned = type.spawn(world,
					entity -> entity.refreshPositionAndAngles(spawnPos, random.nextFloat() * 360.0F, 0.0F),
					spawnPos,
					SpawnReason.EVENT,
					true,
					false);
			VirusMobAllyHelper.mark(spawned);
		}
	}

	/**
	 * Checks if the world has reached the ambient cap for this mob type.
	 */
	private boolean isAtMobCap(ServerWorld world, EntityType<? extends MobEntity> type) {
		int cap = getMobCap(type);
		if (cap <= 0) {
			return false; // No cap
		}
		// Count entities of this type in the world
		int count = world.getEntitiesByType(type, entity -> entity.isAlive()).size();
		return count >= cap;
	}

	private int getMobCap(EntityType<?> type) {
		if (type == EntityType.ZOMBIE) return CAP_ZOMBIE;
		if (type == EntityType.SKELETON) return CAP_SKELETON;
		if (type == EntityType.SPIDER) return CAP_SPIDER;
		if (type == EntityType.HUSK) return CAP_HUSK;
		if (type == EntityType.DROWNED) return CAP_DROWNED;
		if (type == EntityType.ENDERMAN) return CAP_ENDERMAN;
		return 100; // Default cap for unlisted types
	}

	private BlockPos findBoostSpawnPos(ServerWorld world, BlockPos origin, int radius, Random random) {
		for (int tries = 0; tries < 6; tries++) {
			int dx = random.nextBetween(-radius, radius);
			int dz = random.nextBetween(-radius, radius);
			int x = origin.getX() + dx;
			int z = origin.getZ() + dz;
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			// Optimized: check chunk first without allocating BlockPos
			if (!world.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4))) {
				continue;
			}
			BlockPos pos = new BlockPos(x, y, z);
			if (!world.getWorldBorder().contains(pos)) {
				continue;
			}
			if (!world.getBlockState(pos).isAir() || world.getBlockState(pos.down()).isAir()) {
				continue;
			}
			return pos;
		}
		return null;
	}

	private EntityType<? extends MobEntity> pickBoostMobType(int tierIndex, Random random) {
		if (tierIndex >= 3 && random.nextFloat() < 0.25F) {
			return EntityType.ENDERMAN;
		}
		if (tierIndex >= 2 && random.nextFloat() < 0.35F) {
			return random.nextBoolean() ? EntityType.HUSK : EntityType.DROWNED;
		}
		return switch (random.nextInt(3)) {
			case 0 -> EntityType.ZOMBIE;
			case 1 -> EntityType.SKELETON;
			default -> EntityType.SPIDER;
		};
	}

	private void applyDifficultyRulesInternal(ServerWorld world, InfectionTier tier) {
		VirusDifficulty difficulty = host.tiers().difficulty();
		// Only EXTREME has health penalties - skip player iteration for other difficulties
		if (difficulty == VirusDifficulty.EXTREME) {
			applyExtremeHealthPenalty(world, difficulty, tier);
		} else {
			// Only clear if we might have applied penalties before
			// Check first player to avoid iterating if no modifiers exist
			List<ServerPlayerEntity> players = world.getPlayers(PlayerEntity::isAlive);
			if (!players.isEmpty()) {
				ServerPlayerEntity first = players.get(0);
				EntityAttributeInstance attr = first.getAttributeInstance(EntityAttributes.MAX_HEALTH);
				if (attr != null && attr.getModifier(EXTREME_HEALTH_MODIFIER_ID) != null) {
					clearExtremeHealthPenalty(world);
				}
			}
		}
	}

	private void applyExtremeHealthPenalty(ServerWorld world, VirusDifficulty difficulty, InfectionTier tier) {
		double penalty = difficulty.getHealthPenaltyForTier(tier.getIndex());
		if (penalty >= 0.0D) {
			clearExtremeHealthPenalty(world);
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
			if (attribute == null) {
				continue;
			}
			attribute.removeModifier(EXTREME_HEALTH_MODIFIER_ID);
			attribute.addPersistentModifier(new EntityAttributeModifier(EXTREME_HEALTH_MODIFIER_ID, penalty, Operation.ADD_VALUE));
			double max = attribute.getValue();
			if (player.getHealth() > max) {
				player.setHealth((float) max);
			}
		}
	}

	private void clearExtremeHealthPenalty(ServerWorld world) {
		for (ServerPlayerEntity player : world.getPlayers(PlayerEntity::isAlive)) {
			EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
			if (attribute != null) {
				attribute.removeModifier(EXTREME_HEALTH_MODIFIER_ID);
			}
		}
	}
}

