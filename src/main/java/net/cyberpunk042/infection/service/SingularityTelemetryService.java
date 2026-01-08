package net.cyberpunk042.infection.service;

import java.util.List;
import java.util.Locale;

import net.cyberpunk042.infection.SingularityState;
import net.cyberpunk042.infection.singularity.SingularityDiagnostics;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.log.Topic;

/**
 * Emits collapse telemetry snapshots for diagnostics.
 */
public final class SingularityTelemetryService {
    
    private static final Topic LOG = Logging.SINGULARITY.topic("telemetry");

    public SingularityTelemetryService() {}

    public void logCollapseSnapshot(CollapseSnapshot snapshot) {
        if (!SingularityDiagnostics.enabled() || snapshot == null) {
            return;
        }
        String players = formatPlayers(snapshot.players());
        double minX = snapshot.centerX() - snapshot.radius();
        double maxX = snapshot.centerX() + snapshot.radius();
        double minZ = snapshot.centerZ() - snapshot.radius();
        double maxZ = snapshot.centerZ() + snapshot.radius();
        
        LOG.kv("center", round(snapshot.centerX()) + "," + round(snapshot.centerZ()))
           .kv("radius", round(snapshot.radius()))
           .kv("box", "[" + round(minX) + ".." + round(maxX) + "]x[" + round(minZ) + ".." + round(maxZ) + "]")
           .kv("ring", snapshot.ringIndex())
           .kv("pending", snapshot.pendingChunks())
           .kv("columns", snapshot.columnsPerTick())
           .kv("layers", snapshot.layersPerSlice())
           .kv("players", players)
           .info("[telemetry] Collapse snapshot");
    }

    public void logStateChange(SingularityState current, SingularityState next, String reason) {
        if (!SingularityDiagnostics.enabled()) {
            return;
        }
        LOG.info("[state] {} -> {} reason={}",
                current,
                next,
                (reason == null || reason.isBlank()) ? "<none>" : reason);
    }

    public void logCollapseStall(CollapseStallSnapshot snapshot) {
        if (!SingularityDiagnostics.enabled() || snapshot == null) {
            return;
        }
        int ticks = snapshot.stallTicks();
        if (ticks <= 0) {
            return;
        }
        int interval = snapshot.sampleIntervalTicks() > 0 ? snapshot.sampleIntervalTicks() : 20;
        if (ticks % interval != 0) {
            return;
        }
        LOG.kv("ticks", ticks)
           .kv("ringPending", snapshot.ringPendingChunks())
           .kv("enginePending", snapshot.enginePending())
           .kv("schedulerPending", snapshot.schedulerPending())
           .kv("preloadComplete", snapshot.preloadComplete())
           .kv("chunkQueue", snapshot.chunkQueueSize())
           .kv("cooldown", snapshot.cooldownTicks())
           .warn("[telemetry] Collapse stalled");
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static String formatPlayers(List<PlayerSample> players) {
        if (players == null || players.isEmpty()) {
            return "<none>";
        }
        StringBuilder builder = new StringBuilder();
        for (PlayerSample sample : players) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(sample.name())
                    .append("=")
                    .append(formatCoord(sample.x()))
                    .append(",")
                    .append(formatCoord(sample.y()))
                    .append(",")
                    .append(formatCoord(sample.z()));
        }
        return builder.toString();
    }

    private static String formatCoord(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public record CollapseSnapshot(
            double centerX,
            double centerZ,
            double radius,
            int ringIndex,
            int pendingChunks,
            int columnsPerTick,
            int layersPerSlice,
            List<PlayerSample> players) {
    }

    public record PlayerSample(String name, double x, double y, double z) {
    }

    public record CollapseStallSnapshot(
            int stallTicks,
            int ringPendingChunks,
            int enginePending,
            boolean schedulerPending,
            boolean preloadComplete,
            int chunkQueueSize,
            int cooldownTicks,
            int sampleIntervalTicks) {
    }
}
