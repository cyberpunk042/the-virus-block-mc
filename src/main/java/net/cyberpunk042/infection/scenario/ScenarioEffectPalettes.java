package net.cyberpunk042.infection.scenario;

import java.util.Locale;
import java.util.function.Consumer;

import net.cyberpunk042.infection.events.CollapseChunkVeilEvent;
import net.cyberpunk042.infection.events.CoreChargeTickEvent;
import net.cyberpunk042.infection.events.CoreDetonationEvent;
import net.cyberpunk042.infection.events.DissipationTickEvent;
import net.cyberpunk042.infection.events.RingChargeTickEvent;
import net.cyberpunk042.infection.events.RingPulseEvent;
import net.cyberpunk042.infection.service.ServiceConfig;
import net.minecraft.block.BlockState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

final class ScenarioEffectPalettes {
	private ScenarioEffectPalettes() {
	}

	static ScenarioEffectBehavior fromPalette(EffectPaletteConfig palette,
			ServiceConfig.Effects effects,
			ServiceConfig.Audio audio) {
		return new ScenarioEffectBehavior(
				coreCharge(palette.coreCharge(), effects, audio),
				coreDetonation(palette.coreDetonation(), effects, audio),
				ringCharge(palette.ringCharge(), effects),
				ringPulse(palette.ringPulse(), effects, audio),
				dissipation(palette.dissipation(), effects, audio),
				collapseVeil(palette.collapseVeil(), effects, audio));
	}

	private static Consumer<CoreChargeTickEvent> coreCharge(EffectPaletteConfig.SimpleEvent config,
			ServiceConfig.Effects effects,
			ServiceConfig.Audio audio) {
		if (config == null) {
			return null;
		}
		ParticleEffect particle = resolveParticle(config.particle());
		SoundInfo sound = resolveSound(config.sound());
		if ((particle == null || !effects.core) && (sound == null || !audio.core)) {
			return null;
		}
		return event -> {
			if (particle != null && effects.core) {
				event.world().spawnParticles(particle,
						event.center().getX() + 0.5D,
						event.center().getY() + 0.5D,
						event.center().getZ() + 0.5D,
						6,
						0.3D,
						0.3D,
						0.3D,
						0.02D);
			}
			if (sound != null && audio.core && shouldPlayInterval(event.remainingTicks(), sound.intervalTicks())) {
				playSound(event.world(), event.center().getX() + 0.5D, event.center().getY() + 0.5D, event.center().getZ() + 0.5D, sound, 0.0F);
			}
		};
	}

	private static Consumer<CoreDetonationEvent> coreDetonation(EffectPaletteConfig.SimpleEvent config,
			ServiceConfig.Effects effects,
			ServiceConfig.Audio audio) {
		if (config == null) {
			return null;
		}
		ParticleEffect particle = resolveParticle(config.particle());
		SoundInfo sound = resolveSound(config.sound());
		if ((particle == null || !effects.core) && (sound == null || !audio.core)) {
			return null;
		}
		return event -> {
			if (sound != null && audio.core) {
				playSound(event.world(), event.center().getX() + 0.5D, event.center().getY() + 0.5D, event.center().getZ() + 0.5D, sound, 0.0F);
			}
			if (particle != null && effects.core) {
				event.world().spawnParticles(particle,
						event.center().getX() + 0.5D,
						event.center().getY() + 0.5D,
						event.center().getZ() + 0.5D,
						3,
						0.25D,
						0.25D,
						0.25D,
						0.02D);
			}
		};
	}

	private static Consumer<RingChargeTickEvent> ringCharge(EffectPaletteConfig.SimpleEvent config,
			ServiceConfig.Effects effects) {
		if (config == null || !effects.ring) {
			return null;
		}
		ParticleEffect particle = resolveParticle(config.particle());
		if (particle == null) {
			return null;
		}
		return event -> event.world().spawnParticles(particle,
				event.center().getX() + 0.5D,
				event.center().getY() + 0.5D,
				event.center().getZ() + 0.5D,
				5,
				0.25D,
				0.25D,
				0.25D,
				0.01D);
	}

