package net.cyberpunk042.network.gui;

import net.minecraft.util.Identifier;

/**
 * G101: Packet identifiers for GUI network protocol.
 */
public final class GuiPacketIds {
    
    public static final String NAMESPACE = "the-virus-block";
    
    // Server -> Client
    public static final Identifier GUI_OPEN_S2C = id("gui_open");
    public static final Identifier PROFILE_SYNC_S2C = id("profile_sync_s2c");
    public static final Identifier DEBUG_FIELD_S2C = id("debug_field_s2c");
    public static final Identifier SERVER_PROFILES_S2C = id("server_profiles");
    public static final Identifier FIELD_EDIT_UPDATE_S2C = id("field_edit_update"); // Command→Client sync
    public static final Identifier SHOCKWAVE_TRIGGER_S2C = id("shockwave_trigger"); // Broadcast to all
    
    // Client -> Server
    public static final Identifier PROFILE_SAVE_C2S = id("profile_save");
    public static final Identifier PROFILE_LOAD_C2S = id("profile_load");
    public static final Identifier DEBUG_FIELD_C2S = id("debug_field_c2s");
    public static final Identifier REQUEST_PROFILES_C2S = id("request_profiles");
    public static final Identifier FORCE_FIELD_SPAWN_C2S = id("force_field_spawn");
    public static final Identifier SHOCKWAVE_FIELD_SPAWN_C2S = id("shockwave_field_spawn");
    
    private static Identifier id(String path) {
        return Identifier.of(NAMESPACE, path);
    }
    
    private GuiPacketIds() {}
}
