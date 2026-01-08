package net.cyberpunk042.infection.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.cyberpunk042.registry.ModBlocks;
import net.minecraft.util.math.random.Random;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.log.Logging;
import net.minecraft.util.math.BlockPos;

/**
 * Tracks and manages virus source blocks in the world.
 */
public final class VirusSourceService {

    public static class State {
        public final Set<BlockPos> sources = new HashSet<>();
        public final Set<BlockPos> suppressed = new HashSet<>();
    }

    private final VirusWorldState host;
    private final State state;

    public VirusSourceService(VirusWorldState host) {
        this(host, new State());
    }

    public VirusSourceService(VirusWorldState host, State data) {
        this.host = Objects.requireNonNull(host, "host");
        this.state = data != null ? data : new State();
    }

    public State state() {
        return state;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Source registration
    // ─────────────────────────────────────────────────────────────────────────────

    public boolean addSource(State state, BlockPos pos) {
        return state.sources.add(pos.toImmutable());
    }

    public boolean registerSource(State state, BlockPos pos) {
        return addSource(state, pos);
    }

    public boolean removeSource(State state, BlockPos pos) {
        return state.sources.remove(pos);
    }

    public boolean unregisterSource(State state, BlockPos pos) {
        return removeSource(state, pos);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Suppression (during collapse/fuse)
    // ─────────────────────────────────────────────────────────────────────────────

    public void suppressUnregister(State state, BlockPos pos) {
        state.suppressed.add(pos.toImmutable());
    }

    public void clearSuppressed(State state) {
        for (BlockPos pos : state.suppressed) {
            state.sources.remove(pos);
        }
        state.suppressed.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Query methods
    // ─────────────────────────────────────────────────────────────────────────────

    public int count(State state) {
        return state.sources.size();
    }

    public boolean hasSource(State state, BlockPos pos) {
        return state.sources.contains(pos);
    }

    public Set<BlockPos> sources(State state) {
        return Set.copyOf(state.sources);
    }

    public Set<BlockPos> view(State state) {
        return sources(state);
    }

    public boolean isEmpty(State state) {
        return state.sources.isEmpty();
    }

    public BlockPos representativePos(Random random, State state) {
        if (state.sources.isEmpty()) {
            return null;
        }
        List<BlockPos> list = new ArrayList<>(state.sources);
        return list.get(random.nextInt(list.size()));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Persistence
    // ─────────────────────────────────────────────────────────────────────────────

    public List<BlockPos> snapshot(State state) {
        return new ArrayList<>(state.sources);
    }

    public void restoreSnapshot(State state, List<BlockPos> sources) {
        state.sources.clear();
        if (sources != null) {
            for (BlockPos pos : sources) {
                state.sources.add(pos.toImmutable());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Clear / reset
    // ─────────────────────────────────────────────────────────────────────────────

    public void clear(State state) {
        state.sources.clear();
    }

    public void clearSources(State state) {
        clear(state);
    }

    public void forceContainmentReset(State state) {
        state.sources.clear();
        state.suppressed.clear();
        Logging.INFECTION.topic("sources").info("Force containment reset");
    }

    public void removeMissingSources(State state) {
        if (host == null || host.world() == null) {
            return;
        }
        state.sources.removeIf(pos -> {
            boolean missing = !host.world().getBlockState(pos).isOf(ModBlocks.VIRUS_BLOCK);
            if (missing) {
                Logging.INFECTION.topic("sources").debug("Removed missing source at {}", pos);
            }
            return missing;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Teleportation
    // ─────────────────────────────────────────────────────────────────────────────

    public void teleportSources() {
        // Default no-op, can be overridden by subclasses
        Logging.INFECTION.topic("sources").debug("Teleport sources (no-op)");
    }

    public boolean teleportSources(State state, int chunkRadius, float chance) {
        // Teleport logic - returns true if any sources moved
        if (state.sources.isEmpty()) {
            return false;
        }
        // For now, just log - actual implementation depends on game logic
        Logging.INFECTION.topic("sources").info("Teleport sources radius={} chance={}", chunkRadius, chance);
        return false;
    }

    public void tick() {
        // No-op for now
    }
}
