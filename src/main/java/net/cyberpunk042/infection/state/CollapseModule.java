package net.cyberpunk042.infection.state;

import java.util.Objects;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.service.CollapseExecutionService;
import net.cyberpunk042.infection.service.CollapseQueueService;
import net.cyberpunk042.infection.service.CollapseSnapshotService;
import net.cyberpunk042.infection.service.CollapseWatchdogService;

/**
 * Consolidates collapse-related services into a single module.
 */
public final class CollapseModule {
    private final VirusWorldState host;
    private final CollapseExecutionService executionService;
    private final CollapseQueueService queueService;
    private final CollapseWatchdogService watchdogService;
    private final CollapseSnapshotService snapshotService;

    public CollapseModule(VirusWorldState host) {
        this.host = Objects.requireNonNull(host, "host");
        this.executionService = new CollapseExecutionService(host);
        this.queueService = new CollapseQueueService(host);
        this.watchdogService = new CollapseWatchdogService(host);
        this.snapshotService = new CollapseSnapshotService(host);
    }

    public CollapseExecutionService execution() {
        return executionService;
    }

    public CollapseQueueService queues() {
        return queueService;
    }

    public CollapseWatchdogService watchdog() {
        return watchdogService;
    }

    public CollapseSnapshotService snapshot() {
        return snapshotService;
    }
}
