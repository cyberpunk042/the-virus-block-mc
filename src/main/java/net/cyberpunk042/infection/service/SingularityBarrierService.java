package net.cyberpunk042.infection.service;

import java.util.Objects;
import java.util.Set;

import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.state.TierModule;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Manages the singularity barrier - a protective field that pushes players away
 * from virus sources during the final tier before collapse.
 * 
 * Extracted from {@link SingularityLifecycleService} to improve cohesion.
 */
public final class SingularityBarrierService {

	/**
	 * Persistent state for the singularity barrier.
	 */
	public static final class State {
		public long nextPushTick;
		public boolean active;
		public boolean finalBlastTriggered;
	}

	private final VirusWorldState host;
	private final State state;

	public SingularityBarrierService(VirusWorldState host, State state) {
		this.host = Objects.requireNonNull(host, "host");
		this.state = Objects.requireNonNull(state, "state");
	}

	public State state() {
		return state;
	}

	/**
	 * Ticks the barrier logic. Active during the first half of tier 5 (max tier).
	 */
	public void tick(InfectionTier tier, int tierDuration) {
		ServerWorld world = host.world();
		if (tier.getIndex() < InfectionTier.maxIndex() || host.tiers().isApocalypseMode()) {
			resetTimers();
			deactivate();
			return;
		}
		if (tierDuration <= 0 || host.tiers().ticksInTier() >= tierDuration / 2) {
			resetTimers();
			deactivate();
			return;
		}
		Set<BlockPos> sources = host.getVirusSources();
		if (sources.isEmpty()) {
			deactivate();
			return;
		}
		long now = world.getTime();
		if (now < state.nextPushTick) {
			return;
		}
		state.nextPushTick = now + TierModule.TIER_FIVE_BARRIER_INTERVAL;
		boolean pushed = false;
		for (BlockPos source : sources) {
			if (!world.isChunkLoaded(ChunkPos.toLong(source))) {
				continue;
			}
			host.presentationCoord().pushPlayersFromBlock( source, TierModule.TIER_FIVE_BARRIER_RADIUS, 0.9D, false);
			pushed = true;
		}
		if (pushed && !state.active) {
			state.active = true;
			host.markDirty();
		}
	}

	/**
	 * Deactivates the barrier and notifies players.
	 */
	public void deactivate() {
		ServerWorld world = host.world();
		if (!state.active) {
			return;
		}
		state.active = false;
		host.infection().broadcast(world, Text.translatable("message.the-virus-block.barrier_offline").formatted(Formatting.DARK_PURPLE));
		host.markDirty();
	}

	/**
	 * Fully resets all barrier state.
	 */
	public void reset() {
		boolean dirty = state.active || state.finalBlastTriggered || state.nextPushTick != 0L;
		state.active = false;
		state.finalBlastTriggered = false;
		state.nextPushTick = 0L;
		if (dirty) {
			host.markDirty();
		}
	}

	/**
	 * Resets only the timers, preserving active state.
	 */
	public void resetTimers() {
		boolean dirty = state.finalBlastTriggered || state.nextPushTick != 0L;
		state.finalBlastTriggered = false;
		state.nextPushTick = 0L;
		if (dirty) {
			host.markDirty();
		}
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// State accessors (for persistence and external control)
	// ─────────────────────────────────────────────────────────────────────────────

	public boolean isActive() {
		return state.active;
	}

	public void setActive(boolean active) {
		if (state.active != active) {
			state.active = active;
			host.markDirty();
		}
	}

	public long getNextPushTick() {
		return state.nextPushTick;
	}

	public void setNextPushTick(long tick) {
		state.nextPushTick = tick;
	}

	public boolean isFinalBlastTriggered() {
		return state.finalBlastTriggered;
	}

	public void setFinalBlastTriggered(boolean triggered) {
		if (state.finalBlastTriggered != triggered) {
			state.finalBlastTriggered = triggered;
			host.markDirty();
		}
	}
}

