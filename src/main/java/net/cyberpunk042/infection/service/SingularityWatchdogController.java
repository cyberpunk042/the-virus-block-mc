package net.cyberpunk042.infection.service;

import java.util.function.Consumer;

import net.cyberpunk042.log.Logging;
import net.cyberpunk042.log.Topic;

/**
 * Handles the per-world fuse/collapse watchdog counters. Detects stalls and
 * can auto-skip if configured. All configuration flows through {@link InfectionServices}.
 */
public final class SingularityWatchdogController {
    
    private static final Topic LOG = Logging.SINGULARITY.topic("watchdog");
    private static final ServiceConfig.SingularityWatchdogs DEFAULT_CONFIG = new ServiceConfig.SingularityWatchdogs();
    private static final ServiceConfig.Scheduler DEFAULT_SCHEDULER_CONFIG = new ServiceConfig.Scheduler();
    private static final String PROBE_FUSE = "singularity.fuse";
    private static final String PROBE_COLLAPSE_WARN = "singularity.collapse.warn";
    private static final String PROBE_COLLAPSE_ABORT = "singularity.collapse.abort";
    private static final String PROBE_SCHEDULER_WARN = "scheduler.queue.warn";
    private static final String PROBE_SCHEDULER_ABORT = "scheduler.queue.abort";

    private int fuseWatchdogTicks;
    private int collapseStallTicks;

    public void resetAll() {
        resetFuseWatchdog();
        resetCollapseStallTicks();
    }

    public void onStateTransition(boolean fuseActive, boolean collapseActive) {
        if (!fuseActive) {
            resetFuseWatchdog();
        }
        if (!collapseActive) {
            resetCollapseStallTicks();
        }
    }

    public void resetCollapseStallTicks() {
        collapseStallTicks = 0;
        clearProbe(PROBE_COLLAPSE_WARN);
        clearProbe(PROBE_COLLAPSE_ABORT);
    }

    public int collapseStallTicks() {
        return collapseStallTicks;
    }

    public int recordCollapseStall(CollapseStallSample sample) {
        collapseStallTicks++;
        ServiceConfig.CollapsePhaseWatchdog config = collapseWatchdogConfig();
        if (!config.enabled) {
            clearProbe(PROBE_COLLAPSE_WARN);
            clearProbe(PROBE_COLLAPSE_ABORT);
            return collapseStallTicks;
        }
        int warn = collapseWatchdogWarnTicks(config);
        int abort = collapseWatchdogAbortTicks(config);
        if (warn > 0 && collapseStallTicks == warn) {
            LOG.kv("ringPending", sample.ringPendingChunks())
               .kv("queue", sample.chunkQueueSize())
               .kv("schedulerPending", sample.schedulerPending())
               .warn("[watchdog] Collapse stall warning");
            recordProbe(PROBE_COLLAPSE_WARN, collapseStallTicks, warn);
        }
        if (abort > 0 && collapseStallTicks >= abort) {
            LOG.kv("ringPending", sample.ringPendingChunks())
               .kv("queue", sample.chunkQueueSize())
               .kv("autoSkip", config.autoSkip)
               .error("[watchdog] Collapse abort threshold reached");
            recordProbe(PROBE_COLLAPSE_ABORT, collapseStallTicks, abort);
            collapseStallTicks = 0;
            if (config.autoSkip) {
                sample.autoSkip().accept("collapse watchdog exceeded " + abort + " ticks");
            } else {
                clearProbe(PROBE_COLLAPSE_ABORT);
            }
        }
        return collapseStallTicks;
    }

    public void tickFuseWatchdog(FuseWatchdogSample sample) {
        ServiceConfig.FuseWatchdog config = fuseWatchdogConfig();
        if (!config.enabled) {
            resetFuseWatchdog();
            return;
        }
        if (!sample.fuseActive() || !sample.preloadComplete()) {
            resetFuseWatchdog();
            return;
        }
        fuseWatchdogTicks++;
        int limit = fuseWatchdogMaxExtraTicks(config);
        if (limit > 0 && fuseWatchdogTicks >= limit) {
            LOG.kv("ticks", fuseWatchdogTicks)
               .kv("preloadComplete", sample.preloadComplete())
               .kv("collapseDelay", sample.collapseBarDelay())
               .kv("autoSkip", config.autoSkip)
               .warn("[watchdog] Fuse stall threshold reached");
            recordProbe(PROBE_FUSE, fuseWatchdogTicks, limit);
            fuseWatchdogTicks = 0;
            if (config.autoSkip) {
                sample.autoSkip().accept("fuse watchdog exceeded " + limit + " ticks");
            } else {
                clearProbe(PROBE_FUSE);
            }
        }
    }

