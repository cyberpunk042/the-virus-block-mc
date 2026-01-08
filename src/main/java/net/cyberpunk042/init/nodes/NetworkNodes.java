package net.cyberpunk042.init.nodes;

import net.cyberpunk042.init.InitNode;
import net.cyberpunk042.network.*;
import net.cyberpunk042.network.gui.GuiPacketRegistration;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Network payload registration nodes.
 * 
 * <p>Registers S2C and C2S payload types with Fabric networking.
 */
public final class NetworkNodes {
    
    private NetworkNodes() {}
    
    /**
     * Server-to-client payload registration.
     */
    public static final InitNode S2C_PAYLOADS = InitNode.simple(
        "s2c_payloads", "S2C Payloads",
        () -> {
            PayloadTypeRegistry.playS2C().register(SkyTintPayload.ID, SkyTintPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(HorizonTintPayload.ID, HorizonTintPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(DifficultySyncPayload.ID, DifficultySyncPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(VoidTearSpawnPayload.ID, VoidTearSpawnPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(VoidTearBurstPayload.ID, VoidTearBurstPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(FieldSpawnPayload.ID, FieldSpawnPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(ShieldFieldSpawnPayload.ID, ShieldFieldSpawnPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(ShieldFieldRemovePayload.ID, ShieldFieldRemovePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(FieldRemovePayload.ID, FieldRemovePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(FieldUpdatePayload.ID, FieldUpdatePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(FieldDefinitionSyncPayload.ID, FieldDefinitionSyncPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(SingularityVisualStartPayload.ID, SingularityVisualStartPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(SingularityVisualStopPayload.ID, SingularityVisualStopPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(SingularityBorderPayload.ID, SingularityBorderPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(SingularitySchedulePayload.ID, SingularitySchedulePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(GrowthBeamPayload.ID, GrowthBeamPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(GrowthRingFieldPayload.ID, GrowthRingFieldPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(net.cyberpunk042.network.shader.ShaderTimeSyncS2CPayload.ID, 
                net.cyberpunk042.network.shader.ShaderTimeSyncS2CPayload.CODEC);
            return 18;
        }
    );
    
    /**
     * Client-to-server payload registration.
     */
    public static final InitNode C2S_PAYLOADS = InitNode.simple(
        "c2s_payloads", "C2S Payloads",
        () -> {
            PayloadTypeRegistry.playC2S().register(PurificationTotemSelectPayload.ID, PurificationTotemSelectPayload.CODEC);
            PayloadTypeRegistry.playC2S().register(VirusDifficultySelectPayload.ID, VirusDifficultySelectPayload.CODEC);
            return 2;
        }
    );
    
    /**
     * GUI packet registration (both directions).
     */
    public static final InitNode GUI_PAYLOADS = InitNode.simple(
        "gui_payloads", "GUI Payloads",
        () -> {
            GuiPacketRegistration.registerAll();
            return 1;
        }
    );
    
    /**
     * Server-side networking handlers.
     */
    public static final InitNode SERVER_HANDLERS = InitNode.simple(
        "server_handlers", "Server Handlers",
        () -> {
            GuiPacketRegistration.registerServerHandlers();
            net.cyberpunk042.network.shader.ShaderTimeSyncService.init();
            return 2;
        }
    ).dependsOn("gui_payloads");
}
