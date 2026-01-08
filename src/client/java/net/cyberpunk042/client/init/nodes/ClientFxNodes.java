package net.cyberpunk042.client.init.nodes;

import net.cyberpunk042.client.render.SingularityBorderClientState;
import net.cyberpunk042.client.render.SingularityVisualManager;
import net.cyberpunk042.client.render.VoidTearVisualManager;
import net.cyberpunk042.client.render.beam.GrowthBeamRenderer;
import net.cyberpunk042.client.render.field.GrowthRingFieldRenderer;
import net.cyberpunk042.init.InitNode;

/**
 * Client FX/visual effect initialization nodes.
 */
public final class ClientFxNodes {
    
    private ClientFxNodes() {}
    
    /**
     * Void tear visual effects.
     */
    public static final InitNode VOID_TEAR = InitNode.simple(
        "void_tear", "Void Tear FX",
        () -> {
            VoidTearVisualManager.init();
            return 1;
        }
    );
    
    /**
     * Singularity visual effects.
     */
    public static final InitNode SINGULARITY = InitNode.simple(
        "singularity", "Singularity FX",
        () -> {
            SingularityVisualManager.init();
            return 1;
        }
    );
    
    /**
     * Singularity border state.
     */
    public static final InitNode BORDER_STATE = InitNode.simple(
        "border_state", "Border State",
        () -> {
            SingularityBorderClientState.init();
            return 1;
        }
    );
    
    /**
     * Growth beam renderer.
     */
    public static final InitNode GROWTH_BEAM = InitNode.simple(
        "growth_beam", "Growth Beam",
        () -> {
            GrowthBeamRenderer.init();
            return 1;
        }
    );
    
    /**
     * Growth ring field renderer.
     */
    public static final InitNode GROWTH_RING = InitNode.simple(
        "growth_ring", "Growth Ring",
        () -> {
            GrowthRingFieldRenderer.init();
            return 1;
        }
    );
}