    public void monitorSchedulerBacklog(int backlog) {
        ServiceConfig.Scheduler config = schedulerWatchdogConfig();
        if (!config.enabled) {
            clearProbe(PROBE_SCHEDULER_WARN);
            clearProbe(PROBE_SCHEDULER_ABORT);
            return;
        }
        if (config.warnTasks > 0) {
            if (backlog >= config.warnTasks) {
                recordProbe(PROBE_SCHEDULER_WARN, backlog, config.warnTasks);
            } else {
                clearProbe(PROBE_SCHEDULER_WARN);
            }
        }
        if (config.abortTasks > 0) {
            if (backlog >= config.abortTasks) {
                recordProbe(PROBE_SCHEDULER_ABORT, backlog, config.abortTasks);
            } else {
                clearProbe(PROBE_SCHEDULER_ABORT);
            }
        }
    }

    private void resetFuseWatchdog() {
        fuseWatchdogTicks = 0;
        clearProbe(PROBE_FUSE);
    }

    private ServiceConfig.SingularityWatchdogs singularityWatchdogConfig() {
        InfectionServiceContainer services = InfectionServices.container();
        ServiceConfig settings = services != null ? services.settings() : null;
        ServiceConfig.Watchdog watcher = settings != null ? settings.watchdog : null;
        ServiceConfig.SingularityWatchdogs config = watcher != null ? watcher.singularity : null;
        return config != null ? config : DEFAULT_CONFIG;
    }

    private ServiceConfig.FuseWatchdog fuseWatchdogConfig() {
        ServiceConfig.SingularityWatchdogs config = singularityWatchdogConfig();
        ServiceConfig.FuseWatchdog fuse = config != null ? config.fuse : null;
        return fuse != null ? fuse : DEFAULT_CONFIG.fuse;
    }

    private ServiceConfig.CollapsePhaseWatchdog collapseWatchdogConfig() {
        ServiceConfig.SingularityWatchdogs config = singularityWatchdogConfig();
        ServiceConfig.CollapsePhaseWatchdog collapse = config != null ? config.collapse : null;
        return collapse != null ? collapse : DEFAULT_CONFIG.collapse;
    }

    private ServiceConfig.Scheduler schedulerWatchdogConfig() {
        InfectionServiceContainer services = InfectionServices.container();
        ServiceConfig settings = services != null ? services.settings() : null;
        ServiceConfig.Watchdog watcher = settings != null ? settings.watchdog : null;
        ServiceConfig.Scheduler scheduler = watcher != null ? watcher.scheduler : null;
        return scheduler != null ? scheduler : DEFAULT_SCHEDULER_CONFIG;
    }

    private int fuseWatchdogMaxExtraTicks(ServiceConfig.FuseWatchdog config) {
        int configured = config.maxExtraTicks;
        return configured > 0 ? configured : DEFAULT_CONFIG.fuse.maxExtraTicks;
    }

    private int collapseWatchdogWarnTicks(ServiceConfig.CollapsePhaseWatchdog config) {
        int configured = config.warnTicks;
        return configured > 0 ? configured : DEFAULT_CONFIG.collapse.warnTicks;
    }

    private int collapseWatchdogAbortTicks(ServiceConfig.CollapsePhaseWatchdog config) {
        int configured = config.abortTicks;
        return configured > 0 ? configured : DEFAULT_CONFIG.collapse.abortTicks;
    }

    private void recordProbe(String id, double value, double threshold) {
        WatchdogService watchdog = watchdogService();
        if (watchdog != null) {
            watchdog.record(id, value, threshold);
        }
    }

    private void clearProbe(String id) {
        WatchdogService watchdog = watchdogService();
        if (watchdog != null) {
            watchdog.clear(id);
        }
    }

    private WatchdogService watchdogService() {
        InfectionServiceContainer services = InfectionServices.container();
        return services != null ? services.watchdog() : null;
    }

    public record FuseWatchdogSample(
            boolean fuseActive,
            boolean preloadComplete,
            int collapseBarDelay,
            Consumer<String> autoSkip) {
    }

    public record CollapseStallSample(
            int ringPendingChunks,
            int chunkQueueSize,
            boolean schedulerPending,
            Consumer<String> autoSkip) {
    }
}