	private static Consumer<RingPulseEvent> ringPulse(EffectPaletteConfig.RingPulseEvent config,
			ServiceConfig.Effects effects,
			ServiceConfig.Audio audio) {
		if (config == null) {
			return null;
		}
		ParticleEffect primary = resolveParticle(config.primaryParticle());
		ParticleEffect secondary = resolveParticle(config.secondaryParticle());
		BlockState debris = resolveBlock(config.debrisBlock());
		SoundInfo ambient = resolveSound(config.ambientSound());
		SoundInfo pulse = resolveSound(config.pulseSound());
		boolean hasParticles = effects.ring && (primary != null || secondary != null || debris != null);
		boolean hasSound = audio.ring && (ambient != null || pulse != null);
		if (!hasParticles && !hasSound) {
			return null;
		}
		return event -> {
			double radius = event.radius();
			int segments = 48;
			if (effects.ring) {
				for (int i = 0; i < segments; i++) {
					double angle = (Math.PI * 2 * i) / segments;
					double x = event.center().getX() + 0.5D + Math.cos(angle) * radius;
					double z = event.center().getZ() + 0.5D + Math.sin(angle) * radius;
					double y = event.center().getY() + event.world().random.nextGaussian() * 1.5D;
					if (primary != null) {
						event.world().spawnParticles(primary, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
					}
					if (secondary != null) {
						event.world().spawnParticles(secondary, x, y + 0.5D, z, 1, 0.0D, 0.1D, 0.0D, 0.01D);
					}
					if (debris != null) {
						event.world().spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, debris),
								x,
								y + 1.2D,
								z,
								1,
								0.0D,
								0.05D,
								0.0D,
								0.01D);
					}
				}
			}
			if (audio.ring && ambient != null && shouldPlayInterval(event.remainingTicks(), ambient.intervalTicks())) {
				playSound(event.world(), event.center().getX() + 0.5D, event.center().getY() + 0.5D, event.center().getZ() + 0.5D, ambient, 0.0F);
			}
			if (audio.ring && pulse != null && shouldPlayInterval(event.remainingTicks(), pulse.intervalTicks())) {
				playSound(event.world(),
						event.center().getX() + 0.5D,
						event.center().getY() + 0.5D,
						event.center().getZ() + 0.5D,
						pulse,
						0.2F);
			}
		};
	}

	private static Consumer<DissipationTickEvent> dissipation(EffectPaletteConfig.DissipationEvent config,
			ServiceConfig.Effects effects,
			ServiceConfig.Audio audio) {
		if (config == null) {
			return null;
		}
		ParticleEffect primary = resolveParticle(config.primaryParticle());
		ParticleEffect secondary = resolveParticle(config.secondaryParticle());
		SoundInfo sound = resolveSound(config.sound());
		boolean hasParticles = effects.dissipation && (primary != null || secondary != null);
		boolean hasSound = audio.dissipation && sound != null;
		if (!hasParticles && !hasSound) {
			return null;
		}
		return event -> {
			if (effects.dissipation) {
				if (primary != null) {
					event.world().spawnParticles(primary,
							event.center().getX() + 0.5D,
							event.center().getY() + 0.5D,
							event.center().getZ() + 0.5D,
							16,
							2.0D,
							2.0D,
							2.0D,
							0.05D);
				}
				if (secondary != null) {
					event.world().spawnParticles(secondary,
							event.center().getX() + 0.5D,
							event.center().getY() + 0.5D,
							event.center().getZ() + 0.5D,
							10,
							1.5D,
							1.0D,
							1.5D,
							0.03D);
				}
			}
			if (audio.dissipation && sound != null && shouldPlayRandom(event.world(), sound.intervalTicks())) {
				playSound(event.world(), event.center().getX() + 0.5D, event.center().getY() + 0.5D, event.center().getZ() + 0.5D, sound, 0.0F);
			}
		};
	}

	private static Consumer<CollapseChunkVeilEvent> collapseVeil(EffectPaletteConfig.CollapseVeilEvent config,
			ServiceConfig.Effects effects,
			ServiceConfig.Audio audio) {
		if (config == null) {
			return null;
		}
		ParticleEffect primary = resolveParticle(config.primaryParticle());
		ParticleEffect secondary = resolveParticle(config.secondaryParticle());
		SoundInfo sound = resolveSound(config.sound());
		boolean hasParticles = effects.collapseVeil && (primary != null || secondary != null);
		boolean hasSound = audio.collapseVeil && sound != null;
		if (!hasParticles && !hasSound) {
			return null;
		}
		return event -> {
			double centerX = event.chunk().getCenterX();
			double centerZ = event.chunk().getCenterZ();
			double centerY = event.singularityCenter().getY() + event.world().random.nextGaussian() * 1.5D;
			if (effects.collapseVeil) {
				int spokes = secondary != null ? 10 : 8;
				for (int i = 0; i < spokes; i++) {
					double angle = (Math.PI * 2 * i) / spokes + event.world().random.nextDouble() * 0.2D;
					double radius = 8.0D + event.world().random.nextDouble() * 4.0D;
					double x = centerX + Math.cos(angle) * radius;
					double z = centerZ + Math.sin(angle) * radius;
					double y = centerY + event.world().random.nextGaussian() * 1.5D;
					if (primary != null) {
						event.world().spawnParticles(primary, x, y, z, 3, 0.2D, 0.2D, 0.2D, 0.01D);
					}
					if (secondary != null) {
						event.world().spawnParticles(secondary, x, y + 0.8D, z, 2, 0.05D, 0.1D, 0.05D, 0.05D);
					}
				}
			}
			if (hasSound) {
				playSound(event.world(), centerX, centerY, centerZ, sound, 0.3F);
			}
		};
	}

	private static boolean shouldPlayInterval(int remainingTicks, int interval) {
		return interval <= 0 || (remainingTicks > 0 && interval > 0 && remainingTicks % interval == 0);
	}

	private static boolean shouldPlayRandom(ServerWorld world, int interval) {
		if (interval <= 1) {
			return true;
		}
		return world.random.nextInt(Math.max(1, interval)) == 0;
	}

	private static void playSound(ServerWorld world, double x, double y, double z, SoundInfo sound, float pitchJitter) {
		float pitch = sound.pitch();
		if (pitchJitter > 0.0F) {
			pitch += world.random.nextFloat() * pitchJitter;
		}
		world.playSound(null, x, y, z, sound.event(), sound.category(), sound.volume(), pitch);
	}

	private static ParticleEffect resolveParticle(EffectPaletteConfig.Particle entry) {
		if (entry == null) {
			return null;
		}
		var type = Registries.PARTICLE_TYPE.get(entry.id());
		return type instanceof ParticleEffect effect ? effect : null;
	}

	private static BlockState resolveBlock(Identifier id) {
		if (id == null) {
			return null;
		}
		var block = Registries.BLOCK.get(id);
		return block != null ? block.getDefaultState() : null;
	}

	private static SoundInfo resolveSound(EffectPaletteConfig.Sound entry) {
		if (entry == null) {
			return null;
		}
		SoundEvent event = Registries.SOUND_EVENT.get(entry.id());
		if (event == null) {
			return null;
		}
		SoundCategory category = parseCategory(entry.category());
		return new SoundInfo(event, category, entry.volume(), entry.pitch(), entry.intervalTicks());
	}

	private static SoundCategory parseCategory(String raw) {
		if (raw == null || raw.isBlank()) {
			return SoundCategory.AMBIENT;
		}
		try {
			return SoundCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return SoundCategory.AMBIENT;
		}
	}

	private record SoundInfo(SoundEvent event, SoundCategory category, float volume, float pitch, int intervalTicks) {
	}
}

