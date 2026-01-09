package net.cyberpunk042.client.visual.shader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.cyberpunk042.network.VirusBlockTelemetryPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Client-side state for virus block telemetry.
 * 
 * <p>Receives data from the server about all virus block positions.
 * This is independent of the helmet and works for all players.</p>
 */
public final class VirusBlockTelemetryState {
    
    private static final VirusBlockTelemetryState INSTANCE = new VirusBlockTelemetryState();
    
    private volatile float closestDistance = Float.MAX_VALUE;
    private volatile int totalCount = 0;
    private volatile List<BlockPos> nearbyPositions = Collections.emptyList();
    private volatile long lastUpdateTime = 0;
    
    private static final long DATA_TIMEOUT_MS = 2000;  // 2 seconds
    
    private VirusBlockTelemetryState() {}
    
    public static VirusBlockTelemetryState get() {
        return INSTANCE;
    }
    
    public void updateFromPayload(VirusBlockTelemetryPayload payload) {
        // Always update state - even empty packets keep connection alive
        this.closestDistance = payload.closestDistance();
        this.totalCount = payload.totalCount();
        this.nearbyPositions = new ArrayList<>(payload.nearbyPositions());
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean hasData() {
        return (System.currentTimeMillis() - lastUpdateTime) < DATA_TIMEOUT_MS;
    }
    
    public float getClosestDistance() {
        return closestDistance;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public List<BlockPos> getNearbyPositions() {
        return nearbyPositions;
    }
}
